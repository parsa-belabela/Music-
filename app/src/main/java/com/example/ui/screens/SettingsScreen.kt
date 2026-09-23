package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    onOpenVipPaywall: (String?) -> Unit = {},
    onOpenAchievements: () -> Unit = {},
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
    val isVip = remember(settings) { com.example.monetization.EntitlementManager.isVip(context) }
    val goldAccent = Color(0xFFFFD700)

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

        // Section: VIP Status & Achievements Showcase Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(22.dp),
                        thickness = GlassThickness.THICK,
                        tintColor = if (isVip) goldAccent else palette.primary,
                        tintAlpha = if (isVip) 0.26f else 0.18f,
                        borderWidth = 1.2.dp,
                        appTheme = settings.theme
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(goldAccent.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "VIP",
                                    tint = goldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isVip) {
                                        if (lang == AppLanguage.PERSIAN) "عضویت طلایی Aura VIP فعال است ✨" else "Aura Golden VIP Active ✨"
                                    } else {
                                        if (lang == AppLanguage.PERSIAN) "ارتقا به نسخه VIP آئورا" else "Upgrade to Aura VIP"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = if (isVip) {
                                        val exp = com.example.monetization.EntitlementManager.getVipExpiryMillis(context)
                                        if (exp == Long.MAX_VALUE) {
                                            if (lang == AppLanguage.PERSIAN) "اشتراک دائمی بدون محدودیت" else "Permanent VIP Access"
                                        } else {
                                            val rem = com.example.monetization.EntitlementManager.formatRemainingTime(exp - System.currentTimeMillis(), lang == AppLanguage.PERSIAN)
                                            if (lang == AppLanguage.PERSIAN) "زمان باقی‌مانده: $rem" else "Expires in $rem"
                                        }
                                    } else {
                                        if (lang == AppLanguage.PERSIAN) "پوسته‌های کلکسیونی VIP • ۲۴ ساعت رایگان با تماشای تبلیغ" else "VIP collector skins • 24h free with ad"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFC0C0D4),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenVipPaywall(null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isVip) palette.primary.copy(alpha = 0.6f) else goldAccent,
                                contentColor = if (isVip) Color.White else Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isVip) Icons.Default.Settings else Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isVip) {
                                    if (lang == AppLanguage.PERSIAN) "مدیریت اشتراک / کد" else "Manage VIP / Code"
                                } else {
                                    if (lang == AppLanguage.PERSIAN) "ارتقا یا تست ۲۴ساعته" else "Upgrade / 24h Free"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenAchievements,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "دستاوردها 🏆" else "Milestones 🏆",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Section: Hyped VIP Collector Skins & Cinematic Now Playing Themes
        item {
            val collectorSkins = remember {
                listOf(
                    CollectorSkinOption(
                        id = "default",
                        titleFa = "شیشه‌ای استاندارد (Liquid Glass)",
                        titleEn = "Signature Liquid Glass",
                        tagFa = "رایگان • پیش‌فرض",
                        tagEn = "Free • Default",
                        descFa = "افکت شیشه‌ای مدرن با نور پس‌زمینه پویا هماهنگ با کاور موزیک و کنترل‌های کریستالی",
                        descEn = "Signature frosted liquid glass with dynamic album art aura and smooth glass controls",
                        primaryColor = Color(0xFF00E5FF),
                        secondaryColor = Color(0xFF7C4DFF),
                        icon = Icons.Default.BlurOn,
                        entitlementId = null
                    ),
                    CollectorSkinOption(
                        id = "barbie_dream",
                        titleFa = "باربی دریم (Barbie Dream Glow)",
                        titleEn = "Barbie Dream Glow",
                        tagFa = "پوسته VIP صورتی نئونی ✨",
                        tagEn = "VIP Pink Sparkle ✨",
                        descFa = "اتمسفر رویایی دریم‌هاوس با هاله صورتی نئونی، اشعه‌های طلایی و ذرات معلق پروانه‌ای",
                        descEn = "Dreamhouse neon pink radiance with floating golden sparkles, heart aura and glamour glow",
                        primaryColor = Color(0xFFFF1493),
                        secondaryColor = Color(0xFFFFD700),
                        icon = Icons.Default.Favorite,
                        entitlementId = "now_playing_barbie"
                    ),
                    CollectorSkinOption(
                        id = "batman_knight",
                        titleFa = "شوالیه تاریکی (The Dark Knight)",
                        titleEn = "The Dark Knight (Batman)",
                        tagFa = "پوسته VIP بتمن و گاتهام 🦇",
                        tagEn = "VIP Gotham Armor 🦇",
                        descFa = "اتمسفر تاریک تیتانیومی با نورافکن زرد بت‌سیگنال، رادار رفلکتور و افکت سینمایی باران",
                        descEn = "Titanium armor HUD with Bat-Signal searchlight radar pulse and midnight rain ambiance",
                        primaryColor = Color(0xFFFFCC00),
                        secondaryColor = Color(0xFF1E88E5),
                        icon = Icons.Default.Shield,
                        entitlementId = "now_playing_batman"
                    ),
                    CollectorSkinOption(
                        id = "last_of_us",
                        titleFa = "لست آف آز (The Last of Us)",
                        titleEn = "The Last of Us (Firefly)",
                        tagFa = "پوسته VIP فایرفلای و بقا 🌿",
                        tagEn = "VIP Firefly Spores 🌿",
                        descFa = "قاب چوب ماهوگانی روستیک با ذرات بیولومینسانس درخشان و اتمسفر رازآلود بقا",
                        descEn = "Weathered rustic mahogany frame with glowing bioluminescent firefly spores and warm acoustics",
                        primaryColor = Color(0xFFFFD54F),
                        secondaryColor = Color(0xFF81C784),
                        icon = Icons.Default.FilterVintage,
                        entitlementId = "now_playing_last_of_us"
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(22.dp),
                        thickness = GlassThickness.THICK,
                        tintColor = palette.primary,
                        tintAlpha = 0.16f,
                        borderWidth = 1.2.dp,
                        appTheme = settings.theme
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(palette.accent.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "پوسته‌های سینمایی و کلکسیونی VIP" else "VIP Collector Themes & Skins",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "تغییر کامل تم و محیط صفحه پخش موسیقی" else "Transform your Now Playing visual universe",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A5BA), fontSize = 11.sp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500))))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "۴ تم ویژه" else "4 Themes",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Skin Cards
                    collectorSkins.forEach { skin ->
                        val isSelected = settings.selectedNowPlayingStyle == skin.id
                        val isUnlocked = skin.entitlementId == null ||
                                isVip ||
                                unlockedStyles.contains(skin.id) ||
                                com.example.monetization.EntitlementManager.hasAccess(context, skin.entitlementId)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.linearGradient(
                                            listOf(
                                                skin.primaryColor.copy(alpha = 0.22f),
                                                skin.secondaryColor.copy(alpha = 0.12f),
                                                Color(0xFF0F111A)
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF141624),
                                                Color(0xFF0C0E17)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    brush = if (isSelected) {
                                        Brush.sweepGradient(
                                            listOf(skin.primaryColor, skin.secondaryColor, skin.primaryColor)
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(skin.primaryColor.copy(alpha = 0.35f), Color.Transparent)
                                        )
                                    },
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    if (isUnlocked) {
                                        onSelectNowPlayingStyle(skin.id)
                                        onUpdateSettings(settings.copy(selectedNowPlayingStyle = skin.id))
                                        Toast.makeText(
                                            context,
                                            if (lang == AppLanguage.PERSIAN) "پوسته «${skin.titleFa}» فعال شد ✨" else "Theme \"${skin.titleEn}\" activated ✨",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        onOpenVipPaywall(skin.entitlementId)
                                    }
                                }
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(skin.primaryColor.copy(alpha = 0.25f))
                                                .border(1.dp, skin.primaryColor.copy(alpha = 0.6f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = skin.icon,
                                                contentDescription = null,
                                                tint = skin.primaryColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (lang == AppLanguage.PERSIAN) skin.titleFa else skin.titleEn,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            )
                                            Text(
                                                text = if (lang == AppLanguage.PERSIAN) skin.tagFa else skin.tagEn,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = skin.primaryColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }

                                    // Status Pill
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(skin.primaryColor.copy(alpha = 0.25f))
                                                .border(1.dp, skin.primaryColor, RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = skin.primaryColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (lang == AppLanguage.PERSIAN) "فعال است" else "Active",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else if (isUnlocked) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.12f))
                                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (lang == AppLanguage.PERSIAN) "انتخاب تم" else "Select",
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0x35FFD700))
                                                .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFD700),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (lang == AppLanguage.PERSIAN) "VIP / تبلیغ" else "VIP / Ad",
                                                    color = Color(0xFFFFD700),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) skin.descFa else skin.descEn,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFB0B7C6),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Audio Hardware, DSP & Continuous Mix
        item {
            val hasContinuousAccess = com.example.monetization.EntitlementManager.hasAccess(context, "continuous_mix")
            val hasHearingAccess = com.example.monetization.EntitlementManager.hasAccess(context, "personal_hearing_profile")
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(Localization.getString("continuous_mix", lang), style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
                                if (!hasContinuousAccess) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "VIP", tint = goldAccent, modifier = Modifier.size(13.dp))
                                }
                            }
                            Text(Localization.getString("continuous_mix_desc", lang), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8)))
                        }
                        Switch(
                            checked = settings.continuousMixEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (hasContinuousAccess) {
                                        onUpdateSettings(settings.copy(continuousMixEnabled = true))
                                    } else {
                                        onOpenVipPaywall("continuous_mix")
                                    }
                                } else {
                                    onUpdateSettings(settings.copy(continuousMixEnabled = false))
                                }
                            }
                        )
                    }

                    // Hearing Calibration Profile Button
                    Button(
                        onClick = {
                            if (hasHearingAccess) {
                                onOpenHearingProfileTest()
                            } else {
                                onOpenVipPaywall("personal_hearing_profile")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Hearing, contentDescription = null, modifier = Modifier.size(18.dp), tint = palette.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Localization.getString("hearing_profile", lang), color = Color.White)
                        if (!hasHearingAccess) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.AutoAwesome, contentDescription = "VIP", tint = goldAccent, modifier = Modifier.size(14.dp))
                        }
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
                            val vFeature = getVisualizerFeatureId(preset.mode)
                            val isLocked = vFeature != null && !com.example.monetization.EntitlementManager.hasAccess(context, vFeature)
                            FilterChip(
                                selected = settings.visualizerMode == preset.mode,
                                onClick = {
                                    if (isLocked) {
                                        onOpenVipPaywall(vFeature)
                                    } else {
                                        onSetPreset(preset)
                                    }
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(preset.name, fontSize = 11.sp)
                                        if (isLocked) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.AutoAwesome, contentDescription = "VIP", modifier = Modifier.size(11.dp), tint = goldAccent)
                                        }
                                    }
                                }
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.values().forEach { l ->
                            FilterChip(
                                selected = settings.language == l,
                                onClick = {
                                    onUpdateSettings(settings.copy(language = l))
                                },
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
                            val tFeature = getThemeFeatureId(t)
                            val isLocked = tFeature != null && !com.example.monetization.EntitlementManager.hasAccess(context, tFeature)
                            FilterChip(
                                selected = settings.theme == t,
                                onClick = {
                                    if (isLocked) {
                                        onOpenVipPaywall(tFeature)
                                    } else {
                                        if (settings.theme != t) {
                                            val signatureAccent = when (t) {
                                                AppTheme.PURE_LIQUID_GLASS -> 0xFF00E5FF
                                                AppTheme.CYBER_NIGHTS -> 0xFF00F0FF
                                                AppTheme.Y2K_CHROME -> 0xFF38BDF8
                                                AppTheme.VELVET_NOIR -> 0xFFFFD700
                                                AppTheme.SUNSET_RAVE -> 0xFFFF5E00
                                                AppTheme.DIGITAL_ACID -> 0xFF39FF14
                                                AppTheme.MONOCHROME_NOIR -> 0xFFFFFFFF
                                            }
                                            onUpdateSettings(settings.copy(theme = t, customAccentColor = signatureAccent))
                                        }
                                    }
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (lang == AppLanguage.PERSIAN) t.titleFa else t.titleEn, fontSize = 12.sp)
                                        if (isLocked) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.AutoAwesome, contentDescription = "VIP", modifier = Modifier.size(11.dp), tint = goldAccent)
                                        }
                                    }
                                }
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
                            Toast.makeText(context, if (lang == AppLanguage.PERSIAN) "تاریخچه پخش پاکسازی شد" else "Playback history cleared", Toast.LENGTH_SHORT).show()
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
                                    if (newDevState) {
                                        if (lang == AppLanguage.PERSIAN) "🚀 ابزار تله‌متری و دولوپر فعال شد!" else "🚀 Developer HUD Activated!"
                                    } else {
                                        if (lang == AppLanguage.PERSIAN) "حالت توسعه‌دهنده غیرفعال شد" else "Developer Mode Disabled"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                                devTapCount = 0
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Text(
                    text = if (lang == AppLanguage.PERSIAN)
                        "صدای شفاف استودیویی • پردازش بلادرنگ ۱۲۰ فریم • شیشه مایع آئورا"
                    else "High-Fidelity Audio • 120 FPS Real-Time DSP • Dynamic Aurora",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF707086))
                )
            }
        }
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Text(
                    if (isExportMode) {
                        if (lang == AppLanguage.PERSIAN) "خروجی فایل پشتیبان JSON" else "Export Backup JSON"
                    } else {
                        if (lang == AppLanguage.PERSIAN) "بازیابی از فایل پشتیبان JSON" else "Import Backup JSON"
                    }
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isExportMode) {
                            if (lang == AppLanguage.PERSIAN)
                                "این متن JSON را کپی کنید تا از لیست‌های پخش، علاقه‌مندی‌ها و تنظیمات پشتیبان داشته باشید:"
                            else "Copy this JSON to backup your playlists, favorites and settings:"
                        } else {
                            if (lang == AppLanguage.PERSIAN)
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
                                        if (lang == AppLanguage.PERSIAN) "اطلاعات با موفقیت بازیابی شد!" else "Restored successfully!"
                                    } else {
                                        if (lang == AppLanguage.PERSIAN) "فرمت فایل پشتیبان نامعتبر است" else "Invalid backup JSON"
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
                            if (lang == AppLanguage.PERSIAN) "بستن" else "Close"
                        } else {
                            if (lang == AppLanguage.PERSIAN) "بازیابی" else "Restore"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text(if (lang == AppLanguage.PERSIAN) "انصراف" else "Cancel")
                }
            }
        )
    }
}

