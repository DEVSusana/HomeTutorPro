package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.usecases.IManageScheduleForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleExceptionUseCase
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Default implementation of [IManageScheduleForAgentUseCase].
 *
 * Delegates all business-rule enforcement (conflict detection, duplicate
 * prevention) to [ISaveScheduleExceptionUseCase].
 */
class ManageScheduleForAgentUseCase @Inject constructor(
    private val saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase
) : IManageScheduleForAgentUseCase {

    override suspend fun cancelClass(
        professorId: String,
        studentId: String,
        scheduleId: String,
        date: Long
    ): Result<Unit, DomainError> {
        val exception = ScheduleException(
            studentId = studentId,
            professorId = professorId,
            date = date,
            type = ExceptionType.CANCELLED,
            originalScheduleId = scheduleId
        )
        return saveScheduleExceptionUseCase(professorId, studentId, exception)
    }

    override suspend fun rescheduleClass(
        professorId: String,
        studentId: String,
        scheduleId: String,
        originalDate: Long,
        newDayOfWeek: DayOfWeek?,
        newStartTime: String,
        newEndTime: String
    ): Result<Unit, DomainError> {
        val exception = ScheduleException(
            studentId = studentId,
            professorId = professorId,
            date = originalDate,
            type = ExceptionType.RESCHEDULED,
            originalScheduleId = scheduleId,
            newStartTime = newStartTime,
            newEndTime = newEndTime,
            newDayOfWeek = newDayOfWeek
        )
        return saveScheduleExceptionUseCase(professorId, studentId, exception)
    }
}
