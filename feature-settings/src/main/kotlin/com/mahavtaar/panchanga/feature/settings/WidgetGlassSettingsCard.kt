package com.mahavtaar.panchanga.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.WidgetGlassStyle
import com.mahavtaar.panchanga.ui.components.ResponsiveChoiceGroup
import kotlin.math.roundToInt

/**
 * Interactive Home Screen Widget Glass customization controls with live translucent preview.
 */
@Composable
fun WidgetGlassCustomizationSection(
    glassStyle: WidgetGlassStyle,
    opacity: Float,
    isHindi: Boolean,
    onSelectGlassStyle: (WidgetGlassStyle) -> Unit,
    onChangeOpacity: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val clampedOpacity = opacity.coerceIn(
        UserPreferences.MIN_WIDGET_OPACITY,
        UserPreferences.MAX_WIDGET_OPACITY
    )
    val opacityPercent = (clampedOpacity * 100f).roundToInt()
    val systemDark = isSystemInDarkTheme()
    val previewDark = when (glassStyle) {
        WidgetGlassStyle.AUTO -> systemDark
        WidgetGlassStyle.LIGHT -> false
        WidgetGlassStyle.DARK -> true
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (isHindi) "होम स्क्रीन विजेट ग्लास स्टाइल" else "Home Screen Widget Glass",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isHindi) {
                "सभी होम स्क्रीन विजेट्स की पारदर्शिता व हल्का/गहरा ग्लास चुनें"
            } else {
                "Adjust translucent glass tone and opacity across all home screen widgets"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        ResponsiveChoiceGroup(
            options = WidgetGlassStyle.entries,
            selected = glassStyle,
            labelFor = { style ->
                when (style) {
                    WidgetGlassStyle.AUTO -> if (isHindi) "स्वतः" else "Auto"
                    WidgetGlassStyle.LIGHT -> if (isHindi) "हल्का ग्लास" else "Frosted Light"
                    WidgetGlassStyle.DARK -> if (isHindi) "गहरा ग्लास" else "Smoked Dark"
                }
            },
            tagFor = { "widget_glass_${it.name.lowercase()}" },
            onSelect = onSelectGlassStyle
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isHindi) "ग्लास अपारदर्शिता" else "Glass Opacity",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "$opacityPercent%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("widget_opacity_value")
            )
        }

        Slider(
            value = clampedOpacity,
            onValueChange = onChangeOpacity,
            valueRange = UserPreferences.MIN_WIDGET_OPACITY..UserPreferences.MAX_WIDGET_OPACITY,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("widget_opacity_slider")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Live frosted-glass mini preview over a stylized wallpaper backdrop
        val backdropBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1F3B73),
                Color(0xFF6B4E9B),
                Color(0xFFD98A3C)
            )
        )
        val tileAlpha = (clampedOpacity * 0.72f + 0.16f).coerceIn(0.24f, 0.88f)
        val shellColor = (if (previewDark) Color(0xFF0E1428) else Color(0xFFF8F5EE)).copy(alpha = clampedOpacity)
        val borderColor = if (previewDark) Color(0x66F5C95C) else Color(0xB3FFFFFF)
        val tileColor = (if (previewDark) Color(0xFF1D2948) else Color(0xFFFFFFFF)).copy(alpha = tileAlpha)
        val primaryInk = if (previewDark) Color(0xFFF8F4EC) else Color(0xFF1A1B24)
        val accentGold = if (previewDark) Color(0xFFF5C95C) else Color(0xFF9E5900)
        val accentEmerald = if (previewDark) Color(0xFF4ADE80) else Color(0xFF0F7B47)
        val accentCyan = if (previewDark) Color(0xFF38BDF8) else Color(0xFF0369A1)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(backdropBrush)
                .padding(10.dp)
                .testTag("widget_glass_live_preview")
        ) {
            Surface(
                color = shellColor,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviewGlassChip(
                        label = if (isHindi) "होरा" else "HORA",
                        value = if (isHindi) "गुरु" else "Jupiter",
                        accent = accentGold,
                        ink = primaryInk,
                        bg = tileColor,
                        modifier = Modifier.weight(1f)
                    )
                    PreviewGlassChip(
                        label = if (isHindi) "तत्त्व" else "TATTVA",
                        value = if (isHindi) "पृथ्वी · 14:20" else "Prithvi · 14:20",
                        accent = accentEmerald,
                        ink = primaryInk,
                        bg = tileColor,
                        modifier = Modifier.weight(1f)
                    )
                    PreviewGlassChip(
                        label = if (isHindi) "नाड़ी" else "NADI",
                        value = if (isHindi) "इड़ा" else "Ida",
                        accent = accentCyan,
                        ink = primaryInk,
                        bg = tileColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewGlassChip(
    label: String,
    value: String,
    accent: Color,
    ink: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ink,
                maxLines = 1
            )
        }
    }
}
