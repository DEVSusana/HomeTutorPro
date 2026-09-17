package com.devsusana.hometutorpro.domain.usecases

/**
 * Use case contract for triggering the end-of-class notification.
 *
 * The receiver acts as a thin Android gateway; all business logic
 * (checking whether notifications are enabled and showing the notification)
 * is encapsulated here, keeping the domain layer free of Android SDK classes.
 */
interface INotifyClassEndUseCase {
    /**
     * Executes the notification logic for the given student.
     *
     * @param studentName The name of the student whose class has ended.
     */
    suspend fun execute(studentName: String)
}
