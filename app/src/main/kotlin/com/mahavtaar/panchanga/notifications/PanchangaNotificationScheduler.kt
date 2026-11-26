package com.mahavtaar.panchanga.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mahavtaar.panchanga.MainActivity
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.audio.VedicVoiceAudioController
import com.mahavtaar.panchanga.data.notifications.VedicAlertHorizonPlanner
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NotificationRule
import com.mahavtaar.panchanga.domain.model.ScheduledAlertItem
import com.mahavtaar.panchanga.domain.model.TuneSourceType
import com.mahavtaar.panchanga.domain.model.VoiceRecording
import com.mahavtaar.panchanga.domain.model.VoiceSourceType
import com.mahavtaar.panchanga.domain.notifications.AlertEnginePort
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import com.mahavtaar.panchanga.domain.repository.PanchangaRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(FlowPreview::class)
@Singleton
class PanchangaNotificationScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val panchangaRepository: PanchangaRepository,
    private val notificationStudioRepository: NotificationStudioRepository,
    private val audioController: VedicVoiceAudioController,
    private val liveRunningNotificationManager: LiveRunningNotificationManager
) : AlertEnginePort {

    companion object {
        const val CHANNEL_ID = "panchanga_vedic_alerts_managed"
        const val EXTRA_ALERT_ID = "extra_vedic_alert_id"
        private const val WORK_NAME = "mahavtaar_panchanga_daily_alerts"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val rescheduleMutex = Mutex()
    private val reactiveObserverStarted = AtomicBoolean(false)

    fun ensureDailySchedule() {
        schedulerScope.launch {
            runCatching {
                val request = PeriodicWorkRequestBuilder<PanchangaAlertWorker>(6, TimeUnit.HOURS).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }
        }
        startReactiveRescheduleObserverIfNeeded()
    }

    private fun startReactiveRescheduleObserverIfNeeded() {
        if (!reactiveObserverStarted.compareAndSet(false, true)) return
        schedulerScope.launch {
            combine(
                preferencesRepository.preferences,
                notificationStudioRepository.observeRules(),
                notificationStudioRepository.observeAudioSettings()
            ) { prefs, rules, audio ->
                val enabledSig = rules.filter { it.isEnabled }.joinToString("|") {
                    "${it.eventType.name}:${it.leadTimeMinutes}:${it.notifyAtStart}:${it.deliveryMode}:${it.tuneSource}:${it.tuneUriOrId}:${it.voiceSource}:${it.voiceRecordingId}:${it.customHindiTemplate.hashCode()}:${it.customEnglishTemplate.hashCode()}"
                }
                val loc = prefs.selectedLocation
                "${loc.name}:${loc.latitude}:${loc.longitude}:${loc.timezone}:${prefs.monthSystem}:${prefs.ayanamsha}:${prefs.notifySunrise}:${prefs.notifySunset}:${prefs.notifyTithiChange}:${prefs.notifyVratReminders}:${audio.masterEnabled}:${audio.manualDndEnabled}:${audio.manualDndUntilMs}:${audio.quietHoursEnabled}:${audio.quietStartMinuteOfDay}:${audio.quietEndMinuteOfDay}:${audio.respectSilentAndDnd}:$enabledSig"
            }
                .distinctUntilChanged()
                .debounce(650L)
                .catch { /* Never crash app process if Room/DataStore emits an I/O error */ }
                .collect {
                    runCatching { rescheduleAllAlerts() }
                }
        }
        schedulerScope.launch {
            combine(
                preferencesRepository.preferences,
                notificationStudioRepository.observeAudioSettings()
            ) { prefs, audio ->
                val loc = prefs.selectedLocation
                "${audio.persistentRunningNotificationEnabled}:${audio.manualDndEnabled}:${audio.manualDndUntilMs}:${audio.quietHoursEnabled}:${audio.quietStartMinuteOfDay}:${audio.quietEndMinuteOfDay}:${audio.respectSilentAndDnd}:${prefs.language.code}:${loc.name}:${loc.latitude}:${loc.longitude}:${loc.timezone}:${prefs.monthSystem}:${prefs.ayanamsha}"
            }
                .distinctUntilChanged()
                .debounce(100L)
                .catch { /* Ignore I/O errors cleanly */ }
                .collect {
                    runCatching { liveRunningNotificationManager.sync(allowChimeOnTattvaChange = false) }
                }
        }
    }

    fun rescheduleNow() {
        startReactiveRescheduleObserverIfNeeded()
        schedulerScope.launch {
            runCatching {
                val request = PeriodicWorkRequestBuilder<PanchangaAlertWorker>(6, TimeUnit.HOURS).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }
            runCatching { rescheduleAllAlerts() }
            runCatching { liveRunningNotificationManager.sync(allowChimeOnTattvaChange = false) }
        }
    }

    override fun canScheduleExactAlarms(): Boolean {
        val am = alarmManager ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    override fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun rescheduleAllAlerts(): Int = rescheduleMutex.withLock {
        val previous = runCatching {
            notificationStudioRepository.observeScheduledAlerts().first()
        }.getOrDefault(emptyList())

        previous.forEach { cancelAlarm(it) }

        val audioSettings = notificationStudioRepository.getAudioSettings()
        if (!audioSettings.masterEnabled) {
            notificationStudioRepository.replaceScheduledAlerts(emptyList())
            runCatching { com.mahavtaar.panchanga.widget.WidgetUpdateHelper.updateAllWidgets(context) }
            return@withLock 0
        }

        val rules = notificationStudioRepository.getAllRules()
        if (rules.none { it.isEnabled }) {
            notificationStudioRepository.replaceScheduledAlerts(emptyList())
            runCatching { com.mahavtaar.panchanga.widget.WidgetUpdateHelper.updateAllWidgets(context) }
            return@withLock 0
        }

        val prefs = preferencesRepository.preferences.first()
        val zone = TimeFormatters.safeZoneId(prefs.selectedLocation.timezone)
        val initialNowMs = System.currentTimeMillis()
        val today = Instant.ofEpochMilli(initialNowMs).atZone(zone).toLocalDate()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)

        // Compute yesterday (for pre-sunrise hours 00:00..sunrise), today, and tomorrow
        // passing snap.sunriseMs + 1_000L so HoraEngine always computes that snapshot's own 24 Horas
        val daySnapshots = listOf(yesterday, today, tomorrow).mapNotNull { targetDate ->
            runCatching {
                val snap = panchangaRepository.getDailyPanchanga(
                    location = prefs.selectedLocation,
                    date = targetDate,
                    monthSystem = prefs.monthSystem,
                    ayanamsha = prefs.ayanamsha,
                    includeExtendedTransits = false
                )
                val horas = panchangaRepository.computeDailyHoras(snap, snap.sunriseMs + 1_000L)
                snap to horas
            }.getOrNull()
        }

        val planNowMs = System.currentTimeMillis()
        val planned = VedicAlertHorizonPlanner.planHorizon(
            snapshotsWithHoras = daySnapshots,
            rules = rules,
            audioSettings = audioSettings,
            nowMs = planNowMs,
            horizonHours = 36
        )

        planned.forEach { scheduleAlarm(it) }
        notificationStudioRepository.replaceScheduledAlerts(planned)
        runCatching { com.mahavtaar.panchanga.widget.WidgetUpdateHelper.updateAllWidgets(context) }
        planned.size
    }

    override suspend fun syncRunningNowNotification() {
        liveRunningNotificationManager.sync(allowChimeOnTattvaChange = false)
    }

    override suspend fun fireTestAlert(rule: NotificationRule, language: AppLanguage) {
        val nowMs = System.currentTimeMillis()
        val prefs = preferencesRepository.preferences.first()
        val sample = VedicAlertHorizonPlanner.buildSampleTestItem(
            rule = rule,
            nowMs = nowMs,
            timezone = prefs.selectedLocation.timezone
        )
        val audioSettings = notificationStudioRepository.getAudioSettings()

        val title = if (language == AppLanguage.HINDI) sample.titleHi else sample.titleEn
        val body = if (language == AppLanguage.HINDI) sample.bodyHi else sample.bodyEn
        val spokenText = if (language == AppLanguage.HINDI) sample.speechHi else sample.speechEn
        val fallbackSpokenText = if (language == AppLanguage.HINDI) sample.speechEn else sample.speechHi

        postLocalizedNotification(
            notificationId = sample.requestCode,
            title = title,
            body = body
        )

        val recordingPath = if (sample.voiceSource == VoiceSourceType.CUSTOM_RECORDING) {
            sample.voiceRecordingId?.takeIf { it.isNotBlank() }?.let { id ->
                notificationStudioRepository.getVoiceRecording(id)?.filePath
            }
        } else {
            null
        }

        audioController.playFullAlertSequence(
            deliveryMode = sample.deliveryMode,
            tuneSource = sample.tuneSource,
            tuneUriOrId = sample.tuneUriOrId,
            voiceSource = sample.voiceSource,
            customRecordingPath = recordingPath,
            spokenText = spokenText,
            language = language,
            audioSettings = audioSettings,
            ignoreSilentCheck = true,
            fallbackText = fallbackSpokenText,
            isInAppPreview = true
        )
    }

    override fun playTunePreview(
        tuneSource: TuneSourceType,
        tuneUriOrId: String,
        onCompleted: () -> Unit
    ) {
        audioController.playTunePreview(tuneSource, tuneUriOrId, onCompleted)
    }

    override fun stopAudioPreview() {
        audioController.stopAllPlayback()
    }

    override fun speakSampleVoice(
        text: String,
        language: AppLanguage,
        speechRate: Float,
        pitch: Float,
        onCompleted: () -> Unit
    ) {
        schedulerScope.launch {
            runCatching {
                audioController.speakBlocking(
                    text = text,
                    language = language,
                    speechRate = speechRate,
                    pitch = pitch,
                    isInAppPreview = true
                )
            }
            onCompleted()
        }
    }

    override fun startVoiceRecording(displayName: String): Result<String> =
        audioController.startVoiceRecording(displayName)

    override fun stopVoiceRecording(): Result<VoiceRecording> =
        audioController.stopVoiceRecording()

    override fun cancelVoiceRecording() {
        audioController.cancelVoiceRecording()
    }

    override fun playVoiceRecording(filePath: String, onCompleted: () -> Unit) {
        audioController.playVoiceRecording(filePath, onCompleted)
    }

    override fun deleteVoiceRecordingFile(filePath: String) {
        audioController.deleteVoiceFile(filePath)
    }

    fun buildAlertNotification(notificationId: Int, title: String, body: String): android.app.Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.mahavtaar.panchanga.ui.R.drawable.ic_notification_panchanga)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSilent(true) // Audio & bilingual TTS voice is orchestrated sequentially by VedicVoiceAudioController
            .setContentIntent(contentIntent)
            .build()
    }

    fun postLocalizedNotification(notificationId: Int, title: String, body: String) {
        if (!hasNotificationPermission()) return
        val notification = buildAlertNotification(notificationId, title, body)
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun scheduleAlarm(item: ScheduledAlertItem) {
        val am = alarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(item.id, item.requestCode)
        runCatching {
            if (canScheduleExactAlarms()) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    item.requestCode,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                // setAlarmClock guarantees zero-delay wakeup even during deep Doze mode
                runCatching {
                    am.setAlarmClock(
                        AlarmManager.AlarmClockInfo(item.triggerTimeMs, showIntent),
                        pendingIntent
                    )
                }.onFailure {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        item.triggerTimeMs,
                        pendingIntent
                    )
                }
            } else {
                am.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    item.triggerTimeMs,
                    pendingIntent
                )
            }
        }
    }

    private fun cancelAlarm(item: ScheduledAlertItem) {
        val am = alarmManager ?: return
        val intent = Intent(context, VedicAlertAlarmReceiver::class.java).apply {
            action = "com.mahavtaar.panchanga.ACTION_VEDIC_ALERT"
            data = Uri.parse("mahavtaar://vedic-alert/${item.id}")
            putExtra(EXTRA_ALERT_ID, item.id)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            item.requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            runCatching { am.cancel(pi) }
            runCatching { pi.cancel() }
        }
    }

    private fun buildAlarmPendingIntent(alertId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, VedicAlertAlarmReceiver::class.java).apply {
            action = "com.mahavtaar.panchanga.ACTION_VEDIC_ALERT"
            data = Uri.parse("mahavtaar://vedic-alert/$alertId")
            putExtra(EXTRA_ALERT_ID, alertId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
