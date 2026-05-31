package com.devsusana.hometutorpro.presentation.premium

import android.app.Activity
import com.devsusana.hometutorpro.data.billing.BillingManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Presentation-layer implementation of [BillingLauncher] that bridges the
 * UI ([Activity]) with the data-layer [BillingManager].
 *
 * This class exists to keep `Activity` references out of the data layer,
 * satisfying Clean Architecture boundary rules. It delegates the purchase launch
 * to [BillingManager], keeping the presentation layer agnostic of Google Play Billing SDK details.
 */
@Singleton
class PlayBillingLauncher @Inject constructor(
    private val billingManager: BillingManager
) : BillingLauncher {

    /**
     * Launches the Play Store premium purchase flow by delegating to [BillingManager].
     *
     * @param activity The host activity required by the Google Play Billing Library.
     */
    override fun launchPremiumPurchase(activity: Activity) {
        billingManager.launchPurchaseFlow(activity)
    }
}
