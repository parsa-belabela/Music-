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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.PlaybackState
import com.example.ui.components.LiquidGlassProgressBar
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Neon Electric Blue Cyber Turntable VIP Collector Skin (Image 3 Top-Left):
 * - Weather widget header: 18° Cloudy Tehran • Rain in 2h
 * - 3D Realistic Neon Turntable with Tone Arm and blue/purple glowing light trails
 * - Electric Blue & Neon Cyan cyber aesthetic
 * - Floating glowing mini-player bar
 */
@Composable
fun ElectricTurntableNowPlayingStyle(
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

    val cyberBlue = Color(0xFF00E5FF)
    val neonIndigo = Color(0xFF7C4DFF)
    val cyberDark = Color(0xFF070B19)
    val cyberMetal = Color(0xFF161E36)

    val infiniteTransition = rememberInfiniteTransition(label = "electricVinylAnimation")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylSpin"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 26f else 4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "tonearmAngle"
    )

    val currentRotation = if (isPlaying) spinAngle else 0f
    val kickBoost = if (isPlaying) analysisData.kickPulse * 0.08f else 0f
    val discScale = 1.0f + kickBoost

    val particles = remember {
        List(20) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 4f + 2f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Top Weather Widget Bar (Matching Image 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33101830))
                    .border(1.dp, cyberBlue.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "☁️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "18° Cloudy Tehran",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33101830))
                    .border(1.dp, cyberBlue.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💧", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rain in 2h",
                        color = cyberBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. 3D Neon Vinyl Turntable Platter
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        listOf(cyberMetal, cyberDark, Color(0xFF03050C))
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(listOf(cyberBlue, neonIndigo, cyberBlue)),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(14.dp)
        ) {
            // Ambient Floating Cyber Sparks
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEach { p ->
                    val x = size.width * p.first
                    val y = size.height * p.second
                    drawCircle(
                        color = cyberBlue.copy(alpha = 0.45f * glowPulse),
                        radius = p.third * glowPulse,
                        center = Offset(x, y)
                    )
                }
            }

            // Spinning Vinyl Platter
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
                    .scale(discScale)
            ) {
                // Vinyl Disc Canvas (Groove Rings & Neon Sheen)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(currentRotation)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension / 2f

                    // Platter outer rim
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF1E2638), Color(0xFF090D18)),
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )

                    // Neon Outer Ring
                    drawCircle(
                        color = cyberBlue.copy(alpha = 0.75f * glowPulse),
                        radius = r - 2.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Grooves
                    for (i in 1..10) {
                        drawCircle(
                            color = Color(0xFF28354D).copy(alpha = 0.45f),
                            radius = r * (0.35f + (i * 0.055f)),
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Iridescent Light Sheen
                    rotate(degrees = 45f, pivot = center) {
                        drawLine(
                            brush = Brush.linearGradient(
                                listOf(Color.Transparent, cyberBlue.copy(alpha = 0.4f), Color.Transparent)
                            ),
                            start = Offset(center.x - r * 0.85f, center.y),
                            end = Offset(center.x + r * 0.85f, center.y),
                            strokeWidth = 14.dp.toPx()
                        )
                    }
                }

                // Center Album Artwork Label
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color(0xFF0B1020))
                        .border(3.dp, cyberBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.artworkUri != null) {
                        AsyncImage(
                            model = track.artworkUri,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = cyberBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Center spindle hole
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF050810))
                            .border(2.dp, Color(0xFFC0C5D8), CircleShape)
                    )
                }

                // Cyber Tone Arm
                Canvas(
                    modifier = Modifier
                        .size(100.dp, 160.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = (-8).dp)
                        .rotate(tonearmAngle)
                ) {
                    val base = Offset(size.width - 20.dp.toPx(), 20.dp.toPx())
                    val tip = Offset(20.dp.toPx(), size.height - 20.dp.toPx())

                    // Base pivot
                    drawCircle(color = Color(0xFF6B7280), radius = 14.dp.toPx(), center = base)
                    drawCircle(color = cyberBlue, radius = 8.dp.toPx(), center = base)

                    // Arm bar
                    drawLine(
                        color = Color(0xFFE5E7EB),
                        start = base,
                        end = tip,
                        strokeWidth = 4.dp.toPx()
                    )

                    // Stylus Head cartridge
                    drawCircle(color = cyberBlue, radius = 6.dp.toPx(), center = tip)
                }
            }
        }

        // 3. Track Info & Favorite (Matching Image 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = cyberBlue,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleFavorite()
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(cyberMetal)
                    .border(1.dp, cyberBlue.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) Color.Red else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 4. Glowing Blue Progress Bar
        LiquidGlassProgressBar(
            currentPositionProvider = { playbackState.currentPositionMs },
            durationMs = playbackState.durationMs,
            palette = palette.copy(primary = cyberBlue, accent = neonIndigo),
            analysisDataProvider = { analysisData },
            onSeekTo = onSeek,
            waveformEnvelope = waveformEnvelope,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 5. Glowing Playback Controls (Matching Image 3)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xEE090E1E))
                .border(1.dp, cyberBlue.copy(alpha = 0.35f), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Shuffle */ }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffle) cyberBlue else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPrevious()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Big Glowing Cyber Play Button
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(cyberBlue, neonIndigo))
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNext()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { /* Repeat */ }) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
