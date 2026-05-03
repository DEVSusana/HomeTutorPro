package com.devsusana.hometutorpro.core.sue

/**
 * Definition of the Gemma LLM model for the Koog agent framework.
 * 
 * In a full Koog implementation, this would implement `ai.koog.model.LLMModel`.
 * Since Koog is primarily designed for remote providers (OpenAI, Anthropic),
 * this acts as a descriptor for our custom local MediaPipe executor.
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
