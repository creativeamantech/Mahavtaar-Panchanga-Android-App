package com.mahavtaar.panchanga.domain.repository

import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.NotificationPreset
import com.mahavtaar.panchanga.domain.model.NotificationRule
import com.mahavtaar.panchanga.domain.model.ScheduledAlertItem
import com.mahavtaar.panchanga.domain.model.ScheduledAlertStatus
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import com.mahavtaar.panchanga.domain.model.VoiceRecording
import kotlinx.coroutines.flow.Flow

interface NotificationStudioRepository {
    fun observeRules(): Flow<List<NotificationRule>>
    suspend fun getAllRules(): List<NotificationRule>
    suspend fun getRule(eventType: VedicAlertEventType): NotificationRule
    suspend fun saveRule(rule: NotificationRule)
    suspend fun setEventEnabled(eventType: VedicAlertEventType, enabled: Boolean)
    suspend fun applyPreset(preset: NotificationPreset) = Unit

    fun observeVoiceRecordings(): Flow<List<VoiceRecording>>
    suspend fun getVoiceRecording(id: String): VoiceRecording?
    suspend fun saveVoiceRecording(recording: VoiceRecording)
    suspend fun renameVoiceRecording(id: String, newName: String)
    suspend fun deleteVoiceRecording(id: String)

    fun observeAudioSettings(): Flow<AlertAudioSettings>
    suspend fun getAudioSettings(): AlertAudioSettings
    suspend fun updateAudioSettings(settings: AlertAudioSettings)

    suspend fun getLastRunningTattvaKey(): String
    suspend fun setLastRunningTattvaKey(key: String)

    fun observeScheduledAlerts(): Flow<List<ScheduledAlertItem>>
    fun observeRecentHistory(): Flow<List<ScheduledAlertItem>>
    suspend fun getScheduledAlertById(id: String): ScheduledAlertItem?
    suspend fun replaceScheduledAlerts(alerts: List<ScheduledAlertItem>)
    suspend fun markAlertStatus(id: String, status: ScheduledAlertStatus, timestampMs: Long)
    suspend fun resetNotificationDefaults()
}
