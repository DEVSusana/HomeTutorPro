package com.devsusana.hometutorpro.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.core.billing.PremiumProduct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumPurchaseViewModel @Inject constructor(
    private val billingService: PremiumBillingService
) : ViewModel() {

    private val _premiumProduct = MutableStateFlow<PremiumProduct?>(null)
    val premiumProduct: StateFlow<PremiumProduct?> = _premiumProduct.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val isPremium: StateFlow<Boolean> = billingService.isPremium

    init {
        loadProductDetails()
    }

    private fun loadProductDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            _premiumProduct.value = billingService.getPremiumProduct()
            _isLoading.value = false
        }
    }

    fun buyPremium(activity: Activity) {
        billingService.launchPremiumPurchase(activity)
    }
}
