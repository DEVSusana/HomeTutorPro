package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case to send a password reset email to a user.
 */
interface ISendPasswordResetEmailUseCase {
    suspend operator fun invoke(email: String): Result<Unit, DomainError>
}
