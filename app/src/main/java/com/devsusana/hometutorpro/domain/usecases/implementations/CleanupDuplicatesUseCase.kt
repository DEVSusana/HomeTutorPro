package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import javax.inject.Inject

/**
 * Use case for cleaning up existing duplicated schedule exceptions.
 */
class CleanupDuplicatesUseCase @Inject constructor(
    private val repository: ScheduleExceptionRepository
) {
    suspend operator fun invoke(): Result<Unit, DomainError> {
        return repository.cleanupDuplicates()
    }
}
