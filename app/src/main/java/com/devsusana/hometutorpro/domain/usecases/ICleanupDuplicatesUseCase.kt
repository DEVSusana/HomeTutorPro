package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case contract for cleaning up duplicated schedule exceptions.
 */
interface ICleanupDuplicatesUseCase {
    suspend operator fun invoke(): Result<Unit, DomainError>
}
