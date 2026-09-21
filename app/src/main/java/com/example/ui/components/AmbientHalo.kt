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

@Composable
fun AmbientHalo(
    analysisData: AudioAnalysisData,
    palette: AmbientPalette,
    modifier: Modifier = Modifier,
    glowStrength: Float = 0.85f
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
    glowStrength: Float = 0.85f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "haloBreathing")
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleBreathing"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val data = analysisDataProvider()
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = min(size.width, size.height) * 0.44f

            // Fast Attack for transients, Smooth expansion for bass
            val kick = data.kickPulse
            val bassExpansion = data.haloExpansion
            val totalEnergy = data.totalEnergy

            val dynamicRadius = baseRadius * idleBreathing * (1f + bassExpansion * 0.40f + kick * 0.28f)
            val effectiveAlpha = (0.28f + bassExpansion * 0.42f + kick * 0.35f) * glowStrength

            // Layer 1: Outermost diffuse ambient glow (Bass breathing & dark base blending)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.haloGlow.copy(alpha = (effectiveAlpha * 0.65f).coerceIn(0f, 1f)),
                        palette.primary.copy(alpha = (effectiveAlpha * 0.30f).coerceIn(0f, 1f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 1.6f
                ),
                radius = dynamicRadius * 1.6f,
                center = center
            )

            // Layer 2: Core energetic aura ring (Secondary harmony)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.secondary.copy(alpha = (effectiveAlpha * 0.85f).coerceIn(0f, 1f)),
                        palette.primary.copy(alpha = (effectiveAlpha * 0.40f).coerceIn(0f, 1f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 1.1f
                ),
                radius = dynamicRadius * 1.1f,
                center = center
            )

            // Layer 3: Sharp Kick pulse & transient highlight ring (Fast Attack & Decay)
            if (kick > 0.05f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = (kick * 0.85f * glowStrength).coerceIn(0f, 0.95f)),
                            palette.secondary.copy(alpha = (kick * 0.40f * glowStrength).coerceIn(0f, 0.70f)),
                            Color.Transparent
                        ),
                        center = center,
                        radius = dynamicRadius * 0.90f
                    ),
                    radius = dynamicRadius * 0.90f,
                    center = center
                )
            }
        }
    }
}
