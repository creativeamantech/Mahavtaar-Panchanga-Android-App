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
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import com.mahavtaar.panchanga.domain.model.SwaraNadi

/**
 * WIDGET 2: "होरा, पंच-तत्व व स्वर नाड़ी लाइव" (Frosted-Glass Hora, Tattva & Swara Widget).
 * Dedicated resizable widget with adaptive Day/Night frosted glass and per-tile deep-linking.
 */
class HoraTattvaGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_WIDGET_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = runCatching { LiveWidgetDataResolver.resolve(context) }
            .getOrElse { LiveWidgetDataResolver.fallbackModel() }
        WidgetLiveRefreshScheduler.scheduleNextRefresh(context, model.nextTransitionAtMs)
        WidgetLiveRefreshScheduler.ensurePeriodicWorker(context)

        provideContent {
            val size = LocalSize.current
            HoraTattvaResponsiveContent(model, size)
        }
    }

    @Composable
    private fun HoraTattvaResponsiveContent(m: LiveRunningWidgetModel, size: DpSize) {
        val context = LocalContext.current
        val palette = WidgetGlassThemeResolver.resolve(m.widgetGlassStyle, m.widgetOpacity)
        val isCompactWidth = size.width < 220.dp
        val isTall = size.height >= 210.dp
        val hora = m.activeHora
        val tattva = m.activeTattva
        val nadiTone = if (m.flowingNadi == SwaraNadi.IDA) GlassAccentTone.CYAN else GlassAccentTone.GOLD

        FrostedGlassShell(
            palette = palette,
            outerCornerRadius = 24.dp,
            contentPadding = if (isCompactWidth) 10.dp else 12.dp,
            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HORA)
        ) {
            WidgetLiveHeaderBar(
                title = if (m.isHindi) "होरा, तत्त्व व स्वर" else "Hora, Tattva & Swara",
                city = m.city,
                palette = palette,
                compact = isCompactWidth || !isTall,
                onHeaderClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HOME)
            )
            Spacer(modifier = GlanceModifier.height(7.dp))

            if (isCompactWidth) {
                if (hora != null) {
                    BentoLiveMetricTile(
                        label = if (m.isHindi) "होरा (${hora.dayNight})" else "HORA (${hora.dayNight})",
                        value = hora.ruler,
                        subtitle = hora.range,
                        tone = GlassAccentTone.GOLD,
                        palette = palette,
                        progress = hora.progress,
                        endTimeMs = hora.endMs,
                        nowMs = m.nowMs,
                        countdownTone = CountdownTone.GOLD,
                        compact = true,
                        modifier = GlanceModifier.fillMaxWidth(),
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HORA)
                    )
                }
                Spacer(modifier = GlanceModifier.height(5.dp))
                if (tattva != null) {
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
                        modifier = GlanceModifier.fillMaxWidth(),
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.TATTVA)
                    )
                }
            } else {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    if (hora != null) {
                        val nextHint = if (hora.nextRuler.isNotBlank()) " -> ${hora.nextRuler}" else ""
                        BentoLiveMetricTile(
                            label = if (m.isHindi) "सक्रिय होरा (${hora.dayNight})" else "HORA (${hora.dayNight})",
                            value = hora.ruler,
                            subtitle = "${hora.range}$nextHint",
                            tone = GlassAccentTone.GOLD,
                            palette = palette,
                            progress = hora.progress,
                            endTimeMs = hora.endMs,
                            nowMs = m.nowMs,
                            countdownTone = CountdownTone.GOLD,
                            compact = !isTall,
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
                            compact = !isTall,
                            modifier = GlanceModifier.defaultWeight(),
                            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.TATTVA)
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(7.dp))

                if (isTall) {
                    BentoLiveMetricTile(
                        label = if (m.isHindi) "प्रवहमान नाड़ी व स्वर काल" else "FLOWING NADI & SWARA",
                        value = m.nadiFull,
                        subtitle = m.activeSwaraWindowsLine,
                        tone = nadiTone,
                        palette = palette,
                        modifier = GlanceModifier.fillMaxWidth(),
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SWARA)
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    WidgetFooterRibbon(
                        leftText = "${m.tithiName} · ${m.nakshatraName}",
                        rightText = "${m.sunrise} · ${m.sunset}",
                        palette = palette,
                        leftColor = palette.goldSoft,
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
                    )
                } else {
                    WidgetFooterRibbon(
                        leftText = m.nadiFull,
                        rightText = if (m.isHindi) "उदय: ${m.sunriseSwaraShort}" else "SR: ${m.sunriseSwaraShort}",
                        palette = palette,
                        leftColor = palette.cyan,
                        rightColor = palette.goldSoft,
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SWARA)
                    )
                }
            }
        }
    }
}

class HoraTattvaGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HoraTattvaGlanceWidget()

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

/**
 * WIDGET 3: "शुभ मुहूर्त व चौघड़िया लाइव" (Frosted-Glass Muhurta & Choghadiya Widget).
 * Dedicated resizable widget for Auspicious/Inauspicious Muhurtas, Choghadiya, and Panchanga.
 */
class MuhurtaGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_WIDGET_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val model = runCatching { LiveWidgetDataResolver.resolve(context) }
            .getOrElse { LiveWidgetDataResolver.fallbackModel() }
        WidgetLiveRefreshScheduler.scheduleNextRefresh(context, model.nextTransitionAtMs)
        WidgetLiveRefreshScheduler.ensurePeriodicWorker(context)

        provideContent {
            val size = LocalSize.current
            MuhurtaResponsiveContent(model, size)
        }
    }

    @Composable
    private fun MuhurtaResponsiveContent(m: LiveRunningWidgetModel, size: DpSize) {
        val context = LocalContext.current
        val palette = WidgetGlassThemeResolver.resolve(m.widgetGlassStyle, m.widgetOpacity)
        val isCompactWidth = size.width < 220.dp
        val isTall = size.height >= 210.dp
        val topMuhurta = m.activeMuhurtas.firstOrNull()
        val chog = m.activeChoghadiya

        FrostedGlassShell(
            palette = palette,
            outerCornerRadius = 24.dp,
            contentPadding = if (isCompactWidth) 10.dp else 12.dp,
            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
        ) {
            WidgetLiveHeaderBar(
                title = if (m.isHindi) "मुहूर्त व चौघड़िया" else "Muhurta & Choghadiya",
                city = m.city,
                palette = palette,
                compact = isCompactWidth || !isTall,
                onHeaderClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HOME)
            )
            Spacer(modifier = GlanceModifier.height(7.dp))

            val muhTitle = topMuhurta?.name ?: if (m.isHindi) "शुभ / निर्बाध काल" else "Clear Window"
            val muhSub = topMuhurta?.range ?: if (m.isHindi) "राहु काल सक्रिय नहीं" else "No Rahu/Yamaganda"
            val muhTone = if (topMuhurta != null && !topMuhurta.isAuspicious) {
                GlassAccentTone.ROSE
            } else {
                GlassAccentTone.EMERALD
            }
            val muhCountdown = if (muhTone == GlassAccentTone.ROSE) CountdownTone.ROSE else CountdownTone.EMERALD

            if (isCompactWidth) {
                BentoLiveMetricTile(
                    label = if (m.isMuhurtaInauspicious) "KALA ALERT" else "MUHURTA",
                    value = muhTitle,
                    subtitle = muhSub,
                    tone = muhTone,
                    palette = palette,
                    progress = topMuhurta?.progress,
                    endTimeMs = topMuhurta?.endMs,
                    nowMs = m.nowMs,
                    countdownTone = muhCountdown,
                    compact = true,
                    modifier = GlanceModifier.fillMaxWidth(),
                    onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
                )
                if (chog != null) {
                    Spacer(modifier = GlanceModifier.height(5.dp))
                    BentoLiveMetricTile(
                        label = if (m.isHindi) "चौघड़िया" else "CHOGHADIYA",
                        value = chog.name,
                        subtitle = chog.range,
                        tone = if (chog.isAuspicious) GlassAccentTone.GOLD else GlassAccentTone.ROSE,
                        palette = palette,
                        progress = chog.progress,
                        endTimeMs = chog.endMs,
                        nowMs = m.nowMs,
                        countdownTone = if (chog.isAuspicious) CountdownTone.GOLD else CountdownTone.ROSE,
                        compact = true,
                        modifier = GlanceModifier.fillMaxWidth(),
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
                    )
                }
            } else {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    BentoLiveMetricTile(
                        label = if (m.isMuhurtaInauspicious) {
                            if (m.isHindi) "अशुभ काल" else "INAUSPICIOUS"
                        } else {
                            if (m.isHindi) "सक्रिय मुहूर्त" else "LIVE MUHURTA"
                        },
                        value = muhTitle,
                        subtitle = muhSub,
                        tone = muhTone,
                        palette = palette,
                        progress = topMuhurta?.progress,
                        endTimeMs = topMuhurta?.endMs,
                        nowMs = m.nowMs,
                        countdownTone = muhCountdown,
                        compact = !isTall,
                        modifier = GlanceModifier.defaultWeight(),
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
                    )
                    if (chog != null) {
                        Spacer(modifier = GlanceModifier.width(7.dp))
                        BentoLiveMetricTile(
                            label = if (m.isHindi) "चौघड़िया" else "CHOGHADIYA",
                            value = chog.name,
                            subtitle = chog.range,
                            tone = if (chog.isAuspicious) GlassAccentTone.GOLD else GlassAccentTone.ROSE,
                            palette = palette,
                            progress = chog.progress,
                            endTimeMs = chog.endMs,
                            nowMs = m.nowMs,
                            countdownTone = if (chog.isAuspicious) CountdownTone.GOLD else CountdownTone.ROSE,
                            compact = !isTall,
                            modifier = GlanceModifier.defaultWeight(),
                            onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.SCHEDULE)
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(7.dp))

                if (isTall) {
                    BentoLiveMetricTile(
                        label = if (m.isHindi) "तिथि · नक्षत्र · सूर्योदय तिथि" else "TITHI · NAKSHATRA · SUNRISE",
                        value = "${m.tithiName} · ${m.nakshatraName}",
                        subtitle = "${m.sunriseTithiLine} · ${m.tithiEndShort}",
                        tone = GlassAccentTone.NEUTRAL,
                        palette = palette,
                        progress = m.tithiProgress,
                        modifier = GlanceModifier.fillMaxWidth(),
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    WidgetFooterRibbon(
                        leftText = "${m.sunrise}  ·  ${m.sunset}",
                        rightText = m.activeHora?.ruler ?: m.nadiShort,
                        palette = palette,
                        leftColor = palette.gold,
                        rightColor = palette.cyan,
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.HORA)
                    )
                } else {
                    WidgetFooterRibbon(
                        leftText = "${m.tithiName} · ${m.nakshatraName}",
                        rightText = "${m.sunrise} · ${m.sunset}",
                        palette = palette,
                        leftColor = palette.goldSoft,
                        rightColor = palette.textSecondary,
                        onClick = WidgetDeepLinkDestination.launchAction(context, WidgetDeepLinkDestination.PANCHANGA)
                    )
                }
            }
        }
    }
}

class MuhurtaGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MuhurtaGlanceWidget()

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
