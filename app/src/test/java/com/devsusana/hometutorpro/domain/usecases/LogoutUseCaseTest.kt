package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.repository.AuthRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.LogoutUseCase
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [LogoutUseCase].
 * 
 * Tests user logout functionality and sync data cleanup.
 */
class LogoutUseCaseTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var syncCoordinator: ISyncCoordinator
    private lateinit var stopActiveSessionUseCase: IStopActiveSessionUseCase
    private lateinit var cancelClassEndNotificationUseCase: ICancelClassEndNotificationUseCase
    private lateinit var logoutUseCase: LogoutUseCase

    @Before
    fun setup() {
        authRepository = mockk(relaxed = true)
        syncCoordinator = mockk(relaxed = true)
        stopActiveSessionUseCase = mockk(relaxed = true)
        cancelClassEndNotificationUseCase = mockk(relaxed = true)
        logoutUseCase = LogoutUseCase(
            repository = authRepository,
            syncCoordinator = syncCoordinator,
            stopActiveSessionUseCase = stopActiveSessionUseCase,
            cancelClassEndNotificationUseCase = cancelClassEndNotificationUseCase
        )
    }

    @Test
    fun `invoke should call repository logout and clear sync data and stop active session`() = runTest {
        // When
        logoutUseCase()

        // Then
        coVerify(exactly = 1) { authRepository.logout() }
        coVerify(exactly = 1) { syncCoordinator.clearSyncData() }
        coVerify(exactly = 1) { stopActiveSessionUseCase() }
        coVerify(exactly = 1) { cancelClassEndNotificationUseCase() }
    }

    @Test
    fun `invoke should call repository logout multiple times when called multiple times`() = runTest {
        // When
        logoutUseCase()
        logoutUseCase()
        logoutUseCase()

        // Then
        coVerify(exactly = 3) { authRepository.logout() }
        coVerify(exactly = 3) { syncCoordinator.clearSyncData() }
    }
}
