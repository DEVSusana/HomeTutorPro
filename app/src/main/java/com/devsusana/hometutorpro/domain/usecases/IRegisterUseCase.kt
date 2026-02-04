package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User

interface IRegisterUseCase {
    suspend operator fun invoke(email: String, password: String, name: String): Result<User, DomainError>
}
