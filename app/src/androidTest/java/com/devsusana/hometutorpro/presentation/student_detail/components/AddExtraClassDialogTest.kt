package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.devsusana.hometutorpro.MainActivity
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.presentation.utils.DayOfWeekUtils
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AddExtraClassDialogTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun dialog_displaysDayOfWeekSelection() {
        var confirmedDay: DayOfWeek? = null
        composeTestRule.setContent {
            AddExtraClassDialog(
                onDismiss = {},
                onConfirm = { _, _, _, dayOfWeek -> 
                    confirmedDay = dayOfWeek
                }
            )
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dayOfWeekLabel = context.getString(R.string.schedule_exception_day_of_week)
        val mondayLabel = context.getString(DayOfWeekUtils.getResourceId(DayOfWeek.MONDAY))
        val saveLabel = context.getString(R.string.save)

        // Click on the Day of Week dropdown (it's read-only OutlinedTextField, clicking it expands menu)
        composeTestRule.onNodeWithText(dayOfWeekLabel, substring = true, ignoreCase = true)
            .assertExists()
            .performClick()
            
        // Select Monday
        composeTestRule.onNodeWithText(mondayLabel, substring = true, ignoreCase = true)
            .assertExists()
            .performClick()

        // Click Save
        composeTestRule.onNodeWithText(saveLabel, substring = true, ignoreCase = true)
            .assertExists()
            .performClick()

        assert(confirmedDay == DayOfWeek.MONDAY)
    }
}
