package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import java.time.LocalDate

/**
 * Use case contract for GenerateCalendarOccurrences operations.
 */
interface IGenerateCalendarOccurrencesUseCase {
    /**
     * Executes the use case.
     */
    /**
     * Executes the use case.
     */
    suspend operator fun invoke(
        students: List<StudentSummary>,
        schedules: List<Schedule>,
        exceptions: List<ScheduleException>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CalendarOccurrence>
}
