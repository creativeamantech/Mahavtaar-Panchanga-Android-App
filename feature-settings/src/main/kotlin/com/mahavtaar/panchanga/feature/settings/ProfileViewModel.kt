package com.mahavtaar.panchanga.feature.settings

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.data.backup.GoogleDriveBackupManager
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.CloudSyncStatus
import com.mahavtaar.panchanga.domain.model.CloudUser
import com.mahavtaar.panchanga.domain.model.GoogleDriveBackupStatus
import com.mahavtaar.panchanga.domain.model.NavtaraProfile
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.CloudAccountRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val cloudUser: CloudUser? = null,
    val cloudSyncStatus: CloudSyncStatus = CloudSyncStatus(),
    val driveBackupStatus: GoogleDriveBackupStatus = GoogleDriveBackupStatus(),
    val preferences: UserPreferences = UserPreferences(),
    val profiles: List<NavtaraProfile> = emptyList(),
    val selectedProfileId: Long? = null,
    val selectedProfileFeedbackCount: Int = 0,
    val savedReportCount: Int = 0,
    val statusMessage: String? = null
) {
    val activeProfile: NavtaraProfile?
        get() = profiles.firstOrNull { it.id == selectedProfileId }
            ?: profiles.firstOrNull { it.isDefault }
            ?: profiles.firstOrNull()
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel @Inject constructor(
    private val cloudAccountRepository: CloudAccountRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val astrologyRepository: AstrologyRepository,
    private val googleDriveBackupManager: GoogleDriveBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                cloudAccountRepository.currentUser,
                cloudAccountRepository.syncStatus,
                googleDriveBackupManager.status,
                preferencesRepository.preferences,
                astrologyRepository.observeNavtaraProfiles()
            ) { user, cloudSync, driveStatus, prefs, profiles ->
                listOf(user, cloudSync, driveStatus, prefs, profiles)
            }.collect { tuple ->
                val user = tuple[0] as CloudUser?
                val cloudSync = tuple[1] as CloudSyncStatus
                val driveStatus = tuple[2] as GoogleDriveBackupStatus
                val prefs = tuple[3] as UserPreferences
                @Suppress("UNCHECKED_CAST")
                val profiles = tuple[4] as List<NavtaraProfile>

                if (user != null && profiles.isEmpty()) {
                    runCatching { googleDriveBackupManager.ensureDefaultGoogleProfileIfNeeded(user) }
                }

                _uiState.update { cur ->
                    val resolvedId = cur.selectedProfileId
                        ?.takeIf { id -> profiles.any { it.id == id } }
                        ?: profiles.firstOrNull { it.isDefault }?.id
                        ?: profiles.firstOrNull()?.id
                    cur.copy(
                        cloudUser = user,
                        cloudSyncStatus = cloudSync,
                        driveBackupStatus = driveStatus,
                        preferences = prefs,
                        profiles = profiles,
                        selectedProfileId = resolvedId
                    )
                }
            }
        }

        viewModelScope.launch {
            _uiState
                .map { it.activeProfile?.id }
                .distinctUntilChanged()
                .flatMapLatest { id ->
                    if (id != null && id > 0L) astrologyRepository.observeNavtaraFeedbacks(id)
                    else flowOf(emptyList())
                }
                .collect { feedbacks ->
                    _uiState.update { it.copy(selectedProfileFeedbackCount = feedbacks.size) }
                }
        }

        viewModelScope.launch {
            astrologyRepository.observeSavedNavtaraReports().collect { reports ->
                _uiState.update { it.copy(savedReportCount = reports.size) }
            }
        }
    }

    fun selectProfile(profile: NavtaraProfile) {
        _uiState.update { it.copy(selectedProfileId = profile.id) }
        viewModelScope.launch {
            preferencesRepository.setBirthNakshatra(profile.birthNakshatraIndex)
        }
    }

    fun saveProfile(
        id: Long?,
        name: String,
        birthNakshatra: Int,
        birthPada: Int,
        notes: String,
        makeDefault: Boolean
    ) {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val cleanName = name.trim().ifBlank {
                _uiState.value.cloudUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: if (isHi) "साधक" else "Native"
            }
            val savedId = astrologyRepository.saveNavtaraProfile(
                NavtaraProfile(
                    id = id ?: 0L,
                    name = cleanName,
                    birthNakshatraIndex = birthNakshatra.coerceIn(1, 27),
                    birthPada = birthPada.coerceIn(1, 4),
                    notes = notes.trim(),
                    isDefault = makeDefault || _uiState.value.profiles.isEmpty(),
                    updatedAtMs = System.currentTimeMillis()
                )
            )
            if (makeDefault) {
                astrologyRepository.setDefaultNavtaraProfile(savedId)
            }
            preferencesRepository.setBirthNakshatra(birthNakshatra.coerceIn(1, 27))
            _uiState.update {
                it.copy(
                    selectedProfileId = savedId,
                    statusMessage = if (isHi) "प्रोफ़ाइल '$cleanName' सेव हो गई" else "Saved profile '$cleanName'"
                )
            }
            runCatching { googleDriveBackupManager.syncToGoogleDriveNow() }
        }
    }

    fun setDefaultProfile(profileId: Long) {
        viewModelScope.launch {
            astrologyRepository.setDefaultNavtaraProfile(profileId)
            val target = _uiState.value.profiles.firstOrNull { it.id == profileId }
            if (target != null) {
                preferencesRepository.setBirthNakshatra(target.birthNakshatraIndex)
            }
            _uiState.update { it.copy(selectedProfileId = profileId) }
            runCatching { googleDriveBackupManager.syncToGoogleDriveNow() }
        }
    }

    fun deleteProfile(profileId: Long) {
        viewModelScope.launch {
            astrologyRepository.deleteNavtaraProfile(profileId)
            runCatching { googleDriveBackupManager.syncToGoogleDriveNow() }
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
                            else "Google Drive JSON auto-sync active"
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        statusMessage = err.localizedMessage
                            ?: if (isHi) "Google Drive कनेक्शन विफल" else "Google Drive connection failed"
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
                            else "Connected & auto-synced with your personal Google Drive"
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
                            if (isHi) "Google Drive ऑटो-सिंक फ़ाइल जुड़ गई: $name"
                            else "Linked personal Google Drive auto-sync file: $name"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "Google Drive लिंक विफल" else "Failed to link Drive file"
                        }
                    )
                )
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.syncBidirectionalWithGoogleDrive(forcePullFirst = false)
            _uiState.update {
                it.copy(
                    statusMessage = res.fold(
                        onSuccess = {
                            if (isHi) "आपका संपूर्ण डेटा आपके निजी Google Drive में सिंक हो गया"
                            else "Synced all your data privately with your Google Drive"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "डिवाइस वॉल्ट में सेव, Drive कनेक्ट करें" else "Saved locally; connect Google Drive for cloud sync"
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
                            if (isHi) "JSON बैकअप से $count रिकॉर्ड रीस्टोर हुए"
                            else "Restored $count records from JSON backup"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "रीस्टोर विफल रहा" else "Restore failed"
                        }
                    )
                )
            }
        }
    }

    suspend fun exportFullJson(): String = googleDriveBackupManager.exportFullBundleJsonString()

    fun importJsonContent(rawJson: String) {
        viewModelScope.launch {
            val isHi = _uiState.value.preferences.language == AppLanguage.HINDI
            val res = googleDriveBackupManager.importJsonString(rawJson)
            _uiState.update {
                it.copy(
                    statusMessage = res.fold(
                        onSuccess = { count ->
                            if (isHi) "JSON से $count प्रविष्टियाँ आयात की गईं"
                            else "Imported $count entries from JSON"
                        },
                        onFailure = { err ->
                            err.localizedMessage ?: if (isHi) "JSON आयात विफल" else "JSON import failed"
                        }
                    )
                )
            }
        }
    }

    fun suggestedDriveFileName(): String =
        googleDriveBackupManager.suggestedDriveFileName(_uiState.value.cloudUser)

    fun toggleLanguage() {
        val next = _uiState.value.preferences.language.toggled()
        viewModelScope.launch { preferencesRepository.setLanguage(next) }
    }

    fun setStatusMessage(msg: String) = _uiState.update { it.copy(statusMessage = msg) }
    fun clearStatusMessage() = _uiState.update { it.copy(statusMessage = null) }
}
