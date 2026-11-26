package com.mahavtaar.panchanga.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mahavtaar.panchanga.database.dao.LocationDao
import com.mahavtaar.panchanga.database.dao.NavtaraPersistenceDao
import com.mahavtaar.panchanga.database.dao.NotificationRuleDao
import com.mahavtaar.panchanga.database.dao.PanchangaCacheDao
import com.mahavtaar.panchanga.database.dao.SavedKundliDao
import com.mahavtaar.panchanga.database.dao.ScheduledAlertDao
import com.mahavtaar.panchanga.database.dao.UserAccessCacheDao
import com.mahavtaar.panchanga.database.dao.VoiceRecordingDao
import com.mahavtaar.panchanga.database.dao.VratReminderDao
import com.mahavtaar.panchanga.database.entity.CachedPanchangaEntity
import com.mahavtaar.panchanga.database.entity.CachedUserAccessEntity
import com.mahavtaar.panchanga.database.entity.NavtaraFeedbackEntity
import com.mahavtaar.panchanga.database.entity.NavtaraProfileEntity
import com.mahavtaar.panchanga.database.entity.NotificationRuleEntity
import com.mahavtaar.panchanga.database.entity.SavedKundliChartEntity
import com.mahavtaar.panchanga.database.entity.SavedLocationEntity
import com.mahavtaar.panchanga.database.entity.SavedNavtaraReportEntity
import com.mahavtaar.panchanga.database.entity.ScheduledAlertEntity
import com.mahavtaar.panchanga.database.entity.VoiceRecordingEntity
import com.mahavtaar.panchanga.database.entity.VratReminderEntity

@Database(
    entities = [
        SavedLocationEntity::class,
        CachedPanchangaEntity::class,
        VratReminderEntity::class,
        NotificationRuleEntity::class,
        VoiceRecordingEntity::class,
        ScheduledAlertEntity::class,
        NavtaraProfileEntity::class,
        NavtaraFeedbackEntity::class,
        SavedNavtaraReportEntity::class,
        SavedKundliChartEntity::class,
        CachedUserAccessEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class PanchangaDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun panchangaCacheDao(): PanchangaCacheDao
    abstract fun vratReminderDao(): VratReminderDao
    abstract fun notificationRuleDao(): NotificationRuleDao
    abstract fun voiceRecordingDao(): VoiceRecordingDao
    abstract fun scheduledAlertDao(): ScheduledAlertDao
    abstract fun navtaraPersistenceDao(): NavtaraPersistenceDao
    abstract fun savedKundliDao(): SavedKundliDao
    abstract fun userAccessCacheDao(): UserAccessCacheDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scheduled_alerts ADD COLUMN eventEndTimeMs INTEGER")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_user_access` (
                        `userId` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `displayName` TEXT,
                        `roleName` TEXT NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `allowedPagesCsv` TEXT NOT NULL,
                        `allowedFeaturesCsv` TEXT NOT NULL,
                        `authorizedByUid` TEXT,
                        `sessionToken` TEXT NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_locations ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_locations ADD COLUMN isDefault INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_locations ADD COLUMN accuracyMeters REAL")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `navtara_profiles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `birthNakshatraIndex` INTEGER NOT NULL,
                        `birthPada` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `navtara_daily_feedback` (
                        `profileId` INTEGER NOT NULL,
                        `dateIso` TEXT NOT NULL,
                        `rating` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`profileId`, `dateIso`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_navtara_reports` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        `profileName` TEXT NOT NULL,
                        `birthNakshatraIndex` INTEGER NOT NULL,
                        `birthPada` INTEGER NOT NULL,
                        `startDateIso` TEXT NOT NULL,
                        `endDateIso` TEXT NOT NULL,
                        `isDateRange` INTEGER NOT NULL,
                        `locationName` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `timezone` TEXT NOT NULL,
                        `ayanamshaKey` TEXT NOT NULL,
                        `monthSystemKey` TEXT NOT NULL,
                        `auspiciousCount` INTEGER NOT NULL,
                        `mixedCount` INTEGER NOT NULL,
                        `cautionCount` INTEGER NOT NULL,
                        `neutralCount` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAtMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_kundli_charts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `birthYear` INTEGER NOT NULL,
                        `birthMonth` INTEGER NOT NULL,
                        `birthDay` INTEGER NOT NULL,
                        `birthHour` INTEGER NOT NULL,
                        `birthMinute` INTEGER NOT NULL,
                        `placeName` TEXT NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `timezone` TEXT NOT NULL,
                        `inputJson` TEXT NOT NULL,
                        `lagnaSignIndex` INTEGER NOT NULL,
                        `moonSignIndex` INTEGER NOT NULL,
                        `moonNakshatraNumber` INTEGER NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
