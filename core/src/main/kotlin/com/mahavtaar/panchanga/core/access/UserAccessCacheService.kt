package com.mahavtaar.panchanga.core.access

import kotlinx.coroutines.flow.Flow

/**
 * Device-local persistent representation of a user's access permissions and session token.
 * Stored in Room so the application can render authorized UI immediately on cold start
 * while Firebase Auth and Firestore perform a silent background refresh.
 */
data class CachedUserAccessRecord(
    val userId: String,
    val email: String,
    val displayName: String?,
    val roleName: String,
    val statusName: String,
    val allowedPagesCsv: String,
    val allowedFeaturesCsv: String,
    val authorizedByUid: String?,
    val sessionToken: String,
    val updatedAtMs: Long
)

/**
 * Core service contract for reading and persisting user access permissions in local Room storage.
 */
interface UserAccessCacheService {
    /**
     * Reactive Room Flow emitting cached permissions for a specific [userId].
     */
    fun observeCachedAccess(userId: String): Flow<CachedUserAccessRecord?>

    /**
     * Reactive Room Flow emitting the most recently active user's cached permissions.
     * Used during cold start before Firebase Auth finishes initializing on slow networks.
     */
    fun observeLastActiveAccess(): Flow<CachedUserAccessRecord?>

    /**
     * Non-blocking synchronous read of the warm in-memory Room mirror for instant first-frame UI rendering.
     */
    fun getInstantCachedAccess(userId: String? = null): CachedUserAccessRecord?

    /**
     * Direct suspend Room lookup for [userId].
     */
    suspend fun getCachedAccess(userId: String): CachedUserAccessRecord?

    /**
     * Direct suspend Room lookup for the most recently updated cached access record.
     */
    suspend fun getLastActiveAccess(): CachedUserAccessRecord?

    /**
     * Persists or updates the user's access permissions in Room and updates the warm cache mirror.
     */
    suspend fun upsertCachedAccess(record: CachedUserAccessRecord)

    /**
     * Removes cached access permissions for a specific [userId].
     */
    suspend fun clearCachedAccess(userId: String)

    /**
     * Clears all cached user access records (e.g., on explicit sign-out).
     */
    suspend fun clearAllCachedAccess()
}
