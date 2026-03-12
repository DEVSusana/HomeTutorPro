package com.devsusana.hometutorpro.domain.usecases

import android.app.Activity
import com.devsusana.hometutorpro.core.billing.PremiumProduct

interface IGetPremiumProductUseCase {
    suspend operator fun invoke(): PremiumProduct?
}

interface ILaunchPremiumPurchaseUseCase {
    operator fun invoke(activity: Activity)
}

interface IShowTestNotificationUseCase {
    operator fun invoke()
}

interface IScheduleClassEndNotificationUseCase {
    operator fun invoke(studentName: String, durationMinutes: Long): Boolean
}
