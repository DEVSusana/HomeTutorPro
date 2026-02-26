package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case contract for DeleteResource operations.
 */
interface IDeleteResourceUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(professorId: String, resourceId: String): Result<Unit, DomainError>
}
