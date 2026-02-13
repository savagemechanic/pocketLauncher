package com.github.codeworkscreativehub.mlauncher.voice.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/**
 * Bottom-anchored floating voice command overlay.
 *
 * Renders different states with animations:
 * - Listening: pulsing mic icon + live transcript
 * - Processing: spinner + transcript
 * - Success: checkmark, auto-dismiss 1.5s
 * - Error: red-tinted message, auto-dismiss 2s
 * - Confirmation: clarification text
 */
@Composable
fun VoiceOverlay(
    overlayStateFlow: StateFlow<VoiceOverlayState>,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val state by overlayStateFlow.collectAsState()
    val isVisible = state !is VoiceOverlayState.Hidden

    // Auto-dismiss for Success and Error states
    LaunchedEffect(state) {
        when (state) {
            is VoiceOverlayState.Success -> {
                delay(1500)
                onDismiss()
            }
            is VoiceOverlayState.Error -> {
                delay(2000)
                onDismiss()
            }
            else -> {}
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { it / 2 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 2 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(alpha = 0.85f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                when (val currentState = state) {
                    is VoiceOverlayState.Listening -> ListeningContent(currentState, accentColor)
                    is VoiceOverlayState.Processing -> ProcessingContent(currentState, accentColor)
                    is VoiceOverlayState.Success -> SuccessContent(accentColor)
                    is VoiceOverlayState.Error -> ErrorContent(currentState)
                    is VoiceOverlayState.Confirmation -> ConfirmationContent(currentState, accentColor)
                    is VoiceOverlayState.Hidden -> {}
                }
            }
        }
    }
}

@Composable
private fun ListeningContent(state: VoiceOverlayState.Listening, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulse"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "\uD83C\uDF99",
            fontSize = 28.sp,
            modifier = Modifier.scale(scale)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.transcript.ifEmpty { "Listening…" },
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProcessingContent(state: VoiceOverlayState.Processing, accentColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = accentColor,
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.transcript,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SuccessContent(accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✓",
            color = accentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Done",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ErrorContent(state: VoiceOverlayState.Error) {
    Text(
        text = state.message,
        color = Color(0xFFFF6B6B),
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ConfirmationContent(state: VoiceOverlayState.Confirmation, accentColor: Color) {
    Text(
        text = state.message,
        color = accentColor,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}
