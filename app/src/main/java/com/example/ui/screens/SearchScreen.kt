package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.PlaybackState
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.components.DisintegrationOverlay
import com.example.ui.components.PlaylistDetailSheet
import com.example.ui.components.TrackActionSheet
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization
import com.example.util.WrappedEngine

enum class SearchFilterScope {
    ALL,
    SONGS,
    ARTISTS,
    ALBUMS,
    GENRES
}

data class RealArtistSummary(
    val name: String,
    val tracks: List<Track>,
    val totalPlays: Int,
    val favoriteCount: Int,
    val artworkUri: String?,
    val score: Long
)

data class RealGenreBucket(
    val genreName: String,
    val displayTitleFa: String,
    val tracks: List<Track>,
    val totalPlays: Int,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    tracks: List<Track>,
    playlists: List<Playlist> = emptyList(),
    playbackState: PlaybackState,
    palette: AmbientPalette,
    appSettings: AppSettings,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onDeletePlaylist: (String) -> Unit = {},
    onAddToPlaylist: (Playlist, Track) -> Unit = { _, _ -> },
    onRemoveTrackFromPlaylist: (String, String) -> Unit = { _, _ -> },
    onEditMetadata: (Track) -> Unit = {},
    onDeleteTrack: (Track) -> Unit = {},
    onPlayTrackList: (List<Track>) -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedScope by remember { mutableStateOf(SearchFilterScope.ALL) }
    var selectedGenreBucket by remember { mutableStateOf<RealGenreBucket?>(null) }
    var selectedArtistProfile by remember { mutableStateOf<RealArtistSummary?>(null) }
    var quickSmartFilter by remember { mutableStateOf<String?>(null) } // "MOST_PLAYED", "FAVORITES", "HIRES"
    var selectedTrackMenu by remember { mutableStateOf<Track?>(null) }
    var disintegratingTrackId by remember { mutableStateOf<String?>(null) }
    var selectedDetailPlaylist by remember { mutableStateOf<Playlist?>(null) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val lang = appSettings.language
    val isFa = lang == AppLanguage.PERSIAN

    // ۱. محاسبه ۱۰۰٪ واقعی هنرمندان برتر بر اساس مجموع دفعات پخش، علاقه‌مندی‌ها و تعداد قطعات (بدون باگ حروف الفبای فارسی!)
    val rankedTopArtists = remember(tracks) {
        tracks
            .filter { it.artist.isNotBlank() && !it.artist.equals("Unknown Artist", ignoreCase = true) && !it.artist.equals("<unknown>", ignoreCase = true) }
            .groupBy { it.artist.trim() }
            .map { (artistName, artistTracks) ->
                val plays = artistTracks.sumOf { it.playCount }
                val favs = artistTracks.count { it.isFavorite }
                val recentBonus = if (artistTracks.any { it.lastPlayedTimestamp > 0L }) 25L else 0L
                val score = (plays * 100L) + (favs * 60L) + (artistTracks.size * 15L) + recentBonus
                val bestArtwork = artistTracks.firstOrNull { !it.artworkUri.isNullOrBlank() }?.artworkUri
                RealArtistSummary(
                    name = artistName,
                    tracks = artistTracks.sortedByDescending { it.playCount },
                    totalPlays = plays,
                    favoriteCount = favs,
                    artworkUri = bestArtwork,
                    score = score
                )
            }
            .sortedWith(compareByDescending<RealArtistSummary> { it.score }.thenByDescending { it.tracks.size })
    }

    // ۲. استخراج و دسته‌بندی ۱۰۰٪ واقعی ژانرها بر اساس متادیتا و تحلیل هوشمند آهنگ‌های موجود در دستگاه کاربر
    val realGenreBuckets = remember(tracks) {
        val genreColors = listOf(
            Color(0xFF8B5CF6),
            Color(0xFF06B6D4),
            Color(0xFFF43F5E),
            Color(0xFF10B981),
            Color(0xFFF59E0B),
            Color(0xFF3B82F6),
            Color(0xFFEC4899),
            Color(0xFF14B8A6)
        )

        tracks
            .groupBy { trk -> WrappedEngine.resolveRealGenreForStats(trk, trk.genre) }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { it.playCount + 1 } }
            .mapIndexed { idx, (genreName, genreTracks) ->
                val faTitle = when (genreName) {
                    "Hip-Hop & Rap" -> "هیپ‌هاپ و رپ جهانی"
                    "Persian Rap / Hip-Hop" -> "رپ و هیپ‌هاپ فارسی"
                    "Pop & Melodic" -> "پاپ و ملودیک"
                    "Rock & Alternative" -> "راک و آلترناتیو"
                    "R&B & Soul" -> "آر اند بی و سول"
                    "Chill & Ambient" -> "چیل، لوفای و امبینت"
                    "Soundtrack & Score" -> "موسیقی متن و بیکلام"
                    "Electronic & Synth" -> "الکترونیک و سینث"
                    else -> genreName
                }
                RealGenreBucket(
                    genreName = genreName,
                    displayTitleFa = faTitle,
                    tracks = genreTracks,
                    totalPlays = genreTracks.sumOf { it.playCount },
                    accentColor = genreColors[idx % genreColors.size]
                )
            }
    }

    // ۳. فیلتر هوشمند نتایج جستجو
    val filteredTracks = remember(tracks, searchQuery, selectedScope, selectedGenreBucket, selectedArtistProfile, quickSmartFilter) {
        val basePool = when {
            selectedArtistProfile != null -> selectedArtistProfile!!.tracks
            selectedGenreBucket != null -> selectedGenreBucket!!.tracks
            quickSmartFilter == "MOST_PLAYED" -> tracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.ifEmpty { tracks }
            quickSmartFilter == "FAVORITES" -> tracks.filter { it.isFavorite }
            quickSmartFilter == "HIRES" -> tracks.filter { it.bitrate >= 256 }.sortedByDescending { it.bitrate }
            else -> tracks
        }

        if (searchQuery.isBlank()) {
            if (selectedArtistProfile != null || selectedGenreBucket != null || quickSmartFilter != null) {
                basePool
            } else {
                emptyList()
            }
        } else {
            val q = searchQuery.trim()
            basePool.filter { track ->
                val resolvedG = WrappedEngine.resolveRealGenreForStats(track, track.genre)
                when (selectedScope) {
                    SearchFilterScope.ALL -> {
                        track.title.contains(q, ignoreCase = true) ||
                                track.artist.contains(q, ignoreCase = true) ||
                                track.album.contains(q, ignoreCase = true) ||
                                resolvedG.contains(q, ignoreCase = true)
                    }
                    SearchFilterScope.SONGS -> track.title.contains(q, ignoreCase = true)
                    SearchFilterScope.ARTISTS -> track.artist.contains(q, ignoreCase = true)
                    SearchFilterScope.ALBUMS -> track.album.contains(q, ignoreCase = true)
                    SearchFilterScope.GENRES -> resolvedG.contains(q, ignoreCase = true)
                }
            }
        }
    }

    val scopeLabels = listOf(
        SearchFilterScope.ALL to if (isFa) "همه" else "All",
        SearchFilterScope.SONGS to if (isFa) "آهنگ‌ها" else "Songs",
        SearchFilterScope.ARTISTS to if (isFa) "هنرمندان" else "Artists",
        SearchFilterScope.ALBUMS to if (isFa) "آلبوم‌ها" else "Albums",
        SearchFilterScope.GENRES to if (isFa) "ژانرها" else "Genres"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = maxOf(bottomPadding + 24.dp, 128.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // هدر و نوار جستجوی شیشه‌ای
        // =========================================================================
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isFa) "کاوش هوشمند در آرشیو شما" else "SONIC DISCOVERY & SEARCH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = palette.accent,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = Localization.getString("nav_search", lang),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.18f,
                            borderWidth = 1.2.dp
                        )
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = palette.accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = Localization.getString("search_hint", lang),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9096AC))
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = palette.accent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("search_input_field")
                        )
                        if (searchQuery.isNotEmpty() || selectedGenreBucket != null || selectedArtistProfile != null || quickSmartFilter != null) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedGenreBucket = null
                                    selectedArtistProfile = null
                                    quickSmartFilter = null
                                }
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // چیپ‌های دامنه جستجو + فیلترهای سریع هوشمند
        // =========================================================================
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(scopeLabels) { (scope, label) ->
                        val isSelected = selectedScope == scope
                        Box(
                            modifier = Modifier
                                .liquidGlass(
                                    shape = RoundedCornerShape(14.dp),
                                    thickness = if (isSelected) GlassThickness.THICK else GlassThickness.THIN,
                                    tintColor = if (isSelected) palette.accent else palette.primary,
                                    tintAlpha = if (isSelected) 0.32f else 0.10f,
                                    borderWidth = if (isSelected) 1.3.dp else 0.7.dp
                                )
                                .clickable { selectedScope = scope }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFFC0C5D8)
                                )
                            )
                        }
                    }
                }

                // فیلترهای سریع کاربردی
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val smartPills = listOf(
                        "MOST_PLAYED" to (if (isFa) "🔥 بیشترین پخش" else "🔥 Most Played"),
                        "FAVORITES" to (if (isFa) "❤️ علاقه‌مندی‌ها" else "❤️ Favorites"),
                        "HIRES" to (if (isFa) "⚡ کیفیت استودیویی (320kbps)" else "⚡ Studio Hi-Res")
                    )
                    items(smartPills) { (key, title) ->
                        val active = quickSmartFilter == key
                        FilterChip(
                            selected = active,
                            onClick = { quickSmartFilter = if (active) null else key },
                            label = { Text(title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary.copy(alpha = 0.45f),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x18FFFFFF),
                                labelColor = Color(0xFFD0D5E8)
                            )
                        )
                    }
                }
            }
        }

        // =========================================================================
        // هدر فعال زمانی که کاربر روی یک هنرمند یا ژانر کلیک کرده است
        // =========================================================================
        if (selectedArtistProfile != null || selectedGenreBucket != null) {
            item {
                val title = selectedArtistProfile?.name
                    ?: (if (isFa) selectedGenreBucket!!.displayTitleFa else selectedGenreBucket!!.genreName)
                val sub = if (selectedArtistProfile != null) {
                    if (isFa) "${selectedArtistProfile!!.tracks.size} آهنگ • ${selectedArtistProfile!!.totalPlays} بار پخش"
                    else "${selectedArtistProfile!!.tracks.size} Tracks • ${selectedArtistProfile!!.totalPlays} Plays"
                } else {
                    if (isFa) "${selectedGenreBucket!!.tracks.size} آهنگ در این سبک"
                    else "${selectedGenreBucket!!.tracks.size} Tracks in Genre"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(24.dp),
                            thickness = GlassThickness.THICK,
                            tintColor = selectedGenreBucket?.accentColor ?: palette.primary,
                            tintAlpha = 0.26f,
                            borderWidth = 1.4.dp
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            TrackArtworkThumbnail(
                                artworkUri = selectedArtistProfile?.artworkUri ?: filteredTracks.firstOrNull()?.artworkUri,
                                accentColor = palette.accent,
                                size = 60.dp,
                                shape = if (selectedArtistProfile != null) CircleShape else RoundedCornerShape(16.dp),
                                title = title
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = sub,
                                    color = palette.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // دکمه پخش همه قطعات این هنرمند یا ژانر
                            IconButton(
                                onClick = {
                                    if (filteredTracks.isNotEmpty()) {
                                        onPlayTrack(filteredTracks.first(), filteredTracks)
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(palette.accent)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = Color.Black)
                            }

                            IconButton(
                                onClick = {
                                    selectedArtistProfile = null
                                    selectedGenreBucket = null
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // نمایش نتایج جستجو یا فیلتر فعال
        // =========================================================================
        if (searchQuery.isNotBlank() || selectedGenreBucket != null || selectedArtistProfile != null || quickSmartFilter != null) {
            if (filteredTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (isFa) "نتیجه‌ای برای جستجوی شما یافت نشد" else "No matching tracks found",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredTracks.size} ${Localization.getString("tracks", lang)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = palette.accent,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        TextButton(
                            onClick = {
                                val shuffled = filteredTracks.shuffled()
                                shuffled.firstOrNull()?.let { onPlayTrack(it, shuffled) }
                            }
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFa) "پخش تصادفی نتایج" else "Shuffle Results", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(filteredTracks, key = { it.id }) { track ->
                    val isCurrent = playbackState.currentTrack?.id == track.id
                    val isDisintegrating = disintegratingTrackId == track.id
                    val realGenre = remember(track) { WrappedEngine.resolveRealGenreForStats(track, track.genre) }

                    AnimatedVisibility(
                        visible = !isDisintegrating,
                        exit = fadeOut(tween(550)) + shrinkVertically(tween(550))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    thickness = if (isCurrent) GlassThickness.THICK else GlassThickness.REGULAR,
                                    tintColor = if (isCurrent) palette.primary else Color(0xFF1A1D2E),
                                    tintAlpha = if (isCurrent) 0.28f else 0.10f,
                                    borderWidth = if (isCurrent) 1.3.dp else 0.7.dp,
                                    appTheme = appSettings.theme
                                )
                                .clickable { onPlayTrack(track, filteredTracks) }
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TrackArtworkThumbnail(
                                    artworkUri = track.artworkUri,
                                    accentColor = palette.accent,
                                    size = 50.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    title = track.title,
                                    artist = track.artist,
                                    trackId = track.id
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isCurrent) palette.accent else Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} • $realGenre",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA5ABC0)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(onClick = { onToggleFavorite(track) }) {
                                    Icon(
                                        if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (track.isFavorite) Color(0xFFF43F5E) else Color(0xFF888EA8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = { selectedTrackMenu = track }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color(0xFF9096AC),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isDisintegrating) {
                        DisintegrationOverlay(
                            isDisintegrating = true,
                            primaryColor = palette.primary,
                            accentColor = palette.accent,
                            onAnimationEnd = {
                                onDeleteTrack(track)
                                disintegratingTrackId = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        )
                    }
                }
            }
        } else {
            // =========================================================================
            // حالت کاوش (Explore): ۱. هنرمندان برتر واقعی بر اساس آمار پخش کاربر
            // =========================================================================
            if (rankedTopArtists.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isFa) "هنرمندان برتر شما (بر اساس آمار واقعی پخش)" else "Your Top Artists (Ranked by Real Plays)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isFa) "برای مشاهده تمام آهنگ‌های هر هنرمند لمس کنید" else "Tap any artist to open their track collection",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A6BC), fontSize = 11.5.sp)
                            )
                        }
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(rankedTopArtists.take(12), key = { it.name }) { artistItem ->
                            Box(
                                modifier = Modifier
                                    .width(136.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(22.dp),
                                        thickness = GlassThickness.REGULAR,
                                        tintColor = palette.primary,
                                        tintAlpha = 0.16f,
                                        borderWidth = 1.dp
                                    )
                                    .clickable { selectedArtistProfile = artistItem }
                                    .padding(14.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(contentAlignment = Alignment.BottomEnd) {
                                        TrackArtworkThumbnail(
                                            artworkUri = artistItem.artworkUri,
                                            accentColor = palette.accent,
                                            size = 76.dp,
                                            shape = CircleShape,
                                            title = artistItem.name,
                                            artist = artistItem.name
                                        )
                                        if (artistItem.totalPlays > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(palette.accent)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${artistItem.totalPlays}x",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.5.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = artistItem.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isFa) "${artistItem.tracks.size} آهنگ" else "${artistItem.tracks.size} tracks",
                                        color = Color(0xFFA5ABC0),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // حالت کاوش (Explore): ۲. دسته‌بندی واقعی ژانرها به همراه کاور و تعداد آهنگ‌ها
            // =========================================================================
            if (realGenreBuckets.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        Text(
                            text = if (isFa) "دسته‌بندی واقعی سبک‌ها و ژانرها" else "Authentic Genres & Sonic Moods",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (isFa) "استخراج‌شده از متادیتا و پروفایل صوتی آهنگ‌های شما" else "Classified from your library metadata & audio profiles",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A6BC), fontSize = 11.5.sp)
                        )
                    }
                }

                items(realGenreBuckets.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { bucket ->
                            val sampleArt = bucket.tracks.firstOrNull { !it.artworkUri.isNullOrBlank() }?.artworkUri
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(104.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(22.dp),
                                        thickness = GlassThickness.REGULAR,
                                        tintColor = bucket.accentColor,
                                        tintAlpha = 0.24f,
                                        borderWidth = 1.2.dp
                                    )
                                    .clickable { selectedGenreBucket = bucket }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isFa) bucket.displayTitleFa else bucket.genreName,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            color = bucket.accentColor.copy(alpha = 0.28f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (isFa) "${bucket.tracks.size} قطعه" else "${bucket.tracks.size} tracks",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    TrackArtworkThumbnail(
                                        artworkUri = sampleArt,
                                        accentColor = bucket.accentColor,
                                        size = 54.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        title = bucket.genreName
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    selectedDetailPlaylist?.let { pl ->
        PlaylistDetailSheet(
            playlist = pl,
            allTracks = tracks,
            settings = appSettings,
            palette = palette,
            onDismiss = { selectedDetailPlaylist = null },
            onPlayTrack = { trk, list -> onPlayTrack(trk, list) },
            onPlayAll = { list -> onPlayTrackList(list) },
            onShuffleAll = { list -> onPlayTrackList(list) },
            onRemoveTrackFromPlaylist = { plId, trkId -> onRemoveTrackFromPlaylist(plId, trkId) },
            onDeletePlaylist = { plId ->
                onDeletePlaylist(plId)
                selectedDetailPlaylist = null
            }
        )
    }

    selectedTrackMenu?.let { trk ->
        TrackActionSheet(
            track = trk,
            playlists = playlists,
            settings = appSettings,
            palette = palette,
            onDismiss = { selectedTrackMenu = null },
            onPlayNow = { onPlayTrack(it, tracks) },
            onPlayNext = { onPlayNext(it) },
            onAddToQueue = { onAddToQueue(it) },
            onToggleFavorite = { onToggleFavorite(it) },
            onAddToPlaylist = { pl, t -> onAddToPlaylist(pl, t) },
            onEditMetadata = { onEditMetadata(it) },
            onDeleteTrack = { t ->
                disintegratingTrackId = t.id
            }
        )
    }
}
