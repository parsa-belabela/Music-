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

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, if (isFa) "اشتراک‌گذاری خلاصه Wrapped" else "Share Wrapped"))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("wrapped_screen"),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = bottomPadding + 24.dp,
            start = 18.dp,
            end = 18.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // ۱. بنر ویژه اطلاع‌رسانی آمادگی گزارش جدید (Spotify Wrapped Alert Banner)
        // =========================================================================
        if (hasNewUnseenBanner && wrappedStats != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
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
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "تماشای استوری" else "Play Story",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // ۳. تب‌بار دسته‌بندی ۳‌گانه (هفتگی، ماهانه، سالانه)
        // =========================================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(20.dp),
                        thickness = GlassThickness.THIN,
                        tintColor = palette.primary,
                        tintAlpha = 0.18f
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val categories = listOf(
                    Triple(WrappedTimeframe.WEEKLY, "هفتگی", "Weekly"),
                    Triple(WrappedTimeframe.MONTHLY, "ماهانه", "Monthly"),
                    Triple(WrappedTimeframe.YEARLY, "سالانه", "Yearly")
                )

                categories.forEach { (cat, labelFa, labelEn) ->
                    val isSelected = activeCategory == cat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) palette.primary else Color.Transparent)
                            .clickable {
                                activeCategory = cat
                                val matching = periods.firstOrNull { it.timeframe == cat }
                                if (matching != null) {
                                    onSelectPeriod(matching)
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFa) labelFa else labelEn,
                            color = if (isSelected) Color.White else Color(0xFF9096AC),
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // =========================================================================
        // ۴. انتخابگر افقی دوره‌های زمانی متناسب با دسته‌بندی انتخاب‌شده
        // =========================================================================
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredPeriods) { period ->
                    val isSelected = selectedPeriod?.id == period.id
                    val titleText = if (isFa) period.displayNameFa else period.displayNameEn
                    val subtitleText = if (isFa) period.subtitleFa else period.subtitleEn

                    Surface(
                        selected = isSelected,
                        onClick = { onSelectPeriod(period) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) palette.primary.copy(alpha = 0.35f) else Color(0x12FFFFFF),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) palette.accent else Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.width(220.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                if (period.isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color(0xFFFFB703),
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Unlocked",
                                        tint = palette.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (period.isLocked) Color(0xFFFFC6FF) else Color(0xFFB0B6CE),
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // ۵. نمایش محتوای آمار در صورت قفل بودن یا باز بودن دوره انتخاب‌شده
        // =========================================================================
        if (selectedPeriod != null && selectedPeriod.isLocked) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(28.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = Color(0xFF2A1B4E),
                            tintAlpha = 0.35f,
                            borderWidth = 1.2.dp
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38235E))
                                .border(1.5.dp, Color(0xFFFFB703), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFFB703),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = if (isFa) "گزارش «${selectedPeriod.displayNameFa}» هنوز قفل است" else "${selectedPeriod.displayNameEn} is Locked",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isFa) selectedPeriod.unlockTargetDateFa else selectedPeriod.unlockTargetDateEn,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFD3D8EC),
                                fontSize = 13.5.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            color = Color(0x33FFB703),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isFa) "💡 با شنیدن موزیک در طول این دوره، آمار شما دقیق‌تر محاسبه می‌شود." else "💡 Keep listening! Your stats are being collected in real time.",
                                color = Color(0xFFFFE3A8),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        } else if (wrappedStats != null) {
            // =========================================================================
            // ۶. آمار کامل و بازشده دوره انتخاب‌شده
            // =========================================================================
            item {
                val stats = wrappedStats
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(28.dp),
                            thickness = GlassThickness.THICK,
                            tintColor = palette.primary,
                            tintAlpha = 0.22f,
                            borderWidth = 1.2.dp
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isFa) stats.audioPersonalityFa else stats.audioPersonalityEn,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = if (isFa) stats.audioPersonalityDescFa else stats.audioPersonalityDescEn,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFBCC2D8),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            WrappedMiniMetric(
                                value = formatNum(stats.totalListeningMinutes),
                                label = Localization.getString("minutes", lang),
                                color = palette.accent
                            )
                            WrappedMiniMetric(
                                value = formatNum(stats.totalTracksPlayed),
                                label = Localization.getString("tracks", lang),
                                color = Color.White
                            )
                            WrappedMiniMetric(
                                value = formatNum(stats.listeningStreakDays),
                                label = Localization.getString("streak_days", lang),
                                color = Color(0xFFFFB703)
                            )
                        }

                        if (stats.topSongs.isNotEmpty()) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                            Text(
                                text = Localization.getString("top_songs", lang),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )

                            for ((idx, song) in stats.topSongs.take(5).withIndex()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onPlayTrackById(song.trackId) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${formatNum(idx + 1)}",
                                        color = palette.accent,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(30.dp)
                                    )
                                    TrackArtworkThumbnail(
                                        artworkUri = song.artworkUri,
                                        accentColor = palette.accent,
                                        size = 42.dp,
                                        shape = RoundedCornerShape(10.dp),
                                        title = song.title
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            color = Color(0xFF9EA4BA),
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${formatNum(song.playCount)}x",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // ۲۴ ساعته (24-Hour Heatmap)
                        val maxHourVal = (stats.hourlyDistribution.maxOrNull() ?: 1).coerceAtLeast(1)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                        ) {
                            val barCount = 24
                            val gap = 4.dp.toPx()
                            val barW = (size.width - gap * (barCount - 1)) / barCount
                            for (h in 0 until 24) {
                                val count = stats.hourlyDistribution.getOrElse(h) { 0 }
                                val ratio = (count.toFloat() / maxHourVal.toFloat()).coerceIn(0.12f, 1f)
                                val barH = size.height * ratio
                                val x = h * (barW + gap)
                                val y = size.height - barH
                                val isPeak = h == stats.peakHour
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
                                text = if (isFa) stats.favoriteTimeSlot.labelFa else stats.favoriteTimeSlot.labelEn,
                                color = palette.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                            Text(text = if (isFa) "۲۳:۰۰" else "11 PM", color = Color(0xFF9096AC), fontSize = 10.5.sp)
                        }

                        if (stats.topGenres.isNotEmpty()) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                            Text(
                                text = Localization.getString("top_genres", lang),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )

                            for (genre in stats.topGenres.take(4)) {
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
                            onClick = { shareRecap(stats) },
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

/**
 * استوری تمام‌صفحه ۱۰ اسلایدی به سبک Spotify Wrapped:
 * - پس‌زمینه ۱۰۰٪ مات و عمیق (Opaque Deep Obsidian + Dynamic Aurora) که صفحات پشت آن هرگز دیده نمی‌شوند
 * - دارای ۱۰ اسلاید واقعی و تحلیلی از آمار شنیداری کاربر
 * - پخش خودکار اوج آهنگ (Peak/Chorus Hook) همزمان با نمایش استوری‌ها از طریق موتور صوتی برنامه
 * - قابلیت نگه داشتن انگشت برای توقف موقت استوری (Tap & Hold) و اشتراک‌گذاری مستقیم برای استوری اینستاگرام
 */
@Composable
private fun SpotifyWrappedStoryModal(
    stats: WrappedStats,
    palette: AmbientPalette,
    isFa: Boolean,
    onPlayTrack: (String) -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val totalSlides = 10
    var currentSlide by remember { mutableIntStateOf(0) }
    var slideProgress by remember { mutableFloatStateOf(0f) }
    var isPausedByTouch by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    val app = remember { try { com.example.AuraApplication.instance } catch (_: Exception) { null } }
    val allTracks by (app?.repository?.allTracks ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    fun fmt(v: Any): String = if (isFa) WrappedEngine.toPersianDigits(v.toString()) else v.toString()

    // پیدا کردن بهترین قسمت آهنگ (اوج انرژی از روی پروفایل صوتی یا ثانیه ۴۵/وسط آهنگ) و پخش همزمان با اسلایدها
    LaunchedEffect(currentSlide, isMuted) {
        if (isMuted || app == null) return@LaunchedEffect

        val targetTrackId: String? = when (currentSlide) {
            0, 1, 2 -> stats.topSongs.getOrNull(0)?.trackId
            3 -> {
                val topArtistName = stats.topArtists.firstOrNull()?.name
                allTracks.firstOrNull { it.artist.equals(topArtistName, ignoreCase = true) }?.id
                    ?: stats.topSongs.getOrNull(0)?.trackId
            }
            4 -> stats.topSongs.getOrNull(1)?.trackId ?: stats.topSongs.getOrNull(0)?.trackId
            5, 6, 7, 8, 9 -> stats.topSongs.getOrNull(0)?.trackId
            else -> null
        }

        val trackToPlay = allTracks.firstOrNull { it.id == targetTrackId }
            ?: allTracks.firstOrNull { it.playCount > 0 }

        if (trackToPlay != null) {
            val currentEngineTrackId = app.audioEngine.currentTrackId
            val isAlreadyPlayingSame = currentEngineTrackId == trackToPlay.id &&
                    app.audioEngine.status.value == com.example.data.model.PlayerStatus.PLAYING

            if (!isAlreadyPlayingSame || currentSlide == 2 || currentSlide == 4) {
                // استخراج نقطه اوج آهنگ (Peak Chorus) از روی پروفایل موج صوتی
                val profile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    app.repository.getAudioProfile(trackToPlay.id)
                }
                val hookStartMs = computeBestHighlightPositionMs(trackToPlay.durationMs, profile?.waveformEnvelope)
                app.audioEngine.playTrack(trackToPlay, startPositionMs = hookStartMs, autoPlay = true)
            }
        }
    }

    // تایمر نرم ۶۰ فریم بر ثانیه برای جلو بردن نوار استوری بالا
    LaunchedEffect(currentSlide, isPausedByTouch) {
        slideProgress = 0f
        val durationMs = 7000L
        val stepMs = 16L
        var elapsed = 0L
        while (elapsed < durationMs) {
            if (!isPausedByTouch) {
                elapsed += stepMs
                slideProgress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            }
            delay(stepMs)
        }
        if (currentSlide < totalSlides - 1) {
            currentSlide++
        } else {
            onDismiss()
        }
    }

    // پالت رنگی پویا و اختصاصی برای هر کدام از ۱۰ اسلاید تا هر استوری حس و حال منحصربه‌فرد خودش را داشته باشد
    val slideColors = remember(currentSlide, palette) {
        when (currentSlide) {
            0 -> listOf(palette.primary, Color(0xFF090A14), palette.accent)
            1 -> listOf(Color(0xFF7B2CBF), Color(0xFF070812), Color(0xFF00F5D4))
            2 -> listOf(Color(0xFFFF1E56), Color(0xFF090612), Color(0xFFFFBE0B))
            3 -> listOf(Color(0xFF3A86FF), Color(0xFF060914), Color(0xFFFF006E))
            4 -> listOf(Color(0xFF06D6A0), Color(0xFF050B0E), Color(0xFF8338EC))
            5 -> listOf(Color(0xFFF72585), Color(0xFF080510), Color(0xFF4CC9F0))
            6 -> listOf(Color(0xFFFF9E00), Color(0xFF0B0705), Color(0xFFFF0054))
            7 -> listOf(Color(0xFF00BBF9), Color(0xFF050811), Color(0xFF9B5DE5))
            8 -> listOf(palette.accent, Color(0xFF070812), palette.primary)
            else -> listOf(Color(0xFF9D4EDD), Color(0xFF05060D), Color(0xFF00E5FF))
        }
    }

    val animColorTop by animateColorAsState(slideColors[0], tween(700), label = "sTop")
    val animColorMid by animateColorAsState(slideColors[1], tween(700), label = "sMid")
    val animColorBot by animateColorAsState(slideColors[2], tween(700), label = "sBot")

    val infiniteTransition = rememberInfiniteTransition(label = "StoryAurora")
    val orbAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Restart),
        label = "orbAngle"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // لایه پایه ۱۰۰٪ مات (#FF04050B) که هیچ نوری یا محتوایی از صفحه پشت استوری را عبور نمی‌دهد
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF04050B))
        ) {
            // گرافیک متحرک شفق قطبی و گوی‌های بلر شده مخصوص استوری
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val rad = Math.toRadians(orbAngle.toDouble())
                val ox1 = w * 0.5f + (kotlin.math.cos(rad).toFloat() * w * 0.28f)
                val oy1 = h * 0.28f + (kotlin.math.sin(rad).toFloat() * h * 0.14f)

                val ox2 = w * 0.5f - (kotlin.math.sin(rad).toFloat() * w * 0.26f)
                val oy2 = h * 0.72f - (kotlin.math.cos(rad).toFloat() * h * 0.14f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animColorTop.copy(alpha = 0.65f), Color.Transparent),
                        center = Offset(ox1, oy1),
                        radius = w * 0.85f * pulseScale
                    ),
                    radius = w * 0.85f * pulseScale,
                    center = Offset(ox1, oy1)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(animColorBot.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(ox2, oy2),
                        radius = w * 0.80f * pulseScale
                    ),
                    radius = w * 0.80f * pulseScale,
                    center = Offset(ox2, oy2)
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF04050B).copy(alpha = 0.35f),
                            animColorMid.copy(alpha = 0.45f),
                            Color(0xFF04050B).copy(alpha = 0.85f)
                        )
                    )
                )
            }

            // لایه لمسی چپ و راست استوری (مشابه اینستاگرام و اسپاتیفای)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentSlide > 0) currentSlide--
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss()
                        }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // =========================================================================
                // هدر استوری: ۱۰ نوار پیشرفت + نشانگر آهنگ در حال پخش در پس‌زمینه استوری
                // =========================================================================
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 0 until totalSlides) {
                            val fill = when {
                                i < currentSlide -> 1f
                                i == currentSlide -> slideProgress
                                else -> 0f
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.24f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fill)
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(animColorTop.copy(alpha = 0.35f))
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${fmt(currentSlide + 1)} / ${fmt(totalSlides)}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    if (isMuted) app?.audioEngine?.pause() else app?.audioEngine?.play()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
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
                }

                // =========================================================================
                // بدنه اصلی ۱۰ اسلاید متنوع، واقعی و گرافیکی
                // =========================================================================
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        (fadeIn(tween(400)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400)))
                            .togetherWith(fadeOut(tween(250)))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "StorySlideContent"
                ) { slideIndex ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (slideIndex) {
                            // اسلاید ۱: خوش‌آمدگویی و شروع سفر صوتی
                            0 -> StorySlideCard(borderColor = animColorTop) {
                                Text(
                                    text = if (isFa) "✨ داستان شنیداری شما" else "✨ YOUR SONIC STORY",
                                    color = animColorBot,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 34.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isFa)
                                        "در این دوره، شما با ${fmt(stats.uniqueTracksCount)} آهنگ مختلف و ${fmt(stats.uniqueArtistsCount)} هنرمند خاطره ساختید. بیایید مرور کنیم!"
                                    else
                                        "You explored ${stats.uniqueTracksCount} unique songs across ${stats.uniqueArtistsCount} artists. Let's dive into your sound.",
                                    color = Color(0xFFD8DEEF),
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // اسلاید ۲: مجموع دقایق و ساعت‌های گوش دادن
                            1 -> StorySlideCard(borderColor = animColorBot) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = animColorBot,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isFa) "مجموع زمان غرق شدن در موسیقی" else "TOTAL LISTENING TIME",
                                    color = animColorBot,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = fmt(stats.totalListeningMinutes),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 72.sp
                                )
                                Text(
                                    text = if (isFa) "دقیقه (${fmt(String.format("%.1f", stats.totalListeningHours))} ساعت کامل)"
                                    else "Minutes (${String.format("%.1f", stats.totalListeningHours)} Hours)",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isFa) "در مجموع ${fmt(stats.totalTracksPlayed)} بار پخش کامل به ثبت رسید."
                                    else "Across ${stats.totalTracksPlayed} total track plays.",
                                    color = Color(0xFFB8C0D8),
                                    fontSize = 13.5.sp
                                )
                            }

                            // اسلاید ۳: آهنگ شماره ۱ (همراه با پخش اوج همان آهنگ در پس‌زمینه!)
                            2 -> {
                                val topSong = stats.topSongs.firstOrNull()
                                StorySlideCard(borderColor = Color(0xFFFFBE0B)) {
                                    Text(
                                        text = if (isFa) "🔥 آهنگی که بیش از همه گوش دادید" else "🔥 YOUR #1 MOST PLAYED SONG",
                                        color = Color(0xFFFFBE0B),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TrackArtworkThumbnail(
                                        artworkUri = topSong?.artworkUri,
                                        accentColor = Color(0xFFFFBE0B),
                                        size = 175.dp,
                                        shape = RoundedCornerShape(28.dp),
                                        title = topSong?.title,
                                        artist = topSong?.artist
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = topSong?.title ?: "-",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 26.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = topSong?.artist ?: "",
                                        color = Color(0xFFD0D6EA),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        color = Color(0xFFFFBE0B).copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFBE0B))
                                    ) {
                                        Text(
                                            text = if (isFa) "🎵 در حال پخش قسمت محبوب شما • ${fmt(topSong?.playCount ?: 1)} بار پخش"
                                            else "🎵 Playing Highlight Hook • ${topSong?.playCount ?: 1} Plays",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            // اسلاید ۴: هنرمند شماره ۱ شما
                            3 -> {
                                val topArtist = stats.topArtists.firstOrNull()
                                StorySlideCard(borderColor = Color(0xFF3A86FF)) {
                                    Text(
                                        text = if (isFa) "👑 ستاره اول پلی‌لیست شما" else "👑 YOUR #1 TOP ARTIST",
                                        color = Color(0xFF4CC9F0),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TrackArtworkThumbnail(
                                        artworkUri = topArtist?.artworkUri ?: stats.topSongs.firstOrNull()?.artworkUri,
                                        accentColor = Color(0xFF3A86FF),
                                        size = 160.dp,
                                        shape = CircleShape,
                                        title = topArtist?.name
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = topArtist?.name ?: "-",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 30.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isFa)
                                            "${fmt(topArtist?.count ?: 1)} بار پخش (${fmt(topArtist?.percentage?.toInt() ?: 100)}% از کل شنیده‌های شما)"
                                        else
                                            "${topArtist?.count ?: 1} plays (${topArtist?.percentage?.toInt() ?: 100}% of your listening)",
                                        color = Color(0xFFD0D6EA),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // اسلاید ۵: ۵ آهنگ برتر (Top 5 Countdown)
                            4 -> StorySlideCard(borderColor = Color(0xFF06D6A0)) {
                                Text(
                                    text = if (isFa) "🏆 ۵ آهنگ برتر شما در این دوره" else "🏆 YOUR TOP 5 ANTHEMS",
                                    color = Color(0xFF06D6A0),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    for ((idx, s) in stats.topSongs.take(5).withIndex()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${fmt(idx + 1)}",
                                                color = if (idx == 0) Color(0xFFFFD700) else Color(0xFF06D6A0),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                modifier = Modifier.width(32.dp)
                                            )
                                            TrackArtworkThumbnail(
                                                artworkUri = s.artworkUri,
                                                accentColor = palette.accent,
                                                size = 40.dp,
                                                shape = RoundedCornerShape(10.dp),
                                                title = s.title
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = s.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = s.artist,
                                                    color = Color(0xFFB4B9CE),
                                                    fontSize = 11.5.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "${fmt(s.playCount)}x",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // اسلاید ۶: هنرمندان منتخب (Top Artists Roster)
                            5 -> StorySlideCard(borderColor = Color(0xFFF72585)) {
                                Text(
                                    text = if (isFa) "🎤 جمع ستارگان موسیقی شما" else "🎤 YOUR TOP ARTISTS ROSTER",
                                    color = Color(0xFFF72585),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    for ((idx, art) in stats.topArtists.take(5).withIndex()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "#${fmt(idx + 1)}",
                                                    color = Color(0xFF4CC9F0),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    modifier = Modifier.width(34.dp)
                                                )
                                                Text(
                                                    text = art.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp
                                                )
                                            }
                                            Text(
                                                text = "${fmt(art.count)} ${if (isFa) "پخش" else "plays"}",
                                                color = Color(0xFFF72585),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // اسلاید ۷: قطب‌نمای سبک‌ها و ژانرها (Top Genres)
                            6 -> StorySlideCard(borderColor = Color(0xFFFF9E00)) {
                                Text(
                                    text = if (isFa) "🧬 دی‌ان‌ای و سبک موسیقی شما" else "🧬 YOUR GENRE DNA",
                                    color = Color(0xFFFF9E00),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                val topGenre = stats.topGenres.firstOrNull()?.name ?: "Electronic"
                                Text(
                                    text = topGenre,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    for (g in stats.topGenres.take(4)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(g.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("${fmt(g.percentage.toInt())}%", color = Color(0xFFFF9E00), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            }
                                            LinearProgressIndicator(
                                                progress = { (g.percentage / 100f).coerceIn(0.1f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = Color(0xFFFF9E00),
                                                trackColor = Color.White.copy(alpha = 0.12f)
                                            )
                                        }
                                    }
                                }
                            }

                            // اسلاید ۸: ساعت طلایی گوش دادن (Listening Clock)
                            7 -> StorySlideCard(borderColor = Color(0xFF00BBF9)) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF00BBF9),
                                    modifier = Modifier.size(46.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isFa) "⏰ ساعت طلایی شنیدن شما" else "⏰ YOUR PRIME LISTENING HOUR",
                                    color = Color(0xFF00BBF9),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "${fmt(String.format("%02d:00", stats.peakHour))}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 54.sp
                                )
                                Text(
                                    text = if (isFa) stats.favoriteTimeSlot.labelFa else stats.favoriteTimeSlot.labelEn,
                                    color = Color(0xFF9B5DE5),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isFa) "لقب زمانی شما: ${stats.favoriteTimeSlot.personaFa}"
                                    else "Time Persona: ${stats.favoriteTimeSlot.personaEn}",
                                    color = Color(0xFFD0D6EA),
                                    fontSize = 14.sp
                                )
                            }

                            // اسلاید ۹: رکورد وفاداری، روزهای متوالی و نرخ تکمیل آهنگ‌ها
                            8 -> StorySlideCard(borderColor = palette.accent) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isFa) "🔥 وفاداری و تمرکز شنیداری" else "🔥 STREAK & LOYALTY",
                                    color = Color(0xFFF97316),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${fmt(stats.listeningStreakDays)} ${if (isFa) "روز متوالی" else "Days Streak"}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 40.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isFa)
                                        "شما ${fmt((stats.completionRate * 100).toInt())}% از آهنگ‌ها را بدون رد کردن (Skip) تا انتها گوش دادید!"
                                    else
                                        "You completed ${(stats.completionRate * 100).toInt()}% of your tracks without skipping!",
                                    color = Color(0xFFD8DEEF),
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }

                            // اسلاید ۱۰: شخصیت صوتی نهایی و کارت پوستر آماده اشتراک‌گذاری در استوری اینستاگرام
                            else -> StorySlideCard(borderColor = Color(0xFF00E5FF)) {
                                Text(
                                    text = if (isFa) "🌌 شخصیت صوتی سال شما" else "🌌 YOUR AUDIO ARCHETYPE",
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isFa) stats.audioPersonalityFa else stats.audioPersonalityEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isFa) stats.audioPersonalityDescFa else stats.audioPersonalityDescEn,
                                    color = Color(0xFFD4D8EC),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 21.sp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = onShare,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isFa) "اشتراک‌گذاری پوستر در اینستاگرام" else "Share Story Poster",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // فوتر استوری: کنترل اسلاید قبلی/بعدی و دکمه اشتراک‌گذاری سریع
                // =========================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { if (currentSlide > 0) currentSlide-- },
                        enabled = currentSlide > 0
                    ) {
                        Text(
                            text = if (isFa) "اسلاید قبلی" else "Previous",
                            color = Color.White.copy(alpha = if (currentSlide > 0) 0.9f else 0.3f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onShare,
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFa) "استوری کن" else "Share",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }

                    TextButton(
                        onClick = { if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss() }
                    ) {
                        Text(
                            text = if (currentSlide < totalSlides - 1) (if (isFa) "اسلاید بعدی" else "Next") else (if (isFa) "پایان" else "Finish"),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorySlideCard(
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(28.dp, RoundedCornerShape(32.dp), spotColor = borderColor)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF15182B).copy(alpha = 0.94f),
                        Color(0xFF0A0C16).copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.6.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.85f),
                        borderColor,
                        borderColor.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * پیدا کردن پرانرژی‌ترین و محبوب‌ترین بخش آهنگ (Chorus / Peak Hook) از روی پروفایل موج صوتی
 * و در صورت نبود پروفایل، انتخاب هوشمند از یک‌سوم میانی آهنگ (مشابه پیش‌نمایش Spotify Wrapped)
 */
private fun computeBestHighlightPositionMs(durationMs: Long, waveformEnvelope: String?): Long {
    if (durationMs <= 15000L) return 0L
    if (!waveformEnvelope.isNullOrBlank()) {
        val points = waveformEnvelope.split(",").mapNotNull { it.trim().toFloatOrNull() }
        if (points.size >= 20) {
            val startIdx = (points.size * 0.20f).toInt()
            val endIdx = (points.size * 0.75f).toInt().coerceAtLeast(startIdx + 1)
            var bestWindowIdx = startIdx
            var maxEnergySum = -1f
            val windowSize = 8.coerceAtMost(points.size - startIdx)

            for (i in startIdx..(endIdx - windowSize).coerceAtLeast(startIdx)) {
                var sum = 0f
                for (w in 0 until windowSize) {
                    sum += points[i + w]
                }
                if (sum > maxEnergySum) {
                    maxEnergySum = sum
                    bestWindowIdx = i
                }
            }
            val ratio = bestWindowIdx.toFloat() / points.size.toFloat()
            return (durationMs * ratio).toLong().coerceIn(10000L, (durationMs - 15000L).coerceAtLeast(10000L))
        }
    }
    // اگر پروفایل موج صوتی هنوز ساخته نشده بود، از ۳۵٪ زمان آهنگ (محل استاندارد شروع کورس/ترجیع‌بند) پخش می‌کند
    return (durationMs * 0.35f).toLong().coerceIn(10000L, (durationMs - 15000L).coerceAtLeast(10000L))
}
