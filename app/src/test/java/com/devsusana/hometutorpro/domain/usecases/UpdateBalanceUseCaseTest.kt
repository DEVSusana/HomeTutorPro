package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.UpdateBalanceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UpdateBalanceUseCase].
 */
class UpdateBalanceUseCaseTest {

    private val repository: StudentRepository = mockk()
    private val useCase = UpdateBalanceUseCase(repository)

    @Test
    fun `invoke should call repository updateBalance and return success`() = runTest {
        // Given
        val professorId = "prof123"
        val studentId = "student456"
        val newBalance = 150.0
        coEvery { repository.updateBalance(professorId, studentId, newBalance) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentId, newBalance)

        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { repository.updateBalance(professorId, studentId, newBalance) }
    }

    @Test
    fun `invoke should propagate error from repository`() = runTest {
        // Given
        val professorId = "prof123"
        val studentId = "student456"
        val newBalance = 150.0
        val error = DomainError.Unknown
        coEvery { repository.updateBalance(professorId, studentId, newBalance) } returns Result.Error(error)

        // When
        val result = useCase(professorId, studentId, newBalance)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
        coVerify(exactly = 1) { repository.updateBalance(professorId, studentId, newBalance) }
    }
}
