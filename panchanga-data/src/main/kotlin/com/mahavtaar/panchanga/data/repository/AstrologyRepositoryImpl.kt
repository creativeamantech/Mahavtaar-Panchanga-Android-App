package com.mahavtaar.panchanga.data.repository

import com.mahavtaar.panchanga.astronomy.DailyScheduleBuilder
import com.mahavtaar.panchanga.astronomy.DrikPanchangaEngine
import com.mahavtaar.panchanga.astronomy.GunaMilanEngine
import com.mahavtaar.panchanga.astronomy.KundliAstrologyEngine
import com.mahavtaar.panchanga.astronomy.KundliRulesAndGocharEngine
import com.mahavtaar.panchanga.astronomy.NavagrahaCatalog
import com.mahavtaar.panchanga.astronomy.NavtaraEngine
import com.mahavtaar.panchanga.astronomy.PanchaTattvaEngine
import com.mahavtaar.panchanga.astronomy.SwaraYogaEngine
import com.mahavtaar.panchanga.core.AppDispatchers
import com.mahavtaar.panchanga.data.audio.VedicVoiceAudioController
import com.mahavtaar.panchanga.database.dao.NavtaraPersistenceDao
import com.mahavtaar.panchanga.database.dao.SavedKundliDao
import com.mahavtaar.panchanga.database.entity.NavtaraFeedbackEntity
import com.mahavtaar.panchanga.database.entity.NavtaraProfileEntity
import com.mahavtaar.panchanga.database.entity.SavedKundliChartEntity
import com.mahavtaar.panchanga.database.entity.SavedNavtaraReportEntity
import com.mahavtaar.panchanga.domain.model.AnnualFestivalItem
import com.mahavtaar.panchanga.domain.model.BuiltinVedicTone
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.GocharReport
import com.mahavtaar.panchanga.domain.model.GunaMilanReport
import com.mahavtaar.panchanga.domain.model.KundliBirthInput
import com.mahavtaar.panchanga.domain.model.KundliReport
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.NakshatraSwaraAlignmentResult
import com.mahavtaar.panchanga.domain.model.NavagrahaRemedy
import com.mahavtaar.panchanga.domain.model.NavtaraDailyFeedback
import com.mahavtaar.panchanga.domain.model.NavtaraDayCategory
import com.mahavtaar.panchanga.domain.model.NavtaraFeedbackRating
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodDay
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodReport
import com.mahavtaar.panchanga.domain.model.NavtaraPeriodStats
import com.mahavtaar.panchanga.domain.model.NavtaraProfile
import com.mahavtaar.panchanga.domain.model.NavtaraResult
import com.mahavtaar.panchanga.domain.model.NavtaraTransitionItem
import com.mahavtaar.panchanga.domain.model.PanchaTattvaReport
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SavedKundliChart
import com.mahavtaar.panchanga.domain.model.SavedNavtaraReport
import com.mahavtaar.panchanga.domain.model.ScheduleEvent
import com.mahavtaar.panchanga.domain.model.TuneSourceType
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AstrologyRepositoryImpl @Inject constructor(
    private val engine: DrikPanchangaEngine,
    private val audioController: VedicVoiceAudioController,
    private val navtaraDao: NavtaraPersistenceDao,
    private val savedKundliDao: SavedKundliDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val dispatchers: AppDispatchers
) : AstrologyRepository {

    private val jsonCodec = Json { ignoreUnknownKeys = true }
    private val annualFestivalCache = ConcurrentHashMap<String, List<AnnualFestivalItem>>()
    private val migrationMutex = Mutex()
    @Volatile
    private var kundliMigrated = false

    override fun buildDailySchedule(
        snapshot: PanchangaSnapshot,
        dailyHoras: DailyHoras,
        nowMs: Long
    ): List<ScheduleEvent> = DailyScheduleBuilder.build(snapshot, dailyHoras, nowMs)

    override fun computeNavtara(
        birthNakshatraIndex: Int,
        targetNakshatraIndex: Int
    ): NavtaraResult = NavtaraEngine.calculate(birthNakshatraIndex, targetNakshatraIndex)

    override fun computeNavtaraMatrix(
        birthNakshatraIndex: Int
    ): List<NavtaraResult> = NavtaraEngine.generateMatrix(birthNakshatraIndex)

    override suspend fun computeNavtaraPeriodReport(
        profileId: Long,
        profileName: String,
        birthNakshatraIndex: Int,
        birthPada: Int,
        startDate: LocalDate,
        endDate: LocalDate,
        isDateRange: Boolean,
        location: CityLocation,
        monthSystem: MonthSystem,
        ayanamsha: CoordinateSelection
    ): NavtaraPeriodReport {
        val clampedEnd = if (endDate.isBefore(startDate)) startDate else {
            val diff = ChronoUnit.DAYS.between(startDate, endDate)
            if (diff > 92L) startDate.plusDays(92L) else endDate
        }
        val feedbacksByDate = if (profileId > 0L) {
            withContext(dispatchers.io) {
                navtaraDao.getFeedbacksInRange(profileId, startDate.toString(), clampedEnd.toString())
                    .associate { e ->
                        e.dateIso to NavtaraDailyFeedback(
                            profileId = e.profileId,
                            dateIso = e.dateIso,
                            rating = NavtaraFeedbackRating.fromName(e.rating),
                            note = e.note,
                            updatedAtMs = e.updatedAtMs
                        )
                    }
            }
        } else {
            emptyMap()
        }

        return withContext(dispatchers.default) {
            val days = mutableListOf<NavtaraPeriodDay>()
            val transitions = mutableListOf<NavtaraTransitionItem>()

            var auspiciousCount = 0
            var mixedCount = 0
            var cautionCount = 0
            var neutralCount = 0
            var feedbackCount = 0
            var auspiciousFb = 0
            var neutralFb = 0
            var cautionFb = 0

            var cursor = startDate
            while (!cursor.isAfter(clampedEnd)) {
                val snap = engine.computePanchanga(
                    location = location,
                    date = cursor,
                    monthSystem = monthSystem,
                    coordinateSelection = ayanamsha,
                    includeExtendedTransits = false
                )
                val primaryTithi = snap.tithi.firstOrNull()
                val curNak = snap.nakshatra.firstOrNull()
                val nextNak = snap.nakshatra.getOrNull(1)
                val curNakNum = (curNak?.number ?: 1).coerceIn(1, 27)
                val curTara = NavtaraEngine.calculate(birthNakshatraIndex, curNakNum)
                val nextTara = nextNak?.let { NavtaraEngine.calculate(birthNakshatraIndex, it.number.coerceIn(1, 27)) }

                val category = when {
                    nextTara != null && curTara.isAuspicious != nextTara.isAuspicious -> NavtaraDayCategory.MIXED
                    curTara.isAuspicious -> NavtaraDayCategory.AUSPICIOUS
                    curTara.isNeutral -> NavtaraDayCategory.NEUTRAL
                    else -> NavtaraDayCategory.CAUTION
                }

                when (category) {
                    NavtaraDayCategory.AUSPICIOUS -> auspiciousCount++
                    NavtaraDayCategory.MIXED -> mixedCount++
                    NavtaraDayCategory.CAUTION -> cautionCount++
                    NavtaraDayCategory.NEUTRAL -> neutralCount++
                }

                val (recEn, recHi) = buildDayRecommendation(curTara, nextTara, category, curNak?.ends)
                val fb = feedbacksByDate[snap.dateIso]
                if (fb != null) {
                    feedbackCount++
                    when (fb.rating) {
                        NavtaraFeedbackRating.AUSPICIOUS -> auspiciousFb++
                        NavtaraFeedbackRating.NEUTRAL -> neutralFb++
                        NavtaraFeedbackRating.CAUTION -> cautionFb++
                    }
                }

                days += NavtaraPeriodDay(
                    dateIso = snap.dateIso,
                    dayOfMonth = cursor.dayOfMonth,
                    month = cursor.monthValue,
                    year = cursor.year,
                    weekday = snap.weekday,
                    vaara = snap.vaara,
                    tithiNumber = primaryTithi?.number ?: 1,
                    tithiName = primaryTithi?.name ?: "",
                    paksha = snap.paksha,
                    tithiEnds = primaryTithi?.ends,
                    nakshatraNumber = curNakNum,
                    nakshatraName = curNak?.name ?: "",
                    nakshatraEnds = curNak?.ends,
                    nextNakshatraNumber = nextNak?.number,
                    nextNakshatraName = nextNak?.name,
                    moonRasi = snap.moonRasi,
                    currentTara = curTara,
                    nextTara = nextTara,
                    category = category,
                    recommendationEn = recEn,
                    recommendationHi = recHi,
                    feedback = fb
                )

                if (nextNak != null && nextTara != null) {
                    transitions += NavtaraTransitionItem(
                        dateIso = snap.dateIso,
                        dayOfMonth = cursor.dayOfMonth,
                        fromNakshatraNumber = curNakNum,
                        fromNakshatraName = curNak?.name ?: "",
                        toNakshatraNumber = nextNak.number,
                        toNakshatraName = nextNak.name,
                        transitionTime = curNak?.ends ?: nextNak.starts ?: "—",
                        fromTara = curTara,
                        toTara = nextTara
                    )
                }

                cursor = cursor.plusDays(1L)
            }

            NavtaraPeriodReport(
                profileId = profileId,
                profileName = profileName,
                birthNakshatraIndex = birthNakshatraIndex,
                birthPada = birthPada,
                startDateIso = startDate.toString(),
                endDateIso = clampedEnd.toString(),
                isDateRange = isDateRange,
                location = location,
                ayanamsha = ayanamsha,
                monthSystem = monthSystem,
                days = days,
                transitions = transitions,
                stats = NavtaraPeriodStats(
                    totalDays = days.size,
                    auspiciousCount = auspiciousCount,
                    mixedCount = mixedCount,
                    cautionCount = cautionCount,
                    neutralCount = neutralCount,
                    feedbackCount = feedbackCount,
                    auspiciousFeedbackCount = auspiciousFb,
                    neutralFeedbackCount = neutralFb,
                    cautionFeedbackCount = cautionFb
                )
            )
        }
    }

    private fun buildDayRecommendation(
        curTara: NavtaraResult,
        nextTara: NavtaraResult?,
        category: NavtaraDayCategory,
        nakEnds: String?
    ): Pair<String, String> {
        if (category == NavtaraDayCategory.MIXED && nextTara != null) {
            val switchStr = nakEnds?.let { " at $it" } ?: ""
            val switchStrHi = nakEnds?.let { " ($it पर)" } ?: ""
            return if (curTara.isAuspicious && !nextTara.isAuspicious) {
                "Favorable window until Nakshatra changes$switchStr; complete key work early before ${nextTara.taraNameEn}." to
                    "नक्षत्र परिवर्तन$switchStrHi तक शुभ समय; ${nextTara.taraNameHi} प्रारम्भ होने से पूर्व मुख्य कार्य पूर्ण करें।"
            } else {
                "Caution early in the day; opens into favorable ${nextTara.taraNameEn}$switchStr." to
                    "प्रारम्भिक समय संभलकर बिताएँ; परिवर्तन$switchStrHi के पश्चात् शुभ ${nextTara.taraNameHi} प्रारम्भ होगा।"
            }
        }
        return when (category) {
            NavtaraDayCategory.AUSPICIOUS ->
                "${curTara.taraNameEn} (${curTara.score}/100): Ideal for important launches, finance, and auspicious vows." to
                    "${curTara.taraNameHi} (${curTara.score}/100): नए कार्यों, आर्थिक निर्णय व शुभ संकल्प हेतु उत्तम दिन।"
            NavtaraDayCategory.NEUTRAL ->
                "${curTara.taraNameEn} (${curTara.score}/100): Steady for routine work; stay mindful of health and rest." to
                    "${curTara.taraNameHi} (${curTara.score}/100): सामान्य नित्य कर्म हेतु उपयुक्त; स्वास्थ्य व संयम बनाए रखें।"
            else ->
                "${curTara.taraNameEn} (${curTara.score}/100): Avoid risky commitments or disputes; practice ${curTara.parihara?.mantraEn ?: "mindful restraint"}." to
                    "${curTara.taraNameHi} (${curTara.score}/100): जोखिम भरे निवेश व विवाद से बचें; ${curTara.parihara?.mantraHi ?: "संयम व जप"} करें।"
        }
    }

    override fun observeNavtaraProfiles(): Flow<List<NavtaraProfile>> =
        navtaraDao.observeProfiles().map { rows ->
            rows.map { e ->
                NavtaraProfile(
                    id = e.id,
                    name = e.name,
                    birthNakshatraIndex = e.birthNakshatraIndex,
                    birthPada = e.birthPada,
                    notes = e.notes,
                    isDefault = e.isDefault,
                    updatedAtMs = e.updatedAtMs
                )
            }
        }

    override suspend fun saveNavtaraProfile(profile: NavtaraProfile): Long = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val existingCount = navtaraDao.observeProfiles().firstOrNull()?.size ?: 0
        val shouldBeDefault = profile.isDefault || existingCount == 0
        val insertedId = navtaraDao.upsertProfile(
            NavtaraProfileEntity(
                id = profile.id,
                name = profile.name.trim().ifBlank { "Native" },
                birthNakshatraIndex = profile.birthNakshatraIndex.coerceIn(1, 27),
                birthPada = profile.birthPada.coerceIn(1, 4),
                notes = profile.notes.trim(),
                isDefault = shouldBeDefault,
                updatedAtMs = now
            )
        )
        val resolvedId = if (profile.id > 0L) profile.id else insertedId
        if (shouldBeDefault) {
            navtaraDao.setDefaultProfile(resolvedId)
            preferencesRepository.setBirthNakshatra(profile.birthNakshatraIndex.coerceIn(1, 27))
        }
        resolvedId
    }

    override suspend fun setDefaultNavtaraProfile(profileId: Long) {
        withContext(dispatchers.io) {
            navtaraDao.setDefaultProfile(profileId)
            navtaraDao.getProfileById(profileId)?.let { p ->
                preferencesRepository.setBirthNakshatra(p.birthNakshatraIndex.coerceIn(1, 27))
            }
        }
    }

    override suspend fun deleteNavtaraProfile(profileId: Long) = withContext(dispatchers.io) {
        navtaraDao.deleteProfile(profileId)
    }

    override fun observeNavtaraFeedbacks(profileId: Long): Flow<List<NavtaraDailyFeedback>> =
        navtaraDao.observeFeedbacksForProfile(profileId).map { list ->
            list.map { e ->
                NavtaraDailyFeedback(
                    profileId = e.profileId,
                    dateIso = e.dateIso,
                    rating = NavtaraFeedbackRating.fromName(e.rating),
                    note = e.note,
                    updatedAtMs = e.updatedAtMs
                )
            }
        }

    override suspend fun saveNavtaraFeedback(feedback: NavtaraDailyFeedback) = withContext(dispatchers.io) {
        navtaraDao.upsertFeedback(
            NavtaraFeedbackEntity(
                profileId = feedback.profileId,
                dateIso = feedback.dateIso,
                rating = feedback.rating.name,
                note = feedback.note.trim(),
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteNavtaraFeedback(profileId: Long, dateIso: String) = withContext(dispatchers.io) {
        navtaraDao.deleteFeedback(profileId, dateIso)
    }

    override fun observeSavedNavtaraReports(): Flow<List<SavedNavtaraReport>> =
        navtaraDao.observeSavedReports().map { rows ->
            rows.map { e ->
                SavedNavtaraReport(
                    id = e.id,
                    profileId = e.profileId,
                    profileName = e.profileName,
                    birthNakshatraIndex = e.birthNakshatraIndex,
                    birthPada = e.birthPada,
                    startDateIso = e.startDateIso,
                    endDateIso = e.endDateIso,
                    isDateRange = e.isDateRange,
                    locationName = e.locationName,
                    latitude = e.latitude,
                    longitude = e.longitude,
                    timezone = e.timezone,
                    ayanamshaKey = CoordinateSelection.fromKey(e.ayanamshaKey),
                    monthSystem = MonthSystem.fromKey(e.monthSystemKey),
                    auspiciousCount = e.auspiciousCount,
                    mixedCount = e.mixedCount,
                    cautionCount = e.cautionCount,
                    neutralCount = e.neutralCount,
                    notes = e.notes,
                    createdAtMs = e.createdAtMs
                )
            }
        }

    override suspend fun saveNavtaraReport(report: SavedNavtaraReport): Long = withContext(dispatchers.io) {
        navtaraDao.upsertReport(
            SavedNavtaraReportEntity(
                id = report.id,
                profileId = report.profileId,
                profileName = report.profileName,
                birthNakshatraIndex = report.birthNakshatraIndex,
                birthPada = report.birthPada,
                startDateIso = report.startDateIso,
                endDateIso = report.endDateIso,
                isDateRange = report.isDateRange,
                locationName = report.locationName,
                latitude = report.latitude,
                longitude = report.longitude,
                timezone = report.timezone,
                ayanamshaKey = report.ayanamshaKey.apiKey,
                monthSystemKey = report.monthSystem.apiKey,
                auspiciousCount = report.auspiciousCount,
                mixedCount = report.mixedCount,
                cautionCount = report.cautionCount,
                neutralCount = report.neutralCount,
                notes = report.notes,
                createdAtMs = if (report.createdAtMs > 0L) report.createdAtMs else System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteNavtaraReport(reportId: Long) = withContext(dispatchers.io) {
        navtaraDao.deleteReport(reportId)
    }

    private suspend fun migrateLegacyKundliProfilesIfNeeded() {
        if (kundliMigrated) return
        migrationMutex.withLock {
            if (kundliMigrated) return
            withContext(dispatchers.io) {
                val existingCount = savedKundliDao.count()
                if (existingCount == 0) {
                    val legacy = preferencesRepository.observeSavedKundliProfiles().firstOrNull().orEmpty()
                    val now = System.currentTimeMillis()
                    legacy.forEach { input ->
                        val computed = runCatching { KundliAstrologyEngine.compute(input) }.getOrNull()
                        savedKundliDao.upsert(
                            SavedKundliChartEntity(
                                name = input.name,
                                birthYear = input.year,
                                birthMonth = input.month,
                                birthDay = input.day,
                                birthHour = input.hour,
                                birthMinute = input.minute,
                                placeName = input.location.name,
                                latitude = input.location.latitude,
                                longitude = input.location.longitude,
                                timezone = input.location.timezone,
                                inputJson = jsonCodec.encodeToString(input),
                                lagnaSignIndex = computed?.lagnaSignIndex ?: 0,
                                moonSignIndex = computed?.moonSignIndex ?: 0,
                                moonNakshatraNumber = computed?.moonNakshatraNumber ?: 1,
                                createdAtMs = now,
                                updatedAtMs = now
                            )
                        )
                    }
                }
            }
            kundliMigrated = true
        }
    }

    override fun observeSavedKundliCharts(): Flow<List<SavedKundliChart>> =
        savedKundliDao.observeAll()
            .onStart { migrateLegacyKundliProfilesIfNeeded() }
            .map { rows ->
                rows.mapNotNull { e ->
                    val input = runCatching {
                        jsonCodec.decodeFromString<KundliBirthInput>(e.inputJson)
                    }.getOrNull() ?: return@mapNotNull null
                    SavedKundliChart(
                        id = e.id,
                        input = input,
                        lagnaSignIndex = e.lagnaSignIndex,
                        moonSignIndex = e.moonSignIndex,
                        moonNakshatraNumber = e.moonNakshatraNumber,
                        createdAtMs = e.createdAtMs,
                        updatedAtMs = e.updatedAtMs
                    )
                }
            }

    override suspend fun saveKundliChart(chart: SavedKundliChart): Long = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val normInput = chart.input.copy(name = chart.input.name.trim().ifBlank { "Native" })
        savedKundliDao.upsert(
            SavedKundliChartEntity(
                id = chart.id,
                name = normInput.name,
                birthYear = normInput.year,
                birthMonth = normInput.month,
                birthDay = normInput.day,
                birthHour = normInput.hour,
                birthMinute = normInput.minute,
                placeName = normInput.location.name,
                latitude = normInput.location.latitude,
                longitude = normInput.location.longitude,
                timezone = normInput.location.timezone,
                inputJson = jsonCodec.encodeToString(normInput),
                lagnaSignIndex = chart.lagnaSignIndex,
                moonSignIndex = chart.moonSignIndex,
                moonNakshatraNumber = chart.moonNakshatraNumber,
                createdAtMs = if (chart.createdAtMs > 0L) chart.createdAtMs else now,
                updatedAtMs = now
            )
        )
    }

    override suspend fun deleteKundliChart(chartId: Long) = withContext(dispatchers.io) {
        savedKundliDao.deleteById(chartId)
    }

    override fun computePanchaTattva(
        snapshot: PanchangaSnapshot,
        dailyHoras: DailyHoras,
        nowMs: Long,
        cycleOffset: Int
    ): PanchaTattvaReport = PanchaTattvaEngine.compute(snapshot, dailyHoras, nowMs, cycleOffset)

    override fun getNavagrahaRemedies(): List<NavagrahaRemedy> = NavagrahaCatalog.remedies

    override suspend fun getAnnualFestivals(
        location: CityLocation,
        year: Int,
        monthSystem: MonthSystem,
        ayanamsha: CoordinateSelection
    ): List<AnnualFestivalItem> = withContext(dispatchers.default) {
        val latKey = "%.5f".format(Locale.US, location.latitude)
        val lonKey = "%.5f".format(Locale.US, location.longitude)
        val key = "${location.name}|$latKey|$lonKey|${location.timezone}|$year|${monthSystem.apiKey}|${ayanamsha.apiKey}"
        annualFestivalCache[key]?.let { return@withContext it }

        val items = mutableListOf<AnnualFestivalItem>()
        for (month in 1..12) {
            val days = engine.computeMonthlyCalendar(
                location = location,
                year = year,
                month = month,
                monthSystem = monthSystem,
                coordinateSelection = ayanamsha
            )
            days.forEach { day ->
                if (day.observances.isNotEmpty()) {
                    val date = LocalDate.of(day.year, day.month, day.dayOfMonth)
                    val weekday = date.dayOfWeek.value % 7
                    day.observances.forEach { obs ->
                        items += AnnualFestivalItem(
                            id = "${day.dateIso}_${obs.id}",
                            dateIso = day.dateIso,
                            monthValue = day.month,
                            dayOfMonth = day.dayOfMonth,
                            weekday = weekday,
                            masaNumber = 0,
                            masaName = day.masa,
                            paksha = day.paksha,
                            tithiNumber = day.primaryTithiNumber,
                            tithiName = day.tithiName,
                            observance = obs
                        )
                    }
                }
            }
        }
        annualFestivalCache[key] = items
        items
    }

    override suspend fun computeKundli(input: KundliBirthInput): KundliReport =
        withContext(dispatchers.default) {
            KundliAstrologyEngine.compute(input)
        }

    override fun computeGunaMilan(
        boyName: String,
        boyNakshatra: Int,
        boyPada: Int,
        boyRasi: Int,
        boyManglik: Boolean,
        girlName: String,
        girlNakshatra: Int,
        girlPada: Int,
        girlRasi: Int,
        girlManglik: Boolean
    ): GunaMilanReport = GunaMilanEngine.compute(
        boyName = boyName,
        boyNakshatra = boyNakshatra,
        boyPada = boyPada,
        boyRasi = boyRasi,
        boyManglik = boyManglik,
        girlName = girlName,
        girlNakshatra = girlNakshatra,
        girlPada = girlPada,
        girlRasi = girlRasi,
        girlManglik = girlManglik
    )

    override fun computeGocharForDate(
        natalMoonSignIndex: Int,
        natalLagnaSignIndex: Int,
        transitDate: LocalDate,
        ayanamsha: CoordinateSelection
    ): GocharReport = KundliRulesAndGocharEngine.computeGocharForDate(
        natalMoonSignIndex = natalMoonSignIndex,
        natalLagnaSignIndex = natalLagnaSignIndex,
        transitDate = transitDate,
        ayanamsha = ayanamsha
    )

    override fun evaluateNakshatraSwaraAlignment(
        paksha: String,
        tithiNumber: Int,
        nakshatraNumber: Int,
        pada: Int
    ): NakshatraSwaraAlignmentResult = SwaraYogaEngine.evaluateNakshatraSwaraAlignment(
        paksha = paksha,
        tithiNumber = tithiNumber,
        nakshatraNumber = nakshatraNumber,
        pada = pada
    )

    override fun previewTattvaChime() {
        audioController.playTunePreview(
            tuneSource = TuneSourceType.BUILTIN_VEDIC,
            tuneUriOrId = BuiltinVedicTone.SINGING_BOWL.id
        )
    }
}
