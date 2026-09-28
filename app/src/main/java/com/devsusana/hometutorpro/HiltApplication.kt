package com.devsusana.hometutorpro

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.devsusana.hometutorpro.presentation.utils.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Custom [Application] class for the project. Configures Hilt for dependency injection
 * and sets up WorkManager with a custom worker factory.
 *
 * Fully covered by unit tests in [HiltApplicationTest] (app/src/test/java/com/devsusana/hometutorpro/HiltApplicationTest.kt)
 * verifying lifecycle creation, AppInitializer trigger, and WorkManager custom HiltWorkerFactory configuration.
 */
@HiltAndroidApp
class HiltApplication : Application(), Configuration.Provider {

    /** Factory used for injecting dependencies into WorkManager workers. */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** Initializer responsible for starting app-wide observers and setups on startup. */
    @Inject
    lateinit var appInitializer: com.devsusana.hometutorpro.domain.usecases.AppInitializer

    /** Attaches the base context and applies locale settings via [LocaleHelper]. */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    /** Runs application startup logic using the injected app initializer. */
    override fun onCreate() {
        super.onCreate()
        appInitializer.initialize()
    }

    /** Provides the configuration for WorkManager including the Hilt-injected worker factory. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
