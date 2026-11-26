package com.mahavtaar.panchanga.widget

import android.content.Context
import com.mahavtaar.panchanga.astronomy.DrikPanchangaEngine
import com.mahavtaar.panchanga.astronomy.HoraEngine
import com.mahavtaar.panchanga.astronomy.SwaraYogaEngine
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.preferences.UserPreferencesRepositoryImpl
import com.mahavtaar.panchanga.domain.model.AngaSegment
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.WidgetGlassStyle
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.localization.stringsFor
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.format.DateTimeFormatter

internal data class WidgetMuhurtaItem(
    val name: String,
    val range: String,
    val isAuspicious: Boolean,
    val startMs: Long?,
    val endMs: Long?,
    val progress: Float
)

internal data class WidgetChoghadiyaItem(
    val name: String,
    val range: String,
    val isAuspicious: Boolean,
    val startMs: Long?,
    val endMs: Long?,
    val progress: Float
)

internal data class WidgetHoraItem(
    val ruler: String,
    val dayNight: String,
    val range: String,
    val nextRuler: String,
    val startMs: Long,
    val endMs: Long,
    val progress: Float
)

internal data class WidgetTattvaItem(
    val name: String,
    val element: TattvaElement,
    val range: String,
    val startMs: Long,
    val endMs: Long,
    val progress: Float
)

internal data class WidgetSwaraAlertItem(
    val title: String,
    val nadiShort: String,
    val range: String,
    val isIda: Boolean
)

/**
 * Rich, structured snapshot of all currently running live Vedic events
 * for the Home-Screen Glance widgets and automatic boundary scheduler.
 */
internal data class LiveRunningWidgetModel(
    val isHindi: Boolean,
    val nowMs: Long,
    val headerTitle: String,
    val city: String,
    val updatedClock: String,
    val masaPakshaVaara: String,
    val activeTithiLine: String,
    val sunriseTithiLine: String,
    val activeNakshatraLine: String,
    val activeYogaKaranaLine: String,
    val sunMoonLine: String,
    val activeMuhurtasLine: String,
    val isMuhurtaInauspicious: Boolean,
    val activeChoghadiyaLine: String,
    val activeHoraLine: String,
    val activeTattvaLine: String,
    val activeNadiLine: String,
    val activeSwaraWindowsLine: String,
    // Structured Bento fields
    val tithiName: String,
    val tithiEndShort: String,
    val tithiProgress: Float,
    val sunriseTithiShort: String,
    val nakshatraName: String,
    val nakshatraEndShort: String,
    val nakshatraProgress: Float,
    val yogaName: String,
    val karanaName: String,
    val sunrise: String,
    val sunset: String,
    val moonrise: String,
    val moonset: String,
    val activeMuhurtas: List<WidgetMuhurtaItem>,
    val activeChoghadiya: WidgetChoghadiyaItem?,
    val activeHora: WidgetHoraItem?,
    val activeTattva: WidgetTattvaItem?,
    val flowingNadi: SwaraNadi,
    val nadiShort: String,
    val nadiFull: String,
    val sunriseSwaraShort: String,
    val sunsetSwaraShort: String,
    val swaraAlerts: List<WidgetSwaraAlertItem>,
    val nextTransitionAtMs: Long,
    val widgetGlassStyle: WidgetGlassStyle = WidgetGlassStyle.AUTO,
    val widgetOpacity: Float = UserPreferences.DEFAULT_WIDGET_OPACITY
)

internal object LiveWidgetDataResolver {
    private val timeFormatter12h = DateTimeFormatter.ofPattern("hh:mm:ss a")

    private fun resolveActiveAnga(segments: List<AngaSegment>, nowMs: Long): AngaSegment? {
        if (segments.isEmpty()) return null
        return segments.firstOrNull { seg ->
            val s = seg.startTimeMs ?: Long.MIN_VALUE
            val e = seg.endTimeMs ?: Long.MAX_VALUE
            nowMs in s..e
        } ?: segments.first()
    }

    private fun inWindow(interval: TimingInterval?, nowMs: Long): Boolean {
        val s = interval?.startTimeMs ?: return false
        val e = interval.endTimeMs ?: return false
        return nowMs in s..e
    }

