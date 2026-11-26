package com.mahavtaar.panchanga.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val population: Long,
    val isGpsResolved: Boolean,
    val savedAtMs: Long,
    val isCustom: Boolean = false,
    val isDefault: Boolean = false,
    val accuracyMeters: Double? = null
)

@Entity(tableName = "cached_panchanga")
data class CachedPanchangaEntity(
    @PrimaryKey val cacheKey: String,
    val dateIso: String,
    val cityName: String,
    val payloadJson: String,
    val updatedAtMs: Long
)

@Entity(tableName = "vrat_reminders")
data class VratReminderEntity(
    @PrimaryKey val reminderKey: String,
    val title: String,
    val dateIso: String,
    val notifyAtMs: Long,
    val enabled: Boolean
)

@Entity(tableName = "navtara_profiles")
data class NavtaraProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val birthNakshatraIndex: Int,
    val birthPada: Int,
    val notes: String,
    val isDefault: Boolean,
    val updatedAtMs: Long
)

@Entity(
    tableName = "navtara_daily_feedback",
    primaryKeys = ["profileId", "dateIso"]
)
data class NavtaraFeedbackEntity(
    val profileId: Long,
    val dateIso: String,
    val rating: String,
    val note: String,
    val updatedAtMs: Long
)

@Entity(tableName = "saved_navtara_reports")
data class SavedNavtaraReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val profileId: Long,
    val profileName: String,
    val birthNakshatraIndex: Int,
    val birthPada: Int,
    val startDateIso: String,
    val endDateIso: String,
    val isDateRange: Boolean,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val ayanamshaKey: String,
    val monthSystemKey: String,
    val auspiciousCount: Int,
    val mixedCount: Int,
    val cautionCount: Int,
    val neutralCount: Int,
    val notes: String,
    val createdAtMs: Long
)

@Entity(tableName = "saved_kundli_charts")
data class SavedKundliChartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val birthHour: Int,
    val birthMinute: Int,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val inputJson: String,
    val lagnaSignIndex: Int,
    val moonSignIndex: Int,
    val moonNakshatraNumber: Int,
    val createdAtMs: Long,
    val updatedAtMs: Long
)

@Entity(tableName = "cached_user_access")
data class CachedUserAccessEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val displayName: String?,
    val roleName: String,
    val statusName: String,
    val allowedPagesCsv: String,
    val allowedFeaturesCsv: String,
    val authorizedByUid: String?,
    val sessionToken: String,
    val updatedAtMs: Long
)
