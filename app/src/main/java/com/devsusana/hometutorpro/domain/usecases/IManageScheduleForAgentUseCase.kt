package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import java.time.DayOfWeek

/**
 * Use case contract for schedule management actions triggered by the Sue AI agent.
 *
 * Both operations delegate to [ISaveScheduleExceptionUseCase] which enforces
 * all business rules (conflict detection, duplicate prevention, etc.).
 */
interface IManageScheduleForAgentUseCase {

    /**
     * Cancels a specific occurrence of a regular class.
     *
     * Creates a [ScheduleException] of type CANCELLED for the given [date].
     * The regular schedule is NOT deleted — only this occurrence is skipped.
     *
     * @param professorId The current professor's ID.
     * @param studentId   The student's ID.
     * @param scheduleId  The regular schedule entry ID ([ScheduleException.originalScheduleId]).
     * @param date        Epoch-millis timestamp of the class occurrence to cancel.
     */
    suspend fun cancelClass(
        professorId: String,
        studentId: String,
        scheduleId: String,
        date: Long
    ): Result<Unit, DomainError>

    /**
     * Reschedules a specific occurrence of a regular class to a new day/time.
     *
     * Creates a [ScheduleException] of type RESCHEDULED. Conflict checking is
     * delegated to [ISaveScheduleExceptionUseCase].
     *
     * @param professorId    The current professor's ID.
     * @param studentId      The student's ID.
     * @param scheduleId     The regular schedule entry ID.
     * @param originalDate   Epoch-millis of the original occurrence being moved.
     * @param newDayOfWeek   Target day of week, or null if same day.
     * @param newStartTime   New start time in "HH:mm" format.
     * @param newEndTime     New end time in "HH:mm" format.
     */
    suspend fun rescheduleClass(
        professorId: String,
        studentId: String,
        scheduleId: String,
        originalDate: Long,
        newDayOfWeek: DayOfWeek?,
        newStartTime: String,
        newEndTime: String
    ): Result<Unit, DomainError>
}
