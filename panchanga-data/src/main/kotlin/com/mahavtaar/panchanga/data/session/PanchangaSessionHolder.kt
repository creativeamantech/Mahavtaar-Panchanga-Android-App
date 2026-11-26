package com.mahavtaar.panchanga.data.session

import com.mahavtaar.panchanga.core.TimeFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared singleton session holder synchronizing the selected calendar date across all feature tabs.
 */
@Singleton
class PanchangaSessionHolder @Inject constructor() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun selectToday(timeZoneId: String) {
        val zone = TimeFormatters.safeZoneId(timeZoneId)
        _selectedDate.value = Instant.now().atZone(zone).toLocalDate()
    }

    fun shiftDays(deltaDays: Long) {
        _selectedDate.value = _selectedDate.value.plusDays(deltaDays)
    }
}
