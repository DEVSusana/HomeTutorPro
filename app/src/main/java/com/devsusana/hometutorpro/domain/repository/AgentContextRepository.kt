package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.entities.AgentBalanceSummary
import com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.AgentStudentDetail
import com.devsusana.hometutorpro.domain.entities.AgentStudentSummary

/**
 * Repository contract for providing contextual data to the Sue AI agent.
 *
 * Implementations must scope all queries to the current professor's data
 * to maintain multi-user isolation.
 */
interface AgentContextRepository {

    /** Returns summaries of all non-deleted students for the current professor. */
    suspend fun getAllStudentSummaries(): List<AgentStudentSummary>

    /** Returns all schedule entries joined with student names. */
    suspend fun getAllSchedules(): List<AgentScheduleSummary>

    /** Returns students with non-zero pending balance. */
    suspend fun getStudentsWithBalance(): List<AgentBalanceSummary>

    /** Searches for students whose name matches the given [query]. */
    suspend fun searchStudentByName(query: String): List<AgentStudentDetail>

    /** Returns the total count of active students. */
    suspend fun getActiveStudentCount(): Int

    /**
     * Returns all schedule entries with full detail (scheduleId, studentId, etc.)
     * needed to create [ScheduleException] entries for cancellations/reschedules.
     */
    suspend fun getScheduleDetails(): List<AgentScheduleDetail>

    /**
     * Returns all schedules for students whose name partially matches [studentName].
     * Used by the agent to locate the correct scheduleId/studentId when the user
     * asks to cancel or reschedule a class by student name.
     */
    suspend fun getSchedulesByStudentName(studentName: String): List<AgentScheduleDetail>
}
