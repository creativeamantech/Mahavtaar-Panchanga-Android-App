package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.AngaSegment
import com.mahavtaar.panchanga.domain.model.AngaSwaraInfo
import com.mahavtaar.panchanga.domain.model.SwaraBoundaryEvent
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.SwaraWindow
import com.mahavtaar.panchanga.domain.model.SwaraYogaState

/**
 * Classical Shiva Swarodaya engine preserving exact domain rules from:
 * - `src/swaraYoga.ts`
 * - `src/lib/tithiSwaraEngine.ts`
 * - `src/lib/nakshatraSwaraEngine.ts`
 */
object SwaraYogaEngine {

    private const val ONE_HOUR_MS = 60L * 60L * 1000L

    data class SwaraTithiRule(
        val dayNumber: Int,
        val tithiName: String,
        val paksha: String,
        val sunriseSwara: SwaraNadi,
        val sunsetSwara: SwaraNadi
    )

    private val tithiNames15 = listOf(
        "Pratipada", "Dwitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadasi", "Dwadashi", "Trayodashi", "Chaturdashi", "Purnima"
    )

    val swaraCycleRules: List<SwaraTithiRule> = (1..30).map { day ->
        val isShukla = day <= 15
        val idxInPaksha = if (isShukla) day else day - 15
        val triadIndex = (idxInPaksha - 1) / 3
        val sunriseSwara = if (isShukla) {
            if (triadIndex % 2 == 0) SwaraNadi.IDA else SwaraNadi.PINGALA
        } else {
            if (triadIndex % 2 == 0) SwaraNadi.PINGALA else SwaraNadi.IDA
        }
        val sunsetSwara = oppositeNadi(sunriseSwara)
        val tithiName = when (day) {
            15 -> "Purnima"
            30 -> "Amawashya"
            else -> tithiNames15[idxInPaksha - 1]
        }
        val paksha = when (day) {
            15 -> "Full moon"
            30 -> "No Moon"
            in 1..14 -> "Shukla Paksha"
            else -> "Krishna Paksha"
        }
        SwaraTithiRule(day, tithiName, paksha, sunriseSwara, sunsetSwara)
    }

    /**
     * Odd Rashi -> Pingala (Right), Even Rashi -> Ida (Left) per Shiva Swarodaya Verses 73-74.
     */
    private val nakshatraStartNadiByNum: Map<Int, SwaraNadi> = mapOf(
        1 to SwaraNadi.PINGALA, 2 to SwaraNadi.PINGALA, 3 to SwaraNadi.PINGALA,
        4 to SwaraNadi.IDA, 5 to SwaraNadi.IDA,
        6 to SwaraNadi.PINGALA, 7 to SwaraNadi.PINGALA,
        8 to SwaraNadi.IDA, 9 to SwaraNadi.IDA,
        10 to SwaraNadi.PINGALA, 11 to SwaraNadi.PINGALA, 12 to SwaraNadi.PINGALA,
        13 to SwaraNadi.IDA, 14 to SwaraNadi.IDA,
        15 to SwaraNadi.PINGALA, 16 to SwaraNadi.PINGALA,
        17 to SwaraNadi.IDA, 18 to SwaraNadi.IDA,
        19 to SwaraNadi.PINGALA, 20 to SwaraNadi.PINGALA, 21 to SwaraNadi.PINGALA,
        22 to SwaraNadi.IDA, 23 to SwaraNadi.IDA,
        24 to SwaraNadi.PINGALA, 25 to SwaraNadi.PINGALA,
        26 to SwaraNadi.IDA, 27 to SwaraNadi.IDA,
        28 to SwaraNadi.IDA
    )

    private val nakshatraRashiNumber: Map<Int, Int> = mapOf(
        1 to 1, 2 to 1, 3 to 1, 4 to 2, 5 to 2, 6 to 3, 7 to 3, 8 to 4, 9 to 4,
        10 to 5, 11 to 5, 12 to 5, 13 to 6, 14 to 6, 15 to 7, 16 to 7, 17 to 8, 18 to 8,
        19 to 9, 20 to 9, 21 to 9, 22 to 10, 23 to 10, 24 to 11, 25 to 11, 26 to 12, 27 to 12, 28 to 10
    )

