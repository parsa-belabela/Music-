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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.rememberGyroscopeTilt
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
    loopStartMs: Long? = null,
    loopEndMs: Long? = null,
    onSetAbLoop: (Long?, Long?) -> Unit = { _, _ -> },
    onClearAbLoop: () -> Unit = {}
) {
    val context = LocalContext.current
    val track = playbackState.currentTrack ?: return
    var centerView by remember { mutableStateOf(NowPlayingCenterView.ARTWORK_AND_HALO) }
    var isImmersive by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRadialMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val (gyroRoll, gyroPitch) = rememberGyroscopeTilt()

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

    // موتور ۱۲۰ فریم بر ثانیه برای چرخش نرم قاب کاور (۳۵٪ سریع‌تر و کاملاً هماهنگ با ریتم و بیس آهنگ)
    var rotationPhaseRad by remember { mutableFloatStateOf(0f) }
    var currentAngularVelocity by remember { mutableFloatStateOf(0.45f) }
    var smoothBassLevel by remember { mutableFloatStateOf(0f) }
    var smoothKickImpact by remember { mutableFloatStateOf(0f) }
    var smoothEnergyLevel by remember { mutableFloatStateOf(0.2f) }

    val latestAnalysisProvider by rememberUpdatedState(analysisDataProvider)
    val trackBaseBpm = remember(audioProfile?.estimatedTempoBpm) {
        (audioProfile?.estimatedTempoBpm ?: 105).coerceIn(65, 175)
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
                val rawBass = if (isPlaying) audio.bass.coerceIn(0f, 1f) else 0f
                val rawKick = if (isPlaying) audio.kickPulse.coerceIn(0f, 1f) else 0f
                val rawEnergy = if (isPlaying) {
                    (audio.totalEnergy * 0.55f + rawBass * 0.45f).coerceIn(0.05f, 1f)
                } else 0.03f

                // واکنش سریع به ضربه بیس (Fast Attack) و برگشت نرم (Smooth Release) برای بالا و پایین رفتن کاور
                val bassLerp = if (rawBass > smoothBassLevel) (dtSec * 20f) else (dtSec * 8f)
                smoothBassLevel += (rawBass - smoothBassLevel) * bassLerp.coerceIn(0f, 1f)

                val kickLerp = if (rawKick > smoothKickImpact) (dtSec * 24f) else (dtSec * 9.5f)
                smoothKickImpact += (rawKick - smoothKickImpact) * kickLerp.coerceIn(0f, 1f)

                smoothEnergyLevel += (rawEnergy - smoothEnergyLevel) * (dtSec * 7.5f).coerceIn(0f, 1f)

                val targetAngularSpeed = if (isPlaying) {
                    val bpmFactor = (trackBaseBpm / 108f).coerceIn(0.65f, 1.75f)
                    // سرعت حدود ۳۵٪ بیشتر و وابسته به تمپو، انرژی و کیک آهنگ
                    0.48f + (smoothEnergyLevel * 1.98f * bpmFactor) + (smoothKickImpact * 1.25f) + (smoothBassLevel * 0.45f)
                } else {
                    0.08f
                }

                // تغییر کاملاً نرم و تدریجی سرعت چرخش
                currentAngularVelocity += (targetAngularSpeed - currentAngularVelocity) * (dtSec * 5.5f).coerceIn(0f, 1f)
                rotationPhaseRad = (rotationPhaseRad + currentAngularVelocity * dtSec) % (2f * PI.toFloat() * 100f)
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

    // حذف کامل ژست‌های لمسی تغییر آهنگ از روی صفحه تا کلیک در سمت راست، بالا یا پایین صفحه هرگز باعث رفتن به آهنگ بعدی نشود
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
            // ۱. هدر بالایی پلیر
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
                                    text = { Text(if (appSettings.language == AppLanguage.PERSIAN) "اشتراک‌گذاری کارت استوری" else "Share Story Card", color = Color.White) },
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

            // ۲. استیج مرکزی: چرخش سه‌بعدی قاب کاور + بالا و پایین شدن زنده و ریتمیک با بیس واقعی آهنگ
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                when (centerView) {
                    NowPlayingCenterView.ARTWORK_AND_HALO -> {
                        // محاسبه جهش عمودی (Up & Down Bass Bounce) مستقیماً بر اساس بیس و کیک واقعی آهنگ
                        // هر بار که بیس می‌کوبد، قاب کاور در حین چرخش با نرمی بالا و پایین می‌جهد
                        val bassVerticalBouncePx = if (isPlaying) {
                            val waveFloat = sin(rotationPhaseRad * 1.4f) * (6f + smoothBassLevel * 12f)
                            val bassLift = -(smoothBassLevel * 22f + smoothKickImpact * 18f)
                            waveFloat + bassLift
                        } else {
                            sin(rotationPhaseRad) * 3f
                        }

                        val dynamicTiltDeg = if (isPlaying) {
                            1.8f + (smoothEnergyLevel * 4.5f) + (smoothBassLevel * 2.5f)
                        } else 0.5f

                        // ذرات نوری معلق حساس به فرکانس‌های زیر و بم (Shader Starlight Dust)
                        ShaderStarlightDustLayer(
                            isPlaying = isPlaying,
                            analysisDataProvider = analysisDataProvider,
                            primaryColor = coverPrimary,
                            secondaryColor = coverSecondary,
                            accentColor = coverAccent,
                            modifier = Modifier.fillMaxSize()
                        )

                        // هاله نوری پشت کاور که همزمان با قاب کاور بالا و پایین می‌رود
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = (size.height / 2f) + bassVerticalBouncePx
                            val pulseExpand = 1f + (smoothBassLevel * 0.22f) + (smoothKickImpact * 0.18f)
                            val baseR = size.minDimension * 0.40f * pulseExpand

                            drawIntoCanvas { canvas ->
                                val paintPrimary = Paint().apply {
                                    asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        maskFilter = BlurMaskFilter(90f, BlurMaskFilter.Blur.NORMAL)
                                        color = coverPrimary.copy(alpha = if (isPlaying) 0.65f else 0.30f).toArgb()
                                    }
                                }
                                canvas.drawCircle(
                                    Offset(cx, cy),
                                    baseR,
                                    paintPrimary
                                )
                            }
                        }

                        // قاب سه‌بعدی کاور آهنگ (بدون ژست Swipe تا لمس کردن هیچ نقطه‌ای باعث پرش به آهنگ بعدی نشود)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.76f)
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    val bassScalePunch = if (isPlaying) {
                                        (smoothBassLevel * 0.045f + smoothKickImpact * 0.035f)
                                    } else 0f
                                    val totalScale = artworkBaseScale + bassScalePunch

                                    scaleX = totalScale
                                    scaleY = totalScale
                                    translationY = bassVerticalBouncePx
                                    rotationX = sin(rotationPhaseRad) * dynamicTiltDeg + (gyroPitch * 8.5f)
                                    rotationY = cos(rotationPhaseRad) * dynamicTiltDeg + (gyroRoll * 8.5f)
                                    rotationZ = sin(rotationPhaseRad * 0.5f) * (if (isPlaying) smoothBassLevel * 2.4f else 0f)
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

            // ۳. داک شیشه‌ای پایینی پلیر
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                            // خطوط متحرک کنار اسم آهنگ: بدون هیچ بک‌گراندی، با کنتراست ۱۰۰٪ که هیچ‌وقت محو نمی‌شوند
                            LiveCoverEnergyBars(
                                isPlaying = isPlaying,
                                bass = smoothBassLevel,
                                kick = smoothKickImpact,
                                primaryColor = coverPrimary,
                                accentColor = coverAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "${track.artist} • ${track.album}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.85f),
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
                    loopStartMs = loopStartMs,
                    loopEndMs = loopEndMs,
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

                    // دکمه حلقه تکرار A-B برای تمرین یا حفظ کردن بخشی از آهنگ
                    val isAbActive = loopStartMs != null
                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                thickness = GlassThickness.THIN,
                                tintColor = if (isAbActive) Color(0xFF00E5FF) else coverPrimary,
                                tintAlpha = if (isAbActive) 0.34f else 0.14f
                            )
                            .clickable {
                                triggerHaptic()
                                val nowPos = currentPositionProvider()
                                when {
                                    loopStartMs == null -> onSetAbLoop(nowPos, null)
                                    loopEndMs == null -> {
                                        if (nowPos > loopStartMs + 800L) {
                                            onSetAbLoop(loopStartMs, nowPos)
                                        } else {
                                            onSetAbLoop(loopStartMs, (loopStartMs + 5000L).coerceAtMost(dur))
                                        }
                                    }
                                    else -> onClearAbLoop()
                                }
                            }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        val abLabel = when {
                            loopStartMs == null -> "A-B"
                            loopEndMs == null -> "A→..."
                            else -> "A⟲B"
                        }
                        Text(
                            text = abLabel,
                            color = if (isAbActive) Color(0xFF00E5FF) else Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
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

/**
 * خطوط اکولایزر کنار نام آهنگ:
 * - بدون هیچ‌گونه کادر یا بک‌گراند اضافه (فقط خود خط‌ها)
 * - دارای حاشیه کنتراست تیره میکروسکوپی در پشت هر خط تا روی هر نوری (حتی سفید خالص) ۱۰۰٪ واضح بمانند و هرگز محو نشوند
 * - حرکت تند، زنده و کاملاً محسوس هماهنگ با بیس و ریتم آهنگ
 */
@Composable
private fun LiveCoverEnergyBars(
    isPlaying: Boolean,
    bass: Float,
    kick: Float,
    primaryColor: Color,
    accentColor: Color
) {
    val transition = rememberInfiniteTransition(label = "CrispEnergyBars")
    val w1 by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(tween(210, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val w2 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(165, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val w3 by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(250, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w3"
    )
    val w4 by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.96f,
        animationSpec = infiniteRepeatable(tween(190, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w4"
    )

    val boost = (bass * 0.35f + kick * 0.25f).coerceIn(0f, 0.45f)
    val heights = if (isPlaying) {
        listOf(
            (w1 * 0.75f + boost).coerceIn(0.20f, 1f),
            (w2 * 0.80f + boost).coerceIn(0.24f, 1f),
            (w3 * 0.75f + boost).coerceIn(0.20f, 1f),
            (w4 * 0.78f + boost).coerceIn(0.22f, 1f)
        )
    } else {
        listOf(0.22f, 0.22f, 0.22f, 0.22f)
    }

    Canvas(
        modifier = Modifier
            .width(22.dp)
            .height(18.dp)
    ) {
        val barCount = heights.size
        val barWidth = 3.2.dp.toPx()
        val spacing = (size.width - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

        heights.forEachIndexed { idx, ratio ->
            val barH = (size.height * ratio).coerceAtLeast(barWidth)
            val x = idx * (barWidth + spacing)
            val y = (size.height - barH) / 2f
            val corner = CornerRadius(barWidth / 2f, barWidth / 2f)

            // ۱. لایه محافظ کنتراست (Outline Shadow) دقیقاً هم‌شکل خودِ خط (بدون هیچ بک‌گراند مستطیلی)
            // باعث می‌شود حتی اگر نور پس‌زمینه کاملاً سفید یا هم‌رنگ خط باشد، خط هرگز محو نشود
            drawRoundRect(
                color = Color(0xCC05070E),
                topLeft = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()),
                size = Size(barWidth + 2.dp.toPx(), barH + 2.dp.toPx()),
                cornerRadius = CornerRadius((barWidth + 2.dp.toPx()) / 2f)
            )

            // ۲. خودِ خط نوری با هسته روشن و درخشان
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        accentColor,
                        primaryColor
                    ),
                    startY = y,
                    endY = y + barH
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = corner
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

/**
 * ذرات نوری معلق حساس به فرکانس‌های زیر و بم (Shader Starlight Dust)
 * با صدای خواننده و های‌هت‌ها (mid و high) در اطراف کاور شناور می‌شوند و با ضربه کیک به بیرون پرتاب می‌شوند.
 */
@Composable
private fun ShaderStarlightDustLayer(
    isPlaying: Boolean,
    analysisDataProvider: () -> AudioAnalysisData,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StarlightDust")
    val driftTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dustDrift"
    )

    var smoothMidHigh by remember { mutableFloatStateOf(0.15f) }
    var smoothKickBurst by remember { mutableFloatStateOf(0f) }
    val latestProvider by rememberUpdatedState(analysisDataProvider)

    LaunchedEffect(isPlaying) {
        var lastNanos = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now ->
                val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = now
                val audio = latestProvider()
                val targetMidHigh = if (isPlaying) {
                    (audio.mid * 0.55f + audio.high * 0.45f).coerceIn(0.05f, 1f)
                } else 0.04f
                val targetKick = if (isPlaying) audio.kickPulse.coerceIn(0f, 1f) else 0f

                smoothMidHigh += (targetMidHigh - smoothMidHigh) * (dt * 12f).coerceIn(0f, 1f)
                val kickLerp = if (targetKick > smoothKickBurst) (dt * 26f) else (dt * 6f)
                smoothKickBurst += (targetKick - smoothKickBurst) * kickLerp.coerceIn(0f, 1f)
            }
        }
    }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = size.minDimension * 0.34f
        val particleCount = 34

        for (i in 0 until particleCount) {
            val seed = i * 19.37f
            val ringFactor = 0.68f + ((i % 7) / 6f) * 0.72f
            val burstOutward = smoothKickBurst * (size.minDimension * 0.14f) * (0.5f + (i % 4) * 0.2f)
            val vocalFloat = sin(driftTime * (1.2f + (i % 3) * 0.4f) + seed) * (10f + smoothMidHigh * 26f)
            val angle = (i.toFloat() / particleCount.toFloat()) * (2f * PI.toFloat()) +
                driftTime * (if (i % 2 == 0) 0.45f else -0.35f)

            val r = baseRadius * ringFactor + burstOutward + vocalFloat
            val px = cx + cos(angle) * r
            val py = cy + sin(angle) * r

            val twinkle = (0.35f + 0.65f * ((sin(driftTime * 3f + seed) + 1f) * 0.5f))
            val alpha = ((0.18f + smoothMidHigh * 0.58f + smoothKickBurst * 0.24f) * twinkle).coerceIn(0.06f, 0.92f)
            val radiusPx = (1.6f + (i % 4) * 1.15f + smoothMidHigh * 2.2f + smoothKickBurst * 1.5f).dp.toPx()

            val particleColor = when (i % 3) {
                0 -> Color.White
                1 -> accentColor
                else -> secondaryColor
            }

            // هاله لطیف اطراف هر ذره نوری
            drawCircle(
                color = primaryColor.copy(alpha = alpha * 0.32f),
                radius = radiusPx * 2.4f,
                center = Offset(px, py)
            )
            // هسته درخشان ذره نوری
            drawCircle(
                color = particleColor.copy(alpha = alpha),
                radius = radiusPx,
                center = Offset(px, py)
            )
        }
    }
}
