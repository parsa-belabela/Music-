package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppTheme
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * نشان و لوگوی اختصاصی، شیک و دوست‌داشتنی Aura Music
 * (ترکیب هدفون کریستالی، قلب نوری و نت موسیقی شیشه‌ای)
 */
@Composable
fun AuraBrandLogoBadge(
    size: Dp = 44.dp,
    cornerRadius: Dp = 15.dp,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraLogoPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )
    val shimmerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logoShimmer"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (animated) {
                    scaleX = pulse
                    scaleY = pulse
                }
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF240D3D),
                        Color(0xFF120722),
                        Color(0xFF090414)
                    ),
                    start = Offset.Zero,
                    end = Offset(200f, 200f)
                )
            )
            .border(
                width = 1.4.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF2E93),
                        Color(0xFFFF85A1),
                        Color(0xFFFFFFFF),
                        Color(0xFF00F5D4),
                        Color(0xFFC77DFF),
                        Color(0xFFFF2E93)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        // هاله نوری لطیف پشت لوگو
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rad = Math.toRadians(shimmerAngle.toDouble())
            val glowCenter = Offset(
                x = this.size.width * (0.5f + 0.18f * cos(rad).toFloat()),
                y = this.size.height * (0.5f + 0.18f * sin(rad).toFloat())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF2E93).copy(alpha = 0.42f),
                        Color(0xFF00F5D4).copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = glowCenter,
                    radius = this.size.width * 0.65f
                )
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "Aura Music Logo",
            modifier = Modifier.fillMaxSize(0.96f)
        )
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
        initialValue = 0.94f,
        targetValue = 1.06f,
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
        Color(0xFFFF2E93), // Rose Magenta
        Color(0xFF00F5D4), // Crystal Aqua
        Color(0xFFC77DFF)  // Dreamy Lavender
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF140826),
                        Color(0xFF090412),
                        Color(0xFF040208)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Multi-point kinetic aurora light field
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer dynamic rose-aqua aura halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        brandColors[0].copy(alpha = glowAlpha * 0.42f),
                        brandColors[2].copy(alpha = glowAlpha * 0.26f),
                        brandColors[1].copy(alpha = glowAlpha * 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.72f * pulseScale
                )
            )

            // Orbiting chromatic satellites
            val rad = Math.toRadians(rotationAngle.toDouble())
            val orbitRadius = size.width * 0.30f
            val sat1 = Offset(
                center.x + (orbitRadius * cos(rad)).toFloat(),
                center.y + (orbitRadius * sin(rad)).toFloat()
            )
            val sat2 = Offset(
                center.x + (orbitRadius * cos(rad + Math.PI)).toFloat(),
                center.y + (orbitRadius * sin(rad + Math.PI)).toFloat()
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(brandColors[0].copy(alpha = 0.75f), Color.Transparent),
                    center = sat1,
                    radius = 110f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(brandColors[1].copy(alpha = 0.75f), Color.Transparent),
                    center = sat2,
                    radius = 110f
                )
            )

            // Rotating Prismatic Silk Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        brandColors[0],
                        Color(0xFFFFD166),
                        brandColors[1],
                        brandColors[2],
                        brandColors[0]
                    )
                ),
                center = center,
                radius = 114.dp.toPx() * pulseScale,
                style = Stroke(width = 2.2.dp.toPx())
            )
        }

        // Center Luxury Emblem & Typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .liquidGlass(
                        shape = RoundedCornerShape(38.dp),
                        thickness = GlassThickness.THICK,
                        tintColor = brandColors[0],
                        tintAlpha = 0.24f,
                        borderWidth = 1.8.dp,
                        secondaryAccent = brandColors[1],
                        appTheme = theme
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Aura Music Emblem",
                    modifier = Modifier.size(124.dp)
                )
            }

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
                text = "VELVET LIQUID GLASS • HI-FI SOUND",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.4.sp,
                    color = Color(0xFFFF85A1)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Shimmering micro indicator dots
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
