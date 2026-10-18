package com.devsusana.hometutorpro.data.sync

import com.devsusana.hometutorpro.data.local.dao.SyncMetadataDao
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
 * Lives in the data layer as an infrastructure coordinator implementing [ISyncCoordinator].
 */
@Singleton
class SyncCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val dataSynchronizer: DataSynchronizer,
    private val syncScheduler: SyncScheduler,
    private val syncMetadataDao: SyncMetadataDao,
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

    /**
     * Clears local sync metadata and cancels scheduled sync jobs.
     */
    override suspend fun clearSyncData() {
        syncMetadataDao.deleteAllMetadata()
        syncScheduler.cancelAllSync()
    }
}
