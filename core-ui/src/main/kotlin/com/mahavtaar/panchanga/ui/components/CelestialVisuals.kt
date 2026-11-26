package com.mahavtaar.panchanga.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.MonthlyCalendarDay
import com.mahavtaar.panchanga.domain.model.NavtaraResult
import com.mahavtaar.panchanga.domain.model.PlanetPosition
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * Elevated Solar Horizon Arc showing Sunrise → Solar Noon → Sunset with highlighted
 * Abhijit Muhūrta (emerald) and Rāhu Kāla (crimson) bands plus a glowing live Sun orb.
 */
@Composable
fun CelestialHorizonArcCanvas(
    solarProgress: Float,
    abhijitRange: Pair<Float, Float>? = null,
    rahuRange: Pair<Float, Float>? = null,
    modifier: Modifier = Modifier
) {
    val vedic = LocalVedicColors.current
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val elapsedColor = MaterialTheme.colorScheme.primary
    val sunGlow = MaterialTheme.colorScheme.secondary
    val horizonColor = MaterialTheme.colorScheme.outlineVariant

    var targetProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(solarProgress) {
        targetProgress = solarProgress.coerceIn(0f, 1f)
    }
    val animProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 320),
        label = "horizon_arc_progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(106.dp)
            .semantics {
                val pct = (solarProgress.coerceIn(0f, 1f) * 100).toInt()
                contentDescription = "Celestial solar horizon arc at $pct percent of daylight"
            }
    ) {
        val padH = 18.dp.toPx()
        val padBottom = 14.dp.toPx()
        val availWidth = (size.width - padH * 2f).coerceAtLeast(40f)
        val maxRadiusFromHeight = (size.height - padBottom - 10.dp.toPx()).coerceAtLeast(24f)
        val radius = min(availWidth / 2f, maxRadiusFromHeight)
        val centerX = size.width / 2f
        val horizonY = size.height - padBottom
        val topLeft = Offset(centerX - radius, horizonY - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        // Subtle atmospheric radial wash inside the sky dome
        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(sunGlow.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(centerX, horizonY),
                radius = radius * 1.05f
            ),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize
        )

        // Dashed horizon baseline
        drawLine(
            color = horizonColor,
            start = Offset(padH, horizonY),
            end = Offset(size.width - padH, horizonY),
            strokeWidth = 1.2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )

        // Solar Noon meridian tick
        drawLine(
            color = trackColor,
            start = Offset(centerX, horizonY - radius - 4.dp.toPx()),
            end = Offset(centerX, horizonY - radius + 5.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )

        // Base trajectory track
        drawArc(
            color = trackColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Rahu Kala band (inauspicious crimson arc)
        rahuRange?.let { (rs, re) ->
            val s = rs.coerceIn(0f, 1f)
            val e = re.coerceIn(s, 1f)
            if (e > s) {
                drawArc(
                    color = vedic.inauspicious.copy(alpha = 0.72f),
                    startAngle = 180f + s * 180f,
                    sweepAngle = ((e - s) * 180f).coerceAtLeast(3f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Abhijit Muhurta band (auspicious emerald arc near zenith)
        abhijitRange?.let { (asStart, asEnd) ->
            val s = asStart.coerceIn(0f, 1f)
            val e = asEnd.coerceIn(s, 1f)
            if (e > s) {
                drawArc(
                    color = vedic.auspicious.copy(alpha = 0.85f),
                    startAngle = 180f + s * 180f,
                    sweepAngle = ((e - s) * 180f).coerceAtLeast(3f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Elapsed luminous daylight arc
        if (animProgress > 0f) {
            drawArc(
                color = elapsedColor,
                startAngle = 180f,
                sweepAngle = 180f * animProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Sunrise & Sunset anchor pins
        drawCircle(color = elapsedColor, radius = 3.5.dp.toPx(), center = Offset(centerX - radius, horizonY))
        drawCircle(color = sunGlow, radius = 3.5.dp.toPx(), center = Offset(centerX + radius, horizonY))

        // Live Sun orb along semicircle
        if (solarProgress in 0f..1f) {
            val angleRad = PI * (1.0 - animProgress.toDouble())
            val sunX = centerX + (radius * cos(angleRad)).toFloat()
            val sunY = horizonY - (radius * sin(angleRad)).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(sunGlow.copy(alpha = 0.50f), Color.Transparent),
                    center = Offset(sunX, sunY),
                    radius = 16.dp.toPx()
                ),
                radius = 16.dp.toPx(),
                center = Offset(sunX, sunY)
            )
            drawCircle(color = sunGlow, radius = 6.5.dp.toPx(), center = Offset(sunX, sunY))
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 2.8.dp.toPx(), center = Offset(sunX, sunY))
        }
    }
}

/**
 * Interactive 360° Sidereal Zodiac Wheel (Rashichakra) plotting 12 Rāśis,
 * 27 Nakṣatra outer ticks, the live Sun–Moon elongation chord, and all 9 Navagrahas.
 */
@Composable
fun SiderealZodiacWheelCanvas(
    planets: List<PlanetPosition>,
    selectedPlanetId: String?,
    onSelectPlanet: (PlanetPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    val vedic = LocalVedicColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val planetAccentFor: (String) -> Color = { id ->
        when (id.lowercase()) {
            "sun" -> vedic.petalSaffron
            "moon" -> tertiary
            "mars" -> vedic.inauspicious
            "mercury" -> vedic.auspicious
            "jupiter" -> secondary
            "venus" -> vedic.petalPlum
            "saturn" -> primary
            "rahu" -> vedic.sushumnaNadi
            else -> vedic.caution
        }
    }

    Canvas(
        modifier = modifier
            .size(256.dp)
            .semantics {
                contentDescription = "360 degree sidereal zodiac wheel with ${planets.size} planetary positions"
            }
            .pointerInput(planets) {
                detectTapGestures { tapOffset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outerR = min(cx, cy) - 10.dp.toPx()
                    val planetTrackR = outerR * 0.66f
                    val hit = planets.minByOrNull { planet ->
                        val angleRad = Math.toRadians(planet.siderealLongitude - 90.0)
                        val px = cx + (planetTrackR * cos(angleRad)).toFloat()
                        val py = cy + (planetTrackR * sin(angleRad)).toFloat()
                        hypot((tapOffset.x - px).toDouble(), (tapOffset.y - py).toDouble())
                    }
                    if (hit != null) {
                        val angleRad = Math.toRadians(hit.siderealLongitude - 90.0)
                        val px = cx + (planetTrackR * cos(angleRad)).toFloat()
                        val py = cy + (planetTrackR * sin(angleRad)).toFloat()
                        val dist = hypot((tapOffset.x - px).toDouble(), (tapOffset.y - py).toDouble())
                        if (dist <= 44.dp.toPx()) {
                            onSelectPlanet(hit)
                        }
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerR = (size.minDimension / 2f) - 10.dp.toPx()
        val rasiInnerR = outerR * 0.80f
        val planetTrackR = outerR * 0.66f
        val coreR = outerR * 0.34f

        // Subtle celestial backdrop glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.12f), Color.Transparent),
                center = center,
                radius = outerR
            ),
            radius = outerR,
            center = center
        )

        // Concentric zodiac rings
        drawCircle(color = surfaceVariant.copy(alpha = 0.45f), radius = outerR, center = center)
        drawCircle(color = outline.copy(alpha = 0.55f), radius = outerR, center = center, style = Stroke(1.4.dp.toPx()))
        drawCircle(color = outlineVariant, radius = rasiInnerR, center = center, style = Stroke(1.dp.toPx()))
        drawCircle(color = outlineVariant.copy(alpha = 0.7f), radius = planetTrackR, center = center, style = Stroke(0.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))))
        drawCircle(color = primary.copy(alpha = 0.25f), radius = coreR, center = center, style = Stroke(1.dp.toPx()))

        // 27 Nakshatra outer micro-ticks (13.333° each)
        for (n in 0 until 27) {
            val angleRad = Math.toRadians(n * (360.0 / 27.0) - 90.0)
            val x1 = center.x + (outerR * 0.94f * cos(angleRad)).toFloat()
            val y1 = center.y + (outerR * 0.94f * sin(angleRad)).toFloat()
            val x2 = center.x + (outerR * cos(angleRad)).toFloat()
            val y2 = center.y + (outerR * sin(angleRad)).toFloat()
            drawLine(
                color = secondary.copy(alpha = 0.45f),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 12 Rasi spoke dividers (30° each)
        for (r in 0 until 12) {
            val angleRad = Math.toRadians(r * 30.0 - 90.0)
            val x1 = center.x + (coreR * cos(angleRad)).toFloat()
            val y1 = center.y + (coreR * sin(angleRad)).toFloat()
            val x2 = center.x + (outerR * cos(angleRad)).toFloat()
            val y2 = center.y + (outerR * sin(angleRad)).toFloat()
            drawLine(
                color = outlineVariant,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Sun-Moon elongation chord (visualizing Tithi angle)
        val sun = planets.firstOrNull { it.id.equals("sun", ignoreCase = true) }
        val moon = planets.firstOrNull { it.id.equals("moon", ignoreCase = true) }
        if (sun != null && moon != null) {
            val sa = Math.toRadians(sun.siderealLongitude - 90.0)
            val ma = Math.toRadians(moon.siderealLongitude - 90.0)
            val sPt = Offset(center.x + (planetTrackR * cos(sa)).toFloat(), center.y + (planetTrackR * sin(sa)).toFloat())
            val mPt = Offset(center.x + (planetTrackR * cos(ma)).toFloat(), center.y + (planetTrackR * sin(ma)).toFloat())
            drawLine(
                color = secondary.copy(alpha = 0.55f),
                start = sPt,
                end = mPt,
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            )
        }

        // Plot 9 Navagrahas at true sidereal longitude (stagger radial distance slightly if close)
        val sorted = planets.sortedBy { it.siderealLongitude }
        sorted.forEachIndexed { idx, planet ->
            val prev = sorted.getOrNull(idx - 1)
            val crowded = prev != null && kotlin.math.abs(planet.siderealLongitude - prev.siderealLongitude) < 11.0
            val radialOffset = if (crowded) planetTrackR * 0.80f else planetTrackR
            val angleRad = Math.toRadians(planet.siderealLongitude - 90.0)
            val px = center.x + (radialOffset * cos(angleRad)).toFloat()
            val py = center.y + (radialOffset * sin(angleRad)).toFloat()
            val color = planetAccentFor(planet.id)
            val isSelected = selectedPlanetId.equals(planet.id, ignoreCase = true)

            // Outer longitude ray pin
            val edgeX = center.x + (rasiInnerR * cos(angleRad)).toFloat()
            val edgeY = center.y + (rasiInnerR * sin(angleRad)).toFloat()
            drawLine(
                color = color.copy(alpha = if (isSelected) 0.85f else 0.35f),
                start = Offset(px, py),
                end = Offset(edgeX, edgeY),
                strokeWidth = if (isSelected) 2.dp.toPx() else 1.dp.toPx()
            )

            // Combust / Selected halo
            if (isSelected || planet.isCombust) {
                drawCircle(
                    color = (if (planet.isCombust) vedic.caution else color).copy(alpha = 0.28f),
                    radius = if (isSelected) 15.dp.toPx() else 12.dp.toPx(),
                    center = Offset(px, py)
                )
            }

            // Planet node
            drawCircle(
                color = color,
                radius = if (isSelected) 8.5.dp.toPx() else 6.5.dp.toPx(),
                center = Offset(px, py)
            )

            // Retrograde white inner dot indicator
            if (planet.isRetrograde) {
                drawCircle(
                    color = Color.White,
                    radius = 2.4.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}

/**
 * Concentric dual progress ring for active Planetary Hora (outer) and active sub-Tattva (inner).
 */
@Composable
fun HoraTattvaRingCanvas(
    horaProgress: Float,
    tattvaProgress: Float,
    centerPrimary: String,
    centerSecondary: String,
    modifier: Modifier = Modifier,
    tattvaColor: Color = LocalVedicColors.current.auspicious,
    diameter: Dp = 104.dp
) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    val animHora by animateFloatAsState(
        targetValue = horaProgress.coerceIn(0f, 1f),
        animationSpec = tween(280),
        label = "hora_ring"
    )
    val animTattva by animateFloatAsState(
        targetValue = tattvaProgress.coerceIn(0f, 1f),
        animationSpec = tween(280),
        label = "tattva_ring"
    )

    Box(
        modifier = modifier
            .size(diameter)
            .semantics {
                contentDescription = "Active Hora $centerPrimary $centerSecondary"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val outerStroke = 8.dp.toPx()
            val innerStroke = 6.dp.toPx()
            val outerPad = outerStroke / 2f + 2.dp.toPx()
            val innerPad = outerPad + outerStroke + 5.dp.toPx()

            // Outer Hora ring
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(outerPad, outerPad),
                size = Size(size.width - outerPad * 2, size.height - outerPad * 2),
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = (360f * animHora).coerceAtLeast(4f),
                useCenter = false,
                topLeft = Offset(outerPad, outerPad),
                size = Size(size.width - outerPad * 2, size.height - outerPad * 2),
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )

            // Inner Tattva ring
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(innerPad, innerPad),
                size = Size(size.width - innerPad * 2, size.height - innerPad * 2),
                style = Stroke(width = innerStroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = tattvaColor,
                startAngle = -90f,
                sweepAngle = (360f * animTattva).coerceAtLeast(4f),
                useCenter = false,
                topLeft = Offset(innerPad, innerPad),
                size = Size(size.width - innerPad * 2, size.height - innerPad * 2),
                style = Stroke(width = innerStroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerPrimary,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = centerSecondary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Semi-circular Nadi Polarity Gauge visualizing Idā (Lunar Left), Suṣumnā (Center), and Piṅgalā (Solar Right).
 */
@Composable
fun NadiPolarityArcCanvas(
    activeNadi: SwaraNadi,
    modifier: Modifier = Modifier
) {
    val vedic = LocalVedicColors.current
    val needleTarget = when (activeNadi) {
        SwaraNadi.IDA -> 0.18f
        SwaraNadi.SUSHUMNA -> 0.50f
        SwaraNadi.PINGALA -> 0.82f
    }
    val animNeedle by animateFloatAsState(
        targetValue = needleTarget,
        animationSpec = tween(300),
        label = "nadi_polarity_needle"
    )
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .size(width = 160.dp, height = 88.dp)
            .semantics {
                contentDescription = "Nadi polarity gauge showing active ${activeNadi.label} swara"
            }
    ) {
        val stroke = 10.dp.toPx()
        val pad = stroke / 2f + 4.dp.toPx()
        val radius = min((size.width - pad * 2f) / 2f, size.height - pad - 8.dp.toPx())
        val cx = size.width / 2f
        val cy = size.height - 10.dp.toPx()
        val topLeft = Offset(cx - radius, cy - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        // Left Ida band
        drawArc(
            color = vedic.idaNadi.copy(alpha = if (activeNadi == SwaraNadi.IDA) 1f else 0.32f),
            startAngle = 180f,
            sweepAngle = 64f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // Center Sushumna crown band
        drawArc(
            color = vedic.sushumnaNadi.copy(alpha = if (activeNadi == SwaraNadi.SUSHUMNA) 1f else 0.32f),
            startAngle = 250f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // Right Pingala band
        drawArc(
            color = vedic.pingalaNadi.copy(alpha = if (activeNadi == SwaraNadi.PINGALA) 1f else 0.32f),
            startAngle = 296f,
            sweepAngle = 64f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Needle pointing to active polarity
        val angleRad = PI * (1.0 - animNeedle.toDouble())
        val tipX = cx + ((radius - stroke * 0.7f) * cos(angleRad)).toFloat()
        val tipY = cy - ((radius - stroke * 0.7f) * sin(angleRad)).toFloat()
        val activeColor = when (activeNadi) {
            SwaraNadi.IDA -> vedic.idaNadi
            SwaraNadi.PINGALA -> vedic.pingalaNadi
            SwaraNadi.SUSHUMNA -> vedic.sushumnaNadi
        }
        drawLine(
            color = activeColor,
            start = Offset(cx, cy),
            end = Offset(tipX, tipY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = activeColor, radius = 6.dp.toPx(), center = Offset(cx, cy))
        drawCircle(color = onSurface.copy(alpha = 0.15f), radius = 10.dp.toPx(), center = Offset(cx, cy))
    }
}

/**
 * Sacred classical Tattva Geometry Mandala Canvas:
 * - Prithvi: Golden Square
 * - Jala: Silver-Azure Crescent
 * - Tejas (Agni): Crimson Upward Triangle
 * - Vayu: Teal Hexagram (Six-pointed star)
 * - Akash: Luminous Royal-Plum Oval + Bindu
 */
@Composable
fun TattvaMandalaCanvas(
    element: TattvaElement,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 44.dp
) {
    val vedic = LocalVedicColors.current
    val accent = when (element) {
        TattvaElement.PRITHVI -> vedic.petalSaffron
        TattvaElement.JALA -> vedic.idaNadi
        TattvaElement.TEJAS -> vedic.inauspicious
        TattvaElement.VAYU -> vedic.auspicious
        TattvaElement.AKASH -> vedic.sushumnaNadi
    }

    Canvas(
        modifier = modifier
            .size(sizeDp)
            .semantics {
                contentDescription = "${element.displayName} (${element.sanskritName}) sacred tattva geometry"
            }
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension * 0.38f
        val strokeW = (size.minDimension * 0.06f).coerceIn(1.5.dp.toPx(), 3.dp.toPx())

        // Soft ambient medallion
        drawCircle(color = accent.copy(alpha = 0.14f), radius = size.minDimension * 0.48f, center = c)
        drawCircle(
            color = accent.copy(alpha = 0.45f),
            radius = size.minDimension * 0.48f,
            center = c,
            style = Stroke(width = 1.dp.toPx())
        )

        when (element) {
            TattvaElement.PRITHVI -> {
                val side = r * 1.48f
                val tl = Offset(c.x - side / 2f, c.y - side / 2f)
                drawRect(color = accent.copy(alpha = 0.25f), topLeft = tl, size = Size(side, side))
                drawRect(color = accent, topLeft = tl, size = Size(side, side), style = Stroke(width = strokeW))
            }
            TattvaElement.JALA -> {
                val rect = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
                val crescent = Path().apply {
                    arcTo(rect, 20f, 140f, false)
                    quadraticTo(c.x, c.y + r * 0.25f, c.x + (r * cos(Math.toRadians(20.0))).toFloat(), c.y + (r * sin(Math.toRadians(20.0))).toFloat())
                    close()
                }
                drawPath(crescent, color = accent.copy(alpha = 0.35f))
                drawPath(crescent, color = accent, style = Stroke(width = strokeW))
            }
            TattvaElement.TEJAS -> {
                val tri = Path().apply {
                    moveTo(c.x, c.y - r)
                    lineTo(c.x + r * 0.92f, c.y + r * 0.72f)
                    lineTo(c.x - r * 0.92f, c.y + r * 0.72f)
                    close()
                }
                drawPath(tri, color = accent.copy(alpha = 0.28f))
                drawPath(tri, color = accent, style = Stroke(width = strokeW))
            }
            TattvaElement.VAYU -> {
                val up = Path().apply {
                    moveTo(c.x, c.y - r)
                    lineTo(c.x + r * 0.86f, c.y + r * 0.52f)
                    lineTo(c.x - r * 0.86f, c.y + r * 0.52f)
                    close()
                }
                val down = Path().apply {
                    moveTo(c.x, c.y + r)
                    lineTo(c.x + r * 0.86f, c.y - r * 0.52f)
                    lineTo(c.x - r * 0.86f, c.y - r * 0.52f)
                    close()
                }
                drawPath(up, color = accent.copy(alpha = 0.20f))
                drawPath(down, color = accent.copy(alpha = 0.20f))
                drawPath(up, color = accent, style = Stroke(width = strokeW * 0.85f))
                drawPath(down, color = accent, style = Stroke(width = strokeW * 0.85f))
            }
            TattvaElement.AKASH -> {
                drawOval(
                    color = accent.copy(alpha = 0.25f),
                    topLeft = Offset(c.x - r * 0.72f, c.y - r),
                    size = Size(r * 1.44f, r * 2f)
                )
                drawOval(
                    color = accent,
                    topLeft = Offset(c.x - r * 0.72f, c.y - r),
                    size = Size(r * 1.44f, r * 2f),
                    style = Stroke(width = strokeW)
                )
                drawCircle(color = accent, radius = r * 0.22f, center = c)
            }
        }
    }
}

/**
 * Interactive 9-Sector Navtara Radial Wheel highlighting today's active Tara
 * and allowing taps on any of the 9 Taras (Janma through Ati-Mitra) to inspect classical guidance.
 */
@Composable
fun NavtaraRadialWheelCanvas(
    taraResults: List<NavtaraResult>,
    activeTaraNumber: Int,
    onSelectTaraNumber: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val vedic = LocalVedicColors.current
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline

    val distinctNine = remember(taraResults) {
        (1..9).mapNotNull { num -> taraResults.firstOrNull { ((it.distance - 1) % 9) + 1 == num } }
    }

    Canvas(
        modifier = modifier
            .size(236.dp)
            .semantics {
                contentDescription = "Nine sector Navtara radial wheel with active tara $activeTaraNumber"
            }
            .pointerInput(distinctNine) {
                detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val dist = hypot(dx.toDouble(), dy.toDouble())
                    if (dist >= 24.dp.toPx() && distinctNine.isNotEmpty()) {
                        val deg = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0 + 360.0) % 360.0)
                        val sectorIdx = (deg / 40.0).toInt().coerceIn(0, distinctNine.lastIndex)
                        onSelectTaraNumber(sectorIdx + 1)
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerR = (size.minDimension / 2f) - 10.dp.toPx()
        val innerR = outerR * 0.40f
        val sweep = 40f

        distinctNine.forEachIndexed { idx, tara ->
            val startAngle = -90f + idx * sweep
            val taraNum = idx + 1
            val isActive = taraNum == activeTaraNumber
            val baseColor = when {
                tara.isAuspicious -> vedic.auspicious
                tara.isNeutral -> vedic.caution
                else -> vedic.inauspicious
            }
            val radius = if (isActive) outerR else outerR * 0.92f
            drawArc(
                color = baseColor.copy(alpha = if (isActive) 0.88f else 0.28f),
                startAngle = startAngle + 1.5f,
                sweepAngle = sweep - 3f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f)
            )
            drawArc(
                color = baseColor.copy(alpha = if (isActive) 1f else 0.55f),
                startAngle = startAngle + 1.5f,
                sweepAngle = sweep - 3f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = if (isActive) 2.2.dp.toPx() else 1.dp.toPx())
            )
        }

        // Clean inner observatory hub
        drawCircle(color = surface, radius = innerR, center = center)
        drawCircle(color = outline.copy(alpha = 0.5f), radius = innerR, center = center, style = Stroke(1.2.dp.toPx()))
    }
}

/**
 * Interactive Monthly Lunar Paksha Illumination Strip showing waxing/waning Moon cycle
 * across the entire month with Purnima (gold peak) & Amavasya (plum trough) markers.
 */
@Composable
fun LunarPakshaStripCanvas(
    days: List<MonthlyCalendarDay>,
    selectedDateIso: String?,
    onSelectDay: (MonthlyCalendarDay) -> Unit,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return
    val vedic = LocalVedicColors.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val outline = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics {
                contentDescription = "Monthly lunar paksha phase wave across ${days.size} days"
            }
            .pointerInput(days) {
                detectTapGestures { offset ->
                    val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 0.999f)
                    val idx = (ratio * days.size).toInt().coerceIn(0, days.lastIndex)
                    onSelectDay(days[idx])
                }
            }
    ) {
        val padX = 10.dp.toPx()
        val padTop = 10.dp.toPx()
        val padBottom = 8.dp.toPx()
        val usableW = (size.width - padX * 2f).coerceAtLeast(10f)
        val usableH = (size.height - padTop - padBottom).coerceAtLeast(10f)
        val stepX = if (days.size > 1) usableW / (days.size - 1).toFloat() else usableW

        // Baseline
        drawLine(
            color = outline,
            start = Offset(padX, size.height - padBottom),
            end = Offset(size.width - padX, size.height - padBottom),
            strokeWidth = 1.dp.toPx()
        )

        val curvePath = Path()
        val fillPath = Path()

        days.forEachIndexed { idx, day ->
            val t = day.primaryTithiNumber.coerceIn(1, 30)
            val illum = if (t <= 15) t / 15f else (30 - t) / 15f
            val x = padX + idx * stepX
            val y = (size.height - padBottom) - (illum * usableH)

            if (idx == 0) {
                curvePath.moveTo(x, y)
                fillPath.moveTo(x, size.height - padBottom)
                fillPath.lineTo(x, y)
            } else {
                curvePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(padX + (days.size - 1) * stepX, size.height - padBottom)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(secondary.copy(alpha = 0.28f), Color.Transparent),
                startY = padTop,
                endY = size.height - padBottom
            )
        )
        drawPath(
            path = curvePath,
            color = secondary,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        days.forEachIndexed { idx, day ->
            val t = day.primaryTithiNumber.coerceIn(1, 30)
            val illum = if (t <= 15) t / 15f else (30 - t) / 15f
            val x = padX + idx * stepX
            val y = (size.height - padBottom) - (illum * usableH)
            val isSelected = day.dateIso == selectedDateIso
            val isPurnima = t == 15
            val isAmavasya = t == 30
            val hasFestival = day.observances.isNotEmpty()

            if (isSelected) {
                drawLine(
                    color = primary.copy(alpha = 0.65f),
                    start = Offset(x, padTop * 0.4f),
                    end = Offset(x, size.height - padBottom),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(color = primary, radius = 5.dp.toPx(), center = Offset(x, y))
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
            } else if (isPurnima) {
                drawCircle(color = secondary, radius = 4.dp.toPx(), center = Offset(x, y))
            } else if (isAmavasya) {
                drawCircle(color = vedic.petalPlum, radius = 4.dp.toPx(), center = Offset(x, y))
            } else if (hasFestival) {
                drawCircle(color = vedic.auspicious, radius = 2.8.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}
