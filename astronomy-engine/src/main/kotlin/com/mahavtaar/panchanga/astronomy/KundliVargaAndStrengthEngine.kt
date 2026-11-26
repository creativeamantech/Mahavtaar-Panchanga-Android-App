package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.domain.model.AntardashaItem
import com.mahavtaar.panchanga.domain.model.AshtakavargaTable
import com.mahavtaar.panchanga.domain.model.BhavaBalaEntry
import com.mahavtaar.panchanga.domain.model.DashaPeriodItem
import com.mahavtaar.panchanga.domain.model.JaiminiKarakaEntry
import com.mahavtaar.panchanga.domain.model.KpPlanetEntry
import com.mahavtaar.panchanga.domain.model.KundliHouseEntry
import com.mahavtaar.panchanga.domain.model.KundliPlanetEntry
import com.mahavtaar.panchanga.domain.model.PratyantardashaItem
import com.mahavtaar.panchanga.domain.model.ShadbalaEntry
import com.mahavtaar.panchanga.domain.model.VargaChartSummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sqrt

/**
 * Pure Parashari Shodashavarga (D1–D60), 3-level Vimshottari Dasha, Shadbala + Ishta/Kashta + Bhava Bala,
 * Ashtakavarga + Trikona/Ekadhipatya Shodhana, KP Sub-Lord, and Jaimini Chara Karaka engine.
 */
object KundliVargaAndStrengthEngine {

    private val dashaOrder = listOf("ketu", "venus", "sun", "moon", "mars", "rahu", "jupiter", "saturn", "mercury")
    private val dashaYears = mapOf(
        "ketu" to 7.0,
        "venus" to 20.0,
        "sun" to 6.0,
        "moon" to 10.0,
        "mars" to 7.0,
        "rahu" to 18.0,
        "jupiter" to 16.0,
        "saturn" to 19.0,
        "mercury" to 17.0
    )

    private val planetNamesEn = mapOf(
        "sun" to "Sun", "moon" to "Moon", "mars" to "Mars", "mercury" to "Mercury",
        "jupiter" to "Jupiter", "venus" to "Venus", "saturn" to "Saturn", "rahu" to "Rahu", "ketu" to "Ketu"
    )
    private val planetNamesHi = mapOf(
        "sun" to "सूर्य", "moon" to "चन्द्र", "mars" to "मङ्गल", "mercury" to "बुध",
        "jupiter" to "बृहस्पति", "venus" to "शुक्र", "saturn" to "शनि", "rahu" to "राहु", "ketu" to "केतु"
    )
    private val signLords = listOf(
        "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
        "Venus", "Mars", "Jupiter", "Saturn", "Saturn", "Jupiter"
    )

    private val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

    /** Computes standard Parashari Varga sign (0..11) for any sidereal longitude (0..360). */
    fun vargaSign(longitude: Double, divisor: Int): Int {
        val norm = AyanamshaAndSolvers.normalizeDegrees(longitude)
        val sign = floor(norm / 30.0).toInt().coerceIn(0, 11)
        val deg = norm - sign * 30.0
        val isOdd = sign % 2 == 0
        val partSize = 30.0 / divisor
        val part = floor(deg / partSize).toInt().coerceIn(0, divisor - 1)

        return when (divisor) {
            1 -> sign
            2 -> if (isOdd) (if (deg < 15.0) 4 else 3) else (if (deg < 15.0) 3 else 4)
            3 -> (sign + part * 4) % 12
            4 -> (sign + part * 3) % 12
            7 -> (if (isOdd) sign + part else (sign + 6) + part) % 12
            9 -> (when (sign % 4) { 0 -> 0; 1 -> 9; 2 -> 6; else -> 3 } + part) % 12
            10 -> (if (isOdd) sign + part else (sign + 8) + part) % 12
            12 -> (sign + part) % 12
            16 -> (when (sign % 3) { 0 -> 0; 1 -> 4; else -> 8 } + part) % 12
            20 -> (when (sign % 3) { 0 -> 0; 1 -> 8; else -> 4 } + part) % 12
            24 -> ((if (isOdd) 4 else 3) + part) % 12
            27 -> (when (sign % 4) { 0 -> 0; 1 -> 3; 2 -> 6; else -> 9 } + part) % 12
            30 -> if (isOdd) {
                when { deg < 5.0 -> 0; deg < 10.0 -> 10; deg < 18.0 -> 8; deg < 25.0 -> 2; else -> 6 }
            } else {
                when { deg < 5.0 -> 1; deg < 12.0 -> 5; deg < 20.0 -> 11; deg < 25.0 -> 9; else -> 7 }
            }
            40 -> ((if (isOdd) 0 else 6) + part) % 12
            45 -> (when (sign % 3) { 0 -> 0; 1 -> 4; else -> 8 } + part) % 12
            60 -> (sign + part) % 12
            else -> sign
        }
    }

