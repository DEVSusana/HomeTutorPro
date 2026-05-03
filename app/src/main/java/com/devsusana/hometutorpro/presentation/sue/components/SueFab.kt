package com.devsusana.hometutorpro.presentation.sue.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import com.devsusana.hometutorpro.core.sue.SpeechState

/**
 * Animated Floating Action Button for the Sue AI agent.
 *
 * Visual states:
 * - **Idle:** Primary color with mic icon.
 * - **Listening:** Red with pulse animation.
 * - **Processing:** Tertiary color with smart toy icon.
 * - **Speaking:** Secondary color with smart toy icon.
 * - **Error:** Error color with mic-off icon.
 *
 * @param speechState The current [SpeechState] controlling visual appearance.
 * @param onClick Callback when the FAB is tapped.
 * @param modifier Modifier for layout customization.
 */
@Composable
fun SueFab(
    speechState: SpeechState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = when (speechState) {
            SpeechState.IDLE -> MaterialTheme.colorScheme.primaryContainer
            SpeechState.LISTENING -> MaterialTheme.colorScheme.errorContainer
            SpeechState.PROCESSING -> MaterialTheme.colorScheme.tertiaryContainer
            SpeechState.SPEAKING -> MaterialTheme.colorScheme.secondaryContainer
            SpeechState.ERROR -> MaterialTheme.colorScheme.errorContainer
        },
        animationSpec = tween(durationMillis = 300),
        label = "SueFabContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when (speechState) {
            SpeechState.IDLE -> MaterialTheme.colorScheme.onPrimaryContainer
            SpeechState.LISTENING -> MaterialTheme.colorScheme.onErrorContainer
            SpeechState.PROCESSING -> MaterialTheme.colorScheme.onTertiaryContainer
            SpeechState.SPEAKING -> MaterialTheme.colorScheme.onSecondaryContainer
            SpeechState.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        },
        animationSpec = tween(durationMillis = 300),
        label = "SueFabContentColor"
    )

    val icon = when (speechState) {
        SpeechState.IDLE -> Icons.Default.Mic
        SpeechState.LISTENING -> Icons.Default.Mic
        SpeechState.PROCESSING -> Icons.Default.SmartToy
        SpeechState.SPEAKING -> Icons.Default.SmartToy
        SpeechState.ERROR -> Icons.Default.MicOff
    }

    val pulseScale = if (speechState == SpeechState.LISTENING) {
        val infiniteTransition = rememberInfiniteTransition(label = "SuePulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "SuePulseScale"
        )
        scale
    } else {
        1f
    }

    // Pulse ring effect while listening
    val ringColor = if (speechState == SpeechState.LISTENING) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    val ringRadius = if (speechState == SpeechState.LISTENING) {
        val infiniteTransition = rememberInfiniteTransition(label = "SueRing")
        val radius by infiniteTransition.animateFloat(
            initialValue = 28f,
            targetValue = 40f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "SueRingRadius"
        )
        radius
    } else {
        0f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = CircleShape,
            modifier = Modifier
                .size(56.dp)
                .scale(pulseScale)
                .drawBehind {
                    if (ringRadius > 0f) {
                        drawCircle(
                            color = ringColor,
                            radius = ringRadius.dp.toPx()
                        )
                    }
                }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.sue_fab_content_description)
            )
        }
    }
}

@Preview(showBackground = true, name = "Sue FAB - Idle")
@Composable
private fun SueFabIdlePreview() {
    MaterialTheme {
        SueFab(speechState = SpeechState.IDLE, onClick = {})
    }
}

@Preview(showBackground = true, name = "Sue FAB - Listening")
@Composable
private fun SueFabListeningPreview() {
    MaterialTheme {
        SueFab(speechState = SpeechState.LISTENING, onClick = {})
    }
}

@Preview(showBackground = true, name = "Sue FAB - Processing")
@Composable
private fun SueFabProcessingPreview() {
    MaterialTheme {
        SueFab(speechState = SpeechState.PROCESSING, onClick = {})
    }
}
