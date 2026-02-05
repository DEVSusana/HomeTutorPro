package com.devsusana.hometutorpro.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assert
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.domain.entities.Student
import com.devsusana.hometutorpro.presentation.student_list.components.ModernStudentCard
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModernStudentCardInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun modernStudentCard_displaysPositiveBalanceCorrectly() {
        val student = Student(
            id = "1",
            name = "John Doe",
            pendingBalance = 150.0,
            isActive = true
        )

        composeTestRule.setContent {
            HomeTutorProTheme {
                ModernStudentCard(student = student, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("150.00€").assertIsDisplayed()
        // Further checks for color would require custom matchers or checking internal implementation,
        // which is often brittle. Asserting text presence is usually sufficient for UI tests.
    }

    @Test
    fun modernStudentCard_displaysNegativeBalanceCorrectly() {
        val student = Student(
            id = "2",
            name = "Jane Doe",
            pendingBalance = -50.0,
            isActive = true
        )

        composeTestRule.setContent {
            HomeTutorProTheme {
                ModernStudentCard(student = student, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("-50.00€").assertIsDisplayed()
    }

    @Test
    fun modernStudentCard_displaysZeroBalanceCheckmarkCorrectly() {
        val student = Student(
            id = "3",
            name = "Peter Pan",
            pendingBalance = 0.0,
            isActive = true
        )

        composeTestRule.setContent {
            HomeTutorProTheme {
                ModernStudentCard(student = student, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("✓").assertIsDisplayed()
    }

    @Test
    fun modernStudentCard_inactiveStudentWithPositiveBalance() {
        val student = Student(
            id = "4",
            name = "Inactive John",
            pendingBalance = 75.0,
            isActive = false
        )

        composeTestRule.setContent {
            HomeTutorProTheme {
                ModernStudentCard(student = student, onClick = {})
            }
        }
        composeTestRule.onNodeWithText("75.00€").assertIsDisplayed()
        composeTestRule.onNodeWithText("INACTIVE").assertIsDisplayed()
    }
}
