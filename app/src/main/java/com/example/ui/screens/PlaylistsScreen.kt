package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.components.PlaylistDetailSheet
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.components.TrackPickerSheet
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.util.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    allTracks: List<Track>,
    favoriteTracks: List<Track> = emptyList(),
    mostPlayedTracks: List<Track> = emptyList(),
    recentlyAddedTracks: List<Track> = emptyList(),
    palette: AmbientPalette,
    settings: AppSettings = AppSettings(),
    onCreatePlaylist: (String) -> Unit,
    onCreatePlaylistWithTracks: (String, List<String>) -> Unit = { name, _ -> onCreatePlaylist(name) },
    onAddTracksToPlaylist: (String, List<String>) -> Unit = { _, _ -> },
    onRenamePlaylist: (String, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit,
    onRemoveTrackFromPlaylist: (String, String) -> Unit = { _, _ -> },
    onPlayTrack: (Track, List<Track>) -> Unit = { _, _ -> },
    onPlayTrackList: (List<Track>) -> Unit,
    bottomPadding: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showSongPickerForNewPlaylist by remember { mutableStateOf(false) }
    var pendingPlaylistName by remember { mutableStateOf("") }
    var targetPlaylistForAdding by remember { mutableStateOf<Playlist?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var renamePlaylistNewName by remember { mutableStateOf("") }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var playlistSearchQuery by remember { mutableStateOf("") }
    val lang = settings.language
    val isFa = lang == AppLanguage.PERSIAN

    val selectedPlaylist = remember(selectedPlaylistId, playlists) {
        playlists.firstOrNull { it.id == selectedPlaylistId }
    }

    val filteredPlaylists = remember(playlists, playlistSearchQuery) {
        if (playlistSearchQuery.isBlank()) playlists
        else {
            val q = playlistSearchQuery.trim().lowercase()
            playlists.filter { it.name.lowercase().contains(q) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // 1. Spotify Playlists Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Localization.getString("playlists", lang),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 23.sp
                )
            )

            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AuraSpotifyGreen)
                    .clickable {
                        pendingPlaylistName = ""
                        showNameDialog = true
                    }
                    .padding(horizontal = 14.dp)
                    .testTag("create_playlist_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Playlist",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = Localization.getString("new_playlist", lang),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 12.5.sp
                )
            }
        }

        // 2. Search in Playlists
        if (playlists.size > 2) {
            OutlinedTextField(
                value = playlistSearchQuery,
                onValueChange = { playlistSearchQuery = it },
                placeholder = {
                    Text(
                        text = if (isFa) "جستجوی پلی‌لیست..." else "Find in playlists...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = AuraSurfaceElevated,
                    unfocusedContainerColor = AuraSurface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        // 3. Playlists List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = maxOf(bottomPadding + 24.dp, 128.dp)),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (playlists.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AuraSurface)
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(AuraSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = if (isFa) "اولین پلی‌لیست خود را بسازید" else "Create your first playlist",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isFa)
                                    "آهنگ‌های مورد علاقه‌تان را در مجموعه‌های دلخواه دسته‌بندی کنید."
                                else
                                    "It's easy, we'll help you organize your favorite songs.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    pendingPlaylistName = ""
                                    showNameDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text(
                                    text = Localization.getString("create_playlist", lang),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredPlaylists, key = { it.id }) { pl ->
                    val plTracks = remember(pl.trackIds, allTracks) {
                        val trackMap = allTracks.associateBy { it.id }
                        pl.trackIds.mapNotNull { trackMap[it] }
                    }
                    val firstArt = plTracks.firstOrNull()?.artworkUri

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedPlaylistId = pl.id }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AuraSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            if (firstArt != null) {
                                TrackArtworkThumbnail(
                                    artworkUri = firstArt,
                                    accentColor = palette.primary,
                                    size = 56.dp,
                                    shape = RoundedCornerShape(6.dp),
                                    iconSize = 26.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 15.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${pl.trackIds.size} ${Localization.getString("tracks", lang)}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (plTracks.isNotEmpty()) {
                                IconButton(
                                    onClick = { onPlayTrackList(plTracks) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleFilled,
                                        contentDescription = "Play",
                                        tint = AuraSpotifyGreen,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            var menuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.background(AuraSurfaceElevated)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isFa) "افزودن آهنگ" else "Add Songs",
                                                color = Color.White
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.PlaylistAdd,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            targetPlaylistForAdding = pl
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isFa) "تغییر نام" else "Rename",
                                                color = Color.White
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            renamePlaylistNewName = pl.name
                                            playlistToRename = pl
                                        }
                                    )

                                    HorizontalDivider(color = Color(0x18FFFFFF))

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isFa) "حذف پلی‌لیست" else "Delete Playlist",
                                                color = Color(0xFFEF4444)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onDeletePlaylist(pl.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPlaylist?.let { pl ->
        PlaylistDetailSheet(
            playlist = pl,
            allTracks = allTracks,
            favoriteTracks = favoriteTracks,
            settings = settings,
            palette = palette,
            onDismiss = { selectedPlaylistId = null },
            onPlayTrack = { trk, list -> onPlayTrack(trk, list) },
            onPlayAll = { list -> onPlayTrackList(list) },
            onShuffleAll = { list -> onPlayTrackList(list) },
            onAddTracksToPlaylist = { plId, trackIds ->
                onAddTracksToPlaylist(plId, trackIds)
            },
            onRemoveTrackFromPlaylist = { plId, trkId -> onRemoveTrackFromPlaylist(plId, trkId) },
            onRenamePlaylist = { plId, newName -> onRenamePlaylist(plId, newName) },
            onDeletePlaylist = { plId ->
                onDeletePlaylist(plId)
                selectedPlaylistId = null
            }
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = AuraSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = Localization.getString("create_playlist", lang),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFa)
                            "نام پلی‌لیست جدید را وارد کنید و سپس آهنگ‌های دلخواه را انتخاب نمایید."
                        else
                            "Give your playlist a name, then select tracks to include.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    OutlinedTextField(
                        value = pendingPlaylistName,
                        onValueChange = { pendingPlaylistName = it },
                        label = { Text(Localization.getString("playlist_name", lang)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AuraSpotifyGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pendingPlaylistName.isNotBlank()) {
                            showNameDialog = false
                            showSongPickerForNewPlaylist = true
                        }
                    },
                    enabled = pendingPlaylistName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraSpotifyGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isFa) "انتخاب آهنگ‌ها" else "Select Songs",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(Localization.getString("cancel", lang), color = TextSecondary)
                }
            }
        )
    }

    if (showSongPickerForNewPlaylist) {
        TrackPickerSheet(
            allTracks = allTracks,
            favoriteTracks = favoriteTracks,
            initialSelectedTrackIds = emptySet(),
            playlistTitle = pendingPlaylistName,
            titleText = if (isFa) "افزودن آهنگ به «$pendingPlaylistName»" else "Add Songs to \"$pendingPlaylistName\"",
            confirmButtonText = if (isFa) "ایجاد پلی‌لیست" else "Create Playlist",
            settings = settings,
            palette = palette,
            onDismiss = { showSongPickerForNewPlaylist = false },
            onConfirm = { selectedIds ->
                if (pendingPlaylistName.isNotBlank()) {
                    onCreatePlaylistWithTracks(pendingPlaylistName.trim(), selectedIds)
                }
                showSongPickerForNewPlaylist = false
                pendingPlaylistName = ""
            }
        )
    }

    targetPlaylistForAdding?.let { pl ->
        TrackPickerSheet(
            allTracks = allTracks,
            favoriteTracks = favoriteTracks,
            initialSelectedTrackIds = pl.trackIds.toSet(),
            playlistTitle = pl.name,
            titleText = if (isFa) "افزودن آهنگ به «${pl.name}»" else "Add Songs to \"${pl.name}\"",
            confirmButtonText = if (isFa) "ذخیره تغییرات" else "Save Changes",
            settings = settings,
            palette = palette,
            onDismiss = { targetPlaylistForAdding = null },
            onConfirm = { selectedIds ->
                onAddTracksToPlaylist(pl.id, selectedIds)
                targetPlaylistForAdding = null
            }
        )
    }

    playlistToRename?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            containerColor = AuraSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = if (isFa) "تغییر نام پلی‌لیست" else "Rename Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = renamePlaylistNewName,
                    onValueChange = { renamePlaylistNewName = it },
                    singleLine = true,
                    label = { Text(Localization.getString("playlist_name", lang)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AuraSpotifyGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renamePlaylistNewName.trim()
                        if (trimmed.isNotBlank()) {
                            onRenamePlaylist(pl.id, trimmed)
                        }
                        playlistToRename = null
                    },
                    enabled = renamePlaylistNewName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraSpotifyGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(Localization.getString("save", lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text(Localization.getString("cancel", lang), color = TextSecondary)
                }
            }
        )
    }
}

