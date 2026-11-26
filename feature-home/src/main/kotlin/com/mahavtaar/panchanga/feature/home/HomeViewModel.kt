package com.mahavtaar.panchanga.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.session.PanchangaSessionHolder
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.CloudUser
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SwaraYogaState
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.notifications.AlertEnginePort
import com.mahavtaar.panchanga.domain.repository.CloudAccountRepository
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
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

sealed interface HomeContentState {
    data object Loading : HomeContentState
    data class Success(
        val snapshot: PanchangaSnapshot,
        val dailyHoras: DailyHoras,
        val liveSwara: SwaraYogaState
    ) : HomeContentState
    data class Error(val message: String) : HomeContentState
}

data class HomeUiState(
    val preferences: UserPreferences = UserPreferences(),
    val audioSettings: AlertAudioSettings = AlertAudioSettings(),
    val cloudUser: CloudUser? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val nowMs: Long = System.currentTimeMillis(),
    val liveClockText: String = "",
    val content: HomeContentState = HomeContentState.Loading
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val notificationStudioRepository: NotificationStudioRepository,
    private val alertEnginePort: AlertEnginePort,
    private val cloudAccountRepository: CloudAccountRepository,
    private val sessionHolder: PanchangaSessionHolder,
    private val getDailyPanchanga: GetDailyPanchangaUseCase,
    private val getLiveHora: GetLiveHoraUseCase,
    private val getLiveSwaraYoga: GetLiveSwaraYogaUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var latestSnapshot: PanchangaSnapshot? = null
    private var lastComputeKey: String? = null

    init {
        observeSelectionChanges()
        observeCloudUser()
        observeAudioSettings()
        startLiveClock()
    }

    private fun observeAudioSettings() {
        viewModelScope.launch {
            notificationStudioRepository.observeAudioSettings().collect { audio ->
                _uiState.update { it.copy(audioSettings = audio) }
            }
        }
    }

    private fun observeCloudUser() {
        viewModelScope.launch {
            cloudAccountRepository.currentUser.collect { user ->
                _uiState.update { it.copy(cloudUser = user) }
            }
        }
    }

    private fun observeSelectionChanges() {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferences,
                sessionHolder.selectedDate
            ) { prefs, date -> prefs to date }
                .collect { (prefs, date) ->
                    _uiState.update {
                        it.copy(
                            preferences = prefs,
                            selectedDate = date,
                            liveClockText = formatCityClock(System.currentTimeMillis(), prefs)
                        )
                    }
                    val key = "${prefs.selectedLocation.name}|${prefs.selectedLocation.latitude}|${prefs.selectedLocation.longitude}|${prefs.selectedLocation.timezone}|${prefs.monthSystem.apiKey}|${prefs.ayanamsha.apiKey}|$date"
                    if (key != lastComputeKey || latestSnapshot == null) {
                        lastComputeKey = key
                        loadSnapshot(prefs, date)
                    }
                }
        }
    }

    private fun startLiveClock() {
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
                    val updatedContent = if (snap != null && state.content is HomeContentState.Success) {
                        HomeContentState.Success(
                            snapshot = snap,
                            dailyHoras = getLiveHora(snap, now),
                            liveSwara = getLiveSwaraYoga(snap, now)
                        )
                    } else {
                        state.content
                    }
                    state.copy(
                        nowMs = now,
                        liveClockText = formatCityClock(now, prefs),
                        content = updatedContent
                    )
                }
            }
        }
    }

    private suspend fun loadSnapshot(prefs: UserPreferences, date: LocalDate) {
        _uiState.update { it.copy(content = HomeContentState.Loading) }
        runCatching {
            getDailyPanchanga(
                location = prefs.selectedLocation,
                date = date,
                monthSystem = prefs.monthSystem,
                ayanamsha = prefs.ayanamsha,
                includeExtendedTransits = true
            )
        }.onSuccess { snap ->
            latestSnapshot = snap
            val now = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    nowMs = now,
                    content = HomeContentState.Success(
                        snapshot = snap,
                        dailyHoras = getLiveHora(snap, now),
                        liveSwara = getLiveSwaraYoga(snap, now)
                    )
                )
            }
        }.onFailure { err ->
            _uiState.update {
                it.copy(content = HomeContentState.Error(err.message ?: "Failed to compute Panchanga"))
            }
        }
    }

    private fun formatCityClock(nowMs: Long, prefs: UserPreferences): String {
        val tz = prefs.selectedLocation.timezone
        return if (prefs.use24HourFormat) {
            TimeFormatters.format24HourHMS(nowMs, tz)
        } else {
            TimeFormatters.format12HourHMS(nowMs, tz)
        }
    }

    fun onPreviousDay() = sessionHolder.shiftDays(-1)
    fun onNextDay() = sessionHolder.shiftDays(1)
    fun onSelectToday() = sessionHolder.selectToday(_uiState.value.preferences.selectedLocation.timezone)
    fun onSelectDate(date: LocalDate) = sessionHolder.selectDate(date)
    fun onToggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }
    fun updateAudioSettings(updated: AlertAudioSettings) {
        viewModelScope.launch {
            notificationStudioRepository.updateAudioSettings(updated)
            runCatching { alertEnginePort.rescheduleAllAlerts() }
            runCatching { alertEnginePort.syncRunningNowNotification() }
        }
    }
    fun setPinnedHomeModules(moduleKeys: List<String>) {
        viewModelScope.launch {
            preferencesRepository.setPinnedHomeModules(moduleKeys)
        }
    }
    fun onRetry() {
        val s = _uiState.value
        lastComputeKey = null
        viewModelScope.launch { loadSnapshot(s.preferences, s.selectedDate) }
    }
}
