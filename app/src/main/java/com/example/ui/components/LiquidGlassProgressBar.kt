package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.AppTheme
import com.example.ui.theme.LocalAppTheme

@Composable
fun LiquidGlassProgressBar(
    currentPositionProvider: () -> Long,
    durationMs: Long,
    palette: AmbientPalette,
    analysisDataProvider: () -> AudioAnalysisData,
    onSeekTo: (Long) -> Unit,
    waveformEnvelope: FloatArray? = null,
    modifier: Modifier = Modifier
) {
    val currentTheme = LocalAppTheme.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var componentWidthPx by remember { mutableFloatStateOf(1f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val currentMs = currentPositionProvider()
    val playbackFraction = (currentMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    val displayedProgress = if (isDragging) dragProgress else playbackFraction

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "trackHeight"
    )

    val thumbRadiusDp by animateDpAsState(
        targetValue = if (isDragging) 9.5.dp else 6.5.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "thumbRadius"
    )

    val haloExpansion by animateFloatAsState(
        targetValue = if (isDragging) 1.0f else 0.0f,
        animationSpec = tween(180),
        label = "haloExpansion"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .onSizeChanged { size ->
                    componentWidthPx = size.width.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(safeDuration) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isDragging = true
                        val width = componentWidthPx.coerceAtLeast(1f)
                        val initialFraction = (down.position.x / width).coerceIn(0f, 1f)
                        dragProgress = initialFraction

                        var lastThrottledSeekTime = 0L

                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!pointer.pressed) {
                                pointer.consume()
                                val targetMs = (dragProgress * safeDuration).toLong()
                                onSeekTo(targetMs)
                                isDragging = false
                                break
                            } else {
                                pointer.consume()
                                val newFraction = (pointer.position.x / width).coerceIn(0f, 1f)
                                dragProgress = newFraction

                                val now = System.currentTimeMillis()
                                if (now - lastThrottledSeekTime >= 100L) {
                                    lastThrottledSeekTime = now
                                    onSeekTo((newFraction * safeDuration).toLong())
                                }
                            }
                        }
                        isDragging = false
                    }
                }
                .testTag("seek_slider"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                val w = size.width
                val h = size.height
                val centerY = h / 2f
                val activeWidth = (w * displayedProgress).coerceIn(0f, w)

                if (waveformEnvelope != null && waveformEnvelope.isNotEmpty()) {
                    // Feature 1: Real Waveform Envelope Rendering
                    val bars = waveformEnvelope
                    val barCount = bars.size
                    val totalGapRatio = 0.35f
                    val barWidth = (w / barCount) * (1f - totalGapRatio)
                    val barGap = (w / barCount) * totalGapRatio
                    val maxBarHeight = h * 0.85f

                    for (i in 0 until barCount) {
                        val x = i * (barWidth + barGap)
                        val barH = (bars[i] * maxBarHeight).coerceIn(4f, maxBarHeight)
                        val y = centerY - (barH / 2f)
                        val isPlayed = (x + barWidth) <= activeWidth

                        val color = if (isPlayed) {
                            palette.accent
                        } else {
                            Color(0x35FFFFFF)
                        }

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }

                    // Thumb position on waveform
                    val thumbCenter = Offset(activeWidth, centerY)
                    val thumbRadiusPx = thumbRadiusDp.toPx()

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.accent.copy(alpha = 0.65f),
                                Color.Transparent
                            ),
                            center = thumbCenter,
                            radius = thumbRadiusPx * 2.5f
                        ),
                        radius = thumbRadiusPx * 2.5f,
                        center = thumbCenter
                    )

                    drawCircle(
                        color = palette.accent,
                        radius = thumbRadiusPx,
                        center = thumbCenter
                    )

                    drawCircle(
                        color = Color.White,
                        radius = thumbRadiusPx * 0.6f,
                        center = thumbCenter
                    )
                } else {
                    // Standard Liquid Glass Bar Bed
                    val trackH = trackHeight.toPx()
                    val corner = CornerRadius(trackH / 2f, trackH / 2f)
                    val trackTop = centerY - (trackH / 2f)

                    val audioData = analysisDataProvider()
                    val bassPulse = if (!isDragging) audioData.haloExpansion * 0.15f else 0.28f

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x35FFFFFF),
                                Color(0x15FFFFFF)
                            )
                        ),
                        topLeft = Offset(0f, trackTop),
                        size = Size(w, trackH),
                        cornerRadius = corner
                    )

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0x45FFFFFF),
                                Color(0x08FFFFFF)
                            )
                        ),
                        topLeft = Offset(0f, trackTop),
                        size = Size(w, trackH),
                        cornerRadius = corner,
                        style = Stroke(width = 0.8f)
                    )

                    if (activeWidth > 0f) {
                        val glowHeight = trackH * (2.8f + bassPulse + haloExpansion * 0.6f)
                        val glowTop = centerY - (glowHeight / 2f)
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    palette.primary.copy(alpha = 0.35f + haloExpansion * 0.25f),
                                    palette.accent.copy(alpha = 0.55f + haloExpansion * 0.30f)
                                )
                            ),
                            topLeft = Offset(0f, glowTop),
                            size = Size(activeWidth, glowHeight),
                            cornerRadius = CornerRadius(glowHeight / 2f, glowHeight / 2f)
                        )

                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    palette.primary,
                                    palette.accent,
                                    Color.White.copy(alpha = 0.95f)
                                ),
                                startX = 0f,
                                endX = activeWidth
                            ),
                            topLeft = Offset(0f, trackTop),
                            size = Size(activeWidth, trackH),
                            cornerRadius = corner
                        )
                    }

                    val thumbCenter = Offset(activeWidth.coerceIn(0f, w), centerY)
                    val thumbRadiusPx = thumbRadiusDp.toPx()

                    val bloomRadius = thumbRadiusPx * (2.8f + haloExpansion * 0.8f + bassPulse * 0.5f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.accent.copy(alpha = 0.60f + haloExpansion * 0.25f),
                                palette.primary.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = thumbCenter,
                            radius = bloomRadius
                        ),
                        radius = bloomRadius,
                        center = thumbCenter
                    )

                    drawCircle(
                        color = Color.Black.copy(alpha = 0.50f),
                        radius = thumbRadiusPx + 1.5f,
                        center = Offset(thumbCenter.x, thumbCenter.y + 1.5f)
                    )

                    drawCircle(
                        color = palette.accent,
                        radius = thumbRadiusPx,
                        center = thumbCenter
                    )

                    drawCircle(
                        color = Color.White,
                        radius = thumbRadiusPx * 0.65f,
                        center = thumbCenter
                    )
                }
            }
        }

        val displayedMs = if (isDragging) (dragProgress * safeDuration).toLong() else currentMs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDuration(displayedMs),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDragging) palette.accent else Color(0xFFA5ABC0),
                    fontSize = 12.sp,
                    fontWeight = if (isDragging) FontWeight.Bold else FontWeight.SemiBold
                )
            )

            Text(
                text = "-${formatDuration((safeDuration - displayedMs).coerceAtLeast(0L))}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFA5ABC0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun LiquidGlassProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    palette: AmbientPalette,
    waveformEnvelope: FloatArray? = null,
    modifier: Modifier = Modifier
) {
    LiquidGlassProgressBar(
        currentPositionProvider = { (progress * 1000f).toLong() },
        durationMs = 1000L,
        palette = palette,
        analysisDataProvider = { AudioAnalysisData() },
        onSeekTo = { ms -> onSeek((ms / 1000f).coerceIn(0f, 1f)) },
        waveformEnvelope = waveformEnvelope,
        modifier = modifier
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
