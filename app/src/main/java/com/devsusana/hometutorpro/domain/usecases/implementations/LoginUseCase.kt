package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.ILoginUseCase

import javax.inject.Inject

/**
 * Default implementation of [ILoginUseCase].
 */
class LoginUseCase @Inject constructor(private val repository: AuthRepository) : ILoginUseCase {
    override suspend operator fun invoke(email: String, password: String): Result<User, DomainError> = repository.login(email, password)
}
