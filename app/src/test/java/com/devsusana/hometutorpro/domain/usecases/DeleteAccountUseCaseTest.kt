package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteAccountUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DeleteAccountUseCase].
 */
class DeleteAccountUseCaseTest {

    private val repository: AuthRepository = mockk()
    private val useCase = DeleteAccountUseCase(repository)

    @Test
    fun `invoke should call repository deleteAccount and return success`() = runTest {
        // Given
        val password = "secretPassword123"
        coEvery { repository.deleteAccount(password) } returns Result.Success(Unit)

        // When
        val result = useCase(password)

        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.deleteAccount(password) }
    }

    @Test
    fun `invoke should propagate error from repository`() = runTest {
        // Given
        val password = "wrongPassword"
        val error = DomainError.InvalidCredentials
        coEvery { repository.deleteAccount(password) } returns Result.Error(error)

        // When
        val result = useCase(password)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
        coVerify(exactly = 1) { repository.deleteAccount(password) }
    }
}
