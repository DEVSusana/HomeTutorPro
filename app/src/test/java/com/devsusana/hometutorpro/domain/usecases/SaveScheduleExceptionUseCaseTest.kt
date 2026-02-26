package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.SaveScheduleExceptionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class SaveScheduleExceptionUseCaseTest {

    private val repository: ScheduleExceptionRepository = mockk()
    private val studentRepository: StudentRepository = mockk()
    private val useCase = SaveScheduleExceptionUseCase(repository, studentRepository)

    @Test
    fun `invoke should save exception when no conflicts`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val date = LocalDate.of(2023, 10, 23) // Monday
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val exception = ScheduleException(
            id = "1", 
            originalScheduleId = "s1", 
            date = dateMillis, 
            type = ExceptionType.RESCHEDULED,
            newStartTime = "12:00",
            newEndTime = "13:00"
        )
        
        val studentSummary = StudentSummary(id = studentId, name = "Student 1", subjects = "", color = null, pendingBalance = 0.0, pricePerHour = 0.0, isActive = true, lastClassDate = null)

        every { studentRepository.getStudents(professorId) } returns flowOf(listOf(studentSummary))
        every { studentRepository.getSchedules(professorId, studentId) } returns flowOf(emptyList())
        every { repository.getExceptions(professorId, studentId) } returns flowOf(emptyList())
        coEvery { repository.saveException(professorId, studentId, exception) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentId, exception)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.saveException(professorId, studentId, exception) }
    }

    @Test
    fun `invoke should return error when conflict exists with regular schedule`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val date = LocalDate.of(2023, 10, 23) // Monday
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val exception = ScheduleException(
            id = "1", 
            originalScheduleId = "s1", 
            date = dateMillis, 
            type = ExceptionType.RESCHEDULED,
            newStartTime = "10:30",
            newEndTime = "11:30"
        )
        
        val studentSummary = StudentSummary(id = studentId, name = "Student 1", subjects = "", color = null, pendingBalance = 0.0, pricePerHour = 0.0, isActive = true, lastClassDate = null)
        val existingSchedule = Schedule(id = "s2", studentId = studentId, dayOfWeek = DayOfWeek.MONDAY, startTime = "10:00", endTime = "11:00")

        every { studentRepository.getStudents(professorId) } returns flowOf(listOf(studentSummary))
        every { studentRepository.getSchedules(professorId, studentId) } returns flowOf(listOf(existingSchedule))
        every { repository.getExceptions(professorId, studentId) } returns flowOf(emptyList())

        // When
        val result = useCase(professorId, studentId, exception)

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is DomainError.ConflictingStudent)
    }

    @Test
    fun `invoke should save when regular schedule is cancelled for that date`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val date = LocalDate.of(2023, 10, 23) // Monday
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val exception = ScheduleException(
            id = "1", 
            date = dateMillis, 
            type = ExceptionType.EXTRA,
            newStartTime = "10:00",
            newEndTime = "11:00"
        )
        
        val studentSummary = StudentSummary(id = "student2", name = "Student 2", subjects = "", color = null, pendingBalance = 0.0, pricePerHour = 0.0, isActive = true, lastClassDate = null)
        val conflictingSchedule = Schedule(id = "s2", studentId = "student2", dayOfWeek = DayOfWeek.MONDAY, startTime = "10:00", endTime = "11:00")
        val cancellation = ScheduleException(
            id = "exc_cancel",
            studentId = "student2",
            date = dateMillis,
            type = ExceptionType.CANCELLED,
            originalScheduleId = "s2"
        )

        every { studentRepository.getStudents(professorId) } returns flowOf(listOf(studentSummary))
        every { studentRepository.getSchedules(professorId, "student2") } returns flowOf(listOf(conflictingSchedule))
        every { repository.getExceptions(professorId, "student2") } returns flowOf(listOf(cancellation))
        coEvery { repository.saveException(professorId, studentId, exception) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentId, exception)

        // Then
        assertTrue(result is Result.Success)
    }

    @Test
    fun `invoke should return error when conflict exists with another exception on same date`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val date = LocalDate.of(2023, 10, 23) // Monday
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val exception = ScheduleException(
            id = "1", 
            date = dateMillis, 
            type = ExceptionType.EXTRA,
            newStartTime = "14:00",
            newEndTime = "15:00"
        )
        
        val studentSummary = StudentSummary(id = "student2", name = "Student 2", subjects = "", color = null, pendingBalance = 0.0, pricePerHour = 0.0, isActive = true, lastClassDate = null)
        val existingException = ScheduleException(
            id = "2",
            studentId = "student2",
            date = dateMillis,
            type = ExceptionType.EXTRA,
            newStartTime = "14:30",
            newEndTime = "15:30"
        )

        every { studentRepository.getStudents(professorId) } returns flowOf(listOf(studentSummary))
        every { studentRepository.getSchedules(professorId, "student2") } returns flowOf(emptyList())
        every { repository.getExceptions(professorId, "student2") } returns flowOf(listOf(existingException))

        // When
        val result = useCase(professorId, studentId, exception)

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is DomainError.ConflictingStudent)
    }

    @Test
    fun `invoke should reuse existing exception ID to prevent duplicates`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val date = LocalDate.of(2023, 10, 23)
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        // Existing exception in DB
        val existingId = "existing_id_123"
        val existingException = ScheduleException(
            id = existingId,
            studentId = studentId,
            date = dateMillis,
            type = ExceptionType.CANCELLED,
            originalScheduleId = "s1"
        )
        
        // New exception being saved (e.g. user changed from CANCELLED to RESCHEDULED)
        // Note: It comes with an EMPTY ID from the UI because it might be a new selection
        val newException = ScheduleException(
            id = "", 
            studentId = studentId,
            date = dateMillis,
            type = ExceptionType.RESCHEDULED,
            originalScheduleId = "s1",
            newStartTime = "17:00",
            newEndTime = "18:00"
        )
        
        val studentSummary = StudentSummary(id = studentId, name = "Student 1", subjects = "", color = null, pendingBalance = 0.0, pricePerHour = 0.0, isActive = true, lastClassDate = null)

        every { repository.getExceptions(professorId, studentId) } returns flowOf(listOf(existingException))
        every { studentRepository.getStudents(professorId) } returns flowOf(listOf(studentSummary))
        every { studentRepository.getSchedules(professorId, studentId) } returns flowOf(emptyList())
        
        // We expect the repository to receive the exception with the PRE-EXISTING ID
        coEvery { repository.saveException(professorId, studentId, any()) } returns Result.Success(Unit)

        // When
        useCase(professorId, studentId, newException)

        // Then
        coVerify { 
            repository.saveException(professorId, studentId, match { 
                it.id == existingId && it.type == ExceptionType.RESCHEDULED 
            }) 
        }
    }

    @Test
    fun `invoke should allow scheduling when original class is rescheduled away`() = runTest {
        // Given
        val professorId = "prof1"
        val date = LocalDate.of(2023, 10, 23)
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        // Student A has a regular class at 16:30 but reschedules it to 18:00
        val studentAId = "studentA"
        val scheduleA = Schedule(id = "sA", studentId = studentAId, dayOfWeek = DayOfWeek.MONDAY, startTime = "16:30", endTime = "17:30")
        val rescheduleA = ScheduleException(
            id = "excA",
            studentId = studentAId,
            date = dateMillis,
            type = ExceptionType.RESCHEDULED,
            originalScheduleId = "sA",
            newStartTime = "18:00",
            newEndTime = "19:00"
        )
        
        // Student B wants to take an extra class at 16:30 (the slot Student A freed)
        val studentBId = "studentB"
        val extraClassB = ScheduleException(
            id = "",
            studentId = studentBId,
            date = dateMillis,
            type = ExceptionType.EXTRA,
            newStartTime = "16:30",
            newEndTime = "17:30"
        )
        
        val studentA = StudentSummary(id = studentAId, name = "Student A", subjects = "", color = null, pendingBalance = 0.0, pricePerHour = 0.0, isActive = true, lastClassDate = null)
        val studentB = StudentSummary(id = studentBId, name = "Student B", subjects = "", color = null, pendingBalance = 0.0, pricePerHour = 0.0, isActive = true, lastClassDate = null)

        every { studentRepository.getStudents(professorId) } returns flowOf(listOf(studentA, studentB))
        
        // Setup for Student A checks
        every { studentRepository.getSchedules(professorId, studentAId) } returns flowOf(listOf(scheduleA))
        every { repository.getExceptions(professorId, studentAId) } returns flowOf(listOf(rescheduleA))
        
        // Setup for Student B checks
        every { studentRepository.getSchedules(professorId, studentBId) } returns flowOf(emptyList())
        every { repository.getExceptions(professorId, studentBId) } returns flowOf(emptyList())
        
        coEvery { repository.saveException(professorId, studentBId, any()) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentBId, extraClassB)

        // Then
        assertTrue("Should succeed because the slot was freed by rescheduling", result is Result.Success)
    }
}
