package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.ConnectedAudioDevice

/**
 * Apple-grade AirPods & Bluetooth audio device indicator.
 * Features:
 * - Dynamic Island-inspired Liquid Glass pill
 * - Apple-style micro-specular sheen
 * - Pulsing status indicator LED
 * - Spring entrance and exit physics
 */
@Composable
fun AudioDeviceIndicator(
    device: ConnectedAudioDevice?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    AnimatedVisibility(
        visible = device != null,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.9f, animationSpec = tween(200)),
        modifier = modifier
    ) {
        if (device == null) return@AnimatedVisibility

        val infiniteTransition = rememberInfiniteTransition(label = "airpodsLedPulse")
        val ledAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "airpodsPulse"
        )

        val isAirPods = device.isAirPods

        Box(
            modifier = Modifier
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = if (isAirPods) Color(0x5538BDF8) else Color(0x338B5CF6),
                    spotColor = if (isAirPods) Color(0x660284C7) else Color(0x446366F1)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xEE1E2235),
                            Color(0xF5111320)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0x66FFFFFF),
                            Color(0x18FFFFFF),
                            if (isAirPods) Color(0x4438BDF8) else Color(0x338B5CF6)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(
                    horizontal = if (compact) 9.dp else 13.dp,
                    vertical = if (compact) 4.dp else 6.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
            ) {
                // Device Icon
                if (isAirPods) {
                    AirPodsVectorGlyph(modifier = Modifier.size(if (compact) 14.dp else 17.dp))
                } else {
                    val icon = when {
                        device.isBluetooth -> Icons.Default.Bluetooth
                        device.isHeadphones -> Icons.Default.Headphones
                        else -> Icons.Default.Speaker
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = device.name,
                        tint = if (device.isBluetooth) Color(0xFF38BDF8) else Color.White,
                        modifier = Modifier.size(if (compact) 14.dp else 17.dp)
                    )
                }

                // Device Label
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (compact) 11.sp else 12.sp,
                        letterSpacing = 0.2.sp
                    ),
                    maxLines = 1
                )

                // Emerald active signal LED
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = ledAlpha))
                )
            }
        }
    }
}

/**
 * Custom-drawn crisp vector glyph of Apple AirPods.
 */
@Composable
fun AirPodsVectorGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        val color = Color.White

        // Left pod head & stem
        drawCircle(
            color = color,
            radius = w * 0.22f,
            center = Offset(w * 0.32f, h * 0.30f)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.32f, h * 0.35f),
            end = Offset(w * 0.22f, h * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Right pod head & stem
        drawCircle(
            color = color,
            radius = w * 0.22f,
            center = Offset(w * 0.68f, h * 0.30f)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.68f, h * 0.35f),
            end = Offset(w * 0.78f, h * 0.88f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
