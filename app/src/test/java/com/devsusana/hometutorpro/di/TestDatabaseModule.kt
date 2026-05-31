package com.devsusana.hometutorpro.di

import android.content.Context
import androidx.room.Room
import com.devsusana.hometutorpro.data.local.AppDatabase
import com.devsusana.hometutorpro.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @Provides
    fun provideStudentDao(database: AppDatabase): StudentDao = database.studentDao()

    @Provides
    fun provideScheduleDao(database: AppDatabase): ScheduleDao = database.scheduleDao()

    @Provides
    fun provideScheduleExceptionDao(database: AppDatabase): ScheduleExceptionDao = database.scheduleExceptionDao()

    @Provides
    fun provideResourceDao(database: AppDatabase): ResourceDao = database.resourceDao()

    @Provides
    fun provideSyncMetadataDao(database: AppDatabase): SyncMetadataDao = database.syncMetadataDao()

    @Provides
    fun provideSharedResourceDao(database: AppDatabase): SharedResourceDao = database.sharedResourceDao()

    @Provides
    @Singleton
    fun provideCryptographyProvider(): com.devsusana.hometutorpro.core.auth.CryptographyProvider {
        return object : com.devsusana.hometutorpro.core.auth.CryptographyProvider {
            override fun encrypt(text: String?): String = text ?: ""
            override fun decrypt(encrypted: String?): String = encrypted ?: ""
        }
    }

    @Provides
    @Singleton
    fun providePasswordHasher(): com.devsusana.hometutorpro.domain.auth.PasswordHasher {
        return object : com.devsusana.hometutorpro.domain.auth.PasswordHasher {
            override fun generateSalt(): String = "test_salt"
            override fun hashPassword(password: String, saltBase64: String): String = password + saltBase64
            override fun verifyPassword(password: String, storedHash: String, storedSalt: String): Boolean =
                (password + storedSalt) == storedHash
        }
    }

    @Provides
    @Singleton
    fun provideSecureAuthManager(
        @ApplicationContext context: Context,
        cryptographyProvider: com.devsusana.hometutorpro.core.auth.CryptographyProvider,
        passwordHasher: com.devsusana.hometutorpro.domain.auth.PasswordHasher
    ): com.devsusana.hometutorpro.data.security.SecureAuthManager {
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        return com.devsusana.hometutorpro.data.security.SecureAuthManager(
            prefs,
            cryptographyProvider,
            passwordHasher
        )
    }
}
