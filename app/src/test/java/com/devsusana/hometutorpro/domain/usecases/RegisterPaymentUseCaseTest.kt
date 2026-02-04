package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.RegisterPaymentUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterPaymentUseCaseTest {

    private val repository: StudentRepository = mockk()
    private val useCase = RegisterPaymentUseCase(repository)

    @Test
    fun `invoke should call repository registerPayment`() = runTest {
        // Given
        val professorId = "prof1"
        val studentId = "student1"
        val amount = 50.0
        val type = PaymentType.EFFECTIVE
        coEvery { repository.registerPayment(professorId, studentId, amount, type) } returns Result.Success(Unit)

        // When
        val result = useCase(professorId, studentId, amount, type)

        // Then
        assertTrue(result is Result.Success)
        coVerify { repository.registerPayment(professorId, studentId, amount, type) }
    }
}
