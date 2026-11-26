package com.mahavtaar.panchanga.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mahavtaar.panchanga.database.entity.CachedPanchangaEntity
import com.mahavtaar.panchanga.database.entity.NavtaraFeedbackEntity
import com.mahavtaar.panchanga.database.entity.NavtaraProfileEntity
import com.mahavtaar.panchanga.database.entity.SavedKundliChartEntity
import com.mahavtaar.panchanga.database.entity.SavedLocationEntity
import com.mahavtaar.panchanga.database.entity.SavedNavtaraReportEntity
import com.mahavtaar.panchanga.database.entity.VratReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM saved_locations ORDER BY isDefault DESC, savedAtMs DESC")
    fun observeSavedLocations(): Flow<List<SavedLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(entity: SavedLocationEntity)

    @Query("UPDATE saved_locations SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE saved_locations SET isDefault = 1 WHERE name = :name")
    suspend fun markDefaultByName(name: String)

    @Transaction
    suspend fun setDefaultLocation(name: String) {
        clearDefaultFlags()
        markDefaultByName(name)
    }

    @Query("DELETE FROM saved_locations WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT COUNT(*) FROM saved_locations")
    suspend fun count(): Int
}

@Dao
interface PanchangaCacheDao {
    @Query("SELECT * FROM cached_panchanga WHERE cacheKey = :key LIMIT 1")
    suspend fun getByKey(key: String): CachedPanchangaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedPanchangaEntity)

    @Query("DELETE FROM cached_panchanga WHERE updatedAtMs < :olderThanMs")
    suspend fun pruneOlderThan(olderThanMs: Long)

    @Query("DELETE FROM cached_panchanga")
    suspend fun clearAll()
}

@Dao
interface VratReminderDao {
    @Query("SELECT * FROM vrat_reminders WHERE enabled = 1 ORDER BY notifyAtMs ASC")
    fun observeEnabledReminders(): Flow<List<VratReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VratReminderEntity)

    @Query("DELETE FROM vrat_reminders WHERE reminderKey = :key")
    suspend fun delete(key: String)
}

@Dao
interface NavtaraPersistenceDao {
    @Query("SELECT * FROM navtara_profiles ORDER BY isDefault DESC, updatedAtMs DESC")
    fun observeProfiles(): Flow<List<NavtaraProfileEntity>>

    @Query("SELECT * FROM navtara_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): NavtaraProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: NavtaraProfileEntity): Long

    @Query("UPDATE navtara_profiles SET isDefault = 0")
    suspend fun clearDefaultProfileFlags()

    @Query("UPDATE navtara_profiles SET isDefault = 1 WHERE id = :id")
    suspend fun markDefaultProfile(id: Long)

    @Transaction
    suspend fun setDefaultProfile(id: Long) {
        clearDefaultProfileFlags()
        markDefaultProfile(id)
    }

    @Query("DELETE FROM navtara_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)

    @Query("SELECT * FROM navtara_daily_feedback WHERE profileId = :profileId ORDER BY dateIso ASC")
    fun observeFeedbacksForProfile(profileId: Long): Flow<List<NavtaraFeedbackEntity>>

    @Query("SELECT * FROM navtara_daily_feedback WHERE profileId = :profileId AND dateIso BETWEEN :startIso AND :endIso")
    suspend fun getFeedbacksInRange(profileId: Long, startIso: String, endIso: String): List<NavtaraFeedbackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeedback(entity: NavtaraFeedbackEntity)

    @Query("DELETE FROM navtara_daily_feedback WHERE profileId = :profileId AND dateIso = :dateIso")
    suspend fun deleteFeedback(profileId: Long, dateIso: String)

    @Query("SELECT * FROM saved_navtara_reports ORDER BY createdAtMs DESC")
    fun observeSavedReports(): Flow<List<SavedNavtaraReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReport(entity: SavedNavtaraReportEntity): Long

    @Query("DELETE FROM saved_navtara_reports WHERE id = :reportId")
    suspend fun deleteReport(reportId: Long)
}

@Dao
interface SavedKundliDao {
    @Query("SELECT * FROM saved_kundli_charts ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<SavedKundliChartEntity>>

    @Query("SELECT COUNT(*) FROM saved_kundli_charts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedKundliChartEntity): Long

    @Query("DELETE FROM saved_kundli_charts WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface UserAccessCacheDao {
    @Query("SELECT * FROM cached_user_access WHERE userId = :userId LIMIT 1")
    fun observeByUserId(userId: String): Flow<com.mahavtaar.panchanga.database.entity.CachedUserAccessEntity?>

    @Query("SELECT * FROM cached_user_access ORDER BY updatedAtMs DESC LIMIT 1")
    fun observeMostRecent(): Flow<com.mahavtaar.panchanga.database.entity.CachedUserAccessEntity?>

    @Query("SELECT * FROM cached_user_access WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): com.mahavtaar.panchanga.database.entity.CachedUserAccessEntity?

    @Query("SELECT * FROM cached_user_access ORDER BY updatedAtMs DESC LIMIT 1")
    suspend fun getMostRecent(): com.mahavtaar.panchanga.database.entity.CachedUserAccessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: com.mahavtaar.panchanga.database.entity.CachedUserAccessEntity)

    @Query("DELETE FROM cached_user_access WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("DELETE FROM cached_user_access")
    suspend fun clearAll()
}
