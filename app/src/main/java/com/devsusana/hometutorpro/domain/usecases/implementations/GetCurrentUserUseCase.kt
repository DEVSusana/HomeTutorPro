package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.AuthRepository

import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Default implementation of [IGetCurrentUserUseCase].
 */
class GetCurrentUserUseCase @Inject constructor(private val repository: AuthRepository) :
    IGetCurrentUserUseCase {
    override operator fun invoke(): StateFlow<User?> = repository.currentUser
}
