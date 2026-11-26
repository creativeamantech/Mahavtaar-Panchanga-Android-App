package com.mahavtaar.panchanga.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ScheduleCategory {
    ALL,
    ASTRONOMICAL,
    PANCHANGA,
    MUHURTA,
    CHOGHADIYA,
    HORA,
    SWARA
}

@Serializable
enum class Auspiciousness {
    AUSPICIOUS,
    INAUSPICIOUS,
    NEUTRAL
}

@Serializable
data class ScheduleEvent(
    val id: String,
    val category: ScheduleCategory,
    val titleEn: String,
    val titleHi: String,
    val subtitleEn: String,
    val subtitleHi: String,
    val startTimeMs: Long,
    val endTimeMs: Long?,
    val formattedStart: String,
    val formattedRange: String,
    val formattedEnd: String = "",
    val primaryNadi: SwaraNadi? = null,
    val secondaryNadi: SwaraNadi? = null,
    val tattvaElement: TattvaElement? = null,
    val auspiciousness: Auspiciousness = Auspiciousness.NEUTRAL,
    val isCurrent: Boolean = false,
    val isCompleted: Boolean = false
)

@Serializable
data class NavtaraParihara(
    val deityEn: String,
    val deityHi: String,
    val mantraEn: String,
    val mantraHi: String,
    val donationEn: String,
    val donationHi: String,
    val avoidEn: List<String>,
    val avoidHi: List<String>
)

@Serializable
data class NavtaraResult(
    val birthNakshatraIndex: Int,
    val targetNakshatraIndex: Int,
    val distance: Int,
    val paryaya: Int,
    val taraNumber: Int,
    val taraNameEn: String,
    val taraNameHi: String,
    val natureEn: String,
    val natureHi: String,
    val isAuspicious: Boolean,
    val isNeutral: Boolean,
    val score: Int,
    val descriptionEn: String,
    val descriptionHi: String,
    val intensityLevelEn: String,
    val intensityLevelHi: String,
    val parihara: NavtaraParihara? = null
)

@Serializable
data class VaraTattvaInfo(
    val weekday: Int,
    val element: TattvaElement,
    val elementLabelEn: String,
    val elementLabelHi: String,
    val nadi: SwaraNadi
)

@Serializable
data class TithiTattvaInfo(
    val paksha: String,
    val tithiNumber: Int,
    val tithiInPaksha: Int = 1,
    val tithiName: String,
    val element: TattvaElement,
    val nadi: SwaraNadi,
    val endNadi: SwaraNadi = SwaraNadi.PINGALA,
    val alternatingNadi: SwaraNadi = SwaraNadi.IDA,
    val starts: String? = null,
    val ends: String?,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null
)

@Serializable
data class KaranaTattvaInfo(
    val karanaName: String,
    val element: TattvaElement,
    val nadi: SwaraNadi = SwaraNadi.IDA,
    val starts: String? = null,
    val ends: String?,
    val isBhadraCaution: Boolean = false
)

@Serializable
data class TithiTattvaPeriodItem(
    val number: Int,
    val tithiInPaksha: Int = 1,
    val name: String,
    val paksha: String,
    val groupNameEn: String = "",
    val groupNameHi: String = "",
    val element: TattvaElement,
    val endElement: TattvaElement = element,
    val nadi: SwaraNadi,
    val endNadi: SwaraNadi = SwaraNadi.PINGALA,
    val alternatingNadi: SwaraNadi = nadi,
    val starts: String?,
    val ends: String?,
    val startTimeMs: Long?,
    val endTimeMs: Long?,
    val isActive: Boolean = false
)

@Serializable
data class DailyKaranaTattvaItem(
    val number: Int,
    val karanaName: String,
    val paksha: String,
    val tithiNumber: Int,
    val tithiName: String,
    val element: TattvaElement,
    val nadi: SwaraNadi,
    val starts: String?,
    val ends: String?,
    val startTimeMs: Long?,
    val endTimeMs: Long?,
    val isBhadraCaution: Boolean = false,
    val isActive: Boolean = false
)

@Serializable
data class VaraMasterRuleItem(
    val weekday: Int,
    val dayNameEn: String,
    val dayNameHi: String,
    val rulerEn: String,
    val rulerHi: String,
    val element: TattvaElement,
    val elementLabelEn: String,
    val elementLabelHi: String,
    val nadi: SwaraNadi,
    val isToday: Boolean = false
)

