package com.mahavtaar.panchanga.data.city

import android.content.Context
import com.mahavtaar.panchanga.core.AppDispatchers
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.location.HaversineCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class RawCityDto(
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
    val population: Long = 0L,
    val timezone: String
)

/**
 * Offline global city database loaded lazily from `assets/cities.json` with
 * Hindi/Devanagari aliases, popular Tirtha cities, and Haversine nearest-city resolution.
 */
@Singleton
class OfflineCityCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    @Volatile
    private var cachedCities: List<CityLocation>? = null

    private val aliasMap = mapOf(
        "भोपाल" to "bhopal",
        "उज्जैन" to "ujjain",
        "इंदौर" to "indore",
        "वाराणसी" to "varanasi",
        "काशी" to "varanasi",
        "बनारस" to "varanasi",
        "kashi" to "varanasi",
        "banaras" to "varanasi",
        "दिल्ली" to "delhi",
        "नई दिल्ली" to "new delhi",
        "मुंबई" to "mumbai",
        "बंबई" to "mumbai",
        "bombay" to "mumbai",
        "बेंगलुरु" to "bengaluru",
        "bangalore" to "bengaluru",
        "चेन्नई" to "chennai",
        "madras" to "chennai",
        "कोलकाता" to "kolkata",
        "calcutta" to "kolkata",
        "अयोध्या" to "ayodhya",
        "प्रयागराज" to "prayagraj",
        "allahabad" to "prayagraj",
        "मथुरा" to "mathura",
        "हरिद्वार" to "haridwar",
        "ऋषिकेश" to "rishikesh",
        "जयपुर" to "jaipur",
        "अहमदाबाद" to "ahmedabad",
        "पुणे" to "pune",
        "हैदराबाद" to "hyderabad",
        "तिरुपति" to "tirupati",
        "द्वारका" to "dwarka",
        "पुरी" to "puri"
    )

    val popularCities: List<CityLocation> = listOf(
        CityLocation("Bhopal, IN", "IN", 23.2599, 77.4126, "Asia/Kolkata", 1798218),
        CityLocation("Ujjain, IN", "IN", 23.1765, 75.7885, "Asia/Kolkata", 515215),
        CityLocation("Varanasi, IN", "IN", 25.3176, 82.9739, "Asia/Kolkata", 1198491),
        CityLocation("Indore, IN", "IN", 22.7196, 75.8577, "Asia/Kolkata", 1994397),
        CityLocation("New Delhi, IN", "IN", 28.6139, 77.2090, "Asia/Kolkata", 16787941),
        CityLocation("Mumbai, IN", "IN", 19.0760, 72.8777, "Asia/Kolkata", 12442373),
        CityLocation("Bengaluru, IN", "IN", 12.9716, 77.5946, "Asia/Kolkata", 8443675),
        CityLocation("Chennai, IN", "IN", 13.0827, 80.2707, "Asia/Kolkata", 4646732),
        CityLocation("Ayodhya, IN", "IN", 26.7922, 82.1998, "Asia/Kolkata", 256000),
        CityLocation("Haridwar, IN", "IN", 29.9457, 78.1642, "Asia/Kolkata", 228832),
        CityLocation("London, GB", "GB", 51.5074, -0.1278, "Europe/London", 8982000),
        CityLocation("New York, US", "US", 40.7128, -74.0060, "America/New_York", 8336817),
        CityLocation("Singapore, SG", "SG", 1.3521, 103.8198, "Asia/Singapore", 5686000),
        CityLocation("Sydney, AU", "AU", -33.8688, 151.2093, "Australia/Sydney", 5312000)
    )

    private suspend fun loadAllCities(): List<CityLocation> {
        cachedCities?.let { return it }
        return mutex.withLock {
            cachedCities?.let { return it }
            withContext(dispatchers.io) {
                val parsed = runCatching {
                    context.assets.open("cities.json").bufferedReader().use { reader ->
                        val map = json.decodeFromString<Map<String, RawCityDto>>(reader.readText())
                        map.entries.map { (key, dto) ->
                            CityLocation(
                                name = key,
                                country = dto.country,
                                latitude = dto.latitude,
                                longitude = dto.longitude,
                                timezone = dto.timezone,
                                population = dto.population
                            )
                        }.sortedByDescending { it.population }
                    }
                }.getOrElse { popularCities }
                cachedCities = parsed
                parsed
            }
        }
    }

    suspend fun search(query: String, limit: Int = 30): List<CityLocation> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return popularCities
        val normalized = aliasMap[trimmed.lowercase()] ?: aliasMap[trimmed] ?: trimmed.lowercase()
        val all = loadAllCities()

        return withContext(dispatchers.default) {
            val startsWith = mutableListOf<CityLocation>()
            val contains = mutableListOf<CityLocation>()
            for (city in all) {
                val lower = city.name.lowercase()
                if (lower.startsWith(normalized)) {
                    startsWith.add(city)
                } else if (lower.contains(normalized)) {
                    contains.add(city)
                }
                if (startsWith.size >= limit) break
            }
            (startsWith + contains).take(limit)
        }
    }

    suspend fun resolveNearestCity(latitude: Double, longitude: Double): CityLocation {
        val all = loadAllCities()
        return withContext(dispatchers.default) {
            var nearest = popularCities.first()
            var bestDist = Double.MAX_VALUE
            for (city in all) {
                val d = HaversineCalculator.distanceKm(latitude, longitude, city.latitude, city.longitude)
                if (d < bestDist) {
                    bestDist = d
                    nearest = city
                }
            }
            val resolvedTz = if (bestDist > 250.0) {
                java.time.ZoneId.systemDefault().id
            } else {
                nearest.timezone
            }
            val resolvedName = if (bestDist > 250.0) {
                "GPS (%.4f, %.4f)".format(java.util.Locale.US, latitude, longitude)
            } else {
                nearest.name
            }
            nearest.copy(
                name = resolvedName,
                latitude = latitude,
                longitude = longitude,
                timezone = resolvedTz,
                isGpsResolved = true
            )
        }
    }
}
