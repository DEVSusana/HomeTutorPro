package com.devsusana.hometutorpro

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.devsusana.hometutorpro.presentation.utils.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }

    @Inject
    lateinit var syncCoordinator: com.devsusana.hometutorpro.domain.usecases.ISyncCoordinator

    override fun onCreate() {
        super.onCreate()
        com.devsusana.hometutorpro.core.utils.NotificationHelper.createNotificationChannel(this)
        syncCoordinator.startObserving()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
