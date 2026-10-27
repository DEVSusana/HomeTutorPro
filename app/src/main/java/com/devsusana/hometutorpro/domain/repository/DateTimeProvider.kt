package com.devsusana.hometutorpro.domain.repository

import java.time.LocalDateTime
import java.util.Locale

/**
 * Provider interface to decouple date/time and locale dependencies from system-wide statics.
 * Allows clean mocking and deterministic date/time assertions in tests.
 */
interface DateTimeProvider {
    /**
     * Provides the current timestamp as a [LocalDateTime] instance.
     * Useful for ensuring test reproducibility by injecting a fixed time provider.
     */
    fun getNow(): LocalDateTime

    /**
     * Retrieves the current system [Locale] configuration.
     * Used to ensure consistent date formatting and localized data presentation.
     */
    fun getLocale(): Locale

    /**
     * Returns the current Unix epoch time in milliseconds.
     * Implementations rely on this abstraction instead of [System.currentTimeMillis]
     * to facilitate unit testing with mocked time.
     */
    fun currentTimeMillis(): Long

    /**
     * Returns the epoch millisecond timestamp representing the start of the current day in system timezone.
     */
    fun getStartOfDayMillis(): Long
}
