package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.core.billing.PremiumProduct
import com.devsusana.hometutorpro.domain.repository.NotificationRepository
import com.devsusana.hometutorpro.domain.usecases.*
import javax.inject.Inject

class GetPremiumProductUseCase @Inject constructor(
    private val billingService: PremiumBillingService
) : IGetPremiumProductUseCase {
    override suspend operator fun invoke(): PremiumProduct? {
        return billingService.getPremiumProduct()
    }
}

class ShowTestNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) : IShowTestNotificationUseCase {
    override operator fun invoke() {
        notificationRepository.showTestNotification()
    }
}

class ScheduleClassEndNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) : IScheduleClassEndNotificationUseCase {
    override operator fun invoke(studentName: String, durationMinutes: Long): Boolean {
        return notificationRepository.scheduleClassEndNotification(
            studentName,
            durationMinutes
        )
    }
}
