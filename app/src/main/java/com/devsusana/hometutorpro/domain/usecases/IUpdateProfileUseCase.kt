package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

interface IUpdateProfileUseCase {
    suspend operator fun invoke(
        name: String, 
        email: String, 
        workingStartTime: String, 
        workingEndTime: String,
        notes: String
    ): Result<Unit, DomainError>
}
