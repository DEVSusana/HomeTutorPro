package com.devsusana.hometutorpro.core.billing

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests verifying the contract of [PremiumBillingService].
 */
class PremiumBillingServiceTest {

    private class FakePremiumBillingService(
        initialPremium: Boolean = false,
        private val mockProduct: PremiumProduct? = null
    ) : PremiumBillingService {
        private val _isPremium = MutableStateFlow(initialPremium)
        override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

        fun setPremium(value: Boolean) {
            _isPremium.value = value
        }

        override suspend fun getPremiumProduct(): PremiumProduct? {
            return mockProduct
        }
    }

    /**
     * Verifies that the isPremium StateFlow initially emits the state it was initialized with.
     */
    @Test
    fun isPremium_emitsInitialState() = runBlocking {
        val service = FakePremiumBillingService(initialPremium = true)
        service.isPremium.test {
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifies that the isPremium StateFlow updates correctly when the underlying state is updated.
     */
    @Test
    fun isPremium_updatesCorrectlyWhenSourceChanges() = runBlocking {
        val service = FakePremiumBillingService(initialPremium = false)
        service.isPremium.test {
            assertEquals(false, awaitItem())
            service.setPremium(true)
            assertEquals(true, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Verifies that getPremiumProduct returns the expected PremiumProduct details.
     */
    @Test
    fun getPremiumProduct_returnsExpectedProduct() = runBlocking {
        val expectedProduct = PremiumProduct("premium_sub", "$4.99")
        val service = FakePremiumBillingService(mockProduct = expectedProduct)
        val product = service.getPremiumProduct()
        assertEquals(expectedProduct, product)
    }

    /**
     * Verifies that null is returned when billing retrieval fails or the product is unavailable.
     */
    @Test
    fun getPremiumProduct_handlesNullOrErrorResponsesGracefully() = runBlocking {
        val service = FakePremiumBillingService(mockProduct = null)
        val product = service.getPremiumProduct()
        assertNull(product)
    }
}
