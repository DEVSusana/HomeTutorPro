package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.DeleteStudentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteStudentUseCaseTest {

    private val repository: StudentRepository = mockk()
    private val useCase = DeleteStudentUseCase(repository)

    @Test
    fun `invoke should call repository deleteStudent`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        coEvery { repository.deleteStudent(professorId, studentId) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentId)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.deleteStudent(professorId, studentId) }
    }
}
