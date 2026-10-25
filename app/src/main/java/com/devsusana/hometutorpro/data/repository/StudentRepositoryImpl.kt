package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.dao.SharedResourceDao
import com.devsusana.hometutorpro.data.local.dao.ClassLogDao
import com.devsusana.hometutorpro.data.local.dao.TransactionLogDao
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.data.local.entities.TransactionLogEntity
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.mappers.toEntity
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.data.util.toRoomId
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.text.Collator
import java.util.Locale

/**
 * Hybrid implementation of StudentRepository for Premium flavor.
 * Uses Room as the single source of truth for the UI (offline-first).
 * Schedules WorkManager syncs for cloud persistence.
 */
class StudentRepositoryImpl @Inject constructor(
    private val studentDao: StudentDao,
    private val scheduleDao: ScheduleDao,
    private val scheduleExceptionDao: ScheduleExceptionDao,
    private val resourceDao: com.devsusana.hometutorpro.data.local.dao.ResourceDao,
    private val sharedResourceDao: SharedResourceDao,
    private val classLogDao: ClassLogDao,
    private val transactionLogDao: TransactionLogDao,
    private val authRepository: AuthRepository,
    private val dateTimeProvider: DateTimeProvider,
    private val syncScheduler: SyncScheduler
) : StudentRepository {

    // Define constant for Transaction types
    private companion object {
        const val TRANSACTION_TYPE_PAYMENT = "PAYMENT"
        const val TRANSACTION_TYPE_BALANCE_ADD = "BALANCE_ADD"
        const val TRANSACTION_TYPE_BALANCE_SUBTRACT = "BALANCE_SUBTRACT"
    }

    override fun getStudents(professorId: String): Flow<List<StudentSummary>> {
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY // Ignore accents and case
        }
        return studentDao.getStudentSummaries(professorId).map { entities ->
            entities.map { it.toDomain() }
                .sortedWith { s1, s2 -> collator.compare(s1.name, s2.name) }
        }.flowOn(Dispatchers.Default)
    }

    override fun getStudentById(professorId: String, studentId: String): Flow<Student?> {
        val id = studentId.toRoomId() ?: return kotlinx.coroutines.flow.flowOf(null)
        return studentDao.getStudentById(id, professorId).map { it?.toDomain() }.flowOn(Dispatchers.Default)
    }

    override suspend fun saveStudent(professorId: String, student: Student): Result<String, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val existingId = if (student.id.isNotEmpty()) student.id.toRoomId() ?: 0L else 0L
                val entity = student.copy(professorId = professorId).toEntity(
                    existingId = existingId,
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
                
                val finalId = if (existingId > 0) {
                    studentDao.updateStudent(entity)
                    existingId
                } else {
                    studentDao.insertStudent(entity)
                }
                syncScheduler.scheduleSyncNow()

                Result.Success(finalId.toString())
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override suspend fun registerPayment(
        professorId: String,
        studentId: String,
        amountPaid: Double,
        paymentType: PaymentType
    ): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                if (amountPaid <= 0) return@withContext Result.Error(DomainError.InvalidAmount)
                val id = studentId.toRoomId() ?: return@withContext Result.Error(DomainError.StudentNotFound)
                
                // Fetch student once
                val student = studentDao.getStudentById(id, professorId).firstOrNull()
                    ?: return@withContext Result.Error(DomainError.StudentNotFound)

                // Atomic subtraction
                studentDao.subtractFromBalance(
                    studentId = id,
                    professorId = professorId,
                    amount = amountPaid,
                    paymentDate = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    timestamp = System.currentTimeMillis()
                )

                transactionLogDao.insertTransaction(
                    TransactionLogEntity(
                        professorId = professorId,
                        studentId = id,
                        type = TRANSACTION_TYPE_PAYMENT,
                        amount = amountPaid,
                        paymentType = paymentType.name,
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Result.Error(DomainError.Unknown)
            }
        }
    }

    /**
     * Adds an amount to a student's pending balance.
     */
    override suspend fun addToBalance(
        professorId: String,
        studentId: String,
        amount: Double
    ): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                if (amount <= 0) return@withContext Result.Error(DomainError.InvalidAmount)
                val id = studentId.toRoomId() ?: return@withContext Result.Error(DomainError.StudentNotFound)

                val student = studentDao.getStudentById(id, professorId).firstOrNull()
                    ?: return@withContext Result.Error(DomainError.StudentNotFound)

                studentDao.addToBalance(
                    studentId = id,
                    professorId = professorId,
                    amount = amount,
                    classDate = dateTimeProvider.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    timestamp = dateTimeProvider.currentTimeMillis()
                )

                syncScheduler.scheduleSyncNow()

                Result.Success(Unit)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Result.Error(DomainError.Unknown)
            }
        }
    }

    /**
     * Updates a student's pending balance to a specific value and records an adjustment transaction.
     */
    override suspend fun updateBalance(
        professorId: String,
        studentId: String,
        newBalance: Double
    ): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val id = studentId.toRoomId() ?: return@withContext Result.Error(DomainError.StudentNotFound)

                val student = studentDao.getStudentById(id, professorId).firstOrNull()
                    ?: return@withContext Result.Error(DomainError.StudentNotFound)

                val diff = newBalance - student.pendingBalance
                if (diff != 0.0) {
                    transactionLogDao.insertTransaction(
                        TransactionLogEntity(
                            professorId = professorId,
                            studentId = id,
                            type = if (diff > 0) TRANSACTION_TYPE_BALANCE_ADD else TRANSACTION_TYPE_BALANCE_SUBTRACT,
                            amount = kotlin.math.abs(diff),
                            paymentType = null,
                            timestamp = dateTimeProvider.currentTimeMillis()
                        )
                    )
                }

                studentDao.updateBalanceOnly(
                    studentId = id,
                    professorId = professorId,
                    newBalance = newBalance,
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    timestamp = dateTimeProvider.currentTimeMillis()
                )

                syncScheduler.scheduleSyncNow()

                Result.Success(Unit)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Result.Error(DomainError.Unknown)
            }
        }
    }

    /**
     * Retrieves observable schedules for a specific student.
     */
    override fun getSchedules(professorId: String, studentId: String): Flow<List<Schedule>> {
        val id = studentId.toRoomId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return scheduleDao.getSchedulesByStudentId(id, professorId).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Retrieves all observable schedules for a professor.
     */
    override fun getAllSchedules(professorId: String): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedulesWithStudent(professorId).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Persists or updates a schedule for a student.
     */
    override suspend fun saveSchedule(
        professorId: String,
        studentId: String,
        schedule: Schedule
    ): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val sId = studentId.toRoomId() ?: return@withContext Result.Error(DomainError.StudentNotFound)
                val existingId = if (schedule.id.isNotEmpty()) schedule.id.toRoomId() ?: 0L else 0L
                
                val entity = schedule.toEntity(
                    studentId = sId,
                    professorId = professorId,
                    existingId = existingId,
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
                
                scheduleDao.insertSchedule(entity)
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(DomainError.Unknown)
            }
        }
    }

    /**
     * Checks if a schedule conflicts with an existing schedule for the professor.
     */
    override suspend fun getConflictingSchedule(
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        scheduleId: String?
    ): Schedule? {
        return withContext(Dispatchers.IO) {
            val professorId = authRepository.currentUser.value?.uid ?: return@withContext null
            val sId = scheduleId?.toRoomId()
            scheduleDao.getConflictingSchedule(dayOfWeek, startTime, endTime, professorId, sId)?.toDomain()
        }
    }

    /**
     * Deletes a schedule by marking it for deletion in local storage.
     */
    override suspend fun deleteSchedule(
        professorId: String,
        studentId: String,
        scheduleId: String
    ): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val id = scheduleId.toRoomId() ?: return@withContext Result.Error(DomainError.Unknown)
                
                scheduleDao.markForDeletion(id, professorId)
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(DomainError.Unknown)
            }
        }
    }

    /**
     * Toggles completion status of a schedule and manages corresponding class logs.
     */
    override suspend fun toggleScheduleCompletion(
        professorId: String,
        scheduleId: String
    ): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val id = scheduleId.toRoomId() ?: return@withContext Result.Error(DomainError.Unknown)
                
                // Direct lookup instead of loading all schedules (O(1) vs O(n))
                val currentSchedule = scheduleDao.getScheduleById(id, professorId)
                    ?: return@withContext Result.Error(DomainError.Unknown)
                
                val newCompletionStatus = !currentSchedule.isCompleted
                val completedDate = if (newCompletionStatus) dateTimeProvider.currentTimeMillis() else null
                
                if (newCompletionStatus) {
                    classLogDao.insertLog(
                        com.devsusana.hometutorpro.data.local.entities.ClassLogEntity(
                            professorId = professorId,
                            studentId = currentSchedule.studentId,
                            scheduleId = scheduleId,
                            date = dateTimeProvider.currentTimeMillis(),
                            startTime = currentSchedule.startTime,
                            endTime = currentSchedule.endTime,
                            isExtra = false
                        )
                    )
                } else {
                    deleteTodayLogForSchedule(currentSchedule.studentId, professorId, scheduleId)
                }

                scheduleDao.updateCompletionStatus(
                    id = id,
                    professorId = professorId,
                    isCompleted = newCompletionStatus,
                    completedDate = completedDate,
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    timestamp = dateTimeProvider.currentTimeMillis()
                )
                
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(DomainError.Unknown)
            }
        }
    }

    private suspend fun deleteTodayLogForSchedule(studentId: Long, professorId: String, scheduleId: String) {
        val now = dateTimeProvider.getNow()
        val todayStart = now.toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1
        val logs = classLogDao.getLogsByStudent(studentId, professorId)
        val matchingLog = logs.firstOrNull { it.scheduleId == scheduleId && it.date in todayStart..todayEnd }
        if (matchingLog != null) {
            classLogDao.deleteLogById(matchingLog.id, professorId)
        }
    }

    override suspend fun deleteStudent(professorId: String, studentId: String): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val id = studentId.toRoomId() ?: return@withContext Result.Error(DomainError.StudentNotFound)
                
                // 1. Mark all associated data for deletion
                scheduleDao.markSchedulesForDeletionByStudentId(id, professorId)
                scheduleExceptionDao.markExceptionsForDeletionByStudentId(id, professorId)
                sharedResourceDao.markSharedResourcesForDeletionByStudent(id, professorId)
                
                // 2. Mark student for deletion
                studentDao.markForDeletion(id, professorId)
                
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override suspend fun rescueOrphanedData(professorId: String): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                studentDao.assignOrphanedDataToProfessor(professorId)
                scheduleDao.assignOrphanedDataToProfessor(professorId)
                scheduleExceptionDao.assignOrphanedDataToProfessor(professorId)
                resourceDao.assignOrphanedDataToProfessor(professorId)
                sharedResourceDao.assignOrphanedDataToProfessor(professorId)
                
                Result.Success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(DomainError.Unknown)
            }
        }
    }
}
