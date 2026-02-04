package com.devsusana.hometutorpro.data.models

/**
 * Data model for Firestore ScheduleException document.
 * Used in the data layer for Firestore operations.
 * ExceptionType and DayOfWeek are stored as Strings for Firestore compatibility.
 */
data class ScheduleExceptionDataModel(
    val id: String = "",
    val studentId: String = "",
    val date: Long = 0L,
    val type: String = "CANCELLED",
    val originalScheduleId: String = "",
    val newStartTime: String = "",
    val newEndTime: String = "",
    val newDayOfWeek: String? = null,
    val reason: String = ""
)
