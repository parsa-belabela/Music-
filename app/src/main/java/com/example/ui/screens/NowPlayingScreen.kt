package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.audio.ConnectedAudioDevice
import com.example.data.model.*
import com.example.data.model.RepeatMode as PlaybackRepeatMode
import com.example.ui.components.*
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization

enum class NowPlayingCenterView {
    ARTWORK_AND_HALO,
    VISUALIZER_FULL,
    LYRICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    analysisData: AudioAnalysisData,
    palette: AmbientPalette,
    currentLyrics: List<LyricsLine>,
    appSettings: AppSettings,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenLyricsEditor: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit,
    onSelectVisualizerMode: (VisualizerMode) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenMetadataEditor: (Track) -> Unit,
    modifier: Modifier = Modifier,
    connectedDevice: ConnectedAudioDevice? = null,
    onShareSong: (Track) -> Unit = {},
    onPlaybackSpeedChange: (Float) -> Unit = {},
    currentPositionProvider: () -> Long = { playbackState.currentPositionMs }
) {
    val track = playbackState.currentTrack ?: return
    var centerView by remember { mutableStateOf(NowPlayingCenterView.ARTWORK_AND_HALO) }
    var isImmersive by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val triggerHaptic = {
        if (appSettings.hapticFeedbackEnabled) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

    // Dynamic scale for artwork responding to Bass/Kick & Focus Mode
    val isPlaying = playbackState.isPlaying

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50) {
                        triggerHaptic()
                        onPrevious()
                    } else if (dragAmount < -50) {
                        triggerHaptic()
                        onNext()
                    }
                }
            }
            .testTag("now_playing_screen")
    ) {
        // IDEA 02, 08: Cinematic Living Atmosphere (Blurred album art + organic audio-reactive lighting)
        CinematicAtmosphereBackground(
            track = track,
            palette = palette,
            analysisDataProvider = { analysisData },
            glowStrength = if (isImmersive) appSettings.visualizerGlow * 1.25f else appSettings.visualizerGlow
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Liquid Glass controls
            AnimatedVisibility(
                visible = !isImmersive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .size(42.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = palette.primary,
                                tintAlpha = 0.10f
                            )
                            .testTag("collapse_now_playing")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Album / Context title
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFA5A5BA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.3.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.album.ifBlank { track.artist },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sleep Timer active pill badge
                        if (appSettings.sleepTimerMinutes > 0) {
                            Box(
                                modifier = Modifier
                                    .liquidGlass(
                                        shape = RoundedCornerShape(14.dp),
                                        thickness = GlassThickness.THIN,
                                        tintColor = palette.primary,
                                        tintAlpha = 0.25f
                                    )
                                    .clickable { onOpenSleepTimer() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${appSettings.sleepTimerMinutes}m",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Options menu in Liquid Glass
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .liquidGlass(
                                        shape = CircleShape,
                                        thickness = GlassThickness.THIN,
                                        tintColor = palette.primary,
                                        tintAlpha = 0.10f
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier.background(Color(0xF212131F))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Equalizer & DSP", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = palette.accent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenEqualizer()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "اشتراک‌گذاری آهنگ" else "Share Song Card", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = palette.accent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onShareSong(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Lyrics Studio", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = palette.accent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenLyricsEditor()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sleep Timer", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = palette.accent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenSleepTimer()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit Metadata", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = palette.accent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenMetadataEditor(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isImmersive) "Exit Focus Mode" else "Cinematic Focus Mode", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null, tint = palette.secondary) },
                                    onClick = {
                                        showMoreMenu = false
                                        isImmersive = !isImmersive
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Apple AirPods / Bluetooth connection indicator
            if (connectedDevice != null && !isImmersive) {
                AudioDeviceIndicator(
                    device = connectedDevice,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            // Liquid Glass Capsule Switcher: Halo | Visualizer | Lyrics
            AnimatedVisibility(
                visible = !isImmersive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .liquidGlass(
                            shape = RoundedCornerShape(22.dp),
                            thickness = GlassThickness.THIN,
                            tintColor = palette.primary,
                            tintAlpha = 0.08f
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = centerView == NowPlayingCenterView.ARTWORK_AND_HALO,
                        onClick = {
                            triggerHaptic()
                            centerView = NowPlayingCenterView.ARTWORK_AND_HALO
                        },
                        label = { Text("Artwork", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary.copy(alpha = 0.85f),
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = Color(0xFFA5A5BA)
                        ),
                        border = null
                    )
                    FilterChip(
                        selected = centerView == NowPlayingCenterView.VISUALIZER_FULL,
                        onClick = {
                            triggerHaptic()
                            centerView = NowPlayingCenterView.VISUALIZER_FULL
                        },
                        label = { Text(appSettings.visualizerMode.title, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary.copy(alpha = 0.85f),
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = Color(0xFFA5A5BA)
                        ),
                        border = null
                    )
                    FilterChip(
                        selected = centerView == NowPlayingCenterView.LYRICS,
                        onClick = {
                            triggerHaptic()
                            centerView = NowPlayingCenterView.LYRICS
                        },
                        label = { Text("Lyrics", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary.copy(alpha = 0.85f),
                            selectedLabelColor = Color.White,
                            containerColor = Color.Transparent,
                            labelColor = Color(0xFFA5A5BA)
                        ),
                        border = null
                    )
                }
            }

            // Center Stage (Artwork, Visualizer, or Lyrics)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                triggerHaptic()
                                val cur = currentPositionProvider()
                                if (offset.x < size.width / 2) {
                                    onSeekTo((cur - 10000L).coerceAtLeast(0L))
                                } else {
                                    onSeekTo((cur + 10000L).coerceAtMost(playbackState.durationMs))
                                }
                            },
                            onTap = {
                                triggerHaptic()
                                // IDEA 10: Cinematic Focus Mode toggle on tap
                                isImmersive = !isImmersive
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when (centerView) {
                    NowPlayingCenterView.ARTWORK_AND_HALO -> {
                        AnimatedContent(
                            targetState = track,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(90)).togetherWith(fadeOut(animationSpec = tween(90)))
                            },
                            label = "nowPlayingArtworkTransition"
                        ) { currentTrack ->
                            // IDEA 01, 02: Album Aura with liquid glass concentric frame and dynamic reactive scale
                            Box(
                                modifier = Modifier
                                    .size(if (isImmersive) 320.dp else 285.dp)
                                    .graphicsLayer {
                                        val bass = if (isPlaying) analysisData.haloExpansion else 0f
                                        val kick = if (isPlaying) analysisData.kickPulse else 0f
                                        val s = if (isImmersive) 1.08f else (1f + bass * 0.035f + kick * 0.025f)
                                        scaleX = s
                                        scaleY = s
                                    }
                                    .shadow(
                                        elevation = 32.dp,
                                        shape = RoundedCornerShape(28.dp),
                                        ambientColor = palette.primary,
                                        spotColor = palette.accent
                                    )
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color(0xFF131322))
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color(0x60FFFFFF),
                                                palette.primary.copy(alpha = 0.45f),
                                                Color(0x10FFFFFF)
                                            )
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentTrack.artworkUri != null) {
                                    AsyncImage(
                                        model = currentTrack.artworkUri,
                                        contentDescription = "Artwork",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(96.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = currentTrack.genre,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = palette.accent,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                    }
                                }

                                // Subtle specular reflection sheen over the artwork glass
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0x20FFFFFF),
                                                    Color.Transparent
                                                ),
                                                startY = 0f,
                                                endY = 250f
                                            )
                                        )
                                )
                            }
                        }
                    }

                    NowPlayingCenterView.VISUALIZER_FULL -> {
                        AudioVisualizer(
                            mode = appSettings.visualizerMode,
                            analysisData = analysisData,
                            palette = palette,
                            modifier = Modifier.fillMaxSize(),
                            sensitivity = appSettings.visualizerSensitivity,
                            glow = appSettings.visualizerGlow
                        )
                    }

                    NowPlayingCenterView.LYRICS -> {
                        LyricsView(
                            lyrics = currentLyrics,
                            currentPositionProvider = currentPositionProvider,
                            displayMode = appSettings.lyricsDisplayMode,
                            palette = palette,
                            onSeekTo = onSeekTo,
                            onOpenEditor = onOpenLyricsEditor,
                            modifier = Modifier.fillMaxSize(),
                            enableWordHighlight = appSettings.lyricsKaraokeWordHighlight,
                            baseFontSize = appSettings.lyricsFontSize,
                            activeTrackId = track.id
                        )
                    }
                }
            }

            // Typography & Metadata (Confidently sized, crisp hierarchy)
            AnimatedVisibility(visible = !isImmersive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = track,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(90)).togetherWith(fadeOut(animationSpec = tween(90)))
                        },
                        modifier = Modifier.weight(1f),
                        label = "nowPlayingTitleTransition"
                    ) { currentTrack ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = currentTrack.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = (-0.4).sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = currentTrack.artist,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFFA5A5BC),
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Tactile Favorite button with Liquid Glass pill & burst
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onToggleFavorite(track)
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = if (track.isFavorite) Color(0xFFF43F5E) else Color.Transparent,
                                tintAlpha = 0.20f
                            )
                            .testTag("toggle_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) Color(0xFFF43F5E) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // IDEA 09: Liquid Glass Progress Scrubber
            AnimatedVisibility(visible = !isImmersive) {
                LiquidGlassProgressBar(
                    currentPositionProvider = currentPositionProvider,
                    durationMs = playbackState.durationMs,
                    palette = palette,
                    analysisDataProvider = { analysisData },
                    onSeekTo = {
                        triggerHaptic()
                        onSeekTo(it)
                    }
                )
            }

            // Controls Bar in Liquid Glass capsule: Shuffle, Previous, Play/Pause, Next, Repeat
            AnimatedVisibility(visible = !isImmersive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .liquidGlass(
                            shape = RoundedCornerShape(32.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.12f,
                            borderWidth = 1.2.dp
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                onToggleShuffle()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (playbackState.isShuffle) palette.accent.copy(alpha = 0.22f)
                                    else Color(0x14FFFFFF)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (playbackState.isShuffle) palette.accent else Color(0xFFA0A0B5),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Previous
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                onPrevious()
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x18FFFFFF))
                                .testTag("previous_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Glowing Liquid Glass Play / Pause Button with reactive pulse
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .shadow(18.dp, CircleShape, ambientColor = palette.primary, spotColor = palette.accent)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            palette.primary,
                                            palette.accent
                                        )
                                    )
                                )
                                .border(1.5.dp, Color(0x66FFFFFF), CircleShape)
                                .clickable {
                                    triggerHaptic()
                                    onTogglePlay()
                                }
                                .testTag("play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Next
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                onNext()
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x18FFFFFF))
                                .testTag("next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Repeat Mode
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                onCycleRepeat()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (playbackState.repeatMode != PlaybackRepeatMode.OFF) palette.accent.copy(alpha = 0.22f)
                                    else Color(0x14FFFFFF)
                                )
                        ) {
                            val (icon, tint) = when (playbackState.repeatMode) {
                                PlaybackRepeatMode.OFF -> Icons.Default.Repeat to Color(0xFFA0A0B5)
                                PlaybackRepeatMode.ALL -> Icons.Default.Repeat to palette.accent
                                PlaybackRepeatMode.ONE -> Icons.Default.RepeatOne to palette.accent
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "Repeat",
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Bottom bar: Queue and Lyric shortcuts
            AnimatedVisibility(visible = !isImmersive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            triggerHaptic()
                            onOpenLyricsEditor()
                        },
                        modifier = Modifier.liquidGlass(
                            shape = RoundedCornerShape(16.dp),
                            thickness = GlassThickness.THIN,
                            tintColor = palette.primary,
                            tintAlpha = 0.08f
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Lyrics, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (appSettings.language == AppLanguage.PERSIAN) "متن آهنگ" else "Lyrics", color = Color.White, fontSize = 13.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Playback Speed Selector Pill
                        Box(
                            modifier = Modifier
                                .liquidGlass(
                                    shape = RoundedCornerShape(14.dp),
                                    thickness = GlassThickness.THIN,
                                    tintColor = palette.primary,
                                    tintAlpha = 0.12f
                                )
                                .clickable {
                                    triggerHaptic()
                                    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 0.5f)
                                    val currentIdx = speeds.indexOfFirst { kotlin.math.abs(it - appSettings.playbackSpeed) < 0.05f }
                                    val nextSpeed = speeds[(if (currentIdx == -1) 1 else currentIdx + 1) % speeds.size]
                                    onPlaybackSpeedChange(nextSpeed)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${"%.2f".format(appSettings.playbackSpeed).trimEnd('0').trimEnd('.')}x",
                                color = palette.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Share Song Card Button
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                onShareSong(track)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .liquidGlass(
                                    shape = CircleShape,
                                    thickness = GlassThickness.THIN,
                                    tintColor = palette.primary,
                                    tintAlpha = 0.08f
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Song",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Queue Button
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                onOpenQueue()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .liquidGlass(
                                    shape = CircleShape,
                                    thickness = GlassThickness.THIN,
                                    tintColor = palette.primary,
                                    tintAlpha = 0.08f
                                )
                                .testTag("open_queue_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Queue",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Developer Mode HUD if enabled
        if (appSettings.developerModeEnabled) {
            DeveloperHud(
                analysisData = analysisData,
                playbackState = playbackState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 80.dp, start = 16.dp)
            )
        }
    }
}
