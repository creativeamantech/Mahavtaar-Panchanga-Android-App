package com.mahavtaar.panchanga.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import com.mahavtaar.panchanga.MainActivity
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.WidgetGlassStyle

/**
 * Per-tile deep-link destinations launched from Jetpack Glance Home Screen widgets.
 */
enum class WidgetDeepLinkDestination(val routeKey: String) {
    HOME("home"),
    PANCHANGA("panchanga"),
    HORA("hora"),
    TATTVA("tattva"),
    SWARA("swara"),
    SCHEDULE("schedule");

    companion object {
        const val EXTRA_WIDGET_DESTINATION = "com.mahavtaar.panchanga.EXTRA_WIDGET_DESTINATION"

        fun fromKey(raw: String?): WidgetDeepLinkDestination? =
            entries.firstOrNull { it.routeKey.equals(raw, ignoreCase = true) }

        fun buildIntent(context: Context, destination: WidgetDeepLinkDestination): Intent =
            Intent(context, MainActivity::class.java).apply {
                action = "com.mahavtaar.panchanga.ACTION_OPEN_${destination.routeKey.uppercase()}"
                putExtra(EXTRA_WIDGET_DESTINATION, destination.routeKey)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        fun launchAction(context: Context, destination: WidgetDeepLinkDestination): Action =
            actionStartActivity(buildIntent(context, destination))
    }
}

internal enum class GlassAccentTone {
    NEUTRAL,
    GOLD,
    EMERALD,
    ROSE,
    CYAN,
    VIOLET
}

/**
 * Resolved semi-transparent frosted-glass palette supporting AUTO (Day/Night), LIGHT, and DARK
 * styles with continuous user-adjustable opacity.
 */
internal data class WidgetGlassPalette(
    val style: WidgetGlassStyle,
    val clampedOpacity: Float,
    val shellBg: GlanceColorProvider,
    val shellBorder: GlanceColorProvider,
    val pillBg: GlanceColorProvider,
    val livePillBg: GlanceColorProvider,
    val textPrimary: GlanceColorProvider,
    val textSecondary: GlanceColorProvider,
    val textSubtle: GlanceColorProvider,
    val progressTrack: GlanceColorProvider,
    val gold: GlanceColorProvider,
    val goldSoft: GlanceColorProvider,
    val emerald: GlanceColorProvider,
    val rose: GlanceColorProvider,
    val cyan: GlanceColorProvider,
    val violet: GlanceColorProvider,
    val tileBgByTone: Map<GlassAccentTone, GlanceColorProvider>,
    val tileBorderByTone: Map<GlassAccentTone, GlanceColorProvider>,
    val clockOverrideArgb: Int?,
    val countdownOverridesArgb: Map<CountdownTone, Int>
) {
    fun accentFor(tone: GlassAccentTone): GlanceColorProvider = when (tone) {
        GlassAccentTone.NEUTRAL -> goldSoft
        GlassAccentTone.GOLD -> gold
        GlassAccentTone.EMERALD -> emerald
        GlassAccentTone.ROSE -> rose
        GlassAccentTone.CYAN -> cyan
        GlassAccentTone.VIOLET -> violet
    }

    fun tileBgFor(tone: GlassAccentTone): GlanceColorProvider =
        tileBgByTone[tone] ?: tileBgByTone.getValue(GlassAccentTone.NEUTRAL)

    fun tileBorderFor(tone: GlassAccentTone): GlanceColorProvider =
        tileBorderByTone[tone] ?: tileBorderByTone.getValue(GlassAccentTone.NEUTRAL)
}

internal fun tattvaGlassTone(element: TattvaElement): GlassAccentTone = when (element) {
    TattvaElement.PRITHVI -> GlassAccentTone.GOLD
    TattvaElement.JALA -> GlassAccentTone.CYAN
    TattvaElement.TEJAS -> GlassAccentTone.ROSE
    TattvaElement.VAYU -> GlassAccentTone.EMERALD
    TattvaElement.AKASH -> GlassAccentTone.VIOLET
}

internal object WidgetGlassThemeResolver {

