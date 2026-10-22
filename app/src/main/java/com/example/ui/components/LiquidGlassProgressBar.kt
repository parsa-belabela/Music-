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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import kotlinx.coroutines.delay

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
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var ignoreExternalSyncUntilMs by remember { mutableLongStateOf(0L) }
    var componentWidthPx by remember { mutableFloatStateOf(1f) }

    val currentDurationMs by rememberUpdatedState(durationMs)
    val currentOnSeekTo by rememberUpdatedState(onSeekTo)
    val currentPositionProviderUpdated by rememberUpdatedState(currentPositionProvider)

    var internalCurrentMs by remember { mutableLongStateOf(currentPositionProvider()) }

    LaunchedEffect(durationMs) {
        internalCurrentMs = currentPositionProviderUpdated()
        dragProgress = 0f
        ignoreExternalSyncUntilMs = 0L
    }

    LaunchedEffect(isDragging) {
        if (!isDragging) {
            while (true) {
                if (System.currentTimeMillis() > ignoreExternalSyncUntilMs) {
                    internalCurrentMs = currentPositionProviderUpdated()
                }
                delay(16L) // 60-120fps smooth position polling
            }
        }
    }

    val safeDuration = currentDurationMs.coerceAtLeast(1L)
    val effectiveMs = if (isDragging) (dragProgress * safeDuration).toLong() else internalCurrentMs
    val displayedProgress = (effectiveMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 9.dp else 5.5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "trackHeight"
    )

    val thumbRadiusDp by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 6.5.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "thumbRadius"
    )

    val haloExpansion by animateFloatAsState(
        targetValue = if (isDragging) 1.0f else 0.0f,
        animationSpec = tween(180),
        label = "haloExpansion"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                            val initialFraction = (down.position.x / width).coerceIn(0f, 0.992f)
                            dragProgress = initialFraction
                            val curSafeDur = currentDurationMs.coerceAtLeast(1L)
                            internalCurrentMs = (initialFraction * curSafeDur).toLong()

                            var activePointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val pointer = event.changes.firstOrNull { it.id == activePointerId }
                                    ?: event.changes.firstOrNull()
                                if (pointer == null || !pointer.pressed) {
                                    pointer?.consume()
                                    break
                                }
                                activePointerId = pointer.id
                                if (pointer.positionChange() != Offset.Zero) {
                                    pointer.consume()
                                }
                                val newFraction = (pointer.position.x / width).coerceIn(0f, 0.992f)
                                dragProgress = newFraction
                                internalCurrentMs = (newFraction * curSafeDur).toLong()
                            }

                            val maxSafeSeek = if (curSafeDur > 1500L) curSafeDur - 900L else curSafeDur
                            val targetMs = (dragProgress * curSafeDur).toLong().coerceIn(0L, maxSafeSeek)
                            internalCurrentMs = targetMs
                            ignoreExternalSyncUntilMs = System.currentTimeMillis() + 350L
                            isDragging = false
                            currentOnSeekTo(targetMs)
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

                            drawRoundRect(
                                color = if (isPlayed) palette.accent else Color(0x35FFFFFF),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }

                        val thumbCenter = Offset(activeWidth, centerY)
                        val thumbRadiusPx = thumbRadiusDp.toPx()

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(palette.accent.copy(alpha = 0.65f), Color.Transparent),
                                center = thumbCenter,
                                radius = thumbRadiusPx * 2.5f
                            ),
                            radius = thumbRadiusPx * 2.5f,
                            center = thumbCenter
                        )
                        drawCircle(color = palette.accent, radius = thumbRadiusPx, center = thumbCenter)
                        drawCircle(color = Color.White, radius = thumbRadiusPx * 0.6f, center = thumbCenter)
                    } else {
                        val trackH = trackHeight.toPx()
                        val corner = CornerRadius(trackH / 2f, trackH / 2f)
                        val trackTop = centerY - (trackH / 2f)
                        val audioData = analysisDataProvider()
                        val bassPulse = if (!isDragging) audioData.haloExpansion * 0.15f else 0.28f

                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(Color(0x35FFFFFF), Color(0x15FFFFFF))),
                            topLeft = Offset(0f, trackTop),
                            size = Size(w, trackH),
                            cornerRadius = corner
                        )

                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(Color(0x45FFFFFF), Color(0x08FFFFFF))),
                            topLeft = Offset(0f, trackTop),
                            size = Size(w, trackH),
                            cornerRadius = corner,
                            style = Stroke(width = 0.8f)
                        )

                        if (activeWidth > 0f) {
                            val glowHeight = trackH * (2.6f + bassPulse + haloExpansion * 0.6f)
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
                                    listOf(palette.primary, palette.accent, Color.White.copy(alpha = 0.95f)),
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
                        val bloomRadius = thumbRadiusPx * (2.6f + haloExpansion * 0.8f + bassPulse * 0.4f)

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
                        drawCircle(color = palette.accent, radius = thumbRadiusPx, center = thumbCenter)
                        drawCircle(color = Color.White, radius = thumbRadiusPx * 0.65f, center = thumbCenter)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(effectiveMs),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isDragging) palette.accent else Color(0xFFA5ABC0),
                        fontSize = 12.sp,
                        fontWeight = if (isDragging) FontWeight.Bold else FontWeight.SemiBold
                    )
                )

                Text(
                    text = "-${formatDuration((safeDuration - effectiveMs).coerceAtLeast(0L))}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFA5ABC0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun LiquidGlassProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF9D4EDD),
    secondaryColor: Color = Color(0xFF00E5FF)
) {
    val dummyPalette = remember(primaryColor, secondaryColor) {
        AmbientPalette(primary = primaryColor, secondary = secondaryColor, accent = secondaryColor)
    }
    val safeProgress = progress.coerceIn(0f, 1f)
    val dummyDurationMs = 180000L
    val dummyPositionMs = (safeProgress * dummyDurationMs).toLong()

    LiquidGlassProgressBar(
        currentPositionProvider = { dummyPositionMs },
        durationMs = dummyDurationMs,
        palette = dummyPalette,
        analysisDataProvider = { AudioAnalysisData() },
        onSeekTo = { pos -> onSeek(pos.toFloat() / dummyDurationMs.toFloat()) },
        modifier = modifier
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
