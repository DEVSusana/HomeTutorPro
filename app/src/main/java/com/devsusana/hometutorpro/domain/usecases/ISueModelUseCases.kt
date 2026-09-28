package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.entities.SueDeviceCompatibility
import com.devsusana.hometutorpro.domain.entities.SueModelStatus
import kotlinx.coroutines.flow.Flow

/**
 * Use case to check device OS and hardware compatibility for running the Sue on-device assistant.
 */
interface ICheckSueCompatibilityUseCase {
    operator fun invoke(): SueDeviceCompatibility
}

/**
 * Use case to observe the current status of the Sue on-device LLM model.
 */
interface IGetSueModelStatusUseCase {
    operator fun invoke(): Flow<SueModelStatus>
}

/**
 * Use case to trigger downloading the Sue LLM model.
 */
interface IDownloadSueModelUseCase {
    suspend operator fun invoke(url: String? = null): Flow<SueModelStatus>
}

/**
 * Use case to cancel an ongoing Sue model download.
 */
interface ICancelSueModelDownloadUseCase {
    operator fun invoke()
}

/**
 * Use case to delete the downloaded Sue LLM model from device storage.
 */
interface IDeleteSueModelUseCase {
    suspend operator fun invoke(): Boolean
}
