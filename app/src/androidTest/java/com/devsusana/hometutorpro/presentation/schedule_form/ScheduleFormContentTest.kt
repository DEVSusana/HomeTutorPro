package com.devsusana.hometutorpro.presentation.schedule_form

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.MainActivity
import com.devsusana.hometutorpro.presentation.schedule.ScheduleFormState
import com.devsusana.hometutorpro.presentation.schedule_form.components.ScheduleFormContent
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ScheduleFormContentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun saveButton_isDisabled_whenLoading() {
        composeTestRule.setContent {
            HomeTutorProTheme {
                ScheduleFormContent(
                    state = ScheduleFormState(isLoading = true),
                    onDayOfWeekChange = {},
                    onStartTimeChange = {},
                    onEndTimeChange = {},
                    onSaveSchedule = {},
                    onBack = {},
                    showStartTimePicker = false,
                    onShowStartTimePickerChange = {},
                    showEndTimePicker = false,
                    onShowEndTimePickerChange = {},
                    isDayOfWeekMenuExpanded = false,
                    onDayOfWeekMenuExpandedChange = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("save_schedule_button").assertIsNotEnabled()
    }
}
