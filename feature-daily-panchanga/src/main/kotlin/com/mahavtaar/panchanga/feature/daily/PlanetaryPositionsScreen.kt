package com.mahavtaar.panchanga.feature.daily

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.KundliPlanetEntry
import com.mahavtaar.panchanga.domain.model.PlanetPosition
import com.mahavtaar.panchanga.domain.model.PlanetTransitionEvent
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.AuspiciousnessMeterBar
import com.mahavtaar.panchanga.ui.components.ColorDot
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.ExpandableInfoCard
import com.mahavtaar.panchanga.ui.components.InspectorBottomSheetFrame
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.MoonPhaseCanvas
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.SiderealZodiacWheelCanvas
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TelemetryStatTile
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors

private enum class TransitTimelineFilter { ALL, RASI, NAKSHATRA, TODAY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanetaryPositionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DailyPanchangaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var chartStyle by remember { mutableStateOf(KundliChartStyle.NORTH_INDIAN) }
    var transitFilter by remember { mutableStateOf(TransitTimelineFilter.ALL) }
    var inspectedPlanet by remember { mutableStateOf<PlanetPosition?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.planetsTopBarTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("planets_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    LanguageTogglePill(onToggleLanguage = viewModel::onToggleLanguage, modifier = Modifier.padding(end = 12.dp))
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("planetary_positions_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "planets_tabs") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            shape = SegmentedButtonDefaults.itemShape(0, 3),
                            modifier = Modifier.testTag("planets_tab_grahas")
                        ) { Text(ext.tabGrahaCards) }
                        SegmentedButton(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            shape = SegmentedButtonDefaults.itemShape(1, 3),
                            modifier = Modifier.testTag("planets_tab_chakra")
                        ) { Text(ext.tabTransitChakra) }
                        SegmentedButton(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            shape = SegmentedButtonDefaults.itemShape(2, 3),
                            modifier = Modifier.testTag("planets_tab_transits")
                        ) { Text(ext.tabTransitsTimeline) }
                    }
                }

