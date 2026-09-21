package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: AppSettings,
    palette: AmbientPalette,
    onUpdateSettings: (AppSettings) -> Unit,
    onSetPreset: (VisualizerPreset) -> Unit,
    onExportBackup: suspend () -> String,
    onImportBackup: suspend (String) -> Boolean,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }
    var isExportMode by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        color = palette.accent,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        // Section: Visualizer & Ambient Halo
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131325)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Visualizer & Ambient Halo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Presets", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(VisualizerPreset.DEFAULT_PRESETS) { p ->
                            FilterChip(
                                selected = settings.visualizerMode == p.mode,
                                onClick = { onSetPreset(p) },
                                label = { Text(p.name, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Selection Dropdown / Selector
                    Text("Current Mode: ${settings.visualizerMode.title}", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(VisualizerMode.values().toList()) { mode ->
                            SuggestionChip(
                                onClick = { onUpdateSettings(settings.copy(visualizerMode = mode)) },
                                label = { Text(mode.title, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sensitivity Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Audio Sensitivity", style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                        Text("${"%.1f".format(settings.visualizerSensitivity)}x", color = palette.accent, style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = settings.visualizerSensitivity,
                        onValueChange = { onUpdateSettings(settings.copy(visualizerSensitivity = it)) },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = palette.primary, activeTrackColor = palette.primary)
                    )

                    // Glow Strength Slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Halo Glow & Bloom", style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                        Text("${(settings.visualizerGlow * 100).toInt()}%", color = palette.accent, style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = settings.visualizerGlow,
                        onValueChange = { onUpdateSettings(settings.copy(visualizerGlow = it)) },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = palette.secondary, activeTrackColor = palette.secondary)
                    )

                    // Target FPS (30 / 60 / 120)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Visualizer Engine FPS", style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(30, 60, 120).forEach { fps ->
                                FilterChip(
                                    selected = settings.visualizerFps == fps,
                                    onClick = { onUpdateSettings(settings.copy(visualizerFps = fps)) },
                                    label = { Text("${fps}Hz", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Auto Color from Artwork toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto Color From Artwork", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.autoColorFromArtwork,
                            onCheckedChange = { onUpdateSettings(settings.copy(autoColorFromArtwork = it)) }
                        )
                    }
                }
            }
        }

        // Section: Lyrics & Typography
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131325)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lyrics, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Lyrics Display & Karaoke", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Display Mode", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(LyricsDisplayMode.values().toList()) { mode ->
                            FilterChip(
                                selected = settings.lyricsDisplayMode == mode,
                                onClick = { onUpdateSettings(settings.copy(lyricsDisplayMode = mode)) },
                                label = { Text(mode.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Font size slider
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Font Size", style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                        Text("${settings.lyricsFontSize.toInt()}sp", color = palette.accent, style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = settings.lyricsFontSize,
                        onValueChange = { onUpdateSettings(settings.copy(lyricsFontSize = it)) },
                        valueRange = 14f..32f,
                        colors = SliderDefaults.colors(thumbColor = palette.primary, activeTrackColor = palette.primary)
                    )

                    // Word-level karaoke highlight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Karaoke Word-Level Highlight", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.lyricsKaraokeWordHighlight,
                            onCheckedChange = { onUpdateSettings(settings.copy(lyricsKaraokeWordHighlight = it)) }
                        )
                    }
                }
            }
        }

        // Section: Themes & Aesthetics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131325)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Theme & Dark Atmosphere", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppTheme.values().toList()) { t ->
                            FilterChip(
                                selected = settings.theme == t,
                                onClick = { onUpdateSettings(settings.copy(theme = t)) },
                                label = { Text(t.title, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Accent Color Palette", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)))
                    Spacer(modifier = Modifier.height(8.dp))
                    val accentColors = listOf(
                        0xFF8B5CF6 to "Radiant Violet",
                        0xFF06B6D4 to "Cyan Glow",
                        0xFFF43F5E to "Neon Rose",
                        0xFF10B981 to "Emerald",
                        0xFFF59E0B to "Amber Gold",
                        0xFF38BDF8 to "Electric Blue"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        accentColors.forEach { (colorVal, _) ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorVal))
                                    .clickable {
                                        onUpdateSettings(settings.copy(customAccentColor = colorVal))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (settings.customAccentColor == colorVal) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Playback & Audio Focus
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131325)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Playback & Hardware", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pause on Headphone Disconnect", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.pauseOnHeadphoneDisconnect,
                            onCheckedChange = { onUpdateSettings(settings.copy(pauseOnHeadphoneDisconnect = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Duck Volume on Call/Notification", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.duckVolumeOnInterruption,
                            onCheckedChange = { onUpdateSettings(settings.copy(duckVolumeOnInterruption = it)) }
                        )
                    }
                }
            }
        }

        // Section: Backup & Developer HUD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131325)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("System & Developer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Developer Mode (DSP Telemetry HUD)", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.developerModeEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(developerModeEnabled = it)) },
                            modifier = Modifier.testTag("developer_mode_switch")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    backupJsonText = onExportBackup()
                                    isExportMode = true
                                    showBackupDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup")
                        }

                        OutlinedButton(
                            onClick = {
                                backupJsonText = ""
                                isExportMode = false
                                showBackupDialog = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Backup")
                        }
                    }

                    TextButton(onClick = onRescanLibrary) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = palette.secondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger MediaStore Rescan", color = palette.secondary)
                    }
                }
            }
        }

        // Section: About
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Aura Music Player v1.0.0",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "High-Fidelity Audio • Real-Time DSP • Synced Lyrics",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF88889C))
                )
            }
        }
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(if (isExportMode) "Export Backup JSON" else "Import Backup JSON") },
            text = {
                Column {
                    Text(
                        text = if (isExportMode) "Copy this JSON to backup your playlists, favorites and settings:" else "Paste your backup JSON below:",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B0))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = { backupJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isExportMode) {
                            coroutineScope.launch {
                                val success = onImportBackup(backupJsonText)
                                Toast.makeText(context, if (success) "Restored successfully!" else "Invalid backup JSON", Toast.LENGTH_SHORT).show()
                                showBackupDialog = false
                            }
                        } else {
                            showBackupDialog = false
                        }
                    }
                ) {
                    Text(if (isExportMode) "Close" else "Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Cancel") }
            }
        )
    }
}
