package com.devsusana.hometutorpro.domain.entities

/**
 * Represents a response from the Sue AI agent.
 *
 * @param text The text content of Sue's response.
 * @param isError Whether this response represents an error condition.
 */
data class SueResponse(
    val text: String,
    val isError: Boolean = false
)
