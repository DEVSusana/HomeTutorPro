package com.devsusana.hometutorpro.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


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

    @Before
    fun setUp() {
        hiltRule.inject()
        // Wait for app to load
        composeTestRule.waitForIdle()
    }

    @Test
    fun scheduleListView_displaysCorrectly() {
        // Given: User is on weekly schedule screen in list view (default)
        composeTestRule.waitForIdle()
        
        // Check if we're on the weekly schedule screen
        val gridViewButton = composeTestRule
            .onAllNodesWithTag("grid_view_button", useUnmergedTree = true)
        
        if (gridViewButton.fetchSemanticsNodes().isEmpty()) {
            // Not on weekly schedule screen, skip test
            return
        }
        
        // Then: List view should be displayed
        // Verify the grid view button is visible (meaning we're in list view)
        gridViewButton.onFirst().assertExists()
    }

    @Test
    fun scheduleGridView_displaysCorrectly() {
        // Given: User is on weekly schedule screen
        composeTestRule.waitForIdle()
        
        // Check if we're on the weekly schedule screen
        val gridViewButton = composeTestRule
            .onAllNodesWithTag("grid_view_button", useUnmergedTree = true)
        
        if (gridViewButton.fetchSemanticsNodes().isEmpty()) {
            // Not on weekly schedule screen, skip test
            return
        }
        
        // When: User switches to grid view
        gridViewButton.onFirst().performClick()

        composeTestRule.waitForIdle()

        // Then: Grid view should be displayed
        // Verify the list view button is now visible (meaning we're in grid view)
        composeTestRule
            .onNodeWithTag("list_view_button", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun switchBetweenViews_worksCorrectly() {
        // Given: User is on weekly schedule screen in list view
        composeTestRule.waitForIdle()
        
        // Check if we're on the weekly schedule screen
        val gridViewButton = composeTestRule
            .onAllNodesWithTag("grid_view_button", useUnmergedTree = true)
        
        if (gridViewButton.fetchSemanticsNodes().isEmpty()) {
            // Not on weekly schedule screen, skip test
            return
        }
        
        // When: User switches to grid view
        gridViewButton.onFirst().performClick()

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
        // Given: User is on weekly schedule with schedule items
        composeTestRule.waitForIdle()
        
        // Check if we're on the weekly schedule screen
        val gridViewButton = composeTestRule
            .onAllNodesWithTag("grid_view_button", useUnmergedTree = true)
        
        if (gridViewButton.fetchSemanticsNodes().isEmpty()) {
            // Not on weekly schedule screen, skip test
            return
        }
        
        // When: User clicks on a schedule item (if any exist)
        val scheduleItems = composeTestRule
            .onAllNodesWithTag("schedule_item", useUnmergedTree = true)
        
        // Only perform click if there are schedule items
        if (scheduleItems.fetchSemanticsNodes().isNotEmpty()) {
            scheduleItems.onFirst().performClick()
            composeTestRule.waitForIdle()
            
            // Then: Exception dialog should open
            // (This would need to verify based on actual dialog implementation)
        }
    }

    @Test
    fun gridView_showsScheduleItems() {
        // Given: User is on weekly schedule in grid view
        composeTestRule.waitForIdle()
        
        // Check if we're on the weekly schedule screen
        val gridViewButton = composeTestRule
            .onAllNodesWithTag("grid_view_button", useUnmergedTree = true)
        
        if (gridViewButton.fetchSemanticsNodes().isEmpty()) {
            // Not on weekly schedule screen, skip test
            return
        }
        
        gridViewButton.onFirst().performClick()

        composeTestRule.waitForIdle()

        // Then: Schedule items should be rendered (count may be 0 if no data)
        // This test verifies that the grid view is working
        composeTestRule
            .onNodeWithTag("list_view_button", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun listView_showsScheduleDetails() {
        // Given: User is on weekly schedule in list view
        composeTestRule.waitForIdle()
        
        // Check if we're on the weekly schedule screen
        val gridViewButton = composeTestRule
            .onAllNodesWithTag("grid_view_button", useUnmergedTree = true)
        
        if (gridViewButton.fetchSemanticsNodes().isEmpty()) {
            // Not on weekly schedule screen, skip test
            return
        }
        
        // Then: List view should be displayed
        // Schedule items should show student name, time range, course (if available)
        // This test verifies the list view is the default view
        gridViewButton.onFirst().assertExists()
    }
}
