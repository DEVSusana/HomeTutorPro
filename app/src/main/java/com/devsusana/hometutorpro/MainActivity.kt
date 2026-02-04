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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var settingsManager: com.devsusana.hometutorpro.core.settings.SettingsManager
    
    @javax.inject.Inject
    lateinit var duplicateCleanupUtil: com.devsusana.hometutorpro.data.util.DuplicateCleanupUtil

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.devsusana.hometutorpro.presentation.utils.LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Block screenshots and screen recording for privacy protection
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        NotificationHelper.createNotificationChannel(this) // Create notification channel
        
        // Request notification permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
        
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
                initial = com.devsusana.hometutorpro.core.settings.SettingsManager.ThemeMode.SYSTEM
            )
            
            HomeTutorProTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationHost()
                }
            }
        }
    }
    
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}


