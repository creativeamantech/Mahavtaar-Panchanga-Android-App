package com.mahavtaar.panchanga.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahavtaar.panchanga.domain.model.AngaSegment
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SwaraBoundaryEvent
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.SwaraYogaState
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.LiveNowClarityBanner
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TattvaBadge
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

private data class ActiveMuhurtaBadgeItem(
    val label: String,
    val startFormatted: String,
    val endFormatted: String,
    val isAuspicious: Boolean,
    val progress: Float,
    val remainingMs: Long,
    val endMs: Long?
)

/**
 * Flagship Modern Bento "अभी क्या चल रहा है" (Running Right Now) Live Widget Card.
 * Features pulsing live beacon animation, layered Bento tiles with animated progress bars,
 * real-time countdowns for Hora / 5-Sub-Tattva / Choghadiya / Muhurtas, and one-tap
 * Home-Screen Live Widgets Studio launcher.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RunningNowLiveWidgetCard(
    snapshot: PanchangaSnapshot,
    dailyHoras: DailyHoras,
    liveSwara: SwaraYogaState,
    nowMs: Long,
    liveClockText: String,
    onOpenDailyPanchanga: () -> Unit,
    onOpenHora: () -> Unit,
    onOpenSwaraYoga: () -> Unit,
    onOpenTattva: () -> Unit,
    onOpenTodaySchedule: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val isHi = lang == AppLanguage.HINDI
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val policy = LocalAccessPolicy.current
    var showWidgetStudioSheet by remember { mutableStateOf(false) }

    val activeTithi = remember(snapshot.tithi, nowMs) { resolveRunningAnga(snapshot.tithi, nowMs) }
    val sunriseTithi = snapshot.tithi.firstOrNull()
    val activeNak = remember(snapshot.nakshatra, nowMs) { resolveRunningAnga(snapshot.nakshatra, nowMs) }
    val activeYoga = remember(snapshot.yoga, nowMs) { resolveRunningAnga(snapshot.yoga, nowMs) }
    val activeKarana = remember(snapshot.karana, nowMs) { resolveRunningAnga(snapshot.karana, nowMs) }

    val activeHora = dailyHoras.activeHora ?: dailyHoras.horas.firstOrNull()
    val activeTattva = dailyHoras.activeTattva ?: activeHora?.tattvas?.firstOrNull()

    fun inWindow(interval: TimingInterval?): Boolean {
        val s = interval?.startTimeMs ?: return false
        val e = interval.endTimeMs ?: return false
        return nowMs in s..e
    }

    fun progressOf(startMs: Long?, endMs: Long?): Float {
        if (startMs == null || endMs == null || endMs <= startMs) return 0.5f
        return ((nowMs - startMs).toFloat() / (endMs - startMs).toFloat()).coerceIn(0.03f, 1f)
    }

    val tz = snapshot.timezone
    val activeMuhurtas = remember(snapshot, nowMs, lang) {
        buildList {
            fun addIfActive(iv: TimingInterval?, title: String, auspicious: Boolean) {
                if (iv != null && inWindow(iv)) {
                    val rem = ((iv.endTimeMs ?: nowMs) - nowMs).coerceAtLeast(0L)
                    val sText = iv.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.start
                    val eText = iv.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.end
                    add(ActiveMuhurtaBadgeItem(title, sText, eText, auspicious, progressOf(iv.startTimeMs, iv.endTimeMs), rem, iv.endTimeMs))
                }
            }
            addIfActive(snapshot.rahuKala, VedicLocalizer.muhurtaOrPeriodName("Rāhu Kāla", lang), false)
            addIfActive(snapshot.yamaganda, VedicLocalizer.muhurtaOrPeriodName("Yamagaṇḍa", lang), false)
            addIfActive(snapshot.gulikaKala, VedicLocalizer.muhurtaOrPeriodName("Gulika Kāla", lang), false)
            snapshot.durmuhurta.forEach { addIfActive(it, VedicLocalizer.muhurtaOrPeriodName("Durmuhūrta", lang), false) }
            snapshot.varjyam.forEach { addIfActive(it, VedicLocalizer.muhurtaOrPeriodName("Varjyam", lang), false) }
            addIfActive(snapshot.abhijitMuhurta, VedicLocalizer.muhurtaOrPeriodName("Abhijit Muhūrta", lang), true)
            addIfActive(snapshot.brahmaMuhurta, VedicLocalizer.muhurtaOrPeriodName("Brahma Muhūrta", lang), true)
            snapshot.amritaKala.forEach { addIfActive(it, VedicLocalizer.muhurtaOrPeriodName("Amṛta Kāla", lang), true) }
            addIfActive(snapshot.vijayaMuhurta, if (isHi) "विजय मुहूर्त" else "Vijaya Muhūrta", true)
            addIfActive(snapshot.godhuliMuhurta, if (isHi) "गोधूलि मुहूर्त" else "Godhūli Muhūrta", true)
        }
    }

    val activeChog = remember(snapshot, nowMs) {
        (snapshot.gauriChoghadiyaDay + snapshot.gauriChoghadiyaNight).firstOrNull { inWindow(it) }
    }

    val activeTithiSwaraEvent: Pair<String, SwaraBoundaryEvent>? = remember(snapshot.tithi, nowMs, lang) {
        var found: Pair<String, SwaraBoundaryEvent>? = null
        for (seg in snapshot.tithi) {
            val info = seg.swaraInfo ?: continue
            val tName = VedicLocalizer.tithiName(seg.number, seg.name, lang)
            val sEv = info.startEvent
            if (sEv != null && nowMs in sEv.startTimeMs..sEv.endTimeMs) {
                found = tName to sEv; break
            }
            val eEv = info.endEvent
            if (eEv != null && nowMs in eEv.startTimeMs..eEv.endTimeMs) {
                found = tName to eEv; break
            }
        }
        found
    }

    val activeNakshatraSwaraEvent: Pair<String, SwaraBoundaryEvent>? = remember(snapshot.nakshatra, nowMs, lang) {
        var found: Pair<String, SwaraBoundaryEvent>? = null
        for (seg in snapshot.nakshatra) {
            val info = seg.swaraInfo ?: continue
            val nName = VedicLocalizer.nakshatraName(seg.number, seg.name, lang)
            val sEv = info.startEvent
            if (sEv != null && nowMs in sEv.startTimeMs..sEv.endTimeMs) {
                found = nName to sEv; break
            }
            val eEv = info.endEvent
            if (eEv != null && nowMs in eEv.startTimeMs..eEv.endTimeMs) {
                found = nName to eEv; break
            }
        }
        found
    }

    val activeCelestialWindowInfo = remember(liveSwara, isHi) {
        when (liveSwara.activeCelestialWindow) {
            "sunrise" -> (if (isHi) "सूर्योदय स्वर काल" else "Sunrise Swara Window") to liveSwara.sunriseWindow
            "midday" -> (if (isHi) "मध्याह्न स्वर काल" else "Midday Swara Window") to liveSwara.middayWindow
            "sunset" -> (if (isHi) "सूर्यास्त स्वर काल" else "Sunset Swara Window") to liveSwara.sunsetWindow
            "moonrise" -> liveSwara.moonriseWindow?.let { (if (isHi) "चन्द्रोदय स्वर काल" else "Moonrise Swara Window") to it }
            "moonset" -> liveSwara.moonsetWindow?.let { (if (isHi) "चन्द्रास्त स्वर काल" else "Moonset Swara Window") to it }
            else -> null
        }
    }

    val surfaceBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f),
            MaterialTheme.colorScheme.surface
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("running_now_live_widget_card"),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceBrush)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Animated Pulsing LIVE Beacon + Title + Live Clock + Widget Studio Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    PulsingLiveBeacon(color = vedic.auspicious)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isHi) "अभी क्या चल रहा है" else "Running Right Now",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold
                            )
                            StatusPill(
                                label = "LIVE",
                                textColor = vedic.auspicious,
                                containerColor = vedic.auspiciousContainer
                            )
                        }
                        Text(
                            text = "📍 ${snapshot.city}  ·  $liveClockText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { showWidgetStudioSheet = true },
                    modifier = Modifier.testTag("open_home_widget_studio_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = if (isHi) "होम स्क्रीन विजेट जोड़ें" else "Add Home Widget",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isHi) "विजेट स्टूडियो" else "Widgets",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Clear simple-sentence "Now" summary banner
            val headlineMuhurta = activeMuhurtas.firstOrNull { !it.isAuspicious } ?: activeMuhurtas.firstOrNull()
            if (headlineMuhurta != null) {
                LiveNowClarityBanner(
                    sentence = VedicLocalizer.simpleNowIntervalSentence(
                        title = headlineMuhurta.label,
                        isCaution = !headlineMuhurta.isAuspicious,
                        endMs = headlineMuhurta.endMs,
                        nowMs = nowMs,
                        lang = lang
                    ),
                    accentColor = if (headlineMuhurta.isAuspicious) vedic.auspicious else vedic.inauspicious,
                    containerColor = if (headlineMuhurta.isAuspicious) vedic.auspiciousContainer else vedic.inauspiciousContainer
                )
            } else {
                LiveNowClarityBanner(
                    sentence = VedicLocalizer.simpleNowClearWindowSentence(lang),
                    accentColor = vedic.auspicious,
                    containerColor = vedic.auspiciousContainer
                )
            }

            // BENTO ROW 1: Active Tithi & Active Nakshatra Tiles
            val tithiName = activeTithi?.let { VedicLocalizer.tithiName(it.number, it.name, lang) } ?: "—"
            val tithiStartText = activeTithi?.let { VedicLocalizer.formatAngaStart(it, snapshot.sunriseMs, tz, lang) } ?: "—"
            val tithiEndText = activeTithi?.let { VedicLocalizer.formatAngaEnd(it, snapshot.nextSunriseMs, tz, lang) } ?: strings.fullDay
            val nakName = activeNak?.let { VedicLocalizer.nakshatraName(it.number, it.name, lang) } ?: "—"
            val nakStartText = activeNak?.let { VedicLocalizer.formatAngaStart(it, snapshot.sunriseMs, tz, lang) } ?: "—"
            val nakEndText = activeNak?.let { VedicLocalizer.formatAngaEnd(it, snapshot.nextSunriseMs, tz, lang) } ?: strings.fullDay

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiveBentoTile(
                    label = if (isHi) "🌙 सक्रिय तिथि" else "🌙 ACTIVE TITHI",
                    value = tithiName,
                    startText = tithiStartText,
                    endText = tithiEndText,
                    accent = MaterialTheme.colorScheme.primary,
                    progress = progressOf(activeTithi?.startTimeMs, activeTithi?.endTimeMs),
                    nadiBadge = activeTithi?.swaraInfo?.startNadi,
                    onClick = { if (policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)) onOpenDailyPanchanga() },
                    modifier = Modifier.weight(1f)
                )
                LiveBentoTile(
                    label = if (isHi) "✦ सक्रिय नक्षत्र" else "✦ NAKSHATRA",
                    value = nakName,
                    startText = nakStartText,
                    endText = nakEndText,
                    accent = MaterialTheme.colorScheme.tertiary,
                    progress = progressOf(activeNak?.startTimeMs, activeNak?.endTimeMs),
                    nadiBadge = activeNak?.swaraInfo?.startNadi,
                    onClick = { if (policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)) onOpenDailyPanchanga() },
                    modifier = Modifier.weight(1f)
                )
            }

            // Sunrise Tithi + Yoga/Karana Compact Ribbon
            val yogaName = activeYoga?.let { VedicLocalizer.yogaName(it.number, it.name, lang) } ?: "—"
            val karanaName = activeKarana?.let { VedicLocalizer.karanaName(it.name, lang) } ?: "—"
            val srTithiName = sunriseTithi?.let { VedicLocalizer.tithiName(it.number, it.name, lang) } ?: tithiName

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { if (policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)) onOpenDailyPanchanga() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHi) "☀ सूर्योदय तिथि: $srTithiName" else "☀ Sunrise Tithi: $srTithiName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$yogaName · $karanaName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // BENTO ROW 2: Active Hora & Active 5-Sub-Tattva with Live Countdowns & Animated Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeHora != null) {
                    val horaRuler = VedicLocalizer.horaRulerName(activeHora.ruler, lang)
                    val dayNight = if (activeHora.isDay) (if (isHi) "दिन" else "Day") else (if (isHi) "रात्रि" else "Night")
                    val horaRem = (activeHora.endTimeMs - nowMs).coerceAtLeast(0L)
                    LiveBentoTile(
                        label = if (isHi) "⏳ होरा ($dayNight)" else "⏳ HORA ($dayNight)",
                        value = horaRuler,
                        startText = VedicLocalizer.formatDateTimeWithSeconds(activeHora.startTimeMs, tz, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(activeHora.endTimeMs, tz, lang),
                        badgeText = VedicGlossaryCatalog.formatLiveCountdown(horaRem, lang),
                        accent = MaterialTheme.colorScheme.primary,
                        progress = progressOf(activeHora.startTimeMs, activeHora.endTimeMs),
                        nadiBadge = activeHora.nadi,
                        onClick = { if (policy.canAccessPage(AppPageKey.HORA)) onOpenHora() },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (activeTattva != null) {
                    val tattvaLabel = VedicLocalizer.tattvaName(activeTattva.element, lang)
                    val tattvaRem = (activeTattva.endTimeMs - nowMs).coerceAtLeast(0L)
                    LiveBentoTile(
                        label = if (isHi) "🌿 उप-तत्त्व" else "🌿 SUB-TATTVA",
                        value = tattvaLabel,
                        startText = VedicLocalizer.formatDateTimeWithSeconds(activeTattva.startTimeMs, tz, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(activeTattva.endTimeMs, tz, lang),
                        badgeText = VedicGlossaryCatalog.formatLiveCountdown(tattvaRem, lang),
                        accent = elementAccentColor(activeTattva.element),
                        progress = progressOf(activeTattva.startTimeMs, activeTattva.endTimeMs),
                        tattvaBadge = activeTattva.element,
                        onClick = { if (policy.canAccessPage(AppPageKey.TATTVA)) onOpenTattva() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ACTIVE MUHURTAS & CHOGHADIYA LIVE PILLS
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (activeMuhurtas.isEmpty()) {
                    StatusPill(
                        label = if (isHi) "🟢 राहु/अशुभ काल मुक्त समय" else "🟢 Clear Window · No Rahu Kala",
                        textColor = vedic.auspicious,
                        containerColor = vedic.auspiciousContainer
                    )
                } else {
                    activeMuhurtas.forEach { m ->
                        val remStr = VedicGlossaryCatalog.formatLiveCountdown(m.remainingMs, lang)
                        StatusPill(
                            label = "${if (m.isAuspicious) "✨" else "⚠"} ${m.label} (${m.startFormatted} – ${m.endFormatted} · $remStr)",
                            textColor = if (m.isAuspicious) vedic.auspicious else vedic.inauspicious,
                            containerColor = if (m.isAuspicious) vedic.auspiciousContainer else vedic.inauspiciousContainer
                        )
                    }
                }

                if (activeChog != null) {
                    val chogName = VedicLocalizer.muhurtaOrPeriodName(activeChog.name ?: "", lang)
                    val chogRem = ((activeChog.endTimeMs ?: nowMs) - nowMs).coerceAtLeast(0L)
                    val chogStart = activeChog.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: activeChog.start
                    val chogEnd = activeChog.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: activeChog.end
                    StatusPill(
                        label = "${if (isHi) "चौघड़िया" else "Choghadiya"}: $chogName ($chogStart – $chogEnd · ${VedicGlossaryCatalog.formatLiveCountdown(chogRem, lang)})",
                        textColor = if (activeChog.isAuspicious) vedic.auspicious else vedic.caution,
                        containerColor = if (activeChog.isAuspicious) vedic.auspiciousContainer else vedic.cautionContainer
                    )
                }
            }

            // FLOWING SWARA NADI + ACTIVE BOUNDARY WINDOWS
            val flowingNadi = activeHora?.nadi ?: liveSwara.currentActiveSwara
            val nadiFullLabel = when (flowingNadi) {
                SwaraNadi.IDA -> if (isHi) "🌙 इड़ा (वाम / चन्द्र नाड़ी)" else "🌙 Ida (Left / Lunar Nadi)"
                SwaraNadi.PINGALA -> if (isHi) "☀️ पिङ्गला (दक्षिण / सूर्य नाड़ी)" else "☀️ Pingala (Right / Solar Nadi)"
                SwaraNadi.SUSHUMNA -> if (isHi) "✨ सुषुम्ना (मध्य नाड़ी)" else "✨ Sushumna (Center Nadi)"
            }
            val nadiAccent = if (flowingNadi == SwaraNadi.IDA) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { if (policy.canAccessPage(AppPageKey.SWARA_YOGA)) onOpenSwaraYoga() },
                shape = RoundedCornerShape(14.dp),
                color = nadiAccent.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, nadiAccent.copy(alpha = 0.28f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isHi) "प्रवहमान स्वर नाड़ी" else "FLOWING SWARA NADI",
                                style = MaterialTheme.typography.labelSmall,
                                color = nadiAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = nadiFullLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = VedicLocalizer.simpleNowNadiSentence(flowingNadi, lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SwaraNadiBadge(nadi = flowingNadi)
                                activeTattva?.let { TattvaBadge(element = it.element) }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = if (isHi) "स्वर योग खोलें" else "Open Swara Yoga",
                                tint = nadiAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    activeCelestialWindowInfo?.let { (winTitle, win) ->
                        val sText = win.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: win.windowFormatted
                        val eText = win.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: ""
                        SwaraBoundaryActiveBanner(winTitle, win.nadi, sText, eText, lang)
                    }
                    activeTithiSwaraEvent?.let { (tName, ev) ->
                        val typeLabel = if (ev.type == "start") {
                            if (isHi) "तिथि आरंभ स्वर काल ($tName)" else "Tithi Start Swara ($tName)"
                        } else {
                            if (isHi) "तिथि समापन स्वर काल ($tName)" else "Tithi End Swara ($tName)"
                        }
                        SwaraBoundaryActiveBanner(
                            title = typeLabel,
                            nadi = ev.nadi,
                            startText = VedicLocalizer.formatDateTimeWithSeconds(ev.startTimeMs, tz, lang),
                            endText = VedicLocalizer.formatDateTimeWithSeconds(ev.endTimeMs, tz, lang),
                            lang = lang
                        )
                    }
                    activeNakshatraSwaraEvent?.let { (nName, ev) ->
                        val typeLabel = if (ev.type == "start") {
                            if (isHi) "नक्षत्र आरंभ स्वर काल ($nName)" else "Nakshatra Start Swara ($nName)"
                        } else {
                            if (isHi) "नक्षत्र समापन स्वर काल ($nName)" else "Nakshatra End Swara ($nName)"
                        }
                        SwaraBoundaryActiveBanner(
                            title = typeLabel,
                            nadi = ev.nadi,
                            startText = VedicLocalizer.formatDateTimeWithSeconds(ev.startTimeMs, tz, lang),
                            endText = VedicLocalizer.formatDateTimeWithSeconds(ev.endTimeMs, tz, lang),
                            lang = lang
                        )
                    }
                }
            }

            // Quick Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (policy.canAccessPage(AppPageKey.TODAY_SCHEDULE)) {
                    OutlinedButton(
                        onClick = onOpenTodaySchedule,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) "आज की समय-सारणी" else "Day Timeline",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (policy.canAccessPage(AppPageKey.TATTVA)) {
                    OutlinedButton(
                        onClick = onOpenTattva,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Spa, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) "तत्त्व समय खोज" else "Tattva Search",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showWidgetStudioSheet) {
        HomeScreenWidgetsStudioModal(
            snapshot = snapshot,
            dailyHoras = dailyHoras,
            liveSwara = liveSwara,
            nowMs = nowMs,
            liveClockText = liveClockText,
            onDismiss = { showWidgetStudioSheet = false }
        )
    }
}

@Composable
internal fun PulsingLiveBeacon(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_beacon")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beacon_alpha"
    )

    Box(
        modifier = modifier.size(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(color.copy(alpha = pulseAlpha))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun LiveBentoTile(
    label: String,
    value: String,
    startText: String,
    endText: String,
    accent: Color,
    progress: Float,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    nadiBadge: SwaraNadi? = null,
    tattvaBadge: TattvaElement? = null,
    onClick: (() -> Unit)? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.03f, 1f),
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "bento_progress"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(15.dp),
        color = accent.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!badgeText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (nadiBadge != null) {
                SwaraNadiBadge(nadi = nadiBadge)
            }
            if (tattvaBadge != null) {
                TattvaBadge(element = tattvaBadge, showElementWord = false)
            }
            EventStartEndDateTimeView(
                startText = startText,
                endText = endText,
                accentColor = accent,
                compact = true
            )
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.18f)
            )
        }
    }
}

@Composable
private fun SwaraBoundaryActiveBanner(
    title: String,
    nadi: SwaraNadi,
    startText: String,
    endText: String,
    lang: AppLanguage
) {
    val isIda = nadi == SwaraNadi.IDA
    val accent = if (isIda) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val container = if (isIda) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
                if (endText.isNotBlank()) {
                    EventStartEndDateTimeView(startText = startText, endText = endText, accentColor = accent, compact = true)
                } else {
                    Text(
                        text = startText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            SwaraNadiBadge(nadi = nadi)
        }
    }
}

@Composable
private fun elementAccentColor(element: TattvaElement): Color {
    val vedic = LocalVedicColors.current
    return when (element) {
        TattvaElement.PRITHVI -> MaterialTheme.colorScheme.primary
        TattvaElement.JALA -> MaterialTheme.colorScheme.tertiary
        TattvaElement.TEJAS -> vedic.inauspicious
        TattvaElement.VAYU -> vedic.auspicious
        TattvaElement.AKASH -> MaterialTheme.colorScheme.secondary
    }
}

internal fun resolveRunningAnga(segments: List<AngaSegment>, nowMs: Long): AngaSegment? {
    if (segments.isEmpty()) return null
    return segments.firstOrNull { seg ->
        val s = seg.startTimeMs ?: Long.MIN_VALUE
        val e = seg.endTimeMs ?: Long.MAX_VALUE
        nowMs in s..e
    } ?: segments.first()
}
