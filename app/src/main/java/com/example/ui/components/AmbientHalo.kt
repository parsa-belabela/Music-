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
    val infiniteTransition = rememberInfiniteTransition(label = "haloBreathing")
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleBreathing"
    )

    // Smooth dynamic bass & kick expansion
    val animatedExpansion by animateFloatAsState(
        targetValue = analysisData.haloExpansion,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "animatedExpansion"
    )

    val animatedKickPulse by animateFloatAsState(
        targetValue = analysisData.kickPulse,
        animationSpec = tween(durationMillis = 80, easing = LinearEasing),
        label = "animatedKickPulse"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = min(size.width, size.height) * 0.42f

            // Calculate dynamic halo radius based on bass expansion + kick pulse
            val dynamicRadius = baseRadius * idleBreathing * (1f + animatedExpansion * 0.35f + animatedKickPulse * 0.25f)
            val effectiveAlpha = (0.25f + animatedExpansion * 0.4f + animatedKickPulse * 0.3f) * glowStrength

            // Layer 1: Outermost diffuse ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.haloGlow.copy(alpha = (effectiveAlpha * 0.6f).coerceIn(0f, 1f)),
                        palette.primary.copy(alpha = (effectiveAlpha * 0.3f).coerceIn(0f, 1f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius * 1.5f
                ),
                radius = dynamicRadius * 1.5f,
                center = center
            )

            // Layer 2: Core energetic aura ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.secondary.copy(alpha = (effectiveAlpha * 0.8f).coerceIn(0f, 1f)),
                        palette.primary.copy(alpha = (effectiveAlpha * 0.45f).coerceIn(0f, 1f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicRadius
                ),
                radius = dynamicRadius,
                center = center
            )

            // Layer 3: Sharp Kick pulse inner halo
            if (animatedKickPulse > 0.1f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = (animatedKickPulse * 0.7f).coerceIn(0f, 0.9f)),
                            Color.Transparent
                        ),
                        center = center,
                        radius = dynamicRadius * 0.85f
                    ),
                    radius = dynamicRadius * 0.85f,
                    center = center
                )
            }
        }
    }
}
