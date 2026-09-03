package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result
import com.devsusana.hometutorpro.domain.entities.User

/**
 * Use case contract for silently restoring a user session using Android Zero-Tap Restore Credentials.
 */
interface IRestoreSessionUseCase {
    /**
     * Attempts silent session restoration from cloud backup / device migration.
     */
    suspend operator fun invoke(): Result<User, DomainError>
}
