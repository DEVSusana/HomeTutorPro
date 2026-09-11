package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.IUpdateProfileUseCase
import javax.inject.Inject

/**
 * Use case implementation for updateprofile operations.
 */
class UpdateProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : IUpdateProfileUseCase {
    override suspend fun invoke(
        params: com.devsusana.hometutorpro.domain.entities.UpdateUserParams
    ): Result<Unit, DomainError> {
        return authRepository.updateProfile(params)
    }
}
