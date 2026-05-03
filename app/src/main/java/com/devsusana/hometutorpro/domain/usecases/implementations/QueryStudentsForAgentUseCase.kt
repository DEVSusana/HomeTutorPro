package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.data.models.AgentBalanceSummary
import com.devsusana.hometutorpro.data.models.AgentStudentDetail
import com.devsusana.hometutorpro.data.models.AgentStudentSummary
import com.devsusana.hometutorpro.domain.repository.AgentContextRepository
import com.devsusana.hometutorpro.domain.usecases.IQueryStudentsForAgentUseCase
import javax.inject.Inject

/**
 * Default implementation of [IQueryStudentsForAgentUseCase].
 * Delegates to [AgentContextRepository] which handles professor scoping.
 */
class QueryStudentsForAgentUseCase @Inject constructor(
    private val repository: AgentContextRepository
) : IQueryStudentsForAgentUseCase {

    override suspend fun getAllStudents(): List<AgentStudentSummary> =
        repository.getAllStudentSummaries()

    override suspend fun searchByName(query: String): List<AgentStudentDetail> =
        repository.searchStudentByName(query)

    override suspend fun getStudentsWithBalance(): List<AgentBalanceSummary> =
        repository.getStudentsWithBalance()

    override suspend fun getActiveStudentCount(): Int =
        repository.getActiveStudentCount()
}
