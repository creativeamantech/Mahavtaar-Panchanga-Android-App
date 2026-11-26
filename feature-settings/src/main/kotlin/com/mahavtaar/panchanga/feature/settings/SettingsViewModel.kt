package com.mahavtaar.panchanga.feature.settings

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.data.backup.GoogleDriveBackupManager
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppThemeMode
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CloudSyncStatus
import com.mahavtaar.panchanga.domain.model.CloudUser
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.GoogleDriveBackupStatus
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.NotificationPreset
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import com.mahavtaar.panchanga.domain.model.WidgetGlassStyle
import com.mahavtaar.panchanga.domain.notifications.AlertEnginePort
import java.time.LocalDate
import java.time.ZoneId
import com.mahavtaar.panchanga.domain.repository.CloudAccountRepository
import com.mahavtaar.panchanga.domain.repository.LocationRepository
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import com.mahavtaar.panchanga.domain.repository.PanchangaRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.domain.usecase.SearchCitiesUseCase
import com.mahavtaar.panchanga.location.DeviceLocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val savedLocations: List<CityLocation> = emptyList(),
    val cloudUser: CloudUser? = null,
    val cloudSyncStatus: CloudSyncStatus = CloudSyncStatus(),
    val driveBackupStatus: GoogleDriveBackupStatus = GoogleDriveBackupStatus(),
    val notifyMoonrise: Boolean = false,
    val notifyMoonset: Boolean = false,
    val persistentRunningNotificationEnabled: Boolean = false,
    val audioSettings: AlertAudioSettings = AlertAudioSettings(),
    val nextSunriseMs: Long? = null,
    val searchQuery: String = "",
    val searchResults: List<CityLocation> = emptyList(),
    val isDetectingGps: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository,
    private val panchangaRepository: PanchangaRepository,
    private val notificationStudioRepository: NotificationStudioRepository,
    private val googleDriveBackupManager: GoogleDriveBackupManager,
    private val searchCitiesUseCase: SearchCitiesUseCase,
    private val deviceLocationProvider: DeviceLocationProvider,
    private val cloudAccountRepository: CloudAccountRepository,
    private val alertEnginePort: AlertEnginePort
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferences,
                locationRepository.observeSavedLocations(),
                cloudAccountRepository.currentUser,
                cloudAccountRepository.syncStatus,
                googleDriveBackupManager.status
            ) { prefs, saved, user, syncStatus, driveStatus ->
                listOf(prefs, saved, user, syncStatus, driveStatus)
            }.collect { tuple ->
                val prefs = tuple[0] as UserPreferences
                @Suppress("UNCHECKED_CAST")
                val saved = tuple[1] as List<CityLocation>
                val user = tuple[2] as CloudUser?
                val syncStatus = tuple[3] as CloudSyncStatus
                val driveStatus = tuple[4] as GoogleDriveBackupStatus
                val nextSunrise = runCatching {
                    val zone = ZoneId.of(prefs.selectedLocation.timezone)
                    val today = LocalDate.now(zone)
                    val now = System.currentTimeMillis()
                    val todayPanchanga = panchangaRepository.getDailyPanchanga(
                        date = today,
                        location = prefs.selectedLocation,
                        ayanamsha = prefs.ayanamsha,
                        monthSystem = prefs.monthSystem
                    )
                    if (todayPanchanga.sunriseMs > now + 60_000L) {
                        todayPanchanga.sunriseMs
                    } else {
                        panchangaRepository.getDailyPanchanga(
                            date = today.plusDays(1),
                            location = prefs.selectedLocation,
                            ayanamsha = prefs.ayanamsha,
                            monthSystem = prefs.monthSystem
                        ).sunriseMs
                    }
                }.getOrNull()
                _uiState.update {
                    it.copy(
                        preferences = prefs,
                        savedLocations = saved,
                        cloudUser = user,
                        cloudSyncStatus = syncStatus,
                        driveBackupStatus = driveStatus,
                        nextSunriseMs = nextSunrise
                    )
                }
            }
        }
        viewModelScope.launch {
            notificationStudioRepository.observeRules().collect { rules ->
                val byType = rules.associateBy { it.eventType }
                _uiState.update { cur ->
                    cur.copy(
                        notifyMoonrise = byType[VedicAlertEventType.MOONRISE]?.isEnabled == true,
                        notifyMoonset = byType[VedicAlertEventType.MOONSET]?.isEnabled == true ||
                            byType[VedicAlertEventType.SWARA_MOONSET_NADI]?.isEnabled == true
                    )
                }
            }
        }
        viewModelScope.launch {
            notificationStudioRepository.observeAudioSettings().collect { audio ->
                _uiState.update { cur ->
                    cur.copy(
                        persistentRunningNotificationEnabled = audio.persistentRunningNotificationEnabled,
                        audioSettings = audio
                    )
                }
            }
        }
        viewModelScope.launch {
            val popular = searchCitiesUseCase("")
            _uiState.update { it.copy(searchResults = popular) }
        }
    }

    fun suggestedDriveFileName(): String =
        googleDriveBackupManager.suggestedDriveFileName(_uiState.value.cloudUser)

    fun linkGoogleDriveBackupUri(uri: Uri) {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.linkGoogleDriveBackupUri(uri)
            _uiState.update {
                it.copy(
                    statusMessage = res.fold(
                        onSuccess = { name ->
                            if (isHi) "Google Drive ऑटो-सिंक सक्रिय: $name"
                            else "Google Drive auto-sync linked: $name"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "Google Drive लिंक विफल रहा" else "Failed to link Google Drive"
                        }
                    )
                )
            }
        }
    }

    fun syncToGoogleDriveNow() {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.syncToGoogleDriveNow()
            _uiState.update {
                it.copy(
                    statusMessage = res.fold(
                        onSuccess = {
                            if (isHi) "डेटा डिवाइस व Google Drive में सुरक्षित सेव हो गया"
                            else "All user data saved to device & synced to Google Drive"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "Google Drive सिंक विफल" else "Google Drive sync failed"
                        }
                    )
                )
            }
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
                            statusMessage = if (isHi) "Google Drive ऑटो-बैकअप सक्रिय हो गया"
                            else "Google Drive auto-backup connected"
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        statusMessage = err.localizedMessage
                            ?: if (isHi) "Google Drive कनेक्शन विफल रहा" else "Failed to connect Google Drive"
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
                            if (isHi) "आपके निजी Google Drive से जुड़कर ऑटो-सिंक सक्रिय हो गया"
                            else "Connected to your personal Google Drive — auto-sync active"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "Google Drive अनुमति रद्द" else "Google Drive authorization failed"
                        }
                    )
                )
            }
        }
    }

    fun restoreFromGoogleDriveUri(uri: Uri? = null) {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.restoreFromGoogleDriveUri(uri)
            _uiState.update {
                it.copy(
                    statusMessage = res.fold(
                        onSuccess = { count ->
                            if (isHi) "Google Drive से $count रिकॉर्ड सफलतापूर्वक रीस्टोर किए गए"
                            else "Restored $count user records from Google Drive backup"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "रीस्टोर विफल रहा" else "Restore failed"
                        }
                    )
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(150L)
            val results = searchCitiesUseCase(query, limit = 30)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    fun selectLocation(location: CityLocation) {
        viewModelScope.launch {
            val existingDefault = _uiState.value.savedLocations.firstOrNull { it.name == location.name }?.isDefault ?: location.isDefault
            val updated = location.copy(isDefault = existingDefault)
            preferencesRepository.setSelectedLocation(updated)
            locationRepository.saveLocation(updated)
            _uiState.update { it.copy(statusMessage = "Switched to ${updated.name}") }
        }
    }

    fun saveCustomLocation(
        name: String,
        latitude: Double,
        longitude: Double,
        timezone: String,
        makeDefault: Boolean,
        originalNameToReplace: String? = null
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            if (cleanName.isBlank()) return@launch
            if (originalNameToReplace != null && originalNameToReplace != cleanName) {
                locationRepository.removeLocation(originalNameToReplace)
            }
            val customLoc = CityLocation(
                name = cleanName,
                country = "Custom",
                latitude = latitude,
                longitude = longitude,
                timezone = timezone.trim().ifBlank { "Asia/Kolkata" },
                population = 0L,
                isGpsResolved = false,
                isCustom = true,
                isDefault = makeDefault,
                accuracyMeters = null
            )
            locationRepository.saveLocation(customLoc)
            if (makeDefault) {
                locationRepository.setDefaultLocation(cleanName)
            }
            preferencesRepository.setSelectedLocation(customLoc)
            _uiState.update { it.copy(statusMessage = "Saved & applied: $cleanName") }
        }
    }

    fun setDefaultLocation(location: CityLocation) {
        viewModelScope.launch {
            val existing = _uiState.value.savedLocations.any { it.name == location.name }
            if (!existing) {
                locationRepository.saveLocation(location.copy(isDefault = true))
            }
            locationRepository.setDefaultLocation(location.name)
            val updated = location.copy(isDefault = true)
            preferencesRepository.setSelectedLocation(updated)
            _uiState.update { it.copy(statusMessage = "Pinned default: ${location.name}") }
        }
    }

    fun removeSavedLocation(name: String) {
        viewModelScope.launch {
            locationRepository.removeLocation(name)
        }
    }

    fun detectCurrentGpsLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetectingGps = true, statusMessage = null) }
            val coordsResult = deviceLocationProvider.getCurrentCoordinates()
            coordsResult.onSuccess { coords ->
                val resolvedBase = locationRepository.resolveNearestCity(coords.latitude, coords.longitude)
                val resolved = resolvedBase.copy(
                    latitude = coords.latitude,
                    longitude = coords.longitude,
                    isGpsResolved = true,
                    accuracyMeters = coords.accuracyMeters
                )
                preferencesRepository.setSelectedLocation(resolved)
                locationRepository.saveLocation(resolved)
                _uiState.update {
                    it.copy(
                        isDetectingGps = false,
                        statusMessage = "GPS locked: ${resolved.name} (${resolved.timezone})"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isDetectingGps = false,
                        statusMessage = err.message ?: "GPS unavailable. Please search a city below."
                    )
                }
            }
        }
    }

    fun clearStatusMessage() = _uiState.update { it.copy(statusMessage = null) }

    fun setMonthSystem(system: MonthSystem) {
        viewModelScope.launch { preferencesRepository.setMonthSystem(system) }
    }

    fun setAyanamsha(coord: CoordinateSelection) {
        viewModelScope.launch { preferencesRepository.setAyanamsha(coord) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
    }

    fun toggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }

    fun setBirthNakshatra(nakshatra: Int) {
        viewModelScope.launch { preferencesRepository.setBirthNakshatra(nakshatra.coerceIn(1, 27)) }
    }

    fun setUse24HourFormat(use24h: Boolean) {
        viewModelScope.launch { preferencesRepository.setUse24HourFormat(use24h) }
    }

    fun setWidgetGlassStyle(style: WidgetGlassStyle) {
        viewModelScope.launch { preferencesRepository.setWidgetGlassStyle(style) }
    }

    fun setWidgetOpacity(opacity: Float) {
        viewModelScope.launch { preferencesRepository.setWidgetOpacity(opacity) }
    }

    fun updateNotifications(
        notifySunrise: Boolean? = null,
        notifySunset: Boolean? = null,
        notifyMoonrise: Boolean? = null,
        notifyMoonset: Boolean? = null,
        notifyTithiChange: Boolean? = null,
        notifyNakshatraChange: Boolean? = null,
        notifyVratReminders: Boolean? = null
    ) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationSettings(
                notifySunrise = notifySunrise,
                notifySunset = notifySunset,
                notifyTithiChange = notifyTithiChange,
                notifyNakshatraChange = notifyNakshatraChange,
                notifyVratReminders = notifyVratReminders
            )
            // Keep NotificationStudioRepository rules directly in sync so AlarmManager schedules them immediately
            notifySunrise?.let {
                notificationStudioRepository.setEventEnabled(VedicAlertEventType.SUNRISE, it)
            }
            notifySunset?.let {
                notificationStudioRepository.setEventEnabled(VedicAlertEventType.SUNSET, it)
            }
            notifyMoonrise?.let {
                notificationStudioRepository.setEventEnabled(VedicAlertEventType.MOONRISE, it)
                notificationStudioRepository.setEventEnabled(VedicAlertEventType.SWARA_MOONRISE_NADI, it)
            }
            notifyMoonset?.let {
                notificationStudioRepository.setEventEnabled(VedicAlertEventType.MOONSET, it)
                notificationStudioRepository.setEventEnabled(VedicAlertEventType.SWARA_MOONSET_NADI, it)
            }
            notifyTithiChange?.let {
                notificationStudioRepository.setEventEnabled(VedicAlertEventType.TITHI_CHANGE, it)
            }
            notifyNakshatraChange?.let {
                notificationStudioRepository.setEventEnabled(VedicAlertEventType.NAKSHATRA_CHANGE, it)
            }
            notifyVratReminders?.let {
                notificationStudioRepository.setEventEnabled(VedicAlertEventType.VRAT_OBSERVANCE, it)
                notificationStudioRepository.setEventEnabled(VedicAlertEventType.MAJOR_FESTIVAL, it)
            }
        }
    }

    fun setPersistentRunningNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = notificationStudioRepository.getAudioSettings()
            notificationStudioRepository.updateAudioSettings(
                current.copy(persistentRunningNotificationEnabled = enabled)
            )
            runCatching { alertEnginePort.syncRunningNowNotification() }
        }
    }

    fun updateAudioSettings(updated: AlertAudioSettings) {
        viewModelScope.launch {
            notificationStudioRepository.updateAudioSettings(updated)
            runCatching { alertEnginePort.rescheduleAllAlerts() }
            runCatching { alertEnginePort.syncRunningNowNotification() }
        }
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            panchangaRepository.clearExpiredCache()
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            _uiState.update {
                it.copy(
                    statusMessage = if (isHi) "ऑफ़लाइन पंचांग कैश साफ़ कर दिया गया"
                    else "Offline ephemeris cache cleared"
                )
            }
        }
    }

    fun syncCloudNow() {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val result = cloudAccountRepository.syncAllNow()
            _uiState.update {
                it.copy(
                    statusMessage = if (result.isSuccess) {
                        if (isHi) "आपके निजी Google Drive व लोकल वॉल्ट में सिंक पूर्ण हुआ"
                        else "Synced privately with your Google Drive & local vault"
                    } else {
                        result.exceptionOrNull()?.localizedMessage
                            ?: if (isHi) "Google Drive सिंक विफल रहा" else "Google Drive sync failed"
                    }
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            cloudAccountRepository.signOut()
        }
    }

    fun replayQuickSetup() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(false)
        }
    }

    fun completeQuickSetup(preset: NotificationPreset? = null) {
        viewModelScope.launch {
            if (preset != null) {
                notificationStudioRepository.applyPreset(preset)
                runCatching { alertEnginePort.rescheduleAllAlerts() }
                runCatching { alertEnginePort.syncRunningNowNotification() }
            }
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun applyNotificationPreset(preset: NotificationPreset) {
        viewModelScope.launch {
            notificationStudioRepository.applyPreset(preset)
            runCatching { alertEnginePort.rescheduleAllAlerts() }
            runCatching { alertEnginePort.syncRunningNowNotification() }
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetDefaults()
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            _uiState.update {
                it.copy(
                    statusMessage = if (isHi) "सभी सेटिंग्स डिफ़ॉल्ट पर बहाल कर दी गई हैं"
                    else "Preferences reset to defaults"
                )
            }
        }
    }
}
