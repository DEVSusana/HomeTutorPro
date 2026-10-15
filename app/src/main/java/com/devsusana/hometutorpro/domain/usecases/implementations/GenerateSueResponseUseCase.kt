package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.InferenceRepository
import com.devsusana.hometutorpro.domain.usecases.IGenerateSueResponseUseCase
import javax.inject.Inject

/**
 * Executes prompt inference through the configured [InferenceRepository].
 */
class GenerateSueResponseUseCase @Inject constructor(
    private val inferenceRepository: InferenceRepository
) : IGenerateSueResponseUseCase {
    override suspend operator fun invoke(prompt: String): String {
        return inferenceRepository.generateResponse(prompt)
    }
}
