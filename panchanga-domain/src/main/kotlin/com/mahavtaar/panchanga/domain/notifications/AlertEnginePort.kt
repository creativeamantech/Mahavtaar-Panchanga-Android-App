package com.mahavtaar.panchanga.domain.notifications

import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.NotificationRule
import com.mahavtaar.panchanga.domain.model.TuneSourceType
import com.mahavtaar.panchanga.domain.model.VoiceRecording

/**
 * Abstraction exposed to feature modules so Notification Studio UI can trigger
 * alarm horizon rescheduling, live tune/voice previews, voice recording, and test alerts
 * without depending directly on the :app module.
 */
interface AlertEnginePort {
    fun canScheduleExactAlarms(): Boolean
    fun hasNotificationPermission(): Boolean
    suspend fun rescheduleAllAlerts(): Int
    suspend fun syncRunningNowNotification()
    suspend fun fireTestAlert(rule: NotificationRule, language: AppLanguage)

    fun playTunePreview(tuneSource: TuneSourceType, tuneUriOrId: String, onCompleted: () -> Unit = {})
    fun stopAudioPreview()
    fun speakSampleVoice(text: String, language: AppLanguage, speechRate: Float, pitch: Float, onCompleted: () -> Unit = {})

    fun startVoiceRecording(displayName: String): Result<String>
    fun stopVoiceRecording(): Result<VoiceRecording>
    fun cancelVoiceRecording()
    fun playVoiceRecording(filePath: String, onCompleted: () -> Unit = {})
    fun deleteVoiceRecordingFile(filePath: String)
}
