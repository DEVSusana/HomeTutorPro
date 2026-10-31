package com.devsusana.hometutorpro.domain.repository

import com.devsusana.hometutorpro.domain.entities.AppFeatureFlags
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for managing remote application configuration and runtime feature flags.
 */
interface IConfigRepository {

    /**
     * Observable reactive stream of current [AppFeatureFlags].
     */
    val featureFlags: StateFlow<AppFeatureFlags>

    /**
     * Asynchronously fetches latest configuration from remote and activates it.
     *
     * @return true if fetch and activation succeeded, false otherwise.
     */
    suspend fun fetchAndActivate(): Boolean

    /**
     * Synchronous snapshot check if interactive widget actions are enabled.
     */
    fun isWidgetActionsEnabled(): Boolean

    /**
     * Synchronous snapshot check if on-device local AI (SUE) is enabled.
     */
    fun isSueLocalAiEnabled(): Boolean

    /**
     * Gets custom model download URL, or empty string if not configured remotely.
     */
    fun getSueModelUrl(): String

    /**
     * Gets optional maintenance message, or empty string if none.
     */
    fun getMaintenanceMessage(): String
}
