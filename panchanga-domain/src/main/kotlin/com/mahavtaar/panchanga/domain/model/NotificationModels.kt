package com.mahavtaar.panchanga.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class VedicAlertCategory {
    TATTVA,
    SWARA_NADI,
    HORA,
    MUHURTA,
    ANGAS_CELESTIAL,
    VRAT_FESTIVAL
}

@Serializable
enum class AlertDeliveryMode {
    TUNE_AND_VOICE,
    VOICE_ONLY,
    TUNE_ONLY,
    SILENT_BANNER
}

@Serializable
enum class TuneSourceType {
    BUILTIN_VEDIC,
    SYSTEM_RINGTONE,
    CUSTOM_AUDIO_URI
}

@Serializable
enum class VoiceSourceType {
    BUILTIN_TTS,
    CUSTOM_RECORDING
}

@Serializable
enum class BuiltinVedicTone(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val subtitleEn: String,
    val subtitleHi: String
) {
    TEMPLE_GHANTI(
        id = "temple_ghanti",
        titleEn = "Temple Ghanti",
        titleHi = "मन्दिर घंटी",
        subtitleEn = "Resonant bronze temple bell",
        subtitleHi = "पावन कांस्य मन्दिर घंटी"
    ),
    SHANKH_RESONANCE(
        id = "shankh_resonance",
        titleEn = "Shankh Naad",
        titleHi = "शंख नाद",
        subtitleEn = "Sacred conch harmonic swell",
        subtitleHi = "मंगल शंख ध्वनि"
    ),
    OM_TANPURA_DRONE(
        id = "om_tanpura",
        titleEn = "Om Tanpura",
        titleHi = "ॐ तानपूरा",
        subtitleEn = "136.1 Hz meditative drone",
        subtitleHi = "ध्यानमग्न ॐ तानपूरा स्वर"
    ),
    SINGING_BOWL(
        id = "singing_bowl",
        titleEn = "Singing Bowl",
        titleHi = "ध्यान पात्र",
        subtitleEn = "432 Hz tranquil singing bowl",
        subtitleHi = "शांत ४३२ Hz ध्यान ध्वनि"
    ),
    SOFT_CHIME(
        id = "soft_chime",
        titleEn = "Soft Chime",
        titleHi = "कोमल झंकार",
        subtitleEn = "Gentle ascending three-note chime",
        subtitleHi = "मृदु स्वर झंकार"
    );

    companion object {
        fun fromId(id: String?): BuiltinVedicTone =
            entries.firstOrNull { it.id == id } ?: TEMPLE_GHANTI
    }
}

