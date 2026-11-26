package com.mahavtaar.panchanga.domain.model

import kotlinx.serialization.Serializable

const val ROOT_ADMIN_EMAIL = "jain9640@gmail.com"

@Serializable
enum class UserRole {
    ADMIN,
    USER
}

@Serializable
enum class AccessStatus {
    PENDING,
    ACTIVE,
    SUSPENDED
}

@Serializable
enum class SwaraTier {
    SADHAKA,
    YOGI,
    TAPASVI
}

@Serializable
enum class PermissionCategory {
    CORE_EVENTS,
    TIMINGS_TRANSITS,
    ASTROLOGY_NAVTATA_KUNDLI,
    SYSTEM_EXPORTS
}

@Serializable
enum class AppPageKey(
    val category: PermissionCategory,
    val routeKey: String
) {
    EVENTS_TIMELINE(PermissionCategory.CORE_EVENTS, "events_timeline"),
    HOME(PermissionCategory.CORE_EVENTS, "home"),
    DAILY_PANCHANGA(PermissionCategory.CORE_EVENTS, "daily_panchanga"),
    CALENDAR(PermissionCategory.CORE_EVENTS, "calendar"),
    ANNUAL_FESTIVALS(PermissionCategory.CORE_EVENTS, "annual_festivals"),

    HORA(PermissionCategory.TIMINGS_TRANSITS, "hora"),
    TODAY_SCHEDULE(PermissionCategory.TIMINGS_TRANSITS, "today_schedule"),
    PLANETARY_POSITIONS(PermissionCategory.TIMINGS_TRANSITS, "planetary_positions"),

    NAVTATA(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, "navtara"),
    KUNDLI(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, "kundli"),
    GUNA_MILAN(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, "guna_milan"),
    SWARA_YOGA(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, "swara_yoga"),
    TATTVA(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, "tattva"),
    NAVAGRAHA_MANTRAS(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, "navagraha_mantras"),

    LOCATIONS(PermissionCategory.SYSTEM_EXPORTS, "locations"),
    SETTINGS(PermissionCategory.SYSTEM_EXPORTS, "settings"),
    ABOUT(PermissionCategory.SYSTEM_EXPORTS, "about");

    companion object {
        fun fromNameOrNull(raw: String): AppPageKey? =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
    }
}

