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
    loopStartMs: Long? = null,
    loopEndMs: Long? = null,
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

    // Ensure a rich Serato/Rekordbox waveform is always available even before background analysis completes
    val activeWaveform = remember(waveformEnvelope, safeDuration) {
        if (waveformEnvelope != null && waveformEnvelope.isNotEmpty()) {
            waveformEnvelope
        } else {
            val count = 110
            val seed = (safeDuration xor (safeDuration ushr 16)).toInt()
            FloatArray(count) { idx ->
                val norm = idx.toFloat() / count.toFloat()
                // Intro / outro taper + energetic beat drop sections
                val envelope = when {
                    norm < 0.08f -> 0.22f + (norm / 0.08f) * 0.35f
                    norm in 0.22f..0.48f -> 0.72f + kotlin.math.sin(idx * 0.85f + seed) * 0.24f
                    norm in 0.48f..0.62f -> 0.30f + kotlin.math.cos(idx * 0.5f) * 0.12f // vocal breakdown
                    norm in 0.62f..0.88f -> 0.78f + kotlin.math.sin(idx * 1.1f + seed) * 0.20f // 2nd drop
                    norm > 0.90f -> (1f - norm) * 4.5f
                    else -> 0.48f + kotlin.math.sin(idx * 0.65f + seed) * 0.18f
                }
                envelope.coerceIn(0.14f, 0.98f)
            }
        }
    }

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
                        .height(34.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f
                    val activeWidth = (w * displayedProgress).coerceIn(0f, w)
                    val audioData = analysisDataProvider()
                    val liveKickBoost = if (!isDragging) audioData.kickPulse * 0.12f else 0.08f

                    // Draw A-B Loop Highlight Region behind bars if active
                    val loopAFraction = loopStartMs?.let { (it.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) }
                    val loopBFraction = loopEndMs?.let { (it.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f) }
                    if (loopAFraction != null && loopBFraction != null && loopBFraction > loopAFraction) {
                        val startX = loopAFraction * w
                        val endX = loopBFraction * w
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0x4000F5D4),
                                    Color(0x28FF1E56),
                                    Color(0x4000F5D4)
                                ),
                                startX = startX,
                                endX = endX
                            ),
                            topLeft = Offset(startX, 1.dp.toPx()),
                            size = Size((endX - startX).coerceAtLeast(2f), h - 2.dp.toPx()),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    val bars = activeWaveform
                    val barCount = bars.size
                    val totalGapRatio = 0.34f
                    val barWidth = (w / barCount) * (1f - totalGapRatio)
                    val barGap = (w / barCount) * totalGapRatio
                    val maxBarHeight = h * 0.88f

                    // Serato / Rekordbox Spectral Color Palette
                    val dropCrimson = Color(0xFFFF1E56)   // Heavy Bass / Beat Drop
                    val dropMagenta = Color(0xFFFF007F)   // Peak Kick & Sub
                    val midViolet = Color(0xFF9D4EDD)     // Mid-range Groove / Synth
                    val vocalIceCyan = Color(0xFF00F5D4)  // Vocal / Quiet / High-hat Air
                    val deepIceBlue = Color(0xFF00BBF9)   // Low-intensity Ambient

                    for (i in 0 until barCount) {
                        val x = i * (barWidth + barGap)
                        val amp = bars[i].coerceIn(0.08f, 1f)
                        val isNearPlayhead = kotlin.math.abs((x + barWidth * 0.5f) - activeWidth) < (barWidth * 3.5f)
                        val dynamicScale = if (isNearPlayhead) (1f + liveKickBoost) else 1f
                        val barH = (amp * maxBarHeight * dynamicScale).coerceIn(4f, maxBarHeight)
                        val y = centerY - (barH / 2f)
                        val isPlayed = (x + barWidth * 0.5f) <= activeWidth

                        // Determine spectral color from amplitude & local energy (Serato / Rekordbox style)
                        val baseBarColor = when {
                            amp >= 0.76f -> dropCrimson
                            amp >= 0.64f -> dropMagenta
                            amp >= 0.45f -> midViolet
                            amp >= 0.28f -> vocalIceCyan
                            else -> deepIceBlue
                        }

                        // Unplayed bars still show their frequency color at 44% opacity so the user can visually spot Beat Drops ahead of time!
                        val finalAlpha = if (isPlayed) 0.96f else 0.44f

                        drawRoundRect(
                            color = baseBarColor.copy(alpha = finalAlpha),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }

                    // Draw A and B markers if set
                    if (loopAFraction != null) {
                        val ax = (loopAFraction * w).coerceIn(0f, w)
                        drawLine(
                            color = Color(0xFF00F5D4),
                            start = Offset(ax, 0f),
                            end = Offset(ax, h),
                            strokeWidth = 2.2.dp.toPx()
                        )
                        drawCircle(
                            color = Color(0xFF00F5D4),
                            radius = 4.dp.toPx(),
                            center = Offset(ax, 4.dp.toPx())
                        )
                    }
                    if (loopBFraction != null) {
                        val bx = (loopBFraction * w).coerceIn(0f, w)
                        drawLine(
                            color = Color(0xFFFF1E56),
                            start = Offset(bx, 0f),
                            end = Offset(bx, h),
                            strokeWidth = 2.2.dp.toPx()
                        )
                        drawCircle(
                            color = Color(0xFFFF1E56),
                            radius = 4.dp.toPx(),
                            center = Offset(bx, h - 4.dp.toPx())
                        )
                    }

                    val thumbCenter = Offset(activeWidth, centerY)
                    val thumbRadiusPx = thumbRadiusDp.toPx()
                    val bloomRadius = thumbRadiusPx * (2.5f + haloExpansion * 0.7f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.accent.copy(alpha = 0.68f), Color.Transparent),
                            center = thumbCenter,
                            radius = bloomRadius
                        ),
                        radius = bloomRadius,
                        center = thumbCenter
                    )
                    drawCircle(color = palette.accent, radius = thumbRadiusPx, center = thumbCenter)
                    drawCircle(color = Color.White, radius = thumbRadiusPx * 0.6f, center = thumbCenter)
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
