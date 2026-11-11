package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.OnlineCacheHolder
import com.example.OnlineMusicRepository
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.audio.ConnectedAudioDevice
import com.example.data.model.*
import com.example.data.model.RepeatMode as PlaybackRepeatMode
import com.example.ui.components.*
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

enum class NowPlayingCenterView {
    ARTWORK_AND_HALO,
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
    currentPositionProvider: () -> Long = { playbackState.currentPositionMs },
    loopStartMs: Long? = null,
    loopEndMs: Long? = null,
    onSetAbLoop: (Long?, Long?) -> Unit = { _, _ -> },
    onClearAbLoop: () -> Unit = {},
    onDownloadTrack: (Track) -> Unit = {}
) {
    val context = LocalContext.current
    val track = playbackState.currentTrack ?: return
    val isFa = appSettings.language == AppLanguage.PERSIAN

    val downloadingIds by OnlineCacheHolder.downloadingTrackIds.collectAsState()
    val downloadedIds by OnlineCacheHolder.downloadedTrackIds.collectAsState()
    val downloadProgressMap by OnlineCacheHolder.downloadProgressMap.collectAsState()

    val isOnlineTrack = track.id.startsWith("online_") || track.uri.startsWith("http")
    val isDownloadedToPhone = downloadedIds.contains(track.id) ||
        downloadedIds.contains(track.id.removePrefix("online_")) ||
        OnlineCacheHolder.isTrackCached(context, track.id) ||
        (!isOnlineTrack && !track.uri.startsWith("http"))
    val isDownloadingNow = downloadingIds.contains(track.id) ||
        downloadingIds.contains(track.id.removePrefix("online_"))
    val currentDownloadProgress = downloadProgressMap[track.id]
        ?: downloadProgressMap[track.id.removePrefix("online_")]
        ?: 0

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

    val coverPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "coverPrimary"
    )
    val coverSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "coverSecondary"
    )
    val coverAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "coverAccent"
    )

    val dynamicCoverPalette = remember(coverPrimary, coverSecondary, coverAccent, palette) {
        palette.copy(
            primary = coverPrimary,
            secondary = coverSecondary,
            accent = coverAccent,
            haloGlow = coverPrimary.copy(alpha = 0.5f)
        )
    }

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
        audioProfile?.waveformEnvelope
            ?.split(",")
            ?.mapNotNull { it.trim().toFloatOrNull() }
            ?.toFloatArray()
    }

    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "artworkScale"
    )

    val coverInfiniteTransition = rememberInfiniteTransition(label = "coverLivingMotion")
    val rawSwayX by coverInfiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coverSwayX"
    )
    val rawSwayY by coverInfiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coverSwayY"
    )
    val rawCoverTilt by coverInfiniteTransition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coverTilt"
    )
    val haloBreath by coverInfiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloBreath"
    )
    val playingFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "playingMotionFactor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("now_playing_screen")
    ) {
        // Dynamic moving aurora & halo background derived from album artwork
        CinematicAtmosphereBackground(
            track = track,
            palette = dynamicCoverPalette,
            analysisDataProvider = analysisDataProvider,
            glowStrength = 1.0f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Bar (Spotify Header: Collapse Chevron, Playing Context, Prominent Lyrics Pill, More Menu)
            AnimatedVisibility(
                visible = !isImmersive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onCollapse()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("collapse_now_playing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = if (isFa) "در حال پخش از" else "PLAYING FROM ALBUM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.album.ifBlank { track.artist },
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Top Lyrics Toggle Button (Unified with Aura Design System)
                    val isLyricsOpen = centerView == NowPlayingCenterView.LYRICS
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isLyricsOpen) AuraSpotifyGreen
                                else AuraSurfaceElevated.copy(alpha = 0.72f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isLyricsOpen) AuraSpotifyGreen else Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                triggerHaptic()
                                centerView = if (isLyricsOpen) {
                                    NowPlayingCenterView.ARTWORK_AND_HALO
                                } else {
                                    NowPlayingCenterView.LYRICS
                                }
                            }
                            .padding(horizontal = 12.dp)
                            .testTag("top_lyrics_toggle_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (isLyricsOpen) Color.Black else AuraSpotifyGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isFa) "متن آهنگ" else "Lyrics",
                            color = if (isLyricsOpen) Color.Black else Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(AuraSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isFa) "تغییر کاور آهنگ" else "Change Song Artwork",
                                        color = Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isFa) "اکولایزر و تنظیمات صدا" else "Equalizer & DSP",
                                        color = Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenEqualizer()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isFa) "تایمر خواب" else "Sleep Timer",
                                        color = Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Snooze,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenSleepTimer()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isFa) "ویرایش اطلاعات آهنگ" else "Edit Metadata",
                                        color = Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenMetadataEditor(track)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isFa) "اشتراک‌گذاری کارت استوری" else "Share Story Card",
                                        color = Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onShareSong(track)
                                }
                            )
                        }
                    }
                }
            }

            // 2. Center Stage: Smoothly switches between Side Utility Rails + High-Res Album Artwork and Full Lyrics View
            val pillSurfaceBg = AuraSurfaceElevated.copy(alpha = 0.75f)
            val pillBorderColor = Color.White.copy(alpha = 0.12f)
            val durForLoop = if (playbackState.durationMs > 1000L) {
                playbackState.durationMs
            } else if (track.durationMs > 0) {
                track.durationMs
            } else {
                1L
            }

            val bigCoverUrl = remember(track.id, track.artworkUri, track.artist) {
                track.coverBig?.takeIf { it.isNotBlank() }
                    ?: OnlineMusicRepository.getBigCoverUrlForTrack(
                        trackId = track.id,
                        fallbackUrl = track.artworkUri,
                        artistName = track.artist
                    )
            }

            Crossfade(
                targetState = centerView,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                label = "nowPlayingCenterStage"
            ) { currentCenterMode ->
                when (currentCenterMode) {
                    NowPlayingCenterView.LYRICS -> {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            LyricsView(
                                lyrics = currentLyrics,
                                currentPositionProvider = currentPositionProvider,
                                displayMode = appSettings.lyricsDisplayMode,
                                palette = dynamicCoverPalette,
                                onSeekTo = onSeekTo,
                                onOpenEditor = onOpenLyricsEditor,
                                enableWordHighlight = appSettings.lyricsKaraokeWordHighlight,
                                baseFontSize = appSettings.lyricsFontSize,
                                activeTrackId = track.id,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    NowPlayingCenterView.ARTWORK_AND_HALO -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val glowMainColor = if (dynamicCoverPalette.isMonochrome) {
                                AuraSpotifyGreen
                            } else {
                                dynamicCoverPalette.primary.copy(alpha = 1f)
                            }
                            val glowSecColor = if (dynamicCoverPalette.isMonochrome) {
                                Color(0xFF14833B)
                            } else {
                                dynamicCoverPalette.accent.copy(alpha = 1f)
                            }

                            // Moving Luminous Backlight Halo directly behind the Cover
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            ) {
                                val cx = size.width / 2f + (rawSwayX * 2.2f * playingFactor)
                                val cy = size.height / 2f + (rawSwayY * 2.2f * playingFactor)
                                val baseRad = size.minDimension * 0.72f * haloBreath
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            glowMainColor.copy(alpha = 0.65f * (0.45f + 0.55f * playingFactor)),
                                            glowSecColor.copy(alpha = 0.30f * (0.45f + 0.55f * playingFactor)),
                                            Color.Transparent
                                        ),
                                        center = Offset(cx, cy),
                                        radius = baseRad.coerceAtLeast(100f)
                                    ),
                                    center = Offset(cx, cy),
                                    radius = baseRad.coerceAtLeast(100f)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .graphicsLayer {
                                        scaleX = artworkScale
                                        scaleY = artworkScale
                                        translationX = rawSwayX * playingFactor
                                        translationY = rawSwayY * playingFactor
                                        rotationZ = rawCoverTilt * playingFactor
                                    }
                                    .shadow(
                                        elevation = 28.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = glowMainColor,
                                        ambientColor = glowMainColor
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF181818))
                                    .pointerInput(track.id) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                triggerHaptic()
                                                onToggleFavorite(track)
                                                Toast.makeText(
                                                    context,
                                                    if (!track.isFavorite) {
                                                        if (isFa) "به پلی‌لیست My Liked Music اضافه شد" else "Added to My Liked Music"
                                                    } else {
                                                        if (isFa) "از پلی‌لیست My Liked Music حذف شد" else "Removed from My Liked Music"
                                                    },
                                                    Toast.LENGTH_SHORT
                                                ).show()
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
                                if (bigCoverUrl.isNotBlank()) {
                                    val highResRequest = com.example.rememberAuraCoverRequest(
                                        trackId = track.id,
                                        coverUrl = bigCoverUrl,
                                        isLarge = true
                                    )
                                    AsyncImage(
                                        model = highResRequest,
                                        contentDescription = track.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    MusicArtworkPlaceholder(
                                        title = track.title,
                                        artist = track.artist,
                                        trackId = track.id,
                                        palette = dynamicCoverPalette,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Bottom Player Controls Section (Spotify Architecture)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Track Title, Artist, Download & Like Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 22.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    if (isOnlineTrack) {
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                onDownloadTrack(track)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("download_online_track_button")
                        ) {
                            when {
                                isDownloadingNow -> {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = {
                                                (currentDownloadProgress / 100f).coerceIn(0.05f, 1f)
                                            },
                                            color = AuraSpotifyGreen,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                isDownloadedToPhone -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = AuraSpotifyGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.DownloadForOffline,
                                        contentDescription = "Download",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Spotify-style "Add to My Liked Music" Playlist Button (+) -> (✓)
                    val isLiked = track.isFavorite
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onToggleFavorite(track)
                            Toast.makeText(
                                context,
                                if (!isLiked) {
                                    if (isFa) "به پلی‌لیست My Liked Music اضافه شد" else "Added to My Liked Music"
                                } else {
                                    if (isFa) "از پلی‌لیست My Liked Music حذف شد" else "Removed from My Liked Music"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("spotify_i_liked_button")
                    ) {
                        if (isLiked) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = if (isFa) "در پلی‌لیست My Liked Music ذخیره شده" else "Added to My Liked Music",
                                tint = AuraSpotifyGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = if (isFa) "افزودن به پلی‌لیست My Liked Music" else "Add to My Liked Music",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Seek Bar
                val dur = if (playbackState.durationMs > 1000L) {
                    playbackState.durationMs
                } else if (track.durationMs > 0) {
                    track.durationMs
                } else {
                    1L
                }

                LiquidGlassProgressBar(
                    currentPositionProvider = currentPositionProvider,
                    durationMs = dur,
                    palette = dynamicCoverPalette,
                    analysisDataProvider = analysisDataProvider,
                    onSeekTo = onSeekTo,
                    waveformEnvelope = waveformFloats,
                    loopStartMs = loopStartMs,
                    loopEndMs = loopEndMs,
                    modifier = Modifier.fillMaxWidth()
                )

                // Main Transport Controls (Shuffle, Previous, Play/Pause, Next, Repeat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Button with Spotify Active Dot
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                onToggleShuffle()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (playbackState.isShuffle) AuraSpotifyGreen else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (playbackState.isShuffle) AuraSpotifyGreen else Color.Transparent)
                        )
                    }

                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onPrevious()
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    SpotifyPlayPauseButton(
                        isPlaying = isPlaying,
                        onClick = {
                            triggerHaptic()
                            onTogglePlay()
                        }
                    )

                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onNext()
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Repeat Button with Spotify Active Dot
                    val isRepeatActive = playbackState.repeatMode != PlaybackRepeatMode.OFF
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                onCycleRepeat()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            val repIcon = when (playbackState.repeatMode) {
                                PlaybackRepeatMode.ONE -> Icons.Default.RepeatOne
                                PlaybackRepeatMode.ALL -> Icons.Default.Repeat
                                PlaybackRepeatMode.OFF -> Icons.Default.Repeat
                            }
                            Icon(
                                imageVector = repIcon,
                                contentDescription = "Repeat",
                                tint = if (isRepeatActive) AuraSpotifyGreen else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(if (isRepeatActive) AuraSpotifyGreen else Color.Transparent)
                        )
                    }
                }

                // Spotify-Style Secondary Utilities Bar (Equalizer, Speed, A-B Loop, Sleep Timer, Share, Queue)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Equalizer
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onOpenEqualizer()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = AuraSpotifyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Playback Speed
                    val isCustomSpeed = appSettings.playbackSpeed != 1.0f
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isCustomSpeed) AuraSpotifyGreen.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                            .clickable {
                                triggerHaptic()
                                val nextSpeed = when (appSettings.playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                                onPlaybackSpeedChange(nextSpeed)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${appSettings.playbackSpeed}x",
                            color = if (isCustomSpeed) AuraSpotifyGreen else Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // A-B Loop
                    val isAbActive = loopStartMs != null
                    val abLabel = when {
                        loopStartMs == null -> "A-B"
                        loopEndMs == null -> "A→"
                        else -> "A⟲B"
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isAbActive) AuraSpotifyGreen.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                            .clickable {
                                triggerHaptic()
                                val nowPos = currentPositionProvider()
                                when {
                                    loopStartMs == null -> onSetAbLoop(nowPos, null)
                                    loopEndMs == null -> {
                                        if (nowPos > loopStartMs + 800L) {
                                            onSetAbLoop(loopStartMs, nowPos)
                                        } else {
                                            onSetAbLoop(
                                                loopStartMs,
                                                (loopStartMs + 5000L).coerceAtMost(durForLoop)
                                            )
                                        }
                                    }
                                    else -> onClearAbLoop()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = abLabel,
                            color = if (isAbActive) AuraSpotifyGreen else Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Sleep Timer
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onOpenSleepTimer()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Sleep Timer",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Share
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onShareSong(track)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Queue
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onOpenQueue()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                // Subtle Volume Slider
                LiquidGlassVolumeControl(
                    palette = dynamicCoverPalette,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showRadialMenu) {
            RadialActionMenu(
                track = track,
                palette = dynamicCoverPalette,
                hapticEnabled = appSettings.hapticFeedbackEnabled,
                onDismiss = { showRadialMenu = false },
                onPlayNext = { onNext() },
                onAddToQueue = { onOpenQueue() },
                onToggleFavorite = { onToggleFavorite(track) },
                onAddToPlaylist = { onOpenMetadataEditor(track) },
                onShare = { onShareSong(track) }
            )
        }

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
private fun SpotifyPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "playButtonScale"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .shadow(12.dp, CircleShape, spotColor = Color.Black)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("play_pause_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Black,
            modifier = Modifier.size(34.dp)
        )
    }
}

