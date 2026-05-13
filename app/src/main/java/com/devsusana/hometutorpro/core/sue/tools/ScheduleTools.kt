package com.devsusana.hometutorpro.core.sue.tools

import com.devsusana.hometutorpro.core.sue.SuePendingAction
import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.usecases.IManageScheduleForAgentUseCase
import com.devsusana.hometutorpro.domain.usecases.IQuerySchedulesForAgentUseCase
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tool definitions for schedule-related queries and management actions.
 *
 * All functions are `suspend` to comply with AGENTS.md Rule 1 (Coroutines),
 * ensuring database I/O does not block the main thread.
 *
 * Provides the Sue agent with:
 * - **Read**: weekly schedule, next upcoming class, free time slots.
 * - **Write**: prepare cancel/reschedule confirmations and execute them once confirmed.
 *
 * All write operations go through [IManageScheduleForAgentUseCase] which delegates
 * business-rule validation to [ISaveScheduleExceptionUseCase].
 */
@Singleton
class ScheduleTools @Inject constructor(
    private val querySchedulesUseCase: IQuerySchedulesForAgentUseCase,
    private val manageScheduleUseCase: IManageScheduleForAgentUseCase,
    private val secureAuthManager: SecureAuthManager
) {

    companion object {
        private val DAY_NAMES_ES = mapOf(
            1 to "lunes",
            2 to "martes",
            3 to "miércoles",
            4 to "jueves",
            5 to "viernes",
            6 to "sábado",
            7 to "domingo"
        )
        private val DATE_FORMATTER_ES = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ operations
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the full weekly schedule formatted in Spanish by day.
     */
    suspend fun getWeeklySchedule(): String {
        val schedules = querySchedulesUseCase.getAllSchedules()
        if (schedules.isEmpty()) return "No tienes ninguna clase programada esta semana."

        val byDay = schedules.groupBy { it.dayOfWeek }
        return buildString {
            appendLine("Horario semanal:")
            for (day in 1..7) {
                val daySchedules = byDay[day] ?: continue
                val dayName = DAY_NAMES_ES[day]?.replaceFirstChar { it.uppercase() } ?: "Día $day"
                appendLine("\n$dayName:")
                daySchedules.forEach { s ->
                    appendLine("  ${s.startTime}–${s.endTime}: ${s.studentName}")
                }
            }
        }
    }

    /**
     * Returns the schedule for a specific day of the week.
     *
     * @param dayOfWeek ISO day value (1=Monday … 7=Sunday).
     * @param timeFilter Optional filter: "morning" (before 14:00), "afternoon" (14:00+), or null (all day).
     */
    suspend fun getScheduleForDay(dayOfWeek: Int, timeFilter: String? = null): String {
        val dayName = DAY_NAMES_ES[dayOfWeek]?.replaceFirstChar { it.uppercase() } ?: "Día $dayOfWeek"
        val allSchedules = querySchedulesUseCase.getAllSchedules()
            .filter { it.dayOfWeek == dayOfWeek }

        val schedules = when (timeFilter) {
            "morning" -> allSchedules.filter {
                LocalTime.parse(it.startTime, TIME_FORMATTER).isBefore(LocalTime.of(14, 0))
            }
            "afternoon" -> allSchedules.filter {
                LocalTime.parse(it.startTime, TIME_FORMATTER).isAfter(LocalTime.of(13, 59))
            }
            else -> allSchedules
        }

        val slotLabel = when (timeFilter) {
            "morning" -> " (mañana)"
            "afternoon" -> " (tarde)"
            else -> ""
        }

        if (schedules.isEmpty()) {
            return if (timeFilter != null) {
                "El $dayName$slotLabel no tienes ninguna clase programada."
            } else {
                "El $dayName no tienes ninguna clase programada."
            }
        }

        return buildString {
            appendLine("$dayName$slotLabel (${schedules.size} clases):")
            schedules.forEach { s ->
                appendLine("  ${s.startTime}–${s.endTime}: ${s.studentName}")
            }
        }
    }


    /**
     * Returns the next upcoming class from the current moment.
     * Searches the current week first, then wraps to the following week.
     */
    suspend fun getNextClass(): String {
        val schedules = querySchedulesUseCase.getAllSchedules()
        if (schedules.isEmpty()) return "No tienes ninguna clase programada."

        val now = LocalDate.now()
        val currentTime = LocalTime.now()
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

        if (candidate == null) return "No se encontró ninguna clase próxima."

        val targetDayOfWeek = DayOfWeek.of(candidate.dayOfWeek)
        val occurrenceDate = if (candidate.dayOfWeek >= todayIso &&
            candidate != sorted.firstOrNull()
        ) {
            now.with(TemporalAdjusters.nextOrSame(targetDayOfWeek))
        } else {
            now.with(TemporalAdjusters.next(targetDayOfWeek))
        }

        val dayLabel = occurrenceDate.format(DATE_FORMATTER_ES)
        return "Tu próxima clase es con ${candidate.studentName} el $dayLabel de ${candidate.startTime} a ${candidate.endTime}."
    }

    /**
     * Returns the weekdays (Monday–Friday) that have NO classes scheduled.
     * Used when the user asks "what free days do I have this week".
     */
    suspend fun getFreeSlots(): String {
        val schedules = querySchedulesUseCase.getAllSchedules()
        val scheduledDays = schedules.map { it.dayOfWeek }.toSet()
        val freeDays = (1..5).filter { it !in scheduledDays }

        if (freeDays.isEmpty()) {
            return "Tienes clases programadas de lunes a viernes. No hay días completamente libres entre semana."
        }

        return buildString {
            appendLine("Días libres esta semana (sin clases programadas):")
            freeDays.forEach { day ->
                val dayName = DAY_NAMES_ES[day]?.replaceFirstChar { it.uppercase() } ?: "Día $day"
                appendLine("  • $dayName")
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // WRITE operations — two-step: prepare (ask confirmation) → execute
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Looks up [studentName]'s schedule for [dayOfWeek] and, if found, builds a
     * [SuePendingAction.CancelClass] with all data pre-resolved.
     *
     * @param studentName Partial or full student name as spoken by the user.
     * @param dayOfWeek   ISO day (1=Monday … 7=Sunday).
     * @return Pair of resolved action (null if not found) + message for the user.
     */
    suspend fun prepareCancelAction(
        studentName: String,
        dayOfWeek: Int
    ): Pair<SuePendingAction.CancelClass?, String> {
        val schedules = querySchedulesUseCase.getSchedulesByStudentName(studentName)
        val match = schedules.firstOrNull { it.dayOfWeek == dayOfWeek }

        if (match == null) {
            val dayName = DAY_NAMES_ES[dayOfWeek] ?: "ese día"
            return Pair(
                null,
                "No he encontrado ninguna clase de $studentName el $dayName. " +
                        "Comprueba el nombre del alumno y el día."
            )
        }

        val targetDate = nextOccurrenceDate(DayOfWeek.of(dayOfWeek))
        val dateMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayLabel = targetDate.format(DATE_FORMATTER_ES)

        val action = SuePendingAction.CancelClass(
            studentName = match.studentName,
            studentId = match.studentId,
            scheduleId = match.scheduleId,
            date = dateMillis,
            dayLabel = dayLabel,
            startTime = match.startTime,
            endTime = match.endTime
        )
        val confirmText = "¿Confirmas que quieres cancelar la clase de ${match.studentName} " +
                "el $dayLabel de ${match.startTime} a ${match.endTime}? " +
                "Di sí para confirmar o no para cancelar."
        return Pair(action, confirmText)
    }

    /**
     * Looks up [studentName]'s schedule for [fromDayOfWeek] and builds a
     * [SuePendingAction.RescheduleClass] targeting [toDayOfWeek] at [newStartTime].
     * Duration is preserved from the original schedule entry.
     *
     * @param studentName   Partial or full student name.
     * @param fromDayOfWeek Source ISO day (1=Monday … 7=Sunday).
     * @param toDayOfWeek   Target ISO day.
     * @param newStartTime  New start time in "HH:mm" format.
     */
    suspend fun prepareRescheduleAction(
        studentName: String,
        fromDayOfWeek: Int,
        toDayOfWeek: Int,
        newStartTime: String
    ): Pair<SuePendingAction.RescheduleClass?, String> {
        val schedules = querySchedulesUseCase.getSchedulesByStudentName(studentName)
        val match = schedules.firstOrNull { it.dayOfWeek == fromDayOfWeek }

        if (match == null) {
            val dayName = DAY_NAMES_ES[fromDayOfWeek] ?: "ese día"
            return Pair(null, "No he encontrado ninguna clase de $studentName el $dayName.")
        }

        // Preserve original class duration
        val origStart = LocalTime.parse(match.startTime, TIME_FORMATTER)
        val origEnd = LocalTime.parse(match.endTime, TIME_FORMATTER)
        val durationMinutes = java.time.Duration.between(origStart, origEnd).toMinutes()
        val parsedNewStart = LocalTime.parse(newStartTime, TIME_FORMATTER)
        val newEndTime = parsedNewStart.plusMinutes(durationMinutes).format(TIME_FORMATTER)

        val originalDate = nextOccurrenceDate(DayOfWeek.of(fromDayOfWeek))
        val originalMillis = originalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val originalLabel = originalDate.format(DATE_FORMATTER_ES)

        val targetDayOfWeek = DayOfWeek.of(toDayOfWeek)
        val targetDate = nextOccurrenceDate(targetDayOfWeek)
        val targetMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val targetLabel = targetDate.format(DATE_FORMATTER_ES)

        val newDayOfWeek = if (fromDayOfWeek != toDayOfWeek) targetDayOfWeek else null

        val action = SuePendingAction.RescheduleClass(
            studentName = match.studentName,
            studentId = match.studentId,
            scheduleId = match.scheduleId,
            originalDate = originalMillis,
            originalDayLabel = originalLabel,
            originalStartTime = match.startTime,
            newDayOfWeek = newDayOfWeek,
            newDate = targetMillis,
            newDayLabel = targetLabel,
            newStartTime = newStartTime,
            newEndTime = newEndTime
        )
        val confirmText = "¿Confirmas mover la clase de ${match.studentName} del $originalLabel " +
                "al $targetLabel de $newStartTime a $newEndTime? " +
                "Di sí para confirmar o no para cancelar."
        return Pair(action, confirmText)
    }

    /**
     * Executes a confirmed [SuePendingAction.CancelClass].
     *
     * @return Human-readable result message in Spanish.
     */
    suspend fun executeCancelAction(action: SuePendingAction.CancelClass): String {
        val professorId = secureAuthManager.getUserId()
            ?: return "No se pudo cancelar la clase: no hay sesión activa."

        return when (val result = manageScheduleUseCase.cancelClass(
            professorId = professorId,
            studentId = action.studentId,
            scheduleId = action.scheduleId,
            date = action.date
        )) {
            is Result.Success ->
                "Hecho. La clase de ${action.studentName} del ${action.dayLabel} de " +
                        "${action.startTime} a ${action.endTime} ha sido cancelada. " +
                        "El alumno sigue teniendo su hora habitual en la siguiente semana."
            is Result.Error ->
                "No se pudo cancelar la clase: ${result.error}."
        }
    }

    /**
     * Executes a confirmed [SuePendingAction.RescheduleClass].
     *
     * @return Human-readable result message in Spanish.
     */
    suspend fun executeRescheduleAction(action: SuePendingAction.RescheduleClass): String {
        val professorId = secureAuthManager.getUserId()
            ?: return "No se pudo mover la clase: no hay sesión activa."

        return when (val result = manageScheduleUseCase.rescheduleClass(
            professorId = professorId,
            studentId = action.studentId,
            scheduleId = action.scheduleId,
            originalDate = action.originalDate,
            newDayOfWeek = action.newDayOfWeek,
            newStartTime = action.newStartTime,
            newEndTime = action.newEndTime
        )) {
            is Result.Success ->
                "Hecho. La clase de ${action.studentName} se ha movido del " +
                        "${action.originalDayLabel} al ${action.newDayLabel} de " +
                        "${action.newStartTime} a ${action.newEndTime}."
            is Result.Error ->
                "No se pudo mover la clase: ${result.error}."
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the next occurrence of [dayOfWeek] from today (inclusive if today matches).
     */
    private fun nextOccurrenceDate(dayOfWeek: DayOfWeek): LocalDate =
        LocalDate.now().with(TemporalAdjusters.nextOrSame(dayOfWeek))
}
