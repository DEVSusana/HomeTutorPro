package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ActiveSession
import com.devsusana.hometutorpro.domain.repository.IActiveSessionRepository
import com.devsusana.hometutorpro.domain.usecases.IStartActiveSessionUseCase
import javax.inject.Inject

class StartActiveSessionUseCase @Inject constructor(
    private val repository: IActiveSessionRepository
) : IStartActiveSessionUseCase {

    override suspend operator fun invoke(session: ActiveSession): Result<Unit, DomainError> {
        if (session.studentId.isBlank()) {
            return Result.Error(DomainError.StudentNotFound)
        }
        if (session.durationMinutes <= 0L) {
            return Result.Error(DomainError.Unknown)
        }
        return repository.startSession(session)
    }
}
