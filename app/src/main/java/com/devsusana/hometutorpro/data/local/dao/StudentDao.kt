package com.devsusana.hometutorpro.data.local.dao

import androidx.room.*
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.StudentSummaryEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import com.devsusana.hometutorpro.data.local.models.BalanceSummaryProjection
import com.devsusana.hometutorpro.data.local.models.StudentDetailProjection
import com.devsusana.hometutorpro.data.local.models.StudentSummaryProjection
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Student operations in local database.
 * Includes queries for offline-first architecture and sync-ready tracking.
 */
@Dao
interface StudentDao {

    /**
     * Retrieves observable student summaries for a specific professor.
     */
    @Query("SELECT id, name, subjects, color, pendingBalance, pricePerHour, isActive, lastClassDate FROM students WHERE professorId = :professorId AND pendingDelete = 0 ORDER BY name ASC")
    fun getStudentSummaries(professorId: String): Flow<List<StudentSummaryEntity>>

    /**
     * Retrieves observable full student entities for a specific professor.
     */
    @Query("SELECT * FROM students WHERE professorId = :professorId AND pendingDelete = 0 ORDER BY name ASC")
    fun getAllStudents(professorId: String): Flow<List<StudentEntity>>

    /**
     * Retrieves a one-shot list of all students for a specific professor.
     */
    @Query("SELECT * FROM students WHERE professorId = :professorId ORDER BY id ASC")
    suspend fun getAllStudentsOnce(professorId: String): List<StudentEntity>

    /**
     * Retrieves an observable student entity by its local id.
     */
    @Query("SELECT * FROM students WHERE id = :studentId AND professorId = :professorId AND pendingDelete = 0")
    fun getStudentById(studentId: Long, professorId: String): Flow<StudentEntity?>

    /**
     * Retrieves a student by their remote cloud identifier.
     */
    @Query("SELECT * FROM students WHERE cloudId = :cloudId AND professorId = :professorId AND pendingDelete = 0")
    suspend fun getStudentByCloudId(cloudId: String, professorId: String): StudentEntity?

    /**
     * Inserts or replaces a student entity in local storage.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    /**
     * Updates an existing student entity.
     */
    @Update
    suspend fun updateStudent(student: StudentEntity)

    /**
     * Deletes a student entity permanently from local storage.
     */
    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    /**
     * Updates student balance and last payment date.
     */
    @Query("UPDATE students SET pendingBalance = :newBalance, lastPaymentDate = :paymentDate, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :studentId AND professorId = :professorId")
    suspend fun updatePendingBalance(
        studentId: Long,
        professorId: String,
        newBalance: Double,
        paymentDate: Long,
        syncStatus: SyncStatus,
        timestamp: Long
    )

    /**
     * Updates only the student balance without altering payment timestamp.
     */
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

    /**
     * Atomic lastClassDate update — used for free classes to avoid full-entity overwrites
     * that could race with concurrent payment operations.
     */
    @Query("UPDATE students SET lastClassDate = :classDate, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :studentId AND professorId = :professorId")
    suspend fun updateLastClassDate(
        studentId: Long,
        professorId: String,
        classDate: Long,
        syncStatus: SyncStatus,
        timestamp: Long
    )

    /**
     * Retrieves students matching a specific synchronization status.
     *
     * @param professorId Owner professor identifier.
     * @param status Synchronization status filter.
     * @return List of matching [StudentEntity] records.
     */
    @Query("SELECT * FROM students WHERE professorId = :professorId AND syncStatus = :status")
    suspend fun getStudentsBySyncStatus(professorId: String, status: SyncStatus): List<StudentEntity>

    /**
     * Retrieves students modified after a given epoch timestamp.
     *
     * @param professorId Owner professor identifier.
     * @param timestamp Epoch timestamp filter in milliseconds.
     * @return List of modified [StudentEntity] records.
     */
    @Query("SELECT * FROM students WHERE professorId = :professorId AND lastModifiedTimestamp > :timestamp")
    suspend fun getStudentsModifiedSince(professorId: String, timestamp: Long): List<StudentEntity>

    /**
     * Updates the synchronization status and modification timestamp of a student.
     *
     * @param id Target student ID.
     * @param professorId Owner professor identifier.
     * @param status New [SyncStatus] value.
     * @param timestamp Modification timestamp in milliseconds.
     */
    @Query("UPDATE students SET syncStatus = :status, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun updateSyncStatus(id: Long, professorId: String, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    /**
     * Marks a student for soft deletion.
     *
     * @param id Target student ID.
     * @param professorId Owner professor identifier.
     * @param syncStatus Synchronization status flag.
     * @param timestamp Modification timestamp in milliseconds.
     */
    @Query("UPDATE students SET pendingDelete = 1, syncStatus = :syncStatus, lastModifiedTimestamp = :timestamp WHERE id = :id AND professorId = :professorId")
    suspend fun markForDeletion(id: Long, professorId: String, syncStatus: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    /**
     * Permanently deletes students marked for deletion whose deletion has synced.
     *
     * @param professorId Owner professor identifier.
     * @param syncStatus Target sync status confirmation.
     */
    @Query("DELETE FROM students WHERE pendingDelete = 1 AND syncStatus = :syncStatus AND professorId = :professorId")
    suspend fun deleteSyncedPendingDeletes(professorId: String, syncStatus: SyncStatus = SyncStatus.SYNCED)

    /**
     * Permanently deletes all students belonging to a professor.
     *
     * @param professorId Owner professor identifier.
     */
    @Query("DELETE FROM students WHERE professorId = :professorId")
    suspend fun deleteAllStudents(professorId: String)

    /**
     * Assigns orphaned student records without an owner to a professor.
     *
     * @param professorId Target professor identifier.
     */
    @Query("UPDATE students SET professorId = :professorId WHERE professorId = '' OR professorId IS NULL")
    suspend fun assignOrphanedDataToProfessor(professorId: String)

    /**
     * Returns a summary of all active, non-deleted students for the given professor.
     *
     * @param professorId The ID of the professor.
     * @return A list of [StudentSummaryProjection] representing the student records.
     */
    @Query(
        """
        SELECT s.name, s.subjects, s.course, s.pricePerHour, s.pendingBalance, s.isActive, s.lastPaymentDate
        FROM students s 
        WHERE s.professorId = :professorId AND s.pendingDelete = 0
        ORDER BY s.name ASC
        """
    )
    suspend fun getAllStudentSummaries(professorId: String): List<StudentSummaryProjection>

    /**
     * Returns students with non-zero pending balance for the given professor.
     *
     * @param professorId The ID of the professor.
     * @return A list of [BalanceSummaryProjection] for students with outstanding balances.
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
    suspend fun getStudentsWithBalance(professorId: String): List<BalanceSummaryProjection>

    /**
     * Searches for students whose name matches the given query (case-insensitive).
     *
     * @param professorId The ID of the professor.
     * @param query The search term to match against student names.
     * @return A list of [StudentDetailProjection] matching the search.
     */
    @Query(
        """
        SELECT s.id AS studentId, s.name, s.subjects, s.course, s.pricePerHour, s.address, s.studentPhone, s.parentPhones, s.studentEmail, s.pendingBalance, s.lastPaymentDate, s.notes, s.educationalAttention
        FROM students s 
        WHERE s.professorId = :professorId 
        AND LOWER(s.name) LIKE '%' || LOWER(:query) || '%' 
        AND s.pendingDelete = 0
        ORDER BY s.name ASC
        """
    )
    suspend fun searchStudentByName(professorId: String, query: String): List<StudentDetailProjection>

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

