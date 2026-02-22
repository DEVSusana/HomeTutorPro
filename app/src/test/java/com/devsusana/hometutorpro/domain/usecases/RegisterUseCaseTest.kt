package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.RegisterUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RegisterUseCase.
 * 
 * Tests user registration business logic including:
 * - Successful registration
 * - Error handling from repository
 */
class RegisterUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var registerUseCase: RegisterUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        registerUseCase = RegisterUseCase(authRepository)
    }

    @Test
    fun `invoke returns Success when repository register succeeds`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val name = "Test User"
        val expectedUser = User(uid = "123", email = email, displayName = name)
        
        coEvery { authRepository.register(email, password, name) } returns Result.Success(expectedUser)

        // When
        val result = registerUseCase(email, password, name)

        // Then
        assert(result is Result.Success)
        assertEquals(expectedUser, (result as Result.Success).data)
        coVerify(exactly = 1) { authRepository.register(email, password, name) }
    }

    @Test
    fun `invoke returns Error when repository register fails`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val name = "Test User"
        val error = DomainError.Unknown
        
        coEvery { authRepository.register(email, password, name) } returns Result.Error(error)

        // When
        val result = registerUseCase(email, password, name)

        // Then
        assert(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
        coVerify(exactly = 1) { authRepository.register(email, password, name) }
    }

    @Test
    fun `invoke returns Error when network error occurs`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val name = "Test User"
        val networkError = DomainError.NetworkError
        
        coEvery { authRepository.register(email, password, name) } returns Result.Error(networkError)

        // When
        val result = registerUseCase(email, password, name)

        // Then
        assert(result is Result.Error)
        assertEquals(networkError, (result as Result.Error).error)
    }
}
