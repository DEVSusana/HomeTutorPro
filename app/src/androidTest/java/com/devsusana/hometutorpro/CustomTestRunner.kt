package com.devsusana.hometutorpro

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.testing.HiltTestApplication

class CustomTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }

    override fun onCreate(arguments: android.os.Bundle?) {
        super.onCreate(arguments)
        
        // Initialize WorkManager for instrumentation tests
        val context = targetContext.applicationContext
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        
        try {
            WorkManager.initialize(context, config)
        } catch (e: IllegalStateException) {
            // WorkManager already initialized, ignore
        }
    }
}
