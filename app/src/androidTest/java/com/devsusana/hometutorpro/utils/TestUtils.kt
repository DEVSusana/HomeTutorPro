package com.devsusana.hometutorpro.utils

import androidx.compose.ui.test.SemanticsMatcher

/**
 * Creates a [SemanticsMatcher] that verifies if a node's test tag matches the given [pattern].
 *
 * @param pattern The regular expression pattern to match.
 */
fun hasTestTag(pattern: Regex): SemanticsMatcher {
    return SemanticsMatcher("TestTag matches pattern '$pattern'") { node ->
        val testTag = node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }
        testTag.matches(pattern)
    }
}
