package com.mahavtaar.panchanga.data.repository

import com.mahavtaar.panchanga.core.AppDispatchers
import com.mahavtaar.panchanga.data.city.OfflineCityCatalog
import com.mahavtaar.panchanga.database.dao.LocationDao
import com.mahavtaar.panchanga.database.entity.SavedLocationEntity
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    private val cityCatalog: OfflineCityCatalog,
    private val dispatchers: AppDispatchers
) : LocationRepository {

    override fun observeSavedLocations(): Flow<List<CityLocation>> =
        locationDao.observeSavedLocations().map { list ->
            list.map { e ->
                CityLocation(
                    name = e.name,
                    country = e.country,
                    latitude = e.latitude,
                    longitude = e.longitude,
                    timezone = e.timezone,
                    population = e.population,
                    isGpsResolved = e.isGpsResolved,
                    isCustom = e.isCustom,
                    isDefault = e.isDefault,
                    accuracyMeters = e.accuracyMeters
                )
            }
        }

    override suspend fun saveLocation(location: CityLocation) = withContext(dispatchers.io) {
        locationDao.upsertLocation(
            SavedLocationEntity(
                name = location.name,
                country = location.country,
                latitude = location.latitude,
                longitude = location.longitude,
                timezone = location.timezone,
                population = location.population,
                isGpsResolved = location.isGpsResolved,
                savedAtMs = System.currentTimeMillis(),
                isCustom = location.isCustom,
                isDefault = location.isDefault,
                accuracyMeters = location.accuracyMeters
            )
        )
    }

    override suspend fun setDefaultLocation(name: String) = withContext(dispatchers.io) {
        locationDao.setDefaultLocation(name)
    }

    override suspend fun removeLocation(name: String) = withContext(dispatchers.io) {
        locationDao.deleteByName(name)
    }

    override suspend fun searchCities(query: String, limit: Int): List<CityLocation> =
        cityCatalog.search(query, limit)

    override suspend fun getPopularCities(): List<CityLocation> = cityCatalog.popularCities

    override suspend fun resolveNearestCity(latitude: Double, longitude: Double): CityLocation =
        cityCatalog.resolveNearestCity(latitude, longitude)
}
