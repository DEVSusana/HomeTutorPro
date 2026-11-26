package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case to stop and clear the active tutoring session.
 */
interface IStopActiveSessionUseCase {
    /**
     * Executes stopping and clearing the active tutoring session.
     *
     * @return [Result.Success] with [Unit] when cleared, or [Result.Error] on persistence failure.
     */
    suspend operator fun invoke(): Result<Unit, DomainError>
}
