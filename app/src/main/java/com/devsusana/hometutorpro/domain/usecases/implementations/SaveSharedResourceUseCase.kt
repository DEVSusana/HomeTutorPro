package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.ISaveSharedResourceUseCase
import javax.inject.Inject

/**
 * Implementation of use case for saving shared resource records.
 * Delegates to ResourceRepository.
 */
class SaveSharedResourceUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository
) : ISaveSharedResourceUseCase {
    
    override suspend operator fun invoke(professorId: String?, resource: SharedResource): Result<Unit, DomainError> {
        return resourceRepository.saveSharedResource(professorId, resource)
    }
}
