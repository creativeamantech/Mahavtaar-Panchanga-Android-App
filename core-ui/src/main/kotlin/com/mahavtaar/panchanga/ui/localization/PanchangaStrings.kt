package com.mahavtaar.panchanga.ui.localization

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.mahavtaar.panchanga.domain.model.AppLanguage

/**
 * Bilingual UI string contract for English and Hindi (Devanagari).
 * Implemented as singleton objects rather than a data class constructor to avoid
 * the JVM/DEX 255-parameter constructor register limit.
 */
@Immutable
interface PanchangaStrings {
    // App & Navigation
    val appTitle: String
    val navHome: String
    val navPanchanga: String
    val navCalendar: String
    val navHora: String
    val navMore: String

    // Common Actions & Header
    val back: String
    val today: String
    val select: String
    val cancel: String
    val retry: String
    val details: String
    val all: String
    val active: String
    val live: String
    val now: String
    val leftSuffix: String
    val untilPrefix: String
    val endsPrefix: String
    val fullDay: String
    val loadingEphemeris: String
    val loadingCalendar: String
    val calculationError: String
    val changeLocationCd: String
    val switchLanguageCd: String
    val previousDayCd: String
    val nextDayCd: String
    val pickDateCd: String
    val previousMonthCd: String
    val nextMonthCd: String

    // Home Dashboard
    val pakshaSuffix: String
    val litSuffix: String
    val vikramaLabel: String
    val sakaLabel: String
    val samvatsaraLabel: String
    val modeLabel: String
    val sunAndMoonTimings: String
    val dayLabel: String
    val nightLabel: String
    val solarNoonLabel: String
    val sunrise: String
    val sunset: String
    val moonrise: String
    val moonset: String
    val noRiseToday: String
    val noSetToday: String
    val fiveLimbsTitle: String
    val tithiLabel: String
    val nakshatraLabel: String
    val yogaLabel: String
    val karanaLabel: String
    val varaLabel: String
    val currentHora: String
    val horaSuffix: String
    val swaraYogaTitle: String
    val keyTimingsToday: String
    val keyTimingsSubtitle: String
    val upcomingEventsTitle: String
    val upcomingEventsSubtitle: String
    val navagrahaShort: String

    // Daily Panchanga
    val tabFiveAngas: String
    val tabMuhurtasChoghadiya: String
    val anga1TithiTitle: String
    val anga2NakshatraTitle: String
    val anga3YogaTitle: String
    val anga4KaranaTitle: String
    val anga4KaranaSubtitle: String
    val anga5VaraTitle: String
    val moonRasiPrefix: String
    val sunRasiPrefix: String
    val kaliDayLabel: String
    val startsLabel: String
    val endsLabel: String
    val beforeSunrise: String
    val continuesPastNextSunrise: String
    val durationPrefix: String
    val remainingPrefix: String
    val nextTransitionPrefix: String
    val startSwaraPrefix: String
    val varaRunsSubtitle: (sunrise: String, nextSunrise: String) -> String
    val muhurtasTitle: String
    val muhurtasSubtitle: String
    val auspiciousMuhurtas: String
    val inauspiciousKalas: String
    val dayChoghadiyaTitle: String
    val nightChoghadiyaTitle: String
    val periodFallback: String

    // Planetary Positions
    val planetsTopBarTitle: String
    val ayanamshaPrefix: String
    val tropicalSayanaLabel: String
    val navagrahaLongitudesTitle: String
    val computedAtSunrise: (sunrise: String, tz: String) -> String
    val retrogradeBadge: String
    val combustBadge: String
    val padaLabel: String
    val transits30DayTitle: String
    val transits30DaySubtitle: String
    val punyaKalaPrefix: String

    // Hindu Calendar
    val adhikaMasaBadge: String
    val masaTransitionBadge: String
    val openFullPanchanga: String
    val festivalsAndVratsTitle: String
    val sacredObservancesCount: (count: Int) -> String

    // Hora Screen
    val dayHorasTab: String
    val nightHorasTab: String
    val activePlanetaryHora: String
    val subTattvaLabel: String
    val upcomingPrefix: String
    val atTimeWord: String
    val nadiWord: String
    val fiveSubTattvas: String
    val minShort: String
    val collapseTattvasCd: String
    val expandTattvasCd: String

    // Swara Yoga Screen
    val swaraTopBarTitle: String
    val dayNumberPrefix: String
    val deitiesPrefix: String
    val fiveCelestialWindowsTitle: String
    val fiveCelestialWindowsSubtitle: String
    val tithiBoundarySwaraTitle: String
    val tithiBoundarySwaraSubtitle: String
    val nakshatraBoundarySwaraTitle: String
    val nakshatraBoundarySwaraSubtitle: String
    val startPrefix: String
    val endPrefix: String
    val guidanceTitle: String
    val favorableWorks: String
    val bestAvoided: String

