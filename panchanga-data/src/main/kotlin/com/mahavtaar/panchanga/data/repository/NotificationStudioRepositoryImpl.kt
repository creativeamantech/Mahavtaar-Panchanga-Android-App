package com.mahavtaar.panchanga.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mahavtaar.panchanga.database.dao.NotificationRuleDao
import com.mahavtaar.panchanga.database.dao.ScheduledAlertDao
import com.mahavtaar.panchanga.database.dao.VoiceRecordingDao
import com.mahavtaar.panchanga.database.entity.NotificationRuleEntity
import com.mahavtaar.panchanga.database.entity.ScheduledAlertEntity
import com.mahavtaar.panchanga.database.entity.VoiceRecordingEntity
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AlertDeliveryMode
import com.mahavtaar.panchanga.domain.model.NotificationPreset
import com.mahavtaar.panchanga.domain.model.NotificationRule
import com.mahavtaar.panchanga.domain.model.ScheduledAlertItem
import com.mahavtaar.panchanga.domain.model.ScheduledAlertStatus
import com.mahavtaar.panchanga.domain.model.TuneSourceType
import com.mahavtaar.panchanga.domain.model.VedicAlertEventType
import com.mahavtaar.panchanga.domain.model.VoiceRecording
import com.mahavtaar.panchanga.domain.model.VoiceSourceType
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationAudioDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mahavtaar_notification_audio_prefs"
)

