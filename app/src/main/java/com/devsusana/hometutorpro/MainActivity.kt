package com.devsusana.hometutorpro

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import com.devsusana.hometutorpro.domain.repository.SettingsRepository
import com.devsusana.hometutorpro.navigation.NavigationHost
import com.devsusana.hometutorpro.presentation.components.PermissionCheckHandler
import com.devsusana.hometutorpro.presentation.utils.LocaleHelper
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point of the application.
 * Handles theme initialization and sets up the root layout with navigation and permission checks.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Repository used to observe current user theme mode configuration (e.g. system, light, dark).
     */
    @Inject
    lateinit var settingsRepository: SettingsRepository

    /**
     * Attaches the base context and applies locale configurations via [LocaleHelper].
     *
     * @param newBase The base context provided by Android.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    /**
     * Initializes theme selection, permissions dialog checking, and application navigation graph.
     *
     * @param savedInstanceState Pre-saved state configurations.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val themeMode by settingsRepository.themeModeFlow.collectAsState(
                initial = AppThemeMode.SYSTEM
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
}
