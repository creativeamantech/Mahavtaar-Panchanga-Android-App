package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.KundliBirthInput
import com.mahavtaar.panchanga.domain.model.KundliJsonExporter
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests covering PanchaTattvaEngine (day tithis + 6-month Karana cycle),
 * SwaraYogaEngine (28-star table + alignment simulator), NavtaraEngine (meta,
 * Pada Purushartha, Gandanta, Tara Dasa), and full Kundli (KP, Jaimini, Yogas,
 * Gochar, and JSON export).
 */
class AdvancedAstrologyEnginesTest {

    private val drikEngine = DrikPanchangaEngine()
    private val bhopal = CityLocation("Bhopal, IN", "IN", 23.2599, 77.4126, "Asia/Kolkata")

    @Test
    fun panchaTattvaEngineProducesDayTithisAndSixMonthKaranaCycle() {
        val snap = drikEngine.computePanchanga(
            location = bhopal,
            date = LocalDate.of(2026, 5, 10),
            monthSystem = MonthSystem.AMANTA,
            coordinateSelection = CoordinateSelection.CITRA,
            includeExtendedTransits = false
        )
        val middayMs = (snap.sunriseMs + snap.sunsetMs) / 2L
        val dailyHoras = HoraEngine.resolveForSnapshot(snap, middayMs)
        val report = PanchaTattvaEngine.compute(snap, dailyHoras, middayMs)

        assertTrue(report.dayTithiTattvas.isNotEmpty())
        assertTrue(report.guidanceEn.isNotBlank() && report.guidanceHi.isNotBlank())

        val cycle = report.karanaCycle
        assertNotNull("Expected 6-month Karana cycle to resolve", cycle)
        cycle!!
        assertEquals(5, cycle.periods.size)
        assertEquals(15, cycle.periods.sumOf { it.weight })
        assertEquals(100.0, cycle.periods.sumOf { it.percentage }, 0.05)
        assertNotNull(cycle.activePeriod)
        assertTrue(cycle.periods.all { it.startTimeMs < it.endTimeMs })
        assertTrue(cycle.periods.any { it.maasOverlaps.isNotEmpty() })
    }

    @Test
    fun swaraYogaEngineEvaluates28StarsAndNakshatraAlignment() {
        val rules28 = SwaraYogaEngine.allNakshatraSwaraRules
        assertEquals(28, rules28.size)
        assertEquals(28, rules28.last().nakshatraNumber)
        assertEquals("Abhijit", rules28.last().nakshatraName)

        // Shukla 1 requires Ida; Nakshatra 4 (Rohini in even sign Taurus) supports Ida -> ALIGNED
        val aligned = SwaraYogaEngine.evaluateNakshatraSwaraAlignment("Shukla", 1, 4, 1)
        assertEquals(SwaraNadi.IDA, aligned.requiredTithiSwara)
        assertEquals(SwaraNadi.IDA, aligned.supportedNakshatraSwara)
        assertEquals("ALIGNED", aligned.rating)
        assertTrue(aligned.padaAdviceEn.contains("Dharma", ignoreCase = true))

        // Shukla 1 requires Ida; Nakshatra 1 (Ashwini Pada 2 in odd sign Aries) supports Pingala -> MISMATCH
        val mismatch = SwaraYogaEngine.evaluateNakshatraSwaraAlignment("Shukla", 1, 1, 2)
        assertEquals(SwaraNadi.IDA, mismatch.requiredTithiSwara)
        assertEquals(SwaraNadi.PINGALA, mismatch.supportedNakshatraSwara)
        assertEquals("MISMATCH", mismatch.rating)
        assertTrue(mismatch.padaAdviceEn.contains("Artha", ignoreCase = true))
        assertTrue(mismatch.cosmicAdviceHi.isNotBlank())
    }

