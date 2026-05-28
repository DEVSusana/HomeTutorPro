package com.devsusana.hometutorpro.presentation.premium

import android.app.Activity
import com.android.billingclient.api.BillingFlowParams
import com.devsusana.hometutorpro.data.billing.BillingManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Presentation-layer implementation of [BillingLauncher] that bridges the
 * UI ([Activity]) with the data-layer [BillingManager].
 *
 * This class exists to keep `Activity` references out of the data layer,
 * satisfying Clean Architecture boundary rules. It reads the cached
 * [com.android.billingclient.api.ProductDetails] from [BillingManager] and
 * launches the Google Play billing flow on the provided [Activity].
 */
@Singleton
class PlayBillingLauncher @Inject constructor(
    private val billingManager: BillingManager
) : BillingLauncher {

    /**
     * Launches the Play Store premium purchase flow.
     *
     * Retrieves the last fetched [com.android.billingclient.api.ProductDetails]
     * from [BillingManager], builds [BillingFlowParams], and starts the billing flow
     * on the given [activity].
     *
     * @param activity The host activity required by the Google Play Billing Library.
     */
    override fun launchPremiumPurchase(activity: Activity) {
        val details = billingManager.lastProductDetails ?: return
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(
                            details.subscriptionOfferDetails
                                ?.firstOrNull()
                                ?.offerToken
                                .orEmpty()
                        )
                        .build()
                )
            )
            .build()
        billingManager.billingClient.launchBillingFlow(activity, flowParams)
    }
}
