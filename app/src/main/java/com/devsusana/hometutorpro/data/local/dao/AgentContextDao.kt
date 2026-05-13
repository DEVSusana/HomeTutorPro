package com.devsusana.hometutorpro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.devsusana.hometutorpro.data.models.AgentBalanceSummary
import com.devsusana.hometutorpro.data.models.AgentScheduleDetail
import com.devsusana.hometutorpro.data.models.AgentScheduleSummary
import com.devsusana.hometutorpro.data.models.AgentStudentDetail
import com.devsusana.hometutorpro.data.models.AgentStudentSummary

/**
 * DAO providing optimized, read-only queries for the Sue AI agent.
 *
 * These queries project only the columns the agent needs, formatted for
 * text-based context injection into the LLM prompt. All queries are scoped
 * to a specific [professorId] and exclude soft-deleted records.
 */
@Dao
interface AgentContextDao {

    /**
     * Returns a summary of all active, non-deleted students for the given professor.
     */
    @Query(
        """
        SELECT s.name, s.subjects, s.course, s.pricePerHour, s.pendingBalance, s.isActive, s.lastPaymentDate
        FROM students s 
        WHERE s.professorId = :professorId AND s.pendingDelete = 0
        ORDER BY s.name ASC
        """
    )
    suspend fun getAllStudentSummariesForAgent(professorId: String): List<AgentStudentSummary>

    /**
     * Returns all schedules joined with student names for the given professor.
     */
    @Query(
        """
        SELECT st.name AS studentName, sch.dayOfWeek, sch.startTime, sch.endTime 
        FROM schedules sch 
        INNER JOIN students st ON sch.studentId = st.id 
        WHERE sch.professorId = :professorId 
        AND sch.pendingDelete = 0 
        AND st.pendingDelete = 0
        ORDER BY sch.dayOfWeek, sch.startTime
        """
    )
    suspend fun getAllSchedulesForAgent(professorId: String): List<AgentScheduleSummary>

    /**
     * Returns students with non-zero pending balance for the given professor.
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
    suspend fun getStudentsWithBalance(professorId: String): List<AgentBalanceSummary>

    /**
     * Searches for students whose name matches the given query (case-insensitive).
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
    suspend fun searchStudentByName(professorId: String, query: String): List<AgentStudentDetail>

    /**
     * Returns the total count of active, non-deleted students for the given professor.
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

    /**
     * Returns all schedule entries with full detail (including IDs) for the given professor.
     * Used by the agent when it needs to cancel or reschedule a specific occurrence.
     */
    @Query(
        """
        SELECT sch.id AS scheduleId, sch.studentId, st.name AS studentName,
               sch.dayOfWeek, sch.startTime, sch.endTime
        FROM schedules sch
        INNER JOIN students st ON sch.studentId = st.id
        WHERE sch.professorId = :professorId
        AND sch.pendingDelete = 0
        AND st.pendingDelete = 0
        ORDER BY sch.dayOfWeek, sch.startTime
        """
    )
    suspend fun getAllScheduleDetailsForAgent(professorId: String): List<AgentScheduleDetail>

    /**
     * Returns schedule entries for students whose name partially matches [studentName].
     * Used to look up the scheduleId and studentId when the user asks to cancel/move a class.
     */
    @Query(
        """
        SELECT sch.id AS scheduleId, sch.studentId, st.name AS studentName,
               sch.dayOfWeek, sch.startTime, sch.endTime
        FROM schedules sch
        INNER JOIN students st ON sch.studentId = st.id
        WHERE sch.professorId = :professorId
        AND LOWER(st.name) LIKE '%' || LOWER(:studentName) || '%'
        AND sch.pendingDelete = 0
        AND st.pendingDelete = 0
        ORDER BY sch.dayOfWeek, sch.startTime
        """
    )
    suspend fun getSchedulesByStudentName(
        professorId: String,
        studentName: String
    ): List<AgentScheduleDetail>
}
