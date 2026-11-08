package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.core.DomainError
import com.devsusana.hometutorpro.domain.core.Result

/**
 * Use case to trigger fetching and activating the latest remote configuration.
 */
interface IFetchRemoteConfigUseCase {
    suspend operator fun invoke(): Result<Boolean, DomainError>
}
