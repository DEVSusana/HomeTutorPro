package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.data.models.AgentBalanceSummary
import com.devsusana.hometutorpro.data.models.AgentStudentDetail
import com.devsusana.hometutorpro.data.models.AgentStudentSummary

/**
 * Use case contract for querying student data on behalf of the Sue AI agent.
 */
interface IQueryStudentsForAgentUseCase {

    /**
     * Returns summaries of all students for the current professor.
     */
    suspend fun getAllStudents(): List<AgentStudentSummary>

    /**
     * Searches students by name.
     */
    suspend fun searchByName(query: String): List<AgentStudentDetail>

    /**
     * Returns students with non-zero pending balance.
     */
    suspend fun getStudentsWithBalance(): List<AgentBalanceSummary>

    /**
     * Returns the count of active students.
     */
    suspend fun getActiveStudentCount(): Int
}
