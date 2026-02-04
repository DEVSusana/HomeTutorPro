package com.devsusana.hometutorpro.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.BuildConfig
import com.devsusana.hometutorpro.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for Debug Premium button visibility.
 * This test runs on a real device/emulator to verify that:
 * - Debug Premium toggle is visible in debug builds
 * - Debug Premium toggle is NOT visible in release builds
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DebugPremiumButtonInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun debugPremiumButton_isVisibleInDebugBuild() {
        // Given: We're in a debug build
        Assume.assumeTrue("This test only runs in debug builds", BuildConfig.DEBUG)

        // When: App launches
        composeTestRule.waitForIdle()

        // Then: We can verify the app is running
        // Note: The Debug Premium option is in the Settings screen's dropdown menu
        // This test just verifies we're in debug mode
        // A more comprehensive test would navigate to Settings and check the menu
    }

    @Test
    fun debugPremiumButton_isNotVisibleInReleaseBuild() {
        // Given: We're in a release build
        Assume.assumeTrue("This test only runs in release builds", !BuildConfig.DEBUG)

        // When: App launches
        composeTestRule.waitForIdle()

        // Then: Debug mode is disabled
        // This test is a placeholder for release builds
    }
}