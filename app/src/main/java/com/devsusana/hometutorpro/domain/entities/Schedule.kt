package com.devsusana.hometutorpro.domain.entities

import java.time.DayOfWeek

/**
 * Represents a single scheduled class for a student.
 */
data class Schedule(
    val id: String = "",
    val studentId: String = "",
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val startTime: String = "", // Format: HH:mm
    val endTime: String = "", // Format: HH:mm
    val isCompleted: Boolean = false,
    val completedDate: Long? = null // Timestamp when class was marked as completed
)
