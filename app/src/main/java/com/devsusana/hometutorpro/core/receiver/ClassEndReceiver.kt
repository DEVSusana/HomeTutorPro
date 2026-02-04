package com.devsusana.hometutorpro.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devsusana.hometutorpro.core.utils.NotificationHelper

/**
 * BroadcastReceiver to handle scheduled class end notifications.
 */
class ClassEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("ClassEndReceiver", "onReceive called - alarm triggered!")
        val studentName = intent.getStringExtra(EXTRA_STUDENT_NAME) ?: context.getString(com.devsusana.hometutorpro.R.string.student_default_name)
        android.util.Log.d("ClassEndReceiver", "Showing notification for student: $studentName")
        NotificationHelper.showClassEndNotification(context, studentName)
        android.util.Log.d("ClassEndReceiver", "Notification shown successfully")
    }

    companion object {
        const val EXTRA_STUDENT_NAME = "extra_student_name"
    }
}
