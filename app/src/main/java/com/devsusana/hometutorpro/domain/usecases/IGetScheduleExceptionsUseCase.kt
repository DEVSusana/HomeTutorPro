package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.ScheduleException
import kotlinx.coroutines.flow.Flow

/**
 * Use case contract for GetScheduleExceptions operations.
 */
interface IGetScheduleExceptionsUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(professorId: String, studentId: String): Flow<List<ScheduleException>>
}
