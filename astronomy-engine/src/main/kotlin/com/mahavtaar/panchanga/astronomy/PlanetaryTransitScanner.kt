package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.PlanetPosition
import com.mahavtaar.panchanga.domain.model.PlanetTransitionEvent
import com.mahavtaar.panchanga.domain.model.TimingInterval
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.geoVector
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Exact Navagraha position & 60-day planetary Rasi/Nakshatra transit scanner
 * ported from `src/lib/panchangaEngine.server.ts`.
 */
object PlanetaryTransitScanner {

    private data class GrahaSpec(
        val id: String,
        val name: String,
        val sanskritName: String,
        val symbol: String,
        val body: Body?,
        val stepHours: Int
    )

    private val grahas = listOf(
        GrahaSpec("sun", "Sun", "Sūrya", "☉", Body.Sun, 6),
        GrahaSpec("moon", "Moon", "Candra", "☽", Body.Moon, 2),
        GrahaSpec("mars", "Mars", "Maṅgala", "♂", Body.Mars, 6),
        GrahaSpec("mercury", "Mercury", "Budha", "☿", Body.Mercury, 6),
        GrahaSpec("jupiter", "Jupiter", "Guru", "♃", Body.Jupiter, 6),
        GrahaSpec("venus", "Venus", "Śukra", "♀", Body.Venus, 6),
        GrahaSpec("saturn", "Saturn", "Śani", "♄", Body.Saturn, 6),
        GrahaSpec("rahu", "Rahu", "Rāhu", "☊", null, 6),
        GrahaSpec("ketu", "Ketu", "Ketu", "☋", null, 6)
    )

    fun computeNavagrahaPositions(
        tSunrise: Time,
        sunTropLon: Double,
        moonTropLon: Double,
        ayanamshaDeg: Double
    ): List<PlanetPosition> {
        val sunSidLon = AyanamshaAndSolvers.getSiderealLon(sunTropLon, ayanamshaDeg)
        val sunriseMs = tSunrise.toMillisecondsSince1970()
        val tNextHour = Time.fromMillisecondsSince1970(sunriseMs + 3_600_000L)
        val julianCenturies = tSunrise.ut / 36525.0
        val rahuTrop = AyanamshaAndSolvers.normalizeDegrees(
            125.04452 - 1934.136261 * julianCenturies + 0.0020708 * julianCenturies * julianCenturies
        )
        val ketuTrop = (rahuTrop + 180.0) % 360.0

        return grahas.map { spec ->
            val tropLon: Double
            val isRetrograde: Boolean

            when (spec.id) {
                "sun" -> {
                    tropLon = sunTropLon
                    isRetrograde = false
                }
                "moon" -> {
                    tropLon = moonTropLon
                    isRetrograde = false
                }
                "rahu" -> {
                    tropLon = rahuTrop
                    isRetrograde = true
                }
                "ketu" -> {
                    tropLon = ketuTrop
                    isRetrograde = true
                }
                else -> {
                    val body = spec.body!!
                    tropLon = equatorialToEcliptic(geoVector(body, tSunrise, Aberration.Corrected)).elon
                    val nextElon = equatorialToEcliptic(geoVector(body, tNextHour, Aberration.Corrected)).elon
                    var diffSpeed = (nextElon - tropLon) % 360.0
                    if (diffSpeed > 180.0) diffSpeed -= 360.0
                    if (diffSpeed < -180.0) diffSpeed += 360.0
                    isRetrograde = diffSpeed < 0.0
                }
            }

            val sidLon = AyanamshaAndSolvers.getSiderealLon(tropLon, ayanamshaDeg)
            val rasiIdx = floor(sidLon / 30.0).toInt().coerceIn(0, 11)
            val degInRasi = sidLon % 30.0
            val nakIdx = floor(sidLon / (360.0 / 27.0)).toInt().coerceIn(0, 26)
            val pada = floor((sidLon % (360.0 / 27.0)) / (360.0 / 108.0)).toInt().coerceIn(0, 3) + 1
            val progressPercent = ((degInRasi / 30.0) * 100.0).roundToInt().coerceIn(0, 100)

            var isCombust = false
            var combustDist: Double? = null
            if (spec.id in setOf("mercury", "venus", "mars", "jupiter", "saturn")) {
                val angDiff = abs((((sidLon - sunSidLon + 540.0) % 360.0 + 360.0) % 360.0) - 180.0)
                combustDist = (angDiff * 10.0).roundToInt() / 10.0
                val limit = when (spec.id) {
                    "mercury" -> if (isRetrograde) 12.0 else 14.0
                    "venus" -> if (isRetrograde) 8.0 else 10.0
                    "mars" -> 17.0
                    "jupiter" -> 11.0
                    "saturn" -> 15.0
                    else -> 15.0
                }
                isCombust = angDiff <= limit
            }

            PlanetPosition(
                id = spec.id,
                name = spec.name,
                sanskritName = spec.sanskritName,
                symbol = spec.symbol,
                tropicalLongitude = tropLon,
                siderealLongitude = sidLon,
                rasi = SanskritNamesCatalog.zodiac[rasiIdx] ?: "meṣa",
                rasiNumber = rasiIdx + 1,
                degreesInRasi = TimeFormatters.formatDegreesDMS(degInRasi),
                degreesInRasiNum = degInRasi,
                progressPercent = progressPercent,
                nakshatra = SanskritNamesCatalog.nakshatras[nakIdx + 1] ?: "Aśvinī",
                nakshatraNumber = nakIdx + 1,
                pada = pada,
                isRetrograde = isRetrograde,
                isCombust = isCombust,
                combustDistanceDeg = combustDist
            )
        }
    }

