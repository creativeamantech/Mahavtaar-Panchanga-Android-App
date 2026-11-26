package com.mahavtaar.panchanga.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

private enum class TimelineCategory { AUSPICIOUS, INAUSPICIOUS, CAUTION }

private data class TimelineBlock(
    val id: String,
    val title: String,
    val startText: String,
    val endText: String,
    val startMs: Long,
    val endMs: Long,
    val category: TimelineCategory,
    val interval: TimingInterval,
    val glossaryQuery: String = ""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuhurtaTimelineBar(
    snapshot: PanchangaSnapshot,
    nowMs: Long,
    modifier: Modifier = Modifier,
    onInspectInterval: ((String, TimingInterval) -> Unit)? = null
) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current

    val dayStart = snapshot.sunriseMs
    val dayEnd = snapshot.nextSunriseMs.coerceAtLeast(dayStart + 86_400_000L)
    val totalSpan = (dayEnd - dayStart).toFloat().coerceAtLeast(1f)

    val blocks = remember(snapshot, lang) {
        buildList {
            fun addInterval(
                id: String,
                name: String,
                interval: TimingInterval?,
                cat: TimelineCategory,
                glossaryQuery: String = ""
            ) {
                val s = interval?.startTimeMs ?: return
                val e = interval.endTimeMs ?: return
                val cs = s.coerceIn(dayStart, dayEnd)
                val ce = e.coerceIn(dayStart, dayEnd)
                if (ce > cs) {
                    val startDateSec = VedicLocalizer.formatDateTimeWithSeconds(s, snapshot.timezone, lang)
                    val endDateSec = VedicLocalizer.formatDateTimeWithSeconds(e, snapshot.timezone, lang)
                    add(TimelineBlock(id, name, startDateSec, endDateSec, cs, ce, cat, interval, glossaryQuery))
                }
            }
            addInterval("abhijit", VedicLocalizer.muhurtaOrPeriodName("Abhijit Muhūrta", lang), snapshot.abhijitMuhurta, TimelineCategory.AUSPICIOUS, "abhijit")
            addInterval("vijaya", ext.vijayaMuhurtaLabel, snapshot.vijayaMuhurta, TimelineCategory.AUSPICIOUS, "abhijit")
            addInterval("godhuli", ext.godhuliMuhurtaLabel, snapshot.godhuliMuhurta, TimelineCategory.AUSPICIOUS, "amrit_kalam")
            addInterval("nishita", ext.nishitaMuhurtaLabel, snapshot.nishitaMuhurta, TimelineCategory.CAUTION, "amrit_kalam")
            addInterval("brahma", VedicLocalizer.muhurtaOrPeriodName("Brahma Muhūrta", lang), snapshot.brahmaMuhurta, TimelineCategory.AUSPICIOUS, "amrit_kalam")
            snapshot.amritaKala.forEachIndexed { idx, it ->
                addInterval("amrit_$idx", VedicLocalizer.muhurtaOrPeriodName("Amṛta Kāla", lang), it, TimelineCategory.AUSPICIOUS, "amrit_kalam")
            }
            addInterval("rahu", VedicLocalizer.muhurtaOrPeriodName("Rāhu Kāla", lang), snapshot.rahuKala, TimelineCategory.INAUSPICIOUS, "rahu_kala")
            addInterval("yamaganda", VedicLocalizer.muhurtaOrPeriodName("Yamagaṇḍa", lang), snapshot.yamaganda, TimelineCategory.INAUSPICIOUS, "yamaganda")
            addInterval("gulika", VedicLocalizer.muhurtaOrPeriodName("Gulika Kāla", lang), snapshot.gulikaKala, TimelineCategory.INAUSPICIOUS, "yamaganda")
            snapshot.durmuhurta.forEachIndexed { idx, it ->
                addInterval("dur_$idx", VedicLocalizer.muhurtaOrPeriodName("Durmuhūrta", lang), it, TimelineCategory.INAUSPICIOUS, "rahu_kala")
            }
            snapshot.varjyam.forEachIndexed { idx, it ->
                addInterval("varj_$idx", VedicLocalizer.muhurtaOrPeriodName("Varjyam", lang), it, TimelineCategory.INAUSPICIOUS, "rahu_kala")
            }
        }
    }

    var selectedBlock by remember(snapshot.dateIso) { mutableStateOf<TimelineBlock?>(null) }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val auspiciousColor = vedic.auspicious
    val inauspiciousColor = vedic.inauspicious
    val cautionColor = vedic.caution
    val sunsetTickColor = MaterialTheme.colorScheme.secondary
    val liveCursorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outline

    PanchangaCard(modifier = modifier) {
        Text(
            text = ext.muhurtaTimelineTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = ext.muhurtaTimelineSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("muhurta_timeline_canvas")
                .semantics {
                    contentDescription = ext.muhurtaTimelineTitle
                }
                .pointerInput(blocks, dayStart, totalSpan) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val tappedMs = dayStart + (ratio * totalSpan).toLong()
                        selectedBlock = blocks.lastOrNull { tappedMs in it.startMs..it.endMs }
                            ?: blocks.minByOrNull { block ->
                                val mid = (block.startMs + block.endMs) / 2L
                                kotlin.math.abs(mid - tappedMs)
                            }
                    }
                }
        ) {
            val barTop = 8.dp.toPx()
            val barHeight = size.height - 18.dp.toPx()
            val radius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

            // Base 24h track with subtle daylight vs night tinting
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, barTop),
                size = Size(size.width, barHeight),
                cornerRadius = radius
            )
            if (snapshot.sunsetMs in dayStart..dayEnd) {
                val sunsetX = ((snapshot.sunsetMs - dayStart) / totalSpan) * size.width
                drawRoundRect(
                    color = sunsetTickColor.copy(alpha = 0.08f),
                    topLeft = Offset(0f, barTop),
                    size = Size(sunsetX.coerceAtLeast(0f), barHeight),
                    cornerRadius = radius
                )
            }

            // Draw segments in two horizontal bands: top = auspicious, bottom = inauspicious/caution
            val rowHeight = (barHeight - 4.dp.toPx()) / 2f
            blocks.forEach { block ->
                val x1 = ((block.startMs - dayStart) / totalSpan) * size.width
                val x2 = ((block.endMs - dayStart) / totalSpan) * size.width
                val w = (x2 - x1).coerceAtLeast(3.dp.toPx())
                val (y, color) = when (block.category) {
                    TimelineCategory.AUSPICIOUS -> barTop to auspiciousColor
                    TimelineCategory.INAUSPICIOUS -> (barTop + rowHeight + 4.dp.toPx()) to inauspiciousColor
                    TimelineCategory.CAUTION -> barTop to cautionColor
                }
                drawRoundRect(
                    color = color.copy(alpha = if (selectedBlock?.id == block.id) 1f else 0.82f),
                    topLeft = Offset(x1, y),
                    size = Size(w, rowHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // Sunset vertical divider
            if (snapshot.sunsetMs in dayStart..dayEnd) {
                val sunsetX = ((snapshot.sunsetMs - dayStart) / totalSpan) * size.width
                drawLine(
                    color = sunsetTickColor,
                    start = Offset(sunsetX, 2.dp.toPx()),
                    end = Offset(sunsetX, size.height - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Frame border
            drawRoundRect(
                color = outlineColor.copy(alpha = 0.4f),
                topLeft = Offset(0f, barTop),
                size = Size(size.width, barHeight),
                cornerRadius = radius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )

            // Live current time marker
            if (nowMs in dayStart..dayEnd) {
                val nowX = ((nowMs - dayStart) / totalSpan) * size.width
                drawLine(
                    color = liveCursorColor,
                    start = Offset(nowX, 0f),
                    end = Offset(nowX, size.height),
                    strokeWidth = 2.5.dp.toPx()
                )
                drawCircle(
                    color = liveCursorColor,
                    radius = 4.dp.toPx(),
                    center = Offset(nowX, 4.dp.toPx())
                )
            }
        }

        // Sunrise -> Sunset -> Next Sunrise axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "☀ ${VedicLocalizer.formatDateTimeWithSeconds(snapshot.sunriseMs, snapshot.timezone, lang)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "🌇 ${VedicLocalizer.formatDateTimeWithSeconds(snapshot.sunsetMs, snapshot.timezone, lang)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "☀ ${VedicLocalizer.formatDateTimeWithSeconds(snapshot.nextSunriseMs, snapshot.timezone, lang)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend row
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LegendItem(color = auspiciousColor, label = ext.auspiciousLegend)
            LegendItem(color = cautionColor, label = ext.cautionLegend)
            LegendItem(color = inauspiciousColor, label = ext.inauspiciousLegend)
        }

        val active = selectedBlock
        if (active != null) {
            Spacer(modifier = Modifier.height(10.dp))
            val pillColor = when (active.category) {
                TimelineCategory.AUSPICIOUS -> vedic.auspicious
                TimelineCategory.INAUSPICIOUS -> vedic.inauspicious
                TimelineCategory.CAUTION -> vedic.caution
            }
            val pillBg = when (active.category) {
                TimelineCategory.AUSPICIOUS -> vedic.auspiciousContainer
                TimelineCategory.INAUSPICIOUS -> vedic.inauspiciousContainer
                TimelineCategory.CAUTION -> vedic.cautionContainer
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, pillColor.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onInspectInterval != null) {
                            Modifier.clickable {
                                onInspectInterval(active.title, active.interval)
                            }
                        } else Modifier
                    )
                    .testTag("muhurta_timeline_tooltip")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = pillColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = active.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        EventStartEndDateTimeView(
                            startText = active.startText,
                            endText = active.endText,
                            accentColor = pillColor,
                            compact = true
                        )
                    }
                    StatusPill(
                        label = when (active.category) {
                            TimelineCategory.AUSPICIOUS -> ext.auspiciousLegend
                            TimelineCategory.INAUSPICIOUS -> ext.inauspiciousLegend
                            TimelineCategory.CAUTION -> ext.cautionLegend
                        },
                        textColor = pillColor,
                        containerColor = pillBg
                    )
                    IconButton(onClick = { selectedBlock = null }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = ext.closeLabel,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ext.tapSegmentHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorDot(color = color)
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
