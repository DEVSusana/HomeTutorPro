package com.devsusana.hometutorpro.domain.usecases

/**
 * Use case contract to generate a natural language response from the on-device inference model.
 */
interface IGenerateSueResponseUseCase {
    suspend operator fun invoke(prompt: String): String
}
