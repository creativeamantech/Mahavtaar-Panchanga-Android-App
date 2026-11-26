package com.mahavtaar.panchanga.data.notifications

import com.mahavtaar.panchanga.astronomy.HoraEngine
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.core.VedicHindiNames
import com.mahavtaar.panchanga.domain.model.AngaSegment
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.TattvaElement
import java.util.Locale

data class RunningNowNotificationContent(
    val title: String,
    val collapsedText: String,
    val expandedBigText: String,
    val tattvaTransitionKey: String,
    val nextTransitionAtMs: Long
)

/**
 * Pure bilingual builder for the persistent "Running Now" priority status notification.
 * Formats active Hora, Sub-Tattva, Nadi (clean consistent naming), and Tithi with
 * separate Start and End timestamps including Date + Seconds.
 */
object RunningNowNotificationBuilder {

    fun build(
        todaySnapshot: PanchangaSnapshot,
        yesterdaySnapshot: PanchangaSnapshot?,
        nowMs: Long,
        language: AppLanguage
    ): RunningNowNotificationContent {
        val isHindi = language == AppLanguage.HINDI
        val tz = todaySnapshot.timezone

        val horaBaseSnap = if (nowMs < todaySnapshot.sunriseMs && yesterdaySnapshot != null) {
            yesterdaySnapshot
        } else {
            todaySnapshot
        }

        val dailyHoras = HoraEngine.resolveForSnapshot(horaBaseSnap, nowMs)
        val activeHora = dailyHoras.activeHora
            ?: dailyHoras.horas.firstOrNull { nowMs in it.startTimeMs..it.endTimeMs }
            ?: dailyHoras.horas.firstOrNull()
        val activeTattva = dailyHoras.activeTattva
            ?: activeHora?.tattvas?.firstOrNull { nowMs in it.startTimeMs..it.endTimeMs }
            ?: activeHora?.tattvas?.firstOrNull()
        val activeNadi = activeHora?.nadi ?: SwaraNadi.IDA

        val candidateTithis = buildList {
            if (nowMs < todaySnapshot.sunriseMs && yesterdaySnapshot != null) {
                addAll(yesterdaySnapshot.tithi)
            }
            addAll(todaySnapshot.tithi)
        }
        val activeTithi = resolveActiveSegment(candidateTithis, nowMs) ?: todaySnapshot.tithi.firstOrNull()

        val horaName = if (activeHora != null) {
            val ruler = if (isHindi) horaRulerHi(activeHora.ruler) else activeHora.ruler
            val dayNight = if (activeHora.isDay) {
                if (isHindi) "दिन" else "Day"
            } else {
                if (isHindi) "रात्रि" else "Night"
            }
            "$ruler ($dayNight)"
        } else {
            if (isHindi) "अज्ञात" else "Active"
        }

        val tattvaName = activeTattva?.let {
            if (isHindi) tattvaHi(it.element) else tattvaEn(it.element)
        } ?: if (isHindi) "तत्व" else "Tattva"

        val nadiName = if (isHindi) nadiLabelHi(activeNadi) else nadiLabelEn(activeNadi)

        val tithiName = activeTithi?.let { seg ->
            if (isHindi) VedicHindiNames.tithiHi(seg.number, seg.name) else seg.name
        } ?: if (isHindi) "तिथि" else "Tithi"

        val startLabel = if (isHindi) "आरम्भ" else "Start"
        val endLabel = if (isHindi) "समाप्ति" else "End"

        fun fmt(ms: Long): String = TimeFormatters.formatLocalizedDateTimeWithSeconds(ms, tz, isHindi)

        val horaStartStr = activeHora?.let { fmt(it.startTimeMs) } ?: "—"
        val horaEndStr = activeHora?.let { fmt(it.endTimeMs) } ?: "—"

        val tattvaStartStr = activeTattva?.let { fmt(it.startTimeMs) } ?: "—"
        val tattvaEndStr = activeTattva?.let { fmt(it.endTimeMs) } ?: "—"

        val tithiStartStr = activeTithi?.startTimeMs?.let(::fmt)
            ?: if (isHindi) "${fmt(horaBaseSnap.sunriseMs)} से पहले" else "Before ${fmt(horaBaseSnap.sunriseMs)}"
        val tithiEndStr = activeTithi?.endTimeMs?.let(::fmt)
            ?: if (isHindi) "${fmt(todaySnapshot.nextSunriseMs)} के बाद" else "After ${fmt(todaySnapshot.nextSunriseMs)}"

        val title = if (isHindi) {
            "अभी चल रहा है · ${activeHora?.let { horaRulerHi(it.ruler) } ?: "सक्रिय"} होरा · $tattvaName"
        } else {
            "Running Now · ${activeHora?.ruler ?: "Active"} Hora · $tattvaName"
        }

        val collapsedText = if (isHindi) {
            "$nadiName · $tithiName · तत्व $endLabel $tattvaEndStr"
        } else {
            "$nadiName · $tithiName · Tattva $endLabel $tattvaEndStr"
        }

        val expandedBigText = buildString {
            if (isHindi) {
                appendLine("होरा: $horaName")
                appendLine("$startLabel: $horaStartStr | $endLabel: $horaEndStr")
                appendLine("तत्व: $tattvaName")
                appendLine("$startLabel: $tattvaStartStr | $endLabel: $tattvaEndStr")
                appendLine("नाड़ी: $nadiName")
                appendLine("तिथि: $tithiName")
                append("$startLabel: $tithiStartStr | $endLabel: $tithiEndStr")
            } else {
                appendLine("Hora: $horaName")
                appendLine("$startLabel: $horaStartStr | $endLabel: $horaEndStr")
                appendLine("Tattva: $tattvaName")
                appendLine("$startLabel: $tattvaStartStr | $endLabel: $tattvaEndStr")
                appendLine("Nadi: $nadiName")
                appendLine("Tithi: $tithiName")
                append("$startLabel: $tithiStartStr | $endLabel: $tithiEndStr")
            }
        }

        val tattvaKey = if (activeTattva != null) {
            "${activeTattva.element.name}_${activeTattva.startTimeMs}"
        } else {
            ""
        }

        val nextTransitionAtMs = listOfNotNull(
            activeTattva?.endTimeMs?.takeIf { it > nowMs },
            activeHora?.endTimeMs?.takeIf { it > nowMs },
            activeTithi?.endTimeMs?.takeIf { it > nowMs }
        ).minOrNull() ?: (nowMs + 5 * 60_000L)

        return RunningNowNotificationContent(
            title = title,
            collapsedText = collapsedText,
            expandedBigText = expandedBigText,
            tattvaTransitionKey = tattvaKey,
            nextTransitionAtMs = nextTransitionAtMs
        )
    }

