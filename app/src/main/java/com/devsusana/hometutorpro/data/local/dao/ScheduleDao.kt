package com.devsusana.hometutorpro.data.local.dao

import androidx.room.*
import com.devsusana.hometutorpro.data.local.entities.ScheduleEntity
import com.devsusana.hometutorpro.data.local.entities.ScheduleWithStudent
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Schedule operations in premium flavor.
 * Includes sync-specific queries.
 */
@Dao
interface ScheduleDao {

    @Query("""
        SELECT s.*, st.name as studentName, st.subjects as studentSubjects, 
               st.color as studentColor, st.isActive as studentIsActive,
               st.pendingBalance as studentPendingBalance, st.pricePerHour as studentPricePerHour
        FROM schedules s
        JOIN students st ON s.studentId = st.id
        WHERE s.professorId = :professorId 
        AND s.pendingDelete = 0 
        AND st.pendingDelete = 0
        ORDER BY s.dayOfWeek, s.startTime
    """)
    fun getAllSchedulesWithStudent(professorId: String): Flow<List<ScheduleWithStudent>>

    @Query("SELECT * FROM schedules WHERE studentId = :studentId AND professorId = :professorId AND pendingDelete = 0 ORDER BY dayOfWeek, startTime")
    fun getSchedulesByStudentId(studentId: Long, professorId: String): Flow<List<ScheduleEntity>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM schedules 
            WHERE dayOfWeek = :dayOfWeek 
            AND professorId = :professorId
            AND pendingDelete = 0 
            AND (:id IS NULL OR id != :id)
            AND (:startTime < endTime AND :endTime > startTime)
        )
    """)
    suspend fun hasConflict(dayOfWeek: Int, startTime: String, endTime: String, professorId: String, id: Long? = null): Boolean

    @Query("""
        SELECT s.*, st.name as studentName 
        FROM schedules s
        JOIN students st ON s.studentId = st.id
        WHERE s.dayOfWeek = :dayOfWeek 
        AND s.professorId = :professorId
        AND s.pendingDelete = 0 
        AND (:id IS NULL OR s.id != :id)
        AND (:startTime < s.endTime AND :endTime > s.startTime)
        LIMIT 1
    """)
    suspend fun getConflictingSchedule(dayOfWeek: Int, startTime: String, endTime: String, professorId: String, id: Long? = null): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE professorId = :professorId AND pendingDelete = 0 ORDER BY dayOfWeek, startTime")
    fun getAllSchedules(professorId: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE professorId = :professorId ORDER BY id ASC")
    suspend fun getAllSchedulesOnce(professorId: String): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :scheduleId AND professorId = :professorId AND pendingDelete = 0")
    suspend fun getScheduleById(scheduleId: Long, professorId: String): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE cloudId = :cloudId AND professorId = :professorId AND pendingDelete = 0")
    suspend fun getScheduleByCloudId(cloudId: String, professorId: String): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :scheduleId AND professorId = :professorId")
    suspend fun deleteScheduleById(scheduleId: Long, professorId: String)

    @Query("DELETE FROM schedules WHERE studentId = :studentId AND professorId = :professorId")
    suspend fun deleteSchedulesByStudentId(studentId: Long, professorId: String)

    // Sync-specific queries
    @Query("SELECT * FROM schedules WHERE professorId = :professorId AND syncStatus = :status")
    suspend fun getSchedulesBySyncStatus(professorId: String, status: SyncStatus): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE professorId = :professorId AND lastModifiedTimestamp > :timestamp")
    suspend fun getSchedulesModifiedSince(professorId: String, timestamp: Long): List<ScheduleEntity>

    @Query("UPDATE schedules SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun updateSyncStatus(id: Long, professorId: String, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE schedules SET isCompleted = :isCompleted, completedDate = :completedDate, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun updateCompletionStatus(
        id: Long,
        professorId: String,
        isCompleted: Boolean,
        completedDate: Long?,
        syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE schedules SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun markForDeletion(id: Long, professorId: String, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE schedules SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE studentId = :studentId AND professorId = :professorId")
    suspend fun markSchedulesForDeletionByStudentId(studentId: Long, professorId: String, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM schedules WHERE pendingDelete = 1 AND syncStatus = :syncStatus AND professorId = :professorId")
    suspend fun deleteSyncedPendingDeletes(professorId: String, syncStatus: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM schedules WHERE professorId = :professorId")
    suspend fun deleteAllSchedules(professorId: String)
}
