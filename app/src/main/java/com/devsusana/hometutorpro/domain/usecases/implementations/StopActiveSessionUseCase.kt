package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.IActiveSessionRepository
import com.devsusana.hometutorpro.domain.usecases.IStopActiveSessionUseCase
import javax.inject.Inject

class StopActiveSessionUseCase @Inject constructor(
    private val repository: IActiveSessionRepository
) : IStopActiveSessionUseCase {

    override suspend operator fun invoke(): Result<Unit, DomainError> {
        return repository.stopSession()
    }
}