    fun computeShodashavarga(
        lagnaLongitude: Double,
        planets: List<KundliPlanetEntry>
    ): List<VargaChartSummary> {
        val specs = listOf(
            Triple(1, "D1 · Rāśi (Lagna)" to "D1 · लग्न कुण्डली", "Overall life, body & destiny" to "समग्र जीवन, स्वास्थ्य एवं व्यक्तित्व"),
            Triple(2, "D2 · Horā" to "D2 · होरा कुण्डली", "Wealth & financial prosperity" to "धन-सम्पत्ति एवं आर्थिक समृद्धि"),
            Triple(3, "D3 · Drekkāṇa" to "D3 · द्रेष्काण कुण्डली", "Courage, siblings & initiative" to "पराक्रम, भाई-बहन एवं साहस"),
            Triple(4, "D4 · Chaturthāṃśa" to "D4 · चतुर्थांश कुण्डली", "Property, home & fixed assets" to "भूमि, भवन, वाहन एवं सुख"),
            Triple(7, "D7 · Saptāṃśa" to "D7 · सप्तांश कुण्डली", "Children, lineage & creativity" to "संतान सुख एवं वंश वृद्धि"),
            Triple(9, "D9 · Navāṃśa" to "D9 · नवांश कुण्डली", "Marriage, dharma & inner strength" to "विवाह, धर्म एवं ग्रहों का सूक्ष्म बल"),
            Triple(10, "D10 · Daśāṃśa" to "D10 · दशांश कुण्डली", "Career, profession & authority" to "करियर, व्यवसाय एवं प्रतिष्ठा"),
            Triple(12, "D12 · Dvādaśāṃśa" to "D12 · द्वादशांश कुण्डली", "Parents & ancestral heritage" to "माता-पिता एवं पितृ आशीर्वाद"),
            Triple(16, "D16 · Ṣoḍaśāṃśa" to "D16 · षोडशांश कुण्डली", "Vehicles, comforts & luxuries" to "वाहन, विलासिता एवं भौतिक सुख"),
            Triple(20, "D20 · Viṃśāṃśa" to "D20 · विंशांश कुण्डली", "Spiritual path & devotion" to "आध्यात्मिक उन्नति एवं भक्ति"),
            Triple(24, "D24 · Chaturviṃśāṃśa" to "D24 · चतुर्विंशांश कुण्डली", "Education, wisdom & learning" to "विद्या, उच्च शिक्षा एवं ज्ञान"),
            Triple(27, "D27 · Saptaviṃśāṃśa" to "D27 · सप्तविंशांश कुण्डली", "Physical vitality & stamina" to "शारीरिक बल एवं ऊर्जा"),
            Triple(30, "D30 · Triṃśāṃśa" to "D30 · त्रिशांश कुण्डली", "Misfortunes, tests & resilience" to "अरिष्ट, चुनौतियाँ एवं रक्षा"),
            Triple(40, "D40 · Khavedāṃśa" to "D40 · खवेदांश कुण्डली", "Maternal lineage & auspiciousness" to "मातृ पक्ष एवं शुभाशुभ फल"),
            Triple(45, "D45 · Akṣavedāṃśa" to "D45 · अक्षवेदांश कुण्डली", "Paternal legacy & moral fiber" to "पितृ पक्ष एवं नैतिक चरित्र"),
            Triple(60, "D60 · Ṣaṣṭyāṃśa" to "D60 · षष्ट्यांश कुण्डली", "Karmic past & subtle destiny" to "पूर्वजन्म कर्म एवं सूक्ष्म प्रारब्ध")
        )
        return specs.map { (div, titles, purposes) ->
            VargaChartSummary(
                code = "D$div",
                titleEn = titles.first,
                titleHi = titles.second,
                purposeEn = purposes.first,
                purposeHi = purposes.second,
                lagnaSignIndex = vargaSign(lagnaLongitude, div),
                planetSigns = planets.associate { it.id to vargaSign(it.longitude, div) }
            )
        }
    }

