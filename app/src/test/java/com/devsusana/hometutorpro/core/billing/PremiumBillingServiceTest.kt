package com.devsusana.hometutorpro.core.billing

import app.cash.turbine.test
import com.android.billingclient.api.*
import com.devsusana.hometutorpro.data.billing.BillingManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying [PremiumBillingService] contract and its concrete implementation [BillingManager]
 * using MockK and Turbine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PremiumBillingServiceTest {

    private val billingClientBuilder = mockk<BillingClient.Builder>(relaxed = true)
    private val billingClient = mockk<BillingClient>(relaxed = true)

    @Before
    fun setUp() {
        every { billingClientBuilder.setListener(any()) } returns billingClientBuilder
        every { billingClientBuilder.build() } returns billingClient
    }

    /**
     * Test state emission of 'isPremium' using Turbine to verify flow updates.
     */
    @Test
    fun testIsPremiumFlowStateEmission() = runTest {
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
     * Test successful retrieval of 'getPremiumProduct' by mocking the billing client response.
     */
    @Test
    fun testGetPremiumProduct_successfulRetrieval() = runTest {
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
     * Test failure/null scenarios for 'getPremiumProduct'.
     */
    @Test
    fun testGetPremiumProduct_failureReturnsNull() = runTest {
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