    val allNakshatraSwaraRules: List<com.mahavtaar.panchanga.domain.model.NakshatraSwaraRuleItem> = (1..28).map { num ->
        val rNum = nakshatraRashiNumber[num] ?: 1
        val start = nakshatraStartNadiByNum[num] ?: SwaraNadi.PINGALA
        val nakName = if (num == 28) "Abhijit" else SanskritNamesCatalog.nakshatras[num] ?: "Aśvinī"
        val rName = SanskritNamesCatalog.zodiacEnglish[(rNum - 1).coerceIn(0, 11)]
        com.mahavtaar.panchanga.domain.model.NakshatraSwaraRuleItem(
            nakshatraNumber = num,
            nakshatraName = nakName,
            rashiNumber = rNum,
            rashiName = rName,
            startNadi = start,
            endNadi = oppositeNadi(start)
        )
    }

    fun evaluateNakshatraSwaraAlignment(
        paksha: String,
        tithiNumber: Int,
        nakshatraNumber: Int,
        pada: Int
    ): com.mahavtaar.panchanga.domain.model.NakshatraSwaraAlignmentResult {
        val isShukla = !paksha.contains("Krishna", ignoreCase = true) && !paksha.contains("Kṛṣṇa", ignoreCase = true)
        val clampedTithi = tithiNumber.coerceIn(1, 15)
        val dayNum = if (isShukla) clampedTithi else 15 + clampedTithi
        val tithiRule = getRuleForTithi(dayNum)
        val requiredSwara = tithiRule.sunriseSwara

        val clampedNak = nakshatraNumber.coerceIn(1, 27)
        val clampedPada = pada.coerceIn(1, 4)
        val startLon = (clampedNak - 1) * (360.0 / 27.0) + (clampedPada - 1) * (360.0 / 108.0) + 0.5
        val rashiIdx = (startLon / 30.0).toInt().coerceIn(0, 11)
        val isOddSign = rashiIdx % 2 == 0 // 0=Aries(odd), 1=Taurus(even)...
        val supportedSwara = if (isOddSign) SwaraNadi.PINGALA else SwaraNadi.IDA
        val endSwara = oppositeNadi(supportedSwara)

        val nakTattva = when (rashiIdx % 4) {
            0 -> com.mahavtaar.panchanga.domain.model.TattvaElement.TEJAS   // Fire
            1 -> com.mahavtaar.panchanga.domain.model.TattvaElement.PRITHVI // Earth
            2 -> com.mahavtaar.panchanga.domain.model.TattvaElement.VAYU    // Air
            else -> com.mahavtaar.panchanga.domain.model.TattvaElement.JALA // Water
        }

        val rating = if (requiredSwara == supportedSwara) "ALIGNED" else "MISMATCH"
        val (warnEn, warnHi) = when {
            requiredSwara == SwaraNadi.IDA && (nakTattva == com.mahavtaar.panchanga.domain.model.TattvaElement.TEJAS || nakTattva == com.mahavtaar.panchanga.domain.model.TattvaElement.VAYU) ->
                "Lunar Ida meets ${nakTattva.displayName} Rāśi — guard against restlessness or mental heat." to
                    "चन्द्र (इड़ा) स्वर ${nakTattva.sanskritName} राशि में — चंचलता या उष्णता से बचने के लिए धैर्य रखें।"
            requiredSwara == SwaraNadi.PINGALA && (nakTattva == com.mahavtaar.panchanga.domain.model.TattvaElement.JALA || nakTattva == com.mahavtaar.panchanga.domain.model.TattvaElement.PRITHVI) ->
                "Solar Pingala meets ${nakTattva.displayName} Rāśi — channel dynamic force steadily without inertia." to
                    "सूर्य (पिङ्गला) स्वर ${nakTattva.sanskritName} राशि में — ऊर्जा को स्थिर और रचनात्मक दिशा दें।"
            else -> null to null
        }

        val (adviceEn, adviceHi) = if (rating == "ALIGNED") {
            "Harmonious Swara Synergy: Sunrise Tithi Nāḍī (${requiredSwara.label}) matches the Nakṣatra's Rāśi polarity. Excellent flow for vows, healing & major decisions." to
                "उत्तम स्वर सामञ्जस्य: तिथि नाड़ी (${if (requiredSwara == SwaraNadi.IDA) "इड़ा" else "पिङ्गला"}) और नक्षत्र राशि स्वर एकरूप हैं। संकल्प, साधना व नवीन कार्यों हेतु श्रेष्ठ समय।"
        } else {
            "Opposite Polarity Shift: Tithi calls for ${requiredSwara.label} while Nakṣatra Rāśi awakens ${supportedSwara.label}. Practice 5 minutes of Nāḍī Śodhana before important acts." to
                "विपरीत स्वर ध्रुवता: तिथि स्वर व नक्षत्र राशि स्वर भिन्न हैं। महत्वपूर्ण कार्य से पूर्व ५ मिनट नाड़ी-शोधन प्राणायाम करें।"
        }

        val (padaEn, padaHi) = when (clampedPada) {
            1 -> "Pada 1 (Dharma): Initiation, noble intent & righteous clarity." to "चरण १ (धर्म): शुभ संकल्प, पवित्रता और नैतिक स्पष्टता।"
            2 -> "Pada 2 (Artha): Resourcefulness, material grounding & trade." to "चरण २ (अर्थ): स्थिर कार्य, वित्त व व्यावहारिक सिद्धि।"
            3 -> "Pada 3 (Kāma): Creativity, communication & social harmony." to "चरण ३ (काम): संवाद, सृजनशीलता और सम्बन्ध सौहार्द।"
            else -> "Pada 4 (Mokṣa): Introspection, healing & spiritual release." to "चरण ४ (मोक्ष): ध्यान, चिकित्सा और आध्यात्मिक चिन्तन।"
        }

        return com.mahavtaar.panchanga.domain.model.NakshatraSwaraAlignmentResult(
            paksha = if (isShukla) "Śukla" else "Kṛṣṇa",
            tithiNumber = clampedTithi,
            tithiName = tithiRule.tithiName,
            requiredTithiSwara = requiredSwara,
            nakshatraNumber = clampedNak,
            nakshatraName = SanskritNamesCatalog.nakshatras[clampedNak] ?: "Aśvinī",
            pada = clampedPada,
            rashiIndex = rashiIdx,
            rashiName = SanskritNamesCatalog.zodiacEnglish[rashiIdx],
            supportedNakshatraSwara = supportedSwara,
            nakshatraEndSwara = endSwara,
            nakshatraElement = nakTattva,
            rating = rating,
            elementWarningEn = warnEn,
            elementWarningHi = warnHi,
            cosmicAdviceEn = adviceEn,
            cosmicAdviceHi = adviceHi,
            padaAdviceEn = padaEn,
            padaAdviceHi = padaHi
        )
    }

