package com.devsusana.hometutorpro.presentation.premium

import android.app.Activity
import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.core.billing.PremiumProduct
import com.devsusana.hometutorpro.domain.usecases.IGetPremiumProductUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PremiumPurchaseViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PremiumPurchaseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val billingService: PremiumBillingService = mockk()
    private val billingLauncher: BillingLauncher = mockk(relaxed = true)
    private val getPremiumProductUseCase: IGetPremiumProductUseCase = mockk()
    private val isPremiumFlow = MutableStateFlow(false)

    private val sampleProduct = PremiumProduct(
        productId = "premium_lifetime",
        formattedPrice = "$4.99"
    )

    private lateinit var viewModel: PremiumPurchaseViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { billingService.isPremium } returns isPremiumFlow
        coEvery { getPremiumProductUseCase() } returns sampleProduct
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads premium product and sets loading to false`() = runTest {
        viewModel = PremiumPurchaseViewModel(billingService, billingLauncher, getPremiumProductUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(sampleProduct, viewModel.premiumProduct.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `isPremium exposes billingService isPremium flow`() = runTest {
        viewModel = PremiumPurchaseViewModel(billingService, billingLauncher, getPremiumProductUseCase)
        assertFalse(viewModel.isPremium.value)

        isPremiumFlow.value = true
        assertTrue(viewModel.isPremium.value)
    }

    @Test
    fun `buyPremium delegates to billingLauncher launchPremiumPurchase`() = runTest {
        viewModel = PremiumPurchaseViewModel(billingService, billingLauncher, getPremiumProductUseCase)
        val mockActivity: Activity = mockk()

        viewModel.buyPremium(mockActivity)

        verify(exactly = 1) { billingLauncher.launchPremiumPurchase(mockActivity) }
    }
}
