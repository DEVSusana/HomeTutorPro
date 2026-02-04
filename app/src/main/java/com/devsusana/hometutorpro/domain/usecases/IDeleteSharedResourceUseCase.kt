package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case for deleting a shared resource record.
 */
interface IDeleteSharedResourceUseCase {
    suspend operator fun invoke(professorId: String?, resourceId: String): Result<Unit, DomainError>
}
