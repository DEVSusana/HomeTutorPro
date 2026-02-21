package com.devsusana.hometutorpro.di

import com.devsusana.hometutorpro.data.repository.*
import com.devsusana.hometutorpro.domain.repository.*
import com.devsusana.hometutorpro.data.remote.FirestoreRemoteDataSource
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
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

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
}
