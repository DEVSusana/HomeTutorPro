package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.data.local.dao.AgentContextDao
import com.devsusana.hometutorpro.domain.entities.AgentBalanceSummary
import com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.AgentStudentSummary
import com.devsusana.hometutorpro.domain.repository.AgentContextRepository
import javax.inject.Inject

/**
 * Implementation of [AgentContextRepository] that delegates to [AgentContextDao].
 *
 * Automatically resolves the current professor's ID from [SecureAuthManager]
 * to scope all queries to the authenticated user's data.
 */
class AgentContextRepositoryImpl @Inject constructor(
    private val agentContextDao: AgentContextDao,
    private val secureAuthManager: SecureAuthManager
) : AgentContextRepository {

    private val professorId: String
        get() = secureAuthManager.getUserId()
            ?: throw IllegalStateException("No authenticated user found. Cannot query agent context.")

    override suspend fun getAllStudentSummaries(): List<AgentStudentSummary> =
        agentContextDao.getAllStudentSummariesForAgent(professorId)

    override suspend fun getAllSchedules(): List<AgentScheduleSummary> =
        agentContextDao.getAllSchedulesForAgent(professorId)

    override suspend fun getStudentsWithBalance(): List<AgentBalanceSummary> =
        agentContextDao.getStudentsWithBalance(professorId)

    override suspend fun searchStudentByName(query: String): List<AgentStudentDetail> =
        agentContextDao.searchStudentByName(professorId, query)

    override suspend fun getActiveStudentCount(): Int =
        agentContextDao.getActiveStudentCount(professorId)

    override suspend fun getScheduleDetails(): List<AgentScheduleDetail> =
        agentContextDao.getAllScheduleDetailsForAgent(professorId)

    override suspend fun getSchedulesByStudentName(studentName: String): List<AgentScheduleDetail> =
        agentContextDao.getSchedulesByStudentName(professorId, studentName)
}
