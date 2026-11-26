package com.mahavtaar.panchanga.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mahavtaar.panchanga.domain.model.AppLanguage
import com.mahavtaar.panchanga.domain.model.AppThemeMode
import com.mahavtaar.panchanga.domain.model.CityLocation
import com.mahavtaar.panchanga.domain.model.CoordinateSelection
import com.mahavtaar.panchanga.domain.model.MonthSystem
import com.mahavtaar.panchanga.domain.model.UserPreferences
import com.mahavtaar.panchanga.domain.model.WidgetGlassStyle
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.panchangaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mahavtaar_panchanga_prefs"
)

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object Keys {
        val CITY_NAME = stringPreferencesKey("city_name")
        val CITY_COUNTRY = stringPreferencesKey("city_country")
        val CITY_LAT = doublePreferencesKey("city_lat")
        val CITY_LON = doublePreferencesKey("city_lon")
        val CITY_TZ = stringPreferencesKey("city_tz")
        val CITY_POP = longPreferencesKey("city_pop")
        val CITY_GPS = booleanPreferencesKey("city_gps")
        val CITY_CUSTOM = booleanPreferencesKey("city_custom")
        val CITY_DEFAULT = booleanPreferencesKey("city_default")
        val CITY_ACCURACY = doublePreferencesKey("city_accuracy")

        val MONTH_SYSTEM = stringPreferencesKey("month_system")
        val AYANAMSHA = stringPreferencesKey("ayanamsha")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
        val BIRTH_NAKSHATRA = intPreferencesKey("birth_nakshatra")
        val USE_24H = booleanPreferencesKey("use_24h")

        val NOTIFY_SUNRISE = booleanPreferencesKey("notify_sunrise")
        val NOTIFY_SUNSET = booleanPreferencesKey("notify_sunset")
        val NOTIFY_TITHI = booleanPreferencesKey("notify_tithi")
        val NOTIFY_NAKSHATRA = booleanPreferencesKey("notify_nakshatra")
        val NOTIFY_VRAT = booleanPreferencesKey("notify_vrat")

        val TATTVA_MINIMAL_MODE = booleanPreferencesKey("tattva_minimal_mode")
        val TATTVA_HIDDEN_FEATURES = stringSetPreferencesKey("tattva_hidden_features")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PINNED_HOME_MODULES = stringPreferencesKey("pinned_home_modules")
        val WIDGET_GLASS_STYLE = stringPreferencesKey("widget_glass_style")
        val WIDGET_OPACITY = floatPreferencesKey("widget_opacity")
    }

    private val defaultPrefs = UserPreferences()

    override val preferences: Flow<UserPreferences> = context.panchangaDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { p ->
        val defaultLoc = defaultPrefs.selectedLocation
        val location = CityLocation(
            name = p[Keys.CITY_NAME] ?: defaultLoc.name,
            country = p[Keys.CITY_COUNTRY] ?: defaultLoc.country,
            latitude = p[Keys.CITY_LAT] ?: defaultLoc.latitude,
            longitude = p[Keys.CITY_LON] ?: defaultLoc.longitude,
            timezone = p[Keys.CITY_TZ] ?: defaultLoc.timezone,
            population = p[Keys.CITY_POP] ?: defaultLoc.population,
            isGpsResolved = p[Keys.CITY_GPS] ?: false,
            isCustom = p[Keys.CITY_CUSTOM] ?: false,
            isDefault = p[Keys.CITY_DEFAULT] ?: false,
            accuracyMeters = p[Keys.CITY_ACCURACY]
        )
        val themeMode = p[Keys.THEME_MODE]?.let { raw ->
            runCatching { AppThemeMode.valueOf(raw) }.getOrNull()
        } ?: AppThemeMode.PARCHMENT

        val pinnedRaw = p[Keys.PINNED_HOME_MODULES]
        val pinnedModules = if (pinnedRaw.isNullOrBlank()) {
            UserPreferences.DEFAULT_PINNED_HOME_MODULES
        } else {
            pinnedRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(4)
                .ifEmpty { UserPreferences.DEFAULT_PINNED_HOME_MODULES }
        }
        // New installs (completely empty DataStore) trigger first-launch Quick Setup;
        // existing users with saved preferences are automatically considered onboarded.
        val onboardingDone = p[Keys.ONBOARDING_COMPLETED] ?: p.asMap().isNotEmpty()

        val widgetGlassStyle = p[Keys.WIDGET_GLASS_STYLE]?.let { raw ->
            runCatching { WidgetGlassStyle.valueOf(raw) }.getOrNull()
        } ?: WidgetGlassStyle.AUTO
        val widgetOpacity = (p[Keys.WIDGET_OPACITY] ?: UserPreferences.DEFAULT_WIDGET_OPACITY)
            .coerceIn(UserPreferences.MIN_WIDGET_OPACITY, UserPreferences.MAX_WIDGET_OPACITY)

        UserPreferences(
            selectedLocation = location,
            monthSystem = MonthSystem.fromKey(p[Keys.MONTH_SYSTEM]),
            ayanamsha = CoordinateSelection.fromKey(p[Keys.AYANAMSHA]),
            themeMode = themeMode,
            notifySunrise = p[Keys.NOTIFY_SUNRISE] ?: false,
            notifySunset = p[Keys.NOTIFY_SUNSET] ?: false,
            notifyTithiChange = p[Keys.NOTIFY_TITHI] ?: false,
            notifyNakshatraChange = p[Keys.NOTIFY_NAKSHATRA] ?: false,
            notifyVratReminders = p[Keys.NOTIFY_VRAT] ?: true,
            use24HourFormat = p[Keys.USE_24H] ?: false,
            language = AppLanguage.fromCode(p[Keys.LANGUAGE]),
            birthNakshatra = (p[Keys.BIRTH_NAKSHATRA] ?: 1).coerceIn(1, 27),
            tattvaMinimalMode = p[Keys.TATTVA_MINIMAL_MODE] ?: false,
            tattvaHiddenFeatures = p[Keys.TATTVA_HIDDEN_FEATURES].orEmpty(),
            hasCompletedOnboarding = onboardingDone,
            pinnedHomeModules = pinnedModules,
            widgetGlassStyle = widgetGlassStyle,
            widgetOpacity = widgetOpacity
        )
    }

    override suspend fun setSelectedLocation(location: CityLocation) {
        context.panchangaDataStore.edit { p ->
            p[Keys.CITY_NAME] = location.name
            p[Keys.CITY_COUNTRY] = location.country
            p[Keys.CITY_LAT] = location.latitude
            p[Keys.CITY_LON] = location.longitude
            p[Keys.CITY_TZ] = location.timezone
            p[Keys.CITY_POP] = location.population
            p[Keys.CITY_GPS] = location.isGpsResolved
            p[Keys.CITY_CUSTOM] = location.isCustom
            p[Keys.CITY_DEFAULT] = location.isDefault
            val acc = location.accuracyMeters
            if (acc != null) {
                p[Keys.CITY_ACCURACY] = acc
            } else {
                p.remove(Keys.CITY_ACCURACY)
            }
        }
    }

    override suspend fun setMonthSystem(monthSystem: MonthSystem) {
        context.panchangaDataStore.edit { p ->
            p[Keys.MONTH_SYSTEM] = monthSystem.apiKey
        }
    }

    override suspend fun setAyanamsha(ayanamsha: CoordinateSelection) {
        context.panchangaDataStore.edit { p ->
            p[Keys.AYANAMSHA] = ayanamsha.apiKey
        }
    }

    override suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.panchangaDataStore.edit { p ->
            p[Keys.THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        context.panchangaDataStore.edit { p ->
            p[Keys.LANGUAGE] = language.code
        }
    }

    override suspend fun setBirthNakshatra(nakshatra: Int) {
        context.panchangaDataStore.edit { p ->
            p[Keys.BIRTH_NAKSHATRA] = nakshatra.coerceIn(1, 27)
        }
    }

    override suspend fun setUse24HourFormat(use24Hour: Boolean) {
        context.panchangaDataStore.edit { p ->
            p[Keys.USE_24H] = use24Hour
        }
    }

    override suspend fun setTattvaMinimalMode(enabled: Boolean) {
        context.panchangaDataStore.edit { p ->
            p[Keys.TATTVA_MINIMAL_MODE] = enabled
        }
    }

    override suspend fun setTattvaHiddenFeatures(hiddenFeatures: Set<String>) {
        context.panchangaDataStore.edit { p ->
            p[Keys.TATTVA_HIDDEN_FEATURES] = hiddenFeatures
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.panchangaDataStore.edit { p ->
            p[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setPinnedHomeModules(moduleKeys: List<String>) {
        val cleaned = moduleKeys.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(4)
            .ifEmpty { UserPreferences.DEFAULT_PINNED_HOME_MODULES }
        context.panchangaDataStore.edit { p ->
            p[Keys.PINNED_HOME_MODULES] = cleaned.joinToString(",")
        }
    }

    override suspend fun setWidgetGlassStyle(style: WidgetGlassStyle) {
        context.panchangaDataStore.edit { p ->
            p[Keys.WIDGET_GLASS_STYLE] = style.name
        }
    }

    override suspend fun setWidgetOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(UserPreferences.MIN_WIDGET_OPACITY, UserPreferences.MAX_WIDGET_OPACITY)
        context.panchangaDataStore.edit { p ->
            p[Keys.WIDGET_OPACITY] = clamped
        }
    }

    override suspend fun updateNotificationSettings(
        notifySunrise: Boolean?,
        notifySunset: Boolean?,
        notifyTithiChange: Boolean?,
        notifyNakshatraChange: Boolean?,
        notifyVratReminders: Boolean?
    ) {
        context.panchangaDataStore.edit { p ->
            notifySunrise?.let { p[Keys.NOTIFY_SUNRISE] = it }
            notifySunset?.let { p[Keys.NOTIFY_SUNSET] = it }
            notifyTithiChange?.let { p[Keys.NOTIFY_TITHI] = it }
            notifyNakshatraChange?.let { p[Keys.NOTIFY_NAKSHATRA] = it }
            notifyVratReminders?.let { p[Keys.NOTIFY_VRAT] = it }
        }
    }

    override suspend fun resetDefaults() {
        context.panchangaDataStore.edit { it.clear() }
    }

    private val kundliKey = stringPreferencesKey("saved_kundli_profiles_json")
    private val jsonCodec = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    override fun observeSavedKundliProfiles(): Flow<List<com.mahavtaar.panchanga.domain.model.KundliBirthInput>> =
        context.panchangaDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { p ->
            val raw = p[kundliKey] ?: return@map emptyList()
            runCatching {
                jsonCodec.decodeFromString<List<com.mahavtaar.panchanga.domain.model.KundliBirthInput>>(raw)
            }.getOrDefault(emptyList())
        }

    override suspend fun saveKundliProfile(profile: com.mahavtaar.panchanga.domain.model.KundliBirthInput) {
        val trimmedName = profile.name.trim().ifBlank { "Native" }
        val normalized = profile.copy(name = trimmedName)
        context.panchangaDataStore.edit { p ->
            val current = p[kundliKey]?.let {
                runCatching { jsonCodec.decodeFromString<List<com.mahavtaar.panchanga.domain.model.KundliBirthInput>>(it) }.getOrNull()
            }.orEmpty()
            val updated = (listOf(normalized) + current.filterNot { it.name.equals(trimmedName, ignoreCase = true) }).take(20)
            p[kundliKey] = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(com.mahavtaar.panchanga.domain.model.KundliBirthInput.serializer()),
                updated
            )
        }
    }

    override suspend fun deleteKundliProfile(profileName: String) {
        context.panchangaDataStore.edit { p ->
            val current = p[kundliKey]?.let {
                runCatching { jsonCodec.decodeFromString<List<com.mahavtaar.panchanga.domain.model.KundliBirthInput>>(it) }.getOrNull()
            }.orEmpty()
            val updated = current.filterNot { it.name.equals(profileName, ignoreCase = true) }
            p[kundliKey] = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(com.mahavtaar.panchanga.domain.model.KundliBirthInput.serializer()),
                updated
            )
        }
    }
}
