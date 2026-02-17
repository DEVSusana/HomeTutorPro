package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.SaveBulkSchedulesUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class SaveBulkSchedulesUseCaseTest {

    private val saveScheduleUseCase: ISaveScheduleUseCase = mockk()
    private val checkScheduleConflictUseCase: ICheckScheduleConflictUseCase = mockk()
    private val repository: StudentRepository = mockk()
    private val useCase = SaveBulkSchedulesUseCase(
        saveScheduleUseCase,
        checkScheduleConflictUseCase,
        repository
    )

    @Test
    fun `invoke should process all schedules for new student when no conflicts`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "new"
        val schedules = listOf(
            Schedule(id = "1", dayOfWeek = DayOfWeek.MONDAY, startTime = "10:00", endTime = "11:00"),
            Schedule(id = "2", dayOfWeek = DayOfWeek.TUESDAY, startTime = "10:00", endTime = "11:00")
        )

        coEvery { repository.getConflictingSchedule(any(), any(), any(), any()) } returns null
        coEvery { checkScheduleConflictUseCase(any(), any()) } returns false

        // When
        val result = useCase(professorId, studentId, schedules)

        // Then
        assertTrue(result.isSuccessful)
        assertTrue(result.processedSchedules.size == 2)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `invoke should report conflicts correctly`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val schedules = listOf(
            Schedule(id = "1", dayOfWeek = DayOfWeek.MONDAY, startTime = "10:00", endTime = "11:00")
        )

        coEvery { repository.getConflictingSchedule(any(), any(), any(), any()) } returns Schedule(studentName = "Conflict")

        // When
        val result = useCase(professorId, studentId, schedules)

        // Then
        assertFalse(result.isSuccessful)
        assertTrue(result.errors.containsKey(0))
        assertTrue(result.errors[0] == DomainError.ScheduleConflict)
    }
}
