package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.IAddToBalanceUseCase
import javax.inject.Inject

/**
 * Default implementation of [IAddToBalanceUseCase].
 * Delegates to the repository for atomic balance addition.
 */
class AddToBalanceUseCase @Inject constructor(
    private val repository: StudentRepository
) : IAddToBalanceUseCase {
    override suspend operator fun invoke(
        professorId: String,
        studentId: String,
        amount: Double
    ): Result<Unit, DomainError> = repository.addToBalance(professorId, studentId, amount)
}
