package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.audio.AudioAnalysisData
import com.example.audio.ArtworkPaletteExtractor
import com.example.audio.ConnectedAudioDevice
import com.example.data.model.AppSettings
import com.example.data.model.LyricsLine
import com.example.data.model.PlaybackState
import com.example.data.model.Track
import com.example.data.model.TrackAudioProfile
import com.example.ui.components.AudioDeviceIndicator
import com.example.ui.components.LiquidGlassProgressBar
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.liquidGlass
import com.example.ui.theme.premiumTrackGlowingBorder

@Composable
fun NowPlayingScreen(
    currentTrack: Track?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffleEnabled: Boolean = false,
    repeatMode: Int = 0,
    isFavorite: Boolean = false,
    dominantColor: Color? = null,
    secondaryColor: Color? = null,
    connectedDevice: ConnectedAudioDevice? = null,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onOpenLyrics: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onShareTrack: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentTrack == null) return

    val context = LocalContext.current

    // استخراج خودکار رنگ‌های زنده از کاور آهنگ (اگر از بیرون پاس داده نشده باشد)
    var extractedPrimary by remember(currentTrack.id) { mutableStateOf(Color(0xFF9D4EDD)) }
    var extractedSecondary by remember(currentTrack.id) { mutableStateOf(Color(0xFF00E5FF)) }

    LaunchedEffect(currentTrack.artworkUri, currentTrack.id) {
        try {
            val palette = ArtworkPaletteExtractor.extractColors(context, currentTrack.artworkUri)
            extractedPrimary = palette.first
            extractedSecondary = palette.second
        } catch (_: Exception) {
            // اگر متد استخراج پالت امضای متفاوتی داشت یا کاور خالی بود، بر اساس هش آهنگ رنگ نئونی منحصربه‌فرد می‌سازد
            val palettes = listOf(
                Color(0xFF9D4EDD) to Color(0xFF00E5FF),
                Color(0xFFFF2A85) to Color(0xFFFFB703),
                Color(0xFF00F5D4) to Color(0xFF7B2CBF),
                Color(0xFF3A86FF) to Color(0xFFFF006E),
                Color(0xFF06D6A0) to Color(0xFF118AB2)
            )
            val pick = palettes[(currentTrack.title.hashCode() and Int.MAX_VALUE) % palettes.size]
            extractedPrimary = pick.first
            extractedSecondary = pick.second
        }
    }

    // انیمیشن نرم تغییر رنگ کل صفحه و پنل پایینی با عوض شدن آهنگ
    val activePrimary by animateColorAsState(
        targetValue = dominantColor ?: extractedPrimary,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "PlayerPrimaryColor"
    )
    val activeSecondary by animateColorAsState(
        targetValue = secondaryColor ?: extractedSecondary,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "PlayerSecondaryColor"
    )

    // انیمیشن مقیاس و شناوری کاور آهنگ هنگام پخش و توقف (به سبک iOS 18)
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ArtworkScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "NowPlayingAurora")
    val auroraAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 16000 else 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuroraAngle1"
    )
    val auroraAngle2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 21000 else 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuroraAngle2"
    )
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = if (isPlaying) 1.18f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraPulse"
    )

    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF05060D))
            .drawBehind {
                val w = size.width
                val h = size.height

                // مدار ۱: شفق قطبی بالایی (نور نئونی عمیق منطبق بر رنگ اصلی کاور)
                val orb1X = w * (0.5f + 0.35f * kotlin.math.cos(auroraAngle1.toDouble()).toFloat())
                val orb1Y = h * (0.30f + 0.15f * kotlin.math.sin(auroraAngle1.toDouble()).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activePrimary.copy(alpha = 0.72f),
                            activePrimary.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(orb1X, orb1Y),
                        radius = (w * 0.95f) * auraPulse
                    )
                )

                // مدار ۲: نور فرعی متضاد RGB شفق (سیان / فیروزه‌ای / یاقوتی)
                val orb2X = w * (0.5f - 0.38f * kotlin.math.cos(auroraAngle2.toDouble()).toFloat())
                val orb2Y = h * (0.45f + 0.20f * kotlin.math.sin(auroraAngle2.toDouble()).toFloat())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeSecondary.copy(alpha = 0.65f),
                            activeSecondary.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(orb2X, orb2Y),
                        radius = (w * 0.90f) * auraPulse
                    )
                )

                // مدار ۳: پرتو متحرک گرم/بنفش در نیمه پایینی صفحه برای عمق چندبعدی
                val orb3X = w * (0.5f + 0.25f * kotlin.math.sin(auroraAngle1.toDouble()).toFloat())
                val orb3Y = h * (0.75f + 0.12f * kotlin.math.cos(auroraAngle2.toDouble()).toFloat())
                val warmColor = Color(0xFFFF2A85)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            warmColor.copy(alpha = 0.40f),
                            activePrimary.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(orb3X, orb3Y),
                        radius = (w * 0.85f) * auraPulse
                    )
                )

                // لایه تیره ابسیدین موضعی برای تضمین ۱۰۰٪ خوانایی و کنتراست اطلاعات و کنترل‌ها
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xCC05060D),
                            0.20f to Color.Transparent,
                            0.65f to Color.Transparent,
                            0.90f to Color(0xE605060D),
                            1.00f to Color(0xF805060D)
                        )
                    )
                )
            }
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =========================================================================
            // ۱. هدر بالایی پلیر (دکمه بستن + نشانگر زنده ایرپاد/اسپیکر + منوی اکولایزر)
            // =========================================================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // خط کپسولی بالای شیت (Drag Pill)
                Box(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .width(42.dp)
                        .height(4.5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.30f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SmallGlassIconButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        accentColor = activePrimary,
                        secondaryColor = activeSecondary,
                        onClick = onClose
                    )

                    // نمایشگر هوشمند و موشن‌گرافیک ایرپاد/اسپیکر در مرکز هدر (فقط در زمان اتصال نمایش داده می‌شود)
                    AudioDeviceIndicator(
                        device = connectedDevice,
                        accentColor = activeSecondary,
                        compact = false
                    )

                    SmallGlassIconButton(
                        icon = Icons.Rounded.GraphicEq,
                        accentColor = activePrimary,
                        secondaryColor = activeSecondary,
                        onClick = onOpenEqualizer
                    )
                }
            }

            // =========================================================================
            // ۲. استیج سه‌بعدی کاور آهنگ (با سوایپ چپ/راست برای عوض کردن آهنگ)
            // =========================================================================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                var swipeOffset by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize(0.88f)
                        .scale(artworkScale)
                        .pointerInput(currentTrack.id) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    swipeOffset += dragAmount
                                },
                                onDragEnd = {
                                    if (swipeOffset > 110f) {
                                        onPrevious()
                                    } else if (swipeOffset < -110f) {
                                        onNext()
                                    }
                                    swipeOffset = 0f
                                },
                                onDragCancel = { swipeOffset = 0f }
                            )
                        }
                        .drawBehind {
                            // هاله نوری شناور و گرادیان شعاعی نرم زیر کاور آهنگ
                            drawRoundRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        activePrimary.copy(alpha = if (isPlaying) 0.65f else 0.35f),
                                        activeSecondary.copy(alpha = if (isPlaying) 0.40f else 0.20f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    radius = size.width * 0.72f
                                ),
                                cornerRadius = CornerRadius(36.dp.toPx(), 36.dp.toPx())
                            )
                            // کادر نوری نئونی دور کاور
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        activePrimary.copy(alpha = 0.60f),
                                        activeSecondary.copy(alpha = 0.55f),
                                        activePrimary.copy(alpha = 0.60f)
                                    )
                                ),
                                cornerRadius = CornerRadius(34.dp.toPx(), 34.dp.toPx()),
                                style = Stroke(width = 8.dp.toPx())
                            )
                        }
                        .shadow(
                            elevation = if (isPlaying) 40.dp else 20.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = activePrimary.copy(alpha = 0.85f),
                            spotColor = activeSecondary.copy(alpha = 0.90f)
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.90f),
                                    activeSecondary,
                                    activePrimary,
                                    Color.White.copy(alpha = 0.70f)
                                )
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(4.dp)
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    TrackArtworkThumbnail(
                        artworkUri = currentTrack.artworkUri,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 28.dp,
                        primaryBorderColor = activePrimary,
                        secondaryBorderColor = activeSecondary
                    )
                }
            }

            // =========================================================================
            // ۳. داک پایینی پلیر که کل اجزای آن با رنگ کاور آهنگ تغییر رنگ می‌دهند
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumTrackGlowingBorder(
                        primaryColor = activePrimary,
                        secondaryColor = activeSecondary,
                        isPlaying = isPlaying,
                        cornerRadius = 32.dp,
                        borderWidth = 1.6.dp
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // الف) ردیف نام آهنگ، خواننده، نشانگر ایرپاد/اسپیکر و دکمه لایک
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = currentTrack.artist,
                            color = activeSecondary.copy(alpha = 0.90f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    SmallGlassIconButton(
                        icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        iconTint = if (isFavorite) Color(0xFFFF2A85) else Color.White,
                        accentColor = if (isFavorite) Color(0xFFFF2A85) else activePrimary,
                        secondaryColor = activeSecondary,
                        onClick = onToggleFavorite
                    )
                }

                // ب) نوار پیشرفت (Seekbar) حساس به رنگ آهنگ + زمان و نشانگر هوشمند ایرپاد/اسپیکر
                Column(modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassProgressBar(
                        progress = progress,
                        onSeek = { newRatio ->
                            onSeekTo((newRatio * safeDuration).toLong())
                        },
                        primaryColor = activePrimary,
                        secondaryColor = activeSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDurationMs(currentPositionMs),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // نشانگر هوشمند و موشن‌گرافیک ایرپاد و اسپیکر متصل (جایگزین نشانگر قبلی کیفیت صدا)
                        AudioDeviceIndicator(
                            device = connectedDevice,
                            accentColor = activeSecondary,
                            compact = true
                        )

                        Text(
                            text = formatDurationMs(safeDuration),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ج) دکمه‌های اصلی کنترل پخش (Shuffle, Previous, Dynamic Play Orb, Next, Repeat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallGlassIconButton(
                        icon = Icons.Rounded.Shuffle,
                        iconTint = if (isShuffleEnabled) activeSecondary else Color.White.copy(alpha = 0.70f),
                        accentColor = activePrimary,
                        secondaryColor = activeSecondary,
                        size = 42.dp,
                        onClick = onToggleShuffle
                    )

                    SmallGlassIconButton(
                        icon = Icons.Rounded.SkipPrevious,
                        accentColor = activePrimary,
                        secondaryColor = activeSecondary,
                        size = 54.dp,
                        iconSize = 28.dp,
                        onClick = onPrevious
                    )

                    // دکمه مرکزی Play / Pause که کل گوی نوری آن با رنگ آهنگ تغییر می‌کند
                    DynamicChameleonPlayOrb(
                        isPlaying = isPlaying,
                        primaryColor = activePrimary,
                        secondaryColor = activeSecondary,
                        onClick = onTogglePlay
                    )

                    SmallGlassIconButton(
                        icon = Icons.Rounded.SkipNext,
                        accentColor = activePrimary,
                        secondaryColor = activeSecondary,
                        size = 54.dp,
                        iconSize = 28.dp,
                        onClick = onNext
                    )

                    SmallGlassIconButton(
                        icon = if (repeatMode == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        iconTint = if (repeatMode > 0) activeSecondary else Color.White.copy(alpha = 0.70f),
                        accentColor = activePrimary,
                        secondaryColor = activeSecondary,
                        size = 42.dp,
                        onClick = onToggleRepeat
                    )
                }

                // د) اسلایدر صدای شیشه‌ای هماهنگ با رنگ آهنگ + دکمه‌های دسترسی سریع پایین داک
                DynamicColorVolumeBar(
                    primaryColor = activePrimary,
                    secondaryColor = activeSecondary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomUtilityPill(
                        icon = Icons.Rounded.Lyrics,
                        label = "Lyrics",
                        accentColor = activePrimary,
                        secondaryColor = activeSecondary,
                        onClick = onOpenLyrics
                    )
                    BottomUtilityPill(
                        icon = Icons.Rounded.QueueMusic,
                        label = "Queue",
                        accentColor = activePrimary,
                        secondaryColor = activeSecondary,
                        onClick = onOpenQueue
                    )
                    BottomUtilityPill(
                        icon = Icons.Rounded.Share,
                        label = "Share",
                        accentColor = activePrimary,
                        secondaryColor = activeSecondary,
                        onClick = onShareTrack
                    )
                }
            }
        }
    }
}

