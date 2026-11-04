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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.OnlineCacheHolder
import com.example.OnlineMusicRepository
import com.example.OnlineTasteEngine
import com.example.audio.AmbientPalette
import com.example.data.model.*
import com.example.ui.components.MusicArtworkPlaceholder
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization
import java.util.Calendar
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos

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
    onToggleGymMode: () -> Unit = {},
    currentPositionProvider: () -> Long = { playbackState.currentPositionMs },
    onSeekTo: (Long) -> Unit = {},
    onlineTrendingTracks: List<Track> = emptyList(),
    selectedTrendingMode: String = "FOR_YOU",
    isLoadingOnlineRecommendations: Boolean = false,
    userTopArtists: List<String> = emptyList(),
    onlineRecentTracks: List<Track> = emptyList(),
    onSelectTrendingMode: (String) -> Unit = {},
    onRefreshOnlineRecommendations: () -> Unit = {},
    onDownloadTrack: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = appSettings.language
    val isFa = lang == AppLanguage.PERSIAN
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadingIds by OnlineCacheHolder.downloadingTrackIds.collectAsState()
    val downloadedIds by OnlineCacheHolder.downloadedTrackIds.collectAsState()

    val mergedRecentlyPlayed = remember(onlineRecentTracks, recentlyPlayed) {
        (onlineRecentTracks + recentlyPlayed).distinctBy { "${it.title.lowercase().trim()}|${it.artist.lowercase().trim()}" }.take(16)
    }

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val timeSlotGreeting = when (currentHour) {
        in 5..11 -> if (isFa) "وایب صبح ☀️" else "Morning Vibe ☀️"
        in 12..16 -> if (isFa) "انرژی روز ⚡" else "Daytime Energy ⚡"
        in 17..21 -> if (isFa) "وایب عصر 🌆" else "Evening Vibe 🌆"
        else -> if (isFa) "وایب شب 🌙" else "Late Night 🌙"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = maxOf(bottomPadding + 20.dp, 120.dp)),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // App Header & Greeting + Gym / High-Energy Pump Mode Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                    // دکمه اختصاصی حالت باشگاه و انرژی بالا (Gym / High-Energy Pump Mode)
                    val isGymActive = appSettings.gymModeEnabled
                    Surface(
                        onClick = onToggleGymMode,
                        shape = RoundedCornerShape(50),
                        color = if (isGymActive) Color(0xFFFF1E56).copy(alpha = 0.28f) else Color(0x22FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isGymActive) 1.5.dp else 1.dp,
                            color = if (isGymActive) Color(0xFFFF1E56) else palette.primary.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.testTag("gym_mode_toggle_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = "Gym Pump Mode",
                                tint = if (isGymActive) Color(0xFFFF1E56) else palette.accent,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) {
                                    if (isGymActive) "🔥 باشگاه فعال" else "حالت باشگاه"
                                } else {
                                    if (isGymActive) "🔥 PUMP ON" else "Gym Mode"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp
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
        val heroTrack = playbackState.currentTrack
            ?: onlineTrendingTracks.firstOrNull()
            ?: timeSlotTracks.firstOrNull()
            ?: allTracks.firstOrNull()
        if (heroTrack != null) {
            item {
                HeroQuickPlayCard(
                    heroTrack = heroTrack,
                    playbackState = playbackState,
                    palette = palette,
                    allTracks = if (allTracks.isNotEmpty()) allTracks else onlineTrendingTracks.ifEmpty { listOf(heroTrack) },
                    hasActiveTrack = playbackState.currentTrack != null,
                    appSettings = appSettings,
                    currentPositionProvider = currentPositionProvider,
                    onSeekTo = onSeekTo,
                    onPlayTrack = onPlayTrack,
                    onTogglePlay = {
                        if (playbackState.currentTrack != null) {
                            onTogglePlay()
                        } else {
                            onPlayTrack(heroTrack, onlineTrendingTracks.ifEmpty { allTracks.ifEmpty { listOf(heroTrack) } })
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

        // Feature 2: Minimal Time-of-Day Online Carousel
        if (timeSlotTracks.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeSlotGreeting,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )

                        IconButton(
                            onClick = onRefreshOnlineRecommendations,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(timeSlotTracks, key = { "timeslot_${it.id}" }) { track ->
                            val isSaved = downloadedIds.contains(track.id) ||
                                downloadedIds.contains(track.id.removePrefix("online_")) ||
                                OnlineCacheHolder.isTrackCached(context, track.id)
                            val isDownloadingThis = downloadingIds.contains(track.id) ||
                                downloadingIds.contains(track.id.removePrefix("online_"))

                            Box(
                                modifier = Modifier
                                    .width(148.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(18.dp),
                                        thickness = GlassThickness.THIN,
                                        tintColor = Color(0xFF161A2E),
                                        tintAlpha = 0.50f,
                                        borderWidth = 0.8.dp
                                    )
                                    .clickable { onPlayTrack(track, timeSlotTracks) }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Box(modifier = Modifier.size(128.dp)) {
                                        TrackArtworkThumbnail(
                                            artworkUri = track.artworkUri,
                                            accentColor = palette.primary,
                                            size = 128.dp,
                                            shape = RoundedCornerShape(14.dp),
                                            iconSize = 36.dp,
                                            title = track.title,
                                            artist = track.artist,
                                            trackId = track.id
                                        )

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xCC070912))
                                                .clickable { onDownloadTrack(track) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when {
                                                isDownloadingThis -> {
                                                    CircularProgressIndicator(
                                                        color = Color(0xFF1ED760),
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                isSaved -> {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Saved",
                                                        tint = Color(0xFF1ED760),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                                else -> {
                                                    Icon(
                                                        imageVector = Icons.Default.Download,
                                                        contentDescription = "Download",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
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
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA6A8BA), fontSize = 11.5.sp),
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

        // Feature 2.5: Gen-Z Trending & Personalized Taste Feed (Minimal & Clean)
        item {
            val tasteCategories = remember(isFa) {
                listOf(
                    "FOR_YOU" to (if (isFa) "✨ برای تو" else "✨ For You"),
                    "TIKTOK_INSTA" to (if (isFa) "🔥 اینستا و تیک‌تاک" else "🔥 TikTok & Reels"),
                    "YOUTUBE" to (if (isFa) "▶️ یوتیوب" else "▶️ YouTube"),
                    "RAPPERS" to (if (isFa) "🎤 رپرها" else "🎤 Rappers"),
                    "PERSIAN" to (if (isFa) "💎 فارسی" else "💎 Persian"),
                    "PHONK_NIGHT" to (if (isFa) "🌙 فونک و شب" else "🌙 Phonk & Night")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isFa) "ترند روز 🔥" else "Trending Now 🔥",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        if (userTopArtists.isNotEmpty()) {
                            Surface(
                                color = Color(0x1FFFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = userTopArtists.first(),
                                    color = Color(0xFF1ED760),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (isLoadingOnlineRecommendations) {
                        CircularProgressIndicator(
                            color = Color(0xFF1ED760),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // دسته‌بندی‌های مینیمال نسل زد
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tasteCategories, key = { it.first }) { (modeKey, label) ->
                        val isSelected = selectedTrendingMode == modeKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) Color(0xFF1ED760) else Color(0x1FFFFFFF)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { onSelectTrendingMode(modeKey) }
                                .padding(horizontal = 13.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF060A08) else Color.White.copy(alpha = 0.88f),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (onlineTrendingTracks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(onlineTrendingTracks, key = { "trending_${it.id}" }) { track ->
                            val isSaved = downloadedIds.contains(track.id) ||
                                downloadedIds.contains(track.id.removePrefix("online_")) ||
                                OnlineCacheHolder.isTrackCached(context, track.id)
                            val isDownloadingThis = downloadingIds.contains(track.id) ||
                                downloadingIds.contains(track.id.removePrefix("online_"))

                            Box(
                                modifier = Modifier
                                    .width(152.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(18.dp),
                                        thickness = GlassThickness.THIN,
                                        tintColor = palette.primary,
                                        tintAlpha = 0.16f,
                                        borderWidth = 0.8.dp
                                    )
                                    .clickable { onPlayTrack(track, onlineTrendingTracks) }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Box(modifier = Modifier.size(132.dp)) {
                                        TrackArtworkThumbnail(
                                            artworkUri = track.artworkUri,
                                            accentColor = palette.primary,
                                            size = 132.dp,
                                            shape = RoundedCornerShape(14.dp),
                                            iconSize = 36.dp,
                                            title = track.title,
                                            artist = track.artist,
                                            trackId = track.id
                                        )

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xCC070912))
                                                .clickable { onDownloadTrack(track) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when {
                                                isDownloadingThis -> {
                                                    CircularProgressIndicator(
                                                        color = Color(0xFF1ED760),
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                isSaved -> {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Downloaded",
                                                        tint = Color(0xFF1ED760),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                                else -> {
                                                    Icon(
                                                        imageVector = Icons.Default.Download,
                                                        contentDescription = "Download",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
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
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFA6A8BA),
                                            fontSize = 11.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // پلی‌لیست‌ها و میکس‌های هوشمند وایرال (اینستاگرام، تیک‌تاک، یوتیوب، رپ و...) برگرفته از بخش سرچ
                val viralSmartPlaylists = remember(allTracks) {
                    OnlineTasteEngine.buildSmartPlaylists(context)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF1ED760),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = if (isFa) "پلی‌لیست‌های ترند و وایرال ⚡" else "Viral Trend Playlists ⚡",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                    }
                    Text(
                        text = if (isFa) "اینستا • تیک‌تاک • یوتیوب" else "Reels • TikTok • Hits",
                        color = Color(0xFF1ED760),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(viralSmartPlaylists, key = { "home_viral_${it.id}" }) { smartMix ->
                        Box(
                            modifier = Modifier
                                .width(168.dp)
                                .liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    thickness = GlassThickness.THIN,
                                    tintColor = smartMix.accentColor,
                                    tintAlpha = 0.16f,
                                    borderWidth = 0.9.dp
                                )
                                .clickable {
                                    scope.launch {
                                        val onlineList = OnlineMusicRepository.searchCatalog(smartMix.query)
                                            .distinctBy { "${it.title.lowercase().trim()}|${it.artist.lowercase().trim()}" }
                                            .take(16)
                                        val appList = onlineList.map { it.toAppTrack(OnlineMusicRepository.buildStreamUrl(it)) }
                                        if (appList.isNotEmpty()) {
                                            onPlayTrack(appList.first(), appList)
                                        }
                                    }
                                }
                                .padding(10.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .size(148.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(smartMix.accentColor.copy(alpha = 0.35f), Color(0xFF0B0F1A))
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (smartMix.coverUrls.isNotEmpty()) {
                                        AsyncImage(
                                            model = smartMix.coverUrls.first(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = smartMix.accentColor,
                                            modifier = Modifier.size(46.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isFa) smartMix.titleFa else smartMix.titleEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isFa) smartMix.subtitleFa else smartMix.subtitleEn,
                                    color = Color(0xFFA0A8BE),
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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

        // Feature 4: Weekly Music Recap Banner (Locked until Friday for 48-hour window)
        if (weeklyRecapStats != null) {
            item {
                val isWeeklyLocked = weeklyRecapStats.period.isLocked
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = if (isWeeklyLocked) Color(0xFFF59E0B) else palette.primary,
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
                                    .background(
                                        if (isWeeklyLocked) Color(0xFFF59E0B).copy(alpha = 0.25f) else palette.primary.copy(alpha = 0.3f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isWeeklyLocked) Icons.Default.LockClock else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isWeeklyLocked) Color(0xFFFBBF24) else palette.primary,
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
                                    text = if (isWeeklyLocked) {
                                        if (lang == AppLanguage.PERSIAN) {
                                            "🔒 قفل تا روز جمعه (${weeklyRecapStats.period.daysRemainingUntilUnlock} روز مانده • دسترسی محدود ۴۸ ساعته)"
                                        } else {
                                            "🔒 Locked until Friday (${weeklyRecapStats.period.daysRemainingUntilUnlock}d left • 48h window)"
                                        }
                                    } else {
                                        "${weeklyRecapStats.totalListeningMinutes} ${if (lang == AppLanguage.PERSIAN) "دقیقه شنیداری • باز به مدت ۴۸ ساعت" else "minutes listened • Unlocked for 48h"}"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isWeeklyLocked) Color(0xFFFBBF24) else Color(0xFFA0A0B8)
                                    )
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

        // Recently Played (شامل هم آهنگ‌های آنلاین گوش‌داده‌شده و هم آرشیو گوشی)
        if (mergedRecentlyPlayed.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "اخیراً گوش داده شده" else "Recently Played",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    TextButton(onClick = onOpenLibrary) {
                        Text(if (isFa) "آرشیو من" else "My Library", color = palette.accent, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(mergedRecentlyPlayed, key = { "recent_${it.id}" }) { track ->
                        val isOnline = track.id.startsWith("online_") || track.uri.startsWith("http")
                        val isSaved = downloadedIds.contains(track.id) ||
                            downloadedIds.contains(track.id.removePrefix("online_")) ||
                            OnlineCacheHolder.isTrackCached(context, track.id) ||
                            !isOnline

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
                                .clickable { onPlayTrack(track, mergedRecentlyPlayed) }
                                .padding(12.dp)
                        ) {
                            Column {
                                Box(modifier = Modifier.size(124.dp)) {
                                    TrackArtworkThumbnail(
                                        artworkUri = track.artworkUri,
                                        accentColor = palette.primary,
                                        size = 124.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        iconSize = 38.dp,
                                        title = track.title,
                                        artist = track.artist,
                                        trackId = track.id
                                    )
                                    if (isOnline && !isSaved) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xCC070912))
                                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                                .clickable { onDownloadTrack(track) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "Download to Phone",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
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
                                    iconSize = 24.dp,
                                    title = track.title,
                                    artist = track.artist,
                                    trackId = track.id
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

    // The single primary dominant alive color extracted directly from the album cover artwork
    val dominantCoverColor = if (palette.isMonochrome) {
        Color(0xFFE2E8F0) // Pure luminous silver-white for black & white covers (NO spurious blue/cyan)
    } else {
        palette.primary
    }

    // Smooth color animation when artwork/palette changes
    val animDominantColor by androidx.compose.animation.animateColorAsState(
        targetValue = dominantCoverColor,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "heroDominantColor"
    )

    // چرخش مداوم و پیوسته: بدون هیچ پرش ناگهانی به زاویه ۰ یا ریست شدن مسیر
    var continuousHeroAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastNanos = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dtSec = ((now - lastNanos).coerceAtLeast(0L)) / 1_000_000_000f
                    // ۹۰ درجه بر ثانیه = چرخش کامل ۴ ثانیه‌ای فوق‌العاده نرم و مداوم
                    continuousHeroAngle = (continuousHeroAngle + 90f * dtSec) % 360000f
                }
                lastNanos = now
            }
        }
    }
    val rotationAngle = continuousHeroAngle

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
                spotColor = animDominantColor.copy(alpha = 0.85f),
                ambientColor = animDominantColor.copy(alpha = 0.40f)
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

        // Layer 2: Rotating Single-Color Dynamic Sweep Gradient Beam
        // Beautiful contrast: intense glowing head (پررنگ) transitioning to faint/transparent tail (کمرنگ)
        val sweepColors = remember(animDominantColor) {
            listOf(
                animDominantColor.copy(alpha = 0.98f), // Apex / head: bright, punchy, alive
                animDominantColor.copy(alpha = 0.70f), // Leading wing
                animDominantColor.copy(alpha = 0.28f), // Soft falloff
                animDominantColor.copy(alpha = 0.04f), // Distant tail (کمرنگ)
                Color.Transparent,                    // Clear void
                animDominantColor.copy(alpha = 0.06f), // Ambient return
                animDominantColor.copy(alpha = 0.45f), // Ramping back up
                animDominantColor.copy(alpha = 0.98f)  // Seamless closing loop
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

        // Layer 4: Glassmorphic frost overlay with elegant single-cover-color border glow
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
                            animDominantColor.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.40f),
                            animDominantColor.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.10f),
                            animDominantColor.copy(alpha = 0.85f)
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
                // Static compact album artwork thumbnail
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
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
                            MusicArtworkPlaceholder(
                                title = heroTrack?.title,
                                artist = heroTrack?.artist,
                                trackId = heroTrack?.id,
                                palette = palette,
                                modifier = Modifier.fillMaxSize()
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
                        .shadow(14.dp, CircleShape, spotColor = animDominantColor)
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
                        tint = animDominantColor,
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
