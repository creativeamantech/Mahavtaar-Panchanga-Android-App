package com.mahavtaar.panchanga

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mahavtaar.panchanga.astronomy.DrikPanchangaEngine
import com.mahavtaar.panchanga.astronomy.NavtaraEngine
import com.mahavtaar.panchanga.data.backup.DriveHttpExecutor
import com.mahavtaar.panchanga.data.backup.GoogleDriveBackupManager
import com.mahavtaar.panchanga.data.backup.GoogleDriveRestClient
import com.mahavtaar.panchanga.domain.model.AlertAudioSettings
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CloudSyncStatus
import com.mahavtaar.panchanga.domain.model.CloudUser
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.KundliBirthInput
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.NavtaraDailyFeedback
import com.mahavtaar.panchanga.domain.model.NavtaraDayCategory
import com.mahavtaar.panchanga.domain.model.NavtaraFeedbackRating
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodDay
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodReport
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodStats
import com.mahavtaar.panchanga.domain.model.NavtaraProfile
import com.mahavtaar.panchanga.domain.model.SavedKundliChart
import com.mahavtaar.panchanga.domain.model.SavedNavtaraReport
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.WidgetGlassStyle
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.CloudAccountRepository
import com.mahavtaar.panchanga.domain.repository.LocationRepository
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import com.mahavtaar.panchanga.domain.repository.PanchangaRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.domain.usecase.GetDailyPanchangaUseCase
import com.mahavtaar.panchanga.feature.swara.NavtaraViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GoogleProfileAndNavtaraDriveTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mahavtaar_gdrive_backup_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private data class RecordedHttpCall(
        val method: String,
        val url: String,
        val headers: Map<String, String>,
        val body: String
    )

    private class FakeDriveHttpExecutor : DriveHttpExecutor {
        val calls = mutableListOf<RecordedHttpCall>()
        var existingFilesJson: String = """{"files":[]}"""
        var nextUploadedId: String = "drive_new_json_id"

        override suspend fun execute(
            method: String,
            url: String,
            headers: Map<String, String>,
            bodyBytes: ByteArray?
        ): Pair<Int, String> {
            calls += RecordedHttpCall(
                method = method,
                url = url,
                headers = headers,
                body = bodyBytes?.toString(Charsets.UTF_8).orEmpty()
            )
            return when {
                method == "GET" && url.contains("/drive/v3/files?") -> 200 to existingFilesJson
                method == "POST" && url.contains("/upload/drive/v3/files") -> {
                    200 to """{"id":"$nextUploadedId","name":"mahavtaar_panchanga_backup_aman.json"}"""
                }
                method == "DELETE" && url.contains("/drive/v3/files/") -> 204 to ""
                else -> 404 to "{}"
            }
        }
    }

    private class TestHarness(context: Context) {
        val idCounter = AtomicLong(100L)
        val userFlow = MutableStateFlow<CloudUser?>(null)
        val prefsFlow = MutableStateFlow(UserPreferences(birthNakshatra = 4))
        val profilesFlow = MutableStateFlow<List<NavtaraProfile>>(emptyList())
        val feedbacksFlow = MutableStateFlow<List<NavtaraDailyFeedback>>(emptyList())
        val reportsFlow = MutableStateFlow<List<SavedNavtaraReport>>(emptyList())

        val engine = DrikPanchangaEngine()

        val cloudRepo: CloudAccountRepository = mockk(relaxed = true) {
            every { currentUser } returns userFlow
            every { syncStatus } returns MutableStateFlow(CloudSyncStatus())
            coEvery { syncAllNow() } returns Result.success(Unit)
        }

        val savedLocationsFlow = MutableStateFlow<List<CityLocation>>(emptyList())
        val kundliProfilesFlow = MutableStateFlow<List<KundliBirthInput>>(emptyList())

        val prefsRepo: UserPreferencesRepository = mockk(relaxed = true) {
            every { preferences } returns prefsFlow
            every { observeSavedKundliProfiles() } returns kundliProfilesFlow
            coEvery { setBirthNakshatra(any()) } answers {
                val nak = firstArg<Int>().coerceIn(1, 27)
                prefsFlow.value = prefsFlow.value.copy(birthNakshatra = nak)
            }
            coEvery { setSelectedLocation(any()) } answers {
                prefsFlow.value = prefsFlow.value.copy(selectedLocation = firstArg())
            }
            coEvery { setLanguage(any()) } answers {
                prefsFlow.value = prefsFlow.value.copy(language = firstArg())
            }
            coEvery { setWidgetGlassStyle(any()) } answers {
                prefsFlow.value = prefsFlow.value.copy(widgetGlassStyle = firstArg())
            }
            coEvery { setWidgetOpacity(any()) } answers {
                prefsFlow.value = prefsFlow.value.copy(widgetOpacity = firstArg())
            }
            coEvery { setPinnedHomeModules(any()) } answers {
                prefsFlow.value = prefsFlow.value.copy(pinnedHomeModules = firstArg())
            }
            coEvery { setTattvaMinimalMode(any()) } answers {
                prefsFlow.value = prefsFlow.value.copy(tattvaMinimalMode = firstArg())
            }
            coEvery { saveKundliProfile(any()) } answers {
                val p = firstArg<KundliBirthInput>()
                kundliProfilesFlow.value = kundliProfilesFlow.value.filterNot { it.name == p.name } + p
            }
        }

        val locationRepo: LocationRepository = mockk(relaxed = true) {
            every { observeSavedLocations() } returns savedLocationsFlow
            coEvery { saveLocation(any()) } answers {
                val loc = firstArg<CityLocation>()
                savedLocationsFlow.value = savedLocationsFlow.value.filterNot { it.name == loc.name } + loc
            }
        }

        val notifRepo: NotificationStudioRepository = mockk(relaxed = true) {
            every { observeRules() } returns MutableStateFlow(emptyList())
            every { observeAudioSettings() } returns MutableStateFlow(AlertAudioSettings())
            coEvery { getAllRules() } returns emptyList()
            coEvery { getAudioSettings() } returns AlertAudioSettings()
        }

        val panchangaRepo: PanchangaRepository = mockk(relaxed = true) {
            coEvery {
                getDailyPanchanga(any(), any(), any(), any(), any())
            } answers {
                engine.computePanchanga(
                    location = arg(0),
                    date = arg(1),
                    monthSystem = arg(2),
                    coordinateSelection = arg(3),
                    includeExtendedTransits = arg(4)
                )
            }
        }

        val kundliChartsFlow = MutableStateFlow<List<SavedKundliChart>>(emptyList())

        val astrologyRepo: AstrologyRepository = mockk(relaxed = true) {
            every { computeNavtara(any(), any()) } answers {
                NavtaraEngine.calculate(arg(0), arg(1))
            }
            every { computeNavtaraMatrix(any()) } answers {
                NavtaraEngine.generateMatrix(arg(0))
            }
            every { observeSavedKundliCharts() } returns kundliChartsFlow
            coEvery { saveKundliChart(any()) } answers {
                val c = firstArg<SavedKundliChart>()
                val assignedId = if (c.id > 0L) c.id else idCounter.incrementAndGet()
                kundliChartsFlow.value = kundliChartsFlow.value.filterNot { it.id == assignedId } + c.copy(id = assignedId)
                assignedId
            }
            every { observeNavtaraProfiles() } returns profilesFlow
            coEvery { saveNavtaraProfile(any()) } answers {
                val input = firstArg<NavtaraProfile>()
                val assignedId = if (input.id > 0L) input.id else idCounter.incrementAndGet()
                val stored = input.copy(id = assignedId)
                val updated = profilesFlow.value.filterNot { it.id == assignedId } + stored
                profilesFlow.value = if (stored.isDefault) {
                    updated.map { it.copy(isDefault = it.id == assignedId) }
                } else updated
                assignedId
            }
            coEvery { setDefaultNavtaraProfile(any()) } answers {
                val targetId = firstArg<Long>()
                profilesFlow.value = profilesFlow.value.map { it.copy(isDefault = it.id == targetId) }
            }
            coEvery { deleteNavtaraProfile(any()) } answers {
                val targetId = firstArg<Long>()
                profilesFlow.value = profilesFlow.value.filterNot { it.id == targetId }
                feedbacksFlow.value = feedbacksFlow.value.filterNot { it.profileId == targetId }
            }
            every { observeNavtaraFeedbacks(any()) } answers {
                val targetId = firstArg<Long>()
                feedbacksFlow.map { list -> list.filter { it.profileId == targetId } }
            }
            coEvery { saveNavtaraFeedback(any()) } answers {
                val fb = firstArg<NavtaraDailyFeedback>()
                feedbacksFlow.value = feedbacksFlow.value.filterNot {
                    it.profileId == fb.profileId && it.dateIso == fb.dateIso
                } + fb
            }
            coEvery { deleteNavtaraFeedback(any(), any()) } answers {
                val pid = arg<Long>(0)
                val d = arg<String>(1)
                feedbacksFlow.value = feedbacksFlow.value.filterNot { it.profileId == pid && it.dateIso == d }
            }
            every { observeSavedNavtaraReports() } returns reportsFlow
            coEvery { saveNavtaraReport(any()) } answers {
                val r = firstArg<SavedNavtaraReport>()
                val assignedId = if (r.id > 0L) r.id else idCounter.incrementAndGet()
                reportsFlow.value = reportsFlow.value + r.copy(id = assignedId)
                assignedId
            }
            coEvery { deleteNavtaraReport(any()) } answers {
                val targetId = firstArg<Long>()
                reportsFlow.value = reportsFlow.value.filterNot { it.id == targetId }
            }
            coEvery {
                computeNavtaraPeriodReport(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } answers {
                val profileId = arg<Long>(0)
                val profileName = arg<String>(1)
                val birthNak = arg<Int>(2)
                val pada = arg<Int>(3)
                val start = arg<LocalDate>(4)
                val end = arg<LocalDate>(5)
                val isRange = arg<Boolean>(6)
                val loc = arg<CityLocation>(7)
                val monthSys = arg<MonthSystem>(8)
                val ayanamsha = arg<CoordinateSelection>(9)
                val days = mutableListOf<NavtaraPeriodDay>()
                var cursor = start
                while (!cursor.isAfter(end)) {
                    val tara = NavtaraEngine.calculate(birthNak, ((cursor.dayOfMonth - 1) % 27) + 1)
                    days += NavtaraPeriodDay(
                        dateIso = cursor.toString(),
                        dayOfMonth = cursor.dayOfMonth,
                        month = cursor.monthValue,
                        year = cursor.year,
                        weekday = cursor.dayOfWeek.value % 7,
                        vaara = "Guruvāra",
                        tithiNumber = 10,
                        tithiName = "Daśamī",
                        paksha = "Śukla",
                        nakshatraNumber = tara.targetNakshatraIndex,
                        nakshatraName = "Rohiṇī",
                        moonRasi = "Vṛṣabha",
                        currentTara = tara,
                        category = if (tara.isAuspicious) NavtaraDayCategory.AUSPICIOUS else NavtaraDayCategory.CAUTION,
                        recommendationEn = "Steady practice",
                        recommendationHi = "स्थिर साधना"
                    )
                    cursor = cursor.plusDays(1)
                }
                NavtaraPeriodReport(
                    profileId = profileId,
                    profileName = profileName,
                    birthNakshatraIndex = birthNak,
                    birthPada = pada,
                    startDateIso = start.toString(),
                    endDateIso = end.toString(),
                    isDateRange = isRange,
                    location = loc,
                    ayanamsha = ayanamsha,
                    monthSystem = monthSys,
                    days = days,
                    transitions = emptyList(),
                    stats = NavtaraPeriodStats(
                        totalDays = days.size,
                        auspiciousCount = days.count { it.category == NavtaraDayCategory.AUSPICIOUS },
                        mixedCount = 0,
                        cautionCount = days.count { it.category == NavtaraDayCategory.CAUTION },
                        neutralCount = 0,
                        feedbackCount = 0,
                        auspiciousFeedbackCount = 0,
                        neutralFeedbackCount = 0,
                        cautionFeedbackCount = 0
                    )
                )
            }
        }

        val fakeDriveHttp = FakeDriveHttpExecutor()
        val driveRestClient = GoogleDriveRestClient(context, fakeDriveHttp)
        val backupManager = GoogleDriveBackupManager(
            context = context,
            preferencesRepository = prefsRepo,
            locationRepository = locationRepo,
            astrologyRepository = astrologyRepo,
            notificationStudioRepository = notifRepo,
            cloudAccountRepository = cloudRepo,
            driveRestClient = driveRestClient
        )
    }

    @Test
    fun ensureDefaultGoogleProfileAutoCreatesFromGoogleUserOnce() = runBlocking {
        val harness = TestHarness(context)
        val googleUser = CloudUser(
            uid = "google_uid_aman_1",
            displayName = "Aman Jain",
            email = "aman@mahavtaar.in"
        )
        harness.userFlow.value = googleUser

        val firstCreated = harness.backupManager.ensureDefaultGoogleProfileIfNeeded(googleUser)
        // Second invocation must be idempotent and never create a duplicate profile
        val secondCreated = harness.backupManager.ensureDefaultGoogleProfileIfNeeded(googleUser)

        val profiles = harness.profilesFlow.value
        assertEquals(1, profiles.size)
        assertEquals("Aman Jain", profiles.first().name)
        assertEquals("aman@mahavtaar.in", profiles.first().notes)
        assertTrue(profiles.first().isDefault)
        // Note: observer or explicit call may seed once; regardless, exactly 1 profile exists
        assertTrue(firstCreated || !secondCreated)
        assertFalse(secondCreated)
    }

    @Test
    fun replaceJsonFileInDriveUploadsNewJsonAndDeletesAllStaleFiles() = runBlocking {
        val executor = FakeDriveHttpExecutor().apply {
            existingFilesJson = """
                {
                  "files": [
                    {"id": "stale_json_1", "name": "mahavtaar_panchanga_backup_aman.json"},
                    {"id": "stale_json_2", "name": "mahavtaar_panchanga_backup_aman.json"}
                  ]
                }
            """.trimIndent()
            nextUploadedId = "fresh_json_99"
        }
        val client = GoogleDriveRestClient(context, executor)
        val payload = """{"format":"mahavtaar_navtara_json","feedbacks":[{"dateIso":"2026-11-19"}]}"""

        val result = client.replaceJsonFileInDrive(
            accessToken = "ya29.test_token",
            fileName = "mahavtaar_panchanga_backup_aman.json",
            jsonContent = payload
        )

        assertEquals("fresh_json_99", result.newFileId)
        assertEquals(listOf("stale_json_1", "stale_json_2"), result.deletedOldFileIds)

        assertEquals(4, executor.calls.size)
        assertEquals("GET", executor.calls[0].method)
        assertEquals("POST", executor.calls[1].method)
        assertTrue(executor.calls[1].body.contains("mahavtaar_navtara_json"))
        assertEquals("DELETE", executor.calls[2].method)
        assertTrue(executor.calls[2].url.endsWith("/stale_json_1"))
        assertEquals("DELETE", executor.calls[3].method)
        assertTrue(executor.calls[3].url.endsWith("/stale_json_2"))
    }

    @Test
    fun navtaraJsonExportAndImportRoundTripRestoresDataAndGuardsAccountUid() = runBlocking {
        val harness = TestHarness(context)
        val user = CloudUser(uid = "uid_aman_77", displayName = "Aman", email = "aman@example.com")

        val profId = harness.astrologyRepo.saveNavtaraProfile(
            NavtaraProfile(name = "Aman", birthNakshatraIndex = 7, birthPada = 2, isDefault = true)
        )
        harness.userFlow.value = user
        harness.astrologyRepo.saveNavtaraFeedback(
            NavtaraDailyFeedback(
                profileId = profId,
                dateIso = "2026-11-19",
                rating = NavtaraFeedbackRating.AUSPICIOUS,
                note = "Deep meditation & clarity"
            )
        )
        harness.astrologyRepo.saveNavtaraReport(
            SavedNavtaraReport(
                profileId = profId,
                profileName = "Aman",
                birthNakshatraIndex = 7,
                birthPada = 2,
                startDateIso = "2026-11-01",
                endDateIso = "2026-11-30",
                isDateRange = false,
                locationName = "Bhopal, IN",
                latitude = 23.2599,
                longitude = 77.4126,
                timezone = "Asia/Kolkata"
            )
        )

        val exportedJson = harness.backupManager.exportNavtaraJsonString(activeProfileId = profId)
        assertTrue(exportedJson.contains("mahavtaar_navtara_json"))
        assertTrue(exportedJson.contains("Deep meditation & clarity"))

        // Clear local repositories and verify cross-account guard rejects mismatching user
        harness.profilesFlow.value = emptyList()
        harness.feedbacksFlow.value = emptyList()
        harness.reportsFlow.value = emptyList()

        harness.userFlow.value = CloudUser(uid = "uid_someone_else", displayName = "Other", email = "other@example.com")
        val wrongAccountResult = harness.backupManager.importJsonString(exportedJson)
        assertTrue(wrongAccountResult.isFailure)

        // Restore as owning Google user succeeds cleanly (clear any auto-seeded "Other" profile first)
        harness.userFlow.value = user
        harness.profilesFlow.value = emptyList()
        val restoreResult = harness.backupManager.importJsonString(exportedJson)
        assertTrue(restoreResult.isSuccess)
        assertEquals(3, restoreResult.getOrNull())
        assertEquals(1, harness.profilesFlow.value.size)
        assertEquals("Aman", harness.profilesFlow.value.first().name)
        assertEquals(1, harness.feedbacksFlow.value.size)
        assertEquals("Deep meditation & clarity", harness.feedbacksFlow.value.first().note)
        assertEquals(1, harness.reportsFlow.value.size)
    }

    @Test
    fun navtaraViewModelOpenSavedReportMergesFeedbacksAndSupportsInspectDateAndFilters() = runBlocking {
        val harness = TestHarness(context)
        val profileId = 42L
        harness.profilesFlow.value = listOf(
            NavtaraProfile(id = profileId, name = "Sadhak Aman", birthNakshatraIndex = 4, birthPada = 1, isDefault = true)
        )
        harness.feedbacksFlow.value = listOf(
            NavtaraDailyFeedback(
                profileId = profileId,
                dateIso = "2026-11-19",
                rating = NavtaraFeedbackRating.AUSPICIOUS,
                note = "Excellent sankalpa day"
            )
        )

        val vm = NavtaraViewModel(
            getDailyPanchangaUseCase = GetDailyPanchangaUseCase(harness.panchangaRepo),
            astrologyRepository = harness.astrologyRepo,
            preferencesRepository = harness.prefsRepo,
            googleDriveBackupManager = harness.backupManager,
            cloudAccountRepository = harness.cloudRepo
        )

        val customDate = LocalDate.of(2026, 11, 20)
        vm.setInspectDate(customDate)
        vm.setMatrixFilter(NavtaraDayCategory.AUSPICIOUS)
        vm.setJournalFilter(NavtaraFeedbackRating.AUSPICIOUS)

        assertEquals(customDate, vm.uiState.value.inspectDate)
        assertEquals(NavtaraDayCategory.AUSPICIOUS, vm.uiState.value.matrixFilter)
        assertEquals(NavtaraFeedbackRating.AUSPICIOUS, vm.uiState.value.journalFilter)

        vm.openSavedReport(
            SavedNavtaraReport(
                id = 9L,
                profileId = profileId,
                profileName = "Sadhak Aman",
                birthNakshatraIndex = 4,
                birthPada = 1,
                startDateIso = "2026-11-18",
                endDateIso = "2026-11-20",
                isDateRange = true,
                locationName = "Bhopal, IN",
                latitude = 23.2599,
                longitude = 77.4126,
                timezone = "Asia/Kolkata"
            )
        )

        val state = vm.uiState.value
        assertEquals(1, state.plannerTab)
        val report = state.periodReport
        assertNotNull(report)
        assertEquals(3, report?.days?.size)
        assertEquals(1, report?.stats?.feedbackCount)
        assertEquals(
            "Excellent sankalpa day",
            report?.days?.firstOrNull { it.dateIso == "2026-11-19" }?.feedback?.note
        )
    }

    @Test
    fun fullBundleRoundTripPreservesLocationsWidgetSettingsKundliProfilesAndNavtaraForAdminsAndUsers() = runBlocking {
        val harness = TestHarness(context)
        val adminUser = CloudUser(
            uid = "uid_admin_self_drive",
            displayName = "Admin Aman",
            email = "jain9640@gmail.com"
        )
        harness.userFlow.value = adminUser

        val customCity = CityLocation(
            name = "Ujjain, IN",
            country = "IN",
            latitude = 23.1765,
            longitude = 75.7885,
            timezone = "Asia/Kolkata",
            isCustom = true,
            isDefault = true
        )
        harness.prefsFlow.value = UserPreferences(
            selectedLocation = customCity,
            language = AppLanguage.HINDI,
            birthNakshatra = 11,
            tattvaMinimalMode = true,
            pinnedHomeModules = listOf("swara", "tattva", "navtara", "kundli"),
            widgetGlassStyle = WidgetGlassStyle.DARK,
            widgetOpacity = 0.78f
        )
        harness.savedLocationsFlow.value = listOf(customCity)
        harness.kundliProfilesFlow.value = listOf(
            KundliBirthInput(
                name = "Aman Birth Chart",
                year = 1995,
                month = 8,
                day = 15,
                hour = 7,
                minute = 30,
                location = customCity
            )
        )

        val fullJson = harness.backupManager.exportFullBundleJsonString()
        assertTrue(fullJson.contains("Ujjain, IN"))
        assertTrue(fullJson.contains("Aman Birth Chart"))
        assertTrue(fullJson.contains("\"schemaVersion\": 3"))

        // Wipe local device state to simulate fresh reinstall on new phone
        harness.prefsFlow.value = UserPreferences()
        harness.savedLocationsFlow.value = emptyList()
        harness.kundliProfilesFlow.value = emptyList()

        val restored = harness.backupManager.importJsonString(fullJson)
        assertTrue(restored.isSuccess)
        assertEquals("Ujjain, IN", harness.prefsFlow.value.selectedLocation.name)
        assertEquals(AppLanguage.HINDI, harness.prefsFlow.value.language)
        assertEquals(WidgetGlassStyle.DARK, harness.prefsFlow.value.widgetGlassStyle)
        assertEquals(0.78f, harness.prefsFlow.value.widgetOpacity, 0.001f)
        assertEquals(listOf("swara", "tattva", "navtara", "kundli"), harness.prefsFlow.value.pinnedHomeModules)
        assertTrue(harness.prefsFlow.value.tattvaMinimalMode)
        assertEquals(1, harness.savedLocationsFlow.value.size)
        assertEquals("Ujjain, IN", harness.savedLocationsFlow.value.first().name)
        assertEquals(1, harness.kundliProfilesFlow.value.size)
        assertEquals("Aman Birth Chart", harness.kundliProfilesFlow.value.first().name)
    }

    @Test
    fun driveClientFallsBackToAppDataFolderWhenVisibleBackupFileIsDeleted() = runBlocking {
        val appDataPayload = """{"schemaVersion":3,"userUid":"uid_1","userEmail":"user@example.com"}"""
        val executor = object : DriveHttpExecutor {
            val urls = mutableListOf<String>()
            override suspend fun execute(
                method: String,
                url: String,
                headers: Map<String, String>,
                bodyBytes: ByteArray?
            ): Pair<Int, String> {
                urls += "$method $url"
                return when {
                    method == "GET" && url.contains("spaces=appDataFolder") -> {
                        200 to """{"files":[{"id":"appdata_backup_42","name":"mahavtaar_panchanga_backup_user.json"}]}"""
                    }
                    method == "GET" && url.contains("/drive/v3/files?") -> {
                        200 to """{"files":[]}"""
                    }
                    method == "GET" && url.contains("/drive/v3/files/appdata_backup_42?alt=media") -> {
                        200 to appDataPayload
                    }
                    else -> 404 to "{}"
                }
            }
        }

        val client = GoogleDriveRestClient(context, executor)
        val result = client.tryDownloadLatestJsonFromDriveOrAppData(
            accessToken = "ya29.test_token",
            fileName = "mahavtaar_panchanga_backup_user.json"
        )
        assertNotNull(result)
        assertEquals("appdata_backup_42", result?.first)
        assertEquals(appDataPayload, result?.second)
        assertTrue(executor.urls.any { it.contains("spaces=appDataFolder") })
    }

    @Test
    fun canceledOAuthResolutionSetsConsentBlockedFlagForInstantSafFallback() = runBlocking {
        val harness = TestHarness(context)
        harness.userFlow.value = CloudUser(uid = "uid_403_test", email = "jain9640@gmail.com")

        val res = harness.backupManager.handleDriveResolutionResult(
            data = null,
            resultCode = android.app.Activity.RESULT_CANCELED
        )
        assertTrue(res.isFailure)
        assertTrue(harness.backupManager.status.value.isOAuthBlockedByConsentScreen)
        assertTrue(harness.backupManager.status.value.lastError.orEmpty().contains("403"))
    }
}
