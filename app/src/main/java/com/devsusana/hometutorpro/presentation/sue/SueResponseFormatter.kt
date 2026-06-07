package com.devsusana.hometutorpro.presentation.sue

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Maps [SueOperationResult] domain objects to localized, human-friendly strings
 * suitable for display and TTS output.
 *
 * This class is the only place where Spanish presentation strings exist.
 * The domain/data layers remain language-agnostic and return structured data.
 */
object SueResponseFormatter {

    private val DATE_FORMATTER_ES = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))

    private val DAY_NAMES_ES = mapOf(
        1 to "lunes", 2 to "martes", 3 to "miércoles",
        4 to "jueves", 5 to "viernes", 6 to "sábado", 7 to "domingo"
    )

    /**
     * Converts any [SueOperationResult] into a displayable/speakable string.
     */
    fun format(result: SueOperationResult): String = when (result) {
        is SueOperationResult.WeeklySchedule -> formatWeeklySchedule(result)
        is SueOperationResult.DaySchedule -> formatDaySchedule(result)
        is SueOperationResult.NextClass -> formatNextClass(result)
        is SueOperationResult.FreeSlots -> formatFreeSlots(result)
        is SueOperationResult.AllStudentsSummary -> formatAllStudents(result)
        is SueOperationResult.StudentDetails -> formatStudentDetails(result)
        is SueOperationResult.StudentsWithBalance -> formatStudentsWithBalance(result)
        is SueOperationResult.ActiveStudentCount -> "Actualmente tienes ${result.count} alumnos activos."
        is SueOperationResult.Prepare.Success -> formatPreparationConfirmation(result.action)
        is SueOperationResult.Prepare.Error -> formatPreparationError(result)
        is SueOperationResult.Execute.Success -> formatExecutionSuccess(result.action)
        is SueOperationResult.Execute.Error -> formatDomainError(result.domainError)
        is SueOperationResult.Execute.AuthError -> "No hay sesión activa. Por favor, vuelve a iniciar sesión."
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Read results
    // ──────────────────────────────────────────────────────────────────────────

    private fun formatWeeklySchedule(result: SueOperationResult.WeeklySchedule): String {
        if (result.schedules.isEmpty()) return "No tienes ninguna clase programada esta semana."
        val byDay = result.schedules.groupBy { it.dayOfWeek }
        return buildString {
            appendLine("Horario semanal:")
            for (day in 1..7) {
                val daySchedules = byDay[day] ?: continue
                val dayName = dayName(day)
                appendLine("\n$dayName:")
                daySchedules.forEach { s -> appendLine("  ${s.startTime}–${s.endTime}: ${s.studentName}") }
            }
        }
    }

    private fun formatDaySchedule(result: SueOperationResult.DaySchedule): String {
        val dayName = dayName(result.dayOfWeek)
        val slotLabel = when {
            result.timeFilter == "morning" -> " (mañana)"
            result.timeFilter == "afternoon" -> " (tarde)"
            result.timeFilter != null && result.timeFilter.contains(":") -> " a las ${result.timeFilter}"
            else -> ""
        }
        if (result.schedules.isEmpty()) {
            return if (result.timeFilter != null) "El $dayName$slotLabel no tienes ninguna clase programada."
            else "El $dayName no tienes ninguna clase programada."
        }
        return buildString {
            appendLine("$dayName$slotLabel (${result.schedules.size} clases):")
            result.schedules.forEach { s -> appendLine("  ${s.startTime}–${s.endTime}: ${s.studentName}") }
        }
    }

    private fun formatNextClass(result: SueOperationResult.NextClass): String {
        val schedule = result.schedule ?: return "No tienes ninguna clase programada."
        val date = result.occurrenceDate ?: return "No se encontró ninguna clase próxima."
        val dayLabel = date.format(DATE_FORMATTER_ES)
        return "Tu próxima clase es con ${schedule.studentName} el $dayLabel de ${schedule.startTime} a ${schedule.endTime}."
    }

    private fun formatFreeSlots(result: SueOperationResult.FreeSlots): String {
        if (result.freeDays.isEmpty()) {
            return "Tienes clases programadas de lunes a viernes. No hay días completamente libres entre semana."
        }
        return buildString {
            appendLine("Días libres esta semana (sin clases programadas):")
            result.freeDays.forEach { day -> appendLine("  • ${dayName(day)}") }
        }
    }

    private fun formatAllStudents(result: SueOperationResult.AllStudentsSummary): String {
        if (result.students.isEmpty()) return "No se encontraron alumnos."
        return buildString {
            appendLine("Alumnos (${result.students.size} en total):")
            appendLine()
            result.students.forEach { student ->
                val status = if (student.isActive) "Activo" else "Inactivo"
                appendLine("• ${student.name}")
                appendLine("  Asignaturas: ${student.subjects}")
                appendLine("  Curso: ${student.course}")
                appendLine("  Precio: ${student.pricePerHour} euros la hora")
                appendLine("  Saldo pendiente: ${student.pendingBalance} euros")
                if (student.lastPaymentDate != null) {
                    val date = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(java.util.Date(student.lastPaymentDate))
                    appendLine("  Último pago: $date")
                } else {
                    appendLine("  Último pago: Nunca")
                }
                appendLine("  Estado: $status")
                appendLine()
            }
        }
    }

    private fun formatStudentDetails(result: SueOperationResult.StudentDetails): String {
        if (result.students.isEmpty()) return "No se encontraron alumnos con el nombre '${result.query}'."
        return buildString {
            val header = if (result.students.size == 1) result.students.first().name else "'${result.query}'"
            appendLine("Información de $header (${result.students.size} encontrado${if (result.students.size != 1) "s" else ""}):")
            appendLine()
            result.students.forEach { student ->
                appendLine("• ${student.name}")
                appendLine("  Asignaturas: ${student.subjects}")
                appendLine("  Curso: ${student.course}")
                appendLine("  Saldo pendiente: ${student.pendingBalance} euros")
                if (student.lastPaymentDate != null) {
                    val date = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(java.util.Date(student.lastPaymentDate))
                    appendLine("  Último pago: $date")
                } else {
                    appendLine("  Último pago: Nunca")
                }
                appendLine()
            }
        }
    }

    private fun formatStudentsWithBalance(result: SueOperationResult.StudentsWithBalance): String {
        if (result.students.isEmpty()) return "Ningún alumno tiene saldo pendiente."
        return buildString {
            appendLine("Alumnos con saldo pendiente (${result.students.size}):")
            result.students.forEach { s -> appendLine("- ${s.name}: ${s.pendingBalance} euros") }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Confirmation / execution messages
    // ──────────────────────────────────────────────────────────────────────────

    private fun formatPreparationConfirmation(action: SuePendingAction): String = when (action) {
        is SuePendingAction.CancelClass -> {
            val dayLabel = epochMillisToLabel(action.date)
            "¿Confirmas que quieres cancelar la clase de ${action.studentName} " +
                "el $dayLabel de ${action.startTime} a ${action.endTime}? " +
                "Di sí para confirmar o no para cancelar."
        }
        is SuePendingAction.RescheduleClass -> {
            val originalLabel = epochMillisToLabel(action.originalDate)
            val targetLabel = epochMillisToLabel(action.newDate)
            "¿Confirmas mover la clase de ${action.studentName} del $originalLabel " +
                "al $targetLabel de ${action.newStartTime} a ${action.newEndTime}? " +
                "Di sí para confirmar o no para cancelar."
        }
        is SuePendingAction.RegisterPayment -> {
            val typeStr = if (action.paymentType.name == "BIZUM") "por Bizum" else "en efectivo"
            "¿Confirmas registrar un pago de ${action.amount} euros de ${action.studentName} $typeStr? " +
                "Di sí para confirmar o no para cancelar."
        }
        is SuePendingAction.AddBalance -> {
            "¿Confirmas sumar ${action.amount} euros a la deuda de ${action.studentName}? " +
                "Di sí para confirmar o no para cancelar."
        }
        is SuePendingAction.StartClass -> {
            "¿Confirmas iniciar una clase para ${action.studentName} de ${action.durationMinutes} minutos? " +
                "Di sí para confirmar o no para cancelar."
        }
        is SuePendingAction.CreateStudent -> {
            "¿Confirmas crear el alumno ${action.name} de ${action.subjects} a ${action.pricePerHour} euros la hora? " +
                "Di sí para confirmar o no para cancelar."
        }
        is SuePendingAction.DeleteStudent -> {
            "¿Confirmas eliminar al alumno ${action.studentName} y toda su información de forma permanente? " +
                "Di sí para confirmar o no para cancelar."
        }
        is SuePendingAction.CreateSchedule -> {
            val day = dayName(action.dayOfWeek)
            "¿Confirmas programar un horario permanente para ${action.studentName} los $day de ${action.startTime} a ${action.endTime}? " +
                "Di sí para confirmar o no para cancelar."
        }
        is SuePendingAction.DeleteSchedule -> {
            val day = dayName(action.dayOfWeek)
            "¿Confirmas eliminar el horario permanente de ${action.studentName} los $day a las ${action.startTime}? " +
                "Di sí para confirmar o no para cancelar."
        }
        is SuePendingAction.AddExtraClass -> {
            val dayLabel = epochMillisToLabel(action.date)
            "¿Confirmas añadir una clase extra para ${action.studentName} el $dayLabel de ${action.startTime} a ${action.endTime}? " +
                "Di sí para confirmar o no para cancelar."
        }
    }

    private fun formatPreparationError(result: SueOperationResult.Prepare.Error): String {
        if (result.details != null) return result.details
        return when (result.errorType) {
            SueOperationResult.ErrorType.STUDENT_NOT_FOUND ->
                "No he encontrado ningún alumno para realizar la acción."
            SueOperationResult.ErrorType.CLASS_NOT_FOUND ->
                "No he encontrado ninguna clase para ese alumno ese día. Comprueba el nombre y el día."
            SueOperationResult.ErrorType.AUTH_ERROR ->
                "No hay sesión activa. Por favor, vuelve a iniciar sesión."
            SueOperationResult.ErrorType.UNKNOWN ->
                "No se pudo preparar la acción. Inténtalo de nuevo."
        }
    }

    private fun formatExecutionSuccess(action: SuePendingAction): String = when (action) {
        is SuePendingAction.CancelClass -> {
            val dayLabel = epochMillisToLabel(action.date)
            "Hecho. La clase de ${action.studentName} del $dayLabel de " +
                "${action.startTime} a ${action.endTime} ha sido cancelada. " +
                "El alumno sigue teniendo su hora habitual la semana siguiente."
        }
        is SuePendingAction.RescheduleClass -> {
            val originalLabel = epochMillisToLabel(action.originalDate)
            val targetLabel = epochMillisToLabel(action.newDate)
            "Hecho. La clase de ${action.studentName} se ha movido del " +
                "$originalLabel al $targetLabel de ${action.newStartTime} a ${action.newEndTime}."
        }
        is SuePendingAction.RegisterPayment ->
            "Hecho. Se ha registrado el pago de ${action.amount} euros de ${action.studentName}."
        is SuePendingAction.AddBalance ->
            "Hecho. Se han sumado ${action.amount} euros a la cuenta de ${action.studentName}."
        is SuePendingAction.StartClass ->
            "Hecho. Se ha iniciado la clase para ${action.studentName} de ${action.durationMinutes} minutos."
        is SuePendingAction.CreateStudent ->
            "Hecho. Se ha creado el alumno ${action.name} con tarifa de ${action.pricePerHour} euros la hora."
        is SuePendingAction.DeleteStudent ->
            "Hecho. Se ha eliminado al alumno ${action.studentName}."
        is SuePendingAction.CreateSchedule -> {
            val day = dayName(action.dayOfWeek)
            "Hecho. Se ha configurado el horario permanente para ${action.studentName} los $day de ${action.startTime} a ${action.endTime}."
        }
        is SuePendingAction.DeleteSchedule -> {
            val day = dayName(action.dayOfWeek)
            "Hecho. Se ha eliminado el horario de los $day a las ${action.startTime} de ${action.studentName}."
        }
        is SuePendingAction.AddExtraClass -> {
            val dayLabel = epochMillisToLabel(action.date)
            "Hecho. Se ha agendado la clase extra para ${action.studentName} el $dayLabel de ${action.startTime} a ${action.endTime}."
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun epochMillisToLabel(epochMillis: Long): String =
        java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .format(DATE_FORMATTER_ES)

    private fun dayName(isoDay: Int): String =
        DAY_NAMES_ES[isoDay]?.replaceFirstChar { it.uppercase() } ?: "Día $isoDay"

    fun formatFreeDaysList(freeDays: List<Int>): String {
        if (freeDays.isEmpty()) return ""
        val names = freeDays.map { dayName(it).lowercase() }
        return "Los siguientes días están completamente libres esta semana: ${names.joinToString(", ")}."
    }

    fun formatDomainError(error: DomainError, freeSlotsText: String = ""): String = when (error) {
        is DomainError.StudentNotFound -> "No se encontró el alumno en la base de datos."
        is DomainError.ScheduleConflict -> "Hay un conflicto con el horario seleccionado."
        is DomainError.ConflictingStudent -> {
            val base = "No se puede realizar la acción debido a un conflicto de horario. El hueco está ocupado por ${error.studentName} de ${error.time}."
            if (freeSlotsText.isNotEmpty()) "$base $freeSlotsText" else base
        }
        is DomainError.InvalidAmount -> "El importe introducido no es válido."
        is DomainError.NetworkError -> "Error de red. Comprueba tu conexión."
        else -> "No se pudo completar la operación. Inténtalo de nuevo."
    }

    /**
     * Formats an [AgentScheduleSummary] into plain text for LLM prompt injection.
     */
    fun scheduleToContextLine(s: AgentScheduleSummary): String =
        "${dayName(s.dayOfWeek)}: ${s.startTime}–${s.endTime} (${s.studentName})"
}
