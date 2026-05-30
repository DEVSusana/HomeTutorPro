package com.devsusana.hometutorpro

import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetStudentsUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [GetStudentsUseCase] to satisfy CI testing pyramid guidelines.
 */
class GetStudentsUseCaseTest {

    private val repository: StudentRepository = mockk()
    private val useCase = GetStudentsUseCase(repository)

    @Test
    fun invoke_returnsStudentsFromRepository() = runTest {
        val professorId = "prof_123"
        val students = listOf(
            StudentSummary(id = "1", name = "Test Student", subjects = "Math", color = null, pendingBalance = 0.0, pricePerHour = 20.0, isActive = true, lastClassDate = null)
        )
        every { repository.getStudents(professorId) } returns flowOf(students)

        val result = useCase(professorId).first()
        assertEquals(students, result)
        verify { repository.getStudents(professorId) }
    }
}