@Serializable
enum class VedicAlertEventType(
    val category: VedicAlertCategory,
    val titleEn: String,
    val titleHi: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val defaultEnabled: Boolean,
    val defaultLeadMinutes: Int,
    val defaultDeliveryMode: AlertDeliveryMode,
    val defaultTone: BuiltinVedicTone
) {
    // 1. Tattva Transitions
    HORA_SUB_TATTVA(
        category = VedicAlertCategory.TATTVA,
        titleEn = "Hora Sub-Tattva Shift",
        titleHi = "होरा उप-तत्व परिवर्तन",
        descriptionEn = "Announces every Prithvi, Jala, Agni, Vayu & Akasha transition",
        descriptionHi = "प्रत्येक पृथ्वी, जल, अग्नि, वायु व आकाश तत्व बदलने पर सूचना",
        defaultEnabled = true,
        defaultLeadMinutes = 0,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SINGING_BOWL
    ),
    VARA_TATTVA(
        category = VedicAlertCategory.TATTVA,
        titleEn = "Daily Vara Tattva",
        titleHi = "दैनिक वार तत्व",
        descriptionEn = "Sunrise elemental ruling Tattva of the Vedic weekday",
        descriptionHi = "सूर्योदय पर दिन के प्रधान वार तत्व की घोषणा",
        defaultEnabled = false,
        defaultLeadMinutes = 0,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.OM_TANPURA_DRONE
    ),
    TITHI_TATTVA(
        category = VedicAlertCategory.TATTVA,
        titleEn = "Tithi Tattva Shift",
        titleHi = "तिथि तत्व परिवर्तन",
        descriptionEn = "Announces when the lunar Tithi elemental group changes",
        descriptionHi = "तिथि समूह के तत्व परिवर्तन पर सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 10,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SINGING_BOWL
    ),

    // 2. Swara Nadi Transitions
    SWARA_SUNRISE_NADI(
        category = VedicAlertCategory.SWARA_NADI,
        titleEn = "Sunrise Swara Nadi",
        titleHi = "सूर्योदय स्वर नाड़ी",
        descriptionEn = "Announces whether Ida or Pingala begins at sunrise (1st Day Hora)",
        descriptionHi = "सूर्योदय (प्रथम होरा) पर इड़ा या पिंगला नाड़ी आरम्भ होने की घोषणा",
        defaultEnabled = true,
        defaultLeadMinutes = 5,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SHANKH_RESONANCE
    ),
    SWARA_MIDDAY_NADI(
        category = VedicAlertCategory.SWARA_NADI,
        titleEn = "Midday Swara Window",
        titleHi = "मध्याह्न स्वर काल",
        descriptionEn = "7th Day Hora solar midday Swara Nadi alignment check",
        descriptionHi = "मध्याह्न (७वीं होरा) में सक्रिय नाड़ी की सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 5,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.OM_TANPURA_DRONE
    ),
    SWARA_SUNSET_NADI(
        category = VedicAlertCategory.SWARA_NADI,
        titleEn = "Sunset Swara Nadi",
        titleHi = "सूर्यास्त स्वर नाड़ी",
        descriptionEn = "12th Day Hora Swara Nadi window start (leads up to Sunset)",
        descriptionHi = "सूर्यास्त (१२वीं होरा) स्वर नाड़ी आरम्भ होने की सूचना",
        defaultEnabled = true,
        defaultLeadMinutes = 5,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.TEMPLE_GHANTI
    ),
    SWARA_MOONRISE_NADI(
        category = VedicAlertCategory.SWARA_NADI,
        titleEn = "Moonrise Swara Window",
        titleHi = "चन्द्रोदय स्वर काल",
        descriptionEn = "1-hour lunar rise Ida/Pingala breath alignment window",
        descriptionHi = "चन्द्रोदय पर स्वर नाड़ी आरम्भ की सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 5,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SOFT_CHIME
    ),
    SWARA_MOONSET_NADI(
        category = VedicAlertCategory.SWARA_NADI,
        titleEn = "Moonset Swara Window",
        titleHi = "चन्द्रास्त स्वर काल",
        descriptionEn = "1-hour lunar set Swara Nadi window start before Moonset",
        descriptionHi = "चन्द्रास्त से १ घण्टा पूर्व स्वर नाड़ी आरम्भ की सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 5,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SOFT_CHIME
    ),
    SWARA_TITHI_BOUNDARY(
        category = VedicAlertCategory.SWARA_NADI,
        titleEn = "Tithi Boundary Swara",
        titleHi = "तिथि संधि स्वर नाड़ी",
        descriptionEn = "1-hour Shiva Svarodaya Nadi transition window at Tithi change",
        descriptionHi = "तिथि परिवर्तन पर नाड़ी संधि काल की घोषणा",
        defaultEnabled = true,
        defaultLeadMinutes = 10,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.OM_TANPURA_DRONE
    ),
    SWARA_NAKSHATRA_BOUNDARY(
        category = VedicAlertCategory.SWARA_NADI,
        titleEn = "Nakshatra Boundary Swara",
        titleHi = "नक्षत्र संधि स्वर नाड़ी",
        descriptionEn = "Swara Nadi shift when Moon enters a new Nakshatra",
        descriptionHi = "नक्षत्र बदलने पर इड़ा/पिंगला नाड़ी की सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 10,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SINGING_BOWL
    ),

    // 3. Planetary Horas
    PLANETARY_HORA(
        category = VedicAlertCategory.HORA,
        titleEn = "Planetary Hora Change",
        titleHi = "ग्रह होरा परिवर्तन",
        descriptionEn = "Every day & night Hora ruler transition (Sun, Moon, Mars, Mercury, Jupiter, Venus, Saturn)",
        descriptionHi = "दिन व रात्रि की प्रत्येक ग्रह होरा बदलने पर सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 5,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SOFT_CHIME
    ),

    // 4. Muhurtas & Choghadiya
    BRAHMA_MUHURTA(
        category = VedicAlertCategory.MUHURTA,
        titleEn = "Brahma Muhūrta",
        titleHi = "ब्रह्म मुहूर्त",
        descriptionEn = "Pre-dawn sacred meditation & sadhana window",
        descriptionHi = "प्रातःकालीन पावन साधना एवं ध्यान मुहूर्त",
        defaultEnabled = false,
        defaultLeadMinutes = 10,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SHANKH_RESONANCE
    ),
    ABHIJIT_MUHURTA(
        category = VedicAlertCategory.MUHURTA,
        titleEn = "Abhijit Muhūrta",
        titleHi = "अभिजित मुहूर्त",
        descriptionEn = "Midday auspicious victory window for new undertakings",
        descriptionHi = "शुभ कार्यों हेतु मध्याह्न का सर्वोत्तम मुहूर्त",
        defaultEnabled = true,
        defaultLeadMinutes = 10,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.TEMPLE_GHANTI
    ),
    RAHU_KALA(
        category = VedicAlertCategory.MUHURTA,
        titleEn = "Rāhu Kāla Caution",
        titleHi = "राहु काल चेतावनी",
        descriptionEn = "Daily inauspicious Rahu Kala start warning",
        descriptionHi = "दैनिक राहु काल आरम्भ होने से पूर्व व समय पर सूचना",
        defaultEnabled = true,
        defaultLeadMinutes = 10,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SOFT_CHIME
    ),
    YAMAGANDA(
        category = VedicAlertCategory.MUHURTA,
        titleEn = "Yamagaṇḍa Kāla",
        titleHi = "यमगण्ड काल",
        descriptionEn = "Caution alert for daily Yamaganda period",
        descriptionHi = "यमगण्ड काल के आरम्भ की सावधानी सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 5,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SOFT_CHIME
    ),
    GULIKA_KALA(
        category = VedicAlertCategory.MUHURTA,
        titleEn = "Gulika Kāla",
        titleHi = "गुलिक काल",
        descriptionEn = "Daily Gulika / Mandi Kala alert",
        descriptionHi = "गुलिक काल की समय पर सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 5,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SOFT_CHIME
    ),
    AUSPICIOUS_CHOGHADIYA(
        category = VedicAlertCategory.MUHURTA,
        titleEn = "Shubh / Amrit Choghadiya",
        titleHi = "शुभ / अमृत चौघड़िया",
        descriptionEn = "Alert when Amrit, Shubh, or Labh Choghadiya begins",
        descriptionHi = "अमृत, शुभ अथवा लाभ चौघड़िया आरम्भ होने पर सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 5,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.TEMPLE_GHANTI
    ),

    // 5. Five Angas & Sun/Moon
    SUNRISE(
        category = VedicAlertCategory.ANGAS_CELESTIAL,
        titleEn = "Local Sunrise",
        titleHi = "स्थानीय सूर्योदय",
        descriptionEn = "Daily sunrise Arghya & Vedic day start reminder",
        descriptionHi = "दैनिक सूर्योदय अर्घ्य एवं वैदिक दिवस आरम्भ सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 10,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SHANKH_RESONANCE
    ),
    SUNSET(
        category = VedicAlertCategory.ANGAS_CELESTIAL,
        titleEn = "Local Sunset (Sandhya)",
        titleHi = "स्थानीय सूर्यास्त (संध्या)",
        descriptionEn = "Evening Sandhya & lamp-lighting reminder",
        descriptionHi = "सायं संध्या व दीपदान हेतु सूर्यास्त सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 10,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.TEMPLE_GHANTI
    ),
    MOONRISE(
        category = VedicAlertCategory.ANGAS_CELESTIAL,
        titleEn = "Local Moonrise",
        titleHi = "स्थानीय चन्द्रोदय",
        descriptionEn = "Chandrodaya reminder for Sankashti & lunar Vrats",
        descriptionHi = "संकष्टी एवं चन्द्र व्रत हेतु चन्द्रोदय सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 15,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SINGING_BOWL
    ),
    MOONSET(
        category = VedicAlertCategory.ANGAS_CELESTIAL,
        titleEn = "Local Moonset",
        titleHi = "स्थानीय चन्द्रास्त",
        descriptionEn = "Exact local Moonset (Chandrasta) reminder for lunar Vrats & sadhana",
        descriptionHi = "स्थानीय चन्द्रास्त के सटीक समय एवं पूर्व रिमाइंडर की सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 15,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SINGING_BOWL
    ),
    TITHI_CHANGE(
        category = VedicAlertCategory.ANGAS_CELESTIAL,
        titleEn = "Tithi Transition",
        titleHi = "तिथि परिवर्तन",
        descriptionEn = "Exact moment when the active lunar Tithi changes",
        descriptionHi = "सक्रिय तिथि बदलने के सटीक समय पर सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 15,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SOFT_CHIME
    ),
    NAKSHATRA_CHANGE(
        category = VedicAlertCategory.ANGAS_CELESTIAL,
        titleEn = "Nakṣatra Transition",
        titleHi = "नक्षत्र परिवर्तन",
        descriptionEn = "Exact moment when Moon enters the next Nakshatra",
        descriptionHi = "चन्द्रमा के अगले नक्षत्र में प्रवेश पर सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 15,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SOFT_CHIME
    ),
    YOGA_CHANGE(
        category = VedicAlertCategory.ANGAS_CELESTIAL,
        titleEn = "Yoga Transition",
        titleHi = "योग परिवर्तन",
        descriptionEn = "When the active Luni-Solar Yoga changes",
        descriptionHi = "सूर्य-चन्द्र योग बदलने पर सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 10,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SOFT_CHIME
    ),
    KARANA_CHANGE(
        category = VedicAlertCategory.ANGAS_CELESTIAL,
        titleEn = "Karaṇa Transition",
        titleHi = "करण परिवर्तन",
        descriptionEn = "Half-Tithi Karana change including Vishti (Bhadra)",
        descriptionHi = "करण परिवर्तन एवं भद्रा (विष्टि) सूचना",
        defaultEnabled = false,
        defaultLeadMinutes = 10,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SOFT_CHIME
    ),

    // 6. Vratas & Festivals
    VRAT_OBSERVANCE(
        category = VedicAlertCategory.VRAT_FESTIVAL,
        titleEn = "Vrat Reminders (Ekādaśī / Pradoṣam)",
        titleHi = "पावन व्रत (एकादशी / प्रदोष / चतुर्थी)",
        descriptionEn = "Morning alert on Ekadashi, Purnima, Amavasya, Pradosham & Sankashti",
        descriptionHi = "एकादशी, पूर्णिमा, अमावस्या, प्रदोष व संकष्टी पर प्रातःकालीन सूचना",
        defaultEnabled = true,
        defaultLeadMinutes = 0,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.SHANKH_RESONANCE
    ),
    MAJOR_FESTIVAL(
        category = VedicAlertCategory.VRAT_FESTIVAL,
        titleEn = "Major Hindu Festivals",
        titleHi = "प्रमुख हिन्दू त्यौहार एवं पर्व",
        descriptionEn = "Sacred festival observance alerts throughout the year",
        descriptionHi = "वर्ष भर के प्रमुख पावन पर्वों एवं त्यौहारों की सूचना",
        defaultEnabled = true,
        defaultLeadMinutes = 0,
        defaultDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
        defaultTone = BuiltinVedicTone.TEMPLE_GHANTI
    );

    fun defaultRule(): NotificationRule = NotificationRule(
        eventType = this,
        isEnabled = defaultEnabled,
        notifyAtStart = true,
        leadTimeMinutes = defaultLeadMinutes,
        deliveryMode = defaultDeliveryMode,
        tuneSource = TuneSourceType.BUILTIN_VEDIC,
        tuneUriOrId = defaultTone.id,
        tuneDisplayName = defaultTone.titleEn,
        voiceSource = VoiceSourceType.BUILTIN_TTS,
        voiceRecordingId = null,
        customHindiTemplate = "",
        customEnglishTemplate = ""
    )

    companion object {
        fun fromName(name: String?): VedicAlertEventType? =
            entries.firstOrNull { it.name == name }
    }
}

