package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.IRestoreSessionUseCase
import javax.inject.Inject

/**
 * Default implementation of [IRestoreSessionUseCase] for Android Zero-Tap Sign-In.
 */
class RestoreSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) : IRestoreSessionUseCase {
    override suspend operator fun invoke(): Result<User, DomainError> = repository.restoreSessionSilently()
}
