package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.domain.repository.NotificationRepository
import com.devsusana.hometutorpro.domain.repository.SettingsRepository
import com.devsusana.hometutorpro.domain.usecases.INotifyClassEndUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Implementation of [INotifyClassEndUseCase].
 *
 * Encapsulates the business rule that a notification should only be shown
 * when the user has enabled class-end notifications in their settings.
 * The [ClassEndReceiver] delegates all logic here, keeping it as a thin gateway.
 */
class NotifyClassEndUseCaseImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository
) : INotifyClassEndUseCase {

    /**
     * Shows a class-end notification for [studentName] if the setting is enabled.
     *
     * @param studentName The name of the student whose class has ended.
     */
    override suspend fun execute(studentName: String) {
        val areNotificationsEnabled = settingsRepository.classEndNotificationsFlow.first()
        if (areNotificationsEnabled) {
            notificationRepository.showClassEndNotification(studentName)
        }
    }
}
