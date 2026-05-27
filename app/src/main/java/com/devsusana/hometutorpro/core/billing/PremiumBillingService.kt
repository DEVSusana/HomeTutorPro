package com.devsusana.hometutorpro.core.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for premium billing operations.
 */
interface PremiumBillingService {
    val isPremium: StateFlow<Boolean>

    suspend fun getPremiumProduct(): PremiumProduct?

    fun launchPremiumPurchase(launcher: (BillingClient, BillingFlowParams) -> Unit)
}
