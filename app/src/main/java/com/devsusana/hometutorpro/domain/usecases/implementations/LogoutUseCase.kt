package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.ILogoutUseCase

import javax.inject.Inject

/**
 * Default implementation of [ILogoutUseCase].
 */
class LogoutUseCase @Inject constructor(private val repository: AuthRepository) : ILogoutUseCase {
    override suspend operator fun invoke() = repository.logout()
}