    private fun computeProgress(startMs: Long?, endMs: Long?, nowMs: Long): Float {
        if (startMs == null || endMs == null || endMs <= startMs) return 0.5f
        return ((nowMs - startMs).toFloat() / (endMs - startMs).toFloat()).coerceIn(0.03f, 1f)
    }

    suspend fun resolve(context: Context): LiveRunningWidgetModel {
        val prefs = runCatching {
            UserPreferencesRepositoryImpl(context.applicationContext).preferences.firstOrNull()
        }.getOrNull() ?: UserPreferences()

        val lang = prefs.language
        val isHi = lang == AppLanguage.HINDI
        val strings = stringsFor(lang)
        val nowMs = System.currentTimeMillis()
        val zone = TimeFormatters.safeZoneId(prefs.selectedLocation.timezone)
        val zonedNow = Instant.ofEpochMilli(nowMs).atZone(zone)
        val today = zonedNow.toLocalDate()
        val updatedClock = runCatching { zonedNow.format(timeFormatter12h) }.getOrDefault("")

        val engine = DrikPanchangaEngine()
        val snap = engine.computePanchanga(
            location = prefs.selectedLocation,
            date = today,
            monthSystem = prefs.monthSystem,
            coordinateSelection = prefs.ayanamsha,
            includeExtendedTransits = false
        )

        val horaBaseSnap = if (nowMs < snap.sunriseMs) {
            runCatching {
                engine.computePanchanga(
                    location = prefs.selectedLocation,
                    date = today.minusDays(1),
                    monthSystem = prefs.monthSystem,
                    coordinateSelection = prefs.ayanamsha,
                    includeExtendedTransits = false
                )
            }.getOrDefault(snap)
        } else {
            snap
        }

        val horas = HoraEngine.resolveForSnapshot(horaBaseSnap, nowMs)
        val activeHoraPeriod = horas.activeHora ?: horas.horas.firstOrNull()
        val activeHoraIndex = activeHoraPeriod?.let { p -> horas.horas.indexOfFirst { it.index == p.index } } ?: -1
        val nextHoraPeriod = if (activeHoraIndex >= 0 && activeHoraIndex + 1 < horas.horas.size) {
            horas.horas[activeHoraIndex + 1]
        } else null
        val activeTattvaPeriod = horas.activeTattva ?: activeHoraPeriod?.tattvas?.firstOrNull()

        val activeTithi = resolveActiveAnga(snap.tithi, nowMs)
        val sunriseTithi = snap.tithi.firstOrNull()
        val activeNak = resolveActiveAnga(snap.nakshatra, nowMs)
        val activeYoga = resolveActiveAnga(snap.yoga, nowMs)
        val activeKarana = resolveActiveAnga(snap.karana, nowMs)

        val localizedMasa = VedicLocalizer.masaName(snap.masaNumber, snap.isAdhika, snap.isKshaya, snap.masa, lang)
        val localizedPaksha = VedicLocalizer.pakshaName(snap.paksha, lang)
        val localizedVaara = VedicLocalizer.vaaraName(snap.weekday, snap.vaara, lang)

        val tithiName = activeTithi?.let { VedicLocalizer.tithiName(it.number, it.name, lang) } ?: strings.tithiLabel
        val tithiEndShort = activeTithi?.endTimeMs?.let { endMs ->
            val dt = VedicLocalizer.formatDateTimeWithSeconds(endMs, snap.timezone, lang)
            if (isHi) "$dt तक" else "till $dt"
        } ?: activeTithi?.ends?.let { if (isHi) "$it तक" else "till $it" } ?: strings.fullDay
        val activeTithiLine = "$tithiName ($tithiEndShort)".trim()
        val tithiProgress = computeProgress(activeTithi?.startTimeMs, activeTithi?.endTimeMs, nowMs)

        val sunriseTithiName = sunriseTithi?.let { VedicLocalizer.tithiName(it.number, it.name, lang) } ?: tithiName
        val sunriseTithiLine = if (isHi) "सूर्योदय तिथि: $sunriseTithiName" else "Sunrise Tithi: $sunriseTithiName"

        val nakName = activeNak?.let { VedicLocalizer.nakshatraName(it.number, it.name, lang) } ?: strings.nakshatraLabel
        val nakEndShort = activeNak?.endTimeMs?.let { endMs ->
            val dt = VedicLocalizer.formatDateTimeWithSeconds(endMs, snap.timezone, lang)
            if (isHi) "$dt तक" else "till $dt"
        } ?: activeNak?.ends?.let { if (isHi) "$it तक" else "till $it" } ?: strings.fullDay
        val activeNakshatraLine = "$nakName ($nakEndShort)".trim()
        val nakProgress = computeProgress(activeNak?.startTimeMs, activeNak?.endTimeMs, nowMs)

        val yogaName = activeYoga?.let { VedicLocalizer.yogaName(it.number, it.name, lang) } ?: "—"
        val karanaName = activeKarana?.let { VedicLocalizer.karanaName(it.name, lang) } ?: "—"
        val activeYogaKaranaLine = if (isHi) {
            "योग: $yogaName · करण: $karanaName"
        } else {
            "Yoga: $yogaName · Karana: $karanaName"
        }

        // Collect all running Muhurtas (Auspicious + Inauspicious)
        val muhurtaItems = mutableListOf<WidgetMuhurtaItem>()
        val allTrackedIntervals = mutableListOf<TimingInterval>()

        fun checkMuhurta(interval: TimingInterval?, localizedTitle: String, auspicious: Boolean) {
            if (interval == null) return
            allTrackedIntervals += interval
            if (inWindow(interval, nowMs)) {
                val rangeStr = if (interval.startTimeMs != null && interval.endTimeMs != null) {
                    VedicLocalizer.formatDateTimeRangeWithSeconds(interval.startTimeMs!!, interval.endTimeMs!!, snap.timezone, lang)
                } else {
                    "${interval.start}–${interval.end}"
                }
                muhurtaItems += WidgetMuhurtaItem(
                    name = localizedTitle,
                    range = rangeStr,
                    isAuspicious = auspicious,
                    startMs = interval.startTimeMs,
                    endMs = interval.endTimeMs,
                    progress = computeProgress(interval.startTimeMs, interval.endTimeMs, nowMs)
                )
            }
        }

        checkMuhurta(snap.rahuKala, VedicLocalizer.muhurtaOrPeriodName("Rāhu Kāla", lang), false)
        checkMuhurta(snap.yamaganda, VedicLocalizer.muhurtaOrPeriodName("Yamagaṇḍa", lang), false)
        checkMuhurta(snap.gulikaKala, VedicLocalizer.muhurtaOrPeriodName("Gulika Kāla", lang), false)
        snap.durmuhurta.forEach { checkMuhurta(it, VedicLocalizer.muhurtaOrPeriodName("Durmuhūrta", lang), false) }
        snap.varjyam.forEach { checkMuhurta(it, VedicLocalizer.muhurtaOrPeriodName("Varjyam", lang), false) }
        checkMuhurta(snap.abhijitMuhurta, VedicLocalizer.muhurtaOrPeriodName("Abhijit Muhūrta", lang), true)
        checkMuhurta(snap.brahmaMuhurta, VedicLocalizer.muhurtaOrPeriodName("Brahma Muhūrta", lang), true)
        snap.amritaKala.forEach { checkMuhurta(it, VedicLocalizer.muhurtaOrPeriodName("Amṛta Kāla", lang), true) }
        checkMuhurta(snap.vijayaMuhurta, if (isHi) "विजय मुहूर्त" else "Vijaya Muhūrta", true)
        checkMuhurta(snap.godhuliMuhurta, if (isHi) "गोधूलि मुहूर्त" else "Godhūli Muhūrta", true)

        val hasInauspicious = muhurtaItems.any { !it.isAuspicious }
        val activeMuhurtasLine = if (muhurtaItems.isNotEmpty()) {
            muhurtaItems.joinToString(" · ") { item ->
                "${if (item.isAuspicious) "✨" else "⚠"} ${item.name} (${item.range})"
            }
        } else {
            if (isHi) "🟢 अभी कोई राहु/अशुभ काल सक्रिय नहीं है" else "🟢 Clear Window · No Rahu/Inauspicious Kala"
        }

        // Active Choghadiya
        val allChog = horaBaseSnap.gauriChoghadiyaDay + horaBaseSnap.gauriChoghadiyaNight +
            snap.gauriChoghadiyaDay + snap.gauriChoghadiyaNight
        val activeChogRaw = allChog.firstOrNull { inWindow(it, nowMs) }
        val activeChoghadiya = activeChogRaw?.let { c ->
            val rangeStr = if (c.startTimeMs != null && c.endTimeMs != null) {
                VedicLocalizer.formatDateTimeRangeWithSeconds(c.startTimeMs!!, c.endTimeMs!!, snap.timezone, lang)
            } else {
                "${c.start}–${c.end}"
            }
            WidgetChoghadiyaItem(
                name = VedicLocalizer.muhurtaOrPeriodName(c.name ?: "", lang),
                range = rangeStr,
                isAuspicious = c.isAuspicious,
                startMs = c.startTimeMs,
                endMs = c.endTimeMs,
                progress = computeProgress(c.startTimeMs, c.endTimeMs, nowMs)
            )
        }
        val activeChoghadiyaLine = if (activeChoghadiya != null) {
            val prefix = if (isHi) "चौघड़िया" else "Choghadiya"
            "$prefix: ${activeChoghadiya.name} (${activeChoghadiya.range})"
        } else {
            if (isHi) "सूर्योदय: ${snap.sunrise} · सूर्यास्त: ${snap.sunset}" else "Sunrise: ${snap.sunrise} · Sunset: ${snap.sunset}"
        }

        // Active Hora
        val activeHora = activeHoraPeriod?.let { hp ->
            val ruler = VedicLocalizer.horaRulerName(hp.ruler, lang)
            val nextRuler = nextHoraPeriod?.let { VedicLocalizer.horaRulerName(it.ruler, lang) } ?: ""
            val dayNight = if (hp.isDay) {
                if (isHi) "दिन" else "Day"
            } else {
                if (isHi) "रात्रि" else "Night"
            }
            WidgetHoraItem(
                ruler = ruler,
                dayNight = dayNight,
                range = VedicLocalizer.formatDateTimeRangeWithSeconds(hp.startTimeMs, hp.endTimeMs, snap.timezone, lang),
                nextRuler = nextRuler,
                startMs = hp.startTimeMs,
                endMs = hp.endTimeMs,
                progress = computeProgress(hp.startTimeMs, hp.endTimeMs, nowMs)
            )
        }
        val activeHoraLine = activeHora?.let { h ->
            if (isHi) "होरा: ${h.ruler} (${h.dayNight}) · ${h.range}"
            else "Hora: ${h.ruler} (${h.dayNight}) · ${h.range}"
        } ?: ""

        // Active 5-Sub-Tattva
        val activeTattva = activeTattvaPeriod?.let { tp ->
            WidgetTattvaItem(
                name = VedicLocalizer.tattvaName(tp.element, lang),
                element = tp.element,
                range = VedicLocalizer.formatDateTimeRangeWithSeconds(tp.startTimeMs, tp.endTimeMs, snap.timezone, lang),
                startMs = tp.startTimeMs,
                endMs = tp.endTimeMs,
                progress = computeProgress(tp.startTimeMs, tp.endTimeMs, nowMs)
            )
        }
        val activeTattvaLine = activeTattva?.let { t ->
            if (isHi) "सक्रिय तत्त्व: ${t.name} (${t.range})"
            else "Active Tattva: ${t.name} (${t.range})"
        } ?: ""

        // Swara Yoga & Flowing Nadi
        val currentMins = zonedNow.hour * 60.0 + zonedNow.minute + (zonedNow.second / 60.0)
        val swaraState = SwaraYogaEngine.computeSwaraYoga(
            tithiNumber = sunriseTithi?.number ?: 1,
            sunriseStr = snap.sunrise,
            sunsetStr = snap.sunset,
            currentLocalMinutes = currentMins,
            moonriseStr = snap.moonrise,
            moonsetStr = snap.moonset,
            sunriseMs = snap.sunriseMs,
            sunsetMs = snap.sunsetMs,
            moonriseMs = snap.moonriseMs,
            moonsetMs = snap.moonsetMs,
            timeZoneId = snap.timezone,
            nowMs = nowMs
        )

        val flowingNadi = activeHoraPeriod?.nadi ?: swaraState.currentActiveSwara
        val nadiShort = VedicLocalizer.nadiShortLabel(flowingNadi, lang)
        val nadiFull = when (flowingNadi) {
            SwaraNadi.IDA -> if (isHi) "इड़ा (वाम / चन्द्र स्वर)" else "Ida (Left / Lunar)"
            SwaraNadi.PINGALA -> if (isHi) "पिङ्गला (दक्षिण / सूर्य स्वर)" else "Pingala (Right / Solar)"
            SwaraNadi.SUSHUMNA -> if (isHi) "सुषुम्ना (मध्य स्वर)" else "Sushumna (Center)"
        }
        val activeNadiLine = if (isHi) "प्रवहमान नाड़ी: $nadiFull" else "Flowing Nadi: $nadiFull"

        // Active Celestial / Tithi / Nakshatra Swara boundary windows
        fun formatSwaraWindowRange(startMs: Long?, endMs: Long?, fallback: String): String =
            if (startMs != null && endMs != null) {
                VedicLocalizer.formatDateTimeRangeWithSeconds(startMs, endMs, snap.timezone, lang)
            } else {
                fallback
            }

        val swaraAlerts = mutableListOf<WidgetSwaraAlertItem>()
        when (swaraState.activeCelestialWindow) {
            "sunrise" -> swaraAlerts += WidgetSwaraAlertItem(
                title = if (isHi) "सूर्योदय स्वर काल" else "Sunrise Swara",
                nadiShort = VedicLocalizer.nadiShortLabel(swaraState.sunriseWindow.nadi, lang),
                range = formatSwaraWindowRange(swaraState.sunriseWindow.startTimeMs, swaraState.sunriseWindow.endTimeMs, swaraState.sunriseWindow.windowFormatted),
                isIda = swaraState.sunriseWindow.nadi == SwaraNadi.IDA
            )
            "midday" -> swaraAlerts += WidgetSwaraAlertItem(
                title = if (isHi) "मध्याह्न स्वर काल" else "Midday Swara",
                nadiShort = VedicLocalizer.nadiShortLabel(swaraState.middayWindow.nadi, lang),
                range = formatSwaraWindowRange(swaraState.middayWindow.startTimeMs, swaraState.middayWindow.endTimeMs, swaraState.middayWindow.windowFormatted),
                isIda = swaraState.middayWindow.nadi == SwaraNadi.IDA
            )
            "sunset" -> swaraAlerts += WidgetSwaraAlertItem(
                title = if (isHi) "सूर्यास्त स्वर काल" else "Sunset Swara",
                nadiShort = VedicLocalizer.nadiShortLabel(swaraState.sunsetWindow.nadi, lang),
                range = formatSwaraWindowRange(swaraState.sunsetWindow.startTimeMs, swaraState.sunsetWindow.endTimeMs, swaraState.sunsetWindow.windowFormatted),
                isIda = swaraState.sunsetWindow.nadi == SwaraNadi.IDA
            )
            "moonrise" -> swaraState.moonriseWindow?.let { w ->
                swaraAlerts += WidgetSwaraAlertItem(
                    title = if (isHi) "चन्द्रोदय स्वर काल" else "Moonrise Swara",
                    nadiShort = VedicLocalizer.nadiShortLabel(w.nadi, lang),
                    range = formatSwaraWindowRange(w.startTimeMs, w.endTimeMs, w.windowFormatted),
                    isIda = w.nadi == SwaraNadi.IDA
                )
            }
            "moonset" -> swaraState.moonsetWindow?.let { w ->
                swaraAlerts += WidgetSwaraAlertItem(
                    title = if (isHi) "चन्द्रास्त स्वर काल" else "Moonset Swara",
                    nadiShort = VedicLocalizer.nadiShortLabel(w.nadi, lang),
                    range = formatSwaraWindowRange(w.startTimeMs, w.endTimeMs, w.windowFormatted),
                    isIda = w.nadi == SwaraNadi.IDA
                )
            }
        }

        val boundaryCandidates = mutableListOf<Long>()
        for (seg in snap.tithi) {
            val info = SwaraYogaEngine.computeTithiSwaraEvents(seg, snap.timezone)
            info.startEvent?.let { ev ->
                if (nowMs in ev.startTimeMs..ev.endTimeMs) {
                    swaraAlerts += WidgetSwaraAlertItem(
                        title = if (isHi) "तिथि आरंभ स्वर" else "Tithi Start Swara",
                        nadiShort = VedicLocalizer.nadiShortLabel(ev.nadi, lang),
                        range = VedicLocalizer.formatDateTimeRangeWithSeconds(ev.startTimeMs, ev.endTimeMs, snap.timezone, lang),
                        isIda = ev.nadi == SwaraNadi.IDA
                    )
                }
                if (ev.startTimeMs > nowMs) boundaryCandidates += ev.startTimeMs
                if (ev.endTimeMs > nowMs) boundaryCandidates += ev.endTimeMs
            }
            info.endEvent?.let { ev ->
                if (nowMs in ev.startTimeMs..ev.endTimeMs) {
                    swaraAlerts += WidgetSwaraAlertItem(
                        title = if (isHi) "तिथि समापन स्वर" else "Tithi End Swara",
                        nadiShort = VedicLocalizer.nadiShortLabel(ev.nadi, lang),
                        range = VedicLocalizer.formatDateTimeRangeWithSeconds(ev.startTimeMs, ev.endTimeMs, snap.timezone, lang),
                        isIda = ev.nadi == SwaraNadi.IDA
                    )
                }
                if (ev.startTimeMs > nowMs) boundaryCandidates += ev.startTimeMs
                if (ev.endTimeMs > nowMs) boundaryCandidates += ev.endTimeMs
            }
        }
        for (seg in snap.nakshatra) {
            val info = SwaraYogaEngine.computeNakshatraSwaraEvents(seg, snap.timezone)
            info.startEvent?.let { ev ->
                if (nowMs in ev.startTimeMs..ev.endTimeMs) {
                    swaraAlerts += WidgetSwaraAlertItem(
                        title = if (isHi) "नक्षत्र आरंभ स्वर" else "Nakshatra Start Swara",
                        nadiShort = VedicLocalizer.nadiShortLabel(ev.nadi, lang),
                        range = VedicLocalizer.formatDateTimeRangeWithSeconds(ev.startTimeMs, ev.endTimeMs, snap.timezone, lang),
                        isIda = ev.nadi == SwaraNadi.IDA
                    )
                }
                if (ev.startTimeMs > nowMs) boundaryCandidates += ev.startTimeMs
                if (ev.endTimeMs > nowMs) boundaryCandidates += ev.endTimeMs
            }
            info.endEvent?.let { ev ->
                if (nowMs in ev.startTimeMs..ev.endTimeMs) {
                    swaraAlerts += WidgetSwaraAlertItem(
                        title = if (isHi) "नक्षत्र समापन स्वर" else "Nakshatra End Swara",
                        nadiShort = VedicLocalizer.nadiShortLabel(ev.nadi, lang),
                        range = VedicLocalizer.formatDateTimeRangeWithSeconds(ev.startTimeMs, ev.endTimeMs, snap.timezone, lang),
                        isIda = ev.nadi == SwaraNadi.IDA
                    )
                }
                if (ev.startTimeMs > nowMs) boundaryCandidates += ev.startTimeMs
                if (ev.endTimeMs > nowMs) boundaryCandidates += ev.endTimeMs
            }
        }

        val srSwaraShort = VedicLocalizer.nadiShortLabel(swaraState.sunriseSwara, lang)
        val ssSwaraShort = VedicLocalizer.nadiShortLabel(swaraState.sunsetSwara, lang)
        val activeSwaraWindowsLine = if (swaraAlerts.isNotEmpty()) {
            swaraAlerts.joinToString(" · ") { "${it.title} (${it.range})" }
        } else {
            if (isHi) "उदय स्वर: $srSwaraShort · अस्त स्वर: $ssSwaraShort"
            else "Sunrise Swara: $srSwaraShort · Sunset Swara: $ssSwaraShort"
        }

        // Gather all future Vedic transition boundaries for automatic zero-touch refresh
        activeTattva?.endMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
        activeHora?.endMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
        activeChoghadiya?.endMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
        activeTithi?.endTimeMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
        activeNak?.endTimeMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
        activeYoga?.endTimeMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
        activeKarana?.endTimeMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
        allTrackedIntervals.forEach { iv ->
            iv.startTimeMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
            iv.endTimeMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
        }
        allChog.forEach { iv ->
            iv.startTimeMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
            iv.endTimeMs?.takeIf { it > nowMs }?.let { boundaryCandidates += it }
        }

        val nextTransitionAtMs = boundaryCandidates.minOrNull() ?: (nowMs + 5 * 60_000L)

        return LiveRunningWidgetModel(
            isHindi = isHi,
            nowMs = nowMs,
            headerTitle = if (isHi) "अभी क्या चल रहा है" else "Running Right Now",
            city = snap.city,
            updatedClock = updatedClock,
            masaPakshaVaara = "$localizedMasa · $localizedPaksha · $localizedVaara",
            activeTithiLine = activeTithiLine,
            sunriseTithiLine = sunriseTithiLine,
            activeNakshatraLine = activeNakshatraLine,
            activeYogaKaranaLine = activeYogaKaranaLine,
            sunMoonLine = "☀ ${snap.sunrise}  ·  🌇 ${snap.sunset}",
            activeMuhurtasLine = activeMuhurtasLine,
            isMuhurtaInauspicious = hasInauspicious,
            activeChoghadiyaLine = activeChoghadiyaLine,
            activeHoraLine = activeHoraLine,
            activeTattvaLine = activeTattvaLine,
            activeNadiLine = activeNadiLine,
            activeSwaraWindowsLine = activeSwaraWindowsLine,
            tithiName = tithiName,
            tithiEndShort = tithiEndShort,
            tithiProgress = tithiProgress,
            sunriseTithiShort = sunriseTithiName,
            nakshatraName = nakName,
            nakshatraEndShort = nakEndShort,
            nakshatraProgress = nakProgress,
            yogaName = yogaName,
            karanaName = karanaName,
            sunrise = snap.sunrise,
            sunset = snap.sunset,
            moonrise = snap.moonrise ?: "—",
            moonset = snap.moonset ?: "—",
            activeMuhurtas = muhurtaItems,
            activeChoghadiya = activeChoghadiya,
            activeHora = activeHora,
            activeTattva = activeTattva,
            flowingNadi = flowingNadi,
            nadiShort = nadiShort,
            nadiFull = nadiFull,
            sunriseSwaraShort = srSwaraShort,
            sunsetSwaraShort = ssSwaraShort,
            swaraAlerts = swaraAlerts,
            nextTransitionAtMs = nextTransitionAtMs,
            widgetGlassStyle = prefs.widgetGlassStyle,
            widgetOpacity = prefs.widgetOpacity
        )
    }