@Serializable
data class TithiMasterRuleItem(
    val rawNumber: Int, // 1..30
    val tithiInPaksha: Int, // 1..15
    val pakshaEn: String,
    val pakshaHi: String,
    val tithiNameEn: String,
    val tithiNameHi: String,
    val groupNameEn: String,
    val groupNameHi: String,
    val element: TattvaElement,
    val startNadi: SwaraNadi,
    val endNadi: SwaraNadi,
    val isActiveToday: Boolean = false
)

@Serializable
data class KaranaMasterRuleItem(
    val number: Int, // 1..11
    val nameEn: String,
    val nameHi: String,
    val categoryEn: String,
    val categoryHi: String,
    val rulerEn: String,
    val rulerHi: String,
    val element: TattvaElement,
    val defaultNadi: SwaraNadi,
    val effectEn: String,
    val effectHi: String,
    val isBhadraCaution: Boolean = false,
    val isActiveToday: Boolean = false
)

@Serializable
data class TattvaHarmonyWindow(
    val horaIndex: Int,
    val horaRuler: String,
    val isDay: Boolean,
    val startTime: String,
    val endTime: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMins: Int,
    val element: TattvaElement,
    val nadi: SwaraNadi,
    val matchedPillarsCount: Int,
    val matchedPillarsEn: String,
    val matchedPillarsHi: String,
    val isActive: Boolean = false
)

@Serializable
data class KaranaTattvaMaasOverlapItem(
    val element: TattvaElement,
    val maasName: String,
    val isAdhika: Boolean,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val overlapPercent: Int
)

@Serializable
data class KaranaTattvaPeriodItem(
    val element: TattvaElement,
    val weight: Int,
    val percentage: Double,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationDays: Int = 0,
    val durationHours: Int = 0,
    val isActive: Boolean,
    val maasOverlaps: List<KaranaTattvaMaasOverlapItem> = emptyList()
)

@Serializable
data class KaranaTattvaCycleReport(
    val cycleType: String, // "CHAITRA_TO_SHARAD" or "SHARAD_TO_CHAITRA"
    val startTimeMs: Long,
    val endTimeMs: Long,
    val totalDurationDays: Int = 0,
    val totalDurationHours: Int = 0,
    val cycleOffset: Int = 0,
    val activePeriod: KaranaTattvaPeriodItem?,
    val periods: List<KaranaTattvaPeriodItem>
)

@Serializable
data class PanchaTattvaReport(
    val varaTattva: VaraTattvaInfo,
    val tithiTattva: TithiTattvaInfo,
    val karanaTattva: KaranaTattvaInfo,
    val activeHoraRuler: String?,
    val activeHoraTattva: TattvaPeriod?,
    val dominantElement: TattvaElement,
    val guidanceEn: String,
    val guidanceHi: String,
    val dayTithiTattvas: List<TithiTattvaPeriodItem> = emptyList(),
    val dayKaranaTattvas: List<DailyKaranaTattvaItem> = emptyList(),
    val harmonyWindows: List<TattvaHarmonyWindow> = emptyList(),
    val varaMasterTable: List<VaraMasterRuleItem> = emptyList(),
    val tithiMasterTable: List<TithiMasterRuleItem> = emptyList(),
    val karanaMasterTable: List<KaranaMasterRuleItem> = emptyList(),
    val karanaCycle: KaranaTattvaCycleReport? = null
)

@Serializable
data class NakshatraSwaraRuleItem(
    val nakshatraNumber: Int,
    val nakshatraName: String,
    val rashiNumber: Int,
    val rashiName: String,
    val startNadi: SwaraNadi,
    val endNadi: SwaraNadi
)

@Serializable
data class NakshatraSwaraAlignmentResult(
    val paksha: String,
    val tithiNumber: Int,
    val tithiName: String,
    val requiredTithiSwara: SwaraNadi,
    val nakshatraNumber: Int,
    val nakshatraName: String,
    val pada: Int,
    val rashiIndex: Int,
    val rashiName: String,
    val supportedNakshatraSwara: SwaraNadi,
    val nakshatraEndSwara: SwaraNadi,
    val nakshatraElement: TattvaElement,
    val rating: String, // "ALIGNED", "NEUTRAL", "MISMATCH"
    val elementWarningEn: String? = null,
    val elementWarningHi: String? = null,
    val cosmicAdviceEn: String,
    val cosmicAdviceHi: String,
    val padaAdviceEn: String,
    val padaAdviceHi: String
)