    private fun resolveActiveSegment(segments: List<AngaSegment>, nowMs: Long): AngaSegment? {
        if (segments.isEmpty()) return null
        return segments.firstOrNull { seg ->
            val start = seg.startTimeMs ?: Long.MIN_VALUE
            val end = seg.endTimeMs ?: Long.MAX_VALUE
            nowMs in start until end
        } ?: segments.firstOrNull { seg ->
            val start = seg.startTimeMs ?: Long.MIN_VALUE
            val end = seg.endTimeMs ?: Long.MAX_VALUE
            nowMs in start..end
        }
    }

    private fun tattvaEn(element: TattvaElement): String = when (element) {
        TattvaElement.PRITHVI -> "Prithvi (Earth)"
        TattvaElement.JALA -> "Jala (Water)"
        TattvaElement.TEJAS -> "Agni (Fire)"
        TattvaElement.VAYU -> "Vayu (Air)"
        TattvaElement.AKASH -> "Akasha (Space)"
    }

    private fun tattvaHi(element: TattvaElement): String = when (element) {
        TattvaElement.PRITHVI -> "पृथ्वी तत्व"
        TattvaElement.JALA -> "जल तत्व"
        TattvaElement.TEJAS -> "अग्नि तत्व"
        TattvaElement.VAYU -> "वायु तत्व"
        TattvaElement.AKASH -> "आकाश तत्व"
    }

    private fun nadiLabelEn(nadi: SwaraNadi): String = when (nadi) {
        SwaraNadi.IDA -> "Ida Nadi (Left)"
        SwaraNadi.PINGALA -> "Pingala Nadi (Right)"
        SwaraNadi.SUSHUMNA -> "Sushumna Nadi (Both)"
    }

    private fun nadiLabelHi(nadi: SwaraNadi): String = when (nadi) {
        SwaraNadi.IDA -> "इड़ा नाड़ी (बायाँ)"
        SwaraNadi.PINGALA -> "पिंगला नाड़ी (दायाँ)"
        SwaraNadi.SUSHUMNA -> "सुषुम्णा नाड़ी (मध्य)"
    }

    private fun horaRulerHi(ruler: String): String = when (ruler.lowercase(Locale.US)) {
        "sun", "surya" -> "सूर्य"
        "moon", "chandra" -> "चन्द्र"
        "mars", "mangal" -> "मंगल"
        "mercury", "budha" -> "बुध"
        "jupiter", "guru" -> "गुरु"
        "venus", "shukra" -> "शुक्र"
        "saturn", "shani" -> "शनि"
        else -> ruler
    }
}
