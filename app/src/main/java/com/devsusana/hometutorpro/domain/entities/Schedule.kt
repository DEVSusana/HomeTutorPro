package com.devsusana.hometutorpro.domain.entities

import java.time.DayOfWeek

/**
 * Represents a single scheduled class for a student.
 */
data class Schedule(
    val id: String = "",
    val studentId: String = "",
    val professorId: String = "",
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val startTime: String = "09:00", // Format: HH:mm
    val endTime: String = "10:00", // Format: HH:mm
    val isCompleted: Boolean = false,
    val completedDate: Long? = null,
    val studentName: String? = null,
    val studentSubjects: String? = null,
    val studentColor: Int? = null,
    val studentIsActive: Boolean? = null,
    val studentPendingBalance: Double? = null,
    val studentPricePerHour: Double? = null
)

sealed class ScheduleType {
    object Regular : ScheduleType()
    object Extra : ScheduleType()
    
    companion object {
        const val EXTRA_ID = "EXTRA"
    }
}
