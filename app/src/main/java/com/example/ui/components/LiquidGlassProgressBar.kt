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

/**
 * نوار پیشرفت هماهنگ با رنگ کاور آهنگ:
 * - حذف کامل رنگ‌های چندگانه دی‌جی (قرمز/آبی/سبز)
 * - استفاده مستقیم از رنگ کاور آهنگ (palette.primary و palette.accent)
 * - دارای شیار محافظ تیره (Contrast Casing) و هسته سفید-نوری تا هرگز با رنگ‌های پس‌زمینه یکی و محو نشود
 */
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

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 9.5.dp else 6.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "trackHeight"
    )

    val thumbRadiusDp by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 7.dp,
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
                    .height(42.dp)
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
                        .height(30.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f
                    val activeWidth = (w * displayedProgress).coerceIn(0f, w)
                    val trackH = trackHeight.toPx()
                    val corner = CornerRadius(trackH / 2f, trackH / 2f)
                    val trackTop = centerY - (trackH / 2f)

                    // ۱. قاب محافظ کنتراست تیره (Dark Contrast Casing) در زیر نوار
                    // تضمین می‌کند حتی اگر پس‌زمینه دقیقاً هم‌رنگ کاور یا سفید باشد، نوار کاملاً تفکیک و مشخص بماند
                    val casingPadding = 2.dp.toPx()
                    drawRoundRect(
                        color = Color(0xB805070E),
                        topLeft = Offset(0f, trackTop - casingPadding),
                        size = Size(w, trackH + casingPadding * 2f),
                        cornerRadius = CornerRadius((trackH + casingPadding * 2f) / 2f)
                    )

                    // ۲. شیار پس‌زمینه شیشه‌ای نوار
                    drawRoundRect(
                        color = Color(0x38FFFFFF),
                        topLeft = Offset(0f, trackTop),
                        size = Size(w, trackH),
                        cornerRadius = corner
                    )

                    // خط مرزی ظریف دور کل شیار نوار برای جلوگیری از محو شدن
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.28f),
                        topLeft = Offset(0f, trackTop),
                        size = Size(w, trackH),
                        cornerRadius = corner,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // ۳. بخش پرشده نوار بر اساس رنگ کاور آهنگ (همراه با لبه روشن کریستالی)
                    if (activeWidth > 0f) {
                        val glowHeight = trackH * (2.2f + haloExpansion * 0.5f)
                        val glowTop = centerY - (glowHeight / 2f)

                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    coverPrimary.copy(alpha = 0.45f),
                                    coverAccent.copy(alpha = 0.65f)
                                ),
                                startX = 0f,
                                endX = activeWidth.coerceAtLeast(1f)
                            ),
                            topLeft = Offset(0f, glowTop),
                            size = Size(activeWidth, glowHeight),
                            cornerRadius = CornerRadius(glowHeight / 2f, glowHeight / 2f)
                        )

                        // بدنه اصلی نوار پرشده با رنگ کاور + هسته درخشان در نوک نوار
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to coverPrimary,
                                    0.75f to coverAccent,
                                    1.00f to Color.White
                                ),
                                startX = 0f,
                                endX = activeWidth.coerceAtLeast(1f)
                            ),
                            topLeft = Offset(0f, trackTop),
                            size = Size(activeWidth, trackH),
                            cornerRadius = corner
                        )

                        // هایلایت سفید بسیار ظریف روی لبه بالایی قسمت پرشده برای تفکیک از پس‌زمینه
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.45f),
                            topLeft = Offset(0f, trackTop),
                            size = Size(activeWidth, trackH),
                            cornerRadius = corner,
                            style = Stroke(width = 0.9.dp.toPx())
                        )
                    }

                    // نمایش بازه Loop A-B در صورت فعال بودن
                    if (loopStartMs != null && loopStartMs >= 0L) {
                        val startX = ((loopStartMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)) * w
                        drawCircle(
                            color = Color.White,
                            radius = trackH * 0.8f,
                            center = Offset(startX, centerY)
                        )
                        if (loopEndMs != null && loopEndMs > loopStartMs) {
                            val endX = ((loopEndMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)) * w
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.30f),
                                topLeft = Offset(startX, trackTop - 2.dp.toPx()),
                                size = Size((endX - startX).coerceAtLeast(2f), trackH + 4.dp.toPx()),
                                cornerRadius = corner
                            )
                            drawCircle(
                                color = Color.White,
                                radius = trackH * 0.8f,
                                center = Offset(endX, centerY)
                            )
                        }
                    }

                    // ۴. گوی نوری سر نوار (Thumb) با حلقه محافظ تیره + مرکز سفید و رنگی
                    val thumbCenter = Offset(activeWidth.coerceIn(0f, w), centerY)
                    val thumbRadiusPx = thumbRadiusDp.toPx()

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                coverAccent.copy(alpha = 0.70f),
                                coverPrimary.copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = thumbCenter,
                            radius = thumbRadiusPx * 2.4f
                        ),
                        radius = thumbRadiusPx * 2.4f,
                        center = thumbCenter
                    )

                    // حلقه سایه تیره دور گوی تا روی پس‌زمینه روشن هم ۱۰۰٪ مشخص باشد
                    drawCircle(
                        color = Color(0xD905070E),
                        radius = thumbRadiusPx + 2.dp.toPx(),
                        center = thumbCenter
                    )

                    // حلقه سفید بیرونی گوی
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadiusPx,
                        center = thumbCenter
                    )

                    // هسته رنگی کاور در مرکز گوی
                    drawCircle(
                        color = coverAccent,
                        radius = thumbRadiusPx * 0.55f,
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
                        color = if (isDragging) Color.White else Color(0xFFD0D6EA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "-${formatDuration((safeDuration - effectiveMs).coerceAtLeast(0L))}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFD0D6EA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
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
