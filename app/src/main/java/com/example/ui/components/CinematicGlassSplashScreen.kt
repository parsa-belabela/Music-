package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * نشان و لوگوی مینیمال و شیک به سبک اسپاتیفای (Spotify-Inspired Acoustic Wave Emblem)
 * دایره‌ی سبز زمردی درخشان با ۳ موج صوتی منحنی و دقیق
 */
@Composable
fun AuraBrandLogoBadge(
    size: Dp = 44.dp,
    cornerRadius: Dp = 22.dp,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SpotifyStyleLogoPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    val wavePath = remember { Path() }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (animated) {
                    scaleX = pulse
                    scaleY = pulse
                }
            }
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                spotColor = Color(0xFF1ED760),
                ambientColor = Color(0xFF10B981)
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF24F06D),
                        Color(0xFF1ED760),
                        Color(0xFF12A847)
                    ),
                    start = Offset.Zero,
                    end = Offset(180f, 180f)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color(0xFF1ED760).copy(alpha = 0.25f),
                        Color(0xFF05140A).copy(alpha = 0.45f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // موج اول (بالایی - عریض‌ترین)
            wavePath.reset()
            wavePath.moveTo(w * 0.24f, h * 0.36f)
            wavePath.cubicTo(
                w * 0.42f, h * 0.28f,
                w * 0.64f, h * 0.30f,
                w * 0.79f, h * 0.41f
            )
            drawPath(
                path = wavePath,
                color = Color(0xFF060A08),
                style = Stroke(width = w * 0.095f, cap = StrokeCap.Round)
            )

            // موج دوم (میانی)
            wavePath.reset()
            wavePath.moveTo(w * 0.28f, h * 0.53f)
            wavePath.cubicTo(
                w * 0.44f, h * 0.46f,
                w * 0.61f, h * 0.48f,
                w * 0.74f, h * 0.57f
            )
            drawPath(
                path = wavePath,
                color = Color(0xFF060A08),
                style = Stroke(width = w * 0.082f, cap = StrokeCap.Round)
            )

            // موج سوم (پایینی)
            wavePath.reset()
            wavePath.moveTo(w * 0.33f, h * 0.69f)
            wavePath.cubicTo(
                w * 0.46f, h * 0.64f,
                w * 0.58f, h * 0.65f,
                w * 0.68f, h * 0.73f
            )
            drawPath(
                path = wavePath,
                color = Color(0xFF060A08),
                style = Stroke(width = w * 0.070f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun CinematicGlassSplashScreen(
    theme: AppTheme,
    onSplashFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1250)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashOrb")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val brandColors = listOf(
        Color(0xFF1ED760), // Spotify Vibrant Emerald
        Color(0xFF10B981), // Deep Mint
        Color(0xFF00E5FF)  // Electric Cyan Accent
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07120D),
                        Color(0xFF040806),
                        Color(0xFF020403)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        brandColors[0].copy(alpha = glowAlpha * 0.36f),
                        brandColors[1].copy(alpha = glowAlpha * 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.68f * pulseScale
                )
            )

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        brandColors[0],
                        brandColors[2],
                        brandColors[1],
                        brandColors[0]
                    )
                ),
                center = center,
                radius = 96.dp.toPx() * pulseScale,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            AuraBrandLogoBadge(
                size = 116.dp,
                animated = true
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "AURA MUSIC",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "STUDIO LIQUID GLASS • HI-FI SOUND",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.4.sp,
                    color = Color(0xFF1ED760)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { i ->
                    val dotAlpha = (sin(Math.toRadians(rotationAngle * 2.0 + (i * 45))).toFloat() + 1f) / 2f
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(brandColors[i % brandColors.size].copy(alpha = 0.35f + dotAlpha * 0.65f))
                    )
                }
            }
        }
    }
}
