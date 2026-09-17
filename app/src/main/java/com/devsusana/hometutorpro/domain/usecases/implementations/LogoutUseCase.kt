package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.ILogoutUseCase
import com.devsusana.hometutorpro.data.local.dao.SyncMetadataDao
import com.devsusana.hometutorpro.data.sync.SyncScheduler

import javax.inject.Inject

/**
 * Default implementation of [ILogoutUseCase].
 */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val syncMetadataDao: SyncMetadataDao,
    private val syncScheduler: SyncScheduler
) : ILogoutUseCase {
    override suspend operator fun invoke() {
        repository.logout()
        // Handle side effects of logging out separately from auth logic
        syncMetadataDao.deleteAllMetadata()
        syncScheduler.cancelAllSync()
    }
}