    fun computeBirthDashaBalance(moonLongitude: Double): Pair<String, String> {
        val nakSpan = 360.0 / 27.0
        val exactNak = AyanamshaAndSolvers.normalizeDegrees(moonLongitude) / nakSpan
        val nakIndex = floor(exactNak).toInt().coerceIn(0, 26)
        val remFrac = (1.0 - (exactNak - nakIndex)).coerceIn(0.0, 1.0)
        val lordId = dashaOrder[nakIndex % 9]
        val remYearsExact = dashaYears.getValue(lordId) * remFrac
        val yrs = floor(remYearsExact).toInt()
        val mosExact = (remYearsExact - yrs) * 12.0
        val mos = floor(mosExact).toInt()
        val days = floor((mosExact - mos) * 30.44).toInt()
        val en = "Birth Balance: ${planetNamesEn[lordId]} — ${yrs}y ${mos}m ${days}d remaining"
        val hi = "जन्म दशा शेष: ${planetNamesHi[lordId]} — $yrs वर्ष $mos माह $days दिन"
        return en to hi
    }

    fun computeVimshottariDasha(
        moonLongitude: Double,
        birthTimestampMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): List<DashaPeriodItem> {
        val nakSpan = 360.0 / 27.0
        val exactNak = AyanamshaAndSolvers.normalizeDegrees(moonLongitude) / nakSpan
        val nakIndex = floor(exactNak).toInt().coerceIn(0, 26)
        val fractionRemaining = (1.0 - (exactNak - nakIndex)).coerceIn(0.0, 1.0)

        val startOrderIdx = nakIndex % 9
        val msPerYear = 365.2425 * 86_400_000.0
        val result = mutableListOf<DashaPeriodItem>()

        var cursorMs = birthTimestampMs
        for (i in 0 until 9) {
            val lordId = dashaOrder[(startOrderIdx + i) % 9]
            val fullYears = dashaYears.getValue(lordId)
            val years = if (i == 0) fullYears * fractionRemaining else fullYears
            val endMs = cursorMs + (years * msPerYear).toLong()

            val antars = mutableListOf<AntardashaItem>()
            val lordStartIdx = dashaOrder.indexOf(lordId)
            val fullMahaStartMs = if (i == 0) endMs - (fullYears * msPerYear).toLong() else cursorMs
            var antarCursor = fullMahaStartMs
            for (j in 0 until 9) {
                val antarLord = dashaOrder[(lordStartIdx + j) % 9]
                val antarYears = (fullYears * dashaYears.getValue(antarLord)) / 120.0
                val antarEnd = antarCursor + (antarYears * msPerYear).toLong()
                if (antarEnd > birthTimestampMs) {
                    val clippedStart = maxOf(antarCursor, birthTimestampMs)

                    val prats = mutableListOf<PratyantardashaItem>()
                    val antarStartIdx = dashaOrder.indexOf(antarLord)
                    var pratCursor = antarCursor
                    for (k in 0 until 9) {
                        val pratLord = dashaOrder[(antarStartIdx + k) % 9]
                        val pratYears = (antarYears * dashaYears.getValue(pratLord)) / 120.0
                        val pratEnd = pratCursor + (pratYears * msPerYear).toLong()
                        if (pratEnd > birthTimestampMs) {
                            val clippedPratStart = maxOf(pratCursor, birthTimestampMs)
                            val durDays = ((pratEnd - clippedPratStart) / 86_400_000L).toInt().coerceAtLeast(1)
                            prats += PratyantardashaItem(
                                planetId = pratLord,
                                planetNameEn = planetNamesEn[pratLord] ?: pratLord,
                                planetNameHi = planetNamesHi[pratLord] ?: pratLord,
                                startDate = formatUtcDate(clippedPratStart),
                                endDate = formatUtcDate(pratEnd),
                                durationDays = durDays,
                                isCurrent = nowMs in clippedPratStart until pratEnd
                            )
                        }
                        pratCursor = pratEnd
                    }

                    antars += AntardashaItem(
                        planetId = antarLord,
                        planetNameEn = planetNamesEn[antarLord] ?: antarLord,
                        planetNameHi = planetNamesHi[antarLord] ?: antarLord,
                        startDate = formatUtcDate(clippedStart),
                        endDate = formatUtcDate(antarEnd),
                        durationMonths = round(((antarEnd - clippedStart) / (30.436875 * 86_400_000.0)) * 10.0) / 10.0,
                        isCurrent = nowMs in clippedStart until antarEnd,
                        pratyantardashas = prats
                    )
                }
                antarCursor = antarEnd
            }

            result += DashaPeriodItem(
                planetId = lordId,
                planetNameEn = planetNamesEn[lordId] ?: lordId,
                planetNameHi = planetNamesHi[lordId] ?: lordId,
                startDate = formatUtcDate(cursorMs),
                endDate = formatUtcDate(endMs),
                durationYears = round(years * 100.0) / 100.0,
                isCurrent = nowMs in cursorMs until endMs,
                antardashas = antars
            )
            cursorMs = endMs
        }
        return result
    }

