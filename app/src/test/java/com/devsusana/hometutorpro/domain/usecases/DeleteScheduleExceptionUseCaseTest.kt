package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteScheduleExceptionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteScheduleExceptionUseCaseTest {

    private val repository: ScheduleExceptionRepository = mockk()
    private val useCase = DeleteScheduleExceptionUseCase(repository)

    @Test
    fun `invoke should call repository deleteException`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val exceptionId = "ex1"
        coEvery { repository.deleteException(professorId, studentId, exceptionId) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentId, exceptionId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.deleteException(professorId, studentId, exceptionId) }
    }
}
