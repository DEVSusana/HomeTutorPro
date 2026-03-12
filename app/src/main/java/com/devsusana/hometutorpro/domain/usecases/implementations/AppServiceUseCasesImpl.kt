package com.devsusana.hometutorpro.domain.usecases.implementations

import android.app.Activity
import android.app.Application
import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.core.billing.PremiumProduct
import com.devsusana.hometutorpro.core.utils.NotificationHelper
import com.devsusana.hometutorpro.domain.usecases.*
import javax.inject.Inject

class GetPremiumProductUseCase @Inject constructor(
    private val billingService: PremiumBillingService
) : IGetPremiumProductUseCase {
    override suspend operator fun invoke(): PremiumProduct? {
        return billingService.getPremiumProduct()
    }
}

class LaunchPremiumPurchaseUseCase @Inject constructor(
    private val billingService: PremiumBillingService
) : ILaunchPremiumPurchaseUseCase {
    override operator fun invoke(activity: Activity) {
        billingService.launchPremiumPurchase(activity)
    }
}

class ShowTestNotificationUseCase @Inject constructor(
    private val application: Application
) : IShowTestNotificationUseCase {
    override operator fun invoke() {
        NotificationHelper.showTestNotification(application)
    }
}

class ScheduleClassEndNotificationUseCase @Inject constructor(
    private val application: Application
) : IScheduleClassEndNotificationUseCase {
    override operator fun invoke(studentName: String, durationMinutes: Long): Boolean {
        return NotificationHelper.scheduleClassEndNotification(
            application,
            studentName,
            durationMinutes
        )
    }
}