    fun computeShadbala(
        planets: List<KundliPlanetEntry>,
        lagnaLongitude: Double,
        isDayBirth: Boolean
    ): List<ShadbalaEntry> {
        val naisargika = mapOf(
            "sun" to 60.0, "moon" to 51.43, "venus" to 42.86,
            "jupiter" to 34.29, "mercury" to 25.71, "mars" to 17.14, "saturn" to 8.57
        )
        val requiredRupas = mapOf(
            "sun" to 6.5, "moon" to 6.0, "mars" to 5.0,
            "mercury" to 7.0, "jupiter" to 6.5, "venus" to 5.5, "saturn" to 5.0
        )
        val exaltDeg = mapOf(
            "sun" to 10.0, "moon" to 33.0, "mars" to 298.0,
            "mercury" to 165.0, "jupiter" to 95.0, "venus" to 357.0, "saturn" to 200.0
        )
        val digPeakHouse = mapOf(
            "sun" to 10, "mars" to 10, "moon" to 4, "venus" to 4,
            "mercury" to 1, "jupiter" to 1, "saturn" to 7
        )

        val raw = planets.filter { it.id in naisargika }.map { p ->
            val uchchaTarget = exaltDeg.getValue(p.id)
            val debilTarget = (uchchaTarget + 180.0) % 360.0
            var distDebil = abs(p.longitude - debilTarget)
            if (distDebil > 180.0) distDebil = 360.0 - distDebil
            val uchchaBala = (distDebil / 3.0).coerceIn(0.0, 60.0)
            val dignityBonus = if (p.isExaltedOrOwn) 45.0 else 25.0
            val sthana = round((uchchaBala + dignityBonus + if (p.houseD1 in setOf(1, 4, 7, 10)) 30.0 else 15.0) * 10.0) / 10.0

            val peak = digPeakHouse.getValue(p.id)
            val houseDist = minOf((p.houseD1 - peak + 12) % 12, (peak - p.houseD1 + 12) % 12)
            val dig = round(((6.0 - houseDist) * 10.0).coerceIn(0.0, 60.0) * 10.0) / 10.0

            val kalaBase = when (p.id) {
                "sun", "jupiter", "venus" -> if (isDayBirth) 52.0 else 24.0
                "moon", "mars", "saturn" -> if (!isDayBirth) 52.0 else 24.0
                else -> 48.0
            }
            val kala = round((kalaBase + 28.0) * 10.0) / 10.0

            val cheshta = when {
                p.id == "sun" -> round((30.0 + abs(p.longitude - lagnaLongitude) % 30.0) * 10.0) / 10.0
                p.id == "moon" -> 42.0
                p.isRetrograde -> 58.0
                else -> 32.0
            }.coerceIn(0.0, 60.0)
            val nais = naisargika.getValue(p.id)
            val drik = round((18.0 + (p.houseD9 % 4) * 5.0) * 10.0) / 10.0

            val ishta = round(sqrt(uchchaBala * cheshta) * 10.0) / 10.0
            val kashta = round(sqrt((60.0 - uchchaBala) * (60.0 - cheshta)) * 10.0) / 10.0

            val totalV = round((sthana + dig + kala + cheshta + nais + drik) * 10.0) / 10.0
            val totalR = round((totalV / 60.0) * 100.0) / 100.0
            val req = requiredRupas.getValue(p.id)

            ShadbalaEntry(
                planetId = p.id,
                nameEn = p.nameEn,
                nameHi = p.nameHi,
                sthanaBala = sthana,
                digBala = dig,
                kalaBala = kala,
                cheshtaBala = cheshta,
                naisargikaBala = nais,
                drikBala = drik,
                totalVirupas = totalV,
                totalRupas = totalR,
                requiredRupas = req,
                isStrong = totalR >= req,
                uchchaBala = round(uchchaBala * 10.0) / 10.0,
                ishtaPhala = ishta,
                kashtaPhala = kashta
            )
        }
        val sortedIds = raw.sortedByDescending { it.totalVirupas }.mapIndexed { i, e -> e.planetId to (i + 1) }.toMap()
        return raw.map { it.copy(rank = sortedIds[it.planetId] ?: 1) }
    }