    // Frosted Pearl Glass (Day) base colors
    private val dayShellBase = Color(0xFFF8F5EE)
    private val dayBorderBase = Color(0xFFFFFFFF)
    private val dayTileBase = Color(0xFFFFFFFF)
    private val dayPillBase = Color(0xFFE7DFCF)
    private val dayPrimaryInk = Color(0xFF1A1B24)
    private val daySecondaryInk = Color(0xFF454C60)
    private val daySubtleInk = Color(0xFF636B82)
    private val dayTrack = Color(0x281A1B24)

    private val dayGold = Color(0xFF9E5900)
    private val dayGoldSoft = Color(0xFF7A4B08)
    private val dayEmerald = Color(0xFF0F7B47)
    private val dayRose = Color(0xFFC62839)
    private val dayCyan = Color(0xFF0369A1)
    private val dayViolet = Color(0xFF6D28D9)

    // Smoked Obsidian Glass (Night) base colors
    private val nightShellBase = Color(0xFF0E1428)
    private val nightBorderBase = Color(0xFFF5C95C)
    private val nightTileBase = Color(0xFF1D2948)
    private val nightPillBase = Color(0xFF233256)
    private val nightPrimaryInk = Color(0xFFF8F4EC)
    private val nightSecondaryInk = Color(0xFFC4CEE6)
    private val nightSubtleInk = Color(0xFF96A7CA)
    private val nightTrack = Color(0x36FFFFFF)

    private val nightGold = Color(0xFFF5C95C)
    private val nightGoldSoft = Color(0xFFFCE4A8)
    private val nightEmerald = Color(0xFF4ADE80)
    private val nightRose = Color(0xFFFB7185)
    private val nightCyan = Color(0xFF38BDF8)
    private val nightViolet = Color(0xFFC084FC)

    fun computeShellAlpha(opacity: Float): Float =
        opacity.coerceIn(UserPreferences.MIN_WIDGET_OPACITY, UserPreferences.MAX_WIDGET_OPACITY)

    /**
     * Inner glass tiles keep a legibility floor even at low shell opacity so text stays crisp over wallpapers.
     */
    fun computeTileAlpha(opacity: Float): Float {
        val shell = computeShellAlpha(opacity)
        return (shell * 0.72f + 0.16f).coerceIn(0.24f, 0.88f)
    }

    fun computeBorderAlpha(opacity: Float): Float {
        val shell = computeShellAlpha(opacity)
        return (0.28f + shell * 0.34f).coerceIn(0.28f, 0.65f)
    }

    fun resolve(style: WidgetGlassStyle, opacity: Float): WidgetGlassPalette {
        val shellAlpha = computeShellAlpha(opacity)
        val tileAlpha = computeTileAlpha(opacity)
        val borderAlpha = computeBorderAlpha(opacity)
        val pillAlpha = (shellAlpha * 0.62f + 0.18f).coerceIn(0.22f, 0.78f)
        val tintMixAlpha = (tileAlpha * 0.24f).coerceIn(0.08f, 0.24f)

        fun pick(day: Color, night: Color): GlanceColorProvider = when (style) {
            WidgetGlassStyle.AUTO -> ColorProvider(day = day, night = night)
            WidgetGlassStyle.LIGHT -> ColorProvider(day = day, night = day)
            WidgetGlassStyle.DARK -> ColorProvider(day = night, night = night)
        }

        fun blendTile(base: Color, tint: Color, alpha: Float): Color {
            val inv = 1f - tintMixAlpha
            val r = (base.red * inv + tint.red * tintMixAlpha).coerceIn(0f, 1f)
            val g = (base.green * inv + tint.green * tintMixAlpha).coerceIn(0f, 1f)
            val b = (base.blue * inv + tint.blue * tintMixAlpha).coerceIn(0f, 1f)
            return Color(red = r, green = g, blue = b, alpha = alpha)
        }

        val tileBgs = mapOf(
            GlassAccentTone.NEUTRAL to pick(
                dayTileBase.copy(alpha = tileAlpha),
                nightTileBase.copy(alpha = tileAlpha)
            ),
            GlassAccentTone.GOLD to pick(
                blendTile(dayTileBase, dayGold, tileAlpha),
                blendTile(nightTileBase, nightGold, tileAlpha)
            ),
            GlassAccentTone.EMERALD to pick(
                blendTile(dayTileBase, dayEmerald, tileAlpha),
                blendTile(nightTileBase, nightEmerald, tileAlpha)
            ),
            GlassAccentTone.ROSE to pick(
                blendTile(dayTileBase, dayRose, tileAlpha),
                blendTile(nightTileBase, nightRose, tileAlpha)
            ),
            GlassAccentTone.CYAN to pick(
                blendTile(dayTileBase, dayCyan, tileAlpha),
                blendTile(nightTileBase, nightCyan, tileAlpha)
            ),
            GlassAccentTone.VIOLET to pick(
                blendTile(dayTileBase, dayViolet, tileAlpha),
                blendTile(nightTileBase, nightViolet, tileAlpha)
            )
        )

        val tileBorders = mapOf(
            GlassAccentTone.NEUTRAL to pick(
                dayPrimaryInk.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.18f)
            ),
            GlassAccentTone.GOLD to pick(
                dayGold.copy(alpha = 0.28f),
                nightGold.copy(alpha = 0.34f)
            ),
            GlassAccentTone.EMERALD to pick(
                dayEmerald.copy(alpha = 0.28f),
                nightEmerald.copy(alpha = 0.34f)
            ),
            GlassAccentTone.ROSE to pick(
                dayRose.copy(alpha = 0.30f),
                nightRose.copy(alpha = 0.38f)
            ),
            GlassAccentTone.CYAN to pick(
                dayCyan.copy(alpha = 0.28f),
                nightCyan.copy(alpha = 0.34f)
            ),
            GlassAccentTone.VIOLET to pick(
                dayViolet.copy(alpha = 0.28f),
                nightViolet.copy(alpha = 0.34f)
            )
        )

