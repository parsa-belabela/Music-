package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    // Infinite rotation for spinning vinyl
    val infiniteTransition = rememberInfiniteTransition(label = "vinylSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "vinylAngle"
    )

    val currentRotation = if (isPlaying) spinAngle else 0f

    // Reactive bass pulse on vinyl outer rim
    val bassScale = 1f + (analysisData.bass * 0.04f).coerceIn(0f, 0.06f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Vinyl Turntable Display
        Box(
            modifier = Modifier
                .size(310.dp)
                .scale(bassScale),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl Record Canvas (Grooves & Specular Highlights)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Deep Obsidian Base
                drawCircle(
                    color = Color(0xFF111116),
                    radius = radius,
                    center = center
                )

                // Concentric Grooves
                for (r in 40 until (radius - 10).toInt() step 6) {
                    drawCircle(
                        color = Color(0x18FFFFFF),
                        radius = r.toFloat(),
                        center = center,
                        style = Stroke(width = 1f)
                    )
                }

                // Specular Light Sheen (Opposing Cones)
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.25f to Color(0x28FFFFFF),
                        0.30f to Color.Transparent,
                        0.75f to Color(0x28FFFFFF),
                        0.80f to Color.Transparent,
                        1.0f to Color.Transparent
                    ),
                    radius = radius - 5f,
                    center = center
                )

                // Outer Vinyl Rim
                drawCircle(
                    color = Color(0x44FFFFFF),
                    radius = radius - 2f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }

            // Center Label / Album Artwork
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .rotate(currentRotation)
                    .background(palette.primary),
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

                // Center Spindle Hole
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06060A))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Artist
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFFA0A5BA)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Waveform / Scrubber
        LiquidGlassProgressBar(
            progress = playbackState.progress,
            onSeek = { fraction -> onSeek((fraction * playbackState.durationMs).toLong()) },
            palette = palette,
            waveformEnvelope = waveformEnvelope,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(playbackState.positionFormatted, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8888A0)))
            Text(playbackState.remainingFormatted, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8888A0)))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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

            IconButton(onClick = { /* More details */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
