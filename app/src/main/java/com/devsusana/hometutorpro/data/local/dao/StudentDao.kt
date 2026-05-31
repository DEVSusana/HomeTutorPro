package com.devsusana.hometutorpro.data.local.dao

import androidx.room.*
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.StudentSummaryEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Student operations in premium flavor.
 * Includes sync-specific queries for offline-first architecture.
 */
@Dao
interface StudentDao {

    @Query("SELECT id, name, subjects, color, pendingBalance, pricePerHour, isActive, lastClassDate FROM students WHERE professorId = :professorId AND pendingDelete = 0 ORDER BY name ASC")
    fun getStudentSummaries(professorId: String): Flow<List<StudentSummaryEntity>>

    @Query("SELECT * FROM students WHERE professorId = :professorId AND pendingDelete = 0 ORDER BY name ASC")
    fun getAllStudents(professorId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE professorId = :professorId ORDER BY id ASC")
    suspend fun getAllStudentsOnce(professorId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :studentId AND professorId = :professorId AND pendingDelete = 0")
    fun getStudentById(studentId: Long, professorId: String): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE cloudId = :cloudId AND professorId = :professorId AND pendingDelete = 0")
    suspend fun getStudentByCloudId(cloudId: String, professorId: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("UPDATE students SET pendingBalance = :newBalance, lastPaymentDate = :paymentDate, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :studentId AND professorId = :professorId")
    suspend fun updatePendingBalance(
        studentId: Long,
        professorId: String,
        newBalance: Double,
        paymentDate: Long,
        syncStatus: SyncStatus,
        timestamp: Long
    )

    @Query("UPDATE students SET pendingBalance = :newBalance, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :studentId AND professorId = :professorId")
    suspend fun updateBalanceOnly(
        studentId: Long,
        professorId: String,
        newBalance: Double,
        syncStatus: SyncStatus,
        timestamp: Long
    )

    /**
     * Atomic balance subtraction — prevents TOCTOU race conditions on concurrent payments.
     */
    @Query("UPDATE students SET pendingBalance = pendingBalance - :amount, lastPaymentDate = :paymentDate, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :studentId AND professorId = :professorId")
    suspend fun subtractFromBalance(
        studentId: Long,
        professorId: String,
        amount: Double,
        paymentDate: Long,
        syncStatus: SyncStatus,
        timestamp: Long
    )

    /**
     * Atomic balance addition — used by "start class" to avoid full-entity overwrites
     * that could race with concurrent payment operations.
     */
    @Query("UPDATE students SET pendingBalance = pendingBalance + :amount, lastClassDate = :classDate, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :studentId AND professorId = :professorId")
    suspend fun addToBalance(
        studentId: Long,
        professorId: String,
        amount: Double,
        classDate: Long,
        syncStatus: SyncStatus,
        timestamp: Long
    )

    // Sync-specific queries
    @Query("SELECT * FROM students WHERE professorId = :professorId AND syncStatus = :status")
    suspend fun getStudentsBySyncStatus(professorId: String, status: SyncStatus): List<StudentEntity>

    @Query("SELECT * FROM students WHERE professorId = :professorId AND lastModifiedTimestamp > :timestamp")
    suspend fun getStudentsModifiedSince(professorId: String, timestamp: Long): List<StudentEntity>

    @Query("UPDATE students SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun updateSyncStatus(id: Long, professorId: String, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE students SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun markForDeletion(id: Long, professorId: String, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM students WHERE pendingDelete = 1 AND syncStatus = :syncStatus AND professorId = :professorId")
    suspend fun deleteSyncedPendingDeletes(professorId: String, syncStatus: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM students WHERE professorId = :professorId")
    suspend fun deleteAllStudents(professorId: String)

    @Query("UPDATE students SET professorId = :professorId WHERE professorId = '' OR professorId IS NULL")
    suspend fun assignOrphanedDataToProfessor(professorId: String)

    /**
     * Returns a summary of all active, non-deleted students for the given professor.
     *
     * @param professorId The ID of the professor.
     * @return A list of [com.devsusana.hometutorpro.domain.entities.AgentStudentSummary] representing the student records.
     */
    @Query(
        """
        SELECT s.name, s.subjects, s.course, s.pricePerHour, s.pendingBalance, s.isActive, s.lastPaymentDate
        FROM students s 
        WHERE s.professorId = :professorId AND s.pendingDelete = 0
        ORDER BY s.name ASC
        """
    )
    suspend fun getAllStudentSummariesForAgent(professorId: String): List<com.devsusana.hometutorpro.domain.entities.AgentStudentSummary>

    /**
     * Returns students with non-zero pending balance for the given professor.
     *
     * @param professorId The ID of the professor.
     * @return A list of [com.devsusana.hometutorpro.domain.entities.AgentBalanceSummary] for students with outstanding balances.
     */
    @Query(
        """
        SELECT s.name, s.pendingBalance 
        FROM students s 
        WHERE s.professorId = :professorId 
        AND s.pendingBalance != 0 
        AND s.pendingDelete = 0
        ORDER BY s.pendingBalance DESC
        """
    )
    suspend fun getStudentsWithBalance(professorId: String): List<com.devsusana.hometutorpro.domain.entities.AgentBalanceSummary>

    /**
     * Searches for students whose name matches the given query (case-insensitive).
     *
     * @param professorId The ID of the professor.
     * @param query The search term to match against student names.
     * @return A list of [com.devsusana.hometutorpro.domain.entities.AgentStudentDetail] matching the search.
     */
    @Query(
        """
        SELECT s.id AS studentId, s.name, s.subjects, s.course, s.pendingBalance, s.lastPaymentDate
        FROM students s 
        WHERE s.professorId = :professorId 
        AND LOWER(s.name) LIKE '%' || LOWER(:query) || '%' 
        AND s.pendingDelete = 0
        ORDER BY s.name ASC
        """
    )
    suspend fun searchStudentByName(professorId: String, query: String): List<com.devsusana.hometutorpro.domain.entities.AgentStudentDetail>

    /**
     * Returns the total count of active, non-deleted students for the given professor.
     *
     * @param professorId The ID of the professor.
     * @return The integer count of active students.
     */
    @Query(
        """
        SELECT COUNT(*) 
        FROM students s 
        WHERE s.professorId = :professorId 
        AND s.isActive = 1 
        AND s.pendingDelete = 0
        """
    )
    suspend fun getActiveStudentCount(professorId: String): Int
}

