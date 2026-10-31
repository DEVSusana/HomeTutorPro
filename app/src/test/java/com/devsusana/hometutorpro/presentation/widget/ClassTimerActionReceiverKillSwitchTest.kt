package com.devsusana.hometutorpro.presentation.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import com.devsusana.hometutorpro.data.repository.FirebaseConfigRepositoryImpl
import com.devsusana.hometutorpro.domain.usecases.implementations.StartActiveSessionUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class ClassTimerActionReceiverKillSwitchTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
        mockkConstructor(FirebaseConfigRepositoryImpl::class)
        mockkConstructor(StartActiveSessionUseCase::class)
    }

    @After
    fun tearDown() {
        unmockkConstructor(FirebaseConfigRepositoryImpl::class)
        unmockkConstructor(StartActiveSessionUseCase::class)
    }

    @Test
    fun whenWidgetActionsDisabled_onReceiveIgnoresStartClassBroadcast() = runTest {
        every { anyConstructed<FirebaseConfigRepositoryImpl>().isWidgetActionsEnabled() } returns false

        val receiver = ClassTimerActionReceiver()
        val context = RuntimeEnvironment.getApplication()
        val intent = Intent(ClassTimerWidget.ACTION_START_CLASS).apply {
            `package` = context.packageName
        }

        context.registerReceiver(receiver, IntentFilter(ClassTimerWidget.ACTION_START_CLASS))
        context.sendBroadcast(intent)

        shadowOf(Looper.getMainLooper()).idle()

        coVerify(exactly = 0) { anyConstructed<StartActiveSessionUseCase>().invoke(any()) }
    }
}
