package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AuraApplication
import com.example.audio.AmbientPalette
import com.example.data.model.*
import com.example.data.repository.UserProfileData
import com.example.data.repository.UserProfileManager
import com.example.ui.components.*
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.util.Localization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    bottomPadding: Dp = 120.dp,
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
    val app = remember { try { AuraApplication.instance } catch (_: Exception) { null } }

    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showFeaturesGuideDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var devTapCount by remember { mutableIntStateOf(0) }

    val lang = settings.language
    val isFa = lang == AppLanguage.PERSIAN

    val allTracks by (app?.repository?.allTracks ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteTracks by (app?.repository?.favoriteTracks ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(Unit) {
        UserProfileManager.init(context)
        if (app != null) {
            withContext(Dispatchers.IO) {
                UserProfileManager.refreshProfileWithDatabase(context, app.repository)
            }
        }
    }
    val userProfile by UserProfileManager.profileFlow.collectAsState()

    val exportJsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonContent = onExportBackup()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                                writer.write(jsonContent)
                                writer.flush()
                            }
                        }
                    }
                    Toast.makeText(
                        context,
                        if (isFa) "فایل پشتیبان (.json) با موفقیت ذخیره شد!" else "Backup .json file saved successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        if (isFa) "خطا در ذخیره فایل: ${e.localizedMessage}" else "Export failed: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val importJsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val rawJson = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                            it.readText()
                        } ?: ""
                    }
                    val ok = if (rawJson.isNotBlank()) onImportBackup(rawJson) else false
                    Toast.makeText(
                        context,
                        if (ok) {
                            if (isFa) "اطلاعات از فایل JSON با موفقیت بازیابی شد!" else "Backup restored from JSON file!"
                        } else {
                            if (isFa) "ساختار فایل JSON معتبر نیست." else "Invalid JSON backup file."
                        },
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        if (isFa) "خطا در خواندن فایل: ${e.localizedMessage}" else "Import failed: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

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
        contentPadding = PaddingValues(top = 14.dp, bottom = maxOf(bottomPadding + 24.dp, 128.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Clean Spotify Settings Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.getString("settings", lang),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 23.sp
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onOpenAchievements,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AuraSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Achievements",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showFeaturesGuideDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AuraSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Guide",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Spotify User Profile Card
        item {
            AudiophileProfileDashboardCard(
                profile = userProfile,
                totalLibraryTracks = allTracks.size,
                totalFavorites = favoriteTracks.size,
                palette = palette,
                isFa = isFa,
                onEditAvatar = { showAvatarPickerDialog = true }
            )
        }

        // 3. Music Flashback Card
        item {
            MusicFlashbackCard(
                currentTrack = currentPlayingTrack,
                palette = palette,
                language = lang,
                customInsight = userProfile?.currentInsight,
                onShareInsight = { text ->
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, text)
                        type = "text/plain"
                    }
                    context.startActivity(
                        Intent.createChooser(
                            sendIntent,
                            if (isFa) "اشتراک خاطره موسیقی" else "Share Memory"
                        )
                    )
                }
            )
        }

        // 4. Playback & Audio Quality Section
        item {
            SettingsGlassSection(
                title = if (isFa) "کیفیت صدا و پخش" else "Playback & Audio Quality",
                subtitle = if (isFa) "اکولایزر، کالیبراسیون شنوایی و کراس‌فید" else "Equalizer, hearing profile & crossfade",
                icon = Icons.Default.GraphicEq,
                palette = palette
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingsActionTile(
                        icon = Icons.Default.Tune,
                        title = if (isFa) "اکولایزر" else "Equalizer",
                        subtitle = settings.eqPreset,
                        accentColor = AuraSpotifyGreen,
                        onClick = onOpenEqualizer,
                        modifier = Modifier.weight(1f)
                    )

                    SettingsActionTile(
                        icon = Icons.Default.Hearing,
                        title = if (isFa) "پروفایل شنوایی" else "Hearing Profile",
                        subtitle = if (isFa) "تست شخصی‌سازی" else "Personal EQ Test",
                        accentColor = Color.White,
                        onClick = onOpenHearingProfileTest,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.getString("crossfade_duration", lang),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (settings.crossfadeDurationSeconds == 0) {
                                if (isFa) "خاموش" else "Off"
                            } else "${settings.crossfadeDurationSeconds} s",
                            color = AuraSpotifyGreen,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.crossfadeDurationSeconds.toFloat(),
                        onValueChange = { onUpdateSettings(settings.copy(crossfadeDurationSeconds = it.toInt())) },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = AuraSpotifyGreen,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }

                SettingsToggleRow(
                    title = Localization.getString("gapless_playback", lang),
                    subtitle = if (isFa) "پخش پیوسته بدون مکث بین قطعات" else "Allows gapless playback between songs",
                    checked = settings.gaplessEnabled,
                    accentColor = AuraSpotifyGreen,
                    onCheckedChange = { onUpdateSettings(settings.copy(gaplessEnabled = it)) }
                )

                SettingsToggleRow(
                    title = if (isFa) "توقف هنگام جداسازی هدفون" else "Pause on Headphone Disconnect",
                    subtitle = if (isFa) "جلوگیری از پخش ناگهانی روی اسپیکر" else "Automatically pause when unplugged",
                    checked = settings.pauseOnHeadphoneDisconnect,
                    accentColor = AuraSpotifyGreen,
                    onCheckedChange = { onUpdateSettings(settings.copy(pauseOnHeadphoneDisconnect = it)) }
                )

                SettingsToggleRow(
                    title = if (isFa) "بازخورد لمسی (Haptic)" else "Haptic Feedback",
                    subtitle = if (isFa) "لرزش ظریف هنگام لمس کنترل‌ها" else "Subtle tactile response on controls",
                    checked = settings.hapticFeedbackEnabled,
                    accentColor = AuraSpotifyGreen,
                    onCheckedChange = { onUpdateSettings(settings.copy(hapticFeedbackEnabled = it)) }
                )
            }
        }

        // 5. Storage, Backup & Library Management
        item {
            SettingsGlassSection(
                title = if (isFa) "حافظه و فایل پشتیبان" else "Storage & Backup",
                subtitle = if (isFa) "ذخیره و بازیابی اطلاعات (.JSON) و مدیریت آرشیو" else "Export/import .json backup & manage local files",
                icon = Icons.Default.FolderOpen,
                palette = palette
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            exportJsonFileLauncher.launch("AuraMusic_Backup_$dateTag.json")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuraSpotifyGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFa) "ذخیره پشتیبان" else "Export JSON",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            importJsonFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Icon(
                            Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFa) "بازیابی فایل" else "Import JSON",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingsActionTile(
                        icon = Icons.Default.Sync,
                        title = if (isFa) "اسکن مجدد حافظه" else "Rescan Library",
                        subtitle = if (isFa) "${allTracks.size} آهنگ" else "${allTracks.size} tracks",
                        accentColor = AuraSpotifyGreen,
                        onClick = onRescanLibrary,
                        modifier = Modifier.weight(1f)
                    )

                    SettingsActionTile(
                        icon = Icons.Default.CleaningServices,
                        title = if (isFa) "حذف تکراری‌ها" else "Duplicates",
                        subtitle = if (isFa) "بهینه‌سازی آرشیو" else "Clean up files",
                        accentColor = Color(0xFFFBBF24),
                        onClick = onOpenDuplicatesReview,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = { showClearHistoryConfirm = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Localization.getString("clear_history", lang),
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 6. Language & Support
        item {
            SettingsGlassSection(
                title = if (isFa) "زبان و پشتیبانی" else "Languages & Support",
                subtitle = if (isFa) "انتخاب زبان برنامه و ارسال بازخورد" else "App language & user feedback",
                icon = Icons.Default.Language,
                palette = palette
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppLanguage.entries.forEach { l ->
                        val selected = settings.language == l
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) AuraSpotifyGreen else AuraSurfaceElevated)
                                .clickable { onUpdateSettings(settings.copy(language = l)) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (l == AppLanguage.ENGLISH) "English" else "فارسی",
                                color = if (selected) Color.Black else Color.White,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showFeedbackDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_feedback_dialog_button")
                ) {
                    Icon(
                        Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Localization.getString("feedback_dialog_title", lang),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 7. About & Developer Mode Trigger
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Aura Music • v2.0",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (settings.developerModeEnabled) AuraSpotifyGreen else Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            devTapCount++
                            if (devTapCount >= 7) {
                                val nextState = !settings.developerModeEnabled
                                onUpdateSettings(settings.copy(developerModeEnabled = nextState))
                                Toast.makeText(
                                    context,
                                    if (nextState) "Developer Telemetry HUD Enabled" else "Developer Mode Disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                                devTapCount = 0
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
                Text(
                    text = if (isFa) "طراحی مینیمال و مهندسی صوتی پیشرفته" else "Minimalist Design & High-Fidelity Audio Engine",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
    }

    if (showAvatarPickerDialog) {
        val emojis = listOf("🎧", "🎵", "✨", "👑", "🎸", "🌌", "🔥", "⚡", "💎", "🌙", "🦁", "🦋")
        AlertDialog(
            onDismissRequest = { showAvatarPickerDialog = false },
            containerColor = AuraSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    if (isFa) "انتخاب آواتار" else "Choose Avatar",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    emojis.chunked(4).forEach { rowEmojis ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowEmojis.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(AuraSurface)
                                        .clickable {
                                            UserProfileManager.setAvatarEmoji(context, emoji)
                                            showAvatarPickerDialog = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAvatarPickerDialog = false }) {
                    Text(if (isFa) "بستن" else "Close", color = TextSecondary)
                }
            }
        )
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            containerColor = AuraSurfaceElevated,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    if (isFa) "پاک‌سازی تاریخچه پخش" else "Clear Listening History",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isFa)
                        "آیا مطمئن هستید که می‌خواهید تاریخچه شنیداری و آمار Wrapped را پاک کنید؟ (آهنگ‌ها و پلی‌لیست‌های شما حذف نمی‌شوند)"
                    else
                        "Are you sure you want to clear your playback history? (Your songs and playlists will not be deleted)",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearPlaybackHistory()
                        showClearHistoryConfirm = false
                        Toast.makeText(
                            context,
                            if (isFa) "تاریخچه پاک شد" else "History cleared",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(if (isFa) "پاک کردن" else "Clear", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text(if (isFa) "انصراف" else "Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun AudiophileProfileDashboardCard(
    profile: UserProfileData?,
    totalLibraryTracks: Int,
    totalFavorites: Int,
    palette: AmbientPalette,
    isFa: Boolean,
    onEditAvatar: () -> Unit
) {
    val p = profile ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuraSurface)
            .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(AuraSurfaceElevated)
                        .clickable { onEditAvatar() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = p.avatarEmoji, fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFa) "پروفایل کاربری" else "Aura Listener",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isFa) "همراه از ${p.joinDays} روز پیش" else "${p.joinDays} days with Aura",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AuraSurfaceElevated)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatColumn(
                    value = "$totalLibraryTracks",
                    label = if (isFa) "آهنگ‌ها" else "Songs",
                    color = Color.White
                )
                ProfileStatColumn(
                    value = "$totalFavorites",
                    label = if (isFa) "پسندها" else "Liked",
                    color = AuraSpotifyGreen
                )
                ProfileStatColumn(
                    value = "${p.totalTracksPlayed}",
                    label = if (isFa) "پخش‌ها" else "Streams",
                    color = Color.White
                )
                ProfileStatColumn(
                    value = String.format(Locale.US, "%.1fh", p.totalListeningHours),
                    label = if (isFa) "ساعت" else "Hours",
                    color = AuraSpotifyGreen
                )
            }
        }
    }
}

@Composable
private fun ProfileStatColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun SettingsGlassSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    palette: AmbientPalette,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuraSurface)
            .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.5.sp
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            HorizontalDivider(color = Color(0x14FFFFFF))

            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = AuraSurfaceElevated
            )
        )
    }
}

@Composable
private fun SettingsActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AuraSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

