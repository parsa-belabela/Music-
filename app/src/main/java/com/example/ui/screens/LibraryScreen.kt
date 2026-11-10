package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.AppSettings
import com.example.data.model.PlaybackState
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.components.DisintegrationOverlay
import com.example.ui.components.TrackActionSheet
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.util.Localization

enum class SortOption(val labelEn: String, val labelFa: String) {
    TITLE("Title", "نام آهنگ"),
    ARTIST("Artist", "نام هنرمند"),
    DURATION("Duration", "مدت زمان"),
    DATE_ADDED("Recently Added", "اخیراً اضافه شده");

    fun getLabel(lang: AppLanguage): String = if (lang == AppLanguage.PERSIAN) labelFa else labelEn
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
    onRescanMedia: () -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onCreatePlaylistWithTracks: (String, List<String>) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit = {},
    onRemoveTrackFromPlaylist: (String, String) -> Unit = { _, _ -> },
    onShareTrack: (Track) -> Unit = {},
    isScanningMedia: Boolean = false,
    bottomPadding: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    var selectedSort by remember { mutableStateOf(SortOption.DATE_ADDED) }
    var filterQuery by remember { mutableStateOf("") }
    var selectedTrackMenu by remember { mutableStateOf<Track?>(null) }
    var disintegratingTrackId by remember { mutableStateOf<String?>(null) }
    val lang = settings.language
    val isFa = lang == AppLanguage.PERSIAN

    val sortedTracks = remember(tracks, selectedSort, filterQuery) {
        val filtered = if (filterQuery.isBlank()) tracks else {
            tracks.filter {
                it.title.contains(filterQuery, ignoreCase = true) ||
                    it.artist.contains(filterQuery, ignoreCase = true) ||
                    it.album.contains(filterQuery, ignoreCase = true)
            }
        }
        when (selectedSort) {
            SortOption.TITLE -> filtered.sortedWith(compareBy<Track> { it.title.lowercase() }.thenBy { it.id })
            SortOption.ARTIST -> filtered.sortedWith(compareBy<Track> { it.artist.lowercase() }.thenBy { it.id })
            SortOption.DURATION -> filtered.sortedWith(compareByDescending<Track> { it.durationMs }.thenBy { it.id })
            SortOption.DATE_ADDED -> filtered.sortedWith(compareByDescending<Track> { it.dateAdded }.thenByDescending { it.id })
        }
    }

    val listState = rememberLazyListState()
    val currentTrackId = playbackState.currentTrack?.id

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 1. Spotify Library Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = Localization.getString("library", lang),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 23.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isScanningMedia) Color(0xFF38BDF8) else AuraSpotifyGreen)
                    )
                    Text(
                        text = if (isScanningMedia) {
                            if (isFa) "در حال بروزرسانی آرشیو..." else "Scanning media..."
                        } else {
                            "${tracks.size} ${Localization.getString("tracks", lang)}"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val syncSpinTransition = rememberInfiniteTransition(label = "rescanSpin")
                val spinAngle by syncSpinTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "spinAngle"
                )

                IconButton(
                    onClick = onRescanMedia,
                    modifier = Modifier.testTag("rescan_library_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Rescan Library",
                        tint = if (isScanningMedia) AuraSpotifyGreen else Color.White,
                        modifier = if (isScanningMedia) Modifier.graphicsLayer { rotationZ = spinAngle } else Modifier
                    )
                }

                if (sortedTracks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AuraSpotifyGreen)
                            .clickable {
                                val randomTrack = sortedTracks.random()
                                onPlayTrack(randomTrack, sortedTracks)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play All",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 2. Clean Search Field
        OutlinedTextField(
            value = filterQuery,
            onValueChange = { filterQuery = it },
            placeholder = {
                Text(
                    Localization.getString("search_hint", lang),
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (filterQuery.isNotEmpty()) {
                    IconButton(onClick = { filterQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
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
                .padding(vertical = 6.dp)
        )

        // 3. Spotify Sort Pills Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SortOption.values()) { option ->
                val isSelected = selectedSort == option
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) AuraSpotifyGreen else AuraSurfaceElevated)
                        .clickable { selectedSort = option }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.getLabel(lang),
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // 4. Flat Spotify Track Rows
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                top = 4.dp,
                bottom = maxOf(bottomPadding + 24.dp, 128.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(sortedTracks, key = { it.id }) { track ->
                val isCurrent = currentTrackId == track.id
                val isDisintegrating = disintegratingTrackId == track.id

                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = !isDisintegrating,
                        exit = fadeOut(tween(400)) + shrinkVertically(tween(400))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPlayTrack(track, sortedTracks) }
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .testTag("library_track_${track.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TrackArtworkThumbnail(
                                    artworkUri = track.artworkUri,
                                    accentColor = if (isCurrent) AuraSpotifyGreen else palette.primary,
                                    size = 48.dp,
                                    shape = RoundedCornerShape(6.dp),
                                    iconSize = 22.dp,
                                    title = track.title,
                                    artist = track.artist,
                                    trackId = track.id
                                )
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black.copy(alpha = 0.55f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Equalizer,
                                            contentDescription = "Playing",
                                            tint = AuraSpotifyGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) AuraSpotifyGreen else Color.White,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${track.artist} • ${track.album}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 12.5.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = track.durationFormatted,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            )

                            IconButton(
                                onClick = { selectedTrackMenu = track },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (isDisintegrating) {
                        DisintegrationOverlay(
                            isDisintegrating = true,
                            primaryColor = palette.primary,
                            accentColor = AuraSpotifyGreen,
                            onAnimationEnd = {
                                onDeleteTrack(track)
                                disintegratingTrackId = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )
                    }
                }
            }
        }
    }

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

