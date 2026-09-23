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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AmbientPalette
import com.example.data.model.*
import com.example.ui.components.FeaturesGuideDialog
import com.example.ui.components.FeedbackDialog
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    palette: AmbientPalette,
    onUpdateSettings: (AppSettings) -> Unit,
    onSetPreset: (VisualizerPreset) -> Unit,
    onExportBackup: suspend () -> String,
    onImportBackup: suspend (String) -> Boolean,
    onRescanLibrary: () -> Unit,
    onClearPlaybackHistory: () -> Unit,
    onOpenEqualizer: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 120.dp,
    unlockedStyles: List<String> = listOf("default"),
    onOpenHearingProfileTest: () -> Unit = {},
    onOpenDuplicatesReview: () -> Unit = {},
    onSelectNowPlayingStyle: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showBackupDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showFeaturesGuideDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }
    var isExportMode by remember { mutableStateOf(true) }

    var devTapCount by remember { mutableStateOf(0) }

    val lang = settings.language

    if (showFeaturesGuideDialog) {
        FeaturesGuideDialog(
            settings = settings,
            palette = palette,
            onDismiss = { showFeaturesGuideDialog = false }
        )
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            appSettings = settings,
            palette = palette,
            onDismiss = { showFeedbackDialog = false }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = maxOf(bottomPadding + 20.dp, 120.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.getString("settings", lang),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )

                IconButton(
                    onClick = { showFeaturesGuideDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .liquidGlass(
                            shape = CircleShape,
                            thickness = GlassThickness.THIN,
                            tintColor = palette.accent,
                            tintAlpha = 0.15f
                        )
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Help Guide", tint = palette.accent)
                }
            }
        }

        // Section: Now Playing Visualizer Styles
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.14f,
                        borderWidth = 1.dp,
                        appTheme = settings.theme
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Style, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(Localization.getString("unlocked_styles", lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.selectedNowPlayingStyle == "default",
                            onClick = {
                                onSelectNowPlayingStyle("default")
                                onUpdateSettings(settings.copy(selectedNowPlayingStyle = "default"))
                            },
                            label = { Text(if (lang == AppLanguage.PERSIAN) "طراحی شیشه‌ای Liquid Glass" else "Liquid Glass Standard", fontSize = 12.sp) }
                        )

                        val isVinylUnlocked = unlockedStyles.contains("vinyl_turntable")
                        FilterChip(
                            selected = settings.selectedNowPlayingStyle == "vinyl_turntable",
                            onClick = {
                                if (isVinylUnlocked) {
                                    onSelectNowPlayingStyle("vinyl_turntable")
                                    onUpdateSettings(settings.copy(selectedNowPlayingStyle = "vinyl_turntable"))
                                } else {
                                    Toast.makeText(context, if (lang == AppLanguage.PERSIAN) "این استایل پس از ۵ ساعت گوش دادن باز می‌شود!" else "Unlocked after 5 hours of total listening!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isVinylUnlocked) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(Localization.getString("vinyl_style", lang), fontSize = 12.sp)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Section: Audio Hardware, DSP & Continuous Mix
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.12f,
                        borderWidth = 1.dp,
                        appTheme = settings.theme
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Equalizer, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(Localization.getString("audio_hardware", lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    // Continuous Mix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Localization.getString("continuous_mix", lang), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
                            Text(Localization.getString("continuous_mix_desc", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)))
                        }
                        Switch(
                            checked = settings.continuousMixEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(continuousMixEnabled = it)) }
                        )
                    }

                    // Hearing Calibration Profile Button
                    Button(
                        onClick = onOpenHearingProfileTest,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Hearing, contentDescription = null, modifier = Modifier.size(18.dp), tint = palette.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.getString("hearing_profile", lang), color = Color.White)
                    }

                    // Open Standard Equalizer Button
                    Button(
                        onClick = onOpenEqualizer,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp), tint = palette.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.getString("equalizer", lang), color = Color.White)
                    }

                    // Crossfade duration slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Localization.getString("crossfade_duration", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                            Text("${settings.crossfadeDurationSeconds}s", color = palette.accent, style = MaterialTheme.typography.bodySmall)
                        }
                        Slider(
                            value = settings.crossfadeDurationSeconds.toFloat(),
                            onValueChange = { onUpdateSettings(settings.copy(crossfadeDurationSeconds = it.toInt())) },
                            valueRange = 0f..12f,
                            steps = 11,
                            colors = SliderDefaults.colors(thumbColor = palette.primary, activeTrackColor = palette.primary)
                        )
                    }

                    // Gapless Playback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Localization.getString("gapless_playback", lang), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.gaplessEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(gaplessEnabled = it)) }
                        )
                    }
                }
            }
        }

        // Section: Focus & Study Mode
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.accent,
                        tintAlpha = 0.14f,
                        borderWidth = 1.dp,
                        appTheme = settings.theme
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.SelfImprovement, contentDescription = null, tint = palette.accent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(Localization.getString("focus_mode", lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                Text(Localization.getString("focus_mode_desc", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)))
                            }
                        }
                        Switch(
                            checked = settings.focusModeEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(focusModeEnabled = it)) }
                        )
                    }
                }
            }
        }

        // Section: Visualizer & Ambient Halo
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.12f,
                        borderWidth = 1.dp,
                        appTheme = settings.theme
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(Localization.getString("visualizer_dsp", lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(Localization.getString("visualizer_presets", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(VisualizerPreset.DEFAULT_PRESETS) { preset ->
                            FilterChip(
                                selected = settings.visualizerMode == preset.mode,
                                onClick = { onSetPreset(preset) },
                                label = { Text(preset.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto Color from Artwork toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Localization.getString("auto_color", lang), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.autoColorFromArtwork,
                            onCheckedChange = { onUpdateSettings(settings.copy(autoColorFromArtwork = it)) }
                        )
                    }
                }
            }
        }

        // Section: Themes & Aesthetics
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.12f,
                        borderWidth = 1.dp,
                        appTheme = settings.theme
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(Localization.getString("theme_appearance", lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AppTheme.values().toList()) { t ->
                            FilterChip(
                                selected = settings.theme == t,
                                onClick = {
                                    if (settings.theme != t) {
                                        onUpdateSettings(settings.copy(theme = t))
                                    }
                                },
                                label = { Text(if (lang == AppLanguage.PERSIAN) t.titleFa else t.titleEn, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(Localization.getString("accent_palette", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)))
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

        // Section: Storage, Library & Duplicate Optimizer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.12f,
                        borderWidth = 1.dp,
                        appTheme = settings.theme
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(Localization.getString("storage_cache", lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    // Duplicate Audio Optimizer
                    Button(
                        onClick = onOpenDuplicatesReview,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFFFBBF24))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Localization.getString("duplicates_manager", lang), color = Color.White)
                    }

                    Button(
                        onClick = onRescanLibrary,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = palette.secondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Localization.getString("rescan_library", lang), color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            onClearPlaybackHistory()
                            Toast.makeText(context, "Playback history cleared", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFF43F5E))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Localization.getString("clear_history", lang), color = Color(0xFFF43F5E))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    backupJsonText = onExportBackup()
                                    isExportMode = true
                                    showBackupDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.getString("export_backup", lang), fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                backupJsonText = ""
                                isExportMode = false
                                showBackupDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(Localization.getString("import_backup", lang), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section: Dedicated Feedback & Support
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.12f,
                        borderWidth = 1.dp,
                        appTheme = settings.theme
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Feedback, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(Localization.getString("feedback_section", lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Text(
                        text = Localization.getString("send_feedback", lang),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0C0))
                    )

                    Button(
                        onClick = { showFeedbackDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_feedback_dialog_button")
                    ) {
                        Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.getString("feedback_dialog_title", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: About & Warm Note
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "امیدوارم با لذت بیشتری بتونید آهنگ گوش کنید 💙" else "Hope you enjoy your music with even greater pleasure 💙",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFC7D7FE),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = "Aura Music Player v1.0.0",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (settings.developerModeEnabled) palette.accent else Color(0xFFA0A0B8),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            devTapCount++
                            if (devTapCount in 3..6) {
                                val remaining = 7 - devTapCount
                                Toast.makeText(
                                    context,
                                    if (lang == AppLanguage.PERSIAN) "$remaining ضربه تا فعال‌سازی حالت توسعه‌دهنده" else "You are $remaining steps away from Developer Mode",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (devTapCount >= 7) {
                                val newDevState = !settings.developerModeEnabled
                                onUpdateSettings(settings.copy(developerModeEnabled = newDevState))
                                Toast.makeText(
                                    context,
                                    if (newDevState) "🚀 Developer HUD Activated!" else "Developer Mode Disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                                devTapCount = 0
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text(
                    text = "High-Fidelity Audio • 120 FPS Real-Time DSP • Dynamic Aurora",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF707086))
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
