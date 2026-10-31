package com.devsusana.hometutorpro.domain.utils

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility for parsing schedule time strings robustly without throwing exceptions.
 */
object ScheduleTimeParser {

    private val FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("H:m"),
        DateTimeFormatter.ofPattern("H:m:s"),
        DateTimeFormatter.ofPattern("h:m a", Locale.US),
        DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
    )

    /**
     * Parses a time string (e.g., "16:00", "04:00 PM", "16:00:00") into a [LocalTime].
     * Strips any milliseconds or unexpected characters safely.
     *
     * @param timeStr The time string to parse, potentially containing various time formats.
     * @return The parsed [LocalTime] if successful, or `null` if the input is blank or cannot be parsed.
     */
    fun parseTimeSafely(timeStr: String?): LocalTime? {
        if (timeStr.isNullOrBlank()) return null
        val sanitized = timeStr.trim().substringBefore('.')

        for (formatter in FORMATTERS) {
            try {
                return LocalTime.parse(sanitized, formatter)
            } catch (_: Exception) {
                // Try next formatter
            }
        }
        return null
    }
}
