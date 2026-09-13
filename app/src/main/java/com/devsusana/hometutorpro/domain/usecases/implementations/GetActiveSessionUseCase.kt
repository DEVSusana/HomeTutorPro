package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.repository.IActiveSessionRepository
import com.devsusana.hometutorpro.domain.usecases.IGetActiveSessionUseCase
import javax.inject.Inject

class GetActiveSessionUseCase @Inject constructor(
    private val repository: IActiveSessionRepository
) : IGetActiveSessionUseCase {
    override fun invoke(): ActiveSession? = repository.getActiveSession()
}
