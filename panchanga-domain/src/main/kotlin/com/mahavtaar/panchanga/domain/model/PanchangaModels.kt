package com.mahavtaar.panchanga.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(val code: String, val nativeLabel: String, val shortBadge: String) {
    ENGLISH("en", "English", "EN"),
    HINDI("hi", "हिन्दी", "हि");

    fun toggled(): AppLanguage = if (this == ENGLISH) HINDI else ENGLISH

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

@Serializable
enum class AppThemeMode {
    NIGHT_SKY,
    PARCHMENT,
    SYSTEM
}

@Serializable
enum class MonthSystem(val apiKey: String, val displayLabel: String) {
    AMANTA("amanta", "Amānta (New Moon to New Moon)"),
    PURNIMANTA("purnimanta", "Pūrṇimānta (Full Moon to Full Moon)");

    companion object {
        fun fromKey(key: String?): MonthSystem =
            entries.firstOrNull { it.apiKey.equals(key, ignoreCase = true) } ?: AMANTA
    }
}

@Serializable
enum class CoordinateSelection(val apiKey: String, val displayLabel: String) {
    CITRA("citra", "Chitra Paksha (Lahiri)"),
    REVATI("revati", "Revati (Usha-Shashi)"),
    ROHINI("rohini", "Rohini"),
    PUSHYA("pushya", "Pushya"),
    MULA("mula", "Mula"),
    KRISHNAMURTI("krishnamurti", "Krishnamurti (KP)"),
    RAMAN("raman", "B.V. Raman"),
    TROPICAL("tropical", "Tropical (Sayana)");

    companion object {
        fun fromKey(key: String?): CoordinateSelection =
            entries.firstOrNull { it.apiKey.equals(key, ignoreCase = true) } ?: CITRA
    }
}

@Serializable
enum class SwaraNadi(val label: String, val nostril: String, val sanskritName: String) {
    IDA("Ida", "Left", "इड़ा नाड़ी (चन्द्र स्वर)"),
    PINGALA("Pingala", "Right", "पिङ्गला नाड़ी (सूर्य स्वर)"),
    SUSHUMNA("Sushumna", "Both", "सुषुम्णा नाड़ी (मध्य / ब्रह्म स्वर)")
}

@Serializable
enum class TattvaElement(val displayName: String, val sanskritName: String, val quality: String) {
    PRITHVI("Earth", "Pṛthvī", "Steady Success"),
    JALA("Water", "Jala", "Fluid Gain"),
    TEJAS("Fire", "Agni / Tejas", "High Energy / Caution"),
    VAYU("Air", "Vāyu", "Movement / Mobility"),
    AKASH("Space", "Ākāśa", "Spiritual Contemplation")
}

@Serializable
data class CityLocation(
    val name: String,
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val population: Long = 0L,
    val isGpsResolved: Boolean = false,
    val isCustom: Boolean = false,
    val isDefault: Boolean = false,
    val accuracyMeters: Double? = null
)

@Serializable
data class TimingInterval(
    val start: String,
    val end: String,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
    val name: String? = null,
    val isAuspicious: Boolean = false
)

@Serializable
data class SwaraBoundaryEvent(
    val type: String, // "start" or "end"
    val nadi: SwaraNadi,
    val nadiLabel: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val formattedRange: String,
    val hasOverlap: Boolean = false
)

@Serializable
data class AngaSwaraInfo(
    val startNadi: SwaraNadi,
    val endNadi: SwaraNadi,
    val startEvent: SwaraBoundaryEvent? = null,
    val endEvent: SwaraBoundaryEvent? = null,
    val hasOverlap: Boolean = false
)

@Serializable
data class AngaSegment(
    val number: Int,
    val name: String,
    val starts: String? = null,
    val ends: String? = null,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
    val swaraInfo: AngaSwaraInfo? = null
)

@Serializable
data class PlanetPosition(
    val id: String,
    val name: String,
    val sanskritName: String,
    val symbol: String,
    val tropicalLongitude: Double,
    val siderealLongitude: Double,
    val rasi: String,
    val rasiNumber: Int,
    val degreesInRasi: String,
    val degreesInRasiNum: Double,
    val progressPercent: Int,
    val nakshatra: String,
    val nakshatraNumber: Int,
    val pada: Int,
    val isRetrograde: Boolean,
    val isCombust: Boolean = false,
    val combustDistanceDeg: Double? = null
)

@Serializable
data class PlanetTransitionEvent(
    val id: String,
    val planetId: String,
    val planetName: String,
    val sanskritName: String,
    val symbol: String,
    val type: String, // "rasi" or "nakshatra"
    val timestampMs: Long,
    val dateStr: String,
    val timeStr: String,
    val dayOfWeek: String,
    val relativeText: String,
    val isToday: Boolean,
    val fromValue: String,
    val toValue: String,
    val fromSanskrit: String,
    val toSanskrit: String,
    val specialName: String? = null,
    val punyaKala: TimingInterval? = null,
    val mahaPunyaKala: TimingInterval? = null,
    val description: String
)

@Serializable
data class SwaraWindow(
    val label: String,
    val start: String,
    val end: String,
    val windowFormatted: String,
    val nadi: SwaraNadi,
    val ruleDescription: String,
    val isActive: Boolean = false,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null
)

@Serializable
data class SwaraYogaState(
    val dayNumber: Int,
    val tithiName: String,
    val paksha: String,
    val sunriseSwara: SwaraNadi,
    val sunsetSwara: SwaraNadi,
    val moonriseSwara: SwaraNadi,
    val moonsetSwara: SwaraNadi,
    val sunriseWindow: SwaraWindow,
    val middayWindow: SwaraWindow,
    val sunsetWindow: SwaraWindow,
    val moonriseWindow: SwaraWindow? = null,
    val moonsetWindow: SwaraWindow? = null,
    val activeCelestialWindow: String? = null,
    val currentActiveSwara: SwaraNadi,
    val auspiciousWorks: List<String>,
    val inauspiciousWorks: List<String>,
    val energySummary: String,
    val rulingDeities: String
)

@Serializable
data class TattvaPeriod(
    val name: String,
    val sanskrit: String,
    val element: TattvaElement,
    val durationMs: Long,
    val durationMins: Int,
    val startTime: String,
    val endTime: String,
    val startTimeMs: Long,
    val endTimeMs: Long
)

@Serializable
data class HoraPeriod(
    val index: Int,
    val isDay: Boolean,
    val startTime: String,
    val endTime: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val ruler: String,
    val nadi: SwaraNadi,
    val tattvas: List<TattvaPeriod>
)

@Serializable
data class DailyHoras(
    val date: String,
    val timezone: String,
    val horas: List<HoraPeriod>,
    val activeHora: HoraPeriod? = null,
    val activeTattva: TattvaPeriod? = null
)

@Serializable
data class VratObservance(
    val id: String,
    val title: String,
    val subtitle: String,
    val deity: String,
    val category: String, // "ekadashi", "purnima", "amavasya", "pradosham", "chaturthi", "shivaratri", "festival"
    val significance: String,
    val practices: List<String> = emptyList()
)

@Serializable
data class LunarPhaseInfo(
    val phaseName: String,
    val illuminationPercent: Int,
    val elongationDegrees: Double,
    val isWaxing: Boolean
)

@Serializable
data class PanchangaSnapshot(
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val dateIso: String, // YYYY-MM-DD
    val dateDisplay: String, // DD/MM/YYYY
    val timezone: String,
    val jd: Double,
    val sunriseJd: Double,
    val coordinateMode: String, // "sidereal" or "tropical"
    val coordinateLabel: String,
    val ayanamsha: String?,
    val ayanamshaKey: CoordinateSelection,
    val ayanamshaDegrees: Double?,
    val monthSystem: MonthSystem,
    val monthSystemLabel: String,
    val samvatsara: String,
    val ayana: String,
    val drikAyana: String,
    val masa: String,
    val masaNumber: Int,
    val isAdhika: Boolean,
    val isKshaya: Boolean = false,
    val rtu: String,
    val drikRtu: String,
    val vaara: String,
    val weekday: Int, // 0 = Sun .. 6 = Sat
    val kaliDay: Long,
    val sakaYear: Int,
    val kaliYear: Int,
    val vikramaYear: Int,
    val sunrise: String,
    val sunset: String,
    val solarNoon: String,
    val nextSunrise: String,
    val sunriseHours: Double,
    val sunsetHours: Double,
    val nextSunriseHours: Double,
    val previousSunriseMs: Long,
    val previousSunsetMs: Long,
    val sunriseMs: Long,
    val sunsetMs: Long,
    val nextSunriseMs: Long,
    val moonrise: String?,
    val moonriseStatus: String,
    val moonset: String?,
    val moonsetStatus: String,
    val dayDuration: String,
    val nightDuration: String,
    val paksha: String, // "Śukla" or "Kṛṣṇa"
    val lunarPhase: LunarPhaseInfo,
    val rahuKala: TimingInterval,
    val yamaganda: TimingInterval,
    val gulikaKala: TimingInterval,
    val abhijitMuhurta: TimingInterval,
    val brahmaMuhurta: TimingInterval,
    val amritaKala: List<TimingInterval>,
    val durmuhurta: List<TimingInterval>,
    val varjyam: List<TimingInterval>,
    val gauriChoghadiyaDay: List<TimingInterval>,
    val gauriChoghadiyaNight: List<TimingInterval>,
    val tithi: List<AngaSegment>,
    val nakshatra: List<AngaSegment>,
    val yoga: List<AngaSegment>,
    val karana: List<AngaSegment>,
    val planets: List<PlanetPosition>,
    val sunRasi: String,
    val moonRasi: String,
    val swaraYoga: SwaraYogaState,
    val upcomingTransits: List<PlanetTransitionEvent>,
    val observances: List<VratObservance>,
    val godhuliMuhurta: TimingInterval? = null,
    val vijayaMuhurta: TimingInterval? = null,
    val nishitaMuhurta: TimingInterval? = null,
    val pratahSandhya: TimingInterval? = null,
    val sayahnaSandhya: TimingInterval? = null,
    val dishaShoolaEn: String = "",
    val dishaShoolaHi: String = "",
    val panchakaStatusEn: String = "",
    val panchakaStatusHi: String = "",
    val hasBhadra: Boolean = false,
    val moonriseMs: Long? = null,
    val moonsetMs: Long? = null
)

@Serializable
data class MonthlyCalendarDay(
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,
    val dateIso: String,
    val vaara: String,
    val masa: String,
    val isAdhika: Boolean,
    val paksha: String,
    val primaryTithiNumber: Int,
    val tithiName: String,
    val tithiEnds: String?,
    val nakshatraName: String,
    val nakshatraEnds: String?,
    val yogaName: String,
    val karanaName: String,
    val sunrise: String,
    val sunset: String,
    val observances: List<VratObservance> = emptyList(),
    val isMasaTransition: Boolean = false
)

@Serializable
enum class WidgetGlassStyle {
    AUTO,
    LIGHT,
    DARK
}

@Serializable
data class UserPreferences(
    val selectedLocation: CityLocation = CityLocation(
        name = "Bhopal, IN",
        country = "IN",
        latitude = 23.2599,
        longitude = 77.4126,
        timezone = "Asia/Kolkata",
        population = 1798218
    ),
    val monthSystem: MonthSystem = MonthSystem.AMANTA,
    val ayanamsha: CoordinateSelection = CoordinateSelection.CITRA,
    val themeMode: AppThemeMode = AppThemeMode.PARCHMENT,
    val notifySunrise: Boolean = false,
    val notifySunset: Boolean = false,
    val notifyTithiChange: Boolean = false,
    val notifyNakshatraChange: Boolean = false,
    val notifyVratReminders: Boolean = true,
    val use24HourFormat: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val birthNakshatra: Int = 1,
    val tattvaMinimalMode: Boolean = false,
    val tattvaHiddenFeatures: Set<String> = emptySet(),
    val hasCompletedOnboarding: Boolean = true,
    val pinnedHomeModules: List<String> = DEFAULT_PINNED_HOME_MODULES,
    val widgetGlassStyle: WidgetGlassStyle = WidgetGlassStyle.AUTO,
    val widgetOpacity: Float = DEFAULT_WIDGET_OPACITY
) {
    companion object {
        const val DEFAULT_WIDGET_OPACITY: Float = 0.62f
        const val MIN_WIDGET_OPACITY: Float = 0.15f
        const val MAX_WIDGET_OPACITY: Float = 0.95f

        val DEFAULT_PINNED_HOME_MODULES: List<String> = listOf(
            "schedule",
            "swara",
            "tattva",
            "notif_studio"
        )
    }
}
