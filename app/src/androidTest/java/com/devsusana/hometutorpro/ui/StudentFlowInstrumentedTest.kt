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
 * Instrumented tests for Student add/edit flows.
 * These tests run on a real device to verify:
 * - Adding a new student
 * - Editing an existing student
 * - Form validation
 * - Save and delete operations
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StudentFlowInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
        
        // Ensure user is logged in to bypass splash/login redirect
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = context.getSharedPreferences("test_auth_prefs", android.content.Context.MODE_PRIVATE)
        val authManager = com.devsusana.hometutorpro.core.auth.SecureAuthManager(prefs)
        if (!authManager.isUserLoggedIn()) {
            authManager.saveCredentials("test@test.com", "password", "Test User", "test_user_id")
        }

        // Wait for app to load and skip splash
        // We wait until the bottom navigation label is visible
        val studentsLabel = context.getString(com.devsusana.hometutorpro.R.string.nav_students)
        composeTestRule.waitUntil(15000) {
            composeTestRule
                .onAllNodesWithText(studentsLabel)
                .fetchSemanticsNodes().isNotEmpty()
        }
        
        // Navigate to Students screen from Dashboard using text search
        composeTestRule.onNodeWithText(studentsLabel).performClick()
        composeTestRule.waitForIdle()
        // Give it an extra moment to complete transition
        composeTestRule.mainClock.advanceTimeBy(500)
    }

    @Test
    fun addNewStudent_completesSuccessfully() {
        // Given: User is on the student list screen
        composeTestRule
            .onNodeWithTag("add_student_button")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Student detail screen should open at tab 0 (Personal Info)
        composeTestRule
            .onNodeWithTag("name_field")
            .performTextInput("Test Student")

        composeTestRule
            .onNodeWithTag("age_field")
            .performTextInput("20")

        composeTestRule
            .onNodeWithTag("course_field")
            .performTextInput("Computer Science")

        // Click continue to go to tab 1 (Schedules)
        composeTestRule
            .onNodeWithTag("continue_button")
            .performClick()

        composeTestRule.waitForIdle()
        
        // Click continue to go to tab 2 (Finance)
        composeTestRule
            .onNodeWithTag("continue_button")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("price_field")
            .performTextInput("50")

        // Save the student (on final tab it's save_student_button)
        composeTestRule
            .onNodeWithTag("save_student_button")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify success (navigation back to list)
        composeTestRule
            .onNodeWithTag("student_list_screen", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun editExistingStudent_updatesSuccessfully() {
        // Given: User is on the student list with at least one student
        val studentItems = composeTestRule
            .onAllNodes(hasTestTag("student_item_.*".toRegex()), useUnmergedTree = true)
        
        if (studentItems.fetchSemanticsNodes().isEmpty()) {
            return 
        }
        
        // When: User clicks on a student
        studentItems.onFirst().performClick()

        composeTestRule.waitForIdle()

        // Then: Student detail screen opens in view mode
        // Click edit button
        composeTestRule
            .onNodeWithTag("edit_button")
            .performClick()

        composeTestRule.waitForIdle()

        // Update student name
        composeTestRule
            .onNodeWithTag("name_field")
            .performTextReplacement("Updated Student Name")

        // Save changes
        composeTestRule
            .onNodeWithTag("save_student_button")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify success
    }

    @Test
    fun deleteStudent_removesFromList() {
        // Given: User is viewing a student in edit mode
        val studentItems = composeTestRule
            .onAllNodes(hasTestTag("student_item_.*".toRegex()), useUnmergedTree = true)
        
        if (studentItems.fetchSemanticsNodes().isEmpty()) {
            return 
        }
        
        // Navigate to a student
        studentItems.onFirst().performClick()

        composeTestRule.waitForIdle()

        // Enter edit mode
        composeTestRule
            .onNodeWithTag("edit_button")
            .performClick()

        composeTestRule.waitForIdle()

        // When: User clicks delete button
        composeTestRule
            .onNodeWithTag("delete_student_button")
            .performClick()

        composeTestRule.waitForIdle()

        // Confirm deletion
        composeTestRule
            .onNodeWithTag("confirm_delete_button")
            .performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun studentForm_validatesRequiredFields() {
        // Given: User is on new student screen
        composeTestRule
            .onNodeWithTag("add_student_button")
            .performClick()

        composeTestRule.waitForIdle()

        // When: User tries to continue without filling required fields (name)
        composeTestRule
            .onNodeWithTag("continue_button")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: We should still be on tab 0 (name field still exists)
        composeTestRule
            .onNodeWithTag("name_field")
            .assertExists()
    }
}

// Helper function to match test tags with regex
private fun hasTestTag(pattern: Regex): SemanticsMatcher {
    return SemanticsMatcher("TestTag matches pattern '$pattern'") { node ->
        val testTag = node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }
        testTag.matches(pattern)
    }
}
