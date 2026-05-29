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
 *
 * Fully covered by unit tests in [ClassEndReceiverTest] (app/src/test/java/com/devsusana/hometutorpro/core/receiver/ClassEndReceiverTest.kt)
 * verifying intent extra extraction and background coroutine usecase execution.
 */
@AndroidEntryPoint
class ClassEndReceiver : BroadcastReceiver() {

    /** Use case responsible for triggering the class-end notification logic. */
    @Inject
    lateinit var notifyClassEndUseCase: INotifyClassEndUseCase

    /** Application-scoped coroutine scope for background work that outlives the receiver. */
    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    /**
     * Handles the incoming broadcast intent for the class-end event.
     *
     * Extracts the student name from the intent extras and delegates the notification
     * logic to [INotifyClassEndUseCase] within [applicationScope]. Uses [goAsync] to
     * keep the receiver alive during the coroutine execution.
     *
     * @param context The context in which the receiver is running.
     * @param intent The intent being received, containing the student name extra.
     */
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
