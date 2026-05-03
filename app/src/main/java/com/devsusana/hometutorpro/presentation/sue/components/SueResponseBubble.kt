package com.devsusana.hometutorpro.presentation.sue.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Chat bubble displaying Sue's response, with an animated typing indicator
 * when the response is being generated.
 *
 * @param response The text response from Sue. Empty string triggers typing animation.
 * @param isTyping Whether Sue is currently generating a response (shows dots animation).
 * @param isVisible Whether the bubble should be shown.
 * @param modifier Modifier for layout customization.
 */
@Composable
fun SueResponseBubble(
    response: String,
    isTyping: Boolean,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) +
                slideInVertically(animationSpec = tween(300)) { it / 2 }
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sue",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )

                    if (isTyping) {
                        TypingIndicator()
                    } else {
                        Text(
                            text = response,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated "typing..." indicator with pulsing dots.
 */
@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "Typing")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400,
                        delayMillis = index * 150
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "TypingDot$index"
            )

            Text(
                text = "●",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.alpha(alpha)
            )

            if (index < 2) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "Response Bubble - Text")
@Composable
private fun SueResponseBubblePreview() {
    MaterialTheme {
        SueResponseBubble(
            response = "Tienes 12 alumnos activos actualmente.",
            isTyping = false,
            isVisible = true
        )
    }
}

@Preview(showBackground = true, name = "Response Bubble - Typing")
@Composable
private fun SueResponseBubbleTypingPreview() {
    MaterialTheme {
        SueResponseBubble(
            response = "",
            isTyping = true,
            isVisible = true
        )
    }
}
