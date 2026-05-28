package com.devsusana.hometutorpro.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devsusana.hometutorpro.di.ApplicationScope
import com.devsusana.hometutorpro.domain.usecases.INotifyClassEndUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Thin BroadcastReceiver gateway for scheduled class-end alarms.
 *
 * Delegates all business logic (settings check, notification display) to
 * [INotifyClassEndUseCase], keeping this class free of domain or data dependencies
 * beyond the use case interface contract.
 */
@AndroidEntryPoint
class ClassEndReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notifyClassEndUseCase: INotifyClassEndUseCase

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val studentName = intent.getStringExtra(EXTRA_STUDENT_NAME)
            ?: context.getString(com.devsusana.hometutorpro.R.string.student_default_name)

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                notifyClassEndUseCase.execute(studentName)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error processing class end notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ClassEndReceiver"

        /** Intent extra key for passing the student's name to show in the notification. */
        const val EXTRA_STUDENT_NAME = "extra_student_name"
    }
}
