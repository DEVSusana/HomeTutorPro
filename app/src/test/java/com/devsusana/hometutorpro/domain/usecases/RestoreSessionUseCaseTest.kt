package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.RestoreSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RestoreSessionUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: RestoreSessionUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        useCase = RestoreSessionUseCase(authRepository)
    }

    @Test
    fun `invoke calls restoreSessionSilently and returns success`() = runTest {
        val expectedUser = User(uid = "user123", email = "test@example.com", displayName = "Test")
        coEvery { authRepository.restoreSessionSilently() } returns Result.Success(expectedUser)

        val result = useCase()

        assertTrue(result is Result.Success)
        assertEquals(expectedUser, (result as Result.Success).data)
        coVerify(exactly = 1) { authRepository.restoreSessionSilently() }
    }

    @Test
    fun `invoke calls restoreSessionSilently and returns error when not found`() = runTest {
        coEvery { authRepository.restoreSessionSilently() } returns Result.Error(DomainError.UserNotFound)

        val result = useCase()

        assertTrue(result is Result.Error)
        assertEquals(DomainError.UserNotFound, (result as Result.Error).error)
        coVerify(exactly = 1) { authRepository.restoreSessionSilently() }
    }
}
