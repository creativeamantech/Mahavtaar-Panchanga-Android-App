package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.MonthlyCalendarDay
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.TimingInterval
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.searchAltitude
import io.github.cosinekitty.astronomy.searchMoonPhase
import io.github.cosinekitty.astronomy.searchRiseSet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.floor
import kotlin.math.round

/**
 * Pure Kotlin Drik Ganita Panchanga Engine preserving exact scientific parity with
 * `src/lib/panchangaEngine.server.ts` using `com.github.cosinekitty:astronomy:2.1.19`.
 */
class DrikPanchangaEngine {

    private val varjyamStartGhatis = intArrayOf(
        0, 50, 24, 30, 40, 14, 21, 30, 20, 32, 30, 20, 18, 21, 20, 14, 14, 10, 14, 56, 24, 20, 10, 10, 18, 16, 24, 30
    )

    private val rahuOffsets = doubleArrayOf(0.875, 0.125, 0.75, 0.5, 0.625, 0.375, 0.25)
    private val gulikaOffsets = doubleArrayOf(0.75, 0.625, 0.5, 0.375, 0.25, 0.125, 0.0)
    private val yamaOffsets = doubleArrayOf(0.5, 0.375, 0.25, 0.125, 0.0, 0.75, 0.625)

    private val durmuhurtaOffsets = arrayOf(
        doubleArrayOf(10.4, 0.0),
        doubleArrayOf(6.4, 8.8),
        doubleArrayOf(2.4, 4.8),
        doubleArrayOf(5.6, 0.0),
        doubleArrayOf(4.0, 8.8),
        doubleArrayOf(2.4, 6.4),
        doubleArrayOf(1.6, 0.0)
    )

    private val choghadiyaTypes = listOf("Udvega", "Chara", "Labha", "Amrita", "Kala", "Shubha", "Roga")
    private val choghadiyaAuspicious = setOf("Amrita", "Shubha", "Labha", "Chara")
    private val dayChoghadiyaStarts = intArrayOf(0, 3, 6, 2, 5, 1, 4)
    private val nightChoghadiyaStarts = intArrayOf(5, 1, 6, 4, 2, 0, 3)

    /**
     * Resolves timezone offset hours at local noon (exact match to TypeScript `getTimezoneOffsetHours`).
     */
    private fun getTimezoneOffsetHours(timeZoneId: String, date: LocalDate): Double {
        timeZoneId.trim().toDoubleOrNull()?.let { numeric ->
            if (numeric in -14.0..14.0) return numeric
        }
        val zone = TimeFormatters.safeZoneId(timeZoneId)
        val noonUtc: Instant = date.atTime(12, 0).toInstant(ZoneOffset.UTC)
        return zone.rules.getOffset(noonUtc).totalSeconds / 3600.0
    }