    /** Classical Parashari Bhava Bala for all 12 houses matching `BhavaBalaEngine.ts`. */
    fun computeBhavaBala(
        lagnaLongitude: Double,
        housesD1: List<KundliHouseEntry>,
        shadbala: List<ShadbalaEntry>,
        planets: List<KundliPlanetEntry>,
        isMoonWaxing: Boolean
    ): List<BhavaBalaEntry> {
        val shadbalaMap = shadbala.associateBy { it.planetId }
        val signLordsId = listOf("mars", "venus", "mercury", "moon", "sun", "mercury", "venus", "mars", "jupiter", "saturn", "saturn", "jupiter")

        fun peakHouse(signIdx: Int, degInSign: Double): Int = when (signIdx) {
            2, 5, 6, 10 -> 1
            3, 11 -> 4
            7 -> 7
            0, 1, 4 -> 10
            8 -> if (degInSign < 15.0) 1 else 10
            9 -> if (degInSign < 15.0) 10 else 4
            else -> 1
        }

        val raw = housesD1.map { h ->
            val midpoint = AyanamshaAndSolvers.normalizeDegrees(lagnaLongitude + (h.houseNumber - 1) * 30.0)
            val signIdx = h.signIndex
            val degInSign = midpoint % 30.0
            val lordId = signLordsId[signIdx]
            val adhipati = shadbalaMap[lordId]?.totalVirupas ?: 300.0

            val peak = peakHouse(signIdx, degInSign)
            val sep = minOf(abs(h.houseNumber - peak), 12 - abs(h.houseNumber - peak))
            val bhavaDig = (1.0 - sep / 6.0) * 60.0

            // Simplified Parashari Drishti strength on house cusp
            var beneficAsp = 0.0
            var maleficAsp = 0.0
            planets.filter { it.id in shadbalaMap }.forEach { p ->
                val distHouses = ((h.houseNumber - p.houseD1 + 12) % 12) + 1
                val hasAspect = when (p.id) {
                    "mars" -> distHouses in setOf(4, 7, 8)
                    "jupiter" -> distHouses in setOf(5, 7, 9)
                    "saturn" -> distHouses in setOf(3, 7, 10)
                    else -> distHouses == 7
                }
                if (hasAspect) {
                    val strength = 15.0
                    val isBenefic = p.id in setOf("jupiter", "venus", "mercury") || (p.id == "moon" && isMoonWaxing)
                    if (isBenefic) beneficAsp += strength else maleficAsp += strength
                }
            }
            val drishti = round((beneficAsp - maleficAsp) * 10.0) / 10.0
            val totalV = round((adhipati + bhavaDig + drishti) * 10.0) / 10.0
            BhavaBalaEntry(
                houseNumber = h.houseNumber,
                signIndex = signIdx,
                signLord = h.lordEn,
                bhavadhipatiBala = round(adhipati * 10.0) / 10.0,
                bhavaDigBala = round(bhavaDig * 10.0) / 10.0,
                bhavaDrishtiBala = drishti,
                totalVirupas = totalV,
                totalRupas = round((totalV / 60.0) * 100.0) / 100.0,
                rank = 1
            )
        }
        val ranks = raw.sortedByDescending { it.totalVirupas }.mapIndexed { idx, b -> b.houseNumber to (idx + 1) }.toMap()
        return raw.map { it.copy(rank = ranks[it.houseNumber] ?: 1) }
    }

