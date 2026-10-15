package com.devsusana.hometutorpro.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.delay

/**
 * Screen entry point for app launch and routing. Connects [SplashViewModel] with [SplashContent].
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 800,
                easing = { it }
            )
        )
        delay(700)
        if (viewModel.isUserLoggedIn() || viewModel.attemptZeroTapRestore()) {
            onNavigateToHome()
        } else {
            if (viewModel.isOnboardingCompleted()) {
                onNavigateToLogin()
            } else {
                onNavigateToOnboarding()
            }
        }
    }

    SplashContent(scale = scale.value)
}
