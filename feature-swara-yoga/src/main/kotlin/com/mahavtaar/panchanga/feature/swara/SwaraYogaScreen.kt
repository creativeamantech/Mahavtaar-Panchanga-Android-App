package com.mahavtaar.panchanga.feature.swara

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AngaSegment
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.SwaraWindow
import com.mahavtaar.panchanga.domain.model.SwaraYogaState
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.AtmosphericHeroCard
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.ExpandableInfoCard
import com.mahavtaar.panchanga.ui.components.InspectorBottomSheetFrame
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.LiveNowClarityBanner
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.NadiPolarityArcCanvas
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TattvaBadge
import com.mahavtaar.panchanga.ui.components.TattvaMandalaCanvas
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.components.LiveCountdownWithMutedTimestamps
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.localization.VedicSwaraNavtaraCatalog
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwaraYogaScreen(
    onNavigateBack: () -> Unit,
    onOpenNotificationStudio: () -> Unit = {},
    viewModel: SwaraYogaViewModel = hiltViewModel()
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val controls by viewModel.uiControls.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val policy = LocalAccessPolicy.current
    var inspectedTattva by remember { mutableStateOf<TattvaElement?>(null) }

    val vis = remember(policy) { resolveSwaraVisibility(policy) }
    val visibleTabs = vis.visibleTabs
    val safeTabIdx = controls.selectedTab.coerceIn(0, visibleTabs.lastIndex)
    val activeTab = visibleTabs[safeTabIdx]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.swaraTopBarTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("swara_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                        IconButton(
                            onClick = onOpenNotificationStudio,
                            modifier = Modifier.testTag("swara_open_notif_studio")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = strings.moreNotificationStudioTitle,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    LanguageTogglePill(
                        onToggleLanguage = viewModel::toggleLanguage,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (visibleTabs.size > 1) {
                    PrimaryTabRow(
                        selectedTabIndex = safeTabIdx,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        visibleTabs.forEachIndexed { idx, tabType ->
                            val label = when (tabType) {
                                SwaraTabType.LIVE -> ext.swaraTabLive
                                SwaraTabType.GUIDANCE -> ext.swaraTabGuidance
                                SwaraTabType.TATTVAS -> ext.swaraTabTattvas
                                SwaraTabType.ALIGNMENT -> ext.swaraTabNakshatraAlignment
                            }
                            Tab(
                                selected = safeTabIdx == idx,
                                onClick = { viewModel.selectTab(idx) },
                                modifier = Modifier.testTag("swara_tab_$idx"),
                                text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 720.dp)
                        .animateContentSize(animationSpec = spring())
                        .testTag("swara_yoga_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (val state = content) {
                        is SwaraContentState.Loading -> item { LoadingStateBox() }
                        is SwaraContentState.Error -> item {
                            ErrorStateCard(message = state.message, onRetry = viewModel::retry)
                        }
                        is SwaraContentState.Success -> {
                            val swara = state.swaraState
                            val snap = state.snapshot

                            when (activeTab) {
                                SwaraTabType.LIVE -> {
                                    item(key = "active_nadi") {
                                        ActiveSwaraCard(
                                            swara = swara,
                                            dailyHoras = state.dailyHoras,
                                            timezone = snap.timezone,
                                            nowMs = state.nowMs,
                                            vis = vis,
                                            onOpenAlertRule = if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                                                onOpenNotificationStudio
                                            } else null
                                        )
                                    }
                                    if (vis.showSlokaCard) {
                                        item(key = "swara_sloka") {
                                            SwaraSlokaCard(nadi = swara.currentActiveSwara)
                                        }
                                    }
                                    item(key = "celestial_windows") {
                                        CelestialSwaraWindowsCard(
                                            swara = swara,
                                            dailyHoras = state.dailyHoras,
                                            timezone = snap.timezone,
                                            includeLunarWindows = vis.includeLunarWindows
                                        )
                                    }
                                    if (vis.showBoundarySwaras) {
                                        item(key = "tithi_swara") {
                                            BoundarySwaraCard(
                                                title = strings.tithiBoundarySwaraTitle,
                                                subtitle = strings.tithiBoundarySwaraSubtitle,
                                                segments = snap.tithi,
                                                timezone = snap.timezone,
                                                nowMs = state.nowMs,
                                                nameResolver = { seg -> VedicLocalizer.tithiName(seg.number, seg.name, lang) }
                                            )
                                        }
                                        item(key = "nakshatra_swara") {
                                            BoundarySwaraCard(
                                                title = strings.nakshatraBoundarySwaraTitle,
                                                subtitle = strings.nakshatraBoundarySwaraSubtitle,
                                                segments = snap.nakshatra,
                                                timezone = snap.timezone,
                                                nowMs = state.nowMs,
                                                nameResolver = { seg -> VedicLocalizer.nakshatraName(seg.number, seg.name, lang) }
                                            )
                                        }
                                    }
                                }
                                SwaraTabType.GUIDANCE -> {
                                    if (vis.showGuidanceWorks) {
                                        item(key = "guidance") {
                                            SwaraGuidanceCard(swara = swara)
                                        }
                                    }
                                    if (vis.showTithiRulesTable) {
                                        item(key = "cycle_filters") {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                    SwaraCycleFilter.ALL to ext.filterAllDays,
                                                    SwaraCycleFilter.SHUKLA to ext.filterShukla,
                                                    SwaraCycleFilter.KRISHNA to ext.filterKrishna
                                                ).forEach { (filter, label) ->
                                                    FilterChip(
                                                        selected = controls.cycleFilter == filter,
                                                        onClick = { viewModel.selectCycleFilter(filter) },
                                                        label = { Text(label) },
                                                        modifier = Modifier.testTag("swara_cycle_${filter.name.lowercase()}")
                                                    )
                                                }
                                            }
                                        }
                                        val rules = VedicSwaraNavtaraCatalog.all30TithiSwaraRules.filter { rule ->
                                            when (controls.cycleFilter) {
                                                SwaraCycleFilter.ALL -> true
                                                SwaraCycleFilter.SHUKLA -> rule.isShukla
                                                SwaraCycleFilter.KRISHNA -> !rule.isShukla
                                            }
                                        }
                                        items(rules, key = { "tithi_rule_${it.dayNumber}" }) { rule ->
                                            val isToday = rule.dayNumber == swara.dayNumber
                                            PanchangaCard(
                                                containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                else MaterialTheme.colorScheme.surface
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "${rule.dayNumber}. ${if (lang == AppLanguage.HINDI) "${rule.pakshaHi} ${rule.nameHi}" else "${rule.pakshaEn} ${rule.nameEn}"}",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = "${strings.sunset}: ${VedicLocalizer.nadiShortLabel(rule.sunsetNadi, lang)}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    SwaraNadiBadge(nadi = rule.sunriseNadi)
                                                }
                                            }
                                        }
                                    }
                                }
                                SwaraTabType.TATTVAS -> {
                                    if (vis.showTattvaExplorer) {
                                        val activeTattva = state.dailyHoras.activeTattva?.element
                                        items(VedicSwaraNavtaraCatalog.tattvaExplorerItems, key = { it.element.name }) { item ->
                                            val isActive = item.element == activeTattva
                                            var expanded by remember(item.element) { mutableStateOf(isActive) }
                                            ExpandableInfoCard(
                                                title = VedicLocalizer.tattvaName(item.element, lang),
                                                subtitle = if (lang == AppLanguage.HINDI) "${item.angulasHi} · ${item.shapeColorHi}"
                                                else "${item.angulasEn} · ${item.shapeColorEn}",
                                                expanded = expanded,
                                                onToggleExpand = { expanded = !expanded },
                                                accentColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                leadingBadge = {
                                                    TattvaMandalaCanvas(element = item.element, sizeDp = 42.dp)
                                                },
                                                trailingBadge = {
                                                    StatusPill(
                                                        label = "${item.ratioMins} ${strings.minShort}",
                                                        textColor = MaterialTheme.colorScheme.primary,
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                                    )
                                                }
                                            ) {
                                                Text(
                                                    text = if (lang == AppLanguage.HINDI) item.guidanceHi else item.guidanceEn,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                OutlinedButton(
                                                    onClick = { inspectedTattva = item.element },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Rounded.Spa, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(strings.details)
                                                }
                                            }
                                        }
                                    }
                                }
                                SwaraTabType.ALIGNMENT -> {
                                    if (vis.showNakshatraAlignment) {
                                        item(key = "live_alignment") {
                                            AlignmentEvaluationCard(
                                                title = ext.nakshatraAlignmentTitle,
                                                result = state.liveAlignment
                                            )
                                        }
                                        item(key = "simulator") {
                                            SwaraSimulatorCard(
                                                simulator = controls.simulator,
                                                onUpdate = { s, t, n, p -> viewModel.evaluateSimulator(s, t, n, p) }
                                            )
                                        }
                                        controls.simulator.result?.let { simRes ->
                                            item(key = "sim_result") {
                                                AlignmentEvaluationCard(
                                                    title = ext.swaraSimulatorTitle,
                                                    result = simRes
                                                )
                                            }
                                        }
                                    }
                                    if (vis.showMasterTables) {
                                        item(key = "star_28_table") {
                                            Star28MasterTableCard()
                                        }
                                        item(key = "zodiac_nadi_table") {
                                            ZodiacNadiPolarityTableCard()
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }

            if (vis.showTattvaExplorer) {
                inspectedTattva?.let { el ->
                    val item = VedicSwaraNavtaraCatalog.tattvaExplorerItems.firstOrNull { it.element == el }
                    if (item != null) {
                        InspectorBottomSheetFrame(
                            title = VedicLocalizer.tattvaName(el, lang),
                            subtitle = if (lang == AppLanguage.HINDI) "${item.angulasHi} · ${item.shapeColorHi}" else "${item.angulasEn} · ${item.shapeColorEn}",
                            icon = Icons.Rounded.Spa,
                            accentColor = MaterialTheme.colorScheme.primary,
                            statusLabel = "${item.ratioMins} ${strings.minShort}",
                            sheetTestTag = "swara_tattva_inspector_sheet",
                            onDismiss = { inspectedTattva = null },
                            heroVisual = {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    TattvaMandalaCanvas(element = el, sizeDp = 72.dp)
                                }
                            }
                        ) {
                            Text(
                                text = VedicLocalizer.tattvaQuality(el, lang),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (lang == AppLanguage.HINDI) item.guidanceHi else item.guidanceEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveSwaraCard(
    swara: SwaraYogaState,
    dailyHoras: DailyHoras,
    timezone: String,
    nowMs: Long,
    vis: SwaraVisibilityConfig,
    onOpenAlertRule: (() -> Unit)? = null
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val tithiDisplay = VedicLocalizer.tithiName(swara.dayNumber, swara.tithiName, lang)
    val pakshaDisplay = VedicLocalizer.pakshaName(swara.paksha, lang)

    AtmosphericHeroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${strings.dayNumberPrefix} ${swara.dayNumber} · $tithiDisplay ($pakshaDisplay)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    label = vis.tier.localizedLabel(lang),
                    textColor = MaterialTheme.colorScheme.secondary,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.testTag("swara_tier_pill")
                )
                SwaraNadiBadge(nadi = swara.currentActiveSwara)
                if (onOpenAlertRule != null) {
                    IconButton(
                        onClick = onOpenAlertRule,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("swara_hero_alert_bell")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = ext.setAlert,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LiveNowClarityBanner(
            sentence = VedicLocalizer.simpleNowNadiSentence(swara.currentActiveSwara, lang),
            accentColor = when (swara.currentActiveSwara) {
                SwaraNadi.IDA -> vedic.idaNadi
                SwaraNadi.PINGALA -> vedic.pingalaNadi
                SwaraNadi.SUSHUMNA -> vedic.sushumnaNadi
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (vis.showHeroTattvaAndDetails) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NadiPolarityArcCanvas(
                    activeNadi = swara.currentActiveSwara,
                    modifier = Modifier.size(width = 116.dp, height = 74.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = VedicLocalizer.nadiFullTitle(swara.currentActiveSwara, lang),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("swara_hero_active_title")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${strings.deitiesPrefix}: ${VedicLocalizer.swaraRulingDeities(swara.currentActiveSwara, swara.rulingDeities, lang)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = VedicLocalizer.swaraEnergySummary(swara.currentActiveSwara, swara.energySummary, lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            dailyHoras.activeHora?.let { hora ->
                val horaRemMs = (hora.endTimeMs - nowMs).coerceAtLeast(0L)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${strings.currentHora}: ${VedicLocalizer.horaRulerName(hora.ruler, lang)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (horaRemMs > 0L) {
                    LiveCountdownWithMutedTimestamps(
                        countdownText = "${VedicGlossaryCatalog.formatLiveCountdown(horaRemMs, lang)} ${strings.leftSuffix}",
                        startText = VedicLocalizer.formatDateTimeWithSeconds(hora.startTimeMs, timezone, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(hora.endTimeMs, timezone, lang),
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                    EventStartEndDateTimeView(
                        startText = VedicLocalizer.formatDateTimeWithSeconds(hora.startTimeMs, timezone, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(hora.endTimeMs, timezone, lang),
                        accentColor = MaterialTheme.colorScheme.primary,
                        compact = true
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SwaraNadiBadge(nadi = hora.nadi)
                    dailyHoras.activeTattva?.let { t ->
                        TattvaBadge(element = t.element)
                    }
                }
                dailyHoras.activeTattva?.let { t ->
                    val tattvaRemMs = (t.endTimeMs - nowMs).coerceAtLeast(0L)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (tattvaRemMs > 0L) {
                        LiveCountdownWithMutedTimestamps(
                            countdownText = "${VedicLocalizer.tattvaName(t.element, lang)} · ${VedicGlossaryCatalog.formatLiveCountdown(tattvaRemMs, lang)} ${strings.leftSuffix}",
                            startText = VedicLocalizer.formatDateTimeWithSeconds(t.startTimeMs, timezone, lang),
                            endText = VedicLocalizer.formatDateTimeWithSeconds(t.endTimeMs, timezone, lang),
                            accentColor = vedic.auspicious
                        )
                    } else {
                        EventStartEndDateTimeView(
                            startText = VedicLocalizer.formatDateTimeWithSeconds(t.startTimeMs, timezone, lang),
                            endText = VedicLocalizer.formatDateTimeWithSeconds(t.endTimeMs, timezone, lang),
                            accentColor = vedic.auspicious,
                            compact = true
                        )
                    }
                }
            }
        } else {
            Text(
                text = VedicLocalizer.nadiFullTitle(swara.currentActiveSwara, lang),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.testTag("swara_hero_active_title")
            )
            dailyHoras.activeHora?.let { hora ->
                Spacer(modifier = Modifier.height(6.dp))
                EventStartEndDateTimeView(
                    startText = VedicLocalizer.formatDateTimeWithSeconds(hora.startTimeMs, timezone, lang),
                    endText = VedicLocalizer.formatDateTimeWithSeconds(hora.endTimeMs, timezone, lang),
                    accentColor = MaterialTheme.colorScheme.primary,
                    compact = true
                )
            }
            dailyHoras.activeTattva?.let { t ->
                Spacer(modifier = Modifier.height(6.dp))
                TattvaBadge(element = t.element)
            }
        }
    }
}

@Composable
private fun SwaraSlokaCard(nadi: SwaraNadi) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val (sanskrit, meaning) = VedicSwaraNavtaraCatalog.swaraSloka(nadi, lang)
    PanchangaCard {
        Text(ext.swaraSlokaHeader, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(sanskrit, style = MaterialTheme.typography.titleMedium, fontStyle = FontStyle.Italic)
        Spacer(modifier = Modifier.height(4.dp))
        Text(meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CelestialSwaraWindowsCard(
    swara: SwaraYogaState,
    dailyHoras: DailyHoras,
    timezone: String,
    includeLunarWindows: Boolean
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val windows = buildList {
        add(swara.sunriseWindow)
        add(swara.middayWindow)
        add(swara.sunsetWindow)
        if (includeLunarWindows) {
            swara.moonriseWindow?.let { add(it) }
            swara.moonsetWindow?.let { add(it) }
        }
    }
    val vedic = LocalVedicColors.current
    val title = if (includeLunarWindows) {
        strings.fiveCelestialWindowsTitle
    } else {
        if (lang == AppLanguage.HINDI) "त्रिकाल सूर्य स्वर (सूर्योदय · मध्याह्न · सूर्यास्त)"
        else "Solar Swara Windows (Sunrise · Midday · Sunset)"
    }
    val subtitle = if (includeLunarWindows) {
        strings.fiveCelestialWindowsSubtitle
    } else {
        if (lang == AppLanguage.HINDI) "साधक स्तर — सूर्य संधि एवं सक्रिय नाड़ी"
        else "Sādhaka Tier — Solar transitions & Nadis"
    }

    PanchangaCard {
        SectionHeader(title, subtitle)
        Spacer(modifier = Modifier.height(10.dp))
        windows.forEachIndexed { idx, w ->
            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
            SwaraWindowRow(
                window = w,
                dailyHoras = dailyHoras,
                timezone = timezone,
                activeColor = vedic.auspicious,
                activeBg = vedic.auspiciousContainer
            )
        }
    }
}

@Composable
private fun SwaraWindowRow(
    window: SwaraWindow,
    dailyHoras: DailyHoras,
    timezone: String,
    activeColor: androidx.compose.ui.graphics.Color,
    activeBg: androidx.compose.ui.graphics.Color
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val startMs = window.startTimeMs
    val endMs = window.endTimeMs
    val matchingTattva = if (startMs != null) {
        dailyHoras.horas.asSequence()
            .flatMap { it.tattvas.asSequence() }
            .firstOrNull { startMs in it.startTimeMs until it.endTimeMs }
            ?.element
    } else null

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = VedicLocalizer.swaraWindowLabel(window.label, lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (window.isActive) StatusPill(strings.active, activeColor, activeBg)
                }
                Text(
                    text = VedicLocalizer.swaraWindowRule(window.label, window.ruleDescription, lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (startMs != null && endMs != null) {
                    EventStartEndDateTimeView(
                        startText = VedicLocalizer.formatDateTimeWithSeconds(startMs, timezone, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(endMs, timezone, lang),
                        accentColor = if (window.isActive) activeColor else MaterialTheme.colorScheme.primary,
                        compact = true
                    )
                } else {
                    Text(
                        text = window.windowFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SwaraNadiBadge(nadi = window.nadi)
                matchingTattva?.let { TattvaBadge(element = it) }
            }
        }
        if (window.isActive) {
            LiveNowClarityBanner(
                sentence = VedicLocalizer.simpleNowNadiSentence(window.nadi, lang),
                accentColor = activeColor,
                containerColor = activeBg
            )
        }
    }
}

@Composable
private fun BoundarySwaraCard(
    title: String,
    subtitle: String,
    segments: List<AngaSegment>,
    timezone: String,
    nowMs: Long,
    nameResolver: (AngaSegment) -> String
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val boundarySegments = segments.mapNotNull { seg ->
        val info = seg.swaraInfo ?: return@mapNotNull null
        if (info.startEvent == null && info.endEvent == null) null else Triple(seg, info.startEvent, info.endEvent)
    }

    if (boundarySegments.isEmpty()) return

    PanchangaCard {
        SectionHeader(title = title, subtitle = subtitle)
        Spacer(modifier = Modifier.height(8.dp))
        boundarySegments.forEachIndexed { idx, (seg, startEv, endEv) ->
            if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
            Text(nameResolver(seg), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            startEv?.let { ev ->
                Spacer(modifier = Modifier.height(6.dp))
                val isLive = nowMs in ev.startTimeMs..ev.endTimeMs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.startPrefix,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SwaraNadiBadge(nadi = ev.nadi)
                }
                Spacer(modifier = Modifier.height(4.dp))
                EventStartEndDateTimeView(
                    startText = VedicLocalizer.formatDateTimeWithSeconds(ev.startTimeMs, timezone, lang),
                    endText = VedicLocalizer.formatDateTimeWithSeconds(ev.endTimeMs, timezone, lang),
                    accentColor = if (isLive) vedic.auspicious else MaterialTheme.colorScheme.primary,
                    compact = true
                )
                if (isLive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LiveNowClarityBanner(
                        sentence = VedicLocalizer.simpleNowNadiSentence(ev.nadi, lang),
                        accentColor = vedic.auspicious,
                        containerColor = vedic.auspiciousContainer
                    )
                }
            }
            endEv?.let { ev ->
                Spacer(modifier = Modifier.height(6.dp))
                val isLive = nowMs in ev.startTimeMs..ev.endTimeMs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.endPrefix,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    SwaraNadiBadge(nadi = ev.nadi)
                }
                Spacer(modifier = Modifier.height(4.dp))
                EventStartEndDateTimeView(
                    startText = VedicLocalizer.formatDateTimeWithSeconds(ev.startTimeMs, timezone, lang),
                    endText = VedicLocalizer.formatDateTimeWithSeconds(ev.endTimeMs, timezone, lang),
                    accentColor = if (isLive) vedic.auspicious else MaterialTheme.colorScheme.secondary,
                    compact = true
                )
                if (isLive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LiveNowClarityBanner(
                        sentence = VedicLocalizer.simpleNowNadiSentence(ev.nadi, lang),
                        accentColor = vedic.auspicious,
                        containerColor = vedic.auspiciousContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SwaraGuidanceCard(swara: SwaraYogaState) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val auspicious = VedicLocalizer.swaraAuspiciousWorks(swara.currentActiveSwara, swara.auspiciousWorks, lang)
    val inauspicious = VedicLocalizer.swaraInauspiciousWorks(swara.currentActiveSwara, swara.inauspiciousWorks, lang)

    PanchangaCard {
        SectionHeader(strings.guidanceTitle, VedicLocalizer.nadiFullTitle(swara.currentActiveSwara, lang))
        Spacer(modifier = Modifier.height(10.dp))
        Text(strings.favorableWorks, style = MaterialTheme.typography.labelLarge, color = vedic.auspicious)
        Spacer(modifier = Modifier.height(4.dp))
        auspicious.forEach { work ->
            Text("• $work", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(strings.bestAvoided, style = MaterialTheme.typography.labelLarge, color = vedic.inauspicious)
        Spacer(modifier = Modifier.height(4.dp))
        inauspicious.forEach { work ->
            Text("• $work", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}
