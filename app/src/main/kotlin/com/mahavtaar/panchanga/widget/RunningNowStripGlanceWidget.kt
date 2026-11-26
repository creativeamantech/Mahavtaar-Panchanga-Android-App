package com.mahavtaar.panchanga.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mahavtaar.panchanga.domain.model.SwaraNadi

private val STRIP_SIZE_COMPACT_2X1 = DpSize(120.dp, 56.dp)
private val STRIP_SIZE_BAR_4X1 = DpSize(250.dp, 64.dp)
private val STRIP_SIZE_WIDE_5X2 = DpSize(310.dp, 125.dp)

private val STRIP_RESPONSIVE_SIZES = setOf(
    STRIP_SIZE_COMPACT_2X1,
    STRIP_SIZE_BAR_4X1,
    STRIP_SIZE_WIDE_5X2
)

/**
 * WIDGET 4: "रनिंग नाउ लाइव पट्टी" (Running Now Live Strip).
 * Sleek, modern semi-transparent frosted-glass strip resizeable from a 2x1 live capsule
 * to a 4x1 horizontal ticker bar and a 5x2 expanded multi-tile live dashboard.
 */
class RunningNowStripGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(STRIP_RESPONSIVE_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = runCatching { LiveWidgetDataResolver.resolve(context) }
            .getOrElse { LiveWidgetDataResolver.fallbackModel() }
        WidgetLiveRefreshScheduler.scheduleNextRefresh(context, model.nextTransitionAtMs)
        WidgetLiveRefreshScheduler.ensurePeriodicWorker(context)

        provideContent {
            val size = LocalSize.current
            RunningNowStripContent(model, size)
        }
    }

    @Composable
    private fun RunningNowStripContent(m: LiveRunningWidgetModel, size: DpSize) {
        val context = LocalContext.current
        val palette = WidgetGlassThemeResolver.resolve(m.widgetGlassStyle, m.widgetOpacity)
        val isCompactCapsule = size.width < 220.dp
        val isExpandedTwoRow = size.height >= 110.dp

        FrostedGlassShell(
            palette = palette,
            outerCornerRadius = if (isExpandedTwoRow) 22.dp else 20.dp,
            contentPadding = if (isCompactCapsule) 7.dp else 9.dp,
            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HOME)
        ) {
            when {
                isCompactCapsule -> Compact2x1Capsule(m, palette, context)
                !isExpandedTwoRow -> Horizontal4x1Bar(m, palette, context)
                else -> Expanded5x2Ticker(m, palette, context)
            }
        }
    }

    @Composable
    private fun Compact2x1Capsule(
        m: LiveRunningWidgetModel,
        palette: WidgetGlassPalette,
        context: Context
    ) {
        val primaryMuhurta = m.activeMuhurtas.firstOrNull()
        val tattva = m.activeTattva
        val hora = m.activeHora

        when {
            primaryMuhurta != null && !primaryMuhurta.isAuspicious -> {
                StripGlassPillTile(
                    label = if (m.isHindi) "● अशुभ काल" else "● CAUTION",
                    value = primaryMuhurta.name,
                    tone = GlassAccentTone.ROSE,
                    palette = palette,
                    endTimeMs = primaryMuhurta.endMs,
                    nowMs = m.nowMs,
                    countdownTone = CountdownTone.ROSE,
                    modifier = GlanceModifier.fillMaxWidth(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
                )
            }
            tattva != null -> {
                StripGlassPillTile(
                    label = if (m.isHindi) "● तत्त्व · ${m.nadiShort}" else "● TATTVA · ${m.nadiShort}",
                    value = tattva.name,
                    tone = tattvaGlassTone(tattva.element),
                    palette = palette,
                    endTimeMs = tattva.endMs,
                    nowMs = m.nowMs,
                    countdownTone = tattvaCountdownTone(tattva.element),
                    modifier = GlanceModifier.fillMaxWidth(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.TATTVA)
                )
            }
            hora != null -> {
                StripGlassPillTile(
                    label = if (m.isHindi) "● होरा" else "● HORA",
                    value = hora.ruler,
                    tone = GlassAccentTone.GOLD,
                    palette = palette,
                    endTimeMs = hora.endMs,
                    nowMs = m.nowMs,
                    countdownTone = CountdownTone.GOLD,
                    modifier = GlanceModifier.fillMaxWidth(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HORA)
                )
            }
            else -> {
                StripGlassPillTile(
                    label = if (m.isHindi) "● तिथि" else "● TITHI",
                    value = m.tithiName,
                    tone = GlassAccentTone.GOLD,
                    palette = palette,
                    modifier = GlanceModifier.fillMaxWidth(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
                )
            }
        }
    }

    @Composable
    private fun Horizontal4x1Bar(
        m: LiveRunningWidgetModel,
        palette: WidgetGlassPalette,
        context: Context
    ) {
        val hora = m.activeHora
        val tattva = m.activeTattva
        val primaryMuhurta = m.activeMuhurtas.firstOrNull()
        val nadiTone = if (m.flowingNadi == SwaraNadi.IDA) GlassAccentTone.CYAN else GlassAccentTone.GOLD

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill 1: Hora (or Tithi if Hora absent)
            if (hora != null) {
                StripGlassPillTile(
                    label = if (m.isHindi) "होरा" else "HORA",
                    value = hora.ruler,
                    tone = GlassAccentTone.GOLD,
                    palette = palette,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HORA)
                )
            } else {
                StripGlassPillTile(
                    label = if (m.isHindi) "तिथि" else "TITHI",
                    value = m.tithiName,
                    tone = GlassAccentTone.GOLD,
                    palette = palette,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Pill 2: Active Sub-Tattva with live countdown
            if (tattva != null) {
                StripGlassPillTile(
                    label = if (m.isHindi) "तत्त्व" else "TATTVA",
                    value = tattva.name,
                    tone = tattvaGlassTone(tattva.element),
                    palette = palette,
                    endTimeMs = tattva.endMs,
                    nowMs = m.nowMs,
                    countdownTone = tattvaCountdownTone(tattva.element),
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.TATTVA)
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
            }

            // Pill 3: Active Muhurta Alert (if caution/auspicious active) or Flowing Swara Nadi + Tithi
            if (primaryMuhurta != null) {
                val tone = if (primaryMuhurta.isAuspicious) GlassAccentTone.EMERALD else GlassAccentTone.ROSE
                StripGlassPillTile(
                    label = if (primaryMuhurta.isAuspicious) {
                        if (m.isHindi) "मुहूर्त" else "MUHURTA"
                    } else {
                        if (m.isHindi) "चेतावनी" else "ALERT"
                    },
                    value = primaryMuhurta.name,
                    tone = tone,
                    palette = palette,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
                )
            } else {
                StripGlassPillTile(
                    label = if (m.isHindi) "नाड़ी" else "NADI",
                    value = "${m.nadiShort} · ${m.tithiName}",
                    tone = nadiTone,
                    palette = palette,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SWARA)
                )
            }

            Spacer(modifier = GlanceModifier.width(5.dp))

            // Quiet 1-tap live clock refresh capsule
            Box(
                modifier = GlanceModifier
                    .cornerRadius(999.dp)
                    .background(palette.pillBg)
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .clickable(actionRunCallback<RefreshAllWidgetsAction>()),
                contentAlignment = Alignment.Center
            ) {
                LiveWidgetClock(palette)
            }
        }
    }

    @Composable
    private fun Expanded5x2Ticker(
        m: LiveRunningWidgetModel,
        palette: WidgetGlassPalette,
        context: Context
    ) {
        val hora = m.activeHora
        val tattva = m.activeTattva
        val primaryMuhurta = m.activeMuhurtas.firstOrNull()
        val chog = m.activeChoghadiya
        val nadiTone = if (m.flowingNadi == SwaraNadi.IDA) GlassAccentTone.CYAN else GlassAccentTone.GOLD

        WidgetLiveHeaderBar(
            title = m.headerTitle,
            city = m.city,
            palette = palette,
            compact = true,
            onHeaderClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HOME)
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            // Tile 1: Active Tattva & Hora
            if (tattva != null) {
                BentoLiveMetricTile(
                    label = if (m.isHindi) "तत्त्व · होरा" else "TATTVA · HORA",
                    value = tattva.name,
                    subtitle = hora?.let { "${it.ruler} (${it.dayNight})" } ?: tattva.range,
                    tone = tattvaGlassTone(tattva.element),
                    palette = palette,
                    progress = tattva.progress,
                    endTimeMs = tattva.endMs,
                    nowMs = m.nowMs,
                    countdownTone = tattvaCountdownTone(tattva.element),
                    compact = true,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.TATTVA)
                )
            } else {
                BentoLiveMetricTile(
                    label = if (m.isHindi) "तिथि" else "TITHI",
                    value = m.tithiName,
                    subtitle = m.nakshatraName,
                    tone = GlassAccentTone.GOLD,
                    palette = palette,
                    progress = m.tithiProgress,
                    compact = true,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
                )
            }

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Tile 2: Flowing Swara Nadi & Tithi
            BentoLiveMetricTile(
                label = if (m.isHindi) "स्वर · तिथि" else "SWARA · TITHI",
                value = m.nadiShort,
                subtitle = m.tithiName,
                tone = nadiTone,
                palette = palette,
                progress = m.tithiProgress,
                compact = true,
                modifier = GlanceModifier.defaultWeight(),
                onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SWARA)
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            // Tile 3: Live Muhurta / Choghadiya
            if (primaryMuhurta != null) {
                val tone = if (primaryMuhurta.isAuspicious) GlassAccentTone.EMERALD else GlassAccentTone.ROSE
                BentoLiveMetricTile(
                    label = if (primaryMuhurta.isAuspicious) {
                        if (m.isHindi) "मुहूर्त" else "MUHURTA"
                    } else {
                        if (m.isHindi) "अशुभ काल" else "CAUTION"
                    },
                    value = primaryMuhurta.name,
                    subtitle = chog?.name ?: primaryMuhurta.range,
                    tone = tone,
                    palette = palette,
                    progress = primaryMuhurta.progress,
                    endTimeMs = primaryMuhurta.endMs,
                    nowMs = m.nowMs,
                    countdownTone = if (primaryMuhurta.isAuspicious) CountdownTone.EMERALD else CountdownTone.ROSE,
                    compact = true,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
                )
            } else if (chog != null) {
                val tone = if (chog.isAuspicious) GlassAccentTone.EMERALD else GlassAccentTone.ROSE
                BentoLiveMetricTile(
                    label = if (m.isHindi) "चौघड़िया" else "CHOGHADIYA",
                    value = chog.name,
                    subtitle = chog.range,
                    tone = tone,
                    palette = palette,
                    progress = chog.progress,
                    endTimeMs = chog.endMs,
                    nowMs = m.nowMs,
                    countdownTone = if (chog.isAuspicious) CountdownTone.EMERALD else CountdownTone.ROSE,
                    compact = true,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
                )
            } else {
                BentoLiveMetricTile(
                    label = if (m.isHindi) "नक्षत्र" else "NAKSHATRA",
                    value = m.nakshatraName,
                    subtitle = m.tithiEndShort,
                    tone = GlassAccentTone.NEUTRAL,
                    palette = palette,
                    progress = m.nakshatraProgress,
                    compact = true,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
                )
            }
        }
    }
}

class RunningNowStripGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RunningNowStripGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetLiveRefreshScheduler.ensureLiveRefreshActive(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetLiveRefreshScheduler.ensureLiveRefreshActive(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetLiveRefreshScheduler.cancelAllIfNoWidgets(context)
    }
}