@Serializable
data class NotificationRule(
    val eventType: VedicAlertEventType,
    val isEnabled: Boolean,
    val notifyAtStart: Boolean = true,
    val leadTimeMinutes: Int = 5,
    val deliveryMode: AlertDeliveryMode = AlertDeliveryMode.TUNE_AND_VOICE,
    val tuneSource: TuneSourceType = TuneSourceType.BUILTIN_VEDIC,
    val tuneUriOrId: String = BuiltinVedicTone.TEMPLE_GHANTI.id,
    val tuneDisplayName: String = BuiltinVedicTone.TEMPLE_GHANTI.titleEn,
    val voiceSource: VoiceSourceType = VoiceSourceType.BUILTIN_TTS,
    val voiceRecordingId: String? = null,
    val customHindiTemplate: String = "",
    val customEnglishTemplate: String = ""
)

@Serializable
data class VoiceRecording(
    val id: String,
    val displayName: String,
    val filePath: String,
    val durationMs: Long,
    val createdAtMs: Long
)

@Serializable
enum class NotificationPreset(
    val titleEn: String,
    val titleHi: String,
    val subtitleEn: String,
    val subtitleHi: String
) {
    ESSENTIAL(
        titleEn = "Essential Only",
        titleHi = "केवल मुख्य",
        subtitleEn = "Sunrise, Tithi, Abhijit, Rahu Kala & Festivals",
        subtitleHi = "सूर्योदय, तिथि, अभिजित, राहु काल व पर्व"
    ),
    PRACTICE(
        titleEn = "Practice & Meditation",
        titleHi = "साधना एवं ध्यान",
        subtitleEn = "Swara Nadi, Sub-Tattva, Hora & Brahma Muhurta",
        subtitleHi = "स्वर नाड़ी, उप-तत्व, होरा व ब्रह्म मुहूर्त"
    ),
    FULL(
        titleEn = "Full Vedic Alerts",
        titleHi = "सम्पूर्ण वैदिक अलर्ट",
        subtitleEn = "All Tattva, Swara, Hora, Muhurta & Anga alerts",
        subtitleHi = "सभी तत्व, स्वर, होरा, मुहूर्त व पंचांग सूचनाएं"
    ),
    SILENT(
        titleEn = "Quiet / Alerts Off",
        titleHi = "शांत / अलर्ट बंद",
        subtitleEn = "Pause all scheduled alerts for now",
        subtitleHi = "अभी सभी निर्धारित अलर्ट बंद रखें"
    );

    fun enabledEventTypes(): Set<VedicAlertEventType> = when (this) {
        ESSENTIAL -> setOf(
            VedicAlertEventType.SUNRISE,
            VedicAlertEventType.SUNSET,
            VedicAlertEventType.TITHI_CHANGE,
            VedicAlertEventType.ABHIJIT_MUHURTA,
            VedicAlertEventType.RAHU_KALA,
            VedicAlertEventType.VRAT_OBSERVANCE,
            VedicAlertEventType.MAJOR_FESTIVAL
        )
        PRACTICE -> setOf(
            VedicAlertEventType.HORA_SUB_TATTVA,
            VedicAlertEventType.SWARA_SUNRISE_NADI,
            VedicAlertEventType.SWARA_MIDDAY_NADI,
            VedicAlertEventType.SWARA_SUNSET_NADI,
            VedicAlertEventType.SWARA_MOONSET_NADI,
            VedicAlertEventType.SWARA_TITHI_BOUNDARY,
            VedicAlertEventType.PLANETARY_HORA,
            VedicAlertEventType.BRAHMA_MUHURTA,
            VedicAlertEventType.ABHIJIT_MUHURTA,
            VedicAlertEventType.SUNRISE,
            VedicAlertEventType.SUNSET
        )
        FULL -> VedicAlertEventType.entries.toSet()
        SILENT -> emptySet()
    }
}

