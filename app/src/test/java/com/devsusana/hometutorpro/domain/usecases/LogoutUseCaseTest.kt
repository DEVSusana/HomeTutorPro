package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.LogoutUseCase
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LogoutUseCase.
 * 
 * Tests user logout functionality.
 */
class LogoutUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var logoutUseCase: LogoutUseCase

    @Before
    fun setup() {
        authRepository = mockk(relaxed = true)
        logoutUseCase = LogoutUseCase(authRepository)
    }

    @Test
    fun `invoke should call repository logout`() = runTest {
        // When
        logoutUseCase()

        // Then
        coVerify(exactly = 1) { authRepository.logout() }
    }

    @Test
    fun `invoke should call repository logout multiple times when called multiple times`() = runTest {
        // When
        logoutUseCase()
        logoutUseCase()
        logoutUseCase()

        // Then
        coVerify(exactly = 3) { authRepository.logout() }
    }
}
