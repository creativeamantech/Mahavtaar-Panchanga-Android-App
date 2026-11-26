package com.mahavtaar.panchanga.widget

import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mahavtaar.panchanga.R
import com.mahavtaar.panchanga.domain.model.TattvaElement

internal enum class CountdownTone(val layoutRes: Int) {
    GOLD(R.layout.widget_live_countdown_gold),
    EMERALD(R.layout.widget_live_countdown_emerald),
    ROSE(R.layout.widget_live_countdown_rose),
    CYAN(R.layout.widget_live_countdown_cyan)
}

internal fun tattvaCountdownTone(element: TattvaElement): CountdownTone = when (element) {
    TattvaElement.PRITHVI -> CountdownTone.GOLD
    TattvaElement.JALA -> CountdownTone.CYAN
    TattvaElement.TEJAS -> CountdownTone.ROSE
    TattvaElement.VAYU -> CountdownTone.EMERALD
    TattvaElement.AKASH -> CountdownTone.CYAN
}

/**
 * Outer semi-transparent frosted-glass widget container with a 1dp specular hairline rim
 * and continuous user-adjustable opacity across Day and Night themes.
 */
@Composable
internal fun FrostedGlassShell(
    palette: WidgetGlassPalette,
    outerCornerRadius: Dp = 24.dp,
    contentPadding: Dp = 12.dp,
    onClick: Action? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(outerCornerRadius)
        .background(palette.shellBorder)
        .padding(1.dp)

    Box(modifier = baseModifier) {
        val innerModifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(outerCornerRadius - 1.dp)
            .background(palette.shellBg)
            .padding(contentPadding)
            .let { if (onClick != null) it.clickable(onClick) else it }

        Column(
            modifier = innerModifier,
            content = content
        )
    }
}

/**
 * Native Android <TextClock> ticking continuously on the Home Screen without app wakeups.
 * Automatically follows Day/Night in AUTO mode or applies exact Light/Dark override colors.
 */
@Composable
internal fun LiveWidgetClock(palette: WidgetGlassPalette) {
    val context = LocalContext.current
    val rv = RemoteViews(context.packageName, R.layout.widget_live_clock).apply {
        palette.clockOverrideArgb?.let { setTextColor(R.id.widget_live_text_clock, it) }
    }
    AndroidRemoteViews(remoteViews = rv)
}

/**
 * Native Android <Chronometer countDown="true"> ticking down second-by-second on the Home Screen.
 */
