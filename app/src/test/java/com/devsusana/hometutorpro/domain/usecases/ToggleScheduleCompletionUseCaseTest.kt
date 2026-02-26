package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.ToggleScheduleCompletionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ToggleScheduleCompletionUseCaseTest {

    private val studentRepository: StudentRepository = mockk()
    private val useCase = ToggleScheduleCompletionUseCase(studentRepository)

    @Test
    fun `invoke should return success when repository succeeds`() = runTest {
        val professorId = "prof1"
        val scheduleId = "schedule1"

        coEvery { studentRepository.toggleScheduleCompletion(professorId, scheduleId) } returns Result.Success(Unit)

        val result = useCase(professorId, scheduleId)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { studentRepository.toggleScheduleCompletion(professorId, scheduleId) }
    }

    @Test
    fun `invoke should return error when repository fails`() = runTest {
        val professorId = "prof1"
        val scheduleId = "schedule1"

        coEvery { studentRepository.toggleScheduleCompletion(professorId, scheduleId) } returns Result.Error(DomainError.Unknown)

        val result = useCase(professorId, scheduleId)

        assertTrue(result is Result.Error)
        coVerify(exactly = 1) { studentRepository.toggleScheduleCompletion(professorId, scheduleId) }
    }
}
