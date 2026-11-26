package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.JaiminiArudhaEntry
import com.mahavtaar.panchanga.domain.model.JaiminiCharaDashaPeriod
import com.mahavtaar.panchanga.domain.model.JaiminiCharaDashaSubPeriod
import com.mahavtaar.panchanga.domain.model.JaiminiKarakamshaInfo
import com.mahavtaar.panchanga.domain.model.JaiminiRashiDrishtiEntry
import com.mahavtaar.panchanga.domain.model.KpCuspEntry
import com.mahavtaar.panchanga.domain.model.KpPlanetEntry
import com.mahavtaar.panchanga.domain.model.KpRulingPlanetEntry
import com.mahavtaar.panchanga.domain.model.KundliPlanetEntry
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.floor

/**
 * Complete Krishnamurti Paddhati (KP Cusps, 4-level significators, Ruling Planets)
 * and Jaimini Sutras (12 Arudhas with Neelakantha exceptions, Karakamsha/Swamsha,
 * Rashi Drishti, and K.N. Rao Chara Dasha) engine matching the web implementation.
 */
object KundliKpJaiminiEngine {

    private val vimOrder = listOf(
        "Ketu" to 7.0, "Venus" to 20.0, "Sun" to 6.0, "Moon" to 10.0,
        "Mars" to 7.0, "Rahu" to 18.0, "Jupiter" to 16.0, "Saturn" to 19.0, "Mercury" to 17.0
    )

    private val signLordsEn = listOf(
        "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
        "Venus", "Mars", "Jupiter", "Saturn", "Saturn", "Jupiter"
    )

    private val planetNameHiByEn = mapOf(
        "Sun" to "सूर्य", "Moon" to "चन्द्र", "Mars" to "मंगल",
        "Mercury" to "बुध", "Jupiter" to "बृहस्पति", "Venus" to "शुक्र",
        "Saturn" to "शनि", "Rahu" to "राहु", "Ketu" to "केतु"
    )

    private val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    data class KpLordChain(
        val signLord: String,
        val starLord: String,
        val subLord: String,
        val subSubLord: String
    )

    data class KpFullResult(
        val planetEntries: List<KpPlanetEntry>,
        val cusps: List<KpCuspEntry>,
        val rulingPlanets: List<KpRulingPlanetEntry>
    )

    fun getKpLords(longitudeDeg: Double): KpLordChain {
        val norm = AyanamshaAndSolvers.normalizeDegrees(longitudeDeg)
        val signIndex = floor(norm / 30.0).toInt().coerceIn(0, 11)
        val signLord = signLordsEn[signIndex]

        val nakSpanMin = 800.0 // 13°20' = 800 arc-minutes
        val totalMinutes = norm * 60.0
        val nakIdx = (floor(totalMinutes / nakSpanMin).toInt()) % 27
        val starIdx = nakIdx % 9
        val starLord = vimOrder[starIdx].first

        val minInNak = totalMinutes % nakSpanMin
        var acc = 0.0
        var subIdx = starIdx
        var subLord = starLord
        for (i in 0 until 9) {
            val candIdx = (starIdx + i) % 9
            val span = (vimOrder[candIdx].second / 120.0) * nakSpanMin
            if (minInNak < acc + span + 1e-9) {
                subIdx = candIdx
                subLord = vimOrder[candIdx].first
                break
            }
            acc += span
        }

        val minInSub = (minInNak - acc).coerceAtLeast(0.0)
        val subTotalSpan = (vimOrder[subIdx].second / 120.0) * nakSpanMin
        var accSub = 0.0
        var subSubLord = subLord
        for (j in 0 until 9) {
            val candIdx = (subIdx + j) % 9
            val span = (vimOrder[candIdx].second / 120.0) * subTotalSpan
            if (minInSub < accSub + span + 1e-9) {
                subSubLord = vimOrder[candIdx].first
                break
            }
            accSub += span
        }

        return KpLordChain(signLord, starLord, subLord, subSubLord)
    }