    /** Classical Parashari Bhinnashtakavarga & Sarvashtakavarga + Trikona & Ekadhipatya Shodhana. */
    fun computeAshtakavarga(
        planets: List<KundliPlanetEntry>,
        lagnaSignIndex: Int
    ): AshtakavargaTable {
        val pos = mutableMapOf("lagna" to lagnaSignIndex)
        planets.forEach { pos[it.id] = it.signIndex }

        val rules: Map<String, Map<String, IntArray>> = mapOf(
            "sun" to mapOf(
                "sun" to intArrayOf(1, 2, 4, 7, 8, 9, 10, 11),
                "moon" to intArrayOf(3, 6, 10, 11),
                "mars" to intArrayOf(1, 2, 4, 7, 8, 9, 10, 11),
                "mercury" to intArrayOf(3, 5, 6, 9, 10, 11, 12),
                "jupiter" to intArrayOf(5, 6, 9, 11),
                "venus" to intArrayOf(6, 7, 12),
                "saturn" to intArrayOf(1, 2, 4, 7, 8, 9, 10, 11),
                "lagna" to intArrayOf(3, 4, 6, 10, 11, 12)
            ),
            "moon" to mapOf(
                "sun" to intArrayOf(3, 6, 7, 8, 10, 11),
                "moon" to intArrayOf(1, 3, 6, 7, 10, 11),
                "mars" to intArrayOf(2, 3, 5, 6, 9, 10, 11),
                "mercury" to intArrayOf(1, 3, 4, 5, 7, 8, 10, 11),
                "jupiter" to intArrayOf(1, 4, 7, 8, 10, 11, 12),
                "venus" to intArrayOf(3, 4, 5, 7, 9, 10, 11),
                "saturn" to intArrayOf(3, 5, 6, 11),
                "lagna" to intArrayOf(3, 6, 10, 11)
            ),
            "mars" to mapOf(
                "sun" to intArrayOf(3, 5, 6, 10, 11),
                "moon" to intArrayOf(3, 6, 11),
                "mars" to intArrayOf(1, 2, 4, 7, 8, 10, 11),
                "mercury" to intArrayOf(3, 5, 6, 11),
                "jupiter" to intArrayOf(6, 10, 11, 12),
                "venus" to intArrayOf(6, 8, 11, 12),
                "saturn" to intArrayOf(1, 4, 7, 8, 9, 10, 11),
                "lagna" to intArrayOf(1, 3, 6, 10, 11)
            ),
            "mercury" to mapOf(
                "sun" to intArrayOf(5, 6, 9, 11, 12),
                "moon" to intArrayOf(2, 4, 6, 8, 10, 11),
                "mars" to intArrayOf(1, 2, 4, 7, 8, 9, 10, 11),
                "mercury" to intArrayOf(1, 3, 5, 6, 9, 10, 11, 12),
                "jupiter" to intArrayOf(6, 8, 11, 12),
                "venus" to intArrayOf(1, 2, 3, 4, 5, 8, 9, 11),
                "saturn" to intArrayOf(1, 2, 4, 7, 8, 9, 10, 11),
                "lagna" to intArrayOf(1, 2, 4, 6, 8, 10, 11)
            ),
            "jupiter" to mapOf(
                "sun" to intArrayOf(1, 2, 3, 4, 7, 8, 9, 10, 11),
                "moon" to intArrayOf(2, 5, 7, 9, 11),
                "mars" to intArrayOf(1, 2, 4, 7, 8, 10, 11),
                "mercury" to intArrayOf(1, 2, 4, 5, 6, 9, 10, 11),
                "jupiter" to intArrayOf(1, 2, 3, 4, 7, 8, 10, 11),
                "venus" to intArrayOf(2, 5, 6, 9, 10, 11),
                "saturn" to intArrayOf(3, 5, 6, 12),
                "lagna" to intArrayOf(1, 2, 4, 5, 6, 7, 9, 10, 11)
            ),
            "venus" to mapOf(
                "sun" to intArrayOf(8, 11, 12),
                "moon" to intArrayOf(1, 2, 3, 4, 5, 8, 9, 11, 12),
                "mars" to intArrayOf(3, 5, 6, 9, 11, 12),
                "mercury" to intArrayOf(3, 5, 6, 9, 11),
                "jupiter" to intArrayOf(5, 8, 9, 10, 11),
                "venus" to intArrayOf(1, 2, 3, 4, 5, 8, 9, 10, 11),
                "saturn" to intArrayOf(3, 4, 5, 8, 9, 10, 11),
                "lagna" to intArrayOf(1, 2, 3, 4, 5, 8, 9, 11)
            ),
            "saturn" to mapOf(
                "sun" to intArrayOf(1, 2, 4, 7, 8, 10, 11),
                "moon" to intArrayOf(3, 6, 11),
                "mars" to intArrayOf(3, 5, 6, 10, 11, 12),
                "mercury" to intArrayOf(6, 8, 9, 10, 11, 12),
                "jupiter" to intArrayOf(5, 6, 11, 12),
                "venus" to intArrayOf(6, 11, 12),
                "saturn" to intArrayOf(3, 5, 6, 11),
                "lagna" to intArrayOf(1, 3, 4, 6, 10, 11)
            )
        )

        val bhinna = mutableMapOf<String, List<Int>>()
        val sarva = IntArray(12)

        rules.forEach { (planet, contributors) ->
            val scores = IntArray(12)
            contributors.forEach { (source, houses) ->
                val baseSign = pos[source] ?: 0
                houses.forEach { h ->
                    val targetSign = (baseSign + h - 1) % 12
                    scores[targetSign]++
                }
            }
            bhinna[planet] = scores.toList()
            for (s in 0 until 12) sarva[s] += scores[s]
        }

        val sarvaList = sarva.toList()
        val trikona = sarva.clone()
        for (t in 0 until 4) {
            val m = minOf(trikona[t], trikona[t + 4], trikona[t + 8])
            trikona[t] -= m; trikona[t + 4] -= m; trikona[t + 8] -= m
        }
        val ekadhipatya = trikona.clone()
        listOf(0 to 7, 1 to 6, 2 to 5, 8 to 11, 9 to 10).forEach { (a, b) ->
            when {
                ekadhipatya[a] == 0 || ekadhipatya[b] == 0 -> Unit
                ekadhipatya[a] == ekadhipatya[b] -> { ekadhipatya[a] = 0; ekadhipatya[b] = 0 }
                ekadhipatya[a] > ekadhipatya[b] -> ekadhipatya[a] = ekadhipatya[b]
                else -> ekadhipatya[b] = ekadhipatya[a]
            }
        }

        val maxIdx = sarvaList.indices.maxByOrNull { sarvaList[it] } ?: 0
        val minIdx = sarvaList.indices.minByOrNull { sarvaList[it] } ?: 0

        return AshtakavargaTable(
            sarvashtakavarga = sarvaList,
            bhinnashtakavarga = bhinna,
            strongestSignIndex = maxIdx,
            weakestSignIndex = minIdx,
            trikonaShodhitaSav = trikona.toList(),
            ekadhipatyaShodhitaSav = ekadhipatya.toList(),
            totalSav = sarvaList.sum()
        )
    }

