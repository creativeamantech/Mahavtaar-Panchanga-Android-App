package com.mahavtaar.panchanga.feature.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.GocharReport
import com.mahavtaar.panchanga.domain.model.GunaMilanReport
import com.mahavtaar.panchanga.domain.model.KundliBirthInput
import com.mahavtaar.panchanga.domain.model.KundliJsonExporter
import com.mahavtaar.panchanga.domain.model.KundliReport
import com.mahavtaar.panchanga.domain.model.SavedKundliChart
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.LocationRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

enum class KundliTab {
    CHARTS,
    PLANETS,
    DASHA,
    VARGAS,
    SHADBALA,
    ASHTAKAVARGA,
    KP,
    JAIMINI,
    YOGAS,
    GOCHAR,
    AUDIT_EXPORT,
    GUNA_MILAN
}

data class KundliUiState(
    val preferences: UserPreferences = UserPreferences(),
    val input: KundliBirthInput = KundliBirthInput(),
    val savedCharts: List<SavedKundliChart> = emptyList(),
    val savedLocations: List<CityLocation> = emptyList(),
    val citySearchQuery: String = "",
    val cityResults: List<CityLocation> = emptyList(),
    val chartStyle: KundliChartStyle = KundliChartStyle.NORTH_INDIAN,
    val selectedTab: KundliTab = KundliTab.CHARTS,
    val selectedVargaCode: String = "D9",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val report: KundliReport? = null,
    val selectedTransitDate: LocalDate = LocalDate.now(),
    val customGocharReport: GocharReport? = null,
    // Guna Milan inputs
    val boyName: String = "Var",
    val boyNakshatra: Int = 1,
    val boyPada: Int = 1,
    val boyRasi: Int = 0,
    val boyManglik: Boolean = false,
    val girlName: String = "Kanya",
    val girlNakshatra: Int = 4,
    val girlPada: Int = 2,
    val girlRasi: Int = 1,
    val girlManglik: Boolean = false,
    val gunaMilanReport: GunaMilanReport? = null
)

