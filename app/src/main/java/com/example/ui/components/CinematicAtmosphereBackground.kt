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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.Track
import kotlin.math.min

/**
 * Cinematic Living Atmosphere Background:
 * - Heavily blurred, low-contrast, darkened album art back-layer
 * - Dynamic audio-reactive ambient aura field (Idea 02 & Idea 08)
 * - True deep black preservation with organic light emission
 * - Contrast protection layer for crystal clear text readability
 */
@Composable
fun CinematicAtmosphereBackground(
    track: Track?,
    palette: AmbientPalette,
    analysisDataProvider: () -> AudioAnalysisData,
    modifier: Modifier = Modifier,
    dimmingFactor: Float = 0.72f,
    glowStrength: Float = 0.9f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "livingAtmosphereMotion")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientWave"
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF06060A))) {
        // Layer 1: Heavily blurred, soft, darkened Album Art (idea 2)
        if (track?.artworkUri != null) {
            AsyncImage(
                model = track.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(90.dp)
            )
        }

        // Layer 2: Atmospheric Dimming & Contrast Preservation Veil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.deepAtmosphere.copy(alpha = 0.82f),
                            Color(0xEE07070D),
                            Color(0xFA050508)
                        )
                    )
                )
        )

        // Layer 3: Organic Energy Field & Audio-reactive Aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            val data = analysisDataProvider()
            val center = Offset(size.width * 0.5f, size.height * 0.42f)
            val maxDimension = min(size.width, size.height)

            val bass = data.haloExpansion
            val kick = data.kickPulse
            val energy = data.totalEnergy

            // Dynamic radius driven by song energy and gentle cosmic rotation
            val baseRadius = maxDimension * (0.65f + bass * 0.35f)
            val radAngle = Math.toRadians(waveOffset.toDouble())
            val driftX = (Math.cos(radAngle) * 35.0).toFloat()
            val driftY = (Math.sin(radAngle) * 25.0).toFloat()
            val auraCenter = Offset(center.x + driftX, center.y + driftY)

            // Outermost deep ambient aura wash
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.primary.copy(alpha = (0.28f + bass * 0.25f + kick * 0.15f) * glowStrength),
                        palette.secondary.copy(alpha = (0.16f + energy * 0.18f) * glowStrength),
                        Color.Transparent
                    ),
                    center = auraCenter,
                    radius = baseRadius * 1.5f
                ),
                radius = baseRadius * 1.5f,
                center = auraCenter
            )

            // Concentric secondary bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.secondary.copy(alpha = (0.22f + kick * 0.28f) * glowStrength),
                        palette.accent.copy(alpha = (0.10f + bass * 0.12f) * glowStrength),
                        Color.Transparent
                    ),
                    center = auraCenter,
                    radius = baseRadius * 0.95f
                ),
                radius = baseRadius * 0.95f,
                center = auraCenter
            )

            // Transient kick pulse burst (organic micro-interaction)
            if (kick > 0.08f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = (kick * 0.45f * glowStrength).coerceIn(0f, 0.65f)),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 0.65f
                    ),
                    radius = baseRadius * 0.65f,
                    center = center
                )
            }
        }

        // Layer 4: Vignette and dark base protector
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xC0050508)
                        ),
                        radius = 1200f
                    )
                )
        )
    }
}
