package com.devsusana.hometutorpro.presentation.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.billing.PremiumProduct
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme

/**
 * Pure presentation composable for premium subscription purchase.
 */
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
                    // Back navigation handled if needed
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

@Preview(showBackground = true)
@Composable
private fun PremiumPurchaseContentPreview() {
    HomeTutorProTheme {
        PremiumPurchaseContent(
            premiumProduct = PremiumProduct(productId = "premium_lifetime", formattedPrice = "$4.99"),
            isLoading = false,
            onBuyPremium = {},
            onNavigateBack = {}
        )
    }
}
