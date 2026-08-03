package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User

/**
 * Contract interface for Google Sign-In use case.
 */
interface ISignInWithGoogleUseCase {
    suspend operator fun invoke(idToken: String): Result<User, DomainError>
}
