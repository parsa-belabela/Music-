package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData

@Composable
fun LiquidGlassProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF9D4EDD),
    secondaryColor: Color = Color(0xFF00E5FF)
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }

    val activeProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(600),
        label = "SeekPrimaryColor"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(600),
        label = "SeekSecondaryColor"
    )

    // بزرگ شدن ضخامت نوار هنگام لمس (مشابه Apple Music iOS 18)
    val barHeight by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "BarHeight"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.85f else 0.45f,
        animationSpec = tween(250),
        label = "BarGlow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newPos = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(newPos)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        onSeek(dragProgress)
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val trackH = barHeight.toPx()
            val topY = (h - trackH) / 2f
            val corner = CornerRadius(trackH / 2f, trackH / 2f)
            val filledWidth = (w * activeProgress).coerceIn(trackH, w)

            // ۱. پس‌زمینه شیشه‌ای مسیر نوار پیشرفت
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                topLeft = Offset(0f, topY),
                size = Size(w, trackH),
                cornerRadius = corner
            )

            // ۲. هاله نوری زیر نوار پرشده (هماهنگ با رنگ کاور آهنگ)
            if (activeProgress > 0.005f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            animatedPrimary.copy(alpha = glowAlpha * 0.55f),
                            animatedSecondary.copy(alpha = glowAlpha * 0.70f)
                        ),
                        startX = 0f,
                        endX = filledWidth
                    ),
                    topLeft = Offset(0f, topY - 3.dp.toPx()),
                    size = Size(filledWidth, trackH + 6.dp.toPx()),
                    cornerRadius = CornerRadius((trackH + 6.dp.toPx()) / 2f)
                )

                // ۳. نوار اصلی پرشده با گرادیانت زنده رنگ آهنگ و سرِ درخشان سفید
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to animatedPrimary,
                            0.75f to animatedSecondary,
                            1.0f to Color.White
                        ),
                        startX = 0f,
                        endX = filledWidth
                    ),
                    topLeft = Offset(0f, topY),
                    size = Size(filledWidth, trackH),
                    cornerRadius = corner
                )

                // ۴. گوی نوری سر نوار (Thumb) هنگام لمس یا پخش
                val thumbRadius = if (isDragging) 9.dp.toPx() else 5.5.dp.toPx()
                val thumbCenter = Offset(filledWidth.coerceIn(thumbRadius, w - thumbRadius), h / 2f)

                drawCircle(
                    color = animatedSecondary.copy(alpha = 0.55f),
                    radius = thumbRadius * 1.6f,
                    center = thumbCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = thumbRadius,
                    center = thumbCenter
                )
            }
        }
    }
}

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
    val safeDur = durationMs.coerceAtLeast(1L)
    val curMs = currentPositionProvider()
    val progress = (curMs.toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)

    LiquidGlassProgressBar(
        progress = progress,
        onSeek = { fraction -> onSeekTo((fraction * safeDur).toLong()) },
        modifier = modifier,
        primaryColor = palette.primary,
        secondaryColor = palette.accent
    )
}

@Composable
fun LiquidGlassProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    palette: AmbientPalette,
    waveformEnvelope: FloatArray? = null,
    modifier: Modifier = Modifier,
    durationMs: Long = 180000L,
    currentPositionMs: Long = (progress * durationMs).toLong()
) {
    val safeDur = durationMs.coerceAtLeast(1L)
    LiquidGlassProgressBar(
        progress = progress,
        onSeek = onSeek,
        modifier = modifier,
        primaryColor = palette.primary,
        secondaryColor = palette.accent
    )
}
