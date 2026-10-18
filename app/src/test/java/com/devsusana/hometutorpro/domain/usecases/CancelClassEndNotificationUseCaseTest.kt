package com.devsusana.hometutorpro.domain.usecases

import com.devsusana.hometutorpro.domain.repository.NotificationRepository
import com.devsusana.hometutorpro.domain.usecases.implementations.CancelClassEndNotificationUseCase
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class CancelClassEndNotificationUseCaseTest {

    private val notificationRepository: NotificationRepository = mockk()
    private lateinit var useCase: CancelClassEndNotificationUseCase

    @Before
    fun setup() {
        useCase = CancelClassEndNotificationUseCase(notificationRepository)
    }

    @Test
    fun `invoke should call notificationRepository cancelClassEndNotification`() {
        every { notificationRepository.cancelClassEndNotification() } just runs

        useCase()

        verify(exactly = 1) { notificationRepository.cancelClassEndNotification() }
    }
}