    fun computePanchanga(
        location: CityLocation,
        date: LocalDate,
        monthSystem: MonthSystem = MonthSystem.AMANTA,
        coordinateSelection: CoordinateSelection = CoordinateSelection.CITRA,
        includeExtendedTransits: Boolean = true
    ): PanchangaSnapshot {
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth

        val tzHours = getTimezoneOffsetHours(location.timezone, date)
        val utcMidnightMs = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val localMidnightMs = utcMidnightMs - (tzHours * 3_600_000.0).toLong()
        val localMidnightTime = Time.fromMillisecondsSince1970(localMidnightMs)

        val observer = Observer(location.latitude, location.longitude, 0.0)

        // Exact SearchAltitude(Body.Sun, observer, Direction.Rise/Set, start, 1.0, 0.0) matching web engine
        val tSunrise = runCatching {
            searchAltitude(Body.Sun, observer, Direction.Rise, localMidnightTime, 1.0, 0.0)
        }.getOrNull() ?: Time.fromMillisecondsSince1970(localMidnightMs + 6L * 3_600_000L)

        val tPrevSunrise = runCatching {
            searchAltitude(
                Body.Sun,
                observer,
                Direction.Rise,
                Time.fromMillisecondsSince1970(localMidnightMs - 24L * 3_600_000L),
                1.0,
                0.0
            )
        }.getOrNull() ?: Time.fromMillisecondsSince1970(localMidnightMs - 18L * 3_600_000L)

        val tPrevSunset = runCatching {
            searchAltitude(Body.Sun, observer, Direction.Set, tPrevSunrise, 1.0, 0.0)
        }.getOrNull() ?: Time.fromMillisecondsSince1970(tPrevSunrise.toMillisecondsSince1970() + 12L * 3_600_000L)

        val tSunset = runCatching {
            searchAltitude(Body.Sun, observer, Direction.Set, tSunrise, 1.0, 0.0)
        }.getOrNull() ?: Time.fromMillisecondsSince1970(tSunrise.toMillisecondsSince1970() + 12L * 3_600_000L)

        val tNextSunrise = runCatching {
            searchAltitude(
                Body.Sun,
                observer,
                Direction.Rise,
                Time.fromMillisecondsSince1970(tSunrise.toMillisecondsSince1970() + 12L * 3_600_000L),
                1.0,
                0.0
            )
        }.getOrNull() ?: Time.fromMillisecondsSince1970(tSunrise.toMillisecondsSince1970() + 24L * 3_600_000L)

        val sunriseMs = tSunrise.toMillisecondsSince1970()
        val sunsetMs = tSunset.toMillisecondsSince1970()
        val nextSunriseMs = tNextSunrise.toMillisecondsSince1970()
        val prevSunriseMs = tPrevSunrise.toMillisecondsSince1970()
        val prevSunsetMs = tPrevSunset.toMillisecondsSince1970()

        val sunriseLocalHours = (sunriseMs - localMidnightMs) / 3_600_000.0
        val sunsetLocalHours = (sunsetMs - localMidnightMs) / 3_600_000.0
        val nextSunriseLocalHours = (nextSunriseMs - localMidnightMs) / 3_600_000.0
        val dayDurationHours = sunsetLocalHours - sunriseLocalHours
        val nightDurationHours = nextSunriseLocalHours - sunsetLocalHours
        val solarNoonHours = (sunriseLocalHours + sunsetLocalHours) / 2.0

        val sunriseStr = TimeFormatters.formatHoursHMS(sunriseLocalHours)
        val sunsetStr = TimeFormatters.formatHoursHMS(sunsetLocalHours)
        val solarNoonStr = TimeFormatters.formatHoursHMS(solarNoonHours)

        // Moonrise & Moonset via SearchRiseSet matching web engine
        var moonriseStr: String? = null
        var moonriseMs: Long? = null
        var moonriseStatus = "none_today"
        runCatching {
            val mr = searchRiseSet(Body.Moon, observer, Direction.Rise, localMidnightTime, 1.0)
            if (mr != null) {
                val mrMs = mr.toMillisecondsSince1970()
                if (mrMs >= localMidnightMs && mrMs < localMidnightMs + 24L * 3_600_000L) {
                    moonriseMs = mrMs
                    moonriseStr = TimeFormatters.formatHoursHMS((mrMs - localMidnightMs) / 3_600_000.0)
                    moonriseStatus = "ok"
                }
            }
        }.onFailure { moonriseStatus = "unavailable" }

        var moonsetStr: String? = null
        var moonsetMs: Long? = null
        var moonsetStatus = "none_today"
        runCatching {
            val ms = searchRiseSet(Body.Moon, observer, Direction.Set, localMidnightTime, 1.0)
            if (ms != null) {
                val msMs = ms.toMillisecondsSince1970()
                if (msMs >= localMidnightMs && msMs < localMidnightMs + 24L * 3_600_000L) {
                    moonsetMs = msMs
                    moonsetStr = TimeFormatters.formatHoursHMS((msMs - localMidnightMs) / 3_600_000.0)
                    moonsetStatus = "ok"
                }
            }
        }.onFailure { moonsetStatus = "unavailable" }

        // Weekday (0 = Sunday .. 6 = Saturday)
        val civilWeekday = date.dayOfWeek.value % 7
        val vaaraName = SanskritNamesCatalog.varas[civilWeekday] ?: "Ravivāra"

        val ayanamshaDeg = AyanamshaAndSolvers.calculateAyanamsha(tSunrise, coordinateSelection)

        // Five Angas segments
        val tithiSegments = AyanamshaAndSolvers.findSegments(
            fn = AyanamshaAndSolvers::getTithiFraction,
            namesMap = SanskritNamesCatalog.tithis,
            tSunrise = tSunrise,
            tNextSunrise = tNextSunrise,
            localMidnightMs = localMidnightMs,
            totalItems = 30,
            timeZoneId = location.timezone
        ).map { seg ->
            seg.copy(swaraInfo = SwaraYogaEngine.computeTithiSwaraEvents(seg, location.timezone))
        }

        val nakshatraSegments = AyanamshaAndSolvers.findSegments(
            fn = { t -> AyanamshaAndSolvers.getNakshatraFraction(t, coordinateSelection) },
            namesMap = SanskritNamesCatalog.nakshatras,
            tSunrise = tSunrise,
            tNextSunrise = tNextSunrise,
            localMidnightMs = localMidnightMs,
            totalItems = 27,
            timeZoneId = location.timezone
        ).map { seg ->
            seg.copy(swaraInfo = SwaraYogaEngine.computeNakshatraSwaraEvents(seg, location.timezone))
        }

        val yogaSegments = AyanamshaAndSolvers.findSegments(
            fn = { t -> AyanamshaAndSolvers.getYogaFraction(t, coordinateSelection) },
            namesMap = SanskritNamesCatalog.yogas,
            tSunrise = tSunrise,
            tNextSunrise = tNextSunrise,
            localMidnightMs = localMidnightMs,
            totalItems = 27,
            timeZoneId = location.timezone
        )

        val karanaSegments = AyanamshaAndSolvers.findSegments(
            fn = AyanamshaAndSolvers::getKaranaFraction,
            namesMap = SanskritNamesCatalog.karanas,
            tSunrise = tSunrise,
            tNextSunrise = tNextSunrise,
            localMidnightMs = localMidnightMs,
            totalItems = 60,
            timeZoneId = location.timezone
        )

        val sunTropLon = AyanamshaAndSolvers.getTropicalSunLon(tSunrise)
        val sunSidLon = AyanamshaAndSolvers.getSiderealLon(sunTropLon, ayanamshaDeg)
        val moonTropLon = AyanamshaAndSolvers.getTropicalMoonLon(tSunrise)
        val moonSidLon = AyanamshaAndSolvers.getSiderealLon(moonTropLon, ayanamshaDeg)

        val sunRasiIdx = floor(sunSidLon / 30.0).toInt().coerceIn(0, 11)
        val moonRasiIdx = floor(moonSidLon / 30.0).toInt().coerceIn(0, 11)
        val sunRasiName = SanskritNamesCatalog.zodiac[sunRasiIdx] ?: "meṣa"
        val moonRasiName = SanskritNamesCatalog.zodiac[moonRasiIdx] ?: "meṣa"

        // Lunar Month (Masa) & Adhika / Kshaya Masa calculation
        val lastNmTime = runCatching {
            searchMoonPhase(0.0, Time.fromMillisecondsSince1970(sunriseMs - 32L * 86_400_000L), 33.0)
        }.getOrNull() ?: tSunrise

        val sunSidAtNm = AyanamshaAndSolvers.getSiderealLon(
            AyanamshaAndSolvers.getTropicalSunLon(lastNmTime),
            ayanamshaDeg
        )
        val solarRasiAtNm = floor(sunSidAtNm / 30.0).toInt().coerceIn(0, 11)
        val amantaMasaNum = ((solarRasiAtNm + 2) % 12).let { if (it == 0) 12 else it }

        var isAdhika = false
        var isKshaya = false
        runCatching {
            val nextNmTime = searchMoonPhase(
                0.0,
                Time.fromMillisecondsSince1970(lastNmTime.toMillisecondsSince1970() + 15L * 86_400_000L),
                20.0
            )
            if (nextNmTime != null) {
                val sunSidAtNextNm = AyanamshaAndSolvers.getSiderealLon(
                    AyanamshaAndSolvers.getTropicalSunLon(nextNmTime),
                    ayanamshaDeg
                )
                val solarRasiAtNextNm = floor(sunSidAtNextNm / 30.0).toInt().coerceIn(0, 11)
                val rasiAdvance = ((solarRasiAtNextNm - solarRasiAtNm) + 12) % 12
                if (rasiAdvance == 0) {
                    isAdhika = true
                } else if (rasiAdvance >= 2) {
                    isKshaya = true
                }
            }
        }

        val primaryTithi = tithiSegments.firstOrNull()?.number ?: 1
        val displayMasaNum = if (monthSystem == MonthSystem.PURNIMANTA && primaryTithi > 15 && !isAdhika) {
            (amantaMasaNum % 12) + 1
        } else {
            amantaMasaNum
        }

        val baseMasaName = SanskritNamesCatalog.masas[displayMasaNum] ?: "Caitra"
        val masaLabel = when {
            isAdhika -> "Adhika $baseMasaName"
            isKshaya -> "Kṣaya $baseMasaName"
            else -> baseMasaName
        }

        val rtuIdx = ((amantaMasaNum - 1) / 2).coerceIn(0, 5)
        val rtuName = SanskritNamesCatalog.ritus[rtuIdx] ?: "Vasanta"
        val drikRtuIdx = floor(((sunTropLon + 30.0) % 360.0) / 60.0).toInt().coerceIn(0, 5)
        val drikRtuName = SanskritNamesCatalog.ritus[drikRtuIdx] ?: "Vasanta"

        val ayana = if (sunRasiIdx >= 9 || sunRasiIdx <= 2) "Uttarāyaṇa" else "Dakṣiṇāyana"
        val drikAyana = if (sunTropLon >= 270.0 || sunTropLon < 90.0) "Uttarāyaṇa" else "Dakṣiṇāyana"

        val jd = tSunrise.ut + 2451545.0
        val kaliDay = floor(jd - 588465.5).toLong()
        var sakaYear = y - 78
        if (m < 3 || (m == 3 && d < 22)) {
            sakaYear -= 1
        }
        val kaliYear = sakaYear + 3179
        val vikramaYear = sakaYear + 135
        val samvatIdx = ((sakaYear + 12) % 60 + 60) % 60
        val samvatsara = SanskritNamesCatalog.samvats[samvatIdx] ?: "Parābhava"

        // Auspicious & Inauspicious periods
        val rahuStart = sunriseLocalHours + dayDurationHours * rahuOffsets[civilWeekday]
        val rahuEnd = rahuStart + 0.125 * dayDurationHours

        val yamaStart = sunriseLocalHours + dayDurationHours * yamaOffsets[civilWeekday]
        val yamaEnd = yamaStart + 0.125 * dayDurationHours

        val gulikaStart = sunriseLocalHours + dayDurationHours * gulikaOffsets[civilWeekday]
        val gulikaEnd = gulikaStart + 0.125 * dayDurationHours

        val abhijitStart = sunriseLocalHours + (7.0 / 15.0) * dayDurationHours
        val abhijitEnd = sunriseLocalHours + (8.0 / 15.0) * dayDurationHours

        val vijayaStart = sunriseLocalHours + (10.0 / 15.0) * dayDurationHours
        val vijayaEnd = sunriseLocalHours + (11.0 / 15.0) * dayDurationHours

        val godhuliStart = sunsetLocalHours - (12.0 / 60.0)
        val godhuliEnd = sunsetLocalHours + (12.0 / 60.0)

        val solarMidnightHours = (sunsetLocalHours + nextSunriseLocalHours) / 2.0
        val nishitaStart = solarMidnightHours - (24.0 / 60.0)
        val nishitaEnd = solarMidnightHours + (24.0 / 60.0)

        val pratahSandhyaStart = sunriseLocalHours - (72.0 / 60.0)
        val pratahSandhyaEnd = sunriseLocalHours
        val sayahnaSandhyaStart = sunsetLocalHours
        val sayahnaSandhyaEnd = sunsetLocalHours + (72.0 / 60.0)

        val brahmaStart = sunriseLocalHours - 96.0 / 60.0
        val brahmaEnd = sunriseLocalHours - 48.0 / 60.0

        val durmuhurtaList = mutableListOf<TimingInterval>()
        val curDmOffsets = durmuhurtaOffsets[civilWeekday]
        for (i in 0 until 2) {
            val off = curDmOffsets[i]
            if (off > 0.0) {
                var durBase = sunriseLocalHours
                var durLen = dayDurationHours
                if (civilWeekday == 2 && i == 1) {
                    durBase = sunsetLocalHours
                    durLen = nightDurationHours
                }
                val dmStart = durBase + (durLen * off) / 12.0
                val dmEnd = dmStart + (dayDurationHours * 0.8) / 12.0
                durmuhurtaList.add(
                    buildInterval(dmStart, dmEnd, localMidnightMs, "Durmuhūrta", isAuspicious = false)
                )
            }
        }

        // Varjyam & Amrita Kala
        val primaryNak = nakshatraSegments.firstOrNull()?.number ?: 1
        val vGhati = varjyamStartGhatis.getOrElse(primaryNak) { 30 }
        val nakStartTimeHours = sunriseLocalHours - 6.0
        val vStartHours = nakStartTimeHours + (24.0 * vGhati) / 60.0
        val vEndHours = vStartHours + (24.0 * 4.0) / 60.0
        val varjyamList = if (vStartHours >= sunriseLocalHours && vStartHours < nextSunriseLocalHours) {
            listOf(buildInterval(vStartHours, vEndHours, localMidnightMs, "Varjyam", isAuspicious = false))
        } else {
            emptyList()
        }

        val amStartHours = vStartHours + (14.0 * 24.0) / 60.0
        val amEndHours = amStartHours + (4.0 * 24.0) / 60.0
        val amritaList = if (amStartHours >= sunriseLocalHours && amStartHours < nextSunriseLocalHours) {
            listOf(buildInterval(amStartHours, amEndHours, localMidnightMs, "Amṛta Kāla", isAuspicious = true))
        } else {
            emptyList()
        }

        // Choghadiya
        val dayPart = dayDurationHours / 8.0
        val startDayIdx = dayChoghadiyaStarts[civilWeekday]
        val choghadiyaDay = (0 until 8).map { i ->
            val type = choghadiyaTypes[(startDayIdx + i) % 7]
            val cStart = sunriseLocalHours + i * dayPart
            buildInterval(cStart, cStart + dayPart, localMidnightMs, type, type in choghadiyaAuspicious)
        }

        val nightPart = nightDurationHours / 8.0
        val startNightIdx = nightChoghadiyaStarts[civilWeekday]
        val choghadiyaNight = (0 until 8).map { i ->
            val type = choghadiyaTypes[(startNightIdx + i) % 7]
            val cStart = sunsetLocalHours + i * nightPart
            buildInterval(cStart, cStart + nightPart, localMidnightMs, type, type in choghadiyaAuspicious)
        }

        val (dishaEn, dishaHi) = when (civilWeekday) {
            0 -> "West" to "पश्चिम"
            1 -> "East" to "पूर्व"
            2 -> "North" to "उत्तर"
            3 -> "North" to "उत्तर"
            4 -> "South" to "दक्षिण"
            5 -> "West" to "पश्चिम"
            else -> "East" to "पूर्व"
        }

        val inPanchakaZone = moonSidLon >= 293.333333 // Dhanishta Pada 3..Revati end
        val (panchakaEn, panchakaHi) = if (!inPanchakaZone) {
            "No Panchaka (Auspicious)" to "पंचक नहीं है (शुभ)"
        } else {
            when (civilWeekday) {
                0 -> "Roga Panchaka (Sunday)" to "रोग पंचक (रविवार)"
                1 -> "Raja Panchaka (Monday)" to "राज पंचक (सोमवार)"
                2 -> "Agni Panchaka (Tuesday)" to "अग्नि पंचक (मंगलवार)"
                3, 4 -> "Dosha-Free Panchaka" to "दोष-रहित शुभ पंचक"
                5 -> "Chora Panchaka (Friday)" to "चोर पंचक (शुक्रवार)"
                else -> "Mrityu Panchaka (Saturday)" to "मृत्यु पंचक (शनिवार)"
            }
        }

        val hasBhadra = karanaSegments.any {
            it.name.contains("Viṣṭi", ignoreCase = true) ||
                it.name.contains("Vishti", ignoreCase = true) ||
                it.name.contains("Bhadra", ignoreCase = true)
        }

        val planets = PlanetaryTransitScanner.computeNavagrahaPositions(
            tSunrise = tSunrise,
            sunTropLon = sunTropLon,
            moonTropLon = moonTropLon,
            ayanamshaDeg = ayanamshaDeg
        )

        val paksha = if (primaryTithi <= 15) "Śukla" else "Kṛṣṇa"
        val lunarPhase = FestivalAndMoonDetector.computeLunarPhase(primaryTithi, paksha)
        val observances = FestivalAndMoonDetector.detectObservances(
            primaryTithiNum = primaryTithi,
            masaNumber = displayMasaNum,
            isAdhika = isAdhika,
            paksha = paksha,
            sunRasiIndex = sunRasiIdx
        )

        val swaraYoga = SwaraYogaEngine.computeSwaraYoga(
            tithiNumber = primaryTithi,
            sunriseStr = sunriseStr,
            sunsetStr = sunsetStr,
            moonriseStr = moonriseStr,
            moonsetStr = moonsetStr,
            sunriseMs = sunriseMs,
            sunsetMs = sunsetMs,
            timeZoneId = location.timezone,
            moonriseMs = moonriseMs,
            moonsetMs = moonsetMs
        )

        val upcomingTransits = if (includeExtendedTransits) {
            PlanetaryTransitScanner.scanUpcomingTransits(
                location = location,
                tSunrise = tSunrise,
                tNextSunrise = tNextSunrise,
                ayanamshaKey = coordinateSelection,
                daysAhead = 30
            )
        } else {
            emptyList()
        }

        return PanchangaSnapshot(
            city = location.name,
            latitude = location.latitude,
            longitude = location.longitude,
            dateIso = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            dateDisplay = "${d.toString().padStart(2, '0')}/${m.toString().padStart(2, '0')}/$y",
            timezone = location.timezone,
            jd = round(jd * 100000.0) / 100000.0,
            sunriseJd = round(tSunrise.ut * 100000.0) / 100000.0,
            coordinateMode = if (coordinateSelection == CoordinateSelection.TROPICAL) "tropical" else "sidereal",
            coordinateLabel = coordinateSelection.displayLabel,
            ayanamsha = if (coordinateSelection == CoordinateSelection.TROPICAL) null else coordinateSelection.displayLabel,
            ayanamshaKey = coordinateSelection,
            ayanamshaDegrees = if (coordinateSelection == CoordinateSelection.TROPICAL) null else round(ayanamshaDeg * 1_000_000.0) / 1_000_000.0,
            monthSystem = monthSystem,
            monthSystemLabel = monthSystem.displayLabel,
            samvatsara = samvatsara,
            ayana = ayana,
            drikAyana = drikAyana,
            masa = masaLabel,
            masaNumber = displayMasaNum,
            isAdhika = isAdhika,
            isKshaya = isKshaya,
            rtu = "$rtuName Ṛtu",
            drikRtu = "$drikRtuName Ṛtu",
            vaara = vaaraName,
            weekday = civilWeekday,
            kaliDay = kaliDay,
            sakaYear = sakaYear,
            kaliYear = kaliYear,
            vikramaYear = vikramaYear,
            sunrise = sunriseStr,
            sunset = sunsetStr,
            solarNoon = solarNoonStr,
            nextSunrise = TimeFormatters.formatHoursHMS(nextSunriseLocalHours),
            sunriseHours = sunriseLocalHours,
            sunsetHours = sunsetLocalHours,
            nextSunriseHours = nextSunriseLocalHours,
            previousSunriseMs = prevSunriseMs,
            previousSunsetMs = prevSunsetMs,
            sunriseMs = sunriseMs,
            sunsetMs = sunsetMs,
            nextSunriseMs = nextSunriseMs,
            moonrise = moonriseStr,
            moonriseStatus = moonriseStatus,
            moonset = moonsetStr,
            moonsetStatus = moonsetStatus,
            dayDuration = TimeFormatters.formatHoursHMS(dayDurationHours),
            nightDuration = TimeFormatters.formatHoursHMS(nightDurationHours),
            paksha = paksha,
            lunarPhase = lunarPhase,
            rahuKala = buildInterval(rahuStart, rahuEnd, localMidnightMs, "Rāhu Kāla", false),
            yamaganda = buildInterval(yamaStart, yamaEnd, localMidnightMs, "Yamagaṇḍa", false),
            gulikaKala = buildInterval(gulikaStart, gulikaEnd, localMidnightMs, "Gulikā Kāla", false),
            abhijitMuhurta = buildInterval(abhijitStart, abhijitEnd, localMidnightMs, "Abhijit Muhūrta", true),
            brahmaMuhurta = buildInterval(brahmaStart, brahmaEnd, localMidnightMs, "Brahma Muhūrta", true),
            amritaKala = amritaList,
            durmuhurta = durmuhurtaList,
            varjyam = varjyamList,
            gauriChoghadiyaDay = choghadiyaDay,
            gauriChoghadiyaNight = choghadiyaNight,
            tithi = tithiSegments,
            nakshatra = nakshatraSegments,
            yoga = yogaSegments,
            karana = karanaSegments,
            planets = planets,
            sunRasi = sunRasiName,
            moonRasi = moonRasiName,
            swaraYoga = swaraYoga,
            upcomingTransits = upcomingTransits,
            observances = observances,
            godhuliMuhurta = buildInterval(godhuliStart, godhuliEnd, localMidnightMs, "Godhūli Muhūrta", true),
            vijayaMuhurta = buildInterval(vijayaStart, vijayaEnd, localMidnightMs, "Vijaya Muhūrta", true),
            nishitaMuhurta = buildInterval(nishitaStart, nishitaEnd, localMidnightMs, "Niśītha Muhūrta", true),
            pratahSandhya = buildInterval(pratahSandhyaStart, pratahSandhyaEnd, localMidnightMs, "Prātaḥ Sandhyā", true),
            sayahnaSandhya = buildInterval(sayahnaSandhyaStart, sayahnaSandhyaEnd, localMidnightMs, "Sāyaṃ Sandhyā", true),
            dishaShoolaEn = dishaEn,
            dishaShoolaHi = dishaHi,
            panchakaStatusEn = panchakaEn,
            panchakaStatusHi = panchakaHi,
            hasBhadra = hasBhadra,
            moonriseMs = moonriseMs,
            moonsetMs = moonsetMs
        )
    }

