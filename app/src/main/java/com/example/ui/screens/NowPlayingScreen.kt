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

private data class SkinOption(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val previewColor: Color
)

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
    val isVipUser = remember(appSettings) { com.example.monetization.EntitlementManager.isVip(context) }
    var centerView by remember { mutableStateOf(NowPlayingCenterView.ARTWORK_AND_HALO) }
    var isImmersive by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRadialMenu by remember { mutableStateOf(false) }
    var showSkinPickerSheet by remember { mutableStateOf(false) }
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

    val skinOptions = remember {
        listOf(
            SkinOption("default", "شیشه‌ای استاندارد (Liquid Glass)", "Liquid Glass Signature", Color(0xFF00E5FF)),
            SkinOption("electric_turntable", "نئون سایبرپانک (Electric Turntable)", "Electric Cyber Turntable", Color(0xFF00E5FF)),
            SkinOption("barbie_dream", "باربی دریم (Barbie)", "Barbie Dream Glow", Color(0xFFFF1493)),
            SkinOption("batman_knight", "شوالیه تاریکی (Batman)", "The Dark Knight", Color(0xFFFFCC00)),
            SkinOption("last_of_us", "لست آف آز (The Last of Us)", "The Last of Us (Firefly)", Color(0xFF81C784))
        )
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
        // Living Atmosphere
        CinematicAtmosphereBackground(
            track = track,
            palette = palette,
            analysisDataProvider = analysisDataProvider,
            glowStrength = if (isImmersive) appSettings.visualizerGlow * 1.25f else appSettings.visualizerGlow
        )

        val selectedStyle = appSettings.selectedNowPlayingStyle

        when (selectedStyle) {
            "electric_turntable" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(top = 8.dp)
                ) {
                    CollectorSkinHeader(
                        title = "CYBER TURNTABLE",
                        palette = palette.copy(primary = Color(0xFF00E5FF), accent = Color(0xFF7C4DFF)),
                        onCollapse = onCollapse,
                        onShare = { onShareSong(track) },
                        onOpenSkinPicker = { showSkinPickerSheet = true }
                    )

                    ElectricTurntableNowPlayingStyle(
                        playbackState = playbackState,
                        palette = palette,
                        analysisData = analysisDataProvider(),
                        waveformEnvelope = waveformFloats,
                        onPlayPause = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeekTo,
                        onToggleFavorite = { onToggleFavorite(track) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            "barbie_dream" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(top = 8.dp)
                ) {
                    CollectorSkinHeader(
                        title = "BARBIE DREAM",
                        palette = palette.copy(primary = Color(0xFFFF1493), accent = Color(0xFFFFD700)),
                        onCollapse = onCollapse,
                        onShare = { onShareSong(track) },
                        onOpenSkinPicker = { showSkinPickerSheet = true }
                    )

                    BarbieNowPlayingStyle(
                        playbackState = playbackState,
                        palette = palette,
                        analysisData = analysisDataProvider(),
                        waveformEnvelope = waveformFloats,
                        onPlayPause = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeekTo,
                        onToggleFavorite = { onToggleFavorite(track) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            "batman_knight" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(top = 8.dp)
                ) {
                    CollectorSkinHeader(
                        title = "THE DARK KNIGHT",
                        palette = palette.copy(primary = Color(0xFFFFCC00), accent = Color(0xFF64B5F6)),
                        onCollapse = onCollapse,
                        onShare = { onShareSong(track) },
                        onOpenSkinPicker = { showSkinPickerSheet = true }
                    )

                    BatmanNowPlayingStyle(
                        playbackState = playbackState,
                        palette = palette,
                        analysisData = analysisDataProvider(),
                        waveformEnvelope = waveformFloats,
                        onPlayPause = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeekTo,
                        onToggleFavorite = { onToggleFavorite(track) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            "last_of_us" -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(top = 8.dp)
                ) {
                    CollectorSkinHeader(
                        title = "THE LAST OF US",
                        palette = palette.copy(primary = Color(0xFFFFB300), accent = Color(0xFF81C784)),
                        onCollapse = onCollapse,
                        onShare = { onShareSong(track) },
                        onOpenSkinPicker = { showSkinPickerSheet = true }
                    )

                    LastOfUsNowPlayingStyle(
                        playbackState = playbackState,
                        palette = palette,
                        analysisData = analysisDataProvider(),
                        waveformEnvelope = waveformFloats,
                        onPlayPause = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeekTo,
                        onToggleFavorite = { onToggleFavorite(track) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            else -> {
                // Standard Liquid Glass Mode (Matching Screenshot 2)
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

                            // Right Options (Skins, Sleep Timer & More)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Skin Switcher Button
                                IconButton(
                                    onClick = { showSkinPickerSheet = true },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .liquidGlass(
                                            shape = CircleShape,
                                            thickness = GlassThickness.THIN,
                                            tintColor = palette.accent,
                                            tintAlpha = 0.15f
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Skins",
                                        tint = palette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
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
                                            text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "انتخاب پوسته و تم پلیر" else "Select Player Skin", color = Color.White) },
                                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = palette.accent) },
                                            onClick = {
                                                showMoreMenu = false
                                                showSkinPickerSheet = true
                                            }
                                        )
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
                    }

                    // 2. Segmented Pills [ Artwork | Ambient Halo | Lyrics ] (Matching Screenshot 2)
                    if (!isImmersive) {
                        Box(
                            modifier = Modifier
                                .liquidGlass(
                                    shape = RoundedCornerShape(20.dp),
                                    thickness = GlassThickness.THIN,
                                    tintColor = palette.primary,
                                    tintAlpha = 0.12f
                                )
                                .padding(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val tabs = listOf(
                                    Triple("Artwork", NowPlayingCenterView.ARTWORK_AND_HALO, if (appSettings.language == AppLanguage.PERSIAN) "کاور آهنگ" else "Artwork"),
                                    Triple("Ambient Halo", NowPlayingCenterView.VISUALIZER_FULL, if (appSettings.language == AppLanguage.PERSIAN) "هاله صوتی" else "Ambient Halo"),
                                    Triple("Lyrics", NowPlayingCenterView.LYRICS, if (appSettings.language == AppLanguage.PERSIAN) "متن آهنگ" else "Lyrics")
                                )

                                tabs.forEach { (_, view, label) ->
                                    val isSelected = centerView == view
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) palette.primary else Color.Transparent)
                                            .clickable {
                                                triggerHaptic()
                                                centerView = view
                                            }
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Center View (Artwork / Visualizer / Lyrics)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (centerView) {
                            NowPlayingCenterView.ARTWORK_AND_HALO -> {
                                Box(
                                    modifier = Modifier
                                        .size(285.dp)
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(Color(0xFF141524))
                                        .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (track.artworkUri != null) {
                                        AsyncImage(
                                            model = track.artworkUri,
                                            contentDescription = "Cover",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(80.dp)
                                        )
                                    }
                                }
                            }
                            NowPlayingCenterView.VISUALIZER_FULL -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { centerView = NowPlayingCenterView.ARTWORK_AND_HALO }
                                ) {
                                    AudioVisualizer(
                                        mode = appSettings.visualizerMode,
                                        palette = palette,
                                        analysisDataProvider = analysisDataProvider,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            NowPlayingCenterView.LYRICS -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { centerView = NowPlayingCenterView.ARTWORK_AND_HALO }
                                ) {
                                    LyricsView(
                                        lyrics = currentLyrics,
                                        currentPositionProvider = currentPositionProvider,
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

                    // 4. Track Info & Favorite Row (Matching Screenshot 2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleLarge.copy(
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
                                    color = palette.accent,
                                    fontWeight = FontWeight.SemiBold,
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
                                    tintColor = if (track.isFavorite) Color.Red else palette.primary,
                                    tintAlpha = if (track.isFavorite) 0.35f else 0.12f
                                )
                        ) {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (track.isFavorite) Color(0xFFFF3366) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5. Liquid Glass Waveform Progress Bar
                    LiquidGlassProgressBar(
                        currentPositionProvider = currentPositionProvider,
                        durationMs = playbackState.durationMs,
                        palette = palette,
                        analysisDataProvider = analysisDataProvider,
                        onSeekTo = onSeekTo,
                        waveformEnvelope = waveformFloats,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 6. Playback Controls Capsule Bar (Matching Screenshot 2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(32.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = Color(0xFF0F111E),
                                tintAlpha = 0.65f
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    triggerHaptic()
                                    onToggleShuffle()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (playbackState.isShuffle) palette.accent else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    triggerHaptic()
                                    onPrevious()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Large Glowing Play/Pause Button
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(palette.primary, palette.accent)
                                        )
                                    )
                                    .clickable {
                                        triggerHaptic()
                                        onTogglePlay()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    triggerHaptic()
                                    onNext()
                                },
                                modifier = Modifier.size(48.dp)
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
                                    triggerHaptic()
                                    onCycleRepeat()
                                }
                            ) {
                                Icon(
                                    imageVector = when (playbackState.repeatMode) {
                                        PlaybackRepeatMode.ONE -> Icons.Default.RepeatOne
                                        PlaybackRepeatMode.ALL -> Icons.Default.Repeat
                                        PlaybackRepeatMode.OFF -> Icons.Default.Repeat
                                    },
                                    contentDescription = "Repeat",
                                    tint = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) palette.accent else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // 7. Volume Control Row
                    LiquidGlassVolumeControl(
                        palette = palette,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    // 8. Bottom Lyrics & Speed Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
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

        // Skin Selector Modal Bottom Sheet
        if (showSkinPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSkinPickerSheet = false },
                containerColor = Color(0xFF0F111E),
                scrimColor = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (appSettings.language == AppLanguage.PERSIAN) "انتخاب پوسته پلیر (Collector Skins)" else "Select Player Skin",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    skinOptions.forEach { skin ->
                        val isSelected = appSettings.selectedNowPlayingStyle == skin.id || (skin.id == "default" && appSettings.selectedNowPlayingStyle.isEmpty())
                        val isPremiumSkin = skin.id != "default"
                        val hasAccess = !isPremiumSkin || isVipUser || com.example.monetization.EntitlementManager.hasAccess(context, "now_playing_${skin.id.removePrefix("vinyl_")}")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) skin.previewColor.copy(alpha = 0.22f) else Color(0xFF171A2B))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) skin.previewColor else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    if (hasAccess) {
                                        triggerHaptic()
                                        onSelectNowPlayingStyle(skin.id)
                                        showSkinPickerSheet = false
                                    } else {
                                        showSkinPickerSheet = false
                                        onOpenVipPaywall("now_playing_${skin.id}")
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(skin.previewColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (appSettings.language == AppLanguage.PERSIAN) skin.titleFa else skin.titleEn,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = skin.previewColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else if (!hasAccess) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "VIP",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
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
private fun CollectorSkinHeader(
    title: String,
    palette: AmbientPalette,
    onCollapse: () -> Unit,
    onShare: () -> Unit,
    onOpenSkinPicker: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                    tintAlpha = 0.15f
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
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = palette.accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onOpenSkinPicker,
                modifier = Modifier
                    .size(42.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.THIN,
                        tintColor = palette.accent,
                        tintAlpha = 0.15f
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Change Skin",
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(42.dp)
                    .liquidGlass(
                        shape = CircleShape,
                        thickness = GlassThickness.THIN,
                        tintColor = palette.primary,
                        tintAlpha = 0.15f
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
    }
}
