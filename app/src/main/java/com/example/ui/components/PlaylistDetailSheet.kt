package com.example.ui.components

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailSheet(
    playlist: Playlist,
    allTracks: List<Track>,
    favoriteTracks: List<Track> = emptyList(),
    settings: AppSettings,
    palette: AmbientPalette,
    onDismiss: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onAddTracksToPlaylist: (String, List<String>) -> Unit = { _, _ -> },
    onRemoveTrackFromPlaylist: (String, String) -> Unit,
    onRenamePlaylist: (String, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit
) {
    val lang = settings.language
    var showTrackPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(playlist.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val playlistTracks = remember(playlist.trackIds, allTracks) {
        val trackMap = allTracks.associateBy { it.id }
        playlist.trackIds.mapNotNull { trackMap[it] }
    }

    val totalDurationFormatted = remember(playlistTracks) {
        val totalMs = playlistTracks.sumOf { it.durationMs }
        val totalSec = totalMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        if (min >= 60) {
            val hrs = min / 60
            val remMin = min % 60
            if (lang == AppLanguage.PERSIAN) "$hrs ساعت و $remMin دقیقه" else "${hrs}h ${remMin}m"
        } else {
            if (lang == AppLanguage.PERSIAN) "$min دقیقه و $sec ثانیه" else "${min}m ${sec}s"
        }
    }

    val displayedTracks = remember(playlistTracks, searchQuery) {
        if (searchQuery.isBlank()) playlistTracks
        else {
            val q = searchQuery.trim().lowercase()
            playlistTracks.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        scrimColor = Color(0xFF030308).copy(alpha = 0.85f),
        dragHandle = null,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(28.dp),
                    thickness = GlassThickness.THICK,
                    tintColor = palette.primary,
                    tintAlpha = 0.28f,
                    borderWidth = 1.2.dp,
                    appTheme = settings.theme
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Playlist Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.primary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val firstArt = playlistTracks.firstOrNull()?.artworkUri
                        if (firstArt != null) {
                            TrackArtworkThumbnail(
                                artworkUri = firstArt,
                                accentColor = palette.primary,
                                size = 68.dp,
                                shape = RoundedCornerShape(18.dp),
                                iconSize = 34.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    renameText = playlist.name
                                    showRenameDialog = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename",
                                    tint = palette.accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${playlistTracks.size} ${Localization.getString("tracks", lang)} • $totalDurationFormatted",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA6A6C0))
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x22EF4444))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Playlist",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Play All, Shuffle, and + Add Songs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (playlistTracks.isNotEmpty()) {
                                onPlayAll(playlistTracks)
                                onDismiss()
                            }
                        },
                        enabled = playlistTracks.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Localization.getString("play_all", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (playlistTracks.isNotEmpty()) {
                                onShuffleAll(playlistTracks.shuffled())
                                onDismiss()
                            }
                        },
                        enabled = playlistTracks.isNotEmpty(),
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Localization.getString("shuffle", lang), fontSize = 13.sp)
                    }

                    // Add songs button
                    Button(
                        onClick = { showTrackPicker = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent.copy(alpha = 0.25f)),
                        modifier = Modifier.testTag("add_tracks_to_playlist_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "افزودن" else "Add",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search inside playlist if has tracks
                if (playlistTracks.size > 4) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "جستجو در این پلی‌لیست..." else "Filter songs in playlist...",
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
                            .padding(bottom = 8.dp)
                    )
                }

                // Track List
                if (playlistTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = palette.accent.copy(alpha = 0.6f),
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = Localization.getString("empty_playlist", lang),
                                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "روی دکمه زیر ضربه بزنید و آهنگ‌های دلخواهتان را اضافه کنید." else "Tap the button below to add your favorite songs.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { showTrackPicker = true },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "افزودن آهنگ به پلی‌لیست" else "Add Songs to Playlist",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedTracks, key = { it.id }) { track ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x18FFFFFF))
                                    .clickable {
                                        onPlayTrack(track, playlistTracks)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TrackArtworkThumbnail(
                                        artworkUri = track.artworkUri,
                                        accentColor = palette.secondary,
                                        size = 46.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        iconSize = 24.dp
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
                                            text = "${track.artist} • ${track.durationFormatted}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Remove from playlist button
                                    IconButton(
                                        onClick = {
                                            onRemoveTrackFromPlaylist(playlist.id, track.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color(0xFF88889C),
                                            modifier = Modifier.size(18.dp)
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

    // Modal Track Picker Sheet
    if (showTrackPicker) {
        TrackPickerSheet(
            allTracks = allTracks,
            favoriteTracks = favoriteTracks,
            initialSelectedTrackIds = playlist.trackIds.toSet(),
            playlistTitle = playlist.name,
            settings = settings,
            palette = palette,
            onDismiss = { showTrackPicker = false },
            onConfirm = { selectedIds ->
                onAddTracksToPlaylist(playlist.id, selectedIds)
                showTrackPicker = false
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "تغییر نام پلی‌لیست" else "Rename Playlist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
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
                        if (renameText.isNotBlank()) {
                            onRenamePlaylist(playlist.id, renameText.trim())
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text(if (lang == AppLanguage.PERSIAN) "ذخیره" else "Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(Localization.getString("cancel", lang), color = Color.White)
                }
            },
            containerColor = Color(0xFF18182B),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = Localization.getString("delete_playlist_title", lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
            },
            text = {
                Text(
                    text = Localization.getString("delete_playlist_confirm", lang),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD1D1E0))
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeletePlaylist(playlist.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(Localization.getString("delete", lang), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(Localization.getString("cancel", lang), color = Color.White)
                }
            },
            containerColor = Color(0xFF18182B),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
