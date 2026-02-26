package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleExceptionUseCase
import javax.inject.Inject

/**
 * Use case implementation for deletescheduleexception operations.
 */
class DeleteScheduleExceptionUseCase @Inject constructor(
    private val repository: ScheduleExceptionRepository
) : IDeleteScheduleExceptionUseCase {
    override suspend operator fun invoke(
        professorId: String,
        studentId: String,
        exceptionId: String
    ): Result<Unit, DomainError> {
        return repository.deleteException(professorId, studentId, exceptionId)
    }
}
