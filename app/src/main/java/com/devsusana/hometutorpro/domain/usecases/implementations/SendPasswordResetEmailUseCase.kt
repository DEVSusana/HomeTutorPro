package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.ISendPasswordResetEmailUseCase
import javax.inject.Inject

/**
 * Default implementation of [ISendPasswordResetEmailUseCase].
 */
class SendPasswordResetEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) : ISendPasswordResetEmailUseCase {
    override suspend operator fun invoke(email: String): Result<Unit, DomainError> {
        return repository.sendPasswordResetEmail(email)
    }
}
