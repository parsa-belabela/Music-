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
import androidx.compose.ui.graphics.Path
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
 * Batman Dark Knight Collector Skin:
 * - Gotham midnight mist & rain motion graphic particles
 * - Rotating Bat-Signal searchlight disc with sharp bat wing emblem
 * - Matte Kevlar stealth carbon textures & tactical yellow accents
 * - Audio-reactive sonar radar pulses
 */
@Composable
fun BatmanNowPlayingStyle(
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

    val batmanYellow = Color(0xFFFFCC00)
    val batmanTitanium = Color(0xFF2C3240)
    val batmanArmorBlack = Color(0xFF07080C)
    val gothamBlue = Color(0xFF0F172A)

    val infiniteTransition = rememberInfiniteTransition(label = "batmanAnimation")
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAngle"
    )

    val searchlightPulse by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "searchlightPulse"
    )

    val currentRotation = if (isPlaying) radarAngle else 0f
    val bassExpand = if (isPlaying) analysisData.kickPulse * 0.07f else 0f

    // Rain / Mist particle streaks
    val rainDrops = remember {
        List(30) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 12f + 8f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Gotham Tactical Cockpit Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            gothamBlue,
                            batmanArmorBlack,
                            Color(0xFF020305)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            batmanYellow.copy(alpha = 0.85f),
                            batmanTitanium,
                            batmanYellow.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(16.dp)
        ) {
            // Gotham Atmospheric Rain & Fog
            Canvas(modifier = Modifier.fillMaxSize()) {
                rainDrops.forEachIndexed { idx, drop ->
                    val x = size.width * drop.first
                    val rawY = (drop.second + (if (isPlaying) radarAngle * 0.003f * (idx % 2 + 1) else 0f)) % 1.0f
                    val y = size.height * rawY
                    val len = drop.third

                    drawLine(
                        color = Color(0x3564B5F6),
                        start = Offset(x, y),
                        end = Offset(x - 2.dp.toPx(), y + len),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }

            // Bat-Signal Sonar Disc
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .align(Alignment.Center)
                    .scale(1.0f + bassExpand)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(currentRotation)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension / 2f

                    // Deep Matte Armor Surface
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                Color(0xFF1B202E),
                                Color(0xFF0A0C13),
                                Color(0xFF030406)
                            ),
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )

                    // Tactical Sonar Concentric Grids
                    for (i in 1..8) {
                        val ringR = r * (0.35f + (i * 0.075f))
                        drawCircle(
                            color = batmanYellow.copy(alpha = if (i == 4 || i == 8) 0.35f else 0.12f),
                            radius = ringR,
                            center = center,
                            style = Stroke(width = if (i == 4) 1.5.dp.toPx() else 1.dp.toPx())
                        )
                    }

                    // Rotating Bat-Signal Searchlight Beam
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                batmanYellow.copy(alpha = 0.38f * searchlightPulse),
                                Color.Transparent
                            ),
                            center = center
                        ),
                        radius = r,
                        center = center
                    )

                    // Carbon Fiber Border
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(batmanYellow, batmanTitanium, batmanYellow),
                            center = center
                        ),
                        radius = r,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Center Bat Emblem & Cover Art
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .align(Alignment.Center)
                        .rotate(currentRotation)
                        .clip(CircleShape)
                        .background(Color(0xFF08090E))
                        .border(3.dp, batmanYellow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.artworkUri != null) {
                        AsyncImage(
                            model = track.artworkUri,
                            contentDescription = "Bat Signal Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Tactical Shield Icon
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = batmanYellow,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    // Center Tactical Core
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(batmanYellow)
                            .border(1.5.dp, Color.Black, CircleShape)
                    )
                }
            }

            // Top Gotham Tactical Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .liquidGlass(
                        shape = RoundedCornerShape(12.dp),
                        thickness = GlassThickness.THIN,
                        tintColor = batmanYellow,
                        tintAlpha = 0.25f,
                        borderWidth = 1.dp
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = batmanYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "THE DARK KNIGHT • GOTHAM EDITION",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Track Title & Artist
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
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
                    color = Color(0xFFB0B7C6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Liquid Progress Bar with Batman Tactical Palette
        LiquidGlassProgressBar(
            currentPositionProvider = { playbackState.currentPositionMs },
            durationMs = playbackState.durationMs,
            palette = palette.copy(primary = batmanYellow, accent = Color(0xFF64B5F6)),
            analysisDataProvider = { analysisData },
            onSeekTo = onSeek,
            waveformEnvelope = waveformEnvelope,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Batman Tactical Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(46.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.REGULAR,
                        tintColor = batmanYellow,
                        tintAlpha = 0.22f
                    )
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) batmanYellow else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(52.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.REGULAR,
                        tintColor = batmanYellow,
                        tintAlpha = 0.22f
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Bat Master Play/Pause Shield Knob
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(batmanYellow, Color(0xFFCCA000), Color(0xFF0F121C))
                        )
                    )
                    .border(2.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(52.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.REGULAR,
                        tintColor = batmanYellow,
                        tintAlpha = 0.22f
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                modifier = Modifier
                    .size(46.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.REGULAR,
                        tintColor = batmanYellow,
                        tintAlpha = 0.25f
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = "Sonar",
                    tint = batmanYellow,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
