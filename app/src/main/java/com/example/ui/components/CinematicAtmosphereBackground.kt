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
    dimmingFactor: Float = 0.72f,
    glowStrength: Float = 0.95f
) {
    val animatedPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "npBgPrimary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "npBgSecondary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "npBgAccent"
    )
    val animatedDeep by animateColorAsState(
        targetValue = palette.deepAtmosphere,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "npBgDeep"
    )

    // حرکت بسیار آرام، سنگین و نامحسوس (چرخه ۳۶ ثانیه‌ای)؛ فقط در حدی که زنده بودن نور حس شود
    val infiniteTransition = rememberInfiniteTransition(label = "SubtleAmbientDrift")
    val slowDriftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 36000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "slowDriftPhase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF04050A))
    ) {
        // لایه ۱: تصویر کاور آهنگ با بلر عمیق در پس‌زمینه
        val context = LocalContext.current
        AnimatedContent(
            targetState = track?.artworkUri,
            transitionSpec = {
                fadeIn(animationSpec = tween(900, easing = FastOutSlowInEasing))
                    .togetherWith(fadeOut(animationSpec = tween(700, easing = FastOutSlowInEasing)))
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
                        .blur(68.dp),
                    alpha = 0.44f
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    animatedPrimary.copy(alpha = 0.38f),
                                    animatedSecondary.copy(alpha = 0.25f),
                                    animatedDeep
                                )
                            )
                        )
                )
            }
        }

        // لایه ۲: هاله نوری بسیار آرام که شدت و درخشش آن مستقیماً با قدرت واقعی بیس و کیک آهنگ کم و زیاد می‌شود
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val audio = analysisDataProvider()
            val realBass = audio.bass.coerceIn(0f, 1f)
            val realKick = audio.kickPulse.coerceIn(0f, 1f)
            val realEnergy = audio.totalEnergy.coerceIn(0f, 1f)

            val minDim = min(w, h)

            // جابجایی بسیار ظریف و آرام (فقط ۵ تا ۶ درصد عرض صفحه در طول ۳۶ ثانیه)
            val subtleOffsetX = cos(slowDriftPhase) * (w * 0.055f)
            val subtleOffsetY = sin(slowDriftPhase) * (h * 0.045f)

            // شدت پایه آرام است (0.28f) و فقط وقتی بیس و کیک واقعی قوی می‌شوند تا 0.92f اوج می‌گیرد
            val primaryGlowAlpha = ((0.28f + realBass * 0.42f + realKick * 0.25f) * glowStrength).coerceIn(0.18f, 0.92f)
            val secondaryGlowAlpha = ((0.22f + realEnergy * 0.35f + realBass * 0.20f) * glowStrength).coerceIn(0.15f, 0.82f)
            val dynamicRadiusScale = 1f + (realBass * 0.28f) + (realKick * 0.18f)

            // هاله اول: رنگ غالب کاور در پشت استیج کاور آهنگ
            val center1 = Offset(w * 0.50f + subtleOffsetX, h * 0.36f + subtleOffsetY)
            val radius1 = minDim * 0.78f * dynamicRadiusScale
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to animatedPrimary.copy(alpha = primaryGlowAlpha),
                        0.45f to animatedPrimary.copy(alpha = primaryGlowAlpha * 0.52f),
                        0.80f to animatedSecondary.copy(alpha = primaryGlowAlpha * 0.18f),
                        1.00f to Color.Transparent
                    ),
                    center = center1,
                    radius = radius1
                ),
                radius = radius1,
                center = center1,
                blendMode = BlendMode.Screen
            )

            // هاله دوم: رنگ مکمل کاور با حرکت معکوس بسیار آرام
            val center2 = Offset(w * 0.48f - subtleOffsetX * 0.8f, h * 0.68f - subtleOffsetY * 0.8f)
            val radius2 = minDim * 0.72f * (1f + realEnergy * 0.22f)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to animatedSecondary.copy(alpha = secondaryGlowAlpha),
                        0.50f to animatedAccent.copy(alpha = secondaryGlowAlpha * 0.45f),
                        1.00f to Color.Transparent
                    ),
                    center = center2,
                    radius = radius2
                ),
                radius = radius2,
                center = center2,
                blendMode = BlendMode.Screen
            )

            // هاله سوم: درخشش عمیق و محو (Gaussian Blur) که دقیقاً با ضربه واقعی کیک و بیس جان می‌گیرد
            if (realBass > 0.05f || realKick > 0.05f) {
                drawIntoCanvas { canvas ->
                    val kickIntensity = (realBass * 0.50f + realKick * 0.50f).coerceIn(0f, 1f)
                    val blurPaint = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            maskFilter = BlurMaskFilter(115f, BlurMaskFilter.Blur.NORMAL)
                            color = animatedAccent.copy(alpha = (kickIntensity * 0.68f * glowStrength).coerceIn(0f, 0.85f)).toArgb()
                        }
                    }
                    canvas.drawCircle(
                        Offset(w * 0.5f, h * 0.36f),
                        minDim * (0.36f + kickIntensity * 0.22f),
                        blurPaint
                    )
                }
            }
        }

        // لایه ۳: پوشش تاریک ملایم برای کنتراست عالی آیکون‌ها و نوشته‌ها
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color(0x5004050A),
                        0.30f to Color.Transparent,
                        0.70f to Color(0x3504050A),
                        1.00f to Color(0xB804050A)
                    )
                )
        )
    }
}
