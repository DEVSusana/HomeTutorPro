package com.devsusana.hometutorpro.di

import com.devsusana.hometutorpro.domain.usecases.IConsolidateActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.INotifyClassEndUseCase
import com.devsusana.hometutorpro.domain.usecases.IStopActiveSessionUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * Hilt EntryPoint for injecting dependencies into [ClassEndReceiver]
 * without requiring ASM bytecode transformation on the BroadcastReceiver class.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ClassEndReceiverEntryPoint {
    /** Provides the class end notification use case. */
    fun notifyClassEndUseCase(): INotifyClassEndUseCase

    /** Provides the stop active session use case. */
    fun stopActiveSessionUseCase(): IStopActiveSessionUseCase

    /** Provides the session billing consolidation use case. */
    fun consolidateActiveSessionUseCase(): IConsolidateActiveSessionUseCase

    /** Provides the application-scoped CoroutineScope. */
    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
