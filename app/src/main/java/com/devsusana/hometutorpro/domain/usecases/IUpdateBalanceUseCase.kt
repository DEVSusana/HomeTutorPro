package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case to update a student's balance to an absolute value atomically.
 */
interface IUpdateBalanceUseCase {
    suspend operator fun invoke(
        professorId: String,
        studentId: String,
        newBalance: Double
    ): Result<Unit, DomainError>
}
