package com.mahavtaar.panchanga

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.mahavtaar.panchanga.astronomy.DrikPanchangaEngine
import com.mahavtaar.panchanga.astronomy.HoraEngine
import com.mahavtaar.panchanga.database.access.RoomUserAccessCacheService
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AccessStatus
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.ROOT_ADMIN_EMAIL
import com.mahavtaar.panchanga.domain.model.SwaraNadi
import com.mahavtaar.panchanga.domain.model.SwaraTier
import com.mahavtaar.panchanga.domain.model.TattvaElement
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.UserRole
import com.mahavtaar.panchanga.feature.swara.SwaraTabType
import com.mahavtaar.panchanga.feature.swara.resolveSwaraVisibility
import com.mahavtaar.panchanga.feature.swara.resolveTattvaVisibility
import com.mahavtaar.panchanga.ui.localization.VedicLocalizer
import com.mahavtaar.panchanga.widget.LiveWidgetDataResolver
import com.mahavtaar.panchanga.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Local JVM Robolectric + Compose UI test verifying:
 * - Strict Google Sign-In gating on MainActivity when unauthenticated (no bypass)
 * - Bilingual AuthScreen switching (English <-> Hindi)
 * - RBAC AccessPolicy enforcement (Root Admin vs Pending/Restricted user)
 * - Core Vedic astronomy, Hora, Live Widget resolver, and Room access cache
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PanchangaAppRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val engine = DrikPanchangaEngine()

    @Test
    fun mainActivityStrictlyEnforcesGoogleAuthScreenWhenSignedOut() {
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("google_sign_in_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("auth_language_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("auth_brand_logo").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        // Unauthenticated users must NEVER bypass into the main dashboard
        assertTrue(
            composeRule.onAllNodesWithTag("home_dashboard_list").fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun authScreenLanguageToggleSwitchesBetweenEnglishAndHindi() {
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithTag("auth_language_toggle").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("auth_language_toggle").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("महावतार पंचांग").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("महावतार पंचांग").assertIsDisplayed()

        // Toggle back to English
        composeRule.onNodeWithTag("auth_language_toggle").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Mahavtaar Panchanga").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Mahavtaar Panchanga").assertIsDisplayed()
    }

    @Test
    fun rbacAccessPolicyEnforcesAdminVsRestrictedUserPermissions() {
        assertEquals("jain9640@gmail.com", ROOT_ADMIN_EMAIL)
        val admin = AccessPolicy.ADMIN_FULL
        assertTrue(admin.isAdmin)
        assertTrue(admin.canAccessPage(AppPageKey.HOME))
        assertTrue(admin.canAccessPage(AppPageKey.DAILY_PANCHANGA))
        assertTrue(admin.hasFeature(AppFeatureKey.KUNDLI_SAVE_IMPORT))

        val pendingUser = AccessPolicy(
            userId = "uid_pending",
            email = "newuser@example.com",
            role = UserRole.USER,
            status = AccessStatus.PENDING,
            allowedPages = emptySet(),
            allowedFeatures = emptySet()
        )
        assertFalse(pendingUser.isAdmin)
        assertFalse(pendingUser.canAccessPage(AppPageKey.HOME))
        assertFalse(pendingUser.hasFeature(AppFeatureKey.NOTIFICATIONS_STUDIO))

        val restrictedActiveUser = AccessPolicy(
            userId = "uid_limited",
            email = "member@example.com",
            role = UserRole.USER,
            status = AccessStatus.ACTIVE,
            allowedPages = setOf(AppPageKey.EVENTS_TIMELINE, AppPageKey.DAILY_PANCHANGA),
            allowedFeatures = setOf(AppFeatureKey.HTML_EXPORT_PRINT)
        )
        assertFalse(restrictedActiveUser.isAdmin)
        assertTrue(restrictedActiveUser.canAccessPage(AppPageKey.EVENTS_TIMELINE))
        assertTrue(restrictedActiveUser.canAccessPage(AppPageKey.DAILY_PANCHANGA))
        assertFalse(restrictedActiveUser.canAccessPage(AppPageKey.KUNDLI))
        assertTrue(restrictedActiveUser.hasFeature(AppFeatureKey.HTML_EXPORT_PRINT))
        assertFalse(restrictedActiveUser.hasFeature(AppFeatureKey.KUNDLI_SAVE_IMPORT))
    }

    @Test
    fun tattvaPageBifurcationEnforcesTimeOnlyAndGranularFlags() {
        // 1. Admin restricts user to Tattva page ONLY (0 Tattva extra features) -> strict Tattva & Time mode
        val timeOnlyPolicy = AccessPolicy(
            userId = "uid_tattva_time",
            email = "tattvatime@example.com",
            role = UserRole.USER,
            status = AccessStatus.ACTIVE,
            allowedPages = setOf(AppPageKey.TATTVA),
            allowedFeatures = emptySet()
        )
        val timeOnlyVis = resolveTattvaVisibility(timeOnlyPolicy, UserPreferences())
        assertTrue(timeOnlyVis.minimalTimeOnly)
        assertEquals(0, timeOnlyVis.permittedCount)
        assertFalse(timeOnlyVis.showHeroGuidance)
        assertFalse(timeOnlyVis.showTestChime)
        assertFalse(timeOnlyVis.showNadiPolarity)
        assertFalse(timeOnlyVis.showFourPillarGrid)
        assertFalse(timeOnlyVis.showVaraMasterTable)
        assertFalse(timeOnlyVis.showTithiMasterTable)
        assertFalse(timeOnlyVis.showKaranaMasterTable)
        assertFalse(timeOnlyVis.showHoraExplorerTable)

        // 2. Admin grants ONLY 30-Tithi Master Table -> strictly that single table is visible, no Nadi/summary
        val singleTablePolicy = timeOnlyPolicy.copy(
            allowedFeatures = setOf(AppFeatureKey.TATTVA_TITHI_MASTER_TABLE)
        )
        val singleTableVis = resolveTattvaVisibility(singleTablePolicy, UserPreferences())
        assertFalse(singleTableVis.minimalTimeOnly)
        assertEquals(1, singleTableVis.permittedCount)
        assertTrue(singleTableVis.showTithiMasterTable)
        assertFalse(singleTableVis.showTithiSummary)
        assertFalse(singleTableVis.showTithiDayTable)
        assertFalse(singleTableVis.showNadiPolarity)

        // 3. Root Admin with full permissions toggles local Minimal "Tattva & Time" mode -> hides all 16 extras
        val adminMinimalVis = resolveTattvaVisibility(
            policy = AccessPolicy.ADMIN_FULL,
            prefs = UserPreferences(tattvaMinimalMode = true)
        )
        assertTrue(adminMinimalVis.minimalTimeOnly)
        assertEquals(AppFeatureKey.tattvaFeatures.size, adminMinimalVis.permittedCount)
        assertEquals(0, adminMinimalVis.activeLocalCount)
        assertFalse(adminMinimalVis.showHarmonyReport)
        assertFalse(adminMinimalVis.showMacroKaranaCycle)
        assertFalse(adminMinimalVis.showMahabhutaReference)

        // 4. Admin hides only Hora Explorer locally -> all other 15 remain visible
        val adminCustomVis = resolveTattvaVisibility(
            policy = AccessPolicy.ADMIN_FULL,
            prefs = UserPreferences(tattvaHiddenFeatures = setOf(AppFeatureKey.TATTVA_HORA_EXPLORER_TABLE.name))
        )
        assertFalse(adminCustomVis.minimalTimeOnly)
        assertFalse(adminCustomVis.showHoraExplorerTable)
        assertTrue(adminCustomVis.showHoraStrip)
        assertTrue(adminCustomVis.showVaraMasterTable)
    }

    @Test
    fun swaraYogaTiersEnforceSadhakaYogiTapasviAndIndependentTabs() {
        // 1. Sadhaka baseline (explicit SWARA_TIER_SADHAK or 0 Swara keys) -> Sunrise/Midday/Sunset only, no Tattva hero/boundary/tabs
        val sadhakaPolicy = AccessPolicy(
            userId = "uid_swara_sadhak",
            email = "sadhak@example.com",
            role = UserRole.USER,
            status = AccessStatus.ACTIVE,
            allowedPages = setOf(AppPageKey.SWARA_YOGA),
            allowedFeatures = setOf(AppFeatureKey.SWARA_TIER_SADHAK)
        )
        val sadhakaVis = resolveSwaraVisibility(sadhakaPolicy)
        assertEquals(SwaraTier.SADHAKA, sadhakaVis.tier)
        assertFalse(sadhakaVis.includeLunarWindows)
        assertFalse(sadhakaVis.showHeroTattvaAndDetails)
        assertFalse(sadhakaVis.showSlokaCard)
        assertFalse(sadhakaVis.showBoundarySwaras)
        assertFalse(sadhakaVis.showTattvaExplorer)
        assertEquals(listOf(SwaraTabType.LIVE), sadhakaVis.visibleTabs)

        // Fallback when user has SWARA_YOGA page but zero Swara keys -> also Sadhaka
        val emptySwaraVis = resolveSwaraVisibility(sadhakaPolicy.copy(allowedFeatures = emptySet()))
        assertEquals(SwaraTier.SADHAKA, emptySwaraVis.tier)
        assertFalse(emptySwaraVis.includeLunarWindows)
        assertEquals(listOf(SwaraTabType.LIVE), emptySwaraVis.visibleTabs)

        // 2. Yogi tier + separate Tattva Explorer tab toggle -> 5 windows (Solar + Lunar), still no Tapasvi hero/boundary
        val yogiWithTattvaPolicy = sadhakaPolicy.copy(
            allowedFeatures = setOf(
                AppFeatureKey.SWARA_TIER_YOGI,
                AppFeatureKey.SWARA_TATTVA_EXPLORER
            )
        )
        val yogiVis = resolveSwaraVisibility(yogiWithTattvaPolicy)
        assertEquals(SwaraTier.YOGI, yogiVis.tier)
        assertTrue(yogiVis.includeLunarWindows)
        assertFalse(yogiVis.showHeroTattvaAndDetails)
        assertFalse(yogiVis.showSlokaCard)
        assertFalse(yogiVis.showBoundarySwaras)
        assertTrue(yogiVis.showTattvaExplorer)
        assertEquals(listOf(SwaraTabType.LIVE, SwaraTabType.TATTVAS), yogiVis.visibleTabs)

        // 3. Tapasvi tier + independent Guidance Works & Nakshatra Alignment -> full Live (Hero + Tattva + Sloka + 5 windows + Boundaries) + selected tabs
        val tapasviSelectivePolicy = sadhakaPolicy.copy(
            allowedFeatures = setOf(
                AppFeatureKey.SWARA_TIER_TAPASVI,
                AppFeatureKey.SWARA_GUIDANCE_WORKS,
                AppFeatureKey.SWARA_NAKSHATRA_ALIGNMENT
            )
        )
        val tapasviVis = resolveSwaraVisibility(tapasviSelectivePolicy)
        assertEquals(SwaraTier.TAPASVI, tapasviVis.tier)
        assertTrue(tapasviVis.includeLunarWindows)
        assertTrue(tapasviVis.showHeroTattvaAndDetails)
        assertTrue(tapasviVis.showSlokaCard)
        assertTrue(tapasviVis.showBoundarySwaras)
        assertTrue(tapasviVis.showGuidanceWorks)
        assertFalse(tapasviVis.showTithiRulesTable)
        assertFalse(tapasviVis.showTattvaExplorer)
        assertTrue(tapasviVis.showNakshatraAlignment)
        assertFalse(tapasviVis.showMasterTables)
        assertEquals(
            listOf(SwaraTabType.LIVE, SwaraTabType.GUIDANCE, SwaraTabType.ALIGNMENT),
            tapasviVis.visibleTabs
        )

        // 4. Root Admin (ADMIN_FULL) -> Tapasvi tier + all 4 tabs unlocked
        val adminVis = resolveSwaraVisibility(AccessPolicy.ADMIN_FULL)
        assertEquals(SwaraTier.TAPASVI, adminVis.tier)
        assertTrue(adminVis.includeLunarWindows)
        assertTrue(adminVis.showHeroTattvaAndDetails)
        assertTrue(adminVis.showBoundarySwaras)
        assertEquals(
            listOf(SwaraTabType.LIVE, SwaraTabType.GUIDANCE, SwaraTabType.TATTVAS, SwaraTabType.ALIGNMENT),
            adminVis.visibleTabs
        )
    }

    @Test
    fun vedicLocalizerMapsAllCoreHinduEntitiesToAuthenticHindi() {
        assertEquals("शुक्ल एकादशी", VedicLocalizer.tithiName(11, "Śukla pakṣa ekādaśī", AppLanguage.HINDI))
        assertEquals("पूर्णिमा", VedicLocalizer.tithiName(15, "Pūrṇimā", AppLanguage.HINDI))
        assertEquals("अमावस्या", VedicLocalizer.tithiName(30, "Amāvāsyā", AppLanguage.HINDI))
        assertEquals("रोहिणी", VedicLocalizer.nakshatraName(4, "Rohiṇī", AppLanguage.HINDI))
        assertEquals("सौभाग्य", VedicLocalizer.yogaName(4, "Saubhāgya", AppLanguage.HINDI))
        assertEquals("विष्टि (भद्रा)", VedicLocalizer.karanaName("Viṣṭi", AppLanguage.HINDI))
        assertEquals("चैत्र", VedicLocalizer.masaName(1, false, false, "Caitra", AppLanguage.HINDI))
        assertEquals("अधिक श्रावण", VedicLocalizer.masaName(5, true, false, "Adhika Śrāvaṇa", AppLanguage.HINDI))
        assertEquals("गुरुवार", VedicLocalizer.vaaraName(4, "Guruvāra", AppLanguage.HINDI))
        assertEquals("राहु काल", VedicLocalizer.muhurtaOrPeriodName("Rāhu Kāla", AppLanguage.HINDI))
        assertEquals("बृहस्पति (गुरु)", VedicLocalizer.planetName("jupiter", "Jupiter", AppLanguage.HINDI))
    }

    @Test
    fun monthlyCalendarGeneratesAllDaysWithAccurateMasaAndObservances() {
        val bhopal = CityLocation("Bhopal, IN", "IN", 23.2599, 77.4126, "Asia/Kolkata")
        val days = engine.computeMonthlyCalendar(
            location = bhopal,
            year = 2026,
            month = 3,
            monthSystem = MonthSystem.AMANTA,
            coordinateSelection = CoordinateSelection.CITRA
        )
        assertEquals(31, days.size)
        assertTrue(days.any { it.observances.isNotEmpty() })
        assertTrue(days.any { it.isMasaTransition })
    }

    @Test
    fun dailySnapshotAndHoraEngineIntegrateCleanlyUnderAndroidRuntime() {
        val varanasi = CityLocation("Varanasi, IN", "IN", 25.3176, 82.9739, "Asia/Kolkata")
        val snap = engine.computePanchanga(
            location = varanasi,
            date = LocalDate.of(2026, 11, 19),
            monthSystem = MonthSystem.PURNIMANTA,
            coordinateSelection = CoordinateSelection.CITRA,
            includeExtendedTransits = true
        )
        assertEquals(9, snap.planets.size)
        assertTrue(snap.upcomingTransits.isNotEmpty())
        val horas = HoraEngine.resolveForSnapshot(snap, snap.sunriseMs + 3_600_000L)
        assertEquals(24, horas.horas.size)
        assertNotNull(horas.activeHora)
        assertNotNull(horas.activeTattva)
    }

    @Test
    fun liveWidgetDataResolverAndWidgetUpdateHelperRunSafely() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val model = LiveWidgetDataResolver.resolve(context)
        assertTrue(model.headerTitle.isNotBlank())
        assertTrue(model.activeTithiLine.isNotBlank())
        assertTrue(model.activeNakshatraLine.isNotBlank())
        assertTrue(model.activeHoraLine.isNotBlank())
        assertTrue(model.activeTattvaLine.isNotBlank())
        assertTrue(model.activeMuhurtasLine.isNotBlank())
        assertTrue(model.activeChoghadiyaLine.isNotBlank())
        assertTrue(model.tithiName.isNotBlank())
        assertTrue(model.nakshatraName.isNotBlank())
        assertNotNull(model.activeHora)
        assertNotNull(model.activeTattva)
        // Verify widget date-time strings include seconds (HH:MM:SS AM/PM) and month/date
        val secRegex = Regex("""\d{2}:\d{2}:\d{2}\s[AP]M""")
        assertTrue("Expected seconds in widget Hora range: ${model.activeHora?.range}", secRegex.containsMatchIn(model.activeHora!!.range))
        assertTrue("Expected seconds in widget Tattva range: ${model.activeTattva?.range}", secRegex.containsMatchIn(model.activeTattva!!.range))
        assertTrue(model.nextTransitionAtMs > model.nowMs)

        // Must complete without throwing even when 0 home-screen widgets are placed
        WidgetUpdateHelper.updateAllWidgets(context)
    }

    @Test
    fun eventDateTimeWithSecondsAndSimpleNowClarityAndSwaraWindowsAreAccurate() {
        val zone = "Asia/Kolkata"
        val zId = ZoneId.of(zone)
        val overnightStart = ZonedDateTime.of(2026, 11, 25, 22, 15, 42, 0, zId).toInstant().toEpochMilli()
        val overnightEnd = ZonedDateTime.of(2026, 11, 26, 5, 48, 9, 0, zId).toInstant().toEpochMilli()

        val enRange = VedicLocalizer.formatDateTimeRangeWithSeconds(overnightStart, overnightEnd, zone, AppLanguage.ENGLISH)
        assertTrue("English range should include Start date+seconds: $enRange", enRange.contains("25 Nov") && enRange.contains("10:15:42 PM"))
        assertTrue("English range should include End date+seconds across midnight: $enRange", enRange.contains("26 Nov") && enRange.contains("05:48:09 AM"))

        val hiRange = VedicLocalizer.formatDateTimeRangeWithSeconds(overnightStart, overnightEnd, zone, AppLanguage.HINDI)
        assertTrue("Hindi range should include Start date+seconds: $hiRange", hiRange.contains("25 नव") && hiRange.contains("10:15:42 PM"))
        assertTrue("Hindi range should include End date+seconds across midnight: $hiRange", hiRange.contains("26 नव") && hiRange.contains("05:48:09 AM"))

        val simpleEn = VedicLocalizer.simpleNowIntervalSentence("Abhijit Muhurta", false, overnightEnd, overnightStart, AppLanguage.ENGLISH)
        val simpleHi = VedicLocalizer.simpleNowIntervalSentence("अभिजीत मुहूर्त", false, overnightEnd, overnightStart, AppLanguage.HINDI)
        val simpleHora = VedicLocalizer.simpleNowHoraSentence("Jupiter", SwaraNadi.IDA, TattvaElement.JALA, AppLanguage.ENGLISH)
        assertTrue(simpleEn.startsWith("Right now:"))
        assertTrue(simpleHi.startsWith("अभी"))
        assertTrue(simpleHora.contains("Right now:") && simpleHora.contains("Jala"))

        val bhopal = CityLocation("Bhopal, IN", "IN", 23.2599, 77.4126, zone)
        val snap = engine.computePanchanga(
            location = bhopal,
            date = LocalDate.of(2026, 11, 25),
            monthSystem = MonthSystem.PURNIMANTA,
            coordinateSelection = CoordinateSelection.CITRA,
            includeExtendedTransits = false
        )
        assertNotNull(snap.swaraYoga.sunriseWindow.startTimeMs)
        assertNotNull(snap.swaraYoga.sunriseWindow.endTimeMs)
        assertNotNull(snap.swaraYoga.middayWindow.startTimeMs)
        assertNotNull(snap.swaraYoga.sunsetWindow.startTimeMs)
        snap.swaraYoga.moonriseWindow?.let { mr ->
            assertNotNull("Moonrise Swara window should have startTimeMs", mr.startTimeMs)
            assertNotNull("Moonrise Swara window should have endTimeMs", mr.endTimeMs)
        }
        snap.swaraYoga.moonsetWindow?.let { ms ->
            assertNotNull("Moonset Swara window should have startTimeMs", ms.startTimeMs)
            assertNotNull("Moonset Swara window should have endTimeMs", ms.endTimeMs)
        }
    }

    @Test
    fun roomUserAccessCacheServicePersistsAndIsolatesPerUserRecordsCleanly() = runBlocking {
        val service = composeRule.activity.userAccessCacheService
        service.clearAllCachedAccess()
        // Unknown / unapproved user must NEVER receive a synthetic or another user's cached record
        org.junit.Assert.assertNull(service.getInstantCachedAccess("uid_unknown_unapproved"))
        org.junit.Assert.assertNull(service.getCachedAccess("uid_unknown_unapproved"))

        val record = com.mahavtaar.panchanga.core.access.CachedUserAccessRecord(
            userId = "uid_test_123",
            email = "tester@mahavtaar.in",
            displayName = "Test Devotee",
            roleName = UserRole.USER.name,
            statusName = AccessStatus.ACTIVE.name,
            allowedPagesCsv = AppPageKey.DAILY_PANCHANGA.name,
            allowedFeaturesCsv = AppFeatureKey.HTML_EXPORT_PRINT.name,
            authorizedByUid = "admin_uid",
            sessionToken = "session_tok_123",
            updatedAtMs = System.currentTimeMillis()
        )
        service.upsertCachedAccess(record)
        val fetched = service.getCachedAccess("uid_test_123")
        assertNotNull(fetched)
        assertEquals("tester@mahavtaar.in", fetched?.email)
        val observed = service.observeCachedAccess("uid_test_123").first()
        assertEquals("Test Devotee", observed?.displayName)
        // A different UID must NEVER inherit uid_test_123's cached record
        org.junit.Assert.assertNull(service.getInstantCachedAccess("uid_other_999"))
        org.junit.Assert.assertNull(service.getCachedAccess("uid_other_999"))
        service.clearCachedAccess("uid_test_123")
    }
}
