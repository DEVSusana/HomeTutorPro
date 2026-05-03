package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.data.models.AgentScheduleSummary

/**
 * Use case contract for querying schedule data on behalf of the Sue AI agent.
 */
interface IQuerySchedulesForAgentUseCase {

    /**
     * Returns all schedules for the current professor, joined with student names.
     */
    suspend fun getAllSchedules(): List<AgentScheduleSummary>
}
