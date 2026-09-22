package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.model.PlaybackState
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.components.AlphabetIndexScrubber
import com.example.ui.components.DisintegrationOverlay
import com.example.ui.components.TrackActionSheet
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization
import kotlinx.coroutines.launch

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
    playlists: List<Playlist> = emptyList(),
    playbackState: PlaybackState,
    palette: AmbientPalette,
    settings: AppSettings = AppSettings(),
    onPlayTrack: (Track, List<Track>) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Playlist, Track) -> Unit = { _, _ -> },
    onEditMetadata: (Track) -> Unit,
    onDeleteTrack: (Track) -> Unit = {},
    onRescanMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSort by remember { mutableStateOf(SortOption.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }
    var filterQuery by remember { mutableStateOf("") }
    var selectedTrackMenu by remember { mutableStateOf<Track?>(null) }
    var disintegratingTrackId by remember { mutableStateOf<String?>(null) }
    val lang = settings.language

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

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val alphabet = remember(lang) {
        if (lang == AppLanguage.PERSIAN) {
            listOf(
                "آ", "ا", "ب", "پ", "ت", "ث", "ج", "چ", "ح", "خ",
                "د", "ذ", "ر", "ز", "ژ", "س", "ش", "ص", "ض", "ط",
                "ظ", "ع", "غ", "ف", "ق", "ک", "گ", "ل", "م", "ن",
                "و", "ه", "ی", "#"
            )
        } else {
            listOf(
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
                "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
                "U", "V", "W", "X", "Y", "Z", "#"
            )
        }
    }

    val letterIndices = remember(sortedTracks, alphabet, selectedSort) {
        val map = mutableMapOf<String, Int>()
        alphabet.forEach { letter ->
            val idx = sortedTracks.indexOfFirst { track ->
                val primaryText = when (selectedSort) {
                    SortOption.ARTIST -> track.artist
                    else -> track.title
                }.trim()

                if (letter == "#") {
                    primaryText.isNotEmpty() && !primaryText.first().isLetter()
                } else {
                    primaryText.startsWith(letter, ignoreCase = true)
                }
            }
            if (idx != -1) {
                map[letter] = idx
            }
        }
        map
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Localization.getString("library", lang).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        color = palette.accent,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${Localization.getString("tracks", lang)} (${tracks.size})",
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
            placeholder = { Text(Localization.getString("search_hint", lang), color = Color(0xFF7E7E9A), fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = palette.primary) },
            trailingIcon = {
                if (filterQuery.isNotEmpty()) {
                    IconButton(onClick = { filterQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFFA0A0B8))
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

        // Tracks List with A-Z Alphabet Scrubber
        val showScrubber = (selectedSort == SortOption.TITLE || selectedSort == SortOption.ARTIST) && filterQuery.isBlank() && sortedTracks.isNotEmpty()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (showScrubber) 24.dp else 0.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            items(sortedTracks, key = { it.id }) { track ->
                val isCurrent = playbackState.currentTrack?.id == track.id
                val isDisintegrating = disintegratingTrackId == track.id

                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = !isDisintegrating,
                        exit = fadeOut(tween(550)) + shrinkVertically(tween(550))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isCurrent) {
                                        Modifier.liquidGlass(
                                            shape = RoundedCornerShape(16.dp),
                                            thickness = GlassThickness.REGULAR,
                                            tintColor = palette.primary,
                                            tintAlpha = 0.22f,
                                            borderWidth = 1.dp,
                                            appTheme = settings.theme
                                        )
                                    } else {
                                        Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF11111E))
                                    }
                                )
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
                                modifier = Modifier.size(46.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TrackArtworkThumbnail(
                                    artworkUri = track.artworkUri,
                                    accentColor = if (isCurrent) palette.accent else palette.primary,
                                    size = 46.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    iconSize = 24.dp
                                )
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Equalizer,
                                            contentDescription = "Playing",
                                            tint = palette.accent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
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

                // Render Disintegration Shatter Overlay if active
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
    }

        if (showScrubber) {
            AlphabetIndexScrubber(
                alphabet = alphabet,
                letterIndices = letterIndices,
                onLetterSelected = { _, index ->
                    coroutineScope.launch {
                        listState.scrollToItem(index)
                    }
                },
                palette = palette,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

    // High-Graphic Liquid Glass Track Action Sheet
    selectedTrackMenu?.let { trk ->
        TrackActionSheet(
            track = trk,
            playlists = playlists,
            settings = settings,
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
