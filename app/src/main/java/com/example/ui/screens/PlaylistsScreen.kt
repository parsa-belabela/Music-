package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppSettings
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.components.PlaylistDetailSheet
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    allTracks: List<Track>,
    favoriteTracks: List<Track>,
    mostPlayedTracks: List<Track>,
    recentlyAddedTracks: List<Track>,
    palette: AmbientPalette,
    settings: AppSettings = AppSettings(),
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveTrackFromPlaylist: (String, String) -> Unit = { _, _ -> },
    onPlayTrack: (Track, List<Track>) -> Unit = { _, _ -> },
    onPlayTrackList: (List<Track>) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    val lang = settings.language

    Column(
        modifier = modifier
            .fillMaxSize()
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

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("create_playlist_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(Localization.getString("new_playlist", lang), fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = maxOf(bottomPadding + 20.dp, 120.dp)),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Smart Playlists Section
            item {
                Text(
                    text = Localization.getString("smart_playlists", lang),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            val smartPlaylists = listOf(
                Triple("Starred Echoes", "${favoriteTracks.size} ${Localization.getString("tracks", lang)}", favoriteTracks),
                Triple("Most Played Heavyweights", "${mostPlayedTracks.size} ${Localization.getString("tracks", lang)}", mostPlayedTracks),
                Triple("Recently Added Inflows", "${recentlyAddedTracks.size} ${Localization.getString("tracks", lang)}", recentlyAddedTracks),
                Triple("Deep Odyssey (>3 min)", "${allTracks.filter { it.durationMs > 180000 }.size} ${Localization.getString("tracks", lang)}", allTracks.filter { it.durationMs > 180000 })
            )

            items(smartPlaylists) { (title, countStr, trackList) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(16.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.secondary,
                            tintAlpha = 0.14f,
                            borderWidth = 1.dp,
                            appTheme = settings.theme
                        )
                        .clickable {
                            val dummyPl = Playlist(
                                id = "smart_${title.hashCode()}",
                                name = title,
                                description = countStr,
                                trackIds = trackList.map { it.id },
                                isSmart = true
                            )
                            selectedPlaylist = dummyPl
                        }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val firstTrack = trackList.firstOrNull()
                        if (firstTrack?.artworkUri != null) {
                            TrackArtworkThumbnail(
                                artworkUri = firstTrack.artworkUri,
                                accentColor = palette.secondary,
                                size = 48.dp,
                                shape = RoundedCornerShape(12.dp),
                                iconSize = 24.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.secondary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = palette.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = countStr,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8))
                            )
                        }

                        IconButton(onClick = { onPlayTrackList(trackList) }) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "Play",
                                tint = palette.accent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // User Playlists Section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = Localization.getString("custom_playlists", lang),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (playlists.isEmpty()) {
                item {
                    Text(
                        text = Localization.getString("no_playlists_msg", lang),
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF88889C)),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }

            items(playlists) { pl ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(16.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.16f,
                            borderWidth = 1.dp,
                            appTheme = settings.theme
                        )
                        .clickable { selectedPlaylist = pl }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.primary.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pl.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "${pl.trackIds.size} ${Localization.getString("tracks", lang)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Playlist Detail Sheet
    selectedPlaylist?.let { pl ->
        PlaylistDetailSheet(
            playlist = pl,
            allTracks = allTracks,
            settings = settings,
            palette = palette,
            onDismiss = { selectedPlaylist = null },
            onPlayTrack = { trk, list -> onPlayTrack(trk, list) },
            onPlayAll = { list -> onPlayTrackList(list) },
            onShuffleAll = { list -> onPlayTrackList(list) },
            onRemoveTrackFromPlaylist = { plId, trkId -> onRemoveTrackFromPlaylist(plId, trkId) },
            onDeletePlaylist = { plId ->
                onDeletePlaylist(plId)
                selectedPlaylist = null
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF161628),
            title = {
                Text(
                    Localization.getString("create_playlist", lang),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text(Localization.getString("playlist_name", lang)) },
                    singleLine = true,
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
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text(Localization.getString("create", lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(Localization.getString("cancel", lang), color = Color(0xFFA0A0B8))
                }
            }
        )
    }
}
