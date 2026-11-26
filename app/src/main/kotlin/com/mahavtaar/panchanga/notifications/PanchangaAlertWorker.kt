package com.mahavtaar.panchanga.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs every 12 hours to top up the rolling 36-hour exact AlarmManager horizon
 * for all enabled Tattva, Swara Nadi, Hora, Muhurta, Anga, and Vrat alerts.
 */
@HiltWorker
class PanchangaAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val notificationScheduler: PanchangaNotificationScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            notificationScheduler.rescheduleAllAlerts()
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
