package com.devsusana.hometutorpro.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository contract for Subscription data operations.
 */
interface SubscriptionRepository {
    val isPremium: StateFlow<Boolean>
    suspend fun checkSubscriptionStatus()
}
