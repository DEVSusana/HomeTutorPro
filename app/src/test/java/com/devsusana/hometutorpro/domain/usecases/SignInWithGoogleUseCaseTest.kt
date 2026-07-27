package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.SignInWithGoogleUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignInWithGoogleUseCaseTest {

    private val authRepository: AuthRepository = mockk()
    private lateinit var useCase: SignInWithGoogleUseCase

    @Before
    fun setUp() {
        useCase = SignInWithGoogleUseCase(authRepository)
    }

    @Test
    fun `invoke with valid idToken returns success user`() = runTest {
        val idToken = "valid_token_123"
        val expectedUser = User("uid_123", "test@gmail.com", "Test User")

        coEvery { authRepository.signInWithGoogle(idToken) } returns Result.Success(expectedUser)

        val result = useCase(idToken)

        assertTrue(result is Result.Success)
        assertEquals(expectedUser, (result as Result.Success).data)
        coVerify(exactly = 1) { authRepository.signInWithGoogle(idToken) }
    }

    @Test
    fun `invoke when repository fails returns error`() = runTest {
        val idToken = "invalid_token"
        coEvery { authRepository.signInWithGoogle(idToken) } returns Result.Error(DomainError.Unknown)

        val result = useCase(idToken)

        assertTrue(result is Result.Error)
        assertEquals(DomainError.Unknown, (result as Result.Error).error)
        coVerify(exactly = 1) { authRepository.signInWithGoogle(idToken) }
    }
}
