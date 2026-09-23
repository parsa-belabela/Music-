package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.PlaybackState
import com.example.data.model.Track
import com.example.ui.components.LiquidGlassProgressBar
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class TurntablePlinth(val id: String, val titleEn: String, val titleFa: String, val baseColor: Color, val accentColor: Color) {
    WALNUT_WOOD("walnut", "Walnut Masterwood", "چوب گردوی سلطنتی", Color(0xFF2C1810), Color(0xFFC68B59)),
    OBSIDIAN_CARBON("obsidian", "Obsidian Carbon", "ابسیدین مشکی براق", Color(0xFF0D0E14), Color(0xFF00E5FF)),
    VINTAGE_GOLD("gold", "Vintage 24K Brass", "برنجی و طلای کلاسیک", Color(0xFF1E190E), Color(0xFFFFD700))
}

@Composable
fun VinylNowPlayingStyle(
    playbackState: PlaybackState,
    palette: AmbientPalette,
    analysisData: AudioAnalysisData,
    waveformEnvelope: FloatArray? = null,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    val isPlaying = playbackState.isPlaying
    val haptic = LocalHapticFeedback.current

    var selectedPlinth by remember { mutableStateOf(TurntablePlinth.WALNUT_WOOD) }
    var vinylWarmthEnabled by remember { mutableStateOf(true) }

    // Infinite rotation for spinning vinyl
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylAngle"
    )

    val currentRotation = if (isPlaying) spinAngle else 0f

    // Animated Tonearm / Needle Angle:
    // Rest position = -26f, On vinyl groove = 18f -> 32f based on track progress
    val trackProgress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val targetArmAngle = if (isPlaying) {
        18f + (trackProgress * 14f)
    } else {
        -26f
    }

    val tonearmAngle by animateFloatAsState(
        targetValue = targetArmAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tonearmAngle"
    )

    // Trigger haptic when needle hits the vinyl record
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            kotlinx.coroutines.delay(260)
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
        }
    }

    // Reactive bass pulse on vinyl outer rim
    val bassScale = 1f + (analysisData.bass * 0.035f).coerceIn(0f, 0.05f)

    // Ensure entire Now Playing layout for controls, seekbar, and turntable is LTR
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Plinth & Vinyl Warmth Quick Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Plinth Selector Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TurntablePlinth.values().forEach { plinth ->
                        val isSelected = selectedPlinth == plinth
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) plinth.accentColor.copy(alpha = 0.28f) else Color(0x16FFFFFF))
                                .border(1.dp, if (isSelected) plinth.accentColor else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedPlinth = plinth
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = plinth.titleEn.substringBefore(" "),
                            color = if (isSelected) plinth.accentColor else Color(0xFFC0C0D0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Vinyl Warmth / Needle Surface Noise Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (vinylWarmthEnabled) palette.accent.copy(alpha = 0.25f) else Color(0x14FFFFFF))
                    .border(1.dp, if (vinylWarmthEnabled) palette.accent else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vinylWarmthEnabled = !vinylWarmthEnabled
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (vinylWarmthEnabled) palette.accent else Color(0xFF888898),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (vinylWarmthEnabled) "Warmth ON" else "Warmth OFF",
                        color = if (vinylWarmthEnabled) palette.accent else Color(0xFF888898),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Turntable Plinth Platform Deck
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            selectedPlinth.baseColor,
                            selectedPlinth.baseColor.copy(alpha = 0.85f),
                            Color(0xFF07080D)
                        )
                    )
                )
                .border(1.5.dp, selectedPlinth.accentColor.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl Record with Grooves and Realistic Platter
            Box(
                modifier = Modifier
                    .size(265.dp)
                    .scale(bassScale),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl Record Grooves & Lighting Sheen Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f

                    // Heavy Metallic Platter Edge
                    drawCircle(
                        color = Color(0xFF1E212B),
                        radius = radius,
                        center = center
                    )

                    // Deep Obsidian Vinyl Disc Base
                    drawCircle(
                        color = Color(0xFF090A0E),
                        radius = radius - 4f,
                        center = center
                    )

                    // Micro Concentric Audio Grooves
                    for (r in 38 until (radius - 12).toInt() step 5) {
                        drawCircle(
                            color = Color(0x18FFFFFF),
                            radius = r.toFloat(),
                            center = center,
                            style = Stroke(width = 0.85f)
                        )
                    }

                    // Rotating Specular Light Sheen (Two opposing light reflection cones)
                    rotate(currentRotation, pivot = center) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                0.0f to Color.Transparent,
                                0.22f to Color(0x35FFFFFF),
                                0.28f to Color.Transparent,
                                0.72f to Color(0x35FFFFFF),
                                0.78f to Color.Transparent,
                                1.0f to Color.Transparent
                            ),
                            radius = radius - 8f,
                            center = center
                        )
                    }

                    // Outer Run-out Groove Rim
                    drawCircle(
                        color = Color(0x40FFFFFF),
                        radius = radius - 6f,
                        center = center,
                        style = Stroke(width = 1.8f)
                    )
                }

                // Center Label / Album Artwork
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .rotate(currentRotation)
                        .background(palette.primary)
                        .border(2.dp, selectedPlinth.accentColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.artworkUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = track.artworkUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = track.title.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }

                    // Center Brass Spindle Hole
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(selectedPlinth.accentColor)
                            .border(1.dp, Color.Black, CircleShape)
                    )
                }

                // Drifting Dust / Ambient Light Particles when playing
                if (isPlaying) {
                    VinylDustParticles(spinAngle = spinAngle)
                }
            }

            // Realistic Tonearm with Pivot, Chrome Arm, Counterweight & Cartridge Needle
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                val pivotX = size.width - 24.dp.toPx()
                val pivotY = 32.dp.toPx()
                val armLength = 175.dp.toPx()

                // Tonearm Gimbal Pivot Base (Brushed Metallic Gimbal)
                drawCircle(
                    color = Color(0xFF333846),
                    radius = 16.dp.toPx(),
                    center = Offset(pivotX, pivotY)
                )
                drawCircle(
                    color = selectedPlinth.accentColor,
                    radius = 8.dp.toPx(),
                    center = Offset(pivotX, pivotY)
                )

                // Rotate Tonearm from Pivot
                rotate(degrees = tonearmAngle, pivot = Offset(pivotX, pivotY)) {
                    val start = Offset(pivotX, pivotY)
                    val end = Offset(pivotX - armLength * 0.85f, pivotY + armLength)

                    // Counterweight behind pivot
                    val counterOffset = Offset(pivotX + 14.dp.toPx(), pivotY - 14.dp.toPx())
                    drawCircle(
                        color = Color(0xFF686F80),
                        radius = 9.dp.toPx(),
                        center = counterOffset
                    )

                    // Chrome Silver Tonearm Wand
                    drawLine(
                        color = Color(0xFFD4D8E2),
                        start = start,
                        end = end,
                        strokeWidth = 3.8f,
                        cap = StrokeCap.Round
                    )

                    // Headshell / Cartridge & Needle Point
                    val headshellEnd = Offset(end.x - 14.dp.toPx(), end.y + 12.dp.toPx())
                    drawLine(
                        color = selectedPlinth.accentColor,
                        start = end,
                        end = headshellEnd,
                        strokeWidth = 6.5f,
                        cap = StrokeCap.Square
                    )

                    // Glowing Needle Tip
                    drawCircle(
                        color = if (isPlaying) Color(0xFFFF3366) else Color(0xFF9094A0),
                        radius = 2.5.dp.toPx(),
                        center = headshellEnd
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title and Artist Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${track.artist} • ${track.album}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFA0A5BA)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Waveform / Scrubber
        LiquidGlassProgressBar(
            currentPositionProvider = { playbackState.currentPositionMs },
            durationMs = playbackState.durationMs,
            palette = palette,
            analysisDataProvider = { analysisData },
            onSeekTo = onSeek,
            waveformEnvelope = waveformEnvelope,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Playback Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) palette.accent else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.35f,
                        borderWidth = 1.5.dp
                    )
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = { /* Additional vinyl audio filters */ }) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Tune",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
}

@Composable
private fun VinylDustParticles(spinAngle: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val particleCount = 8
        val rand = Random(42)

        for (i in 0 until particleCount) {
            val dist = 40.dp.toPx() + (i * 12.dp.toPx())
            val angleRad = Math.toRadians((spinAngle * (0.6f + (i * 0.1f)) + (i * 45f)).toDouble())
            val x = center.x + (dist * cos(angleRad)).toFloat()
            val y = center.y + (dist * sin(angleRad)).toFloat()

            drawCircle(
                color = Color(0x35FFFFFF),
                radius = 1.2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
