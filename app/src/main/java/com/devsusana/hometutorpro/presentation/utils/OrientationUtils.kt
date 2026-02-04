package com.devsusana.hometutorpro.presentation.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Extension function to find the Activity from a Context.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Composable that locks the screen orientation.
 * @param orientation The orientation to lock to (e.g., ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
 */
@SuppressLint("WrongConstant")
@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current

    DisposableEffect(orientation) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation ?: -1

        activity?.requestedOrientation = orientation

        onDispose {
            // Restore original orientation when composable is disposed
            if (originalOrientation != -1) {
                activity?.requestedOrientation = originalOrientation
            }
        }
    }
}
