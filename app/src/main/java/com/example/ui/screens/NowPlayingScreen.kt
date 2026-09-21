package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.data.model.*
import com.example.ui.components.*

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
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack ?: return
    var centerView by remember { mutableStateOf(NowPlayingCenterView.ARTWORK_AND_HALO) }
    var isImmersive by remember { mutableStateOf(false) }
    var showVisualizerDropdown by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        palette.primary.copy(alpha = 0.22f),
                        Color(0xFF0A0A14),
                        Color(0xFF050508)
                    )
                )
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50) onPrevious()
                    else if (dragAmount < -50) onNext()
                }
            }
            .testTag("now_playing_screen")
    ) {
        // Ambient Halo / Background aura that breathes continuously
        AmbientHalo(
            analysisData = analysisData,
            palette = palette,
            modifier = Modifier.fillMaxSize(),
            glowStrength = appSettings.visualizerGlow
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
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
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCollapse, modifier = Modifier.testTag("collapse_now_playing")) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Playing from album or playlist label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF9898B0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row {
                        // Visualizer mode menu button
                        Box {
                            IconButton(onClick = { showVisualizerDropdown = true }) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Visualizer Mode",
                                    tint = palette.accent
                                )
                            }
                            DropdownMenu(
                                expanded = showVisualizerDropdown,
                                onDismissRequest = { showVisualizerDropdown = false },
                                modifier = Modifier.background(Color(0xFF141424))
                            ) {
                                VisualizerMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = mode.title,
                                                color = if (appSettings.visualizerMode == mode) palette.primary else Color.White
                                            )
                                        },
                                        onClick = {
                                            onSelectVisualizerMode(mode)
                                            showVisualizerDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // More options menu (EQ, Lyrics Editor, Sleep Timer, Info)
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier.background(Color(0xFF141424))
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
                                    text = { Text("Lyrics Editor & Sync", color = Color.White) },
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
                                    text = { Text("Edit Track Metadata", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = palette.accent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenMetadataEditor(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isImmersive) "Exit Immersive" else "Immersive Mode", color = Color.White) },
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

            // Center View Switcher Tabs (Artwork & Halo | Visualizer | Lyrics)
            AnimatedVisibility(visible = !isImmersive) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = centerView == NowPlayingCenterView.ARTWORK_AND_HALO,
                        onClick = { centerView = NowPlayingCenterView.ARTWORK_AND_HALO },
                        label = { Text("Halo", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = centerView == NowPlayingCenterView.VISUALIZER_FULL,
                        onClick = { centerView = NowPlayingCenterView.VISUALIZER_FULL },
                        label = { Text(appSettings.visualizerMode.title, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = centerView == NowPlayingCenterView.LYRICS,
                        onClick = { centerView = NowPlayingCenterView.LYRICS },
                        label = { Text("Lyrics", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Center Display Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                if (offset.x < size.width / 2) {
                                    onSeekTo((playbackState.currentPositionMs - 10000L).coerceAtLeast(0L))
                                } else {
                                    onSeekTo((playbackState.currentPositionMs + 10000L).coerceAtMost(playbackState.durationMs))
                                }
                            },
                            onTap = {
                                if (isImmersive) {
                                    isImmersive = false
                                } else {
                                    // Cycle view on tap
                                    centerView = when (centerView) {
                                        NowPlayingCenterView.ARTWORK_AND_HALO -> NowPlayingCenterView.VISUALIZER_FULL
                                        NowPlayingCenterView.VISUALIZER_FULL -> NowPlayingCenterView.LYRICS
                                        NowPlayingCenterView.LYRICS -> NowPlayingCenterView.ARTWORK_AND_HALO
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                when (centerView) {
                    NowPlayingCenterView.ARTWORK_AND_HALO -> {
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = palette.primary, spotColor = palette.accent)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF16152B))
                                .border(1.5.dp, palette.primary.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (track.artworkUri != null) {
                                AsyncImage(
                                    model = track.artworkUri,
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
                                        text = track.genre,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = palette.accent,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
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
                            currentPositionMs = playbackState.currentPositionMs,
                            displayMode = appSettings.lyricsDisplayMode,
                            palette = palette,
                            onSeekTo = onSeekTo,
                            onOpenEditor = onOpenLyricsEditor,
                            modifier = Modifier.fillMaxSize(),
                            enableWordHighlight = appSettings.lyricsKaraokeWordHighlight,
                            baseFontSize = appSettings.lyricsFontSize
                        )
                    }
                }
            }

            // Metadata info: Title, Artist, Favorite Heart
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
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFA5A5BC),
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier.testTag("toggle_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) Color(0xFFF43F5E) else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Seek Bar & Timestamps
            AnimatedVisibility(visible = !isImmersive) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = playbackState.currentPositionMs.toFloat(),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..(playbackState.durationMs.toFloat().coerceAtLeast(1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("seek_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.primary,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = playbackState.positionFormatted,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B0), fontSize = 12.sp)
                        )
                        Text(
                            text = playbackState.remainingFormatted,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B0), fontSize = 12.sp)
                        )
                    }
                }
            }

            // Controls Bar: Shuffle, Previous, Play/Pause, Next, Repeat
            AnimatedVisibility(visible = !isImmersive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffle) palette.primary else Color(0xFF707085),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("previous_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Big Glowing Play / Pause Button
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(16.dp, CircleShape, ambientColor = palette.primary, spotColor = palette.primary)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(palette.primary, palette.secondary)
                                )
                            )
                            .clickable { onTogglePlay() }
                            .testTag("play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        val isPlaying = playbackState.status == PlayerStatus.PLAYING
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Repeat Mode
                    IconButton(onClick = onCycleRepeat) {
                        val (icon, color) = when (playbackState.repeatMode) {
                            RepeatMode.OFF -> Icons.Default.Repeat to Color(0xFF707085)
                            RepeatMode.ALL -> Icons.Default.Repeat to palette.primary
                            RepeatMode.ONE -> Icons.Default.RepeatOne to palette.accent
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
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
                    TextButton(onClick = onOpenLyricsEditor) {
                        Icon(imageVector = Icons.Default.Lyrics, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lyrics Studio", color = Color.White, fontSize = 13.sp)
                    }

                    IconButton(onClick = onOpenQueue, modifier = Modifier.testTag("open_queue_button")) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
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
