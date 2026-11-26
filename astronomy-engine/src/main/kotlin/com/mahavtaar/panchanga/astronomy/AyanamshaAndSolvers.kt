package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.AngaSegment
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.sunPosition
import kotlin.math.abs
import kotlin.math.floor

/**
 * Core astronomical helpers, Ayanamsha formulas, and angular root-finding solver
 * preserving exact numerical parity with `src/lib/panchangaEngine.server.ts`.
 */
object AyanamshaAndSolvers {

    fun calculateAyanamsha(t: Time, key: CoordinateSelection): Double {
        if (key == CoordinateSelection.TROPICAL) return 0.0
        val julianCenturies = t.ut / 36525.0
        val lahiri = 23.857092 + 1.3969713 * julianCenturies + 0.0003086 * julianCenturies * julianCenturies
        return when (key) {
            CoordinateSelection.CITRA -> lahiri
            CoordinateSelection.REVATI -> lahiri - 0.25
            CoordinateSelection.ROHINI -> lahiri + 1.25
            CoordinateSelection.PUSHYA -> lahiri - 0.5
            CoordinateSelection.MULA -> lahiri + 0.75
            CoordinateSelection.KRISHNAMURTI -> lahiri - 0.0980556
            CoordinateSelection.RAMAN -> lahiri - 1.4666667
            CoordinateSelection.TROPICAL -> 0.0
        }
    }