    @Test
    fun navtaraEngineComputesMatrixGandantaPadaAndTaraDasa() {
        val t1 = NavtaraEngine.calculate(1, 1)
        assertEquals(1, t1.distance)
        assertEquals(1, t1.taraNumber)
        assertEquals("Janma", t1.taraNameEn)

        val t2 = NavtaraEngine.calculate(1, 2)
        assertEquals(2, t2.taraNumber)
        assertTrue(t2.isAuspicious)

        val t7Paryaya2 = NavtaraEngine.calculate(1, 16)
        assertEquals(16, t7Paryaya2.distance)
        assertEquals(2, t7Paryaya2.paryaya)
        assertEquals(7, t7Paryaya2.taraNumber)
        assertNotNull(t7Paryaya2.parihara)

        assertEquals(27, NavtaraEngine.generateMatrix(5).size)

        // Gandanta junction checks
        assertTrue(NavtaraEngine.checkGandanta(9, 4).isGandanta)
        assertTrue(NavtaraEngine.checkGandanta(1, 1).isGandanta)
        assertTrue(NavtaraEngine.checkGandanta(27, 4).isGandanta)
        val nonGandanta = NavtaraEngine.checkGandanta(4, 2)
        assertFalse(nonGandanta.isGandanta)
        assertFalse(nonGandanta.isMoolaNakshatra)

        // Pada Purushartha
        assertTrue(NavtaraEngine.getPadaPurushartha(1).purusharthaEn.contains("Dharma"))
        assertTrue(NavtaraEngine.getPadaPurushartha(2).purusharthaEn.contains("Artha"))
        assertTrue(NavtaraEngine.getPadaPurushartha(3).purusharthaEn.contains("Kama"))
        assertTrue(NavtaraEngine.getPadaPurushartha(4).purusharthaEn.contains("Moksha"))

        // Tara Dasa sums to 120 Vimshottari years and starts on specified lord
        val dasa = NavtaraEngine.computeTaraDasa("Jupiter")
        assertEquals(9, dasa.size)
        assertEquals("Jupiter", dasa.first().planetEn)
        assertEquals(120, dasa.sumOf { it.years })

        // Nakshatra Meta
        val pushyaMeta = NavtaraEngine.getNakshatraMeta(8)
        assertEquals("Saturn", pushyaMeta.lordEn)
        assertEquals("Brihaspati", pushyaMeta.deityEn)
    }

    @Test
    fun kundliEngineComputesCompleteKpJaiminiYogasGocharAndJsonExport() {
        val input = KundliBirthInput(
            name = "Audit Native",
            year = 1995,
            month = 8,
            day = 15,
            hour = 9,
            minute = 30,
            location = bhopal,
            ayanamsha = CoordinateSelection.CITRA
        )
        val report = KundliAstrologyEngine.compute(input, nowMs = 1_780_000_000_000L)

        assertEquals(9, report.planets.size)
        assertEquals(12, report.housesD1.size)
        assertEquals(12, report.housesD9.size)
        assertEquals(9, report.kpTable.size)
        assertEquals(12, report.kpCusps.size)
        assertEquals(6, report.kpRulingPlanets.size)

        // Jaimini Sutras parity
        assertEquals(12, report.arudhaPadas.size)
        assertEquals("AL", report.arudhaPadas.first().code)
        assertEquals("UL", report.arudhaPadas.last().code)
        assertEquals(12, report.jaiminiRashiDrishti.size)
        assertEquals(12, report.charaDashas.size)
        assertTrue(report.charaDashas.all { it.subPeriods.size == 12 && it.durationYears in 1..12 })
        assertNotNull(report.karakamsha)

        // Canonical Yogas & Gochar
        assertTrue(report.yogas.size >= 14)
        assertTrue(report.yogas.all { it.matchedConditionsEn.isNotEmpty() || it.failedConditionsEn.isNotEmpty() })
        assertEquals(9, report.gocharEffects.size)

        // Check custom Gochar date calculation
        val customGochar = KundliRulesAndGocharEngine.computeGocharForDate(
            natalMoonSignIndex = report.moonSignIndex,
            natalLagnaSignIndex = report.lagnaSignIndex,
            transitDate = LocalDate.of(2026, 6, 15),
            ayanamsha = CoordinateSelection.CITRA
        )
        assertEquals("2026-06-15", customGochar.transitDateIso)
        assertEquals(9, customGochar.transits.size)

        // Audit & JSON export
        val chartHash = report.audit?.chartHash.orEmpty()
        assertTrue(chartHash.isNotBlank())
        val jsonText = KundliJsonExporter.toJson(report)
        assertTrue(jsonText.contains(chartHash))
        assertTrue(jsonText.contains("\"kpCusps\""))
        assertTrue(jsonText.contains("\"arudhaPadas\""))
    }
}
