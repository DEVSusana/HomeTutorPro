package com.devsusana.hometutorpro

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric unit tests for [MainActivity].
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [33])
class MainActivityTest {

    /** Hilt DI rule for injecting mock/real dependencies. */
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    /**
     * Initializes the DI graph before each test execution.
     */
    @Before
    fun setUp() {
        hiltRule.inject()
    }

    /**
     * Verifies that the MainActivity launches successfully and moves to RESUMED state.
     */
    @Test
    fun testActivityLaunchAndResumedState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }
}
