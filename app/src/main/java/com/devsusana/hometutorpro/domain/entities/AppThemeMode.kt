package com.devsusana.hometutorpro.domain.entities

/**
 * Represents the theme settings of the application.
 */
enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        /**
         * Resolves the theme mode from a string representation.
         *
         * @param value The string name of the theme mode.
         * @return The corresponding [AppThemeMode], defaulting to [SYSTEM] if unrecognized.
         */
        fun fromString(value: String): AppThemeMode {
            return when (value) {
                "LIGHT" -> LIGHT
                "DARK" -> DARK
                else -> SYSTEM
            }
        }
    }
}
