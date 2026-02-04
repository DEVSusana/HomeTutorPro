package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType

interface IRegisterPaymentUseCase {
    suspend operator fun invoke(
        professorId: String,
        studentId: String,
        amountPaid: Double,
        paymentType: PaymentType
    ): Result<Unit, DomainError>
}
