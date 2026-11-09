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
import com.example.ui.components.SpotifyArtistAvatar
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

    var userShuffledHeroTrack by remember { mutableStateOf<Track?>(null) }
    val sessionRandomTrack = remember(allTracks.size, onlineRecentTracks.size) {
        val pool = (allTracks + onlineRecentTracks).ifEmpty { favoriteTracks }
        pool.shuffled().firstOrNull()
    }
    val heroTrack = userShuffledHeroTrack
        ?: playbackState.currentTrack
        ?: mergedRecentlyPlayed.firstOrNull()
        ?: sessionRandomTrack
        ?: favoriteTracks.firstOrNull()
        ?: allTracks.firstOrNull()

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
        if (heroTrack != null) {
            item {
                HeroQuickPlayCard(
                    heroTrack = heroTrack,
                    playbackState = playbackState,
                    palette = palette,
                    allTracks = if (allTracks.isNotEmpty()) allTracks else mergedRecentlyPlayed.ifEmpty { listOf(heroTrack) },
                    hasActiveTrack = playbackState.currentTrack != null,
                    appSettings = appSettings,
                    currentPositionProvider = currentPositionProvider,
                    onSeekTo = onSeekTo,
                    onPlayTrack = onPlayTrack,
                    onTogglePlay = {
                        if (playbackState.currentTrack != null) {
                            onTogglePlay()
                        } else {
                            onPlayTrack(heroTrack, mergedRecentlyPlayed.ifEmpty { allTracks.ifEmpty { listOf(heroTrack) } })
                        }
                    },
                    onShuffleNextHero = {
                        val pool = (allTracks + onlineRecentTracks + mergedRecentlyPlayed + favoriteTracks).distinctBy { it.id }
                        if (pool.size > 1) {
                            userShuffledHeroTrack = pool.filter { it.id != heroTrack.id }.shuffled().firstOrNull()
                        }
                    },
                    onExpandNowPlaying = onExpandNowPlaying
                )
            }
        }

        // بخش آهنگ‌های پیشنهادی برای امروز (بالای صفحه اصلی با بارگذاری فوری)
        item {
            HomeCloudflareSpotlightSection(
                cloudflareTracks = onlineTrendingTracks,
                isFa = isFa,
                onPlayTrack = onPlayTrack
            )
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
    onShuffleNextHero: () -> Unit = {},
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

    val heroTransition = rememberInfiniteTransition(label = "heroVinylRotation")
    val rotationAngle by if (isPlaying) {
        heroTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "heroRotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

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

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onShuffleNextHero,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle another track",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

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

/**
 * ویجت آهنگ‌های پیشنهادی برای امروز در بالای صفحه اصلی با بارگذاری فوری از کش حافظه/دیسک
 */
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
    var shuffleSeed by remember { mutableIntStateOf(0) }

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
    }

    val displayPool = when {
        repoTracks.isNotEmpty() -> repoTracks
        loadedOnlineTracks.isNotEmpty() -> loadedOnlineTracks
        else -> cloudflareTracks
    }

    val spotlightTracks = remember(displayPool.size, shuffleSeed) {
        if (displayPool.isEmpty()) emptyList()
        else displayPool.shuffled().take(14)
    }

    // پیش‌بارگذاری فوری کاورهای آهنگ‌های پیشنهادی امروز در پس‌زمینه
    LaunchedEffect(spotlightTracks) {
        spotlightTracks.take(8).forEach { track ->
            val url = track.artworkUri
            if (!url.isNullOrBlank()) {
                launch(kotlinx.coroutines.Dispatchers.IO) {
                    OnlineMusicRepository.loadCoverBitmap(url)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isFa) "آهنگ‌های پیشنهادی برای امروز" else "Suggested Songs for Today",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isFa) "منتخب روزانه متناسب با سلیقه شما" else "Daily picks tailored for you",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            if (spotlightTracks.isNotEmpty()) {
                TextButton(onClick = { shuffleSeed++ }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isFa) "پیشنهاد جدید" else "Refresh",
                        color = Color(0xFF1DB954),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (spotlightTracks.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(spotlightTracks, key = { "home_cf_${it.id}" }) { track ->
                    Box(
                        modifier = Modifier
                            .width(148.dp)
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = Color(0xFF1DB954),
                                tintAlpha = 0.16f,
                                borderWidth = 1.dp
                            )
                            .clickable { onPlayTrack(track, spotlightTracks) }
                            .padding(12.dp)
                    ) {
                        Column {
                            TrackArtworkThumbnail(
                                artworkUri = track.artworkUri,
                                accentColor = Color(0xFF1DB954),
                                size = 124.dp,
                                shape = RoundedCornerShape(14.dp),
                                iconSize = 38.dp,
                                title = track.title,
                                artist = track.artist,
                                trackId = track.id
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
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1DB954), fontSize = 12.sp),
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
