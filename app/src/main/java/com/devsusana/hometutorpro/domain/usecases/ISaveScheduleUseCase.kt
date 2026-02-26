package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.Schedule

/**
 * Use case contract for SaveSchedule operations.
 */
interface ISaveScheduleUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(professorId: String, studentId: String, schedule: Schedule): Result<Unit, DomainError>
}
