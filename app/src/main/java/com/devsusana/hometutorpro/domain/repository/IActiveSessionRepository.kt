package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.entities.ActiveSession

/**
 * Interface for active tutoring session state persistence.
 */
interface IActiveSessionRepository {
    fun getActiveSession(): ActiveSession?
    fun startSession(session: ActiveSession)
    fun stopSession()
}
