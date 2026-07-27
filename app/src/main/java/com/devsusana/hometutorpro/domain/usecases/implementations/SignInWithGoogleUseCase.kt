package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.ISignInWithGoogleUseCase
import javax.inject.Inject

/**
 * Implementation of [ISignInWithGoogleUseCase].
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : ISignInWithGoogleUseCase {
    override suspend operator fun invoke(idToken: String): Result<User, DomainError> {
        return authRepository.signInWithGoogle(idToken)
    }
}
