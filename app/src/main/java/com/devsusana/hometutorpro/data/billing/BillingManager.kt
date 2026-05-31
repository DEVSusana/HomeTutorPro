package com.devsusana.hometutorpro.data.billing

import com.android.billingclient.api.*
import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.core.billing.PremiumProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Data-layer implementation of [PremiumBillingService] that manages in-app billing
 * using the Google Play Billing Library.
 *
 * Handles product queries, purchase status tracking, and subscription state persistence.
 * This class does **not** interact with any UI components; the purchase flow launch
 * is delegated to a presentation-layer [com.devsusana.hometutorpro.presentation.premium.BillingLauncher].
 */
@Singleton
class BillingManager @Inject constructor(
    billingClientBuilder: BillingClient.Builder
) : PurchasesUpdatedListener, PremiumBillingService {

    private val _realPremium = MutableStateFlow(false)

    /** Exposed Flow containing the active premium/subscription status. */
    override val isPremium: StateFlow<Boolean> = _realPremium.asStateFlow()

    private val billingClient: BillingClient = billingClientBuilder
        .setListener(this)
        .build()

    private var lastProductDetails: ProductDetails? = null

    /**
     * Launches the Google Play billing flow for the premium subscription using cached product details.
     *
     * @param activity The host activity required by the Google Play Billing Library.
     */
    fun launchPurchaseFlow(activity: android.app.Activity) {
        val details = lastProductDetails ?: return
        launchPurchaseFlow(activity, details)
    }

    /**
     * Launches the Google Play billing flow for the specified product.
     *
     * @param activity The host activity required by the Google Play Billing Library.
     * @param productDetails The Google Play product details to purchase.
     */
    fun launchPurchaseFlow(activity: android.app.Activity, productDetails: ProductDetails) {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(
                            productDetails.subscriptionOfferDetails
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

    init {
        startConnection()
    }

    /**
     * Establishes a connection to the Google Play Billing service.
     * On successful setup, triggers [queryPurchases] to restore existing entitlements.
     */
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

    /**
     * Evaluates a list of purchases and updates the premium status accordingly.
     *
     * @param purchases The list of [Purchase] objects returned by the Billing Library.
     */
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
     *
     * @param billingResult The result of the billing operation.
     * @param purchases The list of updated purchases, or null if none.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        }
    }

    /**
     * Fetches pricing and details for a specific product ID from the Google Play Store.
     *
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
     *
     * @return The formatted product information, or null if the query failed.
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

    companion object {
        /** The Google Play product ID for the premium subscription. */
        internal const val PREMIUM_PRODUCT_ID = "hometutorpro_premium"
    }
}
