package com.devsusana.hometutorpro.core.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for premium billing operations.
 */
interface PremiumBillingService {
    /**
     * Emits the current premium/subscription status of the user.
     */
    val isPremium: StateFlow<Boolean>

    /**
     * Retrieves the premium product details.
     *
     * @return The [PremiumProduct] if available, null otherwise.
     */
    suspend fun getPremiumProduct(): PremiumProduct?
}
