package com.mahavtaar.panchanga.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import com.mahavtaar.panchanga.domain.model.SwaraNadi

internal val SIZE_COMPACT_2X2 = DpSize(130.dp, 120.dp)
internal val SIZE_WIDE_4X2 = DpSize(250.dp, 125.dp)
internal val SIZE_MEDIUM_4X3 = DpSize(250.dp, 215.dp)
internal val SIZE_LARGE_4X4 = DpSize(290.dp, 300.dp)
internal val SIZE_FULL_5X5 = DpSize(350.dp, 410.dp)

internal val RESPONSIVE_WIDGET_SIZES = setOf(
    SIZE_COMPACT_2X2,
    SIZE_WIDE_4X2,
    SIZE_MEDIUM_4X3,
    SIZE_LARGE_4X4,
    SIZE_FULL_5X5
)

/**
 * WIDGET 1: "अभी क्या चल रहा है" (Flagship Modern Frosted-Glass Bento Live Dashboard Widget).
 * Smoothly resizes from a crisp 2x2 Live Tile up to a scrollable 5x5 complete Vedic dashboard,
 * with adaptive Day/Night frosted glass, adjustable opacity, live countdowns, and per-tile deep-linking.
 */
class PanchangaGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_WIDGET_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = runCatching { LiveWidgetDataResolver.resolve(context) }
            .getOrElse { LiveWidgetDataResolver.fallbackModel() }
        WidgetLiveRefreshScheduler.scheduleNextRefresh(context, model.nextTransitionAtMs)
        WidgetLiveRefreshScheduler.ensurePeriodicWorker(context)

        provideContent {
            val size = LocalSize.current
            RunningNowResponsiveContent(model, size)
        }
    }

    @Composable
    private fun RunningNowResponsiveContent(m: LiveRunningWidgetModel, size: DpSize) {
        val context = LocalContext.current
        val palette = WidgetGlassThemeResolver.resolve(m.widgetGlassStyle, m.widgetOpacity)
        val isCompactWidth = size.width < 220.dp
        val isShort = size.height < 185.dp
        val isTall = size.height >= 280.dp

        FrostedGlassShell(
            palette = palette,
            outerCornerRadius = 24.dp,
            contentPadding = if (isCompactWidth) 10.dp else 12.dp,
            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HOME)
        ) {
            WidgetLiveHeaderBar(
                title = m.headerTitle,
                city = m.city,
                palette = palette,
                compact = isCompactWidth || isShort,
                onHeaderClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HOME)
            )

            Spacer(modifier = GlanceModifier.height(if (isCompactWidth) 6.dp else 8.dp))

            when {
                isCompactWidth && isShort -> Compact2x2Dashboard(m, palette, context)
                isShort -> Wide4x2Dashboard(m, palette, context)
                !isTall -> Balanced4x3Dashboard(m, palette, context)
                else -> FullScrollableDashboard(m, palette, context)
            }
        }
    }

    @Composable
    private fun Compact2x2Dashboard(
        m: LiveRunningWidgetModel,
        palette: WidgetGlassPalette,
        context: Context
    ) {
        val primaryMuhurta = m.activeMuhurtas.firstOrNull()
        BentoLiveMetricTile(
            label = if (m.isHindi) "तिथि · नक्षत्र" else "TITHI · NAKSHATRA",
            value = m.tithiName,
            subtitle = "${m.nakshatraName} · ${m.tithiEndShort}",
            tone = GlassAccentTone.GOLD,
            palette = palette,
            progress = m.tithiProgress,
            compact = true,
            modifier = GlanceModifier.fillMaxWidth(),
            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
        )
        Spacer(modifier = GlanceModifier.height(5.dp))
        val hora = m.activeHora
        if (primaryMuhurta != null) {
            val tone = if (primaryMuhurta.isAuspicious) GlassAccentTone.EMERALD else GlassAccentTone.ROSE
            BentoLiveMetricTile(
                label = if (primaryMuhurta.isAuspicious) "MUHURTA" else "ALERT",
                value = primaryMuhurta.name,
                subtitle = primaryMuhurta.range,
                tone = tone,
                palette = palette,
                endTimeMs = primaryMuhurta.endMs,
                nowMs = m.nowMs,
                countdownTone = if (primaryMuhurta.isAuspicious) CountdownTone.EMERALD else CountdownTone.ROSE,
                compact = true,
                modifier = GlanceModifier.fillMaxWidth(),
                onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
            )
        } else if (hora != null) {
            BentoLiveMetricTile(
                label = if (m.isHindi) "सक्रिय होरा" else "HORA",
                value = "${hora.ruler} (${hora.dayNight})",
                subtitle = m.activeTattva?.name ?: m.nadiShort,
                tone = GlassAccentTone.CYAN,
                palette = palette,
                endTimeMs = hora.endMs,
                nowMs = m.nowMs,
                countdownTone = CountdownTone.CYAN,
                compact = true,
                modifier = GlanceModifier.fillMaxWidth(),
                onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HORA)
            )
        }
    }

    @Composable
    private fun Wide4x2Dashboard(
        m: LiveRunningWidgetModel,
        palette: WidgetGlassPalette,
        context: Context
    ) {
        val hora = m.activeHora
        val tattva = m.activeTattva
        val topMuhurta = m.activeMuhurtas.firstOrNull()

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            BentoLiveMetricTile(
                label = if (m.isHindi) "तिथि व नक्षत्र" else "PANCHANGA",
                value = m.tithiName,
                subtitle = "${m.nakshatraName} · ${m.tithiEndShort}",
                tone = GlassAccentTone.GOLD,
                palette = palette,
                progress = m.tithiProgress,
                compact = true,
                modifier = GlanceModifier.defaultWeight(),
                onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
            )
            Spacer(modifier = GlanceModifier.width(7.dp))
            if (hora != null) {
                BentoLiveMetricTile(
                    label = if (m.isHindi) "होरा (${hora.dayNight})" else "HORA (${hora.dayNight})",
                    value = hora.ruler,
                    subtitle = tattva?.let { "${it.name} · ${m.nadiShort}" } ?: m.nadiShort,
                    tone = tattva?.let { tattvaGlassTone(it.element) } ?: GlassAccentTone.CYAN,
                    palette = palette,
                    progress = tattva?.progress ?: hora.progress,
                    endTimeMs = tattva?.endMs ?: hora.endMs,
                    nowMs = m.nowMs,
                    countdownTone = tattva?.let { tattvaCountdownTone(it.element) } ?: CountdownTone.CYAN,
                    compact = true,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HORA)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        val leftRibbon = when {
            topMuhurta != null -> "${if (topMuhurta.isAuspicious) "●" else "!"} ${topMuhurta.name} (${topMuhurta.range})"
            m.activeChoghadiya != null -> "${m.activeChoghadiya.name} (${m.activeChoghadiya.range})"
            else -> m.masaPakshaVaara
        }
        val leftColor = when {
            topMuhurta != null && !topMuhurta.isAuspicious -> palette.rose
            topMuhurta != null -> palette.emerald
            else -> palette.goldSoft
        }
        WidgetFooterRibbon(
            leftText = leftRibbon,
            rightText = m.nadiShort,
            palette = palette,
            leftColor = leftColor,
            rightColor = palette.cyan,
            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
        )
    }

    @Composable
    private fun Balanced4x3Dashboard(
        m: LiveRunningWidgetModel,
        palette: WidgetGlassPalette,
        context: Context
    ) {
        val hora = m.activeHora
        val tattva = m.activeTattva
        val topMuhurta = m.activeMuhurtas.firstOrNull()
        val chog = m.activeChoghadiya

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            BentoLiveMetricTile(
                label = if (m.isHindi) "तिथि" else "TITHI",
                value = m.tithiName,
                subtitle = m.tithiEndShort,
                tone = GlassAccentTone.GOLD,
                palette = palette,
                progress = m.tithiProgress,
                compact = true,
                modifier = GlanceModifier.defaultWeight(),
                onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
            )
            Spacer(modifier = GlanceModifier.width(7.dp))
            BentoLiveMetricTile(
                label = if (m.isHindi) "नक्षत्र" else "NAKSHATRA",
                value = m.nakshatraName,
                subtitle = m.nakshatraEndShort.ifBlank { m.yogaName },
                tone = GlassAccentTone.VIOLET,
                palette = palette,
                progress = m.nakshatraProgress,
                compact = true,
                modifier = GlanceModifier.defaultWeight(),
                onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            if (hora != null) {
                BentoLiveMetricTile(
                    label = if (m.isHindi) "होरा (${hora.dayNight})" else "HORA (${hora.dayNight})",
                    value = hora.ruler,
                    subtitle = hora.range,
                    tone = GlassAccentTone.CYAN,
                    palette = palette,
                    progress = hora.progress,
                    endTimeMs = hora.endMs,
                    nowMs = m.nowMs,
                    countdownTone = CountdownTone.CYAN,
                    compact = true,
                    modifier = GlanceModifier.defaultWeight(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HORA)
                )
            }
            if (tattva != null) {
                Spacer(modifier = GlanceModifier.width(7.dp))
                BentoLiveMetricTile(
                    label = if (m.isHindi) "तत्त्व · ${m.nadiShort}" else "TATTVA · ${m.nadiShort}",
                    value = tattva.name,
                    subtitle = tattva.range,
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
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        val muhurtaTitle = when {
            topMuhurta != null -> topMuhurta.name
            chog != null -> chog.name
            else -> if (m.isHindi) "शुभ / निर्बाध काल" else "Clear Auspicious Window"
        }
        val muhurtaSub = when {
            topMuhurta != null && chog != null -> "${topMuhurta.range} · ${chog.name}"
            topMuhurta != null -> topMuhurta.range
            chog != null -> chog.range
            else -> m.sunMoonLine
        }
        val muhurtaTone = when {
            topMuhurta != null && !topMuhurta.isAuspicious -> GlassAccentTone.ROSE
            topMuhurta != null -> GlassAccentTone.EMERALD
            chog != null && !chog.isAuspicious -> GlassAccentTone.ROSE
            else -> GlassAccentTone.EMERALD
        }
        BentoLiveMetricTile(
            label = if (m.isMuhurtaInauspicious) {
                if (m.isHindi) "अशुभ काल सक्रिय" else "CAUTION WINDOW ACTIVE"
            } else {
                if (m.isHindi) "मुहूर्त व चौघड़िया" else "MUHURTA & CHOGHADIYA"
            },
            value = muhurtaTitle,
            subtitle = muhurtaSub,
            tone = muhurtaTone,
            palette = palette,
            progress = topMuhurta?.progress ?: chog?.progress,
            endTimeMs = topMuhurta?.endMs ?: chog?.endMs,
            nowMs = m.nowMs,
            countdownTone = if (muhurtaTone == GlassAccentTone.ROSE) CountdownTone.ROSE else CountdownTone.EMERALD,
            compact = true,
            modifier = GlanceModifier.fillMaxWidth(),
            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
        )
    }

    @Composable
    private fun FullScrollableDashboard(
        m: LiveRunningWidgetModel,
        palette: WidgetGlassPalette,
        context: Context
    ) {
        val hora = m.activeHora
        val tattva = m.activeTattva
        val chog = m.activeChoghadiya

        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            item(itemId = 1L) {
                WidgetFooterRibbon(
                    leftText = m.masaPakshaVaara,
                    rightText = "${m.sunrise} · ${m.sunset}",
                    palette = palette,
                    leftColor = palette.goldSoft,
                    rightColor = palette.textSecondary,
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
                )
                Spacer(modifier = GlanceModifier.height(7.dp))
            }

            item(itemId = 2L) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    BentoLiveMetricTile(
                        label = if (m.isHindi) "तिथि" else "TITHI",
                        value = m.tithiName,
                        subtitle = "${m.tithiEndShort} · ${m.sunriseTithiShort}",
                        tone = GlassAccentTone.GOLD,
                        palette = palette,
                        progress = m.tithiProgress,
                        modifier = GlanceModifier.defaultWeight(),
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
                    )
                    Spacer(modifier = GlanceModifier.width(7.dp))
                    BentoLiveMetricTile(
                        label = if (m.isHindi) "नक्षत्र" else "NAKSHATRA",
                        value = m.nakshatraName,
                        subtitle = m.nakshatraEndShort.ifBlank { "${m.yogaName} · ${m.karanaName}" },
                        tone = GlassAccentTone.VIOLET,
                        palette = palette,
                        progress = m.nakshatraProgress,
                        modifier = GlanceModifier.defaultWeight(),
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
                    )
                }
                Spacer(modifier = GlanceModifier.height(7.dp))
            }

            item(itemId = 3L) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    if (hora != null) {
                        val nextHint = if (hora.nextRuler.isNotBlank()) " -> ${hora.nextRuler}" else ""
                        BentoLiveMetricTile(
                            label = if (m.isHindi) "होरा (${hora.dayNight})" else "HORA (${hora.dayNight})",
                            value = hora.ruler,
                            subtitle = "${hora.range}$nextHint",
                            tone = GlassAccentTone.CYAN,
                            palette = palette,
                            progress = hora.progress,
                            endTimeMs = hora.endMs,
                            nowMs = m.nowMs,
                            countdownTone = CountdownTone.CYAN,
                            modifier = GlanceModifier.defaultWeight(),
                            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HORA)
                        )
                    }
                    if (tattva != null) {
                        Spacer(modifier = GlanceModifier.width(7.dp))
                        BentoLiveMetricTile(
                            label = if (m.isHindi) "पंच उप-तत्त्व" else "SUB-TATTVA",
                            value = tattva.name,
                            subtitle = tattva.range,
                            tone = tattvaGlassTone(tattva.element),
                            palette = palette,
                            progress = tattva.progress,
                            endTimeMs = tattva.endMs,
                            nowMs = m.nowMs,
                            countdownTone = tattvaCountdownTone(tattva.element),
                            modifier = GlanceModifier.defaultWeight(),
                            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.TATTVA)
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.height(7.dp))
            }

            if (m.activeMuhurtas.isNotEmpty()) {
                items(
                    items = m.activeMuhurtas,
                    itemId = { item -> 100L + item.name.hashCode().toLong() }
                ) { muh ->
                    val tone = if (muh.isAuspicious) GlassAccentTone.EMERALD else GlassAccentTone.ROSE
                    BentoLiveMetricTile(
                        label = if (muh.isAuspicious) {
                            if (m.isHindi) "शुभ मुहूर्त सक्रिय" else "AUSPICIOUS MUHURTA"
                        } else {
                            if (m.isHindi) "अशुभ काल चेतावनी" else "CAUTION WINDOW"
                        },
                        value = muh.name,
                        subtitle = muh.range,
                        tone = tone,
                        palette = palette,
                        progress = muh.progress,
                        endTimeMs = muh.endMs,
                        nowMs = m.nowMs,
                        countdownTone = if (muh.isAuspicious) CountdownTone.EMERALD else CountdownTone.ROSE,
                        compact = true,
                        modifier = GlanceModifier.fillMaxWidth(),
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }

            if (chog != null) {
                item(itemId = 4L) {
                    val tone = if (chog.isAuspicious) GlassAccentTone.EMERALD else GlassAccentTone.ROSE
                    BentoLiveMetricTile(
                        label = if (m.isHindi) "सक्रिय चौघड़िया · योग/करण" else "CHOGHADIYA · YOGA/KARANA",
                        value = chog.name,
                        subtitle = "${chog.range} · ${m.yogaName} / ${m.karanaName}",
                        tone = tone,
                        palette = palette,
                        progress = chog.progress,
                        endTimeMs = chog.endMs,
                        nowMs = m.nowMs,
                        countdownTone = if (chog.isAuspicious) CountdownTone.EMERALD else CountdownTone.ROSE,
                        compact = true,
                        modifier = GlanceModifier.fillMaxWidth(),
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }

            item(itemId = 5L) {
                val nadiTone = if (m.flowingNadi == SwaraNadi.IDA) GlassAccentTone.CYAN else GlassAccentTone.GOLD
                BentoLiveMetricTile(
                    label = if (m.isHindi) "स्वर नाड़ी व संधि काल" else "SWARA NADI & WINDOWS",
                    value = m.nadiFull,
                    subtitle = m.activeSwaraWindowsLine,
                    tone = nadiTone,
                    palette = palette,
                    compact = true,
                    modifier = GlanceModifier.fillMaxWidth(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SWARA)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
            }
        }
    }
}

class PanchangaGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PanchangaGlanceWidget()

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
