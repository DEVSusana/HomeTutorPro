package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case contract for DeleteStudent operations.
 */
interface IDeleteStudentUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(professorId: String, studentId: String): Result<Unit, DomainError>
}
