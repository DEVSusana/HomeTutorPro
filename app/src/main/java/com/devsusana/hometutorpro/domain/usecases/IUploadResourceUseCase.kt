package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case contract for UploadResource operations.
 */
interface IUploadResourceUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(professorId: String, name: String, fileType: String, fileUri: String): Result<Unit, DomainError>
}
