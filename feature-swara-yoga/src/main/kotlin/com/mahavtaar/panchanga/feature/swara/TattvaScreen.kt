package com.mahavtaar.panchanga.feature.swara

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Tune
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.session.PanchangaSessionHolder
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.HoraPeriod
import com.mahavtaar.panchanga.domain.model.PanchaTattvaReport
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.domain.usecase.GetDailyPanchangaUseCase
import com.mahavtaar.panchanga.domain.usecase.GetLiveHoraUseCase
import com.mahavtaar.panchanga.ui.access.AccessPermissionCatalog
import com.mahavtaar.panchanga.ui.access.LocalAccessPolicy
import com.mahavtaar.panchanga.ui.components.AtmosphericHeroCard
import com.mahavtaar.panchanga.ui.components.DateLocationHeaderBar
import com.mahavtaar.panchanga.ui.components.ErrorStateCard
import com.mahavtaar.panchanga.ui.components.EventStartEndDateTimeView
import com.mahavtaar.panchanga.ui.components.InspectorBottomSheetFrame
import com.mahavtaar.panchanga.ui.components.LanguageTogglePill
import com.mahavtaar.panchanga.ui.components.LiveCountdownWithMutedTimestamps
import com.mahavtaar.panchanga.ui.components.LiveNowClarityBanner
import com.mahavtaar.panchanga.ui.components.LoadingStateBox
import com.mahavtaar.panchanga.ui.components.PanchangaCard
import com.mahavtaar.panchanga.ui.components.SectionHeader
import com.mahavtaar.panchanga.ui.components.StatusPill
import com.mahavtaar.panchanga.ui.components.StickyBackToTodayPill
import com.mahavtaar.panchanga.ui.components.SwaraNadiBadge
import com.mahavtaar.panchanga.ui.components.TattvaBadge
import com.mahavtaar.panchanga.ui.components.TattvaMandalaCanvas
import com.mahavtaar.panchanga.ui.localization.LocalAppLanguage
import com.mahavtaar.panchanga.ui.localization.LocalExtendedStrings
import com.mahavtaar.panchanga.ui.localization.LocalStrings
import com.mahavtaar.panchanga.ui.localization.VedicGlossaryCatalog
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.ui.localization.VedicSwaraNavtaraCatalog
import com.mahavtaar.panchanga.ui.theme.LocalVedicColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

data class TattvaUiState(
    val preferences: UserPreferences = UserPreferences(),
    val selectedDate: LocalDate = LocalDate.now(),
    val liveClockText: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val nowMs: Long = System.currentTimeMillis(),
    val report: PanchaTattvaReport? = null,
    val dailyHoras: DailyHoras? = null,
    val activeHora: HoraPeriod? = null,
    val cycleOffset: Int = 0,
    val selectedElementFilter: TattvaElement? = null,
    val horaDayNightFilter: Int = 0,
    val searchQuery: String = "",
    val showVaraMasterTable: Boolean = false,
    val showTithiMasterTable: Boolean = false,
    val tithiMasterPakshaFilter: Int = 0,
    val showKaranaMasterTable: Boolean = false,
    val chimeCounter: Int = 0
)

