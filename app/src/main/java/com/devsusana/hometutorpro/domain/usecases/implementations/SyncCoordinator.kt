package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.data.billing.BillingManager
import com.devsusana.hometutorpro.data.sync.DataSynchronizer
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates synchronization logic by observing Auth and Billing states.
 * Replaces the tight coupling previously present in AuthRepositoryImpl.
 */
@Singleton
class SyncCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val billingManager: BillingManager,
    private val dataSynchronizer: DataSynchronizer,
    private val syncScheduler: SyncScheduler,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    /**
     * Starts observing authentication and billing state changes to trigger synchronization
     * dynamically, freeing AuthRepository from knowing about synchronization logic.
     */
    fun startObserving() {
        applicationScope.launch {
            combine(authRepository.currentUser, billingManager.isPremium) { user, isPremium ->
                Pair(user, isPremium)
            }.collect { (user, isPremium) ->
                if (user != null && isPremium) {
                    try {
                        dataSynchronizer.performSync()
                    } catch (e: Exception) {
                        syncScheduler.scheduleSyncNow()
                    }
                }
            }
        }
    }
}
