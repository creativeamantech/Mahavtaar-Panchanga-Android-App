package com.mahavtaar.panchanga.domain.repository

import com.mahavtaar.panchanga.domain.model.CloudSyncStatus
import com.mahavtaar.panchanga.domain.model.CloudUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CloudAccountRepository {
    val currentUser: Flow<CloudUser?>
    val syncStatus: StateFlow<CloudSyncStatus>

    suspend fun startSyncForUser(uid: String)
    suspend fun stopSync()
    suspend fun syncAllNow(): Result<Unit>
    suspend fun signOut(): Result<Unit>
}
