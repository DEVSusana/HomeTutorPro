package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ActiveSession

/**
 * Use case to start a new active tutoring session.
 */
interface IStartActiveSessionUseCase {
    suspend operator fun invoke(session: ActiveSession)
}