@Serializable
data class NavagrahaRemedy(
    val id: String,
    val symbol: String,
    val nameEn: String,
    val nameHi: String,
    val deityEn: String,
    val deityHi: String,
    val beejMantra: String,
    val dhyanMantra: String,
    val gayatriMantra: String,
    val vedoktaMantra: String,
    val japCountEn: String,
    val japCountHi: String,
    val gemstoneEn: String,
    val gemstoneHi: String,
    val samidhaEn: String,
    val samidhaHi: String,
    val directionEn: String,
    val directionHi: String,
    val daanEn: String,
    val daanHi: String,
    val fastDayEn: String,
    val fastDayHi: String
)

@Serializable
data class AnnualFestivalItem(
    val id: String,
    val dateIso: String,
    val monthValue: Int,
    val dayOfMonth: Int,
    val weekday: Int,
    val masaNumber: Int,
    val masaName: String,
    val paksha: String,
    val tithiNumber: Int,
    val tithiName: String,
    val observance: VratObservance
)

@Serializable
enum class BirthGender { UNSPECIFIED, MALE, FEMALE }

@Serializable
data class KundliBirthInput(
    val name: String = "Native",
    val year: Int = 1995,
    val month: Int = 8,
    val day: Int = 15,
    val hour: Int = 9,
    val minute: Int = 30,
    val location: CityLocation = CityLocation(
        name = "Bhopal, IN",
        country = "IN",
        latitude = 23.2599,
        longitude = 77.4126,
        timezone = "Asia/Kolkata",
        population = 1798218
    ),
    val ayanamsha: CoordinateSelection = CoordinateSelection.CITRA,
    val gender: BirthGender = BirthGender.UNSPECIFIED,
    val second: Int = 0
)

@Serializable
data class KundliPlanetEntry(
    val id: String,
    val symbol: String,
    val nameEn: String,
    val nameHi: String,
    val shortEn: String,
    val shortHi: String,
    val longitude: Double,
    val signIndex: Int, // 0..11
    val degreeInSign: Double,
    val dmsLabel: String,
    val houseD1: Int, // 1..12
    val signD9: Int, // 0..11
    val houseD9: Int, // 1..12
    val nakshatraNumber: Int, // 1..27
    val nakshatraLord: String,
    val pada: Int, // 1..4
    val isRetrograde: Boolean,
    val isCombust: Boolean,
    val dignityEn: String,
    val dignityHi: String,
    val isExaltedOrOwn: Boolean
)

@Serializable
data class KundliHouseEntry(
    val houseNumber: Int, // 1..12
    val signIndex: Int, // 0..11
    val lordEn: String,
    val lordHi: String,
    val planetIds: List<String>,
    val significanceEn: String,
    val significanceHi: String
)

@Serializable
data class VargaChartSummary(
    val code: String, // D1, D2, D3 ... D60
    val titleEn: String,
    val titleHi: String,
    val purposeEn: String,
    val purposeHi: String,
    val lagnaSignIndex: Int,
    val planetSigns: Map<String, Int> // planetId -> signIndex 0..11
)

@Serializable
data class PratyantardashaItem(
    val planetId: String,
    val planetNameEn: String,
    val planetNameHi: String,
    val startDate: String,
    val endDate: String,
    val durationDays: Int,
    val isCurrent: Boolean
)

@Serializable
data class AntardashaItem(
    val planetId: String,
    val planetNameEn: String,
    val planetNameHi: String,
    val startDate: String,
    val endDate: String,
    val durationMonths: Double,
    val isCurrent: Boolean,
    val pratyantardashas: List<PratyantardashaItem> = emptyList()
)

@Serializable
data class DashaPeriodItem(
    val planetId: String,
    val planetNameEn: String,
    val planetNameHi: String,
    val startDate: String,
    val endDate: String,
    val durationYears: Double,
    val isCurrent: Boolean,
    val antardashas: List<AntardashaItem>
)

@Serializable
data class ShadbalaEntry(
    val planetId: String,
    val nameEn: String,
    val nameHi: String,
    val sthanaBala: Double,
    val digBala: Double,
    val kalaBala: Double,
    val cheshtaBala: Double,
    val naisargikaBala: Double,
    val drikBala: Double,
    val totalVirupas: Double,
    val totalRupas: Double,
    val requiredRupas: Double,
    val isStrong: Boolean,
    val uchchaBala: Double = 0.0,
    val ishtaPhala: Double = 0.0,
    val kashtaPhala: Double = 0.0,
    val rank: Int = 1
)

@Serializable
data class BhavaBalaEntry(
    val houseNumber: Int,
    val signIndex: Int,
    val signLord: String,
    val bhavadhipatiBala: Double,
    val bhavaDigBala: Double,
    val bhavaDrishtiBala: Double,
    val totalVirupas: Double,
    val totalRupas: Double,
    val rank: Int
)

