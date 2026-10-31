package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.TodayClassesSummary
import com.devsusana.hometutorpro.domain.usecases.IGetActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetAllScheduleExceptionsUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetAllSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGenerateCalendarOccurrencesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetTodayClassesSummaryUseCase
import com.devsusana.hometutorpro.domain.utils.ScheduleTimeParser
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Implementation of [IGetTodayClassesSummaryUseCase] that computes the total
 * and completed classes scheduled for the current day.
 */
class GetTodayClassesSummaryUseCase @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val getStudentsUseCase: IGetStudentsUseCase,
    private val getAllSchedulesUseCase: IGetAllSchedulesUseCase,
    private val getAllScheduleExceptionsUseCase: IGetAllScheduleExceptionsUseCase,
    private val generateCalendarOccurrencesUseCase: IGenerateCalendarOccurrencesUseCase,
    private val getActiveSessionUseCase: IGetActiveSessionUseCase
) : IGetTodayClassesSummaryUseCase {

    override suspend fun invoke(): TodayClassesSummary {
        val tutorId = getCurrentUserUseCase().value?.uid
            ?: getActiveSessionUseCase()?.tutorId?.takeIf { it.isNotBlank() }
            ?: return TodayClassesSummary()
        val students = getStudentsUseCase(tutorId).firstOrNull() ?: return TodayClassesSummary()
        val schedules = getAllSchedulesUseCase(tutorId).firstOrNull() ?: return TodayClassesSummary()
        val exceptions = getAllScheduleExceptionsUseCase(tutorId).firstOrNull() ?: emptyList()

        val today = LocalDate.now()
        val timeNow = LocalTime.now()
        val occurrences = generateCalendarOccurrencesUseCase(
            students = students,
            schedules = schedules,
            exceptions = exceptions,
            startDate = today,
            endDate = today
        )

        val validOccurrences = occurrences.filter { occurrence ->
            val exc = occurrence.exception ?: return@filter true
            if (exc.type == ExceptionType.CANCELLED) return@filter false
            if (exc.type == ExceptionType.RESCHEDULED) {
                val isSourceSlotRescheduledToOtherDay = exc.newDayOfWeek != null && exc.newDayOfWeek != occurrence.date.dayOfWeek
                val hasNoStartTime = exc.newStartTime.isBlank()
                if (isSourceSlotRescheduledToOtherDay || hasNoStartTime) {
                    return@filter false
                }
            }
            true
        }

        val total = validOccurrences.size
        if (total == 0) return TodayClassesSummary(0, 0)

        val activeSession = getActiveSessionUseCase()?.takeIf { it.isOngoing }

        val completedCount = validOccurrences.count { occurrence ->
            // If there's an ongoing active session for this student right now, it is currently in progress
            if (activeSession != null && activeSession.studentId == occurrence.student.id) {
                return@count false
            }

            // If the student has already had a class started/completed today
            val lastClassDate = occurrence.student.lastClassDate
            if (lastClassDate != null) {
                val lastClassLocalDate = Instant.ofEpochMilli(lastClassDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                if (lastClassLocalDate == today) {
                    return@count true
                }
            }

            // Or if the scheduled time slot has already passed
            val classEnd = ScheduleTimeParser.parseTimeSafely(occurrence.endTime)
            classEnd != null && timeNow.isAfter(classEnd)
        }

        return TodayClassesSummary(
            totalClassesToday = total,
            completedClassesToday = completedCount.coerceAtMost(total)
        )
    }
}
