package com.devsusana.hometutorpro.data.local.dao

import androidx.room.*
import com.devsusana.hometutorpro.data.local.entities.ScheduleEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Schedule operations in premium flavor.
 * Includes sync-specific queries.
 */
@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules WHERE studentId = :studentId AND pendingDelete = 0 ORDER BY dayOfWeek, startTime")
    fun getSchedulesByStudentId(studentId: Long): Flow<List<ScheduleEntity>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM schedules 
            WHERE dayOfWeek = :dayOfWeek 
            AND pendingDelete = 0 
            AND (:id IS NULL OR id != :id)
            AND (:startTime < endTime AND :endTime > startTime)
        )
    """)
    suspend fun hasConflict(dayOfWeek: Int, startTime: String, endTime: String, id: Long? = null): Boolean

    @Query("""
        SELECT s.*, st.name as studentName 
        FROM schedules s
        JOIN students st ON s.studentId = st.id
        WHERE s.dayOfWeek = :dayOfWeek 
        AND s.pendingDelete = 0 
        AND (:id IS NULL OR s.id != :id)
        AND (:startTime < s.endTime AND :endTime > s.startTime)
        LIMIT 1
    """)
    suspend fun getConflictingSchedule(dayOfWeek: Int, startTime: String, endTime: String, id: Long? = null): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE pendingDelete = 0 ORDER BY dayOfWeek, startTime")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules ORDER BY id ASC")
    suspend fun getAllSchedulesOnce(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE cloudId = :cloudId AND pendingDelete = 0")
    suspend fun getScheduleByCloudId(cloudId: String): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :scheduleId")
    suspend fun deleteScheduleById(scheduleId: Long)

    @Query("DELETE FROM schedules WHERE studentId = :studentId")
    suspend fun deleteSchedulesByStudentId(studentId: Long)

    // Sync-specific queries
    @Query("SELECT * FROM schedules WHERE syncStatus = :status")
    suspend fun getSchedulesBySyncStatus(status: SyncStatus): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE lastModifiedTimestamp > :timestamp")
    suspend fun getSchedulesModifiedSince(timestamp: Long): List<ScheduleEntity>

    @Query("UPDATE schedules SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE schedules SET isCompleted = :isCompleted, completedDate = :completedDate, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateCompletionStatus(
        id: Long,
        isCompleted: Boolean,
        completedDate: Long?,
        syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE schedules SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun markForDeletion(id: Long, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE schedules SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE studentId = :studentId")
    suspend fun markSchedulesForDeletionByStudentId(studentId: Long, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM schedules WHERE pendingDelete = 1 AND syncStatus = :syncStatus")
    suspend fun deleteSyncedPendingDeletes(syncStatus: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()
}
