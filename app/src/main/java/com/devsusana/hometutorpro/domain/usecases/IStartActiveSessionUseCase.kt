package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ActiveSession

/**
 * Use case to start a new active tutoring session.
 */
interface IStartActiveSessionUseCase {
    /**
     * Executes starting and persisting an active tutoring session.
     *
     * @param session The active session details to persist.
     * @return [Result.Success] with [Unit] when persisted, or [Result.Error] on validation/persistence failure.
     */
    suspend operator fun invoke(session: ActiveSession): Result<Unit, DomainError>
}
