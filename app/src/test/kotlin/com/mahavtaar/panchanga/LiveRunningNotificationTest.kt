package com.mahavtaar.panchanga

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.mahavtaar.panchanga.astronomy.DrikPanchangaEngine
import com.mahavtaar.panchanga.data.audio.VedicVoiceAudioController
import com.mahavtaar.panchanga.data.notifications.RunningNowNotificationBuilder
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.NotificationRule
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.ScheduledAlertItem
import com.mahavtaar.panchanga.domain.model.ScheduledAlertStatus
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import com.mahavtaar.panchanga.domain.model.VoiceRecording
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import com.mahavtaar.panchanga.domain.repository.PanchangaRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.notifications.LiveRunningNotificationManager
import com.mahavtaar.panchanga.notifications.LiveRunningNotificationReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveRunningNotificationTest {

    private val bhopal = CityLocation("Bhopal, IN", "IN", 23.2599, 77.4126, "Asia/Kolkata")
    private val dateTimeSecondsRegex = Regex("""\d{1,2}\s+[^,]+,\s+\d{2}:\d{2}:\d{2}\s+(AM|PM)""")

    @Test
    fun permanentNotificationIsOffByDefaultAndManifestDeclaresReceiver() {
        assertFalse(AlertAudioSettings().persistentRunningNotificationEnabled)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val receiverInfo = context.packageManager.getReceiverInfo(
            ComponentName(context, LiveRunningNotificationReceiver::class.java),
            0
        )
        assertNotNull(receiverInfo)
    }

    @Test
    fun builderFormatsHoraTattvaNadiTithiWithStartEndSecondsInBothLanguages() {
        val engine = DrikPanchangaEngine()
        val snap = engine.computePanchanga(
            location = bhopal,
            date = LocalDate.of(2026, 6, 21),
            monthSystem = MonthSystem.AMANTA,
            coordinateSelection = CoordinateSelection.CITRA,
            includeExtendedTransits = false
        )
        val firstMoment = snap.sunriseMs + 60_000L

        val en = RunningNowNotificationBuilder.build(snap, null, firstMoment, AppLanguage.ENGLISH)
        val hi = RunningNowNotificationBuilder.build(snap, null, firstMoment, AppLanguage.HINDI)

        assertTrue(en.expandedBigText.contains("Hora:"))
        assertTrue(en.expandedBigText.contains("Tattva:"))
        assertTrue(en.expandedBigText.contains("Nadi:"))
        assertTrue(en.expandedBigText.contains("Tithi:"))
        assertTrue(en.expandedBigText.contains("Start: "))
        assertTrue(en.expandedBigText.contains("End: "))
        assertEquals(3, dateTimeSecondsRegex.findAll(en.expandedBigText).filterIndexed { i, _ -> i % 2 == 0 }.count())

        assertTrue(hi.expandedBigText.contains("होरा:"))
        assertTrue(hi.expandedBigText.contains("तत्व:"))
        assertTrue(hi.expandedBigText.contains("नाड़ी:"))
        assertTrue(hi.expandedBigText.contains("तिथि:"))
        assertTrue(hi.expandedBigText.contains("आरम्भ: "))
        assertTrue(hi.expandedBigText.contains("समाप्ति: "))

        val combined = "${en.title} ${en.collapsedText} ${en.expandedBigText} ${hi.title} ${hi.collapsedText} ${hi.expandedBigText}"
        assertFalse(combined.contains("Surya Swara", ignoreCase = true))
        assertFalse(combined.contains("Chandra Swara", ignoreCase = true))
        assertFalse(combined.contains("सूर्य स्वर"))
        assertFalse(combined.contains("चन्द्र स्वर"))

        // Tattva transition key stays stable within same sub-tattva and changes on next sub-tattva
        val sameTattva = RunningNowNotificationBuilder.build(snap, null, firstMoment + 120_000L, AppLanguage.ENGLISH)
        assertEquals(en.tattvaTransitionKey, sameTattva.tattvaTransitionKey)

        val afterTransition = RunningNowNotificationBuilder.build(snap, null, en.nextTransitionAtMs + 5_000L, AppLanguage.ENGLISH)
        assertNotEquals(en.tattvaTransitionKey, afterTransition.tattvaTransitionKey)
    }

    @Test
    fun managerPostsOngoingHighPriorityNotificationOnlyWhenEnabledAndCancelsWhenDisabled() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = DrikPanchangaEngine()
        val snap = engine.computePanchanga(
            location = bhopal,
            date = LocalDate.now(),
            monthSystem = MonthSystem.AMANTA,
            coordinateSelection = CoordinateSelection.CITRA,
            includeExtendedTransits = false
        )
        val fakeRepo = FakeStudioRepo(AlertAudioSettings(persistentRunningNotificationEnabled = false))
        val fakePrefsRepo = FakePrefsRepo(UserPreferences(selectedLocation = bhopal, language = AppLanguage.ENGLISH))
        val fakePanchangaRepo = FakePanchangaRepo(snap)

        val manager = LiveRunningNotificationManager(
            context = context,
            preferencesRepository = fakePrefsRepo,
            panchangaRepository = fakePanchangaRepo,
            notificationStudioRepository = fakeRepo,
            audioController = VedicVoiceAudioController(context)
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(notificationManager)

        // Initially disabled -> no persistent notification posted
        manager.sync(allowChimeOnTattvaChange = false)
        assertEquals(0, shadowNm.allNotifications.count { it.flags and Notification.FLAG_ONGOING_EVENT != 0 })

        // Enabled WITHOUT POST_NOTIFICATIONS permission on SDK 34 -> still suppressed
        fakeRepo.updateAudioSettings(AlertAudioSettings(persistentRunningNotificationEnabled = true))
        manager.sync(allowChimeOnTattvaChange = false)
        assertEquals(null, shadowNm.getNotification(LiveRunningNotificationManager.NOTIFICATION_ID))

        // Grant POST_NOTIFICATIONS -> high-priority ongoing status notification posted with Turn Off action
        Shadows.shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        manager.sync(allowChimeOnTattvaChange = false)

        val channel = notificationManager.getNotificationChannel(LiveRunningNotificationManager.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)

        val activeNotification = shadowNm.getNotification(LiveRunningNotificationManager.NOTIFICATION_ID)
        assertNotNull(activeNotification)
        assertTrue((activeNotification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        assertTrue((activeNotification.flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0)
        assertEquals(0, activeNotification.flags and Notification.FLAG_AUTO_CANCEL)
        assertEquals(NotificationCompat.CATEGORY_STATUS, activeNotification.category)
        assertEquals(NotificationCompat.VISIBILITY_PUBLIC, activeNotification.visibility)
        assertTrue(activeNotification.actions.any { it.title == "Turn Off" })

        // Turning off clears ongoing notification immediately
        manager.turnOffByUserAction()
        assertFalse(fakeRepo.getAudioSettings().persistentRunningNotificationEnabled)
        assertEquals(null, shadowNm.getNotification(LiveRunningNotificationManager.NOTIFICATION_ID))
    }

    private class FakeStudioRepo(initial: AlertAudioSettings) : NotificationStudioRepository {
        private val settingsFlow = MutableStateFlow(initial)
        private var lastKey = ""

        override fun observeRules(): Flow<List<NotificationRule>> = flowOf(emptyList())
        override suspend fun getAllRules(): List<NotificationRule> = emptyList()
        override suspend fun getRule(eventType: VedicAlertEventType): NotificationRule = eventType.defaultRule()
        override suspend fun saveRule(rule: NotificationRule) = Unit
        override suspend fun setEventEnabled(eventType: VedicAlertEventType, enabled: Boolean) = Unit
        override fun observeVoiceRecordings(): Flow<List<VoiceRecording>> = flowOf(emptyList())
        override suspend fun getVoiceRecording(id: String): VoiceRecording? = null
        override suspend fun saveVoiceRecording(recording: VoiceRecording) = Unit
        override suspend fun renameVoiceRecording(id: String, newName: String) = Unit
        override suspend fun deleteVoiceRecording(id: String) = Unit
        override fun observeAudioSettings(): Flow<AlertAudioSettings> = settingsFlow
        override suspend fun getAudioSettings(): AlertAudioSettings = settingsFlow.value
        override suspend fun updateAudioSettings(settings: AlertAudioSettings) { settingsFlow.value = settings }
        override suspend fun getLastRunningTattvaKey(): String = lastKey
        override suspend fun setLastRunningTattvaKey(key: String) { lastKey = key }
        override fun observeScheduledAlerts(): Flow<List<ScheduledAlertItem>> = flowOf(emptyList())
        override fun observeRecentHistory(): Flow<List<ScheduledAlertItem>> = flowOf(emptyList())
        override suspend fun getScheduledAlertById(id: String): ScheduledAlertItem? = null
        override suspend fun replaceScheduledAlerts(alerts: List<ScheduledAlertItem>) = Unit
        override suspend fun markAlertStatus(id: String, status: ScheduledAlertStatus, timestampMs: Long) = Unit
        override suspend fun resetNotificationDefaults() = Unit
    }

    private class FakePrefsRepo(initial: UserPreferences) : UserPreferencesRepository {
        private val flow = MutableStateFlow(initial)
        override val preferences: Flow<UserPreferences> = flow
        override suspend fun setSelectedLocation(location: CityLocation) = Unit
        override suspend fun setMonthSystem(monthSystem: MonthSystem) = Unit
        override suspend fun setAyanamsha(ayanamsha: CoordinateSelection) = Unit
        override suspend fun setThemeMode(themeMode: com.mahavtaar.panchanga.domain.model.AppThemeMode) = Unit
        override suspend fun setLanguage(language: AppLanguage) = Unit
        override suspend fun setBirthNakshatra(nakshatra: Int) = Unit
        override suspend fun setUse24HourFormat(use24Hour: Boolean) = Unit
        override suspend fun updateNotificationSettings(
            notifySunrise: Boolean?,
            notifySunset: Boolean?,
            notifyTithiChange: Boolean?,
            notifyNakshatraChange: Boolean?,
            notifyVratReminders: Boolean?
        ) = Unit
        override suspend fun resetDefaults() = Unit
    }

    private class FakePanchangaRepo(private val snap: PanchangaSnapshot) : PanchangaRepository {
        override suspend fun getDailyPanchanga(
            location: CityLocation,
            date: LocalDate,
            monthSystem: MonthSystem,
            ayanamsha: CoordinateSelection,
            includeExtendedTransits: Boolean
        ): PanchangaSnapshot = snap

        override suspend fun getMonthlyCalendar(
            location: CityLocation,
            year: Int,
            month: Int,
            monthSystem: MonthSystem,
            ayanamsha: CoordinateSelection
        ) = emptyList<com.mahavtaar.panchanga.domain.model.MonthlyCalendarDay>()

        override fun computeDailyHoras(snapshot: PanchangaSnapshot, nowMs: Long) =
            com.mahavtaar.panchanga.astronomy.HoraEngine.resolveForSnapshot(snapshot, nowMs)

        override fun computeLiveSwaraYoga(snapshot: PanchangaSnapshot, nowMs: Long) =
            com.mahavtaar.panchanga.astronomy.SwaraYogaEngine.computeSwaraYoga(
                tithiNumber = snapshot.tithi.firstOrNull()?.number ?: 1,
                sunriseStr = snapshot.sunrise,
                sunsetStr = snapshot.sunset,
                currentLocalMinutes = 360.0,
                sunriseMs = snapshot.sunriseMs,
                sunsetMs = snapshot.sunsetMs,
                timeZoneId = snapshot.timezone,
                nowMs = nowMs
            )

        override suspend fun clearExpiredCache() = Unit
    }
}
