package com.mahavtaar.panchanga

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.mahavtaar.panchanga.astronomy.DrikPanchangaEngine
import com.mahavtaar.panchanga.astronomy.HoraEngine
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.audio.VedicVoiceAudioController
import com.mahavtaar.panchanga.data.notifications.VedicAlertHorizonPlanner
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import com.mahavtaar.panchanga.feature.settings.DndAndQuietHoursCard
import com.mahavtaar.panchanga.notifications.LiveRunningNotificationManager
import com.mahavtaar.panchanga.notifications.PanchangaNotificationScheduler
import com.mahavtaar.panchanga.ui.localization.EnglishStrings
import com.mahavtaar.panchanga.ui.theme.MahavtaarPanchangaTheme
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DndAndQuietHoursTest {

    private val bhopal = CityLocation("Bhopal, IN", "IN", 23.2599, 77.4126, "Asia/Kolkata")

    @Test
    fun quietHoursWindowHandlesOvernightDaytimeEqualBoundsAndDisabledState() {
        val overnight = AlertAudioSettings(
            quietHoursEnabled = true,
            quietStartMinuteOfDay = 22 * 60, // 10:00 PM
            quietEndMinuteOfDay = 5 * 60     // 5:00 AM
        )
        assertTrue(overnight.isWithinQuietHours(22 * 60))
        assertTrue(overnight.isWithinQuietHours(23 * 60 + 45))
        assertTrue(overnight.isWithinQuietHours(0))
        assertTrue(overnight.isWithinQuietHours(4 * 60 + 59))
        assertFalse(overnight.isWithinQuietHours(5 * 60))
        assertFalse(overnight.isWithinQuietHours(12 * 60))
        assertFalse(overnight.isWithinQuietHours(21 * 60 + 59))

        val daytime = AlertAudioSettings(
            quietHoursEnabled = true,
            quietStartMinuteOfDay = 13 * 60,
            quietEndMinuteOfDay = 16 * 60
        )
        assertTrue(daytime.isWithinQuietHours(13 * 60))
        assertTrue(daytime.isWithinQuietHours(14 * 60 + 30))
        assertFalse(daytime.isWithinQuietHours(16 * 60))
        assertFalse(daytime.isWithinQuietHours(12 * 60 + 59))

        val zeroDuration = AlertAudioSettings(
            quietHoursEnabled = true,
            quietStartMinuteOfDay = 22 * 60,
            quietEndMinuteOfDay = 22 * 60
        )
        assertFalse(zeroDuration.isWithinQuietHours(22 * 60))

        val disabled = overnight.copy(quietHoursEnabled = false)
        assertFalse(disabled.isWithinQuietHours(23 * 60))
        assertFalse(disabled.isWithinQuietHours(2 * 60))
    }

    @Test
    fun alertHorizonPlannerExcludesTriggersDuringQuietHoursWhenEnabled() {
        val engine = DrikPanchangaEngine()
        val snap = engine.computePanchanga(
            location = bhopal,
            date = LocalDate.of(2026, 6, 21),
            monthSystem = MonthSystem.AMANTA,
            coordinateSelection = CoordinateSelection.CITRA,
            includeExtendedTransits = false
        )
        val horas = HoraEngine.resolveForSnapshot(snap, snap.sunriseMs + 1_000L)
        val rules = listOf(
            VedicAlertEventType.HORA_SUB_TATTVA.defaultRule().copy(
                isEnabled = true,
                notifyAtStart = true,
                leadTimeMinutes = 0
            )
        )
        val quietEnabled = AlertAudioSettings(
            masterEnabled = true,
            quietHoursEnabled = true,
            quietStartMinuteOfDay = 22 * 60,
            quietEndMinuteOfDay = 5 * 60
        )
        val quietDisabled = quietEnabled.copy(quietHoursEnabled = false)

        val zone = TimeFormatters.safeZoneId(bhopal.timezone)
        val withQuiet = VedicAlertHorizonPlanner.planHorizon(
            snapshotsWithHoras = listOf(snap to horas),
            rules = rules,
            audioSettings = quietEnabled,
            nowMs = snap.sunriseMs - 60_000L,
            horizonHours = 24
        )
        val withoutQuiet = VedicAlertHorizonPlanner.planHorizon(
            snapshotsWithHoras = listOf(snap to horas),
            rules = rules,
            audioSettings = quietDisabled,
            nowMs = snap.sunriseMs - 60_000L,
            horizonHours = 24
        )

        assertTrue(withQuiet.isNotEmpty())
        assertTrue(withoutQuiet.size > withQuiet.size)
        withQuiet.forEach { item ->
            val zdt = Instant.ofEpochMilli(item.triggerTimeMs).atZone(zone)
            val minOfDay = zdt.hour * 60 + zdt.minute
            assertFalse(
                "Alert ${item.id} triggered during quiet hours at ${zdt.toLocalTime()}",
                quietEnabled.isWithinQuietHours(minOfDay)
            )
        }
    }

    @Test
    fun audioControllerRespectsManualDndPhoneSilentAndOsDndFilters() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(notificationManager)
        shadowNm.setNotificationPolicyAccessGranted(true)

        val controller = VedicVoiceAudioController(context)

        // Reset normal baseline
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

        assertFalse(controller.shouldSuppressForDeviceAudioState(AlertAudioSettings(manualDndEnabled = false, respectSilentAndDnd = true)))

        // Manual instant App DND overrides everything even when respectSilentAndDnd = false
        assertTrue(controller.shouldSuppressForDeviceAudioState(AlertAudioSettings(manualDndEnabled = true, respectSilentAndDnd = false)))

        // Phone Silent & Vibrate suppress when respectSilentAndDnd = true
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        assertTrue(controller.shouldSuppressForDeviceAudioState(AlertAudioSettings(manualDndEnabled = false, respectSilentAndDnd = true)))
        assertFalse(controller.shouldSuppressForDeviceAudioState(AlertAudioSettings(manualDndEnabled = false, respectSilentAndDnd = false)))

        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        assertTrue(controller.shouldSuppressForDeviceAudioState(AlertAudioSettings(manualDndEnabled = false, respectSilentAndDnd = true)))

        // Phone OS DND filters (Priority / Alarms / None)
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        listOf(
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            NotificationManager.INTERRUPTION_FILTER_NONE
        ).forEach { filter ->
            notificationManager.setInterruptionFilter(filter)
            assertTrue(controller.isDeviceInDndInterruptionMode())
            assertTrue(controller.shouldSuppressForDeviceAudioState(AlertAudioSettings(respectSilentAndDnd = true)))
            assertFalse(controller.shouldSuppressForDeviceAudioState(AlertAudioSettings(respectSilentAndDnd = false)))
        }

        // Active phone call always suppresses
        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        audioManager.mode = AudioManager.MODE_IN_CALL
        assertTrue(controller.shouldSuppressForDeviceAudioState(AlertAudioSettings(respectSilentAndDnd = false)))
        audioManager.mode = AudioManager.MODE_NORMAL
    }

    @Test
    fun alertNotificationUsesReminderCategorySoAndroidDndIsNeverBypassed() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scheduler = PanchangaNotificationScheduler(
            context = context,
            preferencesRepository = mockk(relaxed = true),
            panchangaRepository = mockk(relaxed = true),
            notificationStudioRepository = mockk(relaxed = true),
            audioController = VedicVoiceAudioController(context),
            liveRunningNotificationManager = mockk<LiveRunningNotificationManager>(relaxed = true)
        )
        val notification = scheduler.buildAlertNotification(
            notificationId = 101,
            title = "Agni Tattva",
            body = "Start: 21 Jun, 06:00:00 AM"
        )
        assertEquals(NotificationCompat.CATEGORY_REMINDER, notification.category)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DndAndQuietHoursUiTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun dndAndQuietHoursCardSupportsInstantDndToggleAndQuietScheduleControls() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        Shadows.shadowOf(context.packageManager).addActivityIfNotPresent(
            ComponentName(context, ComponentActivity::class.java)
        )
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

        var currentSettings by mutableStateOf(AlertAudioSettings())

        activity.setContent {
            MahavtaarPanchangaTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DndAndQuietHoursCard(
                        audioSettings = currentSettings,
                        strings = EnglishStrings,
                        use24HourFormat = false,
                        testTagPrefix = "settings_",
                        onUpdateAudioSettings = { currentSettings = it }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_instant_dnd_switch").performScrollTo().assertIsDisplayed().assertIsOff()
        composeRule.onNodeWithTag("settings_quiet_hours_switch").performScrollTo().assertIsDisplayed().assertIsOn()
        composeRule.onNodeWithTag("settings_respect_silent_dnd_switch").performScrollTo().assertIsDisplayed().assertIsOn()
        composeRule.onNodeWithTag("settings_quiet_start_minus").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_quiet_end_plus").performScrollTo().assertIsDisplayed()

        // Toggle instant DND Mode ON
        composeRule.onNodeWithTag("settings_instant_dnd_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(currentSettings.manualDndEnabled)
        composeRule.onNodeWithTag("settings_instant_dnd_switch").assertIsOn()
        composeRule.onNodeWithText(EnglishStrings.notifDndStatusActive).performScrollTo().assertIsDisplayed()

        // Toggle instant DND Mode OFF -> returns to Quiet Schedule status
        composeRule.onNodeWithTag("settings_instant_dnd_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(currentSettings.manualDndEnabled)
        composeRule.onNodeWithText(EnglishStrings.notifQuietHoursStatusActive).performScrollTo().assertIsDisplayed()

        // Adjust Quiet Start time by -30m (22:00 -> 21:30)
        composeRule.onNodeWithTag("settings_quiet_start_minus").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(21 * 60 + 30, currentSettings.quietStartMinuteOfDay)

        // Turn OFF Quiet Hours -> steppers collapse and badge becomes Normal
        composeRule.onNodeWithTag("settings_quiet_hours_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(currentSettings.quietHoursEnabled)
        assertTrue(composeRule.onAllNodesWithTag("settings_quiet_start_minus").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText(EnglishStrings.notifAlertsNormalStatus).performScrollTo().assertIsDisplayed()

        // Turn OFF Respect Phone Silent & DND
        composeRule.onNodeWithTag("settings_respect_silent_dnd_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(currentSettings.respectSilentAndDnd)
    }
}
