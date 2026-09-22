package com.example.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.audio.AmbientPalette
import com.example.data.model.AppTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern Apple/iOS-style RGB Motion Graphic Aurora Fluid Glow Background.
 * - Multi-node chromatic orbs shifting in harmonic Lissajous orbital waves
 * - Dynamic color morphing between theme & album artwork palette
 * - Deep fluid blur with rich saturation and vivid luminescence
 * - Subtle audio energy reactivity
 */
@Composable
fun ModernAuroraMotionBackground(
    palette: AmbientPalette,
    appTheme: AppTheme,
    modifier: Modifier = Modifier,
    energyReactiveBoost: Float = 0f,
    intensity: Float = 1.0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "iosAuroraMotion")

    // Slow orbital rotation for harmonious motion
    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraOrb1"
    )

    val angle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auroraOrb2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraPulse"
    )

    // Base background tone according to the selected AppTheme
    val baseBackgroundColor = when (appTheme) {
        AppTheme.GLASS -> Color(0xFF050814)
        AppTheme.LEGO -> Color(0xFF101115)
        AppTheme.CARTOON -> Color(0xFF120E1C)
        AppTheme.CYBER_CHROME -> Color(0xFF090A10)
        AppTheme.VAPORWAVE -> Color(0xFF100720)
        AppTheme.OBSIDIAN_MATRIX -> Color(0xFF040608)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackgroundColor)
    ) {
        // LEGO Cinematic Baseplate Studs Layer (drawn on background, never covers text)
        if (appTheme == AppTheme.LEGO) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (w <= 0 || h <= 0) return@Canvas

                val studSpacing = 28.dp.toPx()
                val studRadius = 5.2.dp.toPx()
                val innerHoleRadius = 2.4.dp.toPx()

                val baseplateColors = listOf(
                    Color(0xFF8A1418), // Deep Crimson Brick
                    Color(0xFF8A7200), // Deep Gold Brick
                    Color(0xFF0C3D7A), // Deep Navy Blue Brick
                    Color(0xFF0F5726), // Deep Forest Green Brick
                    Color(0xFF8A4100), // Deep Rust Orange Brick
                    Color(0xFF0E5466), // Deep Azure Brick
                    Color(0xFF1E2028), // Dark Charcoal Brick
                    Color(0xFF2B2E38)  // Dark Stone Brick
                )

                var col = 0
                var x = studSpacing / 2f
                while (x < w) {
                    var row = 0
                    var y = studSpacing / 2f
                    while (y < h) {
                        val colorIdx = ((col * 4) + (row * 7) + col) % baseplateColors.size
                        val studColor = baseplateColors[colorIdx]

                        // 1. Ambient occlusion contact shadow (bottom-right)
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.65f),
                            radius = studRadius + 1.4f,
                            center = Offset(x + 1.5f, y + 2f)
                        )
                        // 2. Plastic stud cylinder body in authentic colored tone
                        drawCircle(
                            color = studColor,
                            radius = studRadius,
                            center = Offset(x, y)
                        )
                        // 3. Molded top rim specular highlight
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = studRadius * 0.85f,
                            center = Offset(x - 0.7f, y - 0.7f)
                        )
                        // 4. Subtle center stud cavity
                        drawCircle(
                            color = Color(0xFF0D0E12),
                            radius = innerHoleRadius,
                            center = Offset(x, y)
                        )
                        y += studSpacing
                        row++
                    }
                    x += studSpacing
                    col++
                }

                // LEGO Cinematic Searchlight / Volumetric Sweep Beam (The LEGO Movie / Gotham style)
                val beamAngleRad = Math.toRadians((angle1 * 0.6).toDouble())
                val beamOriginX = w * 0.5f
                val beamOriginY = -h * 0.1f
                val sweepX = w * 0.5f + (cos(beamAngleRad).toFloat() * w * 0.6f)
                val sweepY = h * 0.7f + (sin(beamAngleRad).toFloat() * h * 0.3f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x30FFC700), // Amber spotlight core
                            Color(0x15E51D24), // Crimson ambient rim
                            Color.Transparent
                        ),
                        center = Offset(sweepX, sweepY),
                        radius = w * 0.75f * (1f + energyReactiveBoost * 0.3f)
                    ),
                    center = Offset(sweepX, sweepY),
                    radius = w * 0.75f * (1f + energyReactiveBoost * 0.3f)
                )
            }
        }

        // Multi-node dynamic RGB mesh layer with high-energy vibrancy and living light nodes
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(36.dp)
        ) {
            val width = size.width
            val height = size.height
            if (width <= 0 || height <= 0) return@Canvas

            val rad1 = Math.toRadians(angle1.toDouble())
            val rad2 = Math.toRadians(angle2.toDouble())

            val effectiveScale = pulseScale * (1f + energyReactiveBoost * 0.45f) * intensity

            // Node 1: Primary Radiant Living Orb (Top Right to Center)
            val orb1X = width * 0.65f + (cos(rad1).toFloat() * width * 0.24f)
            val orb1Y = height * 0.24f + (sin(rad1).toFloat() * height * 0.20f)
            val orb1Radius = (width * 0.62f) * effectiveScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.primary.copy(alpha = 0.85f * intensity),
                        palette.primary.copy(alpha = 0.45f * intensity),
                        palette.primary.copy(alpha = 0f)
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                center = Offset(orb1X, orb1Y),
                radius = orb1Radius
            )

            // Node 2: Secondary Electric Azure/Cyan Fluid Orb (Bottom Left to Center)
            val orb2X = width * 0.30f + (sin(rad2).toFloat() * width * 0.26f)
            val orb2Y = height * 0.58f + (cos(rad2).toFloat() * height * 0.22f)
            val orb2Radius = (width * 0.66f) * effectiveScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.secondary.copy(alpha = 0.80f * intensity),
                        palette.secondary.copy(alpha = 0.40f * intensity),
                        palette.secondary.copy(alpha = 0f)
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                center = Offset(orb2X, orb2Y),
                radius = orb2Radius
            )

            // Node 3: Vivid Accent / Neon Electric Glow Orb (Top Left to Bottom Right)
            val orb3X = width * 0.22f + (cos(rad2 * 0.85).toFloat() * width * 0.22f)
            val orb3Y = height * 0.16f + (sin(rad1 * 0.75).toFloat() * height * 0.18f)
            val orb3Radius = (width * 0.54f) * effectiveScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = 0.85f * intensity),
                        palette.accent.copy(alpha = 0.38f * intensity),
                        palette.accent.copy(alpha = 0f)
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = orb3Radius
                ),
                center = Offset(orb3X, orb3Y),
                radius = orb3Radius
            )

            // Node 4: Dynamic Center Fusion Core (Breathing Harmonic Hearth)
            val orb4X = width * 0.50f + (sin(rad1 * 0.5).toFloat() * width * 0.12f)
            val orb4Y = height * 0.42f + (cos(rad2 * 0.5).toFloat() * height * 0.12f)
            val orb4Radius = (width * 0.48f) * effectiveScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = 0.60f * intensity),
                        palette.primary.copy(alpha = 0.25f * intensity),
                        palette.primary.copy(alpha = 0f)
                    ),
                    center = Offset(orb4X, orb4Y),
                    radius = orb4Radius
                ),
                center = Offset(orb4X, orb4Y),
                radius = orb4Radius
            )

            // Node 5: Deep Atmosphere Floor (Bottom Depth)
            val orb5X = width * 0.78f + (sin(rad1 * 1.1).toFloat() * width * 0.18f)
            val orb5Y = height * 0.82f + (cos(rad2 * 1.05).toFloat() * height * 0.15f)
            val orb5Radius = (width * 0.58f) * effectiveScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.deepAtmosphere.copy(alpha = 0.80f * intensity),
                        palette.primary.copy(alpha = 0.35f * intensity),
                        palette.primary.copy(alpha = 0f)
                    ),
                    center = Offset(orb5X, orb5Y),
                    radius = orb5Radius
                ),
                center = Offset(orb5X, orb5Y),
                radius = orb5Radius
            )
        }

        // Ambient contrast gradient overlay: leaves lights alive while ensuring crisp text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            baseBackgroundColor.copy(alpha = 0.15f),
                            Color.Transparent,
                            baseBackgroundColor.copy(alpha = 0.55f)
                        )
                    )
                )
        )
    }
}
