package com.mahavtaar.panchanga.di

import com.mahavtaar.panchanga.domain.notifications.AlertEnginePort
import com.mahavtaar.panchanga.notifications.PanchangaNotificationScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppNotificationModule {

    @Binds
    @Singleton
    abstract fun bindAlertEnginePort(
        impl: PanchangaNotificationScheduler
    ): AlertEnginePort
}
