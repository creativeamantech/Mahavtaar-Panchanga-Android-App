package com.mahavtaar.panchanga.domain.usecase

import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.DailyHoras
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.MonthlyCalendarDay
import com.mahavtaar.panchanga.domain.model.PanchangaSnapshot
import com.mahavtaar.panchanga.domain.model.SwaraYogaState
import com.mahavtaar.panchanga.domain.repository.LocationRepository
import com.mahavtaar.panchanga.domain.repository.PanchangaRepository
import java.time.LocalDate

class GetDailyPanchangaUseCase(
    private val repository: PanchangaRepository
) {
    suspend operator fun invoke(
        location: CityLocation,
        date: LocalDate,
        monthSystem: MonthSystem,
        ayanamsha: CoordinateSelection,
        includeExtendedTransits: Boolean = true
    ): PanchangaSnapshot = repository.getDailyPanchanga(
        location = location,
        date = date,
        monthSystem = monthSystem,
        ayanamsha = ayanamsha,
        includeExtendedTransits = includeExtendedTransits
    )
}

class GetMonthlyCalendarUseCase(
    private val repository: PanchangaRepository
) {
    suspend operator fun invoke(
        location: CityLocation,
        year: Int,
        month: Int,
        monthSystem: MonthSystem,
        ayanamsha: CoordinateSelection
    ): List<MonthlyCalendarDay> = repository.getMonthlyCalendar(
        location = location,
        year = year,
        month = month,
        monthSystem = monthSystem,
        ayanamsha = ayanamsha
    )
}

class GetLiveHoraUseCase(
    private val repository: PanchangaRepository
) {
    operator fun invoke(snapshot: PanchangaSnapshot, nowMs: Long): DailyHoras =
        repository.computeDailyHoras(snapshot, nowMs)
}

class GetLiveSwaraYogaUseCase(
    private val repository: PanchangaRepository
) {
    operator fun invoke(snapshot: PanchangaSnapshot, nowMs: Long): SwaraYogaState =
        repository.computeLiveSwaraYoga(snapshot, nowMs)
}

class SearchCitiesUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 30): List<CityLocation> =
        if (query.isBlank()) locationRepository.getPopularCities()
        else locationRepository.searchCities(query, limit)
}
