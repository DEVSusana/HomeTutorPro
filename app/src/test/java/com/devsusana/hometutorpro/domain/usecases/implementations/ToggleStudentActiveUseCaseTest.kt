package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleStudentActiveUseCaseTest {

    private lateinit var studentRepository: StudentRepository
    private lateinit var toggleStudentActiveUseCase: ToggleStudentActiveUseCase

    private val professorId = "prof123"
    private val studentId = "student456"

    @Before
    fun setup() {
        studentRepository = mockk()
        toggleStudentActiveUseCase = ToggleStudentActiveUseCase(studentRepository)
    }

    @Test
    fun `invoke should toggle isActive from true to false and save`() = runTest {
        // Given
        val student = Student(id = studentId, professorId = professorId, isActive = true)
        val expectedUpdatedStudent = student.copy(isActive = false)

        every { studentRepository.getStudentById(professorId, studentId) } returns flowOf(student)
        coEvery { studentRepository.saveStudent(professorId, expectedUpdatedStudent) } returns Result.Success(studentId)

        // When
        val result = toggleStudentActiveUseCase(professorId, studentId)

        // Then
        assertTrue(result is Result.Success)
        coVerify {
            studentRepository.getStudentById(professorId, studentId)
            studentRepository.saveStudent(professorId, expectedUpdatedStudent)
        }
        confirmVerified(studentRepository)
    }

    @Test
    fun `invoke should toggle isActive from false to true and save`() = runTest {
        // Given
        val student = Student(id = studentId, professorId = professorId, isActive = false)
        val expectedUpdatedStudent = student.copy(isActive = true)

        every { studentRepository.getStudentById(professorId, studentId) } returns flowOf(student)
        coEvery { studentRepository.saveStudent(professorId, expectedUpdatedStudent) } returns Result.Success(studentId)

        // When
        val result = toggleStudentActiveUseCase(professorId, studentId)

        // Then
        assertTrue(result is Result.Success)
        coVerify {
            studentRepository.getStudentById(professorId, studentId)
            studentRepository.saveStudent(professorId, expectedUpdatedStudent)
        }
    }

    @Test
    fun `invoke should return StudentNotFound error when student does not exist`() = runTest {
        // Given
        every { studentRepository.getStudentById(professorId, studentId) } returns flowOf(null)

        // When
        val result = toggleStudentActiveUseCase(professorId, studentId)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(DomainError.StudentNotFound, (result as Result.Error).error)
        coVerify(exactly = 0) { studentRepository.saveStudent(any(), any()) }
    }
}
