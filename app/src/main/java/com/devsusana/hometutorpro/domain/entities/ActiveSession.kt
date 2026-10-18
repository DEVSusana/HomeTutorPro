package com.devsusana.hometutorpro.domain.entities

/**
 * Represents an active, ongoing tutoring session.
 *
 * @property studentId Unique identifier of the student being tutored.
 * @property studentName Display name of the student.
 * @property startTimeMillis Timestamp (in milliseconds) when the session started.
 * @property durationMinutes Scheduled duration of the class in minutes.
 * @property isOngoing True if the session is currently active.
 * @property tutorId Unique identifier of the authenticated tutor who started the session.
 */
data class ActiveSession(
    val studentId: String,
    val studentName: String,
    val startTimeMillis: Long,
    val durationMinutes: Long,
    val isOngoing: Boolean,
    val tutorId: String = ""
)
