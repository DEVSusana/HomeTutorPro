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
import com.devsusana.hometutorpro.data.local.dao.ResourceDao
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
 * Repository implementation for managing student data and schedules.
 * Uses Room as the single source of truth for offline-first local persistence.
 * Notifies SyncScheduler to orchestrate background synchronization when active.
 */
class StudentRepositoryImpl @Inject constructor(
    private val studentDao: StudentDao,
    private val scheduleDao: ScheduleDao,
    private val scheduleExceptionDao: ScheduleExceptionDao,
    private val resourceDao: ResourceDao,
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

    /**
     * Retrieves observable student summaries ordered alphabetically by name.
     */
    override fun getStudents(professorId: String): Flow<List<StudentSummary>> {
        val collator = Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY // Ignore accents and case
        }
        return studentDao.getStudentSummaries(professorId).map { entities ->
            entities.map { it.toDomain() }
                .sortedWith { s1, s2 -> collator.compare(s1.name, s2.name) }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * Retrieves an observable student domain model by ID.
     */
    override fun getStudentById(professorId: String, studentId: String): Flow<Student?> {
        val id = studentId.toRoomId() ?: return kotlinx.coroutines.flow.flowOf(null)
        return studentDao.getStudentById(id, professorId).map { it?.toDomain() }.flowOn(Dispatchers.Default)
    }

    /**
     * Executes a suspending database block within [Dispatchers.IO], re-throwing [CancellationException]
     * and encapsulating unexpected failures into [Result.Error(DomainError.Unknown)].
     */
    private suspend inline fun <T> safeIoCall(crossinline block: suspend () -> Result<T, DomainError>): Result<T, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.Error(DomainError.Unknown)
            }
        }
    }

    /**
     * Persists or updates a student in local database and triggers background synchronization.
     *
     * @param professorId Owner professor identifier.
     * @param student Domain student model to save or update.
     * @return [Result.Success] with the persisted student ID string, or [Result.Error].
     */
    override suspend fun saveStudent(professorId: String, student: Student): Result<String, DomainError> = safeIoCall {
        val existingId = if (student.id.isNotEmpty()) {
            student.id.toRoomId() ?: return@safeIoCall Result.Error(DomainError.StudentNotFound)
        } else 0L
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
    }

    /**
     * Finds a student entity by ID and verifies ownership.
     *
     * @param professorId Owner professor identifier.
     * @param studentId Domain student identifier.
     * @return [Result.Success] with Room ID and entity, or [Result.Error] if not found or invalid.
     */
    private suspend fun findStudent(professorId: String, studentId: String): Result<Pair<Long, com.devsusana.hometutorpro.data.local.entities.StudentEntity>, DomainError> {
        val id = studentId.toRoomId() ?: return Result.Error(DomainError.StudentNotFound)
        val student = studentDao.getStudentById(id, professorId).firstOrNull()
            ?: return Result.Error(DomainError.StudentNotFound)
        return Result.Success(id to student)
    }

    /**
     * Atomically records a student payment and creates a transaction log entry.
     *
     * @param professorId Owner professor identifier.
     * @param studentId Target student identifier.
     * @param amountPaid Monetary payment amount.
     * @param paymentType Type of payment executed.
     * @return [Result.Success] when recorded, or [Result.Error] on validation/persistence failure.
     */
    override suspend fun registerPayment(
        professorId: String,
        studentId: String,
        amountPaid: Double,
        paymentType: PaymentType
    ): Result<Unit, DomainError> = safeIoCall {
        if (amountPaid <= 0) return@safeIoCall Result.Error(DomainError.InvalidAmount)
        val (id, _) = when (val res = findStudent(professorId, studentId)) {
            is Result.Success -> res.data
            is Result.Error -> return@safeIoCall Result.Error(res.error)
        }

        val now = dateTimeProvider.currentTimeMillis()

        // Atomic subtraction
        studentDao.subtractFromBalance(
            studentId = id,
            professorId = professorId,
            amount = amountPaid,
            paymentDate = now,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            timestamp = now
        )

        transactionLogDao.insertTransaction(
            TransactionLogEntity(
                professorId = professorId,
                studentId = id,
                type = TRANSACTION_TYPE_PAYMENT,
                amount = amountPaid,
                paymentType = paymentType.name,
                timestamp = now
            )
        )
        
        syncScheduler.scheduleSyncNow()
        
        Result.Success(Unit)
    }

    /**
     * Adds an amount to a student's pending balance.
     *
     * @param professorId Owner professor identifier.
     * @param studentId Target student identifier.
     * @param amount Monetary amount to add.
     * @return [Result.Success] on success, or [Result.Error] on failure.
     */
    override suspend fun addToBalance(
        professorId: String,
        studentId: String,
        amount: Double
    ): Result<Unit, DomainError> = safeIoCall {
        if (amount <= 0) return@safeIoCall Result.Error(DomainError.InvalidAmount)
        val (id, _) = when (val res = findStudent(professorId, studentId)) {
            is Result.Success -> res.data
            is Result.Error -> return@safeIoCall Result.Error(res.error)
        }

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
    }

    /**
     * Updates only [lastClassDate] for a student without touching pending balance or other fields.
     */
    override suspend fun updateLastClassDate(
        professorId: String,
        studentId: String,
        classDate: Long
    ): Result<Unit, DomainError> = safeIoCall {
        val (id, _) = when (val res = findStudent(professorId, studentId)) {
            is Result.Success -> res.data
            is Result.Error -> return@safeIoCall Result.Error(res.error)
        }

        studentDao.updateLastClassDate(
            studentId = id,
            professorId = professorId,
            classDate = classDate,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            timestamp = dateTimeProvider.currentTimeMillis()
        )

        syncScheduler.scheduleSyncNow()

        Result.Success(Unit)
    }

    /**
     * Updates a student's pending balance to a specific value and records an adjustment transaction.
     *
     * @param professorId Owner professor identifier.
     * @param studentId Target student identifier.
     * @param newBalance New target pending balance.
     * @return [Result.Success] on success, or [Result.Error] on failure.
     */
    override suspend fun updateBalance(
        professorId: String,
        studentId: String,
        newBalance: Double
    ): Result<Unit, DomainError> = safeIoCall {
        val (id, student) = when (val res = findStudent(professorId, studentId)) {
            is Result.Success -> res.data
            is Result.Error -> return@safeIoCall Result.Error(res.error)
        }

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
     *
     * @param professorId Owner professor identifier.
     * @param studentId Domain student identifier.
     * @param schedule Schedule entity to save.
     * @return [Result.Success] on success, or [Result.Error] on failure.
     */
    override suspend fun saveSchedule(
        professorId: String,
        studentId: String,
        schedule: Schedule
    ): Result<Unit, DomainError> = safeIoCall {
        val sId = studentId.toRoomId() ?: return@safeIoCall Result.Error(DomainError.StudentNotFound)
        val existingId = if (schedule.id.isNotEmpty()) {
            schedule.id.toRoomId() ?: return@safeIoCall Result.Error(DomainError.Unknown)
        } else 0L
        
        val entity = schedule.toEntity(
            studentId = sId,
            professorId = professorId,
            existingId = existingId,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        
        scheduleDao.insertSchedule(entity)
        syncScheduler.scheduleSyncNow()
        
        Result.Success(Unit)
    }

    /**
     * Checks if a schedule conflicts with an existing schedule for the professor.
     *
     * @param dayOfWeek Day index (1 = Monday, 7 = Sunday).
     * @param startTime Start time string HH:mm.
     * @param endTime End time string HH:mm.
     * @param scheduleId Optional schedule ID to exclude from conflict check (for edits).
     * @return Conflicting [Schedule] if collision exists, or null otherwise.
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
     * Deletes a schedule by marking it for deletion in local storage (soft-delete).
     *
     * @param professorId Owner professor identifier.
     * @param studentId Domain student identifier.
     * @param scheduleId Domain schedule identifier.
     * @return [Result.Success] on success, or [Result.Error] on failure.
     */
    override suspend fun deleteSchedule(
        professorId: String,
        studentId: String,
        scheduleId: String
    ): Result<Unit, DomainError> = safeIoCall {
        val id = scheduleId.toRoomId() ?: return@safeIoCall Result.Error(DomainError.Unknown)
        
        scheduleDao.markForDeletion(id, professorId)
        syncScheduler.scheduleSyncNow()
        
        Result.Success(Unit)
    }

    /**
     * Toggles completion status of a schedule and manages corresponding class logs.
     *
     * @param professorId Owner professor identifier.
     * @param scheduleId Domain schedule identifier.
     * @return [Result.Success] on success, or [Result.Error] on failure.
     */
    override suspend fun toggleScheduleCompletion(
        professorId: String,
        scheduleId: String
    ): Result<Unit, DomainError> = safeIoCall {
        val id = scheduleId.toRoomId() ?: return@safeIoCall Result.Error(DomainError.Unknown)
        
        // Direct lookup instead of loading all schedules (O(1) vs O(n))
        val currentSchedule = scheduleDao.getScheduleById(id, professorId)
            ?: return@safeIoCall Result.Error(DomainError.Unknown)
        
        val newCompletionStatus = !currentSchedule.isCompleted
        val completedDate = if (newCompletionStatus) dateTimeProvider.currentTimeMillis() else null
        
        handleClassLog(
            professorId = professorId,
            studentId = currentSchedule.studentId,
            scheduleId = scheduleId,
            startTime = currentSchedule.startTime,
            endTime = currentSchedule.endTime,
            isCompleted = newCompletionStatus
        )

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
    }

    private suspend fun handleClassLog(
        professorId: String,
        studentId: Long,
        scheduleId: String,
        startTime: String,
        endTime: String,
        isCompleted: Boolean
    ) {
        if (isCompleted) {
            classLogDao.insertLog(
                com.devsusana.hometutorpro.data.local.entities.ClassLogEntity(
                    professorId = professorId,
                    studentId = studentId,
                    scheduleId = scheduleId,
                    date = dateTimeProvider.currentTimeMillis(),
                    startTime = startTime,
                    endTime = endTime,
                    isExtra = false
                )
            )
        } else {
            deleteTodayLogForSchedule(studentId, professorId, scheduleId)
        }
    }

    private suspend fun deleteTodayLogForSchedule(studentId: Long, professorId: String, scheduleId: String) {
        val todayStart = dateTimeProvider.getStartOfDayMillis()
        val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1
        val logs = classLogDao.getLogsByStudent(studentId, professorId)
        val matchingLog = logs.firstOrNull { it.scheduleId == scheduleId && it.date in todayStart..todayEnd }
        if (matchingLog != null) {
            classLogDao.deleteLogById(matchingLog.id, professorId)
        }
    }

    /**
     * Soft-deletes a student and all their associated schedules, exceptions, and resources.
     *
     * @param professorId Owner professor identifier.
     * @param studentId Domain student identifier.
     * @return [Result.Success] on success, or [Result.Error] on failure.
     */
    override suspend fun deleteStudent(professorId: String, studentId: String): Result<Unit, DomainError> = safeIoCall {
        val id = studentId.toRoomId() ?: return@safeIoCall Result.Error(DomainError.StudentNotFound)
        
        // 1. Mark all associated data for deletion
        scheduleDao.markSchedulesForDeletionByStudentId(id, professorId)
        scheduleExceptionDao.markExceptionsForDeletionByStudentId(id, professorId)
        sharedResourceDao.markSharedResourcesForDeletionByStudent(id, professorId)
        
        // 2. Mark student for deletion
        studentDao.markForDeletion(id, professorId)
        
        syncScheduler.scheduleSyncNow()
        
        Result.Success(Unit)
    }

    /**
     * Rescues orphaned local data without an owner and assigns it to the current professor.
     *
     * @param professorId Target professor identifier.
     * @return [Result.Success] on success, or [Result.Error] on failure.
     */
    override suspend fun rescueOrphanedData(professorId: String): Result<Unit, DomainError> = safeIoCall {
        studentDao.assignOrphanedDataToProfessor(professorId)
        scheduleDao.assignOrphanedDataToProfessor(professorId)
        scheduleExceptionDao.assignOrphanedDataToProfessor(professorId)
        resourceDao.assignOrphanedDataToProfessor(professorId)
        sharedResourceDao.assignOrphanedDataToProfessor(professorId)
        
        Result.Success(Unit)
    }
}
