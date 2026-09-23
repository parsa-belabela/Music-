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
 * The Last of Us Firefly Edition Collector Skin:
 * - Weathered acoustic guitar wood texture and post-apocalyptic atmosphere
 * - Floating golden firefly spores & warm embers drifting with music rhythm
 * - Weathered Firefly emblem vinyl record disc
 * - Antique brass & acoustic strings visualizer accents
 */
@Composable
fun LastOfUsNowPlayingStyle(
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

    val fireflyGold = Color(0xFFFFB300)
    val acousticWood = Color(0xFF3E2723)
    val forestMoss = Color(0xFF2E3E26)
    val rustedSteel = Color(0xFF4E342E)

    val infiniteTransition = rememberInfiniteTransition(label = "tlouAnimation")
    val fireflyAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tlouSpin"
    )

    val fireflyGlow by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireflyGlow"
    )

    val currentRotation = if (isPlaying) fireflyAngle else 0f
    val acousticPulse = if (isPlaying) analysisData.kickPulse * 0.06f else 0f

    // Firefly Spore motes
    val fireflies = remember {
        List(22) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 5f + 3f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Post-Apocalyptic Weathered Plinth Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            acousticWood,
                            forestMoss,
                            Color(0xFF0C1008)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            fireflyGold.copy(alpha = 0.8f),
                            rustedSteel,
                            fireflyGold.copy(alpha = 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(16.dp)
        ) {
            // Floating Golden Firefly Spores
            Canvas(modifier = Modifier.fillMaxSize()) {
                fireflies.forEachIndexed { idx, spore ->
                    val x = size.width * spore.first + sin((fireflyAngle + idx * 25) * Math.PI / 180.0).toFloat() * 12.dp.toPx()
                    val rawY = (spore.second + (if (isPlaying) fireflyAngle * 0.0008f * (idx % 3 + 1) else 0f)) % 1.0f
                    val y = size.height * rawY
                    val r = spore.third * fireflyGlow

                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                fireflyGold.copy(alpha = 0.95f),
                                Color(0xFF81C784).copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = Offset(x, y),
                            radius = r * 2.8f
                        ),
                        radius = r * 2.8f,
                        center = Offset(x, y)
                    )
                }
            }

            // Weathered Firefly Acoustic Glass Artwork Display (No Vinyl/Gramophone)
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
                    .scale(1.0f + acousticPulse)
            ) {
                // Bio-luminescent Spore Halo Glow
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(currentRotation)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val r = size.minDimension / 2f

                    // Warm Gold & Moss Green Bio-Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                fireflyGold.copy(alpha = 0.50f),
                                Color(0xFF81C784).copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = r
                        ),
                        radius = r,
                        center = center
                    )

                    // Acoustic Rosette Rings
                    for (i in 1..6) {
                        val ringR = r * (0.45f + (i * 0.085f))
                        drawCircle(
                            color = fireflyGold.copy(alpha = if (i == 3 || i == 6) 0.30f else 0.10f),
                            radius = ringR,
                            center = center,
                            style = Stroke(width = if (i == 3) 1.5.dp.toPx() else 1.dp.toPx())
                        )
                    }
                }

                // Weathered Mahogany & Rusted Steel Artwork Frame
                Box(
                    modifier = Modifier
                        .size(255.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF1A120B))
                        .border(
                            width = 2.5.dp,
                            brush = Brush.linearGradient(
                                listOf(fireflyGold, rustedSteel, Color(0xFF81C784), fireflyGold)
                            ),
                            shape = RoundedCornerShape(32.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (track.artworkUri != null) {
                        AsyncImage(
                            model = track.artworkUri,
                            contentDescription = "Firefly Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.FilterVintage,
                            contentDescription = null,
                            tint = fireflyGold,
                            modifier = Modifier.size(72.dp)
                        )
                    }

                    // Weathered Glass & Sunlight Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.16f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        fireflyGold.copy(alpha = 0.15f)
                                    )
                                )
                            )
                    )
                }
            }

            // Top Firefly Motto Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .liquidGlass(
                        shape = RoundedCornerShape(12.dp),
                        thickness = GlassThickness.THIN,
                        tintColor = fireflyGold,
                        tintAlpha = 0.25f,
                        borderWidth = 1.dp
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = fireflyGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "THE LAST OF US • WHEN YOU'RE LOST IN THE DARK",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
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
                    color = Color(0xFFD7CCC8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Liquid Progress Bar with TLOU Palette
        LiquidGlassProgressBar(
            currentPositionProvider = { playbackState.currentPositionMs },
            durationMs = playbackState.durationMs,
            palette = palette.copy(primary = fireflyGold, accent = Color(0xFF81C784)),
            analysisDataProvider = { analysisData },
            onSeekTo = onSeek,
            waveformEnvelope = waveformEnvelope,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // TLOU Controls
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
                        tintColor = fireflyGold,
                        tintAlpha = 0.22f
                    )
            ) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) fireflyGold else Color.White,
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
                        tintColor = fireflyGold,
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

            // Guitar Pick / Master Brass Play Knob
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(fireflyGold, Color(0xFFB8860B), acousticWood)
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
                        tintColor = fireflyGold,
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
                        tintColor = fireflyGold,
                        tintAlpha = 0.25f
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Firefly",
                    tint = fireflyGold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
