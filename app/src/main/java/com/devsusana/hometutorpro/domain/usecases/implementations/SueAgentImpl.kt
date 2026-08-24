package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.ISueAgent
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestration layer for the Sue AI agent.
 *
 * Responsibilities:
 * 1. Detect intent from the user's query (keyword-based routing).
 * 2. Gather relevant tool context for LLM prompts.
 * 3. Detect schedule management intents (cancel/reschedule) and build
 *    [SuePendingAction] instances for the confirmation flow.
 * 4. Provide the system prompt that defines Sue's personality.
 */
@Singleton
class SueAgentImpl @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools,
    private val dateTimeProvider: DateTimeProvider,
    private val authRepository: AuthRepository
) : ISueAgent {

    private fun stripAccents(str: String): String {
        val normalized = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    companion object {

        /**
         * Builds the Sue system prompt with a locale-aware language instruction.
         *
         * @param locale The locale to format instructions for.
         */
        fun buildSystemPrompt(locale: Locale, isQuestion: Boolean = false): String {
            val languageTag = locale.toLanguageTag() // e.g. "es-ES", "en-GB", "en-US"
            val languageName = locale.getDisplayLanguage(Locale.ENGLISH) // e.g. "Spanish"
            val countryName = if (locale.country.isNotEmpty()) {
                locale.getDisplayCountry(Locale.ENGLISH) // e.g. "Spain", "United Kingdom"
            } else null

            val languageInstruction = if (countryName != null) {
                "Always respond in $languageName as spoken in $countryName ($languageTag). " +
                "Use vocabulary, spelling and expressions appropriate for that region."
            } else {
                "Always respond in $languageName ($languageTag)."
            }

            if (isQuestion) {
                return """
                    Eres la asistente inteligente de HomeTutorPro.
                    Tu rol es responder a las preguntas del profesor de forma breve, amable y concisa.
                    NUNCA inventes datos. Usa SOLO la información provista en la sección --- AVAILABLE DATA ---.
                    Si no hay datos en esa sección o la sección está vacía, dile al profesor que no tienes clases o información registrada sobre eso.
                    NUNCA uses la palabra "Sue" en tus respuestas, ni te refieras a ti misma con ese nombre bajo ninguna circunstancia.
                    $languageInstruction
                """.trimIndent()
            }

            return """
                Eres la asistente inteligente de HomeTutorPro. Tu rol es ayudar a profesores particulares a gestionar su trabajo de forma eficiente.

                Tu personalidad:
                - Eres profesional, amable y concisa.
                - Evitas respuestas largas — sé directa y útil.
                - Si no puedes ayudar con algo, dilo claramente.
                - NO te describes a ti misma como IA. Compórtate como asistente de forma natural.
                - NUNCA digas la palabra "Sue" ni te refieras a ti misma con ese nombre en tus respuestas.
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

    private enum class IntentType {
        START_CLASS,
        CREATE_STUDENT,
        DELETE_STUDENT,
        ADD_EXTRA_CLASS,
        CREATE_SCHEDULE,
        DELETE_SCHEDULE,
        CANCEL_CLASS,
        RESCHEDULE_CLASS,
        REGISTER_PAYMENT,
        ADD_BALANCE,
        UPDATE_STUDENT_NOTES,

        // Read Queries
        QUERY_STUDENT_WEEKLY_CLASSES,
        QUERY_TODAY_SUMMARY,
        QUERY_NEXT_CLASS,
        QUERY_FREE_SLOTS,
        QUERY_STUDENTS_WITH_BALANCE,
        QUERY_STUDENT_BALANCE,
        QUERY_STUDENT_COUNT,
        QUERY_STUDENT_DETAILS,
        QUERY_DAY_SCHEDULE
    }

    private var lastMentionedStudentName: String? = null
    private var lastMentionedDayOfWeek: Int? = null
    private var lastMentionedTime: String? = null
    private var lastMentionedAmount: Double? = null
    private var lastMentionedDuration: Int? = null
    private var lastMentionedPrice: Double? = null
    private var lastMentionedSubjects: String? = null
    private var lastMentionedCourse: String? = null
    private var lastActiveIntentType: IntentType? = null

    override fun resetConversationContext() {
        lastMentionedStudentName = null
        lastMentionedDayOfWeek = null
        lastMentionedTime = null
        lastMentionedAmount = null
        lastMentionedDuration = null
        lastMentionedPrice = null
        lastMentionedSubjects = null
        lastMentionedCourse = null
        lastActiveIntentType = null
    }



    // ──────────────────────────────────────────────────────────────────────────
    // Normal LLM prompt building
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun buildPromptWithContext(
        userQuery: String,
        history: List<Pair<String, String>>
    ): String {
        val toolContext = gatherRelevantContext(userQuery)
        val locale = dateTimeProvider.getLocale()
        val isQuestion = isQuestionOrQuery(userQuery)

        // Format date/time in the device locale so it reads naturally
        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, HH:mm", locale)
        val currentDateTime = dateTimeProvider.getNow().format(formatter)

        return buildString {
            appendLine(buildSystemPrompt(locale, isQuestion))
            appendLine()
            appendLine("--- TEMPORAL CONTEXT ---")
            appendLine("Current date/time: $currentDateTime")
            appendLine("--- END OF TEMPORAL CONTEXT ---")
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
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Context gathering — BUG FIX: removed the fallback that dumped all students
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Determines which tool context to inject based on the user's query.
     *
     * This is a `suspend` function because the tool methods perform database I/O.
     *
     * **Bug-fix:** The previous implementation had a fallback `if (isEmpty())`
     * block that called [StudentTools.getAllStudentsSummary] for ANY unrecognised
     * query (including questions about a specific student where the name wasn't
     * detected). The new version returns an empty string when no keyword matches,
     * letting the LLM respond gracefully without raw data.
     */
    private suspend fun gatherRelevantContext(query: String): String {
        val lowerQuery = stripAccents(query.lowercase())

        // 1. Extract and update context memory variables
        val matchedStudent = studentTools.extractRelevantStudent(lowerQuery)
        val studentName = matchedStudent?.name ?: extractStudentName(lowerQuery) ?: extractStudentNameForFinance(lowerQuery)
        if (studentName != null) {
            val capitalizedName = studentName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            lastMentionedStudentName = capitalizedName
        }

        val relativeDay = extractRelativeDayOfWeek(lowerQuery)
        val explicitDay = extractDayOfWeek(lowerQuery)
        val day = relativeDay ?: explicitDay
        if (day != null) {
            lastMentionedDayOfWeek = day
        }

        val time = extractTime(lowerQuery)
        if (time != null) {
            lastMentionedTime = time
        }

        return buildString {
            // 1. Schedule queries
            if (containsScheduleKeywords(lowerQuery)) {
                if (containsNextClassKeywords(lowerQuery)) {
                    appendLine(formatResult(scheduleTools.getNextClass()))
                } else if (containsFreeSlotKeywords(lowerQuery)) {
                    appendLine(formatResult(scheduleTools.getFreeSlots()))
                } else {
                    val daysToInject = mutableSetOf<Int>()
                    daysToInject.add(dateTimeProvider.getNow().dayOfWeek.value)

                    if (day != null) {
                        daysToInject.add(day)
                    }
                    if (lastMentionedDayOfWeek != null) {
                        daysToInject.add(lastMentionedDayOfWeek!!)
                    }

                    val extractedDays = extractTwoDaysOfWeek(lowerQuery)
                    if (extractedDays.first != null) daysToInject.add(extractedDays.first!!)
                    if (extractedDays.second != null) daysToInject.add(extractedDays.second!!)

                    val timeFilter = extractTime(lowerQuery) ?: extractTimeOfDayFilter(lowerQuery)
                    if (day == null && timeFilter != null && timeFilter.contains(":")) {
                        val lookupName = lastMentionedStudentName
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
                }
            }

            // 2. Student queries — mutually exclusive branches to avoid data dumping
            val hasSchedule = containsScheduleKeywords(lowerQuery)

            // 2a. Finance/earnings queries — inject global transaction history
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
                appendLine("--- HISTORIAL GENERAL DE TRANSACCIONES / INGRESOS ---")
                if (allTxs.isNotEmpty()) {
                    val totalEarnings = allTxs.filter { it.type == "PAYMENT" }.sumOf { it.amount }
                    appendLine("Total ganado (pagos recibidos): $totalEarnings euros")
                    allTxs.take(15).forEach { t ->
                        val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(t.timestamp))
                        val typeStr = if (t.type == "PAYMENT") "PAGO" else "DEUDA"
                        val payTypeStr = t.paymentType?.let { " ($it)" } ?: ""
                        appendLine("- $dateStr: $typeStr de ${t.amount} euros$payTypeStr")
                    }
                } else {
                    appendLine("Total ganado (pagos recibidos): 0 euros")
                    appendLine("No se han registrado pagos o facturas todavía en la aplicación (el balance es cero).")
                }
                appendLine("--- FIN HISTORIAL GENERAL ---")

                // Inyectar saldos y deudas actuales de los alumnos
                val balanceResult = studentTools.getStudentsWithBalance()
                if (balanceResult is SueOperationResult.StudentsWithBalance && balanceResult.students.isNotEmpty()) {
                    appendLine("--- DEUDAS Y SALDOS PENDIENTES DE ALUMNOS (FACTURACIÓN POR COBRAR) ---")
                    balanceResult.students.forEach { s ->
                        val prefix = if (s.pendingBalance < 0.0) "saldo a favor de" else "debe"
                        appendLine("- ${s.name}: ${"%.2f".format(Math.abs(s.pendingBalance))} euros ($prefix)")
                    }
                    appendLine("--- FIN DEUDAS Y SALDOS ---")
                }
            }

            // 2b. Global class history queries — inject all class logs when no student is named
            val isGlobalClassHistoryQuery = listOf(
                "cuántas clases he dado", "cuantas clases he dado",
                "total de clases", "clases del mes", "clases este mes",
                "cuántas he impartido", "cuantas he impartido",
                "histórico de clases", "historico de clases",
                "cuántas clases llevo", "cuantas clases llevo"
            ).any { it in lowerQuery }

            if (isGlobalClassHistoryQuery && lastMentionedStudentName == null) {
                val allLogs = studentTools.getAllClassLogs()
                appendLine("--- HISTORIAL GLOBAL DE CLASES IMPARTIDAS ---")
                if (allLogs.isNotEmpty()) {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    appendLine("Total de clases registradas: ${allLogs.size}")
                    allLogs.take(20).forEach { l ->
                        appendLine("- ${sdf.format(java.util.Date(l.date))}: clase de ${l.startTime} a ${l.endTime}")
                    }
                } else {
                    appendLine("Total de clases registradas: 0")
                    appendLine("No hay registros de asistencia ni clases finalizadas en el historial.")
                }
                appendLine("--- FIN HISTORIAL GLOBAL ---")
            }

            // 2c. Global debt queries without a specific student — inject all pending balances
            val isGlobalDebtQuery = listOf(
                "no han pagado", "no ha pagado", "sin pagar",
                "pendiente de pago", "deudas pendientes", "quién debe",
                "quien debe", "who owes"
            ).any { it in lowerQuery }

            if (isGlobalDebtQuery && lastMentionedStudentName == null) {
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

            when {
                lastMentionedStudentName != null -> {
                    appendLine(formatResult(studentTools.searchStudent(lastMentionedStudentName!!)))
                    val studentSchedules = scheduleTools.getSchedulesByStudentName(lastMentionedStudentName!!)
                    if (studentSchedules.isNotEmpty()) {
                        appendLine("--- WEEKLY CLASSES FOR $lastMentionedStudentName ---")
                        studentSchedules.forEach { s ->
                            val summary = com.devsusana.hometutorpro.domain.entities.AgentScheduleSummary(
                                studentName = s.studentName,
                                dayOfWeek = s.dayOfWeek,
                                startTime = s.startTime,
                                endTime = s.endTime
                            )
                            appendLine(formatResult(SueOperationResult.DaySchedule(s.dayOfWeek, null, listOf(summary))))
                        }
                        appendLine("--- END OF WEEKLY CLASSES ---")
                    }

                    // Shared resources injection
                    val resources = studentTools.getSharedResources(lastMentionedStudentName!!)
                    if (resources.isNotEmpty()) {
                        appendLine("--- RECURSOS COMPARTIDOS CON $lastMentionedStudentName ---")
                        resources.forEach { r ->
                            appendLine("- ${r.fileName} (${r.fileType}) compartido vía ${r.sharedVia}")
                        }
                        appendLine("--- FIN RECURSOS COMPARTIDOS ---")
                    }

                    // Completed classes logs injection
                    val logs = studentTools.getClassLogs(lastMentionedStudentName!!)
                    if (logs.isNotEmpty()) {
                        appendLine("--- CLASES COMPLETADAS PARA $lastMentionedStudentName ---")
                        appendLine("Total de clases dadas: ${logs.size}")
                        logs.forEach { l ->
                            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(l.date))
                            appendLine("- Clase del $dateStr a las ${l.startTime} (Duración: de ${l.startTime} a ${l.endTime})")
                        }
                        appendLine("--- FIN CLASES COMPLETADAS ---")
                    }

                    // Student transactions log injection
                    val txs = studentTools.getTransactions(lastMentionedStudentName!!)
                    if (txs.isNotEmpty()) {
                        appendLine("--- TRANSACCIONES / PAGOS DE $lastMentionedStudentName ---")
                        txs.forEach { t ->
                            val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(t.timestamp))
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
                // No else-branch — if nothing matched, no context is injected.
            }
        }
    }

    /**
     * Converts a [SueOperationResult] to a string suitable for LLM context injection.
     * Delegates to the presentation formatter which holds all localized strings.
     */
    private fun formatResult(result: SueOperationResult): String =
        com.devsusana.hometutorpro.presentation.sue.SueResponseFormatter.format(result)

    // ──────────────────────────────────────────────────────────────────────────
    // Keyword helpers
    // ──────────────────────────────────────────────────────────────────────────

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

    /**
     * Detects intent to query free time slots in the schedule.
     * Bare "libre" and "disponible" are excluded to avoid capturing casual conversation
     * like "estoy libre esta tarde" or "tengo una hora libre". Only compound forms are accepted.
     */
    private fun containsFreeSlotKeywords(query: String) =
        listOf(
            "hueco libre", "huecos libres", "día libre", "días libres",
            "tengo libre", "estoy libre", "tengo disponible", "estoy disponible",
            "espacio libre", "rato libre", "momento libre",
            "free slot", "available slot", "am i free", "am i available"
        ).any { it in query }

    private fun containsCancelKeywords(query: String) =
        listOf("cancela", "cancelar", "anula", "anular", "quitar la clase", "quita la clase",
               "cancel", "remove class", "delete class")
            .any { it in query }

    /**
     * Detects intent to reschedule a class.
     * "cambia" and "cambiar" are intentionally excluded as standalone keywords because they are
     * too generic and collide with UPDATE_STUDENT_NOTES ("cambia las notas de...").
     * Only schedule-specific compound forms are allowed.
     */
    private fun containsRescheduleKeywords(query: String): Boolean {
        // High-specificity standalone keywords — safe to match alone
        val specificKeywords = listOf(
            "mueve", "mover", "traslada", "trasladar",
            "reprograma", "reprogramar", "reprograme",
            "pospone", "posponer", "pospón",
            "adelanta", "adelantar",
            "reschedule", "postpone"
        )
        if (specificKeywords.any { it in query }) return true

        // "cambia" / "cambiar" only allowed with a schedule-context word to avoid collision
        val hasChangeVerb = listOf("cambia", "cambiar", "change", "move").any { it in query }
        val hasScheduleContext = listOf(
            "la clase", "el horario", "la hora", "al lunes", "al martes", "al miércoles",
            "al miercoles", "al jueves", "al viernes", "al sábado", "al sabado", "al domingo",
            "para el lunes", "para el martes", "para el jueves", "para el viernes"
        ).any { it in query }
        return hasChangeVerb && hasScheduleContext
    }

    /**
     * Detects intent to register a payment.
     * "pagó" alone is excluded to avoid capturing historical queries like
     * "¿cuánto pagó Juan el mes pasado?". It is only allowed in compound forms
     * that imply a present-tense action or a specific amount.
     */
    private fun containsRegisterPaymentKeywords(query: String): Boolean {
        // Safe multi-word phrases
        val safeKeywords = listOf(
            "registra un pago", "registrar un pago",
            "ha pagado", "abonó", "abono", "pago de",
            "register payment", "made a payment", "payment of"
        )
        if (safeKeywords.any { it in query }) return true

        // "pagó" only accepted when followed by a time reference (implies present action)
        val hasPago = "pagó" in query || "paid" in query
        val hasTimeOrAmount = listOf(
            "hoy", "ayer", "esta semana", "acaba de", "ahora",
            "€", "euro", "euros", "dollar", "dollars"
        ).any { it in query }
        return hasPago && hasTimeOrAmount
    }

    private fun containsAddBalanceKeywords(query: String) =
        listOf("suma saldo", "sumar saldo", "añade saldo", "añadir saldo", "suma a la deuda", "añade a la deuda", "súmale", "sumale",
               "add balance", "add to debt", "add to balance", "add to the debt")
            .any { it in query }

    /**
     * Detects intent to update student notes/observations.
     * Generic words like "nota" and "comentario" are only accepted when combined with an
     * explicit write verb to avoid capturing conversational phrases like
     * "toma nota de que mañana no hay clase" (which should route to CANCEL_CLASS).
     */
    private fun containsUpdateNotesKeywords(query: String): Boolean {
        // Safe multi-word phrases that unambiguously refer to student notes
        val safeKeywords = listOf(
            "apunte", "apuntes", "observacion", "observaciones",
            "observation", "observations"
        )
        if (safeKeywords.any { it in query }) return true

        // "nota", "comentario", "note", "comment" only accepted with an explicit write verb
        val hasNoteWord = listOf("nota", "notas", "comentario", "comentarios", "note", "notes", "comment", "comments").any { it in query }
        val hasWriteVerb = listOf(
            "añade", "añadir", "agrega", "agregar", "pon", "poner",
            "escribe", "escribir", "actualiza", "actualizar",
            "modifica", "modificar", "cambia", "cambiar",
            "add", "write", "update", "change"
        ).any { it in query }
        return hasNoteWord && hasWriteVerb
    }

    private fun extractNotesText(query: String): String? {
        val markers = listOf("que diga que ", "que diga ", "observacion: ", "observaciones: ", "nota: ", "notas: ", "apunte: ", "apuntes: ", "comentario: ", "comentarios: ", "que ", "para: ", "nota ", "notas ")
        for (marker in markers) {
            val idx = query.indexOf(marker)
            if (idx >= 0) {
                val text = query.substring(idx + marker.length).trim()
                if (text.isNotBlank()) return text
            }
        }
        return null
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

    private fun containsStartClassKeywords(query: String) =
        listOf("inicia una clase", "inicia clase", "empieza clase", "comienza clase", "start class").any { it in query }

    private fun containsCreateStudentKeywords(query: String) =
        listOf("crea un alumno", "crear un alumno", "crea al alumno", "crea el alumno", "añade al alumno", "añade al estudiante", "añadir estudiante", "create student", "add student").any { it in query }

    private fun containsDeleteStudentKeywords(query: String) =
        listOf("elimina al alumno", "elimina al estudiante", "borra al alumno", "borra al estudiante", "eliminar alumno", "delete student", "remove student").any { it in query }

    private fun containsAddExtraClassKeywords(query: String) =
        listOf("clase extra", "clase adicional", "tutoría adicional", "tutoria adicional", "extra class", "additional class").any { it in query }

    private fun containsCreateScheduleKeywords(query: String) =
        listOf("añade un horario", "añadir horario", "programa una clase los", "crear horario", "create schedule", "add schedule").any { it in query }

    private fun containsDeleteScheduleKeywords(query: String) =
        listOf(
            "elimina el horario", "borra el horario",
            "quita el horario", "quitar el horario",
            "quitar horario", "delete schedule", "remove schedule"
        ).any { it in query }

    // ──────────────────────────────────────────────────────────────────────────
    // Extraction helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Extracts ISO day-of-week (1=Monday … 7=Sunday) from the query, or null.
     */
    fun extractDayOfWeek(query: String): Int? {
        val dayMappings = mapOf(
            "lunes" to 1, "monday" to 1,
            "martes" to 2, "tuesday" to 2,
            "miércoles" to 3, "miercoles" to 3, "wednesday" to 3,
            "jueves" to 4, "thursday" to 4,
            "viernes" to 5, "friday" to 5,
            "sábado" to 6, "sabado" to 6, "saturday" to 6,
            "domingo" to 7, "sunday" to 7
        )
        return dayMappings.entries.firstOrNull { it.key in query }?.value
    }

    /**
     * Resolves relative time references (hoy, mañana, ayer, today, tomorrow, yesterday)
     * to an ISO day-of-week number (1=Monday … 7=Sunday).
     * Returns null if no relative reference is found.
     */
    private fun extractRelativeDayOfWeek(query: String): Int? {
        val today = dateTimeProvider.getNow().dayOfWeek.value
        return when {
            "hoy" in query || "today" in query ||
            "esta tarde" in query || "esta mañana" in query ||
            "esta noche" in query || "tonight" in query ||
            "luego" in query || "later" in query -> today

            "mañana" in query || "tomorrow" in query -> (today % 7) + 1

            "ayer" in query || "yesterday" in query -> if (today == 1) 7 else today - 1

            else -> null
        }
    }

    /**
     * Detects a time-of-day filter from the query.
     * Returns "morning", "afternoon", or null (all day).
     */
    fun extractTimeOfDayFilter(query: String): String? = when {
        "tarde" in query || "afternoon" in query || "evening" in query -> "afternoon"
        "mañana" in query && ("esta" in query || "por la" in query) -> "morning"
        "morning" in query -> "morning"
        else -> null
    }


    /**
     * Extracts two days of the week from a reschedule query (from/to).
     * Returns (fromDay, toDay) where each can be null if not found.
     */
    private fun extractTwoDaysOfWeek(query: String): Pair<Int?, Int?> {
        val today = dateTimeProvider.getNow().dayOfWeek.value
        val tomorrow = (today % 7) + 1
        val yesterday = if (today == 1) 7 else today - 1

        val rawKeywords = listOf(
            "esta mañana" to today,
            "esta tarde" to today,
            "esta noche" to today,
            "tonight" to today,
            "tomorrow" to tomorrow,
            "mañana" to tomorrow,
            "yesterday" to yesterday,
            "ayer" to yesterday,
            "later" to today,
            "luego" to today,
            "today" to today,
            "hoy" to today,
            "lunes" to 1, "monday" to 1,
            "martes" to 2, "tuesday" to 2,
            "miércoles" to 3, "miercoles" to 3, "wednesday" to 3,
            "jueves" to 4, "thursday" to 4,
            "viernes" to 5, "friday" to 5,
            "sábado" to 6, "sabado" to 6, "saturday" to 6,
            "domingo" to 7, "sunday" to 7
        )

        val allKeywords = rawKeywords.sortedByDescending { it.first.length }
        val matchedIndices = BooleanArray(query.length)
        val matches = mutableListOf<Pair<Int, Int>>() // Pair(startIndex, dayOfWeek)

        for ((keyword, day) in allKeywords) {
            var idx = query.indexOf(keyword)
            while (idx >= 0) {
                val endIdx = idx + keyword.length
                var alreadyMatched = false
                for (i in idx until endIdx) {
                    if (matchedIndices[i]) {
                        alreadyMatched = true
                        break
                    }
                }
                if (!alreadyMatched) {
                    for (i in idx until endIdx) {
                        matchedIndices[i] = true
                    }
                    matches.add(Pair(idx, day))
                }
                idx = query.indexOf(keyword, idx + 1)
            }
        }

        val sortedMatches = matches.sortedBy { it.first }
        val distinctDays = mutableListOf<Int>()
        for (m in sortedMatches) {
            if (m.second !in distinctDays) {
                distinctDays.add(m.second)
            }
        }
        return Pair(distinctDays.getOrNull(0), distinctDays.getOrNull(1))
    }

    /**
     * Attempts to extract a student name from the query by matching against
     * any known student's first name. Returns the matched full name or null.
     */
    private fun extractStudentName(query: String): String? {
        val commonConnectors = listOf(
            "cancela la clase de ", "cancelar la clase de ", "anula la clase de ",
            "anular la clase de ", "mueve la clase de ", "mover la clase de ",
            "cambia la clase de ", "cambiar la clase de ", "pasa la clase de ",
            "traslada la clase de ", "la clase de ",
            "cancela la clase con ", "cancelar la clase con ", "mueve la clase con ",
            "mover la clase con ", "cambia la clase con ", "cambiar la clase con ",
            "cancela a ", "cancelar a ", "anula a ", "anular a ",
            "mueve a ", "mover a ", "cambia a ", "cambiar a ",
            "elimina el horario de ", "borra el horario de ", "eliminar el horario de ",
            "quitar el horario de ", "quita el horario de ",
            "añade un horario para ", "añadir un horario para ", "crea un horario para ",
            "crear un horario para ", "horario para ", "horario de ",
            "elimina al alumno ", "eliminar al alumno ", "borra al alumno ", "borrar al alumno ",
            "elimina a ", "eliminar a ", "borra a ", "borrar a ",
            "cancel class for ", "cancel the class of ", "move class for ",
            "reschedule class for ", "change class for ", "the class of ",
            "cancel class with ", "cancel the class with ", "move class with ",
            "change class with "
        )
        for (connector in commonConnectors) {
            val idx = query.indexOf(connector)
            if (idx >= 0) {
                val rest = query.substring(idx + connector.length).trim()
                // Take up to the next preposition or day keyword
                val stopWords = listOf(" del ", " de ", " el ", " al ", " a ", " en ", " los ", " las ", " con ",
                                       " from ", " of ", " the ", " to ", " at ", " in ", " on ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                val name = rest.substring(0, end).trim()
                if (name.isNotBlank()) {
                    val dayKeywords = setOf(
                        "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo",
                        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
                        "hoy", "mañana", "tomorrow", "esta tarde", "esta mañana", "esta noche", "luego", "ayer", "yesterday",
                        "tarde", "mañana"
                    )
                    if (name.lowercase() in dayKeywords || dayKeywords.any { name.lowercase().contains(it) }) {
                        continue
                    }
                    return name
                }
            }
        }
        return null
    }

    /**
     * Resolves AM/PM ambiguity for 12-hour values (1..8) to PM.
     */
    private fun resolveHour(h: Int, query: String): Int {
        val professor = authRepository.currentUser.value
        val startStr = professor?.workingStartTime ?: "08:00"
        val endStr = professor?.workingEndTime ?: "23:00"
        return resolveHourSmart(h, query, startStr, endStr)
    }

    private fun resolveHourSmart(h: Int, query: String, workingStartTime: String, workingEndTime: String): Int {
        if (h == 0 || h > 12) {
            return h
        }
        val lower = query.lowercase()

        // 1. Explicit morning keywords (AM)
        val morningKeywords = listOf("esta mañana", "por la mañana", "de la mañana", "la mañana de", "madrugada", "am", "a.m.", "morning")
        if (morningKeywords.any { it in lower }) {
            return if (h == 12) 0 else h
        }

        // 2. Explicit afternoon/night keywords (PM)
        val afternoonKeywords = listOf("esta tarde", "esta noche", "por la tarde", "por la noche", "de la tarde", "de la noche", "tarde", "noche", "pm", "p.m.", "afternoon", "evening", "night")
        if (afternoonKeywords.any { it in lower }) {
            return if (h == 12) 12 else h + 12
        }

        // 3. Check working hours fit
        val workStart = try { LocalTime.parse(workingStartTime) } catch (e: Exception) { LocalTime.of(8, 0) }
        val workEnd = try { LocalTime.parse(workingEndTime) } catch (e: Exception) { LocalTime.of(23, 0) }

        val amTime = LocalTime.of(if (h == 12) 0 else h, 0)
        val pmTime = LocalTime.of(if (h == 12) 12 else h + 12, 0)

        val amFits = !amTime.isBefore(workStart) && !amTime.isAfter(workEnd)
        val pmFits = !pmTime.isBefore(workStart) && !pmTime.isAfter(workEnd)

        if (amFits && !pmFits) {
            return if (h == 12) 0 else h
        }
        if (pmFits && !amFits) {
            return if (h == 12) 12 else h + 12
        }

        // 4. Default fallback convention
        if (h in 1..8) {
            return h + 12 // PM (13:00 to 20:00)
        }
        return h // AM (9:00 to 12:00)
    }

    /**
     * Attempts to extract a time in "HH:mm" format from the query.
     * Recognises patterns like "a las 11:00", "a las 11", "11:00", "11h".
     */
    private fun extractTime(query: String): String? {
        val lower = query.lowercase()
        // Pattern: HH:mm
        val colonPattern = Regex("""\b(\d{1,2}):(\d{2})\b""")
        colonPattern.find(lower)?.let { match ->
            val h = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) {
                val resolvedH = resolveHour(h, lower)
                return "%02d:%02d".format(resolvedH, m)
            }
        }
        // Pattern: HHmm (military format with prefix, e.g. "las 1800")
        val militaryPattern = Regex("""\b(?:a\s+)?las\s+(\d{2})(\d{2})\b|\b(?:a\s+)?la\s+(\d{2})(\d{2})\b""")
        militaryPattern.find(lower)?.let { match ->
            val hStr = match.groupValues[1].takeIf { it.isNotEmpty() } ?: match.groupValues[3]
            val mStr = match.groupValues[2].takeIf { it.isNotEmpty() } ?: match.groupValues[4]
            val h = hStr.toIntOrNull()
            val m = mStr.toIntOrNull()
            if (h != null && h in 0..23 && m != null && m in 0..59) {
                val resolvedH = resolveHour(h, lower)
                return "%02d:%02d".format(resolvedH, m)
            }
        }
        // Pattern: "a las NN", "las NN", "a la NN", "la NN" (whole hours)
        val hourPattern = Regex("""\b(?:a\s+)?las\s+(\d{1,2})\b|\b(?:a\s+)?la\s+(\d{1,2})\b""")
        hourPattern.find(lower)?.let { match ->
            val hStr = match.groupValues[1].takeIf { it.isNotEmpty() } ?: match.groupValues[2]
            val h = hStr.toIntOrNull()
            if (h != null && h in 0..23) {
                val resolvedH = resolveHour(h, lower)
                return "%02d:00".format(resolvedH)
            }
        }
        return null
    }

    /**
     * Attempts to extract a student name for financial intents.
     */
    private fun extractStudentNameForFinance(query: String): String? {
        val commonConnectors = listOf(
            "pagado por ", "deuda de ", "saldo de ", "a la cuenta de ",
            "a la deuda de ", "sumale a ", "súmale a ",
            "paid by ", "debt of ", "balance of ", "to the account of ", "to the debt of ", "add to "
        )
        // First try explicit connectors that don't usually precede numbers
        for (connector in commonConnectors) {
            val idx = query.indexOf(connector)
            if (idx >= 0) {
                val rest = query.substring(idx + connector.length).trim()
                val stopWords = listOf(" en ", " con ", " por ", " el ", " los ",
                                       " in ", " with ", " by ", " the ", " for ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                val name = rest.substring(0, end).trim()
                if (name.isNotBlank()) return name
            }
        }
        
        // Fallback for "de " which might appear after the amount
        // Example: "registra un pago de 20 euros de María"
        val deIdx = query.lastIndexOf(" de ")
        val ofIdx = query.lastIndexOf(" of ")
        val splitIdx = if (deIdx >= 0) deIdx + 4 else if (ofIdx >= 0) ofIdx + 4 else -1
        
        if (splitIdx >= 0) {
            val name = query.substring(splitIdx).trim()
            if (name.isNotBlank() && !name.first().isDigit()) {
                val dayKeywords = listOf(
                    "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo",
                    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
                    "hoy", "mañana", "tomorrow", "esta tarde", "esta mañana", "esta noche", "luego", "ayer", "yesterday"
                )
                if (dayKeywords.any { name.lowercase().contains(it) || it.contains(name.lowercase()) }) {
                    return null
                }
                return name
            }
        }
        
        return null
    }

    /**
     * Extracts a numeric amount (e.g. 20, 15.5) from the query.
     */
    private fun extractAmount(query: String): Double? {
        val amountPattern = Regex("""\b(\d+(?:[.,]\d{1,2})?)\s*(?:euros?|€|pavitos|pavos|dollars?|\$)\b""")
        amountPattern.find(query)?.let { match ->
            return match.groupValues[1].replace(',', '.').toDoubleOrNull()
        }
        
        // Fallback for just a number after payment verbs
        val barePattern = Regex("""(?:pago de|sumar|sumale|súmale|añadir|añade|payment of|add)\s+(\d+(?:[.,]\d{1,2})?)\b""")
        barePattern.find(query)?.let { match ->
            return match.groupValues[1].replace(',', '.').toDoubleOrNull()
        }

        // Resolución de pronombres conversacionales
        val hasQuantityPronoun = listOf("esa cantidad", "esa suma", "eso", "todo", "su deuda", "el total", "el saldo").any { it in query }
        if (hasQuantityPronoun) {
            val fallback = lastMentionedAmount
            if (fallback != null && fallback > 0.0) {
                return fallback
            }
        }
        return null
    }

    private fun extractNameAfter(query: String, keywords: List<String>): String? {
        for (keyword in keywords) {
            val idx = query.indexOf(keyword)
            if (idx >= 0) {
                val rest = query.substring(idx + keyword.length).trim()
                val word = rest.substringBefore(" ").trim()
                if (word.isNotBlank()) return word
            }
        }
        return null
    }

    private fun extractDurationOptional(query: String): Int? {
        val pattern = Regex("""\b(\d+)\s*(?:minutos?|min|m)\b""")
        pattern.find(query)?.let { match ->
            return match.groupValues[1].toIntOrNull()
        }
        val hourPattern = Regex("""\b(\d+)\s*(?:horas?|h)\b""")
        hourPattern.find(query)?.let { match ->
            val h = match.groupValues[1].toDoubleOrNull() ?: 1.0
            return (h * 60).toInt()
        }
        return null
    }

    private fun extractDuration(query: String): Int {
        return extractDurationOptional(query) ?: 60
    }

    private fun parseCreateStudent(query: String): SuePendingAction.CreateStudent? {
        val lower = query.lowercase()
        val nameKeywords = listOf(
            "crea al alumno ", "crea el alumno ", "crea al estudiante ", "crea el estudiante ",
            "crear al alumno ", "crear el alumno ", "añade al alumno ", "añade al estudiante ",
            "create student ", "add student "
        )
        var name = ""
        for (kw in nameKeywords) {
            val idx = lower.indexOf(kw)
            if (idx >= 0) {
                val rest = query.substring(idx + kw.length).trim()
                val stopWords = listOf(" de ", " a ", " para ", " in ", " at ", " for ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.lowercase().indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                name = rest.substring(0, end).trim()
                break
            }
        }
        if (name.isBlank()) return null
        
        val price = extractAmount(lower) ?: 15.0
        
        var subjects = "General"
        val subjectKeywords = listOf(" de ", " in ", " para ", " for ")
        for (kw in subjectKeywords) {
            val idx = lower.indexOf(kw)
            if (idx >= 0) {
                val rest = query.substring(idx + kw.length).trim()
                val stopWords = listOf(" a ", " por ", " at ", " for ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.lowercase().indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                val candidate = rest.substring(0, end).trim()
                if (candidate.isNotBlank() && !candidate.contains(Regex("""\d"""))) {
                    subjects = candidate
                    break
                }
            }
        }
        
        val courseKeywords = listOf("eso", "bachillerato", "bach", "primaria", "secundaria", "universidad")
        val course = courseKeywords.find { it in lower } ?: "Other"
        
        return SuePendingAction.CreateStudent(name, course, subjects, price)
    }

    private fun extractTwoTimes(query: String): Pair<String, String>? {
        val lower = query.lowercase()
        val timePattern = Regex("""\b(\d{1,2}):(\d{2})\b""")
        val matches = timePattern.findAll(lower).toList()
        if (matches.size >= 2) {
            val h1 = matches[0].groupValues[1].toInt()
            val m1 = matches[0].groupValues[2].toInt()
            val h2 = matches[1].groupValues[1].toInt()
            val m2 = matches[1].groupValues[2].toInt()
            val resH1 = resolveHour(h1, lower)
            val resH2 = resolveHour(h2, lower)
            return Pair("%02d:%02d".format(resH1, m1), "%02d:%02d".format(resH2, m2))
        }
        
        val hourRangePattern = Regex("""\b(?:de\s+)?(\d{1,2})\s+a\s+(\d{1,2})\b""")
        hourRangePattern.find(lower)?.let { match ->
            val h1 = match.groupValues[1].toInt()
            val h2 = match.groupValues[2].toInt()
            if (h1 in 0..23 && h2 in 0..23) {
                val resH1 = resolveHour(h1, lower)
                val resH2 = resolveHour(h2, lower)
                return Pair("%02d:00".format(resH1), "%02d:00".format(resH2))
            }
        }
        
        val singleTime = extractTime(query)
        if (singleTime != null) {
            val parts = singleTime.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val endH = (h + 1) % 24
            val endTime = "%02d:%02d".format(endH, m)
            return Pair(singleTime, endTime)
        }
        return null
    }

    private fun extractAllTimesInQuery(query: String): List<String> {
        val lower = query.lowercase()
        val matchedIndices = BooleanArray(lower.length)
        
        data class TimeMatch(val index: Int, val timeStr: String)
        val matches = mutableListOf<TimeMatch>()

        // 1. Match HH:mm pattern (e.g. 17:30, 4:30)
        val colonPattern = Regex("""\b(\d{1,2}):(\d{2})\b""")
        for (match in colonPattern.findAll(lower)) {
            val start = match.range.first
            val end = match.range.last
            val h = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) {
                // Mark indices
                for (i in start..end) {
                    matchedIndices[i] = true
                }
                val resolvedH = resolveHour(h, lower)
                matches.add(TimeMatch(start, "%02d:%02d".format(resolvedH, m)))
            }
        }

        // 2. Match H1 a H2 range (e.g. de 5 a 6, 5 a 6)
        val hourRangePattern = Regex("""\b(?:de\s+)?(\d{1,2})\s+a\s+(\d{1,2})\b""")
        for (match in hourRangePattern.findAll(lower)) {
            val start = match.range.first
            val end = match.range.last
            // Check if any index in this match is already matched
            var overlap = false
            for (i in start..end) {
                if (i in matchedIndices.indices && matchedIndices[i]) {
                    overlap = true
                    break
                }
            }
            if (!overlap) {
                val h1 = match.groupValues[1].toInt()
                val h2 = match.groupValues[2].toInt()
                if (h1 in 0..23 && h2 in 0..23) {
                    for (i in start..end) {
                        matchedIndices[i] = true
                    }
                    val resH1 = resolveHour(h1, lower)
                    val resH2 = resolveHour(h2, lower)
                    matches.add(TimeMatch(start, "%02d:00".format(resH1)))
                    matches.add(TimeMatch(start + 1, "%02d:00".format(resH2)))
                }
            }
        }

        // 3. Match single hour patterns (e.g. a las 5, para las 5, de las 5, a la 1)
        val hourPatterns = listOf(
            Regex("""\b(?:a\s+)?las\s+(\d{1,2})\b"""),
            Regex("""\b(?:a\s+)?la\s+(\d{1,2})\b"""),
            Regex("""\bde\s+las?\s+(\d{1,2})\b"""),
            Regex("""\bpara\s+las?\s+(\d{1,2})\b""")
        )

        for (pattern in hourPatterns) {
            for (match in pattern.findAll(lower)) {
                val start = match.range.first
                val end = match.range.last
                var overlap = false
                for (i in start..end) {
                    if (i in matchedIndices.indices && matchedIndices[i]) {
                        overlap = true
                        break
                    }
                }
                if (!overlap) {
                    val hStr = match.groupValues[1]
                    val h = hStr.toIntOrNull()
                    if (h != null && h in 0..23) {
                        for (i in start..end) {
                            matchedIndices[i] = true
                        }
                        val resolvedH = resolveHour(h, lower)
                        matches.add(TimeMatch(start, "%02d:00".format(resolvedH)))
                    }
                }
            }
        }

        return matches.sortedBy { it.index }.map { it.timeStr }
    }

    private fun isTargetDay(query: String, day: Int): Boolean {
        val lower = query.lowercase()
        val today = dateTimeProvider.getNow().dayOfWeek.value
        val tomorrow = (today % 7) + 1
        val yesterday = if (today == 1) 7 else today - 1

        val dayNames = mutableListOf<String>()
        when (day) {
            1 -> dayNames.addAll(listOf("lunes", "monday"))
            2 -> dayNames.addAll(listOf("martes", "tuesday"))
            3 -> dayNames.addAll(listOf("miércoles", "miercoles", "wednesday"))
            4 -> dayNames.addAll(listOf("jueves", "thursday"))
            5 -> dayNames.addAll(listOf("viernes", "friday"))
            6 -> dayNames.addAll(listOf("sábado", "sabado", "saturday"))
            7 -> dayNames.addAll(listOf("domingo", "sunday"))
        }

        if (day == today) {
            dayNames.addAll(listOf("hoy", "today", "esta tarde", "esta mañana", "esta noche", "tonight"))
        }
        if (day == tomorrow) {
            dayNames.addAll(listOf("mañana", "tomorrow"))
        }
        if (day == yesterday) {
            dayNames.addAll(listOf("ayer", "yesterday"))
        }

        for (name in dayNames) {
            val idx = lower.indexOf(name)
            if (idx >= 0) {
                val prefix = lower.substring(0, idx).trim()
                if (prefix.endsWith(" al") || prefix.endsWith(" a") || prefix.endsWith(" para") || prefix.endsWith(" para el") || prefix.endsWith(" to")) {
                    return true
                }
            }
        }
        return false
    }

    private fun isQuestionOrQuery(query: String): Boolean {
        val lower = query.lowercase()
        if (lower.contains("?")) return true
        val questionWords = listOf(
            "qué ", "que ", "cómo ", "como ", "cuándo ", "cuando ", "dónde ", "donde ",
            "quién ", "quien ", "cuánto ", "cuanto ", "cuál ", "cual ", "horario",
            "clases", "alumnos", "información", "ver ", "muestra ", "mostrar", "dime",
            "info", "tengo", "hay", "list", "show", "get", "who", "what", "when", "where",
            "how", "how much", "how many", "which", "schedule", "classes", "students", "tell me"
        )
        return questionWords.any { lower.contains(it) }
    }

    private fun normalizeLlmTime(timeStr: String?): String? {
        if (timeStr == null) return null
        val cleaned = timeStr.trim().lowercase()
        if (cleaned.isEmpty()) return timeStr

        // Try to parse as HH:mm or HH
        val parts = cleaned.split(":")
        val h = parts[0].toIntOrNull() ?: return timeStr
        val m = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0

        // If the hour is in 1..8, convert to 13..20 (PM)
        val resolvedH = if (h in 1..8) h + 12 else h
        
        return String.format(Locale.US, "%02d:%02d", resolvedH, m)
    }

    override suspend fun parseLlmActionResponse(response: String): SueOperationResult.Prepare? {
        val pattern = """\[ACTION:\s*([A-Z_]+)(?:,\s*(.*))?]""".toRegex(RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(response).toList()
        if (matches.isEmpty()) return null

        val preparedActions = mutableListOf<SuePendingAction>()

        for (match in matches) {
            val actionType = match.groupValues[1].trim()
            val paramsString = match.groupValues.getOrNull(2) ?: ""

            val params = mutableMapOf<String, String>()
            if (paramsString.isNotBlank()) {
                val paramPairs = paramsString.split(",")
                for (pair in paramPairs) {
                    val kv = pair.split(":")
                    if (kv.size == 2) {
                        val key = kv[0].trim()
                        val value = kv[1].trim().removeSurrounding("\"").trim()
                        params[key] = value
                    }
                }
            }

            val student = params["student"]
            val time = normalizeLlmTime(params["time"])
            val startTime = normalizeLlmTime(params["startTime"])
            val endTime = normalizeLlmTime(params["endTime"])
            val targetTime = normalizeLlmTime(params["targetTime"])
            val fromTime = normalizeLlmTime(params["fromTime"])

            // Sincronizar memoria a corto plazo del RAG con la extracción del LLM
            if (student != null) lastMentionedStudentName = student
            if (time != null) lastMentionedTime = time
            if (startTime != null) lastMentionedTime = startTime
            if (targetTime != null) lastMentionedTime = targetTime
            params["amount"]?.toDoubleOrNull()?.let { lastMentionedAmount = it }
            params["price"]?.toDoubleOrNull()?.let { lastMentionedPrice = it }
            params["duration"]?.toIntOrNull()?.let { lastMentionedDuration = it }
            params["subjects"]?.let { lastMentionedSubjects = it }
            params["course"]?.let { lastMentionedCourse = it }
            params["day"]?.let { extractDayOfWeek(it)?.let { d -> lastMentionedDayOfWeek = d } }

            val prepResult = when (actionType) {
                "START_CLASS" -> {
                    if (student != null) studentTools.prepareStartClass(student, params["duration"]?.toIntOrNull() ?: 60) else null
                }
                "CREATE_STUDENT" -> {
                    if (student != null) studentTools.prepareCreateStudent(student, params["course"] ?: "General", params["subjects"] ?: "General", params["price"]?.toDoubleOrNull() ?: 0.0) else null
                }
                "DELETE_STUDENT" -> {
                    if (student != null) studentTools.prepareDeleteStudent(student) else null
                }
                "REGISTER_PAYMENT" -> {
                    if (student != null) {
                        val type = if (params["type"] == "EFFECTIVE") com.devsusana.hometutorpro.domain.entities.PaymentType.EFFECTIVE else com.devsusana.hometutorpro.domain.entities.PaymentType.BIZUM
                        studentTools.prepareRegisterPayment(student, params["amount"]?.toDoubleOrNull() ?: 0.0, type)
                    } else null
                }
                "ADD_BALANCE" -> {
                    if (student != null) studentTools.prepareAddBalance(student, params["amount"]?.toDoubleOrNull() ?: 0.0) else null
                }
                "CANCEL_CLASS" -> {
                    if (student != null) scheduleTools.prepareCancelAction(student, extractDayOfWeek(params["day"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value, time) else null
                }
                "DELETE_SCHEDULE" -> {
                    if (student != null) scheduleTools.prepareDeleteSchedule(student, extractDayOfWeek(params["day"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value, time) else null
                }
                "CREATE_SCHEDULE" -> {
                    if (student != null) scheduleTools.prepareCreateSchedule(student, extractDayOfWeek(params["day"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value, startTime ?: "", endTime ?: "") else null
                }
                "ADD_EXTRA_CLASS" -> {
                    if (student != null) {
                        val dayOfWeek = extractDayOfWeek(params["day"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value
                        val targetDate = dateTimeProvider.getNow().toLocalDate().with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(dayOfWeek)))
                        val dateMillis = targetDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        scheduleTools.prepareAddExtraClass(student, dateMillis, targetTime ?: "", endTime ?: "")
                    } else null
                }
                "RESCHEDULE_CLASS" -> {
                    if (student != null) {
                        val fromDay = extractDayOfWeek(params["fromDay"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value
                        val toDay = extractDayOfWeek(params["toDay"] ?: "") ?: dateTimeProvider.getNow().dayOfWeek.value
                        scheduleTools.prepareRescheduleAction(student, fromDay, toDay, targetTime ?: "", fromTime)
                    } else null
                }
                "UPDATE_STUDENT_NOTES" -> {
                    if (student != null) studentTools.prepareUpdateNotesAction(student, params["notes"] ?: "") else null
                }
                else -> null
            }

            if (prepResult is SueOperationResult.Prepare.Success) {
                preparedActions.add(prepResult.action)
            } else if (prepResult is SueOperationResult.Prepare.Error) {
                return prepResult
            }
        }

        return if (preparedActions.size == 1) {
            SueOperationResult.Prepare.Success(preparedActions.first())
        } else if (preparedActions.isNotEmpty()) {
            SueOperationResult.Prepare.MultipleSuccess(preparedActions)
        } else {
            null
        }
    }

    override suspend fun detectActionIntent(query: String): SueOperationResult? {
        val lower = stripAccents(query.lowercase().trim())

        // 1. If we have a pending intent type, check if the user wants to abort or change topic
        if (lastActiveIntentType != null) {
            val abortWords = listOf("no", "nada", "olvídalo", "olvida", "déjalo", "abortar", "aborta")
            if (abortWords.any { it == lower }) {
                resetConversationContext()
                return null
            }
            if (isQuestionOrQuery(lower)) {
                lastActiveIntentType = null
            }
        }

        // 2. Extract and update context memory variables
        val matchedStudent = studentTools.extractRelevantStudent(lower)
        val matchedName = matchedStudent?.name ?: extractStudentName(lower) ?: extractStudentNameForFinance(lower) ?: extractNameAfter(lower, listOf("inicia una clase para ", "inicia clase para ", "empieza clase para ", "start class for "))
        if (matchedName != null) {
            val capitalizedName = matchedName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            lastMentionedStudentName = capitalizedName
        }

        val relativeDay = extractRelativeDayOfWeek(lower)
        val explicitDay = extractDayOfWeek(lower)
        val matchedDay = relativeDay ?: explicitDay
        if (matchedDay != null) {
            lastMentionedDayOfWeek = matchedDay
        }

        val matchedTime = extractTime(lower)
        if (matchedTime != null) {
            lastMentionedTime = matchedTime
        }

        val matchedAmount = extractAmount(lower)
        if (matchedAmount != null) {
            lastMentionedAmount = matchedAmount
        }

        val matchedDuration = extractDurationOptional(lower)
        if (matchedDuration != null) {
            lastMentionedDuration = matchedDuration
        }

        // 3. Determine the intent to process (explicit or fallback to last active intent)
        val explicitIntent = when {
            containsStartClassKeywords(lower) -> IntentType.START_CLASS
            containsCreateStudentKeywords(lower) -> IntentType.CREATE_STUDENT
            containsDeleteStudentKeywords(lower) -> IntentType.DELETE_STUDENT
            containsAddExtraClassKeywords(lower) -> IntentType.ADD_EXTRA_CLASS
            containsCreateScheduleKeywords(lower) -> IntentType.CREATE_SCHEDULE
            containsDeleteScheduleKeywords(lower) -> IntentType.DELETE_SCHEDULE
            containsCancelKeywords(lower) -> IntentType.CANCEL_CLASS
            containsRescheduleKeywords(lower) -> IntentType.RESCHEDULE_CLASS
            containsRegisterPaymentKeywords(lower) -> IntentType.REGISTER_PAYMENT
            containsAddBalanceKeywords(lower) -> IntentType.ADD_BALANCE
            containsUpdateNotesKeywords(lower) -> IntentType.UPDATE_STUDENT_NOTES

            // Read queries — most specific first to avoid catch-all collision
            containsStudentsWithBalanceKeywords(lower) -> IntentType.QUERY_STUDENTS_WITH_BALANCE
            containsStudentBalanceKeywords(lower) -> IntentType.QUERY_STUDENT_BALANCE
            containsStudentWeeklyClassesKeywords(lower) -> IntentType.QUERY_STUDENT_WEEKLY_CLASSES
            containsTodaySummaryKeywords(lower) -> IntentType.QUERY_TODAY_SUMMARY
            containsNextClassKeywords(lower) -> IntentType.QUERY_NEXT_CLASS
            containsFreeSlotKeywords(lower) -> IntentType.QUERY_FREE_SLOTS
            containsStudentCountKeywords(lower) -> IntentType.QUERY_STUDENT_COUNT
            containsStudentDetailsKeywords(lower) -> IntentType.QUERY_STUDENT_DETAILS
            containsDayScheduleKeywords(lower) -> IntentType.QUERY_DAY_SCHEDULE
            else -> null
        }

        val activeIntent = explicitIntent ?: lastActiveIntentType
        if (activeIntent == null) {
            return null
        }

        val isReadQuery = activeIntent == IntentType.QUERY_NEXT_CLASS ||
                activeIntent == IntentType.QUERY_FREE_SLOTS ||
                activeIntent == IntentType.QUERY_STUDENT_COUNT ||
                activeIntent == IntentType.QUERY_STUDENT_DETAILS ||
                activeIntent == IntentType.QUERY_STUDENT_WEEKLY_CLASSES ||
                activeIntent == IntentType.QUERY_TODAY_SUMMARY ||
                activeIntent == IntentType.QUERY_STUDENTS_WITH_BALANCE ||
                activeIntent == IntentType.QUERY_STUDENT_BALANCE ||
                activeIntent == IntentType.QUERY_DAY_SCHEDULE

        if (!isReadQuery) {
            lastActiveIntentType = activeIntent
        }

        val result = when (activeIntent) {
            IntentType.START_CLASS -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿Para qué alumno quieres iniciar la clase?"
                    )
                } else {
                    val duration = lastMentionedDuration
                    if (duration == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿De cuántos minutos será la clase con $studentName?"
                        )
                    } else {
                        studentTools.prepareStartClass(studentName, duration)
                    }
                }
            }

            IntentType.CREATE_STUDENT -> {
                val queryWithoutKeywords = lower
                    .replace("crear un estudiante", "")
                    .replace("crear un alumno", "")
                    .replace("crea un estudiante", "")
                    .replace("crea un alumno", "")
                    .replace("crear al estudiante", "")
                    .replace("crear al alumno", "")
                    .replace("crea al estudiante", "")
                    .replace("crea al alumno", "")
                    .replace("crear el estudiante", "")
                    .replace("crear el alumno", "")
                    .replace("crea el estudiante", "")
                    .replace("crea el alumno", "")
                    .replace("añadir un estudiante", "")
                    .replace("añadir un alumno", "")
                    .replace("añade un estudiante", "")
                    .replace("añade un alumno", "")
                    .replace("añadir al estudiante", "")
                    .replace("añadir al alumno", "")
                    .replace("añade al estudiante", "")
                    .replace("añade al alumno", "")
                    .replace("añadir estudiante", "")
                    .replace("añadir alumno", "")
                    .replace("añade estudiante", "")
                    .replace("añade alumno", "")
                    .replace("create student", "")
                    .replace("add student", "")
                    .trim()

                if (lastMentionedStudentName == null) {
                    val stopWords = listOf(" de ", " a ", " para ", " in ", " at ", " for ")
                    var nameCandidate = queryWithoutKeywords
                    for (stop in stopWords) {
                        val stopIdx = nameCandidate.indexOf(stop)
                        if (stopIdx >= 0) {
                            nameCandidate = nameCandidate.substring(0, stopIdx).trim()
                        }
                    }
                    if (nameCandidate.isNotBlank() && !nameCandidate.contains(Regex("""\d"""))) {
                        lastMentionedStudentName = nameCandidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
                }

                val price = extractAmount(lower)
                if (price != null) {
                    lastMentionedPrice = price
                }

                val subjectKeywords = listOf(" de ", " in ", " para ", " for ")
                for (kw in subjectKeywords) {
                    val idx = lower.indexOf(kw)
                    if (idx >= 0) {
                        val rest = query.substring(idx + kw.length).trim()
                        val stopWords = listOf(" a ", " por ", " at ", " for ")
                        var end = rest.length
                        for (stop in stopWords) {
                            val stopIdx = rest.lowercase().indexOf(stop)
                            if (stopIdx in 1 until end) end = stopIdx
                        }
                        val candidate = rest.substring(0, end).trim()
                        if (candidate.isNotBlank() && !candidate.contains(Regex("""\d""")) && 
                            !listOf("lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo").any { it in candidate.lowercase() }) {
                            lastMentionedSubjects = candidate
                            break
                        }
                    }
                }

                val courseKeywords = listOf("eso", "bachillerato", "bach", "primaria", "secundaria", "universidad")
                val courseMatch = courseKeywords.find { it in lower }
                if (courseMatch != null) {
                    lastMentionedCourse = courseMatch
                }

                val studentName = lastMentionedStudentName
                if (studentName.isNullOrBlank()) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.UNKNOWN,
                        "¿Cómo se llama el nuevo alumno?"
                    )
                } else {
                    val priceVal = lastMentionedPrice
                    if (priceVal == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Cuál será el precio por hora de $studentName?"
                        )
                    } else {
                        val finalCourse = lastMentionedCourse ?: "Other"
                        val finalSubjects = lastMentionedSubjects ?: "General"
                        studentTools.prepareCreateStudent(studentName, finalCourse, finalSubjects, priceVal)
                    }
                }
            }

            IntentType.DELETE_STUDENT -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres eliminar el perfil?"
                    )
                } else {
                    studentTools.prepareDeleteStudent(studentName)
                }
            }

            IntentType.ADD_EXTRA_CLASS -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿Para qué alumno quieres programar la clase extra?"
                    )
                } else {
                    val dayOfWeek = lastMentionedDayOfWeek ?: dateTimeProvider.getNow().dayOfWeek.value
                    val targetDate = dateTimeProvider.getNow().toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)))
                    val dateMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    val times = extractTwoTimes(lower) ?: lastMentionedTime?.let { lastTime ->
                        val parts = lastTime.split(":")
                        val h = parts[0].toInt()
                        val m = parts[1].toInt()
                        val endH = (h + 1) % 24
                        val endTime = "%02d:%02d".format(endH, m)
                        Pair(lastTime, endTime)
                    }
                    if (times == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿A qué hora quieres programar la clase extra de $studentName?"
                        )
                    } else {
                        scheduleTools.prepareAddExtraClass(studentName, dateMillis, times.first, times.second)
                    }
                }
            }

            IntentType.CREATE_SCHEDULE -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿Para qué alumno deseas configurar el horario?"
                    )
                } else {
                    val dayOfWeek = lastMentionedDayOfWeek
                    if (dayOfWeek == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Qué día de la semana será la clase de $studentName?"
                        )
                    } else {
                        val times = extractTwoTimes(lower) ?: lastMentionedTime?.let { lastTime ->
                            val parts = lastTime.split(":")
                            val h = parts[0].toInt()
                            val m = parts[1].toInt()
                            val endH = (h + 1) % 24
                            val endTime = "%02d:%02d".format(endH, m)
                            Pair(lastTime, endTime)
                        }
                        if (times == null) {
                            val dayName = when(dayOfWeek) {
                                1 -> "lunes"
                                2 -> "martes"
                                3 -> "miércoles"
                                4 -> "jueves"
                                5 -> "viernes"
                                6 -> "sábado"
                                7 -> "domingo"
                                else -> "ese día"
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

            IntentType.DELETE_SCHEDULE -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres eliminar el horario?"
                    )
                } else {
                    val dayOfWeek = lastMentionedDayOfWeek
                    if (dayOfWeek == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Qué día de la semana es el horario que quieres eliminar?"
                        )
                    } else {
                        val time = lastMentionedTime
                        scheduleTools.prepareDeleteSchedule(studentName, dayOfWeek, time)
                    }
                }
            }

            IntentType.CANCEL_CLASS -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres cancelar la clase?"
                    )
                } else {
                    var dayOfWeek = lastMentionedDayOfWeek
                    val time = lastMentionedTime

                    if (dayOfWeek == null) {
                        val schedules = scheduleTools.getSchedulesByStudentName(studentName)
                        if (schedules.isEmpty()) {
                            SueOperationResult.Prepare.Error(
                                SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                                "No he encontrado ninguna clase programada para $studentName."
                            )
                        } else if (schedules.size == 1) {
                            dayOfWeek = schedules.first().dayOfWeek
                            scheduleTools.prepareCancelAction(studentName, dayOfWeek, time)
                        } else {
                            SueOperationResult.Prepare.Error(
                                SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                                "¿Qué día es la clase de $studentName que quieres cancelar?"
                            )
                        }
                    } else {
                        scheduleTools.prepareCancelAction(studentName, dayOfWeek, time)
                    }
                }
            }

            IntentType.RESCHEDULE_CLASS -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres mover la clase?"
                    )
                } else {
                    val days = extractTwoDaysOfWeek(lower)
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
                                if (isTargetDay(lower, singleDay)) {
                                    toDay = singleDay
                                } else {
                                    fromDay = singleDay
                                }
                            } else {
                                toDay = singleDay
                            }
                        }

                        if (fromDay == null) {
                            if (schedules.size == 1) {
                                fromDay = schedules.first().dayOfWeek
                            } else {
                                val fallbackDay = lastMentionedDayOfWeek ?: dateTimeProvider.getNow().dayOfWeek.value
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
                            SueOperationResult.Prepare.Error(
                                SueOperationResult.ErrorType.CLASS_NOT_FOUND,
                                "¿Qué día es la clase de $studentName que quieres mover?"
                            )
                        } else {
                            if (toDay == null) {
                                toDay = fromDay
                            }

                            val times = extractAllTimesInQuery(lower)
                            var fromTime: String? = null
                            var targetTime: String? = null

                            if (times.size >= 2) {
                                fromTime = times[0]
                                targetTime = times[1]
                            } else if (times.size == 1) {
                                val singleTime = times[0]
                                val daySchedules = schedules.filter { it.dayOfWeek == fromDay }
                                val hasClassAtTime = daySchedules.any {
                                    it.startTime == singleTime || it.startTime.substringBefore(":") == singleTime.substringBefore(":")
                                }
                                if (hasClassAtTime) {
                                    fromTime = singleTime
                                    targetTime = singleTime
                                } else {
                                    fromTime = null
                                    targetTime = singleTime
                                }
                            } else {
                                fromTime = null
                                targetTime = lastMentionedTime
                            }

                            if (targetTime == null) {
                                val daySchedules = schedules.filter { it.dayOfWeek == fromDay }
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

            IntentType.REGISTER_PAYMENT -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres registrar el pago?"
                    )
                } else {
                    val amount = lastMentionedAmount
                    if (amount == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿De cuánto es el pago de $studentName?"
                        )
                    } else {
                        val paymentType = if (lower.contains("bizum")) PaymentType.BIZUM else PaymentType.EFFECTIVE
                        studentTools.prepareRegisterPayment(studentName, amount, paymentType)
                    }
                }
            }

            IntentType.ADD_BALANCE -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿A qué alumno le quieres sumar saldo?"
                    )
                } else {
                    val amount = lastMentionedAmount
                    if (amount == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Cuánto saldo deseas sumarle a $studentName?"
                        )
                    } else {
                        studentTools.prepareAddBalance(studentName, amount)
                    }
                }
            }

            IntentType.UPDATE_STUDENT_NOTES -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.Prepare.Error(
                        SueOperationResult.ErrorType.STUDENT_NOT_FOUND,
                        "¿De qué alumno quieres actualizar las notas?"
                    )
                } else {
                    val noteText = extractNotesText(lower)
                    if (noteText == null) {
                        SueOperationResult.Prepare.Error(
                            SueOperationResult.ErrorType.UNKNOWN,
                            "¿Qué notas o apuntes quieres añadir para $studentName?"
                        )
                    } else {
                        studentTools.prepareUpdateNotesAction(studentName, noteText)
                    }
                }
            }

            // READ QUERIES IMPLEMENTATION
            IntentType.QUERY_NEXT_CLASS -> {
                val nextClassResult = scheduleTools.getNextClass(lastMentionedStudentName)
                if (nextClassResult is SueOperationResult.NextClass && nextClassResult.schedule != null) {
                    val s = nextClassResult.schedule
                    lastMentionedStudentName = s.studentName
                    lastMentionedDayOfWeek = s.dayOfWeek
                    lastMentionedTime = s.startTime
                }
                nextClassResult
            }

            IntentType.QUERY_FREE_SLOTS -> {
                scheduleTools.getFreeSlots()
            }

            IntentType.QUERY_STUDENT_COUNT -> {
                studentTools.getActiveStudentCount()
            }

            IntentType.QUERY_STUDENT_DETAILS -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.StudentDetails(query, emptyList())
                } else {
                    val detailsResult = studentTools.searchStudent(studentName)
                    if (detailsResult is SueOperationResult.StudentDetails && detailsResult.students.isNotEmpty()) {
                        val s = detailsResult.students.first()
                        lastMentionedStudentName = s.name
                        lastMentionedAmount = s.pendingBalance
                    }
                    detailsResult
                }
            }

            IntentType.QUERY_DAY_SCHEDULE -> {
                val day = lastMentionedDayOfWeek ?: dateTimeProvider.getNow().dayOfWeek.value
                val timeFilter = extractTime(lower) ?: extractTimeOfDayFilter(lower)
                scheduleTools.getScheduleForDay(day, timeFilter)
            }

            IntentType.QUERY_STUDENT_WEEKLY_CLASSES -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.ReadSuccess("No he identificado de qué alumno quieres saber las clases. ¿Puedes decirme su nombre?")
                } else {
                    scheduleTools.getWeeklyClassesForStudent(studentName)
                }
            }

            IntentType.QUERY_STUDENTS_WITH_BALANCE -> {
                studentTools.getStudentsWithBalance()
            }

            IntentType.QUERY_STUDENT_BALANCE -> {
                val studentName = lastMentionedStudentName
                if (studentName == null) {
                    SueOperationResult.ReadSuccess("¿De qué alumno quieres saber el saldo pendiente?")
                } else {
                    val details = studentTools.searchStudent(studentName)
                    if (details is SueOperationResult.StudentDetails && details.students.isNotEmpty()) {
                        val student = details.students.first()
                        lastMentionedStudentName = student.name
                        lastMentionedAmount = student.pendingBalance // Memorize pending balance amount!
                        if (student.pendingBalance <= 0.0) {
                            SueOperationResult.ReadSuccess("${student.name} está al corriente de pago. No tiene ningún saldo pendiente.")
                        } else {
                            SueOperationResult.ReadSuccess("${student.name} tiene un saldo pendiente de ${"%.2f".format(student.pendingBalance)} €.")
                        }
                    } else {
                        SueOperationResult.ReadSuccess("No he encontrado ningún alumno llamado \"$studentName\".")
                    }
                }
            }

            IntentType.QUERY_TODAY_SUMMARY -> {
                val today = dateTimeProvider.getNow().dayOfWeek.value
                val todayResult = scheduleTools.getScheduleForDay(today, null)
                val scheduleText = com.devsusana.hometutorpro.presentation.sue.SueResponseFormatter.format(todayResult)

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

                SueOperationResult.ReadSuccess(scheduleText + debtAlert)
            }
        }

        if (result is SueOperationResult.Prepare.Success) {
            lastActiveIntentType = null
        }
        return result
    }

    private fun containsStudentCountKeywords(query: String) =
        listOf("cuántos alumnos", "cuantos alumnos", "número de alumnos", "numero de alumnos", "cantidad de alumnos").any { it in query }

    private fun containsStudentDetailsKeywords(query: String) =
        listOf("información de", "ficha de", "datos de", "detalles de", "info de").any { it in query }

    /**
     * Detects queries about how many weekly sessions a specific student has.
     * Must be checked BEFORE [containsDayScheduleKeywords] to avoid the word "clases"
     * being mistakenly routed to the day schedule handler.
     */
    private fun containsStudentWeeklyClassesKeywords(query: String): Boolean {
        val hasQuantityWord = listOf("cuántas", "cuantas", "cuántos", "cuantos").any { it in query }
        val hasClassWord = listOf("clase", "clases", "sesión", "sesiones", "veces").any { it in query }
        val hasStudentReference = listOf("de ", "con ", "tiene ", "tengo de ", "tengo con ").any { it in query }
        return hasQuantityWord && hasClassWord && hasStudentReference
    }

    private fun containsDayScheduleKeywords(query: String) =
        listOf("clase", "clases", "horario", "lunes", "martes", "miércoles", "miercoles", "jueves", "viernes", "sábado", "sabado", "domingo", "hoy", "mañana").any { it in query }

    /**
     * Detects queries asking which students owe money (global view, no specific student).
     * Must be evaluated BEFORE [containsStudentBalanceKeywords] which is more specific.
     */
    private fun containsStudentsWithBalanceKeywords(query: String) =
        listOf(
            "quién me debe", "quien me debe",
            "quiénes me deben", "quienes me deben",
            "quién debe", "quien debe",
            "tienen deuda", "deben dinero",
            "pendiente de pago", "sin pagar",
            "no han pagado", "no ha pagado",
            "who owes me", "who hasn't paid"
        ).any { it in query }

    /**
     * Detects queries about a specific student's outstanding balance.
     * Requires the presence of a balance-specific phrase; student name is resolved
     * from [lastMentionedStudentName] in the handler.
     */
    private fun containsStudentBalanceKeywords(query: String) =
        listOf(
            "cuánto me debe", "cuanto me debe",
            "cuánto debe", "cuanto debe",
            "qué me debe", "que me debe",
            "saldo de", "deuda de", "balance de",
            "cuánto tiene pendiente", "cuanto tiene pendiente",
            "how much does", "how much owes"
        ).any { it in query }

    /**
     * Detects queries requesting a holistic summary of today's work session,
     * including schedule AND debt alerts for today's students.
     * Must be evaluated BEFORE [containsDayScheduleKeywords] to take priority.
     */
    private fun containsTodaySummaryKeywords(query: String) =
        listOf(
            "resumen de hoy", "resumen hoy",
            "cómo está mi día", "como esta mi dia",
            "cómo va mi día", "como va mi dia",
            "qué tengo hoy", "que tengo hoy",
            "summary today", "today's summary"
        ).any { it in query }
}
