package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ActiveSession

/**
 * Use case to retrieve the current active tutoring session.
 */
interface IGetActiveSessionUseCase {
    operator fun invoke(): ActiveSession?
}
