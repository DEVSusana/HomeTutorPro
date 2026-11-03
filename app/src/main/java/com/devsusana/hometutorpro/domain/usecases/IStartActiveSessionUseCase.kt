package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ActiveSession

/**
 * Use case to start a new active tutoring session.
 */
interface IStartActiveSessionUseCase {
    /**
     * Executes starting and persisting an active tutoring session.
     *
     * @param session The active session details to persist.
     * @return Unit indicating the session has been persisted.
     */
    suspend operator fun invoke(session: ActiveSession)
}
