package com.devsusana.hometutorpro.presentation.viewmodels

import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel responsible for managing permission request flow and dialog states.
 */
@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {

    private val _showAlarmDialog = MutableStateFlow(false)
    
    /**
     * StateFlow exposing whether the exact alarm permission dialog should be shown.
     */
    val showAlarmDialog: StateFlow<Boolean> = _showAlarmDialog.asStateFlow()

    /**
     * Checks notification and exact alarm permissions, requesting notification permission if not granted.
     *
     * @param context Current [Context] reference.
     * @param requestNotificationPermission Callback to trigger notification permission dialog.
     */
    fun checkPermissions(context: Context, requestNotificationPermission: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                requestNotificationPermission()
                return
            }
        }
        checkAlarmPermission(context)
    }

    /**
     * Handles notification permission request result, checking alarm permission afterwards.
     *
     * @param context Current [Context] reference.
     * @param isGranted True if notification permission was granted.
     */
    fun onNotificationPermissionResult(context: Context, isGranted: Boolean) {
        checkAlarmPermission(context)
    }

    private fun checkAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                _showAlarmDialog.value = true
            }
        }
    }

    /**
     * Dismisses the exact alarm permission request dialog.
     */
    fun dismissAlarmDialog() {
        _showAlarmDialog.value = false
    }
}
