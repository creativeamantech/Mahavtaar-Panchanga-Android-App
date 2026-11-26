package com.mahavtaar.panchanga.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.mahavtaar.panchanga.core.AppDispatchers
import com.mahavtaar.panchanga.core.access.UserAccessCacheService
import com.mahavtaar.panchanga.data.backup.GoogleDriveBackupManager
import com.mahavtaar.panchanga.domain.model.CloudSyncStatus
import com.mahavtaar.panchanga.domain.model.CloudUser
import com.mahavtaar.panchanga.domain.repository.CloudAccountRepository
import com.mahavtaar.panchanga.sync.GoogleDriveAutoSyncWorker
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val SYNC_TAG = "UserDriveSync"

/**
 * Privacy-first [CloudAccountRepository] implementation:
 * - Authenticates users via FirebaseAuth + Google Sign-In (Jetpack Credential Manager).
 * - Stores ZERO personal user data (locations, preferences, Navtara profiles/feedbacks/reports,
 *   Kundli charts/profiles, alert rules) in developer Firestore.
 * - Routes ALL user data sync and auto-backup directly to the signed-in user's OWN Google Drive
 *   via [GoogleDriveBackupManager] — identically for both regular users and admins.
 */
@Singleton
class FirestoreCloudSyncRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val userAccessCacheService: UserAccessCacheService,
    private val driveBackupManagerProvider: Lazy<GoogleDriveBackupManager>,
    private val dispatchers: AppDispatchers
) : CloudAccountRepository {

    private val repoExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(SYNC_TAG, "Background user Google Drive sync error", throwable)
    }
    private val repoScope = CoroutineScope(SupervisorJob() + dispatchers.io + repoExceptionHandler)
    private val syncMutex = Mutex()

    private val _syncStatus = MutableStateFlow(CloudSyncStatus())
    override val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    override val currentUser: Flow<CloudUser?> = firebaseAuth.authStateFlow().map { fbUser ->
        fbUser?.let {
            CloudUser(
                uid = it.uid,
                displayName = it.displayName,
                email = it.email
            )
        }
    }

    @Volatile
    private var activeUid: String? = null
    private var statusMirrorJob: Job? = null
    private var sessionInitJob: Job? = null

    override suspend fun startSyncForUser(uid: String) {
        if (uid.isBlank()) return
        syncMutex.withLock {
            if (activeUid == uid && statusMirrorJob?.isActive == true) {
                return
            }
            statusMirrorJob?.cancel()
            sessionInitJob?.cancel()
            activeUid = uid

            val driveManager = driveBackupManagerProvider.get()
            statusMirrorJob = repoScope.launch {
                driveManager.status.collect { driveStatus ->
                    _syncStatus.value = CloudSyncStatus(
                        isSyncing = driveStatus.isBackingUp,
                        lastSyncedAtMs = driveStatus.lastDriveSyncAtMs ?: driveStatus.lastLocalVaultAtMs,
                        lastError = driveStatus.lastError
                    )
                }
            }

            sessionInitJob = repoScope.launch {
                runCatching {
                    driveManager.startUserDriveSyncSession(uid)
                }.onFailure { err ->
                    if (err is CancellationException) throw err
                    Log.w(SYNC_TAG, "Initial Drive sync session notice: ${err.message}")
                }
                GoogleDriveAutoSyncWorker.schedulePeriodicSync(context)
            }
        }
    }

    override suspend fun stopSync() {
        syncMutex.withLock {
            statusMirrorJob?.cancel()
            statusMirrorJob = null
            sessionInitJob?.cancel()
            sessionInitJob = null
            activeUid = null
            _syncStatus.value = CloudSyncStatus()
        }
    }

    override suspend fun syncAllNow(): Result<Unit> = withContext(dispatchers.io) {
        val uid = activeUid ?: firebaseAuth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Not signed in"))

        _syncStatus.value = _syncStatus.value.copy(isSyncing = true, lastError = null)
        return@withContext try {
            val res = driveBackupManagerProvider.get()
                .syncBidirectionalWithGoogleDrive(forcePullFirst = false)
            if (res.isSuccess) {
                GoogleDriveAutoSyncWorker.enqueueImmediateConnectedFlush(context)
                val latestDrive = driveBackupManagerProvider.get().status.value
                _syncStatus.value = CloudSyncStatus(
                    isSyncing = false,
                    lastSyncedAtMs = latestDrive.lastDriveSyncAtMs ?: latestDrive.lastLocalVaultAtMs ?: System.currentTimeMillis(),
                    lastError = null
                )
            } else {
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    lastError = res.exceptionOrNull()?.localizedMessage ?: "Google Drive sync failed"
                )
            }
            res
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Google Drive sync failed"
            _syncStatus.value = _syncStatus.value.copy(isSyncing = false, lastError = msg)
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        signOutWithCleanup(credentialManager) {
            stopSync()
            runCatching { driveBackupManagerProvider.get().onUserSignedOut() }
            GoogleDriveAutoSyncWorker.cancelAll(context)
            runCatching { userAccessCacheService.clearAllCachedAccess() }
        }
    }
}
