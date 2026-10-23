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
 * استوری مینیمال، خلوت و فوق‌العاده شیک به سبک Apple Music Replay & Spotify Wrapped:
 * - بدون جدول‌ها و باکس‌های شلوغ و گیج‌کننده
 * - تمرکز روی یک المان بزرگ، تایپوگرافی درشت و کاور باکیفیت در هر اسلاید
 * - پخش خودکار بهترین قسمت آهنگ در پس‌زمینه
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
    val totalSlides = 6
    var currentSlide by remember { mutableIntStateOf(0) }
    var slideProgress by remember { mutableFloatStateOf(0f) }
    var isPausedByTouch by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    val app = remember { try { com.example.AuraApplication.instance } catch (_: Exception) { null } }
    val allTracks by (app?.repository?.allTracks ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    fun fmt(v: Any): String = if (isFa) WrappedEngine.toPersianDigits(v.toString()) else v.toString()

    // پخش خودکار اوج آهنگ برتر همزمان با باز شدن استوری
    LaunchedEffect(currentSlide, isMuted, allTracks.size) {
        if (isMuted || app == null || allTracks.isEmpty()) return@LaunchedEffect

        val targetTrackId = when (currentSlide) {
            2 -> stats.topSongs.getOrNull(0)?.trackId
            3 -> {
                val artistName = stats.topArtists.firstOrNull()?.name
                allTracks.firstOrNull { it.artist.equals(artistName, ignoreCase = true) }?.id
                    ?: stats.topSongs.getOrNull(0)?.trackId
            }
            else -> stats.topSongs.getOrNull(0)?.trackId
        }

        val trackToPlay = allTracks.firstOrNull { it.id == targetTrackId }
            ?: allTracks.firstOrNull { it.playCount > 0 }
            ?: allTracks.firstOrNull()

        if (trackToPlay != null) {
            val isSamePlaying = app.audioEngine.currentTrackId == trackToPlay.id &&
                    app.audioEngine.status.value == com.example.data.model.PlayerStatus.PLAYING

            if (!isSamePlaying || currentSlide == 2) {
                val profile = withContext(Dispatchers.IO) {
                    app.repository.getAudioProfile(trackToPlay.id)
                }
                val hookStartMs = computeBestHighlightPositionMs(trackToPlay.durationMs, profile?.waveformEnvelope)
                app.audioEngine.playTrack(trackToPlay, startPositionMs = hookStartMs, autoPlay = true)
            }
        }
    }

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

    val slideAccent = remember(currentSlide, palette) {
        when (currentSlide) {
            0 -> palette.primary to palette.accent
            1 -> Color(0xFF7B2CBF) to Color(0xFF00F5D4)
            2 -> Color(0xFFFF1E56) to Color(0xFFFFBE0B)
            3 -> Color(0xFF3A86FF) to Color(0xFF00E5FF)
            4 -> Color(0xFF06D6A0) to Color(0xFF8338EC)
            else -> palette.accent to palette.primary
        }
    }

    val cPrimary by animateColorAsState(slideAccent.first, tween(700), label = "storyPrimary")
    val cSecondary by animateColorAsState(slideAccent.second, tween(700), label = "storySecondary")

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
                .background(Color(0xFF05060C))
                .pointerInput(currentSlide) {
                    detectTapGestures(
                        onPress = {
                            isPausedByTouch = true
                            tryAwaitRelease()
                            isPausedByTouch = false
                        },
                        onTap = { offset ->
                            if (offset.x < size.width * 0.32f) {
                                if (currentSlide > 0) currentSlide--
                            } else {
                                if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss()
                            }
                        }
                    )
                }
        ) {
            // بک‌گراند بسیار نرم، خلوت و مخملی
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(cPrimary.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.35f),
                        radius = w * 0.85f
                    ),
                    radius = w * 0.85f,
                    center = Offset(w * 0.5f, h * 0.35f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(cSecondary.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.75f),
                        radius = w * 0.75f
                    ),
                    radius = w * 0.75f,
                    center = Offset(w * 0.5f, h * 0.75f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ۱. هدر مینیمال استوری
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
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
                        Text(
                            text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    isMuted = !isMuted
                                    if (isMuted) app?.audioEngine?.pause() else app?.audioEngine?.play()
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // ۲. کارت مرکزی استوری: خلوت، شیک و بدون جزئیات شلوغ
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        (fadeIn(tween(350)) + scaleIn(initialScale = 0.94f, animationSpec = tween(350)))
                            .togetherWith(fadeOut(tween(200)))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "MinimalStorySlide"
                ) { slide ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (slide) {
                            // اسلاید ۱: شروع ساده و شیک
                            0 -> MinimalStoryPoster(accentColor = cSecondary) {
                                Text(
                                    text = if (isFa) "خلاصه موسیقی شما" else "YOUR MUSIC RECAP",
                                    color = cSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 36.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (isFa) stats.audioPersonalityFa else stats.audioPersonalityEn,
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // اسلاید ۲: فقط عدد بزرگ دقایق گوش دادن
                            1 -> MinimalStoryPoster(accentColor = cSecondary) {
                                Text(
                                    text = if (isFa) "مجموع زمان گوش دادن" else "TIME LISTENED",
                                    color = cSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = fmt(stats.totalListeningMinutes),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 78.sp
                                )
                                Text(
                                    text = if (isFa) "دقیقه" else "Minutes",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }

                            // اسلاید ۳: آهنگ شماره ۱ شما (کاور بزرگ و تمیز)
                            2 -> {
                                val topSong = stats.topSongs.firstOrNull()
                                MinimalStoryPoster(accentColor = cSecondary) {
                                    Text(
                                        text = if (isFa) "آهنگ شماره ۱ شما" else "YOUR #1 SONG",
                                        color = cSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(22.dp))
                                    Box(
                                        modifier = Modifier
                                            .shadow(28.dp, RoundedCornerShape(28.dp), spotColor = cPrimary)
                                            .clip(RoundedCornerShape(28.dp))
                                    ) {
                                        TrackArtworkThumbnail(
                                            artworkUri = topSong?.artworkUri,
                                            accentColor = cSecondary,
                                            size = 190.dp,
                                            shape = RoundedCornerShape(28.dp),
                                            title = topSong?.title,
                                            artist = topSong?.artist
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(22.dp))
                                    Text(
                                        text = topSong?.title ?: "-",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 26.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = topSong?.artist ?: "",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isFa) "${fmt(topSong?.playCount ?: 1)} بار پخش" else "${topSong?.playCount ?: 1} Plays",
                                        color = cSecondary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // اسلاید ۴: هنرمند شماره ۱ شما
                            3 -> {
                                val topArtist = stats.topArtists.firstOrNull()
                                MinimalStoryPoster(accentColor = cSecondary) {
                                    Text(
                                        text = if (isFa) "هنرمند شماره ۱ شما" else "YOUR #1 ARTIST",
                                        color = cSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(22.dp))
                                    Box(
                                        modifier = Modifier
                                            .shadow(28.dp, CircleShape, spotColor = cPrimary)
                                            .clip(CircleShape)
                                    ) {
                                        TrackArtworkThumbnail(
                                            artworkUri = topArtist?.artworkUri ?: stats.topSongs.firstOrNull()?.artworkUri,
                                            accentColor = cSecondary,
                                            size = 175.dp,
                                            shape = CircleShape,
                                            title = topArtist?.name
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(22.dp))
                                    Text(
                                        text = topArtist?.name ?: "-",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 28.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (isFa) "${fmt(topArtist?.count ?: 1)} بار پخش" else "${topArtist?.count ?: 1} Plays",
                                        color = cSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            // اسلاید ۵: ۵ آهنگ برتر با چیدمان بسیار تمیز و ساده
                            4 -> MinimalStoryPoster(accentColor = cSecondary) {
                                Text(
                                    text = if (isFa) "آهنگ‌های برتر شما" else "YOUR TOP SONGS",
                                    color = cSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    stats.topSongs.take(5).forEachIndexed { idx, s ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = fmt(idx + 1),
                                                color = cSecondary,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp,
                                                modifier = Modifier.width(28.dp)
                                            )
                                            TrackArtworkThumbnail(
                                                artworkUri = s.artworkUri,
                                                accentColor = cSecondary,
                                                size = 44.dp,
                                                shape = RoundedCornerShape(10.dp),
                                                title = s.title
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = s.title,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = s.artist,
                                                    color = Color.White.copy(alpha = 0.65f),
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // اسلاید ۶: کارت خلاصه پایانی شیک و خلوت
                            else -> {
                                val topSong = stats.topSongs.firstOrNull()
                                val topArtist = stats.topArtists.firstOrNull()
                                MinimalStoryPoster(accentColor = cSecondary) {
                                    TrackArtworkThumbnail(
                                        artworkUri = topSong?.artworkUri ?: topArtist?.artworkUri,
                                        accentColor = cSecondary,
                                        size = 115.dp,
                                        shape = RoundedCornerShape(24.dp),
                                        title = topSong?.title
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                        color = cSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${fmt(stats.totalListeningMinutes)} ${if (isFa) "دقیقه موسیقی" else "Minutes Listened"}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 26.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "1. ${topSong?.title ?: "-"}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = topArtist?.name ?: "",
                                        color = Color.White.copy(alpha = 0.70f),
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // ۳. کنترلر پایینی ساده، تمیز و کاملاً در دید
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentSlide > 0) currentSlide-- },
                        enabled = currentSlide > 0,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (currentSlide > 0) 0.14f else 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            tint = Color.White.copy(alpha = if (currentSlide > 0) 1f else 0.3f)
                        )
                    }

                    Button(
                        onClick = onShare,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFa) "اشتراک‌گذاری استوری" else "Share Story",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = { if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(cPrimary)
                    ) {
                        Icon(
                            imageVector = if (currentSlide < totalSlides - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                            contentDescription = "Next",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalStoryPoster(
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(32.dp),
                thickness = GlassThickness.REGULAR,
                tintColor = accentColor,
                tintAlpha = 0.16f,
                borderWidth = 1.2.dp
            )
            .padding(horizontal = 28.dp, vertical = 36.dp),
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

