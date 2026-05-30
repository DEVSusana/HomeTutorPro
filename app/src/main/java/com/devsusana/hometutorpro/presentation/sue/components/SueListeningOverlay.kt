package com.devsusana.hometutorpro.presentation.sue.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devsusana.hometutorpro.R
import kotlin.math.sin

/**
 * Visual feedback overlay displayed while Sue is listening for speech input.
 *
 * Shows animated audio waveforms, the partial transcription in real-time,
 * and a cancel button.
 *
 * @param partialTranscription Real-time partial transcription text.
 * @param isVisible Whether the listening overlay should be displayed.
 * @param onCancel Callback when the user cancels listening.
 * @param modifier Modifier for layout customization.
 */
@Composable
fun SueListeningOverlay(
    partialTranscription: String,
    isVisible: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header with cancel button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sue_listening_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.sue_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated audio waveform
            AudioWaveAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Partial transcription display
            if (partialTranscription.isNotBlank()) {
                Text(
                    text = partialTranscription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(R.string.sue_listening_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Animated waveform that simulates audio level visualization.
 */
@Composable
private fun AudioWaveAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "AudioWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AudioWavePhase"
    )

    val waveColor = MaterialTheme.colorScheme.primary
    val barCount = 20

    Canvas(modifier = modifier) {
        val barWidth = size.width / (barCount * 2f)
        val maxBarHeight = size.height * 0.8f

        for (i in 0 until barCount) {
            val x = (i * 2 + 1) * barWidth
            val normalizedPos = i.toFloat() / barCount
            val amplitude = (sin(phase + normalizedPos * 4f * Math.PI.toFloat()) + 1f) / 2f
            val barHeight = maxBarHeight * (0.2f + amplitude * 0.8f)

            drawLine(
                color = waveColor.copy(alpha = 0.5f + amplitude * 0.5f),
                start = Offset(x, (size.height - barHeight) / 2f),
                end = Offset(x, (size.height + barHeight) / 2f),
                strokeWidth = barWidth * 0.7f
            )
        }
    }
}

@Preview(showBackground = true, name = "Listening Overlay - With Transcription")
@Composable
private fun SueListeningOverlayPreview() {
    MaterialTheme {
        SueListeningOverlay(
            partialTranscription = "¿Cuántos alumnos tengo?",
            isVisible = true,
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, name = "Listening Overlay - Waiting")
@Composable
private fun SueListeningOverlayWaitingPreview() {
    MaterialTheme {
        SueListeningOverlay(
            partialTranscription = "",
            isVisible = true,
            onCancel = {}
        )
    }
}
