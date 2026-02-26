package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case contract for DeleteScheduleException operations.
 */
interface IDeleteScheduleExceptionUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(
        professorId: String,
        studentId: String,
        exceptionId: String
    ): Result<Unit, DomainError>
}
