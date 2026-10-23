package com.example.ui.screens

import android.content.Intent
import android.graphics.BlurMaskFilter
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

    val filteredPeriods = remember(periods, activeCategory) {
        periods.filter { it.timeframe == activeCategory }.ifEmpty { periods }
    }

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
            for ((idx, s) in stats.topSongs.take(5).withIndex()) {
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
                            Column(modifier = Modifier.weight(1f)) {
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
                        }
                    }
                }
            }

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

                            for ((index, song) in wrappedStats.topSongs.withIndex()) {
                                val barFraction = (song.playCount.toFloat() / maxSongPlays.toFloat()).coerceIn(0.15f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x22000000))
                                        .clickable { onPlayTrackById(song.trackId) }
                                ) {
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

                            for (genre in wrappedStats.topGenres.take(4)) {
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
 * استوری تمام‌صفحه فوق‌پریمیوم ۱۰ اسلایدی به سبک Spotify Wrapped & Apple Music Replay:
 * - دارای داک کنترل شیشه‌ای شناور در پایین صفحه با فاصله امن تضمینی (هرگز زیر دکمه‌های گوشی نمی‌رود)
 * - بک‌گراند چندلایه شامل ذرات نوری معلق (Bokeh Particles)، پرتوهای منشوری چرخان و هاله‌های نئونی بلر شده
 * - پخش خودکار اوج آهنگ (Peak Chorus Hook) همزمان با نمایش استوری‌ها به همراه مینی‌پلیر زنده در هدر
 * - طراحی اختصاصی، پرجزئیات و متفاوت برای تک‌تک ۱۰ اسلاید (صفحه گرامافون چرخان، حلقه‌های نئونی، مدال‌های سه‌بعدی و کارت کلکسیونی)
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
    val totalSlides = 10
    var currentSlide by remember { mutableIntStateOf(0) }
    var slideProgress by remember { mutableFloatStateOf(0f) }
    var isPausedByTouch by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentlyPlayingStoryTrackTitle by remember { mutableStateOf("") }

    val app = remember { try { com.example.AuraApplication.instance } catch (_: Exception) { null } }
    val allTracks by (app?.repository?.allTracks ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    fun fmt(v: Any): String = if (isFa) WrappedEngine.toPersianDigits(v.toString()) else v.toString()

    // پخش خودکار بهترین قسمت آهنگ متناسب با هر اسلاید
    LaunchedEffect(currentSlide, isMuted, allTracks.size) {
        if (isMuted || app == null || allTracks.isEmpty()) return@LaunchedEffect

        val targetTrackId: String? = when (currentSlide) {
            0, 1, 2 -> stats.topSongs.getOrNull(0)?.trackId
            3 -> {
                val topArtistName = stats.topArtists.firstOrNull()?.name
                allTracks.firstOrNull { it.artist.equals(topArtistName, ignoreCase = true) }?.id
                    ?: stats.topSongs.getOrNull(0)?.trackId
            }
            4 -> stats.topSongs.getOrNull(1)?.trackId ?: stats.topSongs.getOrNull(0)?.trackId
            5 -> stats.topSongs.getOrNull(2)?.trackId ?: stats.topSongs.getOrNull(0)?.trackId
            else -> stats.topSongs.getOrNull(0)?.trackId
        }

        val trackToPlay = allTracks.firstOrNull { it.id == targetTrackId }
            ?: allTracks.firstOrNull { it.playCount > 0 }
            ?: allTracks.firstOrNull()

        if (trackToPlay != null) {
            currentlyPlayingStoryTrackTitle = "${trackToPlay.title} • ${trackToPlay.artist}"
            val currentEngineTrackId = app.audioEngine.currentTrackId
            val isAlreadyPlayingSame = currentEngineTrackId == trackToPlay.id &&
                    app.audioEngine.status.value == com.example.data.model.PlayerStatus.PLAYING

            if (!isAlreadyPlayingSame || currentSlide in listOf(2, 3, 4)) {
                val profile = withContext(Dispatchers.IO) {
                    app.repository.getAudioProfile(trackToPlay.id)
                }
                val hookStartMs = computeBestHighlightPositionMs(trackToPlay.durationMs, profile?.waveformEnvelope)
                app.audioEngine.playTrack(trackToPlay, startPositionMs = hookStartMs, autoPlay = true)
            }
        }
    }

    // تایمر پیش‌برنده اسلایدها (۸ ثانیه برای هر اسلاید با قابلیت نگه داشتن انگشت برای توقف)
    LaunchedEffect(currentSlide, isPausedByTouch) {
        slideProgress = 0f
        val durationMs = 8000L
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

    // تم‌های نوری منحصربه‌فرد برای هر ۱۰ اسلاید
    val slideColors = remember(currentSlide, palette) {
        when (currentSlide) {
            0 -> Triple(palette.primary, Color(0xFF00F5D4), Color(0xFFFF2A85))
            1 -> Triple(Color(0xFF7B2CBF), Color(0xFF00E5FF), Color(0xFFFFBE0B))
            2 -> Triple(Color(0xFFFF1E56), Color(0xFFFFBE0B), Color(0xFF7209B7))
            3 -> Triple(Color(0xFF3A86FF), Color(0xFF00F5D4), Color(0xFFFF006E))
            4 -> Triple(Color(0xFF06D6A0), Color(0xFF118AB2), Color(0xFFFFD166))
            5 -> Triple(Color(0xFFF72585), Color(0xFF7209B7), Color(0xFF4CC9F0))
            6 -> Triple(Color(0xFFFF9E00), Color(0xFFFF0054), Color(0xFF00F5D4))
            7 -> Triple(Color(0xFF00BBF9), Color(0xFF9B5DE5), Color(0xFFF15BB5))
            8 -> Triple(Color(0xFFFF5400), Color(0xFFFFBD00), Color(0xFF8338EC))
            else -> Triple(palette.accent, palette.primary, Color(0xFF00F5D4))
        }
    }

    val cPrimary by animateColorAsState(slideColors.first, tween(650), label = "cPrimary")
    val cSecondary by animateColorAsState(slideColors.second, tween(650), label = "cSecondary")
    val cTertiary by animateColorAsState(slideColors.third, tween(650), label = "cTertiary")

    val infiniteTransition = rememberInfiniteTransition(label = "StoryMasterAnim")
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), AnimRepeatMode.Restart),
        label = "rotationDeg"
    )
    val vinylSpinDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5500, easing = LinearEasing), AnimRepeatMode.Restart),
        label = "vinylSpinDeg"
    )
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), AnimRepeatMode.Reverse),
        label = "pulseFactor"
    )

    // استفاده از decorFitsSystemWindows = true به همراه پدینگ امن برای اینکه دکمه‌های پایین هرگز زیر نوار گوشی نروند
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF04050B))
                .pointerInput(currentSlide) {
                    detectTapGestures(
                        onPress = {
                            isPausedByTouch = true
                            tryAwaitRelease()
                            isPausedByTouch = false
                        },
                        onTap = { offset ->
                            val w = size.width
                            if (offset.x < w * 0.32f) {
                                if (currentSlide > 0) currentSlide--
                            } else {
                                if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss()
                            }
                        }
                    )
                }
        ) {
            // =========================================================================
            // ۱. پس‌زمینه سینمایی چندلایه: پرتوهای منشوری + هاله‌های بلر گوسی + ذرات معلق (Bokeh)
            // =========================================================================
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val rad = Math.toRadians(rotationDeg.toDouble())

                // پرتوهای نوری چرخان در پس‌زمینه
                rotate(rotationDeg * 0.5f, pivot = Offset(w * 0.5f, h * 0.38f)) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                cPrimary.copy(alpha = 0.24f),
                                Color.Transparent,
                                cSecondary.copy(alpha = 0.22f),
                                Color.Transparent,
                                cTertiary.copy(alpha = 0.20f),
                                Color.Transparent,
                                cPrimary.copy(alpha = 0.24f)
                            ),
                            center = Offset(w * 0.5f, h * 0.38f)
                        ),
                        radius = maxOf(w, h) * 0.9f,
                        center = Offset(w * 0.5f, h * 0.38f)
                    )
                }

                // گوی‌های نوری بلر شده (Real Gaussian Blur)
                drawIntoCanvas { canvas ->
                    val p1 = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            maskFilter = BlurMaskFilter(120f, BlurMaskFilter.Blur.NORMAL)
                            color = cPrimary.copy(alpha = 0.52f).toArgb()
                        }
                    }
                    val p2 = Paint().apply {
                        asFrameworkPaint().apply {
                            isAntiAlias = true
                            maskFilter = BlurMaskFilter(110f, BlurMaskFilter.Blur.NORMAL)
                            color = cSecondary.copy(alpha = 0.45f).toArgb()
                        }
                    }
                    canvas.drawCircle(
                        Offset(w * (0.3f + 0.2f * cos(rad).toFloat()), h * (0.25f + 0.1f * sin(rad).toFloat())),
                        w * 0.48f * pulseFactor,
                        p1
                    )
                    canvas.drawCircle(
                        Offset(w * (0.7f - 0.2f * sin(rad).toFloat()), h * (0.68f - 0.1f * cos(rad).toFloat())),
                        w * 0.45f * pulseFactor,
                        p2
                    )
                }

                // ۲۴ ذره نوری معلق (Floating Bokeh Particles) برای عمق و جزئیات بصری بالا
                for (i in 0 until 24) {
                    val seed = i * 37.5f
                    val px = ((sin(rad + seed) * 0.45f + 0.5f) * w).toFloat()
                    val py = ((( (rotationDeg / 360f) + (i / 24f) ) % 1f) * h)
                    val pRadius = (2.5f + (i % 4) * 2.2f).dp.toPx()
                    val pColor = when (i % 3) {
                        0 -> cPrimary
                        1 -> cSecondary
                        else -> Color.White
                    }
                    drawCircle(
                        color = pColor.copy(alpha = 0.22f + (i % 3) * 0.12f),
                        radius = pRadius,
                        center = Offset(px, h - py)
                    )
                }
            }

            // =========================================================================
            // ۲. لایه محتوای اصلی با فاصله امن از بالا و پایین گوشی
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ---------------------------------------------------------------------
                // الف) هدر استوری: نوارهای پیشرفت ۱۰گانه + کپسول شیشه‌ای نمایش آهنگ در حال پخش
                // ---------------------------------------------------------------------
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fill)
                                        .background(
                                            Brush.horizontalGradient(listOf(Color.White, cSecondary))
                                        )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // نشانگر دوره و شماره اسلاید
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(cPrimary, cSecondary))
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (currentlyPlayingStoryTrackTitle.isNotBlank() && !isMuted) {
                                    Text(
                                        text = "♪ $currentlyPlayingStoryTrackTitle",
                                        color = cSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // دکمه‌های قطع صدا و بستن استوری
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    if (isMuted) app?.audioEngine?.pause() else app?.audioEngine?.play()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .liquidGlass(
                                        shape = CircleShape,
                                        thickness = GlassThickness.THIN,
                                        tintColor = cPrimary,
                                        tintAlpha = 0.25f
                                    )
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
                                    .liquidGlass(
                                        shape = CircleShape,
                                        thickness = GlassThickness.THIN,
                                        tintColor = cPrimary,
                                        tintAlpha = 0.25f
                                    )
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

                // ---------------------------------------------------------------------
                // ب) مرکز استوری: ۱۰ اسلاید گرافیکی، پرجزئیات و مدرن
                // ---------------------------------------------------------------------
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        (fadeIn(tween(380)) + scaleIn(initialScale = 0.90f, animationSpec = spring(dampingRatio = 0.78f)))
                            .togetherWith(fadeOut(tween(220)))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    label = "RichStorySlide"
                ) { slideIndex ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (slideIndex) {
                            // =========================================================
                            // اسلاید ۱: دروازه ورود هولوگرافیک (The Sonic Portal)
                            // =========================================================
                            0 -> RichStoryCardContainer(
                                primaryColor = cPrimary,
                                secondaryColor = cSecondary,
                                stepBadge = "01 / 10 • AURA WRAPPED"
                            ) {
                                Box(
                                    modifier = Modifier.size(130.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        rotate(vinylSpinDeg) {
                                            drawCircle(
                                                brush = Brush.sweepGradient(
                                                    listOf(cPrimary, cSecondary, cTertiary, cPrimary)
                                                ),
                                                radius = size.minDimension / 2f,
                                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }
                                        drawCircle(
                                            color = cSecondary.copy(alpha = 0.20f),
                                            radius = size.minDimension * 0.36f * pulseFactor
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (isFa)
                                        "هر لحظه‌ای که پلی کردید، یک امضای صوتی منحصربه‌فرد ساخت. این داستان کامل موسیقی شماست."
                                    else
                                        "Every beat you played shaped your sonic universe. Here is your complete listening story.",
                                    color = Color(0xFFD0D6EA),
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StoryGlassStatPill(
                                        value = fmt(stats.uniqueTracksCount),
                                        label = if (isFa) "آهنگ‌ها" else "Tracks",
                                        accent = cSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StoryGlassStatPill(
                                        value = fmt(stats.uniqueArtistsCount),
                                        label = if (isFa) "هنرمندان" else "Artists",
                                        accent = cPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StoryGlassStatPill(
                                        value = fmt(stats.daysWithAura),
                                        label = if (isFa) "روز همراهی" else "Days",
                                        accent = cTertiary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // =========================================================
                            // اسلاید ۲: حلقه نئونی زمان گوش دادن (Total Sonic Minutes)
                            // =========================================================
                            1 -> RichStoryCardContainer(
                                primaryColor = cPrimary,
                                secondaryColor = cSecondary,
                                stepBadge = "02 / 10 • TIME CAPSULE"
                            ) {
                                Box(
                                    modifier = Modifier.size(165.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokeW = 10.dp.toPx()
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.10f),
                                            style = Stroke(width = strokeW)
                                        )
                                        rotate(vinylSpinDeg * 0.4f) {
                                            drawArc(
                                                brush = Brush.sweepGradient(
                                                    listOf(cSecondary, cPrimary, cTertiary, cSecondary)
                                                ),
                                                startAngle = -90f,
                                                sweepAngle = 290f,
                                                useCenter = false,
                                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = fmt(stats.totalListeningMinutes),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 40.sp
                                        )
                                        Text(
                                            text = if (isFa) "دقیقه" else "MINUTES",
                                            color = cSecondary,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (isFa)
                                        "معادل ${fmt(String.format("%.1f", stats.totalListeningHours))} ساعت غرق شدن در دنیای موسیقی"
                                    else
                                        "That's ${String.format("%.1f", stats.totalListeningHours)} hours of pure musical immersion",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StoryGlassStatPill(
                                        value = fmt(stats.totalTracksPlayed),
                                        label = if (isFa) "مجموع دفعات پخش" else "Total Streams",
                                        accent = cSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StoryGlassStatPill(
                                        value = "${fmt((stats.completionRate * 100).toInt())}%",
                                        label = if (isFa) "گوش دادن کامل" else "Completion Rate",
                                        accent = Color(0xFF10B981),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // =========================================================
                            // اسلاید ۳: آهنگ شماره ۱ با صفحه گرامافون چرخان و پخش زنده اوج آهنگ
                            // =========================================================
                            2 -> {
                                val topSong = stats.topSongs.firstOrNull()
                                RichStoryCardContainer(
                                    primaryColor = cPrimary,
                                    secondaryColor = cSecondary,
                                    stepBadge = "03 / 10 • #1 ANTHEM"
                                ) {
                                    Text(
                                        text = if (isFa) "🔥 پادشاه بی‌رقیب پخش‌های شما" else "🔥 YOUR #1 MOST PLAYED TRACK",
                                        color = Color(0xFFFFBE0B),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.5.sp,
                                        letterSpacing = 1.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // کاور آهنگ به همراه صفحه وینیل طلایی چرخان در پشت آن
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(165.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // صفحه گرامافون چرخان که از سمت راست کاور بیرون زده است
                                        Canvas(
                                            modifier = Modifier
                                                .size(145.dp)
                                                .offset(x = 34.dp)
                                        ) {
                                            rotate(vinylSpinDeg) {
                                                drawCircle(color = Color(0xFF11131D))
                                                drawCircle(
                                                    color = Color(0xFFFFBE0B).copy(alpha = 0.45f),
                                                    style = Stroke(width = 2.dp.toPx())
                                                )
                                                drawCircle(
                                                    color = Color.White.copy(alpha = 0.12f),
                                                    radius = size.minDimension * 0.38f,
                                                    style = Stroke(width = 1.5.dp.toPx())
                                                )
                                                drawCircle(
                                                    color = cPrimary,
                                                    radius = size.minDimension * 0.16f
                                                )
                                                drawCircle(
                                                    color = Color.Black,
                                                    radius = size.minDimension * 0.05f
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .offset(x = (-18).dp)
                                                .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = cPrimary)
                                                .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                                                .clip(RoundedCornerShape(24.dp))
                                        ) {
                                            TrackArtworkThumbnail(
                                                artworkUri = topSong?.artworkUri,
                                                accentColor = Color(0xFFFFBE0B),
                                                size = 152.dp,
                                                shape = RoundedCornerShape(24.dp),
                                                title = topSong?.title,
                                                artist = topSong?.artist
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = topSong?.title ?: "-",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 23.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = topSong?.artist ?: "",
                                        color = cSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StoryGlassStatPill(
                                            value = "${fmt(topSong?.playCount ?: 1)}x",
                                            label = if (isFa) "تعداد تکرار" else "Times Played",
                                            accent = Color(0xFFFFBE0B),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StoryGlassStatPill(
                                            value = "${fmt(((topSong?.totalDurationMs ?: 180000L) / 60000L).coerceAtLeast(1L))}m",
                                            label = if (isFa) "مجموع زمان پخش" else "Minutes Spent",
                                            accent = cPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // =========================================================
                            // اسلاید ۴: هنرمند شماره ۱ شما با هاله تاج طلایی
                            // =========================================================
                            3 -> {
                                val topArtist = stats.topArtists.firstOrNull()
                                RichStoryCardContainer(
                                    primaryColor = cPrimary,
                                    secondaryColor = cSecondary,
                                    stepBadge = "04 / 10 • #1 HEADLINER"
                                ) {
                                    Text(
                                        text = if (isFa) "👑 هنرمندی که بیشترین سهم را داشت" else "👑 YOUR #1 TOP ARTIST",
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Box(
                                        modifier = Modifier.size(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            rotate(-vinylSpinDeg) {
                                                drawCircle(
                                                    brush = Brush.sweepGradient(
                                                        listOf(Color(0xFFFFD700), cPrimary, cSecondary, Color(0xFFFFD700))
                                                    ),
                                                    style = Stroke(width = 4.dp.toPx())
                                                )
                                            }
                                        }
                                        TrackArtworkThumbnail(
                                            artworkUri = topArtist?.artworkUri ?: stats.topSongs.firstOrNull()?.artworkUri,
                                            accentColor = Color(0xFFFFD700),
                                            size = 132.dp,
                                            shape = CircleShape,
                                            title = topArtist?.name
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = topArtist?.name ?: "-",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 26.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StoryGlassStatPill(
                                            value = "${fmt(topArtist?.count ?: 1)}x",
                                            label = if (isFa) "دفعات پخش" else "Artist Plays",
                                            accent = Color(0xFFFFD700),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StoryGlassStatPill(
                                            value = "${fmt(topArtist?.percentage?.toInt() ?: 100)}%",
                                            label = if (isFa) "سهم از کل پخش" else "Listening Share",
                                            accent = cSecondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // =========================================================
                            // اسلاید ۵: جدول ۵ آهنگ برتر با مدال‌های طلا، نقره و برنز
                            // =========================================================
                            4 -> RichStoryCardContainer(
                                primaryColor = cPrimary,
                                secondaryColor = cSecondary,
                                stepBadge = "05 / 10 • TOP 5 SONGS"
                            ) {
                                Text(
                                    text = if (isFa) "🏆 ۵ قطعه طلایی پلی‌لیست شما" else "🏆 YOUR TOP 5 GOLDEN TRACKS",
                                    color = cSecondary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                val maxPlays = (stats.topSongs.maxOfOrNull { it.playCount } ?: 1).coerceAtLeast(1)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for ((idx, s) in stats.topSongs.take(5).withIndex()) {
                                        val medalColor = when (idx) {
                                            0 -> Color(0xFFFFD700)
                                            1 -> Color(0xFFE2E8F0)
                                            2 -> Color(0xFFF59E0B)
                                            else -> cSecondary
                                        }
                                        val frac = (s.playCount.toFloat() / maxPlays.toFloat()).coerceIn(0.2f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White.copy(alpha = 0.06f))
                                                .border(0.8.dp, medalColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .fillMaxWidth(frac)
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(medalColor.copy(alpha = 0.22f), Color.Transparent)
                                                        )
                                                    )
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(medalColor.copy(alpha = 0.25f))
                                                        .border(1.dp, medalColor, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = fmt(idx + 1),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 11.5.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                TrackArtworkThumbnail(
                                                    artworkUri = s.artworkUri,
                                                    accentColor = medalColor,
                                                    size = 38.dp,
                                                    shape = RoundedCornerShape(9.dp),
                                                    title = s.title
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = s.title,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = s.artist,
                                                        color = Color(0xFFB4B9CE),
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "${fmt(s.playCount)}x",
                                                    color = medalColor,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.5.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // =========================================================
                            // اسلاید ۶: تالار مشاهیر ۵ هنرمند برتر شما
                            // =========================================================
                            5 -> RichStoryCardContainer(
                                primaryColor = cPrimary,
                                secondaryColor = cSecondary,
                                stepBadge = "06 / 10 • HALL OF FAME"
                            ) {
                                Text(
                                    text = if (isFa) "🎤 ۵ هنرمند محبوب شما" else "🎤 YOUR TOP 5 ARTISTS",
                                    color = cTertiary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for ((idx, art) in stats.topArtists.take(5).withIndex()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White.copy(alpha = 0.07f))
                                                .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "#${fmt(idx + 1)}",
                                                    color = if (idx == 0) Color(0xFFFFD700) else cTertiary,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp,
                                                    modifier = Modifier.width(30.dp)
                                                )
                                                TrackArtworkThumbnail(
                                                    artworkUri = art.artworkUri,
                                                    accentColor = cPrimary,
                                                    size = 36.dp,
                                                    shape = CircleShape,
                                                    title = art.name
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = art.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                color = cPrimary.copy(alpha = 0.28f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "${fmt(art.count)}x • ${fmt(art.percentage.toInt())}%",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // =========================================================
                            // اسلاید ۷: دی‌ان‌ای سبک‌ها و طیف ژانرهای موسیقی شما
                            // =========================================================
                            6 -> RichStoryCardContainer(
                                primaryColor = cPrimary,
                                secondaryColor = cSecondary,
                                stepBadge = "07 / 10 • GENRE SPECTRUM"
                            ) {
                                val topGenre = stats.topGenres.firstOrNull()?.name ?: "Electronic"
                                Text(
                                    text = if (isFa) "🧬 سبک غالب موسیقی شما" else "🧬 YOUR DOMINANT SOUND",
                                    color = cPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = topGenre,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 26.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    for ((idx, g) in stats.topGenres.take(4).withIndex()) {
                                        val barCol = when (idx) {
                                            0 -> cPrimary
                                            1 -> cSecondary
                                            2 -> cTertiary
                                            else -> Color(0xFF38BDF8)
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(g.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                                Text("${fmt(g.percentage.toInt())}% (${fmt(g.count)})", color = barCol, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(9.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(Color.White.copy(alpha = 0.10f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth((g.percentage / 100f).coerceIn(0.12f, 1f))
                                                        .background(Brush.horizontalGradient(listOf(barCol, Color.White)))
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // =========================================================
                            // اسلاید ۸: ساعت زیستی و نمودار ۲۴ ساعته شنیداری شما
                            // =========================================================
                            7 -> RichStoryCardContainer(
                                primaryColor = cPrimary,
                                secondaryColor = cSecondary,
                                stepBadge = "08 / 10 • SONIC BIORHYTHM"
                            ) {
                                Text(
                                    text = if (isFa) "⏰ ساعت اوج شنیدن موسیقی" else "⏰ YOUR PEAK LISTENING HOUR",
                                    color = cPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.5.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = fmt(String.format("%02d:00", stats.peakHour)),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 46.sp
                                )
                                Text(
                                    text = if (isFa) stats.favoriteTimeSlot.personaFa else stats.favoriteTimeSlot.personaEn,
                                    color = cSecondary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                val maxH = (stats.hourlyDistribution.maxOrNull() ?: 1).coerceAtLeast(1)
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                ) {
                                    val count = 24
                                    val gap = 3.dp.toPx()
                                    val bw = (size.width - gap * (count - 1)) / count
                                    for (h in 0 until 24) {
                                        val v = stats.hourlyDistribution.getOrElse(h) { 0 }
                                        val r = (v.toFloat() / maxH.toFloat()).coerceIn(0.14f, 1f)
                                        val bh = size.height * r
                                        val isPeak = h == stats.peakHour
                                        drawRoundRect(
                                            brush = if (isPeak) {
                                                Brush.verticalGradient(listOf(Color(0xFFFFD700), cPrimary))
                                            } else {
                                                Brush.verticalGradient(listOf(cPrimary.copy(alpha = 0.8f), cSecondary.copy(alpha = 0.3f)))
                                            },
                                            topLeft = Offset(h * (bw + gap), size.height - bh),
                                            size = Size(bw, bh),
                                            cornerRadius = CornerRadius(bw / 2f, bw / 2f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isFa) stats.favoriteTimeSlot.labelFa else stats.favoriteTimeSlot.labelEn,
                                    color = Color(0xFFC0C6DC),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // =========================================================
                            // اسلاید ۹: وفاداری، رکورد روزهای متوالی و دقت انتخاب آهنگ
                            // =========================================================
                            8 -> RichStoryCardContainer(
                                primaryColor = cPrimary,
                                secondaryColor = cSecondary,
                                stepBadge = "09 / 10 • STREAK & LOYALTY"
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(listOf(cPrimary.copy(alpha = 0.45f), Color.Transparent))
                                        )
                                        .border(2.dp, cPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9E00),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "${fmt(stats.listeningStreakDays)} ${if (isFa) "روز متوالی" else "DAY STREAK"}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 34.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (isFa)
                                        "شما تقریباً هیچ آهنگی را نیمه‌کاره رها نکردید و با تمرکز کامل به پلی‌لیست خود گوش دادید."
                                    else
                                        "You rarely skipped a beat, staying locked into your favorite frequencies.",
                                    color = Color(0xFFD0D6EA),
                                    fontSize = 13.5.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StoryGlassStatPill(
                                        value = "${fmt((stats.completionRate * 100).toInt())}%",
                                        label = if (isFa) "نرخ تکمیل آهنگ" else "Completion Rate",
                                        accent = Color(0xFF10B981),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StoryGlassStatPill(
                                        value = fmt(stats.totalSkips),
                                        label = if (isFa) "رد کردن (Skip)" else "Total Skips",
                                        accent = cSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // =========================================================
                            // اسلاید ۱۰: کارت کلکسیونی نهایی (VIP Summary Poster Card)
                            // =========================================================
                            else -> RichStoryCardContainer(
                                primaryColor = cPrimary,
                                secondaryColor = cSecondary,
                                stepBadge = "10 / 10 • OFFICIAL SUMMARY CARD"
                            ) {
                                val topSong = stats.topSongs.firstOrNull()
                                val topArtist = stats.topArtists.firstOrNull()
                                val topGenre = stats.topGenres.firstOrNull()?.name ?: "Electronic"

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TrackArtworkThumbnail(
                                        artworkUri = topSong?.artworkUri ?: topArtist?.artworkUri,
                                        accentColor = cPrimary,
                                        size = 70.dp,
                                        shape = RoundedCornerShape(18.dp),
                                        title = topSong?.title
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isFa) "شخصیت شنیداری شما" else "AUDIO ARCHETYPE",
                                            color = cPrimary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.5.sp,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = if (isFa) stats.audioPersonalityFa else stats.audioPersonalityEn,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                            color = Color(0xFFB4B9CE),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // خلاصه ۴ آیتم طلایی روی کارت پایانی
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StoryGlassStatPill(
                                        value = topArtist?.name ?: "-",
                                        label = if (isFa) "هنرمند برتر" else "#1 Artist",
                                        accent = Color(0xFFFFD700),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StoryGlassStatPill(
                                        value = topSong?.title ?: "-",
                                        label = if (isFa) "آهنگ برتر" else "#1 Song",
                                        accent = cPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StoryGlassStatPill(
                                        value = "${fmt(stats.totalListeningMinutes)}m",
                                        label = if (isFa) "زمان گوش دادن" else "Time Listened",
                                        accent = cSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StoryGlassStatPill(
                                        value = topGenre,
                                        label = if (isFa) "سبک برتر" else "Top Genre",
                                        accent = cTertiary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ---------------------------------------------------------------------
                // ج) داک کنترل شیشه‌ای شناور در پایین صفحه (۱۰۰٪ بالاتر از لبه گوشی و کاملاً نمایان)
                // ---------------------------------------------------------------------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .shadow(20.dp, RoundedCornerShape(26.dp), spotColor = cPrimary)
                        .liquidGlass(
                            shape = RoundedCornerShape(26.dp),
                            thickness = GlassThickness.THICK,
                            tintColor = cPrimary,
                            tintAlpha = 0.24f,
                            borderWidth = 1.3.dp,
                            secondaryAccent = cSecondary
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // دکمه اسلاید قبلی
                        OutlinedButton(
                            onClick = { if (currentSlide > 0) currentSlide-- },
                            enabled = currentSlide > 0,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (currentSlide > 0) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = if (currentSlide > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFa) "قبلی" else "Prev",
                                color = if (currentSlide > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }

                        // دکمه مرکزی اشتراک‌گذاری کارت استوری
                        Button(
                            onClick = onShare,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "استوری کن" else "Share Story",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp
                            )
                        }

                        // دکمه اسلاید بعدی / پایان
                        Button(
                            onClick = { if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss() },
                            colors = ButtonDefaults.buttonColors(containerColor = cPrimary),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (currentSlide < totalSlides - 1) (if (isFa) "بعدی" else "Next") else (if (isFa) "پایان" else "Done"),
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (currentSlide < totalSlides - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
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
private fun RichStoryCardContainer(
    primaryColor: Color,
    secondaryColor: Color,
    stepBadge: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(32.dp, RoundedCornerShape(34.dp), spotColor = primaryColor, ambientColor = secondaryColor)
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF171B30).copy(alpha = 0.92f),
                        Color(0xFF0B0E1A).copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                width = 1.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.90f),
                        primaryColor,
                        secondaryColor,
                        Color.White.copy(alpha = 0.50f)
                    )
                ),
                shape = RoundedCornerShape(34.dp)
            )
            .padding(horizontal = 22.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // بج هولوگرافیک بالای کارت
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(primaryColor.copy(alpha = 0.20f))
                    .border(1.dp, secondaryColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stepBadge,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
private fun StoryGlassStatPill(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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
    return (durationMs * 0.35f).toLong().coerceIn(10000L, (durationMs - 15000L).coerceAtLeast(10000L))
}
