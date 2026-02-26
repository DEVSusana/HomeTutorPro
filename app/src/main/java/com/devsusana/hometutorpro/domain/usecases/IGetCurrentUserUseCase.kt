package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case contract for GetCurrentUser operations.
 */
interface IGetCurrentUserUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(): StateFlow<User?>
}
