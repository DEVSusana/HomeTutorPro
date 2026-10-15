package com.devsusana.hometutorpro.domain.usecases

/**
 * Coordinator to manage and observe background synchronization events.
 */
interface ISyncCoordinator {
    /**
     * Starts observing authentication and subscription states to trigger background sync.
     */
    fun startObserving()

    /**
     * Clears local sync metadata and cancels scheduled sync jobs (e.g. on logout).
     */
    suspend fun clearSyncData()
}
