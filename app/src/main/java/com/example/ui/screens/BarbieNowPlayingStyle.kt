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
 * Barbie Dream Glow VIP Collector Skin:
 * - Radiant hot pink neon & soft pastel magenta atmosphere
 * - Spinning Barbie Star/Heart Vinyl Disc with iridescent sheen
 * - Floating sparkling glitter particles reactive to audio energy
 * - Chic golden and crystal pink playback controls
 */
@Composable
fun BarbieNowPlayingStyle(
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

    val barbieHotPink = Color(0xFFFF1493)
    val barbieSoftPink = Color(0xFFFF69B4)
    val barbieGold = Color(0xFFFFD700)
    val barbieDeepMagenta = Color(0xFF2A0824)

    val infiniteTransition = rememberInfiniteTransition(label = "barbieAnimation")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "barbieSpin"
    )

    val sparklePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparklePulse"
    )

    val currentRotation = if (isPlaying) spinAngle else 0f
    val kickBoost = if (isPlaying) analysisData.kickPulse * 0.08f else 0f
    val discScale = 1.0f + kickBoost

    // Floating glitter particles
    val particles = remember {
        List(24) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 4f + 2f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Barbie Dream Stage Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            barbieDeepMagenta,
                            Color(0xFF140212),
                            Color(0xFF0A0109)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            barbieHotPink,
                            barbieGold,
                            barbieSoftPink,
                            barbieGold,
                            barbieHotPink
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(16.dp)
        ) {
            // Floating Glitter Stars and Sparkles
            Canvas(modifier = Modifier.fillMaxSize()) {
                particles.forEachIndexed { idx, p ->
                    val x = size.width * p.first
                    val rawY = (p.second + (if (isPlaying) spinAngle * 0.001f * (idx % 3 + 1) else 0f)) % 1.0f
                    val y = size.height * rawY
                    val r = p.third * sparklePulse

                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                barbieGold.copy(alpha = 0.85f),
                                barbieSoftPink.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(x, y),
                            radius = r * 2.5f
                        ),
                        radius = r * 2.5f,
                        center = Offset(x, y)
                    )
                }
            }

            // Barbie Dream Crystal Artwork Display (No Vinyl/Gramophone)
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
                    .scale(discScale)
            ) {
                // Outer Dreamhouse Neon Aura Glow
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(currentRotation)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension / 2f

                    // Radiant Pink Halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                barbieHotPink.copy(alpha = 0.55f),
                                barbieSoftPink.copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )

                    // Rotating Golden Sparkle Rays
                    for (i in 0 until 8) {
                        rotate(degrees = i * 45f, pivot = center) {
                            drawLine(
                                brush = Brush.linearGradient(
                                    listOf(Color.Transparent, barbieGold.copy(alpha = 0.45f), Color.Transparent)
                                ),
                                start = Offset(center.x - r * 0.95f, center.y),
                                end = Offset(center.x + r * 0.95f, center.y),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }

                // Barbie Crystal Glass Artwork Frame
                Box(
                    modifier = Modifier
                        .size(255.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF280020))
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                listOf(barbieGold, barbieHotPink, barbieSoftPink, barbieGold)
                            ),
                            shape = RoundedCornerShape(32.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.artworkUri != null) {
                        AsyncImage(
                            model = track.artworkUri,
                            contentDescription = "Barbie Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = barbieHotPink,
                            modifier = Modifier.size(72.dp)
                        )
                    }

                    // Glossy Specular Glass Sheen Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent,
                                        barbieHotPink.copy(alpha = 0.15f)
                                    )
                                )
                            )
                    )
                }
            }

            // Top Barbie Ribbon Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .liquidGlass(
                        shape = RoundedCornerShape(12.dp),
                        thickness = GlassThickness.THIN,
                        tintColor = barbieHotPink,
                        tintAlpha = 0.30f,
                        borderWidth = 1.dp
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = barbieGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BARBIE DREAM GLOW • VIP EDITION",
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
                    color = barbieSoftPink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Liquid Progress Bar with Barbie Palette
        LiquidGlassProgressBar(
            currentPositionProvider = { playbackState.currentPositionMs },
            durationMs = playbackState.durationMs,
            palette = palette.copy(primary = barbieHotPink, accent = barbieGold),
            analysisDataProvider = { analysisData },
            onSeekTo = onSeek,
            waveformEnvelope = waveformEnvelope,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Barbie Controls
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
                        tintColor = barbieHotPink,
                        tintAlpha = 0.25f
                    )
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) barbieHotPink else Color.White,
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
                        tintColor = barbieHotPink,
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

            // Barbie Play/Pause Heart Knob
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(barbieHotPink, barbieGold, barbieSoftPink)
                        )
                    )
                    .border(2.5.dp, Color.White, CircleShape)
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
                        tintColor = barbieHotPink,
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
                        tintColor = barbieGold,
                        tintAlpha = 0.25f
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Sparkle",
                    tint = barbieGold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
