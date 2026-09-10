package com.devsusana.hometutorpro.domain.usecases

import android.app.Application
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.domain.usecases.implementations.CancelClassEndNotificationUseCase
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class CancelClassEndNotificationUseCaseTest {

    private val application: Application = mockk()
    private lateinit var useCase: CancelClassEndNotificationUseCase

    @Before
    fun setup() {
        mockkObject(NotificationHelper)
        useCase = CancelClassEndNotificationUseCase(application)
    }

    @After
    fun tearDown() {
        unmockkObject(NotificationHelper)
    }

    @Test
    fun `invoke should call NotificationHelper cancelClassEndNotification`() {
        every { NotificationHelper.cancelClassEndNotification(application) } just runs

        useCase()

        verify(exactly = 1) { NotificationHelper.cancelClassEndNotification(application) }
    }
}
