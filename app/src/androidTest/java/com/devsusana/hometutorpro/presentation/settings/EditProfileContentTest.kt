package com.devsusana.hometutorpro.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devsusana.hometutorpro.MainActivity
import com.devsusana.hometutorpro.ui.theme.HomeTutorProTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for EditProfileContent.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EditProfileContentTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun saveButton_emitsSaveEvent() {
        val events = mutableListOf<EditProfileUiEvent>()

        composeTestRule.setContent {
            HomeTutorProTheme {
                EditProfileContent(
                    state = EditProfileState(),
                    onBackClick = {},
                    onEvent = { events.add(it) }
                )
            }
        }

        composeTestRule.onNodeWithTag("save_button").performClick()
        assertTrue(events.contains(EditProfileUiEvent.SaveProfile))
    }

    @Test
    fun feedbackDialog_showsSuccessMessage() {
        composeTestRule.setContent {
            HomeTutorProTheme {
                EditProfileContent(
                    state = EditProfileState(successMessage = "Saved"),
                    onBackClick = {},
                    onEvent = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Saved").assertIsDisplayed()
    }
}
