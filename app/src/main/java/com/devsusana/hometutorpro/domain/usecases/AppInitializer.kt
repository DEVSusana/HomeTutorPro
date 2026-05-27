package com.devsusana.hometutorpro.domain.usecases

/**
 * Interface representing the application-wide startup initialization logic.
 */
interface AppInitializer {
    /**
     * Initializes app-wide configurations and starts background observers.
     */
    fun initialize()
}
