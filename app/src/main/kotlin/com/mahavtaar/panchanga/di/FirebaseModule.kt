package com.mahavtaar.panchanga.firebase

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.mahavtaar.panchanga.R
import com.mahavtaar.panchanga.domain.repository.AccessControlRepository
import com.mahavtaar.panchanga.domain.repository.CloudAccountRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseProvideModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(
        @ApplicationContext context: Context
    ): FirebaseAuth {
        runCatching {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
        }
        return Firebase.auth
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(
        @ApplicationContext context: Context
    ): FirebaseFirestore {
        runCatching {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
        }
        val dbId = runCatching {
            context.getString(R.string.firestore_database_id).trim()
        }.getOrDefault("")
        return if (dbId.isEmpty() || dbId == "(default)") {
            FirebaseFirestore.getInstance()
        } else {
            runCatching { FirebaseFirestore.getInstance(dbId) }
                .getOrElse { FirebaseFirestore.getInstance() }
        }
    }

    @Provides
    @Singleton
    fun provideCredentialManager(
        @ApplicationContext context: Context
    ): CredentialManager = CredentialManager.create(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseBindModule {

    @Binds
    @Singleton
    abstract fun bindCloudAccountRepository(
        impl: FirestoreCloudSyncRepository
    ): CloudAccountRepository

    @Binds
    @Singleton
    abstract fun bindAccessControlRepository(
        impl: FirestoreAccessControlRepository
    ): AccessControlRepository
}
