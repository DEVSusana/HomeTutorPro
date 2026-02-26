package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Schedule
import kotlinx.coroutines.flow.Flow

/**
 * Use case contract for GetSchedules operations.
 */
interface IGetSchedulesUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(professorId: String, studentId: String): Flow<List<Schedule>>
}
