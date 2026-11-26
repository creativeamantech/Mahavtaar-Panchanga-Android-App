package com.mahavtaar.panchanga.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.MonthlyCalendarDay
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.ColorDot
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.InspectorBottomSheetFrame
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.LunarPakshaStripCanvas
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.TelemetryStatTile
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.ui.components.StickyBackToTodayPill
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HinduCalendarScreen(
    onOpenDailyPanchanga: () -> Unit,
    onOpenAnnualFestivals: () -> Unit = {},
    onOpenNotificationStudio: () -> Unit = {},
    viewModel: HinduCalendarViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val policy = LocalAccessPolicy.current
    val canDailyPanchanga = policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)
    val canAnnualFestivals = policy.canAccessPage(AppPageKey.ANNUAL_FESTIVALS)
    val canNotifs = policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)
    val showEventDetails = policy.hasFeature(AppFeatureKey.EVENT_DESCRIPTIONS)
    var inspectedDay by remember { mutableStateOf<MonthlyCalendarDay?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("hindu_calendar_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "controls") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.preferences.selectedLocation.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LanguageTogglePill(onToggleLanguage = viewModel::toggleLanguage)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = viewModel::previousMonth,
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(Icons.Rounded.ChevronLeft, contentDescription = strings.previousMonthCd)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = VedicLocalizer.formatMonthYear(state.yearMonth, lang),
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = VedicLocalizer.ayanamshaLabel(state.preferences.ayanamsha, lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = viewModel::nextMonth,
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(Icons.Rounded.ChevronRight, contentDescription = strings.nextMonthCd)
                        }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.preferences.monthSystem == MonthSystem.AMANTA,
                            onClick = { viewModel.selectMonthSystem(MonthSystem.AMANTA) },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                            modifier = Modifier.testTag("calendar_amanta_button")
                        ) {
                            Text(VedicLocalizer.monthSystemShort(MonthSystem.AMANTA, lang))
                        }
                        SegmentedButton(
                            selected = state.preferences.monthSystem == MonthSystem.PURNIMANTA,
                            onClick = { viewModel.selectMonthSystem(MonthSystem.PURNIMANTA) },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                            modifier = Modifier.testTag("calendar_purnimanta_button")
                        ) {
                            Text(VedicLocalizer.monthSystemShort(MonthSystem.PURNIMANTA, lang))
                        }
                    }

                    if (canAnnualFestivals) {
                        OutlinedButton(
                            onClick = onOpenAnnualFestivals,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calendar_open_annual_festivals")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Celebration,
                                contentDescription = strings.moreFestivalsTitle,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(strings.moreFestivalsTitle)
                        }
                    }
                }
            }

            when (val content = state.content) {
                is CalendarContentState.Loading -> item { LoadingStateBox(strings.loadingCalendar) }
                is CalendarContentState.Error -> item {
                    ErrorStateCard(message = content.message, onRetry = viewModel::retry)
                }
                is CalendarContentState.Success -> {
                    val days = content.days
                    val distinctMasas = days.map { VedicLocalizer.masaNameByText(it.masa, lang) }.distinct().joinToString(" · ")
                    val hasAdhika = days.any { it.isAdhika }

                    item(key = "masa_banner") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = distinctMasas,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (hasAdhika) {
                                StatusPill(
                                    label = strings.adhikaMasaBadge,
                                    textColor = MaterialTheme.colorScheme.secondary,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            }
                        }
                    }

                    item(key = "lunar_paksha_strip") {
                        PanchangaCard {
                            SectionHeader(
                                title = if (lang == AppLanguage.HINDI) "मासिक चन्द्र कला (पक्ष पट्टी)" else "Monthly Lunar Paksha Illumination",
                                subtitle = if (lang == AppLanguage.HINDI) "तिथि चयन हेतु किसी भी चन्द्रमा पर टैप करें" else "Tap any lunar day along the waxing/waning moon curve"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LunarPakshaStripCanvas(
                                days = days,
                                selectedDateIso = state.selectedDay?.dateIso,
                                onSelectDay = viewModel::selectDay,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("calendar_lunar_strip_canvas")
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ColorDot(MaterialTheme.colorScheme.secondary, size = 7.dp)
                                    Text(if (lang == AppLanguage.HINDI) "पूर्णिमा" else "Purnima", style = MaterialTheme.typography.labelSmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ColorDot(vedic.auspicious, size = 7.dp)
                                    Text(if (lang == AppLanguage.HINDI) "व्रत / पर्व" else "Vrat / Festival", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    item(key = "grid") {
                        MonthlyGridCard(
                            yearMonth = state.yearMonth,
                            days = days,
                            selectedDay = state.selectedDay,
                            onSelectDay = viewModel::selectDay
                        )
                    }

                    state.selectedDay?.let { chosen ->
                        item(key = "selected_day_detail") {
                            SelectedDayDetailCard(
                                day = chosen,
                                onOpenFullPanchanga = {
                                    viewModel.openDayInPanchanga(chosen)
                                    onOpenDailyPanchanga()
                                },
                                onOpenNotificationStudio = onOpenNotificationStudio
                            )
                        }
                    }

                    val festivalDays = days.filter { it.observances.isNotEmpty() }
                    if (festivalDays.isNotEmpty()) {
                        item(key = "festivals_header") {
                            SectionHeader(
                                title = strings.festivalsAndVratsTitle,
                                subtitle = strings.sacredObservancesCount(festivalDays.size)
                            )
                        }
                        items(items = festivalDays, key = { "fest_${it.dateIso}" }) { day ->
                            FestivalMonthRow(
                                day = day,
                                onClick = {
                                    viewModel.selectDay(day)
                                    inspectedDay = day
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(56.dp)) }
                }
            }
        }

        val todayLocal = remember(state.preferences.selectedLocation.timezone) {
            LocalDate.now(TimeFormatters.safeZoneId(state.preferences.selectedLocation.timezone))
        }
        val showBackToToday = state.yearMonth != YearMonth.from(todayLocal) ||
            (state.selectedDay != null && state.selectedDay?.dateIso != todayLocal.toString())
        StickyBackToTodayPill(
            visible = showBackToToday,
            onBackToToday = viewModel::jumpToToday,
            testTag = "calendar_back_to_today_pill",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        inspectedDay?.let { day ->
            val firstObs = day.observances.firstOrNull()
            InspectorBottomSheetFrame(
                title = firstObs?.let { VedicLocalizer.observanceTitle(it, lang) } ?: day.dateIso,
                subtitle = "${day.dateIso} · ${VedicLocalizer.masaNameByText(day.masa, lang)} · ${VedicLocalizer.tithiName(day.primaryTithiNumber, day.tithiName, lang)}",
                icon = Icons.Rounded.AutoAwesome,
                accentColor = MaterialTheme.colorScheme.secondary,
                statusLabel = firstObs?.let { VedicLocalizer.observanceDeity(it, lang) },
                sheetTestTag = "calendar_festival_inspector_sheet",
                onDismiss = { inspectedDay = null },
                footerActions = {
                    if (canDailyPanchanga || canNotifs) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (canDailyPanchanga) {
                                Button(
                                    onClick = {
                                        inspectedDay = null
                                        viewModel.openDayInPanchanga(day)
                                        onOpenDailyPanchanga()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.openFullPanchanga)
                                }
                            }
                            if (canNotifs) {
                                OutlinedButton(
                                    onClick = {
                                        inspectedDay = null
                                        onOpenNotificationStudio()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(ext.addVratReminder)
                                }
                            }
                        }
                    }
                }
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TelemetryStatTile(strings.sunrise, day.sunrise, Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.primary)
                    TelemetryStatTile(strings.sunset, day.sunset, Modifier.weight(1f), valueColor = MaterialTheme.colorScheme.secondary)
                }
                if (showEventDetails) {
                    day.observances.forEach { obs ->
                        Text(
                            text = "${ext.significanceLabel}: ${VedicGlossaryCatalog.observanceSignificance(obs, lang)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val practices = VedicGlossaryCatalog.observancePractices(obs, lang)
                        if (practices.isNotEmpty()) {
                            Text(ext.recommendedPracticesLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            practices.forEach { step ->
                                Text("✓ $step", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyGridCard(
    yearMonth: java.time.YearMonth,
    days: List<MonthlyCalendarDay>,
    selectedDay: MonthlyCalendarDay?,
    onSelectDay: (MonthlyCalendarDay) -> Unit
) {
    val lang = LocalAppLanguage.current
    val firstWeekdayOffset = LocalDate.of(yearMonth.year, yearMonth.monthValue, 1).dayOfWeek.value % 7
    val weekdayLabels = VedicLocalizer.weekdayShortLabels(lang)
    val cells: List<MonthlyCalendarDay?> = List(firstWeekdayOffset) { null } + days
    val rows = cells.chunked(7)
    val vedic = LocalVedicColors.current

    PanchangaCard {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { w ->
                Text(
                    text = w,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        rows.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 7) {
                    val day = week.getOrNull(col)
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val isSelected = selectedDay?.dateIso == day.dateIso
                        val isPurnima = day.primaryTithiNumber == 15
                        val isAmavasya = day.primaryTithiNumber == 30
                        val hasVrat = day.observances.isNotEmpty()
                        val shortTithi = VedicLocalizer.tithiShortName(day.primaryTithiNumber, day.tithiName, lang)

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectDay(day) }
                                .testTag("calendar_day_${day.dayOfMonth}"),
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                day.isMasaTransition -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${day.dayOfMonth}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = shortTithi,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    if (isPurnima) ColorDot(MaterialTheme.colorScheme.primary)
                                    if (isAmavasya) ColorDot(MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (hasVrat) ColorDot(vedic.auspicious)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayDetailCard(
    day: MonthlyCalendarDay,
    onOpenFullPanchanga: () -> Unit,
    onOpenNotificationStudio: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val policy = LocalAccessPolicy.current
    val showEventDetails = policy.hasFeature(AppFeatureKey.EVENT_DESCRIPTIONS)
    val canDailyPanchanga = policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)
    val canNotifs = policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)
    val vaaraDisplay = VedicLocalizer.vaaraByName(day.vaara, lang)
    val masaDisplay = VedicLocalizer.masaNameByText(day.masa, lang)
    val pakshaDisplay = VedicLocalizer.pakshaName(day.paksha, lang)
    val tithiDisplay = VedicLocalizer.tithiName(day.primaryTithiNumber, day.tithiName, lang)
    val nakDisplay = VedicLocalizer.nakshatraByName(day.nakshatraName, lang)
    val yogaDisplay = VedicLocalizer.yogaByName(day.yogaName, lang)
    val karanaDisplay = VedicLocalizer.karanaName(day.karanaName, lang)

    fun formatUntil(t: String?): String = when {
        t == null -> ""
        lang == AppLanguage.HINDI -> " ($t ${strings.untilPrefix})"
        else -> " (${strings.untilPrefix.lowercase()} $t)"
    }

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${day.dateIso} · $vaaraDisplay",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$masaDisplay · $pakshaDisplay ${strings.pakshaSuffix}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (day.isMasaTransition) {
                StatusPill(strings.masaTransitionBadge, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryStatTile(
                label = strings.tithiLabel,
                value = tithiDisplay,
                caption = formatUntil(day.tithiEnds).trim().ifEmpty { null },
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = strings.nakshatraLabel,
                value = nakDisplay,
                caption = formatUntil(day.nakshatraEnds).trim().ifEmpty { null },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TelemetryStatTile(
                label = strings.sunrise,
                value = day.sunrise,
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TelemetryStatTile(
                label = strings.sunset,
                value = day.sunset,
                valueColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("${strings.yogaLabel}: $yogaDisplay · ${strings.karanaLabel}: $karanaDisplay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        day.observances.forEach { obs ->
            Spacer(modifier = Modifier.height(8.dp))
            StatusPill(
                label = "${VedicLocalizer.observanceTitle(obs, lang)} · ${VedicLocalizer.observanceDeity(obs, lang)}",
                textColor = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
            if (showEventDetails) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${ext.significanceLabel}: ${VedicGlossaryCatalog.observanceSignificance(obs, lang)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val practices = VedicGlossaryCatalog.observancePractices(obs, lang)
                if (practices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ext.recommendedPracticesLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    practices.forEach { step ->
                        Text(
                            text = "✓ $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (canDailyPanchanga) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenFullPanchanga,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_open_day_panchanga")
            ) {
                Text(strings.openFullPanchanga)
            }
        }
        if (day.observances.isNotEmpty() && canNotifs) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenNotificationStudio,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_configure_vrat_alert")
            ) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(ext.addVratReminder)
            }
        }
    }
}

@Composable
private fun FestivalMonthRow(
    day: MonthlyCalendarDay,
    onClick: () -> Unit
) {
    val obs = day.observances.first()
    val lang = LocalAppLanguage.current
    val title = VedicLocalizer.observanceTitle(obs, lang)
    val deity = VedicLocalizer.observanceDeity(obs, lang)
    val masaDisplay = VedicLocalizer.masaNameByText(day.masa, lang)
    val tithiDisplay = VedicLocalizer.tithiName(day.primaryTithiNumber, day.tithiName, lang)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text("${day.dateIso} · $masaDisplay $tithiDisplay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(deity, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
