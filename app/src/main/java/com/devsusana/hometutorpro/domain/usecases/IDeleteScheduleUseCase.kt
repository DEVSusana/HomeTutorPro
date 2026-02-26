package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case contract for DeleteSchedule operations.
 */
interface IDeleteScheduleUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(professorId: String, studentId: String, scheduleId: String): Result<Unit, DomainError>
}
