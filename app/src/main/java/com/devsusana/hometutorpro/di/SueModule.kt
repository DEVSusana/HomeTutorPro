package com.devsusana.hometutorpro.di

import com.devsusana.hometutorpro.data.repository.AgentContextRepositoryImpl
import com.devsusana.hometutorpro.data.repository.MediaPipeModelRepository
import com.devsusana.hometutorpro.data.repository.SpeechServiceImpl
import com.devsusana.hometutorpro.domain.repository.AgentContextRepository
import com.devsusana.hometutorpro.domain.repository.InferenceRepository
import com.devsusana.hometutorpro.domain.repository.SpeechService
import com.devsusana.hometutorpro.domain.usecases.IManageScheduleForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IQuerySchedulesForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IQueryStudentsForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.ManageScheduleForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.QuerySchedulesForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.implementations.QueryStudentsForAgentUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing all dependencies for the Sue AI agent feature.
 *
 * Includes repository bindings and use case bindings for the agent's
 * data access layer.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SueModule {

    @Binds
    @Singleton
    abstract fun bindAgentContextRepository(
        impl: AgentContextRepositoryImpl
    ): AgentContextRepository

    @Binds
    @Singleton
    abstract fun bindInferenceRepository(
        impl: MediaPipeModelRepository
    ): InferenceRepository

    @Binds
    @Singleton
    abstract fun bindSpeechService(
        impl: SpeechServiceImpl
    ): SpeechService

    @Binds
    abstract fun bindQueryStudentsForAgentUseCase(
        impl: QueryStudentsForAgentUseCase
    ): IQueryStudentsForAgentUseCase

    @Binds
    abstract fun bindQuerySchedulesForAgentUseCase(
        impl: QuerySchedulesForAgentUseCase
    ): IQuerySchedulesForAgentUseCase

    @Binds
    abstract fun bindManageScheduleForAgentUseCase(
        impl: ManageScheduleForAgentUseCase
    ): IManageScheduleForAgentUseCase
}
