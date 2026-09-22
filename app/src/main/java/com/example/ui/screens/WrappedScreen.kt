package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.*
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization

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
    val isRtl = lang == AppLanguage.PERSIAN

    // Pulse animation for badges
    val infiniteTransition = rememberInfiniteTransition(label = "wrappedPulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    fun shareRecap(stats: WrappedStats) {
        val periodName = if (lang == AppLanguage.PERSIAN) stats.period.displayNameFa else stats.period.displayNameEn
        val shareText = buildString {
            append("✨ AURA MUSIC WRAPPED — $periodName ✨\n\n")
            append("🎧 ${Localization.getString("total_listening_time", lang)}: ${stats.totalListeningMinutes} ${Localization.getString("minutes", lang)}\n")
            append("🎵 ${Localization.getString("tracks_played", lang)}: ${stats.totalTracksPlayed}\n")
            append("🔥 ${Localization.getString("listening_streak", lang)}: ${stats.listeningStreakDays} ${Localization.getString("days", lang)}\n")
            append("🌟 ${Localization.getString("audio_personality", lang)}: ${if (lang == AppLanguage.PERSIAN) stats.audioPersonalityFa else stats.audioPersonalityEn}\n\n")
            append("🏆 ${Localization.getString("top_songs", lang)}:\n")
            stats.topSongs.take(5).forEachIndexed { idx, s ->
                append("  ${idx + 1}. ${s.title} - ${s.artist} (${s.playCount} plays)\n")
            }
            if (stats.topArtists.isNotEmpty()) {
                append("\n🎤 ${Localization.getString("top_artists", lang)}: ${stats.topArtists.take(3).joinToString { it.name }}\n")
            }
            append("\n🌌 Powered by Aura High-Fidelity Music Player")
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
        val shareIntent = Intent.createChooser(sendIntent, Localization.getString("share_wrapped", lang))
        context.startActivity(shareIntent)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = maxOf(bottomPadding + 20.dp, 120.dp)),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Screen Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Localization.getString("wrapped_title", lang),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = palette.accent
                            )
                        )
                        Text(
                            text = if (selectedPeriod?.isYearly == true) {
                                Localization.getString("yearly_wrapped", lang)
                            } else {
                                Localization.getString("monthly_wrapped", lang)
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    if (wrappedStats != null) {
                        IconButton(
                            onClick = { shareRecap(wrappedStats) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.25f))
                                .border(1.dp, palette.primary.copy(alpha = 0.5f), CircleShape)
                                .testTag("wrapped_share_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = palette.accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Localization.getString("wrapped_subtitle", lang),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B8))
                )
            }
        }

        // Timeframe / Period Filter Selector (Past Months & Years)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = Localization.getString("select_period", lang),
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF8888A0), fontWeight = FontWeight.SemiBold)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(periods) { p ->
                        val isSelected = selectedPeriod?.id == p.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectPeriod(p) },
                            label = {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) p.displayNameFa else p.displayNameEn,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (p.isYearly) {
                                { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isSelected) palette.accent else Color(0xFF9090A8)) }
                            } else null,
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

        if (selectedPeriod?.isLocked == true) {
            // Locked Period Experience with Countdown Timer
            item {
                val daysRemaining = selectedPeriod.daysRemainingUntilUnlock
                val isYearly = selectedPeriod.isYearly
                val unlockTarget = if (lang == AppLanguage.PERSIAN) selectedPeriod.unlockTargetDateFa else selectedPeriod.unlockTargetDateEn

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(28.dp),
                            thickness = GlassThickness.THICK,
                            tintColor = palette.primary,
                            tintAlpha = 0.28f,
                            borderWidth = 1.4.dp
                        )
                        .padding(26.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Lock Icon with glowing halo
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(palette.accent.copy(alpha = 0.35f), palette.primary.copy(alpha = 0.12f), Color.Transparent)
                                    )
                                )
                                .border(1.5.dp, palette.accent.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = palette.accent,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // Title
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) {
                                if (isYearly) "رپد سالانه در حال گردآوری است" else "خلاصه ماهانه در حال پردازش است"
                            } else {
                                if (isYearly) "Yearly Wrapped in Progress" else "Monthly Wrapped in Progress"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        )

                        // Countdown Timer Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = palette.accent.copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, palette.accent.copy(alpha = 0.7f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) {
                                        "$daysRemaining روز تا باز شدن قفل مانده است"
                                    } else {
                                        "$daysRemaining days remaining until unlock"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accent
                                    )
                                )
                            }
                        }

                        // Unlock target detail
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) {
                                "تاریخ انتشار: $unlockTarget\nبرای ثبت آمار واقعی و دقیق، خلاصه پس از پایان کامل دوره محاسبه و نمایش داده می‌شود."
                            } else {
                                "Target Unlock: $unlockTarget\nTo guarantee authentic playback stats, your full listening summary unlocks at the conclusion of this cycle."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFA0A0BA),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }
        } else if (wrappedStats == null || (wrappedStats.totalTracksPlayed == 0 && wrappedStats.totalListeningTimeMs == 0L)) {
            // Empty State
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                        .liquidGlass(
                            shape = RoundedCornerShape(24.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.15f
                        )
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = palette.accent, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = Localization.getString("no_history_yet", lang),
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, textAlign = TextAlign.Center)
                        )
                    }
                }
            }
        } else {
            // Pinterest Card 1: Hero Total Listening & Streak Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(26.dp),
                            thickness = GlassThickness.THICK,
                            tintColor = palette.primary,
                            tintAlpha = 0.22f,
                            borderWidth = 1.2.dp
                        )
                        .padding(22.dp)
                        .testTag("wrapped_card_overview")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                                        .background(palette.accent.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Headphones, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = Localization.getString("total_listening_time", lang),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0A0C0))
                                    )
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) wrappedStats.period.displayNameFa else wrappedStats.period.displayNameEn,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                }
                            }

                            // Days with Aura Badge
                            Surface(
                                color = Color(0x30FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = palette.secondary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${wrappedStats.daysWithAura} ${Localization.getString("days", lang)}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        // Big Stats Hero Numbers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                val minutes = wrappedStats.totalListeningMinutes
                                Text(
                                    text = if (minutes >= 60) "${"%.1f".format(wrappedStats.totalListeningHours)}" else "$minutes",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = if (minutes >= 60) Localization.getString("hours", lang) else Localization.getString("minutes", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accent
                                    )
                                )
                            }

                            // Listening Streak Box
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(palette.primary.copy(alpha = 0.4f), palette.secondary.copy(alpha = 0.25f))
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${wrappedStats.listeningStreakDays}",
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color.White)
                                        )
                                    }
                                    Text(
                                        text = Localization.getString("listening_streak", lang),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFD0D0E8), fontSize = 10.sp)
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0x20FFFFFF))

                        // Stats Summary Row (Tracks Played, Skips, Completion)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${wrappedStats.totalTracksPlayed}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                Text(Localization.getString("tracks_played", lang), style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0A0B8), fontSize = 10.sp))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${wrappedStats.totalSkips}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                Text(Localization.getString("skips_count", lang), style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0A0B8), fontSize = 10.sp))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${(wrappedStats.completionRate * 100).toInt()}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = palette.secondary))
                                Text(Localization.getString("completion_rate", lang), style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0A0B8), fontSize = 10.sp))
                            }
                        }
                    }
                }
            }

            // Pinterest Card 2: Top 5 Songs Countdown
            if (wrappedStats.topSongs.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(26.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.secondary,
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
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Localization.getString("top_songs", lang),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                }
                            }

                            wrappedStats.topSongs.forEachIndexed { index, song ->
                                val isTopOne = index == 0
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isTopOne) palette.primary.copy(alpha = 0.25f) else Color(0x10FFFFFF))
                                        .clickable { onPlayTrackById(song.trackId) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Rank Number Badge
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isTopOne) Color(0xFFFBBF24) else Color(0x30FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                color = if (isTopOne) Color.Black else Color.White
                                            )
                                        )
                                    }

                                    TrackArtworkThumbnail(
                                        artworkUri = song.artworkUri,
                                        accentColor = palette.accent,
                                        size = 44.dp,
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFA0A0B8)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Plays count badge
                                    Surface(
                                        color = if (isTopOne) palette.accent.copy(alpha = 0.3f) else Color(0x20FFFFFF),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${song.playCount} plays",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isTopOne) palette.accent else Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pinterest Card 3: Top Artists & Top Albums
            if (wrappedStats.topArtists.isNotEmpty() || wrappedStats.topAlbums.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(26.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.primary,
                                tintAlpha = 0.18f,
                                borderWidth = 1.2.dp
                            )
                            .padding(20.dp)
                            .testTag("wrapped_card_top_artists")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Top Artists Section
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Localization.getString("top_artists", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                wrappedStats.topArtists.take(3).forEachIndexed { idx, artist ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .then(
                                                if (idx == 0) Modifier.background(Brush.verticalGradient(listOf(palette.primary.copy(alpha = 0.4f), Color(0xFF1B1832))))
                                                else Modifier.background(Color(0xFF18162A))
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (idx == 0) palette.accent else Color(0x30FFFFFF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "#${idx + 1}",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (idx == 0) Color.Black else Color.White
                                                    )
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${artist.count} plays",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0A0C0), fontSize = 10.sp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Top Albums Chips
                            if (wrappedStats.topAlbums.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Localization.getString("top_albums", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0A0B8), fontWeight = FontWeight.Bold)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(wrappedStats.topAlbums.take(4)) { album ->
                                        Surface(
                                            color = Color(0x20FFFFFF),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Album, contentDescription = null, tint = palette.secondary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = album.name,
                                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontSize = 11.sp)
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

            // Pinterest Card 4: Audio Personality & Time of Day Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(26.dp),
                            thickness = GlassThickness.THICK,
                            tintColor = palette.primary,
                            tintAlpha = 0.25f,
                            borderWidth = 1.2.dp
                        )
                        .padding(22.dp)
                        .testTag("wrapped_card_personality")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = palette.accent, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = Localization.getString("audio_personality", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                )
                            }

                            // Time Slot Badge
                            Surface(
                                color = palette.secondary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) wrappedStats.favoriteTimeSlot.labelFa else wrappedStats.favoriteTimeSlot.labelEn,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(color = palette.secondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                )
                            }
                        }

                        // Persona Title
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) wrappedStats.audioPersonalityFa else wrappedStats.audioPersonalityEn,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        )

                        Text(
                            text = if (lang == AppLanguage.PERSIAN) wrappedStats.audioPersonalityDescFa else wrappedStats.audioPersonalityDescEn,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFD0D0E8), lineHeight = 20.sp)
                        )

                        // Genres Breakdown Chips
                        if (wrappedStats.topGenres.isNotEmpty()) {
                            Divider(color = Color(0x20FFFFFF))
                            Text(
                                text = Localization.getString("top_genres", lang),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFA0A0C0), fontWeight = FontWeight.Bold)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                wrappedStats.topGenres.take(3).forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(palette.primary.copy(alpha = 0.3f))
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = genre.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Big Share Action Button
                        Button(
                            onClick = { shareRecap(wrappedStats) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Localization.getString("share_wrapped", lang),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
