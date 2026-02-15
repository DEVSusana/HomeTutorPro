package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.usecases.ICheckScheduleConflictUseCase
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class CheckScheduleConflictUseCase @Inject constructor() : ICheckScheduleConflictUseCase {
    override operator fun invoke(newSchedule: Schedule, existingSchedules: List<Schedule>): Boolean {
        return existingSchedules.any { existing ->
            // Skip self if editing (though usually schedules are immutable or replaced)
            if (existing.id == newSchedule.id && existing.id.isNotEmpty()) return@any false
            
            existing.dayOfWeek == newSchedule.dayOfWeek &&
            areTimesOverlapping(existing.startTime, existing.endTime, newSchedule.startTime, newSchedule.endTime)
        }
    }

    private fun areTimesOverlapping(start1: String, end1: String, start2: String, end2: String): Boolean {
        return try {
            val s1 = parseTime(start1)
            val e1 = parseTime(end1)
            val s2 = parseTime(start2)
            val e2 = parseTime(end2)
            
            max(s1, s2) < min(e1, e2)
        } catch (e: Exception) {
            false
        }
    }

    private fun parseTime(time: String): Int {
        return time.replace(":", "").toInt()
    }
}