    fun oppositeNadi(nadi: SwaraNadi): SwaraNadi = when (nadi) {
        SwaraNadi.IDA -> SwaraNadi.PINGALA
        SwaraNadi.PINGALA -> SwaraNadi.IDA
        SwaraNadi.SUSHUMNA -> SwaraNadi.SUSHUMNA
    }

    fun getRuleForTithi(tithiNumber: Int): SwaraTithiRule {
        val clamped = tithiNumber.coerceIn(1, 30)
        return swaraCycleRules[clamped - 1]
    }

    fun computeTithiSwaraEvents(segment: AngaSegment, timeZoneId: String): AngaSwaraInfo {
        val rule = getRuleForTithi(segment.number)
        val startNadi = rule.sunriseSwara
        val endNadi = oppositeNadi(startNadi)
        return buildBoundaryEvents(segment, startNadi, endNadi, timeZoneId)
    }

    fun computeNakshatraSwaraEvents(segment: AngaSegment, timeZoneId: String): AngaSwaraInfo {
        val startNadi = nakshatraStartNadiByNum[segment.number] ?: SwaraNadi.PINGALA
        val endNadi = oppositeNadi(startNadi)
        return buildBoundaryEvents(segment, startNadi, endNadi, timeZoneId)
    }

    private fun buildBoundaryEvents(
        segment: AngaSegment,
        startNadi: SwaraNadi,
        endNadi: SwaraNadi,
        timeZoneId: String
    ): AngaSwaraInfo {
        val startMs = segment.startTimeMs
        val endMs = segment.endTimeMs
        val hasOverlap = startMs != null && endMs != null && (endMs - startMs) < 2L * ONE_HOUR_MS

        val startEvent = startMs?.let { s ->
            val e = s + ONE_HOUR_MS
            SwaraBoundaryEvent(
                type = "start",
                nadi = startNadi,
                nadiLabel = "${startNadi.label} (${startNadi.nostril} Nostril)",
                startTimeMs = s,
                endTimeMs = e,
                formattedRange = TimeFormatters.formatDateTimeRangeWithSeconds(s, e, timeZoneId),
                hasOverlap = hasOverlap
            )
        }

        val endEvent = endMs?.let { e ->
            val s = e - ONE_HOUR_MS
            SwaraBoundaryEvent(
                type = "end",
                nadi = endNadi,
                nadiLabel = "${endNadi.label} (${endNadi.nostril} Nostril)",
                startTimeMs = s,
                endTimeMs = e,
                formattedRange = TimeFormatters.formatDateTimeRangeWithSeconds(s, e, timeZoneId),
                hasOverlap = hasOverlap
            )
        }

        return AngaSwaraInfo(
            startNadi = startNadi,
            endNadi = endNadi,
            startEvent = startEvent,
            endEvent = endEvent,
            hasOverlap = hasOverlap
        )
    }

