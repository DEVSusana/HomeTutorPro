package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteScheduleUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteScheduleUseCaseTest {

    private val repository: StudentRepository = mockk()
    private val useCase = DeleteScheduleUseCase(repository)

    @Test
    fun `invoke should call repository deleteSchedule`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val scheduleId = "schedule1"
        coEvery { repository.deleteSchedule(professorId, studentId, scheduleId) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentId, scheduleId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.deleteSchedule(professorId, studentId, scheduleId) }
    }
}
