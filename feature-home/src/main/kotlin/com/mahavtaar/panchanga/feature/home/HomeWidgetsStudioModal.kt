package com.mahavtaar.panchanga.feature.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SwaraYogaState
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer

private const val RECEIVER_RUNNING_NOW = "com.mahavtaar.panchanga.widget.PanchangaGlanceWidgetReceiver"
private const val RECEIVER_HORA_TATTVA = "com.mahavtaar.panchanga.widget.HoraTattvaGlanceWidgetReceiver"
private const val RECEIVER_MUHURTA = "com.mahavtaar.panchanga.widget.MuhurtaGlanceWidgetReceiver"

private enum class WidgetPreviewSize(val labelEn: String, val labelHi: String, val tag: String) {
    COMPACT_2X2("2×2 Tile", "२×२ टाइल", "widget_preview_size_2x2"),
    WIDE_4X2("4×2 Wide", "४×२ वाइड", "widget_preview_size_4x2"),
    FULL_4X4("4×4 Full", "४×४ फुल", "widget_preview_size_4x4")
}

private val WidgetGold = Color(0xFFF5C95C)
private val WidgetLight = Color(0xFFF8F4EC)
private val WidgetMuted = Color(0xFFBDC8E2)
private val WidgetEmerald = Color(0xFF4ADE80)
private val WidgetRose = Color(0xFFFB7185)
private val WidgetCyan = Color(0xFF38BDF8)
private val WidgetViolet = Color(0xFFC084FC)

