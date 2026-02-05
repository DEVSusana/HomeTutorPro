package com.devsusana.hometutorpro.presentation.weekly_schedule

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import java.time.DayOfWeek

sealed class WeeklyScheduleItem {
    abstract val startTime: String
    abstract val endTime: String
    abstract val date: java.time.LocalDate

    data class Regular(
        val schedule: Schedule,
        val student: Student,
        val exception: ScheduleException? = null,
        override val date: java.time.LocalDate
    ) : WeeklyScheduleItem() {
        override val startTime: String = exception?.newStartTime?.takeIf { it.isNotEmpty() } ?: schedule.startTime
        override val endTime: String = exception?.newEndTime?.takeIf { it.isNotEmpty() } ?: schedule.endTime
    }

    data class FreeSlot(
        override val startTime: String,
        override val endTime: String,
        override val date: java.time.LocalDate
    ) : WeeklyScheduleItem()
}

data class WeeklyScheduleState(
    val schedulesByDay: Map<DayOfWeek, List<WeeklyScheduleItem>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showExceptionDialog: Boolean = false,
    val selectedSchedule: WeeklyScheduleItem.Regular? = null,
    val successMessage: Any? = null,
    val errorMessage: Any? = null,
    val showExtraClassDialog: Boolean = false,
    val selectedStudentIdForExtraClass: String? = null
)
