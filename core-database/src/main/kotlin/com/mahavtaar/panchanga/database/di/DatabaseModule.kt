package com.mahavtaar.panchanga.database.di

import android.content.Context
import androidx.room.Room
import com.mahavtaar.panchanga.core.access.UserAccessCacheService
import com.mahavtaar.panchanga.database.PanchangaDatabase
import com.mahavtaar.panchanga.database.access.RoomUserAccessCacheService
import com.mahavtaar.panchanga.database.dao.LocationDao
import com.mahavtaar.panchanga.database.dao.NavtaraPersistenceDao
import com.mahavtaar.panchanga.database.dao.NotificationRuleDao
import com.mahavtaar.panchanga.database.dao.PanchangaCacheDao
import com.mahavtaar.panchanga.database.dao.SavedKundliDao
import com.mahavtaar.panchanga.database.dao.ScheduledAlertDao
import com.mahavtaar.panchanga.database.dao.UserAccessCacheDao
import com.mahavtaar.panchanga.database.dao.VoiceRecordingDao
import com.mahavtaar.panchanga.database.dao.VratReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PanchangaDatabase =
        Room.databaseBuilder(
            context,
            PanchangaDatabase::class.java,
            "mahavtaar_panchanga.db"
        ).addMigrations(
            PanchangaDatabase.MIGRATION_2_3,
            PanchangaDatabase.MIGRATION_3_4,
            PanchangaDatabase.MIGRATION_4_5
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideLocationDao(db: PanchangaDatabase): LocationDao = db.locationDao()

    @Provides
    fun providePanchangaCacheDao(db: PanchangaDatabase): PanchangaCacheDao = db.panchangaCacheDao()

    @Provides
    fun provideVratReminderDao(db: PanchangaDatabase): VratReminderDao = db.vratReminderDao()

    @Provides
    fun provideNotificationRuleDao(db: PanchangaDatabase): NotificationRuleDao = db.notificationRuleDao()

    @Provides
    fun provideVoiceRecordingDao(db: PanchangaDatabase): VoiceRecordingDao = db.voiceRecordingDao()

    @Provides
    fun provideScheduledAlertDao(db: PanchangaDatabase): ScheduledAlertDao = db.scheduledAlertDao()

    @Provides
    fun provideNavtaraPersistenceDao(db: PanchangaDatabase): NavtaraPersistenceDao = db.navtaraPersistenceDao()

    @Provides
    fun provideSavedKundliDao(db: PanchangaDatabase): SavedKundliDao = db.savedKundliDao()

    @Provides
    fun provideUserAccessCacheDao(db: PanchangaDatabase): UserAccessCacheDao = db.userAccessCacheDao()

    @Provides
    @Singleton
    fun provideUserAccessCacheService(
        impl: RoomUserAccessCacheService
    ): UserAccessCacheService = impl
}
