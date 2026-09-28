package com.devsusana.hometutorpro.data.repository

import com.devsusana.hometutorpro.BuildConfig
import com.devsusana.hometutorpro.data.billing.BillingManager
import com.devsusana.hometutorpro.di.ApplicationScope
import com.devsusana.hometutorpro.domain.repository.SettingsRepository
import com.devsusana.hometutorpro.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data layer implementation of [SubscriptionRepository].
 * Combines Google Play Billing premium state with local developer debug overrides.
 */
@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val billingManager: BillingManager,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : SubscriptionRepository {

    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    init {
        applicationScope.launch {
            combine(billingManager.isPremium, settingsRepository.isDebugPremiumFlow) { real, debug ->
                if (BuildConfig.DEBUG) {
                    debug
                } else {
                    real
                }
            }.collect { combined ->
                _isPremium.value = combined
            }
        }
    }

    override suspend fun checkSubscriptionStatus() {
        billingManager.queryPurchases()
    }
}
