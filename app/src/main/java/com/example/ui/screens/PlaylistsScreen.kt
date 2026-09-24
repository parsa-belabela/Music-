package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
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
    bottomPadding: androidx.compose.ui.unit.Dp = 120.dp,
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
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Localization.getString("collections", lang).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        color = palette.accent,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = Localization.getString("playlists", lang),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Box(
                modifier = Modifier
                    .liquidGlass(
                        shape = RoundedCornerShape(14.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.22f,
                        borderWidth = 1.dp
                    )
                    .clickable {
                        pendingPlaylistName = ""
                        showNameDialog = true
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("create_playlist_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Playlist",
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = Localization.getString("new_playlist", lang),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Search in playlists (if there are playlists)
        if (playlists.size > 3) {
            OutlinedTextField(
                value = playlistSearchQuery,
                onValueChange = { playlistSearchQuery = it },
                placeholder = {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "جستجوی پلی‌لیست..." else "Search playlists...",
                        color = Color(0xFF8E8EA0),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0x18182236),
                    unfocusedContainerColor = Color(0x10182030),
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = Color(0x25FFFFFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = maxOf(bottomPadding + 20.dp, 120.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (playlists.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp)
                            .liquidGlass(
                                shape = RoundedCornerShape(24.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.primary,
                                tintAlpha = 0.16f,
                                borderWidth = 1.dp
                            )
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(palette.primary.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "هنوز هیچ پلی‌لیستی ایجاد نشده است" else "No playlists yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (lang == AppLanguage.PERSIAN)
                                    "برای ساخت کالکشن اختصاصی، نام پلی‌لیست را وارد کرده و آهنگ‌های دلخواهتان را انتخاب کنید."
                                else
                                    "Create your custom collection by naming a playlist and picking your favorite tracks.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFA0A0B8),
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    pendingPlaylistName = ""
                                    showNameDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.getString("create_playlist", lang),
                                    fontWeight = FontWeight.Bold
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(20.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.primary,
                                tintAlpha = 0.16f,
                                borderWidth = 1.dp,
                                appTheme = settings.theme
                            )
                            .clickable { selectedPlaylistId = pl.id }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover / Icon
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.primary.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (firstArt != null) {
                                    TrackArtworkThumbnail(
                                        artworkUri = firstArt,
                                        accentColor = palette.primary,
                                        size = 56.dp,
                                        shape = RoundedCornerShape(14.dp),
                                        iconSize = 28.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Playlist Name & Tracks Count
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pl.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${pl.trackIds.size} ${Localization.getString("tracks", lang)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Actions: Clean, compact, professional
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (plTracks.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(palette.primary.copy(alpha = 0.35f))
                                            .clickable { onPlayTrackList(plTracks) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                var menuExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x14FFFFFF))
                                            .clickable { menuExpanded = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Menu",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    val plMenuAlpha by animateFloatAsState(
                                        targetValue = if (menuExpanded) 1f else 0f,
                                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                        label = "pl_menu_alpha"
                                    )
                                    val plMenuScale by animateFloatAsState(
                                        targetValue = if (menuExpanded) 1f else 0.92f,
                                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                        label = "pl_menu_scale"
                                    )

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        modifier = Modifier
                                            .graphicsLayer {
                                                alpha = plMenuAlpha
                                                scaleX = plMenuScale
                                                scaleY = plMenuScale
                                                transformOrigin = TransformOrigin(0.9f, 0f)
                                            }
                                            .liquidGlass(
                                                shape = RoundedCornerShape(16.dp),
                                                thickness = GlassThickness.REGULAR,
                                                tintColor = palette.deepAtmosphere,
                                                tintAlpha = 0.92f,
                                                borderWidth = 1.dp,
                                                appTheme = settings.theme
                                            )
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (lang == AppLanguage.PERSIAN) "افزودن آهنگ" else "Add Songs",
                                                    color = Color.White
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.PlaylistAdd,
                                                    contentDescription = null,
                                                    tint = palette.accent,
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
                                                    if (lang == AppLanguage.PERSIAN) "تغییر نام" else "Rename",
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

                                        HorizontalDivider(color = Color(0x20FFFFFF))

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (lang == AppLanguage.PERSIAN) "حذف پلی‌لیست" else "Delete Playlist",
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
    }

    // Playlist Details Modal Sheet
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

    // Step 1: Playlist Name Input Dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = Color(0xFF161628),
            shape = RoundedCornerShape(22.dp),
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
                        text = if (lang == AppLanguage.PERSIAN)
                            "نامی برای پلی‌لیست جدید وارد کنید. سپس می‌توانید آهنگ‌های مورد نظر را انتخاب و اضافه کنید."
                        else
                            "Enter a name for your playlist, then select songs to include.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB0B0C4))
                    )
                    OutlinedTextField(
                        value = pendingPlaylistName,
                        onValueChange = { pendingPlaylistName = it },
                        label = { Text(Localization.getString("playlist_name", lang)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = palette.primary
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
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "انتخاب آهنگ‌ها →" else "Select Songs →",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(Localization.getString("cancel", lang), color = Color(0xFFA0A0B8))
                }
            }
        )
    }

    // Step 2: Song Picker for New Playlist Creation
    if (showSongPickerForNewPlaylist) {
        TrackPickerSheet(
            allTracks = allTracks,
            favoriteTracks = favoriteTracks,
            initialSelectedTrackIds = emptySet(),
            playlistTitle = pendingPlaylistName,
            titleText = if (lang == AppLanguage.PERSIAN) "افزودن آهنگ به «$pendingPlaylistName»" else "Add Songs to \"$pendingPlaylistName\"",
            confirmButtonText = if (lang == AppLanguage.PERSIAN) "ایجاد پلی‌لیست و ذخیره" else "Create Playlist & Save",
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

    // Quick Add Songs Modal for an Existing Playlist
    targetPlaylistForAdding?.let { pl ->
        TrackPickerSheet(
            allTracks = allTracks,
            favoriteTracks = favoriteTracks,
            initialSelectedTrackIds = pl.trackIds.toSet(),
            playlistTitle = pl.name,
            titleText = if (lang == AppLanguage.PERSIAN) "افزودن آهنگ به «${pl.name}»" else "Add Songs to \"${pl.name}\"",
            confirmButtonText = if (lang == AppLanguage.PERSIAN) "ذخیره تغییرات پلی‌لیست" else "Save Playlist Changes",
            settings = settings,
            palette = palette,
            onDismiss = { targetPlaylistForAdding = null },
            onConfirm = { selectedIds ->
                onAddTracksToPlaylist(pl.id, selectedIds)
                targetPlaylistForAdding = null
            }
        )
    }

    // Rename Playlist Dialog
    playlistToRename?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            containerColor = Color(0xFF161628),
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "تغییر نام پلی‌لیست" else "Rename Playlist",
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
                        focusedBorderColor = palette.primary
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
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(Localization.getString("save", lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text(Localization.getString("cancel", lang), color = Color(0xFFA0A0B8))
                }
            }
        )
    }
}
