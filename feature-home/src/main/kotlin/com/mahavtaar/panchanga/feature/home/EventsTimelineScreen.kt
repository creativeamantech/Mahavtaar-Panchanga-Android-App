package com.mahavtaar.panchanga.feature.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.TimingInterval
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.AuspiciousnessMeterBar
import com.mahavtaar.panchanga.ui.components.ChakraPetalAccentBar
import com.mahavtaar.panchanga.ui.components.DateLocationHeaderBar
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.LiveNowClarityBanner
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.MahavtaarBrandLogo
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TattvaBadge
import com.mahavtaar.panchanga.ui.localization.AccessControlStrings
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.ui.components.StickyBackToTodayPill
import java.time.LocalDate
import java.util.Locale

private enum class TimelineEventCategory {
    MUHURTA,
    PANCHANGA,
    HORA_TRANSIT
}

private data class MinimalEventItem(
    val id: String,
    val category: TimelineEventCategory,
    val title: String,
    val startFormatted: String,
    val endFormatted: String,
    val badgeText: String,
    val startMs: Long,
    val endMs: Long?,
    val isAuspicious: Boolean,
    val isCaution: Boolean,
    val primaryNadi: SwaraNadi? = null,
    val secondaryNadi: SwaraNadi? = null,
    val tattvaElement: TattvaElement? = null,
    val simpleNowSentence: String? = null,
    val description: String? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventsTimelineScreen(
    onOpenLocations: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdminPanel: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val policy = LocalAccessPolicy.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val ac = AccessControlStrings.forLanguage(lang)
    val vedic = LocalVedicColors.current

    val showDescriptions = policy.hasFeature(AppFeatureKey.EVENT_DESCRIPTIONS)
    val canChangeLocation = policy.canAccessPage(AppPageKey.LOCATIONS)
    var selectedFilter by remember { mutableIntStateOf(0) }

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
                .testTag("events_timeline_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "top_brand_header") {
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MahavtaarBrandLogo(size = 44.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ac.eventsTimelineTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = ac.eventsTimelineSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (policy.isAdmin) {
                                FilledTonalIconButton(
                                    onClick = onOpenAdminPanel,
                                    modifier = Modifier.testTag("events_open_admin_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = ac.adminPanelTitle,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            FilledTonalIconButton(
                                onClick = onOpenProfile,
                                modifier = Modifier.testTag("events_open_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = state.cloudUser?.displayName ?: ext.profileTitle,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            FilledTonalIconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.testTag("events_open_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = strings.settingsTitle
                                )
                            }
                        }
                        ChakraPetalAccentBar(height = 3.dp)
                    }
                }
            }

            item(key = "date_location_bar") {
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
                    onLocationClick = { if (canChangeLocation) onOpenLocations() },
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
                    val nowMs = state.nowMs

                    // 1. Live Active Events Card (right now)
                    item(key = "live_now_card") {
                        LiveEventsNowCard(
                            snapshot = snap,
                            dailyHoras = content.dailyHoras,
                            nowMs = nowMs
                        )
                    }

                    // 2. Today's Vrat & Festival Observances (Events only, description gated by EVENT_DESCRIPTIONS)
                    if (snap.observances.isNotEmpty()) {
                        item(key = "observance_events") {
                            PanchangaCard(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
                            ) {
                                SectionHeader(
                                    title = ac.todayObservancesTitle,
                                    subtitle = "${VedicLocalizer.masaName(snap.masaNumber, snap.isAdhika, snap.isKshaya, snap.masa, lang)} · ${VedicLocalizer.pakshaName(snap.paksha, lang)} ${strings.pakshaSuffix}"
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    snap.observances.forEach { obs ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
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
                                                        Icon(
                                                            imageVector = Icons.Rounded.Celebration,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.secondary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Text(
                                                            text = VedicLocalizer.observanceTitle(obs, lang),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    StatusPill(
                                                        label = VedicLocalizer.observanceDeity(obs, lang),
                                                        textColor = MaterialTheme.colorScheme.secondary,
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                    )
                                                }
                                                EventStartEndDateTimeView(
                                                    startText = VedicLocalizer.formatDateTimeWithSeconds(snap.sunriseMs, snap.timezone, lang),
                                                    endText = VedicLocalizer.formatDateTimeWithSeconds(snap.nextSunriseMs, snap.timezone, lang),
                                                    accentColor = MaterialTheme.colorScheme.secondary,
                                                    compact = true
                                                )
                                                if (showDescriptions) {
                                                    Text(
                                                        text = VedicLocalizer.observanceSubtitle(obs, lang),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Filter chips + Chronological Events Feed
                    val allEvents = buildMinimalEventsList(
                        snapshot = snap,
                        dailyHoras = content.dailyHoras,
                        nowMs = nowMs,
                        lang = lang,
                        showDescriptions = showDescriptions
                    )
                    val filteredEvents = when (selectedFilter) {
                        1 -> allEvents.filter { it.category == TimelineEventCategory.MUHURTA }
                        2 -> allEvents.filter { it.category == TimelineEventCategory.PANCHANGA }
                        3 -> allEvents.filter { it.category == TimelineEventCategory.HORA_TRANSIT }
                        else -> allEvents
                    }

                    item(key = "timeline_filters") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader(
                                title = ac.chronologicalEventsTitle,
                                subtitle = ac.chronologicalEventsSubtitle
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    0 to ac.filterAllEvents,
                                    1 to ac.filterMuhurtaWindows,
                                    2 to ac.filterPanchangaChanges,
                                    3 to ac.filterTransitsFestivals
                                ).forEach { (idx, label) ->
                                    FilterChip(
                                        selected = selectedFilter == idx,
                                        onClick = { selectedFilter = idx },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("events_filter_$idx")
                                    )
                                }
                            }
                        }
                    }

                    items(items = filteredEvents, key = { it.id }) { event ->
                        MinimalEventTimelineCard(
                            event = event,
                            nowMs = nowMs
                        )
                    }

                    // 4. Upcoming Planetary & Nakshatra Transits (Events only)
                    if (snap.upcomingTransits.isNotEmpty()) {
                        item(key = "planetary_transits_header") {
                            SectionHeader(
                                title = ac.upcomingPlanetaryEventsTitle,
                                subtitle = ac.upcomingPlanetaryEventsSubtitle
                            )
                        }
                        items(
                            items = snap.upcomingTransits.take(8),
                            key = { "tr_${it.id}" }
                        ) { transit ->
                            UpcomingTransitRowCard(transit = transit, timezone = snap.timezone)
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        val todayLocal = remember(state.preferences.selectedLocation.timezone) {
            LocalDate.now(TimeFormatters.safeZoneId(state.preferences.selectedLocation.timezone))
        }
        StickyBackToTodayPill(
            visible = state.selectedDate != todayLocal,
            onBackToToday = viewModel::onSelectToday,
            testTag = "events_back_to_today_pill",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun LiveEventsNowCard(
    snapshot: PanchangaSnapshot,
    dailyHoras: DailyHoras,
    nowMs: Long
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val ac = AccessControlStrings.forLanguage(lang)
    val vedic = LocalVedicColors.current

    fun inWindow(iv: TimingInterval?): Boolean {
        val s = iv?.startTimeMs ?: return false
        val e = iv.endTimeMs ?: return false
        return nowMs in s..e
    }

    val activeTithi = snapshot.tithi.firstOrNull {
        val s = it.startTimeMs ?: Long.MIN_VALUE
        val e = it.endTimeMs ?: Long.MAX_VALUE
        nowMs in s..e
    } ?: snapshot.tithi.firstOrNull()

    val activeNakshatra = snapshot.nakshatra.firstOrNull {
        val s = it.startTimeMs ?: Long.MIN_VALUE
        val e = it.endTimeMs ?: Long.MAX_VALUE
        nowMs in s..e
    } ?: snapshot.nakshatra.firstOrNull()

    val activeChoghadiya = (snapshot.gauriChoghadiyaDay + snapshot.gauriChoghadiyaNight).firstOrNull { inWindow(it) }
    val activeHora = dailyHoras.activeHora

    val activeCaution: Pair<String, TimingInterval>? = when {
        inWindow(snapshot.rahuKala) -> VedicLocalizer.muhurtaOrPeriodName("Rāhu Kāla", lang) to snapshot.rahuKala
        inWindow(snapshot.yamaganda) -> VedicLocalizer.muhurtaOrPeriodName("Yamagaṇḍa", lang) to snapshot.yamaganda
        inWindow(snapshot.gulikaKala) -> VedicLocalizer.muhurtaOrPeriodName("Gulika Kāla", lang) to snapshot.gulikaKala
        snapshot.durmuhurta.any { inWindow(it) } ->
            VedicLocalizer.muhurtaOrPeriodName("Durmuhūrta", lang) to snapshot.durmuhurta.first { inWindow(it) }
        else -> null
    }

    val activeAuspicious: Pair<String, TimingInterval>? = when {
        inWindow(snapshot.abhijitMuhurta) -> VedicLocalizer.muhurtaOrPeriodName("Abhijit Muhūrta", lang) to snapshot.abhijitMuhurta
        inWindow(snapshot.brahmaMuhurta) -> VedicLocalizer.muhurtaOrPeriodName("Brahma Muhūrta", lang) to snapshot.brahmaMuhurta
        snapshot.amritaKala.any { inWindow(it) } ->
            VedicLocalizer.muhurtaOrPeriodName("Amṛta Kāla", lang) to snapshot.amritaKala.first { inWindow(it) }
        else -> null
    }

    val tz = snapshot.timezone
    val sunriseStr = VedicLocalizer.formatDateTimeWithSeconds(snapshot.sunriseMs, tz, lang)
    val sunsetStr = VedicLocalizer.formatDateTimeWithSeconds(snapshot.sunsetMs, tz, lang)

    PanchangaCard {
        SectionHeader(
            title = ac.liveActiveEventsTitle,
            subtitle = "${strings.sunrise}: $sunriseStr · ${strings.sunset}: $sunsetStr"
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (activeCaution != null) {
            val (name, iv) = activeCaution
            LiveNowClarityBanner(
                sentence = VedicLocalizer.simpleNowIntervalSentence(name, isCaution = true, endMs = iv.endTimeMs, nowMs = nowMs, lang = lang),
                accentColor = vedic.inauspicious,
                containerColor = vedic.inauspiciousContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else if (activeAuspicious != null) {
            val (name, iv) = activeAuspicious
            LiveNowClarityBanner(
                sentence = VedicLocalizer.simpleNowIntervalSentence(name, isCaution = false, endMs = iv.endTimeMs, nowMs = nowMs, lang = lang),
                accentColor = vedic.auspicious,
                containerColor = vedic.auspiciousContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            LiveNowClarityBanner(
                sentence = VedicLocalizer.simpleNowClearWindowSentence(lang),
                accentColor = vedic.auspicious,
                containerColor = vedic.auspiciousContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Current Tithi & Nakshatra summary pills with Start & End Date+Seconds + bold Nadi badge
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeTithi != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.tithiLabel}: ${VedicLocalizer.tithiName(activeTithi.number, activeTithi.name, lang)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            activeTithi.swaraInfo?.let { SwaraNadiBadge(nadi = it.startNadi) }
                        }
                        EventStartEndDateTimeView(
                            startText = VedicLocalizer.formatAngaStart(activeTithi, snapshot.sunriseMs, tz, lang),
                            endText = VedicLocalizer.formatAngaEnd(activeTithi, snapshot.nextSunriseMs, tz, lang),
                            accentColor = MaterialTheme.colorScheme.primary,
                            compact = true
                        )
                    }
                }
            }
            if (activeNakshatra != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.nakshatraLabel}: ${VedicLocalizer.nakshatraName(activeNakshatra.number, activeNakshatra.name, lang)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            activeNakshatra.swaraInfo?.let { SwaraNadiBadge(nadi = it.startNadi) }
                        }
                        EventStartEndDateTimeView(
                            startText = VedicLocalizer.formatAngaStart(activeNakshatra, snapshot.sunriseMs, tz, lang),
                            endText = VedicLocalizer.formatAngaEnd(activeNakshatra, snapshot.nextSunriseMs, tz, lang),
                            accentColor = MaterialTheme.colorScheme.secondary,
                            compact = true
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeCaution != null) {
            val (name, iv) = activeCaution
            val rem = ((iv.endTimeMs ?: nowMs) - nowMs).coerceAtLeast(0L)
            val sText = iv.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.start
            val eText = iv.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.end
            LiveBannerRow(
                title = name,
                startText = sText,
                endText = eText,
                badge = VedicGlossaryCatalog.formatLiveCountdown(rem, lang),
                accent = vedic.inauspicious,
                container = vedic.inauspiciousContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
        } else if (activeAuspicious != null) {
            val (name, iv) = activeAuspicious
            val rem = ((iv.endTimeMs ?: nowMs) - nowMs).coerceAtLeast(0L)
            val sText = iv.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.start
            val eText = iv.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.end
            LiveBannerRow(
                title = name,
                startText = sText,
                endText = eText,
                badge = VedicGlossaryCatalog.formatLiveCountdown(rem, lang),
                accent = vedic.auspicious,
                container = vedic.auspiciousContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeChoghadiya != null) {
                val chogName = VedicLocalizer.muhurtaOrPeriodName(activeChoghadiya.name.orEmpty(), lang)
                val accent = if (activeChoghadiya.isAuspicious) vedic.auspicious else vedic.caution
                val sText = activeChoghadiya.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: activeChoghadiya.start
                val eText = activeChoghadiya.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: activeChoghadiya.end
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${ext.activeChoghadiyaLabel}: $chogName", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
                        EventStartEndDateTimeView(startText = sText, endText = eText, accentColor = accent, compact = true)
                    }
                }
            }
            if (activeHora != null) {
                val activeTattva = dailyHoras.activeTattva ?: activeHora.tattvas.firstOrNull { nowMs in it.startTimeMs..it.endTimeMs }
                val sText = VedicLocalizer.formatDateTimeWithSeconds(activeHora.startTimeMs, tz, lang)
                val eText = VedicLocalizer.formatDateTimeWithSeconds(activeHora.endTimeMs, tz, lang)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.currentHora}: ${VedicLocalizer.horaRulerName(activeHora.ruler, lang)} ${strings.horaSuffix}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SwaraNadiBadge(nadi = activeHora.nadi)
                            activeTattva?.let { TattvaBadge(element = it.element) }
                        }
                        EventStartEndDateTimeView(startText = sText, endText = eText, accentColor = MaterialTheme.colorScheme.primary, compact = true)
                        LiveNowClarityBanner(
                            sentence = VedicLocalizer.simpleNowHoraSentence(activeHora.ruler, activeHora.nadi, activeTattva?.element, lang),
                            accentColor = MaterialTheme.colorScheme.primary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBannerRow(
    title: String,
    startText: String,
    endText: String,
    badge: String,
    accent: Color,
    container: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    label = badge,
                    textColor = accent,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }
            EventStartEndDateTimeView(
                startText = startText,
                endText = endText,
                accentColor = accent,
                compact = true
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MinimalEventTimelineCard(
    event: MinimalEventItem,
    nowMs: Long
) {
    val vedic = LocalVedicColors.current
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current

    val isActive = event.endMs != null && nowMs in event.startMs..event.endMs
    val progress = if (isActive && event.endMs != null && event.endMs > event.startMs) {
        ((nowMs - event.startMs).toFloat() / (event.endMs - event.startMs).toFloat()).coerceIn(0f, 1f)
    } else null

    val accent = when {
        event.isCaution -> vedic.inauspicious
        event.isAuspicious -> vedic.auspicious
        else -> MaterialTheme.colorScheme.primary
    }
    val container = when {
        event.isCaution -> vedic.inauspiciousContainer
        event.isAuspicious -> vedic.auspiciousContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val icon = when (event.category) {
        TimelineEventCategory.MUHURTA -> if (event.isCaution) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle
        TimelineEventCategory.PANCHANGA -> Icons.Rounded.AutoAwesome
        TimelineEventCategory.HORA_TRANSIT -> Icons.Rounded.Schedule
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (isActive) container.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isActive) 1.4.dp else 1.dp,
            color = if (isActive) accent else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    label = if (isActive) strings.active else event.badgeText,
                    textColor = accent,
                    containerColor = container,
                    fontWeight = FontWeight.Bold
                )
            }

            EventStartEndDateTimeView(
                startText = event.startFormatted,
                endText = event.endFormatted,
                accentColor = accent,
                compact = true
            )

            if (event.primaryNadi != null || event.secondaryNadi != null || event.tattvaElement != null) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    event.primaryNadi?.let { SwaraNadiBadge(nadi = it) }
                    event.secondaryNadi?.takeIf { it != event.primaryNadi }?.let { SwaraNadiBadge(nadi = it) }
                    event.tattvaElement?.let { TattvaBadge(element = it) }
                }
            }

            if (isActive && !event.simpleNowSentence.isNullOrBlank()) {
                LiveNowClarityBanner(
                    sentence = event.simpleNowSentence,
                    accentColor = accent,
                    containerColor = container
                )
            }

            if (progress != null && event.endMs != null) {
                val remMs = (event.endMs - nowMs).coerceAtLeast(0L)
                AuspiciousnessMeterBar(
                    progress = progress,
                    accentColor = accent,
                    label = "${VedicLocalizer.formatDurationRemaining(remMs, lang)} ${strings.leftSuffix}",
                    trailingValue = "${(progress * 100).toInt()}%"
                )
            }

            if (!event.description.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun buildMinimalEventsList(
    snapshot: PanchangaSnapshot,
    dailyHoras: DailyHoras,
    nowMs: Long,
    lang: AppLanguage,
    showDescriptions: Boolean
): List<MinimalEventItem> {
    val events = mutableListOf<MinimalEventItem>()
    val isHi = lang == AppLanguage.HINDI
    val tz = snapshot.timezone

    fun addInterval(
        id: String,
        rawName: String,
        iv: TimingInterval?,
        isAuspicious: Boolean,
        isCaution: Boolean,
        descEn: String,
        descHi: String
    ) {
        if (iv == null) return
        val startMs = iv.startTimeMs ?: snapshot.sunriseMs
        val title = VedicLocalizer.muhurtaOrPeriodName(rawName, lang)
        val sText = iv.startTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.start
        val eText = iv.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) } ?: iv.end
        events += MinimalEventItem(
            id = id,
            category = TimelineEventCategory.MUHURTA,
            title = title,
            startFormatted = sText,
            endFormatted = eText,
            badgeText = when {
                isCaution -> if (isHi) "सावधानी" else "Caution"
                isAuspicious -> if (isHi) "शुभ" else "Auspicious"
                else -> if (isHi) "काल" else "Window"
            },
            startMs = startMs,
            endMs = iv.endTimeMs,
            isAuspicious = isAuspicious,
            isCaution = isCaution,
            simpleNowSentence = VedicLocalizer.simpleNowIntervalSentence(title, isCaution, iv.endTimeMs, nowMs, lang),
            description = if (showDescriptions) (if (isHi) descHi else descEn) else null
        )
    }

    addInterval(
        id = "ev_brahma",
        rawName = "Brahma Muhūrta",
        iv = snapshot.brahmaMuhurta,
        isAuspicious = true,
        isCaution = false,
        descEn = "Pre-dawn spiritual & meditation window.",
        descHi = "प्रातः ध्यान, जप एवं स्वाध्याय का शुभ मुहूर्त।"
    )
    addInterval(
        id = "ev_abhijit",
        rawName = "Abhijit Muhūrta",
        iv = snapshot.abhijitMuhurta,
        isAuspicious = true,
        isCaution = false,
        descEn = "Midday victorious Muhurta for important beginnings.",
        descHi = "मध्याह्न का सर्वार्थ शुभ अभिजीत मुहूर्त।"
    )
    snapshot.amritaKala.forEachIndexed { idx, iv ->
        addInterval(
            id = "ev_amrita_$idx",
            rawName = "Amṛta Kāla",
            iv = iv,
            isAuspicious = true,
            isCaution = false,
            descEn = "Nectar lunar window for auspicious tasks.",
            descHi = "शुभ कार्यों हेतु अमृत काल।"
        )
    }
    addInterval(
        id = "ev_vijaya",
        rawName = if (isHi) "विजय मुहूर्त" else "Vijaya Muhūrta",
        iv = snapshot.vijayaMuhurta,
        isAuspicious = true,
        isCaution = false,
        descEn = "Afternoon window associated with success.",
        descHi = "अपराह्न का विजय मुहूर्त।"
    )
    addInterval(
        id = "ev_godhuli",
        rawName = if (isHi) "गोधूलि मुहूर्त" else "Godhūli Muhūrta",
        iv = snapshot.godhuliMuhurta,
        isAuspicious = true,
        isCaution = false,
        descEn = "Sacred sunset twilight transition.",
        descHi = "सूर्यास्त संध्या का गोधूलि काल।"
    )
    addInterval(
        id = "ev_rahu",
        rawName = "Rāhu Kāla",
        iv = snapshot.rahuKala,
        isAuspicious = false,
        isCaution = true,
        descEn = "Avoid launching new ventures during Rahu Kala.",
        descHi = "राहु काल में नवीन शुभ कार्य का आरम्भ वर्जित।"
    )
    addInterval(
        id = "ev_yama",
        rawName = "Yamagaṇḍa",
        iv = snapshot.yamaganda,
        isAuspicious = false,
        isCaution = true,
        descEn = "Inauspicious window for fresh ceremonial starts.",
        descHi = "यमगण्ड काल में नए मांगलिक कार्य टालें।"
    )
    addInterval(
        id = "ev_gulika",
        rawName = "Gulika Kāla",
        iv = snapshot.gulikaKala,
        isAuspicious = false,
        isCaution = true,
        descEn = "Saturnine caution window.",
        descHi = "गुलिक कालखण्ड।"
    )
    snapshot.durmuhurta.forEachIndexed { idx, iv ->
        addInterval(
            id = "ev_dur_$idx",
            rawName = "Durmuhūrta",
            iv = iv,
            isAuspicious = false,
            isCaution = true,
            descEn = "Short caution period to avoid new starts.",
            descHi = "दुर्मुहूर्त सावधानी काल।"
        )
    }

    // Panchanga Anga segments (Tithi, Nakshatra, Yoga) with Date+Seconds and bold Nadi/Tattva
    snapshot.tithi.forEachIndexed { idx, seg ->
        val tName = VedicLocalizer.tithiName(seg.number, seg.name, lang)
        val title = "${if (isHi) "तिथि" else "Tithi"}: $tName"
        events += MinimalEventItem(
            id = "ev_tithi_$idx",
            category = TimelineEventCategory.PANCHANGA,
            title = title,
            startFormatted = VedicLocalizer.formatAngaStart(seg, snapshot.sunriseMs, tz, lang),
            endFormatted = VedicLocalizer.formatAngaEnd(seg, snapshot.nextSunriseMs, tz, lang),
            badgeText = if (isHi) "तिथि" else "Tithi",
            startMs = seg.startTimeMs ?: snapshot.sunriseMs,
            endMs = seg.endTimeMs,
            isAuspicious = true,
            isCaution = false,
            primaryNadi = seg.swaraInfo?.startNadi,
            secondaryNadi = seg.swaraInfo?.endNadi,
            simpleNowSentence = VedicLocalizer.simpleNowIntervalSentence(title, false, seg.endTimeMs, nowMs, lang)
        )
    }

    snapshot.nakshatra.forEachIndexed { idx, seg ->
        val nName = VedicLocalizer.nakshatraName(seg.number, seg.name, lang)
        val title = "${if (isHi) "नक्षत्र" else "Nakshatra"}: $nName"
        events += MinimalEventItem(
            id = "ev_nak_$idx",
            category = TimelineEventCategory.PANCHANGA,
            title = title,
            startFormatted = VedicLocalizer.formatAngaStart(seg, snapshot.sunriseMs, tz, lang),
            endFormatted = VedicLocalizer.formatAngaEnd(seg, snapshot.nextSunriseMs, tz, lang),
            badgeText = if (isHi) "नक्षत्र" else "Nakshatra",
            startMs = seg.startTimeMs ?: snapshot.sunriseMs,
            endMs = seg.endTimeMs,
            isAuspicious = true,
            isCaution = false,
            primaryNadi = seg.swaraInfo?.startNadi,
            secondaryNadi = seg.swaraInfo?.endNadi,
            simpleNowSentence = VedicLocalizer.simpleNowIntervalSentence(title, false, seg.endTimeMs, nowMs, lang)
        )
    }

    snapshot.yoga.forEachIndexed { idx, seg ->
        val yName = VedicLocalizer.yogaName(seg.number, seg.name, lang)
        val title = "${if (isHi) "योग" else "Yoga"}: $yName"
        events += MinimalEventItem(
            id = "ev_yoga_$idx",
            category = TimelineEventCategory.PANCHANGA,
            title = title,
            startFormatted = VedicLocalizer.formatAngaStart(seg, snapshot.sunriseMs, tz, lang),
            endFormatted = VedicLocalizer.formatAngaEnd(seg, snapshot.nextSunriseMs, tz, lang),
            badgeText = if (isHi) "योग" else "Yoga",
            startMs = seg.startTimeMs ?: snapshot.sunriseMs,
            endMs = seg.endTimeMs,
            isAuspicious = false,
            isCaution = false,
            simpleNowSentence = VedicLocalizer.simpleNowIntervalSentence(title, false, seg.endTimeMs, nowMs, lang)
        )
    }

    // Active & Upcoming Hora windows with Nadi & Tattva badges
    dailyHoras.horas.forEach { hora ->
        val ruler = VedicLocalizer.horaRulerName(hora.ruler, lang)
        val activeSubTattva = hora.tattvas.firstOrNull { nowMs in it.startTimeMs..it.endTimeMs }
            ?: hora.tattvas.firstOrNull()
        events += MinimalEventItem(
            id = "ev_hora_${hora.index}",
            category = TimelineEventCategory.HORA_TRANSIT,
            title = "$ruler ${if (isHi) "होरा" else "Hora"} (#${hora.index})",
            startFormatted = VedicLocalizer.formatDateTimeWithSeconds(hora.startTimeMs, tz, lang),
            endFormatted = VedicLocalizer.formatDateTimeWithSeconds(hora.endTimeMs, tz, lang),
            badgeText = if (isHi) "होरा" else "Hora",
            startMs = hora.startTimeMs,
            endMs = hora.endTimeMs,
            isAuspicious = hora.ruler in listOf("Jupiter", "Venus", "Mercury", "Moon"),
            isCaution = hora.ruler in listOf("Saturn", "Mars"),
            primaryNadi = hora.nadi,
            tattvaElement = activeSubTattva?.element,
            simpleNowSentence = VedicLocalizer.simpleNowHoraSentence(hora.ruler, hora.nadi, activeSubTattva?.element, lang)
        )
    }

    // Swara Yoga celestial windows
    val swaraWindows = listOfNotNull(
        snapshot.swaraYoga.sunriseWindow.let { "sr" to it },
        snapshot.swaraYoga.middayWindow.let { "md" to it },
        snapshot.swaraYoga.sunsetWindow.let { "ss" to it },
        snapshot.swaraYoga.moonriseWindow?.let { "mr" to it },
        snapshot.swaraYoga.moonsetWindow?.let { "ms" to it }
    )
    swaraWindows.forEach { (key, win) ->
        val sMs = win.startTimeMs ?: return@forEach
        val eMs = win.endTimeMs ?: return@forEach
        val title = when (key) {
            "sr" -> if (isHi) "सूर्योदय स्वर काल" else "Sunrise Swara Window"
            "md" -> if (isHi) "मध्याह्न स्वर काल" else "Midday Swara Window"
            "ss" -> if (isHi) "सूर्यास्त स्वर काल" else "Sunset Swara Window"
            "mr" -> if (isHi) "चन्द्रोदय स्वर काल" else "Moonrise Swara Window"
            else -> if (isHi) "चन्द्रास्त स्वर काल" else "Moonset Swara Window"
        }
        val winTattva = dailyHoras.horas
            .firstOrNull { sMs in it.startTimeMs..it.endTimeMs }
            ?.let { h -> h.tattvas.firstOrNull { sMs in it.startTimeMs..it.endTimeMs }?.element ?: h.tattvas.firstOrNull()?.element }
        events += MinimalEventItem(
            id = "ev_swara_$key",
            category = TimelineEventCategory.HORA_TRANSIT,
            title = title,
            startFormatted = VedicLocalizer.formatDateTimeWithSeconds(sMs, tz, lang),
            endFormatted = VedicLocalizer.formatDateTimeWithSeconds(eMs, tz, lang),
            badgeText = if (isHi) "स्वर योग" else "Swara",
            startMs = sMs,
            endMs = eMs,
            isAuspicious = win.nadi != SwaraNadi.SUSHUMNA,
            isCaution = win.nadi == SwaraNadi.SUSHUMNA,
            primaryNadi = win.nadi,
            tattvaElement = winTattva,
            simpleNowSentence = VedicLocalizer.simpleNowNadiSentence(win.nadi, lang)
        )
    }

    return events.sortedBy { it.startMs }
}
