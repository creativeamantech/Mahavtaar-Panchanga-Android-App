package com.mahavtaar.panchanga.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mahavtaar.panchanga.data.backup.GoogleDriveBackupManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic & connectivity-aware background worker that keeps the signed-in user's
 * (or admin's) personal Google Drive backup automatically synced — similar to AppSheet —
 * so edits made while offline are uploaded automatically once internet returns.
 */
@HiltWorker
class GoogleDriveAutoSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val googleDriveBackupManager: GoogleDriveBackupManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val status = googleDriveBackupManager.status.value
        if (!status.isDriveLinked && !status.hasPendingOfflineChanges) {
            return Result.success()
        }
        return googleDriveBackupManager.syncBidirectionalWithGoogleDrive(forcePullFirst = false)
            .fold(
                onSuccess = { Result.success() },
                onFailure = { if (runAttemptCount < 3) Result.retry() else Result.success() }
            )
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "mahavtaar_gdrive_periodic_sync"
        private const val ONE_TIME_WORK_NAME = "mahavtaar_gdrive_flush_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<GoogleDriveAutoSyncWorker>(
                6L, TimeUnit.HOURS,
                30L, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            runCatching {
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }

        fun enqueueImmediateConnectedFlush(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<GoogleDriveAutoSyncWorker>()
                .setConstraints(constraints)
                .build()
            runCatching {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    ONE_TIME_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }

        fun cancelAll(context: Context) {
            runCatching {
                val wm = WorkManager.getInstance(context)
                wm.cancelUniqueWork(PERIODIC_WORK_NAME)
                wm.cancelUniqueWork(ONE_TIME_WORK_NAME)
            }
        }
    }
}
