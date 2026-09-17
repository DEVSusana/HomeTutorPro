package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.repository.AgentContextRepository
import com.devsusana.hometutorpro.domain.usecases.IQuerySchedulesForAgentUseCase
import javax.inject.Inject

/**
 * Default implementation of [IQuerySchedulesForAgentUseCase].
 * Delegates to [AgentContextRepository] which handles professor scoping.
 */
class QuerySchedulesForAgentUseCase @Inject constructor(
    private val repository: AgentContextRepository
) : IQuerySchedulesForAgentUseCase {

    override suspend fun getAllSchedules(): List<AgentScheduleSummary> =
        repository.getAllSchedules()

    override suspend fun getScheduleDetails(): List<AgentScheduleDetail> =
        repository.getScheduleDetails()

    override suspend fun getSchedulesByStudentName(studentName: String): List<AgentScheduleDetail> =
        repository.getSchedulesByStudentName(studentName)
}
