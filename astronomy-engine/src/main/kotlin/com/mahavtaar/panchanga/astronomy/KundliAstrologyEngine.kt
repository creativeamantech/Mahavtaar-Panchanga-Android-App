package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.KundliBirthInput
import com.mahavtaar.panchanga.domain.model.KundliCalculationAudit
import com.mahavtaar.panchanga.domain.model.KundliHouseEntry
import com.mahavtaar.panchanga.domain.model.KundliPlanetEntry
import com.mahavtaar.panchanga.domain.model.KundliReport
import io.github.cosinekitty.astronomy.Time
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure Kotlin sidereal Janam Kundli Engine matching `src/lib/lagnaEngine.server.ts`
 * and `src/lib/kundliEngine.ts`.
 * Computes exact Sidereal Lagna, Navagraha longitudes, D1/D9 charts, Shodashavarga,
 * 3-level Vimshottari Dasha, Shadbala + Bhava Bala, Ashtakavarga, KP, Jaimini, Gochar, Doshas, Yogas, and Audit.
 */
object KundliAstrologyEngine {

    private data class PlanetSpec(
        val id: String,
        val symbol: String,
        val nameEn: String,
        val nameHi: String,
        val shortEn: String,
        val shortHi: String,
        val exaltSign: Int,
        val debilSign: Int,
        val ownSigns: Set<Int>,
        val combustDeg: Double?
    )

    private val specs = listOf(
        PlanetSpec("sun", "☉", "Sun", "सूर्य", "Su", "सू", 0, 6, setOf(4), null),
        PlanetSpec("moon", "☽", "Moon", "चन्द्र", "Mo", "चं", 1, 7, setOf(3), 12.0),
        PlanetSpec("mars", "♂", "Mars", "मङ्गल", "Ma", "मं", 9, 3, setOf(0, 7), 17.0),
        PlanetSpec("mercury", "☿", "Mercury", "बुध", "Me", "बु", 5, 11, setOf(2, 5), 14.0),
        PlanetSpec("jupiter", "♃", "Jupiter", "बृहस्पति", "Ju", "गु", 3, 9, setOf(8, 11), 11.0),
        PlanetSpec("venus", "♀", "Venus", "शुक्र", "Ve", "शु", 11, 5, setOf(1, 6), 10.0),
        PlanetSpec("saturn", "♄", "Saturn", "शनि", "Sa", "श", 6, 0, setOf(9, 10), 15.0),
        PlanetSpec("rahu", "☊", "Rahu", "राहु", "Ra", "रा", 1, 7, setOf(10), null),
        PlanetSpec("ketu", "☋", "Ketu", "केतु", "Ke", "के", 7, 1, setOf(7), null)
    )

    private val signLordsEn = listOf(
        "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
        "Venus", "Mars", "Jupiter", "Saturn", "Saturn", "Jupiter"
    )
    private val signLordsHi = listOf(
        "मङ्गल", "शुक्र", "बुध", "चन्द्र", "सूर्य", "बुध",
        "शुक्र", "मङ्गल", "बृहस्पति", "शनि", "शनि", "बृहस्पति"
    )
    private val houseSigEn = listOf(
        "Self, Vitality & Personality",
        "Wealth, Family & Speech",
        "Courage, Siblings & Effort",
        "Home, Mother & Comforts",
        "Intellect, Children & Purva Punya",
        "Health, Service & Obstacles",
        "Marriage, Partnerships & Trade",
        "Longevity, Transformation & Mystery",
        "Fortune, Dharma & Higher Wisdom",
        "Career, Karma & Public Honor",
        "Gains, Income & Fulfilled Desires",
        "Liberation, Foreign Lands & Expenses"
    )
    private val houseSigHi = listOf(
        "तनु भाव · शरीर, व्यक्तित्व एवं स्वास्थ्य",
        "धन भाव · कुटुम्ब, वाणी एवं संचित धन",
        "सहज भाव · पराक्रम, साहस एवं भाई-बहन",
        "सुख भाव · माता, भूमि, भवन एवं वाहन",
        "पुत्र भाव · विद्या, बुद्धि एवं संतान",
        "रिपु भाव · रोग, ऋण, सेवा एवं शत्रु",
        "कलत्र भाव · विवाह, जीवनसाथी एवं व्यापार",
        "आयु भाव · आयु, गूढ़ ज्ञान एवं परिवर्तन",
        "धर्म भाव · भाग्य, धर्म एवं गुरु कृपा",
        "कर्म भाव · करियर, पद-प्रतिष्ठा एवं कर्म",
        "लाभ भाव · आय, लाभ एवं मनोरथ सिद्धि",
        "व्यय भाव · मोक्ष, विदेश यात्रा एवं व्यय"
    )

