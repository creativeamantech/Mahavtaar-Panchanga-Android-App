package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit & edge-case tests for HoraEngine, SwaraYogaEngine, DST transitions, leap years,
 * historical/future dates, and equatorial/high-latitude cities.
 */
class HoraAndSwaraParityTest {

    private val engine = DrikPanchangaEngine()

    private val bhopal = CityLocation("Bhopal, IN", "IN", 23.2599, 77.4126, "Asia/Kolkata")
    private val newYork = CityLocation("New York, US", "US", 40.7128, -74.0060, "America/New_York")
    private val singapore = CityLocation("Singapore, SG", "SG", 1.3521, 103.8198, "Asia/Singapore")
    private val reykjavik = CityLocation("Reykjavik, IS", "IS", 64.1466, -21.9426, "Atlantic/Reykjavik")

    @Test
    fun horaEngineProduces24ContinuousHorasAnd5SubTattvasPerHora() {
        val snap = engine.computePanchanga(bhopal, LocalDate.of(2026, 3, 20), MonthSystem.AMANTA, CoordinateSelection.CITRA, false)
        val middayMs = (snap.sunriseMs + snap.sunsetMs) / 2L
        val daily = HoraEngine.resolveForSnapshot(snap, middayMs)

        assertEquals(24, daily.horas.size)
        assertNotNull(daily.activeHora)
        assertNotNull(daily.activeTattva)

        // Friday (weekday 5) first Hora ruler is Venus
        assertEquals("Venus", daily.horas[0].ruler)
        assertEquals(snap.sunriseMs, daily.horas.first().startTimeMs)
        assertEquals(snap.sunsetMs, daily.horas[11].endTimeMs)
        assertEquals(snap.nextSunriseMs, daily.horas.last().endTimeMs)

        // Every Hora has 5 continuous sub-Tattvas summing cleanly to Hora bounds
        for (i in daily.horas.indices) {
            val h = daily.horas[i]
            assertEquals(5, h.tattvas.size)
            assertEquals(h.startTimeMs, h.tattvas.first().startTimeMs)
            assertEquals(h.endTimeMs, h.tattvas.last().endTimeMs)
            if (i > 0) {
                assertEquals(daily.horas[i - 1].endTimeMs, h.startTimeMs)
            }
        }
    }

    @Test
    fun horaEngineHandlesPreSunriseRolloverToPreviousDayNightHoras() {
        val snap = engine.computePanchanga(bhopal, LocalDate.of(2026, 3, 20), MonthSystem.AMANTA, CoordinateSelection.CITRA, false)
        val preSunriseMs = snap.sunriseMs - 30L * 60L * 1000L // 30 mins before Friday sunrise
        val daily = HoraEngine.resolveForSnapshot(snap, preSunriseMs)

        assertEquals(24, daily.horas.size)
        // Thursday (weekday 4) starts with Jupiter
        assertEquals("Jupiter", daily.horas[0].ruler)
        assertNotNull(daily.activeHora)
        assertEquals(24, daily.activeHora!!.index)
    }

    @Test
    fun swaraYogaRulesMatch30TithiClassicalCycle() {
        // Shukla 1-3 -> Ida; Shukla 4-6 -> Pingala; Krishna 16-18 -> Pingala; Krishna 19-21 -> Ida
        assertEquals(SwaraNadi.IDA, SwaraYogaEngine.getRuleForTithi(1).sunriseSwara)
        assertEquals(SwaraNadi.IDA, SwaraYogaEngine.getRuleForTithi(3).sunriseSwara)
        assertEquals(SwaraNadi.PINGALA, SwaraYogaEngine.getRuleForTithi(4).sunriseSwara)
        assertEquals(SwaraNadi.PINGALA, SwaraYogaEngine.getRuleForTithi(16).sunriseSwara)
        assertEquals(SwaraNadi.IDA, SwaraYogaEngine.getRuleForTithi(19).sunriseSwara)
        assertEquals(SwaraNadi.PINGALA, SwaraYogaEngine.getRuleForTithi(1).sunsetSwara)
    }

