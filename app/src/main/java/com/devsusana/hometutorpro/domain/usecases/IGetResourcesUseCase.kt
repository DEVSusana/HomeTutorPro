package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Use case contract for GetResources operations.
 */
interface IGetResourcesUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(professorId: String): Flow<List<Resource>>
}