    private val nakLords = listOf(
        "Ketu", "Venus", "Sun", "Moon", "Mars", "Rahu", "Jupiter", "Saturn", "Mercury"
    )

    fun compute(input: KundliBirthInput, nowMs: Long = System.currentTimeMillis()): KundliReport {
        val zone = TimeFormatters.safeZoneId(input.location.timezone)
        val ldt = LocalDateTime.of(
            input.year.coerceIn(1900, 2100),
            input.month.coerceIn(1, 12),
            input.day.coerceIn(1, 28),
            input.hour.coerceIn(0, 23),
            input.minute.coerceIn(0, 59)
        )
        val safeDay = input.day.coerceIn(1, ldt.toLocalDate().lengthOfMonth())
        val birthZdt = ZonedDateTime.of(input.year, input.month, safeDay, input.hour, input.minute, input.second.coerceIn(0, 59), 0, zone)
        val birthMs = birthZdt.toInstant().toEpochMilli()
        val t = Time.fromMillisecondsSince1970(birthMs)
        val tPrev = Time.fromMillisecondsSince1970(birthMs - 3_600_000L)

        val ayanDeg = AyanamshaAndSolvers.calculateAyanamsha(t, input.ayanamsha)
        val (tropAsc, lstDeg) = calculateTropicalAscendantAndLst(t, input.location.latitude, input.location.longitude)
        val sidAsc = AyanamshaAndSolvers.normalizeDegrees(tropAsc - ayanDeg)
        val lagnaSign = floor(sidAsc / 30.0).toInt().coerceIn(0, 11)
        val lagnaDegInSign = sidAsc - lagnaSign * 30.0
        val (lagnaNak, lagnaPada) = nakshatraAndPada(sidAsc)
        val d9LagnaSign = KundliVargaAndStrengthEngine.vargaSign(sidAsc, 9)

        val sunLon = AyanamshaAndSolvers.getBodySiderealLongitude("sun", t, input.ayanamsha)

        val planets = specs.map { spec ->
            val lon = AyanamshaAndSolvers.getBodySiderealLongitude(spec.id, t, input.ayanamsha)
            val sign = floor(lon / 30.0).toInt().coerceIn(0, 11)
            val degInSign = lon - sign * 30.0
            val (nakNum, pada) = nakshatraAndPada(lon)
            val houseD1 = ((sign - lagnaSign + 12) % 12) + 1
            val signD9 = KundliVargaAndStrengthEngine.vargaSign(lon, 9)
            val houseD9 = ((signD9 - d9LagnaSign + 12) % 12) + 1

            val isRetro = when (spec.id) {
                "rahu", "ketu" -> true
                "sun", "moon" -> false
                else -> {
                    val lonPrev = AyanamshaAndSolvers.getBodySiderealLongitude(spec.id, tPrev, input.ayanamsha)
                    var diff = lon - lonPrev
                    if (diff > 180.0) diff -= 360.0
                    if (diff < -180.0) diff += 360.0
                    diff < 0.0
                }
            }

            val isCombust = spec.combustDeg?.let { limit ->
                var d = abs(lon - sunLon)
                if (d > 180.0) d = 360.0 - d
                d <= limit
            } ?: false

            val (digEn, digHi, isStrongDignity) = when {
                sign == spec.exaltSign -> Triple("Exalted", "उच्च", true)
                sign == spec.debilSign -> Triple("Debilitated", "नीच", false)
                sign in spec.ownSigns -> Triple("Own Sign", "स्वराशि", true)
                else -> Triple("Normal", "सामान्य", false)
            }

            KundliPlanetEntry(
                id = spec.id,
                symbol = spec.symbol,
                nameEn = spec.nameEn,
                nameHi = spec.nameHi,
                shortEn = spec.shortEn,
                shortHi = spec.shortHi,
                longitude = lon,
                signIndex = sign,
                degreeInSign = degInSign,
                dmsLabel = formatDms(degInSign),
                houseD1 = houseD1,
                signD9 = signD9,
                houseD9 = houseD9,
                nakshatraNumber = nakNum,
                nakshatraLord = nakLords[(nakNum - 1) % 9],
                pada = pada,
                isRetrograde = isRetro,
                isCombust = isCombust,
                dignityEn = digEn,
                dignityHi = digHi,
                isExaltedOrOwn = isStrongDignity
            )
        }

        val housesD1 = (0 until 12).map { idx ->
            val sign = (lagnaSign + idx) % 12
            val houseNum = idx + 1
            KundliHouseEntry(
                houseNumber = houseNum,
                signIndex = sign,
                lordEn = signLordsEn[sign],
                lordHi = signLordsHi[sign],
                planetIds = planets.filter { it.houseD1 == houseNum }.map { it.id },
                significanceEn = houseSigEn[idx],
                significanceHi = houseSigHi[idx]
            )
        }

        val housesD9 = (0 until 12).map { idx ->
            val sign = (d9LagnaSign + idx) % 12
            val houseNum = idx + 1
            KundliHouseEntry(
                houseNumber = houseNum,
                signIndex = sign,
                lordEn = signLordsEn[sign],
                lordHi = signLordsHi[sign],
                planetIds = planets.filter { it.houseD9 == houseNum }.map { it.id },
                significanceEn = houseSigEn[idx],
                significanceHi = houseSigHi[idx]
            )
        }

        val moon = planets.first { it.id == "moon" }
        val sun = planets.first { it.id == "sun" }
        val isMoonWaxing = AyanamshaAndSolvers.normalizeDegrees(moon.longitude - sun.longitude) < 180.0
        val vargas = KundliVargaAndStrengthEngine.computeShodashavarga(sidAsc, planets)
        val dashas = KundliVargaAndStrengthEngine.computeVimshottariDasha(moon.longitude, birthMs, nowMs)
        val (balanceEn, balanceHi) = KundliVargaAndStrengthEngine.computeBirthDashaBalance(moon.longitude)
        val curMaha = dashas.firstOrNull { it.isCurrent } ?: dashas.first()
        val curAntar = curMaha.antardashas.firstOrNull { it.isCurrent }
        val curPrat = curAntar?.pratyantardashas?.firstOrNull { it.isCurrent }
        val currentMahaEn = when {
            curAntar != null && curPrat != null -> "${curMaha.planetNameEn} / ${curAntar.planetNameEn} / ${curPrat.planetNameEn} (until ${curPrat.endDate})"
            curAntar != null -> "${curMaha.planetNameEn} / ${curAntar.planetNameEn} (until ${curAntar.endDate})"
            else -> "${curMaha.planetNameEn} (until ${curMaha.endDate})"
        }
        val currentMahaHi = when {
            curAntar != null && curPrat != null -> "${curMaha.planetNameHi} / ${curAntar.planetNameHi} / ${curPrat.planetNameHi} (${curPrat.endDate} तक)"
            curAntar != null -> "${curMaha.planetNameHi} महादशा / ${curAntar.planetNameHi} अन्तर (${curAntar.endDate} तक)"
            else -> "${curMaha.planetNameHi} महादशा (${curMaha.endDate} तक)"
        }

        val isDayBirth = input.hour in 6..17
        val shadbala = KundliVargaAndStrengthEngine.computeShadbala(planets, sidAsc, isDayBirth)
        val bhavaBala = KundliVargaAndStrengthEngine.computeBhavaBala(sidAsc, housesD1, shadbala, planets, isMoonWaxing)
        val ashtakavarga = KundliVargaAndStrengthEngine.computeAshtakavarga(planets, lagnaSign)
        val kpFull = KundliKpJaiminiEngine.computeKpFull(sidAsc, planets)
        val (jaiminiKarakas, arudhaLagna) = KundliVargaAndStrengthEngine.computeJaimini(planets, lagnaSign)
        val akId = jaiminiKarakas.firstOrNull()?.planetId ?: "sun"
        val jaiminiFull = KundliKpJaiminiEngine.computeJaiminiFull(lagnaSign, d9LagnaSign, planets, akId, birthMs, nowMs)
        val gocharReport = KundliRulesAndGocharEngine.computeGochar(moon.signIndex, lagnaSign, input.ayanamsha, nowMs)
        val doshas = KundliRulesAndGocharEngine.computeDoshas(planets, gocharReport)
        val yogas = KundliRulesAndGocharEngine.evaluateAllCanonicalYogas(planets, housesD1, moon)

        val jd = t.ut + 2451545.0
        val hashSrc = "${birthMs}|${input.location.latitude}|${input.location.longitude}|${input.ayanamsha.apiKey}|$lagnaSign"
        val digest = MessageDigest.getInstance("SHA-256").digest(hashSrc.toByteArray()).joinToString("") { "%02x".format(it) }.take(12).uppercase()
        val audit = KundliCalculationAudit(
            julianDay = round(jd * 100000.0) / 100000.0,
            lstDeg = round(lstDeg * 1000.0) / 1000.0,
            ayanamshaModel = input.ayanamsha.displayLabel,
            ayanamshaDeg = round(ayanDeg * 10000.0) / 10000.0,
            latitude = input.location.latitude,
            longitude = input.location.longitude,
            timezone = input.location.timezone,
            chartHash = digest
        )

        return KundliReport(
            input = input,
            ayanamshaDeg = ayanDeg,
            lagnaLongitude = sidAsc,
            lagnaSignIndex = lagnaSign,
            lagnaDegreeInSign = lagnaDegInSign,
            lagnaDms = formatDms(lagnaDegInSign),
            lagnaNakshatraNumber = lagnaNak,
            lagnaPada = lagnaPada,
            navamshaLagnaSignIndex = d9LagnaSign,
            moonSignIndex = moon.signIndex,
            moonNakshatraNumber = moon.nakshatraNumber,
            moonPada = moon.pada,
            sunSignIndex = sun.signIndex,
            planets = planets,
            housesD1 = housesD1,
            housesD9 = housesD9,
            vargas = vargas,
            vimshottariDashas = dashas,
            currentMahadashaLabelEn = currentMahaEn,
            currentMahadashaLabelHi = currentMahaHi,
            shadbala = shadbala,
            ashtakavarga = ashtakavarga,
            kpTable = kpFull.planetEntries,
            jaiminiKarakas = jaiminiKarakas,
            arudhaLagnaSignIndex = arudhaLagna,
            gocharEffects = gocharReport.transits,
            doshas = doshas,
            yogas = yogas,
            bhavaBala = bhavaBala,
            kpCusps = kpFull.cusps,
            kpRulingPlanets = kpFull.rulingPlanets,
            arudhaPadas = jaiminiFull.arudhaPadas,
            upapadaLagnaSignIndex = jaiminiFull.upapadaLagnaSignIndex,
            karakamsha = jaiminiFull.karakamsha,
            jaiminiRashiDrishti = jaiminiFull.rashiDrishti,
            charaDashas = jaiminiFull.charaDashas,
            sadeSatiPhaseEn = gocharReport.sadeSatiPhaseEn,
            sadeSatiPhaseHi = gocharReport.sadeSatiPhaseHi,
            sadeSatiDescEn = gocharReport.sadeSatiDescEn,
            sadeSatiDescHi = gocharReport.sadeSatiDescHi,
            audit = audit,
            birthBalanceEn = balanceEn,
            birthBalanceHi = balanceHi
        )
    }

