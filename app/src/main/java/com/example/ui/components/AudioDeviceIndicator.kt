package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.ConnectedAudioDevice

/**
 * Dynamic Island-grade smart Audio Device Indicator.
 * - Rules: Strictly visible ONLY when an external/Bluetooth device is connected.
 * - Completely collapses and hides when sound plays from the internal phone speaker.
 * - Features dynamic live Canvas vector animations:
 *   * Radiating acoustic soundwave rings
 *   * Specular neon aura matching album artwork color
 *   * Pulsing emerald connection status LED
 *   * Organic spring entrance/exit physics
 */
@Composable
fun AudioDeviceIndicator(
    device: ConnectedAudioDevice? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    accentColor: Color = Color(0xFF00E5FF)
) {
    val isConnected = device != null

    AnimatedVisibility(
        visible = isConnected,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(
                    initialScale = 0.78f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) +
                expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
        exit = fadeOut(tween(180)) +
                scaleOut(targetScale = 0.82f, animationSpec = tween(180)) +
                shrinkHorizontally(animationSpec = tween(180)),
        modifier = modifier
    ) {
        val currentDevice = device ?: return@AnimatedVisibility

        val infiniteTransition = rememberInfiniteTransition(label = "AudioDeviceAnimation")

        // Pulsing connection status light
        val ledAlpha by infiniteTransition.animateFloat(
            initialValue = 0.40f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "LedPulse"
        )

        // Radiating wave progress for acoustic rings (0f -> 1f)
        val waveProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "WaveProgress"
        )

        val isAirPods = currentDevice.isAirPods

        Box(
            modifier = Modifier
                .shadow(
                    elevation = if (compact) 6.dp else 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = accentColor.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.55f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF0181B2B),
                            Color(0xF80B0D18)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.75f),
                            accentColor.copy(alpha = 0.65f),
                            Color(0xFF38BDF8).copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.20f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(300f, 100f)
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(
                    horizontal = if (compact) 9.dp else 13.dp,
                    vertical = if (compact) 4.5.dp else 6.5.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)
            ) {
                // Animated Vector Icon: AirPods or Speaker with radiating soundwave rings
                if (isAirPods) {
                    AirPodsMotionGlyph(
                        waveProgress = waveProgress,
                        accentColor = accentColor,
                        modifier = Modifier.size(if (compact) 18.dp else 22.dp)
                    )
                } else {
                    SpeakerMotionGlyph(
                        isHeadphones = currentDevice.isHeadphones,
                        waveProgress = waveProgress,
                        accentColor = accentColor,
                        modifier = Modifier.size(if (compact) 18.dp else 22.dp)
                    )
                }

                // Device Label
                Text(
                    text = currentDevice.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 10.5.sp else 12.sp,
                        letterSpacing = 0.3.sp
                    ),
                    maxLines = 1
                )

                // Emerald active signal LED with luminous halo
                Box(contentAlignment = Alignment.Center) {
                    // Pulsing glow halo
                    Box(
                        modifier = Modifier
                            .size(if (compact) 9.dp else 11.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = ledAlpha * 0.35f))
                    )
                    // Core LED
                    Box(
                        modifier = Modifier
                            .size(if (compact) 5.dp else 6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34D399))
                    )
                }
            }
        }
    }
}

/**
 * High-precision Canvas vector of Apple AirPods with radiating animated soundwave rings.
 */
