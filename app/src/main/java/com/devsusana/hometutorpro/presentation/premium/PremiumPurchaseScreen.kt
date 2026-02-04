package com.devsusana.hometutorpro.presentation.premium

import android.app.Activity
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPurchaseScreen(
    onPurchaseSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PremiumPurchaseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val productDetails by viewModel.productDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    LaunchedEffect(isPremium) {
        if (isPremium) {
            onPurchaseSuccess()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Upgrade to Premium") },
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
                        text = "Unlock Full Potential",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Get access to Cloud Sync, Multi-device support, and more!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (productDetails != null) {
                        val offer = productDetails?.subscriptionOfferDetails?.firstOrNull()
                        val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                        
                        Button(
                            onClick = {
                                if (context is Activity) {
                                    viewModel.buyPremium(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Subscribe for $price")
                        }
                    } else {
                        Text(
                            text = "Product details not found. Please check your internet connection or try again later.",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { onNavigateBack() }) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}
