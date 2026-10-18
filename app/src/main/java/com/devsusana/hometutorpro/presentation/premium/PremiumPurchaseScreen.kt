package com.devsusana.hometutorpro.presentation.premium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen entry point for premium purchase. Connects [PremiumPurchaseViewModel] with [PremiumPurchaseContent].
 */
@Composable
fun PremiumPurchaseScreen(
    onPurchaseSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PremiumPurchaseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val premiumProduct by viewModel.premiumProduct.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()

    LaunchedEffect(isPremium) {
        if (isPremium) {
            onPurchaseSuccess()
        }
    }

    PremiumPurchaseContent(
        premiumProduct = premiumProduct,
        isLoading = isLoading,
        onBuyPremium = {
            val activity = context as? android.app.Activity
            if (activity != null) {
                viewModel.buyPremium(activity)
            }
        },
        onNavigateBack = onNavigateBack
    )
}