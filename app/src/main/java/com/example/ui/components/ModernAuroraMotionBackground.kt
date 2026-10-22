package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.audio.AmbientPalette
import com.example.data.model.AppTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ModernAuroraMotionBackground(
    dominantColor: Color = Color(0xFF9D4EDD),
    secondaryColor: Color = Color(0xFF00E5FF),
    tertiaryColor: Color = Color(0xFFFF2A85),
    isPlaying: Boolean = true,
    audioIntensity: Float = 0.65f,
    modifier: Modifier = Modifier
) {
    // تغییر نرم رنگهای نور پسزمینه هماهنگ با کاور آهنگ
    val color1 by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "AuroraC1"
    )
    val color2 by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "AuroraC2"
    )
    val color3 by animateColorAsState(
        targetValue = tertiaryColor,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "AuroraC3"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "MotionBlurAurora")

    // موجهای آرام و لوکس برای حرکت باوقار و آرام در پس‌زمینه (Slow, Calm & Elegant Ambient Waves)
    val verticalWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 18000 else 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VWave1"
    )

    val verticalWave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 24000 else 36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VWave2"
    )

    val verticalWave3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 20000 else 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VWave3"
    )

    // تنفس نرم و آرام نورها (Gentle Organic Breathing Pulse)
    val beatPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = if (isPlaying) 1.10f else 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isPlaying) 4600 else 6400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BeatPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05050B))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val energy = if (isPlaying) (0.85f + audioIntensity * 0.35f) * beatPulse else 0.75f

            // تابع کمکی برای رسم گوی نوری بلر شده به همراه دنباله موشنبلر (Motion Blur Trail) در جهت حرکت بالا/پایین
            fun drawMotionBlurredLightOrb(
                centerX: Float,
                centerY: Float,
                velocityY: Float,
                baseRadius: Float,
                orbColor: Color,
                coreAlpha: Float
            ) {
                // کشیدگی عمودی بر اساس سرعت بالا و پایین رفتن (ایجاد حس واقعی Motion Blur)
                val stretchY = (1f + kotlin.math.abs(velocityY) * 0.55f).coerceIn(1f, 1.85f)

                // رسم ۳ لایه دنباله نوری بلر شده (Motion Trail)
                for (trailIndex in 2 downTo 0) {
                    val trailFactor = trailIndex.toFloat() * 0.14f
                    val trailY = centerY - (velocityY * h * 0.08f * trailIndex)
                    val trailRadius = baseRadius * (1f - trailFactor * 0.4f)
                    val layerAlpha = (coreAlpha * (1f - trailIndex * 0.28f)).coerceIn(0.05f, 0.9f)

                    withTransform({
                        scale(
                            scaleX = 1f,
                            scaleY = stretchY,
                            pivot = Offset(centerX, trailY)
                        )
                    }) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to orbColor.copy(alpha = layerAlpha),
                                    0.22f to orbColor.copy(alpha = layerAlpha * 0.88f),
                                    0.45f to orbColor.copy(alpha = layerAlpha * 0.58f),
                                    0.68f to orbColor.copy(alpha = layerAlpha * 0.28f),
                                    0.88f to orbColor.copy(alpha = layerAlpha * 0.08f),
                                    1.00f to Color.Transparent
                                ),
                                center = Offset(centerX, trailY),
                                radius = trailRadius
                            ),
                            radius = trailRadius,
                            center = Offset(centerX, trailY),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
            }

            // ۱. موج نوری اول (رنگ اصلی کاور - حرکت عمیق بالا و پایین در سمت چپ)
            val y1 = h * (0.28f + 0.26f * sin(verticalWave1))
            val vel1 = cos(verticalWave1)
            drawMotionBlurredLightOrb(
                centerX = w * (0.25f + 0.12f * cos(verticalWave3)),
                centerY = y1,
                velocityY = vel1,
                baseRadius = w * 0.72f * energy,
                orbColor = color1,
                coreAlpha = 0.62f
            )

            // ۲. موج نوری دوم (رنگ دوم کاور/فیروزهای نئون - حرکت پرسرعت بالا و پایین در سمت راست)
            val y2 = h * (0.62f - 0.30f * sin(verticalWave2))
            val vel2 = -cos(verticalWave2)
            drawMotionBlurredLightOrb(
                centerX = w * (0.78f - 0.14f * sin(verticalWave1)),
                centerY = y2,
                velocityY = vel2,
                baseRadius = w * 0.68f * energy,
                orbColor = color2,
                coreAlpha = 0.58f
            )

            // ۳. موج نوری سوم (مخمل سرخابی/نئون - حرکت نوسانی در مرکز برای زنده کردن پشت کاور)
            val y3 = h * (0.45f + 0.28f * cos(verticalWave2 + 1.2f))
            val vel3 = -sin(verticalWave2 + 1.2f)
            drawMotionBlurredLightOrb(
                centerX = w * (0.50f + 0.18f * sin(verticalWave3)),
                centerY = y3,
                velocityY = vel3,
                baseRadius = w * 0.60f * energy,
                orbColor = color3,
                coreAlpha = 0.48f
            )

            // ۴. موج نوری چهارم (نور مکمل در بخش پایینی صفحه که زیر دکمههای پلیر بالا و پایین میرود)
            val y4 = h * (0.82f + 0.16f * sin(verticalWave1 + 2.4f))
            val vel4 = cos(verticalWave1 + 2.4f)
            drawMotionBlurredLightOrb(
                centerX = w * (0.35f + 0.25f * cos(verticalWave2)),
                centerY = y4,
                velocityY = vel4,
                baseRadius = w * 0.75f * energy,
                orbColor = color1.copy(
                    red = (color1.red * 0.6f + color2.red * 0.4f).coerceIn(0f, 1f),
                    green = (color1.green * 0.5f + color3.green * 0.5f).coerceIn(0f, 1f),
                    blue = (color1.blue * 0.4f + color2.blue * 0.6f).coerceIn(0f, 1f)
                ),
                coreAlpha = 0.52f
            )

            // ۵. گویهای نوری سریعتر (Fast Floating Bokeh Light Orbs) برای حس عمق و هیجان موزیک
            val y5 = h * (0.20f + 0.65f * ((sin(verticalWave2 * 1.5f) + 1f) * 0.5f))
            drawMotionBlurredLightOrb(
                centerX = w * (0.18f + 0.10f * sin(verticalWave1)),
                centerY = y5,
                velocityY = cos(verticalWave2 * 1.5f) * 1.4f,
                baseRadius = w * 0.38f * beatPulse,
                orbColor = color2,
                coreAlpha = 0.42f
            )

            val y6 = h * (0.85f - 0.65f * ((cos(verticalWave1 * 1.3f) + 1f) * 0.5f))
            drawMotionBlurredLightOrb(
                centerX = w * (0.84f - 0.10f * cos(verticalWave2)),
                centerY = y6,
                velocityY = sin(verticalWave1 * 1.3f) * 1.4f,
                baseRadius = w * 0.40f * beatPulse,
                orbColor = color3,
                coreAlpha = 0.40f
            )

            // ۶. لایه چندلایه اتمسفریک و بافت کریستالی مات (Frosted Micro-Grain & Multi-Stop Obsidian Veil)
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color(0xFF04050A).copy(alpha = 0.35f),
                        0.25f to Color.Transparent,
                        0.55f to color1.copy(alpha = 0.04f),
                        0.80f to Color(0xFF04050A).copy(alpha = 0.40f),
                        1.00f to Color(0xFF030307).copy(alpha = 0.75f)
                    )
                )
            )

            // لایه تکسچر بازتاب منشوری (Specular Edge Refraction)
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Transparent,
                        0.70f to Color.Transparent,
                        0.92f to color2.copy(alpha = 0.05f),
                        1.00f to Color(0xFF030307).copy(alpha = 0.55f)
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = (w * 0.85f).coerceAtLeast(h * 0.55f)
                )
            )
        }
    }
}

/**
 * Overload for ModernAuroraMotionBackground to seamlessly bind with MainScreen palette and theme
 */
@Composable
fun ModernAuroraMotionBackground(
    palette: AmbientPalette,
    appTheme: AppTheme,
    modifier: Modifier = Modifier,
    energyBoostProvider: () -> Float = { 0f },
    intensity: Float = 1.0f
) {
    ModernAuroraMotionBackground(
        dominantColor = palette.primary,
        secondaryColor = palette.secondary,
        tertiaryColor = palette.accent,
        isPlaying = true,
        audioIntensity = (energyBoostProvider() * intensity).coerceIn(0f, 1.5f),
        modifier = modifier
    )
}
