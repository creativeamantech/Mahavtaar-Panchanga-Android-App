package com.mahavtaar.panchanga.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

/**
 * Deterministic astronomical and local-timezone formatters matching the reference TypeScript engine.
 */
object TimeFormatters {

    private val shortTime12hFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    private val hms12hFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US)
    private val hms24hFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.UK)
    private val dateGbFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.UK)
    private val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.US)
    private val panchangaTimestampFormatter = DateTimeFormatter.ofPattern("MMM d, hh:mm a", Locale.US)
    private val dateTimeWithSecondsFormatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm:ss a", Locale.US)

    /**
     * Exact match for TypeScript `formatTimeHMS(totalHours)`.
     * Supports values > 24h (e.g. `30:27:21` for next sunrise) and negative values.
     */
    fun formatHoursHMS(totalHours: Double): String {
        if (totalHours.isNaN() || totalHours.isInfinite()) return "--:--:--"
        val isNegative = totalHours < 0.0
        val absHours = abs(totalHours)
        val h = floor(absHours).toInt()
        val remMinutes = (absHours - h) * 60.0
        val m = floor(remMinutes).toInt()
        val s = floor((remMinutes - m) * 60.0).toInt()
        val sign = if (isNegative) "-" else ""
        return "$sign${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }

    /**
     * Exact match for TypeScript `formatDegreesDMS(deg)`.
     */
    fun formatDegreesDMS(deg: Double): String {
        val norm = ((deg % 360.0) + 360.0) % 360.0
        val d = floor(norm).toInt()
        val remM = (norm - d) * 60.0
        val m = floor(remM).toInt()
        val s = floor((remM - m) * 60.0).toInt()
        return "$d° ${m.toString().padStart(2, '0')}' ${s.toString().padStart(2, '0')}\""
    }

    fun formatDurationRemaining(remainingMs: Long): String {
        if (remainingMs <= 0L) return "0m"
        val totalSeconds = remainingMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0L -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
            minutes > 0L -> "${minutes}m ${seconds.toString().padStart(2, '0')}s"
            else -> "${seconds}s"
        }
    }

    fun formatDurationHoursMinutes(durationMs: Long): String {
        if (durationMs <= 0L) return "0m"
        val totalMinutes = durationMs / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun safeZoneId(timeZoneId: String): ZoneId =
        runCatching { ZoneId.of(timeZoneId) }.getOrElse { ZoneId.of("Asia/Kolkata") }

    fun format12HourHMS(epochMs: Long, timeZoneId: String): String =
        Instant.ofEpochMilli(epochMs).atZone(safeZoneId(timeZoneId)).format(hms12hFormatter)

    fun format12HourHM(epochMs: Long, timeZoneId: String): String =
        Instant.ofEpochMilli(epochMs).atZone(safeZoneId(timeZoneId)).format(shortTime12hFormatter)

    fun format24HourHMS(epochMs: Long, timeZoneId: String): String =
        Instant.ofEpochMilli(epochMs).atZone(safeZoneId(timeZoneId)).format(hms24hFormatter)

    fun formatDateGB(epochMs: Long, timeZoneId: String): String =
        Instant.ofEpochMilli(epochMs).atZone(safeZoneId(timeZoneId)).format(dateGbFormatter)

    fun formatDayOfWeek(epochMs: Long, timeZoneId: String): String =
        Instant.ofEpochMilli(epochMs).atZone(safeZoneId(timeZoneId)).format(dayOfWeekFormatter)

    fun formatPanchangaDateTime(epochMs: Long, timeZoneId: String): String =
        Instant.ofEpochMilli(epochMs).atZone(safeZoneId(timeZoneId)).format(panchangaTimestampFormatter)

    private val gregorianMonthsShortHindi = listOf(
        "जन", "फ़र", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अग", "सित", "अक्टू", "नव", "दिस"
    )

    fun formatDateTimeWithSeconds(epochMs: Long, timeZoneId: String): String =
        Instant.ofEpochMilli(epochMs).atZone(safeZoneId(timeZoneId)).format(dateTimeWithSecondsFormatter)

    fun formatLocalizedDateTimeWithSeconds(epochMs: Long, timeZoneId: String, isHindi: Boolean): String {
        if (!isHindi) return formatDateTimeWithSeconds(epochMs, timeZoneId)
        val zdt = Instant.ofEpochMilli(epochMs).atZone(safeZoneId(timeZoneId))
        val mon = gregorianMonthsShortHindi[zdt.monthValue - 1]
        return "${zdt.dayOfMonth} $mon, ${format12HourHMS(epochMs, timeZoneId)}"
    }

    fun formatDateTimeRangeWithSeconds(startMs: Long, endMs: Long, timeZoneId: String): String =
        "${formatDateTimeWithSeconds(startMs, timeZoneId)} – ${formatDateTimeWithSeconds(endMs, timeZoneId)}"

    fun formatMinutesTo24h(minutes: Double): String {
        val rounded = kotlin.math.round(minutes).toInt()
        val norm = ((rounded % 1440) + 1440) % 1440
        val h = norm / 60
        val m = norm % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }

    fun parseTimeToMinutes(timeStr: String?): Double? {
        if (timeStr.isNullOrBlank() || timeStr == "None" || timeStr == "unavailable") return null
        val parts = timeStr.trim().split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val s = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return h * 60.0 + m + s / 60.0
    }
}
