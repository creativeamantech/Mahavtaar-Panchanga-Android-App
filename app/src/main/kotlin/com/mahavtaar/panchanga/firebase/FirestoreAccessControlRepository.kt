package com.mahavtaar.panchanga.firebase

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mahavtaar.panchanga.core.access.UserAccessCacheService
import com.mahavtaar.panchanga.database.access.toAccessPolicy
import com.mahavtaar.panchanga.database.access.toCachedRecord
import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AccessStatus
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppGateState
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.EmailInviteRule
import com.mahavtaar.panchanga.domain.model.ROOT_ADMIN_EMAIL
import com.mahavtaar.panchanga.domain.model.UserRole
import com.mahavtaar.panchanga.domain.model.UserSessionLock
import com.mahavtaar.panchanga.domain.repository.AccessControlRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FirestoreAccessControlRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val userAccessCacheService: UserAccessCacheService
) : AccessControlRepository {

    private val localSessionTokens = ConcurrentHashMap<String, String>()
    private val claimMutex = Mutex()
    private val bootstrapMutex = Mutex()
    private val cacheExceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val cacheIoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + cacheExceptionHandler)
    private val cachePrefs by lazy {
        context.getSharedPreferences("mahavtaar_access_gate_cache", Context.MODE_PRIVATE)
    }

    private fun saveCachedPolicy(uid: String, policy: AccessPolicy, sessionToken: String) {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) return
        val normalizedPolicy = policy.copy(userId = cleanUid)
        cachePrefs.edit()
            .putString("role_$cleanUid", normalizedPolicy.role.name)
            .putString("status_$cleanUid", normalizedPolicy.status.name)
            .putString("email_$cleanUid", normalizedPolicy.email)
            .putString("name_$cleanUid", normalizedPolicy.displayName.orEmpty())
            .putStringSet("pages_$cleanUid", normalizedPolicy.allowedPages.map { it.name }.toSet())
            .putStringSet("features_$cleanUid", normalizedPolicy.allowedFeatures.map { it.name }.toSet())
            .putString("token_$cleanUid", sessionToken)
            .apply()
        cacheIoScope.launch {
            runCatching {
                userAccessCacheService.upsertCachedAccess(
                    normalizedPolicy.toCachedRecord(sessionToken)
                )
            }
        }
    }

    private fun loadCachedPolicy(uid: String, email: String?, displayName: String?): Pair<AccessPolicy, String>? {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) return null

        // 1) Check warm Room cache mirror first
        userAccessCacheService.getInstantCachedAccess(cleanUid)?.takeIf { it.userId == cleanUid }?.let { rec ->
            val token = rec.sessionToken.ifBlank {
                localSessionTokens[cleanUid] ?: UUID.randomUUID().toString()
            }
            localSessionTokens.putIfAbsent(cleanUid, token)
            return rec.toAccessPolicy() to token
        }

        // 2) Fallback to fast prefs mirror and hydrate Room asynchronously
        val roleStr = cachePrefs.getString("role_$cleanUid", null) ?: return null
        val statusStr = cachePrefs.getString("status_$cleanUid", null) ?: return null
        val role = runCatching { UserRole.valueOf(roleStr) }.getOrNull() ?: return null
        val status = runCatching { AccessStatus.valueOf(statusStr) }.getOrNull() ?: return null
        val pages = cachePrefs.getStringSet("pages_$cleanUid", emptySet()).orEmpty()
            .mapNotNull { name -> runCatching { AppPageKey.valueOf(name) }.getOrNull() }
            .toSet()
        val features = cachePrefs.getStringSet("features_$cleanUid", emptySet()).orEmpty()
            .mapNotNull { name -> runCatching { AppFeatureKey.valueOf(name) }.getOrNull() }
            .toSet()
        val token = cachePrefs.getString("token_$cleanUid", null).orEmpty()
            .ifBlank { localSessionTokens[cleanUid] ?: UUID.randomUUID().toString() }
        localSessionTokens.putIfAbsent(cleanUid, token)
        val policy = AccessPolicy(
            userId = cleanUid,
            email = email?.trim()?.lowercase()?.ifBlank { null } ?: cachePrefs.getString("email_$cleanUid", "").orEmpty(),
            displayName = displayName ?: cachePrefs.getString("name_$cleanUid", null),
            role = role,
            status = status,
            allowedPages = if (role == UserRole.ADMIN) AppPageKey.entries.toSet() else pages,
            allowedFeatures = if (role == UserRole.ADMIN) AppFeatureKey.entries.toSet() else features
        )
        cacheIoScope.launch {
            runCatching { userAccessCacheService.upsertCachedAccess(policy.toCachedRecord(token)) }
        }
        return policy to token
    }

    private fun currentDeviceLabel(): String {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()
        val combined = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model".trim()
        }
        return combined.ifBlank { "Android Device" }.take(80)
    }

    private fun isBootstrapAdminAccount(email: String?, isEmailVerified: Boolean): Boolean =
        isEmailVerified && email?.trim()?.equals(ROOT_ADMIN_EMAIL, ignoreCase = true) == true

    override suspend fun claimSingleDeviceSession(uid: String): Result<String> = claimMutex.withLock {
        val cleanUid = uid.trim()
        if (cleanUid.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID is required"))
        }
        val signedInUid = runCatching { firebaseAuth.currentUser?.uid }.getOrNull()
        if (signedInUid.isNullOrBlank() || signedInUid != cleanUid) {
            val fallback = localSessionTokens[cleanUid] ?: UUID.randomUUID().toString().also {
                localSessionTokens[cleanUid] = it
            }
            return Result.success(fallback)
        }
        val path = "user_sessions/$cleanUid"
        return try {
            val token = UUID.randomUUID().toString()
            localSessionTokens[cleanUid] = token
            val docRef = firestore.collection("user_sessions").document(cleanUid)
            val existing = docRef.get().await()
            val payload = FirestoreUserSessionDoc(
                userId = cleanUid,
                activeSessionToken = token,
                deviceName = currentDeviceLabel(),
                forcedLogout = false
            )
            if (existing.exists()) {
                docRef.update(payload.toClaimUpdateMap()).await()
            } else {
                docRef.set(payload.toClaimCreateMap()).await()
            }
            Result.success(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            // Keep local token so transient offline launch still functions if already authorized
            val fallback = localSessionTokens[cleanUid] ?: UUID.randomUUID().toString().also {
                localSessionTokens[cleanUid] = it
            }
            Result.success(fallback)
        }
    }

    private suspend fun ensureUserAccessBootstrapped(
        uid: String,
        email: String?,
        displayName: String?,
        isEmailVerified: Boolean
    ): AccessPolicy? = bootstrapMutex.withLock {
        val cleanUid = uid.trim()
        val cleanEmail = email?.trim()?.lowercase().orEmpty()
        if (cleanUid.isBlank() || cleanEmail.isBlank()) return null

        val accessRef = firestore.collection("user_access").document(cleanUid)
        val path = "user_access/$cleanUid"
        try {
            val currentSnap = accessRef.get().await()
            if (currentSnap.exists()) {
                return currentSnap.toObject(FirestoreUserAccessDoc::class.java)?.toDomain(currentSnap)
            }

            if (isBootstrapAdminAccount(cleanEmail, isEmailVerified)) {
                val allPages = AppPageKey.entries.map { it.name }
                val allFeatures = AppFeatureKey.entries.map { it.name }
                val adminDoc = FirestoreUserAccessDoc(
                    userId = cleanUid,
                    email = cleanEmail,
                    displayName = displayName?.trim()?.take(100),
                    role = UserRole.ADMIN.name,
                    status = AccessStatus.ACTIVE.name,
                    allowedPages = allPages,
                    allowedFeatures = allFeatures,
                    authorizedByUid = cleanUid
                )
                accessRef.set(adminDoc.toCreateMap()).await()
                return AccessPolicy(
                    userId = cleanUid,
                    email = cleanEmail,
                    displayName = displayName,
                    role = UserRole.ADMIN,
                    status = AccessStatus.ACTIVE,
                    allowedPages = AppPageKey.entries.toSet(),
                    allowedFeatures = AppFeatureKey.entries.toSet(),
                    authorizedByUid = cleanUid,
                    updatedAtMs = System.currentTimeMillis()
                )
            }

            // Check if Admin pre-authorized this email in /email_invites/{sha256(email)}
            if (isEmailVerified) {
                val hash = sha256EmailHash(cleanEmail)
                val invitePath = "email_invites/$hash"
                val inviteSnap = try {
                    firestore.collection("email_invites").document(hash).get().await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.GET, invitePath)
                    null
                }
                if (inviteSnap != null && inviteSnap.exists()) {
                    val inviteDoc = inviteSnap.toObject(FirestoreEmailInviteDoc::class.java)
                    if (inviteDoc != null && inviteDoc.email.trim().equals(cleanEmail, ignoreCase = true)) {
                        val claimedDoc = FirestoreUserAccessDoc(
                            userId = cleanUid,
                            email = cleanEmail,
                            displayName = displayName?.trim()?.take(100),
                            role = inviteDoc.role,
                            status = inviteDoc.status,
                            allowedPages = inviteDoc.allowedPages,
                            allowedFeatures = inviteDoc.allowedFeatures,
                            authorizedByUid = inviteDoc.createdByUid.takeIf { it.isNotBlank() },
                            inviteHash = hash
                        )
                        accessRef.set(claimedDoc.toCreateMap()).await()
                        return claimedDoc.toDomain(inviteSnap).copy(userId = cleanUid)
                    }
                }
            }

            // Default: register as PENDING with 0 pages and 0 features until Admin authorizes
            val pendingDoc = FirestoreUserAccessDoc(
                userId = cleanUid,
                email = cleanEmail,
                displayName = displayName?.trim()?.take(100),
                role = UserRole.USER.name,
                status = AccessStatus.PENDING.name,
                allowedPages = emptyList(),
                allowedFeatures = emptyList(),
                authorizedByUid = null
            )
            accessRef.set(pendingDoc.toCreateMap()).await()
            return AccessPolicy(
                userId = cleanUid,
                email = cleanEmail,
                displayName = displayName,
                role = UserRole.USER,
                status = AccessStatus.PENDING,
                allowedPages = emptySet(),
                allowedFeatures = emptySet(),
                authorizedByUid = null,
                updatedAtMs = System.currentTimeMillis()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            if (isBootstrapAdminAccount(cleanEmail, isEmailVerified)) {
                return AccessPolicy.ADMIN_FULL.copy(
                    userId = cleanUid,
                    email = cleanEmail,
                    displayName = displayName
                )
            }
            return null
        }
    }

    private fun observeSessionDocument(uid: String): Flow<DocumentSnapshot?> = callbackFlow {
        val cleanUid = uid.trim()
        val signedInUid = runCatching { firebaseAuth.currentUser?.uid }.getOrNull()
        if (cleanUid.isBlank() || signedInUid != cleanUid) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val path = "user_sessions/$cleanUid"
        val registration = runCatching {
            firestore.collection("user_sessions")
                .document(cleanUid)
                .addSnapshotListener { snapshot, error ->
                    runCatching {
                        if (error != null) {
                            handleFirestoreError(error, OperationType.GET, path)
                            return@runCatching
                        }
                        trySend(snapshot)
                    }
                }
        }.getOrNull()
        awaitClose { runCatching { registration?.remove() } }
    }

    private fun observeUserAccessDocument(
        uid: String,
        email: String?,
        displayName: String?,
        isEmailVerified: Boolean
    ): Flow<AccessPolicy?> = callbackFlow {
        val cleanUid = uid.trim()
        val signedInUid = runCatching { firebaseAuth.currentUser?.uid }.getOrNull()
        if (cleanUid.isBlank() || signedInUid != cleanUid) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val path = "user_access/$cleanUid"
        val registration = runCatching {
            firestore.collection("user_access")
                .document(cleanUid)
                .addSnapshotListener { snapshot, error ->
                    runCatching {
                        if (error != null) {
                            handleFirestoreError(error, OperationType.GET, path)
                            if (isBootstrapAdminAccount(email, isEmailVerified)) {
                                trySend(
                                    AccessPolicy.ADMIN_FULL.copy(
                                        userId = cleanUid,
                                        email = email?.trim()?.lowercase().orEmpty(),
                                        displayName = displayName
                                    )
                                )
                            }
                            return@runCatching
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val doc = runCatching {
                                snapshot.toObject(FirestoreUserAccessDoc::class.java)
                            }.getOrNull()
                            trySend(doc?.toDomain(snapshot))
                        } else {
                            trySend(null)
                            launch {
                                val bootstrapped = runCatching {
                                    ensureUserAccessBootstrapped(
                                        uid = cleanUid,
                                        email = email,
                                        displayName = displayName,
                                        isEmailVerified = isEmailVerified
                                    )
                                }.getOrNull()
                                if (bootstrapped != null) {
                                    trySend(bootstrapped)
                                }
                            }
                        }
                    }
                }
        }.getOrNull()
        awaitClose { runCatching { registration?.remove() } }
    }

    override fun observeGateState(
        uid: String,
        email: String?,
        displayName: String?,
        isEmailVerified: Boolean
    ): Flow<AppGateState> = callbackFlow {
        val cleanUid = uid.trim()
        val cleanEmail = email?.trim()?.lowercase().orEmpty()
        val initialToken = localSessionTokens.getOrPut(cleanUid) {
            userAccessCacheService.getInstantCachedAccess(cleanUid)?.sessionToken?.takeIf { it.isNotBlank() }
                ?: cachePrefs.getString("token_$cleanUid", null)?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()
        }

        // 1) Immediate 0ms emission from bootstrap admin or locally cached Room policy
        var latestCachedPair: Pair<AccessPolicy, String>? = loadCachedPolicy(cleanUid, cleanEmail, displayName)
        var hasEmittedTerminalState = false
        when {
            isBootstrapAdminAccount(cleanEmail, isEmailVerified) -> {
                val adminPolicy = AccessPolicy.ADMIN_FULL.copy(
                    userId = cleanUid,
                    email = cleanEmail,
                    displayName = displayName
                )
                saveCachedPolicy(cleanUid, adminPolicy, initialToken)
                hasEmittedTerminalState = true
                trySend(AppGateState.Authorized(policy = adminPolicy, sessionToken = initialToken))
            }
            latestCachedPair != null &&
                latestCachedPair?.first?.status == AccessStatus.ACTIVE &&
                (latestCachedPair?.first?.isAdmin == true || latestCachedPair?.first?.allowedPages?.isNotEmpty() == true) -> {
                val pair = latestCachedPair!!
                hasEmittedTerminalState = true
                trySend(AppGateState.Authorized(policy = pair.first, sessionToken = pair.second))
            }
            else -> {
                trySend(AppGateState.ResolvingAccess(cleanEmail))
                // Asynchronously query Room SQLite if warm mirror wasn't populated yet
                launch {
                    val roomRecord = userAccessCacheService.getCachedAccess(cleanUid)
                        ?.takeIf { it.userId == cleanUid }
                    if (roomRecord != null) {
                        val rPolicy = roomRecord.toAccessPolicy()
                        val rToken = roomRecord.sessionToken.ifBlank { initialToken }
                        latestCachedPair = rPolicy to rToken
                        if (
                            !hasEmittedTerminalState &&
                            rPolicy.status == AccessStatus.ACTIVE &&
                            (rPolicy.isAdmin || rPolicy.allowedPages.isNotEmpty())
                        ) {
                            hasEmittedTerminalState = true
                            trySend(AppGateState.Authorized(policy = rPolicy, sessionToken = rToken))
                        }
                    }
                }
            }
        }

        // 2) Claim single-device session asynchronously in the background without blocking startup
        var sessionClaimCompleted = false
        val claimJob = launch {
            claimSingleDeviceSession(cleanUid)
            sessionClaimCompleted = true
        }

        // 3) Safety timeout fallback so slow/offline networks never leave the user stuck on ResolvingAccessScreen
        val timeoutJob = launch {
            delay(4_500L)
            if (!hasEmittedTerminalState) {
                val pair = latestCachedPair
                    ?: userAccessCacheService.getCachedAccess(cleanUid)
                        ?.takeIf { it.userId == cleanUid }
                        ?.let { it.toAccessPolicy() to it.sessionToken.ifBlank { initialToken } }
                val fallbackState = if (pair != null) {
                    val cPolicy = pair.first
                    when {
                        cPolicy.status == AccessStatus.SUSPENDED -> AppGateState.Suspended(cleanEmail)
                        cPolicy.status == AccessStatus.ACTIVE && (cPolicy.isAdmin || cPolicy.allowedPages.isNotEmpty()) ->
                            AppGateState.Authorized(cPolicy, pair.second)
                        else -> AppGateState.PendingApproval(cleanUid, cleanEmail, displayName)
                    }
                } else {
                    // Fail closed: unknown/offline non-admin users must stay on PendingApproval (with Check Status & Sign Out)
                    AppGateState.PendingApproval(cleanUid, cleanEmail, displayName)
                }
                hasEmittedTerminalState = true
                trySend(fallbackState)
            }
        }

        val job = launch {
            combine(
                userAccessCacheService.observeCachedAccess(cleanUid).onStart { emit(userAccessCacheService.getInstantCachedAccess(cleanUid)) },
                observeSessionDocument(cleanUid).onStart { emit(null) },
                observeUserAccessDocument(cleanUid, cleanEmail, displayName, isEmailVerified).onStart { emit(null) }
            ) { roomCachedRecord, sessionSnap, remoteAccessPolicy ->
                val expectedToken = localSessionTokens[cleanUid] ?: roomCachedRecord?.sessionToken?.takeIf { it.isNotBlank() } ?: initialToken
                if (
                    sessionClaimCompleted &&
                    sessionSnap != null &&
                    sessionSnap.exists() &&
                    !sessionSnap.metadata.hasPendingWrites()
                ) {
                    val sessionDoc = runCatching {
                        sessionSnap.toObject(FirestoreUserSessionDoc::class.java)
                    }.getOrNull()
                    val remoteForced = sessionDoc?.forcedLogout == true
                    val remoteToken = sessionDoc?.activeSessionToken.orEmpty()
                    if (remoteForced || (remoteToken.isNotBlank() && remoteToken != expectedToken)) {
                        val otherDevice = sessionDoc?.deviceName?.takeIf { it.isNotBlank() } ?: "Another Device"
                        return@combine AppGateState.SessionDisplaced(otherDevice)
                    }
                }

                val roomCachedPolicy = roomCachedRecord
                    ?.takeIf { it.userId == cleanUid }
                    ?.toAccessPolicy()
                    ?: latestCachedPair?.first?.takeIf { it.userId == cleanUid }
                val effectivePolicy = when {
                    isBootstrapAdminAccount(cleanEmail, isEmailVerified) &&
                        (remoteAccessPolicy == null || remoteAccessPolicy.role != UserRole.ADMIN || remoteAccessPolicy.status != AccessStatus.ACTIVE) ->
                        AccessPolicy.ADMIN_FULL.copy(
                            userId = cleanUid,
                            email = cleanEmail,
                            displayName = displayName
                        )
                    remoteAccessPolicy != null -> remoteAccessPolicy
                    else -> roomCachedPolicy
                }

                if (remoteAccessPolicy != null) {
                    latestCachedPair = remoteAccessPolicy to expectedToken
                    saveCachedPolicy(cleanUid, remoteAccessPolicy, expectedToken)
                }

                when {
                    effectivePolicy == null -> AppGateState.ResolvingAccess(cleanEmail)
                    effectivePolicy.status == AccessStatus.SUSPENDED ->
                        AppGateState.Suspended(cleanEmail)
                    effectivePolicy.status == AccessStatus.PENDING && !effectivePolicy.isAdmin ->
                        AppGateState.PendingApproval(
                            uid = cleanUid,
                            email = cleanEmail,
                            displayName = displayName
                        )
                    !effectivePolicy.isAdmin && effectivePolicy.allowedPages.isEmpty() ->
                        AppGateState.PendingApproval(
                            uid = cleanUid,
                            email = cleanEmail,
                            displayName = displayName
                        )
                    else -> AppGateState.Authorized(
                        policy = effectivePolicy,
                        sessionToken = expectedToken
                    )
                }
            }
                .distinctUntilChanged()
                .catch { /* Never crash access gate on transient Firestore/Room stream error */ }
                .collect { state ->
                    if (state !is AppGateState.ResolvingAccess) {
                        hasEmittedTerminalState = true
                    }
                    trySend(state)
                }
        }

        awaitClose {
            claimJob.cancel()
            timeoutJob.cancel()
            job.cancel()
        }
    }

    override suspend fun refreshAccessGateOnce(
        uid: String,
        email: String?,
        displayName: String?,
        isEmailVerified: Boolean
    ): Result<AppGateState> {
        val cleanUid = uid.trim()
        val cleanEmail = email?.trim()?.lowercase().orEmpty()
        val path = "user_access/$cleanUid"
        return try {
            val snap = firestore.collection("user_access").document(cleanUid).get().await()
            val policy = if (snap.exists()) {
                snap.toObject(FirestoreUserAccessDoc::class.java)?.toDomain(snap)
            } else {
                ensureUserAccessBootstrapped(cleanUid, cleanEmail, displayName, isEmailVerified)
            }
            val token = localSessionTokens[cleanUid].orEmpty()
            if (policy != null) {
                saveCachedPolicy(cleanUid, policy, token)
            }
            val state = when {
                isBootstrapAdminAccount(cleanEmail, isEmailVerified) -> {
                    val adminPolicy = AccessPolicy.ADMIN_FULL.copy(
                        userId = cleanUid,
                        email = cleanEmail,
                        displayName = displayName
                    )
                    saveCachedPolicy(cleanUid, adminPolicy, token)
                    AppGateState.Authorized(
                        policy = adminPolicy,
                        sessionToken = token
                    )
                }
                policy == null -> AppGateState.PendingApproval(cleanUid, cleanEmail, displayName)
                policy.status == AccessStatus.SUSPENDED -> AppGateState.Suspended(cleanEmail)
                policy.status == AccessStatus.PENDING -> AppGateState.PendingApproval(cleanUid, cleanEmail, displayName)
                else -> AppGateState.Authorized(policy, token)
            }
            Result.success(state)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val json = handleFirestoreError(e, OperationType.GET, path)
            Result.failure(IllegalStateException(json, e))
        }
    }

    /**
     * Emits the signed-in user's UID only when Firebase Auth is authenticated AND the user
     * satisfies `isActiveAdmin()` in `firestore.rules` (either verified bootstrap admin or
     * active ADMIN document in `/user_access/{uid}`). Emits `null` whenever unauthenticated
     * or non-admin so collection-wide `list` listeners are never opened without permission.
     */
    private fun observeVerifiedAdminUid(): Flow<String?> =
        firebaseAuth.authStateFlow()
            .flatMapLatest { fbUser ->
                val uid = fbUser?.uid?.trim().orEmpty()
                if (fbUser == null || uid.isBlank()) {
                    flowOf(null)
                } else if (isBootstrapAdminAccount(fbUser.email, fbUser.isEmailVerified)) {
                    flowOf(uid)
                } else {
                    observeUserAccessDocument(
                        uid = uid,
                        email = fbUser.email,
                        displayName = fbUser.displayName,
                        isEmailVerified = fbUser.isEmailVerified
                    ).map { policy ->
                        if (policy?.role == UserRole.ADMIN && policy.status == AccessStatus.ACTIVE) uid else null
                    }
                }
            }
            .distinctUntilChanged()
            .catch { emit(null) }

    override fun observeAllUserAccess(): Flow<Result<List<AccessPolicy>>> =
        observeVerifiedAdminUid().flatMapLatest { adminUid ->
            if (adminUid.isNullOrBlank()) {
                flowOf(Result.success(emptyList()))
            } else {
                callbackFlow {
                    val path = "user_access"
                    val registration = runCatching {
                        firestore.collection("user_access")
                            .orderBy("updatedAt", Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, error ->
                                runCatching {
                                    if (error != null) {
                                        val json = handleFirestoreError(error, OperationType.LIST, path)
                                        trySend(Result.failure(IllegalStateException(json, error)))
                                        return@runCatching
                                    }
                                    val list = snapshot?.documents?.mapNotNull { doc ->
                                        runCatching {
                                            doc.toObject(FirestoreUserAccessDoc::class.java)?.toDomain(doc)
                                        }.getOrNull()
                                    }.orEmpty()
                                    trySend(Result.success(list))
                                }
                            }
                    }.getOrNull()
                    awaitClose { runCatching { registration?.remove() } }
                }
            }
        }

    override fun observeAllUserSessions(): Flow<Result<List<UserSessionLock>>> =
        observeVerifiedAdminUid().flatMapLatest { adminUid ->
            if (adminUid.isNullOrBlank()) {
                flowOf(Result.success(emptyList()))
            } else {
                callbackFlow {
                    val path = "user_sessions"
                    val registration = runCatching {
                        firestore.collection("user_sessions")
                            .addSnapshotListener { snapshot, error ->
                                runCatching {
                                    if (error != null) {
                                        val json = handleFirestoreError(error, OperationType.LIST, path)
                                        trySend(Result.failure(IllegalStateException(json, error)))
                                        return@runCatching
                                    }
                                    val list = snapshot?.documents?.mapNotNull { doc ->
                                        runCatching {
                                            doc.toObject(FirestoreUserSessionDoc::class.java)?.toDomain(doc)
                                        }.getOrNull()
                                    }.orEmpty()
                                    trySend(Result.success(list))
                                }
                            }
                    }.getOrNull()
                    awaitClose { runCatching { registration?.remove() } }
                }
            }
        }

    override fun observeEmailInvites(): Flow<Result<List<EmailInviteRule>>> =
        observeVerifiedAdminUid().flatMapLatest { adminUid ->
            if (adminUid.isNullOrBlank()) {
                flowOf(Result.success(emptyList()))
            } else {
                callbackFlow {
                    val path = "email_invites"
                    val registration = runCatching {
                        firestore.collection("email_invites")
                            .orderBy("updatedAt", Query.Direction.DESCENDING)
                            .addSnapshotListener { snapshot, error ->
                                runCatching {
                                    if (error != null) {
                                        val json = handleFirestoreError(error, OperationType.LIST, path)
                                        trySend(Result.failure(IllegalStateException(json, error)))
                                        return@runCatching
                                    }
                                    val list = snapshot?.documents?.mapNotNull { doc ->
                                        runCatching {
                                            doc.toObject(FirestoreEmailInviteDoc::class.java)?.toDomain(doc)
                                        }.getOrNull()
                                    }.orEmpty()
                                    trySend(Result.success(list))
                                }
                            }
                    }.getOrNull()
                    awaitClose { runCatching { registration?.remove() } }
                }
            }
        }

    override suspend fun saveUserAccess(
        userId: String,
        email: String,
        displayName: String?,
        role: UserRole,
        status: AccessStatus,
        allowedPages: Set<AppPageKey>,
        allowedFeatures: Set<AppFeatureKey>
    ): Result<Unit> {
        val cleanUid = userId.trim()
        val cleanEmail = email.trim().lowercase()
        require(cleanUid.isNotBlank()) { "User ID must not be blank" }
        require(cleanEmail.contains("@")) { "Valid email address is required" }

        val adminUid = runCatching { firebaseAuth.currentUser?.uid }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Please sign in with Google as an administrator first."))
        val path = "user_access/$cleanUid"
        val effectivePages = if (role == UserRole.ADMIN) {
            AppPageKey.entries.map { it.name }
        } else {
            allowedPages.map { it.name }
        }
        val effectiveFeatures = if (role == UserRole.ADMIN) {
            AppFeatureKey.entries.map { it.name }
        } else {
            allowedFeatures.map { it.name }
        }

        val docModel = FirestoreUserAccessDoc(
            userId = cleanUid,
            email = cleanEmail,
            displayName = displayName?.trim()?.take(100),
            role = role.name,
            status = status.name,
            allowedPages = effectivePages,
            allowedFeatures = effectiveFeatures,
            authorizedByUid = adminUid
        )

        return try {
            val docRef = firestore.collection("user_access").document(cleanUid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val json = handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(IllegalStateException(json, e))
        }
    }

    override suspend fun forceSignOutUserSession(targetUserId: String): Result<Unit> {
        val cleanUid = targetUserId.trim()
        require(cleanUid.isNotBlank()) { "Target user ID is required" }
        val adminUid = runCatching { firebaseAuth.currentUser?.uid }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Please sign in with Google as an administrator first."))
        val path = "user_sessions/$cleanUid"
        return try {
            val docRef = firestore.collection("user_sessions").document(cleanUid)
            val existing = docRef.get().await()
            val revokedToken = "revoked_${UUID.randomUUID()}"
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "activeSessionToken" to revokedToken,
                        "forcedLogout" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to cleanUid,
                        "activeSessionToken" to revokedToken,
                        "deviceName" to "Force Signed Out by Admin",
                        "forcedLogout" to true,
                        "claimedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val json = handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(IllegalStateException(json, e))
        }
    }

    override suspend fun upsertEmailInvite(
        email: String,
        role: UserRole,
        status: AccessStatus,
        allowedPages: Set<AppPageKey>,
        allowedFeatures: Set<AppFeatureKey>
    ): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Invalid Google email address"))
        }
        val emailHash = sha256EmailHash(cleanEmail)
        val adminUid = runCatching { firebaseAuth.currentUser?.uid }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Please sign in with Google as an administrator first."))
        val path = "email_invites/$emailHash"

        val effectivePages = if (role == UserRole.ADMIN) {
            AppPageKey.entries.map { it.name }
        } else {
            allowedPages.map { it.name }
        }
        val effectiveFeatures = if (role == UserRole.ADMIN) {
            AppFeatureKey.entries.map { it.name }
        } else {
            allowedFeatures.map { it.name }
        }

        val docModel = FirestoreEmailInviteDoc(
            emailHash = emailHash,
            email = cleanEmail,
            role = role.name,
            status = status.name,
            allowedPages = effectivePages,
            allowedFeatures = effectiveFeatures,
            createdByUid = adminUid
        )

        return try {
            val docRef = firestore.collection("email_invites").document(emailHash)
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(docModel.toUpdateMap()).await()
            } else {
                docRef.set(docModel.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val json = handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(IllegalStateException(json, e))
        }
    }

    override suspend fun deleteEmailInvite(emailHash: String): Result<Unit> {
        val cleanHash = emailHash.trim()
        require(cleanHash.isNotBlank()) { "Email hash is required" }
        val adminUid = runCatching { firebaseAuth.currentUser?.uid }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Please sign in with Google as an administrator first."))
        val path = "email_invites/$cleanHash"
        return try {
            firestore.collection("email_invites").document(cleanHash).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val json = handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(IllegalStateException(json, e))
        }
    }
}
