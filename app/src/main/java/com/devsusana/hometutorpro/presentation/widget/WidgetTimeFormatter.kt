package com.devsusana.hometutorpro.presentation.widget

import android.content.Context
import android.text.format.DateFormat
import com.devsusana.hometutorpro.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utility functions for parsing and formatting dates and times in widgets.
 */
object WidgetTimeFormatter {

    /**
     * Parses a time string (e.g. "16:00", "04:00 PM", "16:00:00") into a [LocalTime],
     * strictly stripping any accidental milliseconds or extra characters.
     */
    fun parseTimeSafely(timeStr: String): LocalTime? {
        val sanitized = timeStr.trim().substringBefore('.')
        return try {
            LocalTime.parse(sanitized, DateTimeFormatter.ofPattern("H:m"))
        } catch (_: Exception) {
            try {
                LocalTime.parse(sanitized, DateTimeFormatter.ofPattern("H:m:s"))
            } catch (_: Exception) {
                try {
                    LocalTime.parse(sanitized, DateTimeFormatter.ofPattern("h:m a", Locale.US))
                } catch (_: Exception) {
                    try {
                        LocalTime.parse(sanitized, DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }
    }

    /**
     * Formats a [LocalTime] honoring the user's 12-hour (AM/PM) vs 24-hour preference,
     * ensuring strictly clean format without seconds or milliseconds.
     */
    fun formatLocalTime(context: Context, time: LocalTime): String {
        val is24Hour = DateFormat.is24HourFormat(context)
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        return time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
    }

    /**
     * Formats a raw time string cleanly into either 12-hour AM/PM or 24-hour format.
     */
    fun formatTimeDisplay(context: Context, rawTime: String): String {
        val parsed = parseTimeSafely(rawTime)
        return if (parsed != null) {
            formatLocalTime(context, parsed)
        } else {
            rawTime.substringBefore('.').trim()
        }
    }

    /**
     * Formats the schedule date cleanly as "Today", "Tomorrow", or "E, d MMM".
     */
    fun formatScheduleDate(context: Context, date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> context.getString(R.string.widget_today)
            today.plusDays(1) -> context.getString(R.string.widget_tomorrow)
            else -> {
                val pattern = "E, d MMM"
                date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
            }
        }
    }
}
