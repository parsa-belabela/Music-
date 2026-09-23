package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppTheme
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CinematicGlassSplashScreen(
    theme: AppTheme,
    onSplashFinished: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1200)
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
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val themeColors = when (theme) {
        AppTheme.PURE_LIQUID_GLASS -> listOf(Color(0xFF00E5FF), Color(0xFF8B5CF6), Color(0xFFFFFFFF))
        AppTheme.CYBER_NIGHTS -> listOf(Color(0xFF00F0FF), Color(0xFFFF007F), Color(0xFF1E3A8A))
        AppTheme.VELVET_NOIR -> listOf(Color(0xFFFFD700), Color(0xFFA855F7), Color(0xFF701A75))
        AppTheme.SUNSET_RAVE -> listOf(Color(0xFFFF6D00), Color(0xFFFF007F), Color(0xFFFFD600))
        AppTheme.DIGITAL_ACID -> listOf(Color(0xFF39FF14), Color(0xFF00FF66), Color(0xFF059669))
        AppTheme.Y2K_CHROME -> listOf(Color(0xFFE2E8F0), Color(0xFF38BDF8), Color(0xFF818CF8))
        AppTheme.MONOCHROME_NOIR -> listOf(Color(0xFFFFFFFF), Color(0xFF94A3B8), Color(0xFF1E293B))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04060C)),
        contentAlignment = Alignment.Center
    ) {
        // Multi-point kinetic light field
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            
            // Outer dynamic aura halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        themeColors[0].copy(alpha = glowAlpha * 0.45f),
                        themeColors[1].copy(alpha = glowAlpha * 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.7f * pulseScale
                )
            )

            // Orbiting chromatic satellites
            val rad = Math.toRadians(rotationAngle.toDouble())
            val orbitRadius = size.width * 0.32f
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
                    colors = listOf(themeColors[0].copy(alpha = 0.8f), Color.Transparent),
                    center = sat1,
                    radius = 90f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(themeColors[1].copy(alpha = 0.8f), Color.Transparent),
                    center = sat2,
                    radius = 90f
                )
            )

            // Rotating Prismatic Rings
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        themeColors[0],
                        themeColors[1],
                        themeColors[2].copy(alpha = 0.8f),
                        themeColors[0]
                    )
                ),
                center = center,
                radius = 110.dp.toPx() * pulseScale,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Frosted Crystal Glass Capsule
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.THICK,
                        tintColor = themeColors[0],
                        tintAlpha = 0.25f,
                        borderWidth = 1.5.dp,
                        appTheme = theme
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
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
                text = "PURE LIQUID GLASS • HI-FI AUDIO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    color = themeColors[0].copy(alpha = 0.9f)
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
                            .background(themeColors[i % themeColors.size].copy(alpha = 0.3f + dotAlpha * 0.7f))
                    )
                }
            }
        }
    }
}