    // More Hub & Locations
    val exploreAndConfigure: String
    val moreSwaraTitle: String
    val moreSwaraSubtitle: String
    val morePlanetsTitle: String
    val morePlanetsSubtitle: String
    val moreLocationsTitle: String
    val currentLocationPrefix: String
    val moreSettingsTitle: String
    val moreSettingsSubtitle: String
    val moreAboutTitle: String
    val moreAboutSubtitle: String
    val selectLocationTitle: String
    val acquiringGps: String
    val useCurrentGps: String
    val searchCityHint: String
    val savedLocationsTitle: String
    val savedLocationsSubtitle: String
    val popularCitiesTitle: String
    val matchingCitiesTitle: String
    val offlineCatalogSubtitle: String
    val activeCityCd: String
    val removeLocationCd: String

    // Settings & About
    val settingsTitle: String
    val languageSectionTitle: String
    val languageSectionSubtitle: String
    val appearanceTitle: String
    val appearanceSubtitle: String
    val themeNightSky: String
    val themeParchment: String
    val themeSystem: String
    val lunarTraditionTitle: String
    val lunarTraditionSubtitle: String
    val ayanamshaSectionTitle: String
    val ayanamshaSectionSubtitle: String
    val use24HourTitle: String
    val use24HourSubtitle: String
    val notificationsTitle: String
    val notificationsSubtitle: String
    val notifySunriseTitle: String
    val notifySunriseSubtitle: String
    val notifySunsetTitle: String
    val notifySunsetSubtitle: String
    val notifyTithiTitle: String
    val notifyTithiSubtitle: String
    val notifyVratTitle: String
    val notifyVratSubtitle: String
    val offlineStorageTitle: String
    val offlineStorageSubtitle: String
    val clearEphemerisCache: String
    val cacheClearedStatus: String
    val aboutTopBarTitle: String
    val aboutLeadCopy: String
    val aboutMethodologyTitle: String
    val aboutMethodologySubtitle: String
    val aboutMethodologyBody: String
    val aboutVersionTitle: String
    val aboutAttribution: String
    val aboutVersionBody: String

    // Home Hub & Invocation
    val vedicInvocationBanner: String
    val vedicModulesTitle: String
    val vedicModulesSubtitle: String
    val runningNowTitle: String

    // More Hub Section Headers & New Destinations
    val sectionDailyTime: String
    val sectionAstrologyKundli: String
    val sectionSwarodayaTattva: String
    val sectionFestivalsVratas: String
    val sectionAppConfig: String
    val moreScheduleTitle: String
    val moreScheduleSubtitle: String
    val moreKundliTitle: String
    val moreKundliSubtitle: String
    val moreGunaMilanTitle: String
    val moreGunaMilanSubtitle: String
    val moreNavagrahaMantrasTitle: String
    val moreNavagrahaMantrasSubtitle: String
    val moreNavtaraTitle: String
    val moreNavtaraSubtitle: String
    val moreTattvaTitle: String
    val moreTattvaSubtitle: String
    val moreFestivalsTitle: String
    val moreFestivalsSubtitle: String

    // Settings Additions
    val birthNakshatraTitle: String
    val birthNakshatraSubtitle: String
    val resetDefaultsTitle: String
    val resetDefaultsSubtitle: String
    val resetDefaultsButton: String
    val defaultsRestoredStatus: String

    // Today's Schedule
    val scheduleTopBarTitle: String
    val filterAll: String
    val filterMuhurta: String
    val filterChoghadiya: String
    val filterPanchanga: String
    val filterHora: String
    val filterAstro: String
    val completedBadge: String

    // Navtara Chakra
    val navtaraTopBarTitle: String
    val selectBirthNakshatraLabel: String
    val todayTransitNakshatraLabel: String
    val taraScoreLabel: String
    val paryayaCycleLabel: String
    val pariharaRemediesTitle: String
    val full27MatrixTitle: String
    val full27MatrixSubtitle: String

    // Pancha Tattva
    val tattvaTopBarTitle: String
    val dominantTattvaLabel: String
    val varaTattvaTitle: String
    val tithiTattvaTitle: String
    val karanaTattvaTitle: String
    val horaSubTattvaTitle: String
    val elementalGuidanceTitle: String

    // Navagraha Mantras
    val navagrahaMantrasTopBarTitle: String
    val beejMantraLabel: String
    val dhyanMantraLabel: String
    val gayatriMantraLabel: String
    val vedoktaMantraLabel: String
    val gemstoneLabel: String
    val samidhaLabel: String
    val directionLabel: String
    val japCountLabel: String
    val charityDaanLabel: String
    val fastDayLabel: String

