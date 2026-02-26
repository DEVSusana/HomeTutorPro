package com.devsusana.hometutorpro.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assert
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.domain.entities.StudentSummary
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
        val student = StudentSummary(
            id = "1",
            name = "John Doe",
            subjects = "Math",
            color = null,
            pendingBalance = 150.0,
            pricePerHour = 0.0,
            isActive = true,
            lastClassDate = null
        )

        composeTestRule.setContent {
            HomeTutorProTheme {
                ModernStudentCard(student = student, onClick = {})
            }
        }

        val expectedText = "%.2f€".format(150.0)
        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }

    @Test
    fun modernStudentCard_displaysNegativeBalanceCorrectly() {
        val student = StudentSummary(
            id = "2",
            name = "Jane Doe",
            subjects = "Math",
            color = null,
            pendingBalance = -50.0,
            pricePerHour = 0.0,
            isActive = true,
            lastClassDate = null
        )

        composeTestRule.setContent {
            HomeTutorProTheme {
                ModernStudentCard(student = student, onClick = {})
            }
        }

        val expectedText = "%.2f€".format(-50.0)
        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }

    @Test
    fun modernStudentCard_displaysZeroBalanceCheckmarkCorrectly() {
        val student = StudentSummary(
            id = "3",
            name = "Peter Pan",
            subjects = "Math",
            color = null,
            pendingBalance = 0.0,
            pricePerHour = 0.0,
            isActive = true,
            lastClassDate = null
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
        val student = StudentSummary(
            id = "4",
            name = "Inactive John",
            subjects = "Math",
            color = null,
            pendingBalance = 75.0,
            pricePerHour = 0.0,
            isActive = false,
            lastClassDate = null
        )

        composeTestRule.setContent {
            HomeTutorProTheme {
                ModernStudentCard(student = student, onClick = {})
            }
        }
        val expectedText = "%.2f€".format(75.0)
        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
        
        // Use localized string to avoid failures in non-English locales
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val inactiveText = context.getString(com.devsusana.hometutorpro.R.string.student_status_inactive).uppercase()
        composeTestRule.onNodeWithText(inactiveText).assertIsDisplayed()
    }
}
