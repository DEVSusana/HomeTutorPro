package com.devsusana.hometutorpro.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.R

/**
 * Model representing a single page in the onboarding walkthrough.
 */
data class OnboardingPage(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector? = null,
    val drawableRes: Int? = null
)

/**
 * Screen entry point for onboarding. Connects [OnboardingViewModel] with [OnboardingContent].
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                titleRes = R.string.onboarding_page1_title,
                descriptionRes = R.string.onboarding_page1_desc,
                drawableRes = R.drawable.ic_app_icon
            ),
            OnboardingPage(
                titleRes = R.string.onboarding_page2_title,
                descriptionRes = R.string.onboarding_page2_desc,
                icon = Icons.AutoMirrored.Filled.TrendingUp
            ),
            OnboardingPage(
                titleRes = R.string.onboarding_page3_title,
                descriptionRes = R.string.onboarding_page3_desc,
                icon = Icons.Default.Description
            )
        )
    }

    OnboardingContent(
        pages = pages,
        onComplete = {
            viewModel.completeOnboarding()
            onOnboardingComplete()
        }
    )
}