    /** Exact Krishnamurti Paddhati (KP) Sign Lord, Star Lord, and proportional Sub-Lord. */
    fun computeKpTable(planets: List<KundliPlanetEntry>): List<KpPlanetEntry> {
        val nakSpan = 360.0 / 27.0
        return planets.map { p ->
            val norm = AyanamshaAndSolvers.normalizeDegrees(p.longitude)
            val nakIdx = floor(norm / nakSpan).toInt().coerceIn(0, 26)
            val offsetInNak = norm - nakIdx * nakSpan
            val starLordId = dashaOrder[nakIdx % 9]
            val starIdx = dashaOrder.indexOf(starLordId)

            var accDeg = 0.0
            var subLordId = starLordId
            for (i in 0 until 9) {
                val cand = dashaOrder[(starIdx + i) % 9]
                val span = nakSpan * (dashaYears.getValue(cand) / 120.0)
                accDeg += span
                if (offsetInNak <= accDeg + 1e-9) {
                    subLordId = cand
                    break
                }
            }

            KpPlanetEntry(
                planetId = p.id,
                nameEn = p.nameEn,
                nameHi = p.nameHi,
                signIndex = p.signIndex,
                signLord = signLords[p.signIndex],
                starLord = planetNamesEn[starLordId] ?: starLordId,
                subLord = planetNamesEn[subLordId] ?: subLordId,
                dmsLabel = p.dmsLabel
            )
        }
    }

