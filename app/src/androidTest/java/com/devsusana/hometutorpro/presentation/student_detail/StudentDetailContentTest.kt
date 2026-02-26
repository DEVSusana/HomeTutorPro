package com.devsusana.hometutorpro.presentation.student_detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.MainActivity
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_detail.components.StudentDetailContent
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StudentDetailContentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun newStudent_showsContinueButton() {
        val state = StudentDetailState(
            student = Student(id = "new"),
            currentTab = 0
        )

        composeTestRule.setContent {
            HomeTutorProTheme {
                StudentDetailContent(
                    state = state,
                    onEvent = {},
                    onBack = {},
                    isEditMode = true,
                    onToggleEditMode = {},
                    onSetEditMode = {},
                    showPaymentDialog = false,
                    selectedPaymentType = null,
                    onPaymentClick = {},
                    onDismissPaymentDialog = {},
                    showStartClassDialog = false,
                    onStartClassClick = {},
                    onDismissStartClassDialog = {},
                    showDeleteDialog = false,
                    onShowDeleteDialog = {},
                    onDismissDeleteDialog = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("continue_button").assertIsDisplayed()
    }

    @Test
    fun existingStudent_showsSaveButton_whenEditMode() {
        val state = StudentDetailState(
            student = Student(id = "1", name = "Student"),
            currentTab = 0
        )

        composeTestRule.setContent {
            HomeTutorProTheme {
                StudentDetailContent(
                    state = state,
                    onEvent = {},
                    onBack = {},
                    isEditMode = true,
                    onToggleEditMode = {},
                    onSetEditMode = {},
                    showPaymentDialog = false,
                    selectedPaymentType = null,
                    onPaymentClick = {},
                    onDismissPaymentDialog = {},
                    showStartClassDialog = false,
                    onStartClassClick = {},
                    onDismissStartClassDialog = {},
                    showDeleteDialog = false,
                    onShowDeleteDialog = {},
                    onDismissDeleteDialog = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("save_student_button").assertIsDisplayed()
    }
}
