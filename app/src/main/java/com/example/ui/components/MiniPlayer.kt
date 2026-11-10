package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.audio.ConnectedAudioDevice
import com.example.data.model.PlaybackState
import com.example.data.model.PlayerStatus
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.TextSecondary

/**
 * Spotify-style Floating MiniPlayer Bar:
 * - Compact 56dp height with 8dp rounded corners
 * - Tonal dark surface subtly tinted from the album cover
 * - Crisp 40dp artwork, clear title & artist/device subtitle, tactile Play/Pause & Next controls
 * - Zero-recomposition 2dp bottom progress bar
 */
@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    palette: AmbientPalette,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpandNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    connectedDevice: ConnectedAudioDevice? = null,
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() },
    currentPositionProvider: () -> Long = { playbackState.currentPositionMs },
    onSeekTo: (Long) -> Unit = {}
) {
    val track = playbackState.currentTrack ?: return
    val isPlaying = playbackState.status == PlayerStatus.PLAYING

    val targetContainerColor = remember(palette.primary, palette.isMonochrome) {
        if (palette.isMonochrome) {
            Color(0xFF282828)
        } else {
            lerp(Color(0xFF222222), palette.primary.copy(alpha = 1f), 0.24f)
        }
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "miniPlayerBg"
    )

    val durationMs = remember(playbackState.durationMs, track.durationMs) {
        (if (playbackState.durationMs > 1000L) playbackState.durationMs else track.durationMs).coerceAtLeast(1L)
    }

    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(56.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(8.dp), spotColor = Color.Black)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .pointerInput(track.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulated = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulated += dragAmount
                    },
                    onDragEnd = {
                        if (dragAccumulated < -72f) {
                            onNext()
                        } else if (dragAccumulated > 72f) {
                            onPrevious()
                        }
                        dragAccumulated = 0f
                    }
                )
            }
            .clickable { onExpandNowPlaying() }
            .drawWithContent {
                drawContent()
                val progress = (currentPositionProvider().toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                val barHeight = 2.dp.toPx()
                val y = size.height - barHeight
                // Subtle track background line
                drawRect(
                    color = Color.White.copy(alpha = 0.16f),
                    topLeft = Offset(0f, y),
                    size = Size(size.width, barHeight)
                )
                // Active progress indicator
                drawRect(
                    color = Color.White,
                    topLeft = Offset(0f, y),
                    size = Size(size.width * progress, barHeight)
                )
            }
            .testTag("mini_player")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtworkThumbnail(
                artworkUri = track.artworkUri,
                accentColor = palette.primary,
                size = 40.dp,
                shape = RoundedCornerShape(6.dp),
                iconSize = 20.dp,
                title = track.title,
                artist = track.artist,
                trackId = track.id
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (connectedDevice != null) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = AuraSpotifyGreen,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("mini_player_next")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

