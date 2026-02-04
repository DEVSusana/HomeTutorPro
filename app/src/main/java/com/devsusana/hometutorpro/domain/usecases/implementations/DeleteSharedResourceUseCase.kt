package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.IDeleteSharedResourceUseCase
import javax.inject.Inject

/**
 * Implementation of use case for deleting shared resource records.
 * Delegates to ResourceRepository.
 */
class DeleteSharedResourceUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository
) : IDeleteSharedResourceUseCase {
    
    override suspend operator fun invoke(professorId: String?, resourceId: String): Result<Unit, DomainError> {
        return resourceRepository.deleteSharedResource(professorId, resourceId)
    }
}
