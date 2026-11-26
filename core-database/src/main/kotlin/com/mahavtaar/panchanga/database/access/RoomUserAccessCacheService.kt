package com.mahavtaar.panchanga.database.access

import com.mahavtaar.panchanga.core.access.CachedUserAccessRecord
import com.mahavtaar.panchanga.core.access.UserAccessCacheService
import com.mahavtaar.panchanga.database.dao.UserAccessCacheDao
import com.mahavtaar.panchanga.database.entity.CachedUserAccessEntity
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AccessStatus
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.UserRole
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [UserAccessCacheService] (defined in `:core`).
 * Stores user permissions persistently in SQLite (`cached_user_access` table) and keeps
 * a warm in-memory mirror so app startup renders the UI immediately without waiting for
 * Firebase Auth or Firestore network round-trips.
 */
@Singleton
class RoomUserAccessCacheService @Inject constructor(
    private val userAccessCacheDao: UserAccessCacheDao
) : UserAccessCacheService {

    companion object {
        private const val LEGACY_SEED_USER_ID = "local_cached_user"
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val warmRecordsByUid = ConcurrentHashMap<String, CachedUserAccessRecord>()
    private val lastActiveRecordRef = AtomicReference<CachedUserAccessRecord?>(null)

    init {
        backgroundScope.launch {
            runCatching {
                // Purge any legacy synthetic seed record from earlier builds
                userAccessCacheDao.deleteByUserId(LEGACY_SEED_USER_ID)
                val recent = userAccessCacheDao.getMostRecent()?.toRecord()
                    ?.takeIf { it.userId != LEGACY_SEED_USER_ID }
                if (recent != null) {
                    warmRecordsByUid[recent.userId] = recent
                    lastActiveRecordRef.set(recent)
                }
            }
        }
    }

    override fun observeCachedAccess(userId: String): Flow<CachedUserAccessRecord?> {
        val cleanUid = userId.trim()
        return userAccessCacheDao.observeByUserId(cleanUid)
            .map { entity ->
                val record = entity?.toRecord()?.takeIf { it.userId == cleanUid }
                if (record != null) {
                    record
                } else {
                    warmRecordsByUid[cleanUid]
                }
            }
            .onEach { record ->
                if (record != null && record.userId == cleanUid) {
                    warmRecordsByUid[cleanUid] = record
                    lastActiveRecordRef.set(record)
                }
            }
            .catch {
                emit(warmRecordsByUid[cleanUid])
            }
    }

    override fun observeLastActiveAccess(): Flow<CachedUserAccessRecord?> =
        userAccessCacheDao.observeMostRecent()
            .map { entity ->
                entity?.toRecord()?.takeIf { it.userId != LEGACY_SEED_USER_ID }
            }
            .onEach { record ->
                if (record != null) {
                    warmRecordsByUid[record.userId] = record
                    lastActiveRecordRef.set(record)
                }
            }
            .catch {
                emit(lastActiveRecordRef.get())
            }

    override fun getInstantCachedAccess(userId: String?): CachedUserAccessRecord? {
        val cleanUid = userId?.trim().orEmpty()
        if (cleanUid.isEmpty() || cleanUid == LEGACY_SEED_USER_ID) return null
        warmRecordsByUid[cleanUid]?.let { return it }
        val recent = lastActiveRecordRef.get()
        return if (recent != null && recent.userId == cleanUid) recent else null
    }

    override suspend fun getCachedAccess(userId: String): CachedUserAccessRecord? =
        withContext(Dispatchers.IO) {
            val cleanUid = userId.trim()
            if (cleanUid.isBlank() || cleanUid == LEGACY_SEED_USER_ID) return@withContext null
            warmRecordsByUid[cleanUid]?.let { return@withContext it }
            val loaded = runCatching {
                userAccessCacheDao.getByUserId(cleanUid)?.toRecord()
            }.getOrNull()?.takeIf { it.userId == cleanUid }
            if (loaded != null) {
                warmRecordsByUid[cleanUid] = loaded
                lastActiveRecordRef.set(loaded)
            }
            loaded
        }

    override suspend fun getLastActiveAccess(): CachedUserAccessRecord? =
        withContext(Dispatchers.IO) {
            val loaded = runCatching {
                userAccessCacheDao.getMostRecent()?.toRecord()
            }.getOrNull()?.takeIf { it.userId != LEGACY_SEED_USER_ID }
            if (loaded != null) {
                warmRecordsByUid[loaded.userId] = loaded
                lastActiveRecordRef.set(loaded)
            }
            loaded
        }

    override suspend fun upsertCachedAccess(record: CachedUserAccessRecord) {
        val cleanUid = record.userId.trim()
        if (cleanUid.isBlank() || cleanUid == LEGACY_SEED_USER_ID) return
        val normalized = record.copy(userId = cleanUid)
        warmRecordsByUid[cleanUid] = normalized
        lastActiveRecordRef.set(normalized)
        withContext(Dispatchers.IO) {
            runCatching { userAccessCacheDao.upsert(normalized.toEntity()) }
        }
    }

    override suspend fun clearCachedAccess(userId: String) {
        val cleanUid = userId.trim()
        if (cleanUid.isBlank()) return
        warmRecordsByUid.remove(cleanUid)
        if (lastActiveRecordRef.get()?.userId == cleanUid) {
            lastActiveRecordRef.set(null)
        }
        withContext(Dispatchers.IO) {
            runCatching { userAccessCacheDao.deleteByUserId(cleanUid) }
        }
    }

    override suspend fun clearAllCachedAccess() {
        warmRecordsByUid.clear()
        lastActiveRecordRef.set(null)
        withContext(Dispatchers.IO) {
            runCatching { userAccessCacheDao.clearAll() }
        }
    }
}

private fun CachedUserAccessEntity.toRecord(): CachedUserAccessRecord = CachedUserAccessRecord(
    userId = userId,
    email = email,
    displayName = displayName,
    roleName = roleName,
    statusName = statusName,
    allowedPagesCsv = allowedPagesCsv,
    allowedFeaturesCsv = allowedFeaturesCsv,
    authorizedByUid = authorizedByUid,
    sessionToken = sessionToken,
    updatedAtMs = updatedAtMs
)

private fun CachedUserAccessRecord.toEntity(): CachedUserAccessEntity = CachedUserAccessEntity(
    userId = userId,
    email = email,
    displayName = displayName,
    roleName = roleName,
    statusName = statusName,
    allowedPagesCsv = allowedPagesCsv,
    allowedFeaturesCsv = allowedFeaturesCsv,
    authorizedByUid = authorizedByUid,
    sessionToken = sessionToken,
    updatedAtMs = updatedAtMs
)

fun CachedUserAccessRecord.toAccessPolicy(): AccessPolicy {
    val role = runCatching { UserRole.valueOf(roleName) }.getOrDefault(UserRole.USER)
    val status = runCatching { AccessStatus.valueOf(statusName) }.getOrDefault(AccessStatus.PENDING)
    val pages = if (role == UserRole.ADMIN) {
        AppPageKey.entries.toSet()
    } else {
        allowedPagesCsv.split(",")
            .mapNotNull { token -> token.trim().takeIf { it.isNotEmpty() }?.let { runCatching { AppPageKey.valueOf(it) }.getOrNull() } }
            .toSet()
    }
    val features = if (role == UserRole.ADMIN) {
        AppFeatureKey.entries.toSet()
    } else {
        allowedFeaturesCsv.split(",")
            .mapNotNull { token -> token.trim().takeIf { it.isNotEmpty() }?.let { runCatching { AppFeatureKey.valueOf(it) }.getOrNull() } }
            .toSet()
    }
    return AccessPolicy(
        userId = userId,
        email = email,
        displayName = displayName,
        role = role,
        status = status,
        allowedPages = pages,
        allowedFeatures = features,
        authorizedByUid = authorizedByUid,
        updatedAtMs = updatedAtMs
    )
}

fun AccessPolicy.toCachedRecord(sessionToken: String): CachedUserAccessRecord = CachedUserAccessRecord(
    userId = userId,
    email = email,
    displayName = displayName,
    roleName = role.name,
    statusName = status.name,
    allowedPagesCsv = allowedPages.joinToString(",") { it.name },
    allowedFeaturesCsv = allowedFeatures.joinToString(",") { it.name },
    authorizedByUid = authorizedByUid,
    sessionToken = sessionToken,
    updatedAtMs = if (updatedAtMs > 0L) updatedAtMs else System.currentTimeMillis()
)
