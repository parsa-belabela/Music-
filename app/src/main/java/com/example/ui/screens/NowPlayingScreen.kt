package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.ui.text.TextStyle
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
    currentPositionProvider: () -> Long = { playbackState.currentPositionMs }
) {
    val context = LocalContext.current
    val track = playbackState.currentTrack ?: return
    var centerView by remember { mutableStateOf(NowPlayingCenterView.ARTWORK_AND_HALO) }
    var isImmersive by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRadialMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isFa = appSettings.language == AppLanguage.PERSIAN

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

    val highContrastAccent = remember(coverAccent) {
        val lum = 0.299f * coverAccent.red + 0.587f * coverAccent.green + 0.114f * coverAccent.blue
        if (lum < 0.35f) Color(0xFF00E5FF) else coverAccent
    }

    val dynamicCoverPalette = remember(coverPrimary, coverSecondary, highContrastAccent, palette) {
        palette.copy(
            primary = coverPrimary,
            secondary = coverSecondary,
            accent = highContrastAccent,
            haloGlow = coverPrimary.copy(alpha = 0.85f)
        )
    }

    // اشتراک‌گذاری مستقیم فایل یا متن معمولی آهنگ
    val shareRegularAudio = {
        val text = "🎵 ${track.title} — ${track.artist}\nAlbum: ${track.album}"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(sendIntent, if (isFa) "اشتراک‌گذاری آهنگ" else "Share Track"))
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onUpdateTrackArtwork(track, uri.toString())
        }
    }

    val waveformFloats = remember(audioProfile?.waveformEnvelope) {
        audioProfile?.waveformEnvelope?.split(",")?.mapNotNull { it.trim().toFloatOrNull() }?.toFloatArray()
    }

    var dynamicPhaseRad by remember { mutableFloatStateOf(0f) }
    var smoothReactiveEnergy by remember { mutableFloatStateOf(0.35f) }
    var smoothReactiveKick by remember { mutableFloatStateOf(0.1f) }

    val latestAnalysisProvider by rememberUpdatedState(analysisDataProvider)
    val trackBaseBpm = remember(audioProfile?.estimatedTempoBpm) {
        (audioProfile?.estimatedTempoBpm ?: 105).coerceIn(60, 180)
    }
    val trackBaseEnergy = remember(audioProfile?.energyLevel) {
        (audioProfile?.energyLevel ?: 0.55f).coerceIn(0.20f, 0.95f)
    }

    LaunchedEffect(isPlaying, track.id) {
        var lastFrameNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { nowNanos ->
                val dtSec = ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastFrameNanos = nowNanos

                val audio = latestAnalysisProvider()
                val targetEnergy = if (isPlaying) {
                    (audio.totalEnergy * 0.55f + audio.mid * 0.25f + trackBaseEnergy * 0.20f).coerceIn(0.20f, 1.0f)
                } else {
                    0.08f
                }

                smoothReactiveEnergy += (targetEnergy - smoothReactiveEnergy) * (dtSec * 6f).coerceIn(0f, 1f)

                val targetKick = if (isPlaying) (audio.kickPulse * 0.8f + audio.bass * 0.2f).coerceIn(0f, 1f) else 0f
                smoothReactiveKick += (targetKick - smoothReactiveKick) * (dtSec * 12f).coerceIn(0f, 1f)

                if (isPlaying) {
                    val bpmFactor = (trackBaseBpm / 105f).coerceIn(0.65f, 1.65f)
                    val angularVelocity = (0.55f + (smoothReactiveEnergy * 1.85f * bpmFactor))
                    dynamicPhaseRad = (dynamicPhaseRad + angularVelocity * dtSec) % (2f * PI.toFloat() * 100f)
                } else {
                    dynamicPhaseRad = (dynamicPhaseRad + 0.12f * dtSec) % (2f * PI.toFloat() * 100f)
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
                            .background(Color.White.copy(alpha = 0.45f))
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
                                    tintAlpha = 0.22f
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
                                        tintAlpha = 0.22f
                                    )
                                    .padding(3.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SegmentPillItem(
                                        title = if (isFa) "کاور آهنگ" else "Artwork",
                                        isSelected = centerView == NowPlayingCenterView.ARTWORK_AND_HALO,
                                        activeColor = highContrastAccent,
                                        onClick = {
                                            triggerHaptic()
                                            centerView = NowPlayingCenterView.ARTWORK_AND_HALO
                                        }
                                    )
                                    SegmentPillItem(
                                        title = if (isFa) "متن آهنگ" else "Lyrics",
                                        isSelected = centerView == NowPlayingCenterView.LYRICS,
                                        activeColor = highContrastAccent,
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
                                        tintAlpha = 0.22f
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
                                        tintAlpha = 0.28f,
                                        borderWidth = 1.dp
                                    )
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isFa) "تغییر کاور آهنگ" else "Change Song Artwork", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = highContrastAccent) },
                                    onClick = {
                                        showMoreMenu = false
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isFa) "کارت استوری اینستاگرام (Spotify Card)" else "Share Story Card", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = highContrastAccent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onShareSong(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isFa) "اشتراک‌گذاری معمولی آهنگ" else "Share Audio / Info", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = highContrastAccent) },
                                    onClick = {
                                        showMoreMenu = false
                                        shareRegularAudio()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isFa) "اکولایزر و تنظیمات صدا" else "Equalizer & DSP", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = highContrastAccent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenEqualizer()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isFa) "تایمر خواب" else "Sleep Timer", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Snooze, contentDescription = null, tint = highContrastAccent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenSleepTimer()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isFa) "ویرایش اطلاعات آهنگ" else "Edit Metadata", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = highContrastAccent) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenMetadataEditor(track)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // استیج مرکزی کاور با نور متحرک هماهنگ با فضای کلی آهنگ
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

                        // محاسبه حرکت عمودی بالا و پایین (Vertical Bobbing) متناسب با حال و هوای آهنگ:
                        // در آهنگ آرام: نوسان بسیار نرم و کوتاه | در آهنگ پرانرژی: نوسان زندهتر ولی کنترلشده
                        val moodIntensity = (smoothReactiveEnergy * 0.7f + smoothReactiveKick * 0.3f).coerceIn(0.08f, 1f)
                        val verticalBobbingPx = if (isPlaying) {
                            (sin(dynamicPhaseRad * 1.15f) * (5.5f + moodIntensity * 9.5f)) +
                                (cos(dynamicPhaseRad * 2.3f) * (smoothReactiveKick * 5.0f))
                        } else {
                            sin(dynamicPhaseRad) * 2.0f
                        }

                        val dynamicTiltAmplitude = 1.0f + (moodIntensity * 4.2f)

                        // هاله نوری قوی و کاملاً همرنگ با کاور آهنگ در اطراف و پشت کاور
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = (size.height / 2f) + verticalBobbingPx
                            val haloScale = 1f + (moodIntensity * 0.22f) + (smoothReactiveKick * 0.16f)
                            val baseR = size.minDimension * 0.44f * haloScale

                            drawIntoCanvas { canvas ->
                                // لایه اول: رنگ اصلی کاور با غلظت بالا دقیقاً دور کاور
                                val primaryPaint = Paint().apply {
                                    asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        maskFilter = BlurMaskFilter(105f, BlurMaskFilter.Blur.NORMAL)
                                        color = coverPrimary.copy(alpha = if (isPlaying) 0.88f else 0.50f).toArgb()
                                    }
                                }
                                canvas.drawCircle(
                                    Offset(cx, cy),
                                    baseR * 1.05f,
                                    primaryPaint
                                )

                                // لایه دوم: رنگ دوم کاور که همراه با کاور بالا و پایین میرود
                                val secondaryPaint = Paint().apply {
                                    asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        maskFilter = BlurMaskFilter(90f, BlurMaskFilter.Blur.NORMAL)
                                        color = coverSecondary.copy(alpha = if (isPlaying) 0.78f else 0.40f).toArgb()
                                    }
                                }
                                canvas.drawCircle(
                                    Offset(cx + 18f * cos(dynamicPhaseRad * 0.7f), cy + 16f * sin(dynamicPhaseRad * 0.7f)),
                                    baseR * 0.92f,
                                    secondaryPaint
                                )
                            }
                        }

                        // قاب سهبعدی کاور با حرکت بالا و پایین ملایم و چرخش هماهنگ با موزیک
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.76f)
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    val beatScaleBoost = smoothReactiveKick * 0.032f + moodIntensity * 0.015f
                                    val totalScale = artworkBaseScale + (if (isPlaying) beatScaleBoost else 0f)
                                    scaleX = totalScale
                                    scaleY = totalScale
                                    translationY = verticalBobbingPx
                                    translationX = cos(dynamicPhaseRad * 0.6f) * (if (isPlaying) 3.5f * moodIntensity else 0f)
                                    rotationX = sin(dynamicPhaseRad) * (if (isPlaying) dynamicTiltAmplitude else 0.4f)
                                    rotationY = cos(dynamicPhaseRad) * (if (isPlaying) dynamicTiltAmplitude else 0.4f)
                                    rotationZ = sin(dynamicPhaseRad * 0.5f) * (if (isPlaying) moodIntensity * 1.8f else 0f)
                                }
                                .shadow(
                                    elevation = if (isPlaying) 36.dp else 18.dp,
                                    shape = RoundedCornerShape(32.dp),
                                    spotColor = coverPrimary,
                                    ambientColor = coverSecondary
                                )
                                .border(
                                    width = 2.2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            coverPrimary,
                                            Color.White.copy(alpha = 0.75f),
                                            coverSecondary,
                                            coverAccent
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
                                        onDragCancel = { artworkSwipeAccum = 0f }
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
                                    thickness = GlassThickness.THICK,
                                    tintColor = coverPrimary,
                                    tintAlpha = 0.22f,
                                    secondaryAccent = highContrastAccent
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

            // داک پایینی با کنتراست ۱۰۰٪ تضمین‌شده برای تمام متن‌ها، خطوط متحرک و آیکن‌ها
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(32.dp),
                        thickness = GlassThickness.THICK,
                        tintColor = coverPrimary,
                        tintAlpha = 0.25f,
                        borderWidth = 1.4.dp,
                        secondaryAccent = highContrastAccent
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.85f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 6f
                                    )
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            // خطوط متحرک کنار اسم آهنگ داخل کپسول تیره ضد محو شدن با حرکت پرانرژی و کاملاً محسوس
                            LiveCoverEnergyBars(
                                isPlaying = isPlaying,
                                moodEnergy = smoothReactiveEnergy,
                                accentColor = highContrastAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "${track.artist} • ${track.album}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 4f
                                )
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
                                tintAlpha = if (track.isFavorite) 0.35f else 0.18f
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
                                tintColor = if (playbackState.isShuffle) highContrastAccent else coverPrimary,
                                tintAlpha = if (playbackState.isShuffle) 0.35f else 0.16f
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffle) highContrastAccent else Color.White,
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
                                tintAlpha = 0.22f
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
                        accentColor = highContrastAccent,
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
                                tintAlpha = 0.22f
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
                                tintColor = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) highContrastAccent else coverPrimary,
                                tintAlpha = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) 0.35f else 0.16f
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
                            tint = if (playbackState.repeatMode != PlaybackRepeatMode.OFF) highContrastAccent else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                LiquidGlassVolumeControl(
                    palette = dynamicCoverPalette,
                    modifier = Modifier.fillMaxWidth()
                )

                // ردیف ابزارهای پایین شامل هر دو قابلیت: ۱. کارت استوری گرافیکی (Story Card) ۲. اشتراک‌گذاری معمولی آهنگ
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
                                tintColor = if (centerView == NowPlayingCenterView.LYRICS) highContrastAccent else coverPrimary,
                                tintAlpha = if (centerView == NowPlayingCenterView.LYRICS) 0.35f else 0.18f
                            )
                            .clickable {
                                triggerHaptic()
                                centerView = if (centerView == NowPlayingCenterView.LYRICS) {
                                    NowPlayingCenterView.ARTWORK_AND_HALO
                                } else {
                                    NowPlayingCenterView.LYRICS
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isFa) "متن" else "Lyrics",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // دکمه اختصاصی ساخت و اشتراک‌گذاری کارت استوری گرافیکی (مشابه Spotify Story Card)
                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                thickness = GlassThickness.THIN,
                                tintColor = highContrastAccent,
                                tintAlpha = 0.28f
                            )
                            .clickable {
                                triggerHaptic()
                                onShareSong(track)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Story Card",
                                tint = highContrastAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isFa) "کارت استوری" else "Story Card",
                                color = Color.White,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // دکمه اشتراک‌گذاری معمولی آهنگ
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            shareRegularAudio()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = coverPrimary,
                                tintAlpha = 0.18f
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Audio",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // دکمه سرعت پخش
                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                thickness = GlassThickness.THIN,
                                tintColor = coverPrimary,
                                tintAlpha = 0.18f
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
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "${appSettings.playbackSpeed}x",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // دکمه صف پخش
                    IconButton(
                        onClick = {
                            triggerHaptic()
                            onOpenQueue()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .liquidGlass(
                                shape = CircleShape,
                                thickness = GlassThickness.THIN,
                                tintColor = coverPrimary,
                                tintAlpha = 0.18f
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
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
    val iconColor = if (luminance > 0.60f) Color(0xFF070912) else Color.White

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
                        color = primaryColor.copy(alpha = 0.78f).toArgb()
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
                            accentColor.copy(alpha = 0.92f),
                            secondaryColor
                        )
                    )
                )
                .border(
                    width = 1.8.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.92f),
                            Color.White.copy(alpha = 0.30f)
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

