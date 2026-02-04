package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.IUpdatePasswordUseCase
import javax.inject.Inject

class UpdatePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : IUpdatePasswordUseCase {
    override suspend fun invoke(newPassword: String): Result<Unit, DomainError> {
        return authRepository.updatePassword(newPassword)
    }
}
