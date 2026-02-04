package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

interface IUpdatePasswordUseCase {
    suspend operator fun invoke(newPassword: String): Result<Unit, DomainError>
}
