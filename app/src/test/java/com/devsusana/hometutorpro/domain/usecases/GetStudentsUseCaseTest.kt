package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Student
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
            Student(id = "1", name = "Student 1", professorId = professorId),
            Student(id = "2", name = "Student 2", professorId = professorId)
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
        assertEquals(emptyList<Student>(), result)
        verify { repository.getStudents(professorId) }
    }
}