/**
 * Flagship Modal Bottom Sheet displaying animated, true-to-life Bento previews of all 3
 * resizable Home-Screen Live Widgets with an interactive 2×2 / 4×2 / 4×4 size switcher
 * and one-tap Pin to Home Screen actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreenWidgetsStudioModal(
    snapshot: PanchangaSnapshot,
    dailyHoras: DailyHoras,
    liveSwara: SwaraYogaState,
    nowMs: Long = System.currentTimeMillis(),
    liveClockText: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val isHi = lang == AppLanguage.HINDI
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var statusBannerText by remember { mutableStateOf<String?>(null) }
    var selectedSize by remember { mutableStateOf(WidgetPreviewSize.FULL_4X4) }

    val activeTithi = remember(snapshot.tithi, nowMs) { resolveRunningAnga(snapshot.tithi, nowMs) }
    val activeNak = remember(snapshot.nakshatra, nowMs) { resolveRunningAnga(snapshot.nakshatra, nowMs) }
    val activeHora = dailyHoras.activeHora ?: dailyHoras.horas.firstOrNull()
    val activeTattva = dailyHoras.activeTattva ?: activeHora?.tattvas?.firstOrNull()

    fun inWin(iv: TimingInterval?): Boolean {
        val s = iv?.startTimeMs ?: return false
        val e = iv.endTimeMs ?: return false
        return nowMs in s..e
    }

    fun prog(startMs: Long?, endMs: Long?): Float {
        if (startMs == null || endMs == null || endMs <= startMs) return 0.55f
        return ((nowMs - startMs).toFloat() / (endMs - startMs).toFloat()).coerceIn(0.05f, 1f)
    }

    val tz = snapshot.timezone
    fun fmtIv(iv: TimingInterval): String {
        val s = iv.startTimeMs
        val e = iv.endTimeMs
        return if (s != null && e != null) {
            VedicLocalizer.formatDateTimeRangeWithSeconds(s, e, tz, lang)
        } else {
            "${iv.start}–${iv.end}"
        }
    }

    val activeChog = (snapshot.gauriChoghadiyaDay + snapshot.gauriChoghadiyaNight).firstOrNull { inWin(it) }
    val activeMuhurtaPair: Triple<String, String, Boolean>? = when {
        inWin(snapshot.rahuKala) -> Triple(VedicLocalizer.muhurtaOrPeriodName("Rāhu Kāla", lang), fmtIv(snapshot.rahuKala), false)
        inWin(snapshot.yamaganda) -> Triple(VedicLocalizer.muhurtaOrPeriodName("Yamagaṇḍa", lang), fmtIv(snapshot.yamaganda), false)
        inWin(snapshot.abhijitMuhurta) -> Triple(VedicLocalizer.muhurtaOrPeriodName("Abhijit Muhūrta", lang), fmtIv(snapshot.abhijitMuhurta), true)
        inWin(snapshot.brahmaMuhurta) -> Triple(VedicLocalizer.muhurtaOrPeriodName("Brahma Muhūrta", lang), fmtIv(snapshot.brahmaMuhurta), true)
        else -> null
    }

    val tithiName = activeTithi?.let { VedicLocalizer.tithiName(it.number, it.name, lang) } ?: "—"
    val tithiEnd = activeTithi?.endTimeMs?.let {
        val dt = VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang)
        if (isHi) "$dt तक" else "till $dt"
    } ?: activeTithi?.ends?.let { if (isHi) "$it तक" else "till $it" } ?: ""
    val nakName = activeNak?.let { VedicLocalizer.nakshatraName(it.number, it.name, lang) } ?: "—"
    val nakEnd = activeNak?.endTimeMs?.let {
        val dt = VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang)
        if (isHi) "$dt तक" else "till $dt"
    } ?: activeNak?.ends?.let { if (isHi) "$it तक" else "till $it" } ?: ""
    val horaName = activeHora?.let { VedicLocalizer.horaRulerName(it.ruler, lang) } ?: "—"
    val horaRange = activeHora?.let { VedicLocalizer.formatDateTimeRangeWithSeconds(it.startTimeMs, it.endTimeMs, tz, lang) } ?: ""
    val horaRem = activeHora?.let { VedicGlossaryCatalog.formatLiveCountdown((it.endTimeMs - nowMs).coerceAtLeast(0L), lang) } ?: ""
    val tattvaName = activeTattva?.let { VedicLocalizer.tattvaName(it.element, lang) } ?: "—"
    val tattvaRange = activeTattva?.let { VedicLocalizer.formatDateTimeRangeWithSeconds(it.startTimeMs, it.endTimeMs, tz, lang) } ?: ""
    val tattvaRem = activeTattva?.let { VedicGlossaryCatalog.formatLiveCountdown((it.endTimeMs - nowMs).coerceAtLeast(0L), lang) } ?: ""
    val nadiLabel = VedicLocalizer.nadiShortLabel(activeHora?.nadi ?: liveSwara.currentActiveSwara, lang)
    val chogName = activeChog?.let { VedicLocalizer.muhurtaOrPeriodName(it.name ?: "", lang) } ?: (if (isHi) "शुभ काल" else "Clear")
    val sunriseFmt = VedicLocalizer.formatDateTimeWithSeconds(snapshot.sunriseMs, tz, lang)
    val sunsetFmt = VedicLocalizer.formatDateTimeWithSeconds(snapshot.sunsetMs, tz, lang)
    val chogRange = activeChog?.let { fmtIv(it) } ?: "☀ $sunriseFmt"
    val clockDisplay = liveClockText.ifBlank { sunriseFmt }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHi) "होम-स्क्रीन लाइव विजेट स्टूडियो" else "Home-Screen Live Widgets Studio",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isHi) {
                            "फ्रीली रिसाइजेबल लाइव टाइल्स · हर वैदिक बदलाव पर स्वतः ऑटो-रिफ्रेश"
                        } else {
                            "Freely resizable Live Bento Tiles · Auto-refreshes live at every Vedic transition"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Auto-Live Info Pill + Interactive Size Preview Switcher
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PulsingLiveBeacon(color = WidgetEmerald)
                        Text(
                            text = if (isHi) {
                                "जीरो-टच ऑटो लाइव: घड़ी व काउंटडाउन लाइव चलते हैं, रिफ्रेश करने की जरूरत नहीं"
                            } else {
                                "Zero-Touch Auto-Live: Live clock, live second countdowns & automatic boundary sync"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHi) "साइज प्रीव्यू:" else "Preview Size:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        WidgetPreviewSize.entries.forEach { sz ->
                            FilterChip(
                                selected = selectedSize == sz,
                                onClick = { selectedSize = sz },
                                modifier = Modifier.testTag(sz.tag),
                                label = {
                                    Text(
                                        text = if (isHi) sz.labelHi else sz.labelEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }
                }
            }

            statusBannerText?.let { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // WIDGET 1: Midnight Cosmos — अभी क्या चल रहा है (All Running Events)
            StudioWidgetCard(
                badge = if (isHi) "विजेट १ · रिसाइजेबल २×२ से ५×५" else "Widget 1 · Resizable 2×2 → 5×5",
                title = if (isHi) "अभी क्या चल रहा है (Complete Live Dashboard)" else "Running Right Now (Complete Live Bento)",
                description = if (isHi) {
                    "तिथि, नक्षत्र, सूर्योदय तिथि, सक्रिय मुहूर्त, चौघड़िया, होरा, पंच उप-तत्त्व और स्वर नाड़ी — लाइव काउंटडाउन और प्रोग्रेस बार के साथ।"
                } else {
                    "Tithi, Nakshatra, Sunrise Tithi, Muhurtas, Choghadiya, Hora, 5-Sub-Tattva & Swara Nadi with live ticking countdowns & progress bars."
                },
                buttonLabel = if (isHi) "होम स्क्रीन पर जोड़ें (Widget 1)" else "Add to Home Screen (Widget 1)",
                onPinClick = { statusBannerText = requestPinGlanceWidget(context, RECEIVER_RUNNING_NOW, isHi) }
            ) {
                AnimatedContent(
                    targetState = selectedSize,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "w1_preview"
                ) { size ->
                    WidgetSurfaceMock(
                        gradientColors = listOf(Color(0xFF151E3F), Color(0xFF0E1530), Color(0xFF0A1026)),
                        borderColor = WidgetGold.copy(alpha = 0.45f),
                        title = if (isHi) "अभी क्या चल रहा है" else "Running Right Now",
                        city = snapshot.city,
                        clockText = clockDisplay,
                        compact = size == WidgetPreviewSize.COMPACT_2X2
                    ) {
                        when (size) {
                            WidgetPreviewSize.COMPACT_2X2 -> {
                                MiniGlassTile(
                                    label = if (isHi) "तिथि · नक्षत्र" else "TITHI · NAKSHATRA",
                                    value = tithiName,
                                    subtitle = "$nakName · $tithiEnd",
                                    accent = WidgetGold,
                                    progress = prog(activeTithi?.startTimeMs, activeTithi?.endTimeMs)
                                )
                                MiniGlassTile(
                                    label = if (isHi) "⏳ होरा" else "⏳ HORA",
                                    value = horaName,
                                    subtitle = tattvaName,
                                    countdown = horaRem,
                                    accent = WidgetCyan,
                                    progress = prog(activeHora?.startTimeMs, activeHora?.endTimeMs)
                                )
                            }
                            WidgetPreviewSize.WIDE_4X2 -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    MiniGlassTile(
                                        label = if (isHi) "🌙 पंचांग" else "🌙 PANCHANGA",
                                        value = tithiName,
                                        subtitle = "$nakName · $tithiEnd",
                                        accent = WidgetGold,
                                        progress = prog(activeTithi?.startTimeMs, activeTithi?.endTimeMs),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MiniGlassTile(
                                        label = if (isHi) "⏳ होरा व तत्त्व" else "⏳ HORA & TATTVA",
                                        value = "$horaName · $tattvaName",
                                        subtitle = "🌬 $nadiLabel",
                                        countdown = tattvaRem,
                                        accent = WidgetCyan,
                                        progress = prog(activeTattva?.startTimeMs, activeTattva?.endTimeMs),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                MiniFooterStrip(
                                    left = activeMuhurtaPair?.let { "${if (it.third) "✨" else "⚠"} ${it.first} (${it.second})" }
                                        ?: (if (isHi) "🟢 शुभ / राहु मुक्त काल" else "🟢 Clear Window · $chogName"),
                                    right = "☀ $sunriseFmt",
                                    leftColor = if (activeMuhurtaPair?.third == false) WidgetRose else WidgetEmerald
                                )
                            }
                            WidgetPreviewSize.FULL_4X4 -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    MiniGlassTile(
                                        label = if (isHi) "🌙 सक्रिय तिथि" else "🌙 ACTIVE TITHI",
                                        value = tithiName,
                                        subtitle = tithiEnd,
                                        accent = WidgetGold,
                                        progress = prog(activeTithi?.startTimeMs, activeTithi?.endTimeMs),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MiniGlassTile(
                                        label = if (isHi) "✦ नक्षत्र" else "✦ NAKSHATRA",
                                        value = nakName,
                                        subtitle = nakEnd,
                                        accent = WidgetViolet,
                                        progress = prog(activeNak?.startTimeMs, activeNak?.endTimeMs),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    MiniGlassTile(
                                        label = if (isHi) "⏳ सक्रिय होरा" else "⏳ LIVE HORA",
                                        value = horaName,
                                        subtitle = horaRange,
                                        countdown = horaRem,
                                        accent = WidgetGold,
                                        progress = prog(activeHora?.startTimeMs, activeHora?.endTimeMs),
                                        modifier = Modifier.weight(1f)
                                    )
                                    MiniGlassTile(
                                        label = if (isHi) "🌿 उप-तत्त्व" else "🌿 SUB-TATTVA",
                                        value = tattvaName,
                                        subtitle = tattvaRange,
                                        countdown = tattvaRem,
                                        accent = WidgetEmerald,
                                        progress = prog(activeTattva?.startTimeMs, activeTattva?.endTimeMs),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                MiniFooterStrip(
                                    left = activeMuhurtaPair?.let { "${if (it.third) "✨" else "⚠"} ${it.first}" }
                                        ?: (if (isHi) "🟢 शुभ काल · $chogName" else "🟢 Clear · $chogName"),
                                    right = "🌬 $nadiLabel",
                                    leftColor = if (activeMuhurtaPair?.third == false) WidgetRose else WidgetEmerald
                                )
                            }
                        }
                    }
                }
            }

            // WIDGET 2: Celestial Sapphire-Teal — Hora, 5-Sub-Tattva & Swara
            StudioWidgetCard(
                badge = if (isHi) "विजेट २ · रिसाइजेबल २×२ से ५×५" else "Widget 2 · Resizable 2×2 → 5×5",
                title = if (isHi) "होरा, पंच-तत्व एवं स्वर नाड़ी लाइव" else "Hora, Pancha-Tattva & Swara Live",
                description = if (isHi) {
                    "ग्रह होरा, ५ उप-तत्त्व और प्रवहमान इड़ा/पिङ्गला नाड़ी के लाइव काउंटडाउन के लिए समर्पित सैफायर-टील लाइव टाइल।"
                } else {
                    "Dedicated sapphire-teal live tile for Planetary Hora, 5-Sub-Tattva element meter, and Flowing Ida/Pingala Nadi."
                },
                buttonLabel = if (isHi) "होम स्क्रीन पर जोड़ें (Widget 2)" else "Add to Home Screen (Widget 2)",
                onPinClick = { statusBannerText = requestPinGlanceWidget(context, RECEIVER_HORA_TATTVA, isHi) }
            ) {
                WidgetSurfaceMock(
                    gradientColors = listOf(Color(0xFF0F2C3D), Color(0xFF0D2034), Color(0xFF0A1428)),
                    borderColor = WidgetCyan.copy(alpha = 0.45f),
                    title = if (isHi) "होरा, तत्त्व व स्वर" else "Hora, Tattva & Swara",
                    city = snapshot.city,
                    clockText = clockDisplay,
                    compact = selectedSize == WidgetPreviewSize.COMPACT_2X2
                ) {
                    if (selectedSize == WidgetPreviewSize.COMPACT_2X2) {
                        MiniGlassTile(
                            label = if (isHi) "⏳ होरा" else "⏳ HORA",
                            value = horaName,
                            subtitle = horaRange,
                            countdown = horaRem,
                            accent = WidgetGold,
                            progress = prog(activeHora?.startTimeMs, activeHora?.endTimeMs)
                        )
                        MiniGlassTile(
                            label = "🌿 $tattvaName",
                            value = "🌬 $nadiLabel",
                            subtitle = tattvaRem,
                            accent = WidgetCyan,
                            progress = prog(activeTattva?.startTimeMs, activeTattva?.endTimeMs)
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            MiniGlassTile(
                                label = if (isHi) "⏳ सक्रिय होरा" else "⏳ LIVE HORA",
                                value = horaName,
                                subtitle = horaRange,
                                countdown = horaRem,
                                accent = WidgetGold,
                                progress = prog(activeHora?.startTimeMs, activeHora?.endTimeMs),
                                modifier = Modifier.weight(1f)
                            )
                            MiniGlassTile(
                                label = if (isHi) "🌿 उप-तत्त्व" else "🌿 SUB-TATTVA",
                                value = tattvaName,
                                subtitle = tattvaRange,
                                countdown = tattvaRem,
                                accent = WidgetCyan,
                                progress = prog(activeTattva?.startTimeMs, activeTattva?.endTimeMs),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MiniFooterStrip(
                            left = if (isHi) "🌬 प्रवहमान नाड़ी: $nadiLabel" else "🌬 Flowing Nadi: $nadiLabel",
                            right = "☀ ${VedicLocalizer.nadiShortLabel(liveSwara.sunriseSwara, lang)}",
                            leftColor = WidgetCyan
                        )
                    }
                }
            }

            // WIDGET 3: Regal Plum-Amber — Muhurta & Choghadiya Live
            StudioWidgetCard(
                badge = if (isHi) "विजेट ३ · रिसाइजेबल २×२ से ५×५" else "Widget 3 · Resizable 2×2 → 5×5",
                title = if (isHi) "शुभ-अशुभ मुहूर्त एवं चौघड़िया लाइव" else "Muhurta & Choghadiya Live Tile",
                description = if (isHi) {
                    "राहु काल, अभिजित्, ब्रह्म मुहूर्त, अमृत काल और सक्रिय चौघड़िया के लाइव अलर्ट एवं काउंटडाउन।"
                } else {
                    "Sacred plum-amber live tile for Rahu Kala alerts, Abhijit/Brahma/Amrita Muhurta windows, and live Choghadiya."
                },
                buttonLabel = if (isHi) "होम स्क्रीन पर जोड़ें (Widget 3)" else "Add to Home Screen (Widget 3)",
                onPinClick = { statusBannerText = requestPinGlanceWidget(context, RECEIVER_MUHURTA, isHi) }
            ) {
                val muhName = activeMuhurtaPair?.first ?: (if (isHi) "शुभ / निर्बाध काल" else "Clear Window")
                val muhSub = activeMuhurtaPair?.second ?: (if (isHi) "राहु काल सक्रिय नहीं" else "No Rahu Kala")
                val muhAccent = if (activeMuhurtaPair?.third == false) WidgetRose else WidgetEmerald
                WidgetSurfaceMock(
                    gradientColors = listOf(Color(0xFF2E1A36), Color(0xFF1E142E), Color(0xFF131126)),
                    borderColor = WidgetGold.copy(alpha = 0.48f),
                    title = if (isHi) "मुहूर्त व चौघड़िया" else "Muhurta & Choghadiya",
                    city = snapshot.city,
                    clockText = clockDisplay,
                    compact = selectedSize == WidgetPreviewSize.COMPACT_2X2
                ) {
                    if (selectedSize == WidgetPreviewSize.COMPACT_2X2) {
                        MiniGlassTile(
                            label = if (activeMuhurtaPair?.third == false) "⚠ ALERT" else "✨ MUHURTA",
                            value = muhName,
                            subtitle = muhSub,
                            accent = muhAccent,
                            progress = 0.62f
                        )
                        MiniGlassTile(
                            label = if (isHi) "☸ चौघड़िया" else "☸ CHOGHADIYA",
                            value = chogName,
                            subtitle = chogRange,
                            accent = WidgetGold,
                            progress = prog(activeChog?.startTimeMs, activeChog?.endTimeMs)
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            MiniGlassTile(
                                label = if (activeMuhurtaPair?.third == false) "⚠ KALA ALERT" else "✨ LIVE MUHURTA",
                                value = muhName,
                                subtitle = muhSub,
                                accent = muhAccent,
                                progress = 0.62f,
                                modifier = Modifier.weight(1f)
                            )
                            MiniGlassTile(
                                label = if (isHi) "☸ चौघड़िया" else "☸ CHOGHADIYA",
                                value = chogName,
                                subtitle = chogRange,
                                accent = WidgetGold,
                                progress = prog(activeChog?.startTimeMs, activeChog?.endTimeMs),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MiniFooterStrip(
                            left = "$tithiName · $nakName",
                            right = "☀ $sunriseFmt · 🌇 $sunsetFmt",
                            leftColor = WidgetGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StudioWidgetCard(
    badge: String,
    title: String,
    description: String,
    buttonLabel: String,
    onPinClick: () -> Unit,
    previewContent: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            StatusPill(
                label = badge,
                textColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            previewContent()

            Button(
                onClick = onPinClick,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun WidgetSurfaceMock(
    gradientColors: List<Color>,
    borderColor: Color,
    title: String,
    city: String,
    clockText: String,
    compact: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .then(if (compact) Modifier.widthIn(max = 215.dp) else Modifier.fillMaxWidth()),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.2.dp, borderColor),
            shadowElevation = 6.dp,
            color = gradientColors.first()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(gradientColors))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Header Bar matching real Glance WidgetLiveHeaderBar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF0E3826),
                            border = BorderStroke(1.dp, WidgetEmerald.copy(alpha = 0.65f))
                        ) {
                            Text(
                                text = "● LIVE",
                                color = WidgetEmerald,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Column {
                            Text(
                                text = title,
                                color = WidgetLight,
                                fontSize = if (compact) 11.sp else 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!compact) {
                                Text(
                                    text = "📍 $city",
                                    color = WidgetMuted,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x28FFFFFF),
                        border = BorderStroke(1.dp, Color(0x22FFFFFF))
                    ) {
                        Text(
                            text = clockText,
                            color = WidgetGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                content()
            }
        }
    }
}

@Composable
private fun MiniGlassTile(
    label: String,
    value: String,
    subtitle: String,
    accent: Color,
    progress: Float,
    modifier: Modifier = Modifier,
    countdown: String = ""
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "mini_progress"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.36f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (countdown.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = countdown,
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = value,
                color = WidgetLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = WidgetMuted,
                    fontSize = 9.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = Color(0x33FFFFFF)
            )
        }
    }
}

@Composable
private fun MiniFooterStrip(
    left: String,
    right: String,
    leftColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = Color(0x22FFFFFF),
        border = BorderStroke(1.dp, Color(0x1FFFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = left,
                color = leftColor,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = right,
                color = WidgetMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

private fun requestPinGlanceWidget(
    context: Context,
    receiverClassName: String,
    isHindi: Boolean
): String {
    refreshAllPlacedHomeWidgets(context)
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context.packageName, receiverClassName)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val pinned = appWidgetManager.requestPinAppWidget(provider, null, null)
                if (pinned) {
                    val msg = if (isHindi) {
                        "होम स्क्रीन पर विजेट जोड़ने का पॉपअप भेजा गया है! 'Add' पर टैप करें और अपनी पसंद के साइज में रिसाइज करें।"
                    } else {
                        "Pin-to-Home-Screen prompt opened! Tap 'Add' and freely resize it to any size."
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    return@runCatching msg
                }
            }
        }
        val fallbackMsg = if (isHindi) {
            "होम स्क्रीन पर खाली जगह को दबाकर रखें (Long-Press) → 'Widgets' → 'Mahavtaar Panchanga' से यह लाइव विजेट जोड़ें।"
        } else {
            "Long-press an empty space on your Home Screen → tap 'Widgets' → select 'Mahavtaar Panchanga'."
        }
        Toast.makeText(context, fallbackMsg, Toast.LENGTH_LONG).show()
        fallbackMsg
    }.getOrElse {
        if (isHindi) {
            "होम स्क्रीन पर खाली जगह दबाकर रखें → 'Widgets' से 'Mahavtaar Panchanga' विजेट जोड़ें।"
        } else {
            "Long-press your Home Screen → tap 'Widgets' → add 'Mahavtaar Panchanga'."
        }
    }
}

private fun refreshAllPlacedHomeWidgets(context: Context) {
    val receivers = listOf(RECEIVER_RUNNING_NOW, RECEIVER_HORA_TATTVA, RECEIVER_MUHURTA)
    val awm = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return
    for (cls in receivers) {
        runCatching {
            val component = ComponentName(context.packageName, cls)
            val ids = awm.getAppWidgetIds(component)
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                this.component = component
                if (ids.isNotEmpty()) {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
            }
            context.sendBroadcast(intent)
        }
    }
}
