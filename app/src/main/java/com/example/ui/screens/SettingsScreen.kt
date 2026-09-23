package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AmbientPalette
import com.example.data.model.*
import com.example.data.repository.UserBadge
import com.example.data.repository.UserProfileData
import com.example.data.repository.UserProfileManager
import com.example.data.repository.UserQuest
import com.example.ui.components.*
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
    onOpenVipPaywall: (String?) -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    currentPlayingTrack: Track? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showBackupDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showFeaturesGuideDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }
    var isExportMode by remember { mutableStateOf(true) }

    var devTapCount by remember { mutableStateOf(0) }

    val lang = settings.language
    val isFa = lang == AppLanguage.PERSIAN

    // User Profile Data
    LaunchedEffect(Unit) {
        UserProfileManager.init(context)
    }
    val userProfile by UserProfileManager.profileFlow.collectAsState()

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

    if (showSupportDialog) {
        SupportDonationDialog(
            palette = palette,
            language = lang,
            onDismiss = { showSupportDialog = false },
            onSupportSuccess = {
                showSupportDialog = false
                UserProfileManager.refreshProfile(context)
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = maxOf(bottomPadding + 20.dp, 120.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
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

        // 1. GORGEOUS USER PROFILE SECTION
        item {
            UserProfileCard(
                profile = userProfile,
                palette = palette,
                isFa = isFa,
                onEditName = { showEditProfileDialog = true },
                onEditAvatar = { showAvatarPickerDialog = true },
                onOpenSupport = { showSupportDialog = true }
            )
        }

        // 2. HONOR BADGES & ACHIEVEMENTS SHELF
        item {
            BadgesAndHonorsSection(
                badges = userProfile?.badges ?: emptyList(),
                palette = palette,
                isFa = isFa,
                onOpenSupport = { showSupportDialog = true }
            )
        }

        // 3. WEEKLY MISSIONS & QUESTS
        item {
            QuestsMissionsSection(
                quests = userProfile?.quests ?: emptyList(),
                palette = palette,
                isFa = isFa,
                onOpenSupport = { showSupportDialog = true }
            )
        }

        // 4. SPOTIFY-LIKE MUSIC FLASHBACK MEMORY CARD
        item {
            MusicFlashbackCard(
                currentTrack = null,
                palette = palette,
                language = lang,
                customInsight = userProfile?.currentInsight,
                onShareInsight = { text ->
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "اشتراک خاطره موسیقی"))
                }
            )
        }

        // 5. Section: Audio Hardware, DSP & Equalizer
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

                    // Haptic Feedback
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isFa) "لرزش و فیدبک لمسی (Haptics)" else "Haptic Feedback", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                        Switch(
                            checked = settings.hapticFeedbackEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(hapticFeedbackEnabled = it)) }
                        )
                    }
                }
            }
        }

        // 6. Section: Visualizer & Ambient Halo
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

        // 7. Section: Language Selection
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.values().forEach { l ->
                            FilterChip(
                                selected = settings.language == l,
                                onClick = { onUpdateSettings(settings.copy(language = l)) },
                                label = {
                                    Text(
                                        text = if (l == AppLanguage.PERSIAN) "فارسی (پیش‌فرض)" else "English",
                                        fontWeight = if (settings.language == l) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = if (settings.language == l) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // 8. Section: Storage & Backup
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
                            Toast.makeText(context, if (isFa) "تاریخچه پخش پاکسازی شد" else "Playback history cleared", Toast.LENGTH_SHORT).show()
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

        // 9. Section: Dedicated Feedback & Support
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

        // 10. Footer / About Note
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isFa) "امیدوارم با لذت بیشتری بتونید آهنگ گوش کنید 💙" else "Hope you enjoy your music with even greater pleasure 💙",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFC7D7FE),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    ),
                    textAlign = TextAlign.Center
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
                                    if (isFa) "$remaining ضربه تا فعال‌سازی حالت توسعه‌دهنده" else "You are $remaining steps away from Developer Mode",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (devTapCount >= 7) {
                                val newDevState = !settings.developerModeEnabled
                                onUpdateSettings(settings.copy(developerModeEnabled = newDevState))
                                Toast.makeText(
                                    context,
                                    if (newDevState) {
                                        if (isFa) "🚀 ابزار تله‌متری و دولوپر فعال شد!" else "🚀 Developer HUD Activated!"
                                    } else {
                                        if (isFa) "حالت توسعه‌دهنده غیرفعال شد" else "Developer Mode Disabled"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                                devTapCount = 0
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text(
                    text = if (isFa)
                        "صدای شفاف استودیویی • پردازش بلادرنگ ۱۲۰ فریم • شیشه مایع آئورا"
                    else "High-Fidelity Audio • 120 FPS Real-Time DSP • Dynamic Aurora",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF707086))
                )
            }
        }
    }

    // Edit Name Dialog
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(userProfile?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(if (isFa) "ویرایش نام پروفایل" else "Edit Profile Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    label = { Text(if (isFa) "نام نمایشی شما" else "Your Display Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.trim().isNotEmpty()) {
                            UserProfileManager.setUserName(context, tempName)
                        }
                        showEditProfileDialog = false
                    }
                ) {
                    Text(if (isFa) "ذخیره" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(if (isFa) "انصراف" else "Cancel")
                }
            }
        )
    }

    // Emoji Avatar Picker Dialog
    if (showAvatarPickerDialog) {
        val emojis = listOf("🎧", "🎵", "✨", "👑", "🎸", "🌌", "❤️", "🦋", "🦁", "💎", "🌙", "🔥", "⚡", "🍀", "🌸")
        AlertDialog(
            onDismissRequest = { showAvatarPickerDialog = false },
            title = { Text(if (isFa) "انتخاب آواتار کاربری" else "Select Profile Avatar") },
            text = {
                Column {
                    Text(
                        text = if (isFa) "آواتار دلخواه خود را برای نمایش در بالای پروفایل انتخاب کنید:" else "Choose your favorite avatar emoji:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0A5BA)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        emojis.take(5).forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        UserProfileManager.setAvatarEmoji(context, emoji)
                                        showAvatarPickerDialog = false
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        emojis.drop(5).take(5).forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        UserProfileManager.setAvatarEmoji(context, emoji)
                                        showAvatarPickerDialog = false
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        emojis.drop(10).forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 28.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        UserProfileManager.setAvatarEmoji(context, emoji)
                                        showAvatarPickerDialog = false
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarPickerDialog = false }) {
                    Text(if (isFa) "بستن" else "Close")
                }
            }
        )
    }

    // Backup Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Text(
                    if (isExportMode) {
                        if (isFa) "خروجی فایل پشتیبان JSON" else "Export Backup JSON"
                    } else {
                        if (isFa) "بازیابی از فایل پشتیبان JSON" else "Import Backup JSON"
                    }
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isExportMode) {
                            if (isFa)
                                "این متن JSON را کپی کنید تا از لیست‌های پخش، علاقه‌مندی‌ها و تنظیمات پشتیبان داشته باشید:"
                            else "Copy this JSON to backup your playlists, favorites and settings:"
                        } else {
                            if (isFa)
                                "متن JSON پشتیبان را در کادر زیر جای‌گذاری کنید:"
                            else "Paste your backup JSON below:"
                        },
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
                                Toast.makeText(
                                    context,
                                    if (success) {
                                        if (isFa) "اطلاعات با موفقیت بازیابی شد!" else "Restored successfully!"
                                    } else {
                                        if (isFa) "فرمت فایل پشتیبان نامعتبر است" else "Invalid backup JSON"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                                showBackupDialog = false
                            }
                        } else {
                            showBackupDialog = false
                        }
                    }
                ) {
                    Text(
                        if (isExportMode) {
                            if (isFa) "بستن" else "Close"
                        } else {
                            if (isFa) "بازیابی" else "Restore"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text(if (isFa) "انصراف" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserProfileCard(
    profile: UserProfileData?,
    palette: AmbientPalette,
    isFa: Boolean,
    onEditName: () -> Unit,
    onEditAvatar: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val p = profile ?: return
    val goldColor = Color(0xFFFFD700)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(26.dp),
                thickness = GlassThickness.THICK,
                tintColor = if (p.isSupporter) goldColor else palette.primary,
                tintAlpha = if (p.isSupporter) 0.22f else 0.16f,
                borderWidth = 1.5.dp
            )
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar with edit badge
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (p.isSupporter) goldColor.copy(alpha = 0.4f) else palette.primary.copy(alpha = 0.35f),
                                        Color(0xFF141727)
                                    )
                                )
                            )
                            .border(
                                width = 2.dp,
                                color = if (p.isSupporter) goldColor else palette.accent,
                                shape = CircleShape
                            )
                            .clickable { onEditAvatar() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = p.avatarEmoji, fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onEditName() }
                        ) {
                            Text(
                                text = p.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Name",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (p.isSupporter) goldColor.copy(alpha = 0.25f) else palette.primary.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = p.supporterTitle,
                                    color = if (p.isSupporter) goldColor else palette.accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = if (isFa) "همراه از ${p.joinDays} روز پیش" else "Member for ${p.joinDays}d",
                                color = Color(0xFFA0A5BA),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22000000))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    label = if (isFa) "قطعات پخش‌شده" else "Tracks Played",
                    value = "${p.totalTracksPlayed} 🎵",
                    color = palette.accent
                )
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                StatItem(
                    label = if (isFa) "ساعت در موسیقی" else "Listening Hours",
                    value = "${String.format("%.1f", p.totalListeningHours)} ⏳",
                    color = Color(0xFF64B5F6)
                )
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
                StatItem(
                    label = if (isFa) "نشان‌های کسب‌شده" else "Badges",
                    value = "${p.badges.count { it.isUnlocked }} / ${p.badges.size} 🏆",
                    color = goldColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Heartwarming Support Button
            Button(
                onClick = onOpenSupport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (p.isSupporter) goldColor.copy(alpha = 0.35f) else Color(0xFFFF4081).copy(alpha = 0.85f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (p.isSupporter) goldColor else Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (p.isSupporter) {
                        if (isFa) "شما حامی مهربون برنامه هستید ❤️ (افزایش حمایت)" else "You are a Kind Supporter ❤️ (Donate More)"
                    } else {
                        if (isFa) "حمایت از برنامه و سازنده ❤️ (رایگان با دیدن تبلیغ یا خرید مایکت)" else "Support Developer ❤️ (Free Ad or Myket Purchase)"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        Text(text = label, color = Color(0xFFA0A5BA), fontSize = 10.sp)
    }
}

@Composable
private fun BadgesAndHonorsSection(
    badges: List<UserBadge>,
    palette: AmbientPalette,
    isFa: Boolean,
    onOpenSupport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                thickness = GlassThickness.REGULAR,
                tintColor = palette.primary,
                tintAlpha = 0.14f,
                borderWidth = 1.dp
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFa) "نشان‌ها و دستاوردهای افتخاری" else "Honor Badges & Achievements",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    )
                }

                Text(
                    text = if (isFa) "برای شما" else "For You",
                    color = palette.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            badges.forEach { badge ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (badge.isUnlocked) Color(0x2BFFFFFF) else Color(0x11FFFFFF))
                        .border(
                            width = 1.dp,
                            color = if (badge.isUnlocked) {
                                if (badge.isKindBadge) Color(0xFFFF4081).copy(alpha = 0.6f) else Color(0xFFFFD700).copy(alpha = 0.5f)
                            } else Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (!badge.isUnlocked && badge.isKindBadge) {
                                onOpenSupport()
                            }
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = badge.emoji, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isFa) badge.titleFa else badge.titleEn,
                                    color = if (badge.isUnlocked) Color.White else Color(0xFFA0A5BA),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isFa) badge.descFa else badge.descEn,
                                    color = if (badge.isUnlocked) Color(0xFFC0C5D8) else Color(0xFF707588),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (badge.isUnlocked) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Unlocked",
                                tint = if (badge.isKindBadge) Color(0xFFFF4081) else Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestsMissionsSection(
    quests: List<UserQuest>,
    palette: AmbientPalette,
    isFa: Boolean,
    onOpenSupport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                thickness = GlassThickness.REGULAR,
                tintColor = palette.primary,
                tintAlpha = 0.14f,
                borderWidth = 1.dp
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFa) "ماموریت‌ها و چالش‌های هفتگی" else "Weekly Quests & Missions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    )
                }

                Text(
                    text = if (isFa) "پیشرفت شما" else "Your Progress",
                    color = palette.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            quests.forEach { quest ->
                val progress = (quest.current.toFloat() / quest.target.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22000000))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = quest.emoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isFa) quest.titleFa else quest.titleEn,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = quest.descFa,
                                        color = Color(0xFFA0A5BA),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Text(
                                text = "${quest.current} / ${quest.target}",
                                color = if (quest.isCompleted) Color(0xFF81C784) else palette.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (quest.isCompleted) Color(0xFF81C784) else palette.primary,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}
