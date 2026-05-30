package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.NotificationRepository
import com.devsusana.hometutorpro.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

import com.devsusana.hometutorpro.domain.entities.AppThemeMode

class NotifyClassEndUseCaseImplTest {

    private class FakeSettingsRepository(val enabled: Boolean) : SettingsRepository {
        override val classEndNotificationsFlow: Flow<Boolean> = flowOf(enabled)
        override val languageFlow: Flow<String> = flowOf("en")
        override val isGridViewFlow: Flow<Boolean> = flowOf(false)
        override val isDebugPremiumFlow: Flow<Boolean> = flowOf(false)
        override val themeModeFlow: Flow<AppThemeMode> = flowOf(AppThemeMode.SYSTEM)
        override suspend fun setClassEndNotifications(enabled: Boolean) {}
        override suspend fun setLanguage(language: String) {}
        override suspend fun setViewMode(isGridView: Boolean) {}
        override suspend fun setDebugPremium(isPremium: Boolean) {}
        override suspend fun setThemeMode(mode: AppThemeMode) {}
    }

    private class FakeNotificationRepository : NotificationRepository {
        var showClassEndNotificationCalled = false
        var lastStudentName: String? = null

        override fun showTestNotification() {}
        override fun scheduleClassEndNotification(studentName: String, durationMinutes: Long): Boolean = true
        override fun showClassEndNotification(studentName: String) {
            showClassEndNotificationCalled = true
            lastStudentName = studentName
        }
    }

    @Test
    fun execute_shouldShowNotification_whenNotificationsAreEnabled() = runTest {
        // Given
        val fakeSettings = FakeSettingsRepository(enabled = true)
        val fakeNotifications = FakeNotificationRepository()
        val useCase = NotifyClassEndUseCaseImpl(fakeSettings, fakeNotifications)

        // When
        useCase.execute("John Doe")

        // Then
        assertTrue(fakeNotifications.showClassEndNotificationCalled)
        assertEquals("John Doe", fakeNotifications.lastStudentName)
    }

    @Test
    fun execute_shouldNotShowNotification_whenNotificationsAreDisabled() = runTest {
        // Given
        val fakeSettings = FakeSettingsRepository(enabled = false)
        val fakeNotifications = FakeNotificationRepository()
        val useCase = NotifyClassEndUseCaseImpl(fakeSettings, fakeNotifications)

        // When
        useCase.execute("John Doe")

        // Then
        assertFalse(fakeNotifications.showClassEndNotificationCalled)
        assertNull(fakeNotifications.lastStudentName)
    }
}
