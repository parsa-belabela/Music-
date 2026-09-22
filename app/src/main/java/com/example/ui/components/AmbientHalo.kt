package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import kotlin.math.min

/**
 * Vivid, highly vibrant, living ambient halo lighting effect.
 * Uses chromatic multi-stop radials avoiding murky black-alpha interpolation
 * to deliver electric, breathing, eye-catching illumination behind artwork.
 */
@Composable
fun AmbientHalo(
    analysisData: AudioAnalysisData,
    palette: AmbientPalette,
    modifier: Modifier = Modifier,
    glowStrength: Float = 0.95f
) {
    AmbientHalo(
        analysisDataProvider = { analysisData },
        palette = palette,
        modifier = modifier,
        glowStrength = glowStrength
    )
}

@Composable
fun AmbientHalo(
    analysisDataProvider: () -> AudioAnalysisData,
    palette: AmbientPalette,
    modifier: Modifier = Modifier,
    glowStrength: Float = 0.95f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "haloBreathing")
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleBreathing"
    )

    val microShimmer by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "microShimmer"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val data = analysisDataProvider()
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = min(size.width, size.height) * 0.46f

            val kick = data.kickPulse
            val bassExpansion = data.haloExpansion
            val totalEnergy = data.totalEnergy

            val dynamicRadius = baseRadius * idleBreathing * (1f + bassExpansion * 0.45f + kick * 0.32f)
            val effectiveAlpha = (0.38f + bassExpansion * 0.48f + kick * 0.38f + totalEnergy * 0.20f) * glowStrength * microShimmer

            // Layer 1: Outermost Deep Radiant Atmosphere Veil (Smooth color-to-alpha without grey dirtying)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.haloGlow.copy(alpha = (effectiveAlpha * 0.85f).coerceIn(0f, 1f)),
                        palette.primary.copy(alpha = (effectiveAlpha * 0.45f).coerceIn(0f, 1f)),
                        palette.primary.copy(alpha = 0f)
                    ),
                    center = center,
                    radius = dynamicRadius * 1.65f
                ),
                radius = dynamicRadius * 1.65f,
                center = center
            )

            // Layer 2: Core Electric Neon Aura (Secondary & Primary harmonic fusion)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accent.copy(alpha = (effectiveAlpha * 0.95f).coerceIn(0f, 1f)),
                        palette.secondary.copy(alpha = (effectiveAlpha * 0.65f).coerceIn(0f, 1f)),
                        palette.primary.copy(alpha = (effectiveAlpha * 0.25f).coerceIn(0f, 1f)),
                        palette.secondary.copy(alpha = 0f)
                    ),
                    center = center,
                    radius = dynamicRadius * 1.15f
                ),
                radius = dynamicRadius * 1.15f,
                center = center
            )

            // Layer 3: Dynamic Bass/Kick Shockwave & Brilliant Center Flash
            if (kick > 0.03f || totalEnergy > 0.15f) {
                val shockAlpha = (kick * 0.95f * glowStrength + totalEnergy * 0.35f).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = shockAlpha * 0.70f),
                            palette.accent.copy(alpha = shockAlpha),
                            palette.accent.copy(alpha = 0f)
                        ),
                        center = center,
                        radius = dynamicRadius * 0.95f
                    ),
                    radius = dynamicRadius * 0.95f,
                    center = center
                )
            }
        }
    }
}
