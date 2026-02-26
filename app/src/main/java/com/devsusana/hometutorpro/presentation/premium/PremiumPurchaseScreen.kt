package com.devsusana.hometutorpro.presentation.premium

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.billing.PremiumProduct
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
            if (context is android.app.Activity) {
                viewModel.buyPremium(context)
            }
        },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPurchaseContent(
    premiumProduct: PremiumProduct?,
    isLoading: Boolean,
    onBuyPremium: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.premium_upgrade_title)) },
                navigationIcon = {
                    // Add back button if needed
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.premium_unlock_potential),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = stringResource(R.string.premium_benefits_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (premiumProduct != null) {
                        Button(
                            onClick = onBuyPremium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.premium_subscribe_button, premiumProduct.formattedPrice))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.premium_error_not_found),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { onNavigateBack() }) {
                            Text(stringResource(R.string.premium_go_back))
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PremiumPurchaseContentPreview() {
    com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme {
        PremiumPurchaseContent(
            premiumProduct = null,
            isLoading = false,
            onBuyPremium = {},
            onNavigateBack = {}
        )
    }
}