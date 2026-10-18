package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import com.devsusana.hometutorpro.domain.usecases.IGetAllScheduleExceptionsUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case implementation for observing all schedule exceptions for a professor.
 */
class GetAllScheduleExceptionsUseCase @Inject constructor(
    private val repository: ScheduleExceptionRepository
) : IGetAllScheduleExceptionsUseCase {
    override operator fun invoke(professorId: String): Flow<List<ScheduleException>> {
        return repository.getAllExceptionsFlow(professorId)
    }
}
