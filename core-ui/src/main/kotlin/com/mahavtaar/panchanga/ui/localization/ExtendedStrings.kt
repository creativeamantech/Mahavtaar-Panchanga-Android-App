package com.mahavtaar.panchanga.ui.localization

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.mahavtaar.panchanga.domain.model.AppLanguage

@Immutable
interface ExtendedStrings {
    // Home Live Enhancements
    val dinaGhatiLabel: String
    val tithiAtSunriseLabel: String
    val activeWindowAlert: String
    val activeWindowAuspicious: String
    val activeChoghadiyaLabel: String
    val nextMajorIngressTitle: String
    val punyaKalaLabel: String
    val mahaPunyaKalaLabel: String

    // Glossary & Sharing
    val vedicGlossaryTitle: String
    val vedicGlossarySubtitle: String
    val searchGlossaryHint: String
    val usageTipLabel: String
    val closeLabel: String
    val shareReport: String
    val shareSummary: String
    val setAlert: String
    val noResultsFound: String

    // Daily Panchanga Enhancements
    val muhurtaTimelineTitle: String
    val muhurtaTimelineSubtitle: String
    val tapSegmentHint: String
    val listViewLabel: String
    val gridViewLabel: String
    val auspiciousLegend: String
    val cautionLegend: String
    val inauspiciousLegend: String
    val dishaShoolaLabel: String
    val panchakaStatusLabel: String
    val bhadraWarningLabel: String
    val bhadraClearLabel: String
    val godhuliMuhurtaLabel: String
    val vijayaMuhurtaLabel: String
    val nishitaMuhurtaLabel: String
    val pratahSandhyaLabel: String
    val sayahnaSandhyaLabel: String
    val sandhyaAndSpecialMuhurtas: String
    val vaaraDeityLabel: String
    val vaaraRulerLabel: String
    val vaaraColorLabel: String
    val vaaraMetalLabel: String
    val startNadiLabel: String
    val endNadiLabel: String
    val nextAngaLabel: String
    val lunarElongationLabel: String
    val elapsedProgressLabel: String

    // Today Schedule & Hora Enhancements
    val jumpToNow: String
    val currentTimeMarker: String
    val liveHeroTitle: String
    val parentNadiLabel: String
    val subTattvaLabel: String
    val horaFilterAll: String
    val horaFilterDay: String
    val horaFilterNight: String
    val favorableActivitiesLabel: String
    val avoidActivitiesLabel: String
    val nadiGuidanceBanner: String

    // Planetary Positions & Transits
    val tabGrahaCards: String
    val tabTransitChakra: String
    val tabTransitsTimeline: String
    val nextRasiIngressLabel: String
    val nextNakshatraIngressLabel: String
    val rasiNadiLabel: String
    val transitFilterAll: String
    val transitFilterRasi: String
    val transitFilterNakshatra: String
    val transitFilterToday: String
    val todayTransitsBanner: String
    val rasiNumberPrefix: String

    // Swara Yoga, Tattva & Navtara Enhancements
    val swaraTabLive: String
    val swaraTabGuidance: String
    val swaraTabTattvas: String
    val swaraTabNakshatraAlignment: String
    val swaraSlokaHeader: String
    val filterAllDays: String
    val filterShukla: String
    val filterKrishna: String
    val nakshatraAlignmentTitle: String
    val swaraSimulatorTitle: String
    val selectTithiLabel: String
    val selectNakshatraLabel: String
    val selectPadaLabel: String
    val starMasterTableTitle: String
    val zodiacNadiTableTitle: String
    val dayTithiTattvaTitle: String
    val karanaTattvaCycleTitle: String
    val sixMonthCycleBadge: String
    val maasOverlapsTitle: String
    val adhikaMaasWarning: String
    val testChimeHaptic: String
    val tattvaChimeTriggered: String
    val nativeNameHint: String
    val birthPadaLabel: String
    val purusharthaLabel: String
    val nakshatraLordLabel: String
    val nakshatraDeityLabel: String
    val nakshatraGanaLabel: String
    val nakshatraNadiLabel: String
    val gandantaWarningTitle: String
    val liveStarTransitTitle: String
    val vedhaOverrideTitle: String
    val taraDasaTitle: String

