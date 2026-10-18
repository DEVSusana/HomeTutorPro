package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.InferenceRepository
import com.devsusana.hometutorpro.domain.usecases.IManageSueInferenceUseCase
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Manages model loading, state emission, and resource releasing via [InferenceRepository].
 */
class ManageSueInferenceUseCase @Inject constructor(
    private val inferenceRepository: InferenceRepository
) : IManageSueInferenceUseCase {

    override val isModelLoaded: StateFlow<Boolean>
        get() = inferenceRepository.isModelLoaded

    override val isLoading: StateFlow<Boolean>
        get() = inferenceRepository.isLoading

    override suspend fun loadModel(): Boolean {
        return inferenceRepository.loadModel()
    }

    override fun release() {
        inferenceRepository.release()
    }
}
