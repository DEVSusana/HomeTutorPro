package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.data.billing.BillingManager
import com.devsusana.hometutorpro.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer implementation of [SubscriptionRepository].
 */
@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val billingManager: BillingManager
) : SubscriptionRepository {

    override val isPremium: StateFlow<Boolean> = billingManager.isPremium

    override suspend fun checkSubscriptionStatus() {
        billingManager.queryPurchases()
    }
}
