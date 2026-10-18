package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ScheduleException
import kotlinx.coroutines.flow.Flow

/**
 * Contract for observing all schedule exceptions for a professor.
 */
interface IGetAllScheduleExceptionsUseCase {
    /**
     * Observes all schedule exceptions for the given [professorId].
     */
    operator fun invoke(professorId: String): Flow<List<ScheduleException>>
}
