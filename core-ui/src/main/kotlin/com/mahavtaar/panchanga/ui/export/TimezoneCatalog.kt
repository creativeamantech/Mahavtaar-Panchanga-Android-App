package com.mahavtaar.panchanga.ui.export

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

data class TimezoneOption(
    val id: String,
    val label: String
)

object TimezoneCatalog {

    private val curatedIds = listOf(
        "Asia/Kolkata",
        "Asia/Kathmandu",
        "Asia/Colombo",
        "Asia/Dhaka",
        "Asia/Dubai",
        "Asia/Singapore",
        "Asia/Tokyo",
        "Europe/London",
        "Europe/Berlin",
        "Europe/Paris",
        "America/New_York",
        "America/Chicago",
        "America/Denver",
        "America/Los_Angeles",
        "America/Toronto",
        "Australia/Sydney",
        "Pacific/Auckland",
        "UTC"
    )

    fun allOptions(): List<TimezoneOption> {
        val now = Instant.now()
        val systemDefault = ZoneId.systemDefault().id
        val available = ZoneId.getAvailableZoneIds()
        val orderedIds = (listOf(systemDefault) + curatedIds + available.sorted())
            .distinct()
            .filter { !it.startsWith("Etc/") || it == "UTC" }
        return orderedIds.mapNotNull { id ->
            runCatching {
                val zone = ZoneId.of(id)
                val offset = zone.rules.getOffset(now).id.let { if (it == "Z") "+00:00" else it }
                TimezoneOption(id = id, label = "$id (UTC$offset)")
            }.getOrNull()
        }
    }

    fun filterOptions(query: String, limit: Int = 50): List<TimezoneOption> {
        val q = query.trim().lowercase(Locale.US)
        val all = allOptions()
        if (q.isBlank()) return all.take(limit)
        return all.filter {
            it.id.lowercase(Locale.US).contains(q) || it.label.lowercase(Locale.US).contains(q)
        }.take(limit)
    }

    fun parseLatitude(raw: String): Double? {
        val v = raw.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return v.takeIf { it.isFinite() && it in -90.0..90.0 }
    }

    fun parseLongitude(raw: String): Double? {
        val v = raw.trim().replace(',', '.').toDoubleOrNull() ?: return null
        return v.takeIf { it.isFinite() && it in -180.0..180.0 }
    }

    fun formatCoordinates(latitude: Double, longitude: Double, decimals: Int = 5): String {
        val pattern = "%.${decimals}f°, %.${decimals}f°"
        return pattern.format(Locale.US, latitude, longitude)
    }

    fun formatSignedDegree(value: Double, isLat: Boolean, decimals: Int = 4): String {
        val dir = if (isLat) (if (value >= 0) "N" else "S") else (if (value >= 0) "E" else "W")
        return "%.${decimals}f° %s".format(Locale.US, kotlin.math.abs(value), dir)
    }
}
