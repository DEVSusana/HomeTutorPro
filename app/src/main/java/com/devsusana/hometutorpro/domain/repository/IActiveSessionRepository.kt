package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ActiveSession

/**
 * Interface defining contract for active tutoring session state persistence.
 */
interface IActiveSessionRepository {
    /**
     * Retrieves the current ongoing tutoring session, if any.
     *
     * @return [ActiveSession] if an active session exists, null otherwise.
     */
    suspend fun getActiveSession(): ActiveSession?

    /**
     * Starts and persists a new active tutoring session.
     *
     * @param session The active session details to persist.
     * @return [Result.Success] with [Unit] when persisted, or [Result.Error] with [DomainError] on failure.
     */
    suspend fun startSession(session: ActiveSession): Result<Unit, DomainError>

    /**
     * Stops and clears the current active tutoring session.
     *
     * @return [Result.Success] with [Unit] when cleared, or [Result.Error] with [DomainError] on failure.
     */
    suspend fun stopSession(): Result<Unit, DomainError>
}
