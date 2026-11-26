package com.mahavtaar.panchanga.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles exact transition boundary wakeups and the quick "Turn Off" action
 * for the persistent Running Now priority notification.
 */
@AndroidEntryPoint
class LiveRunningNotificationReceiver : BroadcastReceiver() {

    companion object {
        private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    }

    @Inject
    lateinit var liveRunningNotificationManager: LiveRunningNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = runCatching {
            powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "mahavtaar:running_now_wakelock"
            )?.apply {
                setReferenceCounted(false)
                acquire(10_000L)
            }
        }.getOrNull()

        receiverScope.launch {
            try {
                when (action) {
                    LiveRunningNotificationManager.ACTION_TURN_OFF_RUNNING_NOW -> {
                        liveRunningNotificationManager.turnOffByUserAction()
                    }
                    LiveRunningNotificationManager.ACTION_REFRESH_RUNNING_NOW -> {
                        liveRunningNotificationManager.sync(allowChimeOnTattvaChange = true)
                    }
                }
            } finally {
                runCatching { pendingResult.finish() }
                runCatching {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                }
            }
        }
    }
}

/**
 * 15-minute WorkManager safety net ensuring the persistent Running Now notification
 * stays synchronized even if OEM Doze delays an exact alarm.
 */
@HiltWorker
class RunningNowWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val liveRunningNotificationManager: LiveRunningNotificationManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            liveRunningNotificationManager.sync(allowChimeOnTattvaChange = true)
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}
