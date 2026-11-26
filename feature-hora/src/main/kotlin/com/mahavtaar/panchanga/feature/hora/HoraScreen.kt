package com.mahavtaar.panchanga.feature.hora

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.HoraPeriod
import com.mahavtaar.panchanga.domain.model.SwaraYogaState
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.TattvaPeriod
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.AtmosphericHeroCard
import com.mahavtaar.panchanga.ui.components.DateLocationHeaderBar
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.HoraTattvaRingCanvas
import com.mahavtaar.panchanga.ui.components.InspectorBottomSheetFrame
import com.mahavtaar.panchanga.ui.components.LiveNowClarityBanner
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.ui.components.LiveCountdownWithMutedTimestamps
import com.mahavtaar.panchanga.ui.components.StickyBackToTodayPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TattvaBadge
import com.mahavtaar.panchanga.ui.components.TelemetryStatTile
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.LocalDate
import java.util.Locale

@Composable
fun HoraScreen(
    onOpenLocations: () -> Unit,
    onOpenNotificationStudio: () -> Unit = {},
    viewModel: HoraViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appLang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val policy = LocalAccessPolicy.current
    val canPickLocation = policy.canAccessPage(AppPageKey.LOCATIONS)
    val canTattvaNadi = policy.hasFeature(AppFeatureKey.TATTVA_NADI_DETAILS)
    var inspectedHora by remember { mutableStateOf<HoraPeriod?>(null) }

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
                .testTag("hora_timeline_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    onLocationClick = { if (canPickLocation) onOpenLocations() },
                    onToggleLanguage = viewModel::onToggleLanguage,
                    coordinateSubtitle = "%.5f°, %.5f°".format(Locale.US, loc.latitude, loc.longitude)
                )
            }

            when (val content = state.content) {
                is HoraContentState.Loading -> item { LoadingStateBox() }
                is HoraContentState.Error -> item {
                    ErrorStateCard(message = content.message, onRetry = viewModel::retry)
                }
                is HoraContentState.Success -> {
                    item(key = "active_hero") {
                        ActiveHoraHeroCard(
                            dailyHoras = content.dailyHoras,
                            nowMs = state.nowMs,
                            onOpenNotificationStudio = onOpenNotificationStudio,
                            onInspectHora = { inspectedHora = it }
                        )
                    }

                    if (canTattvaNadi) {
                        item(key = "nadi_guidance_banner") {
                            NadiGuidanceBannerCard(swaraYoga = content.snapshot.swaraYoga, dailyHoras = content.dailyHoras)
                        }
                    }

                    item(key = "day_night_toggle") {
                        val lang = LocalAppLanguage.current
                        val isHi = lang == AppLanguage.HINDI
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = state.filterMode == HoraFilterMode.ALL,
                                    onClick = { viewModel.selectFilterMode(HoraFilterMode.ALL) },
                                    shape = SegmentedButtonDefaults.itemShape(0, 3),
                                    modifier = Modifier.testTag("hora_all_tab")
                                ) {
                                    Text(ext.horaFilterAll)
                                }
                                SegmentedButton(
                                    selected = state.filterMode == HoraFilterMode.DAY,
                                    onClick = { viewModel.selectFilterMode(HoraFilterMode.DAY) },
                                    shape = SegmentedButtonDefaults.itemShape(1, 3),
                                    modifier = Modifier.testTag("hora_day_tab")
                                ) {
                                    Text(ext.horaFilterDay)
                                }
                                SegmentedButton(
                                    selected = state.filterMode == HoraFilterMode.NIGHT,
                                    onClick = { viewModel.selectFilterMode(HoraFilterMode.NIGHT) },
                                    shape = SegmentedButtonDefaults.itemShape(2, 3),
                                    modifier = Modifier.testTag("hora_night_tab")
                                ) {
                                    Text(ext.horaFilterNight)
                                }
                            }

                            if (canTattvaNadi) {
                                OutlinedTextField(
                                    value = state.searchQuery,
                                    onValueChange = viewModel::updateSearchQuery,
                                    modifier = Modifier.fillMaxWidth().testTag("hora_search_input"),
                                    singleLine = true,
                                    placeholder = {
                                        Text(
                                            text = if (isHi) "होरा ग्रह, समय (जैसे 10:15) या तत्व खोजें..." else "Search planet, time (e.g. 10:15) or Tattva...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = state.selectedElementFilter == null,
                                        onClick = { viewModel.selectElementFilter(null) },
                                        label = { Text(if (isHi) "सभी ५ तत्व" else "All 5 Tattvas") }
                                    )
                                    TattvaElement.entries.forEach { el ->
                                        FilterChip(
                                            selected = state.selectedElementFilter == el,
                                            onClick = {
                                                viewModel.selectElementFilter(if (state.selectedElementFilter == el) null else el)
                                            },
                                            label = { Text(VedicLocalizer.tattvaName(el, lang)) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val q = state.searchQuery.trim().lowercase(Locale.ROOT)
                    val filtered = when (state.filterMode) {
                        HoraFilterMode.ALL -> content.dailyHoras.horas
                        HoraFilterMode.DAY -> content.dailyHoras.horas.filter { it.isDay }
                        HoraFilterMode.NIGHT -> content.dailyHoras.horas.filter { !it.isDay }
                    }.filter { hora ->
                        if (q.isEmpty()) true
                        else {
                            val rulerLoc = VedicLocalizer.horaRulerName(hora.ruler, appLang)
                            val nadiLoc = VedicLocalizer.nadiShortLabel(hora.nadi, appLang)
                            val tattvasMatch = hora.tattvas.any { t ->
                                val tName = VedicLocalizer.tattvaName(t.element, appLang)
                                "$tName ${t.element.name} ${t.startTime} ${t.endTime}".lowercase(Locale.ROOT).contains(q)
                            }
                            "${hora.ruler} $rulerLoc ${hora.startTime} ${hora.endTime} $nadiLoc #${hora.index}".lowercase(Locale.ROOT).contains(q) || tattvasMatch
                        }
                    }

                    items(items = filtered, key = { it.index }) { hora ->
                        HoraRowCard(
                            hora = hora,
                            timezone = content.dailyHoras.timezone,
                            isActive = content.dailyHoras.activeHora?.index == hora.index,
                            activeTattva = content.dailyHoras.activeTattva,
                            nowMs = state.nowMs,
                            isExpanded = state.expandedHoraIndex == hora.index || state.selectedElementFilter != null || q.isNotEmpty(),
                            onClick = { viewModel.toggleExpand(hora.index) },
                            onInspect = { inspectedHora = hora }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(56.dp)) }
                }
            }
        }

        val todayLocal = remember(state.preferences.selectedLocation.timezone) {
            LocalDate.now(TimeFormatters.safeZoneId(state.preferences.selectedLocation.timezone))
        }
        StickyBackToTodayPill(
            visible = state.selectedDate != todayLocal,
            onBackToToday = viewModel::onSelectToday,
            testTag = "hora_back_to_today_pill",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        val currentDailyHoras = (state.content as? HoraContentState.Success)?.dailyHoras
        inspectedHora?.let { hora ->
            HoraInspectorSheet(
                hora = hora,
                timezone = currentDailyHoras?.timezone ?: state.preferences.selectedLocation.timezone,
                nowMs = state.nowMs,
                onDismiss = { inspectedHora = null },
                onOpenNotificationStudio = onOpenNotificationStudio
            )
        }
    }
}

@Composable
private fun ActiveHoraHeroCard(
    dailyHoras: DailyHoras,
    nowMs: Long,
    onOpenNotificationStudio: () -> Unit,
    onInspectHora: (HoraPeriod) -> Unit
) {
    val active = dailyHoras.activeHora ?: dailyHoras.horas.firstOrNull() ?: return
    val nextHora = dailyHoras.horas.firstOrNull { it.index == active.index + 1 }
    val remMs = (active.endTimeMs - nowMs).coerceAtLeast(0L)
    val totalMs = active.durationMs.coerceAtLeast(1L)
    val horaProgress = ((nowMs - active.startTimeMs).toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val activeTattva = dailyHoras.activeTattva
    val tattvaProgress = activeTattva?.let { t ->
        ((nowMs - t.startTimeMs).toFloat() / t.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    } ?: 0f
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val policy = LocalAccessPolicy.current
    val canTattvaNadi = policy.hasFeature(AppFeatureKey.TATTVA_NADI_DETAILS)
    val canNotifs = policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)
    val rulerDisplay = VedicLocalizer.horaRulerName(active.ruler, lang)
    val dayNightText = if (active.isDay) strings.dayLabel else strings.nightLabel
    val (favorable, avoid) = VedicGlossaryCatalog.horaPlanetaryFocus(active.ruler, lang)

    AtmosphericHeroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.activePlanetaryHora,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (canTattvaNadi) {
                SwaraNadiBadge(nadi = active.nadi)
            }
            if (canNotifs) {
                IconButton(
                    onClick = onOpenNotificationStudio,
                    modifier = Modifier.size(36.dp).testTag("hora_open_notif_studio")
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
        Spacer(modifier = Modifier.height(8.dp))
        LiveNowClarityBanner(
            sentence = VedicLocalizer.simpleNowHoraSentence(active.ruler, active.nadi, activeTattva?.element, lang),
            accentColor = vedic.auspicious,
            containerColor = vedic.auspiciousContainer
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HoraTattvaRingCanvas(
                horaProgress = horaProgress,
                tattvaProgress = tattvaProgress,
                centerPrimary = "${(horaProgress * 100).toInt()}%",
                centerSecondary = "#${active.index}",
                diameter = 104.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "$rulerDisplay ${strings.horaSuffix} · $dayNightText",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (remMs > 0L) {
                    LiveCountdownWithMutedTimestamps(
                        countdownText = "${VedicGlossaryCatalog.formatLiveCountdown(remMs, lang)} ${strings.leftSuffix}",
                        startText = VedicLocalizer.formatDateTimeWithSeconds(active.startTimeMs, dailyHoras.timezone, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(active.endTimeMs, dailyHoras.timezone, lang),
                        accentColor = vedic.auspicious
                    )
                } else {
                    EventStartEndDateTimeView(
                        startText = VedicLocalizer.formatDateTimeWithSeconds(active.startTimeMs, dailyHoras.timezone, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(active.endTimeMs, dailyHoras.timezone, lang),
                        accentColor = vedic.auspicious
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SwaraNadiBadge(nadi = active.nadi)
                    activeTattva?.let { t ->
                        TattvaBadge(element = t.element)
                    }
                }
            }
        }

        if (canTattvaNadi && active.tattvas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                active.tattvas.forEach { t ->
                    val isCur = activeTattva?.startTimeMs == t.startTimeMs
                    Box(
                        modifier = Modifier
                            .weight(t.durationMins.coerceAtLeast(4).toFloat())
                            .fillMaxHeight()
                            .background(horaTattvaColor(t.element).copy(alpha = if (isCur) 1f else 0.32f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "• ${ext.favorableActivitiesLabel}: $favorable",
            style = MaterialTheme.typography.bodySmall,
            color = vedic.auspicious
        )
        Text(
            text = "• ${ext.avoidActivitiesLabel}: $avoid",
            style = MaterialTheme.typography.bodySmall,
            color = vedic.inauspicious
        )
        if (nextHora != null) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))
            val nextRuler = VedicLocalizer.horaRulerName(nextHora.ruler, lang)
            val nextStartDt = VedicLocalizer.formatDateTimeWithSeconds(nextHora.startTimeMs, dailyHoras.timezone, lang)
            val nextNadi = VedicLocalizer.nadiShortLabel(nextHora.nadi, lang)
            val upcomingLine = if (lang == AppLanguage.HINDI) {
                "${strings.upcomingPrefix}: $nextRuler ${strings.horaSuffix} · $nextStartDt ${strings.atTimeWord} ($nextNadi ${strings.nadiWord})"
            } else {
                "${strings.upcomingPrefix}: $nextRuler ${strings.horaSuffix} ${strings.atTimeWord} $nextStartDt ($nextNadi ${strings.nadiWord})"
            }
            Text(
                text = upcomingLine,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(onClick = { onInspectHora(active) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(strings.details)
        }
    }
}

@Composable
private fun NadiGuidanceBannerCard(swaraYoga: SwaraYogaState, dailyHoras: DailyHoras) {
    val lang = LocalAppLanguage.current
    val ext = LocalExtendedStrings.current
    val activeNadi = dailyHoras.activeHora?.nadi ?: swaraYoga.currentActiveSwara

    PanchangaCard(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.SelfImprovement,
                    contentDescription = ext.nadiGuidanceBanner,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ext.nadiGuidanceBanner,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            SwaraNadiBadge(nadi = activeNadi)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${VedicLocalizer.nadiFullTitle(activeNadi, lang)} — ${VedicLocalizer.swaraEnergySummary(activeNadi, swaraYoga.energySummary, lang)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HoraRowCard(
    hora: HoraPeriod,
    timezone: String,
    isActive: Boolean,
    activeTattva: TattvaPeriod?,
    nowMs: Long,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onInspect: () -> Unit
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val rulerDisplay = VedicLocalizer.horaRulerName(hora.ruler, lang)
    val dayNightTag = if (hora.isDay) ext.horaFilterDay else ext.horaFilterNight
    val (favorable, avoid) = VedicGlossaryCatalog.horaPlanetaryFocus(hora.ruler, lang)
    val rowActiveTattva = if (isActive) activeTattva else null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("hora_item_${hora.index}"),
        shape = MaterialTheme.shapes.large,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${hora.index}. $rulerDisplay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                        )
                        StatusPill(
                            label = dayNightTag,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        if (isActive) {
                            StatusPill(strings.live, vedic.auspicious, vedic.auspiciousContainer)
                        }
                    }
                    EventStartEndDateTimeView(
                        startText = VedicLocalizer.formatDateTimeWithSeconds(hora.startTimeMs, timezone, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(hora.endTimeMs, timezone, lang),
                        accentColor = if (isActive) vedic.auspicious else MaterialTheme.colorScheme.primary,
                        compact = true
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SwaraNadiBadge(nadi = hora.nadi)
                        rowActiveTattva?.let { t ->
                            TattvaBadge(element = t.element)
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) strings.collapseTattvasCd else strings.expandTattvasCd
                )
            }

            if (isActive) {
                val hProgress = ((nowMs - hora.startTimeMs).toFloat() / hora.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                Spacer(modifier = Modifier.height(8.dp))
                LiveNowClarityBanner(
                    sentence = VedicLocalizer.simpleNowHoraSentence(hora.ruler, hora.nadi, rowActiveTattva?.element, lang),
                    accentColor = vedic.auspicious,
                    containerColor = vedic.auspiciousContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { hProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = vedic.auspicious
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${ext.favorableActivitiesLabel}: $favorable",
                    style = MaterialTheme.typography.bodySmall,
                    color = vedic.auspicious
                )
                Text(
                    text = "${ext.avoidActivitiesLabel}: $avoid",
                    style = MaterialTheme.typography.bodySmall,
                    color = vedic.inauspicious
                )
                if (LocalAccessPolicy.current.hasFeature(AppFeatureKey.TATTVA_NADI_DETAILS)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(strings.fiveSubTattvas, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (hora.tattvas.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            hora.tattvas.forEach { t ->
                                val isCurrentTattva = isActive && activeTattva?.startTimeMs == t.startTimeMs
                                Box(
                                    modifier = Modifier
                                        .weight(t.durationMins.coerceAtLeast(4).toFloat())
                                        .fillMaxHeight()
                                        .background(horaTattvaColor(t.element).copy(alpha = if (isCurrentTattva) 1f else 0.45f))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    hora.tattvas.forEach { t ->
                        val isCurrentTattva = isActive && activeTattva?.startTimeMs == t.startTimeMs
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TattvaBadge(element = t.element)
                                Text(
                                    text = "${t.durationMins}${strings.minShort}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentTattva) vedic.auspicious else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            EventStartEndDateTimeView(
                                startText = VedicLocalizer.formatDateTimeWithSeconds(t.startTimeMs, timezone, lang),
                                endText = VedicLocalizer.formatDateTimeWithSeconds(t.endTimeMs, timezone, lang),
                                accentColor = if (isCurrentTattva) vedic.auspicious else MaterialTheme.colorScheme.primary,
                                compact = true
                            )
                            Text(
                                text = VedicLocalizer.tattvaQuality(t.element, lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isCurrentTattva) {
                                val subProgress = ((nowMs - t.startTimeMs).toFloat() / t.durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                                LiveNowClarityBanner(
                                    sentence = VedicLocalizer.simpleNowTattvaSentence(t.element, lang),
                                    accentColor = vedic.auspicious,
                                    containerColor = vedic.auspiciousContainer
                                )
                                LinearProgressIndicator(
                                    progress = { subProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = vedic.auspicious
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onInspect, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.details)
                }
            }
        }
    }
}

@Composable
private fun HoraInspectorSheet(
    hora: HoraPeriod,
    timezone: String,
    nowMs: Long,
    onDismiss: () -> Unit,
    onOpenNotificationStudio: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val policy = LocalAccessPolicy.current
    val canNotifs = policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)
    val canTattvaNadi = policy.hasFeature(AppFeatureKey.TATTVA_NADI_DETAILS)
    val rulerDisplay = VedicLocalizer.horaRulerName(hora.ruler, lang)
    val (favorable, avoid) = VedicGlossaryCatalog.horaPlanetaryFocus(hora.ruler, lang)
    val startStr = VedicLocalizer.formatDateTimeWithSeconds(hora.startTimeMs, timezone, lang)
    val endStr = VedicLocalizer.formatDateTimeWithSeconds(hora.endTimeMs, timezone, lang)
    val isNow = nowMs in hora.startTimeMs until hora.endTimeMs
    val currentTattva = if (isNow) hora.tattvas.firstOrNull { nowMs in it.startTimeMs until it.endTimeMs } else null

    InspectorBottomSheetFrame(
        title = "$rulerDisplay ${strings.horaSuffix} (#${hora.index})",
        subtitle = "$startStr – $endStr",
        icon = Icons.Rounded.Schedule,
        accentColor = MaterialTheme.colorScheme.primary,
        statusLabel = if (hora.isDay) ext.horaFilterDay else ext.horaFilterNight,
        sheetTestTag = "hora_inspector_sheet",
        onDismiss = onDismiss,
        footerActions = if (canNotifs) {
            {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenNotificationStudio()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(ext.setAlert)
                }
            }
        } else null
    ) {
        if (isNow) {
            LiveNowClarityBanner(
                sentence = VedicLocalizer.simpleNowHoraSentence(hora.ruler, hora.nadi, currentTattva?.element, lang),
                accentColor = vedic.auspicious,
                containerColor = vedic.auspiciousContainer
            )
        }
        EventStartEndDateTimeView(
            startText = startStr,
            endText = endStr,
            accentColor = if (isNow) vedic.auspicious else MaterialTheme.colorScheme.primary
        )
        TelemetryStatTile(strings.durationPrefix, VedicLocalizer.formatDurationHoursMinutes(hora.durationMs, lang), Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SwaraNadiBadge(nadi = hora.nadi)
            currentTattva?.let { TattvaBadge(element = it.element) }
        }
        Text("${ext.favorableActivitiesLabel}: $favorable", style = MaterialTheme.typography.bodyMedium, color = vedic.auspicious)
        Text("${ext.avoidActivitiesLabel}: $avoid", style = MaterialTheme.typography.bodyMedium, color = vedic.inauspicious)
        if (canTattvaNadi) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(strings.fiveSubTattvas, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            hora.tattvas.forEach { t ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TattvaBadge(element = t.element)
                        Text("${t.durationMins}${strings.minShort}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    EventStartEndDateTimeView(
                        startText = VedicLocalizer.formatDateTimeWithSeconds(t.startTimeMs, timezone, lang),
                        endText = VedicLocalizer.formatDateTimeWithSeconds(t.endTimeMs, timezone, lang),
                        compact = true
                    )
                }
            }
        }
    }
}

private fun horaTattvaColor(element: TattvaElement): Color = when (element) {
    TattvaElement.PRITHVI -> Color(0xFFD97706)
    TattvaElement.JALA -> Color(0xFF0284C7)
    TattvaElement.TEJAS -> Color(0xFFDC2626)
    TattvaElement.VAYU -> Color(0xFF0D9488)
    TattvaElement.AKASH -> Color(0xFF7C3AED)
}
