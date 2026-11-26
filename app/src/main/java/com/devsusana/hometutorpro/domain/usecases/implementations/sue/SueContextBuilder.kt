package com.devsusana.hometutorpro.domain.usecases.implementations.sue

import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.implementations.ScheduleTools
import com.devsusana.hometutorpro.domain.usecases.implementations.StudentTools
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * Builds localized LLM system prompts and gathers RAG context from domain tools.
 */
class SueContextBuilder @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools,
    private val dateTimeProvider: DateTimeProvider,
    private val authRepository: AuthRepository,
    private val entityExtractor: SueEntityExtractor,
    private val conversationContext: SueConversationContext
) {

    private val scheduleGatherer = SueScheduleContextGatherer(
        scheduleTools = scheduleTools,
        dateTimeProvider = dateTimeProvider,
        entityExtractor = entityExtractor,
        conversationContext = conversationContext
    )

    private val studentGatherer = SueStudentContextGatherer(
        studentTools = studentTools,
        scheduleTools = scheduleTools,
        conversationContext = conversationContext
    )

    companion object {
        /**
         * Builds the Sue system prompt with a locale-aware language instruction.
         *
         * @param locale Active user locale used to enforce response language.
         * @param isQuestion True when the query is an informational question rather than an action command.
         * @return Localized system prompt string for on-device LLM inference.
         */
        fun buildSystemPrompt(locale: Locale, isQuestion: Boolean = false): String =
            SueSystemPromptProvider.buildSystemPrompt(locale, isQuestion)
    }

    /**
     * Builds the full Gemma prompt injecting temporal context, working hours, recent history,
     * and locally retrieved RAG data for the current query.
     *
     * @param userQuery Natural language query from the professor.
     * @param history Recent `(user, sue)` conversation turns.
     * @return Formatted prompt string ready for on-device LLM inference.
     */
    suspend fun buildPromptWithContext(
        userQuery: String,
        history: List<Pair<String, String>>
    ): String {
        val locale = dateTimeProvider.getLocale()
        val isQuestion = entityExtractor.isQuestionOrQuery(userQuery)

        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, HH:mm", locale)
        val currentDateTime = dateTimeProvider.getNow().format(formatter)

        val professor = authRepository.currentUser.value
            ?: withTimeoutOrNull(2000L) {
                authRepository.currentUser.first { it != null }
            }
        val workingStart = professor?.workingStartTime?.takeIf { it.isNotBlank() } ?: "08:00"
        val workingEnd = professor?.workingEndTime?.takeIf { it.isNotBlank() } ?: "23:00"

        val toolContext = gatherRelevantContext(userQuery, workingStart, workingEnd)

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

    /**
     * Gathers relevant local Room data (students, schedules, balances, free slots) to ground
     * the LLM response without hallucination.
     *
     * @param query Raw user query string.
     * @param workingStart Professor's working start time in `HH:mm` format.
     * @param workingEnd Professor's working end time in `HH:mm` format.
     * @return Structured context block for prompt injection, or an empty string if none applies.
     */
    suspend fun gatherRelevantContext(
        query: String,
        workingStart: String = "08:00",
        workingEnd: String = "23:00"
    ): String {
        val lowerQuery = entityExtractor.stripAccents(query.lowercase())

        val appLocale = dateTimeProvider.getLocale()
        val matchedStudent = studentTools.extractRelevantStudent(lowerQuery)
        val studentName = matchedStudent?.name
            ?: entityExtractor.extractStudentName(lowerQuery)
            ?: entityExtractor.extractStudentNameForFinance(lowerQuery)
        if (studentName != null) {
            val capitalizedName = studentName.trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(appLocale) else it.toString()
            }
            conversationContext.lastMentionedStudentName = capitalizedName
        }

        val relativeDay = entityExtractor.extractRelativeDayOfWeek(lowerQuery)
        val explicitDay = entityExtractor.extractDayOfWeek(lowerQuery)
        val day = relativeDay ?: explicitDay
        if (day != null) {
            conversationContext.lastMentionedDayOfWeek = day
        }

        entityExtractor.extractTime(lowerQuery)?.let {
            conversationContext.lastMentionedTime = it
        }

        return buildString {
            append(scheduleGatherer.gatherScheduleContext(lowerQuery, day, studentName, workingStart, workingEnd))
            studentGatherer.appendStudentContext(this, lowerQuery)
        }
    }
}
