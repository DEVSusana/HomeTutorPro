package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.Resource
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.IGetResourcesUseCase
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

/**
 * Default implementation of [IGetResourcesUseCase].
 */
class GetResourcesUseCase @Inject constructor(private val repository: ResourceRepository) :
    IGetResourcesUseCase {
    override operator fun invoke(professorId: String): Flow<List<Resource>> {
        return repository.getResources(professorId)
    }
}
