package com.mahavtaar.panchanga.domain.repository

import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.MonthlyCalendarDay
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SwaraYogaState
import com.mahavtaar.panchanga.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PanchangaRepository {
    suspend fun getDailyPanchanga(
        location: CityLocation,
        date: LocalDate,
        monthSystem: MonthSystem,
        ayanamsha: CoordinateSelection,
        includeExtendedTransits: Boolean = true
    ): PanchangaSnapshot

    suspend fun getMonthlyCalendar(
        location: CityLocation,
        year: Int,
        month: Int,
        monthSystem: MonthSystem,
        ayanamsha: CoordinateSelection
    ): List<MonthlyCalendarDay>

    fun computeDailyHoras(snapshot: PanchangaSnapshot, nowMs: Long): DailyHoras

    fun computeLiveSwaraYoga(snapshot: PanchangaSnapshot, nowMs: Long): SwaraYogaState

    suspend fun clearExpiredCache()
}

interface LocationRepository {
    fun observeSavedLocations(): Flow<List<CityLocation>>
    suspend fun saveLocation(location: CityLocation)
    suspend fun setDefaultLocation(name: String)
    suspend fun removeLocation(name: String)
    suspend fun searchCities(query: String, limit: Int = 30): List<CityLocation>
    suspend fun getPopularCities(): List<CityLocation>
    suspend fun resolveNearestCity(latitude: Double, longitude: Double): CityLocation
}

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setSelectedLocation(location: CityLocation)
    suspend fun setMonthSystem(monthSystem: MonthSystem)
    suspend fun setAyanamsha(ayanamsha: CoordinateSelection)
    suspend fun setThemeMode(themeMode: com.mahavtaar.panchanga.domain.model.AppThemeMode)
    suspend fun setLanguage(language: com.mahavtaar.panchanga.domain.model.AppLanguage)
    suspend fun setBirthNakshatra(nakshatra: Int)
    suspend fun setUse24HourFormat(use24Hour: Boolean)
    suspend fun setTattvaMinimalMode(enabled: Boolean) = Unit
    suspend fun setTattvaHiddenFeatures(hiddenFeatures: Set<String>) = Unit
    suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    suspend fun setPinnedHomeModules(moduleKeys: List<String>) = Unit
    suspend fun setWidgetGlassStyle(style: com.mahavtaar.panchanga.domain.model.WidgetGlassStyle) = Unit
    suspend fun setWidgetOpacity(opacity: Float) = Unit
    suspend fun updateNotificationSettings(
        notifySunrise: Boolean? = null,
        notifySunset: Boolean? = null,
        notifyTithiChange: Boolean? = null,
        notifyNakshatraChange: Boolean? = null,
        notifyVratReminders: Boolean? = null
    )
    suspend fun resetDefaults()
    fun observeSavedKundliProfiles(): Flow<List<com.mahavtaar.panchanga.domain.model.KundliBirthInput>> = kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun saveKundliProfile(profile: com.mahavtaar.panchanga.domain.model.KundliBirthInput) = Unit
    suspend fun deleteKundliProfile(profileName: String) = Unit
}

interface AstrologyRepository {
    fun buildDailySchedule(
        snapshot: PanchangaSnapshot,
        dailyHoras: DailyHoras,
        nowMs: Long
    ): List<com.mahavtaar.panchanga.domain.model.ScheduleEvent>

    fun computeNavtara(
        birthNakshatraIndex: Int,
        targetNakshatraIndex: Int
    ): com.mahavtaar.panchanga.domain.model.NavtaraResult

    fun computeNavtaraMatrix(
        birthNakshatraIndex: Int
    ): List<com.mahavtaar.panchanga.domain.model.NavtaraResult>

    suspend fun computeNavtaraPeriodReport(
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
    ): com.mahavtaar.panchanga.domain.model.NavtaraPeriodReport

    fun observeNavtaraProfiles(): Flow<List<com.mahavtaar.panchanga.domain.model.NavtaraProfile>>
    suspend fun saveNavtaraProfile(profile: com.mahavtaar.panchanga.domain.model.NavtaraProfile): Long
    suspend fun setDefaultNavtaraProfile(profileId: Long)
    suspend fun deleteNavtaraProfile(profileId: Long)

    fun observeNavtaraFeedbacks(profileId: Long): Flow<List<com.mahavtaar.panchanga.domain.model.NavtaraDailyFeedback>>
    suspend fun saveNavtaraFeedback(feedback: com.mahavtaar.panchanga.domain.model.NavtaraDailyFeedback)
    suspend fun deleteNavtaraFeedback(profileId: Long, dateIso: String)

    fun observeSavedNavtaraReports(): Flow<List<com.mahavtaar.panchanga.domain.model.SavedNavtaraReport>>
    suspend fun saveNavtaraReport(report: com.mahavtaar.panchanga.domain.model.SavedNavtaraReport): Long
    suspend fun deleteNavtaraReport(reportId: Long)

    fun observeSavedKundliCharts(): Flow<List<com.mahavtaar.panchanga.domain.model.SavedKundliChart>>
    suspend fun saveKundliChart(chart: com.mahavtaar.panchanga.domain.model.SavedKundliChart): Long
    suspend fun deleteKundliChart(chartId: Long)

    fun computePanchaTattva(
        snapshot: PanchangaSnapshot,
        dailyHoras: DailyHoras,
        nowMs: Long,
        cycleOffset: Int = 0
    ): com.mahavtaar.panchanga.domain.model.PanchaTattvaReport

    fun getNavagrahaRemedies(): List<com.mahavtaar.panchanga.domain.model.NavagrahaRemedy>

    suspend fun getAnnualFestivals(
        location: CityLocation,
        year: Int,
        monthSystem: MonthSystem,
        ayanamsha: CoordinateSelection
    ): List<com.mahavtaar.panchanga.domain.model.AnnualFestivalItem>

    suspend fun computeKundli(
        input: com.mahavtaar.panchanga.domain.model.KundliBirthInput
    ): com.mahavtaar.panchanga.domain.model.KundliReport

    fun computeGunaMilan(
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
    ): com.mahavtaar.panchanga.domain.model.GunaMilanReport

    fun computeGocharForDate(
        natalMoonSignIndex: Int,
        natalLagnaSignIndex: Int,
        transitDate: LocalDate,
        ayanamsha: CoordinateSelection
    ): com.mahavtaar.panchanga.domain.model.GocharReport

    fun evaluateNakshatraSwaraAlignment(
        paksha: String,
        tithiNumber: Int,
        nakshatraNumber: Int,
        pada: Int
    ): com.mahavtaar.panchanga.domain.model.NakshatraSwaraAlignmentResult

    fun previewTattvaChime()
}
