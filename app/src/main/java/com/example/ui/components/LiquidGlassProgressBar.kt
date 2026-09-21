package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.AppTheme
import com.example.ui.theme.LocalAppTheme

/**
 * Liquid Glass Progress Bar & High-Precision Scrubber
 * - Buttery smooth drag scrubber with zero jumping or lag
 * - Real-time audio energy reactivity and radiant bloom
 * - Precise millisecond seeking
 */
@Composable
fun LiquidGlassProgressBar(
    currentPositionProvider: () -> Long,
    durationMs: Long,
    palette: AmbientPalette,
    analysisDataProvider: () -> AudioAnalysisData,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalAppTheme.current
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableStateOf(0f) }

    val currentMs = currentPositionProvider()
    val safeDuration = durationMs.coerceAtLeast(1L)
    val actualProgress = (currentMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayedProgress = if (isScrubbing) scrubProgress else actualProgress

    val barHeight by animateFloatAsState(
        targetValue = if (isScrubbing) 8.5f else 5.5f,
        animationSpec = tween(150),
        label = "progressBarHeight"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(safeDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isScrubbing = true
                            scrubProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            scrubProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeekTo((scrubProgress * safeDuration).toLong())
                            isScrubbing = false
                        },
                        onDragCancel = {
                            isScrubbing = false
                        }
                    )
                }
                .pointerInput(safeDuration) {
                    detectTapGestures(
                        onPress = { offset ->
                            val target = (offset.x / size.width).coerceIn(0f, 1f)
                            isScrubbing = true
                            scrubProgress = target
                            tryAwaitRelease()
                            onSeekTo((target * safeDuration).toLong())
                            isScrubbing = false
                        }
                    )
                }
                .testTag("seek_slider"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight.dp)
            ) {
                val data = analysisDataProvider()
                val bassReactiveBoost = if (!isScrubbing) data.haloExpansion * 0.18f else 0.35f
                val corner = CornerRadius(barHeight.dp.toPx() / 2f)

                // Glass Track Background (Translucent layered base)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0x30FFFFFF),
                            Color(0x12FFFFFF)
                        )
                    ),
                    cornerRadius = corner,
                    size = size
                )

                // Track Border Sheen
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0x40FFFFFF),
                            Color(0x0AFFFFFF)
                        )
                    ),
                    cornerRadius = corner,
                    size = size,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )

                // Active Played Track (Glowing radiant liquid)
                val progressWidth = size.width * displayedProgress
                if (progressWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                palette.primary,
                                palette.accent
                            )
                        ),
                        cornerRadius = corner,
                        size = Size(progressWidth, size.height)
                    )

                    // Ambient bloom emitting above active track
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                palette.accent.copy(alpha = (0.35f + bassReactiveBoost).coerceIn(0f, 0.7f)),
                                Color.Transparent
                            )
                        ),
                        cornerRadius = corner,
                        size = Size(progressWidth, size.height)
                    )

                    // Scrubber Handle (LEGO Stud or Glow Dot)
                    val thumbX = progressWidth.coerceIn(0f, size.width)
                    val thumbRadius = (barHeight.dp.toPx() * (if (isScrubbing) 1.6f else 1.25f))

                    if (currentTheme == AppTheme.LEGO) {
                        // LEGO Molded Plastic Stud Scrubber Handle
                        val studCenter = Offset(thumbX, size.height / 2f)
                        // Drop shadow
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.65f),
                            radius = thumbRadius + 1.5f,
                            center = Offset(thumbX + 1.2f, size.height / 2f + 1.5f)
                        )
                        // Gold/Amber or Scarlet Stud Outer Rim
                        drawCircle(
                            color = palette.secondary,
                            radius = thumbRadius,
                            center = studCenter
                        )
                        // Specular Bevel Highlight
                        drawCircle(
                            color = Color.White.copy(alpha = 0.55f),
                            radius = thumbRadius * 0.78f,
                            center = Offset(thumbX - 0.8f, size.height / 2f - 0.8f)
                        )
                        // Stud Inner Mold Cavity
                        drawCircle(
                            color = Color(0xFF16171C),
                            radius = thumbRadius * 0.42f,
                            center = studCenter
                        )
                    } else {
                        drawCircle(
                            color = Color.White,
                            radius = thumbRadius * 0.75f,
                            center = Offset(thumbX, size.height / 2f)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    palette.accent.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            ),
                            radius = thumbRadius * 2.2f,
                            center = Offset(thumbX, size.height / 2f)
                        )
                    }
                }
            }
        }

        // Timestamps
        val displayedMs = if (isScrubbing) (scrubProgress * safeDuration).toLong() else currentMs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(displayedMs),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFA0A5BA),
                    fontSize = 12.sp
                )
            )
            Text(
                text = "-${formatDuration((safeDuration - displayedMs).coerceAtLeast(0L))}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFA0A5BA),
                    fontSize = 12.sp
                )
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
