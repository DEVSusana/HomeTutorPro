package com.devsusana.hometutorpro

import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.SaveStudentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests mapping the Add Student capability (implemented via [SaveStudentUseCase])
 * to satisfy CI testing pyramid guidelines.
 */
class AddStudentUseCaseTest {

    private val repository: StudentRepository = mockk()
    private val useCase = SaveStudentUseCase(repository)

    @Test
    fun invoke_savesStudentSuccessfully() = runTest {
        val professorId = "prof_123"
        val student = Student(id = "1", name = "Jane Doe", subjects = "Physics", pricePerHour = 25.0)

        every { repository.getStudents(professorId) } returns flowOf(emptyList())
        coEvery { repository.saveStudent(professorId, any()) } returns Result.Success("1")

        val result = useCase(professorId, student)

        assertEquals(Result.Success("1"), result)
        coVerify { repository.saveStudent(professorId, any()) }
    }
}