    fun scanUpcomingTransits(
        location: CityLocation,
        tSunrise: Time,
        tNextSunrise: Time,
        ayanamshaKey: CoordinateSelection,
        daysAhead: Int = 30
    ): List<PlanetTransitionEvent> {
        val timeZone = location.timezone
        val baseMs = tSunrise.toMillisecondsSince1970()
        val nextSunriseMs = tNextSunrise.toMillisecondsSince1970()
        val endMs = baseMs + daysAhead.toLong() * 86_400_000L
        val events = mutableListOf<PlanetTransitionEvent>()

        val nakSpan = 360.0 / 27.0
        for (g in grahas) {
            var curMs = baseMs
            var prevMs = curMs
            val initLon = AyanamshaAndSolvers.getBodySiderealLongitude(g.id, Time.fromMillisecondsSince1970(curMs), ayanamshaKey)
            var prevRasi = floor(initLon / 30.0).toInt().coerceIn(0, 11)
            var prevNak = floor(initLon / nakSpan).toInt().coerceIn(0, 26)
            val stepMs = g.stepHours.toLong() * 3_600_000L

            while (curMs < endMs) {
                curMs = min(curMs + stepMs, endMs)
                val testLon = AyanamshaAndSolvers.getBodySiderealLongitude(
                    g.id,
                    Time.fromMillisecondsSince1970(curMs),
                    ayanamshaKey
                )
                val testRasi = floor(testLon / 30.0).toInt().coerceIn(0, 11)
                val testNak = floor(testLon / nakSpan).toInt().coerceIn(0, 26)

                if (testRasi != prevRasi) {
                    var low = prevMs
                    var high = curMs
                    while (high - low > 30_000L) {
                        val mid = (low + high) / 2L
                        val midLon = AyanamshaAndSolvers.getBodySiderealLongitude(
                            g.id,
                            Time.fromMillisecondsSince1970(mid),
                            ayanamshaKey
                        )
                        if (floor(midLon / 30.0).toInt().coerceIn(0, 11) == prevRasi) {
                            low = mid
                        } else {
                            high = mid
                        }
                    }
                    val toRasi = floor(
                        AyanamshaAndSolvers.getBodySiderealLongitude(
                            g.id,
                            Time.fromMillisecondsSince1970(high),
                            ayanamshaKey
                        ) / 30.0
                    ).toInt().coerceIn(0, 11)

                    var specialName: String? = null
                    var punyaKala: TimingInterval? = null
                    var mahaPunyaKala: TimingInterval? = null
                    if (g.id == "sun") {
                        specialName = SanskritNamesCatalog.sankrantiNames[toRasi]
                        val pStart = high - (6.4 * 3_600_000.0).toLong()
                        val pEnd = high + (6.4 * 3_600_000.0).toLong()
                        punyaKala = TimingInterval(
                            start = TimeFormatters.format24HourHMS(pStart, timeZone),
                            end = TimeFormatters.format24HourHMS(pEnd, timeZone),
                            startTimeMs = pStart,
                            endTimeMs = pEnd,
                            name = "$specialName Puṇyakāla",
                            isAuspicious = true
                        )
                        val mpStart = high - (1.6 * 3_600_000.0).toLong()
                        val mpEnd = high + (1.6 * 3_600_000.0).toLong()
                        mahaPunyaKala = TimingInterval(
                            start = TimeFormatters.format24HourHMS(mpStart, timeZone),
                            end = TimeFormatters.format24HourHMS(mpEnd, timeZone),
                            startTimeMs = mpStart,
                            endTimeMs = mpEnd,
                            name = "$specialName Mahāpuṇyakāla",
                            isAuspicious = true
                        )
                    }

                    val fromEn = SanskritNamesCatalog.zodiacEnglish[prevRasi]
                    val toEn = SanskritNamesCatalog.zodiacEnglish[toRasi]
                    val fromSa = SanskritNamesCatalog.zodiacDevanagari[prevRasi]
                    val toSa = SanskritNamesCatalog.zodiacDevanagari[toRasi]
                    val isToday = high in baseMs..nextSunriseMs

                    events.add(
                        PlanetTransitionEvent(
                            id = "${g.id}-rasi-$high",
                            planetId = g.id,
                            planetName = g.name,
                            sanskritName = g.sanskritName,
                            symbol = g.symbol,
                            type = "rasi",
                            timestampMs = high,
                            dateStr = TimeFormatters.formatDateGB(high, timeZone),
                            timeStr = TimeFormatters.format24HourHMS(high, timeZone),
                            dayOfWeek = TimeFormatters.formatDayOfWeek(high, timeZone),
                            relativeText = formatRelative(high, baseMs),
                            isToday = isToday,
                            fromValue = fromEn,
                            toValue = toEn,
                            fromSanskrit = fromSa,
                            toSanskrit = toSa,
                            specialName = specialName,
                            punyaKala = punyaKala,
                            mahaPunyaKala = mahaPunyaKala,
                            description = if (specialName != null) {
                                "Sūrya enters $toEn marking sacred $specialName."
                            } else {
                                "${g.sanskritName} enters $toEn ($toSa) from $fromEn."
                            }
                        )
                    )
                    prevRasi = testRasi
                }

                if (testNak != prevNak && (g.id != "moon" || (curMs - baseMs) <= 10L * 86_400_000L)) {
                    var low = prevMs
                    var high = curMs
                    while (high - low > 30_000L) {
                        val mid = (low + high) / 2L
                        val midLon = AyanamshaAndSolvers.getBodySiderealLongitude(
                            g.id,
                            Time.fromMillisecondsSince1970(mid),
                            ayanamshaKey
                        )
                        if (floor(midLon / nakSpan).toInt().coerceIn(0, 26) == prevNak) {
                            low = mid
                        } else {
                            high = mid
                        }
                    }
                    val toNak = floor(
                        AyanamshaAndSolvers.getBodySiderealLongitude(
                            g.id,
                            Time.fromMillisecondsSince1970(high),
                            ayanamshaKey
                        ) / nakSpan
                    ).toInt().coerceIn(0, 26)
                    val fromNakName = SanskritNamesCatalog.nakshatras[prevNak + 1] ?: "Aśvinī"
                    val toNakName = SanskritNamesCatalog.nakshatras[toNak + 1] ?: "Aśvinī"
                    val isToday = high in baseMs..nextSunriseMs
                    events.add(
                        PlanetTransitionEvent(
                            id = "${g.id}-nak-$high",
                            planetId = g.id,
                            planetName = g.name,
                            sanskritName = g.sanskritName,
                            symbol = g.symbol,
                            type = "nakshatra",
                            timestampMs = high,
                            dateStr = TimeFormatters.formatDateGB(high, timeZone),
                            timeStr = TimeFormatters.format24HourHMS(high, timeZone),
                            dayOfWeek = TimeFormatters.formatDayOfWeek(high, timeZone),
                            relativeText = formatRelative(high, baseMs),
                            isToday = isToday,
                            fromValue = fromNakName,
                            toValue = toNakName,
                            fromSanskrit = fromNakName,
                            toSanskrit = toNakName,
                            description = "${g.sanskritName} enters $toNakName Nakṣatra from $fromNakName."
                        )
                    )
                }
                prevNak = testNak
                prevMs = curMs
            }
        }

        return events.sortedBy { it.timestampMs }
    }

    private fun formatRelative(targetMs: Long, baseMs: Long): String {
        val diffMs = targetMs - baseMs
        val isPast = diffMs < 0
        val absMs = abs(diffMs)
        val totalMinutes = absMs / 60_000L
        val totalHours = totalMinutes / 60L
        val days = totalHours / 24L
        val remHours = totalHours % 24L
        val remMins = totalMinutes % 60L
        return when {
            days == 0L && totalHours == 0L -> if (isPast) "${remMins}m ago" else "in ${remMins}m"
            days == 0L -> if (isPast) "${totalHours}h ${remMins}m ago" else "in ${totalHours}h ${remMins}m"
            days == 1L -> if (isPast) "yesterday" else "in 1d ${remHours}h"
            else -> if (isPast) "${days}d ago" else "in ${days}d ${remHours}h"
        }
    }
}
