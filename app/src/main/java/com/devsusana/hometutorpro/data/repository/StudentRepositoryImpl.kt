package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.dao.SharedResourceDao
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.mappers.toEntity
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Hybrid implementation of StudentRepository for Premium flavor.
 * Uses Room as the single source of truth for the UI (offline-first).
 * Schedules WorkManager syncs for cloud persistence.
 */
class StudentRepositoryImpl @Inject constructor(
    private val studentDao: StudentDao,
    private val scheduleDao: ScheduleDao,
    private val scheduleExceptionDao: ScheduleExceptionDao,
    private val sharedResourceDao: SharedResourceDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val syncScheduler: SyncScheduler
) : StudentRepository {

    override fun getStudents(professorId: String): Flow<List<Student>> {
        return studentDao.getAllStudents().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getStudentById(professorId: String, studentId: String): Flow<Student?> {
        val id = studentId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(null)
        return studentDao.getStudentById(id).map { it?.toDomain() }
    }

    override suspend fun saveStudent(professorId: String, student: Student): Result<String, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val existingId = if (student.id.isNotEmpty()) student.id.toLongOrNull() ?: 0L else 0L
                val entity = student.toEntity(
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
                val id = studentId.toLongOrNull() ?: return@withContext Result.Error(DomainError.StudentNotFound)
                
                val studentEntity = studentDao.getStudentById(id).firstOrNull() 
                    ?: return@withContext Result.Error(DomainError.StudentNotFound)
                
                val currentBalance = studentEntity.pendingBalance
                val newBalance = currentBalance - amountPaid
                
                studentDao.updatePendingBalance(
                    studentId = id,
                    newBalance = newBalance,
                    paymentDate = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    timestamp = System.currentTimeMillis()
                )
                
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override fun getSchedules(professorId: String, studentId: String): Flow<List<Schedule>> {
        val id = studentId.toLongOrNull() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return scheduleDao.getSchedulesByStudentId(id).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllSchedules(professorId: String): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveSchedule(
        professorId: String,
        studentId: String,
        schedule: Schedule
    ): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val sId = studentId.toLongOrNull() ?: return@withContext Result.Error(DomainError.StudentNotFound)
                val existingId = if (schedule.id.isNotEmpty()) schedule.id.toLongOrNull() ?: 0L else 0L
                
                val entity = schedule.toEntity(
                    studentId = sId,
                    existingId = existingId,
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
                
                scheduleDao.insertSchedule(entity)
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override suspend fun deleteSchedule(
        professorId: String,
        studentId: String,
        scheduleId: String
    ): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val id = scheduleId.toLongOrNull() ?: return@withContext Result.Error(DomainError.Unknown)
                
                scheduleDao.markForDeletion(id)
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override suspend fun toggleScheduleCompletion(
        professorId: String,
        scheduleId: String
    ): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val id = scheduleId.toLongOrNull() ?: return@withContext Result.Error(DomainError.Unknown)
                
                // Get current schedule to toggle its status
                val currentSchedule = scheduleDao.getAllSchedules().first()
                    .find { it.id == id }
                    ?: return@withContext Result.Error(DomainError.Unknown)
                
                val newCompletionStatus = !currentSchedule.isCompleted
                val completedDate = if (newCompletionStatus) System.currentTimeMillis() else null
                
                scheduleDao.updateCompletionStatus(
                    id = id,
                    isCompleted = newCompletionStatus,
                    completedDate = completedDate,
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    timestamp = System.currentTimeMillis()
                )
                
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override suspend fun deleteStudent(professorId: String, studentId: String): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val id = studentId.toLongOrNull() ?: return@withContext Result.Error(DomainError.StudentNotFound)
                
                // 1. Mark all associated data for deletion
                scheduleDao.markSchedulesForDeletionByStudentId(id)
                scheduleExceptionDao.markExceptionsForDeletionByStudentId(id)
                sharedResourceDao.markSharedResourcesForDeletionByStudent(id)
                
                // 2. Mark student for deletion
                studentDao.markForDeletion(id)
                
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }
}
