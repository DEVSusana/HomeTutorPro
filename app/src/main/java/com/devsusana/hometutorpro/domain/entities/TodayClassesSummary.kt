package com.devsusana.hometutorpro.domain.entities

/**
 * Encapsulates the daily progress of tutoring sessions for the logged-in tutor.
 *
 * @property totalClassesToday Total number of valid scheduled classes for today.
 * @property completedClassesToday Number of classes already taught or completed today.
 */
data class TodayClassesSummary(
    val totalClassesToday: Int = 0,
    val completedClassesToday: Int = 0
) {
    /**
     * True if there were classes scheduled today and all have been completed.
     */
    val isAllCompleted: Boolean
        get() = totalClassesToday > 0 && completedClassesToday >= totalClassesToday

    /**
     * 1-based index of the current or next upcoming class of the day (e.g. 2 for 2nd class of 4).
     */
    val currentClassIndex: Int
        get() = if (totalClassesToday == 0) 0 else (completedClassesToday + 1).coerceAtMost(totalClassesToday)
}