@HiltViewModel
class KundliViewModel @Inject constructor(
    private val astrologyRepository: AstrologyRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KundliUiState())
    val uiState: StateFlow<KundliUiState> = _uiState.asStateFlow()
    private var initializedLocation = false

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.update { cur ->
                    val nextInput = if (!initializedLocation) {
                        initializedLocation = true
                        cur.input.copy(location = prefs.selectedLocation, ayanamsha = prefs.ayanamsha)
                    } else {
                        cur.input.copy(ayanamsha = prefs.ayanamsha)
                    }
                    cur.copy(preferences = prefs, input = nextInput)
                }
                if (_uiState.value.report == null) {
                    computeKundli()
                    recomputeGunaMilan()
                }
            }
        }
        viewModelScope.launch {
            astrologyRepository.observeSavedKundliCharts().collect { charts ->
                _uiState.update { it.copy(savedCharts = charts) }
            }
        }
        viewModelScope.launch {
            locationRepository.observeSavedLocations().collect { saved ->
                val popular = runCatching { locationRepository.getPopularCities() }.getOrDefault(emptyList())
                _uiState.update { cur ->
                    val merged = if (cur.citySearchQuery.isBlank()) {
                        mergeDeduplicatedCities(saved, popular)
                    } else {
                        cur.cityResults
                    }
                    cur.copy(savedLocations = saved, cityResults = merged)
                }
            }
        }
        viewModelScope.launch {
            val popular = runCatching { locationRepository.getPopularCities() }.getOrDefault(emptyList())
            _uiState.update { cur ->
                cur.copy(cityResults = mergeDeduplicatedCities(cur.savedLocations, popular))
            }
        }
    }

    private fun mergeDeduplicatedCities(primary: List<CityLocation>, secondary: List<CityLocation>): List<CityLocation> {
        val seen = HashSet<String>()
        val result = ArrayList<CityLocation>(primary.size + secondary.size)
        for (c in primary + secondary) {
            val key = c.name.trim().lowercase()
            if (seen.add(key)) {
                result.add(c)
            }
        }
        return result
    }

    fun setInitialTab(tab: KundliTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectTab(tab: KundliTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setChartStyle(style: KundliChartStyle) {
        _uiState.update { it.copy(chartStyle = style) }
    }

    fun selectVarga(code: String) {
        _uiState.update { it.copy(selectedVargaCode = code) }
    }

    fun updateBirthInput(transform: (KundliBirthInput) -> KundliBirthInput) {
        _uiState.update { it.copy(input = transform(it.input)) }
    }

    fun searchCities(query: String) {
        _uiState.update { it.copy(citySearchQuery = query) }
        viewModelScope.launch {
            val q = query.trim()
            val savedMatches = if (q.isBlank()) {
                _uiState.value.savedLocations
            } else {
                _uiState.value.savedLocations.filter {
                    it.name.contains(q, ignoreCase = true) || it.country.contains(q, ignoreCase = true)
                }
            }
            val catalogMatches = runCatching {
                if (q.isBlank()) locationRepository.getPopularCities()
                else locationRepository.searchCities(q, limit = 30)
            }.getOrDefault(emptyList())
            _uiState.update { it.copy(cityResults = mergeDeduplicatedCities(savedMatches, catalogMatches)) }
        }
    }

    fun selectBirthCity(city: CityLocation) {
        _uiState.update { it.copy(input = it.input.copy(location = city)) }
        viewModelScope.launch {
            runCatching { locationRepository.saveLocation(city) }
        }
        computeKundli()
    }

    fun rectifyMinutes(deltaMinutes: Int) {
        val cur = _uiState.value.input
        val dt = LocalDateTime.of(
            cur.year.coerceIn(1900, 2100),
            cur.month.coerceIn(1, 12),
            cur.day.coerceIn(1, 28).coerceAtMost(LocalDate.of(cur.year.coerceIn(1900, 2100), cur.month.coerceIn(1, 12), 1).lengthOfMonth()),
            cur.hour.coerceIn(0, 23),
            cur.minute.coerceIn(0, 59)
        ).plusMinutes(deltaMinutes.toLong())
        _uiState.update {
            it.copy(
                input = cur.copy(
                    year = dt.year,
                    month = dt.monthValue,
                    day = dt.dayOfMonth,
                    hour = dt.hour,
                    minute = dt.minute
                )
            )
        }
        computeKundli()
    }

    fun setBirthNow() {
        val cur = _uiState.value.input
        val zone = TimeFormatters.safeZoneId(cur.location.timezone)
        val zdt = Instant.now().atZone(zone)
        _uiState.update {
            it.copy(
                input = cur.copy(
                    year = zdt.year,
                    month = zdt.monthValue,
                    day = zdt.dayOfMonth,
                    hour = zdt.hour,
                    minute = zdt.minute
                )
            )
        }
        computeKundli()
    }

    fun saveCurrentProfile() {
        val currentState = _uiState.value
        val input = currentState.input
        viewModelScope.launch {
            runCatching {
                val rep = currentState.report ?: astrologyRepository.computeKundli(input)
                val nowMs = System.currentTimeMillis()
                val existing = currentState.savedCharts.firstOrNull {
                    it.input.name.equals(input.name, ignoreCase = true) &&
                        it.input.year == input.year &&
                        it.input.month == input.month &&
                        it.input.day == input.day &&
                        it.input.hour == input.hour &&
                        it.input.minute == input.minute
                }
                val chart = SavedKundliChart(
                    id = existing?.id ?: 0L,
                    input = input,
                    lagnaSignIndex = rep.lagnaSignIndex,
                    moonSignIndex = rep.moonSignIndex,
                    moonNakshatraNumber = rep.moonNakshatraNumber,
                    createdAtMs = existing?.createdAtMs ?: nowMs,
                    updatedAtMs = nowMs
                )
                astrologyRepository.saveKundliChart(chart)
            }.onSuccess {
                _uiState.update { it.copy(statusMessage = "Saved chart: ${input.name}") }
            }.onFailure { err ->
                _uiState.update { it.copy(statusMessage = err.message ?: "Save failed") }
            }
        }
    }

    fun loadSavedChart(chart: SavedKundliChart) {
        _uiState.update { it.copy(input = chart.input) }
        computeKundli()
    }

    fun deleteSavedChart(chartId: Long) {
        viewModelScope.launch {
            astrologyRepository.deleteKundliChart(chartId)
        }
    }

    fun importKundliJson(raw: String) {
        KundliJsonExporter.decodeBirthInput(raw)
            .onSuccess { importedInput ->
                _uiState.update {
                    it.copy(
                        input = importedInput,
                        statusMessage = "Imported chart: ${importedInput.name}"
                    )
                }
                computeKundli()
            }
            .onFailure { err ->
                _uiState.update {
                    it.copy(statusMessage = "Invalid JSON: ${err.message.orEmpty()}")
                }
            }
    }

    fun setStatusMessage(msg: String) = _uiState.update { it.copy(statusMessage = msg) }
    fun clearStatusMessage() = _uiState.update { it.copy(statusMessage = null) }

    fun computeKundli() {
        val currentInput = _uiState.value.input
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                astrologyRepository.computeKundli(currentInput)
            }.onSuccess { rep ->
                val gochar = astrologyRepository.computeGocharForDate(
                    natalMoonSignIndex = rep.moonSignIndex,
                    natalLagnaSignIndex = rep.lagnaSignIndex,
                    transitDate = _uiState.value.selectedTransitDate,
                    ayanamsha = currentInput.ayanamsha
                )
                _uiState.update { it.copy(isLoading = false, report = rep, customGocharReport = gochar) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Kundli calculation failed") }
            }
        }
    }

    fun updateGocharDate(date: LocalDate) {
        val rep = _uiState.value.report ?: return
        val ayanamsha = _uiState.value.input.ayanamsha
        val gochar = astrologyRepository.computeGocharForDate(
            natalMoonSignIndex = rep.moonSignIndex,
            natalLagnaSignIndex = rep.lagnaSignIndex,
            transitDate = date,
            ayanamsha = ayanamsha
        )
        _uiState.update { it.copy(selectedTransitDate = date, customGocharReport = gochar) }
    }

    fun resetGocharToday() {
        updateGocharDate(LocalDate.now())
    }

    fun updateGunaMilan(
        boyName: String = _uiState.value.boyName,
        boyNakshatra: Int = _uiState.value.boyNakshatra,
        boyPada: Int = _uiState.value.boyPada,
        boyRasi: Int = _uiState.value.boyRasi,
        boyManglik: Boolean = _uiState.value.boyManglik,
        girlName: String = _uiState.value.girlName,
        girlNakshatra: Int = _uiState.value.girlNakshatra,
        girlPada: Int = _uiState.value.girlPada,
        girlRasi: Int = _uiState.value.girlRasi,
        girlManglik: Boolean = _uiState.value.girlManglik
    ) {
        _uiState.update {
            it.copy(
                boyName = boyName,
                boyNakshatra = boyNakshatra,
                boyPada = boyPada,
                boyRasi = boyRasi,
                boyManglik = boyManglik,
                girlName = girlName,
                girlNakshatra = girlNakshatra,
                girlPada = girlPada,
                girlRasi = girlRasi,
                girlManglik = girlManglik
            )
        }
        recomputeGunaMilan()
    }

    private fun recomputeGunaMilan() {
        val s = _uiState.value
        val result = astrologyRepository.computeGunaMilan(
            boyName = s.boyName,
            boyNakshatra = s.boyNakshatra,
            boyPada = s.boyPada,
            boyRasi = s.boyRasi,
            boyManglik = s.boyManglik,
            girlName = s.girlName,
            girlNakshatra = s.girlNakshatra,
            girlPada = s.girlPada,
            girlRasi = s.girlRasi,
            girlManglik = s.girlManglik
        )
        _uiState.update { it.copy(gunaMilanReport = result) }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val current = preferencesRepository.preferences.first().language
            preferencesRepository.setLanguage(current.toggled())
        }
    }
}
