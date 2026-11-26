package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import java.time.DayOfWeek

/**
 * Gathers schedule, cancellation, and free-slot context blocks for [SueContextBuilder].
 */
internal class SueScheduleContextGatherer(
    private val scheduleTools: ScheduleTools,
    private val dateTimeProvider: DateTimeProvider,
    private val entityExtractor: SueEntityExtractor,
    private val conversationContext: SueConversationContext
) {

    /**
     * Builds schedule-related context blocks when the query targets schedules, cancellations, or free slots.
     *
     * @param lowerQuery Normalized lowercase query string without accents.
     * @param day Optional ISO day-of-week integer (`1`..`7`) extracted from the query.
     * @param studentName Optional student name mentioned in the query.
     * @param workingStart Professor's working start time in `HH:mm` format.
     * @param workingEnd Professor's working end time in `HH:mm` format.
     * @return Formatted schedule context block, or an empty string if not applicable.
     */
    suspend fun gatherScheduleContext(
        lowerQuery: String,
        day: Int?,
        studentName: String?,
        workingStart: String,
        workingEnd: String
    ): String {
        if (!SueContextKeywords.containsScheduleKeywords(lowerQuery) &&
            !SueContextKeywords.containsCancelledQueryKeywords(lowerQuery)
        ) {
            return ""
        }

        val isAskingPurelyAboutCancellations = SueContextKeywords.containsCancelledQueryKeywords(lowerQuery) &&
            !listOf("activa", "activas", "no cancelada", "no canceladas", "todas las clases", "horario completo").any { it in lowerQuery }

        return buildString {
            when {
                SueContextKeywords.containsNextClassKeywords(lowerQuery) -> {
                    appendLine(formatResult(scheduleTools.getNextClass()))
                }
                isAskingPurelyAboutCancellations -> {
                    appendCancelledClassesContext(this, lowerQuery, day, studentName)
                }
                SueContextKeywords.containsFreeSlotWithDayKeywords(lowerQuery) -> {
                    val targetDay = day ?: dateTimeProvider.getNow().dayOfWeek.value
                    appendLine(formatResult(scheduleTools.getScheduleForDay(targetDay, entityExtractor.extractTimeOfDayFilter(lowerQuery))))
                    appendLine(formatResult(scheduleTools.getFreeSlots(workingStart, workingEnd, targetDay)))
                    val exceptionsCtx = scheduleTools.getCancelledClassesDescription(targetDay)
                    if (exceptionsCtx.isNotBlank()) {
                        appendLine(exceptionsCtx)
                    }
                }
                SueContextKeywords.containsFreeSlotKeywords(lowerQuery) -> {
                    appendLine(formatResult(scheduleTools.getFreeSlots(workingStart, workingEnd)))
                    val exceptionsCtx = scheduleTools.getCancelledClassesDescription()
                    if (exceptionsCtx.isNotBlank()) {
                        appendLine(exceptionsCtx)
                    }
                }
                else -> {
                    appendDailyOrWeeklyScheduleContext(this, lowerQuery, day, workingStart, workingEnd)
                }
            }
        }
    }

    private suspend fun appendCancelledClassesContext(
        builder: StringBuilder,
        lowerQuery: String,
        day: Int?,
        studentName: String?
    ) {
        val typeFilter = when {
            listOf("extra", "extras", "adicional", "adicionales").any { it in lowerQuery } -> ExceptionType.EXTRA
            listOf("reprogramada", "reprogramadas", "reprogramado", "reprogramados", "movida", "movidas").any { it in lowerQuery } -> ExceptionType.RESCHEDULED
            listOf("cancelada", "canceladas", "cancelado", "cancelados", "anulada", "anuladas", "anulado", "anulados").any { it in lowerQuery } -> ExceptionType.CANCELLED
            else -> null
        }
        val exceptionsCtx = scheduleTools.getCancelledClassesDescription(day, studentName, typeFilter)
        if (exceptionsCtx.isNotBlank()) {
            builder.appendLine(exceptionsCtx)
        } else {
            val typeLabel = when (typeFilter) {
                ExceptionType.EXTRA -> "clases extra registradas"
                ExceptionType.RESCHEDULED -> "clases reprogramadas registradas"
                ExceptionType.CANCELLED -> "clases canceladas registradas"
                null -> "clases canceladas, reprogramadas ni extra registradas"
            }
            val dayLabel = if (day != null) {
                when (DayOfWeek.of(day)) {
                    DayOfWeek.MONDAY -> "el lunes"
                    DayOfWeek.TUESDAY -> "el martes"
                    DayOfWeek.WEDNESDAY -> "el miércoles"
                    DayOfWeek.THURSDAY -> "el jueves"
                    DayOfWeek.FRIDAY -> "el viernes"
                    DayOfWeek.SATURDAY -> "el sábado"
                    DayOfWeek.SUNDAY -> "el domingo"
                }
            } else if (studentName != null) {
                "con $studentName"
            } else "esta semana"
            builder.appendLine("No hay $typeLabel $dayLabel.")
        }
    }

    private suspend fun appendDailyOrWeeklyScheduleContext(
        builder: StringBuilder,
        lowerQuery: String,
        day: Int?,
        workingStart: String,
        workingEnd: String
    ) {
        val daysToInject = mutableSetOf<Int>()
        val isGeneralWeeklyQuery = listOf("semana", "semanal", "horario", "agenda", "calendario", "mis clases", "que clases tengo").any { it in lowerQuery }
        if (isGeneralWeeklyQuery && day == null) {
            daysToInject.addAll(DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value)
        } else {
            daysToInject.add(dateTimeProvider.getNow().dayOfWeek.value)
            if (day != null) daysToInject.add(day)
            conversationContext.lastMentionedDayOfWeek?.let { daysToInject.add(it) }
            val extractedDays = entityExtractor.extractTwoDaysOfWeek(lowerQuery)
            extractedDays.first?.let { daysToInject.add(it) }
            extractedDays.second?.let { daysToInject.add(it) }
        }

        val timeFilter = entityExtractor.extractTime(lowerQuery) ?: entityExtractor.extractTimeOfDayFilter(lowerQuery)
        if (day == null && timeFilter != null && timeFilter.contains(":")) {
            val lookupName = conversationContext.lastMentionedStudentName
            if (lookupName != null) {
                val studentSchedules = scheduleTools.getSchedulesByStudentName(lookupName)
                studentSchedules.firstOrNull { it.startTime == timeFilter }?.let {
                    daysToInject.add(it.dayOfWeek)
                }
            }
        }

        for (d in daysToInject.sorted()) {
            builder.appendLine(formatResult(scheduleTools.getScheduleForDay(d, timeFilter)))
        }
        val exceptionsCtx = scheduleTools.getCancelledClassesDescription(day)
        if (exceptionsCtx.isNotBlank()) {
            builder.appendLine(exceptionsCtx)
        }
        if (listOf("libre", "libres", "hueco", "huecos", "despejado", "despejada", "disponible").any { it in lowerQuery }) {
            builder.appendLine(formatResult(scheduleTools.getFreeSlots(workingStart, workingEnd, day)))
        }
    }

    private fun formatResult(result: SueOperationResult): String = SueResponseFormatter.format(result)
}
