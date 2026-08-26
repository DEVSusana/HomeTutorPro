package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.entities.Schedule
import com.devsusana.hometutorpro.domain.entities.ScheduleException
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.IManageScheduleForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IQuerySchedulesForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IQueryStudentsForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleUseCase
import com.devsusana.hometutorpro.domain.usecases.IDeleteScheduleUseCase
import com.devsusana.hometutorpro.domain.usecases.ISaveScheduleExceptionUseCase
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
 */
@Singleton
class ScheduleTools @Inject constructor(
    private val querySchedulesUseCase: IQuerySchedulesForAgentUseCase,
    private val manageScheduleUseCase: IManageScheduleForAgentUseCase,
    private val queryStudentsUseCase: IQueryStudentsForAgentUseCase,
    private val saveScheduleUseCase: ISaveScheduleUseCase,
    private val deleteScheduleUseCase: IDeleteScheduleUseCase,
    private val saveScheduleExceptionUseCase: ISaveScheduleExceptionUseCase,
    private val exceptionRepository: com.devsusana.hometutorpro.domain.repository.ScheduleExceptionRepository,
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
     * Returns all recurring weekly sessions for a specific student, grouped by day.
     * Used to answer queries like "¿cuántas clases tengo de [alumno] a la semana?".
     *
     * @param studentName The student's name (partial match, accent-insensitive).
     */
    suspend fun getWeeklyClassesForStudent(studentName: String): SueOperationResult {
        val normalizedFilter = java.text.Normalizer.normalize(
            studentName.lowercase(), java.text.Normalizer.Form.NFD
        ).replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        val allSchedules = querySchedulesUseCase.getAllSchedules()
        val studentSchedules = allSchedules.filter { schedule ->
            val normalizedName = java.text.Normalizer.normalize(
                schedule.studentName.lowercase(), java.text.Normalizer.Form.NFD
            ).replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            normalizedName.contains(normalizedFilter)
        }

        return if (studentSchedules.isEmpty()) {
            SueOperationResult.ReadSuccess("No he encontrado clases semanales registradas para un alumno llamado \"$studentName\".")
        } else {
            SueOperationResult.WeeklyClassesForStudent(studentName, studentSchedules)
        }
    }

    /**
     * Returns the next upcoming class from the current moment.
     * Searches the current week first, then wraps to the following week.
     */
    suspend fun getNextClass(studentNameFilter: String? = null): SueOperationResult {
        var schedules = querySchedulesUseCase.getAllSchedules()
        if (studentNameFilter != null) {
            val normalizedFilter = java.text.Normalizer.normalize(studentNameFilter.lowercase(), java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            schedules = schedules.filter { s ->
                val sName = s.studentName
                val normalizedName = java.text.Normalizer.normalize(sName.lowercase(), java.text.Normalizer.Form.NFD)
                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                normalizedName.contains(normalizedFilter) || normalizedFilter.contains(normalizedName)
            }
        }
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
     * Returns free time gaps within the professor's working hours.
     *
     * If [dayOfWeek] is provided, only that day is analysed. Otherwise all
     * weekdays (Monday–Friday) are checked. The function computes inter-class
     * gaps for each day and also lists days with zero classes (fully free days).
     *
     * @param workingStart Start of the working day in "HH:mm" format (e.g. "08:00").
     * @param workingEnd   End of the working day in "HH:mm" format (e.g. "23:00").
     * @param dayOfWeek    Optional ISO day value (1=Monday … 7=Sunday) to restrict the search.
     */
    suspend fun getFreeSlots(
        workingStart: String = "08:00",
        workingEnd: String = "23:00",
        dayOfWeek: Int? = null
    ): SueOperationResult {
        val allScheduleDetails = querySchedulesUseCase.getScheduleDetails()
        val professorId = getProfessorId().takeIf { it.isNotEmpty() }
        val allExceptions = if (professorId != null) exceptionRepository.getAllExceptions(professorId) else emptyList()

        val daysToCheck = if (dayOfWeek != null) listOf(dayOfWeek) else (1..5).toList()

        val workStart = runCatching { LocalTime.parse(workingStart, TIME_FORMATTER) }.getOrElse { LocalTime.of(8, 0) }
        val workEnd   = runCatching { LocalTime.parse(workingEnd,   TIME_FORMATTER) }.getOrElse { LocalTime.of(23, 0) }

        val freeSlotLines = mutableListOf<String>()
        val freeDays = mutableListOf<Int>()

        for (day in daysToCheck) {
            val targetDate = nextOccurrenceDate(java.time.DayOfWeek.of(day))
            val dayExceptions = allExceptions.filter { exc ->
                val localDate = java.time.Instant.ofEpochMilli(exc.date)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                localDate == targetDate
            }

            val activeIntervals = mutableListOf<Pair<LocalTime, LocalTime>>()

            // 1. Regular schedules for this day (excluding cancelled ones)
            for (sched in allScheduleDetails.filter { it.dayOfWeek == day }) {
                val matchingExc = dayExceptions.find {
                    (it.originalScheduleId == sched.scheduleId || it.studentId == sched.studentId)
                }
                if (matchingExc != null && matchingExc.type == ExceptionType.CANCELLED) {
                    // Cancelled class does NOT occupy time
                    continue
                }
                if (matchingExc != null && matchingExc.type == ExceptionType.RESCHEDULED) {
                    val newStart = runCatching { LocalTime.parse(matchingExc.newStartTime, TIME_FORMATTER) }.getOrNull()
                    val newEnd = runCatching { LocalTime.parse(matchingExc.newEndTime, TIME_FORMATTER) }.getOrNull()
                    if (newStart != null && newEnd != null) {
                        activeIntervals.add(newStart to newEnd)
                    }
                    continue
                }

                val clsStart = runCatching { LocalTime.parse(sched.startTime, TIME_FORMATTER) }.getOrNull() ?: continue
                val clsEnd   = runCatching { LocalTime.parse(sched.endTime,   TIME_FORMATTER) }.getOrNull() ?: continue
                activeIntervals.add(clsStart to clsEnd)
            }

            // 2. Extra classes for this date
            for (exc in dayExceptions.filter { it.type == ExceptionType.EXTRA }) {
                val clsStart = runCatching { LocalTime.parse(exc.newStartTime, TIME_FORMATTER) }.getOrNull() ?: continue
                val clsEnd   = runCatching { LocalTime.parse(exc.newEndTime,   TIME_FORMATTER) }.getOrNull() ?: continue
                activeIntervals.add(clsStart to clsEnd)
            }

            if (activeIntervals.isEmpty()) {
                // Whole day is free within working hours
                freeDays.add(day)
                continue
            }

            activeIntervals.sortBy { it.first }

            // Compute gaps: [workStart .. first class start] and [class end .. next class start] and [last class end .. workEnd]
            val gaps = mutableListOf<Pair<LocalTime, LocalTime>>()
            var cursor = workStart

            for ((clsStart, clsEnd) in activeIntervals) {
                if (clsStart.isAfter(cursor)) {
                    gaps.add(cursor to clsStart)
                }
                if (clsEnd.isAfter(cursor)) cursor = clsEnd
            }
            if (cursor.isBefore(workEnd)) {
                gaps.add(cursor to workEnd)
            }

            // Only surface gaps of at least 60 minutes (1 hour)
            val meaningfulGaps = gaps.filter { (s, e) -> java.time.Duration.between(s, e).toMinutes() >= 60 }
            if (meaningfulGaps.isNotEmpty()) {
                val dayName = when (day) {
                    1 -> "Lunes"; 2 -> "Martes"; 3 -> "Miércoles"
                    4 -> "Jueves"; 5 -> "Viernes"; 6 -> "Sábado"; else -> "Domingo"
                }
                meaningfulGaps.forEach { (s, e) ->
                    freeSlotLines.add("$dayName: hueco libre de ${s.format(TIME_FORMATTER)} a ${e.format(TIME_FORMATTER)}")
                }
            }
        }

        return SueOperationResult.FreeSlotsDetailed(freeDays, freeSlotLines)
    }

    /**
     * Returns all schedules with detailed entity mappings (IDs and names).
     */
    suspend fun getScheduleDetails(): List<com.devsusana.hometutorpro.domain.entities.AgentScheduleDetail> {
        return querySchedulesUseCase.getScheduleDetails()
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
        dayOfWeek: Int,
        time: String? = null
    ): SueOperationResult.Prepare {
        val schedules = querySchedulesUseCase.getSchedulesByStudentName(studentName)
        val daySchedules = schedules.filter { it.dayOfWeek == dayOfWeek }

        val match = if (time != null && daySchedules.size > 1) {
            daySchedules.firstOrNull { it.startTime == time }
                ?: daySchedules.firstOrNull { it.startTime.substringBefore(":") == time.substringBefore(":") }
                ?: daySchedules.firstOrNull()
        } else {
            daySchedules.firstOrNull()
        }

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

    suspend fun prepareRescheduleAction(
        studentName: String,
        fromDayOfWeek: Int,
        toDayOfWeek: Int,
        newStartTime: String,
        fromTime: String? = null
    ): SueOperationResult.Prepare {
        val schedules = querySchedulesUseCase.getSchedulesByStudentName(studentName)
        val daySchedules = schedules.filter { it.dayOfWeek == fromDayOfWeek }
        val match = if (fromTime != null) {
            daySchedules.firstOrNull { it.startTime == fromTime }
                ?: daySchedules.firstOrNull { it.startTime.substringBefore(":") == fromTime.substringBefore(":") }
                ?: daySchedules.firstOrNull()
        } else {
            daySchedules.firstOrNull()
        }

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

    private fun getProfessorId(): String {
        val firebaseUid = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }
        return firebaseUid ?: authRepository.currentUser.value?.uid ?: ""
    }

    /**
     * Executes a confirmed [SuePendingAction.CancelClass].
     */
    suspend fun executeCancelAction(action: SuePendingAction.CancelClass): SueOperationResult.Execute {
        val professorId = getProfessorId().takeIf { it.isNotEmpty() }
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
        val professorId = getProfessorId().takeIf { it.isNotEmpty() }
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

    /**
     * Prepares a CreateSchedule action for user confirmation.
     */
    suspend fun prepareCreateSchedule(
        studentName: String,
        dayOfWeek: Int,
        startTime: String,
        endTime: String
    ): SueOperationResult.Prepare {
        val students = queryStudentsUseCase.searchByName(studentName)
        val match = students.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) }

        if (match == null) {
            return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        }

        val action = SuePendingAction.CreateSchedule(
            studentName = match.name,
            studentId = match.studentId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes a confirmed CreateSchedule action.
     */
    suspend fun executeCreateSchedule(action: SuePendingAction.CreateSchedule): SueOperationResult.Execute {
        val professorId = getProfessorId().takeIf { it.isNotEmpty() }
            ?: return SueOperationResult.Execute.AuthError

        val schedule = Schedule(
            studentId = action.studentId,
            professorId = professorId,
            dayOfWeek = DayOfWeek.of(action.dayOfWeek),
            startTime = action.startTime,
            endTime = action.endTime
        )

        return when (val result = saveScheduleUseCase(professorId, action.studentId, schedule)) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Prepares a DeleteSchedule action for user confirmation.
     */
    suspend fun prepareDeleteSchedule(
        studentName: String,
        dayOfWeek: Int,
        startTime: String? = null
    ): SueOperationResult.Prepare {
        val schedules = querySchedulesUseCase.getSchedulesByStudentName(studentName)
        val daySchedules = schedules.filter { it.dayOfWeek == dayOfWeek }

        val match = if (startTime != null && daySchedules.size > 1) {
            daySchedules.firstOrNull { it.startTime == startTime }
                ?: daySchedules.firstOrNull { it.startTime.substringBefore(":") == startTime.substringBefore(":") }
                ?: daySchedules.firstOrNull()
        } else {
            daySchedules.firstOrNull()
        }

        if (match == null) {
            return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.CLASS_NOT_FOUND)
        }

        val action = SuePendingAction.DeleteSchedule(
            studentName = match.studentName,
            studentId = match.studentId,
            scheduleId = match.scheduleId,
            dayOfWeek = match.dayOfWeek,
            startTime = match.startTime
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes a confirmed DeleteSchedule action.
     */
    suspend fun executeDeleteSchedule(action: SuePendingAction.DeleteSchedule): SueOperationResult.Execute {
        val professorId = getProfessorId().takeIf { it.isNotEmpty() }
            ?: return SueOperationResult.Execute.AuthError

        return when (val result = deleteScheduleUseCase(professorId, action.studentId, action.scheduleId)) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Prepares an AddExtraClass action for user confirmation.
     */
    suspend fun prepareAddExtraClass(
        studentName: String,
        dateMillis: Long,
        startTime: String,
        endTime: String
    ): SueOperationResult.Prepare {
        val students = queryStudentsUseCase.searchByName(studentName)
        val match = students.firstOrNull { it.name.lowercase().contains(studentName.lowercase()) }

        if (match == null) {
            return SueOperationResult.Prepare.Error(SueOperationResult.ErrorType.STUDENT_NOT_FOUND)
        }

        val action = SuePendingAction.AddExtraClass(
            studentName = match.name,
            studentId = match.studentId,
            date = dateMillis,
            startTime = startTime,
            endTime = endTime
        )
        return SueOperationResult.Prepare.Success(action)
    }

    /**
     * Executes a confirmed AddExtraClass action.
     */
    suspend fun executeAddExtraClass(action: SuePendingAction.AddExtraClass): SueOperationResult.Execute {
        val professorId = getProfessorId().takeIf { it.isNotEmpty() }
            ?: return SueOperationResult.Execute.AuthError

        val exception = ScheduleException(
            studentId = action.studentId,
            professorId = professorId,
            date = action.date,
            type = ExceptionType.EXTRA,
            originalScheduleId = "EXTRA", // Special original schedule ID per business rules
            newStartTime = action.startTime,
            newEndTime = action.endTime
        )

        return when (val result = saveScheduleExceptionUseCase(professorId, action.studentId, exception)) {
            is Result.Success -> SueOperationResult.Execute.Success(action)
            is Result.Error -> SueOperationResult.Execute.Error(result.error)
        }
    }

    /**
     * Returns a human-readable description of cancelled/rescheduled/extra class exceptions.
     *
     * @param dayOfWeek Optional ISO day (1=Monday…7=Sunday). When provided, only exceptions
     *                  for that specific day are returned so the LLM receives focused context.
     */
    /**
     * Returns a human-readable description of cancelled/rescheduled/extra class exceptions.
     *
     * @param dayOfWeek Optional ISO day (1=Monday…7=Sunday). When provided, only exceptions
     *                  for that specific day are returned so the LLM receives focused context.
     * @param studentNameFilter Optional student name filter. When provided, only exceptions
     *                          for that specific student are matched.
     */
    suspend fun getCancelledClassesDescription(
        dayOfWeek: Int? = null,
        studentNameFilter: String? = null
    ): String {
        val professorId = getProfessorId().takeIf { it.isNotEmpty() } ?: return ""
        val allExceptions = exceptionRepository.getAllExceptions(professorId)
        if (allExceptions.isEmpty()) return ""

        val students = queryStudentsUseCase.searchByName("")
        val schedules = querySchedulesUseCase.getScheduleDetails()

        val matchingStudentIds = if (!studentNameFilter.isNullOrBlank()) {
            students.filter { it.name.contains(studentNameFilter, ignoreCase = true) }
                .map { it.studentId }
                .toSet()
        } else null

        val today = dateTimeProvider.getNow().toLocalDate()
        val startOfWeek = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

        val exceptions = allExceptions.filter { exc ->
            val localDate = java.time.Instant.ofEpochMilli(exc.date)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

            val matchesDay = if (dayOfWeek != null) {
                val targetDate = nextOccurrenceDate(java.time.DayOfWeek.of(dayOfWeek))
                localDate == targetDate
            } else {
                // Whole week from Monday onwards (not past weeks)
                !localDate.isBefore(startOfWeek)
            }

            val matchesStudent = if (matchingStudentIds != null && matchingStudentIds.isNotEmpty()) {
                exc.studentId in matchingStudentIds
            } else true

            matchesDay && matchesStudent
        }

        if (exceptions.isEmpty()) return ""

        return buildString {
            appendLine("--- EXCEPCIONES Y CAMBIOS DEL CALENDARIO ---")
            exceptions.forEach { exc ->
                val localDate = java.time.Instant.ofEpochMilli(exc.date).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val dayOfWeekEs = when (localDate.dayOfWeek) {
                    java.time.DayOfWeek.MONDAY -> "Lunes"
                    java.time.DayOfWeek.TUESDAY -> "Martes"
                    java.time.DayOfWeek.WEDNESDAY -> "Miércoles"
                    java.time.DayOfWeek.THURSDAY -> "Jueves"
                    java.time.DayOfWeek.FRIDAY -> "Viernes"
                    java.time.DayOfWeek.SATURDAY -> "Sábado"
                    java.time.DayOfWeek.SUNDAY -> "Domingo"
                }
                val dateStr = "$dayOfWeekEs ${localDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
                val studentName = schedules.firstOrNull { it.studentId == exc.studentId }?.studentName
                    ?: students.firstOrNull { it.studentId == exc.studentId }?.name
                    ?: "Alumno"
                val schedule = schedules.firstOrNull { it.scheduleId == exc.originalScheduleId || it.studentId == exc.studentId }
                val timeInfo = if (schedule != null) " (${schedule.startTime} - ${schedule.endTime})" else ""

                when (exc.type) {
                    ExceptionType.CANCELLED -> {
                        appendLine("- Fecha $dateStr: [CLASE CANCELADA] La clase con $studentName$timeInfo está CANCELADA (no se imparte).")
                    }
                    ExceptionType.RESCHEDULED -> {
                        appendLine("- Fecha $dateStr: [CLASE REPROGRAMADA] La clase con $studentName se ha movido al nuevo horario: ${exc.newStartTime} - ${exc.newEndTime}.")
                    }
                    ExceptionType.EXTRA -> {
                        appendLine("- Fecha $dateStr: [CLASE EXTRA ACTIVA] Se ha añadido una clase extra con $studentName de ${exc.newStartTime} a ${exc.newEndTime} (ESTA CLASE SÍ SE IMPARTE, es una clase activa añadida).")
                    }
                }
            }
            appendLine("--- FIN EXCEPCIONES ---")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun nextOccurrenceDate(dayOfWeek: DayOfWeek): LocalDate =
        dateTimeProvider.getNow().toLocalDate().with(TemporalAdjusters.nextOrSame(dayOfWeek))
}