    private fun calculateTropicalAscendantAndLst(t: Time, lat: Double, lon: Double): Pair<Double, Double> {
        val d = t.ut
        val centuries = d / 36525.0
        val gmstDeg = AyanamshaAndSolvers.normalizeDegrees(
            280.46061837 + 360.98564736629 * d + 0.000387933 * centuries * centuries -
                (centuries * centuries * centuries) / 38710000.0
        )
        val lstDeg = AyanamshaAndSolvers.normalizeDegrees(gmstDeg + lon)
        val lstRad = lstDeg * PI / 180.0
        val epsDeg = 23.43929111 - ((46.815 + (0.00059 - 0.001813 * centuries) * centuries) * centuries) / 3600.0
        val epsRad = epsDeg * PI / 180.0
        val latRad = lat * PI / 180.0

        val num = cos(lstRad)
        val den = -(sin(lstRad) * cos(epsRad) + tan(latRad) * sin(epsRad))
        var ascRad = atan2(num, den)
        if (ascRad < 0) ascRad += 2.0 * PI
        return (ascRad * 180.0 / PI) to lstDeg
    }

    private fun nakshatraAndPada(lon: Double): Pair<Int, Int> {
        val exact = AyanamshaAndSolvers.normalizeDegrees(lon) / (360.0 / 27.0)
        val idx = floor(exact).toInt().coerceIn(0, 26)
        val pada = floor((exact - idx) * 4.0).toInt().coerceIn(0, 3) + 1
        return (idx + 1) to pada
    }

    private fun formatDms(deg: Double): String {
        val d = floor(deg).toInt()
        val mExact = (deg - d) * 60.0
        val m = floor(mExact).toInt()
        val s = floor((mExact - m) * 60.0).toInt()
        return "%02d°%02d'%02d\"".format(d, m, s)
    }
}
