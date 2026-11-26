package com.mahavtaar.panchanga.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double? = null
)

/**
 * Provides real device GPS coordinates using Google Play Services FusedLocationProviderClient.
 * Safely checks runtime permissions and returns null if denied or unavailable.
 */
@Singleton
class DeviceLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedClient: FusedLocationProviderClient
) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoordinates(): Result<DeviceCoordinates> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Location permission not granted"))
        }
        return runCatching {
            val tokenSource = CancellationTokenSource()
            val fresh = runCatching {
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token).await()
            }.getOrNull()
            val loc = fresh ?: fusedClient.lastLocation.await()
                ?: error("Unable to acquire GPS fix. Please select a city manually.")
            val acc = if (loc.hasAccuracy() && loc.accuracy > 0f) loc.accuracy.toDouble() else null
            DeviceCoordinates(latitude = loc.latitude, longitude = loc.longitude, accuracyMeters = acc)
        }
    }
}