@Serializable
enum class AppFeatureKey(
    val category: PermissionCategory,
    val parentPage: AppPageKey? = null
) {
    EVENT_DESCRIPTIONS(PermissionCategory.CORE_EVENTS, AppPageKey.EVENTS_TIMELINE),
    GLOSSARY_DICTIONARY(PermissionCategory.CORE_EVENTS, null),

    NOTIFICATIONS_STUDIO(PermissionCategory.TIMINGS_TRANSITS, AppPageKey.TODAY_SCHEDULE),
    TATTVA_NADI_DETAILS(PermissionCategory.TIMINGS_TRANSITS, AppPageKey.HORA),

    NAVTATA_JOURNAL_SAVE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.NAVTATA),
    KUNDLI_ADVANCED_TABS(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.KUNDLI),
    KUNDLI_SAVE_IMPORT(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.KUNDLI),

    TATTVA_HERO_GUIDANCE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_TEST_CHIME(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_NADI_POLARITY(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_FOUR_PILLAR_GRID(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_HARMONY_REPORT(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_HORA_STRIP(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_VARA_SUMMARY(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_VARA_MASTER_TABLE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_TITHI_SUMMARY(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_TITHI_DAY_TABLE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_TITHI_MASTER_TABLE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_KARANA_DAY_TABLE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_KARANA_MASTER_TABLE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_MACRO_KARANA_CYCLE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_HORA_EXPLORER_TABLE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),
    TATTVA_MAHABHUTA_REFERENCE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.TATTVA),

    SWARA_TIER_SADHAK(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.SWARA_YOGA),
    SWARA_TIER_YOGI(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.SWARA_YOGA),
    SWARA_TIER_TAPASVI(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.SWARA_YOGA),
    SWARA_GUIDANCE_WORKS(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.SWARA_YOGA),
    SWARA_TITHI_RULES_TABLE(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.SWARA_YOGA),
    SWARA_TATTVA_EXPLORER(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.SWARA_YOGA),
    SWARA_NAKSHATRA_ALIGNMENT(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.SWARA_YOGA),
    SWARA_MASTER_TABLES(PermissionCategory.ASTROLOGY_NAVTATA_KUNDLI, AppPageKey.SWARA_YOGA),

    HTML_EXPORT_PRINT(PermissionCategory.SYSTEM_EXPORTS, null),
    CUSTOM_COORDINATES(PermissionCategory.SYSTEM_EXPORTS, AppPageKey.LOCATIONS),
    CALCULATION_SETTINGS(PermissionCategory.SYSTEM_EXPORTS, AppPageKey.SETTINGS);

    companion object {
        val tattvaFeatures: List<AppFeatureKey>
            get() = entries.filter { it.parentPage == AppPageKey.TATTVA }

        val swaraTierFeatures: List<AppFeatureKey>
            get() = listOf(SWARA_TIER_SADHAK, SWARA_TIER_YOGI, SWARA_TIER_TAPASVI)

        val swaraFeatures: List<AppFeatureKey>
            get() = entries.filter { it.parentPage == AppPageKey.SWARA_YOGA }

        fun featureForSwaraTier(tier: SwaraTier): AppFeatureKey = when (tier) {
            SwaraTier.SADHAKA -> SWARA_TIER_SADHAK
            SwaraTier.YOGI -> SWARA_TIER_YOGI
            SwaraTier.TAPASVI -> SWARA_TIER_TAPASVI
        }

        fun fromNameOrNull(raw: String): AppFeatureKey? =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
    }
}

@Serializable
data class AccessPolicy(
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val role: UserRole = UserRole.USER,
    val status: AccessStatus = AccessStatus.PENDING,
    val allowedPages: Set<AppPageKey> = emptySet(),
    val allowedFeatures: Set<AppFeatureKey> = emptySet(),
    val authorizedByUid: String? = null,
    val updatedAtMs: Long = 0L
) {
    val isAdmin: Boolean
        get() = role == UserRole.ADMIN && status == AccessStatus.ACTIVE

    fun canAccessPage(key: AppPageKey): Boolean =
        isAdmin || (status == AccessStatus.ACTIVE && key in allowedPages)

    fun hasFeature(key: AppFeatureKey): Boolean =
        isAdmin || (status == AccessStatus.ACTIVE && key in allowedFeatures)

    fun defaultLandingPage(): AppPageKey {
        if (isAdmin) return AppPageKey.HOME
        if (AppPageKey.EVENTS_TIMELINE in allowedPages) return AppPageKey.EVENTS_TIMELINE
        if (AppPageKey.HOME in allowedPages) return AppPageKey.HOME
        if (AppPageKey.DAILY_PANCHANGA in allowedPages) return AppPageKey.DAILY_PANCHANGA
        if (AppPageKey.CALENDAR in allowedPages) return AppPageKey.CALENDAR
        if (AppPageKey.HORA in allowedPages) return AppPageKey.HORA
        return allowedPages.firstOrNull() ?: AppPageKey.EVENTS_TIMELINE
    }

    companion object {
        val ADMIN_FULL = AccessPolicy(
            userId = "admin",
            email = ROOT_ADMIN_EMAIL,
            displayName = "Admin",
            role = UserRole.ADMIN,
            status = AccessStatus.ACTIVE,
            allowedPages = AppPageKey.entries.toSet(),
            allowedFeatures = AppFeatureKey.entries.toSet()
        )

        val MINIMAL_EVENTS = AccessPolicy(
            userId = "guest",
            email = "",
            displayName = null,
            role = UserRole.USER,
            status = AccessStatus.ACTIVE,
            allowedPages = setOf(AppPageKey.EVENTS_TIMELINE),
            allowedFeatures = emptySet()
        )
    }
}

@Serializable
data class UserSessionLock(
    val userId: String,
    val activeSessionToken: String,
    val deviceName: String,
    val forcedLogout: Boolean = false,
    val claimedAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
)

@Serializable
data class EmailInviteRule(
    val emailHash: String,
    val email: String,
    val role: UserRole = UserRole.USER,
    val status: AccessStatus = AccessStatus.ACTIVE,
    val allowedPages: Set<AppPageKey> = setOf(AppPageKey.EVENTS_TIMELINE),
    val allowedFeatures: Set<AppFeatureKey> = emptySet(),
    val createdByUid: String = "",
    val updatedAtMs: Long = 0L
)

@Serializable
data class ManagedUserAccess(
    val policy: AccessPolicy,
    val session: UserSessionLock? = null
)

sealed interface AppGateState {
    data object Unauthenticated : AppGateState
    data class ResolvingAccess(val email: String) : AppGateState
    data class PendingApproval(
        val uid: String,
        val email: String,
        val displayName: String?
    ) : AppGateState
    data class Suspended(val email: String) : AppGateState
    data class SessionDisplaced(val activeDeviceName: String) : AppGateState
    data class Authorized(
        val policy: AccessPolicy,
        val sessionToken: String
    ) : AppGateState
}