    private fun isTimeInsideWindow(currentMins: Double, startMins: Double, endMins: Double): Boolean {
        val cur = ((currentMins % 1440.0) + 1440.0) % 1440.0
        val s = ((startMins % 1440.0) + 1440.0) % 1440.0
        val e = ((endMins % 1440.0) + 1440.0) % 1440.0
        return if (s <= e) cur in s..e else cur >= s || cur <= e
    }

    fun computeSwaraYoga(
        tithiNumber: Int,
        sunriseStr: String,
        sunsetStr: String,
        currentLocalMinutes: Double? = null,
        moonriseStr: String? = null,
        moonsetStr: String? = null,
        sunriseMs: Long? = null,
        sunsetMs: Long? = null,
        timeZoneId: String? = null,
        nowMs: Long? = null,
        moonriseMs: Long? = null,
        moonsetMs: Long? = null
    ): SwaraYogaState {
        val rule = getRuleForTithi(tithiNumber)
        val moonriseSwara = oppositeNadi(rule.sunriseSwara)
        val moonsetSwara = oppositeNadi(rule.sunsetSwara)

        val srMin = TimeFormatters.parseTimeToMinutes(sunriseStr) ?: (6.0 * 60.0)
        val ssMin = TimeFormatters.parseTimeToMinutes(sunsetStr) ?: (18.0 * 60.0)
        val mrMin = TimeFormatters.parseTimeToMinutes(moonriseStr)
        val msMin = TimeFormatters.parseTimeToMinutes(moonsetStr)

        var daytimeMins = ssMin - srMin
        if (daytimeMins < 0.0) daytimeMins += 1440.0
        val dayHoraMins = daytimeMins / 12.0
        val srHoraEndMin = srMin + dayHoraMins
        val mdHoraStartMin = srMin + 6.0 * dayHoraMins
        val mdHoraEndMin = srMin + 7.0 * dayHoraMins
        val ssHoraStartMin = srMin + 11.0 * dayHoraMins

        val dayBounds = if (sunriseMs != null && sunsetMs != null && sunsetMs > sunriseMs && !timeZoneId.isNullOrBlank()) {
            HoraEngine.computeDayHoraBounds(sunriseMs, sunsetMs)
        } else {
            emptyList()
        }

        val srActive: Boolean
        val mdActive: Boolean
        val ssActive: Boolean
        val sunriseWindow: SwaraWindow
        val middayWindow: SwaraWindow
        val sunsetWindow: SwaraWindow

        if (dayBounds.size == 12 && !timeZoneId.isNullOrBlank()) {
            val (srStartMs, srEndMs) = dayBounds[0]
            val (mdStartMs, mdEndMs) = dayBounds[6]
            val (ssStartMs, ssEndMs) = dayBounds[11]

            srActive = nowMs?.let { it in srStartMs until srEndMs }
                ?: currentLocalMinutes?.let { isTimeInsideWindow(it, srMin, srHoraEndMin) }
                ?: false
            mdActive = nowMs?.let { it in mdStartMs until mdEndMs }
                ?: currentLocalMinutes?.let { isTimeInsideWindow(it, mdHoraStartMin, mdHoraEndMin) }
                ?: false
            ssActive = nowMs?.let { it in ssStartMs until ssEndMs }
                ?: currentLocalMinutes?.let { isTimeInsideWindow(it, ssHoraStartMin, ssMin) }
                ?: false

            val srStartFmt = TimeFormatters.format12HourHMS(srStartMs, timeZoneId)
            val srEndFmt = TimeFormatters.format12HourHMS(srEndMs, timeZoneId)
            sunriseWindow = SwaraWindow(
                label = "Sunrise Swara",
                start = srStartFmt,
                end = srEndFmt,
                windowFormatted = "$srStartFmt – $srEndFmt",
                nadi = rule.sunriseSwara,
                ruleDescription = "Matches 1st Day Hora (Sunrise to end of 1st Hora)",
                isActive = srActive,
                startTimeMs = srStartMs,
                endTimeMs = srEndMs
            )

            val mdStartFmt = TimeFormatters.format12HourHMS(mdStartMs, timeZoneId)
            val mdEndFmt = TimeFormatters.format12HourHMS(mdEndMs, timeZoneId)
            middayWindow = SwaraWindow(
                label = "Midday Swara",
                start = mdStartFmt,
                end = mdEndFmt,
                windowFormatted = "$mdStartFmt – $mdEndFmt",
                nadi = rule.sunriseSwara,
                ruleDescription = "Matches 7th Day Hora (Same Nadi as Sunrise)",
                isActive = mdActive,
                startTimeMs = mdStartMs,
                endTimeMs = mdEndMs
            )

            val ssStartFmt = TimeFormatters.format12HourHMS(ssStartMs, timeZoneId)
            val ssEndFmt = TimeFormatters.format12HourHMS(ssEndMs, timeZoneId)
            sunsetWindow = SwaraWindow(
                label = "Sunset Swara",
                start = ssStartFmt,
                end = ssEndFmt,
                windowFormatted = "$ssStartFmt – $ssEndFmt",
                nadi = rule.sunsetSwara,
                ruleDescription = "Matches 12th Day Hora (Leads up to Sunset)",
                isActive = ssActive,
                startTimeMs = ssStartMs,
                endTimeMs = ssEndMs
            )
        } else {
            srActive = currentLocalMinutes?.let { isTimeInsideWindow(it, srMin, srHoraEndMin) } ?: false
            mdActive = currentLocalMinutes?.let { isTimeInsideWindow(it, mdHoraStartMin, mdHoraEndMin) } ?: false
            ssActive = currentLocalMinutes?.let { isTimeInsideWindow(it, ssHoraStartMin, ssMin) } ?: false

            sunriseWindow = SwaraWindow(
                label = "Sunrise Swara",
                start = TimeFormatters.formatMinutesTo24h(srMin),
                end = TimeFormatters.formatMinutesTo24h(srHoraEndMin),
                windowFormatted = "${TimeFormatters.formatMinutesTo24h(srMin)} – ${TimeFormatters.formatMinutesTo24h(srHoraEndMin)}",
                nadi = rule.sunriseSwara,
                ruleDescription = "Matches 1st Day Hora (Sunrise to end of 1st Hora)",
                isActive = srActive
            )

            middayWindow = SwaraWindow(
                label = "Midday Swara",
                start = TimeFormatters.formatMinutesTo24h(mdHoraStartMin),
                end = TimeFormatters.formatMinutesTo24h(mdHoraEndMin),
                windowFormatted = "${TimeFormatters.formatMinutesTo24h(mdHoraStartMin)} – ${TimeFormatters.formatMinutesTo24h(mdHoraEndMin)}",
                nadi = rule.sunriseSwara,
                ruleDescription = "Matches 7th Day Hora (Same Nadi as Sunrise)",
                isActive = mdActive
            )

            sunsetWindow = SwaraWindow(
                label = "Sunset Swara",
                start = TimeFormatters.formatMinutesTo24h(ssHoraStartMin),
                end = TimeFormatters.formatMinutesTo24h(ssMin),
                windowFormatted = "${TimeFormatters.formatMinutesTo24h(ssHoraStartMin)} – ${TimeFormatters.formatMinutesTo24h(ssMin)}",
                nadi = rule.sunsetSwara,
                ruleDescription = "Matches 12th Day Hora (Leads up to Sunset)",
                isActive = ssActive
            )
        }

        val resolvedMrStartMs = moonriseMs ?: if (sunriseMs != null && mrMin != null && !timeZoneId.isNullOrBlank()) {
            val zdt = java.time.Instant.ofEpochMilli(sunriseMs).atZone(TimeFormatters.safeZoneId(timeZoneId))
            val midnightMs = zdt.toLocalDate().atStartOfDay(zdt.zone).toInstant().toEpochMilli()
            midnightMs + (mrMin * 60_000.0).toLong()
        } else null
        val resolvedMrEndMs = resolvedMrStartMs?.plus(ONE_HOUR_MS)

        val resolvedMsEndMs = moonsetMs ?: if (sunriseMs != null && msMin != null && !timeZoneId.isNullOrBlank()) {
            val zdt = java.time.Instant.ofEpochMilli(sunriseMs).atZone(TimeFormatters.safeZoneId(timeZoneId))
            val midnightMs = zdt.toLocalDate().atStartOfDay(zdt.zone).toInstant().toEpochMilli()
            midnightMs + (msMin * 60_000.0).toLong()
        } else null
        val resolvedMsStartMs = resolvedMsEndMs?.minus(ONE_HOUR_MS)

        val mrActive = if (resolvedMrStartMs != null && resolvedMrEndMs != null && nowMs != null) {
            nowMs in resolvedMrStartMs until resolvedMrEndMs
        } else if (mrMin != null && currentLocalMinutes != null) {
            isTimeInsideWindow(currentLocalMinutes, mrMin, mrMin + 60.0)
        } else false

        val msActive = if (resolvedMsStartMs != null && resolvedMsEndMs != null && nowMs != null) {
            nowMs in resolvedMsStartMs until resolvedMsEndMs
        } else if (msMin != null && currentLocalMinutes != null) {
            isTimeInsideWindow(currentLocalMinutes, msMin - 60.0, msMin)
        } else false

        val moonriseWindow = if (resolvedMrStartMs != null && resolvedMrEndMs != null && !timeZoneId.isNullOrBlank()) {
            val sFmt = TimeFormatters.format12HourHMS(resolvedMrStartMs, timeZoneId)
            val eFmt = TimeFormatters.format12HourHMS(resolvedMrEndMs, timeZoneId)
            SwaraWindow(
                label = "Moonrise Swara",
                start = sFmt,
                end = eFmt,
                windowFormatted = "$sFmt – $eFmt",
                nadi = moonriseSwara,
                ruleDescription = "Starts at Moonrise, runs for 1 hour (Opposite of Sunrise)",
                isActive = mrActive,
                startTimeMs = resolvedMrStartMs,
                endTimeMs = resolvedMrEndMs
            )
        } else {
            mrMin?.let {
                SwaraWindow(
                    label = "Moonrise Swara",
                    start = TimeFormatters.formatMinutesTo24h(it),
                    end = TimeFormatters.formatMinutesTo24h(it + 60.0),
                    windowFormatted = "${TimeFormatters.formatMinutesTo24h(it)} – ${TimeFormatters.formatMinutesTo24h(it + 60.0)}",
                    nadi = moonriseSwara,
                    ruleDescription = "Starts at Moonrise, runs for 1 hour (Opposite of Sunrise)",
                    isActive = mrActive
                )
            }
        }

        val moonsetWindow = if (resolvedMsStartMs != null && resolvedMsEndMs != null && !timeZoneId.isNullOrBlank()) {
            val sFmt = TimeFormatters.format12HourHMS(resolvedMsStartMs, timeZoneId)
            val eFmt = TimeFormatters.format12HourHMS(resolvedMsEndMs, timeZoneId)
            SwaraWindow(
                label = "Moonset Swara",
                start = sFmt,
                end = eFmt,
                windowFormatted = "$sFmt – $eFmt",
                nadi = moonsetSwara,
                ruleDescription = "Starts 1 hour before Moonset, runs until Moonset (Opposite of Sunset)",
                isActive = msActive,
                startTimeMs = resolvedMsStartMs,
                endTimeMs = resolvedMsEndMs
            )
        } else {
            msMin?.let {
                SwaraWindow(
                    label = "Moonset Swara",
                    start = TimeFormatters.formatMinutesTo24h(it - 60.0),
                    end = TimeFormatters.formatMinutesTo24h(it),
                    windowFormatted = "${TimeFormatters.formatMinutesTo24h(it - 60.0)} – ${TimeFormatters.formatMinutesTo24h(it)}",
                    nadi = moonsetSwara,
                    ruleDescription = "Starts 1 hour before Moonset, runs until Moonset (Opposite of Sunset)",
                    isActive = msActive
                )
            }
        }

        val activeCelestialWindow = when {
            srActive -> "sunrise"
            mdActive -> "midday"
            ssActive -> "sunset"
            mrActive -> "moonrise"
            msActive -> "moonset"
            else -> null
        }

        val activeSwara = when (activeCelestialWindow) {
            "sunrise", "midday" -> rule.sunriseSwara
            "sunset" -> rule.sunsetSwara
            "moonrise" -> moonriseSwara
            "moonset" -> moonsetSwara
            else -> rule.sunriseSwara
        }

        return SwaraYogaState(
            dayNumber = rule.dayNumber,
            tithiName = rule.tithiName,
            paksha = rule.paksha,
            sunriseSwara = rule.sunriseSwara,
            sunsetSwara = rule.sunsetSwara,
            moonriseSwara = moonriseSwara,
            moonsetSwara = moonsetSwara,
            sunriseWindow = sunriseWindow,
            middayWindow = middayWindow,
            sunsetWindow = sunsetWindow,
            moonriseWindow = moonriseWindow,
            moonsetWindow = moonsetWindow,
            activeCelestialWindow = activeCelestialWindow,
            currentActiveSwara = activeSwara,
            auspiciousWorks = getAuspiciousWorks(activeSwara),
            inauspiciousWorks = getInauspiciousWorks(activeSwara),
            energySummary = getEnergySummary(activeSwara),
            rulingDeities = getRulingDeities(activeSwara)
        )
    }

