package com.devsusana.hometutorpro.domain.entities

/**
 * Immutable entity representing application feature flags and remote configuration.
 *
 * @property isWidgetActionsEnabled Whether interactive actions on the Home Screen Widget (start, stop, direct navigation) are enabled.
 * @property isSueLocalAiEnabled Whether on-device AI inference (SUE / Gemma) is enabled.
 * @property sueModelUrl Custom OTA URL for downloading the AI model; if empty, app-level default is used.
 * @property maintenanceMessage Optional operational maintenance notice to inform users.
 */
data class AppFeatureFlags(
    val isWidgetActionsEnabled: Boolean = true,
    val isSueLocalAiEnabled: Boolean = true,
    val sueModelUrl: String = "",
    val maintenanceMessage: String = ""
)
