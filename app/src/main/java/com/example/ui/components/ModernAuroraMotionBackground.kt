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
import com.example.data.model.AppTheme
import com.example.ui.theme.AuraCanvas

/**
 * Clean Spotify-style ambient top wash over a matte #121212 dark background.
 * Eliminates GPU-heavy moving blobs and distracting neon noise.
 */
@Composable
fun ModernAuroraMotionBackground(
    palette: AmbientPalette,
    appTheme: AppTheme = AppTheme.PURE_LIQUID_GLASS,
    energyBoostProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val baseBackground = when (appTheme) {
        AppTheme.DIGITAL_ACID -> Color(0xFF000000)
        AppTheme.MONOCHROME_NOIR -> Color(0xFF0A0A0A)
        else -> AuraCanvas
    }

    val topWashTarget = lerp(baseBackground, palette.primary.copy(alpha = 1f), 0.16f)
    val animatedTopWash by animateColorAsState(
        targetValue = topWashTarget,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ambientTopWash"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to animatedTopWash,
                        0.32f to baseBackground,
                        1.0f to baseBackground
                    )
                )
            )
    )
}

