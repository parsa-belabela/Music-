package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.example.audio.AmbientPalette
import com.example.data.model.AppTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ModernAuroraMotionBackground(
    palette: AmbientPalette,
    appTheme: AppTheme,
    modifier: Modifier = Modifier,
    energyBoostProvider: () -> Float = { 0f },
    intensity: Float = 1.0f
) {
    val animPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bgPrimary"
    )
    val animSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bgSecondary"
    )
    val animAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bgAccent"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "AuroraFluidMotion")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 19000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val baseBackgroundColor = Color(0xFF03040A)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val boost = energyBoostProvider().coerceIn(0f, 1f)
            val dynamicScale = pulseScale * (1f + boost * 0.35f) * intensity

            fun drawMotionBlurredAuroraOrb(
                cx: Float,
                cy: Float,
                velY: Float,
                radius: Float,
                color: Color,
                coreAlpha: Float
            ) {
                val stretchY = (1f + kotlin.math.abs(velY) * 0.45f).coerceIn(1f, 1.65f)
                for (trail in 2 downTo 0) {
                    val trailY = cy - (velY * h * 0.055f * trail)
                    val trailRadius = radius * (1f - trail * 0.14f)
                    val alpha = (coreAlpha * (1f - trail * 0.28f)).coerceIn(0.05f, 0.90f)

                    withTransform({
                        scale(scaleX = 1f, scaleY = stretchY, pivot = Offset(cx, trailY))
                    }) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to color.copy(alpha = alpha),
                                    0.42f to color.copy(alpha = alpha * 0.60f),
                                    0.78f to color.copy(alpha = alpha * 0.22f),
                                    1.00f to Color.Transparent
                                ),
                                center = Offset(cx, trailY),
                                radius = trailRadius
                            ),
                            radius = trailRadius,
                            center = Offset(cx, trailY),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
            }

            // ۱. گوی نوری اصلی کاور آهنگ
            val orb1X = w * (0.28f + 0.22f * cos(phase1))
            val orb1Y = h * (0.24f + 0.20f * sin(phase1))
            drawMotionBlurredAuroraOrb(
                cx = orb1X,
                cy = orb1Y,
                velY = cos(phase1),
                radius = w * 0.78f * dynamicScale,
                color = animPrimary,
                coreAlpha = 0.68f
            )

            // ۲. گوی نوری مکمل کاور آهنگ
            val orb2X = w * (0.74f - 0.20f * sin(phase2))
            val orb2Y = h * (0.64f - 0.22f * cos(phase2))
            drawMotionBlurredAuroraOrb(
                cx = orb2X,
                cy = orb2Y,
                velY = sin(phase2),
                radius = w * 0.74f * dynamicScale,
                color = animSecondary,
                coreAlpha = 0.60f
            )

            // ۳. گوی نوری تاکیدی (Accent) در مرکز صفحه
            val orb3X = w * (0.48f + 0.20f * sin(phase1 + 1.2f))
            val orb3Y = h * (0.48f + 0.24f * cos(phase1 + 1.2f))
            drawMotionBlurredAuroraOrb(
                cx = orb3X,
                cy = orb3Y,
                velY = -sin(phase1 + 1.2f),
                radius = w * 0.66f * dynamicScale,
                color = animAccent,
                coreAlpha = 0.52f
            )

            // ۴. ذرات معلق نوری در فضای پس‌زمینه کل صفحات اپ
            val particleCount = 34
            for (i in 0 until particleCount) {
                val seed = i * 27.4f
                val speed = 0.35f + (i % 4) * 0.2f
                val px = w * ((sin(seed + phase1 * speed) + 1f) * 0.5f)
                val py = h * (1f - ((i * 0.029f + (phase2 / (2f * PI.toFloat())) * speed) % 1f))
                val twinkle = 0.35f + 0.65f * ((cos(phase1 * 2.5f + seed) + 1f) * 0.5f)
                val dotAlpha = (0.16f + boost * 0.28f) * twinkle
                val dotRadius = (1.3f + (i % 3) * 0.9f).dp.toPx()
                val pColor = when (i % 3) {
                    0 -> Color.White
                    1 -> animAccent
                    else -> animSecondary
                }
                drawCircle(
                    color = animPrimary.copy(alpha = dotAlpha * 0.32f),
                    radius = dotRadius * 2.4f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = pColor.copy(alpha = dotAlpha),
                    radius = dotRadius,
                    center = Offset(px, py)
                )
            }

            // ۵. تکسچر استودیویی میکرو-گرین
            val step = 28.dp.toPx().coerceAtLeast(18f)
            val cols = (w / step).toInt()
            val rows = (h / step).toInt()
            for (r in 0..rows) {
                val y = r * step
                for (c in 0..cols) {
                    if ((r + c) % 2 == 0) {
                        val x = c * step
                        drawCircle(
                            color = Color.White.copy(alpha = 0.024f),
                            radius = 1.1f,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // ۶. لایه محافظ کنتراست مخملی عمیق
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseBackgroundColor.copy(alpha = 0.34f),
                        Color(0x2203040A),
                        baseBackgroundColor.copy(alpha = 0.68f)
                    )
                )
            )
        }
    }
}
