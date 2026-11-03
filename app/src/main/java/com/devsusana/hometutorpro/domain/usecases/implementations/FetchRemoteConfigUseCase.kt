package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.IConfigRepository
import com.devsusana.hometutorpro.domain.usecases.IFetchRemoteConfigUseCase
import javax.inject.Inject

class FetchRemoteConfigUseCase @Inject constructor(
    private val repository: IConfigRepository
) : IFetchRemoteConfigUseCase {

    override suspend operator fun invoke(): Result<Boolean, DomainError> {
        return try {
            val activated = repository.fetchAndActivate()
            Result.Success(activated)
        } catch (e: Exception) {
            Result.Error(DomainError.NetworkError)
        }
    }
}