        val clockOverride = when (style) {
            WidgetGlassStyle.AUTO -> null
            WidgetGlassStyle.LIGHT -> dayPrimaryInk.toArgb()
            WidgetGlassStyle.DARK -> nightGoldSoft.toArgb()
        }

        val countdownOverrides = when (style) {
            WidgetGlassStyle.AUTO -> emptyMap()
            WidgetGlassStyle.LIGHT -> mapOf(
                CountdownTone.GOLD to dayGold.toArgb(),
                CountdownTone.EMERALD to dayEmerald.toArgb(),
                CountdownTone.ROSE to dayRose.toArgb(),
                CountdownTone.CYAN to dayCyan.toArgb()
            )
            WidgetGlassStyle.DARK -> mapOf(
                CountdownTone.GOLD to nightGold.toArgb(),
                CountdownTone.EMERALD to nightEmerald.toArgb(),
                CountdownTone.ROSE to nightRose.toArgb(),
                CountdownTone.CYAN to nightCyan.toArgb()
            )
        }

        return WidgetGlassPalette(
            style = style,
            clampedOpacity = shellAlpha,
            shellBg = pick(
                dayShellBase.copy(alpha = shellAlpha),
                nightShellBase.copy(alpha = shellAlpha)
            ),
            shellBorder = pick(
                dayBorderBase.copy(alpha = (borderAlpha + 0.18f).coerceAtMost(0.85f)),
                nightBorderBase.copy(alpha = (borderAlpha * 0.72f).coerceIn(0.22f, 0.50f))
            ),
            pillBg = pick(
                dayPillBase.copy(alpha = pillAlpha),
                nightPillBase.copy(alpha = pillAlpha)
            ),
            livePillBg = pick(
                dayEmerald.copy(alpha = 0.15f),
                nightEmerald.copy(alpha = 0.20f)
            ),
            textPrimary = pick(dayPrimaryInk, nightPrimaryInk),
            textSecondary = pick(daySecondaryInk, nightSecondaryInk),
            textSubtle = pick(daySubtleInk, nightSubtleInk),
            progressTrack = pick(dayTrack, nightTrack),
            gold = pick(dayGold, nightGold),
            goldSoft = pick(dayGoldSoft, nightGoldSoft),
            emerald = pick(dayEmerald, nightEmerald),
            rose = pick(dayRose, nightRose),
            cyan = pick(dayCyan, nightCyan),
            violet = pick(dayViolet, nightViolet),
            tileBgByTone = tileBgs,
            tileBorderByTone = tileBorders,
            clockOverrideArgb = clockOverride,
            countdownOverridesArgb = countdownOverrides
        )
    }
}
