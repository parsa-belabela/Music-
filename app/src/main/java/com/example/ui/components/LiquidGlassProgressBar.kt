package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() },
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

    val coverPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(500),
        label = "barCoverPrimary"
    )
    val coverAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(500),
        label = "barCoverAccent"
    )

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
                delay(16L)
            }
        }
    }

    val safeDuration = currentDurationMs.coerceAtLeast(1L)
    val effectiveMs = if (isDragging) (dragProgress * safeDuration).toLong() else internalCurrentMs
    val displayedProgress = (effectiveMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    // طراحی بسیار ظریف و نازک به سبک Apple Music مدرن
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 3.5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "trackHeight"
    )

    val thumbRadiusDp by animateDpAsState(
        targetValue = if (isDragging) 7.5.dp else 4.5.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "thumbRadius"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.55f else 0.28f,
        animationSpec = tween(180),
        label = "glowAlpha"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
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
                        .height(24.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f
                    val activeWidth = (w * displayedProgress).coerceIn(0f, w)
                    val trackH = trackHeight.toPx()
                    val corner = CornerRadius(trackH / 2f, trackH / 2f)
                    val trackTop = centerY - (trackH / 2f)

                    // ۱. سایه بسیار ظریف زیر خط برای جلوگیری از محو شدن روی پس‌زمینه‌های روشن
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.40f),
                        topLeft = Offset(0f, trackTop - 1.dp.toPx()),
                        size = Size(w, trackH + 2.dp.toPx()),
                        cornerRadius = CornerRadius((trackH + 2.dp.toPx()) / 2f)
                    )

                    // ۲. مسیر ظریف پس‌زمینه
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.20f),
                        topLeft = Offset(0f, trackTop),
                        size = Size(w, trackH),
                        cornerRadius = corner
                    )

                    // ۳. خط نوری باریک و خوش‌رنگ بر مبنای رنگ کاور آهنگ
                    if (activeWidth > 0f) {
                        val glowH = trackH * 2.2f
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    coverPrimary.copy(alpha = glowAlpha),
                                    coverAccent.copy(alpha = glowAlpha * 1.2f)
                                ),
                                startX = 0f,
                                endX = activeWidth.coerceAtLeast(1f)
                            ),
                            topLeft = Offset(0f, centerY - glowH / 2f),
                            size = Size(activeWidth, glowH),
                            cornerRadius = CornerRadius(glowH / 2f, glowH / 2f)
                        )

                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to coverPrimary,
                                    0.80f to coverAccent,
                                    1.00f to Color.White
                                ),
                                startX = 0f,
                                endX = activeWidth.coerceAtLeast(1f)
                            ),
                            topLeft = Offset(0f, trackTop),
                            size = Size(activeWidth, trackH),
                            cornerRadius = corner
                        )
                    }

                    // ۴. گوی ظریف و مینیمال سر خط
                    val thumbCenter = Offset(activeWidth.coerceIn(0f, w), centerY)
                    val thumbRadiusPx = thumbRadiusDp.toPx()

                    drawCircle(
                        color = coverAccent.copy(alpha = 0.45f),
                        radius = thumbRadiusPx * 1.9f,
                        center = thumbCenter
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.45f),
                        radius = thumbRadiusPx + 1.dp.toPx(),
                        center = thumbCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadiusPx,
                        center = thumbCenter
                    )
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
                        color = if (isDragging) Color.White else Color.White.copy(alpha = 0.72f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                Text(
                    text = "-${formatDuration((safeDuration - effectiveMs).coerceAtLeast(0L))}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
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
    palette: AmbientPalette = AmbientPalette(),
    waveformEnvelope: FloatArray? = null,
    modifier: Modifier = Modifier,
    primaryColor: Color = palette.primary,
    secondaryColor: Color = palette.accent,
    durationMs: Long = 180000L,
    currentPositionMs: Long = (progress * durationMs).toLong()
) {
    val safeDur = durationMs.coerceAtLeast(1L)
    val mergedPalette = palette.copy(primary = primaryColor, accent = secondaryColor)
    LiquidGlassProgressBar(
        currentPositionProvider = { currentPositionMs },
        durationMs = safeDur,
        palette = mergedPalette,
        analysisDataProvider = { AudioAnalysisData() },
        onSeekTo = { ms -> onSeek((ms.toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)) },
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
