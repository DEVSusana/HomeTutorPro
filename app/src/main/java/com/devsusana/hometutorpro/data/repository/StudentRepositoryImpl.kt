package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleDao
import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.dao.SharedResourceDao
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
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
import com.devsusana.hometutorpro.domain.repository.StudentRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val syncScheduler: SyncScheduler
) : StudentRepository {

    override fun getStudents(professorId: String): Flow<List<StudentSummary>> {
        val collator = Collator.getInstance(Locale("es", "ES")).apply {
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
                
                // Atomic subtraction — no TOCTOU race condition
                studentDao.subtractFromBalance(
                    studentId = id,
                    professorId = professorId,
                    amount = amountPaid,
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
        val id = studentId.toRoomId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return scheduleDao.getSchedulesByStudentId(id, professorId).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
    }

    override fun getAllSchedules(professorId: String): Flow<List<Schedule>> {
        return scheduleDao.getAllSchedulesWithStudent(professorId).map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
    }

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
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override suspend fun getConflictingSchedule(
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        scheduleId: String?
    ): Schedule? {
        return withContext(Dispatchers.IO) {
            val professorId = auth.currentUser?.uid ?: return@withContext null
            val sId = scheduleId?.toRoomId()
            scheduleDao.getConflictingSchedule(dayOfWeek, startTime, endTime, professorId, sId)?.toDomain()
        }
    }

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
                val id = scheduleId.toRoomId() ?: return@withContext Result.Error(DomainError.Unknown)
                
                // Direct lookup instead of loading all schedules (O(1) vs O(n))
                val currentSchedule = scheduleDao.getScheduleById(id, professorId)
                    ?: return@withContext Result.Error(DomainError.Unknown)
                
                val newCompletionStatus = !currentSchedule.isCompleted
                val completedDate = if (newCompletionStatus) System.currentTimeMillis() else null
                
                scheduleDao.updateCompletionStatus(
                    id = id,
                    professorId = professorId,
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
                Result.Error(DomainError.Unknown)
            }
        }
    }
}
