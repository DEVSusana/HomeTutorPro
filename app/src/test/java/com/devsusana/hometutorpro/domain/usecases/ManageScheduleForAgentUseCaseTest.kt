package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.usecases.implementations.ManageScheduleForAgentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * Unit tests for [ManageScheduleForAgentUseCase].
 *
 * Verifies that the use case correctly builds [ScheduleException] entities
 * and delegates to [ISaveScheduleExceptionUseCase] for all business-rule validation.
 */
class ManageScheduleForAgentUseCaseTest {

    private val saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase = mockk()
    private val useCase = ManageScheduleForAgentUseCase(saveScheduleExceptionUseCase)

    // ──────────────────────────────────────────────────────────────────────────
    // cancelClass
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `cancelClass should create CANCELLED exception and delegate to SaveScheduleExceptionUseCase`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val scheduleId = "schedule1"
        val date = 1_700_000_000_000L

        val exceptionSlot = slot<ScheduleException>()
        coEvery {
            saveScheduleExceptionUseCase(professorId, studentId, capture(exceptionSlot))
        } returns Result.Success(Unit)

        // When
        val result = useCase.cancelClass(professorId, studentId, scheduleId, date)

        // Then
        assertTrue(result is Result.Success)
        with(exceptionSlot.captured) {
            assertEquals(studentId, this.studentId)
            assertEquals(professorId, this.professorId)
            assertEquals(scheduleId, originalScheduleId)
            assertEquals(date, this.date)
            assertEquals(ExceptionType.CANCELLED, type)
        }
    }

    @Test
    fun `cancelClass should propagate error from SaveScheduleExceptionUseCase`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val scheduleId = "schedule1"
        val date = 1_700_000_000_000L

        coEvery {
            saveScheduleExceptionUseCase(professorId, studentId, any())
        } returns Result.Error(DomainError.Unknown)

        // When
        val result = useCase.cancelClass(professorId, studentId, scheduleId, date)

        // Then
        assertTrue(result is Result.Error)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // rescheduleClass
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `rescheduleClass should create RESCHEDULED exception with new time and delegate to SaveScheduleExceptionUseCase`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val scheduleId = "schedule1"
        val originalDate = 1_700_000_000_000L
        val newStartTime = "11:00"
        val newEndTime = "12:00"
        val newDayOfWeek = DayOfWeek.WEDNESDAY

        val exceptionSlot = slot<ScheduleException>()
        coEvery {
            saveScheduleExceptionUseCase(professorId, studentId, capture(exceptionSlot))
        } returns Result.Success(Unit)

        // When
        val result = useCase.rescheduleClass(
            professorId, studentId, scheduleId, originalDate, newDayOfWeek, newStartTime, newEndTime
        )

        // Then
        assertTrue(result is Result.Success)
        with(exceptionSlot.captured) {
            assertEquals(studentId, this.studentId)
            assertEquals(professorId, this.professorId)
            assertEquals(scheduleId, originalScheduleId)
            assertEquals(originalDate, date)
            assertEquals(ExceptionType.RESCHEDULED, type)
            assertEquals(newStartTime, this.newStartTime)
            assertEquals(newEndTime, this.newEndTime)
            assertEquals(newDayOfWeek, this.newDayOfWeek)
        }
        coVerify(exactly = 1) { saveScheduleExceptionUseCase(professorId, studentId, any()) }
    }

    @Test
    fun `rescheduleClass with null newDayOfWeek should preserve same-day reschedule intent`() = runTest {
        // Given
        coEvery { saveScheduleExceptionUseCase(any(), any(), any()) } returns Result.Success(Unit)

        val exceptionSlot = slot<ScheduleException>()
        coEvery {
            saveScheduleExceptionUseCase("p", "s", capture(exceptionSlot))
        } returns Result.Success(Unit)

        // When
        useCase.rescheduleClass("p", "s", "sched", 0L, null, "10:00", "11:00")

        // Then — newDayOfWeek null means same day
        assertTrue(exceptionSlot.captured.newDayOfWeek == null)
    }

    @Test
    fun `rescheduleClass should propagate error from SaveScheduleExceptionUseCase`() = runTest {
        // Given
        coEvery {
            saveScheduleExceptionUseCase(any(), any(), any())
        } returns Result.Error(DomainError.Unknown)

        // When
        val result = useCase.rescheduleClass("p", "s", "sched", 0L, null, "10:00", "11:00")

        // Then
        assertTrue(result is Result.Error)
    }
}
