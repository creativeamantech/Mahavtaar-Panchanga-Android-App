package com.mahavtaar.panchanga.domain.model

import kotlinx.serialization.Serializable

data class CloudUser(
    val uid: String,
    val displayName: String? = null,
    val email: String? = null
)

data class CloudSyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncedAtMs: Long? = null,
    val lastError: String? = null
)

data class GoogleDriveBackupStatus(
    val isDriveLinked: Boolean = false,
    val isDriveOAuthAuthorized: Boolean = false,
    val linkedDriveUri: String? = null,
    val linkedFileName: String? = null,
    val driveFileId: String? = null,
    val lastReplacedOldFileId: String? = null,
    val localVaultPath: String? = null,
    val navtaraProfileCount: Int = 0,
    val navtaraFeedbackCount: Int = 0,
    val savedReportCount: Int = 0,
    val isBackingUp: Boolean = false,
    val lastDriveSyncAtMs: Long? = null,
    val lastLocalVaultAtMs: Long? = null,
    val lastError: String? = null,
    val syncedAccountEmail: String? = null,
    val isOAuthBlockedByConsentScreen: Boolean = false,
    val lastPullFromDriveAtMs: Long? = null,
    val totalSyncedRecords: Int = 0,
    val hasPendingOfflineChanges: Boolean = false
)

@Serializable
data class UserFullBackupBundle(
    val schemaVersion: Int = 3,
    val userUid: String = "",
    val userEmail: String? = null,
    val userDisplayName: String? = null,
    val exportedAtMs: Long = 0L,
    val preferences: UserPreferences = UserPreferences(),
    val savedLocations: List<CityLocation> = emptyList(),
    val savedKundliCharts: List<SavedKundliChart> = emptyList(),
    val savedKundliProfiles: List<KundliBirthInput> = emptyList(),
    val navtaraProfiles: List<NavtaraProfile> = emptyList(),
    val navtaraFeedbacks: List<NavtaraDailyFeedback> = emptyList(),
    val savedNavtaraReports: List<SavedNavtaraReport> = emptyList(),
    val notificationRules: List<NotificationRule> = emptyList(),
    val alertAudioSettings: AlertAudioSettings = AlertAudioSettings()
)

@Serializable
data class NavtaraJsonBackupBundle(
    val schemaVersion: Int = 1,
    val format: String = "mahavtaar_navtara_json",
    val userUid: String = "",
    val userEmail: String? = null,
    val userDisplayName: String? = null,
    val exportedAtMs: Long = 0L,
    val activeProfileId: Long? = null,
    val profiles: List<NavtaraProfile> = emptyList(),
    val feedbacks: List<NavtaraDailyFeedback> = emptyList(),
    val savedReports: List<SavedNavtaraReport> = emptyList(),
    val activePeriodReport: NavtaraPeriodReport? = null
)
