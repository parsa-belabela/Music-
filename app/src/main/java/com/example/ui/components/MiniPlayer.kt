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
 * - Compact ~54-58dp height matching classic sleek layout
 * - Single source of truth playback state
 * - Direct artwork handling (shows musical icon fallback if no artwork, never borrows)
 * - Smooth progress bar along bottom edge
 * - Responsive play/pause & skip controls
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

    // Smooth color morphing
    val animatedPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "miniPlayerPrimary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "miniPlayerAccent"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "capsuleBreathing")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "capsuleAuraPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.End
    ) {
        if (connectedDevice != null) {
            AudioDeviceIndicator(
                device = connectedDevice,
                compact = true,
                modifier = Modifier.padding(bottom = 2.dp, end = 6.dp)
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
                        .padding(horizontal = 6.dp)
                ) {
                    val data = analysisDataProvider()
                    val bass = data.haloExpansion
                    val kick = data.kickPulse
                    val auraAlpha = (0.16f + bass * 0.16f + kick * 0.14f) * pulse

                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                animatedPrimary.copy(alpha = auraAlpha.coerceIn(0f, 0.45f)),
                                palette.secondary.copy(alpha = (auraAlpha * 0.6f).coerceIn(0f, 0.35f))
                            )
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, size.height + 4.dp.toPx())
                    )
                }
            }

            // The Floating Liquid Glass Capsule container (~56dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = animatedPrimary,
                        tintAlpha = 0.14f,
                        borderWidth = 1.dp
                    )
                    .clickable { onExpandNowPlaying() }
                    .testTag("mini_player_container")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Morphing Track Info (Artwork, Title, Artist)
                        AnimatedContent(
                            targetState = track,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                                 scaleIn(initialScale = 0.95f, animationSpec = tween(260, easing = FastOutSlowInEasing)))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                                        scaleOut(targetScale = 1.02f, animationSpec = tween(200, easing = FastOutSlowInEasing))
                                    )
                            },
                            modifier = Modifier.weight(1f),
                            label = "miniPlayerContentTransition"
                        ) { currentTrack ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Compact album artwork thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .shadow(4.dp, RoundedCornerShape(10.dp), ambientColor = Color.Black.copy(alpha = 0.35f))
                                        .clip(RoundedCornerShape(10.dp))
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
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Title & Artist
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            letterSpacing = 0.1.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = currentTrack.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFA0A5BA),
                                            fontSize = 11.sp,
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
                                .size(34.dp)
                                .testTag("mini_player_previous")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Tactile Play / Pause button in Glass pill
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x35FFFFFF),
                                            Color(0x12FFFFFF)
                                        )
                                    )
                                )
                                .testTag("mini_player_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = animatedAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        // Next Track button
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("mini_player_next")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Progress Track along bottom edge of capsule
                    var miniBarWidthPx by remember { mutableFloatStateOf(1f) }
                    var isMiniDragging by remember { mutableStateOf(false) }
                    var miniDragProgress by remember { mutableFloatStateOf(0f) }
                    var miniCurrentMs by remember { mutableLongStateOf(currentPositionProvider()) }
                    var miniIgnoreSyncUntil by remember { mutableLongStateOf(0L) }

                    val currentMiniDurationMs by rememberUpdatedState(playbackState.durationMs)
                    val currentMiniOnSeekTo by rememberUpdatedState(onSeekTo)
                    val currentMiniPositionProvider by rememberUpdatedState(currentPositionProvider)

                    LaunchedEffect(playbackState.currentTrack?.id, playbackState.durationMs) {
                        miniCurrentMs = currentMiniPositionProvider()
                        miniDragProgress = 0f
                        miniIgnoreSyncUntil = 0L
                    }

                    LaunchedEffect(isMiniDragging) {
                        if (!isMiniDragging) {
                            while (true) {
                                if (System.currentTimeMillis() > miniIgnoreSyncUntil) {
                                    miniCurrentMs = currentMiniPositionProvider()
                                }
                                kotlinx.coroutines.delay(20L)
                            }
                        }
                    }

                    val safeMiniDuration = currentMiniDurationMs.coerceAtLeast(1L)
                    val effectiveMiniMs = if (isMiniDragging) (miniDragProgress * safeMiniDuration).toLong() else miniCurrentMs
                    val effectiveFraction = (effectiveMiniMs.toFloat() / safeMiniDuration.toFloat()).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                            .background(Color(0x20FFFFFF))
                            .onSizeChanged { miniBarWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                            .pointerInput(playbackState.durationMs) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    isMiniDragging = true
                                    val w = miniBarWidthPx.coerceAtLeast(1f)
                                    miniDragProgress = (down.position.x / w).coerceIn(0f, 1f)
                                    down.consume()

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val pointer = event.changes.firstOrNull() ?: break
                                        if (pointer.pressed) {
                                            miniDragProgress = (pointer.position.x / w).coerceIn(0f, 1f)
                                            miniCurrentMs = (miniDragProgress * safeMiniDuration).toLong()
                                            pointer.consume()
                                        } else {
                                            val targetSeekMs = (miniDragProgress * safeMiniDuration).toLong()
                                            miniCurrentMs = targetSeekMs
                                            miniIgnoreSyncUntil = System.currentTimeMillis() + 400L
                                            currentMiniOnSeekTo(targetSeekMs)
                                            isMiniDragging = false
                                            break
                                        }
                                    }
                                }
                            }
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
