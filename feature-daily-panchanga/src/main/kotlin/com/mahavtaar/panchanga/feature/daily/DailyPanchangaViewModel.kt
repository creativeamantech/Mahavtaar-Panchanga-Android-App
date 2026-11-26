package com.mahavtaar.panchanga.feature.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.session.PanchangaSessionHolder
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.domain.usecase.GetDailyPanchangaUseCase
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

sealed interface DailyContentState {
    data object Loading : DailyContentState
    data class Success(val snapshot: PanchangaSnapshot) : DailyContentState
    data class Error(val message: String) : DailyContentState
}

data class DailyPanchangaUiState(
    val preferences: UserPreferences = UserPreferences(),
    val selectedDate: LocalDate = LocalDate.now(),
    val nowMs: Long = System.currentTimeMillis(),
    val liveClockText: String = "",
    val selectedSubTab: Int = 0, // 0 = 5 Angas, 1 = Muhurtas & Choghadiya
    val content: DailyContentState = DailyContentState.Loading
)

@HiltViewModel
class DailyPanchangaViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val sessionHolder: PanchangaSessionHolder,
    private val getDailyPanchanga: GetDailyPanchangaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyPanchangaUiState())
    val uiState: StateFlow<DailyPanchangaUiState> = _uiState.asStateFlow()
    private var lastComputeKey: String? = null

    init {
        viewModelScope.launch {
            combine(preferencesRepository.preferences, sessionHolder.selectedDate) { p, d -> p to d }
                .collect { (prefs, date) ->
                    _uiState.update {
                        it.copy(
                            preferences = prefs,
                            selectedDate = date,
                            liveClockText = formatClock(System.currentTimeMillis(), prefs)
                        )
                    }
                    val key = "${prefs.selectedLocation.name}|${prefs.selectedLocation.latitude}|${prefs.selectedLocation.longitude}|${prefs.selectedLocation.timezone}|${prefs.monthSystem.apiKey}|${prefs.ayanamsha.apiKey}|$date"
                    if (key != lastComputeKey || _uiState.value.content !is DailyContentState.Success) {
                        lastComputeKey = key
                        loadPanchanga(prefs, date)
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
                _uiState.update { it.copy(nowMs = now, liveClockText = formatClock(now, prefs)) }
            }
        }
    }

    private suspend fun loadPanchanga(prefs: UserPreferences, date: LocalDate) {
        _uiState.update { it.copy(content = DailyContentState.Loading) }
        runCatching {
            getDailyPanchanga(
                location = prefs.selectedLocation,
                date = date,
                monthSystem = prefs.monthSystem,
                ayanamsha = prefs.ayanamsha,
                includeExtendedTransits = true
            )
        }.onSuccess { snap ->
            _uiState.update { it.copy(content = DailyContentState.Success(snap)) }
        }.onFailure { err ->
            _uiState.update { it.copy(content = DailyContentState.Error(err.message ?: "Unable to compute Panchanga")) }
        }
    }

    private fun formatClock(nowMs: Long, prefs: UserPreferences): String {
        val tz = prefs.selectedLocation.timezone
        return if (prefs.use24HourFormat) TimeFormatters.format24HourHMS(nowMs, tz)
        else TimeFormatters.format12HourHMS(nowMs, tz)
    }

    fun selectSubTab(index: Int) = _uiState.update { it.copy(selectedSubTab = index) }
    fun onPreviousDay() = sessionHolder.shiftDays(-1)
    fun onNextDay() = sessionHolder.shiftDays(1)
    fun onSelectToday() = sessionHolder.selectToday(_uiState.value.preferences.selectedLocation.timezone)
    fun onSelectDate(date: LocalDate) = sessionHolder.selectDate(date)
    fun onToggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }
    fun onRetry() {
        val s = _uiState.value
        lastComputeKey = null
        viewModelScope.launch { loadPanchanga(s.preferences, s.selectedDate) }
    }
}
