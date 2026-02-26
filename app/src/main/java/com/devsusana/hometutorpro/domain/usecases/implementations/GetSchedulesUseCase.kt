package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.StudentRepository

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.usecases.IGetSchedulesUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Default implementation of [IGetSchedulesUseCase].
 */
class GetSchedulesUseCase @Inject constructor(private val repository: StudentRepository) :
    IGetSchedulesUseCase {
    override operator fun invoke(professorId: String, studentId: String): Flow<List<Schedule>> = repository.getSchedules(professorId, studentId)
}
