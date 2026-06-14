package com.devsusana.hometutorpro.core.receiver

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import com.devsusana.hometutorpro.domain.usecases.implementations.NotifyClassEndUseCaseImpl
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

/**
 * Robolectric unit tests for [ClassEndReceiver].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class ClassEndReceiverTest {

    /** Hilt DI rule for injecting mock/real dependencies. */
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    /**
     * Initializes the DI graph and mockk constructors before each test execution.
     */
    @Before
    fun setUp() {
        hiltRule.inject()
        mockkConstructor(NotifyClassEndUseCaseImpl::class)
    }

    /**
     * Cleans up mockk constructor configuration after each test execution.
     */
    @After
    fun tearDown() {
        unmockkConstructor(NotifyClassEndUseCaseImpl::class)
    }

    /**
     * Verifies that the receiver triggers the class-end usecase when an intent is received.
     */
    @Test
    fun onReceive_triggersNotifyClassEndUseCase() = runTest {
        coEvery { anyConstructed<NotifyClassEndUseCaseImpl>().execute(any()) } just Runs

        val receiver = ClassEndReceiver()

        val context = RuntimeEnvironment.getApplication()
        val intent = Intent("com.devsusana.hometutorpro.CLASS_END").apply {
            putExtra(ClassEndReceiver.EXTRA_STUDENT_NAME, "Test Student")
        }
        
        context.registerReceiver(receiver, IntentFilter("com.devsusana.hometutorpro.CLASS_END"))
        context.sendBroadcast(intent)

        // Force Robolectric main looper to process the broadcast immediately
        shadowOf(Looper.getMainLooper()).idle()

        coVerify { anyConstructed<NotifyClassEndUseCaseImpl>().execute("Test Student") }
    }

    /**
     * Verifies that the receiver uses default name when intent extra is missing.
     */
    @Test
    fun onReceive_missingStudentName_usesDefaultName() = runTest {
        coEvery { anyConstructed<NotifyClassEndUseCaseImpl>().execute(any()) } just Runs

        val receiver = ClassEndReceiver()

        val context = RuntimeEnvironment.getApplication()
        val intent = Intent("com.devsusana.hometutorpro.CLASS_END_EMPTY")
        
        context.registerReceiver(receiver, IntentFilter("com.devsusana.hometutorpro.CLASS_END_EMPTY"))
        context.sendBroadcast(intent)

        // Force Robolectric main looper to process the broadcast immediately
        shadowOf(Looper.getMainLooper()).idle()

        val defaultName = context.getString(com.devsusana.hometutorpro.R.string.student_default_name)
        coVerify { anyConstructed<NotifyClassEndUseCaseImpl>().execute(defaultName) }
    }
}
