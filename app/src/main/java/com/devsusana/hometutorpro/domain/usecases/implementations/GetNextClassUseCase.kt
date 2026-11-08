package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.CalendarOccurrence
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.usecases.IGetActiveSessionUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetAllSchedulesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetCurrentUserUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetScheduleExceptionsUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetStudentsUseCase
import com.devsusana.hometutorpro.domain.usecases.IGenerateCalendarOccurrencesUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetNextClassUseCase
import com.devsusana.hometutorpro.domain.utils.ScheduleTimeParser
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class GetNextClassUseCase @Inject constructor(
    private val getCurrentUserUseCase: IGetCurrentUserUseCase,
    private val getStudentsUseCase: IGetStudentsUseCase,
    private val getAllSchedulesUseCase: IGetAllSchedulesUseCase,
    private val getScheduleExceptionsUseCase: IGetScheduleExceptionsUseCase,
    private val generateCalendarOccurrencesUseCase: IGenerateCalendarOccurrencesUseCase,
    private val getActiveSessionUseCase: IGetActiveSessionUseCase
) : IGetNextClassUseCase {

    override suspend fun invoke(): CalendarOccurrence? {
        val tutorId = getCurrentUserUseCase().value?.uid
            ?: getActiveSessionUseCase()?.tutorId?.takeIf { it.isNotBlank() }
            ?: return null
        val students = getStudentsUseCase(tutorId).firstOrNull() ?: return null
        val schedules = getAllSchedulesUseCase(tutorId).firstOrNull() ?: return null

        val allExceptions = mutableListOf<ScheduleException>()
        students.filter { it.id.isNotEmpty() }.forEach { student ->
            getScheduleExceptionsUseCase(tutorId, student.id).firstOrNull()?.let {
                allExceptions.addAll(it)
            }
        }

        val today = LocalDate.now()
        val occurrences = generateCalendarOccurrencesUseCase(
            students = students,
            schedules = schedules,
            exceptions = allExceptions,
            startDate = today,
            endDate = today.plusDays(7)
        )

        return findNextClass(occurrences)
    }

    private suspend fun findNextClass(occurrences: List<CalendarOccurrence>): CalendarOccurrence? {
        val now = java.time.LocalDateTime.now()
        val today = now.toLocalDate()
        val timeNow = now.toLocalTime()
        val activeSession = getActiveSessionUseCase()?.takeIf { it.isOngoing }

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

        val sorted = validOccurrences.sortedWith(
            compareBy<CalendarOccurrence> { it.date }.thenBy { it.startTime }
        )

        val nextToday = sorted.filter { it.date == today }.find { occurrence ->
            // 1. If this occurrence corresponds to an active ongoing session, skip it (it is already active)
            if (activeSession != null && activeSession.studentId == occurrence.student.id) {
                return@find false
            }

            // 2. Parse times safely
            val classStart = ScheduleTimeParser.parseTimeSafely(occurrence.startTime)
            val classEnd = ScheduleTimeParser.parseTimeSafely(occurrence.endTime) ?: classStart?.plusHours(1)

            // 3. If slot has already ended, skip it
            if (classEnd != null && timeNow.isAfter(classEnd)) {
                return@find false
            }

            // 4. If this student already had a class started/completed today, skip it
            val lastClassDate = occurrence.student.lastClassDate
            if (lastClassDate != null) {
                val lastClassLocalDate = Instant.ofEpochMilli(lastClassDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                if (lastClassLocalDate == today) {
                    return@find false
                }
            }

            // Otherwise, class is upcoming or currently within its slot and ready to be taught
            true
        }

        if (nextToday != null) return nextToday

        val future = sorted.filter { it.date.isAfter(today) }
        if (future.isNotEmpty()) return future.first()

        return null
    }
}
