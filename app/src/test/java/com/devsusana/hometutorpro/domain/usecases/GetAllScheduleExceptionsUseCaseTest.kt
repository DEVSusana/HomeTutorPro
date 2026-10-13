package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetAllScheduleExceptionsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class GetAllScheduleExceptionsUseCaseTest {

    private val repository: ScheduleExceptionRepository = mockk()
    private val useCase = GetAllScheduleExceptionsUseCase(repository)

    @Test
    fun `invoke should return schedule exceptions when repository succeeds`() = runTest {
        val professorId = "prof1"
        val exceptions = listOf(
            ScheduleException(
                id = "1",
                studentId = "student1",
                date = 1000L,
                type = ExceptionType.CANCELLED,
                originalScheduleId = "sch1"
            ),
            ScheduleException(
                id = "2",
                studentId = "student2",
                date = 2000L,
                type = ExceptionType.RESCHEDULED,
                newDayOfWeek = DayOfWeek.FRIDAY,
                newStartTime = "15:00",
                newEndTime = "16:00",
                originalScheduleId = "sch2"
            )
        )

        every { repository.getAllExceptionsFlow(professorId) } returns flowOf(exceptions)

        val result = useCase(professorId).first()

        assertEquals(exceptions, result)
        verify(exactly = 1) { repository.getAllExceptionsFlow(professorId) }
    }

    @Test
    fun `invoke should return empty list when no exceptions exist`() = runTest {
        val professorId = "prof1"
        val exceptions = emptyList<ScheduleException>()

        every { repository.getAllExceptionsFlow(professorId) } returns flowOf(exceptions)

        val result = useCase(professorId).first()

        assertTrue(result.isEmpty())
        verify(exactly = 1) { repository.getAllExceptionsFlow(professorId) }
    }
}
