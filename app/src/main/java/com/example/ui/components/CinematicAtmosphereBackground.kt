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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.Track
import kotlinx.coroutines.isActive
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
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "npBgPrimary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "npBgSecondary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "npBgAccent"
    )
    val animatedDeep by animateColorAsState(
        targetValue = palette.deepAtmosphere,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "npBgDeep"
    )

    val latestAnalysis by rememberUpdatedState(analysisDataProvider)

    var phase1 by remember { mutableFloatStateOf(0f) }
    var phase2 by remember { mutableFloatStateOf(1.8f) }
    var phase3 by remember { mutableFloatStateOf(3.6f) }
    var smoothVibeEnergy by remember { mutableFloatStateOf(0.35f) }

    LaunchedEffect(track?.id) {
        var lastNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { nowNanos ->
                val dt = ((nowNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = nowNanos

                val data = latestAnalysis()
                val overallMoodEnergy = (
                    data.totalEnergy * 0.45f +
                    data.mid * 0.25f +
                    data.lowMid * 0.15f +
                    data.bass * 0.15f
                ).coerceIn(0.18f, 1.0f)

                smoothVibeEnergy += (overallMoodEnergy - smoothVibeEnergy) * (dt * 5.5f).coerceIn(0f, 1f)

                // سرعت حرکت کاملاً محسوس و متناسب با فضای کلی آهنگ (آرام در آهنگ ملایم، سریع در آهنگ پرانرژی)
                val speed1 = 0.75f + (smoothVibeEnergy * 1.95f)
                val speed2 = 0.58f + (smoothVibeEnergy * 1.65f)
                val speed3 = 0.90f + (smoothVibeEnergy * 2.20f)

                val twoPi = (2f * PI).toFloat() * 100f
                phase1 = (phase1 + dt * speed1) % twoPi
                phase2 = (phase2 + dt * speed2) % twoPi
                phase3 = (phase3 + dt * speed3) % twoPi
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05060C))
    ) {
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
                    alpha = 0.36f
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    animatedPrimary.copy(alpha = 0.35f),
                                    animatedSecondary.copy(alpha = 0.22f),
                                    animatedDeep
                                )
                            )
                        )
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val minDim = min(w, h)
            val motionRangeX = w * (0.20f + smoothVibeEnergy * 0.16f)
            val motionRangeY = h * (0.16f + smoothVibeEnergy * 0.14f)
            val dynamicScale = 0.92f + (smoothVibeEnergy * 0.36f)

            fun drawFlowingMoodOrb(
                cx: Float,
                cy: Float,
                velY: Float,
                radius: Float,
                color: Color,
                alpha: Float
            ) {
                val stretchY = (1f + kotlin.math.abs(velY) * (0.28f + smoothVibeEnergy * 0.35f)).coerceIn(1f, 1.75f)
                withTransform({
                    scale(scaleX = 1f, scaleY = stretchY, pivot = Offset(cx, cy))
                }) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to color.copy(alpha = (alpha * glowStrength).coerceIn(0f, 0.85f)),
                                0.45f to color.copy(alpha = (alpha * 0.55f * glowStrength).coerceIn(0f, 0.65f)),
                                0.78f to color.copy(alpha = (alpha * 0.18f * glowStrength).coerceIn(0f, 0.35f)),
                                1.00f to Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(cx, cy),
                        blendMode = BlendMode.Screen
                    )
                }
            }

            // ۱. موج اول: حرکت عمودی و افقی کاملاً ملموس در پشت کاور آهنگ
            val orb1X = (w * 0.50f) + cos(phase1) * motionRangeX
            val orb1Y = (h * 0.34f) + sin(phase1) * motionRangeY
            drawFlowingMoodOrb(
                cx = orb1X,
                cy = orb1Y,
                velY = cos(phase1),
                radius = minDim * 0.75f * dynamicScale,
                color = animatedPrimary,
                alpha = 0.68f + smoothVibeEnergy * 0.18f
            )

            // ۲. موج دوم: حرکت معکوس هماهنگ با فضای آهنگ
            val orb2X = (w * 0.50f) - sin(phase2) * (motionRangeX * 1.1f)
            val orb2Y = (h * 0.44f) + cos(phase2) * (motionRangeY * 1.15f)
            drawFlowingMoodOrb(
                cx = orb2X,
                cy = orb2Y,
                velY = -sin(phase2),
                radius = minDim * 0.68f * dynamicScale,
                color = animatedSecondary,
                alpha = 0.60f + smoothVibeEnergy * 0.18f
            )

            // ۳. موج سوم: گردش نوری در بخش میانی و پایینی
            val orb3X = (w * 0.50f) + sin(phase3) * (motionRangeX * 0.9f)
            val orb3Y = (h * 0.68f) - cos(phase3) * (motionRangeY * 0.85f)
            drawFlowingMoodOrb(
                cx = orb3X,
                cy = orb3Y,
                velY = sin(phase3),
                radius = minDim * 0.64f * dynamicScale,
                color = animatedAccent,
                alpha = 0.52f + smoothVibeEnergy * 0.16f
            )

            // ۴. هاله بلر گوسی مرکزی که با فضای آهنگ بالا و پایین میرود
            drawIntoCanvas { canvas ->
                val blurPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        maskFilter = BlurMaskFilter(105f, BlurMaskFilter.Blur.NORMAL)
                        color = animatedPrimary.copy(
                            alpha = (0.38f + smoothVibeEnergy * 0.38f).coerceIn(0.25f, 0.80f)
                        ).toArgb()
                    }
                }
                canvas.drawCircle(
                    Offset(
                        x = w * 0.5f + sin(phase1) * (w * 0.10f),
                        y = h * 0.36f + cos(phase2) * (h * 0.10f)
                    ),
                    minDim * (0.40f + smoothVibeEnergy * 0.16f),
                    blurPaint
                )
            }
        }

        // لایه محافظ کنتراست (جلوگیری ۱۰۰٪ از محو شدن متنها و آیکنها در نور پسزمینه)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color(0x8804050A),
                        0.18f to Color(0x3804050A),
                        0.55f to Color(0x4504050A),
                        1.00f to Color(0xCC04050A)
                    )
                )
        )
    }
}