@Serializable
data class AshtakavargaTable(
    val sarvashtakavarga: List<Int>, // 12 signs (0..11) bindu totals
    val bhinnashtakavarga: Map<String, List<Int>>, // 7 planets -> 12 signs bindus
    val strongestSignIndex: Int,
    val weakestSignIndex: Int,
    val trikonaShodhitaSav: List<Int> = emptyList(),
    val ekadhipatyaShodhitaSav: List<Int> = emptyList(),
    val totalSav: Int = 337
)

@Serializable
data class KpPlanetEntry(
    val planetId: String,
    val nameEn: String,
    val nameHi: String,
    val signIndex: Int,
    val signLord: String,
    val starLord: String,
    val subLord: String,
    val dmsLabel: String,
    val subSubLord: String = "",
    val houseOccupied: Int = 1,
    val housesOwned: List<Int> = emptyList(),
    val level1Houses: List<Int> = emptyList(),
    val level2Houses: List<Int> = emptyList(),
    val level3Houses: List<Int> = emptyList(),
    val level4Houses: List<Int> = emptyList()
)

@Serializable
data class KpCuspEntry(
    val houseNumber: Int,
    val longitude: Double,
    val signIndex: Int,
    val dmsLabel: String,
    val signLord: String,
    val starLord: String,
    val subLord: String,
    val subSubLord: String
)

@Serializable
data class KpRulingPlanetEntry(
    val factorEn: String,
    val factorHi: String,
    val planet: String
)

@Serializable
data class JaiminiKarakaEntry(
    val code: String, // AK, AmK, BK, MK, PK, GK, DK
    val titleEn: String,
    val titleHi: String,
    val planetId: String,
    val planetNameEn: String,
    val planetNameHi: String,
    val degreeInSign: Double
)

@Serializable
data class JaiminiArudhaEntry(
    val houseNumber: Int,
    val code: String,
    val nameEn: String,
    val nameHi: String,
    val signIndex: Int,
    val finalHouseFromLagna: Int,
    val isExceptionApplied: Boolean,
    val exceptionReasonEn: String = "",
    val exceptionReasonHi: String = ""
)

@Serializable
data class JaiminiRashiDrishtiEntry(
    val signIndex: Int,
    val mobilityEn: String,
    val mobilityHi: String,
    val aspectedSignIndices: List<Int>,
    val aspectingPlanetIds: List<String>,
    val aspectedPlanetIds: List<String>
)

@Serializable
data class JaiminiCharaDashaSubPeriod(
    val signIndex: Int,
    val startDate: String,
    val endDate: String,
    val durationMonths: Int
)

@Serializable
data class JaiminiCharaDashaPeriod(
    val signIndex: Int,
    val durationYears: Int,
    val isDirect: Boolean,
    val startDate: String,
    val endDate: String,
    val isCurrent: Boolean,
    val subPeriods: List<JaiminiCharaDashaSubPeriod> = emptyList()
)

@Serializable
data class JaiminiKarakamshaInfo(
    val atmakarakaPlanetId: String,
    val karakamshaSignIndex: Int,
    val karakamshaHouseInD1: Int,
    val swamshaSignIndex: Int,
    val planetsInKarakamshaD9: List<String>,
    val planetsAspectingKarakamshaD9: List<String>
)

@Serializable
data class GocharPlanetEffect(
    val planetId: String,
    val nameEn: String,
    val nameHi: String,
    val transitSignIndex: Int,
    val houseFromMoon: Int,
    val isFavorable: Boolean,
    val verdictEn: String,
    val verdictHi: String,
    val transitDegreeInSign: Double = 0.0,
    val houseFromLagna: Int = 1,
    val isRetrograde: Boolean = false,
    val hasVedha: Boolean = false,
    val vedhaPlanetNameEn: String? = null,
    val vedhaPlanetNameHi: String? = null
)

@Serializable
data class GocharReport(
    val transitDateIso: String,
    val natalMoonSignIndex: Int,
    val natalLagnaSignIndex: Int,
    val transits: List<GocharPlanetEffect>,
    val inSadeSati: Boolean,
    val sadeSatiPhaseEn: String,
    val sadeSatiPhaseHi: String,
    val sadeSatiDescEn: String,
    val sadeSatiDescHi: String
)

