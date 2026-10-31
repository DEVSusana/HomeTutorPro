package com.devsusana.hometutorpro.domain.usecases

/**
 * Use case to trigger fetching and activating the latest remote configuration.
 */
interface IFetchRemoteConfigUseCase {
    suspend operator fun invoke(): Boolean
}