@Composable
fun AirPodsMotionGlyph(
    waveProgress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        val white = Color.White

        // Radiating soundwave rings behind the heads
        val maxRadius = w * 0.42f
        val ring1Radius = maxRadius * waveProgress
        val ring1Alpha = ((1f - waveProgress) * 0.70f).coerceIn(0f, 0.70f)

        val waveProgress2 = (waveProgress + 0.5f) % 1f
        val ring2Radius = maxRadius * waveProgress2
        val ring2Alpha = ((1f - waveProgress2) * 0.70f).coerceIn(0f, 0.70f)

        // Draw acoustic rings
        drawCircle(
            color = accentColor.copy(alpha = ring1Alpha),
            radius = ring1Radius,
            center = Offset(w * 0.50f, h * 0.32f),
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = accentColor.copy(alpha = ring2Alpha),
            radius = ring2Radius,
            center = Offset(w * 0.50f, h * 0.32f),
            style = Stroke(width = 1.dp.toPx())
        )

        // Left Pod
        val leftHeadCenter = Offset(w * 0.34f, h * 0.32f)
        drawCircle(
            color = white,
            radius = w * 0.18f,
            center = leftHeadCenter
        )
        // Acoustic port dot
        drawCircle(
            color = Color(0xFF1F2937),
            radius = w * 0.05f,
            center = Offset(w * 0.30f, h * 0.30f)
        )
        // Left Stem
        drawLine(
            color = white,
            start = Offset(w * 0.34f, h * 0.40f),
            end = Offset(w * 0.24f, h * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Metallic chrome tip
        drawCircle(
            color = Color(0xFF94A3B8),
            radius = stroke * 0.6f,
            center = Offset(w * 0.24f, h * 0.88f)
        )

        // Right Pod
        val rightHeadCenter = Offset(w * 0.66f, h * 0.32f)
        drawCircle(
            color = white,
            radius = w * 0.18f,
            center = rightHeadCenter
        )
        // Acoustic port dot
        drawCircle(
            color = Color(0xFF1F2937),
            radius = w * 0.05f,
            center = Offset(w * 0.70f, h * 0.30f)
        )
        // Right Stem
        drawLine(
            color = white,
            start = Offset(w * 0.66f, h * 0.40f),
            end = Offset(w * 0.76f, h * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // Metallic chrome tip
        drawCircle(
            color = Color(0xFF94A3B8),
            radius = stroke * 0.6f,
            center = Offset(w * 0.76f, h * 0.88f)
        )
    }
}

/**
 * Modern vector of Bluetooth Speaker / Headphones with animated acoustic wave arcs.
 */
@Composable
fun SpeakerMotionGlyph(
    isHeadphones: Boolean,
    waveProgress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val white = Color.White
        val stroke = 1.5.dp.toPx()

        if (isHeadphones) {
            // Headband arc
            val bandPath = Path().apply {
                moveTo(w * 0.22f, h * 0.58f)
                cubicTo(
                    w * 0.22f, h * 0.14f,
                    w * 0.78f, h * 0.14f,
                    w * 0.78f, h * 0.58f
                )
            }
            drawPath(path = bandPath, color = white, style = Stroke(width = stroke, cap = StrokeCap.Round))

            // Left Ear Cup
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(w * 0.12f, h * 0.50f),
                size = Size(w * 0.18f, h * 0.36f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            // Right Ear Cup
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(w * 0.70f, h * 0.50f),
                size = Size(w * 0.18f, h * 0.36f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Animated acoustic waves outside ear cups
            val arcAlpha1 = ((1f - waveProgress) * 0.80f).coerceIn(0f, 0.80f)
            val offsetRadius1 = w * 0.12f * waveProgress
            drawLine(
                color = white.copy(alpha = arcAlpha1),
                start = Offset(w * 0.08f - offsetRadius1, h * 0.56f),
                end = Offset(w * 0.08f - offsetRadius1, h * 0.80f),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = white.copy(alpha = arcAlpha1),
                start = Offset(w * 0.92f + offsetRadius1, h * 0.56f),
                end = Offset(w * 0.92f + offsetRadius1, h * 0.80f),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        } else {
            // Bluetooth Speaker Cabinet
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(w * 0.22f, h * 0.18f),
                size = Size(w * 0.56f, h * 0.64f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = white,
                topLeft = Offset(w * 0.22f, h * 0.18f),
                size = Size(w * 0.56f, h * 0.64f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = stroke)
            )

            // Tweeter & Woofer cones
            drawCircle(
                color = accentColor,
                radius = w * 0.09f,
                center = Offset(w * 0.50f, h * 0.36f)
            )
            drawCircle(
                color = accentColor,
                radius = w * 0.16f,
                center = Offset(w * 0.50f, h * 0.62f)
            )

            // Radiating wave arcs
            val arcAlpha = ((1f - waveProgress) * 0.85f).coerceIn(0f, 0.85f)
            val spread = w * 0.14f * waveProgress

            // Right side waves
            drawArc(
                color = accentColor.copy(alpha = arcAlpha),
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(w * 0.50f, h * 0.40f - spread),
                size = Size(w * 0.35f + spread * 2, h * 0.44f + spread * 2),
                style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
