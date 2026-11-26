package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Internal delegate handler for [SueRuleIntentDetector] that processes schedule-related intents
 * (e.g. starting classes, adding extra classes, creating/deleting schedules, cancelling,
 * rescheduling, querying free slots, and daily schedule summaries).
 * This component is an internal domain delegate, not a standalone public Use Case.
 */
internal class SueScheduleIntentHandler @Inject constructor(
    private val scheduleTools: ScheduleTools,
    private val studentTools: StudentTools,
    private val authRepository: AuthRepository,
    private val dateTimeProvider: DateTimeProvider,
    private val entityExtractor: SueEntityExtractor,
    private val conversationContext: SueConversationContext
) {

    /**
     * Handles starting an immediate class session for a student.
     *
     * @return Operation result representing the prepared action or conversational slot prompt.
     */
    internal suspend fun handleStartClass(): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
            conversationContext.setAwaitingSlot(IntentType.START_CLASS, SlotType.STUDENT_NAME)
            SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                "¿Para qué alumno quieres iniciar la clase?"
            )
        } else {
            val duration = conversationContext.lastMentionedDuration
            if (duration == null) {
                conversationContext.setAwaitingSlot(IntentType.START_CLASS, SlotType.DURATION)
                SueOperationResult.Prepare.Error(
                    SueOperationResult.ErrorType.UNKNOWN,
                    "¿De cuántos minutos será la clase con $studentName?"
                )
            } else {
                studentTools.prepareStartClass(studentName, duration)
            }
        }
    }

    /**
     * Handles scheduling an extra/ad-hoc class session for a student.
     *
     * @param lower Normalized lowercase user query string.
     * @return Operation result representing the prepared action or prompt.
     */
    internal suspend fun handleAddExtraClass(lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
            conversationContext.setAwaitingSlot(IntentType.ADD_EXTRA_CLASS, SlotType.STUDENT_NAME)
            SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                "¿Para qué alumno quieres programar la clase extra?"
            )
        } else {
            val times = entityExtractor.extractTwoTimes(lower) ?: conversationContext.lastMentionedTime?.let { lastTime ->
                val parts = lastTime.split(":")
                val h = parts[0].toInt()
                val m = parts[1].toInt()
                val endH = (h + 1) % 24
                val endTime = "%02d:%02d".format(endH, m)
                Pair(lastTime, endTime)
            }

            val dayOfWeek = conversationContext.lastMentionedDayOfWeek ?: if (times != null) dateTimeProvider.getNow().dayOfWeek.value else null
            if (dayOfWeek == null) {
                conversationContext.setAwaitingSlot(IntentType.ADD_EXTRA_CLASS, SlotType.DAY_OF_WEEK)
                SueOperationResult.Prepare.Error(
                    SueOperationResult.ErrorType.UNKNOWN,
                    "¿Qué día y a qué hora quieres la clase extra de $studentName?"
                )
            } else if (times == null) {
                conversationContext.setAwaitingSlot(IntentType.ADD_EXTRA_CLASS, SlotType.TIME)
                val dayName = when (dayOfWeek) {
                    1 -> "el lunes"; 2 -> "el martes"; 3 -> "el miércoles"; 4 -> "el jueves"
                    5 -> "el viernes"; 6 -> "el sábado"; 7 -> "el domingo"; else -> "ese día"
                }
                SueOperationResult.Prepare.Error(
                    SueOperationResult.ErrorType.UNKNOWN,
                    "¿A qué hora quieres programar la clase extra de $studentName $dayName?"
                )
            } else {
                val targetDate = dateTimeProvider.getNow().toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)))
                val dateMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                scheduleTools.prepareAddExtraClass(studentName, dateMillis, times.first, times.second)
            }
        }
    }

    /**
     * Handles creating a recurring weekly schedule slot for a student.
     *
     * @param lower Normalized lowercase user query string.
     * @return Operation result representing the prepared creation action or prompt.
     */
    internal suspend fun handleCreateSchedule(lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
            conversationContext.setAwaitingSlot(IntentType.CREATE_SCHEDULE, SlotType.STUDENT_NAME)
            SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                "¿Para qué alumno deseas configurar el horario?"
            )
        } else {
            val dayOfWeek = conversationContext.lastMentionedDayOfWeek
            if (dayOfWeek == null) {
                conversationContext.setAwaitingSlot(IntentType.CREATE_SCHEDULE, SlotType.DAY_OF_WEEK)
                SueOperationResult.Prepare.Error(
                    SueOperationResult.ErrorType.UNKNOWN,
                    "¿Qué día de la semana será la clase de $studentName?"
                )
            } else {
                val times = entityExtractor.extractTwoTimes(lower) ?: conversationContext.lastMentionedTime?.let { lastTime ->
                    val parts = lastTime.split(":")
                    val h = parts[0].toInt()
                    val m = parts[1].toInt()
                    val endH = (h + 1) % 24
                    val endTime = "%02d:%02d".format(endH, m)
                    Pair(lastTime, endTime)
                }
                if (times == null) {
                    conversationContext.setAwaitingSlot(IntentType.CREATE_SCHEDULE, SlotType.TIME)
                    val dayName = when (dayOfWeek) {
                        1 -> "lunes"; 2 -> "martes"; 3 -> "miércoles"; 4 -> "jueves"
                        5 -> "viernes"; 6 -> "sábado"; 7 -> "domingo"; else -> "ese día"
                    }
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.UNKNOWN,
                        "¿A qué hora será la clase de $studentName los $dayName?"
                    )
                } else {
                    scheduleTools.prepareCreateSchedule(studentName, dayOfWeek, times.first, times.second)
                }
            }
        }
    }

    /**
     * Handles deleting a recurring schedule slot for a student.
     *
     * @return Operation result representing the prepared deletion action or prompt.
     */
    internal suspend fun handleDeleteSchedule(): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
            conversationContext.setAwaitingSlot(IntentType.DELETE_SCHEDULE, SlotType.STUDENT_NAME)
            SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                "¿De qué alumno quieres eliminar el horario?"
            )
        } else {
            val dayOfWeek = conversationContext.lastMentionedDayOfWeek
            if (dayOfWeek == null) {
                conversationContext.setAwaitingSlot(IntentType.DELETE_SCHEDULE, SlotType.DAY_OF_WEEK)
                SueOperationResult.Prepare.Error(
                    SueOperationResult.ErrorType.UNKNOWN,
                    "¿Qué día de la semana es el horario que quieres eliminar?"
                )
            } else {
                val time = conversationContext.lastMentionedTime
                scheduleTools.prepareDeleteSchedule(studentName, dayOfWeek, time)
            }
        }
    }

    /**
     * Handles cancelling a scheduled class occurrence.
     *
     * @param lower Normalized lowercase user query string.
     * @return Operation result representing the cancellation action or prompt.
     */
    internal suspend fun handleCancelClass(lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
            conversationContext.setAwaitingSlot(IntentType.CANCEL_CLASS, SlotType.STUDENT_NAME)
            SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                "¿De qué alumno quieres cancelar la clase?"
            )
        } else {
            var dayOfWeek = conversationContext.lastMentionedDayOfWeek
            val time = conversationContext.lastMentionedTime
            val preferExtra = listOf("extra", "extras", "adicional", "adicionales", "refuerzo").any { it in lower }

            if (dayOfWeek == null) {
                val schedules = scheduleTools.getSchedulesByStudentName(studentName)
                if (preferExtra) {
                    scheduleTools.prepareCancelAction(
                        studentName,
                        dateTimeProvider.getNow().dayOfWeek.value,
                        time,
                        preferExtra = true
                    )
                } else if (schedules.isEmpty()) {
                    scheduleTools.prepareCancelAction(
                        studentName,
                        dateTimeProvider.getNow().dayOfWeek.value,
                        time,
                        preferExtra = false
                    )
                } else if (schedules.size == 1) {
                    dayOfWeek = schedules.first().dayOfWeek
                    scheduleTools.prepareCancelAction(studentName, dayOfWeek, time, preferExtra = preferExtra)
                } else {
                    conversationContext.setAwaitingSlot(IntentType.CANCEL_CLASS, SlotType.DAY_OF_WEEK)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                        "¿Qué día es la clase de $studentName que quieres cancelar?"
                    )
                }
            } else {
                scheduleTools.prepareCancelAction(studentName, dayOfWeek, time, preferExtra = preferExtra)
            }
        }
    }

    /**
     * Handles rescheduling a class occurrence to a different date or time.
     *
     * @param lower Normalized lowercase user query string.
     * @return Operation result representing the reschedule action or prompt.
     */
    internal suspend fun handleRescheduleClass(lower: String): SueOperationResult {
        val studentName = conversationContext.lastMentionedStudentName
        return if (studentName == null) {
            conversationContext.setAwaitingSlot(IntentType.RESCHEDULE_CLASS, SlotType.STUDENT_NAME)
            SueOperationResult.Prepare.Error(
                SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                "¿De qué alumno quieres mover la clase?"
            )
        } else {
            val days = entityExtractor.extractTwoDaysOfWeek(lower)
            var fromDay: Int? = null
            var toDay: Int? = null

            val schedules = scheduleTools.getSchedulesByStudentName(studentName)
            if (schedules.isEmpty()) {
                SueOperationResult.Prepare.Error(
                    SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                    "No he encontrado ninguna clase programada para $studentName."
                )
            } else {
                if (days.first != null && days.second != null) {
                    fromDay = days.first
                    toDay = days.second
                } else if (days.first != null) {
                    val singleDay = days.first!!
                    val hasClassOnSingleDay = schedules.any { it.dayOfWeek == singleDay }
                    if (hasClassOnSingleDay) {
                        if (entityExtractor.isTargetDay(lower, singleDay)) {
                            toDay = singleDay
                        } else {
                            fromDay = singleDay
                        }
                    } else {
                        toDay = singleDay
                    }
                }

                val times = entityExtractor.extractAllTimesInQuery(lower)
                var fromTime: String? = null
                var targetTime: String? = null

                if (times.size >= 2) {
                    val time0 = times[0]
                    val time1 = times[1]
                    val rawHour0 = time0.substringBefore(":")
                    val rawHour1 = time1.substringBefore(":")
                    val isTarget0 = Regex("""\b(?:a|para|hacia|to)\s+(?:las?\s+)?(?:$time0|$rawHour0)\b""").containsMatchIn(lower)
                    val isSource1 = Regex("""\b(?:de|desde|que\s+es\s+a|from|which\s+is\s+at)\s+(?:las?\s+)?(?:$time1|$rawHour1)\b""").containsMatchIn(lower)
                    if (isTarget0 && isSource1) {
                        fromTime = time1
                        targetTime = time0
                    } else {
                        fromTime = time0
                        targetTime = time1
                    }
                } else if (times.size == 1) {
                    val singleTime = times[0]
                    val rawHour = singleTime.substringBefore(":")
                    val isSourceMarker = Regex("""\b(?:de|desde|from)\s+(?:las?\s+)?(?:$singleTime|$rawHour)\b""").containsMatchIn(lower)
                    val isTargetMarker = Regex("""\b(?:a|para|hacia|to|at)\s+(?:las?\s+)?(?:$singleTime|$rawHour)\b""").containsMatchIn(lower)
                    val hasClassAtTime = schedules.any {
                        it.startTime == singleTime || it.startTime.substringBefore(":") == rawHour
                    }
                    if (isSourceMarker) {
                        fromTime = singleTime
                        targetTime = null
                    } else if (isTargetMarker && !hasClassAtTime) {
                        fromTime = null
                        targetTime = singleTime
                    } else if (hasClassAtTime && !isTargetMarker) {
                        fromTime = singleTime
                        targetTime = singleTime
                    } else {
                        fromTime = null
                        targetTime = singleTime
                    }
                } else {
                    fromTime = null
                    targetTime = conversationContext.lastMentionedTime
                }

                if (fromDay == null && fromTime != null) {
                    val matching = schedules.firstOrNull {
                        it.startTime == fromTime || it.startTime.substringBefore(":") == fromTime.substringBefore(":")
                    }
                    if (matching != null) {
                        fromDay = matching.dayOfWeek
                    }
                }

                if (fromDay == null) {
                    if (schedules.size == 1) {
                        fromDay = schedules.first().dayOfWeek
                    } else {
                        val fallbackDay = conversationContext.lastMentionedDayOfWeek ?: dateTimeProvider.getNow().dayOfWeek.value
                        val hasClassOnFallback = schedules.any { it.dayOfWeek == fallbackDay }
                        if (hasClassOnFallback) {
                            fromDay = fallbackDay
                        } else {
                            val todayDay = dateTimeProvider.getNow().dayOfWeek.value
                            val todayMatch = schedules.firstOrNull { it.dayOfWeek == todayDay }
                            if (todayMatch != null) {
                                fromDay = todayDay
                            } else {
                                fromDay = schedules.firstOrNull()?.dayOfWeek
                            }
                        }
                    }
                }

                if (fromDay == null) {
                    conversationContext.setAwaitingSlot(IntentType.RESCHEDULE_CLASS, SlotType.DAY_OF_WEEK)
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                        "¿Qué día es la clase de $studentName que quieres mover?"
                    )
                } else {
                    if (toDay == null) {
                        toDay = fromDay
                    }

                    val daySchedules = schedules.filter { it.dayOfWeek == fromDay }
                    if (fromTime == null && daySchedules.size == 1) {
                        fromTime = daySchedules.first().startTime
                    }

                    if (targetTime == null) {
                        if (fromTime == null && daySchedules.size > 1) {
                            return SueOperationResult.Prepare.Error(
                                SueOperationResult.ErrorType.UNKNOWN,
                                "¿Qué clase de $studentName quieres mover? Tiene varias ese día."
                            )
                        }
                        val match = if (fromTime != null) {
                            daySchedules.firstOrNull { it.startTime == fromTime }
                                ?: daySchedules.firstOrNull { it.startTime.substringBefore(":") == fromTime.substringBefore(":") }
                                ?: daySchedules.firstOrNull()
                        } else {
                            daySchedules.firstOrNull()
                        }
                        if (match != null) {
                            targetTime = match.startTime
                        }
                    }

                    if (targetTime == null) {
                        conversationContext.setAwaitingSlot(IntentType.RESCHEDULE_CLASS, SlotType.TIME)
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿A qué hora quieres programar la clase de $studentName?"
                        )
                    } else {
                        scheduleTools.prepareRescheduleAction(studentName, fromDay, toDay, targetTime, fromTime)
                    }
                }
            }
        }
    }

    /**
     * Queries the next scheduled class for the active or all students.
     *
     * @return Operation result representing the next class occurrence.
     */
    internal suspend fun handleQueryNextClass(): SueOperationResult {
        val nextClassResult = scheduleTools.getNextClass(conversationContext.lastMentionedStudentName)
        if (nextClassResult is SueOperationResult.NextClass && nextClassResult.schedule != null) {
            val s = nextClassResult.schedule
            conversationContext.lastMentionedStudentName = s.studentName
            conversationContext.lastMentionedDayOfWeek = s.dayOfWeek
            conversationContext.lastMentionedTime = s.startTime
        }
        return nextClassResult
    }

    /**
     * Queries available free time slots for teaching.
     *
     * @return Operation result containing free schedule slots.
     */
    internal suspend fun handleQueryFreeSlots(): SueOperationResult {
        val day = conversationContext.lastMentionedDayOfWeek
        val professor = authRepository.currentUser.value
        val workingStart = professor?.workingStartTime?.takeIf { it.isNotBlank() } ?: "08:00"
        val workingEnd   = professor?.workingEndTime?.takeIf { it.isNotBlank() } ?: "23:00"
        return scheduleTools.getFreeSlots(workingStart, workingEnd, day)
    }

    /**
     * Queries the schedule for a specific day.
     *
     * @param lower Normalized user query string.
     * @return Operation result with the day's classes.
     */
    internal suspend fun handleQueryDaySchedule(lower: String): SueOperationResult {
        val day = conversationContext.lastMentionedDayOfWeek ?: dateTimeProvider.getNow().dayOfWeek.value
        val timeFilter = entityExtractor.extractTime(lower) ?: entityExtractor.extractTimeOfDayFilter(lower)
        return scheduleTools.getScheduleForDay(day, timeFilter)
    }

    /**
     * Queries the summary of all classes and balances for today.
     *
     * @return Operation result with today's class summary and debt alerts.
     */
    internal suspend fun handleQueryTodaySummary(): SueOperationResult {
        val today = dateTimeProvider.getNow().dayOfWeek.value
        val todayResult = scheduleTools.getScheduleForDay(today, null)
        val scheduleText = SueResponseFormatter.format(todayResult)

        val debtors = studentTools.getStudentsWithBalance()
        val debtorResult = debtors as? SueOperationResult.StudentsWithBalance

        val debtAlert = if (debtorResult != null && debtorResult.students.isNotEmpty()) {
            val todayNames = if (todayResult is SueOperationResult.DaySchedule) {
                todayResult.schedules.map { it.studentName.lowercase() }.toSet()
            } else emptySet()
            val debtorsToday = debtorResult.students.filter { it.name.lowercase() in todayNames }
            if (debtorsToday.isNotEmpty()) {
                "\n\n⚠️ Tienen saldo pendiente y clase hoy: " +
                debtorsToday.joinToString(", ") { "${it.name} (${"%.2f".format(it.pendingBalance)} €)" }
            } else ""
        } else ""

        return SueOperationResult.ReadSuccess(scheduleText + debtAlert)
    }

    /**
     * Queries cancelled classes for a specific day or student.
     *
     * @param lower Normalized user query string.
     * @param matchedStudentName Optional matched student name.
     * @param relativeDay Relative day offset if mentioned.
     * @param explicitDay Explicit day of week value if mentioned.
     * @return Operation result listing cancelled classes.
     */
    internal suspend fun handleQueryCancelledClasses(
        lower: String,
        matchedStudentName: String?,
        relativeDay: Int?,
        explicitDay: Int?
    ): SueOperationResult {
        val queryDay = relativeDay ?: explicitDay
        val typeFilter = when {
            listOf("extra", "extras", "adicional", "adicionales").any { it in lower } -> ExceptionType.EXTRA
            listOf("reprogramada", "reprogramadas", "reprogramado", "reprogramados", "movida", "movidas").any { it in lower } -> ExceptionType.RESCHEDULED
            listOf("cancelada", "canceladas", "cancelado", "cancelados", "anulada", "anuladas", "anulado", "anulados").any { it in lower } -> ExceptionType.CANCELLED
            else -> null
        }
        val cancelledDesc = scheduleTools.getCancelledClassesDescription(queryDay, matchedStudentName, typeFilter)
        return if (cancelledDesc.isNotBlank()) {
            SueOperationResult.ReadSuccess(cancelledDesc)
        } else {
            val typeLabel = when (typeFilter) {
                ExceptionType.EXTRA -> "clases extra registradas"
                ExceptionType.RESCHEDULED -> "clases reprogramadas registradas"
                ExceptionType.CANCELLED -> "clases canceladas registradas"
                null -> "clases canceladas, reprogramadas ni extra registradas"
            }
            val dayLabel = if (queryDay != null) {
                val dayNames = listOf("el lunes", "el martes", "el miércoles", "el jueves", "el viernes", "el sábado", "el domingo")
                dayNames.getOrElse(queryDay - 1) { "el día seleccionado" }
            } else if (matchedStudentName != null) {
                "con $matchedStudentName"
            } else "esta semana"
            SueOperationResult.ReadSuccess("No hay $typeLabel $dayLabel.")
        }
    }
}
