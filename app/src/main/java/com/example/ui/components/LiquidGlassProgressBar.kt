package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Clean, responsive Spotify-style Seek Bar with zero-jank dragging and A-B loop markers.
 */
@Composable
fun LiquidGlassProgressBar(
    currentPositionProvider: () -> Long,
    durationMs: Long,
    palette: AmbientPalette,
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() },
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    waveformEnvelope: FloatArray? = null,
    loopStartMs: Long? = null,
    loopEndMs: Long? = null
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var displayedPositionMs by remember { mutableLongStateOf(currentPositionProvider()) }

    val latestPositionProvider by rememberUpdatedState(currentPositionProvider)

    LaunchedEffect(isDragging, safeDuration) {
        while (isActive) {
            if (!isDragging) {
                displayedPositionMs = latestPositionProvider().coerceIn(0L, safeDuration)
            }
            delay(100L)
        }
    }

    val activeFraction = if (isDragging) {
        dragFraction.coerceIn(0f, 1f)
    } else {
        (displayedPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    }

    val effectiveTimeMs = if (isDragging) {
        (dragFraction * safeDuration).toLong().coerceIn(0L, safeDuration)
    } else {
        displayedPositionMs
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val targetMs = (fraction * safeDuration).toLong()
                        displayedPositionMs = targetMs
                        onSeekTo(targetMs)
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = dragAmount / size.width.toFloat()
                            dragFraction = (dragFraction + delta).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val targetMs = (dragFraction * safeDuration).toLong().coerceIn(0L, safeDuration)
                            displayedPositionMs = targetMs
                            isDragging = false
                            onSeekTo(targetMs)
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackHeight = if (isDragging) 5.dp.toPx() else 4.dp.toPx()
                val centerY = size.height / 2f
                val cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)

                // Unplayed background track
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.24f),
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(size.width, trackHeight),
                    cornerRadius = cornerRadius
                )

                // Optional A-B Loop Highlight Segment
                if (loopStartMs != null) {
                    val startFrac = (loopStartMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
                    val endFrac = ((loopEndMs ?: safeDuration).toFloat() / safeDuration.toFloat()).coerceIn(startFrac, 1f)
                    val startX = size.width * startFrac
                    val endX = size.width * endFrac
                    drawRoundRect(
                        color = AuraSpotifyGreen.copy(alpha = 0.45f),
                        topLeft = Offset(startX, centerY - trackHeight),
                        size = Size((endX - startX).coerceAtLeast(4f), trackHeight * 2f),
                        cornerRadius = cornerRadius
                    )
                }

                // Active played fill
                val fillWidth = (size.width * activeFraction).coerceIn(0f, size.width)
                drawRoundRect(
                    color = if (isDragging) AuraSpotifyGreen else Color.White,
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(fillWidth, trackHeight),
                    cornerRadius = cornerRadius
                )

                // Interactive Thumb
                val thumbRadius = if (isDragging) 7.dp.toPx() else 5.dp.toPx()
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius,
                    center = Offset(fillWidth.coerceIn(thumbRadius, size.width - thumbRadius), centerY)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatSeekTimestamp(effectiveTimeMs),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = formatSeekTimestamp(safeDuration),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

private fun formatSeekTimestamp(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}

