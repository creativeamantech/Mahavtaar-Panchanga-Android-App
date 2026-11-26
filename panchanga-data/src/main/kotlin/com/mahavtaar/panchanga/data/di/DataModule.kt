package com.mahavtaar.panchanga.data.di

import com.mahavtaar.panchanga.astronomy.DrikPanchangaEngine
import com.mahavtaar.panchanga.core.AppDispatchers
import com.mahavtaar.panchanga.core.DefaultAppDispatchers
import com.mahavtaar.panchanga.data.backup.DefaultDriveHttpExecutor
import com.mahavtaar.panchanga.data.backup.DriveHttpExecutor
import com.mahavtaar.panchanga.data.preferences.UserPreferencesRepositoryImpl
import com.mahavtaar.panchanga.data.repository.AstrologyRepositoryImpl
import com.mahavtaar.panchanga.data.repository.LocationRepositoryImpl
import com.mahavtaar.panchanga.data.repository.NotificationStudioRepositoryImpl
import com.mahavtaar.panchanga.data.repository.PanchangaRepositoryImpl
import com.mahavtaar.panchanga.domain.repository.AstrologyRepository
import com.mahavtaar.panchanga.domain.repository.LocationRepository
import com.mahavtaar.panchanga.domain.repository.NotificationStudioRepository
import com.mahavtaar.panchanga.domain.repository.PanchangaRepository
import com.mahavtaar.panchanga.domain.repository.UserPreferencesRepository
import com.mahavtaar.panchanga.domain.usecase.GetDailyPanchangaUseCase
import com.mahavtaar.panchanga.domain.usecase.GetLiveHoraUseCase
import com.mahavtaar.panchanga.domain.usecase.GetLiveSwaraYogaUseCase
import com.mahavtaar.panchanga.domain.usecase.GetMonthlyCalendarUseCase
import com.mahavtaar.panchanga.domain.usecase.SearchCitiesUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindModule {

    @Binds
    @Singleton
    abstract fun bindPanchangaRepository(impl: PanchangaRepositoryImpl): PanchangaRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAstrologyRepository(impl: AstrologyRepositoryImpl): AstrologyRepository

    @Binds
    @Singleton
    abstract fun bindNotificationStudioRepository(impl: NotificationStudioRepositoryImpl): NotificationStudioRepository

    @Binds
    @Singleton
    abstract fun bindDriveHttpExecutor(impl: DefaultDriveHttpExecutor): DriveHttpExecutor
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvideModule {

    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = DefaultAppDispatchers

    @Provides
    @Singleton
    fun provideDrikPanchangaEngine(): DrikPanchangaEngine = DrikPanchangaEngine()

    @Provides
    fun provideGetDailyPanchangaUseCase(repo: PanchangaRepository): GetDailyPanchangaUseCase =
        GetDailyPanchangaUseCase(repo)

    @Provides
    fun provideGetMonthlyCalendarUseCase(repo: PanchangaRepository): GetMonthlyCalendarUseCase =
        GetMonthlyCalendarUseCase(repo)

    @Provides
    fun provideGetLiveHoraUseCase(repo: PanchangaRepository): GetLiveHoraUseCase =
        GetLiveHoraUseCase(repo)

    @Provides
    fun provideGetLiveSwaraYogaUseCase(repo: PanchangaRepository): GetLiveSwaraYogaUseCase =
        GetLiveSwaraYogaUseCase(repo)

    @Provides
    fun provideSearchCitiesUseCase(repo: LocationRepository): SearchCitiesUseCase =
        SearchCitiesUseCase(repo)
}
