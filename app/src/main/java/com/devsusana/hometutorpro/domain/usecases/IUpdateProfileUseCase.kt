package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case contract for UpdateProfile operations.
 */
interface IUpdateProfileUseCase {
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(
        params: com.devsusana.hometutorpro.domain.entities.UpdateUserParams
    ): Result<Unit, DomainError>
}
