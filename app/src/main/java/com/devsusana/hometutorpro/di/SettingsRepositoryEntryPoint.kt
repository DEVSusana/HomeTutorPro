package com.devsusana.hometutorpro.di

import com.devsusana.hometutorpro.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Entry point for accessing SettingsRepository from components outside Hilt's container (e.g. BroadcastReceivers).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsRepositoryEntryPoint {
    /**
     * Retrieves the bound [SettingsRepository] instance.
     */
    fun settingsRepository(): SettingsRepository
}
