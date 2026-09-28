package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.entities.SueDeviceCompatibility
import com.devsusana.hometutorpro.domain.entities.SueModelStatus
import com.devsusana.hometutorpro.domain.repository.SueModelRepository
import com.devsusana.hometutorpro.domain.usecases.ICancelSueModelDownloadUseCase
import com.devsusana.hometutorpro.domain.usecases.ICheckSueCompatibilityUseCase
import com.devsusana.hometutorpro.domain.usecases.IDeleteSueModelUseCase
import com.devsusana.hometutorpro.domain.usecases.IDownloadSueModelUseCase
import com.devsusana.hometutorpro.domain.usecases.IGetSueModelStatusUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of [ICheckSueCompatibilityUseCase].
 */
class CheckSueCompatibilityUseCaseImpl @Inject constructor(
    private val sueModelRepository: SueModelRepository
) : ICheckSueCompatibilityUseCase {
    override fun invoke(): SueDeviceCompatibility = sueModelRepository.checkDeviceCompatibility()
}

/**
 * Implementation of [IGetSueModelStatusUseCase].
 */
class GetSueModelStatusUseCaseImpl @Inject constructor(
    private val sueModelRepository: SueModelRepository
) : IGetSueModelStatusUseCase {
    override fun invoke(): Flow<SueModelStatus> = sueModelRepository.modelStatusFlow
}

/**
 * Implementation of [IDownloadSueModelUseCase].
 */
class DownloadSueModelUseCaseImpl @Inject constructor(
    private val sueModelRepository: SueModelRepository
) : IDownloadSueModelUseCase {
    override suspend fun invoke(url: String?): Flow<SueModelStatus> = sueModelRepository.downloadModel(url)
}

/**
 * Implementation of [ICancelSueModelDownloadUseCase].
 */
class CancelSueModelDownloadUseCaseImpl @Inject constructor(
    private val sueModelRepository: SueModelRepository
) : ICancelSueModelDownloadUseCase {
    override fun invoke() = sueModelRepository.cancelDownload()
}

/**
 * Implementation of [IDeleteSueModelUseCase].
 */
class DeleteSueModelUseCaseImpl @Inject constructor(
    private val sueModelRepository: SueModelRepository
) : IDeleteSueModelUseCase {
    override suspend fun invoke(): Boolean = sueModelRepository.deleteModel()
}
