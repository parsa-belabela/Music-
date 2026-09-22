package com.example.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.AppTheme
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.LocalAppTheme
import com.example.ui.theme.liquidGlass
import kotlin.math.roundToInt

/**
 * Premium Liquid Glass In-App Volume Control for Now Playing Screen.
 * - Directly synchronizes with real device hardware [AudioManager.STREAM_MUSIC]
 * - Instant, jitter-free continuous dragging with zero audio glitches
 * - Multi-layer frosted liquid glass capsule with dynamic specular reflection & neon glow
 * - Dynamic tactile mute toggle and max volume quick-set
 * - Real-time hardware volume button listener for two-way synchronization
 */
@Composable
fun LiquidGlassVolumeControl(
    palette: AmbientPalette,
    modifier: Modifier = Modifier,
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() },
    hapticFeedbackEnabled: Boolean = true,
    onVolumeFractionChange: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currentTheme = LocalAppTheme.current

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    // State for device volume stream
    var deviceVolumeInt by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(deviceVolumeInt.toFloat() / maxVolume.toFloat()) }
    var lastNonZeroVolumeFraction by remember { mutableFloatStateOf(0.5f) }
    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    val triggerHaptic = {
        if (hapticFeedbackEnabled) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

    // Bidirectional sync with system volume changes (hardware volume buttons, bluetooth headsets)
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val streamType = intent?.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (streamType == AudioManager.STREAM_MUSIC || streamType == -1) {
                    val currentSysVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    deviceVolumeInt = currentSysVol
                    if (!isDragging) {
                        dragFraction = (currentSysVol.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)
                        if (currentSysVol > 0) {
                            lastNonZeroVolumeFraction = dragFraction
                        }
                    }
                }
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(receiver, filter)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Determine current displayed fraction
    val displayedFraction = if (isDragging) dragFraction else (deviceVolumeInt.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)

    // Dynamic animations for thumb and track height
    val trackHeightDp by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "volumeTrackHeight"
    )

    val thumbRadiusDp by animateDpAsState(
        targetValue = if (isDragging) 9.5.dp else 6.5.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "volumeThumbRadius"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.85f else 0.45f,
        animationSpec = tween(150),
        label = "volumeGlowAlpha"
    )

    // Mute toggle action
    val toggleMute: () -> Unit = {
        triggerHaptic()
        if (displayedFraction > 0.01f) {
            lastNonZeroVolumeFraction = displayedFraction
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
            deviceVolumeInt = 0
            dragFraction = 0f
            onVolumeFractionChange?.invoke(0f)
        } else {
            val restoreFraction = if (lastNonZeroVolumeFraction > 0.05f) lastNonZeroVolumeFraction else 0.5f
            val restoreInt = (restoreFraction * maxVolume).roundToInt().coerceIn(1, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restoreInt, 0)
            deviceVolumeInt = restoreInt
            dragFraction = restoreFraction
            onVolumeFractionChange?.invoke(restoreFraction)
        }
    }

    // Set Max volume action
    val setMaxVolume: () -> Unit = {
        triggerHaptic()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        deviceVolumeInt = maxVolume
        dragFraction = 1f
        lastNonZeroVolumeFraction = 1f
        onVolumeFractionChange?.invoke(1f)
    }

    // Capsule container with Liquid Glass styling
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(24.dp),
                thickness = GlassThickness.THIN,
                tintColor = palette.primary,
                tintAlpha = 0.08f,
                borderWidth = 1.dp,
                appTheme = currentTheme
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("liquid_glass_volume_control"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left Icon: Mute / Low volume button
            IconButton(
                onClick = toggleMute,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (displayedFraction <= 0.01f) Color(0x33F43F5E) else Color(0x12FFFFFF))
                    .testTag("volume_mute_button")
            ) {
                val icon = when {
                    displayedFraction <= 0.01f -> Icons.Default.VolumeOff
                    displayedFraction < 0.45f -> Icons.Default.VolumeMute
                    else -> Icons.Default.VolumeDown
                }
                val iconTint = when {
                    displayedFraction <= 0.01f -> Color(0xFFF43F5E)
                    displayedFraction < 0.45f -> Color(0xFFE0E0F0)
                    else -> palette.accent
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Volume Down / Mute",
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Center: Interactive Liquid Glass Volume Scrubber
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .onSizeChanged { size ->
                        trackWidthPx = size.width.toFloat().coerceAtLeast(1f)
                    }
                    .pointerInput(maxVolume) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isDragging = true
                            triggerHaptic()

                            val initialFraction = (down.position.x / trackWidthPx).coerceIn(0f, 1f)
                            dragFraction = initialFraction
                            val targetVolInt = (initialFraction * maxVolume).roundToInt().coerceIn(0, maxVolume)
                            if (targetVolInt != deviceVolumeInt) {
                                deviceVolumeInt = targetVolInt
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolInt, 0)
                            }
                            onVolumeFractionChange?.invoke(initialFraction)

                            var pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) break

                                val currentX = change.position.x
                                val fraction = (currentX / trackWidthPx).coerceIn(0f, 1f)
                                dragFraction = fraction

                                val newVolInt = (fraction * maxVolume).roundToInt().coerceIn(0, maxVolume)
                                if (newVolInt != deviceVolumeInt) {
                                    deviceVolumeInt = newVolInt
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolInt, 0)
                                    triggerHaptic()
                                }
                                onVolumeFractionChange?.invoke(fraction)
                                change.consume()
                            }

                            isDragging = false
                            val finalVolInt = (dragFraction * maxVolume).roundToInt().coerceIn(0, maxVolume)
                            deviceVolumeInt = finalVolInt
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, finalVolInt, 0)
                            if (finalVolInt > 0) {
                                lastNonZeroVolumeFraction = dragFraction
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val primaryColor = palette.primary
                val accentColor = palette.accent
                val haloColor = palette.haloGlow

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeightDp)
                ) {
                    val width = size.width
                    val height = size.height
                    val cornerRadius = CornerRadius(height / 2f, height / 2f)

                    // 1. Frosted Inactive Background Groove
                    drawRoundRect(
                        color = Color(0x28FFFFFF),
                        size = Size(width, height),
                        cornerRadius = cornerRadius
                    )

                    // Top specular groove edge
                    drawLine(
                        color = Color(0x40FFFFFF),
                        start = Offset(height / 2f, 0.5f),
                        end = Offset(width - height / 2f, 0.5f),
                        strokeWidth = 0.8f,
                        cap = StrokeCap.Round
                    )

                    // 2. Active Volume Filled Track
                    val activeWidth = (width * displayedFraction).coerceIn(0f, width)
                    if (activeWidth > 0f) {
                        // Ambient Neon Bloom Layer behind active progress
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = glowAlpha * 0.7f),
                                    accentColor.copy(alpha = glowAlpha * 0.9f)
                                ),
                                startX = 0f,
                                endX = activeWidth
                            ),
                            size = Size(activeWidth, height),
                            cornerRadius = cornerRadius
                        )

                        // Vivid Solid Gradient Core
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.95f),
                                    accentColor
                                ),
                                startX = 0f,
                                endX = activeWidth
                            ),
                            size = Size(activeWidth, height),
                            cornerRadius = cornerRadius
                        )

                        // Specular Highlight line along top of filled progress
                        drawLine(
                            color = Color.White.copy(alpha = 0.65f),
                            start = Offset(height / 2f, 0.8f),
                            end = Offset(activeWidth - 1f, 0.8f),
                            strokeWidth = 1f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 3. Glowing Prismatic Thumb
                    val thumbX = activeWidth.coerceIn(0f, width)
                    val thumbRadiusPx = thumbRadiusDp.toPx()

                    // Ambient thumb glow aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.75f),
                                haloColor.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(thumbX, height / 2f),
                            radius = thumbRadiusPx * 2.4f
                        ),
                        radius = thumbRadiusPx * 2.4f,
                        center = Offset(thumbX, height / 2f)
                    )

                    // Outer metallic/glass rim
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadiusPx,
                        center = Offset(thumbX, height / 2f)
                    )

                    // Inner radiant accent core
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White,
                                accentColor
                            )
                        ),
                        radius = thumbRadiusPx * 0.75f,
                        center = Offset(thumbX, height / 2f)
                    )

                    // Specular micro-reflection dot
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = thumbRadiusPx * 0.25f,
                        center = Offset(thumbX - thumbRadiusPx * 0.2f, height / 2f - thumbRadiusPx * 0.2f)
                    )
                }
            }

            // Right Icon: Volume High / Max button
            IconButton(
                onClick = setMaxVolume,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (displayedFraction >= 0.98f) palette.accent.copy(alpha = 0.25f) else Color(0x12FFFFFF))
                    .testTag("volume_max_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Max Volume",
                    tint = if (displayedFraction >= 0.98f) palette.accent else Color(0xFFD5D5E8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Floating Volume Percentage Tooltip while dragging
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-32).dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xE60D0D1E))
                    .border(1.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${(displayedFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
