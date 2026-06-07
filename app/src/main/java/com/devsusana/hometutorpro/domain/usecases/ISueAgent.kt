package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.SueOperationResult

/**
 * Interface representing the Sue AI agent orchestrator.
 */
interface ISueAgent {

    /**
     * Detects an action intent (schedule or financial) from [query] and prepares the
     * corresponding [SueOperationResult.Prepare] with all data pre-resolved.
     *
     * @param query The raw user query.
     * @return A prepared operation result, or null if no action intent is detected.
     */
    suspend fun detectActionIntent(query: String): SueOperationResult.Prepare?

    /**
     * Processes a user query by routing it to the appropriate tools and building
     * a context-enriched prompt for the LLM.
     *
     * @param userQuery The query provided by the user.
     * @param history The conversation history list.
     * @return The built prompt with context.
     */
    suspend fun buildPromptWithContext(
        userQuery: String,
        history: List<Pair<String, String>> = emptyList()
    ): String

    /**
     * Resets the active conversation context values (last mentioned student, time, day).
     */
    fun resetConversationContext()
}
