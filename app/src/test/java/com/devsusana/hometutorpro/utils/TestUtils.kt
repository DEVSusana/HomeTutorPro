package com.devsusana.hometutorpro.utils

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher

/**
 * Creates a [SemanticsMatcher] that verifies if a node's test tag matches the given [pattern].
 *
 * This is a duplicate of the androidTest version, provided here so that JVM unit tests
 * in the `test` source set can reference and verify the matcher logic.
 *
 * @param pattern The regular expression pattern to match against the node's test tag.
 * @return A [SemanticsMatcher] that matches nodes whose test tag satisfies the regex.
 */
fun hasTestTag(pattern: Regex): SemanticsMatcher {
    return SemanticsMatcher("TestTag matches pattern '$pattern'") { node ->
        val testTag = node.config.getOrElse(SemanticsProperties.TestTag) { "" }
        testTag.matches(pattern)
    }
}
