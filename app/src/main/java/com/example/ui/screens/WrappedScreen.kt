package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AmbientPalette
import com.example.data.model.*
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization
import com.example.util.WrappedEngine
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(
    appSettings: AppSettings,
    palette: AmbientPalette,
    periods: List<WrappedPeriod>,
    selectedPeriod: WrappedPeriod?,
    wrappedStats: WrappedStats?,
    onSelectPeriod: (WrappedPeriod) -> Unit,
    onPlayTrackById: (String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = appSettings.language
    val isFa = lang == AppLanguage.PERSIAN

    var activeCategory by remember {
        mutableStateOf(selectedPeriod?.timeframe ?: WrappedTimeframe.WEEKLY)
    }
    var showInteractiveStoryModal by remember { mutableStateOf(false) }
    var hasNewUnseenBanner by remember { mutableStateOf(false) }

    // فیلتر دوره‌ها بر اساس ۳ دسته‌بندی: هفتگی، ماهانه، سالانه
    val filteredPeriods = remember(periods, activeCategory) {
        periods.filter { it.timeframe == activeCategory }.ifEmpty { periods }
    }

    // بررسی خودکار آماده بودن گزارش جدید و ارسال نوتیفیکیشن سیستمی (مشابه Spotify)
    LaunchedEffect(wrappedStats?.period?.id, wrappedStats?.totalTracksPlayed) {
        if (wrappedStats != null && wrappedStats.totalTracksPlayed > 0) {
            val isNew = WrappedEngine.checkAndNotifyNewWrappedIfNeeded(context, wrappedStats, lang)
            hasNewUnseenBanner = isNew
        }
    }

    fun formatNum(value: Any): String {
        val s = value.toString()
        return if (isFa) WrappedEngine.toPersianDigits(s) else s
    }

    fun shareRecap(stats: WrappedStats) {
        val periodName = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn
        val shareText = buildString {
            append("✨ AURA WRAPPED — $periodName ✨\n\n")
            append("🎧 ${Localization.getString("total_listening_time", lang)}: ${formatNum(stats.totalListeningMinutes)} ${Localization.getString("minutes", lang)}\n")
            append("🎵 ${Localization.getString("tracks_played", lang)}: ${formatNum(stats.totalTracksPlayed)}\n")
            append("🔥 ${Localization.getString("listening_streak", lang)}: ${formatNum(stats.listeningStreakDays)} ${Localization.getString("days", lang)}\n")
            append("🌟 ${Localization.getString("audio_personality", lang)}: ${if (isFa) stats.audioPersonalityFa else stats.audioPersonalityEn}\n\n")
            append("🏆 ${Localization.getString("top_songs", lang)}:\n")
            stats.topSongs.take(5).forEachIndexed { idx, s ->
                append("  ${formatNum(idx + 1)}. ${s.title} - ${s.artist} (${formatNum(s.playCount)}x)\n")
            }
        }

        val imageUri = com.example.util.WrappedCardGenerator.generateWrappedStoryBitmap(context, stats, palette, lang)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            if (imageUri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
        }
        context.startActivity(Intent.createChooser(sendIntent, Localization.getString("share_wrapped", lang)))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = maxOf(bottomPadding + 24.dp, 128.dp)),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // =========================================================================
        // ۱. بنر هوشمند آماده شدن گزارش Wrapped جدید (مشابه اعلان داخل برنامه Spotify)
        // =========================================================================
        if (hasNewUnseenBanner && wrappedStats != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = palette.accent)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    palette.primary,
                                    palette.accent.copy(alpha = 0.85f),
                                    Color(0xFF141829)
                                )
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                        .clickable {
                            WrappedEngine.markWrappedSeenInUi(context, wrappedStats.period.id)
                            hasNewUnseenBanner = false
                            showInteractiveStoryModal = true
                        }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFa) "گزارش جدید شما آماده است!" else "NEW WRAPPED READY!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFa) "استوری «${wrappedStats.period.displayNameFa}» را تماشا کنید" else "Watch your ${wrappedStats.period.displayNameEn} Story",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Story",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // ۲. هدر استودیویی Wrapped + دکمه اجرای استوری تمام‌صفحه Spotify Wrapped
        // =========================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFa) "تحلیل هوشمند شنیداری • تقویم شمسی" else "AURA SONIC INTELLIGENCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.6.sp,
                            color = palette.accent
                        )
                    )
                    Text(
                        text = if (isFa) "خلاصه موسیقی شما (Wrapped)" else "Your Music Wrapped",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }

                if (wrappedStats != null && wrappedStats.totalTracksPlayed > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // دکمه پخش استوری تمام‌صفحه به سبک Spotify Wrapped
                        Button(
                            onClick = {
                                WrappedEngine.markWrappedSeenInUi(context, wrappedStats.period.id)
                                hasNewUnseenBanner = false
                                showInteractiveStoryModal = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "پخش استوری" else "Play Story",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = { shareRecap(wrappedStats) },
                            modifier = Modifier
                                .size(40.dp)
                                .liquidGlass(
                                    shape = CircleShape,
                                    thickness = GlassThickness.THIN,
                                    tintColor = palette.primary,
                                    tintAlpha = 0.22f
                                )
                                .testTag("wrapped_share_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // =========================================================================
        // ۳. سوییچر ۳ دسته‌بندی اصلی: هفتگی (Weekly) | ماهانه (Monthly) | سالانه (Yearly)
        // =========================================================================
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(20.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.14f
                    )
                    .padding(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf(
                        WrappedTimeframe.WEEKLY to (if (isFa) "رپد هفتگی" else "Weekly"),
                        WrappedTimeframe.MONTHLY to (if (isFa) "رپد ماهانه" else "Monthly"),
                        WrappedTimeframe.YEARLY to (if (isFa) "رپد سالانه" else "Yearly")
                    )

                    categories.forEach { (tf, title) ->
                        val isSelected = activeCategory == tf
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(15.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(listOf(palette.primary, palette.accent.copy(alpha = 0.85f)))
                                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable {
                                    activeCategory = tf
                                    periods.firstOrNull { it.timeframe == tf }?.let { onSelectPeriod(it) }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else Color(0xFFA5ABC0),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // ۴. انتخابگر بازه زمانی در دسته‌بندی انتخاب‌شده (با تاریخ شمسی در حالت فارسی)
        // =========================================================================
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredPeriods, key = { it.id }) { p ->
                    val isSelected = selectedPeriod?.id == p.id
                    Box(
                        modifier = Modifier
                            .liquidGlass(
                                shape = RoundedCornerShape(16.dp),
                                thickness = if (isSelected) GlassThickness.THICK else GlassThickness.THIN,
                                tintColor = if (isSelected) palette.accent else palette.primary,
                                tintAlpha = if (isSelected) 0.30f else 0.10f,
                                borderWidth = if (isSelected) 1.5.dp else 0.8.dp
                            )
                            .clickable { onSelectPeriod(p) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (p.timeframe) {
                                    WrappedTimeframe.WEEKLY -> Icons.Default.DateRange
                                    WrappedTimeframe.MONTHLY -> Icons.Default.CalendarMonth
                                    WrappedTimeframe.YEARLY -> Icons.Default.AutoAwesome
                                },
                                contentDescription = null,
                                tint = if (isSelected) palette.accent else Color(0xFFA0A5BA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFa) p.displayNameFa else p.displayNameEn,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }

        if (wrappedStats == null || (wrappedStats.totalTracksPlayed == 0 && wrappedStats.totalListeningTimeMs == 0L)) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .liquidGlass(
                            shape = RoundedCornerShape(26.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.16f
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = palette.accent, modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = Localization.getString("no_history_yet", lang),
                            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        )
                    }
                }
            }
        } else {
            // =========================================================================
            // ۵. کارت Hero به سبک Spotify Wrapped (مجموع دقایق، رکورد روزها و شخصیت صوتی)
            // =========================================================================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(28.dp),
                            thickness = GlassThickness.THICK,
                            tintColor = palette.primary,
                            tintAlpha = 0.25f,
                            borderWidth = 1.5.dp,
                            secondaryAccent = palette.accent
                        )
                        .padding(22.dp)
                        .testTag("wrapped_card_overview")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isFa) wrappedStats.period.subtitleFa.ifBlank { wrappedStats.period.displayNameFa }
                                    else wrappedStats.period.subtitleEn.ifBlank { wrappedStats.period.displayNameEn },
                                    color = palette.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (isFa) wrappedStats.audioPersonalityFa else wrappedStats.audioPersonalityEn,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF97316).copy(alpha = 0.22f))
                                    .border(1.dp, Color(0xFFF97316), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${formatNum(wrappedStats.listeningStreakDays)} ${Localization.getString("days", lang)}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // نمایش درشت زمان گوش دادن به سبک پوسترهای Spotify Wrapped
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = formatNum(wrappedStats.totalListeningMinutes),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontSize = 54.sp
                                    )
                                )
                                Text(
                                    text = if (isFa) "دقیقه غرق در موسیقی (${formatNum(String.format("%.1f", wrappedStats.totalListeningHours))} ساعت)"
                                    else "MINUTES LISTENED (${String.format("%.1f", wrappedStats.totalListeningHours)} HRS)",
                                    color = palette.accent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }

                        Text(
                            text = if (isFa) wrappedStats.audioPersonalityDescFa else wrappedStats.audioPersonalityDescEn,
                            color = Color(0xFFD4D8EC),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                        // ۴ شاخص کلیدی واقعی
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WrappedMiniMetric(
                                value = formatNum(wrappedStats.totalTracksPlayed),
                                label = if (isFa) "دفعات پخش" else "Total Plays",
                                color = Color.White
                            )
                            WrappedMiniMetric(
                                value = formatNum(wrappedStats.uniqueTracksCount),
                                label = if (isFa) "آهنگ‌های مجزا" else "Unique Songs",
                                color = palette.accent
                            )
                            WrappedMiniMetric(
                                value = formatNum(wrappedStats.uniqueArtistsCount),
                                label = if (isFa) "هنرمندان" else "Artists",
                                color = palette.secondary
                            )
                            WrappedMiniMetric(
                                value = "${formatNum((wrappedStats.completionRate * 100).toInt())}%",
                                label = if (isFa) "نرخ تکمیل" else "Completion",
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // ۶. کارت ستاره اول موسیقی شما (#1 Top Artist Spotlight با عکس و آمار واقعی)
            // =========================================================================
            if (wrappedStats.topArtists.isNotEmpty()) {
                val topArtist = wrappedStats.topArtists.first()
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(28.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.secondary,
                                tintAlpha = 0.22f,
                                borderWidth = 1.3.dp,
                                secondaryAccent = palette.primary
                            )
                            .padding(20.dp)
                            .testTag("wrapped_card_top_artists")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFa) "👑 هنرمند شماره ۱ شما" else "👑 YOUR #1 TOP ARTIST",
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "${formatNum(topArtist.percentage.toInt())}% ${if (isFa) "از پخش‌های شما" else "of your plays"}",
                                    color = palette.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TrackArtworkThumbnail(
                                    artworkUri = topArtist.artworkUri ?: wrappedStats.topSongs.firstOrNull()?.artworkUri,
                                    accentColor = palette.accent,
                                    size = 76.dp,
                                    shape = CircleShape,
                                    title = topArtist.name,
                                    artist = topArtist.name
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = topArtist.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 24.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isFa) "${formatNum(topArtist.count)} بار پخش ثبت‌شده در این دوره"
                                        else "${topArtist.count} plays recorded in this period",
                                        color = Color(0xFFC0C6DC),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (wrappedStats.topArtists.size > 1) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    wrappedStats.topArtists.drop(1).take(4).forEachIndexed { idx, art ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0x24000000))
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "#${formatNum(idx + 2)}",
                                                    color = palette.accent,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = art.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${formatNum(art.count)}x",
                                                    color = Color(0xFFA0A6BC),
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // ۷. ۵ آهنگ برتر با نمودار مقایسه‌ای زنده و قابلیت پخش آنی
            // =========================================================================
            if (wrappedStats.topSongs.isNotEmpty()) {
                val maxSongPlays = wrappedStats.topSongs.maxOf { it.playCount }.coerceAtLeast(1)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(28.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.primary,
                                tintAlpha = 0.18f,
                                borderWidth = 1.2.dp
                            )
                            .padding(20.dp)
                            .testTag("wrapped_card_top_songs")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Localization.getString("top_songs", lang),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    )
                                }
                                Text(
                                    text = if (isFa) "لمس برای پخش" else "Tap to Play",
                                    color = palette.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            wrappedStats.topSongs.forEachIndexed { index, song ->
                                val barFraction = (song.playCount.toFloat() / maxSongPlays.toFloat()).coerceIn(0.15f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x22000000))
                                        .clickable { onPlayTrackById(song.trackId) }
                                ) {
                                    // نوار گرافیکی تناسب تعداد پخش در پس‌زمینه آیتم
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .fillMaxWidth(barFraction)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        palette.primary.copy(alpha = if (index == 0) 0.36f else 0.18f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(if (index == 0) Color(0xFFFBBF24) else Color(0x28FFFFFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = formatNum(index + 1),
                                                color = if (index == 0) Color.Black else Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        }

                                        TrackArtworkThumbnail(
                                            artworkUri = song.artworkUri,
                                            accentColor = palette.accent,
                                            size = 48.dp,
                                            shape = RoundedCornerShape(12.dp),
                                            title = song.title,
                                            artist = song.artist,
                                            trackId = song.trackId
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.artist,
                                                color = Color(0xFFB4B9CE),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${formatNum(song.playCount)}x",
                                                color = if (index == 0) Color(0xFFFBBF24) else palette.accent,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${formatNum((song.totalDurationMs / 60000L).coerceAtLeast(1L))}m",
                                                color = Color(0xFF9096AC),
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // ۸. نمودار ۲۴ ساعته زمان گوش دادن (Listening Clock) + رادار ژانرهای واقعی
            // =========================================================================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(28.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.20f,
                            borderWidth = 1.2.dp
                        )
                        .padding(20.dp)
                        .testTag("wrapped_card_personality")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isFa) "ساعت طلایی و طیف سبک‌های شما" else "Listening Clock & Genre Spectrum",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White)
                                )
                            }
                        }

                        // نمودار گرافیکی ۲۴ ساعته (24-Hour Sonic Heatmap)
                        val maxHourVal = (wrappedStats.hourlyDistribution.maxOrNull() ?: 1).coerceAtLeast(1)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        ) {
                            val barCount = 24
                            val gap = 4.dp.toPx()
                            val barW = (size.width - gap * (barCount - 1)) / barCount
                            for (h in 0 until 24) {
                                val count = wrappedStats.hourlyDistribution.getOrElse(h) { 0 }
                                val ratio = (count.toFloat() / maxHourVal.toFloat()).coerceIn(0.12f, 1f)
                                val barH = size.height * ratio
                                val x = h * (barW + gap)
                                val y = size.height - barH
                                val isPeak = h == wrappedStats.peakHour
                                drawRoundRect(
                                    brush = if (isPeak) {
                                        Brush.verticalGradient(listOf(Color(0xFFFFD700), palette.accent))
                                    } else {
                                        Brush.verticalGradient(listOf(palette.accent.copy(alpha = 0.85f), palette.primary.copy(alpha = 0.35f)))
                                    },
                                    topLeft = Offset(x, y),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isFa) "۰۰:۰۰" else "12 AM", color = Color(0xFF9096AC), fontSize = 10.5.sp)
                            Text(
                                text = if (isFa) wrappedStats.favoriteTimeSlot.labelFa else wrappedStats.favoriteTimeSlot.labelEn,
                                color = palette.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                            Text(text = if (isFa) "۲۳:۰۰" else "11 PM", color = Color(0xFF9096AC), fontSize = 10.5.sp)
                        }

                        if (wrappedStats.topGenres.isNotEmpty()) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                            Text(
                                text = Localization.getString("top_genres", lang),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )

                            wrappedStats.topGenres.take(4).forEach { genre ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = genre.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                        Text(
                                            text = "${formatNum(genre.percentage.toInt())}% (${formatNum(genre.count)})",
                                            color = palette.accent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (genre.percentage / 100f).coerceIn(0.08f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = palette.accent,
                                        trackColor = Color.White.copy(alpha = 0.10f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { shareRecap(wrappedStats) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.getString("share_wrapped", lang),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // مودال استوری تمام‌صفحه تعاملی به سبک Spotify Wrapped
    // =========================================================================
    if (showInteractiveStoryModal && wrappedStats != null) {
        SpotifyWrappedStoryModal(
            stats = wrappedStats,
            palette = palette,
            isFa = isFa,
            onPlayTrack = { trackId ->
                showInteractiveStoryModal = false
                onPlayTrackById(trackId)
            },
            onShare = { shareRecap(wrappedStats) },
            onDismiss = { showInteractiveStoryModal = false }
        )
    }
}

@Composable
private fun WrappedMiniMetric(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(text = label, color = Color(0xFFA0A5BA), fontSize = 11.sp)
    }
}

@Composable
private fun SpotifyWrappedStoryModal(
    stats: WrappedStats,
    palette: AmbientPalette,
    isFa: Boolean,
    onPlayTrack: (String) -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentSlide by remember { mutableIntStateOf(0) }
    val totalSlides = 4

    LaunchedEffect(currentSlide) {
        delay(6500L)
        if (currentSlide < totalSlides - 1) {
            currentSlide++
        }
    }

    fun fmt(v: Any) = if (isFa) WrappedEngine.toPersianDigits(v.toString()) else v.toString()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.primary.copy(alpha = 0.55f),
                            Color(0xFF070812),
                            palette.secondary.copy(alpha = 0.45f)
                        )
                    )
                )
                .clickable {
                    if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss()
                }
                .systemBarsPadding()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // نوار پیشرفت استوری‌های بالا (مثل اینستاگرام و اسپاتیفای)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (i in 0 until totalSlides) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i <= currentSlide) Color.White else Color.White.copy(alpha = 0.28f)
                                    )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFa) "AURA WRAPPED • ${stats.period.displayNameFa}" else "AURA WRAPPED • ${stats.period.displayNameEn}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // محتوای هر اسلاید استوری
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        (fadeIn(tween(350)) + scaleIn(initialScale = 0.92f)).togetherWith(fadeOut(tween(250)))
                    },
                    label = "WrappedStorySlide"
                ) { slide ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        when (slide) {
                            0 -> {
                                Text(
                                    text = if (isFa) "امسال موسیقی توقف نداشت" else "YOUR SOUND NEVER STOPPED",
                                    color = palette.accent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = fmt(stats.totalListeningMinutes),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 76.sp
                                )
                                Text(
                                    text = if (isFa) "دقیقه گوش دادن خالص در ${fmt(stats.totalTracksPlayed)} پخش"
                                    else "Minutes of pure music across ${stats.totalTracksPlayed} plays",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            1 -> {
                                val topArtist = stats.topArtists.firstOrNull()
                                Text(
                                    text = if (isFa) "ستاره بی‌رقیب پلی‌لیست شما" else "YOUR #1 ARTIST",
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                TrackArtworkThumbnail(
                                    artworkUri = topArtist?.artworkUri ?: stats.topSongs.firstOrNull()?.artworkUri,
                                    accentColor = palette.accent,
                                    size = 150.dp,
                                    shape = CircleShape,
                                    title = topArtist?.name
                                )
                                Text(
                                    text = topArtist?.name ?: "Artist",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 34.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (isFa) "با ${fmt(topArtist?.count ?: 1)} بار پخش در صدر انتخاب‌های شما"
                                    else "${topArtist?.count ?: 1} total plays",
                                    color = palette.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }

                            2 -> {
                                val topSong = stats.topSongs.firstOrNull()
                                Text(
                                    text = if (isFa) "آهنگی که بارها روی تکرار گذاشتید" else "YOUR #1 MOST PLAYED SONG",
                                    color = palette.accent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                TrackArtworkThumbnail(
                                    artworkUri = topSong?.artworkUri,
                                    accentColor = palette.accent,
                                    size = 165.dp,
                                    shape = RoundedCornerShape(28.dp),
                                    title = topSong?.title,
                                    artist = topSong?.artist
                                )
                                Text(
                                    text = topSong?.title ?: "",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${topSong?.artist ?: ""} • ${fmt(topSong?.playCount ?: 1)}x",
                                    color = Color(0xFFD0D5E8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (topSong != null) {
                                    Button(
                                        onClick = { onPlayTrack(topSong.trackId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isFa) "پخش همین آهنگ" else "Play #1 Anthem",
                                            color = Color.Black,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            else -> {
                                Text(
                                    text = if (isFa) "شخصیت شنیداری شما" else "YOUR AUDIO PERSONALITY",
                                    color = palette.accent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isFa) stats.audioPersonalityFa else stats.audioPersonalityEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 32.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (isFa) stats.audioPersonalityDescFa else stats.audioPersonalityDescEn,
                                    color = Color(0xFFD0D6EA),
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }

                // دکمه‌های کنترل پایین استوری
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentSlide > 0) currentSlide-- }
                    ) {
                        Text(
                            text = if (isFa) "قبلی" else "Prev",
                            color = Color.White.copy(alpha = if (currentSlide > 0) 0.9f else 0.3f)
                        )
                    }

                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFa) "اشتراک‌گذاری استوری" else "Share Story",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    TextButton(
                        onClick = { if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss() }
                    ) {
                        Text(
                            text = if (currentSlide < totalSlides - 1) (if (isFa) "بعدی" else "Next") else (if (isFa) "پایان" else "Done"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
