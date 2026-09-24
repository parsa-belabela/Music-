package com.example.ui.components

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.Track
import kotlin.math.min

/**
 * Cinematic Living Atmosphere Background:
 * - Silky smooth interruptible crossfading for album art on track transitions
 * - Dynamic audio-reactive ambient aura field (bass, kick, energy driven)
 * - Animated color morphing between album palettes
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
    // Smooth color morphing between track palettes
    val animatedPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "bgPrimaryMorph"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "bgSecondaryMorph"
    )
    val animatedAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "bgAccentMorph"
    )
    val animatedDeep by animateColorAsState(
        targetValue = palette.deepAtmosphere,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "bgDeepMorph"
    )

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

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF080914))) {
        // Layer 1: Hardware-scaled, soft ambient album art with vivid blending
        val context = LocalContext.current
        AnimatedContent(
            targetState = track?.artworkUri,
            transitionSpec = {
                fadeIn(animationSpec = tween(650, easing = FastOutSlowInEasing))
                    .togetherWith(fadeOut(animationSpec = tween(450, easing = FastOutSlowInEasing)))
            },
            label = "atmosphereArtworkFade",
            modifier = Modifier.fillMaxSize()
        ) { uri ->
            if (uri != null) {
                coil.compose.AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .size(320, 320)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(16.dp),
                    alpha = 0.38f
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    animatedPrimary.copy(alpha = 0.45f),
                                    animatedSecondary.copy(alpha = 0.30f),
                                    animatedDeep.copy(alpha = 0.65f),
                                    Color(0xFF080914)
                                )
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        animatedPrimary.copy(alpha = 0.40f),
                                        animatedAccent.copy(alpha = 0.20f),
                                        Color.Transparent
                                    ),
                                    radius = 800f
                                )
                            )
                    )
                }
            }
        }

        // Layer 2: Radiant Atmospheric Depth Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedDeep.copy(alpha = 0.32f),
                            Color(0x20000000),
                            animatedPrimary.copy(alpha = 0.18f),
                            Color(0x90080914)
                        )
                    )
                )
        )

        // Layer 3: Organic Energy Field & Audio-reactive Living Aura (Apple iOS Liquid Glass style)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val data = analysisDataProvider()
            val center = Offset(size.width * 0.5f, size.height * 0.40f)
            val maxDimension = min(size.width, size.height)

            val bass = data.haloExpansion
            val kick = data.kickPulse
            val energy = data.totalEnergy

            // Dynamic radius driven by audio energy and smooth rotation
            val baseRadius = maxDimension * (0.65f + bass * 0.25f)
            val radAngle = Math.toRadians(waveOffset.toDouble())
            val driftX = (Math.cos(radAngle) * 50.0).toFloat()
            val driftY = (Math.sin(radAngle) * 40.0).toFloat()
            val auraCenter = Offset(center.x + driftX, center.y + driftY)
            val secondaryCenter = Offset(center.x - driftX * 0.85f, center.y + driftY * 0.9f)

            // Primary wide luminous aura wash (Silky, translucent, non-harsh)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = ((0.36f + bass * 0.18f + kick * 0.12f) * glowStrength).coerceIn(0f, 0.65f)),
                        animatedSecondary.copy(alpha = ((0.24f + energy * 0.14f) * glowStrength).coerceIn(0f, 0.50f)),
                        Color.Transparent
                    ),
                    center = auraCenter,
                    radius = baseRadius * 1.60f
                ),
                radius = baseRadius * 1.60f,
                center = auraCenter
            )

            // Secondary vibrant accent bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedAccent.copy(alpha = ((0.32f + kick * 0.20f) * glowStrength).coerceIn(0f, 0.58f)),
                        animatedSecondary.copy(alpha = ((0.20f + bass * 0.14f) * glowStrength).coerceIn(0f, 0.45f)),
                        Color.Transparent
                    ),
                    center = secondaryCenter,
                    radius = baseRadius * 1.25f
                ),
                radius = baseRadius * 1.25f,
                center = secondaryCenter
            )

            // Dynamic central core glow directly behind artwork
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = ((0.40f + bass * 0.20f) * glowStrength).coerceIn(0f, 0.65f)),
                        animatedAccent.copy(alpha = (0.24f * glowStrength).coerceIn(0f, 0.45f)),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.85f
                ),
                radius = baseRadius * 0.85f,
                center = center
            )

            // Transient kick pulse burst
            if (kick > 0.05f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedAccent.copy(alpha = (kick * 0.35f * glowStrength).coerceIn(0f, 0.48f)),
                            animatedSecondary.copy(alpha = (kick * 0.20f * glowStrength).coerceIn(0f, 0.30f)),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 0.80f
                    ),
                    radius = baseRadius * 0.80f,
                    center = center
                )
            }
        }

        // Layer 4: Physical iOS Liquid Glass frosted reflection sheen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.07f),
                            Color.Transparent,
                            Color(0x0600E5FF),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )

        // Layer 5: Minimal subtle edge contrast veil for navigation clarity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x35000000),
                        0.25f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1.0f to Color(0x7506070E)
                    )
                )
        )
    }
}
