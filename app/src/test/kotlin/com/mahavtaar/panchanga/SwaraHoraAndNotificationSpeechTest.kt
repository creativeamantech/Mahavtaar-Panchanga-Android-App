package com.mahavtaar.panchanga

import android.content.ComponentName
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import com.mahavtaar.panchanga.astronomy.DrikPanchangaEngine
import com.mahavtaar.panchanga.astronomy.HoraEngine
import com.mahavtaar.panchanga.data.notifications.VedicAlertHorizonPlanner
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AlertDeliveryMode
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import com.mahavtaar.panchanga.notifications.VedicAnnouncementService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwaraHoraAndNotificationSpeechTest {

    private val bhopal = CityLocation("Bhopal, IN", "IN", 23.2599, 77.4126, "Asia/Kolkata")
    private val dateTimeSecondsRegex = Regex("""\d{1,2}\s+[^,]+,\s+\d{2}:\d{2}:\d{2}\s+(AM|PM)""")

    @Test
    fun alertHorizonPlannerSchedulesSingleSwaraAlertsAt1st7th12thHorasAndMoonsetWindow() {
        val engine = DrikPanchangaEngine()
        val snap = engine.computePanchanga(
            location = bhopal,
            date = LocalDate.of(2026, 6, 21),
            monthSystem = MonthSystem.AMANTA,
            coordinateSelection = CoordinateSelection.CITRA,
            includeExtendedTransits = false
        )
        val dailyHoras = HoraEngine.resolveForSnapshot(snap, snap.sunriseMs + 1_000L)
        val dayHoras = dailyHoras.horas.filter { it.isDay }

        val rules = listOf(
            VedicAlertEventType.SWARA_SUNRISE_NADI.defaultRule().copy(isEnabled = true, leadTimeMinutes = 0),
            VedicAlertEventType.SWARA_MIDDAY_NADI.defaultRule().copy(isEnabled = true, leadTimeMinutes = 0),
            VedicAlertEventType.SWARA_SUNSET_NADI.defaultRule().copy(isEnabled = true, leadTimeMinutes = 0),
            VedicAlertEventType.SWARA_MOONSET_NADI.defaultRule().copy(isEnabled = true, leadTimeMinutes = 0)
        )
        val settings = AlertAudioSettings(masterEnabled = true, quietHoursEnabled = false)

        val planned = VedicAlertHorizonPlanner.planHorizon(
            snapshotsWithHoras = listOf(snap to dailyHoras),
            rules = rules,
            audioSettings = settings,
            nowMs = snap.sunriseMs - 60_000L,
            horizonHours = 24
        )

        val sunriseAlerts = planned.filter { it.eventType == VedicAlertEventType.SWARA_SUNRISE_NADI }
        val middayAlerts = planned.filter { it.eventType == VedicAlertEventType.SWARA_MIDDAY_NADI }
        val sunsetAlerts = planned.filter { it.eventType == VedicAlertEventType.SWARA_SUNSET_NADI }
        val moonsetAlerts = planned.filter { it.eventType == VedicAlertEventType.SWARA_MOONSET_NADI }

        assertEquals(1, sunriseAlerts.size)
        assertEquals(1, middayAlerts.size)
        assertEquals(1, sunsetAlerts.size)
        assertTrue(moonsetAlerts.size <= 1)

        val sunriseAlert = sunriseAlerts.first()
        val middayAlert = middayAlerts.first()
        val sunsetAlert = sunsetAlerts.first()

        assertEquals(dayHoras[0].startTimeMs, sunriseAlert.triggerTimeMs)
        assertEquals(dayHoras[0].endTimeMs, sunriseAlert.eventEndTimeMs)
        assertEquals(dayHoras[6].startTimeMs, middayAlert.triggerTimeMs)
        assertEquals(dayHoras[6].endTimeMs, middayAlert.eventEndTimeMs)
        assertEquals(dayHoras[11].startTimeMs, sunsetAlert.triggerTimeMs)
        assertEquals(dayHoras[11].endTimeMs, sunsetAlert.eventEndTimeMs)
    }

    @Test
    fun notificationsHaveSeparateStartEndWithSecondsConsistentNadiAndMinimalVoice() {
        val engine = DrikPanchangaEngine()
        val snap = engine.computePanchanga(
            location = bhopal,
            date = LocalDate.of(2026, 6, 21),
            monthSystem = MonthSystem.AMANTA,
            coordinateSelection = CoordinateSelection.CITRA,
            includeExtendedTransits = false
        )
        val dailyHoras = HoraEngine.resolveForSnapshot(snap, snap.sunriseMs + 1_000L)
        val rules = VedicAlertEventType.entries.map {
            it.defaultRule().copy(isEnabled = true, notifyAtStart = true, leadTimeMinutes = 5)
        }
        val settings = AlertAudioSettings(masterEnabled = true, quietHoursEnabled = false)

        val planned = VedicAlertHorizonPlanner.planHorizon(
            snapshotsWithHoras = listOf(snap to dailyHoras),
            rules = rules,
            audioSettings = settings,
            nowMs = snap.sunriseMs - 600_000L,
            horizonHours = 24
        )
        assertTrue(planned.isNotEmpty())

        planned.forEach { item ->
            val enLines = item.bodyEn.lines()
            val hiLines = item.bodyHi.lines()

            // Separate Start/End or Time line must include Date + Seconds
            assertTrue(
                "Expected Start: or Time: with seconds in ${item.eventType}: ${item.bodyEn}",
                enLines.any { (it.startsWith("Start: ") || it.startsWith("Time: ")) && dateTimeSecondsRegex.containsMatchIn(it) }
            )
            assertTrue(
                "Expected आरम्भ: or समय: with seconds in ${item.eventType}: ${item.bodyHi}",
                hiLines.any { (it.startsWith("आरम्भ: ") || it.startsWith("समय: ")) && dateTimeSecondsRegex.containsMatchIn(it) }
            )

            if (item.eventEndTimeMs != null) {
                assertTrue(
                    "Expected separate End: line with seconds in ${item.eventType}: ${item.bodyEn}",
                    enLines.any { it.startsWith("End: ") && dateTimeSecondsRegex.containsMatchIn(it) }
                )
                assertTrue(
                    "Expected separate समाप्ति: line with seconds in ${item.eventType}: ${item.bodyHi}",
                    hiLines.any { it.startsWith("समाप्ति: ") && dateTimeSecondsRegex.containsMatchIn(it) }
                )
            }

            // Advance warnings must never reuse live "Started" wording
            if (item.isAdvanceWarning) {
                assertFalse("Advance body must not say Started: ${item.bodyEn}", item.bodyEn.contains("Started", ignoreCase = true))
                assertTrue("Advance body should state minutes left: ${item.bodyEn}", item.bodyEn.contains("minutes", ignoreCase = true))
                assertTrue("Advance speech should state minutes left: ${item.speechEn}", item.speechEn.contains("minutes left", ignoreCase = true))
                assertTrue("Advance Hindi speech should state बाकी: ${item.speechHi}", item.speechHi.contains("बाकी"))
            }

            // No confusing dual Surya/Chandra Swara labels anywhere
            val combinedText = "${item.titleEn} ${item.titleHi} ${item.bodyEn} ${item.bodyHi} ${item.speechEn} ${item.speechHi}"
            assertFalse("Confusing Surya Swara found in $combinedText", combinedText.contains("Surya Swara", ignoreCase = true))
            assertFalse("Confusing Chandra Swara found in $combinedText", combinedText.contains("Chandra Swara", ignoreCase = true))
            assertFalse("Confusing सूर्य स्वर found in $combinedText", combinedText.contains("सूर्य स्वर"))
            assertFalse("Confusing चन्द्र स्वर found in $combinedText", combinedText.contains("चन्द्र स्वर"))

            // Spoken voice must not read clock times aloud
            assertFalse("Speech should not contain AM/PM timestamps: ${item.speechEn}", item.speechEn.contains(" AM") || item.speechEn.contains(" PM"))
            assertFalse("Hindi speech should not contain AM/PM timestamps: ${item.speechHi}", item.speechHi.contains(" AM") || item.speechHi.contains(" PM"))
        }

        // Sample test alert verification
        val sample = VedicAlertHorizonPlanner.buildSampleTestItem(
            rule = VedicAlertEventType.HORA_SUB_TATTVA.defaultRule(),
            nowMs = snap.sunriseMs,
            timezone = bhopal.timezone
        )
        assertNotNull(sample.eventEndTimeMs)
        assertTrue(sample.bodyEn.lines().any { it.startsWith("Start: ") && dateTimeSecondsRegex.containsMatchIn(it) })
        assertTrue(sample.bodyEn.lines().any { it.startsWith("End: ") && dateTimeSecondsRegex.containsMatchIn(it) })
        assertTrue(sample.bodyEn.contains("Nadi:") && sample.bodyEn.contains("Tattva:"))
    }

    @Test
    fun manifestDeclaresAnnouncementServiceWithMediaPlaybackTypeAndVoiceDefaults() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val serviceInfo = pm.getServiceInfo(
            ComponentName(context, VedicAnnouncementService::class.java),
            0
        )
        assertEquals(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            serviceInfo.foregroundServiceType
        )

        // All notification event types speak announcements when enabled by default
        VedicAlertEventType.entries.forEach { eventType ->
            assertTrue(
                "${eventType.name} should default to TUNE_AND_VOICE",
                eventType.defaultDeliveryMode == AlertDeliveryMode.TUNE_AND_VOICE
            )
        }
    }
}
