package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetAllSchedulesUseCase
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

class GetAllSchedulesUseCaseTest {

    private val studentRepository: StudentRepository = mockk()
    private val useCase = GetAllSchedulesUseCase(studentRepository)

    @Test
    fun `invoke should return schedules when repository succeeds`() = runTest {
        val professorId = "prof1"
        val schedules = listOf(
            Schedule(id = "1", dayOfWeek = DayOfWeek.MONDAY, startTime = "09:00", endTime = "10:00"),
            Schedule(id = "2", dayOfWeek = DayOfWeek.TUESDAY, startTime = "11:00", endTime = "12:00")
        )

        every { studentRepository.getAllSchedules(professorId) } returns flowOf(schedules)

        val result = useCase(professorId).first()

        assertEquals(schedules, result)
        verify(exactly = 1) { studentRepository.getAllSchedules(professorId) }
    }

    @Test
    fun `invoke should return empty list when no schedules exist`() = runTest {
        val professorId = "prof1"
        val schedules = emptyList<Schedule>()

        every { studentRepository.getAllSchedules(professorId) } returns flowOf(schedules)

        val result = useCase(professorId).first()

        assertTrue(result.isEmpty())
    }
}
