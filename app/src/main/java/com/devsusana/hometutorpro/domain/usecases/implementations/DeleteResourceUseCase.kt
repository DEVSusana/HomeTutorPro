package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.IDeleteResourceUseCase

import javax.inject.Inject

class DeleteResourceUseCase @Inject constructor(private val repository: ResourceRepository) :
    IDeleteResourceUseCase {
    override suspend operator fun invoke(professorId: String, resourceId: String): Result<Unit, DomainError> {
        return repository.deleteResource(professorId, resourceId)
    }
}