@Serializable
data class AlertAudioSettings(
    val masterEnabled: Boolean = true,
    val ttsSpeechRate: Float = 0.96f,
    val ttsPitch: Float = 1.0f,
    val manualDndEnabled: Boolean = false,
    val manualDndUntilMs: Long? = null,
    val quietHoursEnabled: Boolean = true,
    val quietStartMinuteOfDay: Int = 22 * 60, // 22:00 (10:00 PM)
    val quietEndMinuteOfDay: Int = 5 * 60,    // 05:00 (5:00 AM)
    val respectSilentAndDnd: Boolean = true,
    val persistentRunningNotificationEnabled: Boolean = false
) {
    /** True when either indefinite Instant DND or an unexpired Timed Mute is active. */
    fun isManualDndActive(nowMs: Long = System.currentTimeMillis()): Boolean =
        manualDndEnabled || (manualDndUntilMs != null && nowMs < manualDndUntilMs)

    /** Returns the active timed-mute expiry timestamp when timed mute (not indefinite DND) is active. */
    fun activeTimedDndUntilMs(nowMs: Long = System.currentTimeMillis()): Long? =
        if (!manualDndEnabled && manualDndUntilMs != null && nowMs < manualDndUntilMs) {
            manualDndUntilMs
        } else {
            null
        }

    fun isWithinQuietHours(minuteOfDay: Int): Boolean {
        if (!quietHoursEnabled) return false
        val m = ((minuteOfDay % 1440) + 1440) % 1440
        val start = ((quietStartMinuteOfDay % 1440) + 1440) % 1440
        val end = ((quietEndMinuteOfDay % 1440) + 1440) % 1440
        if (start == end) return false
        return if (start < end) {
            m in start until end
        } else {
            m >= start || m < end
        }
    }

    /** If [atMs] falls inside Quiet Hours in [zoneId], returns the exact epoch millis when Quiet Hours end. */
    fun resolveQuietHoursEndTimestampMs(atMs: Long, zoneId: java.time.ZoneId): Long? {
        if (!quietHoursEnabled) return null
        val start = ((quietStartMinuteOfDay % 1440) + 1440) % 1440
        val end = ((quietEndMinuteOfDay % 1440) + 1440) % 1440
        if (start == end) return null
        val zdt = java.time.Instant.ofEpochMilli(atMs).atZone(zoneId)
        val minOfDay = zdt.hour * 60 + zdt.minute
        if (!isWithinQuietHours(minOfDay)) return null
        val endHour = end / 60
        val endMin = end % 60
        val endDate = if (start < end || minOfDay < end) {
            zdt.toLocalDate()
        } else {
            zdt.toLocalDate().plusDays(1)
        }
        return endDate.atTime(endHour, endMin).atZone(zoneId).toInstant().toEpochMilli()
    }

    /**
     * Resolves the exact epoch millis when muted alerts will automatically resume.
     * Returns null when indefinite [manualDndEnabled] is active or when not currently muted.
     */
    fun resolveNextResumeTimeMs(nowMs: Long, zoneId: java.time.ZoneId): Long? {
        if (manualDndEnabled) return null
        val timedEnd = activeTimedDndUntilMs(nowMs)
        if (timedEnd != null) {
            val quietAfterTimed = resolveQuietHoursEndTimestampMs(timedEnd, zoneId)
            return if (quietAfterTimed != null && quietAfterTimed > timedEnd) quietAfterTimed else timedEnd
        }
        return resolveQuietHoursEndTimestampMs(nowMs, zoneId)
    }
}

@Serializable
enum class ScheduledAlertStatus {
    SCHEDULED,
    DELIVERED,
    SUPPRESSED_QUIET,
    SUPPRESSED_DND
}

@Serializable
data class ScheduledAlertItem(
    val id: String,
    val requestCode: Int,
    val eventType: VedicAlertEventType,
    val triggerTimeMs: Long,
    val eventStartTimeMs: Long,
    val eventEndTimeMs: Long? = null,
    val isAdvanceWarning: Boolean,
    val leadTimeMinutes: Int,
    val titleEn: String,
    val titleHi: String,
    val bodyEn: String,
    val bodyHi: String,
    val speechEn: String,
    val speechHi: String,
    val deliveryMode: AlertDeliveryMode,
    val tuneSource: TuneSourceType,
    val tuneUriOrId: String,
    val voiceSource: VoiceSourceType,
    val voiceRecordingId: String?,
    val status: ScheduledAlertStatus = ScheduledAlertStatus.SCHEDULED,
    val updatedAtMs: Long = 0L
)
