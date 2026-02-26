package com.devsusana.hometutorpro.presentation.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.MainActivity
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DashboardContentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun showsNoUpcomingClassesMessage_whenNextClassIsNull() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val message = context.getString(R.string.dashboard_no_upcoming_classes)

        composeTestRule.setContent {
            HomeTutorProTheme {
                DashboardContent(
                    state = DashboardState(
                        userName = "User",
                        todayPendingClassesCount = 0,
                        totalPendingIncome = 0.0,
                        nextClass = null
                    ),
                    onNavigateToStudents = {},
                    onNavigateToResources = {},
                    onAddStudent = {},
                    onNavigateToNotes = {},
                    onScheduleClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun showsGreetingWithUserName() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val hello = context.getString(R.string.dashboard_hello)
        val expected = "$hello, Alex"

        composeTestRule.setContent {
            HomeTutorProTheme {
                DashboardContent(
                    state = DashboardState(
                        userName = "Alex",
                        todayPendingClassesCount = 0,
                        totalPendingIncome = 0.0,
                        nextClass = null
                    ),
                    onNavigateToStudents = {},
                    onNavigateToResources = {},
                    onAddStudent = {},
                    onNavigateToNotes = {},
                    onScheduleClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }
}