@Serializable
data class KundliDoshaSummary(
    val isManglik: Boolean,
    val manglikLabelEn: String,
    val manglikLabelHi: String,
    val isKalsarpa: Boolean,
    val kalsarpaLabelEn: String,
    val kalsarpaLabelHi: String,
    val sadeSatiLabelEn: String,
    val sadeSatiLabelHi: String,
    val sadeSatiActive: Boolean
)

@Serializable
data class KundliYogaSummary(
    val nameEn: String,
    val nameHi: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val isAuspicious: Boolean,
    val id: String = "",
    val category: String = "General",
    val isPresent: Boolean = true,
    val isMitigated: Boolean = false,
    val sourceEn: String = "Brihat Parashara Hora Shastra",
    val sourceHi: String = "बृहत्पाराशरहोराशास्त्रम्",
    val matchedConditionsEn: List<String> = emptyList(),
    val matchedConditionsHi: List<String> = emptyList(),
    val failedConditionsEn: List<String> = emptyList(),
    val failedConditionsHi: List<String> = emptyList(),
    val cancellationsEn: List<String> = emptyList(),
    val cancellationsHi: List<String> = emptyList()
)

@Serializable
data class KundliCalculationAudit(
    val julianDay: Double,
    val lstDeg: Double,
    val ayanamshaModel: String,
    val ayanamshaDeg: Double,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val engineVersion: String = "Mahavtaar Drik Engine v2.1",
    val chartHash: String
)

@Serializable
data class KundliReport(
    val input: KundliBirthInput,
    val ayanamshaDeg: Double,
    val lagnaLongitude: Double,
    val lagnaSignIndex: Int,
    val lagnaDegreeInSign: Double,
    val lagnaDms: String,
    val lagnaNakshatraNumber: Int,
    val lagnaPada: Int,
    val navamshaLagnaSignIndex: Int,
    val moonSignIndex: Int,
    val moonNakshatraNumber: Int,
    val moonPada: Int,
    val sunSignIndex: Int,
    val planets: List<KundliPlanetEntry>,
    val housesD1: List<KundliHouseEntry>,
    val housesD9: List<KundliHouseEntry>,
    val vargas: List<VargaChartSummary>,
    val vimshottariDashas: List<DashaPeriodItem>,
    val currentMahadashaLabelEn: String,
    val currentMahadashaLabelHi: String,
    val shadbala: List<ShadbalaEntry>,
    val ashtakavarga: AshtakavargaTable,
    val kpTable: List<KpPlanetEntry>,
    val jaiminiKarakas: List<JaiminiKarakaEntry>,
    val arudhaLagnaSignIndex: Int,
    val gocharEffects: List<GocharPlanetEffect>,
    val doshas: KundliDoshaSummary,
    val yogas: List<KundliYogaSummary>,
    val bhavaBala: List<BhavaBalaEntry> = emptyList(),
    val kpCusps: List<KpCuspEntry> = emptyList(),
    val kpRulingPlanets: List<KpRulingPlanetEntry> = emptyList(),
    val arudhaPadas: List<JaiminiArudhaEntry> = emptyList(),
    val upapadaLagnaSignIndex: Int = 0,
    val karakamsha: JaiminiKarakamshaInfo? = null,
    val jaiminiRashiDrishti: List<JaiminiRashiDrishtiEntry> = emptyList(),
    val charaDashas: List<JaiminiCharaDashaPeriod> = emptyList(),
    val sadeSatiPhaseEn: String = "",
    val sadeSatiPhaseHi: String = "",
    val sadeSatiDescEn: String = "",
    val sadeSatiDescHi: String = "",
    val audit: KundliCalculationAudit? = null,
    val birthBalanceEn: String = "",
    val birthBalanceHi: String = ""
)

@Serializable
data class KootaScoreItem(
    val nameEn: String,
    val nameHi: String,
    val maxScore: Double,
    val obtainedScore: Double,
    val isDosha: Boolean,
    val isCancelled: Boolean,
    val descriptionEn: String,
    val descriptionHi: String
)

@Serializable
data class GunaMilanReport(
    val boyName: String,
    val girlName: String,
    val boyNakshatra: Int,
    val girlNakshatra: Int,
    val boyRasi: Int,
    val girlRasi: Int,
    val kootas: List<KootaScoreItem>,
    val totalScore: Double,
    val maxScore: Double,
    val percentage: Int,
    val verdictEn: String,
    val verdictHi: String,
    val isRecommended: Boolean,
    val manglikCompatible: Boolean,
    val manglikNoteEn: String,
    val manglikNoteHi: String,
    val remedyEn: String,
    val remedyHi: String
)
