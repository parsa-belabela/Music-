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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (appSettings.focusModeEnabled) palette.accent.copy(alpha = 0.30f) else Color(0x18FFFFFF))
                            .border(1.dp, if (appSettings.focusModeEnabled) palette.accent else Color(0x30FFFFFF), RoundedCornerShape(16.dp))
                            .clickable { onToggleFocusMode() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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

        // Instant Resume Notification Pill
        if (didRestoreSession && playbackState.currentTrack != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(16.dp),
                            thickness = GlassThickness.THIN,
                            tintColor = palette.secondary,
                            tintAlpha = 0.18f,
                            borderWidth = 1.dp
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = palette.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${Localization.getString("welcome_back", lang)} • ${playbackState.currentTrack.title}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
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
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "heroColor1"
    )
    val animColor2 by androidx.compose.animation.animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "heroColor2"
    )
    val animColor3 by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "heroColor3"
    )

    // Continuous 360 degree ambient light beam rotation
    val infiniteTransition = rememberInfiniteTransition(label = "heroRotationBeam")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 6000 else 14000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "lightBeamAngle"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = cardShape,
                spotColor = animColor1.copy(alpha = 0.65f),
                ambientColor = animColor2.copy(alpha = 0.5f)
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
        // Rotating 3-Color Dynamic Sweep Gradient Canvas
        Canvas(modifier = Modifier.matchParentSize()) {
            rotate(rotationAngle) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            animColor1,
                            animColor2,
                            animColor3,
                            animColor1.copy(alpha = 0.85f),
                            animColor2,
                            animColor3.copy(alpha = 0.9f),
                            animColor1
                        )
                    ),
                    radius = size.maxDimension * 1.05f
                )
            }
        }

        // Soft internal ambient overlay for depth and contrast
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
                .border(
                    width = 1.3.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            animColor3.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    ),
                    shape = cardShape
                )
        )

        // Card Content (Extra tall & elongated glowing frame with delicate compact internal elements)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 230.dp)
                .padding(horizontal = 24.dp, vertical = 34.dp),
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
                                animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
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

            Spacer(modifier = Modifier.height(28.dp))

            // Main Track Info Row (Compact artwork, small title & artist, compact play button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Smaller Compact Album Art
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0x33000000))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
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
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Smaller Title and Artist typography
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = heroTrack?.title ?: "",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.5.sp
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
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Compact refined play button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(12.dp, CircleShape, spotColor = animColor1)
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
                        modifier = Modifier.size(28.dp)
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
