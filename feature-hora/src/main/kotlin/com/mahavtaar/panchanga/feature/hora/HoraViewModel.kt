package com.mahavtaar.panchanga.feature.hora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.session.PanchangaSessionHolder
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.domain.usecase.GetDailyPanchangaUseCase
import com.mahavtaar.panchanga.domain.usecase.GetLiveHoraUseCase
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

sealed interface HoraContentState {
    data object Loading : HoraContentState
    data class Success(val snapshot: PanchangaSnapshot, val dailyHoras: DailyHoras) : HoraContentState
    data class Error(val message: String) : HoraContentState
}

enum class HoraFilterMode {
    ALL,
    DAY,
    NIGHT
}

data class HoraUiState(
    val preferences: UserPreferences = UserPreferences(),
    val selectedDate: LocalDate = LocalDate.now(),
    val nowMs: Long = System.currentTimeMillis(),
    val liveClockText: String = "",
    val filterMode: HoraFilterMode = HoraFilterMode.ALL,
    val selectedElementFilter: com.mahavtaar.panchanga.domain.model.TattvaElement? = null,
    val searchQuery: String = "",
    val expandedHoraIndex: Int? = null,
    val content: HoraContentState = HoraContentState.Loading
)

@HiltViewModel
class HoraViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val sessionHolder: PanchangaSessionHolder,
    private val getDailyPanchanga: GetDailyPanchangaUseCase,
    private val getLiveHora: GetLiveHoraUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HoraUiState())
    val uiState: StateFlow<HoraUiState> = _uiState.asStateFlow()
    private var latestSnapshot: PanchangaSnapshot? = null
    private var lastComputeKey: String? = null

    init {
        viewModelScope.launch {
            combine(preferencesRepository.preferences, sessionHolder.selectedDate) { p, d -> p to d }
                .collect { (prefs, date) ->
                    _uiState.update { it.copy(preferences = prefs, selectedDate = date) }
                    val key = "${prefs.selectedLocation.name}|${prefs.selectedLocation.latitude}|${prefs.selectedLocation.longitude}|${prefs.selectedLocation.timezone}|${prefs.monthSystem.apiKey}|${prefs.ayanamsha.apiKey}|$date"
                    if (key != lastComputeKey || latestSnapshot == null) {
                        lastComputeKey = key
                        loadHoras(prefs, date)
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
                val prefs = _uiState.value.preferences
                val snap = latestSnapshot
                _uiState.update { state ->
                    val updated = if (snap != null && state.content is HoraContentState.Success) {
                        HoraContentState.Success(snap, getLiveHora(snap, now))
                    } else state.content
                    state.copy(
                        nowMs = now,
                        liveClockText = if (prefs.use24HourFormat) TimeFormatters.format24HourHMS(now, prefs.selectedLocation.timezone)
                        else TimeFormatters.format12HourHMS(now, prefs.selectedLocation.timezone),
                        content = updated
                    )
                }
            }
        }
    }

    private suspend fun loadHoras(prefs: UserPreferences, date: LocalDate) {
        _uiState.update { it.copy(content = HoraContentState.Loading) }
        runCatching {
            getDailyPanchanga(prefs.selectedLocation, date, prefs.monthSystem, prefs.ayanamsha, includeExtendedTransits = false)
        }.onSuccess { snap ->
            latestSnapshot = snap
            val now = System.currentTimeMillis()
            val horas = getLiveHora(snap, now)
            _uiState.update {
                it.copy(
                    nowMs = now,
                    expandedHoraIndex = horas.activeHora?.index,
                    content = HoraContentState.Success(snap, horas)
                )
            }
        }.onFailure { err ->
            _uiState.update { it.copy(content = HoraContentState.Error(err.message ?: "Unable to compute Hora table")) }
        }
    }

    fun selectFilterMode(mode: HoraFilterMode) = _uiState.update { it.copy(filterMode = mode) }
    fun selectElementFilter(element: com.mahavtaar.panchanga.domain.model.TattvaElement?) = _uiState.update {
        it.copy(selectedElementFilter = element)
    }
    fun updateSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun toggleExpand(index: Int) = _uiState.update {
        it.copy(expandedHoraIndex = if (it.expandedHoraIndex == index) null else index)
    }
    fun onPreviousDay() = sessionHolder.shiftDays(-1)
    fun onNextDay() = sessionHolder.shiftDays(1)
    fun onSelectToday() = sessionHolder.selectToday(_uiState.value.preferences.selectedLocation.timezone)
    fun onSelectDate(date: LocalDate) = sessionHolder.selectDate(date)
    fun onToggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }
    fun retry() {
        val s = _uiState.value
        lastComputeKey = null
        viewModelScope.launch { loadHoras(s.preferences, s.selectedDate) }
    }
}
