package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.SharedResource
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.IGetSharedResourcesUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of use case for retrieving shared resources.
 * Delegates to ResourceRepository.
 */
class GetSharedResourcesUseCase @Inject constructor(
    private val resourceRepository: ResourceRepository
) : IGetSharedResourcesUseCase {
    
    override operator fun invoke(professorId: String?, studentId: String): Flow<List<SharedResource>> {
        return resourceRepository.getSharedResources(professorId, studentId)
    }
}
