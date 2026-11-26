package com.mahavtaar.panchanga.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mahavtaar.panchanga.database.entity.NotificationRuleEntity
import com.mahavtaar.panchanga.database.entity.ScheduledAlertEntity
import com.mahavtaar.panchanga.database.entity.VoiceRecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationRuleDao {
    @Query("SELECT * FROM notification_rules")
    fun observeAll(): Flow<List<NotificationRuleEntity>>

    @Query("SELECT * FROM notification_rules")
    suspend fun getAll(): List<NotificationRuleEntity>

    @Query("SELECT * FROM notification_rules WHERE eventType = :eventType LIMIT 1")
    suspend fun getByEventType(eventType: String): NotificationRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NotificationRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<NotificationRuleEntity>)

    @Query("DELETE FROM notification_rules")
    suspend fun deleteAll()
}

@Dao
interface VoiceRecordingDao {
    @Query("SELECT * FROM voice_recordings ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<VoiceRecordingEntity>>

    @Query("SELECT * FROM voice_recordings WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VoiceRecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VoiceRecordingEntity)

    @Query("UPDATE voice_recordings SET displayName = :newName WHERE id = :id")
    suspend fun rename(id: String, newName: String)

    @Query("DELETE FROM voice_recordings WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ScheduledAlertDao {
    @Query("SELECT * FROM scheduled_alerts WHERE status = 'SCHEDULED' ORDER BY triggerTimeMs ASC")
    fun observeUpcomingScheduled(): Flow<List<ScheduledAlertEntity>>

    @Query("SELECT * FROM scheduled_alerts WHERE status != 'SCHEDULED' ORDER BY updatedAtMs DESC LIMIT 40")
    fun observeRecentHistory(): Flow<List<ScheduledAlertEntity>>

    @Query("SELECT * FROM scheduled_alerts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ScheduledAlertEntity?

    @Query("SELECT * FROM scheduled_alerts WHERE status = 'SCHEDULED'")
    suspend fun getAllScheduledNow(): List<ScheduledAlertEntity>

    @Query("DELETE FROM scheduled_alerts WHERE status = 'SCHEDULED'")
    suspend fun clearScheduledQueue()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ScheduledAlertEntity>)

    @Query("UPDATE scheduled_alerts SET status = :status, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAtMs: Long)

    @Query("DELETE FROM scheduled_alerts WHERE updatedAtMs < :cutoffMs AND status != 'SCHEDULED'")
    suspend fun pruneOldHistory(cutoffMs: Long)
}
