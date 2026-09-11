package com.devsusana.hometutorpro.domain.repository

/**
 * Interface representing notification dispatch.
 *
 * Keeps the domain layer completely isolated from Android SDK notifications,
 * context, and system alarm managers.
 */
interface NotificationRepository {
    /**
     * Shows a test notification immediately.
     */
    fun showTestNotification()

    /**
     * Schedules a notification for the end of class for the given student.
     *
     * @param studentName The name of the student.
     * @param durationMinutes The duration in minutes until the class ends.
     * @return True if the notification was scheduled successfully, false otherwise.
     */
    fun scheduleClassEndNotification(studentName: String, durationMinutes: Long): Boolean

    /**
     * Displays a class-end notification immediately.
     *
     * @param studentName The name of the student.
     */
    fun showClassEndNotification(studentName: String)
}
