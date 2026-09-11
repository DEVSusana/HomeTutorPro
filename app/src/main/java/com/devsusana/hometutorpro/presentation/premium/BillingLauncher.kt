package com.devsusana.hometutorpro.presentation.premium

import android.app.Activity

/**
 * Interface to trigger the Android Billing purchase flow.
 */
interface BillingLauncher {
    /**
     * Launches the Play Store premium purchase flow.
     *
     * @param activity The host activity.
     */
    fun launchPremiumPurchase(activity: Activity)
}
