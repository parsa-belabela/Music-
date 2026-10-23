package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.Track
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CinematicAtmosphereBackground(
    track: Track?,
    palette: AmbientPalette,
    analysisDataProvider: () -> AudioAnalysisData,
    modifier: Modifier = Modifier,
    dimmingFactor: Float = 0.65f,
    glowStrength: Float = 1.0f
) {
    val animatedPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "coverBgPrimary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "coverBgSecondary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "coverBgAccent"
    )
    val animatedDeep by animateColorAsState(
        targetValue = palette.deepAtmosphere,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "coverBgDeep"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "DeepCinematicAtmosphere")
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "driftPhase"
    )
    val secondaryPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 21000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondaryPhase"
    )
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlePhase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF03040A))
            .background(animatedDeep.copy(alpha = 0.85f))
    ) {
        // لایه ۱: تصویر بلر شده‌ی عمیق کاور آهنگ در پس‌زمینه برای عمق نوری و هماهنگی کامل با کاور
        val context = LocalContext.current
        AnimatedContent(
            targetState = track?.artworkUri,
            transitionSpec = {
                fadeIn(animationSpec = tween(700, easing = FastOutSlowInEasing))
                    .togetherWith(fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)))
            },
            label = "CoverBlurLayer",
            modifier = Modifier.fillMaxSize()
        ) { uri ->
            if (!uri.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .size(256, 256)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(68.dp),
                    alpha = 0.52f
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    animatedPrimary.copy(alpha = 0.48f),
                                    animatedSecondary.copy(alpha = 0.34f),
                                    Color(0xFF03040A)
                                )
                            )
                        )
                )
            }
        }

        // لایه ۲: نورپردازی حجمی عمیق (Deep Volumetric Lighting) + ذرات معلق پس‌زمینه + تکسچر استودیویی
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val audio = analysisDataProvider()
            val realBass = audio.bass.coerceIn(0f, 1f)
            val realKick = audio.kickPulse.coerceIn(0f, 1f)
            val realEnergy = audio.totalEnergy.coerceIn(0f, 1f)
            val realHigh = (audio.mid * 0.5f + audio.high * 0.5f).coerceIn(0f, 1f)

            val minDim = min(w, h)
            val speedBoost = 1f + realEnergy * 0.45f
            val offsetX = cos(driftPhase) * (w * 0.14f * speedBoost)
            val offsetY = sin(driftPhase) * (h * 0.10f * speedBoost)

            // ۱. لایه پایه تاریکی عمیق (Deep Obsidian Depth Vignette) برای ایجاد کنتراست و عمق واقعی نور
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0x28020308),
                        0.65f to Color(0x78020308),
                        1.0f to Color(0xCC010205)
                    ),
                    center = Offset(w * 0.5f, h * 0.38f),
                    radius = minDim * 0.95f
                )
            )

            val primaryAlpha = ((0.58f + realBass * 0.28f + realKick * 0.16f) * glowStrength).coerceIn(0.35f, 0.96f)
            val secondaryAlpha = ((0.50f + realEnergy * 0.28f + realBass * 0.16f) * glowStrength).coerceIn(0.30f, 0.92f)
            val accentAlpha = ((0.44f + realHigh * 0.30f + realKick * 0.20f) * glowStrength).coerceIn(0.26f, 0.88f)
            val scaleBoost = 1f + (realBass * 0.26f) + (realKick * 0.18f)

            // ۲. گوی نوری عمیق اول (پشت کاور در نیمه بالایی)
            val c1 = Offset(w * 0.48f + offsetX, h * 0.34f + offsetY)
            val r1 = minDim * 0.88f * scaleBoost
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to animatedPrimary.copy(alpha = primaryAlpha),
                        0.42f to animatedPrimary.copy(alpha = primaryAlpha * 0.62f),
                        0.76f to animatedSecondary.copy(alpha = primaryAlpha * 0.26f),
                        1.00f to Color.Transparent
                    ),
                    center = c1,
                    radius = r1
                ),
                radius = r1,
                center = c1,
                blendMode = BlendMode.Screen
            )

            // ۳. گوی نوری عمیق دوم (چرخان در نیمه میانی و پایینی)
            val c2 = Offset(
                w * 0.54f - cos(secondaryPhase) * (w * 0.16f),
                h * 0.66f - sin(secondaryPhase) * (h * 0.10f)
            )
            val r2 = minDim * 0.82f * (1f + realEnergy * 0.24f)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to animatedSecondary.copy(alpha = secondaryAlpha),
                        0.48f to animatedAccent.copy(alpha = secondaryAlpha * 0.52f),
                        0.85f to animatedPrimary.copy(alpha = secondaryAlpha * 0.18f),
                        1.00f to Color.Transparent
                    ),
                    center = c2,
                    radius = r2
                ),
                radius = r2,
                center = c2,
                blendMode = BlendMode.Screen
            )

            // ۴. هاله‌های بلر شده Gaussian (نورهای عمیق چندلایه در پس‌زمینه)
            drawIntoCanvas { canvas ->
                val deepPrimaryPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        maskFilter = BlurMaskFilter(125f, BlurMaskFilter.Blur.NORMAL)
                        color = animatedPrimary.copy(
                            alpha = (0.48f + realBass * 0.32f + realKick * 0.20f).coerceIn(0.30f, 0.90f)
                        ).toArgb()
                    }
                }
                canvas.drawCircle(
                    Offset(w * 0.5f + offsetX * 0.45f, h * 0.35f + offsetY * 0.45f),
                    minDim * (0.44f + realBass * 0.16f),
                    deepPrimaryPaint
                )

                val deepAccentPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        maskFilter = BlurMaskFilter(110f, BlurMaskFilter.Blur.NORMAL)
                        color = animatedAccent.copy(
                            alpha = accentAlpha * 0.72f
                        ).toArgb()
                    }
                }
                canvas.drawCircle(
                    Offset(w * 0.5f - offsetX * 0.5f, h * 0.40f - offsetY * 0.4f),
                    minDim * (0.36f + realKick * 0.18f),
                    deepAccentPaint
                )
            }

            // ۵. ذرات نوری معلق و بوکه در کل فضای پس‌زمینه (Ambient Background Particles & Bokeh Dust)
            val bgParticleCount = 46
            for (i in 0 until bgParticleCount) {
                val seed = i * 31.73f
                val speed = 0.4f + (i % 5) * 0.22f
                val pxNorm = ((sin(seed * 0.73f + particlePhase * speed * 0.5f) + 1f) * 0.5f)
                val pyBase = ((i * 0.0217f + (particlePhase / (2f * PI.toFloat())) * speed) % 1f)
                val pyNorm = 1f - pyBase

                val waveX = cos(particlePhase * 1.6f + seed) * (14f + realBass * 22f)
                val waveY = sin(particlePhase * 1.3f + seed) * (10f + realKick * 18f)
                val px = (pxNorm * w + waveX).coerceIn(0f, w)
                val py = (pyNorm * h + waveY).coerceIn(0f, h)

                val twinkle = 0.35f + 0.65f * ((sin(driftPhase * 2.8f + seed) + 1f) * 0.5f)
                val isBokeh = (i % 6 == 0)
                val pColor = when (i % 3) {
                    0 -> Color.White
                    1 -> animatedAccent
                    else -> animatedSecondary
                }

                if (isBokeh) {
                    val bokehRadius = (10f + (i % 4) * 5.5f + realBass * 8f).dp.toPx()
                    val bokehAlpha = ((0.06f + realEnergy * 0.09f) * twinkle).coerceIn(0.03f, 0.22f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                pColor.copy(alpha = bokehAlpha),
                                pColor.copy(alpha = bokehAlpha * 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(px, py),
                            radius = bokehRadius
                        ),
                        radius = bokehRadius,
                        center = Offset(px, py),
                        blendMode = BlendMode.Screen
                    )
                } else {
                    val dotRadius = (1.3f + (i % 3) * 0.85f + realHigh * 1.4f).dp.toPx()
                    val dotAlpha = ((0.18f + realHigh * 0.36f + realKick * 0.22f) * twinkle).coerceIn(0.08f, 0.78f)
                    drawCircle(
                        color = animatedPrimary.copy(alpha = dotAlpha * 0.35f),
                        radius = dotRadius * 2.5f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = pColor.copy(alpha = dotAlpha),
                        radius = dotRadius,
                        center = Offset(px, py)
                    )
                }
            }

            // ۶. تکسچر ظریف آکوستیک و گرین استودیویی (Fine Acoustic Micro-Texture & Grain Mesh)
            val gridStep = 26.dp.toPx().coerceAtLeast(18f)
            val cols = (w / gridStep).toInt()
            val rows = (h / gridStep).toInt()
            for (r in 0..rows) {
                val y = r * gridStep
                // خطوط افقی میکرو-تکسچر ابریشمی
                if (r % 2 == 0) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.016f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }
                for (c in 0..cols) {
                    if ((r + c) % 2 == 0) {
                        val x = c * gridStep + (if (r % 2 == 0) 0f else gridStep * 0.5f)
                        val grainAlpha = if ((r * 7 + c * 13) % 5 == 0) 0.045f else 0.022f
                        drawCircle(
                            color = Color.White.copy(alpha = grainAlpha),
                            radius = 1.15f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }

        // لایه ۳: سایه عمیق و سینمایی لبه‌های بالا و پایین برای کنتراست و خوانایی ۱۰۰٪ متن‌ها
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color(0x66020308),
                        0.24f to Color.Transparent,
                        0.64f to Color.Transparent,
                        1.00f to Color(0xB8020308)
                    )
                )
        )
    }
}
