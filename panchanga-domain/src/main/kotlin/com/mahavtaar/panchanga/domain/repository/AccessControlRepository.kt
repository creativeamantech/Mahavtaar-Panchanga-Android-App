package com.mahavtaar.panchanga.domain.repository

import com.mahavtaar.panchanga.domain.model.AccessPolicy
import com.mahavtaar.panchanga.domain.model.AccessStatus
import com.mahavtaar.panchanga.domain.model.AppFeatureKey
import com.mahavtaar.panchanga.domain.model.AppGateState
import com.mahavtaar.panchanga.domain.model.AppPageKey
import com.mahavtaar.panchanga.domain.model.EmailInviteRule
import com.mahavtaar.panchanga.domain.model.UserRole
import com.mahavtaar.panchanga.domain.model.UserSessionLock
import kotlinx.coroutines.flow.Flow

interface AccessControlRepository {
    fun observeGateState(
        uid: String,
        email: String?,
        displayName: String?,
        isEmailVerified: Boolean
    ): Flow<AppGateState>

    suspend fun claimSingleDeviceSession(uid: String): Result<String>

    suspend fun refreshAccessGateOnce(
        uid: String,
        email: String?,
        displayName: String?,
        isEmailVerified: Boolean
    ): Result<AppGateState>

    fun observeAllUserAccess(): Flow<Result<List<AccessPolicy>>>

    fun observeAllUserSessions(): Flow<Result<List<UserSessionLock>>>

    fun observeEmailInvites(): Flow<Result<List<EmailInviteRule>>>

    suspend fun saveUserAccess(
        userId: String,
        email: String,
        displayName: String?,
        role: UserRole,
        status: AccessStatus,
        allowedPages: Set<AppPageKey>,
        allowedFeatures: Set<AppFeatureKey>
    ): Result<Unit>

    suspend fun forceSignOutUserSession(targetUserId: String): Result<Unit>

    suspend fun upsertEmailInvite(
        email: String,
        role: UserRole,
        status: AccessStatus,
        allowedPages: Set<AppPageKey>,
        allowedFeatures: Set<AppFeatureKey>
    ): Result<Unit>

    suspend fun deleteEmailInvite(emailHash: String): Result<Unit>
}
