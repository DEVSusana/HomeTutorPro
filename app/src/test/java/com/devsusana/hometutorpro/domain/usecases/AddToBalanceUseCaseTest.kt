package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.AddToBalanceUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AddToBalanceUseCaseTest {

    private val repository: StudentRepository = mockk()
    private val useCase = AddToBalanceUseCase(repository)

    @Test
    fun `invoke should call repository addToBalance`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val amount = 7.0
        coEvery { repository.addToBalance(professorId, studentId, amount) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentId, amount)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.addToBalance(professorId, studentId, amount) }
    }

    @Test
    fun `invoke should propagate error from repository`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val amount = 7.0
        coEvery { repository.addToBalance(professorId, studentId, amount) } returns Result.Error(
            com.devsusana.hometutorpro.domain.core.DomainError.Unknown
        )

        // When
        val result = useCase(professorId, studentId, amount)

        // Then
        assertTrue(result is Result.Error)
    }
}
