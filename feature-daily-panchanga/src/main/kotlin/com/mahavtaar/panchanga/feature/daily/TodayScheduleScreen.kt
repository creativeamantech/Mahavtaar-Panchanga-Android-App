package com.mahavtaar.panchanga.feature.daily

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.Auspiciousness
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.HoraPeriod
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.ScheduleCategory
import com.mahavtaar.panchanga.domain.model.ScheduleEvent
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.domain.usecase.GetDailyPanchangaUseCase
import com.mahavtaar.panchanga.domain.usecase.GetLiveHoraUseCase
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.LiveNowClarityBanner
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TattvaBadge
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale
import javax.inject.Inject

data class TodayScheduleUiState(
    val preferences: UserPreferences = UserPreferences(),
    val selectedCategory: ScheduleCategory = ScheduleCategory.ALL,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val nowMs: Long = System.currentTimeMillis(),
    val snapshot: PanchangaSnapshot? = null,
    val dailyHoras: DailyHoras? = null,
    val events: List<ScheduleEvent> = emptyList()
)

@HiltViewModel
class TodayScheduleViewModel @Inject constructor(
    private val getDailyPanchangaUseCase: GetDailyPanchangaUseCase,
    private val getLiveHoraUseCase: GetLiveHoraUseCase,
    private val astrologyRepository: AstrologyRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayScheduleUiState())
    val uiState: StateFlow<TodayScheduleUiState> = _uiState.asStateFlow()
    private var lastComputeKey: String? = null

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
                val loc = prefs.selectedLocation
                val key = "${loc.name}|${"%.5f".format(Locale.US, loc.latitude)}|${"%.5f".format(Locale.US, loc.longitude)}|${loc.timezone}|${prefs.monthSystem.apiKey}|${prefs.ayanamsha.apiKey}"
                if (key != lastComputeKey) {
                    lastComputeKey = key
                    load(prefs)
                }
            }
        }
        viewModelScope.launch {
            flow {
                while (true) {
                    emit(System.currentTimeMillis())
                    delay(1_000L)
                }
            }.collect { now ->
                val snap = _uiState.value.snapshot
                if (snap != null) {
                    val horas = getLiveHoraUseCase(snap, now)
                    _uiState.update { it.copy(nowMs = now, dailyHoras = horas) }
                } else {
                    _uiState.update { it.copy(nowMs = now) }
                }
            }
        }
    }

    private fun load(prefs: UserPreferences) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val now = System.currentTimeMillis()
                val zone = TimeFormatters.safeZoneId(prefs.selectedLocation.timezone)
                val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                val snap = getDailyPanchangaUseCase(
                    location = prefs.selectedLocation,
                    date = today,
                    monthSystem = prefs.monthSystem,
                    ayanamsha = prefs.ayanamsha,
                    includeExtendedTransits = false
                )
                val horas = getLiveHoraUseCase(snap, now)
                Triple(snap, horas, astrologyRepository.buildDailySchedule(snap, horas, now))
            }.onSuccess { (snap, horas, evs) ->
                _uiState.update { it.copy(isLoading = false, snapshot = snap, dailyHoras = horas, events = evs) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Error building schedule") }
            }
        }
    }

    fun selectCategory(cat: ScheduleCategory) = _uiState.update { it.copy(selectedCategory = cat) }

    fun toggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }

    fun retry() {
        lastComputeKey = null
        load(_uiState.value.preferences)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodayScheduleScreen(
    onNavigateBack: () -> Unit,
    onOpenNotificationStudio: () -> Unit = {},
    viewModel: TodayScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val policy = LocalAccessPolicy.current
    val canNotifs = policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)
    val canTattvaNadi = policy.hasFeature(AppFeatureKey.TATTVA_NADI_DETAILS)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val expandedHoraIds = remember { mutableStateMapOf<String, Boolean>() }

    val filters = listOf(
        ScheduleCategory.ALL to strings.filterAll,
        ScheduleCategory.MUHURTA to strings.filterMuhurta,
        ScheduleCategory.CHOGHADIYA to strings.filterChoghadiya,
        ScheduleCategory.PANCHANGA to strings.filterPanchanga,
        ScheduleCategory.HORA to strings.filterHora,
        ScheduleCategory.SWARA to (if (lang == AppLanguage.HINDI) "स्वर योग" else "Swara"),
        ScheduleCategory.ASTRONOMICAL to strings.filterAstro
    )

    val filteredEvents = remember(state.events, state.selectedCategory) {
        if (state.selectedCategory == ScheduleCategory.ALL) state.events
        else state.events.filter { it.category == state.selectedCategory }
    }

    val liveIdx = remember(filteredEvents, state.nowMs) {
        filteredEvents.indexOfFirst { !it.isCompleted && (it.isCurrent || it.startTimeMs >= state.nowMs) }
    }

    val horaByStartMs = remember(state.dailyHoras) {
        state.dailyHoras?.horas?.associateBy { it.startTimeMs }.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.scheduleTopBarTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("schedule_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    if (canNotifs) {
                        IconButton(onClick = onOpenNotificationStudio, modifier = Modifier.testTag("schedule_open_notif_studio")) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = ext.setAlert)
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("today_schedule_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.snapshot != null && state.dailyHoras != null) {
                    item(key = "live_hero") {
                        ScheduleLiveHeroCard(
                            snapshot = state.snapshot!!,
                            dailyHoras = state.dailyHoras!!,
                            nowMs = state.nowMs
                        )
                    }
                }

                item(key = "filter_row") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filters, key = { it.first.name }) { (cat, label) ->
                                FilterChip(
                                    selected = state.selectedCategory == cat,
                                    onClick = { viewModel.selectCategory(cat) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("schedule_filter_${cat.name.lowercase()}")
                                )
                            }
                        }
                        if (liveIdx >= 0) {
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        val headerOffset = if (state.snapshot != null && state.dailyHoras != null) 2 else 1
                                        listState.animateScrollToItem((liveIdx + headerOffset).coerceAtLeast(0))
                                    }
                                },
                                modifier = Modifier.testTag("schedule_jump_to_now")
                            ) {
                                Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(ext.jumpToNow)
                            }
                        }
                    }
                }

                when {
                    state.isLoading -> item(key = "loading") { LoadingStateBox() }
                    state.errorMessage != null -> item(key = "error") { ErrorStateCard(state.errorMessage!!, onRetry = viewModel::retry) }
                    else -> {
                        filteredEvents.forEachIndexed { idx, ev ->
                            if (idx == liveIdx) {
                                item(key = "current_time_divider") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                                        StatusPill(
                                            label = ext.currentTimeMarker,
                                            textColor = MaterialTheme.colorScheme.error,
                                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            item(key = ev.id) {
                                val tz = state.snapshot?.timezone ?: state.preferences.selectedLocation.timezone
                                val matchingHora: HoraPeriod? = if (ev.category == ScheduleCategory.HORA) horaByStartMs[ev.startTimeMs] else null
                                val expanded = expandedHoraIds[ev.id] == true
                                val accent = when (ev.auspiciousness) {
                                    Auspiciousness.AUSPICIOUS -> vedic.auspicious
                                    Auspiciousness.INAUSPICIOUS -> vedic.inauspicious
                                    Auspiciousness.NEUTRAL -> MaterialTheme.colorScheme.primary
                                }
                                val containerTint = when (ev.auspiciousness) {
                                    Auspiciousness.AUSPICIOUS -> vedic.auspiciousContainer
                                    Auspiciousness.INAUSPICIOUS -> vedic.inauspiciousContainer
                                    Auspiciousness.NEUTRAL -> MaterialTheme.colorScheme.primaryContainer
                                }
                                val evStartText = VedicLocalizer.formatDateTimeWithSeconds(ev.startTimeMs, tz, lang)
                                val evEndText = ev.endTimeMs?.let { VedicLocalizer.formatDateTimeWithSeconds(it, tz, lang) }
                                    ?: ev.formattedEnd.ifBlank { ev.formattedRange }
                                val evTitle = if (lang == AppLanguage.HINDI) ev.titleHi else ev.titleEn

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (ev.isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(if (ev.isCurrent) 1.5.dp else 1.dp, if (ev.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(enabled = matchingHora != null && canTattvaNadi) {
                                            expandedHoraIds[ev.id] = !expanded
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.width(4.dp).height(48.dp).background(accent, RoundedCornerShape(4.dp)))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = evTitle,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (ev.isCurrent) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        StatusPill(strings.live, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.Bold)
                                                    } else if (ev.isCompleted) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        StatusPill(strings.completedBadge, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                                                    }
                                                }
                                                Text(if (lang == AppLanguage.HINDI) ev.subtitleHi else ev.subtitleEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                EventStartEndDateTimeView(
                                                    startText = evStartText,
                                                    endText = evEndText,
                                                    accentColor = accent,
                                                    compact = true
                                                )
                                            }
                                            if (matchingHora != null && canTattvaNadi) {
                                                Icon(
                                                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                    contentDescription = strings.details,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (canNotifs) {
                                                IconButton(onClick = onOpenNotificationStudio) {
                                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = ext.setAlert, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }

                                        if (ev.primaryNadi != null || ev.secondaryNadi != null || ev.tattvaElement != null) {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                ev.primaryNadi?.let { SwaraNadiBadge(nadi = it) }
                                                ev.secondaryNadi?.takeIf { it != ev.primaryNadi }?.let { SwaraNadiBadge(nadi = it) }
                                                ev.tattvaElement?.let { TattvaBadge(element = it) }
                                            }
                                        }

                                        if (ev.isCurrent) {
                                            val simpleSentence = when {
                                                ev.category == ScheduleCategory.SWARA && ev.primaryNadi != null ->
                                                    VedicLocalizer.simpleNowNadiSentence(ev.primaryNadi!!, lang)
                                                ev.category == ScheduleCategory.HORA && ev.primaryNadi != null ->
                                                    VedicLocalizer.simpleNowHoraSentence(matchingHora?.ruler ?: evTitle, ev.primaryNadi!!, ev.tattvaElement, lang)
                                                else -> VedicLocalizer.simpleNowIntervalSentence(
                                                    title = evTitle,
                                                    isCaution = ev.auspiciousness == Auspiciousness.INAUSPICIOUS,
                                                    endMs = ev.endTimeMs,
                                                    nowMs = state.nowMs,
                                                    lang = lang
                                                )
                                            }
                                            LiveNowClarityBanner(
                                                sentence = simpleSentence,
                                                accentColor = accent,
                                                containerColor = containerTint
                                            )
                                        }

                                        if (matchingHora != null && canTattvaNadi && expanded) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(ext.nadiGuidanceBanner, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                SwaraNadiBadge(nadi = matchingHora.nadi)
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                matchingHora.tattvas.forEach { t ->
                                                    val subLive = state.nowMs in t.startTimeMs..t.endTimeMs
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (subLive) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                        border = if (subLive) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            TattvaBadge(element = t.element)
                                                            EventStartEndDateTimeView(
                                                                startText = VedicLocalizer.formatDateTimeWithSeconds(t.startTimeMs, tz, lang),
                                                                endText = VedicLocalizer.formatDateTimeWithSeconds(t.endTimeMs, tz, lang),
                                                                accentColor = MaterialTheme.colorScheme.secondary,
                                                                compact = true
                                                            )
                                                            if (subLive) {
                                                                LiveNowClarityBanner(
                                                                    sentence = VedicLocalizer.simpleNowTattvaSentence(t.element, lang),
                                                                    accentColor = MaterialTheme.colorScheme.secondary,
                                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleLiveHeroCard(
    snapshot: PanchangaSnapshot,
    dailyHoras: DailyHoras,
    nowMs: Long
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val tz = snapshot.timezone
    val activeHora = dailyHoras.activeHora ?: dailyHoras.horas.firstOrNull()
    val remHora = activeHora?.let { (it.endTimeMs - nowMs).coerceAtLeast(0L) } ?: 0L
    val progHora = activeHora?.let {
        ((nowMs - it.startTimeMs).toFloat() / (it.endTimeMs - it.startTimeMs).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    } ?: 0f

    PanchangaCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)) {
        SectionHeader(
            title = ext.liveHeroTitle,
            subtitle = activeHora?.let { "${VedicLocalizer.horaRulerName(it.ruler, lang)} ${strings.horaSuffix} · ${VedicGlossaryCatalog.formatLiveCountdown(remHora, lang)}" }
        )
        if (activeHora != null) {
            Spacer(modifier = Modifier.height(6.dp))
            EventStartEndDateTimeView(
                startText = VedicLocalizer.formatDateTimeWithSeconds(activeHora.startTimeMs, tz, lang),
                endText = VedicLocalizer.formatDateTimeWithSeconds(activeHora.endTimeMs, tz, lang),
                accentColor = MaterialTheme.colorScheme.primary,
                compact = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            LiveNowClarityBanner(
                sentence = VedicLocalizer.simpleNowHoraSentence(activeHora.ruler, activeHora.nadi, dailyHoras.activeTattva?.element, lang),
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progHora },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            activeHora?.let { h ->
                SwaraNadiBadge(nadi = h.nadi)
            }
            dailyHoras.activeTattva?.let { t ->
                TattvaBadge(element = t.element)
            }
            snapshot.tithi.firstOrNull()?.let { t ->
                StatusPill(VedicLocalizer.tithiName(t.number, t.name, lang), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.Bold)
            }
            snapshot.nakshatra.firstOrNull()?.let { n ->
                StatusPill(VedicLocalizer.nakshatraName(n.number, n.name, lang), MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}
