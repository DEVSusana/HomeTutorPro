package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.IUpdateBalanceUseCase
import javax.inject.Inject

/**
 * Implementation of [IUpdateBalanceUseCase].
 */
class UpdateBalanceUseCase @Inject constructor(
    private val repository: StudentRepository
) : IUpdateBalanceUseCase {
    override suspend fun invoke(
        professorId: String,
        studentId: String,
        newBalance: Double
    ): Result<Unit, DomainError> {
        return repository.updateBalance(professorId, studentId, newBalance)
    }
}
