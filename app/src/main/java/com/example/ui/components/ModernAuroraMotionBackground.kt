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
        AppTheme.LEGO -> Color(0xFF141416)
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
        // Multi-node blurred RGB mesh layer with enriched vibrancy and alive light nodes
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(68.dp)
        ) {
            val width = size.width
            val height = size.height
            if (width <= 0 || height <= 0) return@Canvas

            val rad1 = Math.toRadians(angle1.toDouble())
            val rad2 = Math.toRadians(angle2.toDouble())

            val effectiveScale = pulseScale * (1f + energyReactiveBoost * 0.38f) * intensity

            // Node 1: Primary Radiant Orb (Top Right to Center)
            val orb1X = width * 0.65f + (cos(rad1).toFloat() * width * 0.24f)
            val orb1Y = height * 0.24f + (sin(rad1).toFloat() * height * 0.20f)
            val orb1Radius = (width * 0.60f) * effectiveScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.primary.copy(alpha = 0.68f * intensity),
                        palette.primary.copy(alpha = 0.30f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                center = Offset(orb1X, orb1Y),
                radius = orb1Radius
            )

            // Node 2: Secondary / Cyan-Blue Fluid Orb (Bottom Left to Center)
            val orb2X = width * 0.30f + (sin(rad2).toFloat() * width * 0.26f)
            val orb2Y = height * 0.58f + (cos(rad2).toFloat() * height * 0.22f)
            val orb2Radius = (width * 0.64f) * effectiveScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.secondary.copy(alpha = 0.62f * intensity),
                        palette.secondary.copy(alpha = 0.26f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                center = Offset(orb2X, orb2Y),
                radius = orb2Radius
            )

            // Node 3: Vivid Accent / Neon Glow Orb (Top Left to Bottom Right)
            val orb3X = width * 0.22f + (cos(rad2 * 0.85).toFloat() * width * 0.22f)
            val orb3Y = height * 0.16f + (sin(rad1 * 0.75).toFloat() * height * 0.18f)
            val orb3Radius = (width * 0.50f) * effectiveScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = 0.66f * intensity),
                        palette.accent.copy(alpha = 0.24f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = orb3Radius
                ),
                center = Offset(orb3X, orb3Y),
                radius = orb3Radius
            )

            // Node 4: Deep Atmosphere Core (Bottom Right & Center-Bottom)
            val orb4X = width * 0.78f + (sin(rad1 * 1.1).toFloat() * width * 0.18f)
            val orb4Y = height * 0.80f + (cos(rad2 * 1.05).toFloat() * height * 0.15f)
            val orb4Radius = (width * 0.56f) * effectiveScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.deepAtmosphere.copy(alpha = 0.72f * intensity),
                        palette.primary.copy(alpha = 0.28f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(orb4X, orb4Y),
                    radius = orb4Radius
                ),
                center = Offset(orb4X, orb4Y),
                radius = orb4Radius
            )
        }

        // Ambient contrast gradient overlay: leaves lights alive while ensuring crisp text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            baseBackgroundColor.copy(alpha = 0.22f),
                            Color.Transparent,
                            baseBackgroundColor.copy(alpha = 0.68f)
                        )
                    )
                )
        )
    }
}