    // Annual Festivals & Vrats
    val festivalsTopBarTitle: String
    val loadingAnnualFestivals: String
    val searchFestivalsHint: String
    val noFestivalsMatched: String

    // Janam Kundli & Guna Milan
    val kundliTopBarTitle: String
    val birthDetailsTitle: String
    val computeChartButton: String
    val northIndianStyle: String
    val southIndianStyle: String
    val tabChart: String
    val tabPlanets: String
    val tabDasha: String
    val tabVargas: String
    val tabShadbala: String
    val tabAshtakavarga: String
    val tabKpJaimini: String
    val tabGochar: String
    val tabGunaMilan: String
    val lagnaChartTitle: String
    val navamshaChartTitle: String
    val doshaAndYogaTitle: String
    val currentDashaLabel: String
    val arudhaLagnaLabel: String
    val groomSectionTitle: String
    val brideSectionTitle: String
    val manglikToggleLabel: String
    val totalScoreLabel: String

    // Custom Notification & Voice Studio
    val notificationStudioTopBarTitle: String
    val moreNotificationStudioTitle: String
    val moreNotificationStudioSubtitle: String
    val openNotificationStudioButton: String
    val notifTabEvents: String
    val notifTabVoiceQuiet: String
    val notifTabQueue: String
    val notifMasterTitle: String
    val notifMasterSubtitle: String
    val notifNextAlertLabel: String
    val notifNoUpcomingAlerts: String
    val notifPermissionNeededTitle: String
    val notifPermissionNeededBody: String
    val notifGrantPermissionButton: String
    val notifExactAlarmBannerTitle: String
    val notifExactAlarmBannerBody: String
    val notifAllowExactAlarmButton: String
    val notifCategoryAll: String
    val notifCategoryTattva: String
    val notifCategorySwaraNadi: String
    val notifCategoryHora: String
    val notifCategoryMuhurta: String
    val notifCategoryAngas: String
    val notifCategoryVrats: String
    val notifAtStartLabel: String
    val notifLeadTimeTitle: String
    val notifLeadTimeOff: String
    val notifMinutesBeforeSuffix: String
    val notifCustomMinutesLabel: String
    val notifDeliveryModeTitle: String
    val notifModeTuneAndVoice: String
    val notifModeVoiceOnly: String
    val notifModeTuneOnly: String
    val notifModeSilentBanner: String
    val notifTuneSectionTitle: String
    val notifTuneSourceBuiltin: String
    val notifTuneSourceRingtone: String
    val notifTuneSourceAudioFile: String
    val notifPickSystemRingtoneButton: String
    val notifPickAudioFileButton: String
    val notifVoiceSectionTitle: String
    val notifVoiceSourceTts: String
    val notifVoiceSourceRecording: String
    val notifCustomScriptHintEn: String
    val notifCustomScriptHintHi: String
    val notifTestAlertNowButton: String
    val notifSaveRuleButton: String
    val notifBuiltInVoiceTitle: String
    val notifBuiltInVoiceSubtitle: String
    val notifLanguageSyncNote: String
    val notifSpeechRateLabel: String
    val notifSpeechPitchLabel: String
    val notifTestTattvaVoiceButton: String
    val notifTestNadiVoiceButton: String
    val notifVoiceRecorderTitle: String
    val notifVoiceRecorderSubtitle: String
    val notifClipNameLabel: String
    val notifStartRecordingButton: String
    val notifStopRecordingButton: String
    val notifCancelRecordingButton: String
    val notifSavedRecordingsTitle: String
    val notifNoRecordingsYet: String
    val notifQuietHoursTitle: String
    val notifQuietHoursSubtitle: String
    val notifQuietStartLabel: String
    val notifQuietEndLabel: String
    val notifRespectSilentDndTitle: String
    val notifRespectSilentDndSubtitle: String
    val notifDndAndQuietSectionTitle: String
    val notifDndAndQuietSectionSubtitle: String
    val notifInstantDndTitle: String
    val notifInstantDndSubtitle: String
    val notifDndStatusActive: String
    val notifQuietHoursStatusActive: String
    val notifAlertsNormalStatus: String
    val notifUpcomingQueueTitle: String
    val notifRecentHistoryTitle: String
    val notifRescheduleNowButton: String
    val notifResetRulesButton: String
    val notifStatusDelivered: String
    val notifStatusScheduled: String
    val notifStatusSuppressedQuiet: String
    val notifStatusSuppressedDnd: String
    val notifRunningNowTitle: String
    val notifRunningNowSubtitle: String
    val notifRunningNowTurnOffAction: String
}

fun stringsFor(language: AppLanguage): PanchangaStrings = when (language) {
    AppLanguage.ENGLISH -> EnglishStrings
    AppLanguage.HINDI -> HindiStrings
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ENGLISH }
val LocalStrings = staticCompositionLocalOf<PanchangaStrings> { EnglishStrings }
