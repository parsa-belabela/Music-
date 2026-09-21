package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.LyricsLine
import com.example.data.model.Track
import com.example.lyrics.LrcParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditorSheet(
    track: Track?,
    currentLyrics: List<LyricsLine>,
    currentPositionMs: Long,
    palette: AmbientPalette,
    onSave: (String, Long) -> Unit,
    onClose: () -> Unit
) {
    var isLiveSyncMode by remember { mutableStateOf(false) }
    var rawTextMode by remember { mutableStateOf(false) }
    var rawTextInput by remember { mutableStateOf("") }

    val lines = remember { mutableStateListOf<LyricsLine>().apply { addAll(currentLyrics) } }
    var globalOffsetMs by remember { mutableStateOf(0L) }
    var activeLiveLineIndex by remember { mutableIntStateOf(0) }

    // Dialog for editing a line
    var editingLineIndex by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }
    var editingTimeMs by remember { mutableStateOf(0L) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFF10101C),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x66FFFFFF)) },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isLiveSyncMode) "Live Sync Recorder" else "Lyrics Studio",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = track?.title ?: "No Track",
                        style = MaterialTheme.typography.bodySmall.copy(color = palette.accent)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Toggle Live Sync
                    FilterChip(
                        selected = isLiveSyncMode,
                        onClick = { isLiveSyncMode = !isLiveSyncMode },
                        label = { Text("Live Sync") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = if (isLiveSyncMode) Color.Red else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    // Save Button
                    Button(
                        onClick = {
                            val exported = LrcParser.exportToLrc(lines.toList(), track?.title, track?.artist)
                            onSave(exported, globalOffsetMs)
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_lyrics_button")
                    ) {
                        Text("Save")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Offset adjustment bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18182A)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Global Offset: ${if (globalOffsetMs >= 0) "+$globalOffsetMs" else "$globalOffsetMs"}ms",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Medium)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(-100L, -50L, -10L, 10L, 50L, 100L).forEach { delta ->
                            AssistChip(
                                onClick = {
                                    globalOffsetMs += delta
                                    for (i in lines.indices) {
                                        val l = lines[i]
                                        lines[i] = l.copy(timestampMs = (l.timestampMs + delta).coerceAtLeast(0L))
                                    }
                                },
                                label = { Text("${if (delta > 0) "+$delta" else delta}", fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live Sync Banner or Quick Action buttons
            if (isLiveSyncMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33A855F7)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Next Line to Stamp (${activeLiveLineIndex + 1}/${lines.size}):",
                            style = MaterialTheme.typography.bodySmall.copy(color = palette.secondary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lines.getOrNull(activeLiveLineIndex)?.text ?: "All lines stamped!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (activeLiveLineIndex in lines.indices) {
                                    val cur = lines[activeLiveLineIndex]
                                    lines[activeLiveLineIndex] = cur.copy(timestampMs = currentPositionMs)
                                    activeLiveLineIndex = (activeLiveLineIndex + 1).coerceAtMost(lines.size)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(46.dp)
                                .testTag("stamp_live_sync_button")
                        ) {
                            Icon(imageVector = Icons.Default.TouchApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TAP TO STAMP (NOW)")
                        }
                    }
                }
            }

            // Action row: Add line, Paste LRC, Clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lines (${lines.size})",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFA0A0B8))
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            rawTextInput = LrcParser.exportToLrc(lines.toList(), track?.title, track?.artist)
                            rawTextMode = true
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste / Raw")
                    }

                    IconButton(
                        onClick = {
                            lines.add(LyricsLine(timestampMs = currentPositionMs, text = "New Lyric Line"))
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Line", tint = palette.accent)
                    }
                }
            }

            // Lines list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(lines) { index, line ->
                    val isTarget = isLiveSyncMode && index == activeLiveLineIndex
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTarget) Color(0x33A855F7) else Color(0xFF161626)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingLineIndex = index
                                editingText = line.text
                                editingTimeMs = line.timestampMs
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Timestamp pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF222238))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = line.formattedTimestamp,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.secondary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                modifier = Modifier.weight(1f)
                            )

                            // Quick timestamp to current audio position
                            IconButton(
                                onClick = {
                                    lines[index] = line.copy(timestampMs = currentPositionMs)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sync to Now",
                                    tint = palette.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = { lines.removeAt(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Line",
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Single Line Dialog
    editingLineIndex?.let { idx ->
        AlertDialog(
            onDismissRequest = { editingLineIndex = null },
            title = { Text("Edit Lyric Line") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editingText,
                        onValueChange = { editingText = it },
                        label = { Text("Lyric Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editingTimeMs.toString(),
                        onValueChange = { editingTimeMs = it.toLongOrNull() ?: editingTimeMs },
                        label = { Text("Timestamp (ms)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (idx in lines.indices) {
                            lines[idx] = lines[idx].copy(text = editingText, timestampMs = editingTimeMs)
                        }
                        editingLineIndex = null
                    }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingLineIndex = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Raw LRC Import / Paste Sheet
    if (rawTextMode) {
        AlertDialog(
            onDismissRequest = { rawTextMode = false },
            title = { Text("Paste / Edit LRC Code") },
            text = {
                OutlinedTextField(
                    value = rawTextInput,
                    onValueChange = { rawTextInput = it },
                    label = { Text("LRC Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = LrcParser.parse(rawTextInput)
                        lines.clear()
                        lines.addAll(parsed)
                        rawTextMode = false
                    }
                ) {
                    Text("Import Lines")
                }
            },
            dismissButton = {
                TextButton(onClick = { rawTextMode = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
