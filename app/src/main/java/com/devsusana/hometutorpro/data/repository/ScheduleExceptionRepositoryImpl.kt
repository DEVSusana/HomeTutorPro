package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.data.local.dao.ScheduleExceptionDao
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.data.mappers.toDomain
import com.devsusana.hometutorpro.data.mappers.toEntity
import com.devsusana.hometutorpro.data.sync.SyncScheduler
import com.devsusana.hometutorpro.data.util.toRoomId
import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Hybrid implementation of ScheduleExceptionRepository for Premium flavor.
 * Uses Room as the single source of truth for the UI (offline-first).
 */
class ScheduleExceptionRepositoryImpl @Inject constructor(
    private val exceptionDao: ScheduleExceptionDao,
    private val syncScheduler: SyncScheduler
) : ScheduleExceptionRepository {

    override fun getExceptions(professorId: String, studentId: String): Flow<List<ScheduleException>> {
        val id = studentId.toRoomId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return exceptionDao.getExceptionsByStudentId(id, professorId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveException(professorId: String, studentId: String, exception: ScheduleException): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val sId = studentId.toRoomId() ?: return@withContext Result.Error(DomainError.Unknown)
                val existingId = if (exception.id.isNotEmpty()) exception.id.toRoomId() ?: 0L else 0L
                val entity = exception.toEntity(
                    studentId = sId,
                    professorId = professorId,
                    existingId = existingId,
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
                
                exceptionDao.insertException(entity)
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override suspend fun deleteException(professorId: String, studentId: String, exceptionId: String): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                val id = exceptionId.toRoomId() ?: return@withContext Result.Error(DomainError.Unknown)
                
                exceptionDao.markForDeletion(id, professorId)
                syncScheduler.scheduleSyncNow()
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }

    override suspend fun cleanupDuplicates(): Result<Unit, DomainError> {
        return withContext(Dispatchers.IO) {
            try {
                exceptionDao.deleteDuplicates()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(DomainError.Unknown)
            }
        }
    }
}
