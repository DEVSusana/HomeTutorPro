package com.devsusana.hometutorpro.core.sue

import com.devsusana.hometutorpro.core.sue.tools.ScheduleTools
import com.devsusana.hometutorpro.core.sue.tools.StudentTools
import com.devsusana.hometutorpro.domain.entities.PaymentType
import com.devsusana.hometutorpro.domain.entities.SueOperationResult
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
class SueAgent @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools
) {

    companion object {

        /**
         * Builds the Sue system prompt with a locale-aware language instruction.
         *
         * Reads [Locale.getDefault()] at call time so that the language and regional
         * variant (e.g. es-ES vs en-GB) always matches the device’s current setting.
         */
        fun buildSystemPrompt(): String {
            val locale = Locale.getDefault()
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

            return """
                Eres Sue, la asistente inteligente de HomeTutorPro. Tu rol es ayudar a profesores particulares a gestionar su trabajo de forma eficiente.

                Tu personalidad:
                - Eres profesional, amable y concisa.
                - Evitas respuestas largas — sé directa y útil.
                - Si no puedes ayudar con algo, dilo claramente.
                - NO te describes a ti misma como IA. Compórtate como la asistente Sue de forma natural.
                - $languageInstruction

                Tus capacidades:
                - Consultar la lista de alumnos (nombres, asignaturas, cursos, precios, saldos).
                - Buscar alumnos específicos por nombre.
                - Consultar los horarios semanales y la próxima clase.
                - Informar sobre saldos pendientes.
                - Contar alumnos activos.
                - Cancelar o mover clases puntuales (solo la ocurrencia indicada).
                - Mostrar huecos libres en la semana actual.

                Limitaciones:
                - Solo puedes CONSULTAR o ejecutar acciones puntuales (cancelar/mover UNA clase).
                - No puedes crear ni eliminar horarios permanentes.
                - Solo conoces los datos del profesor que está usando la app.
            """.trimIndent()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Schedule management intent detection
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Detects an action intent (schedule or financial) from [query] and prepares the
     * corresponding [SueOperationResult.Prepare] with all data pre-resolved.
     *
     * Returns null if no action intent is detected, meaning the
     * caller should fall through to normal LLM processing.
     *
     * Returns [SueOperationResult.Prepare] with the resolved action and confirmation data,
     * or with an error descriptor if the intent was detected but the data could not be resolved.
     */
    suspend fun detectActionIntent(query: String): SueOperationResult.Prepare? {
        val lower = query.lowercase()

        return when {
            containsCancelKeywords(lower) -> {
                val studentName = extractStudentName(lower) ?: return null
                val dayOfWeek = extractDayOfWeek(lower) ?: return null
                scheduleTools.prepareCancelAction(studentName, dayOfWeek)
            }

            containsRescheduleKeywords(lower) -> {
                val studentName = extractStudentName(lower) ?: return null
                val days = extractTwoDaysOfWeek(lower)
                val fromDay = days.first ?: return null
                val toDay = days.second ?: return null
                val newTime = extractTime(lower) ?: return null
                scheduleTools.prepareRescheduleAction(studentName, fromDay, toDay, newTime)
            }

            containsRegisterPaymentKeywords(lower) -> {
                val studentName = extractStudentNameForFinance(lower) ?: return null
                val amount = extractAmount(lower) ?: return null
                val paymentType = if (lower.contains("bizum")) PaymentType.BIZUM else PaymentType.EFFECTIVE
                studentTools.prepareRegisterPayment(studentName, amount, paymentType)
            }

            containsAddBalanceKeywords(lower) -> {
                val studentName = extractStudentNameForFinance(lower) ?: return null
                val amount = extractAmount(lower) ?: return null
                studentTools.prepareAddBalance(studentName, amount)
            }

            else -> null
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Normal LLM prompt building
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Processes a user query by routing it to the appropriate tools and building
     * a context-enriched prompt for the LLM.
     *
     * This is a `suspend` function because [gatherRelevantContext] calls suspend
     * tool methods that perform database I/O.
     */
    suspend fun buildPromptWithContext(userQuery: String): String {
        val toolContext = gatherRelevantContext(userQuery)
        val locale = Locale.getDefault()

        // Format date/time in the device locale so it reads naturally
        val currentDateTime = java.text.SimpleDateFormat(
            "EEEE, d MMMM yyyy, HH:mm", locale
        ).format(java.util.Date())

        return buildString {
            appendLine(buildSystemPrompt())
            appendLine()
            appendLine("--- TEMPORAL CONTEXT ---")
            appendLine("Current date/time: $currentDateTime")
            appendLine("--- END OF TEMPORAL CONTEXT ---")
            appendLine()
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
        val lowerQuery = query.lowercase()

        return buildString {
            // 1. Schedule queries
            if (containsScheduleKeywords(lowerQuery)) {
                if (containsNextClassKeywords(lowerQuery)) {
                    appendLine(formatResult(scheduleTools.getNextClass()))
                } else if (containsFreeSlotKeywords(lowerQuery)) {
                    appendLine(formatResult(scheduleTools.getFreeSlots()))
                } else {
                    val relativeDay = extractRelativeDayOfWeek(lowerQuery)
                    val explicitDay = extractDayOfWeek(lowerQuery)
                    val targetDay = relativeDay ?: explicitDay

                    if (targetDay != null) {
                        val timeFilter = extractTimeOfDayFilter(lowerQuery)
                        appendLine(formatResult(scheduleTools.getScheduleForDay(targetDay, timeFilter)))
                    } else {
                        appendLine(formatResult(scheduleTools.getWeeklySchedule()))
                    }
                }
            }

            // 2. Student queries — mutually exclusive branches to avoid data dumping
            val matchedStudent = studentTools.extractRelevantStudent(lowerQuery)
            val hasSchedule = containsScheduleKeywords(lowerQuery)

            when {
                matchedStudent != null ->
                    appendLine(formatResult(studentTools.searchStudent(matchedStudent.name)))

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

    private fun containsFreeSlotKeywords(query: String) =
        listOf(
            "hueco libre", "huecos libres", "día libre", "días libres",
            "disponible", "disponibles", "libre", "libres", "espacio libre",
            "free slot", "available"
        ).any { it in query }

    private fun containsCancelKeywords(query: String) =
        listOf("cancela", "cancelar", "anula", "anular", "quitar la clase", "quita la clase",
               "cancel", "remove class", "delete class")
            .any { it in query }

    private fun containsRescheduleKeywords(query: String) =
        listOf("mueve", "mover", "cambia", "cambiar", "pasa", "pasar", "traslada", "trasladar",
               "move", "change", "reschedule", "postpone")
            .any { it in query }

    private fun containsRegisterPaymentKeywords(query: String) =
        listOf("registra un pago", "registrar un pago", "ha pagado", "abonó", "abono", "pagó", "pago de",
               "register payment", "made a payment", "paid", "payment of")
            .any { it in query }

    private fun containsAddBalanceKeywords(query: String) =
        listOf("suma saldo", "sumar saldo", "añade saldo", "añadir saldo", "suma a la deuda", "añade a la deuda", "súmale", "sumale",
               "add balance", "add to debt", "add to balance", "add to the debt")
            .any { it in query }

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
        val today = java.time.LocalDate.now().dayOfWeek.value
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
        val dayMappings = mapOf(
            "lunes" to 1, "monday" to 1,
            "martes" to 2, "tuesday" to 2,
            "miércoles" to 3, "miercoles" to 3, "wednesday" to 3,
            "jueves" to 4, "thursday" to 4,
            "viernes" to 5, "friday" to 5,
            "sábado" to 6, "sabado" to 6, "saturday" to 6,
            "domingo" to 7, "sunday" to 7
        )
        val found = dayMappings.entries
            .filter { it.key in query }
            .sortedBy { query.indexOf(it.key) }
            .distinctBy { it.value }
        return Pair(found.getOrNull(0)?.value, found.getOrNull(1)?.value)
    }

    /**
     * Attempts to extract a student name from the query by matching against
     * any known student's first name. Returns the matched full name or null.
     */
    private fun extractStudentName(query: String): String? {
        // Delegate to StudentTools which already loads all students
        // We reuse the extractRelevantStudentContext logic but only need the name
        // We check if any known name keyword appears in the reschedule/cancel phrase
        val commonConnectors = listOf(
            "cancela la clase de ", "cancelar la clase de ", "anula la clase de ",
            "anular la clase de ", "mueve la clase de ", "mover la clase de ",
            "cambia la clase de ", "cambiar la clase de ", "pasa la clase de ",
            "traslada la clase de ", "la clase de ",
            "cancel class for ", "cancel the class of ", "move class for ",
            "reschedule class for ", "change class for ", "the class of "
        )
        for (connector in commonConnectors) {
            val idx = query.indexOf(connector)
            if (idx >= 0) {
                val rest = query.substring(idx + connector.length).trim()
                // Take up to the next preposition or day keyword
                val stopWords = listOf(" del ", " de ", " el ", " al ", " a ", " en ",
                                       " from ", " of ", " the ", " to ", " at ", " in ", " on ")
                var end = rest.length
                for (stop in stopWords) {
                    val stopIdx = rest.indexOf(stop)
                    if (stopIdx in 1 until end) end = stopIdx
                }
                val name = rest.substring(0, end).trim()
                if (name.isNotBlank()) return name
            }
        }
        return null
    }

    /**
     * Attempts to extract a time in "HH:mm" format from the query.
     * Recognises patterns like "a las 11:00", "a las 11", "11:00", "11h".
     */
    private fun extractTime(query: String): String? {
        // Pattern: HH:mm
        val colonPattern = Regex("""\b(\d{1,2}):(\d{2})\b""")
        colonPattern.find(query)?.let { match ->
            val h = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            if (h in 0..23 && m in 0..59) return "%02d:%02d".format(h, m)
        }
        // Pattern: "a las NN" (whole hours)
        val hourPattern = Regex("""a las (\d{1,2})\b""")
        hourPattern.find(query)?.let { match ->
            val h = match.groupValues[1].toInt()
            if (h in 0..23) return "%02d:00".format(h)
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
            if (name.isNotBlank() && !name.first().isDigit()) return name
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
        return null
    }
}
