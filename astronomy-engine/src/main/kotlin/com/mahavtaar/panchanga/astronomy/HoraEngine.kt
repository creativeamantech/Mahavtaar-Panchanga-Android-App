package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.HoraPeriod
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.TattvaPeriod
import kotlin.math.roundToInt

/**
 * Exact 1:1 port of `src/horaEngine.ts`:
 * - 12 unequal day horas (sunrise -> sunset) + 12 unequal night horas (sunset -> next sunrise)
 * - Chaldean planetary sequence starting from weekday ruler
 * - Alternating Swara Nadi per Hora seeded from sunrise Swara
 * - 5 weighted sub-Tattvas per Hora (Akasha 1/15, Vayu 2/15, Agni 3/15, Prithvi 5/15, Jala 4/15)
 */
object HoraEngine {

    private val horaLords = listOf("Sun", "Venus", "Mercury", "Moon", "Saturn", "Jupiter", "Mars")
    private val weekdayStartIndex = intArrayOf(0, 3, 6, 2, 5, 1, 4) // Sun .. Sat

    private data class TattvaDef(
        val name: String,
        val sanskrit: String,
        val element: TattvaElement,
        val ratio: Double
    )

    private val tattvaDefs = listOf(
        TattvaDef("Space", "Akasha", TattvaElement.AKASH, 1.0 / 15.0),
        TattvaDef("Air", "Vayu", TattvaElement.VAYU, 2.0 / 15.0),
        TattvaDef("Fire", "Agni", TattvaElement.TEJAS, 3.0 / 15.0),
        TattvaDef("Earth", "Prithvi", TattvaElement.PRITHVI, 5.0 / 15.0),
        TattvaDef("Water", "Jala", TattvaElement.JALA, 4.0 / 15.0)
    )

    /**
     * Computes exact millisecond boundaries [startTimeMs, endTimeMs) for the 12 unequal Day Horas
     * spanning from sunrise to sunset. Index 0 = 1st Hora, 6 = 7th Hora, 11 = 12th Hora.
     */
    fun computeDayHoraBounds(sunriseMs: Long, sunsetMs: Long): List<Pair<Long, Long>> {
        if (sunsetMs <= sunriseMs) return emptyList()
        val dayHoraDuration = (sunsetMs - sunriseMs) / 12.0
        val bounds = ArrayList<Pair<Long, Long>>(12)
        for (i in 0 until 12) {
            val startTimeMs = if (i == 0) sunriseMs else bounds[i - 1].second
            val endTimeMs = if (i == 11) sunsetMs else (startTimeMs + dayHoraDuration).toLong()
            bounds.add(startTimeMs to endTimeMs)
        }
        return bounds
    }

    fun computeDailyHoras(
        dateDisplay: String,
        sunriseMs: Long,
        sunsetMs: Long,
        nextSunriseMs: Long,
        weekday: Int,
        tithiNumber: Int,
        timeZoneId: String,
        nowMs: Long
    ): DailyHoras {
        require(sunriseMs < sunsetMs && sunsetMs < nextSunriseMs) {
            "Invalid chronological ordering of sunrise and sunset for Hora calculation."
        }
        val dayBounds = computeDayHoraBounds(sunriseMs, sunsetMs)
        val dayHoraDuration = (sunsetMs - sunriseMs) / 12.0
        val nightHoraDuration = (nextSunriseMs - sunsetMs) / 12.0
        val initialNadi = SwaraYogaEngine.getRuleForTithi(tithiNumber).sunriseSwara
        val startIndex = weekdayStartIndex[((weekday % 7) + 7) % 7]

        val horas = mutableListOf<HoraPeriod>()
        var activeHora: HoraPeriod? = null
        var activeTattva: TattvaPeriod? = null

        for (i in 0 until 24) {
            val isDay = i < 12
            val duration = if (isDay) dayHoraDuration else nightHoraDuration
            val startTimeMs = if (isDay) dayBounds[i].first else horas[i - 1].endTimeMs
            val endTimeMs = when {
                isDay -> dayBounds[i].second
                i == 23 -> nextSunriseMs
                else -> (startTimeMs + duration).toLong()
            }

            val ruler = horaLords[(startIndex + i) % 7]
            val nadi = if (i % 2 == 0) initialNadi else SwaraYogaEngine.oppositeNadi(initialNadi)

            val tattvas = mutableListOf<TattvaPeriod>()
            var tattvaStartMs = startTimeMs.toDouble()

            for (t in tattvaDefs.indices) {
                val def = tattvaDefs[t]
                val tDuration = duration * def.ratio
                val tEndMs = if (t == tattvaDefs.lastIndex) endTimeMs else (tattvaStartMs + tDuration).toLong()
                val tStartLong = tattvaStartMs.toLong()
                val tp = TattvaPeriod(
                    name = def.name,
                    sanskrit = def.sanskrit,
                    element = def.element,
                    durationMs = (tEndMs - tStartLong).coerceAtLeast(0L),
                    durationMins = ((tEndMs - tStartLong) / 60_000.0).roundToInt(),
                    startTime = TimeFormatters.format12HourHMS(tStartLong, timeZoneId),
                    endTime = TimeFormatters.format12HourHMS(tEndMs, timeZoneId),
                    startTimeMs = tStartLong,
                    endTimeMs = tEndMs
                )
                tattvas.add(tp)
                if (nowMs in tStartLong until tEndMs) {
                    activeTattva = tp
                }
                tattvaStartMs = tEndMs.toDouble()
            }

            val hora = HoraPeriod(
                index = i + 1,
                isDay = isDay,
                startTime = TimeFormatters.format12HourHMS(startTimeMs, timeZoneId),
                endTime = TimeFormatters.format12HourHMS(endTimeMs, timeZoneId),
                startTimeMs = startTimeMs,
                endTimeMs = endTimeMs,
                durationMs = (endTimeMs - startTimeMs).coerceAtLeast(0L),
                ruler = ruler,
                nadi = nadi,
                tattvas = tattvas
            )
            horas.add(hora)
            if (nowMs in startTimeMs until endTimeMs) {
                activeHora = hora
            }
        }

        return DailyHoras(
            date = dateDisplay,
            timezone = timeZoneId,
            horas = horas,
            activeHora = activeHora,
            activeTattva = activeTattva
        )
    }

    /**
     * Resolves current Hora cleanly across pre-sunrise rollover (uses previous day's night Hora).
     */
    fun resolveForSnapshot(snapshot: PanchangaSnapshot, nowMs: Long): DailyHoras {
        val primaryTithi = snapshot.tithi.firstOrNull()?.number ?: 1
        return if (nowMs < snapshot.sunriseMs && nowMs >= snapshot.previousSunriseMs) {
            val prevWeekday = (snapshot.weekday - 1 + 7) % 7
            computeDailyHoras(
                dateDisplay = snapshot.dateDisplay,
                sunriseMs = snapshot.previousSunriseMs,
                sunsetMs = snapshot.previousSunsetMs,
                nextSunriseMs = snapshot.sunriseMs,
                weekday = prevWeekday,
                tithiNumber = primaryTithi,
                timeZoneId = snapshot.timezone,
                nowMs = nowMs
            )
        } else {
            computeDailyHoras(
                dateDisplay = snapshot.dateDisplay,
                sunriseMs = snapshot.sunriseMs,
                sunsetMs = snapshot.sunsetMs,
                nextSunriseMs = snapshot.nextSunriseMs,
                weekday = snapshot.weekday,
                tithiNumber = primaryTithi,
                timeZoneId = snapshot.timezone,
                nowMs = nowMs
            )
        }
    }
}