    // Kundli Enhancements
    val cityPickerTitle: String
    val searchCityHint: String
    val birthGenderLabel: String
    val genderMale: String
    val genderFemale: String
    val genderOther: String
    val rectifyTimeLabel: String
    val savedProfilesTitle: String
    val saveProfileButton: String
    val noSavedProfiles: String
    val profileSavedMsg: String
    val chartStyleEast: String
    val bhavaPlacementsTitle: String
    val birthDashaBalanceLabel: String
    val shadbalaSubRank: String
    val shadbalaSubMatrix: String
    val shadbalaSubIshtaKashta: String
    val shadbalaSubBhava: String
    val rupasUnit: String
    val virupasUnit: String
    val ishtaPhalaLabel: String
    val kashtaPhalaLabel: String
    val avSubSav: String
    val avSubBav: String
    val avSubShodhana: String
    val avSubKakshya: String
    val bindusUnit: String
    val trikonaShodhitaLabel: String
    val ekadhipatyaShodhitaLabel: String
    val totalSavLabel: String
    val tabKpAstrology: String
    val tabJaimini: String
    val tabAuditExport: String
    val kpRulingPlanetsTitle: String
    val kpCuspsTitle: String
    val kpSignificatorsTitle: String
    val kpLevelsLabel: String
    val charaKarakasTitle: String
    val arudhaPadasTitle: String
    val upapadaLagnaLabel: String
    val karakamshaTitle: String
    val rashiDrishtiTitle: String
    val charaDashaTitle: String
    val filterYogasAll: String
    val filterYogasPresent: String
    val filterYogasMitigated: String
    val searchYogasHint: String
    val matchedConditionsLabel: String
    val failedConditionsLabel: String
    val cancellationsLabel: String
    val transitDateLabel: String
    val selectTransitDate: String
    val resetTransitNow: String
    val sadeSatiCardTitle: String
    val vedhaAlertLabel: String
    val auditProvenanceTitle: String
    val exportTextReport: String
    val exportJsonReport: String
    val kundliNameHint: String
    val kundliPlaceHint: String
    val kundliHourLabel: String
    val kundliMinuteLabel: String

    // Calendar Observance Enhancements
    val significanceLabel: String
    val recommendedPracticesLabel: String
    val addVratReminder: String

    // Custom Location & Precision Coordinates
    val customCoordinatesTab: String
    val addCustomLocationButton: String
    val editLocationTitle: String
    val locationLabelHint: String
    val latitudeHint: String
    val longitudeHint: String
    val timezonePickerTitle: String
    val searchTimezoneHint: String
    val pinAsDefaultLabel: String
    val defaultBadgeLabel: String
    val customBadgeLabel: String
    val gpsBadgeLabel: String
    val invalidLatitudeMsg: String
    val invalidLongitudeMsg: String
    val invalidLocationNameMsg: String
    val saveAndApplyLabel: String

    // Offline HTML Export, Preview & Print
    val saveHtmlReport: String
    val previewAndPrintHtml: String
    val printPdfLabel: String
    val htmlSavedMsg: String
    val jsonSavedMsg: String
    val jsonImportedMsg: String
    val saveJsonFile: String
    val importJsonFile: String
    val reportPreviewTitle: String
    val exportActionError: String
    val calculationConstantsTitle: String
    val ayanamshaFormulaLabel: String
    val houseSystemLabel: String
    val siderealYearDaysLabel: String

    // Navtara Profiles, Live Comparison, Period Planner & Journal
    val navtaraProfilesTitle: String
    val addProfileLabel: String
    val editProfileLabel: String
    val setDefaultLabel: String
    val liveNakshatraShiftTitle: String
    val currentNakshatraLabel: String
    val incomingNakshatraLabel: String
    val transitPlannerTitle: String
    val fullMonthModeLabel: String
    val customRangeModeLabel: String
    val preset15Days: String
    val preset30Days: String
    val presetThisMonth: String
    val presetNextMonth: String
    val generateReportLabel: String
    val saveReportLocallyLabel: String
    val savedReportsLabel: String
    val reportSavedSuccessMsg: String
    val dailyJournalLabel: String
    val journalNoteHint: String
    val saveFeedbackLabel: String
    val reflectionStatsTitle: String
    val transitionsTitle: String
    val auspiciousDaysLabel: String
    val mixedDaysLabel: String
    val cautionDaysLabel: String
    val neutralDaysLabel: String
    val navtaraPeriodTabToday: String
    val navtaraPeriodTabPlanner: String
    val navtaraPeriodTabSaved: String
    val noSavedReportsMsg: String

