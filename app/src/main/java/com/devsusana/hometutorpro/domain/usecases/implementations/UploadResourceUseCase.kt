package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.ResourceRepository
import com.devsusana.hometutorpro.domain.usecases.IUploadResourceUseCase

import javax.inject.Inject

class UploadResourceUseCase @Inject constructor(private val repository: ResourceRepository) :
    IUploadResourceUseCase {
    override suspend operator fun invoke(professorId: String, name: String, fileType: String, fileUri: String): Result<Unit, DomainError> {
        return repository.uploadResource(professorId, name, fileType, fileUri)
    }
}
