package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.Schedule
import kotlinx.coroutines.flow.Flow

/**
 * Use case contract for GetAllSchedules operations.
 */
interface IGetAllSchedulesUseCase {
    /**
     * Executes the use case.
     */
    operator fun invoke(professorId: String): Flow<List<Schedule>>
}
