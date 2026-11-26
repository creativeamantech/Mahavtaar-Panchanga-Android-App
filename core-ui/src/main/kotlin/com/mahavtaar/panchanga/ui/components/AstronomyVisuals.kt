package com.mahavtaar.panchanga.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Jetpack Compose Canvas rendering the astronomical lunar phase disc
 * directly from the Moon-Sun elongation angle (0°..360°).
 */
@Composable
fun MoonPhaseCanvas(
    illuminationPercent: Int,
    isWaxing: Boolean,
    phaseLabel: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 56.dp
) {
    val litColor = MaterialTheme.colorScheme.primary
    val shadowColor = MaterialTheme.colorScheme.surfaceVariant
    val rimColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = modifier
            .size(diameter)
            .semantics {
                contentDescription = "Lunar phase: $phaseLabel, $illuminationPercent percent illuminated"
            }
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Subtle celestial glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(litColor.copy(alpha = 0.25f), Color.Transparent),
                center = center,
                radius = radius * 1.15f
            ),
            radius = radius * 1.15f,
            center = center
        )

        // Dark lunar base
        drawCircle(color = shadowColor, radius = radius * 0.88f, center = center)

        // Illuminated portion approximated cleanly with semi-circle + terminator ellipse
        val r = radius * 0.88f
        val frac = (illuminationPercent.coerceIn(0, 100)) / 100f
        if (frac > 0.02f) {
            if (frac >= 0.98f) {
                drawCircle(color = litColor, radius = r, center = center)
            } else {
                val rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
                val litPath = Path().apply {
                    if (isWaxing) {
                        arcTo(rect, -90f, 180f, false)
                    } else {
                        arcTo(rect, 90f, 180f, false)
                    }
                    close()
                }
                drawPath(path = litPath, color = litColor)

                // Terminator ellipse width scales from r (new/full) to 0 (quarter)
                val terminatorX = r * kotlin.math.abs(1f - 2f * frac)
                val ellipseColor = if (frac < 0.5f) shadowColor else litColor
                drawOval(
                    color = ellipseColor,
                    topLeft = Offset(center.x - terminatorX, center.y - r),
                    size = Size(terminatorX * 2f, r * 2f)
                )
            }
        }

        // Crisp rim
        drawCircle(
            color = rimColor.copy(alpha = 0.6f),
            radius = r,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/**
 * Custom Compose Canvas rendering the daily solar arc from sunrise to sunset
 * with live sun marker position.
 */
@Composable
fun SolarArcCanvas(
    progress: Float, // 0f at sunrise, 1f at sunset, outside [0,1] during night
    modifier: Modifier = Modifier
) {
    val arcColor = MaterialTheme.colorScheme.outline
    val activeColor = MaterialTheme.colorScheme.primary
    val sunGlow = MaterialTheme.colorScheme.secondary

    Canvas(
        modifier = modifier.semantics {
            val pct = (progress.coerceIn(0f, 1f) * 100).toInt()
            contentDescription = "Solar progression arc at $pct percent of daylight"
        }
    ) {
        val pad = 12.dp.toPx()
        val arcWidth = (size.width - pad * 2f).coerceAtLeast(10f)
        val arcRadius = arcWidth / 2f
        val horizonY = size.height - pad
        val topLeft = Offset(pad, horizonY - arcRadius)
        val arcSize = Size(arcWidth, arcWidth)

        // Horizon baseline
        drawLine(
            color = arcColor.copy(alpha = 0.5f),
            start = Offset(pad, horizonY),
            end = Offset(size.width - pad, horizonY),
            strokeWidth = 1.dp.toPx()
        )

        // Background full arc
        drawArc(
            color = arcColor.copy(alpha = 0.45f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Active elapsed arc
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped > 0f) {
            drawArc(
                color = activeColor,
                startAngle = 180f,
                sweepAngle = 180f * clamped,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Sun orb marker along semicircle
        if (progress in 0f..1f) {
            val angleRad = PI * (1.0 - clamped.toDouble())
            val sunX = pad + arcRadius + (arcRadius * cos(angleRad)).toFloat()
            val sunY = horizonY - (arcRadius * sin(angleRad)).toFloat()
            drawCircle(
                color = sunGlow.copy(alpha = 0.30f),
                radius = 10.dp.toPx(),
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = activeColor,
                radius = 5.dp.toPx(),
                center = Offset(sunX, sunY)
            )
        }
    }
}
