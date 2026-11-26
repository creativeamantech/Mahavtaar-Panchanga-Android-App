package com.mahavtaar.panchanga.feature.daily

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.KundliPlanetEntry
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage

enum class KundliChartStyle {
    NORTH_INDIAN,
    SOUTH_INDIAN,
    EAST_INDIAN
}

/**
 * Authentic native Jetpack Compose Canvas Kundali Chart renderer supporting:
 * 1. North Indian Diamond Chart (houses fixed, signs rotate by Lagna)
 * 2. South Indian Square Chart (signs fixed clockwise from Aries)
 * 3. East Indian 3x3 Corner-Split Chart (signs fixed counter-clockwise from Aries Top-Center)
 */
@Composable
fun KundliChartCanvas(
    lagnaSignIndex: Int,
    planets: List<KundliPlanetEntry>,
    planetSignsOverride: Map<String, Int>? = null,
    isNorthIndian: Boolean = true,
    chartStyle: KundliChartStyle = if (isNorthIndian) KundliChartStyle.NORTH_INDIAN else KundliChartStyle.SOUTH_INDIAN,
    centerTitle: String = "D1",
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    val fillColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val signTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val planetTextColor = MaterialTheme.colorScheme.onSurface
    val lagnaAccent = MaterialTheme.colorScheme.primary

    val planetsBySign: Map<Int, List<String>> = buildMap {
        val grouped = mutableMapOf<Int, MutableList<String>>()
        planets.forEach { p ->
            val sign = planetSignsOverride?.get(p.id) ?: p.signIndex
            val label = if (lang == AppLanguage.HINDI) p.shortHi else p.shortEn
            val suffix = if (p.isRetrograde && p.id !in setOf("rahu", "ketu")) "*" else ""
            grouped.getOrPut(sign) { mutableListOf() }.add("$label$suffix")
        }
        putAll(grouped)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val w = size.width
        val h = size.height
        val strokePx = 2.5f

        drawRect(color = fillColor, topLeft = Offset.Zero, size = Size(w, h))
        drawRect(color = lineColor, topLeft = Offset.Zero, size = Size(w, h), style = Stroke(strokePx))

        val signPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = w * 0.035f
            color = signTextColor.toArgb()
            isFakeBoldText = true
        }
        val planetPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = w * 0.039f
            color = planetTextColor.toArgb()
            isFakeBoldText = true
        }
        val lagnaPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = w * 0.040f
            color = lagnaAccent.toArgb()
            isFakeBoldText = true
        }
        val canvas = drawContext.canvas.nativeCanvas
        val ascTag = if (lang == AppLanguage.HINDI) "लग्न" else "Asc"

        when (chartStyle) {
            KundliChartStyle.NORTH_INDIAN -> {
                drawLine(lineColor, Offset(0f, 0f), Offset(w, h), strokeWidth = strokePx)
                drawLine(lineColor, Offset(w, 0f), Offset(0f, h), strokeWidth = strokePx)
                drawLine(lineColor, Offset(w / 2f, 0f), Offset(w, h / 2f), strokeWidth = strokePx)
                drawLine(lineColor, Offset(w, h / 2f), Offset(w / 2f, h), strokeWidth = strokePx)
                drawLine(lineColor, Offset(w / 2f, h), Offset(0f, h / 2f), strokeWidth = strokePx)
                drawLine(lineColor, Offset(0f, h / 2f), Offset(w / 2f, 0f), strokeWidth = strokePx)

                val houseCenters = listOf(
                    0.50f to 0.25f, 0.25f to 0.11f, 0.11f to 0.25f, 0.25f to 0.50f,
                    0.11f to 0.75f, 0.25f to 0.89f, 0.50f to 0.75f, 0.75f to 0.89f,
                    0.89f to 0.75f, 0.75f to 0.50f, 0.89f to 0.25f, 0.75f to 0.11f
                )

                houseCenters.forEachIndexed { idx, (fx, fy) ->
                    val sign = (lagnaSignIndex + idx) % 12
                    val signNum = sign + 1
                    val cx = w * fx
                    val cy = h * fy
                    val pList = planetsBySign[sign].orEmpty()
                    val signLabel = if (idx == 0) "$signNum ($ascTag)" else "$signNum"
                    canvas.drawText(signLabel, cx, cy - h * 0.025f, if (idx == 0) lagnaPaint else signPaint)
                    if (pList.isNotEmpty()) {
                        val line1 = pList.take(3).joinToString(" ")
                        val line2 = pList.drop(3).take(3).joinToString(" ")
                        canvas.drawText(line1, cx, cy + h * 0.028f, planetPaint)
                        if (line2.isNotBlank()) canvas.drawText(line2, cx, cy + h * 0.070f, planetPaint)
                    }
                }
            }

            KundliChartStyle.SOUTH_INDIAN -> {
                val cell = w / 4f
                for (i in 1..3) {
                    drawLine(lineColor, Offset(cell * i, 0f), Offset(cell * i, h), strokeWidth = strokePx)
                    drawLine(lineColor, Offset(0f, cell * i), Offset(w, cell * i), strokeWidth = strokePx)
                }
                drawRect(
                    color = fillColor,
                    topLeft = Offset(cell + strokePx, cell + strokePx),
                    size = Size(cell * 2f - strokePx * 2f, cell * 2f - strokePx * 2f)
                )
                val signCells = listOf(
                    0 to 1, 0 to 2, 0 to 3, 1 to 3,
                    2 to 3, 3 to 3, 3 to 2, 3 to 1,
                    3 to 0, 2 to 0, 1 to 0, 0 to 0
                )
                canvas.drawText(centerTitle, w / 2f, h / 2f + w * 0.015f, lagnaPaint)
                signCells.forEachIndexed { signIdx, (row, col) ->
                    val cx = col * cell + cell / 2f
                    val cy = row * cell + cell / 2f
                    val isLagna = signIdx == lagnaSignIndex
                    val header = if (isLagna) "${signIdx + 1} · $ascTag" else "${signIdx + 1}"
                    canvas.drawText(header, cx, row * cell + cell * 0.28f, if (isLagna) lagnaPaint else signPaint)
                    val pList = planetsBySign[signIdx].orEmpty()
                    if (pList.isNotEmpty()) {
                        canvas.drawText(pList.take(3).joinToString(" "), cx, cy + cell * 0.12f, planetPaint)
                        if (pList.size > 3) {
                            canvas.drawText(pList.drop(3).take(3).joinToString(" "), cx, cy + cell * 0.34f, planetPaint)
                        }
                    }
                }
            }

            KundliChartStyle.EAST_INDIAN -> {
                val third = w / 3f
                val twoThirds = w * 2f / 3f
                // 3x3 orthogonal grid
                drawLine(lineColor, Offset(third, 0f), Offset(third, h), strokeWidth = strokePx)
                drawLine(lineColor, Offset(twoThirds, 0f), Offset(twoThirds, h), strokeWidth = strokePx)
                drawLine(lineColor, Offset(0f, third), Offset(w, third), strokeWidth = strokePx)
                drawLine(lineColor, Offset(0f, twoThirds), Offset(w, twoThirds), strokeWidth = strokePx)
                // Four corner diagonal splits
                drawLine(lineColor, Offset(0f, 0f), Offset(third, third), strokeWidth = strokePx)
                drawLine(lineColor, Offset(w, 0f), Offset(twoThirds, third), strokeWidth = strokePx)
                drawLine(lineColor, Offset(0f, h), Offset(third, twoThirds), strokeWidth = strokePx)
                drawLine(lineColor, Offset(w, h), Offset(twoThirds, twoThirds), strokeWidth = strokePx)

                canvas.drawText(centerTitle, w / 2f, h / 2f + w * 0.015f, lagnaPaint)

                // Counter-clockwise sign centers Aries(0)..Pisces(11)
                val eastSignCenters = listOf(
                    0.50f to 0.17f, // 0 Aries (Top center)
                    0.22f to 0.11f, // 1 Taurus (Top-left upper triangle)
                    0.11f to 0.23f, // 2 Gemini (Top-left side triangle)
                    0.17f to 0.50f, // 3 Cancer (Middle left)
                    0.11f to 0.77f, // 4 Leo (Bottom-left side triangle)
                    0.22f to 0.89f, // 5 Virgo (Bottom-left lower triangle)
                    0.50f to 0.83f, // 6 Libra (Bottom center)
                    0.78f to 0.89f, // 7 Scorpio (Bottom-right lower triangle)
                    0.89f to 0.77f, // 8 Sagittarius (Bottom-right side triangle)
                    0.83f to 0.50f, // 9 Capricorn (Middle right)
                    0.89f to 0.23f, // 10 Aquarius (Top-right side triangle)
                    0.78f to 0.11f  // 11 Pisces (Top-right upper triangle)
                )
                eastSignCenters.forEachIndexed { signIdx, (fx, fy) ->
                    val cx = w * fx
                    val cy = h * fy
                    val isLagna = signIdx == lagnaSignIndex
                    val header = if (isLagna) "${signIdx + 1} $ascTag" else "${signIdx + 1}"
                    canvas.drawText(header, cx, cy - h * 0.025f, if (isLagna) lagnaPaint else signPaint)
                    val pList = planetsBySign[signIdx].orEmpty()
                    if (pList.isNotEmpty()) {
                        canvas.drawText(pList.take(3).joinToString(" "), cx, cy + h * 0.028f, planetPaint)
                        if (pList.size > 3) {
                            canvas.drawText(pList.drop(3).take(3).joinToString(" "), cx, cy + h * 0.068f, planetPaint)
                        }
                    }
                }
            }
        }
    }
}
