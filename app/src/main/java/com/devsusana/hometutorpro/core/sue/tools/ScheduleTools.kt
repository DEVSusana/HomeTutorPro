package com.devsusana.hometutorpro.core.sue.tools

import com.devsusana.hometutorpro.domain.usecases.IQuerySchedulesForAgentUseCase
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Koog-compatible tool definitions for schedule-related queries.
 *
 * Provides formatted schedule data to the LLM for answering
 * questions about the tutor's weekly schedule.
 */
@Singleton
class ScheduleTools @Inject constructor(
    private val querySchedulesUseCase: IQuerySchedulesForAgentUseCase
) {

    companion object {
        private val DAY_NAMES = mapOf(
            1 to "Monday",
            2 to "Tuesday",
            3 to "Wednesday",
            4 to "Thursday",
            5 to "Friday",
            6 to "Saturday",
            7 to "Sunday"
        )
    }

    /**
     * Returns the full weekly schedule formatted by day.
     * Intended for queries like "what's my schedule" or "who do I teach on Monday".
     */
    fun getWeeklySchedule(): String = runBlocking {
        val schedules = querySchedulesUseCase.getAllSchedules()
        if (schedules.isEmpty()) {
            return@runBlocking "No schedules found."
        }

        val byDay = schedules.groupBy { it.dayOfWeek }

        buildString {
            appendLine("Weekly Schedule:")
            for (day in 1..7) {
                val daySchedules = byDay[day]
                val dayName = DAY_NAMES[day] ?: "Day $day"
                if (daySchedules != null) {
                    appendLine("\n$dayName:")
                    daySchedules.forEach { schedule ->
                        appendLine("  - ${schedule.startTime}-${schedule.endTime}: ${schedule.studentName}")
                    }
                }
            }
        }
    }

    /**
     * Returns schedules for a specific day of the week.
     *
     * @param dayOfWeek 1=Monday, 7=Sunday
     */
    fun getScheduleForDay(dayOfWeek: Int): String = runBlocking {
        val schedules = querySchedulesUseCase.getAllSchedules()
            .filter { it.dayOfWeek == dayOfWeek }

        val dayName = DAY_NAMES[dayOfWeek] ?: "Day $dayOfWeek"

        if (schedules.isEmpty()) {
            return@runBlocking "No classes scheduled for $dayName."
        }

        buildString {
            appendLine("$dayName schedule (${schedules.size} classes):")
            schedules.forEach { schedule ->
                appendLine("- ${schedule.startTime}-${schedule.endTime}: ${schedule.studentName}")
            }
        }
    }
}
