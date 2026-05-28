package com.devsusana.hometutorpro.data.billing

import com.android.billingclient.api.*
import com.android.billingclient.api.PendingPurchasesParams
import android.app.Activity
import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.core.billing.PremiumProduct
import com.devsusana.hometutorpro.presentation.premium.BillingLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implementation of [PremiumBillingService] that manages in-app billing using the Google Play Billing Library.
 * Handles product queries, purchase flows, and subscription state persistence.
 */
@Singleton
class BillingManager @Inject constructor(
    billingClientBuilder: BillingClient.Builder
) : PurchasesUpdatedListener, PremiumBillingService, BillingLauncher {

    private val _realPremium = MutableStateFlow(false)

    /** Exposed Flow containing the active premium/subscription status. */
    override val isPremium: StateFlow<Boolean> = _realPremium.asStateFlow()

    private val billingClient = billingClientBuilder
        .setListener(this)
        .build()

    private var lastProductDetails: ProductDetails? = null

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Connection lost. Retries are handled during user-triggered billing actions.
            }
        })
    }

    /**
     * Queries the Google Play Store for existing purchases associated with the user's account.
     */
    fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        var hasPremium = false
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                purchase.products.contains(PREMIUM_PRODUCT_ID)
            ) {
                hasPremium = true
            }
        }
        _realPremium.value = hasPremium
    }

    /**
     * Callback triggered by the Google Play Billing Library when any purchase updates occur.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        }
    }

    /**
     * Fetches pricing and details for a specific product ID from the Google Play Store.
     * @param productId The ID of the product to query.
     * @param onResult Callback invoked with the [ProductDetails] if found, or null otherwise.
     */
    private fun queryProductDetails(productId: String, onResult: (ProductDetails?) -> Unit) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onResult(productDetailsList.firstOrNull())
            } else {
                onResult(null)
            }
        }
    }

    /**
     * Suspends until product details are fetched and returns a [PremiumProduct] mapping.
     * @return The formatted product information, or null if failed.
     */
    override suspend fun getPremiumProduct(): PremiumProduct? {
        return suspendCancellableCoroutine { continuation ->
            queryProductDetails(PREMIUM_PRODUCT_ID) { details ->
                if (!continuation.isActive) return@queryProductDetails
                lastProductDetails = details
                val result = details?.let {
                    val price = it.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases
                        ?.pricingPhaseList
                        ?.firstOrNull()
                        ?.formattedPrice
                        ?: ""
                    PremiumProduct(
                        productId = PREMIUM_PRODUCT_ID,
                        formattedPrice = price
                    )
                }
                continuation.resume(result)
            }
        }
    }

    /**
     * Triggers the purchase flow for the cached premium product details.
     * @param activity The host activity.
     */
    override fun launchPremiumPurchase(activity: Activity) {
        val details = lastProductDetails ?: return
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
        billingClient.launchBillingFlow(activity, flowParams)
    }

    companion object {
        private const val PREMIUM_PRODUCT_ID = "hometutorpro_premium"
    }
}
