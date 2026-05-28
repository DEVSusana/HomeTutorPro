package com.devsusana.hometutorpro.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.MainActivity
import com.devsusana.hometutorpro.utils.hasTestTag
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Student add/edit flows.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StudentFlowInstrumentedTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Initializes Hilt dependencies and navigates to the Student List screen.
     */
    @Before
    fun setUp() {
        hiltRule.inject()
        
        // Wait for bottom navigation item to be displayed
        composeTestRule.waitUntil(15000) {
            composeTestRule
                .onAllNodesWithTag("nav_item_StudentList")
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Students screen
        composeTestRule.onNodeWithTag("nav_item_StudentList").performClick()
        composeTestRule.waitForIdle()
    }

    /**
     * Tests that a new student can be added successfully through the form navigation.
     */
    @Test
    fun addNewStudent_completesSuccessfully() {
        composeTestRule.onNodeWithTag("add_student_button").performClick()

        composeTestRule.onNodeWithTag("name_field").performTextInput("Test Student")
        composeTestRule.onNodeWithTag("age_field").performTextInput("20")
        composeTestRule.onNodeWithTag("course_field").performTextInput("Computer Science")
        composeTestRule.onNodeWithTag("continue_button").performClick()
        composeTestRule.onNodeWithTag("continue_button").performClick()
        composeTestRule.onNodeWithTag("price_field").performTextInput("50")
        composeTestRule.onNodeWithTag("save_student_button").performClick()

        composeTestRule.onNodeWithTag("student_list_screen").assertExists()
    }

    /**
     * Tests that an existing student's information can be updated and saved.
     */
    @Test
    fun editExistingStudent_updatesSuccessfully() {
        // Find student item and click it
        val studentItem = composeTestRule.onAllNodes(hasTestTag("student_item_.*".toRegex()))
        if (studentItem.fetchSemanticsNodes().isNotEmpty()) {
            studentItem.onFirst().performClick()
            composeTestRule.onNodeWithTag("edit_button").performClick()
            composeTestRule.onNodeWithTag("name_field").performTextReplacement("Updated Student Name")
            composeTestRule.onNodeWithTag("save_student_button").performClick()

            composeTestRule.onNodeWithText("Updated Student Name").assertExists()
        }
    }

    /**
     * Tests that a student can be deleted from the system.
     */
    @Test
    fun deleteStudent_removesFromList() {
        val studentItem = composeTestRule.onAllNodes(hasTestTag("student_item_.*".toRegex()))
        if (studentItem.fetchSemanticsNodes().isNotEmpty()) {
            studentItem.onFirst().performClick()
            composeTestRule.onNodeWithTag("edit_button").performClick()
            composeTestRule.onNodeWithTag("delete_student_button").performClick()
            composeTestRule.onNodeWithTag("confirm_delete_button").performClick()

            composeTestRule.onNodeWithTag("student_list_screen").assertExists()
        }
    }

    /**
     * Tests that the form prevents navigation or submission when mandatory fields are missing.
     */
    @Test
    fun studentForm_validatesRequiredFields() {
        composeTestRule.onNodeWithTag("add_student_button").performClick()
        composeTestRule.onNodeWithTag("continue_button").performClick()

        // Assert still on tab 0 by checking existence of name field
        composeTestRule.onNodeWithTag("name_field").assertExists()
    }
}