private fun getThemeFeatureId(theme: AppTheme): String? = when (theme) {
    AppTheme.PURE_LIQUID_GLASS, AppTheme.CYBER_NIGHTS -> null
    AppTheme.VELVET_NOIR -> "theme_velvet_noir"
    AppTheme.SUNSET_RAVE -> "theme_sunset_rave"
    AppTheme.DIGITAL_ACID -> "theme_digital_acid"
    AppTheme.Y2K_CHROME -> "theme_y2k_chrome"
    AppTheme.MONOCHROME_NOIR -> "theme_monochrome_noir"
}

private fun getVisualizerFeatureId(mode: VisualizerMode): String? = when (mode) {
    VisualizerMode.AMBIENT_HALO, VisualizerMode.BASS_GLOW, VisualizerMode.SPECTRUM -> null
    VisualizerMode.CIRCULAR_SPECTRUM -> "visualizer_circular_spectrum"
    VisualizerMode.WAVEFORM -> "visualizer_waveform"
    VisualizerMode.RADIAL_WAVE -> "visualizer_radial_wave"
    VisualizerMode.PULSE_RING -> "visualizer_pulse_ring"
    VisualizerMode.AURORA -> "visualizer_aurora"
    VisualizerMode.LIQUID -> "visualizer_liquid"
    VisualizerMode.PARTICLE_FIELD -> "visualizer_particle_field"
    VisualizerMode.DOTS -> "visualizer_dots"
    VisualizerMode.CINEMATIC_FOG -> "visualizer_cinematic_fog"
}

private data class CollectorSkinOption(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val tagFa: String,
    val tagEn: String,
    val descFa: String,
    val descEn: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val icon: ImageVector,
    val entitlementId: String?
)

