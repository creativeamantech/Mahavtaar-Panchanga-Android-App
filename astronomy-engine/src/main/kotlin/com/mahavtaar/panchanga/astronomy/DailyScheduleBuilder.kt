package com.mahavtaar.panchanga.astronomy

import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.Auspiciousness
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.ScheduleCategory
import com.mahavtaar.panchanga.domain.model.ScheduleEvent
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.SwaraWindow
import com.mahavtaar.panchanga.domain.model.TimingInterval

/**
 * Pure Kotlin unified chronological daily schedule builder matching `src/lib/dailyScheduleModel.ts`.
 * Merges Sunrise, Sunset, Muhurtas, Inauspicious Kalas, Day/Night Choghadiyas, Five Angas,
 * Swara Yoga windows, and 24 Planetary Horas into one sorted timeline with full Date + Seconds
 * and Nadi / Tattva metadata.
 */
object DailyScheduleBuilder {

    fun build(
        snapshot: PanchangaSnapshot,
        dailyHoras: DailyHoras,
        nowMs: Long
    ): List<ScheduleEvent> {
        val events = mutableListOf<ScheduleEvent>()
        val tz = snapshot.timezone

        fun fmt(ms: Long): String = TimeFormatters.formatDateTimeWithSeconds(ms, tz)
        fun fmtRange(s: Long, e: Long): String = "${fmt(s)} – ${fmt(e)}"

        // 1. Astronomical milestones (Vedic daytime & nighttime windows)
        events += ScheduleEvent(
            id = "astro_sunrise_${snapshot.dateIso}",
            category = ScheduleCategory.ASTRONOMICAL,
            titleEn = "Sunrise (Sūryodaya)",
            titleHi = "सूर्योदय (दिन आरम्भ)",
            subtitleEn = "Vedic daytime begins · ${snapshot.vaara}",
            subtitleHi = "वैदिक दिवस आरम्भ · ${snapshot.vaara}",
            startTimeMs = snapshot.sunriseMs,
            endTimeMs = snapshot.sunsetMs,
            formattedStart = fmt(snapshot.sunriseMs),
            formattedEnd = fmt(snapshot.sunsetMs),
            formattedRange = fmtRange(snapshot.sunriseMs, snapshot.sunsetMs),
            primaryNadi = snapshot.swaraYoga.sunriseSwara,
            auspiciousness = Auspiciousness.AUSPICIOUS,
            isCurrent = kotlin.math.abs(nowMs - snapshot.sunriseMs) <= 10 * 60_000L,
            isCompleted = nowMs > snapshot.sunriseMs + 10 * 60_000L
        )

        events += ScheduleEvent(
            id = "astro_sunset_${snapshot.dateIso}",
            category = ScheduleCategory.ASTRONOMICAL,
            titleEn = "Sunset (Sūryāsta)",
            titleHi = "सूर्यास्त (रात्रि आरम्भ)",
            subtitleEn = "Sandhya evening transition · Night Horas begin",
            subtitleHi = "सायं सन्ध्या काल · रात्रि होरा प्रारम्भ",
            startTimeMs = snapshot.sunsetMs,
            endTimeMs = snapshot.nextSunriseMs,
            formattedStart = fmt(snapshot.sunsetMs),
            formattedEnd = fmt(snapshot.nextSunriseMs),
            formattedRange = fmtRange(snapshot.sunsetMs, snapshot.nextSunriseMs),
            primaryNadi = snapshot.swaraYoga.sunsetSwara,
            auspiciousness = Auspiciousness.NEUTRAL,
            isCurrent = kotlin.math.abs(nowMs - snapshot.sunsetMs) <= 10 * 60_000L,
            isCompleted = nowMs > snapshot.sunsetMs + 10 * 60_000L
        )

        // 2. Key Muhurtas & Inauspicious Kalas
        fun addInterval(
            id: String,
            category: ScheduleCategory,
            titleEn: String,
            titleHi: String,
            subtitleEn: String,
            subtitleHi: String,
            interval: TimingInterval,
            auspiciousness: Auspiciousness
        ) {
            val s = interval.startTimeMs ?: return
            val e = interval.endTimeMs ?: (s + 48 * 60_000L)
            events += ScheduleEvent(
                id = id,
                category = category,
                titleEn = titleEn,
                titleHi = titleHi,
                subtitleEn = subtitleEn,
                subtitleHi = subtitleHi,
                startTimeMs = s,
                endTimeMs = e,
                formattedStart = fmt(s),
                formattedEnd = fmt(e),
                formattedRange = fmtRange(s, e),
                auspiciousness = auspiciousness,
                isCurrent = nowMs in s until e,
                isCompleted = nowMs >= e
            )
        }

        addInterval(
            id = "muhurta_brahma_${snapshot.dateIso}",
            category = ScheduleCategory.MUHURTA,
            titleEn = "Brahma Muhūrta",
            titleHi = "ब्रह्म मुहूर्त",
            subtitleEn = "Supreme pre-dawn window for meditation & study",
            subtitleHi = "ध्यान, साधना एवं स्वाध्याय हेतु सर्वोत्तम प्रातः काल",
            interval = snapshot.brahmaMuhurta,
            auspiciousness = Auspiciousness.AUSPICIOUS
        )

        addInterval(
            id = "muhurta_abhijit_${snapshot.dateIso}",
            category = ScheduleCategory.MUHURTA,
            titleEn = "Abhijit Muhūrta",
            titleHi = "अभिजित मुहूर्त",
            subtitleEn = "Victorious midday window for auspicious beginnings",
            subtitleHi = "शुभ कार्यों के प्रारम्भ हेतु सर्वश्रेष्ठ मध्याह्न मुहूर्त",
            interval = snapshot.abhijitMuhurta,
            auspiciousness = Auspiciousness.AUSPICIOUS
        )

        snapshot.amritaKala.forEachIndexed { idx, interval ->
            addInterval(
                id = "muhurta_amrita_${idx}_${snapshot.dateIso}",
                category = ScheduleCategory.MUHURTA,
                titleEn = "Amṛta Kāla",
                titleHi = "अमृत काल",
                subtitleEn = "Nectarine auspicious Nakshatra window",
                subtitleHi = "नक्षत्र आधारित अत्यंत शुभ अमृत काल",
                interval = interval,
                auspiciousness = Auspiciousness.AUSPICIOUS
            )
        }

        addInterval(
            id = "muhurta_rahu_${snapshot.dateIso}",
            category = ScheduleCategory.MUHURTA,
            titleEn = "Rāhu Kāla",
            titleHi = "राहु काल",
            subtitleEn = "Avoid starting important new ventures",
            subtitleHi = "महत्वपूर्ण नए कार्यों का शुभारम्भ टालें",
            interval = snapshot.rahuKala,
            auspiciousness = Auspiciousness.INAUSPICIOUS
        )

        addInterval(
            id = "muhurta_yama_${snapshot.dateIso}",
            category = ScheduleCategory.MUHURTA,
            titleEn = "Yamagaṇḍa",
            titleHi = "यमगण्ड काल",
            subtitleEn = "Inauspicious period for new beginnings",
            subtitleHi = "नए शुभ कार्यों हेतु वर्जित काल",
            interval = snapshot.yamaganda,
            auspiciousness = Auspiciousness.INAUSPICIOUS
        )

        addInterval(
            id = "muhurta_gulika_${snapshot.dateIso}",
            category = ScheduleCategory.MUHURTA,
            titleEn = "Gulika Kāla",
            titleHi = "गुलिक काल",
            subtitleEn = "Saturn-ruled period — activities tend to repeat",
            subtitleHi = "शनि शासित काल — सावधानी अपेक्षित",
            interval = snapshot.gulikaKala,
            auspiciousness = Auspiciousness.INAUSPICIOUS
        )

        snapshot.durmuhurta.forEachIndexed { idx, interval ->
            addInterval(
                id = "muhurta_dur_${idx}_${snapshot.dateIso}",
                category = ScheduleCategory.MUHURTA,
                titleEn = "Durmuhūrta",
                titleHi = "दुर्मुहूर्त",
                subtitleEn = "Inauspicious Vedic slot",
                subtitleHi = "अशुभ मुहूर्त अवधि",
                interval = interval,
                auspiciousness = Auspiciousness.INAUSPICIOUS
            )
        }

        snapshot.varjyam.forEachIndexed { idx, interval ->
            addInterval(
                id = "muhurta_varjyam_${idx}_${snapshot.dateIso}",
                category = ScheduleCategory.MUHURTA,
                titleEn = "Varjyam (Tyājya)",
                titleHi = "वर्ज्यम् (त्याज्य काल)",
                subtitleEn = "Avoid initiating auspicious ceremonies",
                subtitleHi = "शुभ संस्कार व नए कार्य वर्जित",
                interval = interval,
                auspiciousness = Auspiciousness.INAUSPICIOUS
            )
        }

        // 3. Day & Night Choghadiyas
        val allChoghadiya = snapshot.gauriChoghadiyaDay.map { true to it } +
            snapshot.gauriChoghadiyaNight.map { false to it }
        allChoghadiya.forEachIndexed { idx, (isDay, chog) ->
            val rawName = chog.name ?: "Choghadiya"
            val hiName = when {
                rawName.contains("Amrita", true) -> "अमृत"
                rawName.contains("Shubha", true) -> "शुभ"
                rawName.contains("Labha", true) -> "लाभ"
                rawName.contains("Chara", true) -> "चर"
                rawName.contains("Udvega", true) -> "उद्वेग"
                rawName.contains("Roga", true) -> "रोग"
                rawName.contains("Kala", true) -> "काल"
                else -> rawName
            }
            addInterval(
                id = "chog_${idx}_${snapshot.dateIso}",
                category = ScheduleCategory.CHOGHADIYA,
                titleEn = "$rawName Choghadiya",
                titleHi = "$hiName चौघड़िया",
                subtitleEn = if (isDay) "Day Choghadiya" else "Night Choghadiya",
                subtitleHi = if (isDay) "दिन का चौघड़िया" else "रात्रि का चौघड़िया",
                interval = chog,
                auspiciousness = if (chog.isAuspicious) Auspiciousness.AUSPICIOUS else Auspiciousness.INAUSPICIOUS
            )
        }

        // 4. Panchanga Anga segments (Tithi, Nakshatra) with full Start/End & Boundary Nadis
        snapshot.tithi.forEachIndexed { idx, seg ->
            val sMs = seg.startTimeMs ?: snapshot.sunriseMs
            val eMs = seg.endTimeMs ?: snapshot.nextSunriseMs
            events += ScheduleEvent(
                id = "anga_tithi_${idx}_${snapshot.dateIso}",
                category = ScheduleCategory.PANCHANGA,
                titleEn = "Tithi: ${seg.name}",
                titleHi = "तिथि: ${seg.name}",
                subtitleEn = if (seg.endTimeMs != null) "Active lunar day period" else "Continues beyond next sunrise",
                subtitleHi = if (seg.endTimeMs != null) "सक्रिय तिथि अवधि" else "अगले सूर्योदय के बाद भी जारी",
                startTimeMs = sMs,
                endTimeMs = seg.endTimeMs,
                formattedStart = fmt(sMs),
                formattedEnd = if (seg.endTimeMs != null) fmt(eMs) else "${fmt(snapshot.nextSunriseMs)}+",
                formattedRange = if (seg.endTimeMs != null) fmtRange(sMs, eMs) else "${fmt(sMs)} → ${fmt(snapshot.nextSunriseMs)}+",
                primaryNadi = seg.swaraInfo?.startNadi,
                secondaryNadi = seg.swaraInfo?.endNadi,
                auspiciousness = Auspiciousness.AUSPICIOUS,
                isCurrent = nowMs in sMs until eMs,
                isCompleted = seg.endTimeMs != null && nowMs >= eMs
            )
        }

        snapshot.nakshatra.forEachIndexed { idx, seg ->
            val sMs = seg.startTimeMs ?: snapshot.sunriseMs
            val eMs = seg.endTimeMs ?: snapshot.nextSunriseMs
            events += ScheduleEvent(
                id = "anga_nak_${idx}_${snapshot.dateIso}",
                category = ScheduleCategory.PANCHANGA,
                titleEn = "Nakṣatra: ${seg.name}",
                titleHi = "नक्षत्र: ${seg.name}",
                subtitleEn = if (seg.endTimeMs != null) "Active lunar mansion period" else "Continues beyond next sunrise",
                subtitleHi = if (seg.endTimeMs != null) "सक्रिय नक्षत्र अवधि" else "अगले सूर्योदय के बाद भी जारी",
                startTimeMs = sMs,
                endTimeMs = seg.endTimeMs,
                formattedStart = fmt(sMs),
                formattedEnd = if (seg.endTimeMs != null) fmt(eMs) else "${fmt(snapshot.nextSunriseMs)}+",
                formattedRange = if (seg.endTimeMs != null) fmtRange(sMs, eMs) else "${fmt(sMs)} → ${fmt(snapshot.nextSunriseMs)}+",
                primaryNadi = seg.swaraInfo?.startNadi,
                secondaryNadi = seg.swaraInfo?.endNadi,
                auspiciousness = Auspiciousness.AUSPICIOUS,
                isCurrent = nowMs in sMs until eMs,
                isCompleted = seg.endTimeMs != null && nowMs >= eMs
            )
        }

        // 5. Swara Yoga Celestial Windows
        fun addSwaraWindow(id: String, win: SwaraWindow?, titleHi: String) {
            win ?: return
            val s = win.startTimeMs ?: return
            val e = win.endTimeMs ?: (s + 3_600_000L)
            events += ScheduleEvent(
                id = id,
                category = ScheduleCategory.SWARA,
                titleEn = win.label,
                titleHi = titleHi,
                subtitleEn = win.ruleDescription,
                subtitleHi = win.ruleDescription,
                startTimeMs = s,
                endTimeMs = e,
                formattedStart = fmt(s),
                formattedEnd = fmt(e),
                formattedRange = fmtRange(s, e),
                primaryNadi = win.nadi,
                auspiciousness = if (win.nadi == SwaraNadi.SUSHUMNA) Auspiciousness.NEUTRAL else Auspiciousness.AUSPICIOUS,
                isCurrent = nowMs in s until e,
                isCompleted = nowMs >= e
            )
        }

        addSwaraWindow("swara_sr_${snapshot.dateIso}", snapshot.swaraYoga.sunriseWindow, "सूर्योदय स्वर")
        addSwaraWindow("swara_md_${snapshot.dateIso}", snapshot.swaraYoga.middayWindow, "मध्याह्न स्वर")
        addSwaraWindow("swara_ss_${snapshot.dateIso}", snapshot.swaraYoga.sunsetWindow, "सूर्यास्त स्वर")
        addSwaraWindow("swara_mr_${snapshot.dateIso}", snapshot.swaraYoga.moonriseWindow, "चन्द्रोदय स्वर")
        addSwaraWindow("swara_ms_${snapshot.dateIso}", snapshot.swaraYoga.moonsetWindow, "चन्द्रास्त स्वर")

        // 6. 24 Planetary Horas (with Nadi + active/leading Tattva)
        dailyHoras.horas.forEach { hora ->
            val rulerHi = when (hora.ruler) {
                "Sun" -> "सूर्य"
                "Moon" -> "चन्द्र"
                "Mars" -> "मङ्गल"
                "Mercury" -> "बुध"
                "Jupiter" -> "बृहस्पति (गुरु)"
                "Venus" -> "शुक्र"
                "Saturn" -> "शनि"
                else -> hora.ruler
            }
            val auspiciousness = when (hora.ruler) {
                "Jupiter", "Venus", "Mercury", "Moon" -> Auspiciousness.AUSPICIOUS
                "Saturn", "Mars" -> Auspiciousness.NEUTRAL
                else -> Auspiciousness.NEUTRAL
            }
            val isCur = nowMs in hora.startTimeMs until hora.endTimeMs
            val tattvaEl = if (isCur) {
                dailyHoras.activeTattva?.element ?: hora.tattvas.firstOrNull { nowMs in it.startTimeMs until it.endTimeMs }?.element
            } else {
                hora.tattvas.firstOrNull()?.element
            }
            events += ScheduleEvent(
                id = "hora_${hora.index}_${snapshot.dateIso}",
                category = ScheduleCategory.HORA,
                titleEn = "${hora.ruler} Hora (#${hora.index})",
                titleHi = "$rulerHi होरा (#${hora.index})",
                subtitleEn = "${if (hora.isDay) "Day" else "Night"} · ${hora.nadi.label} Nadi",
                subtitleHi = "${if (hora.isDay) "दिन" else "रात्रि"} · ${hora.nadi.sanskritName}",
                startTimeMs = hora.startTimeMs,
                endTimeMs = hora.endTimeMs,
                formattedStart = fmt(hora.startTimeMs),
                formattedEnd = fmt(hora.endTimeMs),
                formattedRange = fmtRange(hora.startTimeMs, hora.endTimeMs),
                primaryNadi = hora.nadi,
                tattvaElement = tattvaEl,
                auspiciousness = auspiciousness,
                isCurrent = isCur,
                isCompleted = nowMs >= hora.endTimeMs
            )
        }

        return events.sortedWith(compareBy<ScheduleEvent> { it.startTimeMs }.thenBy { it.category.ordinal })
    }
}
