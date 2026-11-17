package com.devsusana.hometutorpro.domain.usecases

/**
 * Use case to stop and clear the active tutoring session.
 */
interface IStopActiveSessionUseCase {
    /**
     * Executes stopping and clearing the active tutoring session.
     *
     * @return Unit indicating that any active session has been successfully cleared,
     * or that no action was needed if no session was active.
     */
    suspend operator fun invoke()
}
