package com.devsusana.hometutorpro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo900,
    primaryContainer = Indigo800,
    onPrimaryContainer = Indigo80,
    secondary = Emerald80,
    onSecondary = Emerald700,
    secondaryContainer = Emerald700,
    onSecondaryContainer = Emerald80,
    tertiary = Rose80,
    onTertiary = Color.Black,
    background = Slate900,
    surface = Slate800,
    onBackground = Slate50,
    onSurface = Slate50,
    surfaceVariant = Slate800, // Slightly lighter or same as surface for dark mode card bases
    onSurfaceVariant = Slate100,
    error = ErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo80,
    onPrimaryContainer = Indigo900,
    secondary = Emerald500,
    onSecondary = Color.White,
    secondaryContainer = Emerald80,
    onSecondaryContainer = Emerald700,
    tertiary = Rose500,
    onTertiary = Color.White,
    background = Slate50,
    surface = Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
    surfaceVariant = Slate100, // Card backgrounds
    onSurfaceVariant = Slate500, // Subtitles
    error = ErrorLight
)

@Composable
fun HomeTutorProTheme(
    themeMode: com.devsusana.hometutorpro.core.settings.SettingsManager.ThemeMode = com.devsusana.hometutorpro.core.settings.SettingsManager.ThemeMode.SYSTEM,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        com.devsusana.hometutorpro.core.settings.SettingsManager.ThemeMode.LIGHT -> false
        com.devsusana.hometutorpro.core.settings.SettingsManager.ThemeMode.DARK -> true
        com.devsusana.hometutorpro.core.settings.SettingsManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}