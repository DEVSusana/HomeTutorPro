package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.core.utils.SafeLogger
import com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary
import com.devsusana.hometutorpro.domain.entities.ExceptionType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import com.devsusana.hometutorpro.presentation.sue.SueResponseFormatter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds localized LLM system prompts and gathers RAG context from domain tools.
 */
@Singleton
class SueContextBuilder @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools,
    private val dateTimeProvider: DateTimeProvider,
    private val authRepository: AuthRepository,
    private val entityExtractor: SueEntityExtractor,
    private val conversationContext: SueConversationContext
) {

    companion object {
        /**
         * Builds the Sue system prompt with a locale-aware language instruction.
         */
        fun buildSystemPrompt(locale: Locale, isQuestion: Boolean = false): String {
            val languageTag = locale.toLanguageTag()
            val languageName = locale.getDisplayLanguage(Locale.ENGLISH)
            val countryName = if (locale.country.isNotEmpty()) {
                locale.getDisplayCountry(Locale.ENGLISH)
            } else null

            val languageInstruction = if (countryName != null) {
                "Always respond in $languageName as spoken in $countryName ($languageTag). " +
                "Use vocabulary, spelling and expressions appropriate for that region."
            } else {
                "Always respond in $languageName ($languageTag)."
            }

            if (isQuestion) {
                return """
                    Eres Sue, la asistente inteligente de HomeTutorPro.
                    Tu rol es responder a las preguntas del profesor de forma breve, amable y concisa.
                    NUNCA inventes datos. Usa SOLO la información provista en la sección --- AVAILABLE DATA ---.
                    Si el usuario pregunta por clases de hoy, canceladas, alumnos, finanzas o recursos y materiales compartidos, lee la sección correspondiente en --- AVAILABLE DATA --- y responde con claridad y precisión.
                    Si no hay datos en esa sección o la sección indica que no hay registros, dile al profesor con amabilidad que no tienes información registrada sobre eso.
                    NUNCA llames "Sue" al profesor (tú eres Sue; él es el usuario o profesor).
                    $languageInstruction
                """.trimIndent()
            }

            return """
                Eres Sue, la asistente inteligente de HomeTutorPro. Tu rol es ayudar a profesores particulares a gestionar su trabajo de forma eficiente.

                Tu personalidad:
                - Eres profesional, amable y concisa.
                - Evitas respuestas largas — sé directa y útil.
                - Si no puedes ayudar con algo, dilo claramente.
                - NO te describes a ti misma como IA. Compórtate como asistente de forma natural.
                - Tú eres Sue. Puedes referirte a ti misma como Sue si es natural, pero NUNCA te dirijas al profesor como "Sue".
                - $languageInstruction

                Tus capacidades:
                - Consultar la lista de alumnos (nombres, asignaturas, cursos, precios, saldos, notas).
                - Buscar alumnos específicos por nombre.
                - Consultar los horarios semanales y la próxima clase.
                - Informar sobre saldos pendientes.
                - Contar alumnos activos.
                - Cancelar o mover clases puntuales (solo la ocurrencia indicada).
                - Mostrar huecos libres en la semana actual.
                - Crear o eliminar perfiles de alumnos.
                - Configurar o eliminar horarios recurrentes permanentes de clases.
                - Programar clases extra y registrar el inicio de clases en directo.
                - Guardar, añadir o actualizar notas y observaciones sobre los alumnos.

                Limitaciones:
                - Solo conoces los datos del profesor que está usando la app.

                INSTRUCCIÓN HORARIA:
                Para horas sin especificar (ej. "a las 3", "a las 4", "a las 5", "a las 6"), interpreta SIEMPRE que se refiere a la tarde en formato 24h (15:00, 16:00, 17:00, 18:00) y NUNCA a horas de madrugada (03:00, 04:00, etc.), ya que las clases particulares no se imparten de madrugada. Solo interpreta mañana para horas entre 8 y 12 (08:00 - 12:00) o si se especifica "de la mañana" / "am".

                INSTRUCCIÓN MUY IMPORTANTE (EXTRACCIÓN DE INTENCIONES):
                Si la frase del usuario requiere ejecutar una acción en la app (crear, modificar o borrar clases, estudiantes o pagos, o actualizar sus notas), debes devolver UNA ÚNICA LÍNEA AL PRINCIPIO de tu respuesta con el siguiente formato exacto:
                [ACTION: TIPO_DE_ACCION, parametro1: valor, parametro2: valor]
                
                Tipos de acción soportados: START_CLASS, CREATE_STUDENT, DELETE_STUDENT, ADD_EXTRA_CLASS, CREATE_SCHEDULE, DELETE_SCHEDULE, CANCEL_CLASS, RESCHEDULE_CLASS, REGISTER_PAYMENT, ADD_BALANCE, UPDATE_STUDENT_NOTES
                
                Ejemplos de acciones:
                Usuario: "Añade a Marcos para dar clases de inglés a 15 la hora."
                Tú: [ACTION: CREATE_STUDENT, student: "Marcos", course: "General", subjects: "inglés", price: "15.0"]
                
                Usuario: "Añade un alumno nuevo llamado Pepe los miércoles a las 3 de la tarde."
                Tú: [ACTION: CREATE_STUDENT, student: "Pepe"]

                Usuario: "añade un horario recurrente para Carlos los lunes a las 17:00."
                Tú: [ACTION: CREATE_SCHEDULE, student: "Carlos", day: "lunes", startTime: "17:00", endTime: "18:00"]
                
                Usuario: "Cambia la clase de Ana de hoy a mañana a las 5."
                Tú: [ACTION: RESCHEDULE_CLASS, student: "Ana", fromDay: "hoy", toDay: "mañana", targetTime: "17:00"]
                
                Usuario: "Mueve la clase de Luis a las 18:00."
                Tú: [ACTION: RESCHEDULE_CLASS, student: "Luis", targetTime: "18:00"]
                
                Usuario: "Cancela la clase de María del viernes."
                Tú: [ACTION: CANCEL_CLASS, student: "María", day: "viernes"]
                
                Usuario: "Ana me ha pagado 30 euros en efectivo."
                Tú: [ACTION: REGISTER_PAYMENT, student: "Ana", amount: "30.0", type: "EFFECTIVE"]
                
                Usuario: "Ponle una clase extra a Carlos el viernes a las 10."
                Tú: [ACTION: ADD_EXTRA_CLASS, student: "Carlos", day: "viernes", targetTime: "10:00"]

                Usuario: "pon una nota en Carlos que diga que tiene que estudiar verbos."
                Tú: [ACTION: UPDATE_STUDENT_NOTES, student: "Carlos", notes: "tiene que estudiar verbos"]
                
                Si la frase es solo una pregunta (ej. "¿Qué clases tengo?"), responde de forma natural SIN incluir la etiqueta [ACTION: ...].
            """.trimIndent()
        }
    }

    suspend fun buildPromptWithContext(
        userQuery: String,
        history: List<Pair<String, String>>
    ): String {
        val locale = dateTimeProvider.getLocale()
        val isQuestion = entityExtractor.isQuestionOrQuery(userQuery)

        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, HH:mm", locale)
        val currentDateTime = dateTimeProvider.getNow().format(formatter)

        val firebaseUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }

        val professor = authRepository.currentUser.value
            ?: withTimeoutOrNull(2000L) {
                authRepository.currentUser.first { it != null }
            }
        val workingStart = professor?.workingStartTime?.takeIf { it.isNotBlank() } ?: "08:00"
        val workingEnd = professor?.workingEndTime?.takeIf { it.isNotBlank() } ?: "23:00"

        val toolContext = gatherRelevantContext(userQuery, workingStart, workingEnd)

        SafeLogger.d("SueAgentImpl", "--- SUE DEBUG LOG ---")
        SafeLogger.d("SueAgentImpl", "User Query: $userQuery")
        SafeLogger.d("SueAgentImpl", "Firebase UID: $firebaseUid | AuthRepository UID: ${professor?.uid}")
        SafeLogger.d("SueAgentImpl", "Working hours: $workingStart – $workingEnd")
        SafeLogger.d("SueAgentImpl", "RAG Context:\n$toolContext")
        SafeLogger.d("SueAgentImpl", "----------------------")

        return buildString {
            append("<start_of_turn>user\n")
            appendLine(buildSystemPrompt(locale, isQuestion))
            appendLine()
            appendLine("--- TEMPORAL CONTEXT ---")
            appendLine("Current date/time: $currentDateTime")
            appendLine("--- END OF TEMPORAL CONTEXT ---")
            appendLine()
            appendLine("--- PROFESSOR WORKING HOURS (AGENDA BOUNDARIES) ---")
            appendLine("Working hours limit: from $workingStart to $workingEnd")
            appendLine("--- END OF WORKING HOURS ---")
            appendLine()
            if (history.isNotEmpty()) {
                appendLine("--- RECENT CONVERSATION HISTORY ---")
                for ((usr, bot) in history.takeLast(3)) {
                    appendLine("User: $usr")
                    appendLine("Sue: $bot")
                }
                appendLine("--- END OF HISTORY ---")
                appendLine()
            }
            if (toolContext.isNotBlank()) {
                appendLine("--- AVAILABLE DATA ---")
                appendLine(toolContext)
                appendLine("--- END OF DATA ---")
            }
            appendLine()
            appendLine("User query: $userQuery")
            append("<end_of_turn>\n<start_of_turn>model\n")
        }
    }

    suspend fun gatherRelevantContext(
        query: String,
        workingStart: String = "08:00",
        workingEnd: String = "23:00"
    ): String {
        val lowerQuery = entityExtractor.stripAccents(query.lowercase())

        // 1. Extract and update context memory variables
        val matchedStudent = studentTools.extractRelevantStudent(lowerQuery)
        val studentName = matchedStudent?.name ?: entityExtractor.extractStudentName(lowerQuery) ?: entityExtractor.extractStudentNameForFinance(lowerQuery)
        if (studentName != null) {
            val capitalizedName = studentName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            conversationContext.lastMentionedStudentName = capitalizedName
        }

        val relativeDay = entityExtractor.extractRelativeDayOfWeek(lowerQuery)
        val explicitDay = entityExtractor.extractDayOfWeek(lowerQuery)
        val day = relativeDay ?: explicitDay
        if (day != null) {
            conversationContext.lastMentionedDayOfWeek = day
        }

        val time = entityExtractor.extractTime(lowerQuery)
        if (time != null) {
            conversationContext.lastMentionedTime = time
        }

        return buildString {
            // 1. Schedule queries
            if (containsScheduleKeywords(lowerQuery) || containsCancelledQueryKeywords(lowerQuery)) {
                val isAskingPurelyAboutCancellations = containsCancelledQueryKeywords(lowerQuery) &&
                    !listOf("activa", "activas", "no cancelada", "no canceladas", "todas las clases", "horario completo").any { it in lowerQuery }

                if (containsNextClassKeywords(lowerQuery)) {
                    appendLine(formatResult(scheduleTools.getNextClass()))
                } else if (isAskingPurelyAboutCancellations) {
                    val typeFilter = when {
                        listOf("extra", "extras", "adicional", "adicionales").any { it in lowerQuery } -> ExceptionType.EXTRA
                        listOf("reprogramada", "reprogramadas", "reprogramado", "reprogramados", "movida", "movidas").any { it in lowerQuery } -> ExceptionType.RESCHEDULED
                        listOf("cancelada", "canceladas", "cancelado", "cancelados", "anulada", "anuladas", "anulado", "anulados").any { it in lowerQuery } -> ExceptionType.CANCELLED
                        else -> null
                    }
                    val exceptionsCtx = scheduleTools.getCancelledClassesDescription(day, studentName, typeFilter)
                    if (exceptionsCtx.isNotBlank()) {
                        appendLine(exceptionsCtx)
                    } else {
                        val typeLabel = when (typeFilter) {
                            ExceptionType.EXTRA -> "clases extra registradas"
                            ExceptionType.RESCHEDULED -> "clases reprogramadas registradas"
                            ExceptionType.CANCELLED -> "clases canceladas registradas"
                            null -> "clases canceladas, reprogramadas ni extra registradas"
                        }
                        val dayLabel = if (day != null) {
                            when (day) {
                                1 -> "el lunes"; 2 -> "el martes"; 3 -> "el miércoles"
                                4 -> "el jueves"; 5 -> "el viernes"; 6 -> "el sábado"
                                else -> "el domingo"
                            }
                        } else if (studentName != null) {
                            "con $studentName"
                        } else "esta semana"
                        appendLine("No hay $typeLabel $dayLabel.")
                    }
                } else if (containsFreeSlotWithDayKeywords(lowerQuery)) {
                    val targetDay = day ?: dateTimeProvider.getNow().dayOfWeek.value
                    appendLine(formatResult(scheduleTools.getScheduleForDay(targetDay, entityExtractor.extractTimeOfDayFilter(lowerQuery))))
                    appendLine(formatResult(scheduleTools.getFreeSlots(workingStart, workingEnd, targetDay)))
                    val exceptionsCtx = scheduleTools.getCancelledClassesDescription(targetDay)
                    if (exceptionsCtx.isNotBlank()) {
                        appendLine(exceptionsCtx)
                    }
                } else if (containsFreeSlotKeywords(lowerQuery)) {
                    appendLine(formatResult(scheduleTools.getFreeSlots(workingStart, workingEnd)))
                    val exceptionsCtx = scheduleTools.getCancelledClassesDescription()
                    if (exceptionsCtx.isNotBlank()) {
                        appendLine(exceptionsCtx)
                    }
                } else {
                    val daysToInject = mutableSetOf<Int>()

                    val isGeneralWeeklyQuery = listOf("semana", "semanal", "horario", "agenda", "calendario", "mis clases", "que clases tengo").any { it in lowerQuery }
                    if (isGeneralWeeklyQuery && day == null) {
                        daysToInject.addAll(1..5)
                    } else {
                        daysToInject.add(dateTimeProvider.getNow().dayOfWeek.value)
                        if (day != null) {
                            daysToInject.add(day)
                        }
                        if (conversationContext.lastMentionedDayOfWeek != null) {
                            daysToInject.add(conversationContext.lastMentionedDayOfWeek!!)
                        }
                        val extractedDays = entityExtractor.extractTwoDaysOfWeek(lowerQuery)
                        if (extractedDays.first != null) daysToInject.add(extractedDays.first!!)
                        if (extractedDays.second != null) daysToInject.add(extractedDays.second!!)
                    }

                    val timeFilter = entityExtractor.extractTime(lowerQuery) ?: entityExtractor.extractTimeOfDayFilter(lowerQuery)
                    if (day == null && timeFilter != null && timeFilter.contains(":")) {
                        val lookupName = conversationContext.lastMentionedStudentName
                        if (lookupName != null) {
                            val studentSchedules = scheduleTools.getSchedulesByStudentName(lookupName)
                            val matchingSchedule = studentSchedules.firstOrNull { it.startTime == timeFilter }
                            if (matchingSchedule != null) {
                                daysToInject.add(matchingSchedule.dayOfWeek)
                            }
                        }
                    }

                    for (d in daysToInject.sorted()) {
                        appendLine(formatResult(scheduleTools.getScheduleForDay(d, timeFilter)))
                    }
                    val exceptionsCtx = scheduleTools.getCancelledClassesDescription(day)
                    if (exceptionsCtx.isNotBlank()) {
                        appendLine(exceptionsCtx)
                    }
                    if (listOf("libre", "libres", "hueco", "huecos", "despejado", "despejada", "disponible").any { it in lowerQuery }) {
                        appendLine(formatResult(scheduleTools.getFreeSlots(workingStart, workingEnd, day)))
                    }
                }
            }

            // 2. Student queries
            val hasSchedule = containsScheduleKeywords(lowerQuery)

            // 2a. Finance/earnings queries
            val isFinanceQuery = listOf(
                "ganado", "ingresos", "facturado", "facturacion", "facturación",
                "cobrado", "recibido", "earnings", "income", "caja",
                "cuanto he ganado", "cuánto he ganado",
                "cuánto llevo", "cuanto llevo",
                "cuánto he cobrado", "cuanto he cobrado",
                "total de pagos", "pagos del mes"
            ).any { it in lowerQuery }

            if (isFinanceQuery) {
                val allTxs = studentTools.getAllTransactions()
                val studentsResult = studentTools.getAllStudentsSummary()
                val students = (studentsResult as? SueOperationResult.AllStudentsSummary)?.students ?: emptyList()
                val schedules = scheduleTools.getScheduleDetails()
                val logs = studentTools.getAllClassLogs()

                appendLine("--- HISTORIAL GENERAL DE TRANSACCIONES / INGRESOS ---")
                if (allTxs.isNotEmpty()) {
                    val totalEarnings = allTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
                    appendLine("Total cobrado (pagos recibidos): $totalEarnings euros")
                    allTxs.take(15).forEach { t ->
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t.timestamp))
                        val typeStr = if (t.type == "PAYMENT") "PAGO" else "DEUDA"
                        val payTypeStr = t.paymentType?.let { " ($it)" } ?: ""
                        val sName = schedules.firstOrNull { it.studentId == t.studentId }?.studentName ?: "Alumno"
                        appendLine("- $dateStr: $typeStr de ${t.amount} euros$payTypeStr de $sName")
                    }
                } else {
                    appendLine("Total cobrado (pagos recibidos): 0 euros")
                    appendLine("No se han registrado pagos o facturas todavía en la aplicación (el balance es cero).")
                }
                appendLine("--- FIN HISTORIAL GENERAL ---")

                val balanceResult = studentTools.getStudentsWithBalance()
                if (balanceResult is SueOperationResult.StudentsWithBalance && balanceResult.students.isNotEmpty()) {
                    appendLine("--- DEUDAS Y SALDOS PENDIENTES DE ALUMNOS (FACTURACIÓN POR COBRAR) ---")
                    balanceResult.students.forEach { s ->
                        val prefix = if (s.pendingBalance < 0.0) "saldo a favor de" else "debe"
                        appendLine("- ${s.name}: ${"%.2f".format(Math.abs(s.pendingBalance))} euros ($prefix)")
                    }
                    appendLine("--- FIN DEUDAS Y SALDOS ---")
                }

                appendLine("--- HISTORIAL DE CLASES IMPARTIDAS (COMPLETADAS) ---")
                if (logs.isNotEmpty()) {
                    logs.take(20).forEach { l ->
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(l.date))
                        val sName = schedules.firstOrNull { it.studentId == l.studentId }?.studentName ?: "Alumno"
                        val studentPrice = students.firstOrNull { it.name.lowercase() == sName.lowercase() }?.pricePerHour ?: 0.0
                        appendLine("- $dateStr: Clase impartida a $sName de ${l.startTime} a ${l.endTime} (tarifa habitual: $studentPrice €/hora)")
                    }
                } else {
                    appendLine("No hay registro de clases completadas o impartidas este mes.")
                }
                appendLine("--- FIN HISTORIAL CLASES ---")
            }

            // 2b. Global class history queries
            val isGlobalClassHistoryQuery = listOf(
                "cuántas clases he dado", "cuantas clases he dado",
                "total de clases", "clases del mes", "clases este mes",
                "cuántas he impartido", "cuantas he impartido",
                "histórico de clases", "historico de clases",
                "cuántas clases llevo", "cuantas clases llevo"
            ).any { it in lowerQuery }

            if (isGlobalClassHistoryQuery && conversationContext.lastMentionedStudentName == null) {
                val allLogs = studentTools.getAllClassLogs()
                appendLine("--- HISTORIAL GLOBAL DE CLASES IMPARTIDAS ---")
                if (allLogs.isNotEmpty()) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    appendLine("Total de clases registradas: ${allLogs.size}")
                    allLogs.take(20).forEach { l ->
                        appendLine("- ${sdf.format(Date(l.date))}: clase de ${l.startTime} a ${l.endTime}")
                    }
                } else {
                    appendLine("Total de clases registradas: 0")
                    appendLine("No hay registros de asistencia ni clases finalizadas en el historial.")
                }
                appendLine("--- FIN HISTORIAL GLOBAL ---")
            }

            // 2c. Global debt queries
            val isGlobalDebtQuery = listOf(
                "no han pagado", "no ha pagado", "sin pagar",
                "pendiente de pago", "deudas pendientes", "quién debe",
                "quien debe", "who owes"
            ).any { it in lowerQuery }

            if (isGlobalDebtQuery && conversationContext.lastMentionedStudentName == null) {
                val balanceResult = studentTools.getStudentsWithBalance()
                appendLine("--- ALUMNOS CON SALDO PENDIENTE ---")
                if (balanceResult is SueOperationResult.StudentsWithBalance && balanceResult.students.isNotEmpty()) {
                    balanceResult.students.forEach { s ->
                        appendLine("- ${s.name}: ${"%.2f".format(s.pendingBalance)} € pendientes")
                    }
                } else {
                    appendLine("Todos los alumnos están al corriente de pago. No hay deudas pendientes en este momento.")
                }
                appendLine("--- FIN SALDOS PENDIENTES ---")
            }

            // 2d. Shared resources / materials query
            val isResourceQuery = listOf(
                "material", "materiales", "recurso", "recursos", "archivo", "archivos",
                "documento", "documentos", "pdf", "enviado", "compartido", "compartir",
                "resource", "resources", "shared"
            ).any { it in lowerQuery }

            if (isResourceQuery && conversationContext.lastMentionedStudentName == null) {
                val allResources = studentTools.getAllSharedResources()
                val schedules = scheduleTools.getScheduleDetails()
                val studentMap = schedules.associate { it.studentId to it.studentName }
                val groupedByStudent = allResources.groupBy { it.studentId }
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                appendLine("--- RECURSOS / MATERIALES COMPARTIDOS CON ALUMNOS ---")
                if (allResources.isNotEmpty()) {
                    appendLine("Total de recursos compartidos: ${allResources.size} entre ${groupedByStudent.size} alumnos.")
                    if (allResources.size <= 15) {
                        allResources.sortedByDescending { it.sharedAt }.forEach { r ->
                            val sName = studentMap[r.studentId] ?: "Alumno"
                            val dateStr = sdf.format(Date(r.sharedAt))
                            appendLine("- Alumno: $sName | Archivo: ${r.fileName} (${r.fileType}) compartido el $dateStr vía ${r.sharedVia}")
                        }
                    } else {
                        appendLine("Desglose y resumen por alumno:")
                        groupedByStudent.entries.take(20).forEach { (studentId, resList) ->
                            val sName = studentMap[studentId] ?: "Alumno"
                            val sortedRes = resList.sortedByDescending { it.sharedAt }
                            val count = resList.size
                            val latest = sortedRes.first()
                            val dateStr = sdf.format(Date(latest.sharedAt))
                            appendLine("- $sName: $count archivo(s) compartido(s) (el más reciente: '${latest.fileName}', $dateStr)")
                        }
                        if (groupedByStudent.size > 20) {
                            appendLine("(Se muestran 20 alumnos con actividad más reciente de un total de ${groupedByStudent.size})")
                        }
                        appendLine("Instrucción: Ofrece al profesor un resumen conciso por alumno y recuérdale que puede consultar el detalle completo de un alumno específico.")
                    }
                } else {
                    appendLine("No se han compartido materiales, recursos ni archivos con ningún alumno todavía.")
                }
                appendLine("--- FIN RECURSOS / MATERIALES COMPARTIDOS ---")
            }

            when {
                conversationContext.lastMentionedStudentName != null -> {
                    val currentName = conversationContext.lastMentionedStudentName!!
                    appendLine(formatResult(studentTools.searchStudent(currentName)))
                    val studentSchedules = scheduleTools.getSchedulesByStudentName(currentName)
                    if (studentSchedules.isNotEmpty()) {
                        appendLine("--- WEEKLY CLASSES FOR $currentName ---")
                        studentSchedules.forEach { s ->
                            val summary = AgentScheduleSummary(
                                studentName = s.studentName,
                                dayOfWeek = s.dayOfWeek,
                                startTime = s.startTime,
                                endTime = s.endTime
                            )
                            appendLine(formatResult(SueOperationResult.DaySchedule(s.dayOfWeek, null, listOf(summary))))
                        }
                        appendLine("--- END OF WEEKLY CLASSES ---")
                    }

                    val resources = studentTools.getSharedResources(currentName)
                    if (resources.isNotEmpty()) {
                        appendLine("--- RECURSOS COMPARTIDOS CON $currentName ---")
                        resources.sortedByDescending { it.sharedAt }.take(20).forEach { r ->
                            appendLine("- ${r.fileName} (${r.fileType}) compartido vía ${r.sharedVia}")
                        }
                        if (resources.size > 20) {
                            appendLine("(Se muestran los 20 recursos más recientes)")
                        }
                        appendLine("--- FIN RECURSOS COMPARTIDOS ---")
                    }

                    val logs = studentTools.getClassLogs(currentName)
                    if (logs.isNotEmpty()) {
                        appendLine("--- CLASES COMPLETADAS PARA $currentName ---")
                        appendLine("Total de clases dadas: ${logs.size}")
                        logs.forEach { l ->
                            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(l.date))
                            appendLine("- Clase del $dateStr a las ${l.startTime} (Duración: de ${l.startTime} a ${l.endTime})")
                        }
                        appendLine("--- FIN CLASES COMPLETADAS ---")
                    }

                    val txs = studentTools.getTransactions(currentName)
                    if (txs.isNotEmpty()) {
                        appendLine("--- TRANSACCIONES / PAGOS DE $currentName ---")
                        txs.forEach { t ->
                            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(t.timestamp))
                            val typeStr = if (t.type == "PAYMENT") "PAGO RECIBIDO" else "DEUDA SUMADA"
                            val payTypeStr = t.paymentType?.let { " ($it)" } ?: ""
                            appendLine("- $dateStr: $typeStr de ${t.amount} euros$payTypeStr")
                        }
                        appendLine("--- FIN TRANSACCIONES ---")
                    }
                }

                containsCountKeywords(lowerQuery) ->
                    appendLine(formatResult(studentTools.getActiveStudentCount()))

                containsBalanceKeywords(lowerQuery) ->
                    appendLine(formatResult(studentTools.getStudentsWithBalance()))

                containsAllStudentsKeywords(lowerQuery) && !hasSchedule ->
                    appendLine(formatResult(studentTools.getAllStudentsSummary()))
            }
        }
    }

    private fun formatResult(result: SueOperationResult): String =
        SueResponseFormatter.format(result)

    private fun containsScheduleKeywords(query: String) =
        listOf(
            "horario", "schedule", "clase", "clases", "class",
            "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes",
            "sábado", "sabado", "domingo",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "semana", "week", "hoy", "today", "mañana", "tomorrow", "tarde", "esta tarde",
            "ayer", "esta mañana", "luego", "que viene", "pasado", "el próximo", "la próxima",
            "próxima clase", "siguiente clase", "hueco", "libre"
        ).any { it in query }

    private fun containsNextClassKeywords(query: String) =
        listOf(
            "próxima clase", "siguiente clase", "próxima", "siguiente",
            "cuándo tengo", "cuándo es", "cuándo empieza", "next class"
        ).any { it in query }

    private fun containsFreeSlotKeywords(query: String): Boolean {
        val days = listOf("lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo", "hoy", "mañana")
        if (days.any { it in query }) {
            return false
        }
        return listOf(
            "hueco libre", "huecos libres", "día libre", "días libres",
            "tengo libre", "estoy libre", "tengo disponible", "estoy disponible",
            "espacio libre", "rato libre", "momento libre",
            "free slot", "available slot", "am i free", "am i available"
        ).any { it in query }
    }

    private fun containsFreeSlotWithDayKeywords(query: String): Boolean {
        val hasFreeIntent = listOf(
            "hueco", "huecos", "libre", "libres", "disponible", "disponibles",
            "espacio", "rato", "momento", "free", "available"
        ).any { it in query }
        val hasDay = listOf(
            "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes",
            "sábado", "sabado", "domingo", "hoy", "mañana"
        ).any { it in query }
        return hasFreeIntent && hasDay
    }

    private fun containsCancelledQueryKeywords(query: String): Boolean {
        val hasExceptionQuery = listOf(
            "cancelada", "canceladas", "cancelado", "cancelados",
            "reprogramada", "reprogramadas", "reprogramado", "reprogramados",
            "clase cancelada", "clases canceladas",
            "hay cancelada", "hay canceladas", "tengo cancelada", "tengo canceladas",
            "algo cancelado", "algo cancelada",
            "cancele", "cancelé", "he cancelado", "he cancelada",
            "anulada", "anuladas", "anulado", "anulados", "anule", "anulé",
            "cancelacion", "cancelación", "cancelaciones",
            "que cancelaciones", "qué cancelaciones", "cuales canceladas", "cuáles canceladas",
            "clase extra", "clases extra", "clases extras", "clase adicional", "clases adicionales",
            "reprogramaciones", "excepciones", "excepcion", "excepción"
        ).any { it in query }

        val isActionCommand = listOf(
            "cancela ", "cancela la", "cancela el", "cancela a",
            "cancelar ", "cancelar la", "cancelar el", "cancelar a",
            "anula ", "anula la", "anula el", "anula a",
            "añade una clase extra", "pon una clase extra", "agrega una clase extra",
            "añadir clase extra", "poner clase extra"
        ).any { it in query }

        return hasExceptionQuery && !isActionCommand
    }

    private fun containsAllStudentsKeywords(query: String) =
        listOf(
            "alumno", "alumnos", "estudiante", "estudiantes",
            "student", "students", "quién", "quien", "lista", "list",
            "todos", "all"
        ).any { it in query }

    private fun containsBalanceKeywords(query: String) =
        listOf(
            "saldo", "balance", "deuda", "debt", "dinero", "money",
            "cobrar", "pagar", "pago", "payment", "pendiente", "pending", "debe", "owe"
        ).any { it in query }

    private fun containsCountKeywords(query: String) =
        listOf(
            "cuántos", "cuantos", "how many", "count",
            "total", "número", "numero", "number", "cuánta", "cuenta"
        ).any { it in query }
}
