package com.mahavtaar.panchanga.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class NavtaraFeedbackRating(
    val labelEn: String,
    val labelHi: String,
    val emoji: String,
    val score: Int
) {
    AUSPICIOUS("Auspicious", "शुभ", "🟢", 5),
    NEUTRAL("Neutral", "सामान्य", "🟡", 3),
    CAUTION("Caution", "संभलकर", "🔴", 1);

    companion object {
        fun fromScore(score: Int): NavtaraFeedbackRating = when {
            score >= 4 -> AUSPICIOUS
            score <= 2 -> CAUTION
            else -> NEUTRAL
        }

        fun fromName(raw: String?): NavtaraFeedbackRating =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NEUTRAL
    }
}

@Serializable
data class NavtaraProfile(
    val id: Long = 0L,
    val name: String,
    val birthNakshatraIndex: Int,
    val birthPada: Int = 1,
    val notes: String = "",
    val isDefault: Boolean = false,
    val updatedAtMs: Long = 0L
)

@Serializable
data class NavtaraDailyFeedback(
    val profileId: Long,
    val dateIso: String,
    val rating: NavtaraFeedbackRating,
    val note: String = "",
    val updatedAtMs: Long = 0L
)

@Serializable
data class SavedNavtaraReport(
    val id: Long = 0L,
    val profileId: Long,
    val profileName: String,
    val birthNakshatraIndex: Int,
    val birthPada: Int,
    val startDateIso: String,
    val endDateIso: String,
    val isDateRange: Boolean,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val ayanamshaKey: CoordinateSelection = CoordinateSelection.CITRA,
    val monthSystem: MonthSystem = MonthSystem.AMANTA,
    val auspiciousCount: Int = 0,
    val mixedCount: Int = 0,
    val cautionCount: Int = 0,
    val neutralCount: Int = 0,
    val notes: String = "",
    val createdAtMs: Long = 0L
)

@Serializable
enum class NavtaraDayCategory {
    AUSPICIOUS,
    MIXED,
    CAUTION,
    NEUTRAL
}

@Serializable
data class NavtaraPeriodDay(
    val dateIso: String,
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,
    val weekday: Int, // 0 = Sun .. 6 = Sat
    val vaara: String,
    val tithiNumber: Int,
    val tithiName: String,
    val paksha: String,
    val tithiEnds: String? = null,
    val nakshatraNumber: Int,
    val nakshatraName: String,
    val nakshatraEnds: String? = null,
    val nextNakshatraNumber: Int? = null,
    val nextNakshatraName: String? = null,
    val moonRasi: String,
    val currentTara: NavtaraResult,
    val nextTara: NavtaraResult? = null,
    val category: NavtaraDayCategory,
    val recommendationEn: String,
    val recommendationHi: String,
    val feedback: NavtaraDailyFeedback? = null
)

@Serializable
data class NavtaraTransitionItem(
    val dateIso: String,
    val dayOfMonth: Int,
    val fromNakshatraNumber: Int,
    val fromNakshatraName: String,
    val toNakshatraNumber: Int,
    val toNakshatraName: String,
    val transitionTime: String,
    val fromTara: NavtaraResult,
    val toTara: NavtaraResult
)

@Serializable
data class NavtaraPeriodStats(
    val totalDays: Int,
    val auspiciousCount: Int,
    val mixedCount: Int,
    val cautionCount: Int,
    val neutralCount: Int,
    val feedbackCount: Int,
    val auspiciousFeedbackCount: Int,
    val neutralFeedbackCount: Int,
    val cautionFeedbackCount: Int
)

@Serializable
data class NavtaraPeriodReport(
    val profileId: Long,
    val profileName: String,
    val birthNakshatraIndex: Int,
    val birthPada: Int,
    val startDateIso: String,
    val endDateIso: String,
    val isDateRange: Boolean,
    val location: CityLocation,
    val ayanamsha: CoordinateSelection,
    val monthSystem: MonthSystem,
    val days: List<NavtaraPeriodDay>,
    val transitions: List<NavtaraTransitionItem>,
    val stats: NavtaraPeriodStats
)

@Serializable
data class SavedKundliChart(
    val id: Long = 0L,
    val input: KundliBirthInput,
    val lagnaSignIndex: Int,
    val moonSignIndex: Int,
    val moonNakshatraNumber: Int,
    val createdAtMs: Long,
    val updatedAtMs: Long
)
