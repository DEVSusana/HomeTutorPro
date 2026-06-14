package com.devsusana.hometutorpro.domain.usecases.implementations

import com.devsusana.hometutorpro.core.billing.PremiumBillingService
import com.devsusana.hometutorpro.core.billing.PremiumProduct
import com.devsusana.hometutorpro.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AppServiceUseCasesImplTest {

    private class FakePremiumBillingService(
        override val isPremium: StateFlow<Boolean>,
        val product: PremiumProduct?
    ) : PremiumBillingService {
        override suspend fun getPremiumProduct(): PremiumProduct? = product
    }

    private class FakeNotificationRepository : NotificationRepository {
        var showTestNotificationCalled = false
        var scheduleClassEndNotificationCalled = false
        var lastStudentName: String? = null
        var lastDurationMinutes: Long? = null

        override fun showTestNotification() {
            showTestNotificationCalled = true
        }

        override fun scheduleClassEndNotification(studentName: String, durationMinutes: Long): Boolean {
            scheduleClassEndNotificationCalled = true
            lastStudentName = studentName
            lastDurationMinutes = durationMinutes
            return true
        }

        override fun showClassEndNotification(studentName: String) {}
    }

    @Test
    fun getPremiumProductUseCase_returnsProduct() = runTest {
        val expectedProduct = PremiumProduct("prod_id", "$5.99")
        val fakeBilling = FakePremiumBillingService(MutableStateFlow(true), expectedProduct)
        val useCase = GetPremiumProductUseCase(fakeBilling)

        val result = useCase()

        assertEquals(expectedProduct, result)
    }

    @Test
    fun showTestNotificationUseCase_triggersNotification() {
        val fakeNotifications = FakeNotificationRepository()
        val useCase = ShowTestNotificationUseCase(fakeNotifications)

        useCase()

        assertTrue(fakeNotifications.showTestNotificationCalled)
    }

    @Test
    fun scheduleClassEndNotificationUseCase_schedulesCorrectly() {
        val fakeNotifications = FakeNotificationRepository()
        val useCase = ScheduleClassEndNotificationUseCase(fakeNotifications)

        val result = useCase("Alice", 45)

        assertTrue(result)
        assertTrue(fakeNotifications.scheduleClassEndNotificationCalled)
        assertEquals("Alice", fakeNotifications.lastStudentName)
        assertEquals(45L, fakeNotifications.lastDurationMinutes)
    }
}
