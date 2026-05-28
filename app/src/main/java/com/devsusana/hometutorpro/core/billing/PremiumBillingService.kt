package com.devsusana.hometutorpro.core.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Defines the contract for premium billing and subscription operations.
 *
 * This service is responsible for providing the current subscription status
 * and retrieving product information from the billing provider. Implementations
 * handle the connection to the billing backend (e.g., Google Play Billing Library).
 */
interface PremiumBillingService {
    /**
     * Represents the user's current premium or subscription status.
     * Emits `true` if the user has an active premium entitlement, `false` otherwise.
     * Implementations must ensure thread-safe emission via [StateFlow].
     */
    val isPremium: StateFlow<Boolean>

    /**
     * Retrieves the premium product details from the billing provider.
     *
     * @return The [PremiumProduct] containing product ID and formatted price,
     *         or `null` if the product could not be fetched or does not exist.
     */
    suspend fun getPremiumProduct(): PremiumProduct?
}
