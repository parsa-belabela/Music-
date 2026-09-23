package com.example.ui.components

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.audio.ConnectedAudioDevice
import com.example.data.model.PlaybackState
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass

/**
 * Floating Liquid Glass Capsule Mini Player:
 * - Single source of truth playback state via playbackState.isPlaying
 * - Smooth interruptible morph transition for Artwork, Title & Artist
 * - Responsive dynamic progress track powered by isolated position provider
 * - Audio-reactive ambient light halo
 * - Functional Previous, Play/Pause, Next controls
 * - Apple-inspired AirPods/Bluetooth connection indicator
 */
@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    palette: AmbientPalette,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
    onExpandNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    connectedDevice: ConnectedAudioDevice? = null,
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() },
    currentPositionProvider: () -> Long = { playbackState.currentPositionMs },
    onSeekTo: (Long) -> Unit = {}
) {
    val track = playbackState.currentTrack ?: return
    val isPlaying = playbackState.isPlaying

    // Smooth color morphing for seamless scene transitions
    val animatedPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "miniPlayerPrimary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "miniPlayerAccent"
    )

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.End
    ) {
        if (connectedDevice != null) {
            AudioDeviceIndicator(
                device = connectedDevice,
                compact = true,
                modifier = Modifier.padding(bottom = 4.dp, end = 8.dp)
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
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
                            animatedPrimary.copy(alpha = auraAlpha.coerceIn(0f, 0.6f)),
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
                    tintColor = animatedPrimary,
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
                    // Morphing Track Info (Artwork, Title, Artist)
                    AnimatedContent(
                        targetState = track,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                             scaleIn(initialScale = 0.94f, animationSpec = tween(350, easing = FastOutSlowInEasing)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                    scaleOut(targetScale = 1.04f, animationSpec = tween(280, easing = FastOutSlowInEasing))
                                )
                        },
                        modifier = Modifier.weight(1f),
                        label = "miniPlayerContentTransition"
                    ) { currentTrack ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Album artwork thumbnail with soft glow aura & rounded corner
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.45f))
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF141322)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentTrack.artworkUri != null) {
                                    AsyncImage(
                                        model = currentTrack.artworkUri,
                                        contentDescription = "Track Artwork",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Music",
                                        tint = animatedPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Title & Artist with confident typography
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTrack.title,
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
                                    text = currentTrack.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFA0A5BA),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Previous Track button
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("mini_player_previous")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

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
                            tint = animatedAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Next Track button
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(36.dp)
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

                // Glowing Liquid Progress Track at bottom of capsule with interactive drag-to-seek
                var miniBarWidthPx by remember { mutableFloatStateOf(1f) }
                var isMiniDragging by remember { mutableStateOf(false) }
                var miniDragProgress by remember { mutableFloatStateOf(0f) }
                var miniCurrentMs by remember { mutableLongStateOf(currentPositionProvider()) }
                var miniIgnoreSyncUntil by remember { mutableLongStateOf(0L) }

                LaunchedEffect(isMiniDragging) {
                    if (!isMiniDragging) {
                        while (true) {
                            if (System.currentTimeMillis() > miniIgnoreSyncUntil) {
                                miniCurrentMs = currentPositionProvider()
                            }
                            kotlinx.coroutines.delay(20L)
                        }
                    }
                }

                val safeMiniDuration = playbackState.durationMs.coerceAtLeast(1L)
                val effectiveMiniMs = if (isMiniDragging) (miniDragProgress * safeMiniDuration).toLong() else miniCurrentMs
                val effectiveFraction = (effectiveMiniMs.toFloat() / safeMiniDuration.toFloat()).coerceIn(0f, 1f)

                val barHeight by animateDpAsState(
                    targetValue = if (isMiniDragging) 6.dp else 3.dp,
                    label = "miniBarHeight"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .onSizeChanged { miniBarWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                        .pointerInput(playbackState.durationMs) {
                            awaitEachGesture {
                                try {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()
                                    isMiniDragging = true
                                    val w = miniBarWidthPx.coerceAtLeast(1f)
                                    miniDragProgress = (down.position.x / w).coerceIn(0f, 1f)

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pointer = event.changes.firstOrNull { it.id == down.id }
                                        if (pointer == null || !pointer.pressed) {
                                            pointer?.consume()
                                            break
                                        }
                                        pointer.consume()
                                        miniDragProgress = (pointer.position.x / w).coerceIn(0f, 1f)
                                    }
                                    val targetSeekMs = (miniDragProgress * safeMiniDuration).toLong()
                                    miniCurrentMs = targetSeekMs
                                    miniIgnoreSyncUntil = System.currentTimeMillis() + 450L
                                    onSeekTo(targetSeekMs)
                                } finally {
                                    isMiniDragging = false
                                }
                            }
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .background(Color(0x20FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = effectiveFraction)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            animatedPrimary,
                                            animatedAccent
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
}
