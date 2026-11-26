package com.mahavtaar.panchanga.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_rules")
data class NotificationRuleEntity(
    @PrimaryKey val eventType: String,
    val isEnabled: Boolean,
    val notifyAtStart: Boolean,
    val leadTimeMinutes: Int,
    val deliveryMode: String,
    val tuneSource: String,
    val tuneUriOrId: String,
    val tuneDisplayName: String,
    val voiceSource: String,
    val voiceRecordingId: String?,
    val customHindiTemplate: String,
    val customEnglishTemplate: String
)

@Entity(tableName = "voice_recordings")
data class VoiceRecordingEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val filePath: String,
    val durationMs: Long,
    val createdAtMs: Long
)

@Entity(tableName = "scheduled_alerts")
data class ScheduledAlertEntity(
    @PrimaryKey val id: String,
    val requestCode: Int,
    val eventType: String,
    val triggerTimeMs: Long,
    val eventStartTimeMs: Long,
    val eventEndTimeMs: Long? = null,
    val isAdvanceWarning: Boolean,
    val leadTimeMinutes: Int,
    val titleEn: String,
    val titleHi: String,
    val bodyEn: String,
    val bodyHi: String,
    val speechEn: String,
    val speechHi: String,
    val deliveryMode: String,
    val tuneSource: String,
    val tuneUriOrId: String,
    val voiceSource: String,
    val voiceRecordingId: String?,
    val status: String,
    val updatedAtMs: Long
)
