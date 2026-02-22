package com.devsusana.hometutorpro.presentation.student_detail.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek

class AddExtraClassDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

        // Click on the Day of Week dropdown (it's read-only OutlinedTextField, clicking it expands menu)
        composeTestRule.onNodeWithText("Día de la semana", substring = true, ignoreCase = true)
            .assertExists()
            .performClick()
            
        // Select Monday (Lunes)
        composeTestRule.onNodeWithText("Lunes", substring = true, ignoreCase = true)
            .assertExists()
            .performClick()

        // Click Save
        composeTestRule.onNodeWithText("Guardar", substring = true, ignoreCase = true)
            .assertExists()
            .performClick()

        assert(confirmedDay == DayOfWeek.MONDAY)
    }
}
