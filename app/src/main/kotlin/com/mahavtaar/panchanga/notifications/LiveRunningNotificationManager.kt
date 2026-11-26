package com.mahavtaar.panchanga.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mahavtaar.panchanga.MainActivity
import com.mahavtaar.panchanga.R
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.audio.VedicVoiceAudioController
import com.mahavtaar.panchanga.data.notifications.RunningNowNotificationBuilder
import com.mahavtaar.panchanga.data.notifications.RunningNowNotificationContent
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AlertDeliveryMode
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import com.mahavtaar.panchanga.domain.repository.PanchangaRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates the opt-in permanent "Running Now" high-priority status notification.
 * Displays live Hora, Sub-Tattva, Nadi, and Tithi with exact Start and End timestamps
 * (including Date + Seconds), refreshes automatically at exact transition boundaries via
 * AlarmManager + WorkManager, and plays a soft chime when the active Sub-Tattva changes.
 */
@Singleton
class LiveRunningNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val panchangaRepository: PanchangaRepository,
    private val notificationStudioRepository: NotificationStudioRepository,
    private val audioController: VedicVoiceAudioController
) {
    companion object {
        const val CHANNEL_ID = "panchanga_running_now_persistent"
        const val NOTIFICATION_ID = 44_950
        private const val REQUEST_CODE_ALARM = 44_951
        private const val REQUEST_CODE_CONTENT = 44_952
        private const val REQUEST_CODE_TURN_OFF = 44_953
        private const val PERIODIC_WORK_NAME = "mahavtaar_running_now_periodic"

        const val ACTION_REFRESH_RUNNING_NOW = "com.mahavtaar.panchanga.ACTION_REFRESH_RUNNING_NOW"
        const val ACTION_TURN_OFF_RUNNING_NOW = "com.mahavtaar.panchanga.ACTION_TURN_OFF_RUNNING_NOW"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val syncMutex = Mutex()

    fun ensureChannelExists() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.running_now_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.running_now_channel_description)
            // Keep OS channel sound & vibration silent so updates don't buzz; soft chime plays manually only on Sub-Tattva change.
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun syncAsync(allowChimeOnTattvaChange: Boolean) {
        scope.launch {
            runCatching { sync(allowChimeOnTattvaChange) }
        }
    }

    suspend fun turnOffByUserAction() {
        val current = notificationStudioRepository.getAudioSettings()
        if (current.persistentRunningNotificationEnabled) {
            notificationStudioRepository.updateAudioSettings(
                current.copy(persistentRunningNotificationEnabled = false)
            )
        }
        sync(allowChimeOnTattvaChange = false)
    }

    suspend fun sync(allowChimeOnTattvaChange: Boolean) = syncMutex.withLock {
        val audioSettings = notificationStudioRepository.getAudioSettings()
        if (!audioSettings.persistentRunningNotificationEnabled || !hasNotificationPermission()) {
            cancelAllInternal()
            return@withLock
        }

        ensureChannelExists()

        val prefs = preferencesRepository.preferences.first()
        val nowMs = System.currentTimeMillis()
        val zone = TimeFormatters.safeZoneId(prefs.selectedLocation.timezone)
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

        val todaySnap = panchangaRepository.getDailyPanchanga(
            location = prefs.selectedLocation,
            date = today,
            monthSystem = prefs.monthSystem,
            ayanamsha = prefs.ayanamsha,
            includeExtendedTransits = false
        )
        val yesterdaySnap = if (nowMs < todaySnap.sunriseMs) {
            runCatching {
                panchangaRepository.getDailyPanchanga(
                    location = prefs.selectedLocation,
                    date = today.minusDays(1),
                    monthSystem = prefs.monthSystem,
                    ayanamsha = prefs.ayanamsha,
                    includeExtendedTransits = false
                )
            }.getOrNull()
        } else {
            null
        }

        val content = RunningNowNotificationBuilder.build(
            todaySnapshot = todaySnap,
            yesterdaySnapshot = yesterdaySnap,
            nowMs = nowMs,
            language = prefs.language
        )

        val notification = buildNotification(content, prefs.language)
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        // Play soft chime strictly when active Sub-Tattva changes from previous tracked key
        val previousKey = notificationStudioRepository.getLastRunningTattvaKey()
        if (content.tattvaTransitionKey != previousKey) {
            notificationStudioRepository.setLastRunningTattvaKey(content.tattvaTransitionKey)
        }

        val shouldChime = allowChimeOnTattvaChange &&
            !audioSettings.isManualDndActive(nowMs) &&
            previousKey.isNotBlank() &&
            content.tattvaTransitionKey.isNotBlank() &&
            content.tattvaTransitionKey != previousKey &&
            !isWithinQuietHours(nowMs, zone, audioSettings) &&
            !isScheduledSubTattvaRuleAudible(audioSettings)

        if (shouldChime) {
            runCatching {
                audioController.playSoftChimeIfAllowed(audioSettings)
            }
        }

        scheduleNextBoundaryWakeup(content.nextTransitionAtMs, nowMs)
        ensurePeriodicWorker()
    }

    fun buildNotification(
        content: RunningNowNotificationContent,
        language: AppLanguage
    ): Notification {
        ensureChannelExists()

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_CONTENT,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val turnOffIntent = Intent(context, LiveRunningNotificationReceiver::class.java).apply {
            action = ACTION_TURN_OFF_RUNNING_NOW
        }
        val turnOffPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_TURN_OFF,
            turnOffIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val turnOffLabel = if (language == AppLanguage.HINDI) "बंद करें" else "Turn Off"

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.mahavtaar.panchanga.ui.R.drawable.ic_notification_panchanga)
            .setContentTitle(content.title)
            .setContentText(content.collapsedText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.expandedBigText))
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .addAction(0, turnOffLabel, turnOffPendingIntent)
            .build()
    }

    private suspend fun isScheduledSubTattvaRuleAudible(audioSettings: AlertAudioSettings): Boolean {
        if (!audioSettings.masterEnabled) return false
        val rule = runCatching {
            notificationStudioRepository.getRule(VedicAlertEventType.HORA_SUB_TATTVA)
        }.getOrNull() ?: return false
        return rule.isEnabled && rule.notifyAtStart && rule.deliveryMode != AlertDeliveryMode.SILENT_BANNER
    }

    private fun isWithinQuietHours(nowMs: Long, zone: ZoneId, audioSettings: AlertAudioSettings): Boolean {
        val localNow = Instant.ofEpochMilli(nowMs).atZone(zone)
        val minuteOfDay = localNow.hour * 60 + localNow.minute
        return audioSettings.isWithinQuietHours(minuteOfDay)
    }

    private suspend fun cancelAllInternal() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
        val am = alarmManager
        if (am != null) {
            val pi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ALARM,
                Intent(context, LiveRunningNotificationReceiver::class.java).apply {
                    action = ACTION_REFRESH_RUNNING_NOW
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                runCatching { am.cancel(pi) }
                runCatching { pi.cancel() }
            }
        }
        runCatching {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        }
        runCatching {
            if (notificationStudioRepository.getLastRunningTattvaKey().isNotEmpty()) {
                notificationStudioRepository.setLastRunningTattvaKey("")
            }
        }
    }

    private fun scheduleNextBoundaryWakeup(nextTransitionAtMs: Long, nowMs: Long) {
        val am = alarmManager ?: return
        val triggerAtMs = maxOf(nowMs + 2_000L, nextTransitionAtMs + 1_000L)
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            Intent(context, LiveRunningNotificationReceiver::class.java).apply {
                action = ACTION_REFRESH_RUNNING_NOW
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        runCatching {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        }
    }

    private fun ensurePeriodicWorker() {
        runCatching {
            val request = PeriodicWorkRequestBuilder<RunningNowWorker>(15L, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
