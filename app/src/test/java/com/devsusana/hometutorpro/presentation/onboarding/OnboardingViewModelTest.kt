package com.devsusana.hometutorpro.presentation.onboarding

import com.devsusana.hometutorpro.domain.usecases.ISetOnboardingCompletedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val setOnboardingCompletedUseCase: ISetOnboardingCompletedUseCase = mockk()
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { setOnboardingCompletedUseCase(any()) } returns Unit
        viewModel = OnboardingViewModel(setOnboardingCompletedUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completeOnboarding calls setOnboardingCompletedUseCase with true`() = runTest {
        viewModel.completeOnboarding()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { setOnboardingCompletedUseCase(true) }
    }
}
