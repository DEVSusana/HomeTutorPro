package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType

/**
 * Use case contract for RegisterPayment operations.
 */
interface IRegisterPaymentUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(
        professorId: String,
        studentId: String,
        amountPaid: Double,
        paymentType: PaymentType
    ): Result<Unit, DomainError>
}
