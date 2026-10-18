package com.devsusana.hometutorpro.domain.usecases

/**
 * Use case contract to clean up duplicate student records created during sync or local storage anomalies.
 */
interface ICleanupDuplicateStudentsUseCase {
    suspend operator fun invoke()
}