@Composable
internal fun LiveCountdownChip(
    endTimeMs: Long?,
    nowMs: Long,
    tone: CountdownTone,
    palette: WidgetGlassPalette,
    fallbackText: String = ""
) {
    val remainingMs = if (endTimeMs != null) (endTimeMs - nowMs) else 0L
    val fallbackColor = when (tone) {
        CountdownTone.GOLD -> palette.gold
        CountdownTone.EMERALD -> palette.emerald
        CountdownTone.ROSE -> palette.rose
        CountdownTone.CYAN -> palette.cyan
    }
    if (remainingMs > 2_000L && remainingMs < 24 * 3_600_000L) {
        val context = LocalContext.current
        val baseElapsed = SystemClock.elapsedRealtime() + remainingMs
        val rv = RemoteViews(context.packageName, tone.layoutRes).apply {
            setChronometerCountDown(R.id.widget_live_chronometer, true)
            setChronometer(R.id.widget_live_chronometer, baseElapsed, "%s", true)
            palette.countdownOverridesArgb[tone]?.let { setTextColor(R.id.widget_live_chronometer, it) }
        }
        Box(
            modifier = GlanceModifier
                .cornerRadius(999.dp)
                .background(palette.pillBg)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidRemoteViews(remoteViews = rv)
        }
    } else if (fallbackText.isNotBlank()) {
        Text(
            text = fallbackText,
            style = TextStyle(color = fallbackColor, fontSize = 10.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

/**
 * Modern frosted live header bar with LIVE indicator pill, title, city, and live ticking clock
 * (tapping clock pill forces instant refresh; tapping title opens Home).
 */
@Composable
internal fun WidgetLiveHeaderBar(
    title: String,
    city: String,
    palette: WidgetGlassPalette,
    compact: Boolean = false,
    onHeaderClick: Action? = null
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val titleAreaModifier = GlanceModifier
            .defaultWeight()
            .let { if (onHeaderClick != null) it.clickable(onHeaderClick) else it }

        Row(
            modifier = titleAreaModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .cornerRadius(999.dp)
                    .background(palette.livePillBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "● LIVE",
                    style = TextStyle(color = palette.emerald, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = palette.textPrimary,
                        fontSize = if (compact) 11.5.sp else 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                if (!compact && city.isNotBlank()) {
                    Text(
                        text = city,
                        style = TextStyle(color = palette.textSecondary, fontSize = 9.5.sp),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.width(5.dp))

        Box(
            modifier = GlanceModifier
                .cornerRadius(999.dp)
                .background(palette.pillBg)
                .padding(horizontal = 7.dp, vertical = 3.dp)
                .clickable(actionRunCallback<RefreshAllWidgetsAction>()),
            contentAlignment = Alignment.Center
        ) {
            LiveWidgetClock(palette)
        }
    }
}

/**
 * Sculpted frosted-glass Bento tile with subtle 1dp tinted border, translucent surface,
 * live countdown chip, completion progress indicator, and per-tile deep-link action.
 */
@Composable
internal fun BentoLiveMetricTile(
    label: String,
    value: String,
    subtitle: String,
    tone: GlassAccentTone,
    palette: WidgetGlassPalette,
    progress: Float? = null,
    endTimeMs: Long? = null,
    nowMs: Long = System.currentTimeMillis(),
    countdownTone: CountdownTone = CountdownTone.GOLD,
    modifier: GlanceModifier = GlanceModifier,
    compact: Boolean = false,
    onClick: Action? = null
) {
    val accentColor = palette.accentFor(tone)
    val outerTileModifier = modifier
        .cornerRadius(16.dp)
        .background(palette.tileBorderFor(tone))
        .padding(1.dp)
        .let { if (onClick != null) it.clickable(onClick) else it }

    Box(modifier = outerTileModifier) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(15.dp)
                .background(palette.tileBgFor(tone))
                .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 6.dp else 8.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        color = accentColor,
                        fontSize = if (compact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1
                )
                if (endTimeMs != null && endTimeMs > nowMs) {
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    LiveCountdownChip(
                        endTimeMs = endTimeMs,
                        nowMs = nowMs,
                        tone = countdownTone,
                        palette = palette
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            Text(
                text = value,
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = if (compact) 12.sp else 13.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            if (subtitle.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(
                        color = palette.textSecondary,
                        fontSize = if (compact) 9.sp else 10.sp
                    ),
                    maxLines = 1
                )
            }

            if (progress != null) {
                Spacer(modifier = GlanceModifier.height(5.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0.03f, 1f),
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .cornerRadius(4.dp),
                    color = accentColor,
                    backgroundColor = palette.progressTrack
                )
            }
        }
    }
}

/**
 * Compact horizontal glass pill used in the 2x1 and 4x1 Running Now Live Strip widget.
 */
@Composable
internal fun StripGlassPillTile(
    label: String,
    value: String,
    tone: GlassAccentTone,
    palette: WidgetGlassPalette,
    endTimeMs: Long? = null,
    nowMs: Long = System.currentTimeMillis(),
    countdownTone: CountdownTone = CountdownTone.GOLD,
    modifier: GlanceModifier = GlanceModifier,
    onClick: Action? = null
) {
    val accentColor = palette.accentFor(tone)
    val outerModifier = modifier
        .cornerRadius(14.dp)
        .background(palette.tileBorderFor(tone))
        .padding(1.dp)
        .let { if (onClick != null) it.clickable(onClick) else it }

    Box(modifier = outerModifier) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(13.dp)
                .background(palette.tileBgFor(tone))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        color = accentColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1
                )
                if (endTimeMs != null && endTimeMs > nowMs) {
                    Spacer(modifier = GlanceModifier.width(3.dp))
                    LiveCountdownChip(
                        endTimeMs = endTimeMs,
                        nowMs = nowMs,
                        tone = countdownTone,
                        palette = palette
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(1.dp))
            Text(
                text = value,
                style = TextStyle(
                    color = palette.textPrimary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Sleek bottom frosted pill bar for Sunrise/Sunset, Swara, or Masa/Paksha context.
 */
@Composable
internal fun WidgetFooterRibbon(
    leftText: String,
    rightText: String,
    palette: WidgetGlassPalette,
    leftColor: ColorProvider = palette.goldSoft,
    rightColor: ColorProvider = palette.textSecondary,
    onClick: Action? = null
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(999.dp)
            .background(palette.pillBg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .let { if (onClick != null) it.clickable(onClick) else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = leftText,
            style = TextStyle(color = leftColor, fontSize = 10.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1
        )
        if (rightText.isNotBlank()) {
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = rightText,
                style = TextStyle(color = rightColor, fontSize = 9.5.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
        }
    }
}
