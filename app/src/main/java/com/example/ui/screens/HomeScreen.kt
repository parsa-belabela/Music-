package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    bottomPadding: androidx.compose.ui.unit.Dp = 120.dp,
    timeSlotTracks: List<Track> = emptyList(),
    onThisDayHighlight: TopSongItem? = null,
    weeklyRecapStats: WrappedStats? = null,
    didRestoreSession: Boolean = false,
    duplicateCount: Int = 0,
    onOpenDuplicatesReview: () -> Unit = {},
    onToggleFocusMode: () -> Unit = {},
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

        // Hero CURRENTLY PLAYING Card
        item {
            val heroTrack = playbackState.currentTrack ?: allTracks.firstOrNull()
            HeroQuickPlayCard(
                heroTrack = heroTrack,
                isPlaying = playbackState.status == PlayerStatus.PLAYING,
                palette = palette,
                allTracks = allTracks,
                hasActiveTrack = playbackState.currentTrack != null,
                onPlayTrack = onPlayTrack,
                onTogglePlay = onTogglePlay
            )
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(timeSlotTracks) { track ->
                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(18.dp),
                                        thickness = GlassThickness.REGULAR,
                                        tintColor = palette.primary,
                                        tintAlpha = 0.16f,
                                        borderWidth = 1.dp
                                    )
                                    .clickable { onPlayTrack(track, timeSlotTracks) }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    TrackArtworkThumbnail(
                                        artworkUri = track.artworkUri,
                                        accentColor = palette.primary,
                                        size = 120.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        iconSize = 36.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
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
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B0), fontSize = 11.sp),
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recentlyPlayed) { track ->
                        Box(
                            modifier = Modifier
                                .width(145.dp)
                                .liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = palette.primary,
                                    tintAlpha = 0.16f,
                                    borderWidth = 1.dp
                                )
                                .clickable { onPlayTrack(track, allTracks) }
                                .padding(12.dp)
                        ) {
                            Column {
                                TrackArtworkThumbnail(
                                    artworkUri = track.artworkUri,
                                    accentColor = palette.primary,
                                    size = 121.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    iconSize = 40.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
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
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B0), fontSize = 12.sp),
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
                        text = "${favoriteTracks.size} tracks",
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
    isPlaying: Boolean,
    palette: AmbientPalette,
    allTracks: List<Track>,
    hasActiveTrack: Boolean,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(24.dp)
    val rotationDuration = if (isPlaying) 3200 else 16000

    val infiniteTransition = rememberInfiniteTransition(label = "heroLightBeam")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDuration, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "lightBeamAngle"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(cardShape)
            .clickable {
                heroTrack?.let { onPlayTrack(it, allTracks) }
            }
            .testTag("hero_quick_play_card")
    ) {
        if (heroTrack?.artworkUri != null) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(heroTrack.artworkUri)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(14.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(palette.primary.copy(alpha = 0.35f), Color(0xFF0D0F1B))
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x800A0C16),
                            Color(0xCC080A12)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    rotate(rotationAngle) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    palette.accent,
                                    palette.secondary,
                                    palette.primary,
                                    palette.accent.copy(alpha = 0.8f),
                                    palette.secondary.copy(alpha = 0.9f),
                                    palette.primary.copy(alpha = 0.75f),
                                    palette.accent
                                )
                            ),
                            radius = size.maxDimension * 0.85f,
                            blendMode = BlendMode.Screen
                        )
                    }
                }
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            palette.accent.copy(alpha = 0.65f),
                            palette.secondary.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.20f)
                        )
                    ),
                    shape = cardShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(palette.secondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlaying) "CURRENTLY PLAYING" else "QUICK PLAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = palette.secondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                }

                Text(
                    text = heroTrack?.genre ?: "Audio",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0A0C0))
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (heroTrack != null) {
                    TrackArtworkThumbnail(
                        artworkUri = heroTrack.artworkUri,
                        accentColor = palette.secondary,
                        size = 56.dp,
                        shape = RoundedCornerShape(14.dp),
                        iconSize = 28.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = heroTrack?.title ?: "Select a Track",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = heroTrack?.artist ?: "Local-First Hi-Fi Audio",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFA0A0B8)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(palette.primary)
                        .clickable {
                            if (hasActiveTrack) onTogglePlay()
                            else heroTrack?.let { onPlayTrack(it, allTracks) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
