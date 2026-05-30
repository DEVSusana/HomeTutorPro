package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.IManageScheduleForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IQuerySchedulesForAgentUseCase
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tool definitions for schedule-related queries and management actions.
 *
 * All functions are `suspend` to comply with AGENTS.md Rule 1 (Coroutines),
 * ensuring database I/O does not block the main thread.
 *
 * All write operations go through [IManageScheduleForAgentUseCase] which delegates
 * business-rule validation to [ISaveScheduleExceptionUseCase].
 */
@Singleton
class ScheduleTools @Inject constructor(
    private val querySchedulesUseCase: IQuerySchedulesForAgentUseCase,
    private val manageScheduleUseCase: IManageScheduleForAgentUseCase,
    private val authRepository: AuthRepository,
    private val dateTimeProvider: DateTimeProvider
) {

    companion object {
        private val TIME_FORMATTER = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ operations
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the full weekly schedule.
     */
    suspend fun getWeeklySchedule(): SueOperationResult {
        val schedules = querySchedulesUseCase.getAllSchedules()
        return SueOperationResult.WeeklySchedule(schedules)
    }

    /**
     * Returns the schedule for a specific day of the week.
     *
     * @param dayOfWeek ISO day value (1=Monday … 7=Sunday).
     * @param timeFilter Optional filter: "morning" (before 14:00), "afternoon" (14:00+), or null (all day).
     */
    suspend fun getScheduleForDay(dayOfWeek: Int, timeFilter: String? = null): SueOperationResult {
        val allSchedules = querySchedulesUseCase.getAllSchedules()
            .filter { it.dayOfWeek == dayOfWeek }

        val schedules = when {
            timeFilter == "morning" -> allSchedules.filter {
                LocalTime.parse(it.startTime, TIME_FORMATTER).isBefore(LocalTime.of(14, 0))
            }
            timeFilter == "afternoon" -> allSchedules.filter {
                LocalTime.parse(it.startTime, TIME_FORMATTER).isAfter(LocalTime.of(13, 59))
            }
            timeFilter != null && timeFilter.contains(":") -> {
                val targetTime = LocalTime.parse(timeFilter, TIME_FORMATTER)
                allSchedules.filter { s ->
                    val start = LocalTime.parse(s.startTime, TIME_FORMATTER)
                    val end = LocalTime.parse(s.endTime, TIME_FORMATTER)
                    !targetTime.isBefore(start) && targetTime.isBefore(end)
                }
            }
            else -> allSchedules
        }

        return SueOperationResult.DaySchedule(dayOfWeek, timeFilter, schedules)
    }

    /**
     * Returns the next upcoming class from the current moment.
     * Searches the current week first, then wraps to the following week.
     */
    suspend fun getNextClass(): SueOperationResult {
        val schedules = querySchedulesUseCase.getAllSchedules()
        if (schedules.isEmpty()) return SueOperationResult.NextClass(null, null)

        val now = dateTimeProvider.getNow().toLocalDate()
        val currentTime = dateTimeProvider.getNow().toLocalTime()
        val todayIso = now.dayOfWeek.value

        val sorted = schedules.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))

        // Find the first schedule later today or on a future day this week
        val candidate = sorted.firstOrNull { s ->
            when {
                s.dayOfWeek > todayIso -> true
                s.dayOfWeek == todayIso -> LocalTime.parse(s.startTime, TIME_FORMATTER).isAfter(currentTime)
                else -> false
            }
        } ?: sorted.firstOrNull() // wrap to start of next week

        if (candidate == null) return SueOperationResult.NextClass(null, null)

        val targetDayOfWeek = DayOfWeek.of(candidate.dayOfWeek)
        val startLocalTime = LocalTime.parse(candidate.startTime, TIME_FORMATTER)
        val isNextWeek = when {
            candidate.dayOfWeek < todayIso -> true
            candidate.dayOfWeek == todayIso -> !startLocalTime.isAfter(currentTime)
            else -> false
        }
        val occurrenceDate = if (isNextWeek) {
            now.with(TemporalAdjusters.next(targetDayOfWeek))
        } else {
            now.with(TemporalAdjusters.nextOrSame(targetDayOfWeek))
        }

        return SueOperationResult.NextClass(candidate, occurrenceDate)
    }

    /**
     * Returns the weekdays (Monday–Friday) that have NO classes scheduled.
     */
    suspend fun getFreeSlots(): SueOperationResult {
        val schedules = querySchedulesUseCase.getAllSchedules()
        val scheduledDays = schedules.map { it.dayOfWeek }.toSet()
        val freeDays = (1..5).filter { it !in scheduledDays }

        return SueOperationResult.FreeSlots(freeDays)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // WRITE operations — two-step: prepare (ask confirmation) → execute
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Looks up [studentName]'s schedule for [dayOfWeek] and, if found, builds a
     * [SuePendingAction.CancelClass] with all data pre-resolved.
     */
    suspend fun prepareCancelAction(
        studentName: String,
        dayOfWeek: Int
    ): SueOperationResult.Prepare {
        val schedules = querySchedulesUseCase.getSchedulesByStudentName(studentName)
        val match = schedules.firstOrNull { it.dayOfWeek == dayOfWeek }

        if (match == null) {
            return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.CLASS_NOT_FOUND)
        }

        val targetDate = nextOccurrenceDate(DayOfWeek.of(dayOfWeek))
        val dateMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val action = SuePendingAction.CancelClass(
            studentName = match.studentName,
            studentId = match.studentId,
            scheduleId = match.scheduleId,
            date = dateMillis,
            startTime = match.startTime,
            endTime = match.endTime
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Looks up [studentName]'s schedule for [fromDayOfWeek] and builds a
     * [SuePendingAction.RescheduleClass] targeting [toDayOfWeek] at [newStartTime].
     */
    suspend fun prepareRescheduleAction(
        studentName: String,
        fromDayOfWeek: Int,
        toDayOfWeek: Int,
        newStartTime: String
    ): SueOperationResult.Prepare {
        val schedules = querySchedulesUseCase.getSchedulesByStudentName(studentName)
        val match = schedules.firstOrNull { it.dayOfWeek == fromDayOfWeek }

        if (match == null) {
            return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.CLASS_NOT_FOUND)
        }

        val origStart = LocalTime.parse(match.startTime, TIME_FORMATTER)
        val origEnd = LocalTime.parse(match.endTime, TIME_FORMATTER)
        val durationMinutes = java.time.Duration.between(origStart, origEnd).toMinutes()
        val parsedNewStart = LocalTime.parse(newStartTime, TIME_FORMATTER)
        val newEndTime = parsedNewStart.plusMinutes(durationMinutes).format(TIME_FORMATTER)

        val originalDate = nextOccurrenceDate(DayOfWeek.of(fromDayOfWeek))
        val originalMillis = originalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val targetDayOfWeek = DayOfWeek.of(toDayOfWeek)
        val targetDate = nextOccurrenceDate(targetDayOfWeek)
        val targetMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val newDayOfWeek = if (fromDayOfWeek != toDayOfWeek) targetDayOfWeek else null

        val action = SuePendingAction.RescheduleClass(
            studentName = match.studentName,
            studentId = match.studentId,
            scheduleId = match.scheduleId,
            originalDate = originalMillis,
            originalStartTime = match.startTime,
            newDayOfWeek = newDayOfWeek,
            newDate = targetMillis,
            newStartTime = newStartTime,
            newEndTime = newEndTime
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes a confirmed [SuePendingAction.CancelClass].
     */
    suspend fun executeCancelAction(action: SuePendingAction.CancelClass): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        return when (val result = manageScheduleUseCase.cancelClass(
            professorId = professorId,
            studentId = action.studentId,
            scheduleId = action.scheduleId,
            date = action.date
        )) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Executes a confirmed [SuePendingAction.RescheduleClass].
     */
    suspend fun executeRescheduleAction(action: SuePendingAction.RescheduleClass): SueOperationResult.Execute {
        val professorId = authRepository.currentUser.value?.uid
            ?: return SueOperationResult.Execute.AuthError

        return when (val result = manageScheduleUseCase.rescheduleClass(
            professorId = professorId,
            studentId = action.studentId,
            scheduleId = action.scheduleId,
            originalDate = action.originalDate,
            newDayOfWeek = action.newDayOfWeek,
            newStartTime = action.newStartTime,
            newEndTime = action.newEndTime
        )) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    suspend fun getSchedulesByStudentName(studentName: String): List<com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail> {
        return querySchedulesUseCase.getSchedulesByStudentName(studentName)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun nextOccurrenceDate(dayOfWeek: DayOfWeek): LocalDate =
        dateTimeProvider.getNow().toLocalDate().with(TemporalAdjusters.nextOrSame(dayOfWeek))
}
