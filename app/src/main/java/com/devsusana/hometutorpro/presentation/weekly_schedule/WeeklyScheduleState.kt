package com.devsusana.hometutorpro.presentation.weekly_schedule

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.StudentSummary
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import java.time.DayOfWeek
import androidx.compose.runtime.Immutable

@Immutable
sealed class WeeklyScheduleItem {
    abstract val startTime: String
    abstract val endTime: String
    abstract val date: java.time.LocalDate

    data class Regular(
        val occurrence: CalendarOccurrence
    ) : WeeklyScheduleItem() {
        val schedule: Schedule get() = occurrence.schedule
        val student: StudentSummary get() = occurrence.student
        val exception: ScheduleException? get() = occurrence.exception
        override val date: java.time.LocalDate get() = occurrence.date
        override val startTime: String get() = occurrence.startTime
        override val endTime: String get() = occurrence.endTime
        
        // Secondary constructor for backward compatibility during refactor
        constructor(schedule: Schedule, student: StudentSummary, exception: ScheduleException? = null, date: java.time.LocalDate) 
            : this(CalendarOccurrence(schedule, student, exception, date))
    }

    data class FreeSlot(
        override val startTime: String,
        override val endTime: String,
        override val date: java.time.LocalDate
    ) : WeeklyScheduleItem()
}

@Immutable
data class WeeklyScheduleState(
    val schedulesByDay: Map<DayOfWeek, List<WeeklyScheduleItem>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showExceptionDialog: Boolean = false,
    val selectedSchedule: WeeklyScheduleItem.Regular? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val showExtraClassDialog: Boolean = false,
    val selectedStudentIdForExtraClass: String? = null
)
