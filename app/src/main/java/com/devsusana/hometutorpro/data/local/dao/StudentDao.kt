package com.devsusana.hometutorpro.data.local.dao

import androidx.room.*
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Student operations in premium flavor.
 * Includes sync-specific queries for offline-first architecture.
 */
@Dao
interface StudentDao {

    @Query("SELECT * FROM students WHERE pendingDelete = 0 ORDER BY name ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :studentId AND pendingDelete = 0")
    fun getStudentById(studentId: Long): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE cloudId = :cloudId AND pendingDelete = 0")
    suspend fun getStudentByCloudId(cloudId: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("UPDATE students SET pendingBalance = :newBalance, lastPaymentDate = :paymentDate, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :studentId")
    suspend fun updatePendingBalance(
        studentId: Long,
        newBalance: Double,
        paymentDate: Long,
        syncStatus: SyncStatus,
        timestamp: Long
    )

    // Sync-specific queries
    @Query("SELECT * FROM students WHERE syncStatus = :status")
    suspend fun getStudentsBySyncStatus(status: SyncStatus): List<StudentEntity>

    @Query("SELECT * FROM students WHERE lastModifiedTimestamp > :timestamp")
    suspend fun getStudentsModifiedSince(timestamp: Long): List<StudentEntity>

    @Query("UPDATE students SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE students SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id")
    suspend fun markForDeletion(id: Long, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM students WHERE pendingDelete = 1 AND syncStatus = :syncStatus")
    suspend fun deleteSyncedPendingDeletes(syncStatus: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()
}
