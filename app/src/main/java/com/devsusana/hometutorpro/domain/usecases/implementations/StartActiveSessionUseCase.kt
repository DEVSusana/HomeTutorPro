package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.repository.IActiveSessionRepository
import com.devsusana.hometutorpro.domain.usecases.IStartActiveSessionUseCase
import javax.inject.Inject

class StartActiveSessionUseCase @Inject constructor(
    private val repository: IActiveSessionRepository
) : IStartActiveSessionUseCase {
    override fun invoke(session: ActiveSession) = repository.startSession(session)
}
