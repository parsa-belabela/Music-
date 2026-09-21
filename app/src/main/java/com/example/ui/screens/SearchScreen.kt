package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    onCreatePlaylist: (String) -> Unit = {},
    onCreatePlaylistWithTracks: (String, List<String>) -> Unit = { name, _ -> onCreatePlaylist(name) },
    onDeletePlaylist: (String) -> Unit = {},
    onAddToPlaylist: (Playlist, Track) -> Unit = { _, _ -> },
    onRemoveTrackFromPlaylist: (String, String) -> Unit = { _, _ -> },
    onEditMetadata: (Track) -> Unit = {},
    onDeleteTrack: (Track) -> Unit = {},
    onPlayTrackList: (List<Track>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlaylistFilter by remember { mutableStateOf<String?>(null) }
    var selectedDetailPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var selectedTrackMenu by remember { mutableStateOf<Track?>(null) }
    var disintegratingTrackId by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showSongSelectionDialog by remember { mutableStateOf(false) }
    var pendingPlaylistName by remember { mutableStateOf("") }
    var newPlaylistNameInput by remember { mutableStateOf("") }
    var selectedTrackIdsForNewPlaylist by remember { mutableStateOf(setOf<String>()) }
    var songSelectionSearchQuery by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val lang = appSettings.language

    // Filter tracks dynamically based on search query or selected playlist
    val filteredTracks = remember(tracks, searchQuery, selectedPlaylistFilter, playlists) {
        tracks.filter { track ->
            val matchesQuery = if (searchQuery.isBlank()) true else {
                track.title.contains(searchQuery, ignoreCase = true) ||
                        track.artist.contains(searchQuery, ignoreCase = true) ||
                        track.album.contains(searchQuery, ignoreCase = true) ||
                        track.genre.contains(searchQuery, ignoreCase = true)
            }
            val matchesPlaylist = if (selectedPlaylistFilter == null) true else {
                val targetPlaylist = playlists.firstOrNull { it.name == selectedPlaylistFilter }
                targetPlaylist?.trackIds?.contains(track.id) == true
            }
            matchesQuery && matchesPlaylist
        }
    }

    val vibrantPlaylistColors = listOf(
        listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)),
        listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
        listOf(Color(0xFFF43F5E), Color(0xFFFB7185)),
        listOf(Color(0xFF10B981), Color(0xFF34D399)),
        listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)),
        listOf(Color(0xFF38BDF8), Color(0xFF818CF8)),
        listOf(Color(0xFFA855F7), Color(0xFF6366F1)),
        listOf(Color(0xFFEC4899), Color(0xFFF43F5E))
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
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

                // Modern Liquid Glass Search Field
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

        // Active Filter Pill
        if (selectedPlaylistFilter != null) {
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
                                Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedPlaylistFilter!!,
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
                                    .clickable { selectedPlaylistFilter = null }
                            )
                        }
                    }
                }
            }
        }

        // Search Results or Interactive Playlists Grid
        if (searchQuery.isNotBlank() || selectedPlaylistFilter != null) {
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
            // Playlists Header with Clean Static Liquid Glass "+" Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Localization.getString("playlists", lang),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    // Static, Elegant Liquid Glass Button for Creating Playlists
                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(14.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.primary,
                                tintAlpha = 0.18f,
                                borderWidth = 1.dp
                            )
                            .clickable { showCreatePlaylistDialog = true }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create Playlist",
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = Localization.getString("create_playlist", lang),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // User Playlists / Smart Playlists with Liquid Glass Glow
            if (playlists.isNotEmpty()) {
                items(playlists.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEachIndexed { index, playlist ->
                            val gradientColors = vibrantPlaylistColors[index % vibrantPlaylistColors.size]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(104.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(20.dp),
                                        thickness = GlassThickness.REGULAR,
                                        tintColor = gradientColors[0],
                                        tintAlpha = 0.22f,
                                        borderWidth = 1.2.dp
                                    )
                                    .clickable {
                                        selectedDetailPlaylist = playlist
                                    }
                                    .padding(14.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(gradientColors[0].copy(alpha = 0.35f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlaylistPlay,
                                                contentDescription = null,
                                                tint = gradientColors[0],
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Black.copy(alpha = 0.35f)
                                        ) {
                                            Text(
                                                text = "${playlist.trackIds.size} ${Localization.getString("tracks", lang)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFFD4D4E8),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
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
            } else {
                // Empty state when no playlists yet: clean liquid glass invitation card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.primary,
                                tintAlpha = 0.14f,
                                borderWidth = 1.dp
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(palette.primary.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "هنوز پلی‌لیستی ایجاد نکرده‌اید" else "No playlists created yet",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "با زدن دکمه «ایجاد پلی‌لیست جدید» اولین لیست پخش اختصاصی خود را بسازید" else "Tap 'Create New Playlist' above to craft your first collection",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFA0A0B8),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Step 1: Liquid Glass Modal Dialog to Enter Playlist Name
    if (showCreatePlaylistDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistNameInput = ""
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC06060C))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            showCreatePlaylistDialog = false
                            newPlaylistNameInput = ""
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { /* consume */ }
                        )
                        .liquidGlass(
                            shape = RoundedCornerShape(24.dp),
                            thickness = GlassThickness.THICK,
                            tintColor = palette.primary,
                            tintAlpha = 0.28f,
                            borderWidth = 1.4.dp
                        )
                        .padding(22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = Localization.getString("create_playlist", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }

                            IconButton(
                                onClick = {
                                    showCreatePlaylistDialog = false
                                    newPlaylistNameInput = ""
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA0A0B8), modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = Localization.getString("new_playlist_hint", lang),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8))
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(14.dp),
                                    thickness = GlassThickness.THIN,
                                    tintColor = Color(0xFF1E1E34),
                                    tintAlpha = 0.3f,
                                    borderWidth = 1.dp
                                )
                                .padding(horizontal = 14.dp, vertical = 2.dp)
                        ) {
                            TextField(
                                value = newPlaylistNameInput,
                                onValueChange = { newPlaylistNameInput = it },
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        if (lang == AppLanguage.PERSIAN) "نام پلی‌لیست (مثال: ریمیکس‌های شبانه)" else "Playlist name (e.g. Midnight Waves)",
                                        color = Color(0xFF7A7A90),
                                        fontSize = 14.sp
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showCreatePlaylistDialog = false
                                    newPlaylistNameInput = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = Localization.getString("cancel", lang),
                                    color = Color(0xFFA0A0B8)
                                )
                            }

                            Button(
                                onClick = {
                                    if (newPlaylistNameInput.isNotBlank()) {
                                        pendingPlaylistName = newPlaylistNameInput.trim()
                                        selectedTrackIdsForNewPlaylist = emptySet()
                                        songSelectionSearchQuery = ""
                                        showCreatePlaylistDialog = false
                                        newPlaylistNameInput = ""
                                        showSongSelectionDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) "انتخاب آهنگ‌ها" else "Choose Songs",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Step 2: Liquid Glass Modal Dialog to Select Songs for the Playlist
    if (showSongSelectionDialog) {
        val candidateTracks = remember(tracks, songSelectionSearchQuery) {
            if (songSelectionSearchQuery.isBlank()) tracks
            else tracks.filter {
                it.title.contains(songSelectionSearchQuery, ignoreCase = true) ||
                        it.artist.contains(songSelectionSearchQuery, ignoreCase = true) ||
                        it.album.contains(songSelectionSearchQuery, ignoreCase = true)
            }
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                showSongSelectionDialog = false
                selectedTrackIdsForNewPlaylist = emptySet()
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD06060C)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .fillMaxHeight(0.85f)
                        .liquidGlass(
                            shape = RoundedCornerShape(26.dp),
                            thickness = GlassThickness.THICK,
                            tintColor = palette.primary,
                            tintAlpha = 0.28f,
                            borderWidth = 1.4.dp
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "افزودن آهنگ به پلی‌لیست" else "Select Songs for Playlist",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = pendingPlaylistName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = palette.accent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            // Selected Counter Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.primary.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, palette.accent.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "${selectedTrackIdsForNewPlaylist.size} انتخاب شده" else "${selectedTrackIdsForNewPlaylist.size} selected",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Search Field for song filtering
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(14.dp),
                                    thickness = GlassThickness.THIN,
                                    tintColor = Color(0xFF1B1B2F),
                                    tintAlpha = 0.25f,
                                    borderWidth = 1.dp
                                )
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                TextField(
                                    value = songSelectionSearchQuery,
                                    onValueChange = { songSelectionSearchQuery = it },
                                    singleLine = true,
                                    placeholder = {
                                        Text(
                                            if (lang == AppLanguage.PERSIAN) "جستجوی آهنگ..." else "Search tracks...",
                                            color = Color(0xFF7A7A90),
                                            fontSize = 13.sp
                                        )
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Track List with Checkboxes
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (candidateTracks.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (lang == AppLanguage.PERSIAN) "آهنگی یافت نشد" else "No tracks found",
                                            color = Color(0xFFA0A0B8),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            } else {
                                items(candidateTracks, key = { it.id }) { track ->
                                    val isSelected = track.id in selectedTrackIdsForNewPlaylist
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .liquidGlass(
                                                shape = RoundedCornerShape(14.dp),
                                                thickness = GlassThickness.THIN,
                                                tintColor = if (isSelected) palette.accent else Color(0xFF161628),
                                                tintAlpha = if (isSelected) 0.22f else 0.12f,
                                                borderWidth = if (isSelected) 1.2.dp else 0.8.dp
                                            )
                                            .clickable {
                                                selectedTrackIdsForNewPlaylist = if (isSelected) {
                                                    selectedTrackIdsForNewPlaylist - track.id
                                                } else {
                                                    selectedTrackIdsForNewPlaylist + track.id
                                                }
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TrackArtworkThumbnail(
                                                artworkUri = track.artworkUri,
                                                accentColor = palette.secondary,
                                                size = 42.dp,
                                                shape = RoundedCornerShape(10.dp),
                                                iconSize = 20.dp
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

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
                                                    text = track.artist,
                                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    selectedTrackIdsForNewPlaylist = if (checked) {
                                                        selectedTrackIdsForNewPlaylist + track.id
                                                    } else {
                                                        selectedTrackIdsForNewPlaylist - track.id
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = palette.accent,
                                                    checkmarkColor = Color.Black,
                                                    uncheckedColor = Color(0xFF55556E)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Action Buttons: Save Playlist & Cancel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showSongSelectionDialog = false
                                    selectedTrackIdsForNewPlaylist = emptySet()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = Localization.getString("cancel", lang),
                                    color = Color(0xFFA0A0B8)
                                )
                            }

                            Button(
                                onClick = {
                                    onCreatePlaylistWithTracks(
                                        pendingPlaylistName,
                                        selectedTrackIdsForNewPlaylist.toList()
                                    )
                                    showSongSelectionDialog = false
                                    selectedTrackIdsForNewPlaylist = emptySet()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "ذخیره پلی‌لیست (${selectedTrackIdsForNewPlaylist.size})" else "Save (${selectedTrackIdsForNewPlaylist.size} tracks)",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Playlist Details Modal Sheet
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