    fun computeKpFull(
        lagnaLongitude: Double,
        planets: List<KundliPlanetEntry>
    ): KpFullResult {
        val cusps = (1..12).map { h ->
            val cuspLon = AyanamshaAndSolvers.normalizeDegrees(lagnaLongitude + (h - 1) * 30.0)
            val signIndex = floor(cuspLon / 30.0).toInt().coerceIn(0, 11)
            val degInSign = cuspLon % 30.0
            val lords = getKpLords(cuspLon)
            KpCuspEntry(
                houseNumber = h,
                longitude = cuspLon,
                signIndex = signIndex,
                dmsLabel = formatDms(degInSign),
                signLord = lords.signLord,
                starLord = lords.starLord,
                subLord = lords.subLord,
                subSubLord = lords.subSubLord
            )
        }

        val ownership = mutableMapOf<String, MutableList<Int>>()
        cusps.forEach { c ->
            ownership.getOrPut(c.signLord.lowercase()) { mutableListOf() }.add(c.houseNumber)
        }
        val occupied = planets.associate { it.id.lowercase() to it.houseD1 }

        val entries = planets.map { p ->
            val lords = getKpLords(p.longitude)
            val starKey = lords.starLord.lowercase()
            val ownKey = p.id.lowercase()
            val pHousesOwned = ownership[ownKey]?.toList() ?: emptyList()
            val starOcc = occupied[starKey]?.let { listOf(it) } ?: emptyList()
            val starOwned = ownership[starKey]?.toList() ?: emptyList()

            KpPlanetEntry(
                planetId = p.id,
                nameEn = p.nameEn,
                nameHi = p.nameHi,
                signIndex = p.signIndex,
                signLord = lords.signLord,
                starLord = lords.starLord,
                subLord = lords.subLord,
                dmsLabel = p.dmsLabel,
                subSubLord = lords.subSubLord,
                houseOccupied = p.houseD1,
                housesOwned = pHousesOwned,
                level1Houses = starOcc,
                level2Houses = listOf(p.houseD1),
                level3Houses = starOwned,
                level4Houses = pHousesOwned
            )
        }

        val lagnaLords = getKpLords(lagnaLongitude)
        val moonLon = planets.firstOrNull { it.id == "moon" }?.longitude ?: lagnaLongitude
        val moonLords = getKpLords(moonLon)

        val ruling = listOf(
            KpRulingPlanetEntry("Lagna Sign Lord", "लग्न राशि स्वामी", lagnaLords.signLord),
            KpRulingPlanetEntry("Lagna Star Lord", "लग्न नक्षत्र स्वामी", lagnaLords.starLord),
            KpRulingPlanetEntry("Lagna Sub-Lord", "लग्न उप-स्वामी (Sub-Lord)", lagnaLords.subLord),
            KpRulingPlanetEntry("Moon Sign Lord", "चन्द्र राशि स्वामी", moonLords.signLord),
            KpRulingPlanetEntry("Moon Star Lord", "चन्द्र नक्षत्र स्वामी", moonLords.starLord),
            KpRulingPlanetEntry("Moon Sub-Lord", "चन्द्र उप-स्वामी (Sub-Lord)", moonLords.subLord)
        )

        return KpFullResult(entries, cusps, ruling)
    }

    // =====================================================
    // JAIMINI SYSTEM (ARUDHA PADAS, RASHI DRISHTI, CHARA DASHA, KARAKAMSHA)
    // =====================================================

    private val padaMeta = listOf(
        Triple("AL", "Arudha Lagna", "आरूढ़ लग्न"),
        Triple("A2", "Dhana Pada (A2)", "धन पद (A2)"),
        Triple("A3", "Bhratri Pada (A3)", "भ्रातृ पद (A3)"),
        Triple("A4", "Matri Pada (A4)", "मातृ पद (A4)"),
        Triple("A5", "Putra Pada (A5)", "पुत्र पद (A5)"),
        Triple("A6", "Shatru Pada (A6)", "शत्रु पद (A6)"),
        Triple("A7", "Dara Pada (A7)", "दार पद (A7)"),
        Triple("A8", "Mrityu Pada (A8)", "मृत्यु पद (A8)"),
        Triple("A9", "Bhagya Pada (A9)", "भाग्य पद (A9)"),
        Triple("A10", "Karma Pada (A10)", "कर्म पद (A10)"),
        Triple("A11", "Labha Pada (A11)", "लाभ पद (A11)"),
        Triple("UL", "Upapada Lagna (UL/A12)", "उपपद लग्न (UL/A12)")
    )

    private val directSigns = setOf(0, 1, 2, 6, 7, 8) // Aries..Gemini, Libra..Sagittarius

    data class JaiminiFullResult(
        val arudhaPadas: List<JaiminiArudhaEntry>,
        val upapadaLagnaSignIndex: Int,
        val karakamsha: JaiminiKarakamshaInfo,
        val rashiDrishti: List<JaiminiRashiDrishtiEntry>,
        val charaDashas: List<JaiminiCharaDashaPeriod>
    )