/**
 * Overload for NowPlayingScreen to seamlessly support MainScreen and ViewModel state bindings
 */
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    palette: AmbientPalette,
    currentLyrics: List<LyricsLine> = emptyList(),
    appSettings: AppSettings? = null,
    analysisDataProvider: () -> AudioAnalysisData = { AudioAnalysisData() },
    currentPositionProvider: () -> Long = { playbackState.currentPositionMs },
    currentPositionMs: Long? = null,
    audioProfile: TrackAudioProfile? = null,
    onCollapse: () -> Unit = {},
    onTogglePlay: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onToggleFavorite: (Track) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleShuffleMode: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onOpenLyricsEditor: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onSelectVisualizerMode: (com.example.data.model.VisualizerMode) -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onOpenMetadataEditor: (Track?) -> Unit = {},
    onOpenHearingCalibration: () -> Unit = {},
    onOpenVipPaywall: (String?) -> Unit = {},
    onSelectNowPlayingStyle: (String) -> Unit = {},
    onUpdateTrackArtwork: (Track, String) -> Unit = { _, _ -> },
    connectedDevice: ConnectedAudioDevice? = null,
    onShareSong: (Track) -> Unit = {},
    onPlaybackSpeedChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val curTrack = playbackState.currentTrack
    val effectivePositionMs = currentPositionMs ?: currentPositionProvider()
    NowPlayingScreen(
        currentTrack = curTrack,
        isPlaying = playbackState.isPlaying,
        currentPositionMs = effectivePositionMs,
        durationMs = playbackState.durationMs,
        isShuffleEnabled = playbackState.isShuffle,
        repeatMode = playbackState.repeatMode.ordinal,
        isFavorite = curTrack?.isFavorite ?: false,
        dominantColor = palette.primary,
        secondaryColor = palette.accent,
        connectedDevice = connectedDevice,
        onTogglePlay = onTogglePlay,
        onNext = onNext,
        onPrevious = onPrevious,
        onSeekTo = onSeekTo,
        onToggleShuffle = onToggleShuffle,
        onToggleRepeat = onCycleRepeat,
        onToggleFavorite = { curTrack?.let { onToggleFavorite(it) } },
        onOpenLyrics = onOpenLyricsEditor,
        onOpenEqualizer = onOpenEqualizer,
        onOpenQueue = onOpenQueue,
        onShareTrack = { curTrack?.let { onShareSong(it) } },
        onClose = onCollapse,
        modifier = modifier
    )
}

