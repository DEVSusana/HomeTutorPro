package com.devsusana.hometutorpro.data.repository

import android.content.Context
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.domain.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRepository {

    override fun showTestNotification() {
        NotificationHelper.showTestNotification(context)
    }

    override fun scheduleClassEndNotification(studentName: String, durationMinutes: Long): Boolean {
        return NotificationHelper.scheduleClassEndNotification(context, studentName, durationMinutes)
    }

    override fun showClassEndNotification(studentName: String) {
        NotificationHelper.showClassEndNotification(context, studentName)
    }
}
