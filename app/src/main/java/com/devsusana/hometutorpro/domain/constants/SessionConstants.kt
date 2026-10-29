package com.devsusana.hometutorpro.domain.constants

/**
 * Domain constants for active tutoring sessions and billing rules.
 */
object SessionConstants {
    /**
     * Minimum duration in minutes an active tutoring session must run
     * before being billed and marked completed (BUSINESS_RULES.md Rule 6.B).
     */
    const val MIN_BILLABLE_DURATION_MINUTES = 15L

    /**
     * Minimum billable duration expressed in milliseconds.
     */
    const val MIN_BILLABLE_DURATION_MILLIS = MIN_BILLABLE_DURATION_MINUTES * 60 * 1000L
}
