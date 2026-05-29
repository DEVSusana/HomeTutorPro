package com.devsusana.hometutorpro.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented unit tests for the custom Compose test matcher [hasTestTag].
 * Uses a lightweight compose test rule to verify matcher behavior on rendered layout nodes.
 */
@RunWith(AndroidJUnit4::class)
class TestUtilsTest {

    /** Compose test rule to render components for semantics matching. */
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Verifies that the matcher successfully matches a node when its tag satisfies the regex.
     */
    @Test
    fun hasTestTag_matchingRegex_successfullyMatches() {
        composeTestRule.setContent {
            Box(modifier = Modifier.testTag("student_item_123"))
        }

        composeTestRule.onNode(hasTestTag("student_item_\\d+".toRegex())).assertExists()
    }

    /**
     * Verifies that the matcher does not match a node when its tag does not satisfy the regex.
     */
    @Test
    fun hasTestTag_nonMatchingRegex_failsToMatch() {
        composeTestRule.setContent {
            Box(modifier = Modifier.testTag("student_item_abc"))
        }

        composeTestRule.onNode(hasTestTag("student_item_\\d+".toRegex())).assertDoesNotExist()
    }

    /**
     * Verifies that an empty or missing test tag is handled gracefully and returns no match.
     */
    @Test
    fun hasTestTag_emptyOrMissingTag_doesNotMatch() {
        composeTestRule.setContent {
            Box(modifier = Modifier) // No test tag
        }

        composeTestRule.onNode(hasTestTag("student_item_\\d+".toRegex())).assertDoesNotExist()
    }
}
