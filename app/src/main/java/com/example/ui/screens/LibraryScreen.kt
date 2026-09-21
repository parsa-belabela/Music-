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
import com.example.data.model.PlaybackState
import com.example.data.model.Track

enum class SortOption(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    DURATION("Duration"),
    DATE_ADDED("Recently Added")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracks: List<Track>,
    playbackState: PlaybackState,
    palette: AmbientPalette,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onEditMetadata: (Track) -> Unit,
    onRescanMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSort by remember { mutableStateOf(SortOption.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    var selectedTrackMenu by remember { mutableStateOf<Track?>(null) }

    val sortedTracks = remember(tracks, selectedSort, filterQuery) {
        val filtered = if (filterQuery.isBlank()) tracks else {
            tracks.filter {
                it.title.contains(filterQuery, ignoreCase = true) ||
                it.artist.contains(filterQuery, ignoreCase = true) ||
                it.album.contains(filterQuery, ignoreCase = true)
            }
        }
        when (selectedSort) {
            SortOption.TITLE -> filtered.sortedBy { it.title }
            SortOption.ARTIST -> filtered.sortedBy { it.artist }
            SortOption.DURATION -> filtered.sortedByDescending { it.durationMs }
            SortOption.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LOCAL LIBRARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        color = palette.accent,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Songs (${tracks.size})",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Sort Menu
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color(0xFF161628))
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (selectedSort == option) palette.primary else Color.White
                                    )
                                },
                                onClick = {
                                    selectedSort = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                // Rescan device button
                IconButton(onClick = onRescanMedia, modifier = Modifier.testTag("rescan_library_button")) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = "Rescan Library", tint = palette.accent)
                }
            }
        }

        // Search in Library text field
        OutlinedTextField(
            value = filterQuery,
            onValueChange = { filterQuery = it },
            placeholder = { Text("Filter songs, artists...", color = Color(0xFF707086)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF9090A6)) },
            trailingIcon = {
                if (filterQuery.isNotEmpty()) {
                    IconButton(onClick = { filterQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF141426),
                unfocusedContainerColor = Color(0xFF101020),
                focusedBorderColor = palette.primary,
                unfocusedBorderColor = Color(0x22FFFFFF)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Tracks List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sortedTracks) { track ->
                val isCurrent = playbackState.currentTrack?.id == track.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) palette.primary.copy(alpha = 0.2f) else Color(0xFF121220)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayTrack(track, sortedTracks) }
                        .testTag("library_track_${track.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCurrent) Icons.Default.Equalizer else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isCurrent) palette.accent else palette.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) palette.accent else Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.artist} • ${track.album}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Duration & Format pill
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = track.durationFormatted,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B0))
                            )
                            Text(
                                text = "${track.bitrate}k / ${track.sampleRate / 1000}kHz",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = palette.secondary.copy(alpha = 0.8f),
                                    fontSize = 9.sp
                                )
                            )
                        }

                        // Track context menu button
                        Box {
                            IconButton(onClick = { selectedTrackMenu = track }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color(0xFF9090A8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Context Dropdown for Track
    selectedTrackMenu?.let { trk ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { selectedTrackMenu = null },
            modifier = Modifier.background(Color(0xFF18182E))
        ) {
            DropdownMenuItem(
                text = { Text("Play Now", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = palette.primary) },
                onClick = {
                    onPlayTrack(trk, tracks)
                    selectedTrackMenu = null
                }
            )
            DropdownMenuItem(
                text = { Text("Play Next", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null, tint = palette.secondary) },
                onClick = {
                    onPlayNext(trk)
                    selectedTrackMenu = null
                }
            )
            DropdownMenuItem(
                text = { Text("Add to Queue", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null, tint = palette.accent) },
                onClick = {
                    onAddToQueue(trk)
                    selectedTrackMenu = null
                }
            )
            DropdownMenuItem(
                text = { Text(if (trk.isFavorite) "Remove Favorite" else "Add to Favorites", color = Color.White) },
                leadingIcon = { Icon(if (trk.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = Color(0xFFF43F5E)) },
                onClick = {
                    onToggleFavorite(trk)
                    selectedTrackMenu = null
                }
            )
            DropdownMenuItem(
                text = { Text("Edit Metadata", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) },
                onClick = {
                    onEditMetadata(trk)
                    selectedTrackMenu = null
                }
            )
        }
    }
}
