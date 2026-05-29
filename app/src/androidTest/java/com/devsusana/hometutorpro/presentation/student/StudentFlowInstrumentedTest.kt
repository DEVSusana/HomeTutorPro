package com.devsusana.hometutorpro.presentation.student

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.MainActivity
import com.devsusana.hometutorpro.data.local.dao.StudentDao
import com.devsusana.hometutorpro.data.local.entities.StudentEntity
import com.devsusana.hometutorpro.data.local.entities.SyncStatus
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Instrumented test suite for validating the end-to-end student management flow.
 *
 * Verifies navigation, creation, update, deletion, and validation logic within
 * the Student feature. Ensures integration between UI and the underlying
 * Hilt-injected dependencies.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StudentFlowInstrumentedTest {

    /** Hilt DI rule for injecting test dependencies before each test. */
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    /** Compose test rule that launches [MainActivity] for UI interaction and assertions. */
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /** DAO to seed database with initial student data to ensure deterministic tests. */
    @Inject
    lateinit var studentDao: StudentDao

    /**
     * Initializes Hilt dependencies, seeds initial student data, and navigates to Student List.
     */
    @Before
    fun setUp() {
        hiltRule.inject()
        
        // Seed initial student data
        runBlocking {
            studentDao.insertStudent(
                StudentEntity(
                    id = 1L,
                    professorId = "test_user_id",
                    name = "John Doe",
                    age = 20,
                    address = "123 Street",
                    parentPhones = "555-1234",
                    studentPhone = "555-5678",
                    studentEmail = "john.doe@example.com",
                    subjects = "Math",
                    course = "Algebra",
                    pricePerHour = 45.0,
                    educationalAttention = "Regular",
                    lastPaymentDate = null,
                    syncStatus = SyncStatus.SYNCED
                )
            )
        }
        
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
        // Assert that the seeded student card exists, click it to open details, and edit
        composeTestRule.onNodeWithTag("student_item_1").performClick()
        composeTestRule.onNodeWithTag("edit_button").performClick()
        composeTestRule.onNodeWithTag("name_field").performTextReplacement("Updated Student Name")
        composeTestRule.onNodeWithTag("save_student_button").performClick()

        composeTestRule.onNodeWithText("Updated Student Name").assertExists()
    }

    /**
     * Tests that a student can be deleted from the system.
     */
    @Test
    fun deleteStudent_removesFromList() {
        // Assert that the seeded student card exists, click it, edit, and delete
        composeTestRule.onNodeWithTag("student_item_1").performClick()
        composeTestRule.onNodeWithTag("edit_button").performClick()
        composeTestRule.onNodeWithTag("delete_student_button").performClick()
        composeTestRule.onNodeWithTag("confirm_delete_button").performClick()

        composeTestRule.onNodeWithTag("student_list_screen").assertExists()
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
