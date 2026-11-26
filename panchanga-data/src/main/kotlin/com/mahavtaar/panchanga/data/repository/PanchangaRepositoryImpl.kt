package com.mahavtaar.panchanga.data.repository

import com.mahavtaar.panchanga.astronomy.DrikPanchangaEngine
import com.mahavtaar.panchanga.astronomy.HoraEngine
import com.mahavtaar.panchanga.astronomy.SwaraYogaEngine
import com.mahavtaar.panchanga.core.AppDispatchers
import com.mahavtaar.panchanga.core.TimeFormatters
import com.mahavtaar.panchanga.database.dao.PanchangaCacheDao
import com.mahavtaar.panchanga.database.entity.CachedPanchangaEntity
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.MonthlyCalendarDay
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SwaraYogaState
import com.mahavtaar.panchanga.domain.repository.PanchangaRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PanchangaRepositoryImpl @Inject constructor(
    private val engine: DrikPanchangaEngine,
    private val cacheDao: PanchangaCacheDao,
    private val dispatchers: AppDispatchers
) : PanchangaRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val memoryCache = ConcurrentHashMap<String, PanchangaSnapshot>()
    private val monthMemoryCache = ConcurrentHashMap<String, List<MonthlyCalendarDay>>()

    private fun buildCacheKey(
        location: CityLocation,
        date: LocalDate,
        monthSystem: MonthSystem,
        ayanamsha: CoordinateSelection,
        includeExtendedTransits: Boolean
    ): String {
        val latKey = "%.5f".format(java.util.Locale.US, location.latitude)
        val lonKey = "%.5f".format(java.util.Locale.US, location.longitude)
        return "v4|$date|$latKey|$lonKey|${location.timezone}|${monthSystem.apiKey}|${ayanamsha.apiKey}|$includeExtendedTransits"
    }

    override suspend fun getDailyPanchanga(
        location: CityLocation,
        date: LocalDate,
        monthSystem: MonthSystem,
        ayanamsha: CoordinateSelection,
        includeExtendedTransits: Boolean
    ): PanchangaSnapshot {
        val key = buildCacheKey(location, date, monthSystem, ayanamsha, includeExtendedTransits)
        memoryCache[key]?.let { return it }

        // Check Room offline cache
        val cached = withContext(dispatchers.io) {
            cacheDao.getByKey(key)
        }
        if (cached != null) {
            val decoded = runCatching {
                json.decodeFromString<PanchangaSnapshot>(cached.payloadJson)
            }.getOrNull()
            if (decoded != null) {
                memoryCache[key] = decoded
                return decoded
            }
        }

        // Compute via Drik Ganita engine on Dispatchers.Default
        val computed = withContext(dispatchers.default) {
            engine.computePanchanga(
                location = location,
                date = date,
                monthSystem = monthSystem,
                coordinateSelection = ayanamsha,
                includeExtendedTransits = includeExtendedTransits
            )
        }

        memoryCache[key] = computed
        withContext(dispatchers.io) {
            runCatching {
                cacheDao.upsert(
                    CachedPanchangaEntity(
                        cacheKey = key,
                        dateIso = computed.dateIso,
                        cityName = location.name,
                        payloadJson = json.encodeToString(computed),
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
            }
        }
        return computed
    }

    override suspend fun getMonthlyCalendar(
        location: CityLocation,
        year: Int,
        month: Int,
        monthSystem: MonthSystem,
        ayanamsha: CoordinateSelection
    ): List<MonthlyCalendarDay> {
        val latKey = "%.5f".format(java.util.Locale.US, location.latitude)
        val lonKey = "%.5f".format(java.util.Locale.US, location.longitude)
        val key = "m2|$year-$month|$latKey|$lonKey|${location.timezone}|${monthSystem.apiKey}|${ayanamsha.apiKey}"
        monthMemoryCache[key]?.let { return it }

        val days = withContext(dispatchers.default) {
            engine.computeMonthlyCalendar(
                location = location,
                year = year,
                month = month,
                monthSystem = monthSystem,
                coordinateSelection = ayanamsha
            )
        }
        monthMemoryCache[key] = days
        return days
    }

    override fun computeDailyHoras(snapshot: PanchangaSnapshot, nowMs: Long): DailyHoras =
        HoraEngine.resolveForSnapshot(snapshot, nowMs)

    override fun computeLiveSwaraYoga(snapshot: PanchangaSnapshot, nowMs: Long): SwaraYogaState {
        val zoned = Instant.ofEpochMilli(nowMs).atZone(TimeFormatters.safeZoneId(snapshot.timezone))
        val localMinutes = zoned.hour * 60.0 + zoned.minute + zoned.second / 60.0
        val primaryTithi = snapshot.tithi.firstOrNull()?.number ?: 1
        return SwaraYogaEngine.computeSwaraYoga(
            tithiNumber = primaryTithi,
            sunriseStr = snapshot.sunrise,
            sunsetStr = snapshot.sunset,
            currentLocalMinutes = localMinutes,
            moonriseStr = snapshot.moonrise,
            moonsetStr = snapshot.moonset,
            sunriseMs = snapshot.sunriseMs,
            sunsetMs = snapshot.sunsetMs,
            timeZoneId = snapshot.timezone,
            nowMs = nowMs,
            moonriseMs = snapshot.moonriseMs,
            moonsetMs = snapshot.moonsetMs
        )
    }

    override suspend fun clearExpiredCache() {
        memoryCache.clear()
        monthMemoryCache.clear()
        withContext(dispatchers.io) {
            cacheDao.clearAll()
        }
    }
}
