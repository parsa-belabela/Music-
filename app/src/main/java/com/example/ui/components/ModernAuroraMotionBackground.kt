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
import com.example.data.model.AppTheme
import com.example.ui.theme.AuraCanvas
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic living aurora & ambient light background for the Home and main screens.
 * Renders vivid, smoothly moving light orbs extracted from the active song's palette.
 */
@Composable
fun ModernAuroraMotionBackground(
    palette: AmbientPalette,
    appTheme: AppTheme = AppTheme.PURE_LIQUID_GLASS,
    energyBoostProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val baseBackground = when (appTheme) {
        AppTheme.DIGITAL_ACID -> Color(0xFF050507)
        AppTheme.MONOCHROME_NOIR -> Color(0xFF08080A)
        else -> AuraCanvas
    }

    val rawPrimary = if (palette.isMonochrome) Color(0xFF1ED760) else palette.primary.copy(alpha = 1f)
    val rawSecondary = if (palette.isMonochrome) Color(0xFF169C46) else palette.secondary.copy(alpha = 1f)
    val rawAccent = if (palette.isMonochrome) Color(0xFF2EBD59) else palette.accent.copy(alpha = 1f)

    val primaryGlow by animateColorAsState(
        targetValue = lerp(baseBackground, rawPrimary, 0.72f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "homeAuroraPrimary"
    )
    val secondaryGlow by animateColorAsState(
        targetValue = lerp(baseBackground, rawSecondary, 0.62f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "homeAuroraSecondary"
    )
    val accentGlow by animateColorAsState(
        targetValue = lerp(baseBackground, rawAccent, 0.55f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "homeAuroraAccent"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "homeAuroraMotion")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "homeOrbitAngle"
    )
    val counterOrbitAngle by infiniteTransition.animateFloat(
        initialValue = (2f * Math.PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "homeCounterOrbit"
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "homeBreathe"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val minDim = size.minDimension.coerceAtLeast(1f)
        val energy = energyBoostProvider().coerceIn(0f, 1f)
        val dynamicBoost = 1f + (energy * 0.22f)

        // Base dark canvas
        drawRect(color = baseBackground)

        // 1. Primary Moving Top-Center Aurora Light Orb
        val orb1X = w * (0.5f + 0.28f * cos(orbitAngle))
        val orb1Y = h * (0.16f + 0.09f * sin(orbitAngle))
        val orb1Radius = (minDim * 0.95f * breathe * dynamicBoost).coerceAtLeast(100f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = (0.58f + energy * 0.16f).coerceIn(0f, 0.82f)),
                    primaryGlow.copy(alpha = 0.24f),
                    Color.Transparent
                ),
                center = Offset(orb1X, orb1Y),
                radius = orb1Radius
            ),
            center = Offset(orb1X, orb1Y),
            radius = orb1Radius
        )

        // 2. Secondary Drifting Side Aurora Light Orb
        val orb2X = w * (0.22f + 0.26f * sin(counterOrbitAngle))
        val orb2Y = h * (0.34f + 0.11f * cos(counterOrbitAngle))
        val orb2Radius = (minDim * 0.82f * (2f - breathe * 0.85f) * dynamicBoost).coerceAtLeast(90f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryGlow.copy(alpha = (0.44f + energy * 0.14f).coerceIn(0f, 0.72f)),
                    secondaryGlow.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = Offset(orb2X, orb2Y),
                radius = orb2Radius
            ),
            center = Offset(orb2X, orb2Y),
            radius = orb2Radius
        )

        // 3. Accent Floating Glow
        val orb3X = w * (0.78f + 0.2f * cos(counterOrbitAngle + 1.2f))
        val orb3Y = h * (0.26f + 0.1f * sin(orbitAngle + 0.8f))
        val orb3Radius = (minDim * 0.72f * breathe * dynamicBoost).coerceAtLeast(80f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentGlow.copy(alpha = (0.36f + energy * 0.12f).coerceIn(0f, 0.62f)),
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
