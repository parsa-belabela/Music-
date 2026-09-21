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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppSettings
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionSheet(
    track: Track,
    playlists: List<Playlist>,
    settings: AppSettings,
    palette: AmbientPalette,
    onDismiss: () -> Unit,
    onPlayNow: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Playlist, Track) -> Unit,
    onEditMetadata: (Track) -> Unit,
    onDeleteTrack: (Track) -> Unit
) {
    val lang = settings.language
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        scrimColor = Color(0xFF030308).copy(alpha = 0.82f),
        dragHandle = null,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp)
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
                modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.height(16.dp))

                // Track Header Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            thickness = GlassThickness.THIN,
                            tintColor = palette.secondary,
                            tintAlpha = 0.18f,
                            borderWidth = 1.dp,
                            appTheme = settings.theme
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackArtworkThumbnail(
                            artworkUri = track.artworkUri,
                            accentColor = palette.primary,
                            size = 56.dp,
                            shape = RoundedCornerShape(14.dp),
                            iconSize = 28.dp
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.artist} • ${track.album}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA6A6C0)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.accent.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = "${track.bitrate} kbps",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = palette.accent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = track.durationFormatted,
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8E8EA8))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showPlaylistPicker) {
                    // Playlist Selector View
                    Text(
                        text = Localization.getString("add_to_playlist", lang),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (playlists.isEmpty()) {
                        Text(
                            text = Localization.getString("empty_playlist", lang),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF8E8EA8)),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(playlists) { pl ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF141424))
                                        .clickable {
                                            onAddToPlaylist(pl, track)
                                            showPlaylistPicker = false
                                            onDismiss()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.PlaylistAddCheck,
                                                contentDescription = null,
                                                tint = palette.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = pl.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                        Text(
                                            text = "${pl.trackIds.size} ${Localization.getString("tracks", lang)}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8E8EA8))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(onClick = { showPlaylistPicker = false }) {
                        Text(Localization.getString("cancel", lang), color = palette.primary)
                    }
                } else {
                    // Standard Options List
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ActionSheetItem(
                            icon = Icons.Default.PlayArrow,
                            title = Localization.getString("play_now", lang),
                            tint = palette.primary,
                            onClick = {
                                onPlayNow(track)
                                onDismiss()
                            }
                        )

                        ActionSheetItem(
                            icon = Icons.Default.SkipNext,
                            title = Localization.getString("play_next", lang),
                            tint = palette.secondary,
                            onClick = {
                                onPlayNext(track)
                                onDismiss()
                            }
                        )

                        ActionSheetItem(
                            icon = Icons.Default.QueueMusic,
                            title = Localization.getString("add_to_queue", lang),
                            tint = palette.accent,
                            onClick = {
                                onAddToQueue(track)
                                onDismiss()
                            }
                        )

                        ActionSheetItem(
                            icon = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            title = if (track.isFavorite) Localization.getString("favorites", lang) else Localization.getString("favorites", lang),
                            tint = Color(0xFFF43F5E),
                            onClick = {
                                onToggleFavorite(track)
                                onDismiss()
                            }
                        )

                        ActionSheetItem(
                            icon = Icons.Default.PlaylistAdd,
                            title = Localization.getString("add_to_playlist", lang),
                            tint = palette.primary,
                            onClick = {
                                showPlaylistPicker = true
                            }
                        )

                        ActionSheetItem(
                            icon = Icons.Default.Edit,
                            title = Localization.getString("edit_metadata", lang),
                            tint = Color(0xFF60A5FA),
                            onClick = {
                                onEditMetadata(track)
                                onDismiss()
                            }
                        )

                        // Fantasy Delete Button with Red Accent
                        ActionSheetItem(
                            icon = Icons.Default.DeleteForever,
                            title = Localization.getString("delete_track", lang),
                            tint = Color(0xFFEF4444),
                            isDestructive = true,
                            onClick = {
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = Localization.getString("delete_track_title", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            },
            text = {
                Text(
                    text = Localization.getString("delete_track_confirm", lang),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD1D1E0), lineHeight = 22.sp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteTrack(track)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(Localization.getString("delete", lang), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(Localization.getString("cancel", lang), color = Color.White)
                }
            },
            containerColor = Color(0xFF18182B),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ActionSheetItem(
    icon: ImageVector,
    title: String,
    tint: Color,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDestructive) Color(0x1DEF4444) else Color(0xFF141424).copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isDestructive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isDestructive) Color(0xFFEF4444) else Color.White
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