    @Test
    fun edgeCaseDatesAndLocationsComputeWithoutError() {
        // Leap day
        val leap = engine.computePanchanga(bhopal, LocalDate.of(2024, 2, 29), MonthSystem.AMANTA, CoordinateSelection.CITRA, false)
        assertTrue(leap.tithi.isNotEmpty())

        // US Spring-forward DST date
        val dst = engine.computePanchanga(newYork, LocalDate.of(2026, 3, 8), MonthSystem.AMANTA, CoordinateSelection.CITRA, false)
        assertTrue(dst.sunriseMs < dst.sunsetMs)

        // Equatorial city
        val eq = engine.computePanchanga(singapore, LocalDate.of(2026, 6, 21), MonthSystem.PURNIMANTA, CoordinateSelection.RAMAN, false)
        assertEquals(9, eq.planets.size)

        // High latitude summer solstice (graceful handling)
        val highLat = engine.computePanchanga(reykjavik, LocalDate.of(2026, 6, 21), MonthSystem.AMANTA, CoordinateSelection.CITRA, false)
        assertTrue(highLat.sunriseMs < highLat.sunsetMs)

        // Historical 1950 & Future 2050 dates
        val past = engine.computePanchanga(bhopal, LocalDate.of(1950, 1, 26), MonthSystem.AMANTA, CoordinateSelection.CITRA, false)
        val future = engine.computePanchanga(bhopal, LocalDate.of(2050, 8, 15), MonthSystem.PURNIMANTA, CoordinateSelection.KRISHNAMURTI, false)
        assertTrue(past.tithi.isNotEmpty() && future.tithi.isNotEmpty())
    }

    @Test
    fun swaraWindowsMatchFirstSeventhAndTwelfthDayHoras() {
        val snap = engine.computePanchanga(bhopal, LocalDate.of(2026, 6, 21), MonthSystem.AMANTA, CoordinateSelection.CITRA, false)
        val dailyHoras = HoraEngine.resolveForSnapshot(snap, snap.sunriseMs + 1_000L)
        val dayHoras = dailyHoras.horas.filter { it.isDay }
        assertEquals(12, dayHoras.size)

        val firstHora = dayHoras[0]
        val seventhHora = dayHoras[6]
        val twelfthHora = dayHoras[11]

        // Snapshot windows match 1st, 7th, 12th Day Horas
        assertEquals(firstHora.startTime, snap.swaraYoga.sunriseWindow.start)
        assertEquals(firstHora.endTime, snap.swaraYoga.sunriseWindow.end)
        assertEquals("${firstHora.startTime} – ${firstHora.endTime}", snap.swaraYoga.sunriseWindow.windowFormatted)

        assertEquals(seventhHora.startTime, snap.swaraYoga.middayWindow.start)
        assertEquals(seventhHora.endTime, snap.swaraYoga.middayWindow.end)
        assertEquals("${seventhHora.startTime} – ${seventhHora.endTime}", snap.swaraYoga.middayWindow.windowFormatted)

        assertEquals(twelfthHora.startTime, snap.swaraYoga.sunsetWindow.start)
        assertEquals(twelfthHora.endTime, snap.swaraYoga.sunsetWindow.end)
        assertEquals("${twelfthHora.startTime} – ${twelfthHora.endTime}", snap.swaraYoga.sunsetWindow.windowFormatted)

        // Live evaluation activates each exact window inside its matching Hora
        val tithiNum = snap.tithi.firstOrNull()?.number ?: 1
        val liveDuring1st = SwaraYogaEngine.computeSwaraYoga(
            tithiNumber = tithiNum,
            sunriseStr = snap.sunrise,
            sunsetStr = snap.sunset,
            sunriseMs = snap.sunriseMs,
            sunsetMs = snap.sunsetMs,
            timeZoneId = snap.timezone,
            nowMs = firstHora.startTimeMs + 10_000L
        )
        assertTrue(liveDuring1st.sunriseWindow.isActive)
        assertEquals("sunrise", liveDuring1st.activeCelestialWindow)

        val liveDuring7th = SwaraYogaEngine.computeSwaraYoga(
            tithiNumber = tithiNum,
            sunriseStr = snap.sunrise,
            sunsetStr = snap.sunset,
            sunriseMs = snap.sunriseMs,
            sunsetMs = snap.sunsetMs,
            timeZoneId = snap.timezone,
            nowMs = seventhHora.startTimeMs + 10_000L
        )
        assertTrue(liveDuring7th.middayWindow.isActive)
        assertEquals("midday", liveDuring7th.activeCelestialWindow)

        val liveDuring12th = SwaraYogaEngine.computeSwaraYoga(
            tithiNumber = tithiNum,
            sunriseStr = snap.sunrise,
            sunsetStr = snap.sunset,
            sunriseMs = snap.sunriseMs,
            sunsetMs = snap.sunsetMs,
            timeZoneId = snap.timezone,
            nowMs = twelfthHora.startTimeMs + 10_000L
        )
        assertTrue(liveDuring12th.sunsetWindow.isActive)
        assertEquals("sunset", liveDuring12th.activeCelestialWindow)
    }
}
