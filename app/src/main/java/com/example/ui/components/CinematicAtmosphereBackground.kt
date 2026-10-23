package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    // تغییر نرم و مستقیم رنگ‌های پس‌زمینه بر اساس رنگ‌های استخراج‌شده از کاور آهنگ
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

    val infiniteTransition = rememberInfiniteTransition(label = "CoverAuraDrift")
    val driftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "driftPhase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(animatedDeep)
    ) {
        // لایه ۱: تصویر بلر شده‌ی خودِ کاور آهنگ در پس‌زمینه برای هماهنگی ۱۰۰٪ رنگ‌ها با کاور
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
                        .blur(62.dp),
                    alpha = 0.58f
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    animatedPrimary.copy(alpha = 0.55f),
                                    animatedSecondary.copy(alpha = 0.40f),
                                    animatedDeep
                                )
                            )
                        )
                )
            }
        }

        // لایه ۲: هاله‌های نوری بلر شده (Gaussian Blur) با رنگ دقیق کاور آهنگ و واکنش به انرژی موزیک
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val audio = analysisDataProvider()
            val realBass = audio.bass.coerceIn(0f, 1f)
            val realKick = audio.kickPulse.coerceIn(0f, 1f)
            val realEnergy = audio.totalEnergy.coerceIn(0f, 1f)

            val minDim = min(w, h)
            val speedBoost = 1f + realEnergy * 0.4f
            val offsetX = cos(driftPhase) * (w * 0.10f * speedBoost)
            val offsetY = sin(driftPhase) * (h * 0.08f * speedBoost)

            val primaryAlpha = ((0.48f + realBass * 0.32f + realKick * 0.18f) * glowStrength).coerceIn(0.30f, 0.94f)
            val secondaryAlpha = ((0.42f + realEnergy * 0.30f + realBass * 0.15f) * glowStrength).coerceIn(0.25f, 0.88f)
            val scaleBoost = 1f + (realBass * 0.25f) + (realKick * 0.15f)

            // هاله اصلی کاور در نیمه بالایی (پشت کاور آهنگ)
            val c1 = Offset(w * 0.50f + offsetX, h * 0.35f + offsetY)
            val r1 = minDim * 0.85f * scaleBoost
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to animatedPrimary.copy(alpha = primaryAlpha),
                        0.48f to animatedPrimary.copy(alpha = primaryAlpha * 0.58f),
                        0.82f to animatedSecondary.copy(alpha = primaryAlpha * 0.22f),
                        1.00f to Color.Transparent
                    ),
                    center = c1,
                    radius = r1
                ),
                radius = r1,
                center = c1,
                blendMode = BlendMode.Screen
            )

            // هاله رنگ دوم کاور در نیمه پایینی
            val c2 = Offset(w * 0.50f - offsetX * 0.85f, h * 0.68f - offsetY * 0.85f)
            val r2 = minDim * 0.78f * (1f + realEnergy * 0.22f)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to animatedSecondary.copy(alpha = secondaryAlpha),
                        0.52f to animatedAccent.copy(alpha = secondaryAlpha * 0.48f),
                        1.00f to Color.Transparent
                    ),
                    center = c2,
                    radius = r2
                ),
                radius = r2,
                center = c2,
                blendMode = BlendMode.Screen
            )

            // درخشش مرکزی بلر شده دقیقاً پشت قاب کاور آهنگ
            drawIntoCanvas { canvas ->
                val centerGlowPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        maskFilter = BlurMaskFilter(110f, BlurMaskFilter.Blur.NORMAL)
                        color = animatedAccent.copy(
                            alpha = (0.38f + realBass * 0.35f + realKick * 0.25f).coerceIn(0.25f, 0.88f)
                        ).toArgb()
                    }
                }
                canvas.drawCircle(
                    Offset(w * 0.5f, h * 0.36f),
                    minDim * (0.40f + realBass * 0.16f),
                    centerGlowPaint
                )
            }
        }

        // لایه ۳: سایه ملایم لبه بالا و پایین برای تضمین خوانایی ۱۰۰٪ متن‌ها و دکمه‌ها
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color(0x4804050A),
                        0.28f to Color.Transparent,
                        0.68f to Color.Transparent,
                        1.00f to Color(0x9804050A)
                    )
                )
        )
    }
}
