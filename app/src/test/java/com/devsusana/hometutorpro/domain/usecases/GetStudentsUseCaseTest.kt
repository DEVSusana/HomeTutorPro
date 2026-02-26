package com.devsusana.hometutorpro.domain.usecases

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

class GetStudentsUseCaseTest {

    private val repository: StudentRepository = mockk()
    private val useCase = GetStudentsUseCase(repository)

    @Test
    fun `invoke should return list of students from repository`() = runTest {
        // Given
        val professorId = "prof1"
        val students = listOf(
            StudentSummary(id = "1", name = "Student 1", subjects = "Math", color = null, pendingBalance = 0.0, pricePerHour = 20.0, isActive = true, lastClassDate = null),
            StudentSummary(id = "2", name = "Student 2", subjects = "Physics", color = null, pendingBalance = 0.0, pricePerHour = 25.0, isActive = true, lastClassDate = null)
        )
        every { repository.getStudents(professorId) } returns flowOf(students)

        // When
        val result = useCase(professorId).first()

        // Then
        assertEquals(students, result)
        verify { repository.getStudents(professorId) }
    }

    @Test
    fun `invoke should return empty list when repository returns empty`() = runTest {
        // Given
        val professorId = "prof1"
        every { repository.getStudents(professorId) } returns flowOf(emptyList())

        // When
        val result = useCase(professorId).first()

        // Then
        assertEquals(emptyList<StudentSummary>(), result)
        verify { repository.getStudents(professorId) }
    }
}
