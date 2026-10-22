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
import androidx.compose.ui.graphics.drawscope.withTransform
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
    dimmingFactor: Float = 0.72f,
    glowStrength: Float = 0.95f
) {
    // تغییر رنگ ۱۰۰٪ هماهنگ با رنگ غالب کاور آهنگ
    val animatedPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "npBgPrimary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "npBgSecondary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "npBgAccent"
    )
    val animatedDeep by animateColorAsState(
        targetValue = palette.deepAtmosphere,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "npBgDeep"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "CoverDrivenMotionBlur")

    val motionPhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "motionPhase1"
    )

    val motionPhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "motionPhase2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04050A))
    ) {
        // لایه ۱: تصویر کاور آهنگ با بلر فوقالعاده عمیق در کل پسزمینه (مشابه Apple Music iOS 18)
        val context = LocalContext.current
        AnimatedContent(
            targetState = track?.artworkUri,
            transitionSpec = {
                fadeIn(animationSpec = tween(800, easing = FastOutSlowInEasing))
                    .togetherWith(fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing)))
            },
            label = "BlurredCoverBackdrop",
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
                        .blur(64.dp),
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
                                    animatedSecondary.copy(alpha = 0.35f),
                                    animatedDeep
                                )
                            )
                        )
                )
            }
        }

        // لایه ۲: هالههای نوری محو و موشنبلر شده (Gaussian Blur + Motion Trail) بر اساس رنگ کاور و ضربان موزیک
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val audio = analysisDataProvider()
            val bass = audio.haloExpansion.coerceIn(0f, 1f)
            val kick = audio.kickPulse.coerceIn(0f, 1f)
            val energy = audio.totalEnergy.coerceIn(0f, 1f)

            val pulseMultiplier = 1f + (bass * 0.32f) + (kick * 0.25f)
            val minDim = min(w, h)

            // تابع رسم نور محو و بلر شده با کشیدگی موشنبلر در جهت حرکت
            fun drawBlurredCoverLight(
                cx: Float,
                cy: Float,
                velocityY: Float,
                radius: Float,
                color: Color,
                alpha: Float
            ) {
                val stretch = (1f + kotlin.math.abs(velocityY) * 0.55f + bass * 0.25f).coerceIn(1f, 1.85f)
                for (i in 2 downTo 0) {
                    val trailY = cy - (velocityY * h * 0.065f * i)
                    val r = radius * (1f - i * 0.15f)
                    val a = (alpha * (1f - i * 0.26f) * glowStrength).coerceIn(0.05f, 0.90f)

                    withTransform({
                        scale(scaleX = 1f, scaleY = stretch, pivot = Offset(cx, trailY))
                    }) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.00f to color.copy(alpha = a),
                                    0.38f to color.copy(alpha = a * 0.65f),
                                    0.75f to color.copy(alpha = a * 0.22f),
                                    1.00f to Color.Transparent
                                ),
                                center = Offset(cx, trailY),
                                radius = r
                            ),
                            radius = r,
                            center = Offset(cx, trailY),
                            blendMode = BlendMode.Screen
                        )
                    }
                }
            }

            // ۱. هاله نوری محو و بزرگ دقیقاً پشت کاور آهنگ (رنگ غالب کاور)
            val coverCenterX = w * (0.50f + 0.12f * cos(motionPhase1))
            val coverCenterY = h * (0.36f + 0.14f * sin(motionPhase1))
            drawBlurredCoverLight(
                cx = coverCenterX,
                cy = coverCenterY,
                velocityY = cos(motionPhase1),
                radius = minDim * 0.78f * pulseMultiplier,
                color = animatedPrimary,
                alpha = 0.78f
            )

            // ۲. موج موشنبلر رنگ دوم کاور (حرکت عمودی بالا و پایین در سمت چپ/راست)
            val wave2X = w * (0.28f + 0.22f * sin(motionPhase2))
            val wave2Y = h * (0.32f + 0.26f * cos(motionPhase2))
            drawBlurredCoverLight(
                cx = wave2X,
                cy = wave2Y,
                velocityY = -sin(motionPhase2),
                radius = minDim * 0.68f * pulseMultiplier,
                color = animatedSecondary,
                alpha = 0.68f
            )

            // ۳. موج موشنبلر رنگ تاکیدی کاور در بخش پایینی (زیر کنترلرهای پلیر)
            val wave3X = w * (0.72f - 0.20f * cos(motionPhase1 + 1.5f))
            val wave3Y = h * (0.72f + 0.18f * sin(motionPhase2))
            drawBlurredCoverLight(
                cx = wave3X,
                cy = wave3Y,
                velocityY = cos(motionPhase2),
                radius = minDim * 0.74f * (1f + energy * 0.3f),
                color = animatedAccent,
                alpha = 0.62f
            )

            // ۴. شوک نوری بلر شده (Real Gaussian Blur Kick Flash) در لحظه ضربه بیس آهنگ
            drawIntoCanvas { canvas ->
                val haloPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        maskFilter = BlurMaskFilter(110f, BlurMaskFilter.Blur.NORMAL)
                        color = animatedPrimary.copy(
                            alpha = (0.42f + kick * 0.45f + bass * 0.25f).coerceIn(0.2f, 0.92f)
                        ).toArgb()
                    }
                }
                canvas.drawCircle(
                    Offset(w * 0.5f, h * 0.36f),
                    minDim * 0.42f * pulseMultiplier,
                    haloPaint
                )
            }
        }

        // لایه ۳: گرادیانت عمق تاریک برای یکپارچگی سینمایی و کنتراست عالی آیکونها
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color(0x5504050A),
                        0.32f to Color.Transparent,
                        0.68f to Color(0x3504050A),
                        1.00f to Color(0xB804050A)
                    )
                )
        )
    }
}