@HiltViewModel
class TattvaViewModel @Inject constructor(
    private val getDailyPanchangaUseCase: GetDailyPanchangaUseCase,
    private val getLiveHoraUseCase: GetLiveHoraUseCase,
    private val astrologyRepository: AstrologyRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val sessionHolder: PanchangaSessionHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(TattvaUiState())
    val uiState: StateFlow<TattvaUiState> = _uiState.asStateFlow()
    private var latestSnapshot: PanchangaSnapshot? = null
    private var lastComputeKey: String? = null

    init {
        viewModelScope.launch {
            combine(preferencesRepository.preferences, sessionHolder.selectedDate) { p, d -> p to d }
                .collect { (prefs, date) ->
                    _uiState.update { it.copy(preferences = prefs, selectedDate = date) }
                    val loc = prefs.selectedLocation
                    val key = "${loc.name}|${"%.5f".format(Locale.US, loc.latitude)}|${"%.5f".format(Locale.US, loc.longitude)}|${loc.timezone}|${prefs.monthSystem.apiKey}|${prefs.ayanamsha.apiKey}|$date"
                    if (key != lastComputeKey || latestSnapshot == null) {
                        lastComputeKey = key
                        recompute(prefs, date)
                    }
                }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                val now = System.currentTimeMillis()
                val prefs = _uiState.value.preferences
                val clock = if (prefs.use24HourFormat) {
                    TimeFormatters.format24HourHMS(now, prefs.selectedLocation.timezone)
                } else {
                    TimeFormatters.format12HourHMS(now, prefs.selectedLocation.timezone)
                }
                val snap = latestSnapshot
                if (snap != null) {
                    val evalMs = evaluationTimestampForDate(_uiState.value.selectedDate, prefs.selectedLocation.timezone, now)
                    val horas = getLiveHoraUseCase(snap, evalMs)
                    val rep = astrologyRepository.computePanchaTattva(
                        snapshot = snap,
                        dailyHoras = horas,
                        nowMs = evalMs,
                        cycleOffset = _uiState.value.cycleOffset
                    )
                    _uiState.update {
                        it.copy(
                            nowMs = evalMs,
                            liveClockText = clock,
                            report = rep,
                            dailyHoras = horas,
                            activeHora = horas.activeHora
                        )
                    }
                } else {
                    _uiState.update { it.copy(nowMs = now, liveClockText = clock) }
                }
            }
        }
    }

    private fun evaluationTimestampForDate(date: LocalDate, timezone: String, nowMs: Long): Long {
        val zone = TimeFormatters.safeZoneId(timezone)
        val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        return if (date == today) {
            nowMs
        } else {
            val nowTime = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalTime()
            date.atTime(nowTime).atZone(zone).toInstant().toEpochMilli()
        }
    }

    private fun recompute(prefs: UserPreferences, date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val now = System.currentTimeMillis()
                val evalMs = evaluationTimestampForDate(date, prefs.selectedLocation.timezone, now)
                val snap = getDailyPanchangaUseCase(
                    location = prefs.selectedLocation,
                    date = date,
                    monthSystem = prefs.monthSystem,
                    ayanamsha = prefs.ayanamsha,
                    includeExtendedTransits = false
                )
                latestSnapshot = snap
                val horas = getLiveHoraUseCase(snap, evalMs)
                val rep = astrologyRepository.computePanchaTattva(
                    snapshot = snap,
                    dailyHoras = horas,
                    nowMs = evalMs,
                    cycleOffset = _uiState.value.cycleOffset
                )
                Triple(horas, rep, evalMs)
            }.onSuccess { (horas, rep, evalMs) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = rep,
                        dailyHoras = horas,
                        activeHora = horas.activeHora,
                        nowMs = evalMs
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Error computing Tattva") }
            }
        }
    }

    fun selectCycleOffset(offset: Int) {
        val clamped = offset.coerceIn(-1, 1)
        _uiState.update { it.copy(cycleOffset = clamped) }
        val snap = latestSnapshot ?: return
        val horas = _uiState.value.dailyHoras ?: return
        val evalMs = _uiState.value.nowMs
        val rep = astrologyRepository.computePanchaTattva(snap, horas, evalMs, clamped)
        _uiState.update { it.copy(report = rep) }
    }

    fun selectElementFilter(element: TattvaElement?) {
        _uiState.update { it.copy(selectedElementFilter = element) }
    }

    fun selectHoraDayNightFilter(mode: Int) {
        _uiState.update { it.copy(horaDayNightFilter = mode) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleVaraMasterTable() {
        _uiState.update { it.copy(showVaraMasterTable = !it.showVaraMasterTable) }
    }

    fun toggleTithiMasterTable() {
        _uiState.update { it.copy(showTithiMasterTable = !it.showTithiMasterTable) }
    }

    fun selectTithiMasterPakshaFilter(filter: Int) {
        _uiState.update { it.copy(tithiMasterPakshaFilter = filter) }
    }

    fun toggleKaranaMasterTable() {
        _uiState.update { it.copy(showKaranaMasterTable = !it.showKaranaMasterTable) }
    }

    fun onPreviousDay() = sessionHolder.shiftDays(-1)
    fun onNextDay() = sessionHolder.shiftDays(1)
    fun onSelectToday() = sessionHolder.selectToday(_uiState.value.preferences.selectedLocation.timezone)
    fun onSelectDate(date: LocalDate) = sessionHolder.selectDate(date)

    fun playTestChime() {
        runCatching { astrologyRepository.previewTattvaChime() }
        _uiState.update { it.copy(chimeCounter = it.chimeCounter + 1) }
    }

    fun toggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }

    fun setTattvaMinimalMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setTattvaMinimalMode(enabled)
        }
    }

    fun toggleLocalTattvaFeature(feature: AppFeatureKey, permittedFeatures: List<AppFeatureKey>) {
        val curPrefs = _uiState.value.preferences
        viewModelScope.launch {
            if (curPrefs.tattvaMinimalMode) {
                // Exiting pure minimal mode directly onto a single selected block
                val hideOthers = permittedFeatures.filter { it != feature }.map { it.name }.toSet()
                preferencesRepository.setTattvaHiddenFeatures(hideOthers)
                preferencesRepository.setTattvaMinimalMode(false)
            } else {
                val curHidden = curPrefs.tattvaHiddenFeatures
                val nextHidden = if (feature.name in curHidden) {
                    curHidden - feature.name
                } else {
                    curHidden + feature.name
                }
                preferencesRepository.setTattvaHiddenFeatures(nextHidden)
                if (permittedFeatures.all { it.name in nextHidden }) {
                    preferencesRepository.setTattvaMinimalMode(true)
                }
            }
        }
    }

    fun showAllPermittedTattvaFeatures() {
        viewModelScope.launch {
            preferencesRepository.setTattvaMinimalMode(false)
            preferencesRepository.setTattvaHiddenFeatures(emptySet())
        }
    }

    fun retry() {
        lastComputeKey = null
        recompute(_uiState.value.preferences, _uiState.value.selectedDate)
    }
}

