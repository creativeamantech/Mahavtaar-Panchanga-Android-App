package com.mahavtaar.panchanga

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.mahavtaar.panchanga.notifications.LiveRunningNotificationManager
import com.mahavtaar.panchanga.notifications.PanchangaNotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MahavtaarApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationScheduler: PanchangaNotificationScheduler

    @Inject
    lateinit var liveRunningNotificationManager: LiveRunningNotificationManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().apply {
            if (::workerFactory.isInitialized) {
                setWorkerFactory(workerFactory)
            }
        }.build()

    override fun onCreate() {
        runCatching {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        }
        super.onCreate()
        runCatching { createNotificationChannels() }
        runCatching { liveRunningNotificationManager.ensureChannelExists() }
        runCatching { notificationScheduler.ensureDailySchedule() }
        runCatching { liveRunningNotificationManager.syncAsync(allowChimeOnTattvaChange = false) }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val managedChannel = NotificationChannel(
            PanchangaNotificationScheduler.CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_description)
            // Keep OS channel sound silent so our custom Vedic Tune -> Hindi/English Voice engine plays cleanly without overlapping dings
            setSound(null, null)
            enableVibration(true)
        }
        manager.createNotificationChannel(managedChannel)
    }
}
