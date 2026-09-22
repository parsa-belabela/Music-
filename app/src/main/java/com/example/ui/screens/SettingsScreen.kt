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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showBackupDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showFeaturesGuideDialog by remember { mutableStateOf(false) }
    var showThemeRestartDialog by remember { mutableStateOf(false) }
    var pendingTheme by remember { mutableStateOf<AppTheme?>(null) }
    var backupJsonText by remember { mutableStateOf("") }
    var isExportMode by remember { mutableStateOf(true) }

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

    if (showThemeRestartDialog && pendingTheme != null) {
        val targetTheme = pendingTheme!!
        Dialog(onDismissRequest = { showThemeRestartDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        thickness = GlassThickness.THICK,
                        tintColor = palette.primary,
                        tintAlpha = 0.28f,
                        appTheme = targetTheme
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "تغییر تم برنامه" else "Apply Theme & Restart",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "برای اعمال کامل تم «${targetTheme.titleFa}» و بافت‌های سه‌بعدی آن، برنامه باید یک‌بار بسته شود تا با ظاهر جدید اجرا گردد. آیا برنامه اکنون بسته شود؟"
                        else
                            "To fully apply the \"${targetTheme.titleEn}\" theme and textures across the app, the app will close and apply your selection. Would you like to proceed?",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFC8C8DC), lineHeight = 22.sp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showThemeRestartDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(if (lang == AppLanguage.PERSIAN) "انصراف" else "Cancel")
                        }

                        Button(
                            onClick = {
                                onUpdateSettings(settings.copy(theme = targetTheme))
                                showThemeRestartDialog = false
                                (context as? android.app.Activity)?.finishAffinity()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                        ) {
                            Text(if (lang == AppLanguage.PERSIAN) "تأیید و خروج" else "OK & Close")
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = maxOf(bottomPadding + 20.dp, 120.dp)),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = Localization.getString("preferences", lang),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        color = palette.accent,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = Localization.getString("settings", lang),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }

        // Section: Features Guide & Capabilities Showcase Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(22.dp),
                        thickness = GlassThickness.THICK,
                        tintColor = palette.primary,
                        tintAlpha = 0.25f,
                        borderWidth = 1.2.dp,
                        appTheme = settings.theme
                    )
                    .clickable { showFeaturesGuideDialog = true }
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.28f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = Localization.getString("features_guide_btn", lang),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = Localization.getString("features_guide_desc", lang),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFB4B4CC)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Section: Language Selection
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
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Localization.getString("language_settings", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        AppLanguage.values().forEach { l ->
                            val isSelected = settings.language == l
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateSettings(settings.copy(language = l)) },
                                label = { Text(if (lang == AppLanguage.PERSIAN) l.titleFa else l.titleEn, fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.primary.copy(alpha = 0.35f),
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) palette.accent else Color(0xFF2A2840),
                                    selectedBorderColor = palette.accent
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
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
                        tintColor = palette.secondary,
                        tintAlpha = 0.12f,
                        borderWidth = 1.dp,
                        appTheme = settings.theme
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Localization.getString("visualizer_dsp", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(Localization.getString("visualizer_presets", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)))
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
                        Text(Localization.getString("audio_sensitivity", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
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
                        Text(Localization.getString("halo_glow", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
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
                        Text(Localization.getString("engine_fps", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(30, 60, 120).forEach { fps ->
                                FilterChip(
                                    selected = settings.visualizerFps == fps,
                                    onClick = { onUpdateSettings(settings.copy(visualizerFps = fps)) },
                                    label = { Text("${fps} FPS", fontSize = 11.sp) }
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
                        Text(Localization.getString("auto_color", lang), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.autoColorFromArtwork,
                            onCheckedChange = { onUpdateSettings(settings.copy(autoColorFromArtwork = it)) }
                        )
                    }
                }
            }
        }

        // Section: Audio DSP, Crossfade & Equalizer
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

                    // Open Equalizer Button
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
                }
            }
        }

        // Section: Lyrics & Typography
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.secondary,
                        tintAlpha = 0.12f,
                        borderWidth = 1.dp,
                        appTheme = settings.theme
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lyrics, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(Localization.getString("lyrics_typography", lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(Localization.getString("display_mode", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)))
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
                        Text(Localization.getString("font_size", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
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
                        Text(Localization.getString("karaoke_highlight", lang), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
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
                                        pendingTheme = t
                                        showThemeRestartDialog = true
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

        // Section: Hardware & Haptic Feedback
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.secondary,
                        tintAlpha = 0.12f,
                        borderWidth = 1.dp,
                        appTheme = settings.theme
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = palette.accent)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(Localization.getString("haptic_feedback", lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Localization.getString("haptic_feedback", lang), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.hapticFeedbackEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(hapticFeedbackEnabled = it)) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Localization.getString("pause_on_disconnect", lang), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
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
                        Text(Localization.getString("duck_volume", lang), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.duckVolumeOnInterruption,
                            onCheckedChange = { onUpdateSettings(settings.copy(duckVolumeOnInterruption = it)) }
                        )
                    }
                }
            }
        }

        // Section: Storage, Cache & History Management
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
                // Heartfelt user message requested
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
                    style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFA0A0B8), fontWeight = FontWeight.Bold)
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
