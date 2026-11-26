package com.mahavtaar.panchanga.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Helper to refresh all 3 Home Screen Glance widgets whenever a Vedic transition boundary is reached,
 * user preferences change, or an automatic heartbeat fires.
 */
object WidgetUpdateHelper {
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    private fun hasPlacedWidgets(context: Context, receiverClass: Class<*>): Boolean =
        runCatching {
            val awm = AppWidgetManager.getInstance(context) ?: return@runCatching false
            awm.getAppWidgetIds(ComponentName(context, receiverClass)).isNotEmpty()
        }.getOrDefault(false)

    fun hasAnyPlacedWidgets(context: Context): Boolean {
        val appCtx = context.applicationContext
        return hasPlacedWidgets(appCtx, PanchangaGlanceWidgetReceiver::class.java) ||
            hasPlacedWidgets(appCtx, HoraTattvaGlanceWidgetReceiver::class.java) ||
            hasPlacedWidgets(appCtx, MuhurtaGlanceWidgetReceiver::class.java) ||
            hasPlacedWidgets(appCtx, RunningNowStripGlanceWidgetReceiver::class.java)
    }

    suspend fun updateAllWidgets(context: Context) {
        val appCtx = context.applicationContext
        val hasPanchanga = hasPlacedWidgets(appCtx, PanchangaGlanceWidgetReceiver::class.java)
        val hasHora = hasPlacedWidgets(appCtx, HoraTattvaGlanceWidgetReceiver::class.java)
        val hasMuhurta = hasPlacedWidgets(appCtx, MuhurtaGlanceWidgetReceiver::class.java)
        val hasStrip = hasPlacedWidgets(appCtx, RunningNowStripGlanceWidgetReceiver::class.java)

        if (!hasPanchanga && !hasHora && !hasMuhurta && !hasStrip) {
            WidgetLiveRefreshScheduler.cancelAllIfNoWidgets(appCtx)
            return
        }

        if (hasPanchanga) {
            runCatching { PanchangaGlanceWidget().updateAll(appCtx) }
        }
        if (hasHora) {
            runCatching { HoraTattvaGlanceWidget().updateAll(appCtx) }
        }
        if (hasMuhurta) {
            runCatching { MuhurtaGlanceWidget().updateAll(appCtx) }
        }
        if (hasStrip) {
            runCatching { RunningNowStripGlanceWidget().updateAll(appCtx) }
        }

        // Schedule next automatic boundary wakeup so widgets stay permanently live with zero manual refresh
        val nextBoundaryMs = runCatching {
            LiveWidgetDataResolver.resolve(appCtx).nextTransitionAtMs
        }.getOrElse { System.currentTimeMillis() + 5 * 60_000L }

        WidgetLiveRefreshScheduler.scheduleNextRefresh(appCtx, nextBoundaryMs)
        WidgetLiveRefreshScheduler.ensurePeriodicWorker(appCtx)
    }

    fun triggerAsyncUpdate(context: Context) {
        val appCtx = context.applicationContext
        widgetScope.launch {
            updateAllWidgets(appCtx)
        }
    }
}

/**
 * Automatic zero-touch live refresh engine for Home Screen widgets:
 * 1) Schedules an exact AlarmManager wakeup at the earliest upcoming Vedic boundary
 *    (Sub-Tattva, Hora, Choghadiya, Muhurta, Tithi/Nakshatra, Swara) capped at a 5-min progress heartbeat.
 * 2) Keeps a 15-minute WorkManager PeriodicWork safety net (ExistingPeriodicWorkPolicy.KEEP)
 *    so widgets recover automatically even after Doze/OEM task killers.
 */
object WidgetLiveRefreshScheduler {
    private const val PERIODIC_WORK_NAME = "mahavtaar_widget_live_refresh_periodic"
    private const val ACTION_WIDGET_AUTO_REFRESH = "com.mahavtaar.panchanga.ACTION_WIDGET_AUTO_REFRESH"
    private const val REQUEST_CODE_ALARM = 44_901
    private const val MIN_REFRESH_DELAY_MS = 30_000L
    private const val MAX_HEARTBEAT_DELAY_MS = 5 * 60_000L

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context.applicationContext, WidgetAutoRefreshReceiver::class.java).apply {
            action = ACTION_WIDGET_AUTO_REFRESH
        }
        return PendingIntent.getBroadcast(
            context.applicationContext,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleNextRefresh(context: Context, nextTransitionAtMs: Long) {
        val appCtx = context.applicationContext
        if (!WidgetUpdateHelper.hasAnyPlacedWidgets(appCtx)) {
            cancelAllIfNoWidgets(appCtx)
            return
        }

        val now = System.currentTimeMillis()
        // Wake 1.5s after the exact Vedic boundary, capped at 5 min to keep progress bars fresh
        val desiredTrigger = minOf(nextTransitionAtMs + 1_500L, now + MAX_HEARTBEAT_DELAY_MS)
        val triggerAtMs = maxOf(now + MIN_REFRESH_DELAY_MS, desiredTrigger)

        runCatching {
            val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pi = buildAlarmPendingIntent(appCtx)
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC, triggerAtMs, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC, triggerAtMs, pi)
            }
        }
    }

    fun ensurePeriodicWorker(context: Context) {
        val appCtx = context.applicationContext
        if (!WidgetUpdateHelper.hasAnyPlacedWidgets(appCtx)) return
        runCatching {
            val request = PeriodicWorkRequestBuilder<WidgetLiveRefreshWorker>(
                15L, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(appCtx).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    fun ensureLiveRefreshActive(context: Context) {
        val appCtx = context.applicationContext
        if (!WidgetUpdateHelper.hasAnyPlacedWidgets(appCtx)) return
        ensurePeriodicWorker(appCtx)
        scheduleNextRefresh(appCtx, System.currentTimeMillis() + MAX_HEARTBEAT_DELAY_MS)
    }

    fun cancelAllIfNoWidgets(context: Context) {
        val appCtx = context.applicationContext
        if (WidgetUpdateHelper.hasAnyPlacedWidgets(appCtx)) return
        runCatching {
            val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.cancel(buildAlarmPendingIntent(appCtx))
        }
        runCatching {
            WorkManager.getInstance(appCtx).cancelUniqueWork(PERIODIC_WORK_NAME)
        }
    }
}

/**
 * BroadcastReceiver fired automatically at Vedic transition boundaries and 5-minute progress heartbeats
 * to keep all placed Home-Screen widgets continuously updated without manual user refresh.
 */
class WidgetAutoRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appCtx = context.applicationContext
        val handler = CoroutineExceptionHandler { _, _ -> pendingResult.finish() }
        CoroutineScope(SupervisorJob() + Dispatchers.IO + handler).launch {
            try {
                WidgetUpdateHelper.updateAllWidgets(appCtx)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * 15-minute WorkManager safety net ensuring Home-Screen widgets self-heal after Doze or app restarts.
 */
class WidgetLiveRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!WidgetUpdateHelper.hasAnyPlacedWidgets(applicationContext)) {
            WidgetLiveRefreshScheduler.cancelAllIfNoWidgets(applicationContext)
            return Result.success()
        }
        return runCatching {
            WidgetUpdateHelper.updateAllWidgets(applicationContext)
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }
}

/**
 * Subtle manual-sync ActionCallback in case the user ever wants an instant force-sync after changing location.
 */
class RefreshAllWidgetsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetUpdateHelper.updateAllWidgets(context)
    }
}
