package com.mahavtaar.panchanga.data.backup

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.mahavtaar.panchanga.domain.model.CloudUser
import com.mahavtaar.panchanga.domain.model.GoogleDriveBackupStatus
import com.mahavtaar.panchanga.domain.model.NavtaraDailyFeedback
import com.mahavtaar.panchanga.domain.model.NavtaraJsonBackupBundle
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodReport
import com.mahavtaar.panchanga.domain.model.NavtaraProfile
import com.mahavtaar.panchanga.domain.model.UserFullBackupBundle
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.CloudAccountRepository
import com.mahavtaar.panchanga.domain.repository.LocationRepository
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified, 100% user-owned Google Drive two-way auto-sync & local vault manager
 * (identical for regular users and admins — zero personal user data is sent to developer servers).
 *
 * Works like AppSheet's automatic Google Drive sync:
 * 1. **Pull-before-push on sign-in / reconnect / new device:** Checks the signed-in user's OWN
 *    Google Drive (`drive.file` + hidden `appDataFolder` safety mirror, or linked SAF Drive URI)
 *    and restores newer Drive data before uploading, preventing empty installs from overwriting backups.
 * 2. **Automatic debounced push on every local edit:** Persists every change across preferences,
 *    locations, Navtara profiles/feedbacks/reports, Kundli charts/birth profiles, notification rules,
 *    DND/Quiet Hours audio settings, and widget glass customization to both the per-user on-device
 *    JSON vault and the signed-in user's own Google Drive.
 * 3. **Offline resilience:** Marks pending edits locally when offline and pushes automatically
 *    once connectivity returns.
 */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class GoogleDriveBackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository,
    private val astrologyRepository: AstrologyRepository,
    private val notificationStudioRepository: NotificationStudioRepository,
    private val cloudAccountRepository: CloudAccountRepository,
    private val driveRestClient: GoogleDriveRestClient
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val backupMutex = Mutex()
    private val profileSeedMutex = Mutex()
    private val isRestoring = AtomicBoolean(false)
    private val prefs = context.getSharedPreferences("mahavtaar_gdrive_backup_prefs", Context.MODE_PRIVATE)
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedTokenAccountEmail: String? = null

    @Volatile
    private var pulledSessionUid: String? = null

    @Volatile
    private var oauthBlockedByConsent: Boolean = false

    private val _status = MutableStateFlow(GoogleDriveBackupStatus())
    val status: StateFlow<GoogleDriveBackupStatus> = _status.asStateFlow()

    init {
        observeUserAndAutoBackup()
    }

    private fun observeUserAndAutoBackup() {
        scope.launch {
            cloudAccountRepository.currentUser.collect { user ->
                val incomingEmail = user?.email?.trim()?.lowercase()
                if (cachedTokenAccountEmail != incomingEmail) {
                    cachedAccessToken = null
                    cachedTokenAccountEmail = incomingEmail
                    pulledSessionUid = null
                    oauthBlockedByConsent = false
                }
                refreshStatusForUser(user)
                if (user != null) {
                    runCatching { ensureDefaultGoogleProfileIfNeeded(user) }
                }
            }
        }

        scope.launch {
            val allFeedbacksFlow = astrologyRepository.observeNavtaraProfiles()
                .flatMapLatest { profiles ->
                    val valid = profiles.filter { it.id > 0L }
                    if (valid.isEmpty()) flowOf(emptyList<NavtaraDailyFeedback>())
                    else combine(valid.map { astrologyRepository.observeNavtaraFeedbacks(it.id) }) { arr ->
                        arr.flatMap { it }
                    }
                }

            val groupA = combine(
                cloudAccountRepository.currentUser,
                preferencesRepository.preferences,
                locationRepository.observeSavedLocations(),
                astrologyRepository.observeSavedKundliCharts(),
                astrologyRepository.observeNavtaraProfiles()
            ) { user, userPrefs, locations, kundlis, profiles ->
                "${user?.uid.orEmpty()}|${userPrefs.hashCode()}|${locations.hashCode()}|${kundlis.hashCode()}|${profiles.hashCode()}"
            }

            val groupB = combine(
                allFeedbacksFlow,
                astrologyRepository.observeSavedNavtaraReports(),
                notificationStudioRepository.observeRules(),
                notificationStudioRepository.observeAudioSettings(),
                preferencesRepository.observeSavedKundliProfiles()
            ) { feedbacks, reports, rules, audio, kundliProfiles ->
                "${feedbacks.hashCode()}|${reports.hashCode()}|${rules.hashCode()}|${audio.hashCode()}|${kundliProfiles.hashCode()}"
            }

            combine(groupA, groupB) { a, b -> "$a|$b" }
                .distinctUntilChanged()
                .debounce(900L)
                .collect {
                    if (!isRestoring.get()) {
                        runCatching { performAutoSaveAndDriveSync() }
                    }
                }
        }
    }

    /**
     * Called when an authenticated user (regular user OR admin) enters the app.
     * Attempts silent Drive reconnection + pull-before-push so any data stored in the user's
     * own Google Drive is synced onto this device immediately, and any pending offline edits
     * are pushed safely back to their Drive.
     */
    suspend fun startUserDriveSyncSession(uid: String) = withContext(Dispatchers.IO) {
        if (uid.isBlank()) return@withContext
        val user = cloudAccountRepository.currentUser.first()
        if (user == null || user.uid != uid) return@withContext

        // Probe silent Drive OAuth authorization for this exact user's Google account
        if (!prefs.getBoolean("drive_oauth_granted_$uid", false)) {
            runCatching {
                val auth = driveRestClient.authorizeForDrive(activity = null, accountEmail = user.email)
                if (auth is DriveAuthState.Authorized) {
                    cachedAccessToken = auth.accessToken
                    cachedTokenAccountEmail = user.email?.trim()?.lowercase()
                    oauthBlockedByConsent = false
                    prefs.edit()
                        .putBoolean("drive_oauth_granted_$uid", true)
                        .putString("drive_name_$uid", suggestedDriveFileName(user))
                        .apply()
                }
            }
        }

        // Pull newer Drive state first (AppSheet-style), then push merged/pending changes
        syncBidirectionalWithGoogleDrive(forcePullFirst = false)
    }

    /**
     * Clears cached OAuth tokens and session state immediately on sign-out so credentials
     * are never reused across accounts on shared devices.
     */
    fun onUserSignedOut() {
        cachedAccessToken = null
        cachedTokenAccountEmail = null
        pulledSessionUid = null
        oauthBlockedByConsent = false
        _status.value = GoogleDriveBackupStatus()
    }

    /**
     * Automatically creates a default birth profile initialized with the signed-in Google
     * account's name on first sign-in when no profiles exist yet.
     */
    suspend fun ensureDefaultGoogleProfileIfNeeded(user: CloudUser?): Boolean = withContext(Dispatchers.IO) {
        user ?: return@withContext false
        val uidKey = user.uid.takeIf { it.isNotBlank() } ?: return@withContext false
        profileSeedMutex.withLock {
            val alreadySeeded = prefs.getBoolean("auto_profile_seeded_$uidKey", false)
            val existing = astrologyRepository.observeNavtaraProfiles().first()
            if (alreadySeeded || existing.isNotEmpty()) {
                return@withLock false
            }
            val cleanName = user.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")?.trim()?.takeIf { it.isNotBlank() }
                ?: return@withLock false
            val userPrefs = preferencesRepository.preferences.first()
            astrologyRepository.saveNavtaraProfile(
                NavtaraProfile(
                    name = cleanName,
                    birthNakshatraIndex = userPrefs.birthNakshatra.coerceIn(1, 27),
                    birthPada = 1,
                    notes = user.email.orEmpty(),
                    isDefault = true,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
            prefs.edit().putBoolean("auto_profile_seeded_$uidKey", true).apply()
            true
        }
    }

    private suspend fun refreshStatusForUser(user: CloudUser?) {
        val uidKey = user?.uid?.takeIf { it.isNotBlank() } ?: "local_device"
        val uriStr = prefs.getString("drive_uri_$uidKey", null)
        val oauthAuthorized = prefs.getBoolean("drive_oauth_granted_$uidKey", false)
        val fileName = prefs.getString("drive_name_$uidKey", null) ?: suggestedDriveFileName(user)
        val driveFileId = prefs.getString("drive_file_id_$uidKey", null)
        val lastReplacedOldId = prefs.getString("drive_last_replaced_old_id_$uidKey", null)
        val lastDriveMs = prefs.getLong("drive_last_sync_$uidKey", 0L).takeIf { it > 0L }
        val lastLocalMs = prefs.getLong("local_last_sync_$uidKey", 0L).takeIf { it > 0L }
        val lastPullMs = prefs.getLong("drive_last_pull_$uidKey", 0L).takeIf { it > 0L }
        val pendingOffline = prefs.getBoolean("drive_pending_push_$uidKey", false)
        val vaultPath = localVaultFile(uidKey).absolutePath

        val profiles = runCatching { astrologyRepository.observeNavtaraProfiles().first() }.getOrDefault(emptyList())
        val feedbackCount = runCatching {
            profiles.filter { it.id > 0L }.sumOf { p ->
                astrologyRepository.observeNavtaraFeedbacks(p.id).first().size
            }
        }.getOrDefault(0)
        val reportCount = runCatching { astrologyRepository.observeSavedNavtaraReports().first().size }.getOrDefault(0)
        val locationCount = runCatching { locationRepository.observeSavedLocations().first().size }.getOrDefault(0)
        val kundliCount = runCatching { astrologyRepository.observeSavedKundliCharts().first().size }.getOrDefault(0)
        val kundliProfileCount = runCatching { preferencesRepository.observeSavedKundliProfiles().first().size }.getOrDefault(0)
        val totalRecords = profiles.size + feedbackCount + reportCount + locationCount + kundliCount + kundliProfileCount

        _status.value = GoogleDriveBackupStatus(
            isDriveLinked = oauthAuthorized || !uriStr.isNullOrBlank(),
            isDriveOAuthAuthorized = oauthAuthorized,
            linkedDriveUri = uriStr,
            linkedFileName = fileName,
            driveFileId = driveFileId,
            lastReplacedOldFileId = lastReplacedOldId,
            localVaultPath = vaultPath,
            navtaraProfileCount = profiles.size,
            navtaraFeedbackCount = feedbackCount,
            savedReportCount = reportCount,
            isBackingUp = false,
            lastDriveSyncAtMs = lastDriveMs,
            lastLocalVaultAtMs = lastLocalMs,
            lastError = _status.value.lastError,
            syncedAccountEmail = user?.email,
            isOAuthBlockedByConsentScreen = oauthBlockedByConsent,
            lastPullFromDriveAtMs = lastPullMs,
            totalSyncedRecords = totalRecords,
            hasPendingOfflineChanges = pendingOffline
        )
    }

    fun suggestedDriveFileName(user: CloudUser?): String {
        val rawTag = user?.email?.substringBefore("@")
            ?.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            ?.takeIf { it.isNotBlank() }
            ?: user?.uid?.take(8)
            ?: "user"
        return "mahavtaar_panchanga_backup_$rawTag.json"
    }

    /**
     * Checks or requests OAuth consent strictly for the signed-in user's own Google Drive
     * (`drive.file` + `drive.appdata`). Returns `null` if already authorized silently and
     * immediately runs bidirectional pull-then-push sync; returns a [PendingIntent] when
     * Android's OAuth resolution sheet must be launched by UI.
     */
    suspend fun authorizeDriveWithResolution(activity: Activity?): Result<PendingIntent?> = withContext(Dispatchers.IO) {
        try {
            val user = cloudAccountRepository.currentUser.first()
            val uidKey = user?.uid?.takeIf { it.isNotBlank() } ?: "local_device"
            when (val auth = driveRestClient.authorizeForDrive(activity, user?.email)) {
                is DriveAuthState.Authorized -> {
                    cachedAccessToken = auth.accessToken
                    cachedTokenAccountEmail = user?.email?.trim()?.lowercase()
                    oauthBlockedByConsent = false
                    prefs.edit()
                        .putBoolean("drive_oauth_granted_$uidKey", true)
                        .putString("drive_name_$uidKey", suggestedDriveFileName(user))
                        .apply()
                    refreshStatusForUser(user)
                    syncBidirectionalWithGoogleDrive(forcePullFirst = true).getOrThrow()
                    Result.success(null)
                }
                is DriveAuthState.NeedsResolution -> Result.success(auth.pendingIntent)
            }
        } catch (e: DriveOAuthConsentBlockedException) {
            oauthBlockedByConsent = true
            _status.value = _status.value.copy(
                isOAuthBlockedByConsentScreen = true,
                lastError = e.localizedMessage
            )
            Result.failure(e)
        } catch (e: Exception) {
            _status.value = _status.value.copy(lastError = e.localizedMessage)
            Result.failure(e)
        }
    }

    suspend fun handleDriveResolutionResult(
        data: Intent?,
        resultCode: Int = Activity.RESULT_OK
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = driveRestClient.extractTokenOrThrow(data, resultCode)
            val user = cloudAccountRepository.currentUser.first()
            val uidKey = user?.uid?.takeIf { it.isNotBlank() } ?: "local_device"
            cachedAccessToken = token
            cachedTokenAccountEmail = user?.email?.trim()?.lowercase()
            oauthBlockedByConsent = false
            prefs.edit()
                .putBoolean("drive_oauth_granted_$uidKey", true)
                .putString("drive_name_$uidKey", suggestedDriveFileName(user))
                .apply()
            refreshStatusForUser(user)
            syncBidirectionalWithGoogleDrive(forcePullFirst = true)
        } catch (e: DriveOAuthConsentBlockedException) {
            oauthBlockedByConsent = true
            _status.value = _status.value.copy(
                isOAuthBlockedByConsentScreen = true,
                lastError = e.localizedMessage
            )
            Result.failure(e)
        } catch (e: Exception) {
            _status.value = _status.value.copy(lastError = e.localizedMessage)
            Result.failure(e)
        }
    }

    /**
     * Links a user-chosen Google Drive document URI (Android Storage Access Framework — zero OAuth
     * consent screen required) to the signed-in Google account. Pulls existing data from that file
     * first if it contains a valid backup for this account, then writes the merged canonical JSON.
     */
    suspend fun linkGoogleDriveBackupUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
            val user = cloudAccountRepository.currentUser.first()
            val uidKey = user?.uid?.takeIf { it.isNotBlank() } ?: "local_device"
            val displayName = resolveDisplayName(uri) ?: suggestedDriveFileName(user)

            oauthBlockedByConsent = false
            prefs.edit()
                .putString("drive_uri_$uidKey", uri.toString())
                .putString("drive_name_$uidKey", displayName)
                .apply()

            refreshStatusForUser(user)
            syncBidirectionalWithGoogleDrive(forcePullFirst = true).getOrThrow()
            displayName
        }
    }

    /**
     * Full AppSheet-style two-way sync against the signed-in user's OWN Google Drive:
     * 1. Pull & merge newer remote data from user's Drive (`drive.file`, `appDataFolder`, or SAF URI).
     * 2. Write the canonical merged JSON to the per-user local vault AND user's own Google Drive.
     * Works identically for both regular users and admins.
     */
    suspend fun syncBidirectionalWithGoogleDrive(
        forcePullFirst: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            try {
                _status.value = _status.value.copy(isBackingUp = true, lastError = null)
                val user = cloudAccountRepository.currentUser.first()
                val uidKey = user?.uid?.takeIf { it.isNotBlank() } ?: "local_device"
                val targetFileName = prefs.getString("drive_name_$uidKey", null) ?: suggestedDriveFileName(user)
                val uriStr = prefs.getString("drive_uri_$uidKey", null)
                val oauthGranted = prefs.getBoolean("drive_oauth_granted_$uidKey", false)

                // STEP 1: Pull-before-push if Drive is connected and either forced or not yet pulled in session
                val shouldCheckRemote = forcePullFirst || pulledSessionUid != uidKey
                if (shouldCheckRemote && (oauthGranted || !uriStr.isNullOrBlank())) {
                    pullFromDriveLockedIfNewer(
                        user = user,
                        uidKey = uidKey,
                        targetFileName = targetFileName,
                        oauthGranted = oauthGranted,
                        uriStr = uriStr,
                        forceApply = forcePullFirst
                    )
                    pulledSessionUid = uidKey
                }

                // STEP 2: Push canonical merged bundle to local vault + user's own Google Drive
                val pushResult = pushCurrentStateLocked(user = user, uidKey = uidKey)
                pushResult
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Google Drive sync failed"
                _status.value = _status.value.copy(isBackingUp = false, lastError = msg)
                Result.failure(e)
            }
        }
    }

    suspend fun syncToGoogleDriveNow(): Result<Unit> = syncBidirectionalWithGoogleDrive(forcePullFirst = false)

    private suspend fun pullFromDriveLockedIfNewer(
        user: CloudUser?,
        uidKey: String,
        targetFileName: String,
        oauthGranted: Boolean,
        uriStr: String?,
        forceApply: Boolean
    ) {
        val rawRemoteJson: String? = when {
            oauthGranted -> {
                runCatching {
                    val token = obtainSilentAccessToken(user)
                    val downloaded = driveRestClient.tryDownloadLatestJsonFromDriveOrAppData(token, targetFileName)
                    if (downloaded != null) {
                        prefs.edit().putString("drive_file_id_$uidKey", downloaded.first).apply()
                    }
                    downloaded?.second
                }.onFailure { err ->
                    if (err is DriveUnauthorizedException) {
                        cachedAccessToken = null
                        prefs.edit().putBoolean("drive_oauth_granted_$uidKey", false).apply()
                    }
                }.getOrNull()
            }
            !uriStr.isNullOrBlank() -> {
                runCatching { readTextFromUri(Uri.parse(uriStr)) }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
            }
            else -> null
        }

        if (rawRemoteJson.isNullOrBlank()) return

        val remoteBundle = runCatching {
            json.decodeFromString<UserFullBackupBundle>(rawRemoteJson.trim())
        }.getOrNull() ?: return

        // Strict account ownership check: never apply another user's Drive backup
        if (user != null && remoteBundle.userUid.isNotBlank() && remoteBundle.userUid != user.uid) {
            return
        }

        val lastRemoteExportedMs = prefs.getLong("drive_remote_exported_at_$uidKey", 0L)
        val lastLocalMutationMs = prefs.getLong("local_last_mutation_$uidKey", 0L)
        val localLooksFresh = lastLocalMutationMs == 0L &&
            runCatching { locationRepository.observeSavedLocations().first().isEmpty() }.getOrDefault(true) &&
            runCatching { astrologyRepository.observeSavedKundliCharts().first().isEmpty() }.getOrDefault(true) &&
            runCatching { astrologyRepository.observeSavedNavtaraReports().first().isEmpty() }.getOrDefault(true)

        val remoteHasNewerTimestamp = remoteBundle.exportedAtMs > lastRemoteExportedMs &&
            remoteBundle.exportedAtMs >= lastLocalMutationMs

        if (forceApply || localLooksFresh || remoteHasNewerTimestamp) {
            isRestoring.set(true)
            try {
                applyBundleToDevice(remoteBundle)
                val nowMs = System.currentTimeMillis()
                prefs.edit()
                    .putLong("drive_last_pull_$uidKey", nowMs)
                    .putLong("drive_remote_exported_at_$uidKey", remoteBundle.exportedAtMs.coerceAtLeast(nowMs))
                    .apply()
            } finally {
                isRestoring.set(false)
            }
        }
    }

    private suspend fun pushCurrentStateLocked(user: CloudUser?, uidKey: String): Result<Unit> {
        val bundle = buildCurrentBundle(user)
        val encoded = json.encodeToString(bundle)
        val nowMs = bundle.exportedAtMs

        // 1) Always write to on-device per-user JSON vault
        writeLocalVaultFile(uidKey, encoded)
        prefs.edit().putLong("local_last_sync_$uidKey", nowMs).apply()

        // 2) Sync directly to the signed-in user's OWN Google Drive (visible drive.file + appDataFolder mirror)
        var syncedDrive = false
        var driveErrMsg: String? = null
        val targetFileName = prefs.getString("drive_name_$uidKey", null) ?: suggestedDriveFileName(user)

        if (prefs.getBoolean("drive_oauth_granted_$uidKey", false)) {
            runCatching {
                val token = obtainSilentAccessToken(user)
                val replaceRes = driveRestClient.replaceJsonFileInDrive(
                    accessToken = token,
                    fileName = targetFileName,
                    jsonContent = encoded
                )
                // Best-effort hidden mirror in user's own Drive appDataFolder so deleting the visible file never loses data
                runCatching {
                    driveRestClient.replaceAppDataJsonInDrive(
                        accessToken = token,
                        fileName = targetFileName,
                        jsonContent = encoded
                    )
                }
                prefs.edit()
                    .putString("drive_file_id_$uidKey", replaceRes.newFileId)
                    .putString("drive_last_replaced_old_id_$uidKey", replaceRes.deletedOldFileIds.firstOrNull())
                    .putString("drive_name_$uidKey", targetFileName)
                    .putLong("drive_last_sync_$uidKey", nowMs)
                    .putLong("drive_remote_exported_at_$uidKey", nowMs)
                    .putBoolean("drive_pending_push_$uidKey", false)
                    .apply()
                syncedDrive = true
            }.onFailure { err ->
                if (err is DriveUnauthorizedException) {
                    cachedAccessToken = null
                    prefs.edit().putBoolean("drive_oauth_granted_$uidKey", false).apply()
                }
                prefs.edit().putBoolean("drive_pending_push_$uidKey", true).apply()
                driveErrMsg = err.localizedMessage
            }
        }

        // 3) Companion / zero-OAuth write to linked SAF Google Drive document URI if configured
        val uriStr = prefs.getString("drive_uri_$uidKey", null)
        if (!uriStr.isNullOrBlank()) {
            runCatching {
                writeTextToUri(Uri.parse(uriStr), encoded)
                prefs.edit()
                    .putLong("drive_last_sync_$uidKey", nowMs)
                    .putLong("drive_remote_exported_at_$uidKey", nowMs)
                    .putBoolean("drive_pending_push_$uidKey", false)
                    .apply()
                syncedDrive = true
            }.onFailure { err ->
                prefs.edit().putBoolean("drive_pending_push_$uidKey", true).apply()
                if (driveErrMsg == null) driveErrMsg = err.localizedMessage
            }
        }

        refreshStatusForUser(user)
        _status.value = _status.value.copy(
            isDriveLinked = syncedDrive || prefs.getBoolean("drive_oauth_granted_$uidKey", false) || !uriStr.isNullOrBlank(),
            isBackingUp = false,
            lastLocalVaultAtMs = nowMs,
            lastError = if (syncedDrive) null else driveErrMsg
        )
        return if (!syncedDrive && driveErrMsg != null) {
            Result.failure(IllegalStateException(driveErrMsg))
        } else {
            Result.success(Unit)
        }
    }

    private suspend fun performAutoSaveAndDriveSync() {
        backupMutex.withLock {
            val user = cloudAccountRepository.currentUser.first()
            val uidKey = user?.uid?.takeIf { it.isNotBlank() } ?: "local_device"
            prefs.edit()
                .putLong("local_last_mutation_$uidKey", System.currentTimeMillis())
                .putBoolean("drive_pending_push_$uidKey", true)
                .apply()

            // If user has Drive linked but hasn't pulled yet in this app session, pull first so we never clobber newer Drive data!
            val uriStr = prefs.getString("drive_uri_$uidKey", null)
            val oauthGranted = prefs.getBoolean("drive_oauth_granted_$uidKey", false)
            if (pulledSessionUid != uidKey && (oauthGranted || !uriStr.isNullOrBlank())) {
                val targetFileName = prefs.getString("drive_name_$uidKey", null) ?: suggestedDriveFileName(user)
                pullFromDriveLockedIfNewer(
                    user = user,
                    uidKey = uidKey,
                    targetFileName = targetFileName,
                    oauthGranted = oauthGranted,
                    uriStr = uriStr,
                    forceApply = false
                )
                pulledSessionUid = uidKey
            }

            pushCurrentStateLocked(user = user, uidKey = uidKey)
        }
    }

    private suspend fun obtainSilentAccessToken(user: CloudUser?): String {
        val email = user?.email?.trim()?.lowercase()
        if (cachedTokenAccountEmail == email) {
            cachedAccessToken?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return when (val state = driveRestClient.authorizeForDrive(activity = null, accountEmail = user?.email)) {
            is DriveAuthState.Authorized -> {
                cachedAccessToken = state.accessToken
                cachedTokenAccountEmail = email
                state.accessToken
            }
            is DriveAuthState.NeedsResolution -> {
                throw DriveUnauthorizedException("Please tap Connect Google Drive to refresh permission")
            }
        }
    }

    /**
     * Restores user records from the signed-in user's own Google Drive (`drive.file` or
     * `appDataFolder` safety mirror), a linked SAF Drive URI, or the local per-user JSON vault.
     */
    suspend fun restoreFromGoogleDriveUri(sourceUri: Uri? = null): Result<Int> = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            try {
                isRestoring.set(true)
                _status.value = _status.value.copy(isBackingUp = true, lastError = null)
                val user = cloudAccountRepository.currentUser.first()
                val uidKey = user?.uid?.takeIf { it.isNotBlank() } ?: "local_device"

                val rawJson: String = when {
                    sourceUri != null -> {
                        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        runCatching { context.contentResolver.takePersistableUriPermission(sourceUri, takeFlags) }
                        val name = resolveDisplayName(sourceUri) ?: suggestedDriveFileName(user)
                        prefs.edit()
                            .putString("drive_uri_$uidKey", sourceUri.toString())
                            .putString("drive_name_$uidKey", name)
                            .apply()
                        readTextFromUri(sourceUri)
                    }
                    prefs.getBoolean("drive_oauth_granted_$uidKey", false) -> {
                        val token = obtainSilentAccessToken(user)
                        val fileName = prefs.getString("drive_name_$uidKey", null) ?: suggestedDriveFileName(user)
                        val (fileId, downloaded) = driveRestClient.downloadLatestJsonFromDrive(token, fileName)
                        prefs.edit().putString("drive_file_id_$uidKey", fileId).apply()
                        downloaded
                    }
                    else -> {
                        val linkedUri = prefs.getString("drive_uri_$uidKey", null)?.let { Uri.parse(it) }
                        if (linkedUri != null) {
                            readTextFromUri(linkedUri)
                        } else {
                            readLocalVaultFile(uidKey)
                                ?: throw IllegalStateException("No linked Google Drive backup or local JSON vault found")
                        }
                    }
                }

                val restoredCount = importJsonInternal(rawJson, user)
                val nowMs = System.currentTimeMillis()
                prefs.edit()
                    .putLong("drive_last_sync_$uidKey", nowMs)
                    .putLong("drive_last_pull_$uidKey", nowMs)
                    .putLong("drive_remote_exported_at_$uidKey", nowMs)
                    .apply()
                pulledSessionUid = uidKey
                refreshStatusForUser(user)
                Result.success(restoredCount)
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Restore failed"
                _status.value = _status.value.copy(isBackingUp = false, lastError = msg)
                Result.failure(e)
            } finally {
                isRestoring.set(false)
            }
        }
    }

    suspend fun importJsonString(rawJson: String): Result<Int> = withContext(Dispatchers.IO) {
        backupMutex.withLock {
            try {
                isRestoring.set(true)
                val user = cloudAccountRepository.currentUser.first()
                val restored = importJsonInternal(rawJson, user)
                refreshStatusForUser(user)
                Result.success(restored)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                isRestoring.set(false)
            }
        }
    }

    private suspend fun importJsonInternal(rawJson: String, user: CloudUser?): Int {
        val trimmed = rawJson.trim()
        if (trimmed.contains("\"mahavtaar_navtara_json\"")) {
            val navBundle = json.decodeFromString<NavtaraJsonBackupBundle>(trimmed)
            if (user != null && navBundle.userUid.isNotBlank() && navBundle.userUid != user.uid) {
                throw IllegalStateException(
                    "यह बैकअप फ़ाइल किसी अन्य Google खाते (${navBundle.userEmail ?: navBundle.userUid}) की है।"
                )
            }
            return applyNavtaraSubsetToDevice(
                profiles = navBundle.profiles,
                feedbacks = navBundle.feedbacks,
                reports = navBundle.savedReports
            )
        }
        val bundle = json.decodeFromString<UserFullBackupBundle>(trimmed)
        if (user != null && bundle.userUid.isNotBlank() && bundle.userUid != user.uid) {
            throw IllegalStateException(
                "यह बैकअप फ़ाइल किसी अन्य Google खाते (${bundle.userEmail ?: bundle.userUid}) की है। कृपया उसी Google ID से लॉगिन करें।"
            )
        }
        return applyBundleToDevice(bundle)
    }

    suspend fun exportNavtaraJsonString(
        activeProfileId: Long? = null,
        currentPeriodReport: NavtaraPeriodReport? = null
    ): String = withContext(Dispatchers.IO) {
        val user = cloudAccountRepository.currentUser.first()
        val profiles = astrologyRepository.observeNavtaraProfiles().first()
        val feedbacks = profiles.filter { it.id > 0L }.flatMap { p ->
            astrologyRepository.observeNavtaraFeedbacks(p.id).first()
        }
        val reports = astrologyRepository.observeSavedNavtaraReports().first()
        val payload = NavtaraJsonBackupBundle(
            schemaVersion = 1,
            format = "mahavtaar_navtara_json",
            userUid = user?.uid.orEmpty(),
            userEmail = user?.email,
            userDisplayName = user?.displayName,
            exportedAtMs = System.currentTimeMillis(),
            activeProfileId = activeProfileId,
            profiles = profiles,
            feedbacks = feedbacks,
            savedReports = reports,
            activePeriodReport = currentPeriodReport
        )
        json.encodeToString(payload)
    }

    suspend fun exportFullBundleJsonString(): String = withContext(Dispatchers.IO) {
        val user = cloudAccountRepository.currentUser.first()
        json.encodeToString(buildCurrentBundle(user))
    }

    private suspend fun buildCurrentBundle(user: CloudUser?): UserFullBackupBundle {
        val userPrefs = preferencesRepository.preferences.first()
        val locations = locationRepository.observeSavedLocations().firstOrNull().orEmpty()
        val kundlis = astrologyRepository.observeSavedKundliCharts().firstOrNull().orEmpty()
        val kundliProfiles = runCatching {
            preferencesRepository.observeSavedKundliProfiles().firstOrNull().orEmpty()
        }.getOrDefault(emptyList())
        val profiles = astrologyRepository.observeNavtaraProfiles().firstOrNull().orEmpty()
        val feedbacks = mutableListOf<NavtaraDailyFeedback>()
        for (p in profiles) {
            if (p.id > 0L) {
                feedbacks += astrologyRepository.observeNavtaraFeedbacks(p.id).firstOrNull().orEmpty()
            }
        }
        val reports = astrologyRepository.observeSavedNavtaraReports().firstOrNull().orEmpty()
        val rules = notificationStudioRepository.getAllRules()
        val audio = notificationStudioRepository.getAudioSettings()

        return UserFullBackupBundle(
            schemaVersion = 3,
            userUid = user?.uid.orEmpty(),
            userEmail = user?.email,
            userDisplayName = user?.displayName,
            exportedAtMs = System.currentTimeMillis(),
            preferences = userPrefs,
            savedLocations = locations,
            savedKundliCharts = kundlis,
            savedKundliProfiles = kundliProfiles,
            navtaraProfiles = profiles,
            navtaraFeedbacks = feedbacks,
            savedNavtaraReports = reports,
            notificationRules = rules,
            alertAudioSettings = audio
        )
    }

    private suspend fun applyNavtaraSubsetToDevice(
        profiles: List<NavtaraProfile>,
        feedbacks: List<NavtaraDailyFeedback>,
        reports: List<com.mahavtaar.panchanga.domain.model.SavedNavtaraReport>
    ): Int {
        var totalItems = 0
        val existingProfiles = astrologyRepository.observeNavtaraProfiles().first()
        val oldToNewProfileId = HashMap<Long, Long>()
        profiles.forEach { prof ->
            val match = existingProfiles.firstOrNull {
                it.name.equals(prof.name, ignoreCase = true) &&
                    it.birthNakshatraIndex == prof.birthNakshatraIndex &&
                    it.birthPada == prof.birthPada
            }
            val savedId = astrologyRepository.saveNavtaraProfile(prof.copy(id = match?.id ?: 0L))
            oldToNewProfileId[prof.id] = savedId
            if (prof.isDefault) {
                astrologyRepository.setDefaultNavtaraProfile(savedId)
            }
            totalItems++
        }

        feedbacks.forEach { fb ->
            val mappedProfId = oldToNewProfileId[fb.profileId] ?: fb.profileId
            if (mappedProfId > 0L) {
                astrologyRepository.saveNavtaraFeedback(fb.copy(profileId = mappedProfId))
                totalItems++
            }
        }

        val existingReports = astrologyRepository.observeSavedNavtaraReports().first()
        reports.forEach { rep ->
            val match = existingReports.firstOrNull {
                it.profileName == rep.profileName &&
                    it.startDateIso == rep.startDateIso &&
                    it.endDateIso == rep.endDateIso
            }
            if (match == null) {
                val mappedProfId = oldToNewProfileId[rep.profileId] ?: rep.profileId
                astrologyRepository.saveNavtaraReport(rep.copy(id = 0L, profileId = mappedProfId))
                totalItems++
            }
        }
        return totalItems
    }

    private suspend fun applyBundleToDevice(bundle: UserFullBackupBundle): Int {
        var totalItems = 0

        val p = bundle.preferences
        preferencesRepository.setLanguage(p.language)
        preferencesRepository.setThemeMode(p.themeMode)
        preferencesRepository.setMonthSystem(p.monthSystem)
        preferencesRepository.setAyanamsha(p.ayanamsha)
        preferencesRepository.setBirthNakshatra(p.birthNakshatra)
        preferencesRepository.setUse24HourFormat(p.use24HourFormat)
        preferencesRepository.setTattvaMinimalMode(p.tattvaMinimalMode)
        preferencesRepository.setTattvaHiddenFeatures(p.tattvaHiddenFeatures)
        preferencesRepository.setOnboardingCompleted(p.hasCompletedOnboarding)
        preferencesRepository.setPinnedHomeModules(p.pinnedHomeModules)
        preferencesRepository.setWidgetGlassStyle(p.widgetGlassStyle)
        preferencesRepository.setWidgetOpacity(p.widgetOpacity)
        preferencesRepository.updateNotificationSettings(
            notifySunrise = p.notifySunrise,
            notifySunset = p.notifySunset,
            notifyTithiChange = p.notifyTithiChange,
            notifyNakshatraChange = p.notifyNakshatraChange,
            notifyVratReminders = p.notifyVratReminders
        )
        preferencesRepository.setSelectedLocation(p.selectedLocation)
        totalItems++

        bundle.savedLocations.forEach { loc ->
            locationRepository.saveLocation(loc)
            if (loc.isDefault) {
                locationRepository.setDefaultLocation(loc.name)
            }
            totalItems++
        }

        val existingKundlis = astrologyRepository.observeSavedKundliCharts().firstOrNull().orEmpty()
        bundle.savedKundliCharts.forEach { chart ->
            val match = existingKundlis.firstOrNull {
                it.input.name.equals(chart.input.name, ignoreCase = true) &&
                    it.input.year == chart.input.year &&
                    it.input.month == chart.input.month &&
                    it.input.day == chart.input.day &&
                    it.input.hour == chart.input.hour &&
                    it.input.minute == chart.input.minute
            }
            astrologyRepository.saveKundliChart(chart.copy(id = match?.id ?: 0L))
            totalItems++
        }

        bundle.savedKundliProfiles.forEach { birthProfile ->
            preferencesRepository.saveKundliProfile(birthProfile)
            totalItems++
        }

        totalItems += applyNavtaraSubsetToDevice(
            profiles = bundle.navtaraProfiles,
            feedbacks = bundle.navtaraFeedbacks,
            reports = bundle.savedNavtaraReports
        )

        bundle.notificationRules.forEach { rule ->
            notificationStudioRepository.saveRule(rule)
            totalItems++
        }
        notificationStudioRepository.updateAudioSettings(bundle.alertAudioSettings)

        return totalItems
    }

    private fun localVaultFile(uidKey: String): File {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        return File(dir, "mahavtaar_vault_$uidKey.json")
    }

    private fun writeLocalVaultFile(uidKey: String, content: String) {
        localVaultFile(uidKey).writeText(content, Charsets.UTF_8)
    }

    private fun readLocalVaultFile(uidKey: String): String? {
        val file = localVaultFile(uidKey)
        return if (file.exists()) file.readText(Charsets.UTF_8) else null
    }

    private fun writeTextToUri(uri: Uri, content: String) {
        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: context.contentResolver.openOutputStream(uri)
            ?: throw IllegalStateException("Unable to open Google Drive document for writing")
        out.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.flush()
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open Google Drive backup file for reading")
        return input.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun resolveDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    }.getOrNull()
}
