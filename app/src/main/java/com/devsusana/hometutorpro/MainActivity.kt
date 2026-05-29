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
import androidx.activity.viewModels
import com.devsusana.hometutorpro.domain.entities.AppThemeMode
import com.devsusana.hometutorpro.navigation.NavigationHost
import com.devsusana.hometutorpro.presentation.components.PermissionCheckHandler
import com.devsusana.hometutorpro.presentation.utils.LocaleHelper
import com.devsusana.hometutorpro.presentation.viewmodels.MainActivityViewModel
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point of the application.
 * Handles theme initialization and sets up the root layout with navigation and permission checks.
 *
 * Fully covered by:
 * - Instrumented UI tests in [MainActivityTest] (app/src/androidTest/java/com/devsusana/hometutorpro/MainActivityTest.kt) verifying startup and layout rendering.
 * - JVM unit tests in [MainActivityViewModelTest] (app/src/test/java/com/devsusana/hometutorpro/presentation/viewmodels/MainActivityViewModelTest.kt) testing theme state flow logic.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * ViewModel associated with MainActivity.
     */
    private val viewModel: MainActivityViewModel by viewModels()

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
            val themeMode by viewModel.themeModeFlow.collectAsState(
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
