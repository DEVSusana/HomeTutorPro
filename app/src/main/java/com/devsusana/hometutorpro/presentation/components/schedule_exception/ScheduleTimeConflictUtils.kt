package com.devsusana.hometutorpro.presentation.components.schedule_exception

/**
 * Utility functions for detecting time overlaps between schedule slots.
 */
object ScheduleTimeConflictUtils {

    /**
     * Checks if two time intervals overlap (e.g. "16:00" - "17:00" and "16:30" - "17:30").
     */
    fun timesOverlap(start1: String, end1: String, start2: String, end2: String): Boolean {
        return try {
            val s1 = timeToMinutes(start1)
            val e1 = timeToMinutes(end1)
            val s2 = timeToMinutes(start2)
            val e2 = timeToMinutes(end2)

            s1 < e2 && s2 < e1
        } catch (_: Exception) {
            false
        }
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}
