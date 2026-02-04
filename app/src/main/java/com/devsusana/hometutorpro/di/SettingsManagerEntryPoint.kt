package com.devsusana.hometutorpro.di

import com.devsusana.hometutorpro.core.settings.SettingsManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Entry point for accessing SettingsManager from Composables.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsManagerEntryPoint {
    fun settingsManager(): SettingsManager
}
