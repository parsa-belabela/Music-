package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    onOpenVipPaywall: (String?) -> Unit = {},
    onSelectNowPlayingStyle: (String) -> Unit = {},
    onUpdateTrackArtwork: (Track, String?) -> Unit = { _, _ -> },
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() },
    currentPositionProvider: () -> Long = { playbackState.currentPositionMs }
) {
    val context = LocalContext.current
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

    // Photo picker for custom song cover art
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onUpdateTrackArtwork(track, uri.toString())
        }
    }

    val waveformFloats = remember(audioProfile?.waveformEnvelope) {
        audioProfile?.waveformEnvelope?.split(",")?.mapNotNull { it.trim().toFloatOrNull() }?.toFloatArray()
    }

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
        // Living Atmosphere with vivid ambient light emission
        CinematicAtmosphereBackground(
            track = track,
            palette = palette,
            analysisDataProvider = analysisDataProvider,
            glowStrength = if (isImmersive) appSettings.visualizerGlow * 2.2f else appSettings.visualizerGlow * 1.8f
        )

        // Signature Liquid Glass Mode (Matching Reference Screenshot 2)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar
            AnimatedVisibility(
                visible = !isImmersive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
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
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Options Menu
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
                                text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "تغییر کاور آهنگ" else "Change Song Artwork", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = palette.accent) },
                                onClick = {
                                    showMoreMenu = false
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "اکولایزر و تنظیمات صدا" else "Equalizer & DSP", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = palette.accent) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenEqualizer()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "تایمر خواب" else "Sleep Timer", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null, tint = palette.accent) },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenSleepTimer()
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
                        }
                    }
                }
            }

            // 2. Segmented Pills [ Artwork | Ambient Halo | Lyrics ]
            AnimatedVisibility(
                visible = !isImmersive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0x28000000))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(30.dp))
                        .padding(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SegmentPillItem(
                            title = if (appSettings.language == AppLanguage.PERSIAN) "کاور آهنگ" else "Artwork",
                            isSelected = centerView == NowPlayingCenterView.ARTWORK_AND_HALO,
                            activeColor = Color(0xFFFFA500),
                            onClick = {
                                triggerHaptic()
                                centerView = NowPlayingCenterView.ARTWORK_AND_HALO
                            }
                        )

                        SegmentPillItem(
                            title = if (appSettings.language == AppLanguage.PERSIAN) "هاله صوتی" else "Ambient Halo",
                            isSelected = centerView == NowPlayingCenterView.VISUALIZER_FULL,
                            activeColor = Color(0xFFFFA500),
                            onClick = {
                                triggerHaptic()
                                centerView = NowPlayingCenterView.VISUALIZER_FULL
                            }
                        )

                        SegmentPillItem(
                            title = if (appSettings.language == AppLanguage.PERSIAN) "متن آهنگ" else "Lyrics",
                            isSelected = centerView == NowPlayingCenterView.LYRICS,
                            activeColor = Color(0xFFFFA500),
                            onClick = {
                                triggerHaptic()
                                centerView = NowPlayingCenterView.LYRICS
                            }
                        )
                    }
                }
            }

            // 3. Center Component (Artwork / Visualizer / Lyrics)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                when (centerView) {
                    NowPlayingCenterView.ARTWORK_AND_HALO -> {
                        val audioData = analysisDataProvider()
                        val beatPulse = if (isPlaying) (audioData.kickPulse * 0.035f + audioData.haloExpansion * 0.018f).coerceIn(0f, 0.05f) else 0f
                        val animatedBeatPulse by animateFloatAsState(
                            targetValue = beatPulse,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "artworkBeatPulse"
                        )
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        val offsetYPx = remember(animatedBeatPulse, density) { with(density) { (-12.dp * animatedBeatPulse).toPx() } }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.72f)
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    scaleX = 1.0f + animatedBeatPulse * 0.5f
                                    scaleY = 1.0f + animatedBeatPulse * 0.5f
                                    translationY = offsetYPx
                                }
                                .shadow(28.dp, RoundedCornerShape(28.dp), spotColor = palette.primary)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFF141624))
                                .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(28.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            triggerHaptic()
                                            onToggleFavorite(track)
                                        },
                                        onLongPress = {
                                            triggerHaptic()
                                            showRadialMenu = true
                                        },
                                        onTap = {
                                            isImmersive = !isImmersive
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!track.artworkUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = track.artworkUri,
                                    contentDescription = track.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                listOf(palette.primary.copy(alpha = 0.5f), Color(0xFF141624))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(90.dp)
                                    )
                                }
                            }
                        }
                    }

                    NowPlayingCenterView.VISUALIZER_FULL -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .liquidGlass(
                                    shape = RoundedCornerShape(32.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = palette.primary,
                                    tintAlpha = 0.15f
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AudioVisualizer(
                                mode = appSettings.visualizerMode,
                                palette = palette,
                                glow = appSettings.visualizerGlow,
                                analysisDataProvider = analysisDataProvider,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    NowPlayingCenterView.LYRICS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .liquidGlass(
                                    shape = RoundedCornerShape(32.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = palette.primary,
                                    tintAlpha = 0.15f
                                )
                                .padding(16.dp)
                        ) {
                            LyricsView(
                                lyrics = currentLyrics,
                                currentPositionMs = currentPositionProvider(),
                                displayMode = appSettings.lyricsDisplayMode,
                                palette = palette,
                                onSeekTo = onSeekTo,
                                onOpenEditor = onOpenLyricsEditor,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // 4. Track Info: Title & Subtitle on left, Circular Glass Heart on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 22.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFA5ABC0),
                            fontSize = 14.sp
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
                            tintColor = if (track.isFavorite) Color(0xFFFF1744) else palette.primary,
                            tintAlpha = if (track.isFavorite) 0.35f else 0.12f
                        )
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isFavorite) Color(0xFFFF1744) else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 5. Liquid Glass Progress Bar with Orange Glow
            val currentPos = currentPositionProvider()
            val dur = if (track.durationMs > 0) track.durationMs else 1L
            val elapsedSec = currentPos / 1000
            val remainingSec = (dur - currentPos).coerceAtLeast(0) / 1000
            val elapsedStr = String.format("%d:%02d", elapsedSec / 60, elapsedSec % 60)
            val remainingStr = String.format("-%d:%02d", remainingSec / 60, remainingSec % 60)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                LiquidGlassProgressBar(
                    currentPositionProvider = currentPositionProvider,
                    durationMs = dur,
                    palette = palette.copy(primary = Color(0xFFFFA500), accent = Color(0xFFFF8C00)),
                    analysisDataProvider = analysisDataProvider,
                    onSeekTo = onSeekTo,
                    waveformEnvelope = waveformFloats,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = elapsedStr,
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA5ABC0), fontSize = 12.sp)
                    )
                    Text(
                        text = remainingStr,
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA5ABC0), fontSize = 12.sp)
                    )
                }
            }

            // 6. Playback Controls Capsule (Shuffle, Prev, Big Orange Play, Next, Repeat)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(32.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = Color(0xFF0F111E),
                        tintAlpha = 0.50f,
                        borderWidth = 1.dp
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffle) Color(0xFFFFA500) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Skip Previous
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onPrevious()
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Big Orange Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                                )
                            )
                            .shadow(16.dp, CircleShape, spotColor = Color(0xFFFF9800))
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

                    // Skip Next
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onNext()
                        },
                        modifier = Modifier.size(44.dp)
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
                        modifier = Modifier.size(42.dp)
                    ) {
                        val repIcon = when (playbackState.repeatMode) {
                            PlaybackRepeatMode.ONE -> Icons.Default.RepeatOne
                            PlaybackRepeatMode.ALL -> Icons.Default.Repeat
                            PlaybackRepeatMode.OFF -> Icons.Default.Repeat
                        }
                        Icon(
                            imageVector = repIcon,
                            contentDescription = "Repeat",
                            tint = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) Color(0xFFFFA500) else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 7. Device Volume Control
            LiquidGlassVolumeControl(
                palette = palette.copy(primary = Color(0xFFFFA500), accent = Color(0xFFFF8C00)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // 8. Bottom Action Bar: [ Lyrics ] [ 1x ] [ Share ] [ Queue ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lyrics button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x28000000))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .clickable {
                            triggerHaptic()
                            centerView = NowPlayingCenterView.LYRICS
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (appSettings.language == AppLanguage.PERSIAN) "متن" else "Lyrics",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Playback speed pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x28000000))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .clickable {
                            triggerHaptic()
                            val nextSpeed = when (appSettings.playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            onPlaybackSpeedChange(nextSpeed)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${appSettings.playbackSpeed}x",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Share Song
                IconButton(
                    onClick = {
                        triggerHaptic()
                        onShareSong(track)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .liquidGlass(
                            shape = CircleShape,
                            thickness = GlassThickness.THIN,
                            tintColor = palette.primary,
                            tintAlpha = 0.10f
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Queue
                IconButton(
                    onClick = {
                        triggerHaptic()
                        onOpenQueue()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .liquidGlass(
                            shape = CircleShape,
                            thickness = GlassThickness.THIN,
                            tintColor = palette.primary,
                            tintAlpha = 0.10f
                        )
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

@Composable
private fun SegmentPillItem(
    title: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFFA5ABC0),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}
