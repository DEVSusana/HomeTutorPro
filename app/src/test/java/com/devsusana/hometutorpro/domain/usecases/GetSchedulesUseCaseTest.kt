package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetSchedulesUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

/**
 * Unit tests for GetSchedulesUseCase.
 * 
 * Tests retrieval of schedules for a student.
 */
class GetSchedulesUseCaseTest {

    private lateinit var studentRepository: StudentRepository
    private lateinit var getSchedulesUseCase: GetSchedulesUseCase

    @Before
    fun setup() {
        studentRepository = mockk()
        getSchedulesUseCase = GetSchedulesUseCase(studentRepository)
    }

    @Test
    fun `invoke should return schedules when repository succeeds`() = runTest {
        // Given
        val professorId = "prof123"
        val studentId = "student456"
        val schedules = listOf(
            Schedule(
                id = "schedule1",
                studentId = studentId,
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = "09:00",
                endTime = "10:00"
            ),
            Schedule(
                id = "schedule2",
                studentId = studentId,
                dayOfWeek = DayOfWeek.WEDNESDAY,
                startTime = "14:00",
                endTime = "15:00"
            )
        )
        
        every { studentRepository.getSchedules(professorId, studentId) } returns flowOf(schedules)

        // When
        val result = getSchedulesUseCase(professorId, studentId).first()

        // Then
        assertEquals(2, result.size)
        assertEquals(schedules, result)
        verify(exactly = 1) { studentRepository.getSchedules(professorId, studentId) }
    }

    @Test
    fun `invoke should return empty list when no schedules exist`() = runTest {
        // Given
        val professorId = "prof123"
        val studentId = "student456"
        val emptyList = emptyList<Schedule>()
        
        every { studentRepository.getSchedules(professorId, studentId) } returns flowOf(emptyList)

        // When
        val result = getSchedulesUseCase(professorId, studentId).first()

        // Then
        assertTrue(result.isEmpty())
    }
}
