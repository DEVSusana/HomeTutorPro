package com.devsusana.hometutorpro.domain.usecases

import kotlinx.coroutines.flow.StateFlow

/**
 * Use case contract to manage lifecycle and loading states of on-device Sue LLM inference.
 */
interface IManageSueInferenceUseCase {
    val isModelLoaded: StateFlow<Boolean>
    val isLoading: StateFlow<Boolean>
    suspend fun loadModel(): Boolean
    fun release()
}