/**
 * گوی مرکزی Play/Pause با تغییر رنگ زنده بر اساس کاور آهنگ و فیزیک فنری
 */
@Composable
private fun DynamicChameleonPlayOrb(
    isPlaying: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
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

    Box(
        modifier = Modifier
            .size(74.dp)
            .scale(scale)
            .shadow(
                elevation = 24.dp,
                shape = CircleShape,
                ambientColor = primaryColor.copy(alpha = 0.85f),
                spotColor = Color.Black
            )
            .clip(CircleShape)
            // رینگ تیره اوبسیدین ضد سفیدی برای تضمین کنتراست در هر شرایط
            .background(Color(0xFF060710))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor,
                        secondaryColor,
                        primaryColor.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.30f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(38.dp)
        )
    }
}

/**
 * دکمه شیشه‌ای کوچک با کنتراست ۱۰۰٪ تضمین‌شده، ضد ناپدید شدن در پس‌زمینه سفید
 */
@Composable
private fun SmallGlassIconButton(
    icon: ImageVector,
    accentColor: Color,
    secondaryColor: Color,
    iconTint: Color = Color.White,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SmallBtnScale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color.Black,
                spotColor = accentColor.copy(alpha = 0.5f)
            )
            .clip(CircleShape)
            // پایه تیره اوبسیدین برای ممانعت از محو شدن آیکون‌های سفید روی پس‌زمینه‌های روشن
            .background(Color(0xE60A0C16))
            .liquidGlass(
                shape = CircleShape,
                accentColor = accentColor,
                secondaryAccent = secondaryColor
            )
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.80f),
                        secondaryColor.copy(alpha = 0.50f),
                        accentColor.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.20f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (iconTint != Color.White) iconTint else Color(0xFFF2F5FD),
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * کپسول‌های شیشه‌ای پایین پلیر با کنتراست تقویت‌شده و مرز کریستالی
 */
@Composable
private fun BottomUtilityPill(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    secondaryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xCC0A0C16))
            .liquidGlass(
                shape = RoundedCornerShape(50),
                accentColor = accentColor,
                secondaryAccent = secondaryColor
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.40f),
                        secondaryColor.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = secondaryColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * نوار کنترل صدای سیستم که رنگ آن نیز با رنگ آهنگ در حال پخش تغییر می‌کند
 */
@Composable
private fun DynamicColorVolumeBar(
    primaryColor: Color,
    secondaryColor: Color
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeRatio by remember {
        mutableFloatStateOf(
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.toFloat()
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.VolumeDown,
            contentDescription = "Volume Down",
            tint = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.size(18.dp)
        )

        LiquidGlassProgressBar(
            progress = volumeRatio,
            onSeek = { newRatio ->
                volumeRatio = newRatio
                val targetVol = (newRatio * maxVolume).toInt().coerceIn(0, maxVolume)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            },
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Rounded.VolumeUp,
            contentDescription = "Volume Up",
            tint = secondaryColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
