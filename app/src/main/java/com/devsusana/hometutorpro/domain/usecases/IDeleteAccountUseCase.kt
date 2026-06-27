package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Usecase contract for deleting the user account.
 */
interface IDeleteAccountUseCase {
    suspend operator fun invoke(): Result<Unit, DomainError>
}
