package com.mahavtaar.panchanga.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.mahavtaar.panchanga.ui.components.ChakraPetalAccentBar
import com.mahavtaar.panchanga.ui.components.DateLocationHeaderBar
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.MahavtaarBrandLogo
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StickyBackToTodayPill
import com.mahavtaar.panchanga.ui.components.TimedDndQuickSheet
import com.mahavtaar.panchanga.ui.components.TimingWindowInspectorSheet
import com.mahavtaar.panchanga.ui.components.VedicGlossaryBottomSheet
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

internal data class HomeAngaInspection(
    val angaTitle: String,
    val angaSubtitle: String,
    val displayName: String,
    val segment: AngaSegment,
    val nextDisplayName: String?,
    val timezone: String,
    val glossaryTerm: String
)

@Composable
fun HomeScreen(
    onOpenDailyPanchanga: () -> Unit,
    onOpenHora: () -> Unit,
    onOpenSwaraYoga: () -> Unit,
    onOpenPlanetaryPositions: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenTodaySchedule: () -> Unit = {},
    onOpenKundli: () -> Unit = {},
    onOpenGunaMilan: () -> Unit = {},
    onOpenNavagrahaMantras: () -> Unit = {},
    onOpenNavtara: () -> Unit = {},
    onOpenTattva: () -> Unit = {},
    onOpenAnnualFestivals: () -> Unit = {},
    onOpenNotificationStudio: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenAdminPanel: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val policy = LocalAccessPolicy.current
    val vedic = LocalVedicColors.current
    val showGlossary = policy.hasFeature(AppFeatureKey.GLOSSARY_DICTIONARY)
    var inspectedAnga by remember { mutableStateOf<HomeAngaInspection?>(null) }
    var inspectedTiming by remember { mutableStateOf<Pair<String, TimingInterval>?>(null) }
    var glossaryQuery by remember { mutableStateOf<String?>(null) }
    var showQuickDndSheet by remember { mutableStateOf(false) }

    val zone = remember(state.preferences.selectedLocation.timezone) {
        TimeFormatters.safeZoneId(state.preferences.selectedLocation.timezone)
    }
    val isToday = remember(state.selectedDate, zone) {
        state.selectedDate == LocalDate.now(zone)
    }
    val localNowZdt = Instant.ofEpochMilli(state.nowMs).atZone(zone)
    val isDndOrQuietActive = !state.audioSettings.masterEnabled ||
        state.audioSettings.isManualDndActive(state.nowMs) ||
        state.audioSettings.isWithinQuietHours(localNowZdt.hour * 60 + localNowZdt.minute)

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
                .testTag("home_dashboard_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "invocation_banner") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MahavtaarBrandLogo(size = 50.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = strings.vedicInvocationBanner,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                                FilledTonalIconButton(
                                    onClick = { showQuickDndSheet = true },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (isDndOrQuietActive) vedic.inauspiciousContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isDndOrQuietActive) vedic.inauspicious else MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("home_top_dnd_chip")
                                ) {
                                    Icon(
                                        imageVector = if (isDndOrQuietActive) Icons.AutoMirrored.Rounded.VolumeOff else Icons.Rounded.NotificationsActive,
                                        contentDescription = strings.notifDndAndQuietSectionTitle
                                    )
                                }
                            }
                            if (policy.isAdmin) {
                                FilledTonalIconButton(
                                    onClick = onOpenAdminPanel,
                                    modifier = Modifier.testTag("home_top_admin_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = ext.adminPanelTitle,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            val userName = state.cloudUser?.displayName?.takeIf { it.isNotBlank() }
                                ?: state.cloudUser?.email?.substringBefore("@")
                                ?: ext.profileTitle
                            val initials = userName.split(" ")
                                .filter { it.isNotBlank() }
                                .take(2)
                                .joinToString("") { it.first().uppercaseChar().toString() }
                                .ifBlank { "ॐ" }
                            Surface(
                                onClick = onOpenProfile,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                                modifier = Modifier.testTag("home_profile_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = userName.substringBefore(" "),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        ChakraPetalAccentBar(height = 3.dp)
                    }
                }
            }

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
                    onLocationClick = {
                        if (policy.canAccessPage(AppPageKey.LOCATIONS)) onOpenLocations()
                    },
                    onToggleLanguage = viewModel::onToggleLanguage,
                    coordinateSubtitle = "%.5f°, %.5f°".format(Locale.US, loc.latitude, loc.longitude)
                )
            }

            when (val content = state.content) {
                is HomeContentState.Loading -> {
                    item(key = "loading") { LoadingStateBox() }
                }
                is HomeContentState.Error -> {
                    item(key = "error") {
                        ErrorStateCard(message = content.message, onRetry = viewModel::onRetry)
                    }
                }
                is HomeContentState.Success -> {
                    val snap = content.snapshot
                    item(key = "hero") {
                        VedicHeroCard(snapshot = snap, nowMs = state.nowMs)
                    }

                    item(key = "running_now_live_widget") {
                        RunningNowLiveWidgetCard(
                            snapshot = snap,
                            dailyHoras = content.dailyHoras,
                            liveSwara = content.liveSwara,
                            nowMs = state.nowMs,
                            liveClockText = state.liveClockText,
                            onOpenDailyPanchanga = onOpenDailyPanchanga,
                            onOpenHora = onOpenHora,
                            onOpenSwaraYoga = onOpenSwaraYoga,
                            onOpenTattva = onOpenTattva,
                            onOpenTodaySchedule = onOpenTodaySchedule
                        )
                    }

                    item(key = "active_status_banner") {
                        ActiveMuhurtaStatusBanner(
                            snapshot = snap,
                            nowMs = state.nowMs,
                            onOpenDailyPanchanga = {
                                if (policy.canAccessPage(AppPageKey.DAILY_PANCHANGA)) onOpenDailyPanchanga()
                            }
                        )
                    }

                    if (snap.observances.isNotEmpty()) {
                        item(key = "observances") {
                            ObservanceHighlightCard(snapshot = snap)
                        }
                    }

                    item(key = "vedic_modules_grid") {
                        VedicModulesQuickLauncher(
                            pinnedModuleKeys = state.preferences.pinnedHomeModules,
                            onUpdatePinnedModules = viewModel::setPinnedHomeModules,
                            onOpenTodaySchedule = onOpenTodaySchedule,
                            onOpenKundli = onOpenKundli,
                            onOpenGunaMilan = onOpenGunaMilan,
                            onOpenNavtara = onOpenNavtara,
                            onOpenTattva = onOpenTattva,
                            onOpenNavagrahaMantras = onOpenNavagrahaMantras,
                            onOpenAnnualFestivals = onOpenAnnualFestivals,
                            onOpenSwaraYoga = onOpenSwaraYoga,
                            onOpenPlanetaryPositions = onOpenPlanetaryPositions,
                            onOpenNotificationStudio = onOpenNotificationStudio,
                            onOpenSettings = onOpenSettings,
                            onOpenAdminPanel = onOpenAdminPanel
                        )
                    }

                    item(key = "sun_moon") {
                        SunMoonTelemetryCard(snapshot = snap, nowMs = state.nowMs)
                    }

                    item(key = "five_angas") {
                        FiveAngasOverviewCard(
                            snapshot = snap,
                            nowMs = state.nowMs,
                            onSeeAllClick = onOpenDailyPanchanga,
                            onInspectAnga = { inspectedAnga = it }
                        )
                    }

                    item(key = "live_hora_swara") {
                        LiveHoraAndSwaraRow(
                            dailyHoras = content.dailyHoras,
                            liveSwara = content.liveSwara,
                            nowMs = state.nowMs,
                            onOpenHora = onOpenHora,
                            onOpenSwaraYoga = onOpenSwaraYoga
                        )
                    }

                    val nextMajor = snap.upcomingTransits.firstOrNull { it.type == "rasi" } ?: snap.upcomingTransits.firstOrNull()
                    if (nextMajor != null) {
                        item(key = "next_ingress_spotlight") {
                            NextMajorIngressSpotlightCard(
                                transit = nextMajor,
                                nowMs = state.nowMs,
                                timezone = snap.timezone,
                                onOpenPlanetaryPositions = {
                                    if (policy.canAccessPage(AppPageKey.PLANETARY_POSITIONS)) onOpenPlanetaryPositions()
                                }
                            )
                        }
                    }

                    item(key = "muhurta_quick") {
                        QuickMuhurtaSummaryCard(
                            snapshot = snap,
                            onOpenDailyPanchanga = onOpenDailyPanchanga,
                            onInspectTiming = { title, interval -> inspectedTiming = title to interval }
                        )
                    }

                    if (snap.upcomingTransits.isNotEmpty()) {
                        item(key = "transits_header") {
                            SectionHeader(
                                title = strings.upcomingEventsTitle,
                                subtitle = strings.upcomingEventsSubtitle,
                                trailing = {
                                    if (policy.canAccessPage(AppPageKey.PLANETARY_POSITIONS)) {
                                        TextButton(
                                            onClick = onOpenPlanetaryPositions,
                                            modifier = Modifier.testTag("home_open_planets_button")
                                        ) {
                                            Text(strings.navagrahaShort)
                                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                                        }
                                    }
                                }
                            )
                        }
                        items(
                            items = snap.upcomingTransits.take(4),
                            key = { it.id }
                        ) { transit ->
                            UpcomingTransitRowCard(transit = transit, timezone = snap.timezone)
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(56.dp))
                    }
                }
            }
        }

        StickyBackToTodayPill(
            visible = !isToday,
            onBackToToday = viewModel::onSelectToday,
            testTag = "home_back_to_today_pill",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (showQuickDndSheet) {
        val snap = (state.content as? HomeContentState.Success)?.snapshot
        val targetSunriseMs = when {
            snap == null -> null
            snap.sunriseMs > state.nowMs + 60_000L -> snap.sunriseMs
            snap.nextSunriseMs > state.nowMs + 60_000L -> snap.nextSunriseMs
            else -> null
        }
        TimedDndQuickSheet(
            audioSettings = state.audioSettings,
            nextSunriseMs = targetSunriseMs,
            timezone = state.preferences.selectedLocation.timezone,
            use24HourFormat = state.preferences.use24HourFormat,
            onUpdateAudioSettings = viewModel::updateAudioSettings,
            onOpenNotificationStudio = onOpenNotificationStudio,
            onDismiss = { showQuickDndSheet = false }
        )
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
            onOpenGlossary = if (showGlossary) {
                { query -> glossaryQuery = query }
            } else null
        )
    }

    inspectedTiming?.let { (title, interval) ->
        val tz = (state.content as? HomeContentState.Success)?.snapshot?.timezone
            ?: state.preferences.selectedLocation.timezone
        TimingWindowInspectorSheet(
            displayTitle = title,
            interval = interval,
            nowMs = state.nowMs,
            onDismiss = { inspectedTiming = null },
            onOpenGlossary = if (showGlossary) {
                { query -> glossaryQuery = query }
            } else null,
            timezone = tz
        )
    }

    if (showGlossary && glossaryQuery != null) {
        VedicGlossaryBottomSheet(
            initialQuery = glossaryQuery.orEmpty(),
            onDismiss = { glossaryQuery = null }
        )
    }
}