    // Google Sign-In & Cloud Sync
    val signInScreenTitle: String
    val signInScreenSubtitle: String
    val signInFeatureCloudSync: String
    val signInFeatureOfflineReady: String
    val signInFeatureMultiDevice: String
    val signInWithGoogleButton: String
    val signingInProgress: String
    val signInCancelledMsg: String
    val cloudAccountSectionTitle: String
    val cloudAccountSectionSubtitle: String
    val signedInAsLabel: String
    val syncNowButton: String
    val syncingNowLabel: String
    val lastSyncedLabel: String
    val notSyncedYetLabel: String
    val signOutButton: String

    // Access Control, Single-Device Lock & Admin Panel
    val pendingApprovalTitle: String
    val pendingApprovalSubtitle: String
    val refreshAccessButton: String
    val suspendedAccessTitle: String
    val suspendedAccessSubtitle: String
    val sessionDisplacedTitle: String
    val sessionDisplacedByDeviceSubtitle: String
    val sessionDisplacedByAdminSubtitle: String
    val signInAgainButton: String
    val verifyingAccessLabel: String
    val eventsTabLabel: String
    val eventsTimelineTitle: String
    val eventsTimelineSubtitle: String
    val eventsLiveNowLabel: String
    val eventsUpcomingTodayLabel: String
    val eventsObservancesLabel: String
    val eventsNoItemsToday: String
    val adminPanelTitle: String
    val adminPanelSubtitle: String
    val adminTabPending: String
    val adminTabUsers: String
    val adminTabInviteEmail: String
    val adminNoPendingUsers: String
    val adminSearchUsersHint: String
    val adminAuthorizeUserButton: String
    val adminSuspendUserButton: String
    val adminForceLogoutButton: String
    val adminSavePolicyButton: String
    val adminPreAuthorizeHint: String
    val adminPreAuthorizeButton: String
    val adminClearAllButton: String
    val adminSelectAllButton: String
    val adminEventsOnlyButton: String
    val adminRoleAdmin: String
    val adminRoleUser: String
    val adminStatusPending: String
    val adminStatusActive: String
    val adminStatusSuspended: String
    val adminPagesSectionTitle: String
    val adminFeaturesSectionTitle: String
    val adminBoundDeviceLabel: String

    // User Profile, Easy Login/Logout & Google Drive JSON Auto-Sync
    val profileTitle: String
    val profileSubtitle: String
    val switchAccountButton: String
    val signOutConfirmTitle: String
    val signOutConfirmBody: String
    val connectGoogleDriveButton: String
    val driveConnectedLabel: String
    val driveNotConnectedLabel: String
    val driveSyncJsonNowButton: String
    val driveRestoreJsonButton: String
    val driveReplacedOldNewBadge: String
    val localVaultSavedLabel: String
    val freeStorageNoticeLabel: String
    val navtaraInspectDateLabel: String
    val navtaraMatrixFilterAll: String
    val navtaraJournalHistoryTitle: String
    val navtaraNoJournalHistoryMsg: String
    val actionOpenLabel: String
    val actionDeleteLabel: String
    val gandantaStatusSandhi: String
    val gandantaStatusMoola: String
    val gandantaStatusClear: String
}

val LocalExtendedStrings = staticCompositionLocalOf<ExtendedStrings> { ExtendedEnglishStrings }

fun extendedStringsFor(language: AppLanguage): ExtendedStrings = when (language) {
    AppLanguage.ENGLISH -> ExtendedEnglishStrings
    AppLanguage.HINDI -> ExtendedHindiStrings
}
