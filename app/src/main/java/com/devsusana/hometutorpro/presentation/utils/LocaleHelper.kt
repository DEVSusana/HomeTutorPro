package com.devsusana.hometutorpro.presentation.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.devsusana.hometutorpro.core.settings.dataStore
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
    }

    /**
     * Attach the base context with the saved locale.
     * Should be called in Activity.attachBaseContext().
     */
    fun onAttach(context: Context): Context {
        val language = try {
            kotlinx.coroutines.runBlocking {
                context.dataStore.data.first()[com.devsusana.hometutorpro.core.settings.SettingsManager.LANGUAGE_KEY]
                    ?: com.devsusana.hometutorpro.core.settings.SettingsManager.LANGUAGE_SPANISH
            }
        } catch (e: Exception) {
            // If DataStore is not available yet (e.g., during app initialization),
            // fall back to Spanish as default
            com.devsusana.hometutorpro.core.settings.SettingsManager.LANGUAGE_SPANISH
        }
        
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}
