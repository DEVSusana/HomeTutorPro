package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User

/**
 * Use case contract for Login operations.
 */
interface ILoginUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(email: String, password: String): Result<User, DomainError>
}
