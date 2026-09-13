package com.devsusana.hometutorpro.domain.entities

/**
 * Represents an active, ongoing tutoring session.
 */
data class ActiveSession(
    val studentId: String,
    val studentName: String,
    val startTimeMillis: Long,
    val durationMinutes: Long,
    val isOngoing: Boolean
)
