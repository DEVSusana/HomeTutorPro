package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.GetStudentByIdUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetStudentByIdUseCase.
 * 
 * Tests retrieval of a single student by ID.
 */
class GetStudentByIdUseCaseTest {

    private lateinit var studentRepository: StudentRepository
    private lateinit var getStudentByIdUseCase: GetStudentByIdUseCase

    @Before
    fun setup() {
        studentRepository = mockk()
        getStudentByIdUseCase = GetStudentByIdUseCase(studentRepository)
    }

    @Test
    fun `invoke should return student when repository succeeds`() = runTest {
        // Given
        val professorId = "prof123"
        val studentId = "student456"
        val expectedStudent = Student(
            id = studentId,
            professorId = professorId,
            name = "John Doe",
            age = 15,
            address = "123 Main St",
            parentPhones = "555-1234",
            subjects = "Math, Physics",
            course = "3º ESO",
            pricePerHour = 25.0,
            pendingBalance = 50.0,
            educationalAttention = "",
            lastPaymentDate = null
        )
        
        every { studentRepository.getStudentById(professorId, studentId) } returns flowOf(expectedStudent)

        // When
        val result = getStudentByIdUseCase(professorId, studentId).first()

        // Then
        assertEquals(expectedStudent, result)
        verify(exactly = 1) { studentRepository.getStudentById(professorId, studentId) }
    }

    @Test
    fun `invoke should return student with all fields populated`() = runTest {
        // Given
        val professorId = "prof789"
        val studentId = "student101"
        val expectedStudent = Student(
            id = studentId,
            professorId = professorId,
            name = "Jane Smith",
            age = 16,
            address = "456 Oak Ave",
            parentPhones = "555-5678",
            subjects = "Chemistry, Biology",
            course = "4º ESO",
            pricePerHour = 30.0,
            pendingBalance = 60.0,
            educationalAttention = "Dyslexia",
            lastPaymentDate = 1234567890L
        )
        
        every { studentRepository.getStudentById(professorId, studentId) } returns flowOf(expectedStudent)

        // When
        val result = getStudentByIdUseCase(professorId, studentId).first()

        // Then
        assertNotNull(result)
        assertEquals(expectedStudent.id, result!!.id)
        assertEquals(expectedStudent.name, result.name)
        assertEquals(expectedStudent.educationalAttention, result.educationalAttention)
        assertNotNull(result.lastPaymentDate)
    }
}