    fun normalizeDegrees(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

    fun getTropicalSunLon(t: Time): Double = sunPosition(t).elon

    fun getTropicalMoonLon(t: Time): Double {
        val moonGeo = geoVector(Body.Moon, t, Aberration.Corrected)
        return equatorialToEcliptic(moonGeo).elon
    }

    fun getSiderealLon(tropicalLon: Double, ayanamsha: Double): Double =
        normalizeDegrees(tropicalLon - ayanamsha)

    fun getBodySiderealLongitude(
        bodyId: String,
        t: Time,
        ayanamshaKey: CoordinateSelection
    ): Double {
        val tropLon = when (bodyId) {
            "sun" -> sunPosition(t).elon
            "moon" -> equatorialToEcliptic(geoVector(Body.Moon, t, Aberration.Corrected)).elon
            "mars" -> equatorialToEcliptic(geoVector(Body.Mars, t, Aberration.Corrected)).elon
            "mercury" -> equatorialToEcliptic(geoVector(Body.Mercury, t, Aberration.Corrected)).elon
            "jupiter" -> equatorialToEcliptic(geoVector(Body.Jupiter, t, Aberration.Corrected)).elon
            "venus" -> equatorialToEcliptic(geoVector(Body.Venus, t, Aberration.Corrected)).elon
            "saturn" -> equatorialToEcliptic(geoVector(Body.Saturn, t, Aberration.Corrected)).elon
            "rahu", "ketu" -> {
                val julianCenturies = t.ut / 36525.0
                val rahu = normalizeDegrees(
                    125.04452 - 1934.136261 * julianCenturies + 0.0020708 * julianCenturies * julianCenturies
                )
                if (bodyId == "rahu") rahu else (rahu + 180.0) % 360.0
            }
            else -> 0.0
        }
        val ayanamsha = calculateAyanamsha(t, ayanamshaKey)
        return getSiderealLon(tropLon, ayanamsha)
    }

    fun getTithiFraction(t: Time): Double {
        val diff = normalizeDegrees(getTropicalMoonLon(t) - getTropicalSunLon(t))
        return diff / 12.0
    }

    fun getNakshatraFraction(t: Time, ayanamshaKey: CoordinateSelection): Double {
        val ayanamsha = calculateAyanamsha(t, ayanamshaKey)
        val moonLon = getSiderealLon(getTropicalMoonLon(t), ayanamsha)
        return moonLon / (360.0 / 27.0)
    }

    fun getYogaFraction(t: Time, ayanamshaKey: CoordinateSelection): Double {
        val ayanamsha = calculateAyanamsha(t, ayanamshaKey)
        val sunLon = getSiderealLon(getTropicalSunLon(t), ayanamsha)
        val moonLon = getSiderealLon(getTropicalMoonLon(t), ayanamsha)
        val sum = (sunLon + moonLon) % 360.0
        return sum / (360.0 / 27.0)
    }

    fun getKaranaFraction(t: Time): Double {
        val diff = normalizeDegrees(getTropicalMoonLon(t) - getTropicalSunLon(t))
        return diff / 6.0
    }

    /**
     * Exact port of `findBoundaryCrossing` from `src/lib/panchangaEngine.server.ts`:
     * 30-minute step scan followed by 30-iteration bisection down to < 1 second precision.
     */
    fun findBoundaryCrossing(
        fn: (Time) -> Double,
        target: Double,
        tMin: Time,
        tMax: Time,
        maxIterations: Int = 30
    ): Time? {
        val period = 360.0
        val t1 = tMin.toMillisecondsSince1970()
        val t2 = tMax.toMillisecondsSince1970()
        val stepMs = 30L * 60L * 1000L

        fun wrapDiff(value: Double): Double {
            var diff = (value - target) % period
            if (diff > period / 2.0) diff -= period
            if (diff < -period / 2.0) diff += period
            return diff
        }

        var bracketA = t1
        var bracketB = t2
        var foundBracket = false

        var prevT = t1
        var prevDiff = wrapDiff(fn(Time.fromMillisecondsSince1970(prevT)))

        var curT = t1 + stepMs
        while (curT <= t2 + stepMs) {
            val curVal = fn(Time.fromMillisecondsSince1970(curT))
            val curDiff = wrapDiff(curVal)
            if (prevDiff * curDiff <= 0.0 && abs(curDiff - prevDiff) < period / 2.0) {
                bracketA = prevT
                bracketB = curT
                foundBracket = true
                break
            }
            prevT = curT
            prevDiff = curDiff
            curT += stepMs
        }

        if (!foundBracket) return null

        var low = bracketA.toDouble()
        var high = bracketB.toDouble()

        for (i in 0 until maxIterations) {
            val mid = (low + high) / 2.0
            val midTime = Time.fromMillisecondsSince1970(mid.toLong())
            val midDiff = wrapDiff(fn(midTime))

            if (abs(midDiff) < 1e-6 || abs(high - low) < 1000.0) {
                return midTime
            }

            val lowDiff = wrapDiff(fn(Time.fromMillisecondsSince1970(low.toLong())))
            if (lowDiff * midDiff <= 0.0) {
                high = mid
            } else {
                low = mid
            }
        }

        return Time.fromMillisecondsSince1970(((low + high) / 2.0).toLong())
    }

    /**
     * Exact port of `findSegments` from `src/lib/panchangaEngine.server.ts`.
     */
    fun findSegments(
        fn: (Time) -> Double,
        namesMap: Map<Int, String>,
        tSunrise: Time,
        tNextSunrise: Time,
        localMidnightMs: Long,
        totalItems: Int,
        timeZoneId: String
    ): List<AngaSegment> {
        val vSunrise = fn(tSunrise)
        val currentNum = (floor(vSunrise).toInt() % totalItems) + 1
        val segments = mutableListOf<AngaSegment>()
        val name = namesMap[currentNum] ?: "Item $currentNum"
        val target = floor(vSunrise) + 1.0

        val crossing = findBoundaryCrossing(fn, target, tSunrise, tNextSunrise)

        val sunriseMs = tSunrise.toMillisecondsSince1970()
        val nextSunriseMs = tNextSunrise.toMillisecondsSince1970()

        val tSearchStart = Time.fromMillisecondsSince1970(sunriseMs - 36L * 3_600_000L)
        val tSearchEnd = Time.fromMillisecondsSince1970(sunriseMs + 12L * 3_600_000L)
        val startCrossing = findBoundaryCrossing(fn, floor(vSunrise), tSearchStart, tSearchEnd)

        val startTimeMs = startCrossing?.toMillisecondsSince1970()
        val starts = startTimeMs?.let { TimeFormatters.formatDateTimeWithSeconds(it, timeZoneId) }

        if (crossing != null && crossing.toMillisecondsSince1970() <= nextSunriseMs) {
            val endTimeMs = crossing.toMillisecondsSince1970()
            val localHours = (endTimeMs - localMidnightMs) / 3_600_000.0
            segments.add(
                AngaSegment(
                    number = currentNum,
                    name = name,
                    starts = starts,
                    ends = TimeFormatters.formatHoursHMS(localHours),
                    startTimeMs = startTimeMs,
                    endTimeMs = endTimeMs
                )
            )

            val nextNum = (currentNum % totalItems) + 1
            val nextName = namesMap[nextNum] ?: "Item $nextNum"
            val target2 = target + 1.0
            val crossing2 = findBoundaryCrossing(fn, target2, crossing, tNextSunrise)
            val nextStarts = TimeFormatters.formatDateTimeWithSeconds(endTimeMs, timeZoneId)

            if (crossing2 != null && crossing2.toMillisecondsSince1970() <= nextSunriseMs) {
                val end2Ms = crossing2.toMillisecondsSince1970()
                val localHours2 = (end2Ms - localMidnightMs) / 3_600_000.0
                segments.add(
                    AngaSegment(
                        number = nextNum,
                        name = nextName,
                        starts = nextStarts,
                        ends = TimeFormatters.formatHoursHMS(localHours2),
                        startTimeMs = endTimeMs,
                        endTimeMs = end2Ms
                    )
                )
            } else {
                segments.add(
                    AngaSegment(
                        number = nextNum,
                        name = nextName,
                        starts = nextStarts,
                        ends = null,
                        startTimeMs = endTimeMs,
                        endTimeMs = null
                    )
                )
            }
        } else {
            segments.add(
                AngaSegment(
                    number = currentNum,
                    name = name,
                    starts = starts,
                    ends = null,
                    startTimeMs = startTimeMs,
                    endTimeMs = null
                )
            )
        }
        return segments
    }
}
