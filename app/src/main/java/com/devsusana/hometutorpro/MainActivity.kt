package com.devsusana.hometutorpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.navigation.NavigationHost
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.AlarmManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.settings.SettingsManager
import com.devsusana.hometutorpro.data.util.DuplicateCleanupUtil
import com.devsusana.hometutorpro.presentation.utils.LocaleHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager
    
    @Inject
    lateinit var duplicateCleanupUtil: DuplicateCleanupUtil

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Block screenshots and screen recording for privacy protection
//        window.setFlags(
//            android.view.WindowManager.LayoutParams.FLAG_SECURE,
//            android.view.WindowManager.LayoutParams.FLAG_SECURE
//        )

        NotificationHelper.createNotificationChannel(this)
        
        // Clean up any duplicate students on app startup
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                duplicateCleanupUtil.removeDuplicateStudents()
                duplicateCleanupUtil.removeLocalDuplicates()
            } catch (e: Exception) {
                // Silently fail - cleanup is best effort
            }
        }
        
        setContent {
            val themeMode by settingsManager.themeModeFlow.collectAsState(
                initial = SettingsManager.ThemeMode.SYSTEM
            )
            
            HomeTutorProTheme(themeMode = themeMode) {
                PermissionCheckHandler()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationHost()
                }
            }
        }
    }

    @Composable
    private fun PermissionCheckHandler() {
        val context = LocalContext.current
        var showAlarmDialog by remember { mutableStateOf(false) }

        // Notification Permission Launcher (Android 13+)
        val notificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            // Check for alarm permission after notification check
            checkAlarmPermission { showAlarmDialog = true }
        }

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    checkAlarmPermission { showAlarmDialog = true }
                }
            } else {
                checkAlarmPermission { showAlarmDialog = true }
            }
        }

        if (showAlarmDialog) {
            AlertDialog(
                onDismissRequest = { showAlarmDialog = false },
                title = { Text(stringResource(R.string.permission_alarm_title)) },
                text = { Text(stringResource(R.string.permission_alarm_description)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showAlarmDialog = false
                            NotificationHelper.openExactAlarmSettings(context)
                        }
                    ) {
                        Text(stringResource(R.string.permission_button_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAlarmDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }

    private fun checkAlarmPermission(onPermissionNeeded: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                onPermissionNeeded()
            }
        }
    }
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}


