package com.devsusana.hometutorpro.data.local.entities

/**
 * Room projection for Student list to minimize PII exposure.
 */
data class StudentSummaryEntity(
    val id: Long,
    val name: String,
    val subjects: String,
    val color: Int?,
    val pendingBalance: Double,
    val pricePerHour: Double,
    val isActive: Boolean,
    val lastClassDate: Long?
)
