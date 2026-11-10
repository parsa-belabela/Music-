package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.Track
import com.example.ui.theme.AuraCanvas

/**
 * Clean Spotify-grade vertical gradient background for the full-screen player.
 * Smoothly blends the album artwork's dominant color at the top into a deep matte dark canvas.
 */
@Composable
fun CinematicAtmosphereBackground(
    track: Track?,
    palette: AmbientPalette,
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() },
    glowStrength: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val rawTopColor = if (palette.isMonochrome) {
        Color(0xFF383838)
    } else {
        lerp(Color(0xFF1E1E1E), palette.primary.copy(alpha = 1f), (0.62f * glowStrength).coerceIn(0.25f, 0.72f))
    }

    val rawMidColor = lerp(AuraCanvas, rawTopColor, 0.35f)

    val topColor by animateColorAsState(
        targetValue = rawTopColor,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "npBgTop"
    )

    val midColor by animateColorAsState(
        targetValue = rawMidColor,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "npBgMid"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to topColor,
                        0.52f to midColor,
                        1.0f to Color(0xFF0A0A0A)
                    )
                )
            )
    )
}