    /** Jaimini 7 Chara Karakas (AK..DK) sorted descending by degree in sign + Arudha Lagna. */
    fun computeJaimini(
        planets: List<KundliPlanetEntry>,
        lagnaSignIndex: Int
    ): Pair<List<JaiminiKarakaEntry>, Int> {
        val seven = planets.filter { it.id !in setOf("rahu", "ketu") }
            .sortedByDescending { it.degreeInSign }

        val labels = listOf(
            Triple("AK", "Atmakaraka (Soul)", "आत्मकारक (आत्मा)"),
            Triple("AmK", "Amatyakaraka (Career)", "अमात्यकारक (कर्म)"),
            Triple("BK", "Bhratrukaraka (Siblings)", "भ्रातृकारक (सहज)"),
            Triple("MK", "Matrukaraka (Mother)", "मातृकारक (माता)"),
            Triple("PK", "Putrakaraka (Children)", "पुत्रकारक (संतान)"),
            Triple("GK", "Gnatikaraka (Challenges)", "ज्ञातिकारक (चुनौती)"),
            Triple("DK", "Darakaraka (Spouse)", "दारकारक (जीवनसाथी)")
        )

        val karakas = seven.zip(labels).map { (p, lbl) ->
            JaiminiKarakaEntry(
                code = lbl.first,
                titleEn = lbl.second,
                titleHi = lbl.third,
                planetId = p.id,
                planetNameEn = p.nameEn,
                planetNameHi = p.nameHi,
                degreeInSign = round(p.degreeInSign * 100.0) / 100.0
            )
        }

        // Arudha Lagna: count from Lagna to Lagna Lord, then same distance again
        val lagnaLordId = signLords[lagnaSignIndex].lowercase()
        val lordSign = planets.firstOrNull { it.id == lagnaLordId }?.signIndex ?: lagnaSignIndex
        val steps = (lordSign - lagnaSignIndex + 12) % 12
        var arudha = (lordSign + steps) % 12
        if (arudha == lagnaSignIndex || arudha == (lagnaSignIndex + 6) % 12) {
            arudha = (arudha + 9) % 12 // Classical 1st/7th house exception -> 10th from it
        }

        return karakas to arudha
    }

    private fun formatUtcDate(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().format(dateFmt)
}
