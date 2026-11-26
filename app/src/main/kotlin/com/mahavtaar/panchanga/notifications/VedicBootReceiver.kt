package com.mahavtaar.panchanga.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mahavtaar.panchanga.widget.WidgetLiveRefreshScheduler
import com.mahavtaar.panchanga.widget.WidgetUpdateHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Rebuilds the 36-hour Vedic alarm horizon and live Home-Screen widget schedule
 * after device reboot, clock/timezone changes, or Exact Alarm permission changes.
 */
@AndroidEntryPoint
class VedicBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationScheduler: PanchangaNotificationScheduler

    @Inject
    lateinit var liveRunningNotificationManager: LiveRunningNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            notificationScheduler.ensureDailySchedule()
            notificationScheduler.rescheduleNow()
            liveRunningNotificationManager.syncAsync(allowChimeOnTattvaChange = false)
            WidgetLiveRefreshScheduler.ensureLiveRefreshActive(context)
            WidgetUpdateHelper.triggerAsyncUpdate(context)
        }
    }
}