@Singleton
class NotificationStudioRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val ruleDao: NotificationRuleDao,
    private val voiceRecordingDao: VoiceRecordingDao,
    private val scheduledAlertDao: ScheduledAlertDao
) : NotificationStudioRepository {

    private object AudioKeys {
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        val TTS_RATE = floatPreferencesKey("tts_speech_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val MANUAL_DND = booleanPreferencesKey("manual_dnd_enabled")
        val MANUAL_DND_UNTIL_MS = longPreferencesKey("manual_dnd_until_ms")
        val QUIET_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START = intPreferencesKey("quiet_start_min")
        val QUIET_END = intPreferencesKey("quiet_end_min")
        val RESPECT_DND = booleanPreferencesKey("respect_silent_dnd")
        val PERSISTENT_RUNNING_NOW = booleanPreferencesKey("persistent_running_now_enabled")
        val LAST_RUNNING_TATTVA_KEY = stringPreferencesKey("last_running_tattva_key")
    }

    private val defaultAudio = AlertAudioSettings()

    override fun observeRules(): Flow<List<NotificationRule>> =
        ruleDao.observeAll().map { entities ->
            val byType = entities.associateBy { it.eventType }
            VedicAlertEventType.entries.map { type ->
                byType[type.name]?.toDomain() ?: type.defaultRule()
            }
        }

    override suspend fun getAllRules(): List<NotificationRule> {
        val byType = ruleDao.getAll().associateBy { it.eventType }
        return VedicAlertEventType.entries.map { type ->
            byType[type.name]?.toDomain() ?: type.defaultRule()
        }
    }

    override suspend fun getRule(eventType: VedicAlertEventType): NotificationRule {
        return ruleDao.getByEventType(eventType.name)?.toDomain() ?: eventType.defaultRule()
    }

    override suspend fun saveRule(rule: NotificationRule) {
        ruleDao.upsert(rule.toEntity())
    }

    override suspend fun setEventEnabled(eventType: VedicAlertEventType, enabled: Boolean) {
        val current = getRule(eventType)
        ruleDao.upsert(current.copy(isEnabled = enabled).toEntity())
    }

    override suspend fun applyPreset(preset: NotificationPreset) {
        val audio = getAudioSettings()
        if (preset == NotificationPreset.SILENT) {
            updateAudioSettings(audio.copy(masterEnabled = false))
            return
        }
        val enabledSet = preset.enabledEventTypes()
        val currentRules = getAllRules()
        currentRules.forEach { rule ->
            val shouldEnable = rule.eventType in enabledSet
            if (rule.isEnabled != shouldEnable) {
                ruleDao.upsert(rule.copy(isEnabled = shouldEnable).toEntity())
            } else {
                ruleDao.upsert(rule.toEntity())
            }
        }
        if (!audio.masterEnabled) {
            updateAudioSettings(audio.copy(masterEnabled = true))
        }
    }

    override fun observeVoiceRecordings(): Flow<List<VoiceRecording>> =
        voiceRecordingDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getVoiceRecording(id: String): VoiceRecording? =
        voiceRecordingDao.getById(id)?.toDomain()

    override suspend fun saveVoiceRecording(recording: VoiceRecording) {
        voiceRecordingDao.upsert(recording.toEntity())
    }

    override suspend fun renameVoiceRecording(id: String, newName: String) {
        voiceRecordingDao.rename(id, newName.trim().ifEmpty { "Voice Clip" })
    }

    override suspend fun deleteVoiceRecording(id: String) {
        voiceRecordingDao.deleteById(id)
    }

    override fun observeAudioSettings(): Flow<AlertAudioSettings> =
        context.notificationAudioDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { p ->
            AlertAudioSettings(
                masterEnabled = p[AudioKeys.MASTER_ENABLED] ?: defaultAudio.masterEnabled,
                ttsSpeechRate = (p[AudioKeys.TTS_RATE] ?: defaultAudio.ttsSpeechRate).coerceIn(0.6f, 1.6f),
                ttsPitch = (p[AudioKeys.TTS_PITCH] ?: defaultAudio.ttsPitch).coerceIn(0.7f, 1.4f),
                manualDndEnabled = p[AudioKeys.MANUAL_DND] ?: defaultAudio.manualDndEnabled,
                manualDndUntilMs = p[AudioKeys.MANUAL_DND_UNTIL_MS],
                quietHoursEnabled = p[AudioKeys.QUIET_ENABLED] ?: defaultAudio.quietHoursEnabled,
                quietStartMinuteOfDay = p[AudioKeys.QUIET_START] ?: defaultAudio.quietStartMinuteOfDay,
                quietEndMinuteOfDay = p[AudioKeys.QUIET_END] ?: defaultAudio.quietEndMinuteOfDay,
                respectSilentAndDnd = p[AudioKeys.RESPECT_DND] ?: defaultAudio.respectSilentAndDnd,
                persistentRunningNotificationEnabled = p[AudioKeys.PERSISTENT_RUNNING_NOW]
                    ?: defaultAudio.persistentRunningNotificationEnabled
            )
        }

    override suspend fun getAudioSettings(): AlertAudioSettings = observeAudioSettings().first()

    override suspend fun updateAudioSettings(settings: AlertAudioSettings) {
        context.notificationAudioDataStore.edit { p ->
            p[AudioKeys.MASTER_ENABLED] = settings.masterEnabled
            p[AudioKeys.TTS_RATE] = settings.ttsSpeechRate.coerceIn(0.6f, 1.6f)
            p[AudioKeys.TTS_PITCH] = settings.ttsPitch.coerceIn(0.7f, 1.4f)
            p[AudioKeys.MANUAL_DND] = settings.manualDndEnabled
            val untilMs = settings.manualDndUntilMs
            if (untilMs != null && untilMs > 0L && !settings.manualDndEnabled) {
                p[AudioKeys.MANUAL_DND_UNTIL_MS] = untilMs
            } else {
                p.remove(AudioKeys.MANUAL_DND_UNTIL_MS)
            }
            p[AudioKeys.QUIET_ENABLED] = settings.quietHoursEnabled
            p[AudioKeys.QUIET_START] = settings.quietStartMinuteOfDay.coerceIn(0, 1439)
            p[AudioKeys.QUIET_END] = settings.quietEndMinuteOfDay.coerceIn(0, 1439)
            p[AudioKeys.RESPECT_DND] = settings.respectSilentAndDnd
            p[AudioKeys.PERSISTENT_RUNNING_NOW] = settings.persistentRunningNotificationEnabled
        }
    }

    override suspend fun getLastRunningTattvaKey(): String =
        context.notificationAudioDataStore.data
            .catch { emit(emptyPreferences()) }
            .first()[AudioKeys.LAST_RUNNING_TATTVA_KEY]
            .orEmpty()

    override suspend fun setLastRunningTattvaKey(key: String) {
        context.notificationAudioDataStore.edit { p ->
            p[AudioKeys.LAST_RUNNING_TATTVA_KEY] = key
        }
    }

    override fun observeScheduledAlerts(): Flow<List<ScheduledAlertItem>> =
        scheduledAlertDao.observeUpcomingScheduled().map { list -> list.mapNotNull { it.toDomainOrNull() } }

    override fun observeRecentHistory(): Flow<List<ScheduledAlertItem>> =
        scheduledAlertDao.observeRecentHistory().map { list -> list.mapNotNull { it.toDomainOrNull() } }

    override suspend fun getScheduledAlertById(id: String): ScheduledAlertItem? =
        scheduledAlertDao.getById(id)?.toDomainOrNull()

    override suspend fun replaceScheduledAlerts(alerts: List<ScheduledAlertItem>) {
        scheduledAlertDao.clearScheduledQueue()
        if (alerts.isNotEmpty()) {
            scheduledAlertDao.upsertAll(alerts.map { it.toEntity() })
        }
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24L * 3600_000L
        scheduledAlertDao.pruneOldHistory(sevenDaysAgo)
    }

    override suspend fun markAlertStatus(id: String, status: ScheduledAlertStatus, timestampMs: Long) {
        scheduledAlertDao.updateStatus(id, status.name, timestampMs)
    }

    override suspend fun resetNotificationDefaults() {
        ruleDao.deleteAll()
        updateAudioSettings(defaultAudio)
    }

    private fun NotificationRuleEntity.toDomain(): NotificationRule {
        val type = VedicAlertEventType.fromName(eventType) ?: VedicAlertEventType.HORA_SUB_TATTVA
        return NotificationRule(
            eventType = type,
            isEnabled = isEnabled,
            notifyAtStart = notifyAtStart,
            leadTimeMinutes = leadTimeMinutes.coerceIn(0, 180),
            deliveryMode = runCatching { AlertDeliveryMode.valueOf(deliveryMode) }.getOrDefault(type.defaultDeliveryMode),
            tuneSource = runCatching { TuneSourceType.valueOf(tuneSource) }.getOrDefault(TuneSourceType.BUILTIN_VEDIC),
            tuneUriOrId = tuneUriOrId,
            tuneDisplayName = tuneDisplayName,
            voiceSource = runCatching { VoiceSourceType.valueOf(voiceSource) }.getOrDefault(VoiceSourceType.BUILTIN_TTS),
            voiceRecordingId = voiceRecordingId,
            customHindiTemplate = customHindiTemplate,
            customEnglishTemplate = customEnglishTemplate
        )
    }

    private fun NotificationRule.toEntity(): NotificationRuleEntity = NotificationRuleEntity(
        eventType = eventType.name,
        isEnabled = isEnabled,
        notifyAtStart = notifyAtStart,
        leadTimeMinutes = leadTimeMinutes.coerceIn(0, 180),
        deliveryMode = deliveryMode.name,
        tuneSource = tuneSource.name,
        tuneUriOrId = tuneUriOrId,
        tuneDisplayName = tuneDisplayName,
        voiceSource = voiceSource.name,
        voiceRecordingId = voiceRecordingId,
        customHindiTemplate = customHindiTemplate,
        customEnglishTemplate = customEnglishTemplate
    )

    private fun VoiceRecordingEntity.toDomain(): VoiceRecording = VoiceRecording(
        id = id,
        displayName = displayName,
        filePath = filePath,
        durationMs = durationMs,
        createdAtMs = createdAtMs
    )

    private fun VoiceRecording.toEntity(): VoiceRecordingEntity = VoiceRecordingEntity(
        id = id,
        displayName = displayName,
        filePath = filePath,
        durationMs = durationMs,
        createdAtMs = createdAtMs
    )

    private fun ScheduledAlertEntity.toDomainOrNull(): ScheduledAlertItem? {
        val type = VedicAlertEventType.fromName(eventType) ?: return null
        return ScheduledAlertItem(
            id = id,
            requestCode = requestCode,
            eventType = type,
            triggerTimeMs = triggerTimeMs,
            eventStartTimeMs = eventStartTimeMs,
            eventEndTimeMs = eventEndTimeMs,
            isAdvanceWarning = isAdvanceWarning,
            leadTimeMinutes = leadTimeMinutes,
            titleEn = titleEn,
            titleHi = titleHi,
            bodyEn = bodyEn,
            bodyHi = bodyHi,
            speechEn = speechEn,
            speechHi = speechHi,
            deliveryMode = runCatching { AlertDeliveryMode.valueOf(deliveryMode) }.getOrDefault(AlertDeliveryMode.TUNE_AND_VOICE),
            tuneSource = runCatching { TuneSourceType.valueOf(tuneSource) }.getOrDefault(TuneSourceType.BUILTIN_VEDIC),
            tuneUriOrId = tuneUriOrId,
            voiceSource = runCatching { VoiceSourceType.valueOf(voiceSource) }.getOrDefault(VoiceSourceType.BUILTIN_TTS),
            voiceRecordingId = voiceRecordingId,
            status = runCatching { ScheduledAlertStatus.valueOf(status) }.getOrDefault(ScheduledAlertStatus.SCHEDULED),
            updatedAtMs = updatedAtMs
        )
    }

    private fun ScheduledAlertItem.toEntity(): ScheduledAlertEntity = ScheduledAlertEntity(
        id = id,
        requestCode = requestCode,
        eventType = eventType.name,
        triggerTimeMs = triggerTimeMs,
        eventStartTimeMs = eventStartTimeMs,
        eventEndTimeMs = eventEndTimeMs,
        isAdvanceWarning = isAdvanceWarning,
        leadTimeMinutes = leadTimeMinutes,
        titleEn = titleEn,
        titleHi = titleHi,
        bodyEn = bodyEn,
        bodyHi = bodyHi,
        speechEn = speechEn,
        speechHi = speechHi,
        deliveryMode = deliveryMode.name,
        tuneSource = tuneSource.name,
        tuneUriOrId = tuneUriOrId,
        voiceSource = voiceSource.name,
        voiceRecordingId = voiceRecordingId,
        status = status.name,
        updatedAtMs = updatedAtMs
    )
}
