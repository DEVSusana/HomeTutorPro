package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.ILogoutUseCase
import com.devsusana.hometutorpro.domain.usecases.ISyncCoordinator
import javax.inject.Inject

/**
 * Default implementation of [ILogoutUseCase].
 *
 * Coordinates authentication logout and triggers sync data cleanup through [ISyncCoordinator]
 * without introducing data-layer dependencies into domain logic.
 */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val syncCoordinator: ISyncCoordinator
) : ILogoutUseCase {
    override suspend operator fun invoke() {
        repository.logout()
        syncCoordinator.clearSyncData()
    }
}
