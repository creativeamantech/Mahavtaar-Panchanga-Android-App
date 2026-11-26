package com.mahavtaar.panchanga.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.session.PanchangaSessionHolder
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.MonthlyCalendarDay
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.domain.usecase.GetMonthlyCalendarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

sealed interface CalendarContentState {
    data object Loading : CalendarContentState
    data class Success(val days: List<MonthlyCalendarDay>) : CalendarContentState
    data class Error(val message: String) : CalendarContentState
}

data class HinduCalendarUiState(
    val preferences: UserPreferences = UserPreferences(),
    val yearMonth: YearMonth = YearMonth.now(),
    val selectedDay: MonthlyCalendarDay? = null,
    val content: CalendarContentState = CalendarContentState.Loading
)

@HiltViewModel
class HinduCalendarViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val sessionHolder: PanchangaSessionHolder,
    private val getMonthlyCalendar: GetMonthlyCalendarUseCase
) : ViewModel() {

    private val displayedMonth = MutableStateFlow(YearMonth.now())
    private val _uiState = MutableStateFlow(HinduCalendarUiState())
    val uiState: StateFlow<HinduCalendarUiState> = _uiState.asStateFlow()
    private var lastComputeKey: String? = null

    init {
        viewModelScope.launch {
            combine(preferencesRepository.preferences, displayedMonth) { prefs, ym -> prefs to ym }
                .collect { (prefs, ym) ->
                    _uiState.update { it.copy(preferences = prefs, yearMonth = ym) }
                    val key = "${prefs.selectedLocation.name}|${prefs.selectedLocation.latitude}|${prefs.selectedLocation.longitude}|${prefs.selectedLocation.timezone}|${prefs.monthSystem.apiKey}|${prefs.ayanamsha.apiKey}|$ym"
                    if (key != lastComputeKey || _uiState.value.content !is CalendarContentState.Success) {
                        lastComputeKey = key
                        loadMonth(prefs, ym)
                    }
                }
        }
    }

    private suspend fun loadMonth(prefs: UserPreferences, ym: YearMonth) {
        _uiState.update { it.copy(content = CalendarContentState.Loading) }
        runCatching {
            getMonthlyCalendar(
                location = prefs.selectedLocation,
                year = ym.year,
                month = ym.monthValue,
                monthSystem = prefs.monthSystem,
                ayanamsha = prefs.ayanamsha
            )
        }.onSuccess { days ->
            val currentIso = sessionHolder.selectedDate.value.toString()
            val selected = days.firstOrNull { it.dateIso == currentIso } ?: days.firstOrNull()
            _uiState.update {
                it.copy(
                    selectedDay = selected,
                    content = CalendarContentState.Success(days)
                )
            }
        }.onFailure { err ->
            _uiState.update { it.copy(content = CalendarContentState.Error(err.message ?: "Failed to load month")) }
        }
    }

    fun previousMonth() {
        displayedMonth.value = displayedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        displayedMonth.value = displayedMonth.value.plusMonths(1)
    }

    fun selectMonthSystem(system: MonthSystem) {
        viewModelScope.launch { preferencesRepository.setMonthSystem(system) }
    }

    fun selectDay(day: MonthlyCalendarDay) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun openDayInPanchanga(day: MonthlyCalendarDay) {
        sessionHolder.selectDate(LocalDate.of(day.year, day.month, day.dayOfMonth))
    }

    fun jumpToToday() {
        val tz = _uiState.value.preferences.selectedLocation.timezone
        val today = LocalDate.now(TimeFormatters.safeZoneId(tz))
        sessionHolder.selectToday(tz)
        displayedMonth.value = YearMonth.from(today)
        val successDays = (_uiState.value.content as? CalendarContentState.Success)?.days.orEmpty()
        successDays.firstOrNull { it.dateIso == today.toString() }?.let { todayItem ->
            _uiState.update { it.copy(selectedDay = todayItem) }
        }
    }

    fun toggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }

    fun retry() {
        val s = _uiState.value
        lastComputeKey = null
        viewModelScope.launch { loadMonth(s.preferences, s.yearMonth) }
    }
}
