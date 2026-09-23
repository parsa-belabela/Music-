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
    audioProfile: TrackAudioProfile? = null,
    onCycleShuffleMode: () -> Unit = onToggleShuffle,
    onOpenHearingCalibration: () -> Unit = {},
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() },
    currentPositionProvider: () -> Long = { playbackState.currentPositionMs }
) {
    val track = playbackState.currentTrack ?: return
    var centerView by remember { mutableStateOf(NowPlayingCenterView.ARTWORK_AND_HALO) }
    var isImmersive by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRadialMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val triggerHaptic = {
        if (appSettings.hapticFeedbackEnabled) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

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
        // Living Atmosphere
        CinematicAtmosphereBackground(
            track = track,
            palette = palette,
            analysisDataProvider = analysisDataProvider,
            glowStrength = if (isImmersive) appSettings.visualizerGlow * 1.25f else appSettings.visualizerGlow
        )

        if (appSettings.selectedNowPlayingStyle == "vinyl_turntable") {
            // Vinyl Turntable Mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onCollapse()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = palette.primary,
                                tintAlpha = 0.12f
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = "VINYL CLASSIC",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = palette.accent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )

                    IconButton(
                        onClick = { onShareSong(track) },
                        modifier = Modifier
                            .size(42.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = palette.primary,
                                tintAlpha = 0.12f
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                VinylNowPlayingStyle(
                    playbackState = playbackState,
                    palette = palette,
                    analysisData = analysisDataProvider(),
                    waveformEnvelope = audioProfile?.waveformEnvelope?.split(",")?.mapNotNull { it.trim().toFloatOrNull() }?.toFloatArray(),
                    onPlayPause = onTogglePlay,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeekTo,
                    onToggleFavorite = { onToggleFavorite(track) },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Standard Liquid Glass Mode
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar
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
                            onClick = {
                                triggerHaptic()
                                onCollapse()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .liquidGlass(
                                    shape = CircleShape,
                                    thickness = GlassThickness.THIN,
                                    tintColor = palette.primary,
                                    tintAlpha = 0.12f
                                )
                                .testTag("collapse_now_playing_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (if (appSettings.language == AppLanguage.PERSIAN) "در حال پخش" else "PLAYING FROM LIBRARY").uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFA5ABC0),
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = track.album,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Right Options
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (appSettings.sleepTimerMinutes > 0) {
                                Box(
                                    modifier = Modifier
                                        .liquidGlass(
                                            shape = RoundedCornerShape(12.dp),
                                            thickness = GlassThickness.THIN,
                                            tintColor = palette.accent,
                                            tintAlpha = 0.18f
                                        )
                                        .clickable { onOpenSleepTimer() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
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
                                        text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "اشتراک‌گذاری استوری" else "Share Social Card", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = palette.accent) },
                                        onClick = {
                                            showMoreMenu = false
                                            onShareSong(track)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Hearing Calibration", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Hearing, contentDescription = null, tint = palette.accent) },
                                        onClick = {
                                            showMoreMenu = false
                                            onOpenHearingCalibration()
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

                // Center View (Artwork / Visualizer / Lyrics)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
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
                                onLongPress = {
                                    triggerHaptic()
                                    showRadialMenu = true
                                },
                                onTap = {
                                    triggerHaptic()
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
                                Box(
                                    modifier = Modifier
                                        .size(if (isImmersive) 320.dp else 285.dp)
                                        .graphicsLayer {
                                            val data = analysisDataProvider()
                                            val bass = if (isPlaying) data.haloExpansion else 0f
                                            val kick = if (isPlaying) data.kickPulse else 0f
                                            val s = if (isImmersive) 1.08f else (1f + bass * 0.035f + kick * 0.025f)
                                            scaleX = s
                                            scaleY = s
                                        }
                                        .shadow(
                                            elevation = 16.dp,
                                            shape = RoundedCornerShape(28.dp),
                                            ambientColor = Color.Black.copy(alpha = 0.35f),
                                            spotColor = Color.Black.copy(alpha = 0.50f)
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
                                palette = palette,
                                modifier = Modifier.fillMaxSize(),
                                sensitivity = appSettings.visualizerSensitivity,
                                glow = appSettings.visualizerGlow,
                                analysisDataProvider = analysisDataProvider
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

                // Typography & Metadata
                AnimatedVisibility(visible = !isImmersive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color(0xFFA5ABC0),
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

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

                // Waveform Scrubber
                AnimatedVisibility(visible = !isImmersive) {
                    LiquidGlassProgressBar(
                        currentPositionProvider = currentPositionProvider,
                        durationMs = playbackState.durationMs,
                        palette = palette,
                        analysisDataProvider = analysisDataProvider,
                        onSeekTo = {
                            triggerHaptic()
                            onSeekTo(it)
                        },
                        waveformEnvelope = audioProfile?.waveformEnvelope?.split(",")?.mapNotNull { it.trim().toFloatOrNull() }?.toFloatArray()
                    )
                }

                // Controls Bar
                AnimatedVisibility(visible = !isImmersive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
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
                            // 3-State Shuffle (OFF, SHUFFLE, INTELLIGENT)
                            IconButton(
                                onClick = {
                                    triggerHaptic()
                                    onCycleShuffleMode()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (playbackState.shuffleMode) {
                                            ShuffleMode.OFF -> Color(0x14FFFFFF)
                                            ShuffleMode.SHUFFLE -> palette.accent.copy(alpha = 0.22f)
                                            ShuffleMode.INTELLIGENT -> palette.primary.copy(alpha = 0.35f)
                                        }
                                    )
                            ) {
                                val (icon, tint) = when (playbackState.shuffleMode) {
                                    ShuffleMode.OFF -> Icons.Default.Shuffle to Color(0xFFA0A0B5)
                                    ShuffleMode.SHUFFLE -> Icons.Default.Shuffle to palette.accent
                                    ShuffleMode.INTELLIGENT -> Icons.Default.AutoAwesome to palette.primary
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "Shuffle",
                                    tint = tint,
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

                            // Play / Pause
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.45f))
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

                // Volume Control
                AnimatedVisibility(visible = !isImmersive) {
                    LiquidGlassVolumeControl(
                        palette = palette,
                        analysisDataProvider = analysisDataProvider,
                        hapticFeedbackEnabled = appSettings.hapticFeedbackEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Bottom bar
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
        }

        // Radial Action Menu Modal
        if (showRadialMenu) {
            RadialActionMenu(
                track = track,
                palette = palette,
                hapticEnabled = appSettings.hapticFeedbackEnabled,
                onDismiss = { showRadialMenu = false },
                onPlayNext = { onNext() },
                onAddToQueue = { onOpenQueue() },
                onToggleFavorite = { onToggleFavorite(track) },
                onAddToPlaylist = { onOpenMetadataEditor(track) },
                onShare = { onShareSong(track) }
            )
        }

        // Developer Mode HUD if enabled
        if (appSettings.developerModeEnabled) {
            DeveloperHud(
                analysisDataProvider = analysisDataProvider,
                playbackState = playbackState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 80.dp, start = 16.dp)
            )
        }
    }
}
