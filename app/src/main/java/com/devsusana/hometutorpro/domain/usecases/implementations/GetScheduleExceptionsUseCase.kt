package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import com.devsusana.hometutorpro.domain.usecases.IGetScheduleExceptionsUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetScheduleExceptionsUseCase @Inject constructor(
    private val repository: ScheduleExceptionRepository
) : IGetScheduleExceptionsUseCase {
    override operator fun invoke(professorId: String, studentId: String): Flow<List<ScheduleException>> {
        return repository.getExceptions(professorId, studentId)
    }
}
