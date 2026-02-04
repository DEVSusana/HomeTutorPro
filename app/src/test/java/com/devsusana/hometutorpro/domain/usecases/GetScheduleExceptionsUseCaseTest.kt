package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetScheduleExceptionsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetScheduleExceptionsUseCaseTest {

    private val repository: ScheduleExceptionRepository = mockk()
    private val useCase = GetScheduleExceptionsUseCase(repository)

    @Test
    fun `invoke should return list of exceptions from repository`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val exceptions = listOf(
            ScheduleException(id = "1", originalScheduleId = "s1", date = 123456789L, type = ExceptionType.CANCELLED)
        )
        every { repository.getExceptions(professorId, studentId) } returns flowOf(exceptions)

        // When
        val result = useCase(professorId, studentId).first()

        // Then
        assertEquals(exceptions, result)
        verify { repository.getExceptions(professorId, studentId) }
    }
}
