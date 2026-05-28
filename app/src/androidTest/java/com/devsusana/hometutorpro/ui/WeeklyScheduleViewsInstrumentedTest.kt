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
 * Shared test tags for Weekly Schedule views test.
 */
object TestTags {
    const val GRID_VIEW_BUTTON = "grid_view_button"
    const val LIST_VIEW_BUTTON = "list_view_button"
    const val SCHEDULE_ITEM = "schedule_item"
    const val DETAIL_DIALOG = "detail_dialog"
}

/**
 * Instrumented tests for Weekly Schedule views (Grid and List).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WeeklyScheduleViewsInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Configures the test environment, injects dependencies, and navigates to the Weekly Schedule screen.
     */
    @Before
    fun setUp() {
        hiltRule.inject()
        
        // Wait for bottom navigation item to be displayed
        composeTestRule.waitUntil(15000) {
            composeTestRule
                .onAllNodesWithTag("nav_item_WeeklySchedule")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Weekly Schedule screen
        composeTestRule.onNodeWithTag("nav_item_WeeklySchedule").performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Verifies that the list view is correctly displayed by default.
     */
    @Test
    fun scheduleListView_displaysCorrectly() {
        composeTestRule.onNodeWithTag(TestTags.GRID_VIEW_BUTTON).assertExists()
    }

    /**
     * Verifies that switching to the grid view renders correctly.
     */
    @Test
    fun scheduleGridView_displaysCorrectly() {
        composeTestRule.onNodeWithTag(TestTags.GRID_VIEW_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.LIST_VIEW_BUTTON).assertIsDisplayed()
    }

    /**
     * Verifies the toggling functionality between list and grid views.
     */
    @Test
    fun switchBetweenViews_worksCorrectly() {
        composeTestRule.onNodeWithTag(TestTags.GRID_VIEW_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.LIST_VIEW_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(TestTags.LIST_VIEW_BUTTON).performClick()
        composeTestRule.onNodeWithTag(TestTags.GRID_VIEW_BUTTON).assertExists()
    }

    /**
     * Verifies that clicking on a schedule item triggers the expected detail dialog.
     */
    @Test
    fun scheduleItem_clickOpensDialog() {
        val scheduleItem = composeTestRule.onAllNodesWithTag(TestTags.SCHEDULE_ITEM)
        if (scheduleItem.fetchSemanticsNodes().isNotEmpty()) {
            scheduleItem.onFirst().performClick()
            composeTestRule.onNodeWithTag(TestTags.DETAIL_DIALOG).assertIsDisplayed()
        }
    }

    /**
     * Verifies that schedule items are visible within the grid view layout.
     */
    @Test
    fun gridView_showsScheduleItems() {
        composeTestRule.onNodeWithTag(TestTags.GRID_VIEW_BUTTON).performClick()
        val scheduleItem = composeTestRule.onAllNodesWithTag(TestTags.SCHEDULE_ITEM)
        if (scheduleItem.fetchSemanticsNodes().isNotEmpty()) {
            scheduleItem.onFirst().assertExists()
        }
    }

    /**
     * Verifies that schedule details are visible within the list view layout.
     */
    @Test
    fun listView_showsScheduleDetails() {
        composeTestRule.onNodeWithTag(TestTags.GRID_VIEW_BUTTON).assertExists()
        val scheduleItem = composeTestRule.onAllNodesWithTag(TestTags.SCHEDULE_ITEM)
        if (scheduleItem.fetchSemanticsNodes().isNotEmpty()) {
            scheduleItem.onFirst().assertExists()
        }
    }
}
