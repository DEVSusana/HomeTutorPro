package com.devsusana.hometutorpro.di

import android.content.Context
import com.devsusana.hometutorpro.data.security.SecureAuthManager
import com.devsusana.hometutorpro.core.auth.CryptographyProvider
import com.devsusana.hometutorpro.domain.auth.PasswordHasher
import com.devsusana.hometutorpro.data.security.AndroidCryptographyProvider
import com.devsusana.hometutorpro.data.security.Pbkdf2PasswordHasher
import com.devsusana.hometutorpro.data.local.AppDatabase
import com.devsusana.hometutorpro.data.local.SupportFactoryHelper
import com.devsusana.hometutorpro.data.local.migrations.DatabaseMigrations
import com.devsusana.hometutorpro.data.local.dao.*
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val factory = SupportFactoryHelper.createFactory(context)
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "hometutorpro.db"
        )
            .openHelperFactory(factory)
            .addMigrations(
                DatabaseMigrations.MIGRATION_4_5,
                DatabaseMigrations.MIGRATION_5_6,
                DatabaseMigrations.MIGRATION_6_7,
                DatabaseMigrations.MIGRATION_7_8,
                DatabaseMigrations.MIGRATION_8_9
            )
            .build()
    }

    @Provides
    fun provideStudentDao(database: AppDatabase): StudentDao {
        return database.studentDao()
    }

    @Provides
    fun provideScheduleDao(database: AppDatabase): ScheduleDao {
        return database.scheduleDao()
    }

    @Provides
    fun provideScheduleExceptionDao(database: AppDatabase): ScheduleExceptionDao {
        return database.scheduleExceptionDao()
    }

    @Provides
    fun provideResourceDao(database: AppDatabase): ResourceDao {
        return database.resourceDao()
    }

    @Provides
    fun provideSyncMetadataDao(database: AppDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }

    @Provides
    fun provideSharedResourceDao(database: AppDatabase): SharedResourceDao {
        return database.sharedResourceDao()
    }

    @Provides
    fun provideAgentContextDao(database: AppDatabase): AgentContextDao {
        return database.agentContextDao()
    }

    @Provides
    @Singleton
    fun provideCryptographyProvider(): CryptographyProvider {
        return AndroidCryptographyProvider()
    }

    @Provides
    @Singleton
    fun providePasswordHasher(): PasswordHasher {
        return Pbkdf2PasswordHasher()
    }

    @Provides
    @Singleton
    fun provideSecureAuthManager(
        @ApplicationContext context: Context,
        cryptographyProvider: CryptographyProvider,
        passwordHasher: PasswordHasher
    ): SecureAuthManager {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secure_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return SecureAuthManager(sharedPreferences, cryptographyProvider, passwordHasher)
    }
}
