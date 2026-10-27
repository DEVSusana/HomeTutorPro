package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.SueOperationResult
import com.devsusana.hometutorpro.domain.entities.SuePendingAction
import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.repository.DateTimeProvider
import com.devsusana.hometutorpro.domain.usecases.ISueAgent
import com.devsusana.hometutorpro.domain.usecases.implementations.sue.SueActionParser
import com.devsusana.hometutorpro.domain.usecases.implementations.sue.SueContextBuilder
import com.devsusana.hometutorpro.domain.usecases.implementations.sue.SueConversationContext
import com.devsusana.hometutorpro.domain.usecases.implementations.sue.SueEntityExtractor
import com.devsusana.hometutorpro.domain.usecases.implementations.sue.SueRuleIntentDetector
import java.util.Locale
import javax.inject.Inject

/**
 * Orchestration layer and facade for the Sue AI agent.
 *
 * Coordinates intent detection, entity extraction, context building, and action parsing
 * by delegating to dedicated single-responsibility components in the `sue` package.
 */
class SueAgentImpl @Inject constructor(
    private val studentTools: StudentTools,
    private val scheduleTools: ScheduleTools,
    private val dateTimeProvider: DateTimeProvider,
    private val authRepository: AuthRepository,
    private val conversationContext: SueConversationContext,
    private val entityExtractor: SueEntityExtractor,
    private val contextBuilder: SueContextBuilder,
    private val actionParser: SueActionParser,
    private val ruleIntentDetector: SueRuleIntentDetector
) : ISueAgent {

    /**
     * Secondary constructor for direct instantiation in unit tests and legacy callers.
     */
    constructor(
        studentTools: StudentTools,
        scheduleTools: ScheduleTools,
        dateTimeProvider: DateTimeProvider,
        authRepository: AuthRepository
    ) : this(
        studentTools = studentTools,
        scheduleTools = scheduleTools,
        dateTimeProvider = dateTimeProvider,
        authRepository = authRepository,
        conversationContext = SueConversationContext(),
        entityExtractor = SueEntityExtractor(dateTimeProvider, authRepository)
    )

    private constructor(
        studentTools: StudentTools,
        scheduleTools: ScheduleTools,
        dateTimeProvider: DateTimeProvider,
        authRepository: AuthRepository,
        conversationContext: SueConversationContext,
        entityExtractor: SueEntityExtractor
    ) : this(
        studentTools = studentTools,
        scheduleTools = scheduleTools,
        dateTimeProvider = dateTimeProvider,
        authRepository = authRepository,
        conversationContext = conversationContext,
        entityExtractor = entityExtractor,
        contextBuilder = SueContextBuilder(
            studentTools = studentTools,
            scheduleTools = scheduleTools,
            dateTimeProvider = dateTimeProvider,
            authRepository = authRepository,
            entityExtractor = entityExtractor,
            conversationContext = conversationContext
        ),
        actionParser = SueActionParser(
            studentTools = studentTools,
            scheduleTools = scheduleTools,
            dateTimeProvider = dateTimeProvider,
            entityExtractor = entityExtractor,
            conversationContext = conversationContext
        ),
        ruleIntentDetector = SueRuleIntentDetector(
            studentTools = studentTools,
            scheduleTools = scheduleTools,
            dateTimeProvider = dateTimeProvider,
            authRepository = authRepository,
            entityExtractor = entityExtractor,
            conversationContext = conversationContext
        )
    )

    companion object {
        /**
         * Builds the Sue system prompt with a locale-aware language instruction.
         *
         * @param locale The locale to format instructions for.
         * @param isQuestion Whether the prompt is tailored for answering a user question.
         */
        fun buildSystemPrompt(locale: Locale, isQuestion: Boolean = false): String =
            SueContextBuilder.buildSystemPrompt(locale, isQuestion)
    }

    override fun resetConversationContext() {
        conversationContext.reset()
    }

    override fun preserveContextForConflict(action: SuePendingAction) {
        conversationContext.preserveContextForConflict(action)
    }

    override suspend fun buildPromptWithContext(
        userQuery: String,
        history: List<Pair<String, String>>
    ): String = contextBuilder.buildPromptWithContext(userQuery, history)

    override suspend fun parseLlmActionResponse(
        response: String
    ): SueOperationResult.Prepare? = actionParser.parseLlmActionResponse(response)

    override suspend fun detectActionIntent(
        query: String
    ): SueOperationResult? = ruleIntentDetector.detectActionIntent(query)

    /**
     * Helper methods retained for test compatibility.
     */
    fun extractDayOfWeek(query: String): Int? = entityExtractor.extractDayOfWeek(query)

    fun extractTimeOfDayFilter(query: String): String? = entityExtractor.extractTimeOfDayFilter(query)
}
