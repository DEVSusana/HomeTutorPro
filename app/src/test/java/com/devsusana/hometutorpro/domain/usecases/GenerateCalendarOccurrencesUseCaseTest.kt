package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.*
import com.devsusana.hometutorpro.domain.usecases.implementations.GenerateCalendarOccurrencesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateCalendarOccurrencesUseCaseTest {

    private lateinit var useCase: GenerateCalendarOccurrencesUseCase

    @Before
    fun setup() {
        useCase = GenerateCalendarOccurrencesUseCase()
    }

    @Test
    fun `invoke generates regular occurrences within date range`() = runTest {
        // Given
        val startDate = LocalDate.of(2023, 10, 2) // Monday
        val endDate = LocalDate.of(2023, 10, 8)   // Sunday
        
        val student = StudentSummary(
            id = "student1",
            name = "John Doe",
            subjects = "Math",
            color = 0,
            pendingBalance = 0.0,
            pricePerHour = 20.0,
            isActive = true,
            lastClassDate = null
        )
        
        val schedule = Schedule(
            id = "schedule1",
            studentId = "student1",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00",
            studentName = "John Doe",
            studentIsActive = true
        )
        
        // When
        val result = useCase(
            students = listOf(student),
            schedules = listOf(schedule),
            exceptions = emptyList(),
            startDate = startDate,
            endDate = endDate
        )
        
        // Then
        assertEquals(1, result.size)
        assertEquals(startDate, result[0].date)
        assertEquals("John Doe", result[0].student.name)
    }

    @Test
    fun `invoke respects multiple occurrences for weekly schedule`() = runTest {
        // Given
        val startDate = LocalDate.of(2023, 10, 2) // Monday
        val endDate = LocalDate.of(2023, 10, 15)  // Sunday (2 weeks later)
        
        val student = StudentSummary(
            id = "student1",
            name = "John Doe",
            subjects = "Math",
            color = null,
            pendingBalance = 0.0,
            pricePerHour = 20.0,
            isActive = true,
            lastClassDate = null
        )
        
        val schedule = Schedule(
            id = "schedule1",
            studentId = "student1",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00",
            studentName = "John Doe",
            studentIsActive = true
        )
        
        // When
        val result = useCase(
            students = listOf(student),
            schedules = listOf(schedule),
            exceptions = emptyList(),
            startDate = startDate,
            endDate = endDate
        )
        
        // Then
        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2023, 10, 2), result[0].date)
        assertEquals(LocalDate.of(2023, 10, 9), result[1].date)
    }

    @Test
    fun `invoke attaches exceptions to regular occurrences`() = runTest {
        // Given
        val date = LocalDate.of(2023, 10, 2) // Monday
        val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val student = StudentSummary(
            id = "student1",
            name = "John Doe",
            subjects = "Math",
            color = null,
            pendingBalance = 0.0,
            pricePerHour = 20.0,
            isActive = true,
            lastClassDate = null
        )
        val schedule = Schedule(
            id = "schedule1",
            studentId = "student1",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00",
            studentName = "John Doe",
            studentIsActive = true
        )
        
        val exception = ScheduleException(
            id = "exc1",
            studentId = "student1",
            originalScheduleId = "schedule1",
            date = timestamp,
            type = ExceptionType.CANCELLED,
            reason = "Sick"
        )
        
        // When
        val result = useCase(
            students = listOf(student),
            schedules = listOf(schedule),
            exceptions = listOf(exception),
            startDate = date,
            endDate = date
        )
        
        // Then
        assertEquals(1, result.size)
        assertEquals(exception.id, result[0].exception?.id)
        assertEquals(ExceptionType.CANCELLED, result[0].exception?.type)
    }

    @Test
    fun `invoke handles extra classes`() = runTest {
        // Given
        val date = LocalDate.of(2023, 10, 4) // Wednesday
        val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val student = StudentSummary(
            id = "student1",
            name = "John Doe",
            subjects = "Math",
            color = null,
            pendingBalance = 0.0,
            pricePerHour = 20.0,
            isActive = true,
            lastClassDate = null
        )
        
        val extraClass = ScheduleException(
            id = "extra1",
            studentId = "student1",
            originalScheduleId = ScheduleType.EXTRA_ID,
            date = timestamp,
            type = ExceptionType.EXTRA,
            newStartTime = "15:00",
            newEndTime = "16:00"
        )
        
        // When
        val result = useCase(
            students = listOf(student),
            schedules = emptyList(),
            exceptions = listOf(extraClass),
            startDate = date.minusDays(1),
            endDate = date.plusDays(1)
        )
        
        // Then
        assertEquals(1, result.size)
        assertEquals(date, result[0].date)
        assertEquals("15:00", result[0].startTime)
        assertTrue(result[0].isExtra)
    }

    @Test
    fun `invoke handles rescheduled classes as standalone entries`() = runTest {
        // Given
        val originalDate = LocalDate.of(2023, 10, 2) // Monday
        val originalTimestamp = originalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val rescheduledDate = LocalDate.of(2023, 10, 3) // Tuesday (rescheduled to this date)
        val rescheduledTimestamp = rescheduledDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val student = StudentSummary(
            id = "student1",
            name = "John Doe",
            subjects = "Math",
            color = null,
            pendingBalance = 0.0,
            pricePerHour = 20.0,
            isActive = true,
            lastClassDate = null
        )
        
        // 1. Original class marked as Rescheduled (at its original date)
        val cancellationExc = ScheduleException(
            id = "exc_cancel",
            studentId = "student1",
            originalScheduleId = "schedule1",
            date = originalTimestamp,
            type = ExceptionType.RESCHEDULED,
            newStartTime = "", // Explicitly empty to avoid being a standalone entry
            newEndTime = "",
            reason = "Moved"
        )
        
        // 2. New slot entry for the rescheduled class (at the new date)
        val newSlotExc = ScheduleException(
            id = "exc_new_slot",
            studentId = "student1",
            originalScheduleId = "schedule1", // Links back to original for context
            date = rescheduledTimestamp,
            type = ExceptionType.RESCHEDULED,
            newStartTime = "11:00",
            newEndTime = "12:00",
            newDayOfWeek = DayOfWeek.TUESDAY
        )
        
        val schedule = Schedule(
            id = "schedule1",
            studentId = "student1",
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = "10:00",
            endTime = "11:00",
            studentName = "John Doe",
            studentIsActive = true
        )
        
        // When
        val result = useCase(
            students = listOf(student),
            schedules = listOf(schedule),
            exceptions = listOf(cancellationExc, newSlotExc),
            startDate = originalDate,
            endDate = rescheduledDate
        )
        
        // Then
        // Should have 2 occurrences: 
        // 1. The original one (cancelled/rescheduled)
        // 2. The new one (rescheduled slot)
        assertEquals(2, result.size)
        
        val originalOcc = result.find { it.date == originalDate }
        val newOcc = result.find { it.date == rescheduledDate }
        
        assertEquals(ExceptionType.RESCHEDULED, originalOcc?.exception?.type)
        assertEquals("11:00", newOcc?.startTime)
    }
}