                when (val content = state.content) {
                    is DailyContentState.Loading -> item { LoadingStateBox() }
                    is DailyContentState.Error -> item { ErrorStateCard(message = content.message, onRetry = viewModel::onRetry) }
                    is DailyContentState.Success -> {
                        val snap = content.snapshot
                        val localizedPhase = VedicLocalizer.lunarPhaseName(snap.lunarPhase.phaseName, lang)
                        item(key = "ephemeris_summary") {
                            PanchangaCard {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(VedicLocalizer.coordinateBannerLabel(snap.ayanamshaKey, snap.coordinateLabel, lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(snap.ayanamshaDegrees?.let { "${strings.ayanamshaPrefix}: %.4f°".format(it) } ?: strings.tropicalSayanaLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${strings.solarNoonLabel}: ${snap.solarNoon} · $localizedPhase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    MoonPhaseCanvas(illuminationPercent = snap.lunarPhase.illuminationPercent, isWaxing = snap.lunarPhase.isWaxing, phaseLabel = localizedPhase, diameter = 50.dp)
                                }
                            }
                        }

                        when (selectedTab) {
                            0 -> {
                                item(key = "sidereal_zodiac_wheel") {
                                    val vedic = LocalVedicColors.current
                                    PanchangaCard {
                                        SectionHeader(
                                            title = if (lang == AppLanguage.HINDI) "360° निरयण राशिचक्र एवं २७ नक्षत्र" else "360° Sidereal Rashichakra & 27 Nakshatras",
                                            subtitle = if (lang == AppLanguage.HINDI) "विस्तृत विवरण हेतु किसी भी ग्रह पर टैप करें" else "Tap any plotted Graha to inspect sidereal longitude & transits"
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SiderealZodiacWheelCanvas(
                                                planets = snap.planets,
                                                selectedPlanetId = inspectedPlanet?.id,
                                                onSelectPlanet = { inspectedPlanet = it },
                                                modifier = Modifier
                                                    .size(256.dp)
                                                    .testTag("sidereal_zodiac_wheel_canvas")
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                ColorDot(MaterialTheme.colorScheme.secondary, size = 7.dp)
                                                Text(VedicLocalizer.planetName("sun", "Sun", lang), style = MaterialTheme.typography.labelSmall)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                ColorDot(MaterialTheme.colorScheme.tertiary, size = 7.dp)
                                                Text(VedicLocalizer.planetName("moon", "Moon", lang), style = MaterialTheme.typography.labelSmall)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                ColorDot(vedic.caution, size = 7.dp)
                                                Text(strings.retrogradeBadge, style = MaterialTheme.typography.labelSmall)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                ColorDot(vedic.inauspicious, size = 7.dp)
                                                Text(strings.combustBadge, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                                item(key = "planets_header") {
                                    SectionHeader(strings.navagrahaLongitudesTitle, strings.computedAtSunrise(snap.sunrise, snap.timezone))
                                }
                                items(items = snap.planets, key = { it.id }) { planet ->
                                    val nextRasi = snap.upcomingTransits.firstOrNull { it.planetId == planet.id && it.type == "rasi" && it.timestampMs >= state.nowMs }
                                    val nextNak = snap.upcomingTransits.firstOrNull { it.planetId == planet.id && it.type == "nakshatra" && it.timestampMs >= state.nowMs }
                                    PlanetPositionCard(
                                        planet = planet,
                                        nextRasi = nextRasi,
                                        nextNak = nextNak,
                                        nowMs = state.nowMs,
                                        timezone = snap.timezone,
                                        onInspect = { inspectedPlanet = planet }
                                    )
                                }
                            }

                            1 -> {
                                item(key = "transit_chakra_card") {
                                    val chartPlanets = remember(snap.planets) {
                                        snap.planets.map { p ->
                                            KundliPlanetEntry(
                                                id = p.id,
                                                nameEn = p.name,
                                                nameHi = VedicLocalizer.planetName(p.id, p.name, AppLanguage.HINDI),
                                                shortEn = p.name.take(2),
                                                shortHi = VedicLocalizer.planetName(p.id, p.name, AppLanguage.HINDI).take(2),
                                                symbol = p.symbol,
                                                longitude = p.siderealLongitude,
                                                signIndex = (p.rasiNumber - 1).coerceIn(0, 11),
                                                degreeInSign = p.degreesInRasiNum,
                                                dmsLabel = p.degreesInRasi,
                                                houseD1 = p.rasiNumber,
                                                signD9 = (p.rasiNumber - 1).coerceIn(0, 11),
                                                houseD9 = p.rasiNumber,
                                                nakshatraNumber = p.nakshatraNumber,
                                                nakshatraLord = "",
                                                pada = p.pada,
                                                isRetrograde = p.isRetrograde,
                                                isCombust = p.isCombust,
                                                dignityEn = "",
                                                dignityHi = "",
                                                isExaltedOrOwn = false
                                            )
                                        }
                                    }
                                    val moonSignIdx = (snap.planets.firstOrNull { it.id == "moon" }?.rasiNumber ?: 1) - 1
                                    PanchangaCard {
                                        SectionHeader(ext.tabTransitChakra, VedicLocalizer.rasiNameByZeroIndex(moonSignIdx, lang))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                            SegmentedButton(selected = chartStyle == KundliChartStyle.NORTH_INDIAN, onClick = { chartStyle = KundliChartStyle.NORTH_INDIAN }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text(strings.northIndianStyle) }
                                            SegmentedButton(selected = chartStyle == KundliChartStyle.SOUTH_INDIAN, onClick = { chartStyle = KundliChartStyle.SOUTH_INDIAN }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text(strings.southIndianStyle) }
                                            SegmentedButton(selected = chartStyle == KundliChartStyle.EAST_INDIAN, onClick = { chartStyle = KundliChartStyle.EAST_INDIAN }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text(ext.chartStyleEast) }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        KundliChartCanvas(
                                            lagnaSignIndex = moonSignIdx,
                                            planets = chartPlanets,
                                            chartStyle = chartStyle,
                                            centerTitle = ext.tabTransitChakra
                                        )
                                    }
                                }
                            }

                            2 -> {
                                item(key = "transits_filter_bar") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected = transitFilter == TransitTimelineFilter.ALL, onClick = { transitFilter = TransitTimelineFilter.ALL }, label = { Text(ext.transitFilterAll) }, modifier = Modifier.testTag("transit_filter_all"))
                                        FilterChip(selected = transitFilter == TransitTimelineFilter.RASI, onClick = { transitFilter = TransitTimelineFilter.RASI }, label = { Text(ext.transitFilterRasi) }, modifier = Modifier.testTag("transit_filter_rasi"))
                                        FilterChip(selected = transitFilter == TransitTimelineFilter.NAKSHATRA, onClick = { transitFilter = TransitTimelineFilter.NAKSHATRA }, label = { Text(ext.transitFilterNakshatra) }, modifier = Modifier.testTag("transit_filter_nak"))
                                        FilterChip(selected = transitFilter == TransitTimelineFilter.TODAY, onClick = { transitFilter = TransitTimelineFilter.TODAY }, label = { Text(ext.transitFilterToday) }, modifier = Modifier.testTag("transit_filter_today"))
                                    }
                                }
                                val filteredTransits = snap.upcomingTransits.filter { t ->
                                    when (transitFilter) {
                                        TransitTimelineFilter.ALL -> true
                                        TransitTimelineFilter.RASI -> t.type == "rasi"
                                        TransitTimelineFilter.NAKSHATRA -> t.type == "nakshatra"
                                        TransitTimelineFilter.TODAY -> t.isToday
                                    }
                                }
                                val todayHits = filteredTransits.filter { it.isToday }
                                if (todayHits.isNotEmpty()) {
                                    item(key = "today_transits_banner") {
                                        PanchangaCard(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)) {
                                            Text("${ext.todayTransitsBanner} (${todayHits.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                                items(items = filteredTransits, key = { it.id }) { transit ->
                                    TransitDetailCard(transit = transit, nowMs = state.nowMs, timezone = snap.timezone)
                                }
                            }
                        }
                    }
                }
            }

            val currentSnap = (state.content as? DailyContentState.Success)?.snapshot
            inspectedPlanet?.let { planet ->
                val nextRasi = currentSnap?.upcomingTransits?.firstOrNull { it.planetId == planet.id && it.type == "rasi" && it.timestampMs >= state.nowMs }
                val nextNak = currentSnap?.upcomingTransits?.firstOrNull { it.planetId == planet.id && it.type == "nakshatra" && it.timestampMs >= state.nowMs }
                GrahaInspectorSheet(
                    planet = planet,
                    nextRasi = nextRasi,
                    nextNak = nextNak,
                    nowMs = state.nowMs,
                    timezone = currentSnap?.timezone ?: state.preferences.selectedLocation.timezone,
                    onDismiss = { inspectedPlanet = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanetPositionCard(
    planet: PlanetPosition,
    nextRasi: PlanetTransitionEvent?,
    nextNak: PlanetTransitionEvent?,
    nowMs: Long,
    timezone: String,
    onInspect: () -> Unit
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val titleText = if (lang == AppLanguage.HINDI) VedicLocalizer.planetName(planet.id, planet.name, lang) else "${planet.name} (${planet.sanskritName})"
    val rasiText = VedicLocalizer.rasiName(planet.rasiNumber, planet.rasi, lang)
    val nakText = VedicLocalizer.nakshatraName(planet.nakshatraNumber, planet.nakshatra, lang)
    val (_, rasiNadi) = VedicGlossaryCatalog.rasiNadiBadge(planet.rasiNumber, lang)
    var expanded by remember(planet.id) { mutableStateOf(false) }

    ExpandableInfoCard(
        title = titleText,
        subtitle = "${ext.rasiNumberPrefix}${planet.rasiNumber} $rasiText ${planet.degreesInRasi} · $nakText ${strings.padaLabel} ${planet.pada}",
        expanded = expanded,
        onToggleExpand = { expanded = !expanded },
        leadingBadge = {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(planet.symbol, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        trailingBadge = {
            Column(horizontalAlignment = Alignment.End) {
                Text("%.2f°".format(planet.siderealLongitude), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("${planet.progressPercent}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        summaryContent = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (LocalAccessPolicy.current.hasFeature(AppFeatureKey.TATTVA_NADI_DETAILS)) {
                    SwaraNadiBadge(nadi = rasiNadi)
                }
                if (planet.isRetrograde) StatusPill(strings.retrogradeBadge, vedic.caution, vedic.cautionContainer)
                if (planet.isCombust) StatusPill(strings.combustBadge, vedic.inauspicious, vedic.inauspiciousContainer)
            }
            Spacer(modifier = Modifier.height(6.dp))
            AuspiciousnessMeterBar(
                progress = (planet.progressPercent.coerceIn(0, 100)) / 100f,
                accentColor = MaterialTheme.colorScheme.primary,
                label = rasiText,
                trailingValue = "${planet.progressPercent}%"
            )
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryStatTile(
                label = if (lang == AppLanguage.HINDI) "निरयण अंश" else "Sidereal Lon.",
                value = "%.4f°".format(planet.siderealLongitude),
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = strings.nakshatraLabel,
                value = "$nakText (P${planet.pada})",
                modifier = Modifier.weight(1f)
            )
        }
        nextRasi?.let { tr ->
            val rem = VedicGlossaryCatalog.formatLiveCountdown((tr.timestampMs - nowMs).coerceAtLeast(0L), lang)
            val exact = VedicLocalizer.formatDateTimeWithSeconds(tr.timestampMs, timezone, lang)
            Text("${ext.nextRasiIngressLabel}: ${VedicLocalizer.transitValue(tr.type, tr.toValue, tr.toSanskrit, lang)} · $exact ($rem)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
        nextNak?.let { tr ->
            val rem = VedicGlossaryCatalog.formatLiveCountdown((tr.timestampMs - nowMs).coerceAtLeast(0L), lang)
            val exact = VedicLocalizer.formatDateTimeWithSeconds(tr.timestampMs, timezone, lang)
            Text("${ext.nextNakshatraIngressLabel}: ${VedicLocalizer.transitValue(tr.type, tr.toValue, tr.toSanskrit, lang)} · $exact ($rem)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
        }
        OutlinedButton(onClick = onInspect, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.details)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrahaInspectorSheet(
    planet: PlanetPosition,
    nextRasi: PlanetTransitionEvent?,
    nextNak: PlanetTransitionEvent?,
    nowMs: Long,
    timezone: String,
    onDismiss: () -> Unit
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val rasiText = VedicLocalizer.rasiName(planet.rasiNumber, planet.rasi, lang)
    val nakText = VedicLocalizer.nakshatraName(planet.nakshatraNumber, planet.nakshatra, lang)
    val (_, rasiNadi) = VedicGlossaryCatalog.rasiNadiBadge(planet.rasiNumber, lang)

    InspectorBottomSheetFrame(
        title = "${planet.symbol} ${if (lang == AppLanguage.HINDI) VedicLocalizer.planetName(planet.id, planet.name, lang) else "${planet.name} (${planet.sanskritName})"}",
        subtitle = "$rasiText · ${planet.degreesInRasi}",
        icon = Icons.Rounded.Public,
        accentColor = when {
            planet.isCombust -> vedic.inauspicious
            planet.isRetrograde -> vedic.caution
            else -> MaterialTheme.colorScheme.primary
        },
        statusLabel = when {
            planet.isCombust -> strings.combustBadge
            planet.isRetrograde -> strings.retrogradeBadge
            else -> "${planet.progressPercent}%"
        },
        sheetTestTag = "graha_inspector_sheet",
        onDismiss = onDismiss
    ) {
        AuspiciousnessMeterBar(
            progress = (planet.progressPercent.coerceIn(0, 100)) / 100f,
            accentColor = MaterialTheme.colorScheme.primary,
            label = "${ext.rasiNumberPrefix}${planet.rasiNumber} $rasiText",
            trailingValue = "${planet.progressPercent}%"
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryStatTile(
                label = if (lang == AppLanguage.HINDI) "निरयण रेखांश" else "Sidereal Longitude",
                value = "%.4f°".format(planet.siderealLongitude),
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = if (lang == AppLanguage.HINDI) "राशि अंश" else "Sign Degree",
                value = planet.degreesInRasi,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryStatTile(
                label = strings.nakshatraLabel,
                value = "$nakText #${planet.nakshatraNumber}",
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = strings.padaLabel,
                value = "${strings.padaLabel} ${planet.pada}",
                valueColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (LocalAccessPolicy.current.hasFeature(AppFeatureKey.TATTVA_NADI_DETAILS)) {
                SwaraNadiBadge(nadi = rasiNadi)
            }
            if (planet.isRetrograde) StatusPill(strings.retrogradeBadge, vedic.caution, vedic.cautionContainer)
            if (planet.isCombust) StatusPill(strings.combustBadge, vedic.inauspicious, vedic.inauspiciousContainer)
        }
        if (nextRasi != null || nextNak != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            nextRasi?.let { tr ->
                val rem = VedicGlossaryCatalog.formatLiveCountdown((tr.timestampMs - nowMs).coerceAtLeast(0L), lang)
                val exact = VedicLocalizer.formatDateTimeWithSeconds(tr.timestampMs, timezone, lang)
                TelemetryStatTile(
                    label = ext.nextRasiIngressLabel,
                    value = "${VedicLocalizer.transitValue(tr.type, tr.toValue, tr.toSanskrit, lang)} · $exact",
                    caption = rem,
                    valueColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            nextNak?.let { tr ->
                val rem = VedicGlossaryCatalog.formatLiveCountdown((tr.timestampMs - nowMs).coerceAtLeast(0L), lang)
                val exact = VedicLocalizer.formatDateTimeWithSeconds(tr.timestampMs, timezone, lang)
                TelemetryStatTile(
                    label = ext.nextNakshatraIngressLabel,
                    value = "${VedicLocalizer.transitValue(tr.type, tr.toValue, tr.toSanskrit, lang)} · $exact",
                    caption = rem,
                    valueColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransitDetailCard(transit: PlanetTransitionEvent, nowMs: Long, timezone: String) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val planetDisplay = VedicLocalizer.planetName(transit.planetId, transit.planetName, lang)
    val fromDisplay = VedicLocalizer.transitValue(transit.type, transit.fromValue, transit.fromSanskrit, lang)
    val toDisplay = VedicLocalizer.transitValue(transit.type, transit.toValue, transit.toSanskrit, lang)
    val dayDisplay = VedicLocalizer.vaaraByName(transit.dayOfWeek, lang)
    val headline = if (lang == AppLanguage.HINDI) {
        "${transit.symbol} $planetDisplay · ${VedicLocalizer.transitTypeLabel(transit.type, lang)}"
    } else {
        transit.specialName ?: "${transit.symbol} ${transit.planetName} ${transit.type.replaceFirstChar { it.uppercase() }}"
    }
    val countdown = VedicGlossaryCatalog.formatLiveCountdown((transit.timestampMs - nowMs).coerceAtLeast(0L), lang)
    val exactTimestamp = VedicLocalizer.formatDateTimeWithSeconds(transit.timestampMs, timezone, lang)

    PanchangaCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(headline, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            StatusPill(countdown, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("$fromDisplay → $toDisplay", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text("$dayDisplay · $exactTimestamp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (transit.punyaKala != null || transit.mahaPunyaKala != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                transit.punyaKala?.let { pk ->
                    val pkS = pk.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: pk.start
                    val pkE = pk.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: pk.end
                    Text(strings.punyaKalaPrefix, style = MaterialTheme.typography.labelSmall, color = vedic.auspicious)
                    EventStartEndDateTimeView(startText = pkS, endText = pkE, accentColor = vedic.auspicious, compact = true)
                }
                transit.mahaPunyaKala?.let { mpk ->
                    val mpkS = mpk.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: mpk.start
                    val mpkE = mpk.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, timezone, lang) } ?: mpk.end
                    Text(ext.mahaPunyaKalaLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    EventStartEndDateTimeView(startText = mpkS, endText = mpkE, accentColor = MaterialTheme.colorScheme.secondary, compact = true)
                }
            }
        }
    }
}
