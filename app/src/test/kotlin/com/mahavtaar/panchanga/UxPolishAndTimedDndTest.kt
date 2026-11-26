package com.mahavtaar.panchanga

import android.content.ComponentName
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.mahavtaar.panchanga.astronomy.DrikPanchangaEngine
import com.mahavtaar.panchanga.astronomy.HoraEngine
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.notifications.VedicAlertHorizonPlanner
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.NotificationPreset
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import com.mahavtaar.panchanga.feature.settings.DndAndQuietHoursCard
import com.mahavtaar.panchanga.ui.components.formatMuteStatusLine
import com.mahavtaar.panchanga.ui.localization.EnglishStrings
import com.mahavtaar.panchanga.ui.theme.MahavtaarPanchangaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UxPolishAndTimedDndTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val bhopal = CityLocation("Bhopal, IN", "IN", 23.2599, 77.4126, "Asia/Kolkata")

    @Test
    fun timedDndAndQuietHoursResumeResolutionHandlesAllStates() {
        val noonMs = LocalDateTime.of(2026, 6, 21, 12, 0).atZone(zone).toInstant().toEpochMilli()
        val oneHourLaterMs = noonMs + 3_600_000L

        val timedOnly = AlertAudioSettings(
            masterEnabled = true,
            manualDndEnabled = false,
            manualDndUntilMs = oneHourLaterMs,
            quietHoursEnabled = true,
            quietStartMinuteOfDay = 22 * 60,
            quietEndMinuteOfDay = 5 * 60
        )

        assertTrue(timedOnly.isManualDndActive(noonMs))
        assertEquals(oneHourLaterMs, timedOnly.activeTimedDndUntilMs(noonMs))
        assertEquals(oneHourLaterMs, timedOnly.resolveNextResumeTimeMs(noonMs, zone))

        // After expiry, timed DND is no longer active
        assertFalse(timedOnly.isManualDndActive(oneHourLaterMs + 1_000L))
        assertNull(timedOnly.activeTimedDndUntilMs(oneHourLaterMs + 1_000L))
        assertNull(timedOnly.resolveNextResumeTimeMs(oneHourLaterMs + 1_000L, zone))

        // Timed mute ending inside overnight quiet hours extends resume time to quiet hours end (05:00 next day)
        val lateEveningMs = LocalDateTime.of(2026, 6, 21, 21, 30).atZone(zone).toInstant().toEpochMilli()
        val endsDuringQuietMs = LocalDateTime.of(2026, 6, 21, 22, 30).atZone(zone).toInstant().toEpochMilli()
        val expectedFiveAmNextDay = LocalDateTime.of(2026, 6, 22, 5, 0).atZone(zone).toInstant().toEpochMilli()
        val overlapping = timedOnly.copy(manualDndUntilMs = endsDuringQuietMs)
        assertEquals(expectedFiveAmNextDay, overlapping.resolveNextResumeTimeMs(lateEveningMs, zone))

        // Indefinite manual DND has no automatic resume timestamp
        val permanent = timedOnly.copy(manualDndEnabled = true)
        assertTrue(permanent.isManualDndActive(noonMs))
        assertNull(permanent.activeTimedDndUntilMs(noonMs))
        assertNull(permanent.resolveNextResumeTimeMs(noonMs, zone))
    }

    @Test
    fun alertHorizonPlannerSkipsTimedMuteWindowAndKeepsPostExpiryTriggers() {
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
        val nowMs = snap.sunriseMs
        val muteUntilMs = nowMs + 3L * 3_600_000L // Mute next 3 hours
        val settings = AlertAudioSettings(
            masterEnabled = true,
            manualDndEnabled = false,
            manualDndUntilMs = muteUntilMs,
            quietHoursEnabled = false
        )

        val planned = VedicAlertHorizonPlanner.planHorizon(
            snapshotsWithHoras = listOf(snap to horas),
            rules = rules,
            audioSettings = settings,
            nowMs = nowMs,
            horizonHours = 12
        )

        assertTrue("Expected alerts scheduled after timed mute expiry", planned.isNotEmpty())
        planned.forEach { item ->
            assertTrue(
                "Alert ${item.id} triggered before timed mute expired",
                item.triggerTimeMs >= muteUntilMs
            )
        }
    }

    @Test
    fun notificationPresetsProduceExpectedEnabledEventSets() {
        val essential = NotificationPreset.ESSENTIAL.enabledEventTypes()
        assertTrue(VedicAlertEventType.SUNRISE in essential)
        assertTrue(VedicAlertEventType.ABHIJIT_MUHURTA in essential)
        assertTrue(VedicAlertEventType.RAHU_KALA in essential)
        assertFalse(VedicAlertEventType.HORA_SUB_TATTVA in essential)

        val practice = NotificationPreset.PRACTICE.enabledEventTypes()
        assertTrue(VedicAlertEventType.HORA_SUB_TATTVA in practice)
        assertTrue(VedicAlertEventType.SWARA_SUNRISE_NADI in practice)
        assertTrue(VedicAlertEventType.PLANETARY_HORA in practice)
        assertTrue(VedicAlertEventType.BRAHMA_MUHURTA in practice)

        assertEquals(VedicAlertEventType.entries.toSet(), NotificationPreset.FULL.enabledEventTypes())
        assertTrue(NotificationPreset.SILENT.enabledEventTypes().isEmpty())
    }

    @Test
    fun formatMuteStatusLineCoversAllStatesInEnglishAndHindi() {
        val daytimeMs = LocalDateTime.of(2026, 6, 21, 14, 0).atZone(zone).toInstant().toEpochMilli()
        val nightMs = LocalDateTime.of(2026, 6, 21, 23, 15).atZone(zone).toInstant().toEpochMilli()

        // Normal daytime -> null
        val normal = AlertAudioSettings(masterEnabled = true, quietHoursEnabled = true)
        assertNull(formatMuteStatusLine(normal, bhopal.timezone, false, AppLanguage.ENGLISH, daytimeMs))

        // Master OFF
        val off = normal.copy(masterEnabled = false)
        assertEquals(
            "All alerts are turned off",
            formatMuteStatusLine(off, bhopal.timezone, false, AppLanguage.ENGLISH, daytimeMs)
        )
        assertEquals(
            "सभी अलर्ट बंद हैं",
            formatMuteStatusLine(off, bhopal.timezone, false, AppLanguage.HINDI, daytimeMs)
        )

        // Instant DND
        val dnd = normal.copy(manualDndEnabled = true)
        assertTrue(
            formatMuteStatusLine(dnd, bhopal.timezone, false, AppLanguage.ENGLISH, daytimeMs)
                .orEmpty().contains("DND Active")
        )
        assertTrue(
            formatMuteStatusLine(dnd, bhopal.timezone, false, AppLanguage.HINDI, daytimeMs)
                .orEmpty().contains("DND चालू")
        )

        // Timed Mute (resumes at 15:00)
        val timed = normal.copy(manualDndUntilMs = daytimeMs + 3_600_000L)
        val timedEn = formatMuteStatusLine(timed, bhopal.timezone, true, AppLanguage.ENGLISH, daytimeMs)
        val timedHi = formatMuteStatusLine(timed, bhopal.timezone, true, AppLanguage.HINDI, daytimeMs)
        assertNotNull(timedEn)
        assertTrue(timedEn!!.contains("15:00"))
        assertTrue(timedHi!!.contains("15:00"))

        // Quiet Hours active at 23:15 -> resumes at 05:00
        val quietEn = formatMuteStatusLine(normal, bhopal.timezone, true, AppLanguage.ENGLISH, nightMs)
        val quietHi = formatMuteStatusLine(normal, bhopal.timezone, true, AppLanguage.HINDI, nightMs)
        assertTrue(quietEn.orEmpty().contains("05:00"))
        assertTrue(quietHi.orEmpty().contains("05:00"))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TimedDndCardUiTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun timedMuteChipsUpdateAudioSettingsAndDisplayResumeBanner() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        @Suppress("DEPRECATION")
        Shadows.shadowOf(context.packageManager).addActivityIfNotPresent(
            ComponentName(context, ComponentActivity::class.java)
        )
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

        var currentSettings by mutableStateOf(AlertAudioSettings(quietHoursEnabled = false))
        val futureSunriseMs = System.currentTimeMillis() + 6L * 3_600_000L

        activity.setContent {
            MahavtaarPanchangaTheme {
                DndAndQuietHoursCard(
                    audioSettings = currentSettings,
                    strings = EnglishStrings,
                    timezone = TimeFormatters.safeZoneId("Asia/Kolkata").id,
                    use24HourFormat = false,
                    nextSunriseMs = futureSunriseMs,
                    testTagPrefix = "settings_",
                    onUpdateAudioSettings = { currentSettings = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_timed_mute_1h").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_timed_mute_3h").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_timed_mute_sunrise").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("settings_timed_mute_resume").fetchSemanticsNodes().isEmpty())

        // Tap Mute 1 hr
        composeRule.onNodeWithTag("settings_timed_mute_1h").performClick()
        composeRule.waitForIdle()
        assertFalse(currentSettings.manualDndEnabled)
        assertNotNull(currentSettings.manualDndUntilMs)
        composeRule.onNodeWithTag("settings_mute_resume_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_timed_mute_resume").assertIsDisplayed()

        // Tap Until Sunrise
        composeRule.onNodeWithTag("settings_timed_mute_sunrise").performClick()
        composeRule.waitForIdle()
        assertEquals(futureSunriseMs, currentSettings.manualDndUntilMs)

        // Tap Resume Now -> clears timed mute
        composeRule.onNodeWithTag("settings_timed_mute_resume").performClick()
        composeRule.waitForIdle()
        assertFalse(currentSettings.manualDndEnabled)
        assertNull(currentSettings.manualDndUntilMs)
        assertTrue(composeRule.onAllNodesWithTag("settings_timed_mute_resume").fetchSemanticsNodes().isEmpty())
    }
}
