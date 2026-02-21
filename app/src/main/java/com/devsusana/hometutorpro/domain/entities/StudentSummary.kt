package com.devsusana.hometutorpro.domain.entities

/**
 * Lightweight student projection for list/dashboard views.
 * Contains only the fields needed for display, avoiding PII over-fetching
 * (GDPR Art. 5.1c — data minimization).
 *
 * Use [Student] for detail views where all fields are required.
 */
data class StudentSummary(
    val id: String,
    val name: String,
    val subjects: String,
    val color: Int?,
    val pendingBalance: Double,
    val pricePerHour: Double,
    val isActive: Boolean,
    val lastClassDate: Long?
)
