package com.devsusana.hometutorpro.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.devsusana.hometutorpro.data.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumPurchaseViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val isPremium: StateFlow<Boolean> = billingManager.isPremium

    init {
        loadProductDetails()
    }

    private fun loadProductDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            // TODO: Replace with actual Product ID from Play Console
            val productId = "premium_subscription" 
            billingManager.queryProductDetails(productId) { details ->
                _productDetails.value = details
                _isLoading.value = false
            }
        }
    }

    fun buyPremium(activity: Activity) {
        val details = _productDetails.value
        if (details != null) {
            billingManager.launchPurchaseFlow(activity, details)
        }
    }
}
