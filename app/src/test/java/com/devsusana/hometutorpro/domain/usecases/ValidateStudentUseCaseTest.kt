package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.usecases.implementations.ValidateStudentUseCase
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateStudentUseCaseTest {

    private val useCase = ValidateStudentUseCase()

    @Test
    fun `invoke should return error when name is blank`() {
        val student = Student(name = "")

        val result = useCase(student)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is DomainError.StudentNameRequired)
    }

    @Test
    fun `invoke should return error when price is invalid`() {
        val student = Student(name = "Ana", pricePerHour = 0.0)

        val result = useCase(student)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is DomainError.InvalidPrice)
    }

    @Test
    fun `invoke should return error when balance is out of range`() {
        val student = Student(name = "Ana", pricePerHour = 20.0, pendingBalance = 200000.0)

        val result = useCase(student)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is DomainError.InvalidBalance)
    }

    @Test
    fun `invoke should return success when student is valid`() {
        val student = Student(name = "Ana", pricePerHour = 20.0, pendingBalance = 0.0)

        val result = useCase(student)

        assertTrue(result is Result.Success)
    }
}
