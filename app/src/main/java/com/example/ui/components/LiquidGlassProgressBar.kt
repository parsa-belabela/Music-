package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun LiquidGlassProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF9D4EDD),
    secondaryColor: Color = Color(0xFF00E5FF)
) {
    val safeExternalProgress = progress.coerceIn(0f, 1f)

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(safeExternalProgress) }
    var pendingSeekTarget by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(safeExternalProgress, pendingSeekTarget) {
        val target = pendingSeekTarget
        if (target != null) {
            if (abs(safeExternalProgress - target) < 0.02f) {
                pendingSeekTarget = null
            } else {
                delay(400)
                pendingSeekTarget = null
            }
        }
    }

    val displayedProgress = when {
        isScrubbing -> scrubPosition
        pendingSeekTarget != null -> pendingSeekTarget!!
        else -> safeExternalProgress
    }.coerceIn(0f, 1f)

    // انیمیشن نرم فقط برای رنگ‌ها
    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(500),
        label = "BarPrimary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(500),
        label = "BarSecondary"
    )

    val barHeight by animateDpAsState(
        targetValue = if (isScrubbing) 11.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BarHeightAnim"
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isScrubbing) 1.45f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ThumbScale"
    )

    // قفل کردن جهت نوار روی چپبهراست (LTR) تا در محیط فارسی هرگز مختصات لمس برعکس نشود
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val widthPx = size.width.toFloat().coerceAtLeast(1f)

                        isScrubbing = true
                        // محاسبه موقعیت دقیق نقطه لمس اولیه
                        scrubPosition = (down.position.x / widthPx).coerceIn(0f, 1f)

                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val dragChange = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()

                            if (dragChange == null || !dragChange.pressed) {
                                break
                            }

                            pointerId = dragChange.id
                            if (dragChange.positionChange() != Offset.Zero) {
                                dragChange.consume()
                            }

                            // حرکت ۱۰۰٪ دقیق همراه با انگشت بدون پرش ناگهانی
                            scrubPosition = (dragChange.position.x / widthPx).coerceIn(0f, 1f)
                        }

                        // ثبت نهایی زمان جدید و قفل موقت برای جلوگیری از پرش به عقب
                        val finalTarget = scrubPosition.coerceIn(0f, 1f)
                        pendingSeekTarget = finalTarget
                        isScrubbing = false
                        onSeek(finalTarget)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val trackH = barHeight.toPx()
                val topY = (h - trackH) / 2f
                val corner = CornerRadius(trackH / 2f, trackH / 2f)
                val activeW = (w * displayedProgress).coerceIn(0f, w)

                // ۱. شیار شیشه‌ای پس‌زمینه با عمق ملایم
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.12f),
                    topLeft = Offset(0f, topY),
                    size = Size(w, trackH),
                    cornerRadius = corner
                )

                // ۲. هاله نوری بلر شده (Real Blurred Glow) در زیر نوار پیشرفت
                if (displayedProgress > 0.002f) {
                    drawIntoCanvas { canvas ->
                        val glowPaint = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                maskFilter = BlurMaskFilter(
                                    if (isScrubbing) 28f else 16f,
                                    BlurMaskFilter.Blur.NORMAL
                                )
                                shader = LinearGradientShader(
                                    from = Offset(0f, h / 2f),
                                    to = Offset(activeW, h / 2f),
                                    colors = listOf(
                                        animatedPrimary.copy(alpha = 0.75f),
                                        animatedSecondary.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        }
                        canvas.drawRoundRect(
                            left = 0f,
                            top = topY - 4f,
                            right = activeW,
                            bottom = topY + trackH + 4f,
                            radiusX = trackH,
                            radiusY = trackH,
                            paint = glowPaint
                        )
                    }

                    // ۳. نوار اصلی با گرادیانت زنده و بافت نوری نرم
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.0f to animatedPrimary,
                                0.70f to animatedSecondary,
                                1.0f to Color.White
                            ),
                            startX = 0f,
                            endX = activeW
                        ),
                        topLeft = Offset(0f, topY),
                        size = Size(activeW, trackH),
                        cornerRadius = corner
                    )

                    // ۴. گوی نوری سر نوار (Glowing Liquid Thumb)
                    val baseRadius = 6.dp.toPx() * thumbScale
                    val thumbCenter = Offset(activeW.coerceIn(baseRadius, w - baseRadius), h / 2f)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                animatedSecondary.copy(alpha = 0.65f),
                                Color.Transparent
                            ),
                            center = thumbCenter,
                            radius = baseRadius * 2.4f
                        ),
                        radius = baseRadius * 2.4f,
                        center = thumbCenter
                    )

                    drawCircle(
                        color = Color.White,
                        radius = baseRadius,
                        center = thumbCenter
                    )
                }
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