@Immutable
data class TattvaVisibilityConfig(
    val minimalTimeOnly: Boolean,
    val permittedCount: Int,
    val activeLocalCount: Int,
    val showHeroGuidance: Boolean,
    val showTestChime: Boolean,
    val showNadiPolarity: Boolean,
    val showFourPillarGrid: Boolean,
    val showHarmonyReport: Boolean,
    val showHoraStrip: Boolean,
    val showVaraSummary: Boolean,
    val showVaraMasterTable: Boolean,
    val showTithiSummary: Boolean,
    val showTithiDayTable: Boolean,
    val showTithiMasterTable: Boolean,
    val showKaranaDayTable: Boolean,
    val showKaranaMasterTable: Boolean,
    val showMacroKaranaCycle: Boolean,
    val showHoraExplorerTable: Boolean,
    val showMahabhutaReference: Boolean
)

fun resolveTattvaVisibility(
    policy: AccessPolicy,
    prefs: UserPreferences
): TattvaVisibilityConfig {
    val permitted = AppFeatureKey.tattvaFeatures.filter { policy.hasFeature(it) }
    val hidden = prefs.tattvaHiddenFeatures
    val activePermitted = if (prefs.tattvaMinimalMode) 0 else permitted.count { it.name !in hidden }
    val isPureMinimal = prefs.tattvaMinimalMode || permitted.isEmpty() || activePermitted == 0

    fun isOn(key: AppFeatureKey): Boolean =
        !isPureMinimal && policy.hasFeature(key) && key.name !in hidden

    return TattvaVisibilityConfig(
        minimalTimeOnly = isPureMinimal,
        permittedCount = permitted.size,
        activeLocalCount = activePermitted,
        showHeroGuidance = isOn(AppFeatureKey.TATTVA_HERO_GUIDANCE),
        showTestChime = isOn(AppFeatureKey.TATTVA_TEST_CHIME),
        showNadiPolarity = isOn(AppFeatureKey.TATTVA_NADI_POLARITY),
        showFourPillarGrid = isOn(AppFeatureKey.TATTVA_FOUR_PILLAR_GRID),
        showHarmonyReport = isOn(AppFeatureKey.TATTVA_HARMONY_REPORT),
        showHoraStrip = isOn(AppFeatureKey.TATTVA_HORA_STRIP),
        showVaraSummary = isOn(AppFeatureKey.TATTVA_VARA_SUMMARY),
        showVaraMasterTable = isOn(AppFeatureKey.TATTVA_VARA_MASTER_TABLE),
        showTithiSummary = isOn(AppFeatureKey.TATTVA_TITHI_SUMMARY),
        showTithiDayTable = isOn(AppFeatureKey.TATTVA_TITHI_DAY_TABLE),
        showTithiMasterTable = isOn(AppFeatureKey.TATTVA_TITHI_MASTER_TABLE),
        showKaranaDayTable = isOn(AppFeatureKey.TATTVA_KARANA_DAY_TABLE),
        showKaranaMasterTable = isOn(AppFeatureKey.TATTVA_KARANA_MASTER_TABLE),
        showMacroKaranaCycle = isOn(AppFeatureKey.TATTVA_MACRO_KARANA_CYCLE),
        showHoraExplorerTable = isOn(AppFeatureKey.TATTVA_HORA_EXPLORER_TABLE),
        showMahabhutaReference = isOn(AppFeatureKey.TATTVA_MAHABHUTA_REFERENCE)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TattvaScreen(
    onNavigateBack: () -> Unit,
    onOpenNotificationStudio: () -> Unit = {},
    onOpenLocations: () -> Unit = {},
    viewModel: TattvaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val ext = LocalExtendedStrings.current
    val vedic = LocalVedicColors.current
    val policy = LocalAccessPolicy.current
    val canPickLocation = policy.canAccessPage(AppPageKey.LOCATIONS)
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var inspectedElement by remember { mutableStateOf<TattvaElement?>(null) }
    var showCustomizerSheet by remember { mutableStateOf(false) }

    val vis = remember(policy, state.preferences) {
        resolveTattvaVisibility(policy, state.preferences)
    }
    val permittedTattvaFeatures = remember(policy) {
        AppFeatureKey.tattvaFeatures.filter { policy.hasFeature(it) }
    }

    LaunchedEffect(state.chimeCounter) {
        if (state.chimeCounter > 0) {
            scope.launch { snackbarHostState.showSnackbar(ext.tattvaChimeTriggered) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.tattvaTopBarTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("tattva_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    if (policy.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO)) {
                        IconButton(
                            onClick = onOpenNotificationStudio,
                            modifier = Modifier.testTag("tattva_open_notif_studio")
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
            when {
                state.isLoading -> LoadingStateBox()
                state.errorMessage != null -> Box(Modifier.padding(16.dp)) {
                    ErrorStateCard(state.errorMessage!!, onRetry = viewModel::retry)
                }
                state.report != null -> {
                    val rep = state.report!!
                    val loc = state.preferences.selectedLocation
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 720.dp)
                            .animateContentSize(animationSpec = spring())
                            .testTag("tattva_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(key = "date_location_header") {
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
                                onToggleLanguage = viewModel::toggleLanguage,
                                coordinateSubtitle = if (vis.minimalTimeOnly) null else "%.5f°, %.5f°".format(Locale.US, loc.latitude, loc.longitude)
                            )
                        }

                        if (vis.permittedCount > 0) {
                            item(key = "tattva_view_mode_bar") {
                                TattvaViewModeBar(
                                    lang = lang,
                                    isMinimal = vis.minimalTimeOnly,
                                    activeCount = vis.activeLocalCount,
                                    totalPermitted = vis.permittedCount,
                                    onSelectMinimal = { viewModel.setTattvaMinimalMode(true) },
                                    onSelectFullOrCustom = {
                                        if (state.preferences.tattvaMinimalMode) {
                                            if (state.preferences.tattvaHiddenFeatures.size >= permittedTattvaFeatures.size) {
                                                viewModel.showAllPermittedTattvaFeatures()
                                            } else {
                                                viewModel.setTattvaMinimalMode(false)
                                            }
                                        }
                                    },
                                    onOpenCustomizer = { showCustomizerSheet = true }
                                )
                            }
                        }

                        // 1. Core Active Tattva & Time Hero Card (Always Visible Baseline)
                        item(key = "dominant") {
                            val activeSub = rep.activeHoraTattva
                            val activeEl = activeSub?.element ?: rep.dominantElement
                            val remMs = activeSub?.endTimeMs?.let { (it - state.nowMs).coerceAtLeast(0L) } ?: 0L
                            val isHi = lang == AppLanguage.HINDI

                            AtmosphericHeroCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (vis.minimalTimeOnly) {
                                            if (isHi) "वर्तमान तत्त्व एवं समय" else "Active Tattva & Time"
                                        } else {
                                            strings.dominantTattvaLabel
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (vis.showTestChime) {
                                        OutlinedButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.playTestChime()
                                            },
                                            modifier = Modifier.testTag("tattva_test_chime_button")
                                        ) {
                                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(ext.testChimeHaptic, style = MaterialTheme.typography.labelMedium)
                                        }
                                    } else if (remMs > 0L) {
                                        StatusPill(
                                            label = "${VedicGlossaryCatalog.formatLiveCountdown(remMs, lang)} ${strings.leftSuffix}",
                                            textColor = vedic.auspicious,
                                            containerColor = vedic.auspiciousContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LiveNowClarityBanner(
                                    sentence = VedicLocalizer.simpleNowTattvaSentence(activeEl, lang),
                                    accentColor = tattvaColorFor(activeEl),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TattvaMandalaCanvas(
                                        element = activeEl,
                                        sizeDp = 64.dp
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = VedicLocalizer.tattvaName(activeEl, lang),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.testTag("tattva_hero_active_name")
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TattvaBadge(element = activeEl)
                                            state.activeHora?.let { SwaraNadiBadge(nadi = it.nadi) }
                                        }
                                        if (activeSub != null) {
                                            if (remMs > 0L) {
                                                LiveCountdownWithMutedTimestamps(
                                                    countdownText = "${VedicGlossaryCatalog.formatLiveCountdown(remMs, lang)} ${strings.leftSuffix}",
                                                    startText = VedicLocalizer.formatDateTimeWithSeconds(activeSub.startTimeMs, loc.timezone, lang),
                                                    endText = VedicLocalizer.formatDateTimeWithSeconds(activeSub.endTimeMs, loc.timezone, lang),
                                                    accentColor = vedic.auspicious,
                                                    modifier = Modifier.testTag("tattva_hero_time_window")
                                                )
                                            } else {
                                                EventStartEndDateTimeView(
                                                    startText = VedicLocalizer.formatDateTimeWithSeconds(activeSub.startTimeMs, loc.timezone, lang),
                                                    endText = VedicLocalizer.formatDateTimeWithSeconds(activeSub.endTimeMs, loc.timezone, lang),
                                                    accentColor = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.testTag("tattva_hero_time_window")
                                                )
                                            }
                                        }
                                        if (vis.showHeroGuidance) {
                                            Text(
                                                text = VedicLocalizer.tattvaQuality(activeEl, lang),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = vedic.auspicious
                                            )
                                        }
                                    }
                                }
                                if (vis.showHeroGuidance) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (isHi) rep.guidanceHi else rep.guidanceEn,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // 2. Current State Overview (4-Pillar Quick Grid: Vara, Tithi, Hora, Karana)
                        if (vis.showFourPillarGrid) {
                            item(key = "four_pillars_grid") {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        PanchangaCard(modifier = Modifier.weight(1f)) {
                                            Text(strings.varaTattvaTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            TattvaBadge(element = rep.varaTattva.element)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = VedicLocalizer.vaaraName(rep.varaTattva.weekday, "", lang),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (vis.showNadiPolarity) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                SwaraNadiBadge(nadi = rep.varaTattva.nadi)
                                            }
                                        }
                                        PanchangaCard(modifier = Modifier.weight(1f)) {
                                            Text(strings.tithiTattvaTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            TattvaBadge(element = rep.tithiTattva.element)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${VedicLocalizer.pakshaName(rep.tithiTattva.paksha, lang)} · ${VedicLocalizer.tithiName(rep.tithiTattva.tithiNumber, rep.tithiTattva.tithiName, lang)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (rep.tithiTattva.startTimeMs != null && rep.tithiTattva.endTimeMs != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                EventStartEndDateTimeView(
                                                    startText = VedicLocalizer.formatDateTimeWithSeconds(rep.tithiTattva.startTimeMs!!, loc.timezone, lang),
                                                    endText = VedicLocalizer.formatDateTimeWithSeconds(rep.tithiTattva.endTimeMs!!, loc.timezone, lang),
                                                    compact = true
                                                )
                                            }
                                            if (vis.showNadiPolarity) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                SwaraNadiBadge(nadi = rep.tithiTattva.nadi)
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        PanchangaCard(modifier = Modifier.weight(1f)) {
                                            Text(strings.horaSubTattvaTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val hEl = rep.activeHoraTattva?.element ?: rep.dominantElement
                                            TattvaBadge(element = hEl)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = rep.activeHoraRuler?.let { VedicLocalizer.horaRulerName(it, lang) } ?: "—",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            rep.activeHoraTattva?.let { activeT ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                EventStartEndDateTimeView(
                                                    startText = VedicLocalizer.formatDateTimeWithSeconds(activeT.startTimeMs, loc.timezone, lang),
                                                    endText = VedicLocalizer.formatDateTimeWithSeconds(activeT.endTimeMs, loc.timezone, lang),
                                                    compact = true
                                                )
                                            }
                                            if (vis.showNadiPolarity) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                state.activeHora?.let { SwaraNadiBadge(nadi = it.nadi) }
                                            }
                                        }
                                        PanchangaCard(modifier = Modifier.weight(1f)) {
                                            Text(strings.karanaTattvaTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val macroEl = rep.karanaCycle?.activePeriod?.element ?: rep.karanaTattva.element
                                            TattvaBadge(element = macroEl)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${VedicLocalizer.karanaName(rep.karanaTattva.karanaName, lang)} (${VedicLocalizer.tattvaName(rep.karanaTattva.element, lang)})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (vis.showNadiPolarity) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                SwaraNadiBadge(nadi = rep.karanaTattva.nadi)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. 4-Pillar Tattva Alignment Report (Peak Harmony Hours Today)
                        if (vis.showHarmonyReport) {
                            item(key = "harmony_alignment_report") {
                                TattvaHarmonyAlignmentCard(
                                    report = rep,
                                    timezone = loc.timezone,
                                    onSelectElementFilter = { el ->
                                        viewModel.selectElementFilter(el)
                                    },
                                    showNadiPolarity = vis.showNadiPolarity
                                )
                            }
                        }

                        // 4. Current Hora 5-Tattva Strip
                        if (vis.showHoraStrip) {
                            state.activeHora?.let { hora ->
                                item(key = "hora_5tattva_strip") {
                                    HoraFiveTattvaStripCard(
                                        hora = hora,
                                        timezone = loc.timezone,
                                        activeTattvaEndMs = rep.activeHoraTattva?.endTimeMs,
                                        activeTattvaStartMs = rep.activeHoraTattva?.startTimeMs,
                                        nowMs = state.nowMs,
                                        activeRuler = rep.activeHoraRuler
                                    )
                                }
                            }
                        }

                        // 5. Vara Tattva & Nadi Detailed Card (Today + Expandable 7-Weekday Master Table)
                        if (vis.showVaraSummary || vis.showVaraMasterTable) {
                            item(key = "vara_tattva_section") {
                                VaraTattvaDetailedSectionCard(
                                    report = rep,
                                    showMasterTable = state.showVaraMasterTable,
                                    onToggleMasterTable = viewModel::toggleVaraMasterTable,
                                    showSummary = vis.showVaraSummary,
                                    showMasterTableSection = vis.showVaraMasterTable,
                                    showNadiPolarity = vis.showNadiPolarity
                                )
                            }
                        }

                        // 6. Tithi Tattva & Nadi Detailed Card (Start/End Panels + Today's Table + Expandable 30-Tithi Master Table)
                        if (vis.showTithiSummary || vis.showTithiDayTable || vis.showTithiMasterTable) {
                            item(key = "tithi_tattva_section") {
                                TithiTattvaDetailedSectionCard(
                                    report = rep,
                                    timezone = loc.timezone,
                                    showMasterTable = state.showTithiMasterTable,
                                    onToggleMasterTable = viewModel::toggleTithiMasterTable,
                                    pakshaFilter = state.tithiMasterPakshaFilter,
                                    onSelectPakshaFilter = viewModel::selectTithiMasterPakshaFilter,
                                    showSummary = vis.showTithiSummary,
                                    showDayTable = vis.showTithiDayTable,
                                    showMasterTableSection = vis.showTithiMasterTable,
                                    showNadiPolarity = vis.showNadiPolarity
                                )
                            }
                        }

                        // 7. Daily Half-Tithi Karana Tattva Table + Expandable 11-Karana Master Table
                        if (vis.showKaranaDayTable || vis.showKaranaMasterTable) {
                            item(key = "daily_karana_tattva_section") {
                                DailyKaranaTattvaSectionCard(
                                    report = rep,
                                    timezone = loc.timezone,
                                    showMasterTable = state.showKaranaMasterTable,
                                    onToggleMasterTable = viewModel::toggleKaranaMasterTable,
                                    showDayTable = vis.showKaranaDayTable,
                                    showMasterTableSection = vis.showKaranaMasterTable,
                                    showNadiPolarity = vis.showNadiPolarity
                                )
                            }
                        }

                        // 8. 6-Month Macro Karana Tattva Cycle (Chaitra ↔ Sharad with Cycle Switcher & Maas Overlaps)
                        if (vis.showMacroKaranaCycle) {
                            rep.karanaCycle?.let { cycle ->
                                item(key = "karana_cycle") {
                                    UpgradedKaranaCycleCard(
                                        cycle = cycle,
                                        timezone = loc.timezone,
                                        cycleOffset = state.cycleOffset,
                                        onSelectCycleOffset = viewModel::selectCycleOffset
                                    )
                                }
                            }
                        }

                        // 9. Searchable 24-Hour (120-Window) Hora Sub-Tattva Explorer ("कब से कब तक")
                        if (vis.showHoraExplorerTable) {
                            state.dailyHoras?.let { dailyHoras ->
                                item(key = "hora_sub_tattva_explorer") {
                                    SearchableHoraSubTattvaTimelineCard(
                                        dailyHoras = dailyHoras,
                                        timezone = loc.timezone,
                                        selectedElement = state.selectedElementFilter,
                                        onSelectElement = viewModel::selectElementFilter,
                                        dayNightFilter = state.horaDayNightFilter,
                                        onSelectDayNightFilter = viewModel::selectHoraDayNightFilter,
                                        searchQuery = state.searchQuery,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        nowMs = state.nowMs,
                                        showNadiPolarity = vis.showNadiPolarity
                                    )
                                }
                            }
                        }

                        // 10. Reference Table of all 5 Pancha Mahabhutas
                        if (vis.showMahabhutaReference) {
                            item(key = "all_elements") {
                                PanchangaCard {
                                    SectionHeader(strings.elementalGuidanceTitle)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TattvaElement.entries.forEachIndexed { idx, el ->
                                        if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { inspectedElement = el }
                                                .minimumInteractiveComponentSize()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TattvaMandalaCanvas(element = el, sizeDp = 36.dp)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = VedicLocalizer.tattvaName(el, lang),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = if (el == rep.dominantElement) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = if (el == rep.dominantElement) FontWeight.Bold else FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = VedicLocalizer.tattvaQuality(el, lang),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = VedicLocalizer.tattvaName(el, lang),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(56.dp)) }
                    }
                }
            }

            val todayLocal = remember(state.preferences.selectedLocation.timezone) {
                LocalDate.now(com.mahavtaar.panchanga.core.TimeFormatters.safeZoneId(state.preferences.selectedLocation.timezone))
            }
            StickyBackToTodayPill(
                visible = state.selectedDate != todayLocal,
                onBackToToday = viewModel::onSelectToday,
                testTag = "tattva_back_to_today_pill",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )

            if (vis.showMahabhutaReference) {
                inspectedElement?.let { el ->
                    val explorer = VedicSwaraNavtaraCatalog.tattvaExplorerItems.firstOrNull { it.element == el }
                    InspectorBottomSheetFrame(
                        title = VedicLocalizer.tattvaName(el, lang),
                        subtitle = explorer?.let {
                            if (lang == AppLanguage.HINDI) "${it.angulasHi} · ${it.shapeColorHi}" else "${it.angulasEn} · ${it.shapeColorEn}"
                        },
                        icon = Icons.Rounded.Spa,
                        accentColor = tattvaColorFor(el),
                        statusLabel = explorer?.let { "${it.ratioMins} ${strings.minShort}" },
                        sheetTestTag = "tattva_element_inspector_sheet",
                        onDismiss = { inspectedElement = null },
                        heroVisual = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TattvaMandalaCanvas(element = el, sizeDp = 76.dp)
                            }
                        }
                    ) {
                        Text(
                            text = VedicLocalizer.tattvaQuality(el, lang),
                            style = MaterialTheme.typography.titleSmall,
                            color = tattvaColorFor(el),
                            fontWeight = FontWeight.Bold
                        )
                        explorer?.let {
                            Text(
                                text = if (lang == AppLanguage.HINDI) it.guidanceHi else it.guidanceEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            if (showCustomizerSheet && permittedTattvaFeatures.isNotEmpty()) {
                TattvaDisplayCustomizerSheet(
                    lang = lang,
                    isMinimalMode = vis.minimalTimeOnly,
                    permittedFeatures = permittedTattvaFeatures,
                    hiddenFeatures = state.preferences.tattvaHiddenFeatures,
                    onToggleFeature = { feature ->
                        viewModel.toggleLocalTattvaFeature(feature, permittedTattvaFeatures)
                    },
                    onSetTimeOnly = {
                        viewModel.setTattvaMinimalMode(true)
                        showCustomizerSheet = false
                    },
                    onShowAll = {
                        viewModel.showAllPermittedTattvaFeatures()
                        showCustomizerSheet = false
                    },
                    onDismiss = { showCustomizerSheet = false }
                )
            }
        }
    }
}

@Composable
private fun TattvaViewModeBar(
    lang: AppLanguage,
    isMinimal: Boolean,
    activeCount: Int,
    totalPermitted: Int,
    onSelectMinimal: () -> Unit,
    onSelectFullOrCustom: () -> Unit,
    onOpenCustomizer: () -> Unit
) {
    val isHi = lang == AppLanguage.HINDI
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tattva_view_mode_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            SegmentedButton(
                selected = isMinimal,
                onClick = onSelectMinimal,
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.testTag("tattva_mode_time_only_btn")
            ) {
                Text(
                    text = if (isHi) "केवल तत्त्व व समय" else "Tattva & Time",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SegmentedButton(
                selected = !isMinimal,
                onClick = onSelectFullOrCustom,
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("tattva_mode_full_btn")
            ) {
                Text(
                    text = if (isHi) "विस्तृत ($activeCount/$totalPermitted)" else "Detailed ($activeCount/$totalPermitted)",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        FilterChip(
            selected = !isMinimal && activeCount < totalPermitted,
            onClick = onOpenCustomizer,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = if (isHi) "तत्त्व अनुभाग चुनें" else "Customize Tattva sections",
                    modifier = Modifier.size(16.dp)
                )
            },
            label = {
                Text(
                    text = if (isHi) "फ़िल्टर" else "Tune",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            modifier = Modifier.testTag("tattva_customize_sheet_button")
        )
    }
}

@Composable
private fun TattvaDisplayCustomizerSheet(
    lang: AppLanguage,
    isMinimalMode: Boolean,
    permittedFeatures: List<AppFeatureKey>,
    hiddenFeatures: Set<String>,
    onToggleFeature: (AppFeatureKey) -> Unit,
    onSetTimeOnly: () -> Unit,
    onShowAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val isHi = lang == AppLanguage.HINDI
    val activeCount = if (isMinimalMode) 0 else permittedFeatures.count { it.name !in hiddenFeatures }

    InspectorBottomSheetFrame(
        title = if (isHi) "तत्त्व पृष्ठ अनुकूलन" else "Customize Tattva View",
        subtitle = if (isHi) "प्रत्येक जानकारी, नाड़ी और तालिका को अलग से चालू/बंद करें" else "Toggle any individual detail, Nadi badge, or table",
        icon = Icons.Rounded.Tune,
        accentColor = MaterialTheme.colorScheme.primary,
        statusLabel = "$activeCount/${permittedFeatures.size}",
        sheetTestTag = "tattva_customizer_sheet",
        onDismiss = onDismiss,
        footerActions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSetTimeOnly,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("tattva_customizer_time_only")
                ) {
                    Text(
                        text = if (isHi) "केवल तत्त्व व समय" else "Time Only",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onShowAll,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("tattva_customizer_show_all")
                ) {
                    Text(
                        text = if (isHi) "सभी दिखाएँ" else "Show All",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    ) {
        permittedFeatures.forEach { feature ->
            val checked = !isMinimalMode && feature.name !in hiddenFeatures
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (checked) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
                border = BorderStroke(
                    1.dp,
                    if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleFeature(feature) }
                    .minimumInteractiveComponentSize()
                    .testTag("tattva_local_toggle_${feature.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AccessPermissionCatalog.featureTitle(feature, lang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = AccessPermissionCatalog.featureSubtitle(feature, lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = checked,
                        onCheckedChange = { onToggleFeature(feature) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HoraFiveTattvaStripCard(
    hora: HoraPeriod,
    timezone: String,
    activeTattvaStartMs: Long?,
    activeTattvaEndMs: Long?,
    nowMs: Long,
    activeRuler: String?
) {
    val lang = LocalAppLanguage.current
    val strings = LocalStrings.current
    val vedic = LocalVedicColors.current
    val activeSub = hora.tattvas.firstOrNull { it.startTimeMs == activeTattvaStartMs }
    val rem = activeTattvaEndMs?.let { (it - nowMs).coerceAtLeast(0L) } ?: 0L

    PanchangaCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(strings.horaSubTattvaTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = activeRuler?.let { VedicLocalizer.horaRulerName(it, lang) } ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SwaraNadiBadge(nadi = hora.nadi)
                    activeSub?.let { TattvaBadge(element = it.element) }
                }
            }
            if (rem > 0L) {
                StatusPill(
                    label = "${VedicGlossaryCatalog.formatLiveCountdown(rem, lang)} ${strings.leftSuffix}",
                    textColor = vedic.auspicious,
                    containerColor = vedic.auspiciousContainer
                )
            }
        }
        activeSub?.let { sub ->
            Spacer(modifier = Modifier.height(8.dp))
            LiveNowClarityBanner(
                sentence = VedicLocalizer.simpleNowTattvaSentence(sub.element, lang),
                accentColor = tattvaColorFor(sub.element),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            hora.tattvas.forEach { t ->
                val isCur = t.startTimeMs == activeTattvaStartMs
                Box(
                    modifier = Modifier
                        .weight(t.durationMins.coerceAtLeast(4).toFloat())
                        .fillMaxHeight()
                        .background(tattvaColorFor(t.element).copy(alpha = if (isCur) 1f else 0.35f))
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        hora.tattvas.forEachIndexed { idx, t ->
            val isCur = t.startTimeMs == activeTattvaStartMs
            if (idx > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                        color = if (isCur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EventStartEndDateTimeView(
                    startText = VedicLocalizer.formatDateTimeWithSeconds(t.startTimeMs, timezone, lang),
                    endText = VedicLocalizer.formatDateTimeWithSeconds(t.endTimeMs, timezone, lang),
                    accentColor = if (isCur) vedic.auspicious else MaterialTheme.colorScheme.primary,
                    compact = true
                )
            }
        }
    }
}
