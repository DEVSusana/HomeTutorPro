package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.devsusana.hometutorpro.domain.usecases.IGetAllSchedulesUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case implementation for getallschedules operations.
 */
class GetAllSchedulesUseCase @Inject constructor(
    private val repository: StudentRepository
) : IGetAllSchedulesUseCase {
    override operator fun invoke(professorId: String): Flow<List<Schedule>> {
        return repository.getAllSchedules(professorId)
    }
}
