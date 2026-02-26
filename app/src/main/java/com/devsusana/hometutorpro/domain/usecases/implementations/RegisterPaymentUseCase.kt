package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.IRegisterPaymentUseCase

import javax.inject.Inject

/**
 * Default implementation of [IRegisterPaymentUseCase].
 */
class RegisterPaymentUseCase @Inject constructor(private val repository: StudentRepository) :
    IRegisterPaymentUseCase {
    override suspend operator fun invoke(
        professorId: String,
        studentId: String,
        amountPaid: Double,
        paymentType: PaymentType
    ): Result<Unit, DomainError> = repository.registerPayment(professorId, studentId, amountPaid, paymentType)
}
