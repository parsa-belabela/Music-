package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.PlaybackState
import com.example.data.model.PlayerStatus
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass

/**
 * IDEA 03: Floating Liquid Glass Capsule Mini Player
 * - Translucent liquid glass material (GlassThickness.REGULAR)
 * - Concentric geometry with rounded capsule corners
 * - Audio-reactive ambient light halo leaking subtly around capsule border
 * - Specular highlight reflection across the glass surface
 * - Glowing audio-reactive progress bar
 */
@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    palette: AmbientPalette,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onExpandNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() }
) {
    val track = playbackState.currentTrack ?: return
    val isPlaying = playbackState.status == PlayerStatus.PLAYING

    val infiniteTransition = rememberInfiniteTransition(label = "capsuleBreathing")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "capsuleAuraPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient aura emitting behind the capsule
        if (isPlaying) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp)
            ) {
                val data = analysisDataProvider()
                val bass = data.haloExpansion
                val kick = data.kickPulse
                val auraAlpha = (0.20f + bass * 0.22f + kick * 0.18f) * pulse

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            palette.primary.copy(alpha = auraAlpha.coerceIn(0f, 0.6f)),
                            palette.secondary.copy(alpha = (auraAlpha * 0.65f).coerceIn(0f, 0.45f))
                        )
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height + 6.dp.toPx())
                )
            }
        }

        // The Floating Liquid Glass Capsule container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(26.dp),
                    thickness = GlassThickness.REGULAR,
                    tintColor = palette.primary,
                    tintAlpha = 0.14f,
                    borderWidth = 1.2.dp
                )
                .clickable { onExpandNowPlaying() }
                .testTag("mini_player_container")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album artwork thumbnail with soft glow aura & rounded corner
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = palette.primary, spotColor = palette.accent)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF141322)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (track.artworkUri != null) {
                            AsyncImage(
                                model = track.artworkUri,
                                contentDescription = "Track Artwork",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Music",
                                tint = palette.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Artist with confident typography
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.2.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFA0A5BA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Tactile Play / Pause button in Glass pill
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x35FFFFFF),
                                        Color(0x10FFFFFF)
                                    )
                                )
                            )
                            .testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = palette.accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Next Track button
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("mini_player_next")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Glowing Liquid Progress Track at bottom of capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = playbackState.progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        palette.primary,
                                        palette.accent
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
