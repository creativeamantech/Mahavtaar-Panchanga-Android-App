package com.mahavtaar.panchanga.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.audio.VedicVoiceAudioController
import com.mahavtaar.panchanga.domain.model.AlertDeliveryMode
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.ScheduledAlertStatus
import com.mahavtaar.panchanga.domain.model.VoiceSourceType
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject

/**
 * Receives scheduled exact Vedic transition alarms, acquires a CPU wake lock so screen-off
 * devices never delay delivery, posts the bilingual Hindi/English notification banner, and
 * launches [VedicAnnouncementService] so custom tune + TTS voice playback completes uninterrupted.
 */
@AndroidEntryPoint
class VedicAlertAlarmReceiver : BroadcastReceiver() {

    companion object {
        private val receiverExceptionHandler = CoroutineExceptionHandler { _, _ -> }
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + receiverExceptionHandler)
        private val recentDeliveries = java.util.concurrent.ConcurrentHashMap<String, Long>()
    }

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var notificationStudioRepository: NotificationStudioRepository

    @Inject
    lateinit var audioController: VedicVoiceAudioController

    @Inject
    lateinit var notificationScheduler: PanchangaNotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val alertId = intent.getStringExtra(PanchangaNotificationScheduler.EXTRA_ALERT_ID) ?: return
        val pendingResult = goAsync()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = runCatching {
            powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "mahavtaar:vedic_alert_wakelock"
            )?.apply {
                setReferenceCounted(false)
                acquire(15_000L)
            }
        }.getOrNull()

        receiverScope.launch {
            try {
                // Immediately refresh all Home Screen live widgets at the exact transition boundary
                runCatching {
                    com.mahavtaar.panchanga.widget.WidgetUpdateHelper.updateAllWidgets(context.applicationContext)
                }

                val item = notificationStudioRepository.getScheduledAlertById(alertId) ?: return@launch
                if (item.status == ScheduledAlertStatus.DELIVERED) return@launch

                val nowMs = System.currentTimeMillis()
                val dedupKey = "${item.eventType.name}_${item.isAdvanceWarning}_${item.triggerTimeMs / 30_000L}"
                val prevDeliveredAt = recentDeliveries.put(dedupKey, nowMs)
                if (prevDeliveredAt != null && (nowMs - prevDeliveredAt) < 45_000L) {
                    notificationStudioRepository.markAlertStatus(alertId, ScheduledAlertStatus.DELIVERED, nowMs)
                    return@launch
                }

                val audioSettings = notificationStudioRepository.getAudioSettings()

                if (!audioSettings.masterEnabled) {
                    notificationStudioRepository.markAlertStatus(alertId, ScheduledAlertStatus.SUPPRESSED_QUIET, nowMs)
                    return@launch
                }

                if (audioSettings.isManualDndActive(nowMs)) {
                    notificationStudioRepository.markAlertStatus(alertId, ScheduledAlertStatus.SUPPRESSED_DND, nowMs)
                    return@launch
                }

                val prefs = preferencesRepository.preferences.first()
                val zone = TimeFormatters.safeZoneId(prefs.selectedLocation.timezone)
                val localNow = Instant.ofEpochMilli(nowMs).atZone(zone)
                val minuteOfDay = localNow.hour * 60 + localNow.minute

                if (audioSettings.isWithinQuietHours(minuteOfDay)) {
                    notificationStudioRepository.markAlertStatus(alertId, ScheduledAlertStatus.SUPPRESSED_QUIET, nowMs)
                    return@launch
                }

                if (audioSettings.respectSilentAndDnd && audioController.isDeviceInDndInterruptionMode()) {
                    notificationStudioRepository.markAlertStatus(alertId, ScheduledAlertStatus.SUPPRESSED_DND, nowMs)
                    return@launch
                }

                val isHindi = prefs.language == AppLanguage.HINDI
                val title = if (isHindi) item.titleHi else item.titleEn
                val body = if (isHindi) item.bodyHi else item.bodyEn
                val speech = if (isHindi) item.speechHi else item.speechEn
                val fallbackSpeech = if (isHindi) item.speechEn else item.speechHi

                // 1) Immediately post the visual notification banner at the exact second
                notificationScheduler.postLocalizedNotification(
                    notificationId = item.requestCode,
                    title = title,
                    body = body
                )

                if (audioController.shouldSuppressForDeviceAudioState(audioSettings)) {
                    notificationStudioRepository.markAlertStatus(alertId, ScheduledAlertStatus.SUPPRESSED_DND, nowMs)
                    return@launch
                }

                notificationStudioRepository.markAlertStatus(alertId, ScheduledAlertStatus.DELIVERED, nowMs)
                if (item.deliveryMode == AlertDeliveryMode.SILENT_BANNER) {
                    return@launch
                }

                val recordingPath = if (item.voiceSource == VoiceSourceType.CUSTOM_RECORDING) {
                    item.voiceRecordingId?.takeIf { it.isNotBlank() }?.let { recId ->
                        notificationStudioRepository.getVoiceRecording(recId)?.filePath
                    }
                } else {
                    null
                }

                // 2) Hand off audio tune + TTS playback to ForegroundService so Android never freezes playback
                val serviceIntent = VedicAnnouncementService.createIntent(
                    context = context.applicationContext,
                    notificationId = item.requestCode,
                    title = title,
                    body = body,
                    speech = speech,
                    fallbackSpeech = fallbackSpeech,
                    language = prefs.language,
                    deliveryMode = item.deliveryMode,
                    tuneSource = item.tuneSource,
                    tuneUriOrId = item.tuneUriOrId,
                    voiceSource = item.voiceSource,
                    recordingPath = recordingPath
                )

                val startedForeground = runCatching {
                    ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
                }.isSuccess

                if (!startedForeground) {
                    // Fallback if FGS is blocked on an edge OEM device without exact alarm exemption
                    withTimeoutOrNull(8_000L) {
                        audioController.playFullAlertSequence(
                            deliveryMode = item.deliveryMode,
                            tuneSource = item.tuneSource,
                            tuneUriOrId = item.tuneUriOrId,
                            voiceSource = item.voiceSource,
                            customRecordingPath = recordingPath,
                            spokenText = speech,
                            language = prefs.language,
                            audioSettings = audioSettings,
                            ignoreSilentCheck = false,
                            fallbackText = fallbackSpeech,
                            isInAppPreview = false
                        )
                    }
                }
            } finally {
                runCatching { pendingResult.finish() }
                runCatching {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                }
            }
        }
    }
}
