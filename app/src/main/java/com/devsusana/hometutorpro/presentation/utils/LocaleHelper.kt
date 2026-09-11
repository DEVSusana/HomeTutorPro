package com.devsusana.hometutorpro.presentation.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.devsusana.hometutorpro.di.SettingsRepositoryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * Utility functions for managing app locale/language.
 */
object LocaleHelper {
    
    /**
     * Set the app locale and recreate the activity to apply changes.
     * @param activity The activity to recreate
     * @param languageCode Language code (e.g., "en", "es")
     */
    fun setLocale(activity: Activity, languageCode: String) {
        // We don't need to update configuration here as it's handled in onAttach
        // Just recreate the activity to apply the new language from DataStore
        activity.recreate()
    }
    
    /**
     * Get the current app locale language code.
     * @param context Application context
     * @return Language code (e.g., "en", "es")
     */
    fun getCurrentLanguage(context: Context): String {
        val primaryLocale = androidx.core.os.ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()
        return primaryLocale.language
    }

    /**
     * Attach the base context with the saved locale.
     * Should be called in Activity.attachBaseContext().
     */
    fun onAttach(context: Context): Context {
        val language = try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                SettingsRepositoryEntryPoint::class.java
            )
            val settingsRepository = entryPoint.settingsRepository()
            runBlocking {
                settingsRepository.languageFlow.first()
            }
        } catch (e: Exception) {
            // If Hilt or DataStore is not available yet, fall back to Spanish
            "es"
        }
        
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}
