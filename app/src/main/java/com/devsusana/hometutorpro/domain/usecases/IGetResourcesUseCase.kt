package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Resource
import kotlinx.coroutines.flow.Flow

interface IGetResourcesUseCase {
    operator fun invoke(professorId: String): Flow<List<Resource>>
}
