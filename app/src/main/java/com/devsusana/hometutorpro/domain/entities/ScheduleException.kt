package com.devsusana.hometutorpro.domain.entities

import java.time.DayOfWeek

enum class ExceptionType {
    CANCELLED,    // Class cancelled for this date
    RESCHEDULED,  // Class moved to different time on same date
    EXTRA         // Additional class for this specific date
}

data class ScheduleException(
    val id: String = "",
    val studentId: String = "",
    val date: Long = 0L, // Timestamp of the specific occurrence
    val type: ExceptionType = ExceptionType.CANCELLED,
    val originalScheduleId: String = "", // Reference to regular schedule
    val newStartTime: String = "", // For rescheduled classes
    val newEndTime: String = "",   // For rescheduled classes
    val newDayOfWeek: DayOfWeek? = null, // For rescheduling to a different day
    val reason: String = ""
)
