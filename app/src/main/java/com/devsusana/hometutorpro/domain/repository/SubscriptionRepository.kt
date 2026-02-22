package com.devsusana.hometutorpro.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface SubscriptionRepository {
    val isPremium: StateFlow<Boolean>
    suspend fun checkSubscriptionStatus()
}
