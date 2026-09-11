package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.SueOperationResult

/**
 * Interface representing the Sue AI agent orchestrator.
 */
interface ISueAgent {

    /**
     * Parses the LLM response to detect if it contains an action intent tag like `[ACTION: TYPE, ...]`
     * and prepares the corresponding [SueOperationResult.Prepare].
     *
     * @param response The response from the LLM.
     * @return A prepared operation result, or null if no action tag is found.
     */
    suspend fun parseLlmActionResponse(response: String): SueOperationResult.Prepare?

    /**
     * Detects an action or query intent locally using rules and regex.
     * Returns a [SueOperationResult] if matched, or null if no local match is found.
     *
     * @param query The query provided by the user.
     * @return A resolved operation result (read or write/prepare), or null if not matched.
     */
    suspend fun detectActionIntent(query: String): SueOperationResult?

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
