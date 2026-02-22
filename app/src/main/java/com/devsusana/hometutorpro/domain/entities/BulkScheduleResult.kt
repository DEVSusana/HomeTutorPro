package com.devsusana.hometutorpro.domain.entities

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.entities.Schedule

/**
 * Represents the result of a bulk schedule operation.
 * Contains the successfully processed schedules and a map of validation/conflict errors.
 */
data class BulkScheduleResult(
    val processedSchedules: List<Schedule> = emptyList(),
    val errors: Map<Int, DomainError> = emptyMap(), // index to error
    val isSuccessful: Boolean = false
)
