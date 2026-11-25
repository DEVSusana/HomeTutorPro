package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gathers student, finance, class-log, and shared-resource context blocks for [SueContextBuilder].
 */
internal class SueStudentContextGatherer(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools,
    private val conversationContext: SueConversationContext
) {

    /**
     * Appends finance, global history, debt, resource, and active-student context blocks.
     */
    suspend fun appendStudentContext(builder: StringBuilder, lowerQuery: String) {
        val hasSchedule = SueContextKeywords.containsScheduleKeywords(lowerQuery)

        if (isFinanceQuery(lowerQuery)) {
            appendFinanceContext(builder)
        }
        if (isGlobalClassHistoryQuery(lowerQuery) && conversationContext.lastMentionedStudentName == null) {
            appendGlobalClassHistoryContext(builder)
        }
        if (isGlobalDebtQuery(lowerQuery) && conversationContext.lastMentionedStudentName == null) {
            appendGlobalDebtContext(builder)
        }
        if (isResourceQuery(lowerQuery) && conversationContext.lastMentionedStudentName == null) {
            appendGlobalResourceContext(builder)
        }

        when {
            conversationContext.lastMentionedStudentName != null -> {
                appendActiveStudentContext(builder, conversationContext.lastMentionedStudentName!!)
            }
            SueContextKeywords.containsCountKeywords(lowerQuery) -> {
                builder.appendLine(formatResult(studentTools.getActiveStudentCount()))
            }
            SueContextKeywords.containsBalanceKeywords(lowerQuery) -> {
                builder.appendLine(formatResult(studentTools.getStudentsWithBalance()))
            }
            SueContextKeywords.containsAllStudentsKeywords(lowerQuery) && !hasSchedule -> {
                builder.appendLine(formatResult(studentTools.getAllStudentsSummary()))
            }
        }
    }

    private fun isFinanceQuery(lowerQuery: String) = listOf(
        "ganado", "ingresos", "facturado", "facturacion", "facturación",
        "cobrado", "recibido", "earnings", "income", "caja",
        "cuanto he ganado", "cuánto he ganado", "cuánto llevo", "cuanto llevo",
        "cuánto he cobrado", "cuanto he cobrado", "total de pagos", "pagos del mes"
    ).any { it in lowerQuery }

    private fun isGlobalClassHistoryQuery(lowerQuery: String) = listOf(
        "cuántas clases he dado", "cuantas clases he dado",
        "total de clases", "clases del mes", "clases este mes",
        "cuántas he impartido", "cuantas he impartido",
        "histórico de clases", "historico de clases",
        "cuántas clases llevo", "cuantas clases llevo"
    ).any { it in lowerQuery }

    private fun isGlobalDebtQuery(lowerQuery: String) = listOf(
        "no han pagado", "no ha pagado", "sin pagar",
        "pendiente de pago", "deudas pendientes", "quién debe",
        "quien debe", "who owes"
    ).any { it in lowerQuery }

    private fun isResourceQuery(lowerQuery: String) = listOf(
        "material", "materiales", "recurso", "recursos", "archivo", "archivos",
        "documento", "documentos", "pdf", "enviado", "compartido", "compartir",
        "resource", "resources", "shared"
    ).any { it in lowerQuery }

    private suspend fun appendFinanceContext(builder: StringBuilder) {
        val allTxs = studentTools.getAllTransactions()
        val studentsResult = studentTools.getAllStudentsSummary()
        val students = (studentsResult as? SueOperationResult.AllStudentsSummary)?.students ?: emptyList()
        val schedules = scheduleTools.getScheduleDetails()
        val logs = studentTools.getAllClassLogs()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        builder.appendLine("--- HISTORIAL GENERAL DE TRANSACCIONES / INGRESOS ---")
        if (allTxs.isNotEmpty()) {
            val totalEarnings = allTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
            builder.appendLine("Total cobrado (pagos recibidos): $totalEarnings euros")
            allTxs.take(15).forEach { t ->
                val dateStr = sdf.format(Date(t.timestamp))
                val typeStr = if (t.type == "PAYMENT") "PAGO" else "DEUDA"
                val payTypeStr = t.paymentType?.let { " ($it)" } ?: ""
                val sName = schedules.firstOrNull { it.studentId == t.studentId }?.studentName ?: "Alumno"
                builder.appendLine("- $dateStr: $typeStr de ${t.amount} euros$payTypeStr de $sName")
            }
        } else {
            builder.appendLine("Total cobrado (pagos recibidos): 0 euros")
            builder.appendLine("No se han registrado pagos o facturas todavía en la aplicación (el balance es cero).")
        }
        builder.appendLine("--- FIN HISTORIAL GENERAL ---")

        val balanceResult = studentTools.getStudentsWithBalance()
        if (balanceResult is SueOperationResult.StudentsWithBalance && balanceResult.students.isNotEmpty()) {
            builder.appendLine("--- DEUDAS Y SALDOS PENDIENTES DE ALUMNOS (FACTURACIÓN POR COBRAR) ---")
            balanceResult.students.forEach { s ->
                val prefix = if (s.pendingBalance < 0.0) "saldo a favor de" else "debe"
                builder.appendLine("- ${s.name}: ${"%.2f".format(Math.abs(s.pendingBalance))} euros ($prefix)")
            }
            builder.appendLine("--- FIN DEUDAS Y SALDOS ---")
        }

        builder.appendLine("--- HISTORIAL DE CLASES IMPARTIDAS (COMPLETADAS) ---")
        if (logs.isNotEmpty()) {
            logs.take(20).forEach { l ->
                val dateStr = sdf.format(Date(l.date))
                val sName = schedules.firstOrNull { it.studentId == l.studentId }?.studentName ?: "Alumno"
                val studentPrice = students.firstOrNull { it.name.lowercase() == sName.lowercase() }?.pricePerHour ?: 0.0
                builder.appendLine("- $dateStr: Clase impartida a $sName de ${l.startTime} a ${l.endTime} (tarifa habitual: $studentPrice €/hora)")
            }
        } else {
            builder.appendLine("No hay registro de clases completadas o impartidas este mes.")
        }
        builder.appendLine("--- FIN HISTORIAL CLASES ---")
    }

    private suspend fun appendGlobalClassHistoryContext(builder: StringBuilder) {
        val allLogs = studentTools.getAllClassLogs()
        builder.appendLine("--- HISTORIAL GLOBAL DE CLASES IMPARTIDAS ---")
        if (allLogs.isNotEmpty()) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            builder.appendLine("Total de clases registradas: ${allLogs.size}")
            allLogs.take(20).forEach { l ->
                builder.appendLine("- ${sdf.format(Date(l.date))}: clase de ${l.startTime} a ${l.endTime}")
            }
        } else {
            builder.appendLine("Total de clases registradas: 0")
            builder.appendLine("No hay registros de asistencia ni clases finalizadas en el historial.")
        }
        builder.appendLine("--- FIN HISTORIAL GLOBAL ---")
    }

    private suspend fun appendGlobalDebtContext(builder: StringBuilder) {
        val balanceResult = studentTools.getStudentsWithBalance()
        builder.appendLine("--- ALUMNOS CON SALDO PENDIENTE ---")
        if (balanceResult is SueOperationResult.StudentsWithBalance && balanceResult.students.isNotEmpty()) {
            balanceResult.students.forEach { s ->
                builder.appendLine("- ${s.name}: ${"%.2f".format(s.pendingBalance)} € pendientes")
            }
        } else {
            builder.appendLine("Todos los alumnos están al corriente de pago. No hay deudas pendientes en este momento.")
        }
        builder.appendLine("--- FIN SALDOS PENDIENTES ---")
    }

    private suspend fun appendGlobalResourceContext(builder: StringBuilder) {
        val allResources = studentTools.getAllSharedResources()
        val schedules = scheduleTools.getScheduleDetails()
        val studentMap = schedules.associate { it.studentId to it.studentName }
        val groupedByStudent = allResources.groupBy { it.studentId }
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        builder.appendLine("--- RECURSOS / MATERIALES COMPARTIDOS CON ALUMNOS ---")
        if (allResources.isNotEmpty()) {
            builder.appendLine("Total de recursos compartidos: ${allResources.size} entre ${groupedByStudent.size} alumnos.")
            if (allResources.size <= 15) {
                allResources.sortedByDescending { it.sharedAt }.forEach { r ->
                    val sName = studentMap[r.studentId] ?: "Alumno"
                    builder.appendLine("- Alumno: $sName | Archivo: ${r.fileName} (${r.fileType}) compartido el ${sdf.format(Date(r.sharedAt))} vía ${r.sharedVia}")
                }
            } else {
                builder.appendLine("Desglose y resumen por alumno:")
                groupedByStudent.entries.take(20).forEach { (studentId, resList) ->
                    val sName = studentMap[studentId] ?: "Alumno"
                    val latest = resList.maxBy { it.sharedAt }
                    builder.appendLine("- $sName: ${resList.size} archivo(s) compartido(s) (el más reciente: '${latest.fileName}', ${sdf.format(Date(latest.sharedAt))})")
                }
                if (groupedByStudent.size > 20) {
                    builder.appendLine("(Se muestran 20 alumnos con actividad más reciente de un total de ${groupedByStudent.size})")
                }
                builder.appendLine("Instrucción: Ofrece al profesor un resumen conciso por alumno y recuérdale que puede consultar el detalle completo de un alumno específico.")
            }
        } else {
            builder.appendLine("No se han compartido materiales, recursos ni archivos con ningún alumno todavía.")
        }
        builder.appendLine("--- FIN RECURSOS / MATERIALES COMPARTIDOS ---")
    }

    private suspend fun appendActiveStudentContext(builder: StringBuilder, currentName: String) {
        builder.appendLine(formatResult(studentTools.searchStudent(currentName)))
        val studentSchedules = scheduleTools.getSchedulesByStudentName(currentName)
        if (studentSchedules.isNotEmpty()) {
            builder.appendLine("--- WEEKLY CLASSES FOR $currentName ---")
            studentSchedules.forEach { s ->
                val summary = AgentScheduleSummary(s.studentName, s.dayOfWeek, s.startTime, s.endTime)
                builder.appendLine(formatResult(SueOperationResult.DaySchedule(s.dayOfWeek, null, listOf(summary))))
            }
            builder.appendLine("--- END OF WEEKLY CLASSES ---")
        }

        val resources = studentTools.getSharedResources(currentName)
        if (resources.isNotEmpty()) {
            builder.appendLine("--- RECURSOS COMPARTIDOS CON $currentName ---")
            resources.sortedByDescending { it.sharedAt }.take(20).forEach { r ->
                builder.appendLine("- ${r.fileName} (${r.fileType}) compartido vía ${r.sharedVia}")
            }
            if (resources.size > 20) builder.appendLine("(Se muestran los 20 recursos más recientes)")
            builder.appendLine("--- FIN RECURSOS COMPARTIDOS ---")
        }

        val logs = studentTools.getClassLogs(currentName)
        if (logs.isNotEmpty()) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            builder.appendLine("--- CLASES COMPLETADAS PARA $currentName ---")
            builder.appendLine("Total de clases dadas: ${logs.size}")
            logs.forEach { l ->
                builder.appendLine("- Clase del ${sdf.format(Date(l.date))} a las ${l.startTime} (Duración: de ${l.startTime} a ${l.endTime})")
            }
            builder.appendLine("--- FIN CLASES COMPLETADAS ---")
        }

        val txs = studentTools.getTransactions(currentName)
        if (txs.isNotEmpty()) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            builder.appendLine("--- TRANSACCIONES / PAGOS DE $currentName ---")
            txs.forEach { t ->
                val typeStr = if (t.type == "PAYMENT") "PAGO RECIBIDO" else "DEUDA SUMADA"
                val payTypeStr = t.paymentType?.let { " ($it)" } ?: ""
                builder.appendLine("- ${sdf.format(Date(t.timestamp))}: $typeStr de ${t.amount} euros$payTypeStr")
            }
            builder.appendLine("--- FIN TRANSACCIONES ---")
        }
    }

    private fun formatResult(result: SueOperationResult): String = SueResponseFormatter.format(result)
}
