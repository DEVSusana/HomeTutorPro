package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.IConfigRepository
import com.devsusana.hometutorpro.domain.usecases.IFetchRemoteConfigUseCase
import javax.inject.Inject

class FetchRemoteConfigUseCase @Inject constructor(
    private val repository: IConfigRepository
) : IFetchRemoteConfigUseCase {

    override suspend operator fun invoke(): Boolean {
        return repository.fetchAndActivate()
    }
}
