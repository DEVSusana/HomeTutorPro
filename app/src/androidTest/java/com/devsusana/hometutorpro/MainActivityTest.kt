package com.devsusana.hometutorpro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test suite for validating MainActivity startup, theme initialization,
 * and navigation rendering.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    /** Hilt DI rule for injecting test dependencies before each test. */
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    /** Compose test rule that launches [MainActivity] for UI assertions. */
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Initializes the Hilt dependency injection graph.
     */
    @Before
    fun setUp() {
        hiltRule.inject()
    }

    /**
     * Verifies that the activity launches successfully, loads the theme,
     * and displays the splash screen content.
     */
    @Test
    fun testActivityLaunchAndSplashContent() {
        // Assert that the app icon content description exists, verifying the navigation host renders Splash
        val appIconDescription = composeTestRule.activity.getString(R.string.cd_app_icon)
        composeTestRule.onNodeWithContentDescription(appIconDescription).assertExists()

        // Assert that the app name text is displayed
        val appNameText = composeTestRule.activity.getString(R.string.app_name)
        composeTestRule.onNodeWithText(appNameText).assertExists()
    }
}
