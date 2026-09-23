package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.audio.AmbientPalette
import com.example.data.model.*
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization
import java.util.Calendar

@Composable
fun HomeScreen(
    playbackState: PlaybackState,
    allTracks: List<Track>,
    recentlyPlayed: List<Track>,
    favoriteTracks: List<Track>,
    palette: AmbientPalette,
    appSettings: AppSettings,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTogglePlay: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenWrapped: () -> Unit,
    onExpandNowPlaying: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 120.dp,
    timeSlotTracks: List<Track> = emptyList(),
    onThisDayHighlight: TopSongItem? = null,
    weeklyRecapStats: WrappedStats? = null,
    didRestoreSession: Boolean = false,
    duplicateCount: Int = 0,
    onOpenDuplicatesReview: () -> Unit = {},
    onToggleFocusMode: () -> Unit = {},
    currentPositionProvider: () -> Long = { playbackState.currentPositionMs },
    onSeekTo: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = appSettings.language
    val context = LocalContext.current
    var showFocusInfoDialog by remember { mutableStateOf(false) }

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val timeSlotGreeting = when (currentHour) {
        in 5..11 -> if (lang == AppLanguage.PERSIAN) "موسیقی برای صبح دل‌انگیز شما" else "Music for Your Morning Focus"
        in 12..16 -> if (lang == AppLanguage.PERSIAN) "ریتم پرانرژی بعدازظهر" else "Afternoon Energy Boost"
        in 17..21 -> if (lang == AppLanguage.PERSIAN) "آرامش و موسیقی غروب" else "Evening Wind-Down"
        else -> if (lang == AppLanguage.PERSIAN) "موزیک‌های نیمه‌شب و سکوت" else "Late Night Ambient Flow"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = maxOf(bottomPadding + 20.dp, 120.dp)),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header & Greeting
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Localization.getString("aura_music", lang),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = palette.accent
                        )
                    )
                    Text(
                        text = Localization.getString("audio_environment", lang),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Focus Mode Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (appSettings.focusModeEnabled) palette.accent.copy(alpha = 0.30f) else Color(0x18FFFFFF))
                            .border(1.dp, if (appSettings.focusModeEnabled) palette.accent else Color(0x30FFFFFF), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.clickable { onToggleFocusMode() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = "Focus",
                                tint = if (appSettings.focusModeEnabled) palette.accent else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "تمرکز" else "Focus",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Focus Info",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { showFocusInfoDialog = true }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.2f))
                            .border(1.dp, palette.primary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Aura Engine",
                            tint = palette.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Section 1.1: Elevated Ambient Top Now Playing Card (Shows dynamic playing/featured track)
        val heroTrack = playbackState.currentTrack ?: allTracks.firstOrNull()
        if (heroTrack != null) {
            item {
                HeroQuickPlayCard(
                    heroTrack = heroTrack,
                    playbackState = playbackState,
                    palette = palette,
                    allTracks = allTracks,
                    hasActiveTrack = playbackState.currentTrack != null,
                    appSettings = appSettings,
                    currentPositionProvider = currentPositionProvider,
                    onSeekTo = onSeekTo,
                    onPlayTrack = onPlayTrack,
                    onTogglePlay = {
                        if (playbackState.currentTrack != null) {
                            onTogglePlay()
                        } else {
                            onPlayTrack(heroTrack, if (allTracks.isNotEmpty()) allTracks else listOf(heroTrack))
                        }
                    },
                    onExpandNowPlaying = onExpandNowPlaying
                )
            }
        }

        // Duplicate audio files prompt badge
        if (duplicateCount > 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(16.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = Color(0xFFF59E0B),
                            tintAlpha = 0.20f,
                            borderWidth = 1.dp
                        )
                        .clickable { onOpenDuplicatesReview() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "$duplicateCount فایل تکراری شناسایی شد" else "$duplicateCount Duplicate tracks detected",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "برای بهینه‌سازی و مخفی کردن نسخه‌های کیفیت پایین لمس کنید" else "Tap to review & keep highest quality version",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD1D5DB))
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Feature 2: Time-of-Day Contextual Carousel
        if (timeSlotTracks.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = timeSlotGreeting,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(timeSlotTracks) { track ->
                            Box(
                                modifier = Modifier
                                    .width(148.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(20.dp),
                                        thickness = GlassThickness.REGULAR,
                                        tintColor = Color(0xFF161A2E),
                                        tintAlpha = 0.65f,
                                        borderWidth = 1.dp
                                    )
                                    .clickable { onPlayTrack(track, timeSlotTracks) }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    TrackArtworkThumbnail(
                                        artworkUri = track.artworkUri,
                                        accentColor = palette.primary,
                                        size = 124.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        iconSize = 38.dp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA6A8BA), fontSize = 12.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feature 3: "On This Day" Throwback Card
        if (onThisDayHighlight != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.accent,
                            tintAlpha = 0.16f,
                            borderWidth = 1.dp
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HistoryEdu,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Localization.getString("on_this_day_title", lang),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = palette.accent,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "${onThisDayHighlight.title} • ${onThisDayHighlight.artist}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "آهنگ محبوب شما در سال‌های گذشته" else "Your top played track from this day in history",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8))
                            )
                        }
                    }
                }
            }
        }

        // Feature 4: Weekly Music Recap Banner
        if (weeklyRecapStats != null && weeklyRecapStats.topSongs.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.20f,
                            borderWidth = 1.2.dp
                        )
                        .clickable { onOpenWrapped() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(palette.primary.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = Localization.getString("weekly_recap_title", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "${weeklyRecapStats.totalListeningMinutes} ${if (lang == AppLanguage.PERSIAN) "دقیقه شنیداری این هفته" else "minutes listened this week"}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8))
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Recently Played
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "اخیراً پخش شده" else "Recently Played",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    TextButton(onClick = onOpenLibrary) {
                        Text(if (lang == AppLanguage.PERSIAN) "همه" else "See All", color = palette.accent, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(recentlyPlayed) { track ->
                        Box(
                            modifier = Modifier
                                .width(148.dp)
                                .liquidGlass(
                                    shape = RoundedCornerShape(20.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = Color(0xFF161A2E),
                                    tintAlpha = 0.65f,
                                    borderWidth = 1.dp
                                )
                                .clickable { onPlayTrack(track, allTracks) }
                                .padding(12.dp)
                        ) {
                            Column {
                                TrackArtworkThumbnail(
                                    artworkUri = track.artworkUri,
                                    accentColor = palette.primary,
                                    size = 124.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    iconSize = 38.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA6A8BA), fontSize = 12.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Favorites
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "علاقه‌مندی‌ها" else "Favorites",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                if (favoriteTracks.isNotEmpty()) {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "${favoriteTracks.size} قطعه" else "${favoriteTracks.size} tracks",
                        style = MaterialTheme.typography.bodySmall.copy(color = palette.accent)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (favoriteTracks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    favoriteTracks.forEach { track ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(16.dp),
                                    thickness = GlassThickness.THIN,
                                    tintColor = palette.accent,
                                    tintAlpha = 0.14f,
                                    borderWidth = 1.dp
                                )
                                .clickable { onPlayTrack(track, allTracks) }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrackArtworkThumbnail(
                                    artworkUri = track.artworkUri,
                                    accentColor = palette.accent,
                                    size = 48.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    iconSize = 24.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} • ${track.genre}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = track.durationFormatted,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF88889C))
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(18.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.secondary,
                            tintAlpha = 0.12f,
                            borderWidth = 1.dp
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = Color(0xFF6C6C82),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "هنوز آهنگی به علاقه‌مندی‌ها افزوده نشده" else "No favorites yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }

    if (showFocusInfoDialog) {
        FocusModeExplainerDialog(
            lang = lang,
            palette = palette,
            onDismiss = { showFocusInfoDialog = false }
        )
    }
}

@Composable
private fun HeroQuickPlayCard(
    heroTrack: Track?,
    playbackState: PlaybackState,
    palette: AmbientPalette,
    allTracks: List<Track>,
    hasActiveTrack: Boolean,
    appSettings: AppSettings,
    currentPositionProvider: () -> Long,
    onSeekTo: (Long) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTogglePlay: () -> Unit,
    onExpandNowPlaying: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isPlaying = playbackState.status == PlayerStatus.PLAYING
    val isFa = appSettings.language == AppLanguage.PERSIAN
    val cardShape = RoundedCornerShape(26.dp)

    // Smooth color animation when artwork/palette changes
    val animColor1 by androidx.compose.animation.animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "heroColor1"
    )
    val animColor2 by androidx.compose.animation.animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "heroColor2"
    )
    val animColor3 by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "heroColor3"
    )

    // Dynamic rotation: spins fast and smooth when playing (3200ms), stops completely when paused
    val infiniteTransition = rememberInfiniteTransition(label = "heroRotationBeam")
    val playingAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "lightBeamAngle"
    )

    val rotationAngle = if (isPlaying) playingAngle else 0f

    val beatTransition = rememberInfiniteTransition(label = "heroCoverBeat")
    val beatScale by if (isPlaying) {
        beatTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.10f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "beatScale"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 28.dp,
                shape = cardShape,
                spotColor = animColor1.copy(alpha = 0.85f),
                ambientColor = animColor2.copy(alpha = 0.65f)
            )
            .clip(cardShape)
            .clickable {
                if (hasActiveTrack) {
                    onExpandNowPlaying()
                } else if (heroTrack != null) {
                    onPlayTrack(heroTrack, if (allTracks.isNotEmpty()) allTracks else listOf(heroTrack))
                }
            }
            .testTag("hero_quick_play_card")
    ) {
        // Layer 1: Deep atmospheric ambient background base
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFF090B14))
        )

        // Layer 2: Rotating 3-Color Dynamic Sweep Gradient Canvas with optimized cached brush
        val sweepColors = remember(animColor1, animColor2, animColor3) {
            listOf(
                animColor1.copy(alpha = 0.95f),
                animColor2.copy(alpha = 0.90f),
                animColor3.copy(alpha = 0.95f),
                animColor1.copy(alpha = 0.85f),
                animColor2.copy(alpha = 0.92f),
                animColor3.copy(alpha = 0.88f),
                animColor1.copy(alpha = 0.95f)
            )
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            rotate(rotationAngle, pivot = centerOffset) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = sweepColors,
                        center = centerOffset
                    ),
                    radius = size.maxDimension * 1.25f,
                    center = centerOffset
                )
            }
        }

        // Layer 3: Soft blurred radial dispersion glow in the center for smooth silky luminescence
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.55f)
                        ),
                        radius = 450f
                    )
                )
        )

        // Layer 4: Glassmorphic frost overlay with elegant double-border glow
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.40f)
                        )
                    )
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.White.copy(alpha = 0.70f),
                            animColor3.copy(alpha = 0.60f),
                            Color.White.copy(alpha = 0.25f),
                            animColor1.copy(alpha = 0.70f),
                            Color.White.copy(alpha = 0.70f)
                        )
                    ),
                    shape = cardShape
                )
        )

        // Card Content (Moderate medium height and balanced layout)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 150.dp)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dotScale by if (isPlaying) {
                        val dotTransition = rememberInfiniteTransition(label = "heroDotPulse")
                        dotTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                            ),
                            label = "dotScale"
                        )
                    } else {
                        remember { mutableFloatStateOf(1f) }
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFa) "در حال پخش" else "CURRENTLY PLAYING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.95f),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.5.sp
                        )
                    )
                }

                Text(
                    text = heroTrack?.genre?.ifEmpty { null } ?: (if (isFa) "الکترونیک" else "Electronic"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Track Info Row (Compact artwork, small title & artist, compact play button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Compact Album Art with glowing backlight and beat pulse bounce
                Box(contentAlignment = Alignment.Center) {
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .scale(beatScale * 1.15f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            animColor3.copy(alpha = 0.90f),
                                            animColor1.copy(alpha = 0.60f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .scale(beatScale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33000000))
                            .border(1.2.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!heroTrack?.artworkUri.isNullOrEmpty()) {
                            coil.compose.AsyncImage(
                                model = heroTrack?.artworkUri,
                                contentDescription = heroTrack?.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and Artist typography
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = heroTrack?.title ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = heroTrack?.artist ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.5.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Compact refined play button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(14.dp, CircleShape, spotColor = animColor1)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color.White.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .clickable {
                            if (hasActiveTrack) {
                                onTogglePlay()
                            } else if (heroTrack != null) {
                                onPlayTrack(heroTrack, if (allTracks.isNotEmpty()) allTracks else listOf(heroTrack))
                            }
                        }
                        .testTag("hero_play_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = animColor1,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

private fun formatHeroDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

@Composable
private fun FocusModeExplainerDialog(
    lang: AppLanguage,
    palette: AmbientPalette,
    onDismiss: () -> Unit
) {
    val isFa = lang == AppLanguage.PERSIAN
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(26.dp),
                    thickness = GlassThickness.THICK,
                    tintColor = palette.primary,
                    tintAlpha = 0.22f,
                    borderWidth = 1.2.dp
                )
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(palette.accent.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isFa) "حالت تمرکز و مطالعه (Focus Mode) 🎧" else "Focus & Deep Study Mode 🎧",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 16.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isFa)
                        "حالت تمرکز برای اوقاتی طراحی شده که می‌خواهید مطالعه کنید، برنامه‌نویسی کنید، کار کنید یا عمیقاً تمرکز داشته باشید.\n\nبا فعال کردن این گزینه، اعلان‌ها و شلوغی‌های اضافه برنامه کنار رفته و ریتم‌های آرامش‌بخش، فرکانس‌های آلفا و ابزارهای تمرکز برای افزایش بهره‌وری شما اماده می‌شوند."
                    else
                        "Focus Mode is designed for study sessions, coding, deep work, or relaxation.\n\nWhen activated, unnecessary visual distractions are reduced and calming ambient flows keep you centered and productive.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.5.sp,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isFa) "متوجه شدم 👍" else "Got It 👍",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
