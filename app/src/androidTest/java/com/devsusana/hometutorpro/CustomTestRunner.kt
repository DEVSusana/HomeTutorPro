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
        
        // Initialize WorkManager for instrumentation tests only if not already initialized
        // This prevents IllegalStateException: WorkManager is already initialized
        val context = targetContext.applicationContext
        try {
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
            WorkManager.initialize(context, config)
        } catch (e: Exception) {
            android.util.Log.w("CustomTestRunner", "WorkManager already initialized or failed to initialize: ${e.message}")
        }
    }
}
