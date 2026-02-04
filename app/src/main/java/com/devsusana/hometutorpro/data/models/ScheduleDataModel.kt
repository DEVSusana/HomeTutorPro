package com.devsusana.hometutorpro.data.models

/**
 * Data model for Firestore Schedule document.
 * Used in the data layer for Firestore operations.
 * DayOfWeek is stored as String for Firestore compatibility.
 */
data class ScheduleDataModel(
    val id: String = "",
    val dayOfWeek: String = "MONDAY",
    val startTime: String = "",
    val endTime: String = ""
)
