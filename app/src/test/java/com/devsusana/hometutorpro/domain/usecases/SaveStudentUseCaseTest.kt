package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.SaveStudentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveStudentUseCaseTest {

    private val repository: StudentRepository = mockk()
    private val useCase = SaveStudentUseCase(repository)

    @Test
    fun `invoke should call repository saveStudent`() = runTest {
        // Given
        val professorId = "prof1"
        val student = Student(id = "1", name = "Student 1", professorId = professorId)
        coEvery { repository.saveStudent(professorId, student) } returns Result.Success("1")

        // When
        val result = useCase(professorId, student)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.saveStudent(professorId, student) }
    }
}
