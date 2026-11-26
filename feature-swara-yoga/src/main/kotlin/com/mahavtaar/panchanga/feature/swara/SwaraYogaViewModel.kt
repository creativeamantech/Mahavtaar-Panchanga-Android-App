package com.mahavtaar.panchanga.feature.swara

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.data.session.PanchangaSessionHolder
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.NakshatraSwaraAlignmentResult
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SwaraYogaState
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.domain.usecase.GetDailyPanchangaUseCase
import com.mahavtaar.panchanga.domain.usecase.GetLiveHoraUseCase
import com.mahavtaar.panchanga.domain.usecase.GetLiveSwaraYogaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class SwaraCycleFilter { ALL, SHUKLA, KRISHNA }

sealed interface SwaraContentState {
    data object Loading : SwaraContentState
    data class Success(
        val snapshot: PanchangaSnapshot,
        val swaraState: SwaraYogaState,
        val dailyHoras: DailyHoras,
        val liveAlignment: NakshatraSwaraAlignmentResult,
        val nowMs: Long = System.currentTimeMillis()
    ) : SwaraContentState
    data class Error(val message: String) : SwaraContentState
}

data class SwaraSimulatorState(
    val isShukla: Boolean = true,
    val tithiNumber: Int = 1,
    val nakshatraNumber: Int = 1,
    val pada: Int = 1,
    val result: NakshatraSwaraAlignmentResult? = null
)

data class SwaraUiControlsState(
    val selectedTab: Int = 0,
    val cycleFilter: SwaraCycleFilter = SwaraCycleFilter.ALL,
    val simulator: SwaraSimulatorState = SwaraSimulatorState()
)

@HiltViewModel
class SwaraYogaViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val sessionHolder: PanchangaSessionHolder,
    private val getDailyPanchanga: GetDailyPanchangaUseCase,
    private val getLiveSwara: GetLiveSwaraYogaUseCase,
    private val getLiveHora: GetLiveHoraUseCase,
    private val astrologyRepository: AstrologyRepository
) : ViewModel() {

    private val _content = MutableStateFlow<SwaraContentState>(SwaraContentState.Loading)
    val content: StateFlow<SwaraContentState> = _content.asStateFlow()

    private val _uiControls = MutableStateFlow(SwaraUiControlsState())
    val uiControls: StateFlow<SwaraUiControlsState> = _uiControls.asStateFlow()

    private var latestSnapshot: PanchangaSnapshot? = null
    private var latestPreferences: UserPreferences = UserPreferences()
    private var lastComputeKey: String? = null

    init {
        evaluateSimulator(isShukla = true, tithiNumber = 1, nakshatraNumber = 1, pada = 1)
        viewModelScope.launch {
            combine(preferencesRepository.preferences, sessionHolder.selectedDate) { p, d -> p to d }
                .collect { (prefs, date) ->
                    latestPreferences = prefs
                    val key = "${prefs.selectedLocation.name}|${prefs.selectedLocation.latitude}|${prefs.selectedLocation.longitude}|${prefs.selectedLocation.timezone}|${prefs.monthSystem.apiKey}|${prefs.ayanamsha.apiKey}|$date"
                    if (key != lastComputeKey || latestSnapshot == null) {
                        lastComputeKey = key
                        load(prefs, date)
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
                val snap = latestSnapshot ?: return@collect
                emitSuccess(snap, now)
            }
        }
    }

    private suspend fun load(prefs: UserPreferences, date: LocalDate) {
        _content.value = SwaraContentState.Loading
        runCatching {
            getDailyPanchanga(prefs.selectedLocation, date, prefs.monthSystem, prefs.ayanamsha, includeExtendedTransits = false)
        }.onSuccess { snap ->
            latestSnapshot = snap
            val now = System.currentTimeMillis()
            emitSuccess(snap, now)
            val moon = snap.planets.firstOrNull { it.id.equals("moon", true) }
            val tNum = snap.tithi.firstOrNull()?.number ?: 1
            val isShukla = !snap.paksha.contains("K", ignoreCase = true) || snap.paksha.contains("Śukla", true) || snap.paksha.contains("Shukla", true)
            evaluateSimulator(
                isShukla = isShukla,
                tithiNumber = ((tNum - 1) % 15) + 1,
                nakshatraNumber = moon?.nakshatraNumber ?: (snap.nakshatra.firstOrNull()?.number ?: 1),
                pada = moon?.pada ?: 1
            )
        }.onFailure { err ->
            _content.value = SwaraContentState.Error(err.message ?: "Unable to compute Swara Yoga")
        }
    }

    private fun emitSuccess(snap: PanchangaSnapshot, nowMs: Long) {
        val swara = getLiveSwara(snap, nowMs)
        val horas = getLiveHora(snap, nowMs)
        val moon = snap.planets.firstOrNull { it.id.equals("moon", true) }
        val activeTithiNum = snap.tithi.firstOrNull { nowMs in (it.startTimeMs ?: Long.MIN_VALUE)..(it.endTimeMs ?: Long.MAX_VALUE) }?.number
            ?: snap.tithi.firstOrNull()?.number ?: 1
        val activeNakNum = snap.nakshatra.firstOrNull { nowMs in (it.startTimeMs ?: Long.MIN_VALUE)..(it.endTimeMs ?: Long.MAX_VALUE) }?.number
            ?: moon?.nakshatraNumber ?: 1
        val alignment = astrologyRepository.evaluateNakshatraSwaraAlignment(
            paksha = snap.paksha,
            tithiNumber = ((activeTithiNum - 1) % 15) + 1,
            nakshatraNumber = activeNakNum.coerceIn(1, 27),
            pada = moon?.pada ?: 1
        )
        _content.value = SwaraContentState.Success(snap, swara, horas, alignment, nowMs)
    }

    fun selectTab(tab: Int) = _uiControls.update { it.copy(selectedTab = tab.coerceIn(0, 3)) }

    fun selectCycleFilter(filter: SwaraCycleFilter) = _uiControls.update { it.copy(cycleFilter = filter) }

    fun evaluateSimulator(
        isShukla: Boolean = _uiControls.value.simulator.isShukla,
        tithiNumber: Int = _uiControls.value.simulator.tithiNumber,
        nakshatraNumber: Int = _uiControls.value.simulator.nakshatraNumber,
        pada: Int = _uiControls.value.simulator.pada
    ) {
        val tClamped = tithiNumber.coerceIn(1, 15)
        val nClamped = nakshatraNumber.coerceIn(1, 27)
        val pClamped = pada.coerceIn(1, 4)
        val result = astrologyRepository.evaluateNakshatraSwaraAlignment(
            paksha = if (isShukla) "Shukla" else "Krishna",
            tithiNumber = tClamped,
            nakshatraNumber = nClamped,
            pada = pClamped
        )
        _uiControls.update {
            it.copy(
                simulator = SwaraSimulatorState(
                    isShukla = isShukla,
                    tithiNumber = tClamped,
                    nakshatraNumber = nClamped,
                    pada = pClamped,
                    result = result
                )
            )
        }
    }

    fun toggleLanguage() {
        val next = latestPreferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }

    fun retry() {
        val snap = latestSnapshot
        if (snap != null) {
            emitSuccess(snap, System.currentTimeMillis())
        } else {
            lastComputeKey = null
            viewModelScope.launch { load(latestPreferences, sessionHolder.selectedDate.value) }
        }
    }
}
