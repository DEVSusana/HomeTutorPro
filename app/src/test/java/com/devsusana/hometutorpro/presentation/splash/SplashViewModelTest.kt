package com.devsusana.hometutorpro.presentation.splash

import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetOnboardingCompletedUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashViewModelTest {

    private val getOnboardingCompletedUseCase = mockk<IGetOnboardingCompletedUseCase>(relaxed = true)

    @Test
    fun `isUserLoggedIn should return true when user is not null`() {
        // Given
        val user = User(uid = "123", email = "test@test.com", displayName = "Test")
        val fakeUseCase = object : IGetCurrentUserUseCase {
            override fun invoke() = MutableStateFlow(user)
        }
        val viewModel = SplashViewModel(fakeUseCase, getOnboardingCompletedUseCase)

        // When
        val result = viewModel.isUserLoggedIn()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isUserLoggedIn should return false when user is null`() {
        // Given
        val fakeUseCase = object : IGetCurrentUserUseCase {
            override fun invoke() = MutableStateFlow<User?>(null)
        }
        val viewModel = SplashViewModel(fakeUseCase, getOnboardingCompletedUseCase)

        // When
        val result = viewModel.isUserLoggedIn()

        // Then
        assertFalse(result)
    }
}
