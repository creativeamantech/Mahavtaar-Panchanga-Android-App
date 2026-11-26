package com.mahavtaar.panchanga.feature.swara

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.data.backup.GoogleDriveBackupManager
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CloudUser
import com.mahavtaar.panchanga.domain.model.GoogleDriveBackupStatus
import com.mahavtaar.panchanga.domain.model.NavtaraDailyFeedback
import com.mahavtaar.panchanga.domain.model.NavtaraDayCategory
import com.mahavtaar.panchanga.domain.model.NavtaraFeedbackRating
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodReport
import com.mahavtaar.panchanga.domain.model.NavtaraProfile
import com.mahavtaar.panchanga.domain.model.NavtaraResult
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SavedNavtaraReport
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.CloudAccountRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.domain.usecase.GetDailyPanchangaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class NavtaraUiState(
    val preferences: UserPreferences = UserPreferences(),
    val cloudUser: CloudUser? = null,
    val driveBackupStatus: GoogleDriveBackupStatus = GoogleDriveBackupStatus(),
    val plannerTab: Int = 0,
    val profiles: List<NavtaraProfile> = emptyList(),
    val activeProfileId: Long? = null,
    val nativeName: String = "",
    val birthPada: Int = 1,
    val feedbacksByDate: Map<String, NavtaraDailyFeedback> = emptyMap(),
    val savedReports: List<SavedNavtaraReport> = emptyList(),
    val inspectDate: LocalDate = LocalDate.now(),
    val matrixFilter: NavtaraDayCategory? = null,
    val journalFilter: NavtaraFeedbackRating? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val transitNakshatraNumber: Int = 1,
    val snapshot: PanchangaSnapshot? = null,
    val todayResult: NavtaraResult? = null,
    val matrix: List<NavtaraResult> = emptyList(),
    val isCustomRange: Boolean = false,
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val rangeStartDate: LocalDate = YearMonth.now().atDay(1),
    val rangeEndDate: LocalDate = YearMonth.now().atEndOfMonth(),
    val isGeneratingPeriod: Boolean = false,
    val rawPeriodReport: NavtaraPeriodReport? = null,
    val periodReport: NavtaraPeriodReport? = null,
    val statusMessage: String? = null
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class NavtaraViewModel @Inject constructor(
    private val getDailyPanchangaUseCase: GetDailyPanchangaUseCase,
    private val astrologyRepository: AstrologyRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val googleDriveBackupManager: GoogleDriveBackupManager,
    private val cloudAccountRepository: CloudAccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NavtaraUiState())
    val uiState: StateFlow<NavtaraUiState> = _uiState.asStateFlow()
    private var lastComputeKey: String? = null
    private var autoSelectedProfileOnce = false

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
                recomputeIfNeeded(prefs, _uiState.value.inspectDate)
            }
        }

        viewModelScope.launch {
            cloudAccountRepository.currentUser.collect { user ->
                _uiState.update { it.copy(cloudUser = user) }
                if (user != null) {
                    runCatching { googleDriveBackupManager.ensureDefaultGoogleProfileIfNeeded(user) }
                }
            }
        }

        viewModelScope.launch {
            googleDriveBackupManager.status.collect { driveStatus ->
                _uiState.update { it.copy(driveBackupStatus = driveStatus) }
            }
        }

        viewModelScope.launch {
            astrologyRepository.observeNavtaraProfiles().collect { profiles ->
                val currentActiveId = _uiState.value.activeProfileId
                val activeExists = currentActiveId != null && profiles.any { it.id == currentActiveId }
                if ((!autoSelectedProfileOnce || (currentActiveId != null && !activeExists)) && profiles.isNotEmpty()) {
                    autoSelectedProfileOnce = true
                    val picked = profiles.firstOrNull { it.isDefault } ?: profiles.first()
                    _uiState.update {
                        it.copy(
                            profiles = profiles,
                            activeProfileId = picked.id,
                            nativeName = picked.name,
                            birthPada = picked.birthPada.coerceIn(1, 4)
                        )
                    }
                    if (_uiState.value.preferences.birthNakshatra != picked.birthNakshatraIndex) {
                        preferencesRepository.setBirthNakshatra(picked.birthNakshatraIndex)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            profiles = profiles,
                            activeProfileId = if (activeExists) currentActiveId else null
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            _uiState
                .map { it.activeProfileId }
                .distinctUntilChanged()
                .flatMapLatest { profileId ->
                    if (profileId != null) astrologyRepository.observeNavtaraFeedbacks(profileId)
                    else flowOf(emptyList())
                }
                .collect { fbList ->
                    val map = fbList.associateBy { it.dateIso }
                    _uiState.update { current ->
                        current.copy(
                            feedbacksByDate = map,
                            periodReport = mergeFeedbacks(current.rawPeriodReport, map)
                        )
                    }
                }
        }

        viewModelScope.launch {
            astrologyRepository.observeSavedNavtaraReports().collect { reports ->
                _uiState.update { it.copy(savedReports = reports) }
            }
        }
    }

    private fun recomputeIfNeeded(prefs: UserPreferences, targetDate: LocalDate) {
        val loc = prefs.selectedLocation
        val key = "${loc.name}|${"%.5f".format(Locale.US, loc.latitude)}|${"%.5f".format(Locale.US, loc.longitude)}|${loc.timezone}|${prefs.monthSystem.apiKey}|${prefs.ayanamsha.apiKey}|${prefs.birthNakshatra}|$targetDate"
        if (key != lastComputeKey) {
            lastComputeKey = key
            recompute(prefs, targetDate)
        }
    }

    private fun recompute(prefs: UserPreferences, targetDate: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val now = System.currentTimeMillis()
                val zone = TimeFormatters.safeZoneId(prefs.selectedLocation.timezone)
                val todayInZone = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                val snap = getDailyPanchangaUseCase(
                    location = prefs.selectedLocation,
                    date = targetDate,
                    monthSystem = prefs.monthSystem,
                    ayanamsha = prefs.ayanamsha,
                    includeExtendedTransits = true
                )
                val activeNak = if (targetDate == todayInZone) {
                    snap.nakshatra.firstOrNull { seg ->
                        val s = seg.startTimeMs ?: Long.MIN_VALUE
                        val e = seg.endTimeMs ?: Long.MAX_VALUE
                        now in s..e
                    } ?: snap.nakshatra.firstOrNull()
                } else {
                    snap.nakshatra.firstOrNull()
                }
                val transitNum = (activeNak?.number ?: 1).coerceIn(1, 27)
                val todayRes = astrologyRepository.computeNavtara(prefs.birthNakshatra, transitNum)
                val fullMatrix = astrologyRepository.computeNavtaraMatrix(prefs.birthNakshatra)
                Pair(snap, Triple(transitNum, todayRes, fullMatrix))
            }.onSuccess { (snap, triple) ->
                val (transitNum, todayRes, fullMatrix) = triple
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snapshot = snap,
                        transitNakshatraNumber = transitNum,
                        todayResult = todayRes,
                        matrix = fullMatrix
                    )
                }
                if (_uiState.value.rawPeriodReport != null) {
                    generatePeriodReport()
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Computation failed") }
            }
        }
    }

    fun setInspectDate(date: LocalDate) {
        _uiState.update { it.copy(inspectDate = date) }
        recomputeIfNeeded(_uiState.value.preferences, date)
    }

    fun shiftInspectDate(deltaDays: Long) = setInspectDate(_uiState.value.inspectDate.plusDays(deltaDays))

    fun resetInspectDateToToday() {
        val zone = TimeFormatters.safeZoneId(_uiState.value.preferences.selectedLocation.timezone)
        setInspectDate(LocalDate.now(zone))
    }

    fun setMatrixFilter(category: NavtaraDayCategory?) {
        _uiState.update { it.copy(matrixFilter = category) }
    }

    fun setJournalFilter(rating: NavtaraFeedbackRating?) {
        _uiState.update { it.copy(journalFilter = rating) }
    }

    fun selectPlannerTab(tab: Int) {
        val clamped = tab.coerceIn(0, 2)
        _uiState.update { it.copy(plannerTab = clamped) }
        if (clamped == 1 && _uiState.value.rawPeriodReport == null && !_uiState.value.isGeneratingPeriod) {
            generatePeriodReport()
        }
    }

    fun setNativeName(name: String) = _uiState.update { it.copy(nativeName = name) }
    fun setBirthPada(pada: Int) = _uiState.update { it.copy(birthPada = pada.coerceIn(1, 4)) }
    fun setBirthNakshatra(nakshatra: Int) {
        viewModelScope.launch { preferencesRepository.setBirthNakshatra(nakshatra) }
    }

    fun selectProfile(profile: NavtaraProfile) {
        autoSelectedProfileOnce = true
        _uiState.update {
            it.copy(
                activeProfileId = profile.id,
                nativeName = profile.name,
                birthPada = profile.birthPada.coerceIn(1, 4)
            )
        }
        viewModelScope.launch {
            preferencesRepository.setBirthNakshatra(profile.birthNakshatraIndex)
            if (_uiState.value.rawPeriodReport != null) {
                generatePeriodReport()
            }
        }
    }

    fun saveOrUpdateProfile(
        id: Long?,
        name: String,
        birthNak: Int,
        pada: Int,
        notes: String,
        makeDefault: Boolean
    ) {
        viewModelScope.launch {
            val savedId = astrologyRepository.saveNavtaraProfile(
                NavtaraProfile(
                    id = id ?: 0L,
                    name = name,
                    birthNakshatraIndex = birthNak.coerceIn(1, 27),
                    birthPada = pada.coerceIn(1, 4),
                    notes = notes,
                    isDefault = makeDefault,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
            if (makeDefault) {
                astrologyRepository.setDefaultNavtaraProfile(savedId)
            }
            autoSelectedProfileOnce = true
            _uiState.update {
                it.copy(
                    activeProfileId = savedId,
                    nativeName = name,
                    birthPada = pada.coerceIn(1, 4)
                )
            }
            preferencesRepository.setBirthNakshatra(birthNak.coerceIn(1, 27))
            triggerDriveAutoSync()
        }
    }

    fun setDefaultProfile(profileId: Long) {
        viewModelScope.launch {
            astrologyRepository.setDefaultNavtaraProfile(profileId)
            triggerDriveAutoSync()
        }
    }

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            astrologyRepository.deleteNavtaraProfile(profileId)
            triggerDriveAutoSync()
        }
    }

    fun setPeriodModeCustom(custom: Boolean) = _uiState.update { it.copy(isCustomRange = custom) }

    fun shiftMonth(delta: Long) {
        val next = _uiState.value.selectedYearMonth.plusMonths(delta)
        _uiState.update {
            it.copy(
                selectedYearMonth = next,
                rangeStartDate = next.atDay(1),
                rangeEndDate = next.atEndOfMonth()
            )
        }
        generatePeriodReport()
    }

    fun setCustomRangeDates(start: LocalDate, end: LocalDate) {
        val orderedStart = minOf(start, end)
        val orderedEnd = maxOf(start, end)
        val clampedEnd = if (ChronoUnit.DAYS.between(orderedStart, orderedEnd) > 92L) {
            orderedStart.plusDays(92L)
        } else orderedEnd
        _uiState.update {
            it.copy(isCustomRange = true, rangeStartDate = orderedStart, rangeEndDate = clampedEnd)
        }
    }

    fun applyRangePreset(presetIndex: Int) {
        val zone = TimeFormatters.safeZoneId(_uiState.value.preferences.selectedLocation.timezone)
        val today = LocalDate.now(zone)
        when (presetIndex) {
            0 -> setCustomRangeDates(today, today.plusDays(14))
            1 -> setCustomRangeDates(today, today.plusDays(29))
            2 -> {
                val ym = YearMonth.from(today)
                setCustomRangeDates(ym.atDay(1), ym.atEndOfMonth())
            }
            else -> {
                val ym = YearMonth.from(today).plusMonths(1)
                setCustomRangeDates(ym.atDay(1), ym.atEndOfMonth())
            }
        }
        generatePeriodReport()
    }

    fun generatePeriodReport() {
        val state = _uiState.value
        val prefs = state.preferences
        val start = if (state.isCustomRange) state.rangeStartDate else state.selectedYearMonth.atDay(1)
        val end = if (state.isCustomRange) state.rangeEndDate else state.selectedYearMonth.atEndOfMonth()
        val labelName = state.nativeName.trim().ifBlank {
            if (prefs.language == AppLanguage.HINDI) "साधक" else "Native"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPeriod = true) }
            runCatching {
                astrologyRepository.computeNavtaraPeriodReport(
                    profileId = state.activeProfileId ?: 0L,
                    profileName = labelName,
                    birthNakshatraIndex = prefs.birthNakshatra,
                    birthPada = state.birthPada,
                    startDate = start,
                    endDate = end,
                    isDateRange = state.isCustomRange,
                    location = prefs.selectedLocation,
                    monthSystem = prefs.monthSystem,
                    ayanamsha = prefs.ayanamsha
                )
            }.onSuccess { report ->
                _uiState.update { current ->
                    current.copy(
                        isGeneratingPeriod = false,
                        rawPeriodReport = report,
                        periodReport = mergeFeedbacks(report, current.feedbacksByDate)
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isGeneratingPeriod = false, statusMessage = err.message ?: "Period report generation failed")
                }
            }
        }
    }

    fun saveCurrentPeriodReport(savedMsg: String) {
        val report = _uiState.value.periodReport ?: return
        viewModelScope.launch {
            val profileId = ensureProfileId()
            astrologyRepository.saveNavtaraReport(
                SavedNavtaraReport(
                    profileId = profileId,
                    profileName = report.profileName,
                    birthNakshatraIndex = report.birthNakshatraIndex,
                    birthPada = report.birthPada,
                    startDateIso = report.startDateIso,
                    endDateIso = report.endDateIso,
                    isDateRange = report.isDateRange,
                    locationName = report.location.name,
                    latitude = report.location.latitude,
                    longitude = report.location.longitude,
                    timezone = report.location.timezone,
                    ayanamshaKey = report.ayanamsha,
                    monthSystem = report.monthSystem,
                    auspiciousCount = report.stats.auspiciousCount,
                    mixedCount = report.stats.mixedCount,
                    cautionCount = report.stats.cautionCount,
                    neutralCount = report.stats.neutralCount,
                    createdAtMs = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(statusMessage = savedMsg) }
            triggerDriveAutoSync()
        }
    }

    fun openSavedReport(saved: SavedNavtaraReport) {
        val start = runCatching { LocalDate.parse(saved.startDateIso) }.getOrElse { YearMonth.now().atDay(1) }
        val end = runCatching { LocalDate.parse(saved.endDateIso) }.getOrElse { YearMonth.now().atEndOfMonth() }
        val savedLoc = CityLocation(
            name = saved.locationName,
            country = "",
            latitude = saved.latitude,
            longitude = saved.longitude,
            timezone = saved.timezone,
            isCustom = true
        )
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    plannerTab = 1,
                    isCustomRange = saved.isDateRange,
                    selectedYearMonth = YearMonth.from(start),
                    rangeStartDate = start,
                    rangeEndDate = end,
                    nativeName = saved.profileName,
                    birthPada = saved.birthPada,
                    activeProfileId = if (saved.profileId > 0L) saved.profileId else it.activeProfileId,
                    isGeneratingPeriod = true
                )
            }
            val loadedFeedbacks = if (saved.profileId > 0L) {
                runCatching {
                    astrologyRepository.observeNavtaraFeedbacks(saved.profileId).first()
                }.getOrDefault(emptyList()).associateBy { it.dateIso }
            } else emptyMap()

            runCatching {
                astrologyRepository.computeNavtaraPeriodReport(
                    profileId = saved.profileId,
                    profileName = saved.profileName,
                    birthNakshatraIndex = saved.birthNakshatraIndex,
                    birthPada = saved.birthPada,
                    startDate = start,
                    endDate = end,
                    isDateRange = saved.isDateRange,
                    location = savedLoc,
                    monthSystem = saved.monthSystem,
                    ayanamsha = saved.ayanamshaKey
                )
            }.onSuccess { rep ->
                val mergedMap = loadedFeedbacks.ifEmpty { _uiState.value.feedbacksByDate }
                _uiState.update { current ->
                    current.copy(
                        isGeneratingPeriod = false,
                        feedbacksByDate = mergedMap,
                        rawPeriodReport = rep,
                        periodReport = mergeFeedbacks(rep, mergedMap)
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isGeneratingPeriod = false, statusMessage = err.message) }
            }
            if (saved.birthNakshatraIndex in 1..27) {
                preferencesRepository.setBirthNakshatra(saved.birthNakshatraIndex)
            }
        }
    }

    fun deleteSavedReport(reportId: Long) {
        viewModelScope.launch {
            astrologyRepository.deleteNavtaraReport(reportId)
            triggerDriveAutoSync()
        }
    }

    fun saveDailyFeedback(dateIso: String, rating: NavtaraFeedbackRating, note: String) {
        viewModelScope.launch {
            val profileId = ensureProfileId()
            astrologyRepository.saveNavtaraFeedback(
                NavtaraDailyFeedback(
                    profileId = profileId,
                    dateIso = dateIso,
                    rating = rating,
                    note = note,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            _uiState.update {
                it.copy(
                    statusMessage = if (isHi) "फीडबैक सेव हुआ · JSON वॉल्ट व Google Drive सिंक जारी"
                    else "Feedback saved to local JSON vault & Google Drive"
                )
            }
            triggerDriveAutoSync()
        }
    }

    fun deleteDailyFeedback(dateIso: String) {
        val profileId = _uiState.value.activeProfileId ?: return
        viewModelScope.launch {
            astrologyRepository.deleteNavtaraFeedback(profileId, dateIso)
            triggerDriveAutoSync()
        }
    }

    fun authorizeGoogleDrive(activity: Activity?, onNeedsResolution: (PendingIntent) -> Unit) {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.authorizeDriveWithResolution(activity)
            res.onSuccess { pendingIntent ->
                if (pendingIntent != null) {
                    onNeedsResolution(pendingIntent)
                } else {
                    _uiState.update {
                        it.copy(
                            statusMessage = if (isHi) "Google Drive JSON ऑटो-सिंक सक्रिय"
                            else "Google Drive JSON auto-sync connected"
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        statusMessage = err.localizedMessage
                            ?: if (isHi) "Google Drive कनेक्शन विफल" else "Failed to connect Google Drive"
                    )
                }
            }
        }
    }

    fun handleDriveAuthResult(data: Intent?, resultCode: Int = Activity.RESULT_OK) {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.handleDriveResolutionResult(data, resultCode)
            _uiState.update {
                it.copy(
                    statusMessage = res.fold(
                        onSuccess = {
                            if (isHi) "आपके निजी Google Drive में नवतारा व ऐप डेटा सिंक हो गया"
                            else "Synced Navtara & app data to your personal Google Drive"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "Google Drive अनुमति रद्द" else "Drive authorization cancelled"
                        }
                    )
                )
            }
        }
    }

    fun linkGoogleDriveBackupUri(uri: Uri) {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.linkGoogleDriveBackupUri(uri)
            _uiState.update {
                it.copy(
                    statusMessage = res.fold(
                        onSuccess = { name ->
                            if (isHi) "Google Drive ऑटो-सिंक सक्रिय: $name"
                            else "Linked personal Google Drive auto-sync: $name"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "Google Drive लिंक विफल" else "Failed to link Google Drive file"
                        }
                    )
                )
            }
        }
    }

    fun syncDriveJsonNow() {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.syncBidirectionalWithGoogleDrive(forcePullFirst = false)
            _uiState.update {
                it.copy(
                    statusMessage = res.fold(
                        onSuccess = {
                            if (isHi) "आपके निजी Google Drive में नवीनतम डेटा सिंक हो गया"
                            else "Synced latest data privately with your Google Drive"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "डिवाइस पर JSON सेव, Drive जोड़ें" else "Saved locally; connect Google Drive for cloud sync"
                        }
                    )
                )
            }
        }
    }

    fun restoreFromDrive(uri: Uri? = null) {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.restoreFromGoogleDriveUri(uri)
            _uiState.update {
                it.copy(
                    statusMessage = res.fold(
                        onSuccess = { count ->
                            if (isHi) "JSON से $count प्रविष्टियाँ रीस्टोर हुईं"
                            else "Restored $count entries from JSON backup"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "रीस्टोर विफल" else "Restore failed"
                        }
                    )
                )
            }
        }
    }

    suspend fun exportNavtaraJsonString(): String =
        googleDriveBackupManager.exportNavtaraJsonString(
            activeProfileId = _uiState.value.activeProfileId,
            currentPeriodReport = _uiState.value.periodReport
        )

    fun importJsonString(rawJson: String) {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.importJsonString(rawJson)
            _uiState.update {
                it.copy(
                    statusMessage = res.fold(
                        onSuccess = { count ->
                            if (isHi) "JSON से $count रिकॉर्ड आयात किए गए"
                            else "Imported $count records from JSON"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "JSON आयात विफल" else "JSON import failed"
                        }
                    )
                )
            }
        }
    }

    fun suggestedJsonFileName(): String {
        val cleanName = _uiState.value.nativeName.trim().lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "_").trim('_').ifBlank { "navtara" }
        return "navtara_${cleanName}_${LocalDate.now()}.json"
    }

    private fun triggerDriveAutoSync() {
        viewModelScope.launch {
            runCatching { googleDriveBackupManager.syncToGoogleDriveNow() }
        }
    }

    private suspend fun ensureProfileId(): Long {
        val existing = _uiState.value.activeProfileId
        if (existing != null && existing > 0L) return existing
        val state = _uiState.value
        val lang = state.preferences.language
        val fallbackName = state.nativeName.trim().ifBlank {
            state.cloudUser?.displayName?.takeIf { it.isNotBlank() }
                ?: if (lang == AppLanguage.HINDI) "साधक" else "Native"
        }
        val createdId = astrologyRepository.saveNavtaraProfile(
            NavtaraProfile(
                name = fallbackName,
                birthNakshatraIndex = state.preferences.birthNakshatra,
                birthPada = state.birthPada,
                notes = state.cloudUser?.email.orEmpty(),
                isDefault = state.profiles.isEmpty(),
                updatedAtMs = System.currentTimeMillis()
            )
        )
        autoSelectedProfileOnce = true
        _uiState.update { it.copy(activeProfileId = createdId, nativeName = fallbackName) }
        return createdId
    }

    private fun mergeFeedbacks(
        raw: NavtaraPeriodReport?,
        feedbacksByDate: Map<String, NavtaraDailyFeedback>
    ): NavtaraPeriodReport? {
        raw ?: return null
        val mergedDays = raw.days.map { day -> day.copy(feedback = feedbacksByDate[day.dateIso]) }
        var fbCount = 0
        var goodFb = 0
        var neuFb = 0
        var badFb = 0
        mergedDays.forEach { day ->
            val fb = day.feedback
            if (fb != null) {
                fbCount++
                when (fb.rating) {
                    NavtaraFeedbackRating.AUSPICIOUS -> goodFb++
                    NavtaraFeedbackRating.NEUTRAL -> neuFb++
                    NavtaraFeedbackRating.CAUTION -> badFb++
                }
            }
        }
        return raw.copy(
            days = mergedDays,
            stats = raw.stats.copy(
                feedbackCount = fbCount,
                auspiciousFeedbackCount = goodFb,
                neutralFeedbackCount = neuFb,
                cautionFeedbackCount = badFb
            )
        )
    }

    fun setStatusMessage(msg: String) = _uiState.update { it.copy(statusMessage = msg) }
    fun clearStatusMessage() = _uiState.update { it.copy(statusMessage = null) }

    fun toggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }

    fun retry() {
        lastComputeKey = null
        recompute(_uiState.value.preferences, _uiState.value.inspectDate)
    }
}
