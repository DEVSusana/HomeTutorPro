package com.devsusana.hometutorpro.data.local.dao

import androidx.room.*
import com.devsusana.hometutorpro.data.local.entities.ScheduleExceptionEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for ScheduleException operations in premium flavor.
 * Includes sync-specific queries.
 */
@Dao
interface ScheduleExceptionDao {

    @Query("SELECT * FROM schedule_exceptions WHERE studentId = :studentId AND professorId = :professorId AND pendingDelete = 0 ORDER BY exceptionDate DESC")
    fun getExceptionsByStudentId(studentId: Long, professorId: String): Flow<List<ScheduleExceptionEntity>>

    @Query("SELECT * FROM schedule_exceptions WHERE professorId = :professorId AND pendingDelete = 0 ORDER BY exceptionDate DESC")
    fun getAllExceptions(professorId: String): Flow<List<ScheduleExceptionEntity>>

    @Query("SELECT * FROM schedule_exceptions WHERE professorId = :professorId ORDER BY id ASC")
    suspend fun getAllExceptionsOnce(professorId: String): List<ScheduleExceptionEntity>

    @Query("SELECT * FROM schedule_exceptions WHERE cloudId = :cloudId AND professorId = :professorId AND pendingDelete = 0")
    suspend fun getExceptionByCloudId(cloudId: String, professorId: String): ScheduleExceptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertException(exception: ScheduleExceptionEntity): Long

    @Update
    suspend fun updateException(exception: ScheduleExceptionEntity)

    @Delete
    suspend fun deleteException(exception: ScheduleExceptionEntity)

    @Query("DELETE FROM schedule_exceptions WHERE id = :exceptionId AND professorId = :professorId")
    suspend fun deleteExceptionById(exceptionId: Long, professorId: String)

    @Query("DELETE FROM schedule_exceptions WHERE studentId = :studentId AND professorId = :professorId")
    suspend fun deleteExceptionsByStudentId(studentId: Long, professorId: String)

    // Sync-specific queries
    @Query("SELECT * FROM schedule_exceptions WHERE professorId = :professorId AND syncStatus = :status")
    suspend fun getExceptionsBySyncStatus(professorId: String, status: SyncStatus): List<ScheduleExceptionEntity>

    @Query("SELECT * FROM schedule_exceptions WHERE professorId = :professorId AND lastModifiedTimestamp > :timestamp")
    suspend fun getExceptionsModifiedSince(professorId: String, timestamp: Long): List<ScheduleExceptionEntity>

    @Query("UPDATE schedule_exceptions SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun updateSyncStatus(id: Long, professorId: String, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE schedule_exceptions SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun markForDeletion(id: Long, professorId: String, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE schedule_exceptions SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE studentId = :studentId AND professorId = :professorId")
    suspend fun markExceptionsForDeletionByStudentId(studentId: Long, professorId: String, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM schedule_exceptions WHERE pendingDelete = 1 AND syncStatus = :syncStatus AND professorId = :professorId")
    suspend fun deleteSyncedPendingDeletes(professorId: String, syncStatus: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM schedule_exceptions WHERE professorId = :professorId")
    suspend fun deleteAllExceptions(professorId: String)
}
