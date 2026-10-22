package com.example.ui.screens

import android.graphics.BlurMaskFilter
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    currentPositionMs: Long? = null
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

    // رنگ‌های زنده مستخرج از کاور آهنگ
    val coverPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "coverPrimary"
    )
    val coverSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "coverSecondary"
    )
    val coverAccent by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "coverAccent"
    )

    val dynamicCoverPalette = remember(coverPrimary, coverSecondary, coverAccent, palette) {
        palette.copy(
            primary = coverPrimary,
            secondary = coverSecondary,
            accent = coverAccent,
            haloGlow = coverPrimary.copy(alpha = 0.85f)
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
        audioProfile?.waveformEnvelope?.split(",")?.mapNotNull { it.trim().toFloatOrNull() }?.toFloatArray()
    }

    // موتور فیزیک 120 FPS (VSYNC Frame-Paced) برای چرخش و حرکت کاور متناسب با ریتم، تمپو و انرژی واقعی آهنگ
    var dynamicPhaseRad by remember { mutableFloatStateOf(0f) }
    var smoothReactiveEnergy by remember { mutableFloatStateOf(0.2f) }
    var smoothReactiveKick by remember { mutableFloatStateOf(0f) }

    val latestAnalysisProvider by rememberUpdatedState(analysisDataProvider)
    val trackBaseBpm = remember(audioProfile?.estimatedTempoBpm) {
        (audioProfile?.estimatedTempoBpm ?: 100).coerceIn(60, 180)
    }
    val trackBaseEnergy = remember(audioProfile?.energyLevel) {
        (audioProfile?.energyLevel ?: 0.5f).coerceIn(0.15f, 0.95f)
    }

    LaunchedEffect(isPlaying, track.id) {
        var lastFrameNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { nowNanos ->
                val dtSec = ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastFrameNanos = nowNanos

                val audio = latestAnalysisProvider()
                val targetEnergy = if (isPlaying) {
                    (audio.totalEnergy * 0.55f + audio.bass * 0.30f + trackBaseEnergy * 0.15f).coerceIn(0.05f, 1.0f)
                } else {
                    0.04f
                }

                // نرم‌سازی ۱۲۰ فریم بر ثانیه بدون کوچک‌ترین لگ
                smoothReactiveEnergy += (targetEnergy - smoothReactiveEnergy) * (dtSec * 8f).coerceIn(0f, 1f)
                smoothReactiveKick += (audio.kickPulse - smoothReactiveKick) * (dtSec * 14f).coerceIn(0f, 1f)

                if (isPlaying) {
                    val bpmFactor = (trackBaseBpm / 110f).coerceIn(0.55f, 1.65f)
                    val angularVelocity = (0.28f + (smoothReactiveEnergy * 1.55f * bpmFactor) + (smoothReactiveKick * 1.15f))
                    dynamicPhaseRad = (dynamicPhaseRad + angularVelocity * dtSec) % (2f * PI.toFloat() * 100f)
                } else {
                    dynamicPhaseRad = (dynamicPhaseRad + 0.08f * dtSec) % (2f * PI.toFloat() * 100f)
                }
            }
        }
    }

    val artworkBaseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "artworkPlayScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("now_playing_screen")
    ) {
        // ۱. پس‌زمینه آرام و حساس به قدرت واقعی بیس و کیک
        CinematicAtmosphereBackground(
            track = track,
            palette = dynamicCoverPalette,
            analysisDataProvider = analysisDataProvider,
            glowStrength = if (isImmersive) 1.15f else 1.0f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ۲. هدر بالایی پلیر
            AnimatedVisibility(
                visible = !isImmersive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.38f))
                    )

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
                                .size(42.dp)
                                .liquidGlass(
                                    shape = CircleShape,
                                    thickness = GlassThickness.THIN,
                                    tintColor = coverPrimary,
                                    tintAlpha = 0.18f
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

                        if (connectedDevice != null) {
                            AudioDeviceIndicator(
                                device = connectedDevice,
                                compact = false
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .liquidGlass(
                                        shape = RoundedCornerShape(28.dp),
                                        thickness = GlassThickness.THIN,
                                        tintColor = coverPrimary,
                                        tintAlpha = 0.16f
                                    )
                                    .padding(3.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SegmentPillItem(
                                        title = if (appSettings.language == AppLanguage.PERSIAN) "کاور آهنگ" else "Artwork",
                                        isSelected = centerView == NowPlayingCenterView.ARTWORK_AND_HALO,
                                        activeColor = coverPrimary,
                                        onClick = {
                                            triggerHaptic()
                                            centerView = NowPlayingCenterView.ARTWORK_AND_HALO
                                        }
                                    )
                                    SegmentPillItem(
                                        title = if (appSettings.language == AppLanguage.PERSIAN) "متن آهنگ" else "Lyrics",
                                        isSelected = centerView == NowPlayingCenterView.LYRICS,
                                        activeColor = coverPrimary,
                                        onClick = {
                                            triggerHaptic()
                                            centerView = NowPlayingCenterView.LYRICS
                                        }
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
                                        tintColor = coverPrimary,
                                        tintAlpha = 0.18f
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
                                modifier = Modifier
                                    .liquidGlass(
                                        shape = RoundedCornerShape(18.dp),
                                        thickness = GlassThickness.THICK,
                                        tintColor = coverPrimary,
                                        tintAlpha = 0.25f,
                                        borderWidth = 1.dp
                                    )
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "تغییر کاور آهنگ" else "Change Song Artwork", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = coverAccent) },
                                    onClick = {
                                        showMoreMenu = false
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "اکولایزر و تنظیمات صدا" else "Equalizer & DSP", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = coverAccent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenEqualizer()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "تایمر خواب" else "Sleep Timer", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null, tint = coverAccent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenSleepTimer()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "ویرایش اطلاعات آهنگ" else "Edit Metadata", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = coverAccent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenMetadataEditor(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "اشتراک‌گذاری استوری" else "Share Social Card", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = coverAccent) },
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

            // ۳. استیج کاور با چرخش ۱۲۰ فریم هماهنگ با ریتم واقعی آهنگ + هاله نوری حساس به کیک و بیس
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                when (centerView) {
                    NowPlayingCenterView.ARTWORK_AND_HALO -> {
                        var artworkSwipeAccum by remember { mutableFloatStateOf(0f) }

                        val dynamicTiltAmplitude = 1.2f + (smoothReactiveEnergy * 4.8f) + (smoothReactiveKick * 2.2f)
                        val floatOffsetY = sin(dynamicPhaseRad) * (3f + smoothReactiveEnergy * 11f)

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = (size.height / 2f) + floatOffsetY
                            val realPulseExpand = 1f + (smoothReactiveEnergy * 0.18f) + (smoothReactiveKick * 0.22f)
                            val baseR = size.minDimension * 0.40f * realPulseExpand

                            val primaryAlpha = (0.30f + smoothReactiveEnergy * 0.42f + smoothReactiveKick * 0.25f).coerceIn(0.22f, 0.92f)
                            val secondaryAlpha = (0.24f + smoothReactiveEnergy * 0.38f + smoothReactiveKick * 0.20f).coerceIn(0.18f, 0.85f)

                            drawIntoCanvas { canvas ->
                                val paintPrimary = Paint().apply {
                                    asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        maskFilter = BlurMaskFilter(95f, BlurMaskFilter.Blur.NORMAL)
                                        color = coverPrimary.copy(alpha = if (isPlaying) primaryAlpha else 0.28f).toArgb()
                                    }
                                }
                                canvas.drawCircle(
                                    Offset(cx - 16f * cos(dynamicPhaseRad * 0.5f), cy + 16f * sin(dynamicPhaseRad * 0.5f)),
                                    baseR,
                                    paintPrimary
                                )

                                val paintSecondary = Paint().apply {
                                    asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        maskFilter = BlurMaskFilter(85f, BlurMaskFilter.Blur.NORMAL)
                                        color = coverSecondary.copy(alpha = if (isPlaying) secondaryAlpha else 0.22f).toArgb()
                                    }
                                }
                                canvas.drawCircle(
                                    Offset(cx + 18f * sin(dynamicPhaseRad * 0.5f), cy - 18f * cos(dynamicPhaseRad * 0.5f)),
                                    baseR * 0.90f,
                                    paintSecondary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.76f)
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    val beatScaleBoost = smoothReactiveKick * 0.045f + smoothReactiveEnergy * 0.02f
                                    val totalScale = artworkBaseScale + (if (isPlaying) beatScaleBoost else 0f)
                                    scaleX = totalScale
                                    scaleY = totalScale
                                    translationY = floatOffsetY
                                    rotationX = sin(dynamicPhaseRad) * (if (isPlaying) dynamicTiltAmplitude else 0.5f)
                                    rotationY = cos(dynamicPhaseRad) * (if (isPlaying) dynamicTiltAmplitude else 0.5f)
                                    rotationZ = sin(dynamicPhaseRad * 0.5f) * (if (isPlaying) smoothReactiveEnergy * 2.2f else 0f)
                                }
                                .shadow(
                                    elevation = if (isPlaying) 34.dp else 16.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    spotColor = coverPrimary,
                                    ambientColor = coverSecondary
                                )
                                .border(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.85f),
                                            coverPrimary,
                                            coverAccent,
                                            Color.White.copy(alpha = 0.65f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(32.dp)
                                )
                                .padding(3.dp)
                                .clip(RoundedCornerShape(29.dp))
                                .background(Color(0xFF10121E))
                                .pointerInput(track.id) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            artworkSwipeAccum += dragAmount
                                        },
                                        onDragEnd = {
                                            if (artworkSwipeAccum > 110f) {
                                                triggerHaptic()
                                                onPrevious()
                                            } else if (artworkSwipeAccum < -110f) {
                                                triggerHaptic()
                                                onNext()
                                            }
                                            artworkSwipeAccum = 0f
                                        },
                                        onDragCancel = {
                                            artworkSwipeAccum = 0f
                                        }
                                    )
                                }
                                .pointerInput(track.id) {
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

                    NowPlayingCenterView.LYRICS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .liquidGlass(
                                    shape = RoundedCornerShape(30.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = coverPrimary,
                                    tintAlpha = 0.18f,
                                    secondaryAccent = coverAccent
                                )
                                .padding(14.dp)
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
                }
            }

            // ۴. داک شیشه‌ای پایینی
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(32.dp),
                        thickness = GlassThickness.THICK,
                        tintColor = coverPrimary,
                        tintAlpha = 0.22f,
                        borderWidth = 1.4.dp,
                        secondaryAccent = coverAccent
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 20.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            LiveCoverEnergyBars(
                                isPlaying = isPlaying,
                                energy = smoothReactiveEnergy,
                                kick = smoothReactiveKick,
                                primaryColor = coverPrimary,
                                accentColor = coverAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "${track.artist} • ${track.album}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onToggleFavorite(track)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = if (track.isFavorite) Color(0xFFFF1744) else coverPrimary,
                                tintAlpha = if (track.isFavorite) 0.35f else 0.16f
                            )
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (track.isFavorite) Color(0xFFFF1744) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                val dur = if (track.durationMs > 0) track.durationMs else 1L
                LiquidGlassProgressBar(
                    currentPositionProvider = currentPositionProvider,
                    durationMs = dur,
                    palette = dynamicCoverPalette,
                    analysisDataProvider = analysisDataProvider,
                    onSeekTo = onSeekTo,
                    waveformEnvelope = waveformFloats,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onToggleShuffle()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = if (playbackState.isShuffle) coverAccent else coverPrimary,
                                tintAlpha = if (playbackState.isShuffle) 0.30f else 0.12f
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffle) coverAccent else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onPrevious()
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = coverPrimary,
                                tintAlpha = 0.18f
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    CoverReactivePlayButton(
                        isPlaying = isPlaying,
                        primaryColor = coverPrimary,
                        secondaryColor = coverSecondary,
                        accentColor = coverAccent,
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
                        modifier = Modifier
                            .size(50.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = coverPrimary,
                                tintAlpha = 0.18f
                            )
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
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) coverAccent else coverPrimary,
                                tintAlpha = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) 0.30f else 0.12f
                            )
                    ) {
                        val repIcon = when (playbackState.repeatMode) {
                            PlaybackRepeatMode.ONE -> Icons.Default.RepeatOne
                            PlaybackRepeatMode.ALL -> Icons.Default.Repeat
                            PlaybackRepeatMode.OFF -> Icons.Default.Repeat
                        }
                        Icon(
                            imageVector = repIcon,
                            contentDescription = "Repeat",
                            tint = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) coverAccent else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                LiquidGlassVolumeControl(
                    palette = dynamicCoverPalette,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                thickness = GlassThickness.THIN,
                                tintColor = if (centerView == NowPlayingCenterView.LYRICS) coverAccent else coverPrimary,
                                tintAlpha = if (centerView == NowPlayingCenterView.LYRICS) 0.32f else 0.14f
                            )
                            .clickable {
                                triggerHaptic()
                                centerView = if (centerView == NowPlayingCenterView.LYRICS) {
                                    NowPlayingCenterView.ARTWORK_AND_HALO
                                } else {
                                    NowPlayingCenterView.LYRICS
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (appSettings.language == AppLanguage.PERSIAN) "متن ترانه" else "Lyrics",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                thickness = GlassThickness.THIN,
                                tintColor = coverPrimary,
                                tintAlpha = 0.14f
                            )
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
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "${appSettings.playbackSpeed}x",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onShareSong(track)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = coverPrimary,
                                tintAlpha = 0.14f
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onOpenQueue()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = coverPrimary,
                                tintAlpha = 0.14f
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
private fun CoverReactivePlayButton(
    isPlaying: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "PlayOrbScale"
    )

    val luminance = (0.299f * primaryColor.red + 0.587f * primaryColor.green + 0.114f * primaryColor.blue)
    val iconColor = if (luminance > 0.72f) Color(0xFF090B14) else Color.White

    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val glowPaint = Paint().apply {
                    asFrameworkPaint().apply {
                        isAntiAlias = true
                        maskFilter = BlurMaskFilter(32f, BlurMaskFilter.Blur.NORMAL)
                        color = primaryColor.copy(alpha = 0.75f).toArgb()
                    }
                }
                canvas.drawCircle(
                    Offset(size.width / 2f, size.height / 2f),
                    size.minDimension / 2.1f,
                    glowPaint
                )
            }
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primaryColor,
                            accentColor.copy(alpha = 0.90f),
                            secondaryColor
                        )
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.25f)
                        )
                    ),
                    shape = CircleShape
                )
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
                tint = iconColor,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun LiveCoverEnergyBars(
    isPlaying: Boolean,
    energy: Float,
    kick: Float,
    primaryColor: Color,
    accentColor: Color
) {
    val bars = if (isPlaying) {
        listOf(
            (0.22f + energy * 0.65f + kick * 0.25f).coerceIn(0.2f, 1f),
            (0.30f + kick * 0.60f + energy * 0.35f).coerceIn(0.2f, 1f),
            (0.18f + energy * 0.75f).coerceIn(0.2f, 1f),
            (0.25f + kick * 0.45f + energy * 0.45f).coerceIn(0.2f, 1f)
        )
    } else {
        listOf(0.22f, 0.22f, 0.22f, 0.22f)
    }

    Row(
        modifier = Modifier.height(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        bars.forEach { ratio ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(ratio)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(accentColor, primaryColor)))
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
    val lum = (0.299f * activeColor.red + 0.587f * activeColor.green + 0.114f * activeColor.blue)
    val selectedTextColor = if (lum > 0.72f) Color(0xFF090B14) else Color.White

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 15.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) selectedTextColor else Color(0xFFB0B6CA),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}
