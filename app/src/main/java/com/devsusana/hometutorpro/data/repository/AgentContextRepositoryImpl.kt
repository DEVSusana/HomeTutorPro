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

    override suspend fun getAllStudentSummaries(): List<AgentStudentSummary> =
        studentDao.getAllStudentSummariesForAgent(professorId)

    override suspend fun getAllSchedules(): List<AgentScheduleSummary> =
        scheduleDao.getAllSchedulesForAgent(professorId)

    override suspend fun getStudentsWithBalance(): List<AgentBalanceSummary> =
        studentDao.getStudentsWithBalance(professorId)

    override suspend fun searchStudentByName(query: String): List<AgentStudentDetail> =
        studentDao.searchStudentByName(professorId, query)

    override suspend fun getActiveStudentCount(): Int =
        studentDao.getActiveStudentCount(professorId)

    override suspend fun getScheduleDetails(): List<AgentScheduleDetail> =
        scheduleDao.getAllScheduleDetailsForAgent(professorId)

    override suspend fun getSchedulesByStudentName(studentName: String): List<AgentScheduleDetail> =
        scheduleDao.getSchedulesByStudentName(professorId, studentName)

    override suspend fun getClassLogsForStudent(studentId: String): List<AgentClassLog> {
        val sId = studentId.toRoomId() ?: return emptyList()
        return classLogDao.getLogsByStudent(sId, professorId).map {
            AgentClassLog(
                studentId = it.studentId.toString(),
                scheduleId = it.scheduleId,
                date = it.date,
                startTime = it.startTime,
                endTime = it.endTime,
                isExtra = it.isExtra
            )
        }
    }

    override suspend fun getAllClassLogs(): List<AgentClassLog> =
        classLogDao.getAllLogs(professorId).map {
            AgentClassLog(
                studentId = it.studentId.toString(),
                scheduleId = it.scheduleId,
                date = it.date,
                startTime = it.startTime,
                endTime = it.endTime,
                isExtra = it.isExtra
            )
        }

    override suspend fun getTransactionsForStudent(studentId: String): List<AgentTransactionLog> {
        val sId = studentId.toRoomId() ?: return emptyList()
        return transactionLogDao.getTransactionsByStudent(sId, professorId).map {
            AgentTransactionLog(
                studentId = it.studentId.toString(),
                type = it.type,
                amount = it.amount,
                paymentType = it.paymentType,
                timestamp = it.timestamp
            )
        }
    }

    override suspend fun getAllTransactions(): List<AgentTransactionLog> =
        transactionLogDao.getAllTransactions(professorId).map {
            AgentTransactionLog(
                studentId = it.studentId.toString(),
                type = it.type,
                amount = it.amount,
                paymentType = it.paymentType,
                timestamp = it.timestamp
            )
        }
}