    fun computeJaiminiFull(
        lagnaSignIndex: Int,
        d9LagnaSignIndex: Int,
        planets: List<KundliPlanetEntry>,
        atmakarakaId: String,
        birthTimestampMs: Long,
        nowMs: Long
    ): JaiminiFullResult {
        val arudhas = computeAllArudhas(lagnaSignIndex, planets)
        val ulSign = arudhas.last().signIndex

        val ak = planets.firstOrNull { it.id == atmakarakaId } ?: planets.first()
        val akD9Sign = ak.signD9
        val kmHouseD1 = ((akD9Sign - lagnaSignIndex + 12) % 12) + 1
        val planetsInKmD9 = planets.filter { it.signD9 == akD9Sign }.map { it.id }
        val aspectingSignsForKm = getAspectedSigns(akD9Sign)
        val planetsAspKmD9 = planets.filter { it.signD9 in aspectingSignsForKm }.map { it.id }
        val karakamsha = JaiminiKarakamshaInfo(
            atmakarakaPlanetId = ak.id,
            karakamshaSignIndex = akD9Sign,
            karakamshaHouseInD1 = kmHouseD1,
            swamshaSignIndex = d9LagnaSignIndex,
            planetsInKarakamshaD9 = planetsInKmD9,
            planetsAspectingKarakamshaD9 = planetsAspKmD9
        )

        val rashiDrishti = computeRashiDrishtiAll(planets)
        val charaDashas = computeCharaDashaTimeline(lagnaSignIndex, planets, birthTimestampMs, nowMs)

        return JaiminiFullResult(
            arudhaPadas = arudhas,
            upapadaLagnaSignIndex = ulSign,
            karakamsha = karakamsha,
            rashiDrishti = rashiDrishti,
            charaDashas = charaDashas
        )
    }

    private fun computeAllArudhas(
        lagnaSignIndex: Int,
        planets: List<KundliPlanetEntry>
    ): List<JaiminiArudhaEntry> {
        return (1..12).map { houseNum ->
            val houseSign = (lagnaSignIndex + houseNum - 1) % 12
            val lordId = signLordsEn[houseSign].lowercase()
            val lordSign = planets.firstOrNull { it.id == lordId }?.signIndex ?: houseSign
            val dist = ((lordSign - houseSign + 12) % 12) + 1
            val rawSign = (lordSign + dist - 1) % 12

            var finalSign = rawSign
            var exc = false
            var reasonEn = ""
            var reasonHi = ""

            if (rawSign == houseSign) {
                finalSign = (rawSign + 9) % 12
                exc = true
                reasonEn = "Jaimini Sutra 1.1.30: Raw pada in house itself shifted 10 signs forward."
                reasonHi = "जैमिनी सूत्र १.१.३०: स्वयं भाव में पड़ने पर १०वें स्थान में स्थानान्तरित।"
            } else if (rawSign == (houseSign + 6) % 12) {
                // Neelakantha commentary: 4th from raw sign (10th from original house)
                finalSign = (rawSign + 3) % 12
                exc = true
                reasonEn = "Neelakantha rule: Raw pada in 7th shifted 4 signs forward (10th from house)."
                reasonHi = "नीलकण्ठ नियम: ७वें में पड़ने पर ४ स्थान आगे (भाव से १०वें) स्थानान्तरित।"
            }

            val finalHouse = ((finalSign - lagnaSignIndex + 12) % 12) + 1
            val meta = padaMeta[houseNum - 1]
            JaiminiArudhaEntry(
                houseNumber = houseNum,
                code = meta.first,
                nameEn = meta.second,
                nameHi = meta.third,
                signIndex = finalSign,
                finalHouseFromLagna = finalHouse,
                isExceptionApplied = exc,
                exceptionReasonEn = reasonEn,
                exceptionReasonHi = reasonHi
            )
        }
    }

    fun getAspectedSigns(signIndex: Int): List<Int> {
        val s = ((signIndex % 12) + 12) % 12
        return when (s % 3) {
            0 -> { // Chara aspects Sthira except adjacent (s + 1)
                val adj = (s + 1) % 12
                listOf(1, 4, 7, 10).filter { it != adj }
            }
            1 -> { // Sthira aspects Chara except adjacent (s - 1)
                val adj = (s + 11) % 12
                listOf(0, 3, 6, 9).filter { it != adj }
            }
            else -> listOf(2, 5, 8, 11).filter { it != s } // Dvisvabhava aspects other duals
        }
    }

