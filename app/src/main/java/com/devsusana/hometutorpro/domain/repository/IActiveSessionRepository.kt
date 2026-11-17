package com.devsusana.hometutorpro.domain.repository

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
     * @return Unit indicating the session has been persisted.
     */
    suspend fun startSession(session: ActiveSession)

    /**
     * Stops and clears the current active tutoring session.
     *
     * @return Unit indicating that any active session has been successfully cleared,
     * or that no action was needed if no session was active.
     */
    suspend fun stopSession()
}
