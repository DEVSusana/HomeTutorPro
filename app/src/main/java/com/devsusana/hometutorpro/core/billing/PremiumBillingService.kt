package com.devsusana.hometutorpro.core.billing

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for premium billing operations.
 */
interface PremiumBillingService {
    val isPremium: StateFlow<Boolean>

    suspend fun getPremiumProduct(): PremiumProduct?

    fun launchPremiumPurchase(launcher: (Any, Any) -> Unit)
}
