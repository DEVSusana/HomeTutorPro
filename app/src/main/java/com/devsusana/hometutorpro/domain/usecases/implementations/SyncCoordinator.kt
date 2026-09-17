package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.data.sync.DataSynchronizer
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.di.ApplicationScope
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.SubscriptionRepository
import com.devsusana.hometutorpro.domain.usecases.ISyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates synchronization logic by observing Auth and Subscription states.
 * Refactored to depend on SubscriptionRepository instead of BillingManager.
 */
@Singleton
class SyncCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val dataSynchronizer: DataSynchronizer,
    private val syncScheduler: SyncScheduler,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ISyncCoordinator {

    /**
     * Starts observing authentication and billing state changes to trigger synchronization.
     */
    override fun startObserving() {
        applicationScope.launch {
            combine(authRepository.currentUser, subscriptionRepository.isPremium) { user, isPremium ->
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
