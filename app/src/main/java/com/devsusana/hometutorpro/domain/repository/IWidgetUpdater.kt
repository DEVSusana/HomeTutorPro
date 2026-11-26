package com.devsusana.hometutorpro.domain.repository

/**
 * Interface to trigger active app widget updates without coupling domain or data layers
 * to the presentation layer.
 */
interface IWidgetUpdater {
    /**
     * Refreshes active home screen widgets with current state.
     */
    fun updateWidget()
}
