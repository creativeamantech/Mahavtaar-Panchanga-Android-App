package com.mahavtaar.panchanga.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NotificationPreset
import com.mahavtaar.panchanga.domain.model.NotificationRule
import com.mahavtaar.panchanga.domain.model.ScheduledAlertItem
import com.mahavtaar.panchanga.domain.model.TuneSourceType
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.VedicAlertCategory
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import com.mahavtaar.panchanga.domain.model.VoiceRecording
import com.mahavtaar.panchanga.domain.notifications.AlertEnginePort
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationStudioUiState(
    val preferences: UserPreferences = UserPreferences(),
    val rules: List<NotificationRule> = emptyList(),
    val voiceRecordings: List<VoiceRecording> = emptyList(),
    val audioSettings: AlertAudioSettings = AlertAudioSettings(),
    val upcomingAlerts: List<ScheduledAlertItem> = emptyList(),
    val recentHistory: List<ScheduledAlertItem> = emptyList(),
    val selectedTab: Int = 0,
    val selectedCategory: VedicAlertCategory? = null,
    val editingRule: NotificationRule? = null,
    val isRecording: Boolean = false,
    val recordingElapsedSec: Int = 0,
    val currentlyPlayingKey: String? = null,
    val hasNotificationPermission: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
    val isRescheduling: Boolean = false,
    val statusMessage: String? = null
)

