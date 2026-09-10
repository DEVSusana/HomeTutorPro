package com.devsusana.hometutorpro.presentation.splash

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetOnboardingCompletedUseCase
import com.devsusana.hometutorpro.domain.usecases.IRestoreSessionUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashViewModelTest {

    private val getOnboardingCompletedUseCase = mockk<IGetOnboardingCompletedUseCase>(relaxed = true)
    private val restoreSessionUseCase = mockk<IRestoreSessionUseCase>()

    @Test
    fun `isUserLoggedIn should return true when user is not null`() {
        // Given
        val user = User(uid = "123", email = "test@test.com", displayName = "Test")
        val fakeUseCase = object : IGetCurrentUserUseCase {
            override fun invoke() = MutableStateFlow(user)
        }
        val viewModel = SplashViewModel(fakeUseCase, getOnboardingCompletedUseCase, restoreSessionUseCase)

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
        val viewModel = SplashViewModel(fakeUseCase, getOnboardingCompletedUseCase, restoreSessionUseCase)

        // When
        val result = viewModel.isUserLoggedIn()

        // Then
        assertFalse(result)
    }

    @Test
    fun `attemptZeroTapRestore should return true when session is successfully restored`() = runTest {
        val user = User(uid = "123", email = "test@test.com", displayName = "Test")
        val fakeCurrentUserUseCase = object : IGetCurrentUserUseCase {
            override fun invoke() = MutableStateFlow<User?>(null)
        }
        coEvery { restoreSessionUseCase() } returns Result.Success(user)

        val viewModel = SplashViewModel(fakeCurrentUserUseCase, getOnboardingCompletedUseCase, restoreSessionUseCase)
        val result = viewModel.attemptZeroTapRestore()

        assertTrue(result)
    }

    @Test
    fun `attemptZeroTapRestore should return false when session restore fails`() = runTest {
        val fakeCurrentUserUseCase = object : IGetCurrentUserUseCase {
            override fun invoke() = MutableStateFlow<User?>(null)
        }
        coEvery { restoreSessionUseCase() } returns Result.Error(DomainError.UserNotFound)

        val viewModel = SplashViewModel(fakeCurrentUserUseCase, getOnboardingCompletedUseCase, restoreSessionUseCase)
        val result = viewModel.attemptZeroTapRestore()

        assertFalse(result)
    }

    @Test
    fun `isOnboardingCompleted should return true when use case emits true`() = runTest {
        val fakeCurrentUserUseCase = object : IGetCurrentUserUseCase {
            override fun invoke() = MutableStateFlow<User?>(null)
        }
        every { getOnboardingCompletedUseCase() } returns flowOf(true)

        val viewModel = SplashViewModel(fakeCurrentUserUseCase, getOnboardingCompletedUseCase, restoreSessionUseCase)
        val result = viewModel.isOnboardingCompleted()

        assertTrue(result)
    }
}
