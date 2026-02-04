package com.devsusana.hometutorpro.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devsusana.hometutorpro.domain.repository.SubscriptionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that executes data synchronization.
 * Injected with Hilt to access DataSynchronizer.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dataSynchronizer: DataSynchronizer,
    private val subscriptionRepository: SubscriptionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Check if user is premium
        // We use first() to get the current state. 
        // Note: In a cold start background sync, this might be false if BillingClient hasn't connected yet.
        // Ideally we should wait for connection, but for now we skip to be safe.
        val isPremium = subscriptionRepository.isPremium.value
        if (!isPremium) {
            return Result.success()
        }

        return try {
            dataSynchronizer.performSync()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
