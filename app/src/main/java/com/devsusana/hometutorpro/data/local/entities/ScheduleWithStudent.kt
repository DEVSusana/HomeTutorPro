package com.devsusana.hometutorpro.data.local.entities

import androidx.room.Embedded

/**
 * POJO for JOIN results between Schedule and Student.
 * Used for O(1) performance in UI by fetching both entities in a single transaction.
 */
data class ScheduleWithStudent(
    @Embedded val schedule: ScheduleEntity,
    
    // Student basic data (minimal projection)
    val studentName: String,
    val studentSubjects: String,
    val studentColor: Int?,
    val studentIsActive: Boolean,
    val studentPendingBalance: Double,
    val studentPricePerHour: Double
)