/**
 * خطوط متحرک کنار اسم آهنگ:
 * - دارای کپسول پس‌زمینه تیره و حاشیه نوری تا هرگز با نورهای پس‌زمینه محو نشوند
 * - دارای حرکت پرسرعت، واضح و کاملاً محسوس هماهنگ با فضای آهنگ
 */
@Composable
private fun LiveCoverEnergyBars(
    isPlaying: Boolean,
    moodEnergy: Float,
    accentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FastVividEqualizerBars")
    val speedMultiplier = (0.75f + moodEnergy * 0.65f).coerceIn(0.75f, 1.4f)

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween((210 / speedMultiplier).toInt().coerceAtLeast(120), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween((170 / speedMultiplier).toInt().coerceAtLeast(100), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween((250 / speedMultiplier).toInt().coerceAtLeast(130), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val anim4 by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween((190 / speedMultiplier).toInt().coerceAtLeast(110), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    val bars = if (isPlaying) listOf(anim1, anim2, anim3, anim4) else listOf(0.22f, 0.35f, 0.22f, 0.30f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xD9070912))
            .border(1.dp, accentColor.copy(alpha = 0.70f), RoundedCornerShape(10.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.height(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            bars.forEach { ratio ->
                Box(
                    modifier = Modifier
                        .width(3.2.dp)
                        .fillMaxHeight(ratio.coerceIn(0.18f, 1f))
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White, accentColor)
                            )
                        )
                )
            }
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
    val selectedTextColor = if (lum > 0.62f) Color(0xFF070912) else Color.White

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
            color = if (isSelected) selectedTextColor else Color.White.copy(alpha = 0.85f),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}
