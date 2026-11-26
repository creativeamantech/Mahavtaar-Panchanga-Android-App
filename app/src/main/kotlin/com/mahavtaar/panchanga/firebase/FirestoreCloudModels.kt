package com.mahavtaar.panchanga.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String? = null,
    val language: String = "en",
    val themeMode: String = "PARCHMENT",
    val monthSystem: String = "amanta",
    val ayanamsha: String = "citra",
    val birthNakshatra: Int = 1,
    val use24HourFormat: Boolean = false,
    val notifySunrise: Boolean = false,
    val notifySunset: Boolean = false,
    val notifyTithiChange: Boolean = false,
    val notifyNakshatraChange: Boolean = false,
    val notifyVratReminders: Boolean = true,
    val selectedLocationName: String = "Bhopal, IN",
    val selectedLocationCountry: String = "IN",
    val selectedLocationLat: Double = 23.2599,
    val selectedLocationLon: Double = 77.4126,
    val selectedLocationTz: String = "Asia/Kolkata",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = buildMap<String, Any?> {
        put("userId", uid)
        put("displayName", displayName?.take(100))
        put("language", language)
        put("themeMode", themeMode)
        put("monthSystem", monthSystem)
        put("ayanamsha", ayanamsha)
        put("birthNakshatra", birthNakshatra.coerceIn(1, 27))
        put("use24HourFormat", use24HourFormat)
        put("notifySunrise", notifySunrise)
        put("notifySunset", notifySunset)
        put("notifyTithiChange", notifyTithiChange)
        put("notifyNakshatraChange", notifyNakshatraChange)
        put("notifyVratReminders", notifyVratReminders)
        put("selectedLocationName", selectedLocationName.ifBlank { "Bhopal, IN" }.take(120))
        put("selectedLocationCountry", selectedLocationCountry.take(80))
        put("selectedLocationLat", selectedLocationLat.coerceIn(-90.0, 90.0))
        put("selectedLocationLon", selectedLocationLon.coerceIn(-180.0, 180.0))
        put("selectedLocationTz", selectedLocationTz.ifBlank { "Asia/Kolkata" }.take(80))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null }.mapValues { it.value!! }

    fun toUpdateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("displayName", displayName?.take(100))
        put("language", language)
        put("themeMode", themeMode)
        put("monthSystem", monthSystem)
        put("ayanamsha", ayanamsha)
        put("birthNakshatra", birthNakshatra.coerceIn(1, 27))
        put("use24HourFormat", use24HourFormat)
        put("notifySunrise", notifySunrise)
        put("notifySunset", notifySunset)
        put("notifyTithiChange", notifyTithiChange)
        put("notifyNakshatraChange", notifyNakshatraChange)
        put("notifyVratReminders", notifyVratReminders)
        put("selectedLocationName", selectedLocationName.ifBlank { "Bhopal, IN" }.take(120))
        put("selectedLocationCountry", selectedLocationCountry.take(80))
        put("selectedLocationLat", selectedLocationLat.coerceIn(-90.0, 90.0))
        put("selectedLocationLon", selectedLocationLon.coerceIn(-180.0, 180.0))
        put("selectedLocationTz", selectedLocationTz.ifBlank { "Asia/Kolkata" }.take(80))
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class CloudSavedLocation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val country: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "Asia/Kolkata",
    val population: Long = 0L,
    val gpsResolved: Boolean = false,
    val customLocation: Boolean = false,
    val defaultLocation: Boolean = false,
    val accuracyMeters: Double? = null,
    val savedAtMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = buildMap<String, Any?> {
        put("userId", uid)
        put("name", name.ifBlank { "Location" }.take(120))
        put("country", country.take(80))
        put("latitude", latitude.coerceIn(-90.0, 90.0))
        put("longitude", longitude.coerceIn(-180.0, 180.0))
        put("timezone", timezone.ifBlank { "Asia/Kolkata" }.take(80))
        put("population", population.coerceAtLeast(0L))
        put("gpsResolved", gpsResolved)
        put("customLocation", customLocation)
        put("defaultLocation", defaultLocation)
        put("accuracyMeters", accuracyMeters?.coerceIn(0.0, 100000.0))
        put("savedAtMs", savedAtMs.coerceAtLeast(0L))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null }.mapValues { it.value!! }

    fun toUpdateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("name", name.ifBlank { "Location" }.take(120))
        put("country", country.take(80))
        put("latitude", latitude.coerceIn(-90.0, 90.0))
        put("longitude", longitude.coerceIn(-180.0, 180.0))
        put("timezone", timezone.ifBlank { "Asia/Kolkata" }.take(80))
        put("population", population.coerceAtLeast(0L))
        put("gpsResolved", gpsResolved)
        put("customLocation", customLocation)
        put("defaultLocation", defaultLocation)
        put("accuracyMeters", accuracyMeters?.coerceIn(0.0, 100000.0))
        put("savedAtMs", savedAtMs.coerceAtLeast(0L))
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class CloudNavtaraProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val birthNakshatraIndex: Int = 1,
    val birthPada: Int = 1,
    val notes: String = "",
    val defaultProfile: Boolean = false,
    val updatedAtMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.ifBlank { "Native" }.take(100),
        "birthNakshatraIndex" to birthNakshatraIndex.coerceIn(1, 27),
        "birthPada" to birthPada.coerceIn(1, 4),
        "notes" to notes.take(1000),
        "defaultProfile" to defaultProfile,
        "updatedAtMs" to updatedAtMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.ifBlank { "Native" }.take(100),
        "birthNakshatraIndex" to birthNakshatraIndex.coerceIn(1, 27),
        "birthPada" to birthPada.coerceIn(1, 4),
        "notes" to notes.take(1000),
        "defaultProfile" to defaultProfile,
        "updatedAtMs" to updatedAtMs.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CloudNavtaraFeedback(
    @DocumentId val id: String = "",
    val userId: String = "",
    val profileCloudId: String = "",
    val dateIso: String = "",
    val rating: String = "NEUTRAL",
    val note: String = "",
    val updatedAtMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "profileCloudId" to profileCloudId.ifBlank { "prof_unknown" }.take(128),
        "dateIso" to dateIso.take(10),
        "rating" to rating,
        "note" to note.take(2000),
        "updatedAtMs" to updatedAtMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "profileCloudId" to profileCloudId.ifBlank { "prof_unknown" }.take(128),
        "dateIso" to dateIso.take(10),
        "rating" to rating,
        "note" to note.take(2000),
        "updatedAtMs" to updatedAtMs.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CloudNavtaraReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val profileCloudId: String = "",
    val profileName: String = "",
    val birthNakshatraIndex: Int = 1,
    val birthPada: Int = 1,
    val startDateIso: String = "",
    val endDateIso: String = "",
    val dateRange: Boolean = false,
    val locationName: String = "Bhopal, IN",
    val latitude: Double = 23.2599,
    val longitude: Double = 77.4126,
    val timezone: String = "Asia/Kolkata",
    val ayanamshaKey: String = "citra",
    val monthSystemKey: String = "amanta",
    val auspiciousCount: Int = 0,
    val mixedCount: Int = 0,
    val cautionCount: Int = 0,
    val neutralCount: Int = 0,
    val notes: String = "",
    val createdAtMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "profileCloudId" to profileCloudId.ifBlank { "local_0" }.take(128),
        "profileName" to profileName.ifBlank { "Native" }.take(100),
        "birthNakshatraIndex" to birthNakshatraIndex.coerceIn(1, 27),
        "birthPada" to birthPada.coerceIn(1, 4),
        "startDateIso" to startDateIso.take(10),
        "endDateIso" to endDateIso.take(10),
        "dateRange" to dateRange,
        "locationName" to locationName.ifBlank { "Bhopal, IN" }.take(120),
        "latitude" to latitude.coerceIn(-90.0, 90.0),
        "longitude" to longitude.coerceIn(-180.0, 180.0),
        "timezone" to timezone.ifBlank { "Asia/Kolkata" }.take(80),
        "ayanamshaKey" to ayanamshaKey,
        "monthSystemKey" to monthSystemKey,
        "auspiciousCount" to auspiciousCount.coerceIn(0, 366),
        "mixedCount" to mixedCount.coerceIn(0, 366),
        "cautionCount" to cautionCount.coerceIn(0, 366),
        "neutralCount" to neutralCount.coerceIn(0, 366),
        "notes" to notes.take(2000),
        "createdAtMs" to createdAtMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CloudKundliChart(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val birthYear: Int = 2000,
    val birthMonth: Int = 1,
    val birthDay: Int = 1,
    val birthHour: Int = 12,
    val birthMinute: Int = 0,
    val placeName: String = "Bhopal, IN",
    val latitude: Double = 23.2599,
    val longitude: Double = 77.4126,
    val timezone: String = "Asia/Kolkata",
    val inputJson: String = "{}",
    val lagnaSignIndex: Int = 0,
    val moonSignIndex: Int = 0,
    val moonNakshatraNumber: Int = 1,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.ifBlank { "Native" }.take(100),
        "birthYear" to birthYear.coerceIn(1800, 2200),
        "birthMonth" to birthMonth.coerceIn(1, 12),
        "birthDay" to birthDay.coerceIn(1, 31),
        "birthHour" to birthHour.coerceIn(0, 23),
        "birthMinute" to birthMinute.coerceIn(0, 59),
        "placeName" to placeName.ifBlank { "Bhopal, IN" }.take(120),
        "latitude" to latitude.coerceIn(-90.0, 90.0),
        "longitude" to longitude.coerceIn(-180.0, 180.0),
        "timezone" to timezone.ifBlank { "Asia/Kolkata" }.take(80),
        "inputJson" to inputJson.ifBlank { "{}" }.take(4000),
        "lagnaSignIndex" to lagnaSignIndex.coerceIn(0, 11),
        "moonSignIndex" to moonSignIndex.coerceIn(0, 11),
        "moonNakshatraNumber" to moonNakshatraNumber.coerceIn(1, 27),
        "createdAtMs" to createdAtMs.coerceAtLeast(0L),
        "updatedAtMs" to updatedAtMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.ifBlank { "Native" }.take(100),
        "birthYear" to birthYear.coerceIn(1800, 2200),
        "birthMonth" to birthMonth.coerceIn(1, 12),
        "birthDay" to birthDay.coerceIn(1, 31),
        "birthHour" to birthHour.coerceIn(0, 23),
        "birthMinute" to birthMinute.coerceIn(0, 59),
        "placeName" to placeName.ifBlank { "Bhopal, IN" }.take(120),
        "latitude" to latitude.coerceIn(-90.0, 90.0),
        "longitude" to longitude.coerceIn(-180.0, 180.0),
        "timezone" to timezone.ifBlank { "Asia/Kolkata" }.take(80),
        "inputJson" to inputJson.ifBlank { "{}" }.take(4000),
        "lagnaSignIndex" to lagnaSignIndex.coerceIn(0, 11),
        "moonSignIndex" to moonSignIndex.coerceIn(0, 11),
        "moonNakshatraNumber" to moonNakshatraNumber.coerceIn(1, 27),
        "updatedAtMs" to updatedAtMs.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CloudUserAccess(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String? = null,
    val role: String = "USER",
    val status: String = "PENDING",
    val allowedPages: List<String> = emptyList(),
    val allowedFeatures: List<String> = emptyList(),
    val authorizedByUid: String? = null,
    val claimedInviteId: String? = null,
    val updatedAtMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("userId", userId)
        put("email", email.trim().take(200))
        put("displayName", displayName?.take(100))
        put("role", role)
        put("status", status)
        put("allowedPages", allowedPages)
        put("allowedFeatures", allowedFeatures)
        put("authorizedByUid", authorizedByUid?.take(128))
        put("claimedInviteId", claimedInviteId?.take(128))
        put("updatedAtMs", updatedAtMs.coerceAtLeast(0L))
        put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null }.mapValues { it.value!! }

    fun toAdminUpdateMap(): Map<String, Any> = buildMap<String, Any?> {
        put("email", email.trim().take(200))
        put("displayName", displayName?.take(100))
        put("role", role)
        put("status", status)
        put("allowedPages", allowedPages)
        put("allowedFeatures", allowedFeatures)
        put("authorizedByUid", authorizedByUid?.take(128))
        put("updatedAtMs", updatedAtMs.coerceAtLeast(0L))
        put("updatedAt", FieldValue.serverTimestamp())
    }.filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class CloudEmailInvite(
    @DocumentId val id: String = "",
    val emailLower: String = "",
    val role: String = "USER",
    val status: String = "ACTIVE",
    val allowedPages: List<String> = emptyList(),
    val allowedFeatures: List<String> = emptyList(),
    val createdByUid: String = "",
    val updatedAtMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "emailLower" to emailLower.trim().lowercase().take(200),
        "role" to role,
        "status" to status,
        "allowedPages" to allowedPages,
        "allowedFeatures" to allowedFeatures,
        "createdByUid" to createdByUid.take(128),
        "updatedAtMs" to updatedAtMs.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "emailLower" to emailLower.trim().lowercase().take(200),
        "role" to role,
        "status" to status,
        "allowedPages" to allowedPages,
        "allowedFeatures" to allowedFeatures,
        "createdByUid" to createdByUid.take(128),
        "updatedAtMs" to updatedAtMs.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class CloudUserSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val activeSessionToken: String = "",
    val deviceName: String = "Android Device",
    val forcedLogout: Boolean = false,
    val claimedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toClaimMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "activeSessionToken" to activeSessionToken.take(128),
        "deviceName" to deviceName.ifBlank { "Android Device" }.take(120),
        "forcedLogout" to false,
        "claimedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
