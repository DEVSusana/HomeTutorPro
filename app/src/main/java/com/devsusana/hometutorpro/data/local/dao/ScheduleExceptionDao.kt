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

    @Query("SELECT * FROM schedule_exceptions WHERE studentId = :studentId AND pendingDelete = 0 ORDER BY exceptionDate DESC")
    fun getExceptionsByStudentId(studentId: Long): Flow<List<ScheduleExceptionEntity>>

    @Query("SELECT * FROM schedule_exceptions WHERE pendingDelete = 0 ORDER BY exceptionDate DESC")
    fun getAllExceptions(): Flow<List<ScheduleExceptionEntity>>

    @Query("SELECT * FROM schedule_exceptions ORDER BY id ASC")
    suspend fun getAllExceptionsOnce(): List<ScheduleExceptionEntity>

    @Query("SELECT * FROM schedule_exceptions WHERE cloudId = :cloudId AND pendingDelete = 0")
    suspend fun getExceptionByCloudId(cloudId: String): ScheduleExceptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertException(exception: ScheduleExceptionEntity): Long

    @Update
    suspend fun updateException(exception: ScheduleExceptionEntity)

    @Delete
    suspend fun deleteException(exception: ScheduleExceptionEntity)

    @Query("DELETE FROM schedule_exceptions WHERE id = :exceptionId")
    suspend fun deleteExceptionById(exceptionId: Long)

    @Query("DELETE FROM schedule_exceptions WHERE studentId = :studentId")
    suspend fun deleteExceptionsByStudentId(studentId: Long)

    // Sync-specific queries
    @Query("SELECT * FROM schedule_exceptions WHERE syncStatus = :status")
    suspend fun getExceptionsBySyncStatus(status: SyncStatus): List<ScheduleExceptionEntity>

    @Query("SELECT * FROM schedule_exceptions WHERE lastModifiedTimestamp > :timestamp")
    suspend fun getExceptionsModifiedSince(timestamp: Long): List<ScheduleExceptionEntity>

    @Query("UPDATE schedule_exceptions SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE schedule_exceptions SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun markForDeletion(id: Long, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE schedule_exceptions SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE studentId = :studentId")
    suspend fun markExceptionsForDeletionByStudentId(studentId: Long, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM schedule_exceptions WHERE pendingDelete = 1 AND syncStatus = :syncStatus")
    suspend fun deleteSyncedPendingDeletes(syncStatus: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM schedule_exceptions")
    suspend fun deleteAllExceptions()
}
