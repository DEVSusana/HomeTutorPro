package com.devsusana.hometutorpro.di

import com.devsusana.hometutorpro.data.repository.*
import com.devsusana.hometutorpro.domain.repository.*
import com.devsusana.hometutorpro.data.remote.FirestoreRemoteDataSource
import com.devsusana.hometutorpro.data.remote.RemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSyncCoordinator(
        syncCoordinator: com.devsusana.hometutorpro.data.sync.SyncCoordinator
    ): com.devsusana.hometutorpro.domain.usecases.ISyncCoordinator

    @Binds
    @Singleton
    abstract fun bindActiveSessionRepository(
        activeSessionRepositoryImpl: ActiveSessionRepositoryImpl
    ): IActiveSessionRepository


    @Binds
    @Singleton
    abstract fun bindStudentRepository(
        studentRepositoryImpl: StudentRepositoryImpl
    ): StudentRepository

    @Binds
    @Singleton
    abstract fun bindScheduleExceptionRepository(
        scheduleExceptionRepositoryImpl: ScheduleExceptionRepositoryImpl
    ): ScheduleExceptionRepository

    @Binds
    @Singleton
    abstract fun bindResourceRepository(
        resourceRepositoryImpl: ResourceRepositoryImpl
    ): ResourceRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        subscriptionRepositoryImpl: SubscriptionRepositoryImpl
    ): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindRemoteDataSource(
        firestoreRemoteDataSource: FirestoreRemoteDataSource
    ): RemoteDataSource

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        backupRepositoryImpl: BackupRepositoryImpl
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindDateTimeProvider(
        dateTimeProviderImpl: DateTimeProviderImpl
    ): DateTimeProvider

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindDataMigrator(
        localDataMigrator: com.devsusana.hometutorpro.data.migration.LocalDataMigrator
    ): com.devsusana.hometutorpro.domain.repository.DataMigrator

    @Binds
    @Singleton
    abstract fun bindConfigRepository(
        firebaseConfigRepositoryImpl: FirebaseConfigRepositoryImpl
    ): IConfigRepository
}

