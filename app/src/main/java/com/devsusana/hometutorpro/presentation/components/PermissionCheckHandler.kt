package com.devsusana.hometutorpro.presentation.components

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.presentation.viewmodels.PermissionViewModel

/**
 * Composable responsible for requesting and verifying necessary runtime permissions,
 * specifically for notifications and exact alarms.
 *
 * @param viewModel Injected [PermissionViewModel] instance.
 */
@Composable
fun PermissionCheckHandler(
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val showAlarmDialog by viewModel.showAlarmDialog.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onNotificationPermissionResult(context, isGranted)
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlarmDialog() },
            title = { Text(stringResource(R.string.permission_alarm_title)) },
            text = { Text(stringResource(R.string.permission_alarm_description)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissAlarmDialog()
                        NotificationHelper.openExactAlarmSettings(context)
                    }
                ) {
                    Text(stringResource(R.string.permission_button_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAlarmDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
