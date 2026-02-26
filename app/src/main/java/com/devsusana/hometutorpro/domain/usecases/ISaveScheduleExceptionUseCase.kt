package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ScheduleException

/**
 * Use case contract for SaveScheduleException operations.
 */
interface ISaveScheduleExceptionUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(
        professorId: String,
        studentId: String,
        exception: ScheduleException
    ): Result<Unit, DomainError>
}
