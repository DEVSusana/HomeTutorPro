package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.SharedResource
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving shared resources for a specific student.
 */
interface IGetSharedResourcesUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(professorId: String?, studentId: String): Flow<List<SharedResource>>
}
