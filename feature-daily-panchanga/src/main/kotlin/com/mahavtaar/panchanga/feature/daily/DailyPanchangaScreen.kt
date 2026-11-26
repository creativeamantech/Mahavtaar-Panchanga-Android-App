package com.mahavtaar.panchanga.feature.daily

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.AngaSegment
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.AngaInspectorSheet
import com.mahavtaar.panchanga.ui.components.AuspiciousnessMeterBar
import com.mahavtaar.panchanga.ui.components.DateLocationHeaderBar
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.ExpandableInfoCard
import com.mahavtaar.panchanga.ui.components.LiveNowClarityBanner
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.MuhurtaTimelineBar
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.StickyBackToTodayPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TelemetryStatTile
import com.mahavtaar.panchanga.ui.components.TimingWindowInspectorSheet
import com.mahavtaar.panchanga.ui.components.VedicGlossaryBottomSheet
import com.mahavtaar.panchanga.ui.export.DailyPanchangaHtmlBuilder
import com.mahavtaar.panchanga.ui.export.HtmlReportPreviewDialog
import com.mahavtaar.panchanga.ui.export.OfflineReportExportHelper
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.UiReportFormatter
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

private data class DailyAngaInspection(
    val angaTitle: String,
    val angaSubtitle: String,
    val displayName: String,
    val segment: AngaSegment,
    val nextDisplayName: String?,
    val timezone: String,
    val glossaryTerm: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPanchangaScreen(
    onOpenLocations: () -> Unit,
    viewModel: DailyPanchangaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val context = LocalContext.current
    val policy = LocalAccessPolicy.current
    val canLocations = policy.canAccessPage(AppPageKey.LOCATIONS)
    val canHtmlExport = policy.hasFeature(AppFeatureKey.HTML_EXPORT_PRINT)
    val canGlossary = policy.hasFeature(AppFeatureKey.GLOSSARY_DICTIONARY)
    val canTattvaNadi = policy.hasFeature(AppFeatureKey.TATTVA_NADI_DETAILS)

    var isGridMode by remember { mutableStateOf(false) }
    var glossaryQuery by remember { mutableStateOf<String?>(null) }
    var inspectedAnga by remember { mutableStateOf<DailyAngaInspection?>(null) }
    var inspectedTiming by remember { mutableStateOf<Pair<String, TimingInterval>?>(null) }
    var previewHtmlContent by remember { mutableStateOf<Pair<String, String>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val saveHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri ->
        val successSnap = (state.content as? DailyContentState.Success)?.snapshot ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val html = DailyPanchangaHtmlBuilder.build(successSnap, lang)
            OfflineReportExportHelper.writeTextToUri(context, uri, html)
                .onSuccess { scope.launch { snackbarHostState.showSnackbar(ext.htmlSavedMsg) } }
                .onFailure { err -> scope.launch { snackbarHostState.showSnackbar("${ext.exportActionError}: ${err.message.orEmpty()}") } }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("daily_panchanga_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                val loc = state.preferences.selectedLocation
                DateLocationHeaderBar(
                    cityName = loc.name,
                    timezoneLabel = loc.timezone,
                    liveClockText = state.liveClockText,
                    selectedDate = state.selectedDate,
                    onPreviousDay = viewModel::onPreviousDay,
                    onNextDay = viewModel::onNextDay,
                    onSelectToday = viewModel::onSelectToday,
                    onSelectDate = viewModel::onSelectDate,
                    onLocationClick = { if (canLocations) onOpenLocations() },
                    onToggleLanguage = viewModel::onToggleLanguage,
                    coordinateSubtitle = "%.5f°, %.5f°".format(Locale.US, loc.latitude, loc.longitude)
                )
            }

            item(key = "subtabs") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        PrimaryTabRow(
                            selectedTabIndex = state.selectedSubTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            divider = {}
                        ) {
                            Tab(
                                selected = state.selectedSubTab == 0,
                                onClick = { viewModel.selectSubTab(0) },
                                modifier = Modifier.testTag("daily_tab_angas"),
                                text = {
                                    Text(
                                        text = strings.tabFiveAngas,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                            Tab(
                                selected = state.selectedSubTab == 1,
                                onClick = { viewModel.selectSubTab(1) },
                                modifier = Modifier.testTag("daily_tab_muhurtas"),
                                text = {
                                    Text(
                                        text = strings.tabMuhurtasChoghadiya,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                    if (state.content is DailyContentState.Success) {
                        val snap = (state.content as DailyContentState.Success).snapshot
                        if (canHtmlExport) {
                            FilledTonalIconButton(
                                onClick = {
                                    val html = DailyPanchangaHtmlBuilder.build(snap, lang)
                                    previewHtmlContent = html to "panchanga_${snap.dateIso}.html"
                                },
                                modifier = Modifier.testTag("daily_preview_print_button")
                            ) {
                                Icon(Icons.Rounded.Print, contentDescription = ext.previewAndPrintHtml, modifier = Modifier.size(18.dp))
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    saveHtmlLauncher.launch(OfflineReportExportHelper.sanitizeFileName("panchanga_${snap.dateIso}", "html"))
                                },
                                modifier = Modifier.testTag("daily_save_html_button")
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = ext.saveHtmlReport, modifier = Modifier.size(18.dp))
                            }
                        }
                        FilledTonalIconButton(
                            onClick = {
                                val report = UiReportFormatter.formatDailyPanchanga(snap, lang)
                                UiReportFormatter.shareText(context, ext.shareReport, report)
                            },
                            modifier = Modifier.testTag("daily_share_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = ext.shareReport, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            when (val content = state.content) {
                is DailyContentState.Loading -> item(key = "loading") { LoadingStateBox() }
                is DailyContentState.Error -> item(key = "error") {
                    ErrorStateCard(message = content.message, onRetry = viewModel::onRetry)
                }
                is DailyContentState.Success -> {
                    val snap = content.snapshot
                    item(key = "disha_panchaka_bhadra") {
                        DishaShoolaPanchakaCard(
                            snapshot = snap,
                            onOpenGlossary = { query -> if (canGlossary) glossaryQuery = query }
                        )
                    }

                    if (state.selectedSubTab == 0) {
                        val localizedPaksha = VedicLocalizer.pakshaName(snap.paksha, lang)
                        val localizedMasa = VedicLocalizer.masaName(snap.masaNumber, snap.isAdhika, snap.isKshaya, snap.masa, lang)
                        item(key = "tithi") {
                            AngaDetailCard(
                                title = strings.anga1TithiTitle,
                                subtitle = "$localizedPaksha ${strings.pakshaSuffix} · $localizedMasa",
                                segments = snap.tithi,
                                nameResolver = { seg -> VedicLocalizer.tithiName(seg.number, seg.name, lang) },
                                nowMs = state.nowMs,
                                sunriseMs = snap.sunriseMs,
                                nextSunriseMs = snap.nextSunriseMs,
                                timezone = snap.timezone,
                                glossaryTerm = "Tithi",
                                initiallyExpanded = true,
                                onInfoClick = if (canGlossary) { { glossaryQuery = "Tithi" } } else null,
                                onInspectSegment = { seg, displayName, nextName ->
                                    inspectedAnga = DailyAngaInspection(
                                        angaTitle = strings.anga1TithiTitle,
                                        angaSubtitle = "$localizedPaksha ${strings.pakshaSuffix} · $localizedMasa",
                                        displayName = displayName,
                                        segment = seg,
                                        nextDisplayName = nextName,
                                        timezone = snap.timezone,
                                        glossaryTerm = "Tithi"
                                    )
                                }
                            )
                        }
                        item(key = "nakshatra") {
                            AngaDetailCard(
                                title = strings.anga2NakshatraTitle,
                                subtitle = "${strings.moonRasiPrefix}: ${VedicLocalizer.rasiByName(snap.moonRasi, lang)}",
                                segments = snap.nakshatra,
                                nameResolver = { seg -> VedicLocalizer.nakshatraName(seg.number, seg.name, lang) },
                                nowMs = state.nowMs,
                                sunriseMs = snap.sunriseMs,
                                nextSunriseMs = snap.nextSunriseMs,
                                timezone = snap.timezone,
                                glossaryTerm = "Nak",
                                initiallyExpanded = true,
                                onInfoClick = if (canGlossary) { { glossaryQuery = "Nak" } } else null,
                                onInspectSegment = { seg, displayName, nextName ->
                                    inspectedAnga = DailyAngaInspection(
                                        angaTitle = strings.anga2NakshatraTitle,
                                        angaSubtitle = "${strings.moonRasiPrefix}: ${VedicLocalizer.rasiByName(snap.moonRasi, lang)}",
                                        displayName = displayName,
                                        segment = seg,
                                        nextDisplayName = nextName,
                                        timezone = snap.timezone,
                                        glossaryTerm = "Nak"
                                    )
                                }
                            )
                        }
                        item(key = "yoga") {
                            AngaDetailCard(
                                title = strings.anga3YogaTitle,
                                subtitle = VedicLocalizer.coordinateBannerLabel(snap.ayanamshaKey, snap.coordinateLabel, lang),
                                segments = snap.yoga,
                                nameResolver = { seg -> VedicLocalizer.yogaName(seg.number, seg.name, lang) },
                                nowMs = state.nowMs,
                                sunriseMs = snap.sunriseMs,
                                nextSunriseMs = snap.nextSunriseMs,
                                timezone = snap.timezone,
                                glossaryTerm = "Yoga",
                                initiallyExpanded = false,
                                onInfoClick = if (canGlossary) { { glossaryQuery = "Yoga" } } else null,
                                onInspectSegment = { seg, displayName, nextName ->
                                    inspectedAnga = DailyAngaInspection(
                                        angaTitle = strings.anga3YogaTitle,
                                        angaSubtitle = VedicLocalizer.coordinateBannerLabel(snap.ayanamshaKey, snap.coordinateLabel, lang),
                                        displayName = displayName,
                                        segment = seg,
                                        nextDisplayName = nextName,
                                        timezone = snap.timezone,
                                        glossaryTerm = "Yoga"
                                    )
                                }
                            )
                        }
                        item(key = "karana") {
                            AngaDetailCard(
                                title = strings.anga4KaranaTitle,
                                subtitle = strings.anga4KaranaSubtitle,
                                segments = snap.karana,
                                nameResolver = { seg -> VedicLocalizer.karanaName(seg.name, lang) },
                                nowMs = state.nowMs,
                                sunriseMs = snap.sunriseMs,
                                nextSunriseMs = snap.nextSunriseMs,
                                timezone = snap.timezone,
                                glossaryTerm = "Kara",
                                initiallyExpanded = false,
                                onInfoClick = if (canGlossary) { { glossaryQuery = "Kara" } } else null,
                                onInspectSegment = { seg, displayName, nextName ->
                                    inspectedAnga = DailyAngaInspection(
                                        angaTitle = strings.anga4KaranaTitle,
                                        angaSubtitle = strings.anga4KaranaSubtitle,
                                        displayName = displayName,
                                        segment = seg,
                                        nextDisplayName = nextName,
                                        timezone = snap.timezone,
                                        glossaryTerm = "Kara"
                                    )
                                }
                            )
                        }
                        item(key = "vara") {
                            VaraDetailCard(snapshot = snap)
                        }
                    } else {
                        item(key = "timeline_bar") {
                            MuhurtaTimelineBar(
                                snapshot = snap,
                                nowMs = state.nowMs,
                                modifier = Modifier.testTag("muhurta_timeline_card"),
                                onInspectInterval = { title, interval ->
                                    inspectedTiming = title to interval
                                }
                            )
                        }
                        item(key = "view_mode_toggle") {
                            ViewModeToggleRow(isGridMode = isGridMode, onToggleGridMode = { isGridMode = it })
                        }
                        item(key = "sandhya_special") {
                            SandhyaAndSpecialMuhurtasCard(
                                snapshot = snap,
                                nowMs = state.nowMs,
                                onInspectTiming = { title, interval -> inspectedTiming = title to interval }
                            )
                        }
                        item(key = "auspicious") {
                            MuhurtasSectionCard(
                                snapshot = snap,
                                nowMs = state.nowMs,
                                isGridMode = isGridMode,
                                onInspectTiming = { title, interval -> inspectedTiming = title to interval },
                                onOpenGlossary = { query -> if (canGlossary) glossaryQuery = query }
                            )
                        }
                        item(key = "choghadiya_day") {
                            val srText = VedicLocalizer.formatDateTimeWithSeconds(snap.sunriseMs, snap.timezone, lang)
                            val ssText = VedicLocalizer.formatDateTimeWithSeconds(snap.sunsetMs, snap.timezone, lang)
                            ChoghadiyaCard(
                                title = strings.dayChoghadiyaTitle,
                                subtitle = "${strings.sunrise} $srText → ${strings.sunset} $ssText",
                                intervals = snap.gauriChoghadiyaDay,
                                nowMs = state.nowMs,
                                isGridMode = isGridMode,
                                timezone = snap.timezone,
                                onInspectTiming = { title, interval -> inspectedTiming = title to interval }
                            )
                        }
                        item(key = "choghadiya_night") {
                            val ssText = VedicLocalizer.formatDateTimeWithSeconds(snap.sunsetMs, snap.timezone, lang)
                            val nsrText = VedicLocalizer.formatDateTimeWithSeconds(snap.nextSunriseMs, snap.timezone, lang)
                            ChoghadiyaCard(
                                title = strings.nightChoghadiyaTitle,
                                subtitle = "${strings.sunset} $ssText → ${strings.sunrise} $nsrText",
                                intervals = snap.gauriChoghadiyaNight,
                                nowMs = state.nowMs,
                                isGridMode = isGridMode,
                                timezone = snap.timezone,
                                onInspectTiming = { title, interval -> inspectedTiming = title to interval }
                            )
                        }
                    }
                    item(key = "spacer") { Spacer(modifier = Modifier.height(56.dp)) }
                }
            }
        }
        val todayLocal = remember(state.preferences.selectedLocation.timezone) {
            LocalDate.now(TimeFormatters.safeZoneId(state.preferences.selectedLocation.timezone))
        }
        StickyBackToTodayPill(
            visible = state.selectedDate != todayLocal,
            onBackToToday = viewModel::onSelectToday,
            testTag = "daily_back_to_today_pill",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (canHtmlExport) {
        previewHtmlContent?.let { (html, fileName) ->
            HtmlReportPreviewDialog(
                title = ext.reportPreviewTitle,
                htmlContent = html,
                suggestedFileName = fileName,
                onDismiss = { previewHtmlContent = null },
                onSavedMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
            )
        }
    }

    inspectedAnga?.let { info ->
        AngaInspectorSheet(
            angaTitle = info.angaTitle,
            angaSubtitle = info.angaSubtitle,
            displayName = info.displayName,
            segment = info.segment,
            nextDisplayName = info.nextDisplayName,
            nowMs = state.nowMs,
            timezone = info.timezone,
            glossaryTerm = info.glossaryTerm,
            onDismiss = { inspectedAnga = null },
            onOpenGlossary = { query -> if (canGlossary) glossaryQuery = query }
        )
    }

    inspectedTiming?.let { (title, interval) ->
        val tz = (state.content as? DailyContentState.Success)?.snapshot?.timezone
            ?: state.preferences.selectedLocation.timezone
        TimingWindowInspectorSheet(
            displayTitle = title,
            interval = interval,
            nowMs = state.nowMs,
            onDismiss = { inspectedTiming = null },
            onOpenGlossary = { query -> if (canGlossary) glossaryQuery = query },
            timezone = tz
        )
    }

    if (canGlossary && glossaryQuery != null) {
        VedicGlossaryBottomSheet(
            initialQuery = glossaryQuery.orEmpty(),
            onDismiss = { glossaryQuery = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AngaDetailCard(
    title: String,
    subtitle: String,
    segments: List<AngaSegment>,
    nameResolver: (AngaSegment) -> String,
    nowMs: Long,
    sunriseMs: Long,
    nextSunriseMs: Long,
    timezone: String,
    glossaryTerm: String,
    initiallyExpanded: Boolean,
    onInfoClick: (() -> Unit)?,
    onInspectSegment: (AngaSegment, String, String?) -> Unit
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }

    val primarySegment = segments.firstOrNull { seg ->
        val s = seg.startTimeMs ?: Long.MIN_VALUE
        val e = seg.endTimeMs ?: Long.MAX_VALUE
        nowMs in s..e
    } ?: segments.firstOrNull()

    val primaryStartMs = primarySegment?.startTimeMs
    val primaryEndMs = primarySegment?.endTimeMs
    val primaryActive = primarySegment != null &&
        (primaryStartMs == null || nowMs >= primaryStartMs) &&
        (primaryEndMs == null || nowMs < primaryEndMs)
    val primaryProgress = if (primaryActive && primaryStartMs != null && primaryEndMs != null && primaryEndMs > primaryStartMs) {
        ((nowMs - primaryStartMs).toFloat() / (primaryEndMs - primaryStartMs).toFloat()).coerceIn(0f, 1f)
    } else null
    val primaryRem = if (primaryActive && primaryEndMs != null) (primaryEndMs - nowMs).coerceAtLeast(0L) else null

    ExpandableInfoCard(
        title = title,
        subtitle = subtitle,
        expanded = expanded,
        onToggleExpand = { expanded = !expanded },
        accentColor = MaterialTheme.colorScheme.primary,
        trailingBadge = {
            if (onInfoClick != null) {
                IconButton(onClick = onInfoClick, modifier = Modifier.testTag("anga_glossary_$title")) {
                    Icon(Icons.Rounded.Info, contentDescription = ext.vedicGlossaryTitle, tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        summaryContent = {
            if (primarySegment != null) {
                val pName = nameResolver(primarySegment)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$pName (#${primarySegment.number})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (primaryActive) {
                        StatusPill(strings.active, vedic.auspicious, vedic.auspiciousContainer)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                EventStartEndDateTimeView(
                    startText = VedicLocalizer.formatAngaStart(primarySegment, sunriseMs, timezone, lang),
                    endText = VedicLocalizer.formatAngaEnd(primarySegment, nextSunriseMs, timezone, lang),
                    accentColor = MaterialTheme.colorScheme.primary,
                    compact = true
                )
                primarySegment.swaraInfo?.let { swara ->
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SwaraNadiBadge(nadi = swara.startNadi)
                        if (swara.endNadi != swara.startNadi) {
                            SwaraNadiBadge(nadi = swara.endNadi)
                        }
                    }
                }
                if (primaryActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LiveNowClarityBanner(
                        sentence = VedicLocalizer.simpleNowIntervalSentence(
                            title = "$title: $pName",
                            isCaution = false,
                            endMs = primaryEndMs,
                            nowMs = nowMs,
                            lang = lang
                        ),
                        accentColor = vedic.auspicious,
                        containerColor = vedic.auspiciousContainer
                    )
                }
                if (primaryProgress != null && primaryRem != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AuspiciousnessMeterBar(
                        progress = primaryProgress,
                        accentColor = vedic.auspicious,
                        label = VedicLocalizer.formatAngaEnd(primarySegment, nextSunriseMs, timezone, lang),
                        trailingValue = "${VedicLocalizer.formatDurationRemaining(primaryRem, lang)} ${strings.leftSuffix}"
                    )
                }
            }
        }
    ) {
        segments.forEachIndexed { idx, seg ->
            if (idx > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 4.dp))
            }

            val startMs = seg.startTimeMs
            val endMs = seg.endTimeMs
            val isActive = (startMs == null || nowMs >= startMs) && (endMs == null || nowMs < endMs)
            val totalDurationMs = if (startMs != null && endMs != null) (endMs - startMs).coerceAtLeast(1L) else null
            val remainingMs = if (isActive && endMs != null) (endMs - nowMs).coerceAtLeast(0L) else null
            val nextSeg = segments.getOrNull(idx + 1)
            val displayName = nameResolver(seg)
            val nextName = nextSeg?.let(nameResolver)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$displayName (#${seg.number})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    if (isActive) StatusPill(strings.active, vedic.auspicious, vedic.auspiciousContainer)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelemetryStatTile(
                        label = strings.startsLabel,
                        value = VedicLocalizer.formatAngaStart(seg, sunriseMs, timezone, lang),
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryStatTile(
                        label = strings.endsLabel,
                        value = VedicLocalizer.formatAngaEnd(seg, nextSunriseMs, timezone, lang),
                        valueColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                if ((totalDurationMs != null && totalDurationMs > 1L) || (remainingMs != null && remainingMs > 0L)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (totalDurationMs != null && totalDurationMs > 1L) {
                            Text("${strings.durationPrefix}: ${VedicLocalizer.formatDurationHoursMinutes(totalDurationMs, lang)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (remainingMs != null && remainingMs > 0L) {
                            Text("${strings.remainingPrefix}: ${VedicLocalizer.formatDurationRemaining(remainingMs, lang)}", style = MaterialTheme.typography.labelMedium, color = vedic.auspicious)
                        }
                    }
                }

                if (nextName != null) {
                    Text("${strings.nextTransitionPrefix} → $nextName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }

                seg.swaraInfo?.let { swara ->
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${ext.startNadiLabel}:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SwaraNadiBadge(nadi = swara.startNadi)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${ext.endNadiLabel}:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            SwaraNadiBadge(nadi = swara.endNadi)
                        }
                    }
                }

                OutlinedButton(
                    onClick = { onInspectSegment(seg, displayName, nextName) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.details)
                }
            }
        }
    }
}