private data class LocalStudioControls(
    val selectedTab: Int = 0,
    val selectedCategory: VedicAlertCategory? = null,
    val editingRule: NotificationRule? = null,
    val isRecording: Boolean = false,
    val recordingElapsedSec: Int = 0,
    val currentlyPlayingKey: String? = null,
    val hasNotificationPermission: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
    val isRescheduling: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class NotificationStudioViewModel @Inject constructor(
    private val notificationStudioRepository: NotificationStudioRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val alertEnginePort: AlertEnginePort
) : ViewModel() {

    private val localState = MutableStateFlow(
        LocalStudioControls(
            hasNotificationPermission = alertEnginePort.hasNotificationPermission(),
            canScheduleExactAlarms = alertEnginePort.canScheduleExactAlarms()
        )
    )

    private var recordingTimerJob: Job? = null

    private val repoFlow = combine(
        preferencesRepository.preferences,
        notificationStudioRepository.observeRules(),
        notificationStudioRepository.observeVoiceRecordings(),
        notificationStudioRepository.observeAudioSettings(),
        notificationStudioRepository.observeScheduledAlerts()
    ) { prefs, rules, recordings, audio, upcoming ->
        RepoSnapshot(prefs, rules, recordings, audio, upcoming)
    }

    val uiState: StateFlow<NotificationStudioUiState> = combine(
        repoFlow,
        notificationStudioRepository.observeRecentHistory(),
        localState
    ) { repo, history, local ->
        NotificationStudioUiState(
            preferences = repo.prefs,
            rules = repo.rules,
            voiceRecordings = repo.recordings,
            audioSettings = repo.audio,
            upcomingAlerts = repo.upcoming,
            recentHistory = history,
            selectedTab = local.selectedTab,
            selectedCategory = local.selectedCategory,
            editingRule = local.editingRule,
            isRecording = local.isRecording,
            recordingElapsedSec = local.recordingElapsedSec,
            currentlyPlayingKey = local.currentlyPlayingKey,
            hasNotificationPermission = local.hasNotificationPermission,
            canScheduleExactAlarms = local.canScheduleExactAlarms,
            isRescheduling = local.isRescheduling,
            statusMessage = local.statusMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationStudioUiState(
            hasNotificationPermission = alertEnginePort.hasNotificationPermission(),
            canScheduleExactAlarms = alertEnginePort.canScheduleExactAlarms()
        )
    )

    init {
        refreshPermissionsAndSchedule(silent = true)
    }

    fun selectTab(tab: Int) {
        localState.update { it.copy(selectedTab = tab.coerceIn(0, 2)) }
    }

    fun selectCategory(category: VedicAlertCategory?) {
        localState.update { it.copy(selectedCategory = category) }
    }

    fun openRuleEditor(rule: NotificationRule) {
        stopAudioPreview()
        localState.update { it.copy(editingRule = rule) }
    }

    fun openRuleByEventTypeName(eventTypeName: String): Boolean {
        val target = uiState.value.rules.firstOrNull { it.eventType.name == eventTypeName } ?: return false
        stopAudioPreview()
        localState.update { it.copy(selectedTab = 0, selectedCategory = null, editingRule = target) }
        return true
    }

    fun applyPreset(preset: NotificationPreset) {
        viewModelScope.launch {
            notificationStudioRepository.applyPreset(preset)
            val count = runCatching { alertEnginePort.rescheduleAllAlerts() }.getOrDefault(0)
            runCatching { alertEnginePort.syncRunningNowNotification() }
            val isHi = uiState.value.preferences.language == AppLanguage.HINDI
            val presetLabel = if (isHi) preset.titleHi else preset.titleEn
            val msg = if (isHi) {
                "प्रीसेट लागू: $presetLabel ($count अलर्ट निर्धारित)"
            } else {
                "Preset applied: $presetLabel ($count alerts scheduled)"
            }
            localState.update { it.copy(statusMessage = msg) }
        }
    }

    fun closeRuleEditor() {
        stopAudioPreview()
        localState.update { it.copy(editingRule = null) }
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (uiState.value.preferences.language == AppLanguage.HINDI) {
                AppLanguage.ENGLISH
            } else {
                AppLanguage.HINDI
            }
            preferencesRepository.setLanguage(next)
            rescheduleInternal(silent = true)
        }
    }

    fun toggleMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = uiState.value.audioSettings
            notificationStudioRepository.updateAudioSettings(current.copy(masterEnabled = enabled))
            rescheduleInternal(silent = false)
        }
    }

    fun togglePersistentRunningNotification(enabled: Boolean) {
        viewModelScope.launch {
            val current = notificationStudioRepository.getAudioSettings()
            notificationStudioRepository.updateAudioSettings(
                current.copy(persistentRunningNotificationEnabled = enabled)
            )
            runCatching { alertEnginePort.syncRunningNowNotification() }
            localState.update {
                it.copy(
                    hasNotificationPermission = alertEnginePort.hasNotificationPermission(),
                    canScheduleExactAlarms = alertEnginePort.canScheduleExactAlarms()
                )
            }
        }
    }

    fun toggleEventEnabled(eventType: VedicAlertEventType, enabled: Boolean) {
        viewModelScope.launch {
            notificationStudioRepository.setEventEnabled(eventType, enabled)
            rescheduleInternal(silent = true)
        }
    }

    fun saveRule(rule: NotificationRule) {
        viewModelScope.launch {
            stopAudioPreview()
            notificationStudioRepository.saveRule(rule)
            localState.update { it.copy(editingRule = null) }
            rescheduleInternal(silent = false)
        }
    }

    fun testRule(rule: NotificationRule) {
        viewModelScope.launch {
            val lang = uiState.value.preferences.language
            localState.update { it.copy(currentlyPlayingKey = "test_${rule.eventType.name}") }
            runCatching {
                alertEnginePort.fireTestAlert(rule, lang)
            }
            localState.update { it.copy(currentlyPlayingKey = null) }
        }
    }

    fun playTunePreview(key: String, tuneSource: TuneSourceType, tuneUriOrId: String) {
        if (localState.value.currentlyPlayingKey == key) {
            stopAudioPreview()
            return
        }
        localState.update { it.copy(currentlyPlayingKey = key) }
        alertEnginePort.playTunePreview(tuneSource, tuneUriOrId) {
            localState.update { current ->
                if (current.currentlyPlayingKey == key) current.copy(currentlyPlayingKey = null) else current
            }
        }
    }

    fun stopAudioPreview() {
        alertEnginePort.stopAudioPreview()
        localState.update { it.copy(currentlyPlayingKey = null) }
    }

    fun speakSampleVoice(isTattvaSample: Boolean) {
        val key = if (isTattvaSample) "tts_tattva" else "tts_nadi"
        if (localState.value.currentlyPlayingKey == key) {
            stopAudioPreview()
            return
        }
        val lang = uiState.value.preferences.language
        val audio = uiState.value.audioSettings
        val sampleText = when {
            isTattvaSample && lang == AppLanguage.HINDI ->
                "अग्नि तत्व और पिंगला नाड़ी शुरू हो गई है। वायु तत्व शुरू होने में ५ मिनट बाकी हैं।"
            isTattvaSample ->
                "Agni Tattva with Pingala Nadi has started. 5 minutes left for Vayu Tattva."
            lang == AppLanguage.HINDI ->
                "सूर्योदय इड़ा नाड़ी शुरू हो गई है।"
            else ->
                "Sunrise Ida Nadi has started."
        }
        localState.update { it.copy(currentlyPlayingKey = key) }
        alertEnginePort.speakSampleVoice(
            text = sampleText,
            language = lang,
            speechRate = audio.ttsSpeechRate,
            pitch = audio.ttsPitch
        ) {
            localState.update { current ->
                if (current.currentlyPlayingKey == key) current.copy(currentlyPlayingKey = null) else current
            }
        }
    }

    fun updateAudioSettings(updated: AlertAudioSettings, reschedule: Boolean = true) {
        viewModelScope.launch {
            notificationStudioRepository.updateAudioSettings(updated)
            if (reschedule) rescheduleInternal(silent = true)
        }
    }

    fun startRecording(clipName: String) {
        if (localState.value.isRecording) return
        stopAudioPreview()
        val result = alertEnginePort.startVoiceRecording(clipName)
        result.onSuccess {
            localState.update { it.copy(isRecording = true, recordingElapsedSec = 0) }
            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch {
                for (sec in 1..25) {
                    delay(1_000L)
                    if (!localState.value.isRecording) break
                    localState.update { it.copy(recordingElapsedSec = sec) }
                    if (sec >= 25) {
                        stopRecordingAndSave()
                    }
                }
            }
        }.onFailure { err ->
            localState.update { it.copy(statusMessage = err.message ?: "Unable to start microphone") }
        }
    }

    fun stopRecordingAndSave() {
        if (!localState.value.isRecording) return
        recordingTimerJob?.cancel()
        val result = alertEnginePort.stopVoiceRecording()
        localState.update { it.copy(isRecording = false, recordingElapsedSec = 0) }
        result.onSuccess { clip ->
            viewModelScope.launch {
                notificationStudioRepository.saveVoiceRecording(clip)
                val msg = if (uiState.value.preferences.language == AppLanguage.HINDI) {
                    "वॉयस क्लिप सहेजी गई: ${clip.displayName}"
                } else {
                    "Voice clip saved: ${clip.displayName}"
                }
                localState.update { it.copy(statusMessage = msg) }
            }
        }.onFailure {
            localState.update { it.copy(statusMessage = "Recording too short or failed") }
        }
    }

    fun cancelRecording() {
        recordingTimerJob?.cancel()
        alertEnginePort.cancelVoiceRecording()
        localState.update { it.copy(isRecording = false, recordingElapsedSec = 0) }
    }

    fun playRecording(recording: VoiceRecording) {
        val key = "rec_${recording.id}"
        if (localState.value.currentlyPlayingKey == key) {
            stopAudioPreview()
            return
        }
        localState.update { it.copy(currentlyPlayingKey = key) }
        alertEnginePort.playVoiceRecording(recording.filePath) {
            localState.update { current ->
                if (current.currentlyPlayingKey == key) current.copy(currentlyPlayingKey = null) else current
            }
        }
    }

    fun deleteRecording(recording: VoiceRecording) {
        viewModelScope.launch {
            if (localState.value.currentlyPlayingKey == "rec_${recording.id}") {
                stopAudioPreview()
            }
            alertEnginePort.deleteVoiceRecordingFile(recording.filePath)
            notificationStudioRepository.deleteVoiceRecording(recording.id)
            rescheduleInternal(silent = true)
        }
    }

    fun refreshPermissionsAndSchedule(silent: Boolean = false) {
        localState.update {
            it.copy(
                hasNotificationPermission = alertEnginePort.hasNotificationPermission(),
                canScheduleExactAlarms = alertEnginePort.canScheduleExactAlarms()
            )
        }
        viewModelScope.launch {
            rescheduleInternal(silent = silent)
        }
    }

    fun resetNotificationDefaults() {
        viewModelScope.launch {
            notificationStudioRepository.resetNotificationDefaults()
            rescheduleInternal(silent = false)
        }
    }

    fun clearStatusMessage() {
        localState.update { it.copy(statusMessage = null) }
    }

    private suspend fun rescheduleInternal(silent: Boolean) {
        localState.update { it.copy(isRescheduling = true) }
        val count = runCatching { alertEnginePort.rescheduleAllAlerts() }.getOrDefault(0)
        val lang = uiState.value.preferences.language
        val msg = if (silent) {
            localState.value.statusMessage
        } else if (lang == AppLanguage.HINDI) {
            "$count आगामी वैदिक सूचनाएँ निर्धारित की गईं"
        } else {
            "$count upcoming Vedic alerts scheduled"
        }
        localState.update { it.copy(isRescheduling = false, statusMessage = msg) }
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
        alertEnginePort.stopAudioPreview()
        alertEnginePort.cancelVoiceRecording()
    }

    private data class RepoSnapshot(
        val prefs: UserPreferences,
        val rules: List<NotificationRule>,
        val recordings: List<VoiceRecording>,
        val audio: AlertAudioSettings,
        val upcoming: List<ScheduledAlertItem>
    )
}
