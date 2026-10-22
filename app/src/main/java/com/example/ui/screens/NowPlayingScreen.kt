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

    // استخراج زنده و نرم رنگ‌های غالب کاور آهنگ (بدون هیچ رنگ نارنجی دستی!)
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

    // موشن شناوری سه‌بعدی کاور و هاله نوری پشت آن
    val infiniteTransition = rememberInfiniteTransition(label = "NowPlayingCoverMotion")
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 4200 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floatPhase"
    )

    val artworkBaseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "artworkPlayScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 55) {
                        triggerHaptic()
                        onPrevious()
                    } else if (dragAmount < -55) {
                        triggerHaptic()
                        onNext()
                    }
                }
            }
            .testTag("now_playing_screen")
    ) {
        // ۱. پس‌زمینه سینمایی با بلر عمیق کاور و امواج موشن‌بلر شده هم‌رنگ با کاور آهنگ
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
            // =========================================================================
            // ۲. هدر بالایی پلیر + نشانگر هوشمند ایرپاد/اسپیکر بلوتوثی
            // =========================================================================
            AnimatedVisibility(
                visible = !isImmersive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // دستگیره کپسولی بالای صفحه
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

                        // اگر ایرپاد یا اسپیکر بلوتوثی وصل باشد، با موشن‌گرافیک نمایش داده می‌شود
                        if (connectedDevice != null) {
                            AudioDeviceIndicator(
                                device = connectedDevice,
                                compact = false
                            )
                        } else {
                            // سوییچر شیشه‌ای بین کاور آهنگ و متن ترانه
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

            // =========================================================================
            // ۳. استیج مرکزی: کاور سه‌بعدی با هاله نوری محو و موشن‌بلر تپنده در پشت کاور
            // =========================================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                when (centerView) {
                    NowPlayingCenterView.ARTWORK_AND_HALO -> {
                        val audioData = analysisDataProvider()
                        val beatPulse = if (isPlaying) {
                            (audioData.kickPulse * 0.05f + audioData.haloExpansion * 0.03f).coerceIn(0f, 0.08f)
                        } else 0f

                        val animatedBeatPulse by animateFloatAsState(
                            targetValue = beatPulse,
                            animationSpec = spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            ),
                            label = "artworkBeatPulse"
                        )

                        val floatOffsetY = sin(floatPhase) * (if (isPlaying) 12f else 3f)

                        // الف) هاله‌های نوری کاملاً محو و بلر شده (Gaussian Blurred Aura) دقیقاً پشت کاور آهنگ
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = (size.height / 2f) + floatOffsetY
                            val baseR = size.minDimension * 0.42f * (1f + animatedBeatPulse * 1.8f)

                            drawIntoCanvas { canvas ->
                                // هاله اول: رنگ غالب کاور آهنگ با بلر فوق‌العاده نرم و موشن چرخشی
                                val paintPrimary = Paint().apply {
                                    asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        maskFilter = BlurMaskFilter(95f, BlurMaskFilter.Blur.NORMAL)
                                        color = coverPrimary.copy(alpha = if (isPlaying) 0.82f else 0.45f).toArgb()
                                    }
                                }
                                canvas.drawCircle(
                                    Offset(cx - 22f * cos(floatPhase), cy + 22f * sin(floatPhase)),
                                    baseR,
                                    paintPrimary
                                )

                                // هاله دوم: رنگ مکمل کاور آهنگ با حرکت معکوس برای ایجاد موشن‌بلر پشت کاور
                                val paintSecondary = Paint().apply {
                                    asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        maskFilter = BlurMaskFilter(85f, BlurMaskFilter.Blur.NORMAL)
                                        color = coverSecondary.copy(alpha = if (isPlaying) 0.72f else 0.35f).toArgb()
                                    }
                                }
                                canvas.drawCircle(
                                    Offset(cx + 24f * sin(floatPhase), cy - 24f * cos(floatPhase)),
                                    baseR * 0.90f,
                                    paintSecondary
                                )

                                // هاله سوم: درخشش مرکزی تاکیدی هنگام ضربه بیس
                                val paintAccent = Paint().apply {
                                    asFrameworkPaint().apply {
                                        isAntiAlias = true
                                        maskFilter = BlurMaskFilter(70f, BlurMaskFilter.Blur.NORMAL)
                                        color = coverAccent.copy(alpha = (0.35f + animatedBeatPulse * 4f).coerceIn(0.2f, 0.85f)).toArgb()
                                    }
                                }
                                canvas.drawCircle(
                                    Offset(cx, cy),
                                    baseR * 0.78f,
                                    paintAccent
                                )
                            }
                        }

                        // ب) قاب شیشه‌ای و سه‌بعدی خودِ کاور آهنگ با خط نوری هم‌رنگ کاور
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.76f)
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    val totalScale = artworkBaseScale + (animatedBeatPulse * 0.6f)
                                    scaleX = totalScale
                                    scaleY = totalScale
                                    translationY = floatOffsetY
                                    rotationX = sin(floatPhase) * (if (isPlaying) 3.0f else 0.5f)
                                    rotationY = cos(floatPhase) * (if (isPlaying) 3.0f else 0.5f)
                                }
                                .shadow(
                                    elevation = if (isPlaying) 36.dp else 18.dp,
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

            // =========================================================================
            // ۴. داک شیشه‌ای یکپارچه پایین صفحه (تغییر رنگ ۱۰۰٪ با کاور آهنگ + کنتراست تضمینی)
            // =========================================================================
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
                // الف) نام آهنگ، خواننده، موج انرژی زنده و دکمه قلب
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

                // ب) نوار پیشرفت هماهنگ با رنگ کاور آهنگ (بدون رنگ نارنجی دستی!)
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

                // ج) دکمه‌های کنترل پخش با کنتراست تضمین‌شده و گوی نوری هم‌رنگ کاور
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // دکمه Shuffle
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

                    // دکمه قبلی (Skip Previous)
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

                    // دکمه مرکزی Play / Pause که رنگش را مستقیم از کاور می‌گیرد و آیکونش همیشه ۱۰۰٪ خواناست
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

                    // دکمه بعدی (Skip Next)
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

                    // دکمه Repeat
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

                // د) اسلایدر صدا که رنگش با کاور آهنگ عوض می‌شود
                LiquidGlassVolumeControl(
                    palette = dynamicCoverPalette,
                    modifier = Modifier.fillMaxWidth()
                )

                // ه) نوار ابزار پایینی: [ Lyrics ] [ Speed ] [ Share ] [ Queue ]
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

/**
 * دکمه Play/Pause مرکزی با هاله بلر شده هم‌رنگ با کاور آهنگ و کنتراست هوشمند آیکون
 * (حتی اگر کاور آهنگ کاملاً سفید باشد، آیکون هرگز سفید تو سفید و ناپدید نمی‌شود)
 */
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

    // محاسبه روشنایی رنگ کاور برای اینکه اگر کاور خیلی روشن/سفید بود، آیکون تیره شود و هرگز گم نشود
    val luminance = (0.299f * primaryColor.red + 0.587f * primaryColor.green + 0.114f * primaryColor.blue)
    val iconColor = if (luminance > 0.72f) Color(0xFF090B14) else Color.White

    Box(
        modifier = Modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // هاله نوری بلر شده در پشت دکمه پلی
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
 * میله‌های اکولایزر زنده کنار نام آهنگ که با رنگ کاور آهنگ می‌درخشند
 */
@Composable
private fun LiveCoverEnergyBars(
    isPlaying: Boolean,
    primaryColor: Color,
    accentColor: Color
) {
    val transition = rememberInfiniteTransition(label = "CoverEnergyBars")
    val b1 by transition.animateFloat(0.25f, 1.0f, infiniteRepeatable(tween(320, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b1")
    val b2 by transition.animateFloat(0.35f, 0.95f, infiniteRepeatable(tween(240, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b2")
    val b3 by transition.animateFloat(0.20f, 1.0f, infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b3")
    val b4 by transition.animateFloat(0.30f, 0.90f, infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b4")

    val bars = if (isPlaying) listOf(b1, b2, b3, b4) else listOf(0.25f, 0.25f, 0.25f, 0.25f)

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
