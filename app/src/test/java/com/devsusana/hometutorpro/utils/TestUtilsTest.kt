package com.devsusana.hometutorpro.utils

import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the custom Compose test matcher [hasTestTag].
 */
class TestUtilsTest {

    /**
     * Verifies that the matcher returns true when a node's test tag matches the given regex pattern.
     */
    @Test
    fun hasTestTag_matchingPattern_returnsTrue() {
        val matcher = hasTestTag("student_item_\\d+".toRegex())
        val mockNode = mockk<SemanticsNode>()
        val mockConfig = mockk<SemanticsConfiguration>()
        
        every { mockNode.config } returns mockConfig
        every { mockConfig.getOrElse(SemanticsProperties.TestTag, any()) } returns "student_item_42"
        
        val result = matcher.matches(mockNode)
        assertTrue(result)
    }

    /**
     * Verifies that the matcher returns false when a node's test tag does not match the given regex pattern.
     */
    @Test
    fun hasTestTag_nonMatchingPattern_returnsFalse() {
        val matcher = hasTestTag("student_item_\\d+".toRegex())
        val mockNode = mockk<SemanticsNode>()
        val mockConfig = mockk<SemanticsConfiguration>()
        
        every { mockNode.config } returns mockConfig
        every { mockConfig.getOrElse(SemanticsProperties.TestTag, any()) } returns "student_item_abc"
        
        val result = matcher.matches(mockNode)
        assertFalse(result)
    }

    /**
     * Verifies that the matcher returns false when a node has no test tag configured.
     */
    @Test
    fun hasTestTag_missingTag_returnsFalse() {
        val matcher = hasTestTag("student_item_\\d+".toRegex())
        val mockNode = mockk<SemanticsNode>()
        val mockConfig = mockk<SemanticsConfiguration>()
        
        every { mockNode.config } returns mockConfig
        every { mockConfig.getOrElse(SemanticsProperties.TestTag, any()) } returns ""
        
        val result = matcher.matches(mockNode)
        assertFalse(result)
    }
}
