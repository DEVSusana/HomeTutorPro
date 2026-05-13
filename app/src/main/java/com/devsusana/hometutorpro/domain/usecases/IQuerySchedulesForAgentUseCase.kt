package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.data.models.AgentScheduleDetail
import com.devsusana.hometutorpro.data.models.AgentScheduleSummary

/**
 * Use case contract for querying schedule data on behalf of the Sue AI agent.
 */
interface IQuerySchedulesForAgentUseCase {

    /** Returns all schedules for the current professor, joined with student names. */
    suspend fun getAllSchedules(): List<AgentScheduleSummary>

    /**
     * Returns all schedule entries with full IDs needed for cancelling/rescheduling.
     */
    suspend fun getScheduleDetails(): List<AgentScheduleDetail>

    /**
     * Returns schedules for students whose name partially matches [studentName].
     * Used to resolve the scheduleId / studentId when the user asks to
     * cancel or move a class by the student's name.
     */
    suspend fun getSchedulesByStudentName(studentName: String): List<AgentScheduleDetail>
}