    private fun computeRashiDrishtiAll(planets: List<KundliPlanetEntry>): List<JaiminiRashiDrishtiEntry> {
        val bySign = (0 until 12).associateWith { s -> planets.filter { it.signIndex == s }.map { it.id } }
        return (0 until 12).map { s ->
            val (mobEn, mobHi) = when (s % 3) {
                0 -> "Chara (Movable)" to "चर राशि"
                1 -> "Sthira (Fixed)" to "स्थिर राशि"
                else -> "Dvisvabhava (Dual)" to "द्विस्वभाव राशि"
            }
            val aspectedSigns = getAspectedSigns(s)
            val aspecting = bySign[s] ?: emptyList()
            val aspected = aspectedSigns.flatMap { bySign[it].orEmpty() }
            JaiminiRashiDrishtiEntry(
                signIndex = s,
                mobilityEn = mobEn,
                mobilityHi = mobHi,
                aspectedSignIndices = aspectedSigns,
                aspectingPlanetIds = aspecting,
                aspectedPlanetIds = aspected
            )
        }
    }

    private fun strongerLord(signIndex: Int, planets: List<KundliPlanetEntry>): KundliPlanetEntry? {
        if (signIndex == 7) {
            val mars = planets.firstOrNull { it.id == "mars" }
            val ketu = planets.firstOrNull { it.id == "ketu" }
            if (mars != null && ketu != null) {
                val mConj = planets.count { it.signIndex == mars.signIndex }
                val kConj = planets.count { it.signIndex == ketu.signIndex }
                if (kConj > mConj) return ketu
                if (mConj > kConj) return mars
                return if (ketu.degreeInSign > mars.degreeInSign) ketu else mars
            }
            return mars
        }
        if (signIndex == 10) {
            val saturn = planets.firstOrNull { it.id == "saturn" }
            val rahu = planets.firstOrNull { it.id == "rahu" }
            if (saturn != null && rahu != null) {
                val sConj = planets.count { it.signIndex == saturn.signIndex }
                val rConj = planets.count { it.signIndex == rahu.signIndex }
                if (rConj > sConj) return rahu
                if (sConj > rConj) return saturn
                return if (rahu.degreeInSign > saturn.degreeInSign) rahu else saturn
            }
            return saturn
        }
        val lordId = signLordsEn[signIndex].lowercase()
        return planets.firstOrNull { it.id == lordId }
    }

    private fun charaSignSeq(startSign: Int): List<Int> {
        val isDirect = startSign in directSigns
        return (0 until 12).map { i ->
            if (isDirect) (startSign + i) % 12 else (((startSign - i) % 12) + 12) % 12
        }
    }

    private fun computeCharaDashaTimeline(
        lagnaSignIndex: Int,
        planets: List<KundliPlanetEntry>,
        birthTimestampMs: Long,
        nowMs: Long
    ): List<JaiminiCharaDashaPeriod> {
        val msPerYear = (365.2425 * 86_400_000.0).toLong()
        val seq = charaSignSeq(lagnaSignIndex)
        val out = mutableListOf<JaiminiCharaDashaPeriod>()
        var cursor = birthTimestampMs

        for (s in seq) {
            val isDirect = s in directSigns
            val lord = strongerLord(s, planets)
            val lordSign = lord?.signIndex ?: s
            val dist = if (isDirect) ((lordSign - s + 12) % 12) + 1 else ((s - lordSign + 12) % 12) + 1
            var years = dist - 1
            if (years == 0) years = 12
            if (lord?.dignityEn == "Exalted" && years < 12) years += 1
            else if (lord?.dignityEn == "Debilitated" && years > 1) years -= 1

            val periodDurMs = years.toLong() * msPerYear
            val periodEnd = cursor + periodDurMs

            val subSeq = charaSignSeq(s)
            val subDurMs = periodDurMs / 12L
            var subCursor = cursor
            val subs = subSeq.map { subS ->
                val subEnd = subCursor + subDurMs
                val item = JaiminiCharaDashaSubPeriod(
                    signIndex = subS,
                    startDate = formatUtcDate(subCursor),
                    endDate = formatUtcDate(subEnd),
                    durationMonths = years
                )
                subCursor = subEnd
                item
            }

            out += JaiminiCharaDashaPeriod(
                signIndex = s,
                durationYears = years,
                isDirect = isDirect,
                startDate = formatUtcDate(cursor),
                endDate = formatUtcDate(periodEnd),
                isCurrent = nowMs in cursor until periodEnd,
                subPeriods = subs
            )
            cursor = periodEnd
        }
        return out
    }

    private fun formatDms(deg: Double): String {
        val d = floor(deg).toInt()
        val mExact = (deg - d) * 60.0
        val m = floor(mExact).toInt()
        val s = floor((mExact - m) * 60.0).toInt()
        return "%02d°%02d'%02d\"".format(d, m, s)
    }

    private fun formatUtcDate(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().format(dateFmt)
}
