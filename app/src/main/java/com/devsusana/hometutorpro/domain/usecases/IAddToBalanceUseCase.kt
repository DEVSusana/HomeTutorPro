package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case contract for atomically adding an amount to a student's pending balance.
 * Used when starting a class to avoid full-entity overwrites.
 */
interface IAddToBalanceUseCase {
    /**
     * Atomically adds [amount] to the student's pending balance and records the class date.
     */
    suspend operator fun invoke(
        professorId: String,
        studentId: String,
        amount: Double
    ): Result<Unit, DomainError>
}
