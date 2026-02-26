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
        name: String, 
        email: String, 
        workingStartTime: String, 
        workingEndTime: String,
        notes: String
    ): Result<Unit, DomainError>
}
