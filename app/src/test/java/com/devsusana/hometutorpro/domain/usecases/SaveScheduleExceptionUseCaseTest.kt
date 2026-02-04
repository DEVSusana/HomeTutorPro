package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.entities.Student
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
        
        val student = Student(id = studentId, name = "Student 1", professorId = professorId)

        every { studentRepository.getStudents(professorId) } returns flowOf(listOf(student))
        every { studentRepository.getSchedules(professorId, studentId) } returns flowOf(emptyList())
        coEvery { repository.saveException(professorId, studentId, exception) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentId, exception)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.saveException(professorId, studentId, exception) }
    }

    @Test
    fun `invoke should return error when conflict exists for RESCHEDULED exception`() = runTest {
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
        
        val student = Student(id = studentId, name = "Student 1", professorId = professorId)
        val existingSchedule = Schedule(id = "s2", studentId = studentId, dayOfWeek = DayOfWeek.MONDAY, startTime = "10:00", endTime = "11:00")

        every { studentRepository.getStudents(professorId) } returns flowOf(listOf(student))
        every { studentRepository.getSchedules(professorId, studentId) } returns flowOf(listOf(existingSchedule))

        // When
        val result = useCase(professorId, studentId, exception)

        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is DomainError.ConflictingStudent)
    }
}
