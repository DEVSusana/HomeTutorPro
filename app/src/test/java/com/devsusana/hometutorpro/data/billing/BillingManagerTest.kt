package com.devsusana.hometutorpro.data.billing

import app.cash.turbine.test
import com.android.billingclient.api.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BillingManager] validating Play Billing client integrations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagerTest {

    private val billingClientBuilder = mockk<BillingClient.Builder>(relaxed = true)
    private val billingClient = mockk<BillingClient>(relaxed = true)

    /**
     * Prepares standard stubbing for the BillingClient builder.
     */
    @Before
    fun setUp() {
        every { billingClientBuilder.setListener(any()) } returns billingClientBuilder
        every { billingClientBuilder.build() } returns billingClient
    }

    /**
     * Verifies that the initial premium status is false.
     */
    @Test
    fun isPremium_initiallyFalse() {
        val manager = BillingManager(billingClientBuilder)
        assertFalse(manager.isPremium.value)
    }

    /**
     * Verifies that isPremium updates to true when processPurchases receives a valid premium purchase.
     */
    @Test
    fun processPurchases_withValidPremiumPurchase_updatesIsPremiumToTrue() = runTest {
        val manager = BillingManager(billingClientBuilder)

        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.products } returns listOf(BillingManager.PREMIUM_PRODUCT_ID)

        val purchasesList = listOf(purchase)
        
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK

        manager.isPremium.test {
            assertEquals(false, awaitItem())
            manager.onPurchasesUpdated(billingResult, purchasesList)
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifies that isPremium remains false when empty or mismatched purchases are received.
     */
    @Test
    fun processPurchases_withEmptyOrMismatchedPurchases_isPremiumRemainsFalse() = runTest {
        val manager = BillingManager(billingClientBuilder)

        val purchase = mockk<Purchase>()
        every { purchase.purchaseState } returns Purchase.PurchaseState.PURCHASED
        every { purchase.products } returns listOf("some_other_product")

        val purchasesList = listOf(purchase)
        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK

        manager.isPremium.test {
            assertEquals(false, awaitItem())
            manager.onPurchasesUpdated(billingResult, purchasesList)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifies that successful setup triggers queryPurchasesAsync.
     */
    @Test
    fun billingSetupFinished_triggersQueryPurchases() {
        val listenerSlot = slot<BillingClientStateListener>()
        every { billingClient.startConnection(capture(listenerSlot)) } just Runs

        val manager = BillingManager(billingClientBuilder)

        val billingResult = mockk<BillingResult>()
        every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK

        listenerSlot.captured.onBillingSetupFinished(billingResult)

        verify { billingClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any<PurchasesResponseListener>()) }
    }

    /**
     * Verifies that getPremiumProduct parses ProductDetails and PricingPhase successfully.
     */
    @Test
    fun getPremiumProduct_successfulRetrieval() = runTest {
        val manager = BillingManager(billingClientBuilder)

        val querySlot = slot<QueryProductDetailsParams>()
        val callbackSlot = slot<ProductDetailsResponseListener>()

        every { 
            billingClient.queryProductDetailsAsync(capture(querySlot), capture(callbackSlot)) 
        } answers {
            val billingResult = mockk<BillingResult>()
            every { billingResult.responseCode } returns BillingClient.BillingResponseCode.OK
            
            val mockDetails = mockk<ProductDetails>()
            every { mockDetails.productId } returns BillingManager.PREMIUM_PRODUCT_ID
            
            val pricingPhase = mockk<ProductDetails.PricingPhase>()
            every { pricingPhase.formattedPrice } returns "$4.99"
            
            val pricingPhases = mockk<ProductDetails.PricingPhases>()
            every { pricingPhases.pricingPhaseList } returns listOf(pricingPhase)
            
            val offerDetails = mockk<ProductDetails.SubscriptionOfferDetails>()
            every { offerDetails.pricingPhases } returns pricingPhases
            every { mockDetails.subscriptionOfferDetails } returns listOf(offerDetails)
            
            callbackSlot.captured.onProductDetailsResponse(billingResult, listOf(mockDetails))
        }

        val product = manager.getPremiumProduct()
        assertNotNull(product)
        assertEquals(BillingManager.PREMIUM_PRODUCT_ID, product?.productId)
        assertEquals("$4.99", product?.formattedPrice)
    }

    /**
     * Verifies that getPremiumProduct returns null if the response is unsuccessful.
     */
    @Test
    fun getPremiumProduct_nullOrErrorResponse_returnsNull() = runTest {
        val manager = BillingManager(billingClientBuilder)

        val querySlot = slot<QueryProductDetailsParams>()
        val callbackSlot = slot<ProductDetailsResponseListener>()

        every { 
            billingClient.queryProductDetailsAsync(capture(querySlot), capture(callbackSlot)) 
        } answers {
            val billingResult = mockk<BillingResult>()
            every { billingResult.responseCode } returns BillingClient.BillingResponseCode.ERROR
            
            callbackSlot.captured.onProductDetailsResponse(billingResult, emptyList())
        }

        val product = manager.getPremiumProduct()
        assertNull(product)
    }
}
