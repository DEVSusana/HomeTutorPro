package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

interface IUploadResourceUseCase {
    suspend operator fun invoke(professorId: String, name: String, fileUri: String): Result<Unit, DomainError>
}
