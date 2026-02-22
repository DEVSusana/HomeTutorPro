package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.SaveScheduleUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class SaveScheduleUseCaseTest {

    private val repository: StudentRepository = mockk()
    private val useCase = SaveScheduleUseCase(repository)

    @Test
    fun `invoke should save schedule when no conflicts`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val schedule = Schedule(id = "1", studentId = studentId, dayOfWeek = DayOfWeek.MONDAY, startTime = "10:00", endTime = "11:00")

        coEvery { repository.getConflictingSchedule(any(), any(), any(), any()) } returns null
        coEvery { repository.saveSchedule(professorId, studentId, schedule) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentId, schedule)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.saveSchedule(professorId, studentId, schedule) }
    }

    @Test
    fun `invoke should return error when conflict exists`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val schedule = Schedule(id = "2", studentId = studentId, dayOfWeek = DayOfWeek.MONDAY, startTime = "10:30", endTime = "11:30")
        val conflictingSchedule = Schedule(id = "1", studentId = "other", dayOfWeek = DayOfWeek.MONDAY, startTime = "10:00", endTime = "11:00", studentName = "Conflict")

        coEvery { repository.getConflictingSchedule(schedule.dayOfWeek.value, schedule.startTime, schedule.endTime, schedule.id) } returns conflictingSchedule

        // When
        val result = useCase(professorId, studentId, schedule)

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is DomainError.ConflictingStudent)
        assertTrue((error as DomainError.ConflictingStudent).studentName == "Conflict")
    }
}
