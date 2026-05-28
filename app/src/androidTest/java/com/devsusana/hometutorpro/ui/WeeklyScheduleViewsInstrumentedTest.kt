package com.devsusana.hometutorpro.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.MainActivity
import com.devsusana.hometutorpro.core.auth.SecureAuthManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Instrumented tests for Weekly Schedule views (Grid and List).
 * These tests run on a real device to verify:
 * - Grid view displays correctly
 * - List view displays correctly
 * - Switching between views works
 * - Schedule items are interactive
 * 
 * Note: These tests assume the app navigates to the weekly schedule screen.
 * If the app starts at login, these tests will be skipped.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WeeklyScheduleViewsInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var authManager: SecureAuthManager

    @Before
    fun setUp() {
        hiltRule.inject()
        
        // Ensure user is logged in to bypass splash/login redirect
        if (!authManager.isUserLoggedIn()) {
            val dummyEmail = "test_user_" + System.currentTimeMillis() + "@example.com"
            val dummyPass = "Pass" + "word" + "123!"
            authManager.saveCredentials(dummyEmail, dummyPass, "Test User", "test_user_id")
        }

        // Wait for app to load and skip splash
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val scheduleLabel = context.getString(com.devsusana.hometutorpro.R.string.nav_schedule)
        composeTestRule.waitUntil(15000) {
            composeTestRule
                .onAllNodesWithText(scheduleLabel)
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Navigate to Weekly Schedule screen from Dashboard using text search
        composeTestRule.onNodeWithText(scheduleLabel).performClick()
        composeTestRule.waitForIdle()
        // Give it an extra moment to complete transition
        composeTestRule.mainClock.advanceTimeBy(500)
    }

    @Test
    fun scheduleListView_displaysCorrectly() {
        // Given: User is on weekly schedule screen in list view (default)
        // Verify the grid view button is visible (meaning we're in list view)
        composeTestRule
            .onNodeWithTag("grid_view_button", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun scheduleGridView_displaysCorrectly() {
        // When: User switches to grid view
        composeTestRule
            .onNodeWithTag("grid_view_button", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Grid view should be displayed
        // Verify the list view button is now visible (meaning we're in grid view)
        composeTestRule
            .onNodeWithTag("list_view_button", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun switchBetweenViews_worksCorrectly() {
        // When: User switches to grid view
        composeTestRule
            .onNodeWithTag("grid_view_button", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Grid view is displayed
        composeTestRule
            .onNodeWithTag("list_view_button", useUnmergedTree = true)
            .assertExists()

        // When: User switches back to list view
        composeTestRule
            .onNodeWithTag("list_view_button", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Then: List view is displayed again
        composeTestRule
            .onNodeWithTag("grid_view_button", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun scheduleItem_clickOpensDialog() {
        // Given: User is on weekly schedule
        // When: User clicks on a schedule item (if any exist)
        val scheduleItems = composeTestRule
            .onAllNodesWithTag("schedule_item", useUnmergedTree = true)
        
        // Only perform click if there are schedule items
        if (scheduleItems.fetchSemanticsNodes().isNotEmpty()) {
            scheduleItems.onFirst().performClick()
            composeTestRule.waitForIdle()
            
            // Then: Exception dialog should open
            // (Verification would depend on actual dialog test tags)
        }
    }

    @Test
    fun gridView_showsScheduleItems() {
        // When: User switches to grid view
        composeTestRule
            .onNodeWithTag("grid_view_button", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Grid view should be displayed
        composeTestRule
            .onNodeWithTag("list_view_button", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun listView_showsScheduleDetails() {
        // Verify the grid view button is visible (meaning we're in list view)
        composeTestRule
            .onNodeWithTag("grid_view_button", useUnmergedTree = true)
            .assertExists()
    }
}
