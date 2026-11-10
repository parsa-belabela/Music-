package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.Track
import com.example.ui.theme.AuraCanvas
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic, music-reactive moving aurora & halo background for NowPlayingScreen.
 * Smoothly moves luminous color fields extracted from the album cover behind the player.
 */
@Composable
fun CinematicAtmosphereBackground(
    track: Track?,
    palette: AmbientPalette,
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() },
    glowStrength: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val rawPrimary = if (palette.isMonochrome) {
        Color(0xFF1ED760)
    } else {
        palette.primary.copy(alpha = 1f)
    }
    val rawSecondary = if (palette.isMonochrome) {
        Color(0xFF14833B)
    } else {
        palette.secondary.copy(alpha = 1f)
    }
    val rawAccent = if (palette.isMonochrome) {
        Color(0xFF3BE477)
    } else {
        palette.accent.copy(alpha = 1f)
    }

    val primaryColor by animateColorAsState(
        targetValue = lerp(Color(0xFF141416), rawPrimary, (0.85f * glowStrength).coerceIn(0.45f, 0.95f)),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "npBgPrimary"
    )
    val secondaryColor by animateColorAsState(
        targetValue = lerp(AuraCanvas, rawSecondary, (0.72f * glowStrength).coerceIn(0.35f, 0.88f)),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "npBgSecondary"
    )
    val accentColor by animateColorAsState(
        targetValue = lerp(AuraCanvas, rawAccent, (0.68f * glowStrength).coerceIn(0.3f, 0.85f)),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "npBgAccent"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "npAtmosphereMotion")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "npPhase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = (2f * Math.PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "npPhase2"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "npPulseScale"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val minDim = size.minDimension.coerceAtLeast(1f)
        val analysis = analysisDataProvider()
        val audioBoost = (analysis.haloExpansion * 0.35f).coerceIn(0f, 0.38f)
        val scaleFactor = pulseScale + audioBoost

        // 100% Opaque Solid Base Canvas (Zero transparency so underlying screens never bleed through)
        drawRect(color = Color(0xFF0A0A0C))

        // Opaque base vertical gradient
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to lerp(Color(0xFF0E0E12), primaryColor, 0.55f).copy(alpha = 1f),
                    0.52f to lerp(Color(0xFF0A0A0E), secondaryColor, 0.35f).copy(alpha = 1f),
                    1.0f to Color(0xFF08080A)
                )
            )
        )

        // 1. Main Halo Orb Moving Behind the Album Cover
        val centerOrbX = w * (0.5f + 0.22f * cos(phase1))
        val centerOrbY = h * (0.34f + 0.10f * sin(phase1))
        val centerOrbRadius = (minDim * 0.92f * scaleFactor).coerceAtLeast(120f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = (0.78f + audioBoost * 0.4f).coerceIn(0f, 0.92f)),
                    primaryColor.copy(alpha = 0.36f),
                    Color.Transparent
                ),
                center = Offset(centerOrbX, centerOrbY),
                radius = centerOrbRadius
            ),
            center = Offset(centerOrbX, centerOrbY),
            radius = centerOrbRadius
        )

        // 2. Secondary Sweeping Top-Left / Mid-Right Light Field
        val orb2X = w * (0.28f + 0.28f * sin(phase2))
        val orb2Y = h * (0.20f + 0.12f * cos(phase2))
        val orb2Radius = (minDim * 0.85f * (1.95f - pulseScale * 0.8f) + minDim * audioBoost).coerceAtLeast(100f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = (0.64f + audioBoost * 0.3f).coerceIn(0f, 0.85f)),
                    secondaryColor.copy(alpha = 0.24f),
                    Color.Transparent
                ),
                center = Offset(orb2X, orb2Y),
                radius = orb2Radius
            ),
            center = Offset(orb2X, orb2Y),
            radius = orb2Radius
        )

        // 3. Accent Moving Lower-Mid Glow
        val orb3X = w * (0.72f + 0.24f * cos(phase2 + 1.1f))
        val orb3Y = h * (0.48f + 0.11f * sin(phase1 + 0.9f))
        val orb3Radius = (minDim * 0.78f * scaleFactor).coerceAtLeast(90f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = (0.52f + audioBoost * 0.25f).coerceIn(0f, 0.78f)),
                    accentColor.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = Offset(orb3X, orb3Y),
                radius = orb3Radius
            ),
            center = Offset(orb3X, orb3Y),
            radius = orb3Radius
        )
    }
}
