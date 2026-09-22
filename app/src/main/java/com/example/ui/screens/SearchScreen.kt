package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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

enum class SearchFilterScope {
    ALL,
    SONGS,
    ARTISTS,
    ALBUMS,
    GENRES
}

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
    var selectedGenreFilter by remember { mutableStateOf<String?>(null) }
    var selectedTrackMenu by remember { mutableStateOf<Track?>(null) }
    var disintegratingTrackId by remember { mutableStateOf<String?>(null) }
    var selectedDetailPlaylist by remember { mutableStateOf<Playlist?>(null) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val lang = appSettings.language

    // Distinct Genres & Artists for quick exploration
    val distinctGenres = remember(tracks) {
        tracks.mapNotNull { it.genre.takeIf { g -> g.isNotBlank() && g != "Unknown Genre" } }
            .distinct()
            .take(12)
    }

    val distinctArtists = remember(tracks) {
        tracks.map { it.artist }.filter { it.isNotBlank() && it != "Unknown Artist" }.distinct().take(10)
    }

    // Filter tracks dynamically based on search query, scope, and genre
    val filteredTracks = remember(tracks, searchQuery, selectedScope, selectedGenreFilter) {
        tracks.filter { track ->
            val matchesGenre = if (selectedGenreFilter == null) true else track.genre.equals(selectedGenreFilter, ignoreCase = true)
            if (!matchesGenre) return@filter false

            if (searchQuery.isBlank()) {
                selectedGenreFilter != null
            } else {
                when (selectedScope) {
                    SearchFilterScope.ALL -> {
                        track.title.contains(searchQuery, ignoreCase = true) ||
                                track.artist.contains(searchQuery, ignoreCase = true) ||
                                track.album.contains(searchQuery, ignoreCase = true) ||
                                track.genre.contains(searchQuery, ignoreCase = true)
                    }
                    SearchFilterScope.SONGS -> track.title.contains(searchQuery, ignoreCase = true)
                    SearchFilterScope.ARTISTS -> track.artist.contains(searchQuery, ignoreCase = true)
                    SearchFilterScope.ALBUMS -> track.album.contains(searchQuery, ignoreCase = true)
                    SearchFilterScope.GENRES -> track.genre.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    val scopeLabels = listOf(
        SearchFilterScope.ALL to if (lang == AppLanguage.PERSIAN) "همه" else "All",
        SearchFilterScope.SONGS to if (lang == AppLanguage.PERSIAN) "آهنگ‌ها" else "Songs",
        SearchFilterScope.ARTISTS to if (lang == AppLanguage.PERSIAN) "هنرمندان" else "Artists",
        SearchFilterScope.ALBUMS to if (lang == AppLanguage.PERSIAN) "آلبوم‌ها" else "Albums",
        SearchFilterScope.GENRES to if (lang == AppLanguage.PERSIAN) "سبک‌ها" else "Genres"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = maxOf(bottomPadding + 20.dp, 120.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Localization.getString("nav_search", lang),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Liquid Glass Search Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(18.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.16f,
                            borderWidth = 1.dp
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp)
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
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF8E8E9F))
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
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFFAAAAAA),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Scope Filter Chips (All, Songs, Artists, Albums, Genres)
        item {
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
                                thickness = if (isSelected) GlassThickness.THICK else GlassThickness.REGULAR,
                                tintColor = if (isSelected) palette.accent else palette.primary,
                                tintAlpha = if (isSelected) 0.32f else 0.10f,
                                borderWidth = if (isSelected) 1.2.dp else 0.6.dp
                            )
                            .clickable { selectedScope = scope }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) palette.accent else Color(0xFFC0C0D4)
                            )
                        )
                    }
                }
            }
        }

        // Active Genre Filter Pill
        if (selectedGenreFilter != null) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = palette.accent.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, palette.accent.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedGenreFilter!!,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accent
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove Filter",
                                tint = palette.accent,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { selectedGenreFilter = null }
                            )
                        }
                    }
                }
            }
        }

        // When Query or Genre Filter is Active -> Show Search Results
        if (searchQuery.isNotBlank() || selectedGenreFilter != null) {
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
                                text = if (lang == AppLanguage.PERSIAN) "نتیجه‌ای برای جستجوی شما یافت نشد" else "No matching tracks found",
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
                    Text(
                        text = "${filteredTracks.size} ${Localization.getString("tracks", lang)}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = palette.accent,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                items(filteredTracks, key = { it.id }) { track ->
                    val isCurrent = playbackState.currentTrack?.id == track.id
                    val isDisintegrating = disintegratingTrackId == track.id

                    AnimatedVisibility(
                        visible = !isDisintegrating,
                        exit = fadeOut(tween(550)) + shrinkVertically(tween(550))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(16.dp),
                                    thickness = if (isCurrent) GlassThickness.THICK else GlassThickness.REGULAR,
                                    tintColor = if (isCurrent) palette.primary else Color(0xFF1E1E2E),
                                    tintAlpha = if (isCurrent) 0.25f else 0.08f,
                                    borderWidth = if (isCurrent) 1.2.dp else 0.5.dp,
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
                                    size = 48.dp,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) palette.accent else Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} • ${track.album}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(onClick = { onToggleFavorite(track) }) {
                                    Icon(
                                        if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (track.isFavorite) Color(0xFFF43F5E) else Color(0xFF75758C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(onClick = { selectedTrackMenu = track }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color(0xFF8888A0),
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
            // When Idle / No Query: Quick Exploration Sections (Genres & Artists)
            if (distinctGenres.isNotEmpty()) {
                item {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "کاوش بر اساس سبک" else "Explore by Genre",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(distinctGenres.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(16.dp),
                                        thickness = GlassThickness.REGULAR,
                                        tintColor = palette.secondary,
                                        tintAlpha = 0.16f,
                                        borderWidth = 1.dp
                                    )
                                    .clickable { selectedGenreFilter = genre }
                                    .padding(horizontal = 14.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(palette.secondary.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = palette.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

            if (distinctArtists.isNotEmpty()) {
                item {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "هنرمندان برتر" else "Top Artists",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(distinctArtists) { artist ->
                            Box(
                                modifier = Modifier
                                    .liquidGlass(
                                        shape = RoundedCornerShape(16.dp),
                                        thickness = GlassThickness.REGULAR,
                                        tintColor = palette.primary,
                                        tintAlpha = 0.16f,
                                        borderWidth = 1.dp
                                    )
                                    .clickable {
                                        searchQuery = artist
                                        selectedScope = SearchFilterScope.ARTISTS
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = artist,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Playlist Details Modal Sheet (if opened)
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

    // Track Action Sheet for Search Results
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
