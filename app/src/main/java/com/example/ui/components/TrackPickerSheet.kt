package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.Track
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization

enum class TrackPickerFilter {
    ALL,
    FAVORITES,
    RECENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackPickerSheet(
    allTracks: List<Track>,
    favoriteTracks: List<Track> = emptyList(),
    initialSelectedTrackIds: Set<String> = emptySet(),
    playlistTitle: String = "",
    titleText: String? = null,
    confirmButtonText: String? = null,
    settings: AppSettings,
    palette: AmbientPalette,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val lang = settings.language
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(TrackPickerFilter.ALL) }
    var selectedTrackIds by remember { mutableStateOf(initialSelectedTrackIds.toMutableSet()) }

    val filteredTracks = remember(allTracks, favoriteTracks, searchQuery, selectedFilter) {
        val baseList = when (selectedFilter) {
            TrackPickerFilter.ALL -> allTracks
            TrackPickerFilter.FAVORITES -> favoriteTracks.ifEmpty { allTracks.filter { it.isFavorite } }
            TrackPickerFilter.RECENT -> allTracks.takeLast(30).reversed()
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q)
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
                    tintAlpha = 0.26f,
                    borderWidth = 1.2.dp,
                    appTheme = settings.theme
                )
                .padding(top = 16.dp, start = 18.dp, end = 18.dp, bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titleText ?: if (lang == AppLanguage.PERSIAN) "انتخاب آهنگ‌ها" else "Select Songs",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (playlistTitle.isNotBlank()) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "برای: $playlistTitle" else "For: $playlistTitle",
                                style = MaterialTheme.typography.bodySmall.copy(color = palette.accent),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Quick Select/Deselect All Toggle
                        TextButton(
                            onClick = {
                                val currentIds = filteredTracks.map { it.id }
                                val allInFilterSelected = currentIds.all { it in selectedTrackIds }
                                val updated = selectedTrackIds.toMutableSet()
                                if (allInFilterSelected) {
                                    updated.removeAll(currentIds.toSet())
                                } else {
                                    updated.addAll(currentIds)
                                }
                                selectedTrackIds = updated
                            }
                        ) {
                            val allInFilterSelected = filteredTracks.isNotEmpty() && filteredTracks.all { it.id in selectedTrackIds }
                            Text(
                                text = if (allInFilterSelected) {
                                    if (lang == AppLanguage.PERSIAN) "لغو همه" else "Clear All"
                                } else {
                                    if (lang == AppLanguage.PERSIAN) "انتخاب همه" else "Select All"
                                },
                                color = palette.accent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "جستجوی آهنگ، خواننده، آلبوم..." else "Search tracks, artists, albums...",
                            color = Color(0xFF8E8EA0),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = palette.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFFA0A0B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0x1E1E2E40),
                        unfocusedContainerColor = Color(0x141A2035),
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = Color(0x30FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("track_picker_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == TrackPickerFilter.ALL,
                            onClick = { selectedFilter = TrackPickerFilter.ALL },
                            label = { Text(if (lang == AppLanguage.PERSIAN) "همه آهنگ‌ها (${allTracks.size})" else "All Songs (${allTracks.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary.copy(alpha = 0.35f),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x18FFFFFF),
                                labelColor = Color(0xFFD0D0E0)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == TrackPickerFilter.ALL,
                                borderColor = if (selectedFilter == TrackPickerFilter.ALL) palette.accent else Color(0x22FFFFFF)
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedFilter == TrackPickerFilter.FAVORITES,
                            onClick = { selectedFilter = TrackPickerFilter.FAVORITES },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = if (selectedFilter == TrackPickerFilter.FAVORITES) palette.accent else Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = { Text(if (lang == AppLanguage.PERSIAN) "علاقه‌مندی‌ها" else "Favorites") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary.copy(alpha = 0.35f),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x18FFFFFF),
                                labelColor = Color(0xFFD0D0E0)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == TrackPickerFilter.FAVORITES,
                                borderColor = if (selectedFilter == TrackPickerFilter.FAVORITES) palette.accent else Color(0x22FFFFFF)
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = selectedFilter == TrackPickerFilter.RECENT,
                            onClick = { selectedFilter = TrackPickerFilter.RECENT },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            label = { Text(if (lang == AppLanguage.PERSIAN) "اخیراً اضافه شده" else "Recent") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.primary.copy(alpha = 0.35f),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0x18FFFFFF),
                                labelColor = Color(0xFFD0D0E0)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == TrackPickerFilter.RECENT,
                                borderColor = if (selectedFilter == TrackPickerFilter.RECENT) palette.accent else Color(0x22FFFFFF)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Track Count & Selection Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "${filteredTracks.size} آهنگ موجود"
                        else
                            "${filteredTracks.size} songs found",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0A0B8))
                    )

                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "${selectedTrackIds.size} مورد انتخاب شده"
                        else
                            "${selectedTrackIds.size} selected",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (selectedTrackIds.isNotEmpty()) palette.accent else Color(0xFFA0A0B8),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Track List
                if (filteredTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MusicOff,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "هیچ آهنگی یافت نشد" else "No matching tracks",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9CA3AF))
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
                            val isSelected = track.id in selectedTrackIds
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) palette.primary.copy(alpha = 0.28f)
                                        else Color(0x14FFFFFF)
                                    )
                                    .clickable {
                                        val updated = selectedTrackIds.toMutableSet()
                                        if (isSelected) {
                                            updated.remove(track.id)
                                        } else {
                                            updated.add(track.id)
                                        }
                                        selectedTrackIds = updated
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Checkbox / Circle indicator
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) palette.accent
                                                else Color(0x28FFFFFF)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Thumbnail
                                    TrackArtworkThumbnail(
                                        artworkUri = track.artworkUri,
                                        accentColor = palette.secondary,
                                        size = 44.dp,
                                        shape = RoundedCornerShape(10.dp),
                                        iconSize = 22.dp
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = Color.White
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${track.artist} • ${track.durationFormatted}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isSelected) Color(0xFFD4D4E8) else Color(0xFFA0A0B8)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Confirm / Add button
                Button(
                    onClick = {
                        onConfirm(selectedTrackIds.toList())
                        onDismiss()
                    },
                    enabled = selectedTrackIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        disabledContainerColor = Color(0x25FFFFFF)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_add_tracks_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAddCheck,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = confirmButtonText ?: if (lang == AppLanguage.PERSIAN) {
                            "افزودن ${selectedTrackIds.size} آهنگ به پلی‌لیست"
                        } else {
                            "Add ${selectedTrackIds.size} Songs"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTrackIds.isNotEmpty()) Color.White else Color(0xFF88889C)
                        )
                    )
                }
            }
        }
    }
}
