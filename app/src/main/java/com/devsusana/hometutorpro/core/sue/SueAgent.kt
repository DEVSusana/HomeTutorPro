package com.devsusana.hometutorpro.core.sue

import com.devsusana.hometutorpro.core.sue.tools.ScheduleTools
import com.devsusana.hometutorpro.core.sue.tools.StudentTools
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration and orchestration layer for the Sue AI agent.
 *
 * This class:
 * 1. Builds the system prompt with the agent's personality and instructions.
 * 2. Formats tool results into the LLM context.
 * 3. Determines which tools to call based on the user's query.
 *
 * In Phase 5, this will be connected to the Koog agent framework.
 * Currently provides a simplified rule-based routing to the tools.
 */
@Singleton
class SueAgent @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools
) {

    companion object {
        /**
         * System prompt that defines Sue's personality and capabilities.
         * Written in Spanish since the target users are Spanish-speaking tutors.
         */
        const val SYSTEM_PROMPT = """
Eres Sue, la asistente inteligente de HomeTutorPro. Tu rol es ayudar a profesores 
particulares a gestionar su trabajo de forma eficiente.

Tu personalidad:
- Eres profesional, amable y concisa.
- Evitas respuestas largas — sé directa y útil.
- Si no puedes ayudar con algo, dilo claramente.
- OMITES descripciones sobre ti misma. NO debes decir que eres una inteligencia artificial o un modelo de lenguaje. Compórtate como la asistente Sue de forma natural.

Tus capacidades:
- Consultar la lista de alumnos (nombres, asignaturas, cursos, precios, saldos).
- Buscar alumnos específicos por nombre.
- Consultar los horarios semanales.
- Informar sobre saldos pendientes.
- Contar alumnos activos.

Limitaciones:
- Solo puedes CONSULTAR datos. No puedes crear, modificar o eliminar datos.
- No tienes acceso a internet ni a información externa.
- Solo conoces los datos del profesor que está usando la app.
"""
    }

    /**
     * Processes a user query by routing it to the appropriate tools
     * and building a context-enriched prompt for the LLM.
     *
     * @param userQuery The transcribed text from the user.
     * @return A formatted prompt with system instructions and tool results.
     */
    fun buildPromptWithContext(userQuery: String): String {
        val toolContext = gatherRelevantContext(userQuery)
        val currentLanguage = Locale.getDefault().language
        val languageInstruction = if (currentLanguage == "en") {
            "Respond concisely and usefully in English:"
        } else {
            "Responde de forma concisa y útil en español:"
        }
        
        return buildString {
            appendLine(SYSTEM_PROMPT.trimIndent())
            appendLine()
            if (toolContext.isNotBlank()) {
                appendLine("--- DATOS DISPONIBLES ---")
                appendLine(toolContext)
                appendLine("--- FIN DE DATOS ---")
                appendLine()
            }
            appendLine("Pregunta del usuario: $userQuery")
            appendLine()
            appendLine(languageInstruction)
        }
    }

    /**
     * Simple keyword-based routing to determine which tools to call.
     * In Phase 5, Koog's agent framework will handle this routing
     * automatically through tool descriptions.
     */
    private fun gatherRelevantContext(query: String): String {
        val lowerQuery = query.lowercase()

        return buildString {
            // Schedule queries
            if (containsScheduleKeywords(lowerQuery)) {
                val dayOfWeek = extractDayOfWeek(lowerQuery)
                if (dayOfWeek != null) {
                    appendLine(scheduleTools.getScheduleForDay(dayOfWeek))
                } else {
                    appendLine(scheduleTools.getWeeklySchedule())
                }
            }

            // Student-related queries (mutually exclusive to avoid data dumping)
            if (containsCountKeywords(lowerQuery)) {
                appendLine(studentTools.getActiveStudentCount())
            } else if (containsBalanceKeywords(lowerQuery)) {
                appendLine(studentTools.getStudentsWithBalance())
            } else if (containsStudentKeywords(lowerQuery)) {
                appendLine(studentTools.getAllStudentsSummary())
            }

            // If no specific keyword matched, provide a general overview
            if (isEmpty()) {
                appendLine(studentTools.getActiveStudentCount())
                appendLine(studentTools.getAllStudentsSummary())
            }
        }
    }

    private fun containsScheduleKeywords(query: String): Boolean {
        val keywords = listOf(
            "horario", "schedule", "clase", "class",
            "lunes", "martes", "miércoles", "jueves", "viernes",
            "sábado", "domingo", "monday", "tuesday", "wednesday",
            "thursday", "friday", "saturday", "sunday",
            "semana", "week", "hoy", "today", "mañana", "tomorrow"
        )
        return keywords.any { it in query }
    }

    private fun containsStudentKeywords(query: String): Boolean {
        val keywords = listOf(
            "alumno", "alumnos", "estudiante", "estudiantes",
            "student", "students", "quién", "quien", "lista", "list"
        )
        return keywords.any { it in query }
    }

    private fun containsBalanceKeywords(query: String): Boolean {
        val keywords = listOf(
            "saldo", "balance", "deuda", "debt", "dinero", "money",
            "cobrar", "pagar", "pago", "payment", "pendiente", "pending",
            "debe", "owe"
        )
        return keywords.any { it in query }
    }

    private fun containsCountKeywords(query: String): Boolean {
        val keywords = listOf(
            "cuántos", "cuantos", "how many", "count",
            "total", "número", "numero", "number",
            "cuénta", "cuenta", "dime los"
        )
        return keywords.any { it in query }
    }

    /**
     * Extracts the day of week (1=Monday..7=Sunday) from a query string.
     * Returns null if no specific day is mentioned.
     */
    private fun extractDayOfWeek(query: String): Int? {
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
}
