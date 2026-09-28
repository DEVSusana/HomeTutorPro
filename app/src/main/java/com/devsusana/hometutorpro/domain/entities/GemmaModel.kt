package com.devsusana.hometutorpro.domain.entities

/**
 * Configuration parameters for the on-device Gemma LLM model.
 */
data class GemmaModel(
    val name: String = "gemma-2b-it-gpu-int4",
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val topK: Int = 40
) {
    companion object {
        val E2B = GemmaModel()
    }
}
