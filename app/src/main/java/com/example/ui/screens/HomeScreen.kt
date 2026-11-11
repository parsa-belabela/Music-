package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.OnlineCacheHolder
import com.example.OnlineMusicRepository
import com.example.audio.AmbientPalette
import com.example.data.model.*
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import java.util.Calendar
import kotlinx.coroutines.launch

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
    bottomPadding: Dp = 120.dp,
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
    val downloadedIds by OnlineCacheHolder.downloadedTrackIds.collectAsState()

    val mergedRecentlyPlayed = remember(onlineRecentTracks, recentlyPlayed) {
        (onlineRecentTracks + recentlyPlayed)
            .distinctBy { "${it.title.lowercase().trim()}|${it.artist.lowercase().trim()}" }
            .take(16)
    }

    // 6-item or 8-item Quick-Access Spotify Grid at the top of Home
    val quickGridTracks = remember(mergedRecentlyPlayed, favoriteTracks, allTracks) {
        (mergedRecentlyPlayed + favoriteTracks + allTracks)
            .distinctBy { it.id }
            .take(6)
    }

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingText = remember(currentHour, isFa) {
        when (currentHour) {
            in 5..11 -> if (isFa) "صبح بخیر" else "Good morning"
            in 12..16 -> if (isFa) "ظهر بخیر" else "Good afternoon"
            in 17..21 -> if (isFa) "عصر بخیر" else "Good evening"
            else -> if (isFa) "شب بخیر" else "Good night"
        }
    }

    var userShuffledHeroTrack by remember { mutableStateOf<Track?>(null) }
    val halfHourBucket = remember { System.currentTimeMillis() / SPOTLIGHT_REFRESH_INTERVAL_MS }
    val sessionRandomTrack = remember(allTracks.isNotEmpty(), onlineRecentTracks.isNotEmpty(), halfHourBucket) {
        val pool = (allTracks + onlineRecentTracks).ifEmpty { favoriteTracks }
        pool.shuffled(java.util.Random(halfHourBucket)).firstOrNull()
    }
    val heroTrack = userShuffledHeroTrack
        ?: playbackState.currentTrack
        ?: mergedRecentlyPlayed.firstOrNull()
        ?: sessionRandomTrack
        ?: favoriteTracks.firstOrNull()
        ?: allTracks.firstOrNull()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 14.dp,
            bottom = maxOf(bottomPadding + 24.dp, 128.dp)
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Spotify Top Header & Filter Pills (All, Gym Mode, Stats, Library)
        item(key = "home_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 23.sp
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenWrapped,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Listening Stats",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenLibrary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History & Library",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Filter Pills Row (Spotify Architecture)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpotifyFilterChip(
                        label = if (isFa) "همه" else "All",
                        selected = !appSettings.gymModeEnabled,
                        onClick = {
                            if (appSettings.gymModeEnabled) onToggleGymMode()
                        }
                    )

                    SpotifyFilterChip(
                        label = if (isFa) {
                            if (appSettings.gymModeEnabled) "حالت باشگاه • فعال" else "حالت باشگاه"
                        } else {
                            if (appSettings.gymModeEnabled) "Gym Mode • ON" else "Gym Mode"
                        },
                        selected = appSettings.gymModeEnabled,
                        leadingIcon = Icons.Default.FitnessCenter,
                        onClick = onToggleGymMode,
                        modifier = Modifier.testTag("gym_mode_toggle_btn")
                    )

                    if (duplicateCount > 0) {
                        SpotifyFilterChip(
                            label = if (isFa) "$duplicateCount تکراری" else "$duplicateCount Duplicates",
                            selected = false,
                            leadingIcon = Icons.Default.CleaningServices,
                            onClick = onOpenDuplicatesReview
                        )
                    }
                }
            }
        }

        // 2. Spotify 2-Column Compact Quick-Access Grid
        if (quickGridTracks.isNotEmpty()) {
            item(key = "home_quick_grid") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = quickGridTracks.chunked(2)
                    rows.forEach { rowTracks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTracks.forEach { track ->
                                val isPlayingThis = playbackState.currentTrack?.id == track.id &&
                                    playbackState.status == PlayerStatus.PLAYING
                                SpotifyQuickGridItem(
                                    track = track,
                                    isPlayingThis = isPlayingThis,
                                    palette = palette,
                                    onClick = { onPlayTrack(track, quickGridTracks) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowTracks.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 3. Featured Now Playing / Spotlight Card (Clean Editorial Layout)
        if (heroTrack != null) {
            item(key = "home_featured_card") {
                SpotifyFeaturedCard(
                    heroTrack = heroTrack,
                    playbackState = playbackState,
                    palette = palette,
                    isFa = isFa,
                    hasActiveTrack = playbackState.currentTrack != null,
                    onPlayOrToggle = {
                        if (playbackState.currentTrack?.id == heroTrack.id) {
                            onTogglePlay()
                        } else {
                            val pool = mergedRecentlyPlayed.ifEmpty { allTracks.ifEmpty { listOf(heroTrack) } }
                            onPlayTrack(heroTrack, pool)
                        }
                    },
                    onShuffleNextHero = {
                        val pool = (allTracks + onlineRecentTracks + mergedRecentlyPlayed + favoriteTracks).distinctBy { it.id }
                        if (pool.size > 1) {
                            userShuffledHeroTrack = pool.filter { it.id != heroTrack.id }.shuffled().firstOrNull()
                        }
                    },
                    onExpandNowPlaying = onExpandNowPlaying,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 4. Suggested Songs for Today (Horizontal Spotify Shelf)
        item(key = "home_suggested_tracks") {
            HomeCloudflareSpotlightSection(
                cloudflareTracks = onlineTrendingTracks,
                isFa = isFa,
                onPlayTrack = onPlayTrack
            )
        }

        // 5. Recently Played Shelf
        if (mergedRecentlyPlayed.isNotEmpty()) {
            item(key = "home_recently_played") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SpotifySectionHeader(
                        title = if (isFa) "اخیراً گوش داده شده" else "Recently played",
                        actionText = if (isFa) "نمایش همه" else "Show all",
                        onActionClick = onOpenLibrary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(mergedRecentlyPlayed, key = { "recent_${it.id}" }) { track ->
                            val isOnline = track.id.startsWith("online_") || track.uri.startsWith("http")
                            val isSaved = downloadedIds.contains(track.id) ||
                                downloadedIds.contains(track.id.removePrefix("online_")) ||
                                OnlineCacheHolder.isTrackCached(context, track.id) ||
                                !isOnline

                            SpotifyAlbumCard(
                                track = track,
                                accentColor = palette.primary,
                                showDownloadBadge = isOnline && !isSaved,
                                onClick = { onPlayTrack(track, mergedRecentlyPlayed) },
                                onDownloadClick = { onDownloadTrack(track) }
                            )
                        }
                    }
                }
            }
        }

        // 6. Favorites List / Shelf
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "آهنگ‌های مورد علاقه" else "Liked Songs",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    if (favoriteTracks.isNotEmpty()) {
                        Text(
                            text = if (isFa) "${favoriteTracks.size} آهنگ" else "${favoriteTracks.size} songs",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (favoriteTracks.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        favoriteTracks.take(10).forEach { track ->
                            val isCurrent = playbackState.currentTrack?.id == track.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPlayTrack(track, favoriteTracks) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrackArtworkThumbnail(
                                    artworkUri = track.artworkUri,
                                    accentColor = palette.primary,
                                    size = 48.dp,
                                    shape = RoundedCornerShape(6.dp),
                                    iconSize = 22.dp,
                                    title = track.title,
                                    artist = track.artist,
                                    trackId = track.id
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = if (isCurrent) AuraSpotifyGreen else Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = AuraSpotifyGreen,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(18.dp)
                                )
                                Text(
                                    text = track.durationFormatted,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AuraSurface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isFa) "هنوز آهنگی به علاقه‌مندی‌ها اضافه نشده است" else "Songs you like will appear here",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val bgColor = if (selected) AuraSpotifyGreen else AuraSurfaceElevated
    val contentColor = if (selected) Color.Black else Color.White

    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SpotifyQuickGridItem(
    track: Track,
    isPlayingThis: Boolean,
    palette: AmbientPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AuraSurfaceElevated)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtworkThumbnail(
            artworkUri = track.artworkUri,
            accentColor = palette.primary,
            size = 56.dp,
            shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
            iconSize = 24.dp,
            title = track.title,
            artist = track.artist,
            trackId = track.id
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (isPlayingThis) AuraSpotifyGreen else Color.White,
                fontSize = 12.5.sp,
                lineHeight = 15.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp)
        )
        if (isPlayingThis) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Playing",
                tint = AuraSpotifyGreen,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun SpotifyFeaturedCard(
    heroTrack: Track,
    playbackState: PlaybackState,
    palette: AmbientPalette,
    isFa: Boolean,
    hasActiveTrack: Boolean,
    onPlayOrToggle: () -> Unit,
    onShuffleNextHero: () -> Unit,
    onExpandNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPlaying = playbackState.status == PlayerStatus.PLAYING &&
        playbackState.currentTrack?.id == heroTrack.id

    val heroMotion = rememberInfiniteTransition(label = "heroCardMotion")
    val swayX by heroMotion.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "heroSwayX"
    )
    val swayY by heroMotion.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "heroSwayY"
    )
    val tilt by heroMotion.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "heroTilt"
    )
    val glowSweep by heroMotion.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "heroGlowSweep"
    )
    val playFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "heroPlayFactor"
    )

    val cardGlowColor = if (palette.isMonochrome) AuraSpotifyGreen else palette.primary.copy(alpha = 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuraSurface)
            .border(
                width = 1.dp,
                color = if (isPlaying) cardGlowColor.copy(alpha = 0.42f) else Color(0x18FFFFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable {
                if (hasActiveTrack && playbackState.currentTrack?.id == heroTrack.id) {
                    onExpandNowPlaying()
                } else {
                    onPlayOrToggle()
                }
            }
            .testTag("hero_quick_play_card")
    ) {
        // Moving Backlight Glow behind the Featured Card
        Canvas(modifier = Modifier.matchParentSize()) {
            val glowCenterX = size.width * (0.18f + 0.55f * glowSweep * playFactor)
            val glowCenterY = size.height * 0.5f + (swayY * 2f * playFactor)
            val glowRad = (size.width * 0.65f).coerceAtLeast(120f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        cardGlowColor.copy(alpha = 0.22f + 0.26f * playFactor),
                        cardGlowColor.copy(alpha = 0.08f + 0.10f * playFactor),
                        Color.Transparent
                    ),
                    center = Offset(glowCenterX, glowCenterY),
                    radius = glowRad
                ),
                center = Offset(glowCenterX, glowCenterY),
                radius = glowRad
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    translationX = swayX * playFactor
                    translationY = swayY * playFactor
                    rotationZ = tilt * playFactor
                }
            ) {
                TrackArtworkThumbnail(
                    artworkUri = heroTrack.artworkUri,
                    accentColor = palette.primary,
                    size = 76.dp,
                    shape = RoundedCornerShape(8.dp),
                    iconSize = 32.dp,
                    title = heroTrack.title,
                    artist = heroTrack.artist,
                    trackId = heroTrack.id
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPlaying) {
                        if (isFa) "در حال پخش" else "NOW PLAYING"
                    } else {
                        if (isFa) "پیشنهاد ویژه" else "FEATURED TRACK"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isPlaying) AuraSpotifyGreen else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = heroTrack.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = heroTrack.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onShuffleNextHero,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle Featured",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AuraSpotifyGreen)
                    .clickable(onClick = onPlayOrToggle)
                    .testTag("hero_play_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun SpotifySectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SpotifyAlbumCard(
    track: Track,
    accentColor: Color,
    showDownloadBadge: Boolean,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(142.dp)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.size(142.dp)) {
            TrackArtworkThumbnail(
                artworkUri = track.artworkUri,
                accentColor = accentColor,
                size = 142.dp,
                shape = RoundedCornerShape(8.dp),
                iconSize = 40.dp,
                title = track.title,
                artist = track.artist,
                trackId = track.id
            )
            if (showDownloadBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xD9121212))
                        .clickable(onClick = onDownloadClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save to device",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 13.5.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private const val SPOTLIGHT_REFRESH_INTERVAL_MS = 30 * 60 * 1000L
private var cachedSpotlightTracks: List<Track> = emptyList()
private var cachedSpotlightSlot: Long = -1L

@Composable
fun HomeCloudflareSpotlightSection(
    cloudflareTracks: List<Track> = emptyList(),
    isFa: Boolean = true,
    onPlayTrack: (Track, List<Track>) -> Unit
) {
    val repoTracks by OnlineMusicRepository.onlineTracksFlow.collectAsState()
    var loadedOnlineTracks by remember {
        mutableStateOf(OnlineMusicRepository.getMemoryCachedAppTracks())
    }
    var currentHalfHourSlot by remember {
        mutableLongStateOf(System.currentTimeMillis() / SPOTLIGHT_REFRESH_INTERVAL_MS)
    }

    LaunchedEffect(Unit) {
        if (loadedOnlineTracks.isEmpty() && repoTracks.isEmpty() && cloudflareTracks.isEmpty()) {
            val cached = OnlineMusicRepository.loadCachedAppTracksAsync()
            if (cached.isNotEmpty()) {
                loadedOnlineTracks = cached
            } else {
                try {
                    val resp = OnlineMusicRepository.fetchFromWorker()
                    if (resp.tracks.isNotEmpty()) {
                        loadedOnlineTracks = resp.tracks.map { it.toAppTrack(it.resolvedStreamUrl()) }
                    }
                } catch (_: Exception) {}
            }
        }
        while (true) {
            kotlinx.coroutines.delay(30_000L)
            val newSlot = System.currentTimeMillis() / SPOTLIGHT_REFRESH_INTERVAL_MS
            if (newSlot != currentHalfHourSlot) {
                currentHalfHourSlot = newSlot
            }
        }
    }

    val displayPool = when {
        repoTracks.isNotEmpty() -> repoTracks
        loadedOnlineTracks.isNotEmpty() -> loadedOnlineTracks
        else -> cloudflareTracks
    }

    val spotlightTracks = remember(displayPool.isNotEmpty(), currentHalfHourSlot) {
        if (cachedSpotlightTracks.isNotEmpty() && cachedSpotlightSlot == currentHalfHourSlot) {
            cachedSpotlightTracks
        } else if (displayPool.isEmpty()) {
            cachedSpotlightTracks
        } else {
            val next = displayPool.shuffled(java.util.Random(currentHalfHourSlot)).take(14)
            cachedSpotlightTracks = next
            cachedSpotlightSlot = currentHalfHourSlot
            next
        }
    }

    if (spotlightTracks.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SpotifySectionHeader(
                title = if (isFa) "پیشنهاد امروز برای شما" else "Made For You Today"
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(spotlightTracks, key = { "home_cf_${it.id}" }) { track ->
                    SpotifyAlbumCard(
                        track = track,
                        accentColor = AuraSpotifyGreen,
                        showDownloadBadge = false,
                        onClick = { onPlayTrack(track, spotlightTracks) },
                        onDownloadClick = {}
                    )
                }
            }
        }
    }
}

