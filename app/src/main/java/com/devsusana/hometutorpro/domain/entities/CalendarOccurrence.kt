package com.devsusana.hometutorpro.domain.entities

import java.time.LocalDate

data class CalendarOccurrence(
    val schedule: Schedule,
    val student: StudentSummary,
    val exception: ScheduleException? = null,
    val date: LocalDate
) {
    val startTime: String = exception?.takeIf { it.type != ExceptionType.CANCELLED }?.newStartTime?.takeIf { it.isNotEmpty() } ?: schedule.startTime
    val endTime: String = exception?.takeIf { it.type != ExceptionType.CANCELLED }?.newEndTime?.takeIf { it.isNotEmpty() } ?: schedule.endTime
    
    val isExtra: Boolean = schedule.id.startsWith(ScheduleType.EXTRA_ID) || exception?.originalScheduleId == ScheduleType.EXTRA_ID
}
