package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.data.security.SecureAuthManager
import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleDao
import com.devsusana.hometutorpro.domain.entities.AgentBalanceSummary
import com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.data.local.dao.ClassLogDao
import com.devsusana.hometutorpro.data.local.dao.TransactionLogDao
import com.devsusana.hometutorpro.data.util.toRoomId
import com.devsusana.hometutorpro.data.mappers.toAgentDomain
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.domain.entities.AgentClassLog
import com.devsusana.hometutorpro.domain.entities.AgentTransactionLog
import com.devsusana.hometutorpro.domain.entities.AgentStudentSummary
import com.devsusana.hometutorpro.domain.repository.AgentContextRepository
import javax.inject.Inject

/**
 * Implementation of [AgentContextRepository] that delegates queries to [StudentDao], [ScheduleDao], and log DAOs.
 *
 * Automatically resolves the current professor's ID from [SecureAuthManager]
 * to scope all queries to the authenticated user's data.
 */
class AgentContextRepositoryImpl @Inject constructor(
    private val studentDao: StudentDao,
    private val scheduleDao: ScheduleDao,
    private val secureAuthManager: SecureAuthManager,
    private val classLogDao: ClassLogDao,
    private val transactionLogDao: TransactionLogDao
) : AgentContextRepository {

    private val professorId: String
        get() = secureAuthManager.getUserId()
            ?: throw IllegalStateException("No authenticated user found. Cannot query agent context.")

    /**
     * Retrieves all student summaries for the authenticated professor.
     */
    override suspend fun getAllStudentSummaries(): List<AgentStudentSummary> =
        studentDao.getAllStudentSummaries(professorId).map { it.toDomain() }

    /**
     * Retrieves all schedule summaries for the authenticated professor.
     */
    override suspend fun getAllSchedules(): List<AgentScheduleSummary> =
        scheduleDao.getAllScheduleSummaries(professorId).map { it.toDomain() }

    /**
     * Retrieves summary of students with outstanding balances.
     */
    override suspend fun getStudentsWithBalance(): List<AgentBalanceSummary> =
        studentDao.getStudentsWithBalance(professorId).map { it.toDomain() }

    /**
     * Searches for student records matching a name query.
     */
    override suspend fun searchStudentByName(query: String): List<AgentStudentDetail> =
        studentDao.searchStudentByName(professorId, query).map { it.toDomain() }

    /**
     * Returns total count of active students for the current professor.
     */
    override suspend fun getActiveStudentCount(): Int =
        studentDao.getActiveStudentCount(professorId)

    /**
     * Retrieves detailed schedule records including IDs.
     */
    override suspend fun getScheduleDetails(): List<AgentScheduleDetail> =
        scheduleDao.getAllScheduleDetails(professorId).map { it.toDomain() }

    /**
     * Retrieves schedules for students matching [studentName].
     */
    override suspend fun getSchedulesByStudentName(studentName: String): List<AgentScheduleDetail> =
        scheduleDao.getSchedulesByStudentName(professorId, studentName).map { it.toDomain() }

    /**
     * Retrieves class logs for a specific student.
     */
    override suspend fun getClassLogsForStudent(studentId: String): List<AgentClassLog> {
        val sId = studentId.toRoomId() ?: return emptyList()
        return classLogDao.getLogsByStudent(sId, professorId).map { it.toAgentDomain() }
    }

    /**
     * Retrieves all class logs for the current professor.
     */
    override suspend fun getAllClassLogs(): List<AgentClassLog> =
        classLogDao.getAllLogs(professorId).map { it.toAgentDomain() }

    /**
     * Retrieves transaction history for a specific student.
     */
    override suspend fun getTransactionsForStudent(studentId: String): List<AgentTransactionLog> {
        val sId = studentId.toRoomId() ?: return emptyList()
        return transactionLogDao.getTransactionsByStudent(sId, professorId).map { it.toAgentDomain() }
    }

    /**
     * Retrieves all financial transactions recorded by the professor.
     */
    override suspend fun getAllTransactions(): List<AgentTransactionLog> =
        transactionLogDao.getAllTransactions(professorId).map { it.toAgentDomain() }
}