    fun fallbackModel(): LiveRunningWidgetModel {
        val now = System.currentTimeMillis()
        return LiveRunningWidgetModel(
            isHindi = false,
            nowMs = now,
            headerTitle = "Running Right Now",
            city = "Mahavtaar Panchanga",
            updatedClock = "",
            masaPakshaVaara = "Tap to open live Panchanga",
            activeTithiLine = "Tap to refresh live Panchanga",
            sunriseTithiLine = "",
            activeNakshatraLine = "",
            activeYogaKaranaLine = "",
            sunMoonLine = "",
            activeMuhurtasLine = "Tap to load live Muhurtas",
            isMuhurtaInauspicious = false,
            activeChoghadiyaLine = "",
            activeHoraLine = "",
            activeTattvaLine = "",
            activeNadiLine = "",
            activeSwaraWindowsLine = "",
            tithiName = "Live Panchanga",
            tithiEndShort = "Syncing...",
            tithiProgress = 0.5f,
            sunriseTithiShort = "",
            nakshatraName = "Nakshatra",
            nakshatraEndShort = "",
            nakshatraProgress = 0.5f,
            yogaName = "—",
            karanaName = "—",
            sunrise = "—",
            sunset = "—",
            moonrise = "—",
            moonset = "—",
            activeMuhurtas = emptyList(),
            activeChoghadiya = null,
            activeHora = null,
            activeTattva = null,
            flowingNadi = SwaraNadi.IDA,
            nadiShort = "Ida",
            nadiFull = "Ida (Left / Lunar)",
            sunriseSwaraShort = "Ida",
            sunsetSwaraShort = "Pingala",
            swaraAlerts = emptyList(),
            nextTransitionAtMs = now + 5 * 60_000L,
            widgetGlassStyle = WidgetGlassStyle.AUTO,
            widgetOpacity = UserPreferences.DEFAULT_WIDGET_OPACITY
        )
    }
}
