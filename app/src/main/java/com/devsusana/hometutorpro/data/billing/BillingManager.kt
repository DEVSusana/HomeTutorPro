package com.devsusana.hometutorpro.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.PendingPurchasesParams
import com.devsusana.hometutorpro.BuildConfig
import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.core.billing.PremiumProduct
import com.devsusana.hometutorpro.core.settings.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) : PurchasesUpdatedListener, PremiumBillingService {

    private val _realPremium = MutableStateFlow(false)
    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var lastProductDetails: ProductDetails? = null

    init {
        startConnection()
        // Combine real premium status with debug preference
        CoroutineScope(Dispatchers.Main).launch {
            combine(_realPremium, settingsManager.isDebugPremiumFlow) { real, debug ->
                if (BuildConfig.DEBUG) {
                    // In DEBUG, strict control via toggle to allow testing non-premium state
                    debug
                } else {
                    // In RELEASE, use real premium status
                    real
                }
            }.collect { combined ->
                _isPremium.value = combined
            }
        }
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry connection
                // For simplicity, we just log or try again later
            }
        })
    }

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

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        }
    }

    private fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
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

    override fun launchPremiumPurchase(activity: Activity) {
        val details = lastProductDetails ?: return
        launchPurchaseFlow(activity, details)
    }

    companion object {
        private const val PREMIUM_PRODUCT_ID = "hometutorpro_premium"
    }
}