    fun computeMonthlyCalendar(
        location: CityLocation,
        year: Int,
        month: Int,
        monthSystem: MonthSystem,
        coordinateSelection: CoordinateSelection
    ): List<MonthlyCalendarDay> {
        val firstDay = LocalDate.of(year, month, 1)
        val length = firstDay.lengthOfMonth()
        var previousMasa: String? = null
        return (1..length).map { dayOfMonth ->
            val date = LocalDate.of(year, month, dayOfMonth)
            val snap = computePanchanga(
                location = location,
                date = date,
                monthSystem = monthSystem,
                coordinateSelection = coordinateSelection,
                includeExtendedTransits = false
            )
            val isTransition = previousMasa != null && previousMasa != snap.masa
            previousMasa = snap.masa
            val primaryTithi = snap.tithi.firstOrNull()
            val primaryNak = snap.nakshatra.firstOrNull()
            MonthlyCalendarDay(
                dayOfMonth = dayOfMonth,
                month = month,
                year = year,
                dateIso = snap.dateIso,
                vaara = snap.vaara,
                masa = snap.masa,
                isAdhika = snap.isAdhika,
                paksha = snap.paksha,
                primaryTithiNumber = primaryTithi?.number ?: 1,
                tithiName = primaryTithi?.name ?: "",
                tithiEnds = primaryTithi?.ends,
                nakshatraName = primaryNak?.name ?: "",
                nakshatraEnds = primaryNak?.ends,
                yogaName = snap.yoga.firstOrNull()?.name ?: "",
                karanaName = snap.karana.firstOrNull()?.name ?: "",
                sunrise = snap.sunrise,
                sunset = snap.sunset,
                observances = snap.observances,
                isMasaTransition = isTransition
            )
        }
    }

    private fun buildInterval(
        startHours: Double,
        endHours: Double,
        localMidnightMs: Long,
        name: String,
        isAuspicious: Boolean
    ): TimingInterval = TimingInterval(
        start = TimeFormatters.formatHoursHMS(startHours),
        end = TimeFormatters.formatHoursHMS(endHours),
        startTimeMs = localMidnightMs + (startHours * 3_600_000.0).toLong(),
        endTimeMs = localMidnightMs + (endHours * 3_600_000.0).toLong(),
        name = name,
        isAuspicious = isAuspicious
    )
}