    fun getAuspiciousWorks(nadi: SwaraNadi): List<String> = when (nadi) {
        SwaraNadi.IDA -> listOf(
            "Starting journeys or long travel",
            "Housewarming (Gṛhapraveśa) and construction",
            "Buying clothes, jewelry, or vehicles",
            "Marriage, engagement, and peaceful ceremonies",
            "Learning arts, music, meditation, and literature",
            "Taking medicine or initiating medical treatment"
        )
        SwaraNadi.PINGALA -> listOf(
            "Eating meals and digestion (strong Jatharāgni)",
            "Physical exercise, athletics, and yoga asana",
            "Debates, intellectual contests, exams, negotiations",
            "Courageous undertakings and energetic effort",
            "Bathing, crossing water bodies, driving vehicles",
            "Purchasing tools, metals, or machinery"
        )
        SwaraNadi.SUSHUMNA -> listOf(
            "Dhyāna (Meditation) and spiritual contemplation",
            "Prāṇāyāma and Kuṇḍalinī awareness",
            "Mantra Japa and scriptural study",
            "Prayer for inner clarity and detachment"
        )
    }

    fun getInauspiciousWorks(nadi: SwaraNadi): List<String> = when (nadi) {
        SwaraNadi.IDA -> listOf(
            "Heavy meals (weaker digestive fire)",
            "Fierce combat, athletic exertion, or aggression",
            "Entering risky legal disputes or conflicts"
        )
        SwaraNadi.PINGALA -> listOf(
            "Long peaceful travel or house entry",
            "Delicate peace accords or gentle vows",
            "Excessive intake of cooling liquids"
        )
        SwaraNadi.SUSHUMNA -> listOf(
            "All worldly and commercial transactions",
            "Travel, buying, selling, or litigation"
        )
    }

    private fun getEnergySummary(nadi: SwaraNadi): String = when (nadi) {
        SwaraNadi.IDA -> "Cooling, Calming, Magnetic, Receptive (Somya · Water & Earth)"
        SwaraNadi.PINGALA -> "Heating, Energizing, Active, Dynamic (Raudra/Agni · Fire & Air)"
        SwaraNadi.SUSHUMNA -> "Neutralizing, Transcendent, Yogic, Non-dual (Ākāśa / Space)"
    }

    private fun getRulingDeities(nadi: SwaraNadi): String = when (nadi) {
        SwaraNadi.IDA -> "Moon (Candra), Bṛhaspati, Śukra, Budha"
        SwaraNadi.PINGALA -> "Sun (Sūrya), Maṅgala (Mars), Śani, Rāhu"
        SwaraNadi.SUSHUMNA -> "Lord Śiva & Parāśakti (Brahman)"
    }
}
