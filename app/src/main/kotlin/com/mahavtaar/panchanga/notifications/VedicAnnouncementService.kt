package com.mahavtaar.panchanga.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.audio.VedicVoiceAudioController
import com.mahavtaar.panchanga.domain.model.AlertDeliveryMode
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.TuneSourceType
import com.mahavtaar.panchanga.domain.model.VoiceSourceType
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Short-lived mediaPlayback Foreground Service that keeps the app process awake and exempt
 * from Android background process freezing while sequential Vedic tune + TTS voice alerts play.
 */
@AndroidEntryPoint
class VedicAnnouncementService : Service() {

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notif_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_SPEECH = "extra_speech"
        const val EXTRA_FALLBACK_SPEECH = "extra_fallback_speech"
        const val EXTRA_LANGUAGE = "extra_language"
        const val EXTRA_DELIVERY_MODE = "extra_delivery_mode"
        const val EXTRA_TUNE_SOURCE = "extra_tune_source"
        const val EXTRA_TUNE_URI = "extra_tune_uri"
        const val EXTRA_VOICE_SOURCE = "extra_voice_source"
        const val EXTRA_RECORDING_PATH = "extra_recording_path"

        fun createIntent(
            context: Context,
            notificationId: Int,
            title: String,
            body: String,
            speech: String,
            fallbackSpeech: String,
            language: AppLanguage,
            deliveryMode: AlertDeliveryMode,
            tuneSource: TuneSourceType,
            tuneUriOrId: String,
            voiceSource: VoiceSourceType,
            recordingPath: String?
        ): Intent = Intent(context, VedicAnnouncementService::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_SPEECH, speech)
            putExtra(EXTRA_FALLBACK_SPEECH, fallbackSpeech)
            putExtra(EXTRA_LANGUAGE, language.name)
            putExtra(EXTRA_DELIVERY_MODE, deliveryMode.name)
            putExtra(EXTRA_TUNE_SOURCE, tuneSource.name)
            putExtra(EXTRA_TUNE_URI, tuneUriOrId)
            putExtra(EXTRA_VOICE_SOURCE, voiceSource.name)
            if (!recordingPath.isNullOrBlank()) {
                putExtra(EXTRA_RECORDING_PATH, recordingPath)
            }
        }
    }

    @Inject
    lateinit var audioController: VedicVoiceAudioController

    @Inject
    lateinit var notificationStudioRepository: NotificationStudioRepository

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var notificationScheduler: PanchangaNotificationScheduler

    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + exceptionHandler)
    private val activeTasks = AtomicInteger(0)
    private val lifecycleLock = Any()

    @Volatile
    private var latestStartId: Int = 0

    @Volatile
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationId = intent?.getIntExtra(EXTRA_NOTIFICATION_ID, 44_001) ?: 44_001
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Mahavtaar Panchanga" }
        val body = intent?.getStringExtra(EXTRA_BODY).orEmpty()
        val speech = intent?.getStringExtra(EXTRA_SPEECH).orEmpty()
        val fallbackSpeech = intent?.getStringExtra(EXTRA_FALLBACK_SPEECH).orEmpty()
        val language = intent?.getStringExtra(EXTRA_LANGUAGE)?.let {
            runCatching { AppLanguage.valueOf(it) }.getOrNull()
        } ?: AppLanguage.ENGLISH
        val deliveryMode = intent?.getStringExtra(EXTRA_DELIVERY_MODE)?.let {
            runCatching { AlertDeliveryMode.valueOf(it) }.getOrNull()
        } ?: AlertDeliveryMode.TUNE_AND_VOICE
        val tuneSource = intent?.getStringExtra(EXTRA_TUNE_SOURCE)?.let {
            runCatching { TuneSourceType.valueOf(it) }.getOrNull()
        } ?: TuneSourceType.BUILTIN_VEDIC
        val tuneUriOrId = intent?.getStringExtra(EXTRA_TUNE_URI).orEmpty()
        val voiceSource = intent?.getStringExtra(EXTRA_VOICE_SOURCE)?.let {
            runCatching { VoiceSourceType.valueOf(it) }.getOrNull()
        } ?: VoiceSourceType.BUILTIN_TTS
        val recordingPath = intent?.getStringExtra(EXTRA_RECORDING_PATH)

        val notification = notificationScheduler.buildAlertNotification(notificationId, title, body)
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(this, notificationId, notification, fgType)
        }

        synchronized(lifecycleLock) {
            latestStartId = startId
            activeTasks.incrementAndGet()
            if (wakeLock?.isHeld != true) {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = runCatching {
                    pm?.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "mahavtaar:vedic_announcement_service"
                    )?.apply {
                        setReferenceCounted(false)
                        acquire(45_000L)
                    }
                }.getOrNull()
            }
        }

        serviceScope.launch {
            try {
                val audioSettings = notificationStudioRepository.getAudioSettings()
                val prefs = preferencesRepository.preferences.first()
                val zone = TimeFormatters.safeZoneId(prefs.selectedLocation.timezone)
                val nowMs = System.currentTimeMillis()
                val localNow = Instant.ofEpochMilli(nowMs).atZone(zone)
                val minuteOfDay = localNow.hour * 60 + localNow.minute
                if (audioSettings.masterEnabled &&
                    !audioSettings.isManualDndActive(nowMs) &&
                    !audioSettings.isWithinQuietHours(minuteOfDay)
                ) {
                    audioController.playFullAlertSequence(
                        deliveryMode = deliveryMode,
                        tuneSource = tuneSource,
                        tuneUriOrId = tuneUriOrId,
                        voiceSource = voiceSource,
                        customRecordingPath = recordingPath,
                        spokenText = speech,
                        language = language,
                        audioSettings = audioSettings,
                        ignoreSilentCheck = false,
                        fallbackText = fallbackSpeech,
                        isInAppPreview = false
                    )
                }
            } finally {
                synchronized(lifecycleLock) {
                    val remaining = activeTasks.decrementAndGet()
                    if (remaining <= 0 && latestStartId == startId) {
                        runCatching {
                            if (wakeLock?.isHeld == true) {
                                wakeLock?.release()
                            }
                        }
                        runCatching {
                            ServiceCompat.stopForeground(
                                this@VedicAnnouncementService,
                                ServiceCompat.STOP_FOREGROUND_DETACH
                            )
                        }
                        stopSelf(startId)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
