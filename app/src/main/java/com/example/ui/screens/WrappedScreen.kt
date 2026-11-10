package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.OnlineTasteEngine
import com.example.audio.AmbientPalette
import com.example.data.model.*
import com.example.ui.components.TrackArtworkThumbnail
import com.example.ui.theme.AuraCanvas
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.util.Localization
import com.example.util.WrappedCardGenerator
import com.example.util.WrappedEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

@Composable
fun WrappedScreen(
    periods: List<WrappedPeriod>,
    selectedPeriod: WrappedPeriod?,
    wrappedStats: WrappedStats?,
    palette: AmbientPalette,
    appSettings: AppSettings,
    onSelectPeriod: (WrappedPeriod) -> Unit,
    onPlayTrackById: (String) -> Unit,
    bottomPadding: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = appSettings.language
    val isFa = lang == AppLanguage.PERSIAN

    var showInteractiveStoryModal by remember { mutableStateOf(false) }
    var previewUnlockedOverride by remember(selectedPeriod?.id) { mutableStateOf(false) }

    val selectedTimeframe = selectedPeriod?.timeframe ?: WrappedTimeframe.WEEKLY
    val isPeriodLocked = (selectedPeriod?.isLocked == true) && !previewUnlockedOverride

    fun formatNum(value: Any): String {
        val raw = value.toString()
        return if (isFa) WrappedEngine.toPersianDigits(raw) else raw
    }

    val effectiveStats: WrappedStats? = wrappedStats

    fun formatListenedDuration(durationMs: Long): String {
        val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
        val mins = totalSec / 60L
        val secs = totalSec % 60L
        return if (isFa) {
            if (mins > 0L && secs > 0L) "${formatNum(mins)} دقیقه و ${formatNum(secs)} ثانیه"
            else if (mins > 0L) "${formatNum(mins)} دقیقه"
            else "${formatNum(secs)} ثانیه"
        } else {
            if (mins > 0L && secs > 0L) "${mins}m ${secs}s"
            else if (mins > 0L) "${mins}m"
            else "${secs}s"
        }
    }

    fun shareRecap(stats: WrappedStats) {
        scope.launch {
            val uri = withContext(Dispatchers.IO) {
                WrappedCardGenerator.generateWrappedStoryBitmap(context, stats, palette, lang)
            }
            if (uri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        if (isFa)
                            "کارنامه شنیداری من در Aura Music (${stats.period.displayNameFa}) • ${formatNum(stats.totalListeningMinutes)} دقیقه موسیقی!"
                        else
                            "My Aura Music Wrapped (${stats.period.displayNameEn}) • ${stats.totalListeningMinutes} mins of music!"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        Localization.getString("share_wrapped", lang)
                    )
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("wrapped_screen_list"),
        contentPadding = PaddingValues(top = 14.dp, bottom = maxOf(bottomPadding + 24.dp, 128.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Clean Spotify Wrapped Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Localization.getString("wrapped_title", lang),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 23.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isFa) "آمار و کارنامه شنیداری شما" else "Your listening stats & highlights",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.5.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isPeriodLocked) AuraSurfaceElevated else AuraSpotifyGreen.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isPeriodLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isPeriodLocked) TextSecondary else AuraSpotifyGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isPeriodLocked) {
                                if (isFa) "قفل تا موعد" else "Locked"
                            } else {
                                if (isFa) "باز (۴۸ ساعت)" else "Unlocked"
                            },
                            color = if (isPeriodLocked) TextSecondary else AuraSpotifyGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        // 2. Spotify Filter Chips for Timeframe Selection (Weekly, Monthly, Yearly)
        item {
            val timeframes = listOf(
                Triple(
                    WrappedTimeframe.WEEKLY,
                    if (isFa) "هفتگی" else "Weekly",
                    Icons.Default.DateRange
                ),
                Triple(
                    WrappedTimeframe.MONTHLY,
                    if (isFa) "ماهانه" else "Monthly",
                    Icons.Default.CalendarMonth
                ),
                Triple(
                    WrappedTimeframe.YEARLY,
                    if (isFa) "سالانه" else "Yearly",
                    Icons.Default.EmojiEvents
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timeframes.forEach { (tf, label, icon) ->
                    val isSelected = selectedTimeframe == tf
                    val targetPeriod = periods.firstOrNull { it.timeframe == tf }
                    val isTfLocked = targetPeriod?.isLocked == true

                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) AuraSpotifyGreen else AuraSurfaceElevated)
                            .clickable {
                                if (targetPeriod != null) {
                                    onSelectPeriod(targetPeriod)
                                }
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isTfLocked) Icons.Default.Lock else icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }

        // 3. Locked Vault State vs Unlocked Stats View
        if (isPeriodLocked && selectedPeriod != null) {
            item {
                LockedWrappedVaultCard(
                    period = selectedPeriod,
                    stats = effectiveStats,
                    palette = palette,
                    isFa = isFa,
                    formatNum = ::formatNum,
                    onPreviewStoryAnyway = {
                        if (effectiveStats != null && effectiveStats.topSongs.isNotEmpty()) {
                            showInteractiveStoryModal = true
                        } else {
                            previewUnlockedOverride = true
                        }
                    },
                    onUnlockPreviewPage = {
                        previewUnlockedOverride = true
                    }
                )
            }
        } else if (effectiveStats != null) {
            // Total Listening Summary Card
            item {
                val hoursStr = "%.1f".format(Locale.US, effectiveStats.totalListeningHours)
                val completionRatePct = (effectiveStats.completionRate.coerceIn(0f, 1f) * 100).toInt()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraSurface)
                        .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                        .padding(20.dp)
                        .testTag("wrapped_card_total_time")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isFa) effectiveStats.period.displayNameFa else effectiveStats.period.displayNameEn,
                            color = AuraSpotifyGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = formatNum(effectiveStats.totalListeningMinutes),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 52.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (isFa) {
                                "دقیقه گوش دادن (${formatNum(hoursStr)} ساعت)"
                            } else {
                                "Minutes Listened ($hoursStr Hours)"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AuraSurfaceElevated)
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WrappedMiniMetric(
                                value = formatNum(effectiveStats.totalTracksPlayed),
                                label = if (isFa) "پخش کامل" else "Streams",
                                color = Color.White
                            )
                            WrappedMiniMetric(
                                value = formatNum(effectiveStats.uniqueArtistsCount.coerceAtLeast(effectiveStats.topArtists.size)),
                                label = if (isFa) "خواننده‌ها" else "Artists",
                                color = AuraSpotifyGreen
                            )
                            WrappedMiniMetric(
                                value = "${formatNum(completionRatePct)}%",
                                label = if (isFa) "نرخ تکمیل" else "Completed",
                                color = Color.White
                            )
                            WrappedMiniMetric(
                                value = formatNum(effectiveStats.listeningStreakDays),
                                label = if (isFa) "روز متوالی" else "Streak",
                                color = AuraSpotifyGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val hasValidSongs = effectiveStats.topSongs.isNotEmpty()
                        Button(
                            onClick = { if (hasValidSongs) showInteractiveStoryModal = true },
                            enabled = hasValidSongs,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuraSpotifyGreen,
                                contentColor = Color.Black,
                                disabledContainerColor = AuraSurfaceElevated,
                                disabledContentColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = if (hasValidSongs) Icons.Default.PlayArrow else Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasValidSongs) {
                                    if (isFa) "مشاهده استوری کامل (Wrapped)" else "Watch Wrapped Story"
                                } else {
                                    if (isFa) "حداقل نیمی از یک آهنگ را گوش دهید" else "Listen to 50% of a song to unlock"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (effectiveStats.topSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AuraSurface)
                            .padding(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (isFa) "هنوز آهنگی ثبت نشده است" else "No qualified tracks yet",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (isFa) {
                                    "برای ثبت هر آهنگ در کارنامه شنیداری، باید حداقل نیمی (۵۰٪) از زمان آن را گوش دهید."
                                } else {
                                    "Listen to at least 50% of a track's duration for it to count toward your Wrapped."
                                },
                                color = TextSecondary,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Top Songs List
            if (effectiveStats.topSongs.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AuraSurface)
                            .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                            .testTag("wrapped_card_top_songs")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Localization.getString("top_songs", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 17.sp
                                    )
                                )
                                Text(
                                    text = if (isFa) "پخش" else "Play",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            for ((index, song) in effectiveStats.topSongs.withIndex()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onPlayTrackById(song.trackId) }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = formatNum(index + 1),
                                        color = if (index == 0) AuraSpotifyGreen else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(22.dp)
                                    )

                                    TrackArtworkThumbnail(
                                        artworkUri = song.artworkUri,
                                        accentColor = palette.primary,
                                        size = 46.dp,
                                        shape = RoundedCornerShape(6.dp),
                                        title = song.title,
                                        artist = song.artist,
                                        trackId = song.trackId
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${formatNum(song.playCount)}x",
                                            color = if (index == 0) AuraSpotifyGreen else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = formatListenedDuration(song.totalDurationMs),
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Top Artists List
            if (effectiveStats.topArtists.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AuraSurface)
                            .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (isFa) "خوانندگان برتر شما" else "Your Top Artists",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 17.sp
                                )
                            )

                            for ((idx, artist) in effectiveStats.topArtists.withIndex()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = formatNum(idx + 1),
                                        color = if (idx == 0) AuraSpotifyGreen else TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.width(22.dp)
                                    )
                                    TrackArtworkThumbnail(
                                        artworkUri = artist.artworkUri,
                                        accentColor = palette.primary,
                                        size = 44.dp,
                                        shape = CircleShape,
                                        title = artist.name
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatNum(artist.formattedDuration),
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = "${formatNum(artist.count)}x",
                                        color = AuraSpotifyGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Share Recap Button
            item {
                OutlinedButton(
                    onClick = { shareRecap(effectiveStats) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Localization.getString("share_wrapped", lang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }

    if (showInteractiveStoryModal && effectiveStats != null) {
        SpotifyWrappedStoryModal(
            stats = effectiveStats,
            palette = palette,
            isFa = isFa,
            onPlayTrack = { trackId ->
                showInteractiveStoryModal = false
                onPlayTrackById(trackId)
            },
            onShare = { shareRecap(effectiveStats) },
            onDismiss = { showInteractiveStoryModal = false }
        )
    }
}

@Composable
private fun LockedWrappedVaultCard(
    period: WrappedPeriod,
    stats: WrappedStats?,
    palette: AmbientPalette,
    isFa: Boolean,
    formatNum: (Any) -> String,
    onPreviewStoryAnyway: () -> Unit,
    onUnlockPreviewPage: () -> Unit
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }
    val nowCal = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }
    val currentHour = nowCal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = nowCal.get(Calendar.MINUTE)
    val daysLeft = (period.daysRemainingUntilUnlock - 1).coerceAtLeast(0)
    val hoursLeft = (23 - currentHour).coerceIn(0, 23)
    val minsLeft = (59 - currentMinute).coerceIn(0, 59)

    val ruleDescription = when (period.timeframe) {
        WrappedTimeframe.WEEKLY -> if (isFa) {
            "گزارش هفتگی شما تا روز جمعه قفل است و در روزهای جمعه به مدت ۴۸ ساعت بازگشایی می‌شود."
        } else {
            "Your Weekly Wrapped unlocks every Friday for a 48-hour window."
        }
        WrappedTimeframe.MONTHLY -> if (isFa) {
            "گزارش ماهانه شما در روز آخر هر ماه به مدت ۴۸ ساعت بازگشایی می‌شود."
        } else {
            "Your Monthly Wrapped unlocks on the last day of each month for 48 hours."
        }
        WrappedTimeframe.YEARLY -> if (isFa) {
            "گزارش سالانه شما در پایان سال به مدت ۴۸ ساعت بازگشایی می‌شود."
        } else {
            "Your Annual Wrapped unlocks on the final day of the year for 48 hours."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuraSurface)
            .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(12.dp))
            .padding(20.dp)
            .testTag("wrapped_locked_vault_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AuraSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = AuraSpotifyGreen,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isFa) period.displayNameFa else period.displayNameEn,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isFa) period.unlockTargetDateFa else period.unlockTargetDateEn,
                color = AuraSpotifyGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CountdownUnitBox(
                    value = formatNum(daysLeft),
                    label = if (isFa) "روز" else "Days",
                    modifier = Modifier.weight(1f)
                )
                CountdownUnitBox(
                    value = formatNum(String.format(Locale.US, "%02d", hoursLeft)),
                    label = if (isFa) "ساعت" else "Hours",
                    modifier = Modifier.weight(1f)
                )
                CountdownUnitBox(
                    value = formatNum(String.format(Locale.US, "%02d", minsLeft)),
                    label = if (isFa) "دقیقه" else "Minutes",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = ruleDescription,
                color = TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            val recordedPlays = stats?.totalTracksPlayed ?: 0
            val recordedMins = stats?.totalListeningMinutes ?: 0L
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AuraSurfaceElevated)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isFa) {
                        "ثبت‌شده تا این لحظه: ${formatNum(recordedPlays)} پخش کامل • ${formatNum(recordedMins)} دقیقه"
                    } else {
                        "Recorded so far: ${formatNum(recordedPlays)} plays • ${formatNum(recordedMins)} min"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPreviewStoryAnyway,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraSpotifyGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text(
                        text = if (isFa) "پیش‌نمایش استوری" else "Preview Story",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onUnlockPreviewPage,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text(
                        text = if (isFa) "مشاهده آمار" else "View Stats",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownUnitBox(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AuraSurfaceElevated)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun WrappedMiniMetric(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
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
    val totalSlides = 6
    var currentSlide by remember { mutableIntStateOf(0) }
    var slideProgress by remember { mutableFloatStateOf(0f) }
    var isPausedByTouch by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentlyPlayingStoryTrackTitle by remember { mutableStateOf("") }

    val context = LocalContext.current
    val app = remember { try { com.example.AuraApplication.instance } catch (_: Exception) { null } }
    val allTracks by (app?.repository?.allTracks ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    fun fmt(v: Any): String = if (isFa) WrappedEngine.toPersianDigits(v.toString()) else v.toString()

    LaunchedEffect(isMuted) {
        if (isMuted || app == null) return@LaunchedEffect
        val topSongId = stats.topSongs.firstOrNull()?.trackId ?: return@LaunchedEffect
        val onlineCached = try {
            com.example.OnlineMusicRepository.getMemoryCachedAppTracks()
        } catch (_: Exception) {
            emptyList()
        }
        val recentOnline = OnlineTasteEngine.getRecentOnlineTracks(context)
        val combinedPool = (recentOnline + onlineCached + allTracks).distinctBy { it.id }
        val trackToPlay = combinedPool.firstOrNull { it.id == topSongId }

        if (trackToPlay != null) {
            currentlyPlayingStoryTrackTitle = "${trackToPlay.title} • ${trackToPlay.artist}"
            val isAlreadyPlaying = app.audioEngine.status.value == PlayerStatus.PLAYING
            if (!isAlreadyPlaying) {
                val hookStartMs = (trackToPlay.durationMs * 0.28f).toLong().coerceIn(0L, 45_000L)
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

    val slideAccentColor = remember(currentSlide, palette) {
        when (currentSlide) {
            0 -> AuraSpotifyGreen
            1 -> Color(0xFF38BDF8)
            2 -> Color(0xFFFBBF24)
            3 -> Color(0xFFF43F5E)
            4 -> AuraSpotifyGreen
            else -> palette.primary
        }
    }

    val cPrimary by animateColorAsState(slideAccentColor, tween(450), label = "cPrimary")

    val infiniteTransition = rememberInfiniteTransition(label = "StoryVinylSpin")
    val vinylSpinDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), AnimRepeatMode.Restart),
        label = "vinylSpinDeg"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to cPrimary.copy(alpha = 0.22f),
                                0.45f to AuraCanvas,
                                1.0f to Color(0xFF0A0A0A)
                            )
                        )
                    )
                    .pointerInput(currentSlide) {
                        detectTapGestures(
                            onPress = {
                                isPausedByTouch = true
                                tryAwaitRelease()
                                isPausedByTouch = false
                            },
                            onTap = { offset ->
                                val w = size.width
                                if (offset.x < w * 0.33f) {
                                    if (currentSlide > 0) currentSlide--
                                } else {
                                    if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss()
                                }
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Story Top Progress Indicators & Header
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
                                        .height(12.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            currentSlide = i
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
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
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (currentlyPlayingStoryTrackTitle.isNotBlank() && !isMuted) {
                                    Text(
                                        text = "♪ $currentlyPlayingStoryTrackTitle",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(AuraSurfaceElevated)
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
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(AuraSurfaceElevated)
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

                    // 6 Story Slides
                    AnimatedContent(
                        targetState = currentSlide,
                        transitionSpec = {
                            val forward = targetState >= initialState
                            (fadeIn(tween(260)) +
                                slideInHorizontally(
                                    initialOffsetX = { fullW -> if (forward) fullW / 8 else -fullW / 8 },
                                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                                ))
                                .togetherWith(
                                    fadeOut(tween(180)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { fullW -> if (forward) -fullW / 8 else fullW / 8 },
                                            animationSpec = tween(180)
                                        )
                                )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        label = "CleanStorySlide"
                    ) { slideIndex ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            when (slideIndex) {
                                0 -> CleanStoryCardContainer(
                                    accentColor = cPrimary,
                                    stepBadge = if (isFa) "بخش ۱ از ۶ • خلاصه دوره" else "01 / 06 • OVERVIEW"
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(CircleShape)
                                            .background(AuraSurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = cPrimary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Text(
                                        text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (isFa)
                                            "نگاهی به آهنگ‌ها و هنرمندانی که بیشترین همراهی را با شما داشتند."
                                        else
                                            "A look back at the songs and artists that defined your listening.",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(22.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StoryGlassStatPill(
                                            value = fmt(stats.uniqueTracksCount.coerceAtLeast(stats.topSongs.size)),
                                            label = if (isFa) "آهنگ‌ها" else "Tracks",
                                            accent = cPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StoryGlassStatPill(
                                            value = fmt(stats.uniqueArtistsCount.coerceAtLeast(stats.topArtists.size)),
                                            label = if (isFa) "خواننده‌ها" else "Artists",
                                            accent = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                1 -> CleanStoryCardContainer(
                                    accentColor = cPrimary,
                                    stepBadge = if (isFa) "بخش ۲ از ۶ • زمان شنیداری" else "02 / 06 • LISTENING TIME"
                                ) {
                                    Box(
                                        modifier = Modifier.size(148.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val strokeW = 8.dp.toPx()
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.1f),
                                                style = Stroke(width = strokeW)
                                            )
                                            drawArc(
                                                color = cPrimary,
                                                startAngle = -90f,
                                                sweepAngle = 285f,
                                                useCenter = false,
                                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = fmt(stats.totalListeningMinutes),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 34.sp
                                            )
                                            Text(
                                                text = if (isFa) "دقیقه" else "MINUTES",
                                                color = TextSecondary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = if (isFa)
                                            "معادل ${fmt(String.format(Locale.US, "%.1f", stats.totalListeningHours))} ساعت گوش دادن به موسیقی"
                                        else
                                            "Equivalent to ${String.format(Locale.US, "%.1f", stats.totalListeningHours)} hours of music",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StoryGlassStatPill(
                                            value = fmt(stats.totalTracksPlayed),
                                            label = if (isFa) "پخش کامل" else "Total Streams",
                                            accent = cPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StoryGlassStatPill(
                                            value = fmt(stats.listeningStreakDays),
                                            label = if (isFa) "روز متوالی" else "Day Streak",
                                            accent = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                2 -> {
                                    val topSong = stats.topSongs.firstOrNull()
                                    CleanStoryCardContainer(
                                        accentColor = cPrimary,
                                        stepBadge = if (isFa) "بخش ۳ از ۶ • آهنگ شماره ۱" else "03 / 06 • YOUR #1 SONG"
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(144.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(
                                                modifier = Modifier
                                                    .size(128.dp)
                                                    .offset(x = 28.dp)
                                            ) {
                                                rotate(vinylSpinDeg) {
                                                    drawCircle(color = Color(0xFF101010))
                                                    drawCircle(
                                                        color = cPrimary.copy(alpha = 0.5f),
                                                        style = Stroke(width = 2.dp.toPx())
                                                    )
                                                    drawCircle(
                                                        color = cPrimary,
                                                        radius = size.minDimension * 0.16f
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .offset(x = (-14).dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            ) {
                                                TrackArtworkThumbnail(
                                                    artworkUri = topSong?.artworkUri,
                                                    accentColor = cPrimary,
                                                    size = 132.dp,
                                                    shape = RoundedCornerShape(10.dp),
                                                    title = topSong?.title,
                                                    artist = topSong?.artist
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Text(
                                            text = topSong?.title ?: "-",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = topSong?.artist ?: "",
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            StoryGlassStatPill(
                                                value = "${fmt(topSong?.playCount ?: 1)}x",
                                                label = if (isFa) "تعداد پخش" else "Times Played",
                                                accent = cPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            StoryGlassStatPill(
                                                value = "${fmt(((topSong?.totalDurationMs ?: 180000L) / 60000L).coerceAtLeast(1L))}m",
                                                label = if (isFa) "زمان پخش" else "Minutes",
                                                accent = Color.White,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        if (topSong != null) {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Button(
                                                onClick = { onPlayTrack(topSong.trackId) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = cPrimary,
                                                    contentColor = Color.Black
                                                ),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isFa) "پخش آهنگ" else "Play Song",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                3 -> {
                                    val topArtist = stats.topArtists.firstOrNull()
                                    CleanStoryCardContainer(
                                        accentColor = cPrimary,
                                        stepBadge = if (isFa) "بخش ۴ از ۶ • خواننده شماره ۱" else "04 / 06 • YOUR #1 ARTIST"
                                    ) {
                                        TrackArtworkThumbnail(
                                            artworkUri = topArtist?.artworkUri ?: stats.topSongs.firstOrNull()?.artworkUri,
                                            accentColor = cPrimary,
                                            size = 124.dp,
                                            shape = CircleShape,
                                            title = topArtist?.name
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = topArtist?.name ?: "-",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            StoryGlassStatPill(
                                                value = "${fmt(topArtist?.count ?: 1)}x",
                                                label = if (isFa) "دفعات پخش" else "Artist Plays",
                                                accent = cPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            StoryGlassStatPill(
                                                value = "${fmt(topArtist?.percentage?.toInt() ?: 100)}%",
                                                label = if (isFa) "سهم از پخش" else "Share",
                                                accent = Color.White,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                4 -> CleanStoryCardContainer(
                                    accentColor = cPrimary,
                                    stepBadge = if (isFa) "بخش ۵ از ۶ • ۵ آهنگ برتر" else "05 / 06 • YOUR TOP 5 SONGS"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        stats.topSongs.take(5).forEachIndexed { idx, s ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(AuraSurfaceElevated)
                                                    .clickable { onPlayTrack(s.trackId) }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = fmt(idx + 1),
                                                    color = if (idx == 0) cPrimary else TextSecondary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    modifier = Modifier.width(24.dp)
                                                )
                                                TrackArtworkThumbnail(
                                                    artworkUri = s.artworkUri,
                                                    accentColor = cPrimary,
                                                    size = 38.dp,
                                                    shape = RoundedCornerShape(6.dp),
                                                    title = s.title
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = s.title,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 13.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = s.artist,
                                                        color = TextSecondary,
                                                        fontSize = 11.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "${fmt(s.playCount)}x",
                                                    color = if (idx == 0) cPrimary else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                else -> CleanStoryCardContainer(
                                    accentColor = cPrimary,
                                    stepBadge = if (isFa) "بخش ۶ از ۶ • خلاصه کارنامه" else "06 / 06 • SUMMARY CARD"
                                ) {
                                    val topSong = stats.topSongs.firstOrNull()
                                    val topArtist = stats.topArtists.firstOrNull()
                                    val topGenre = stats.topGenres.firstOrNull()?.name ?: "Pop & Hip-Hop"

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TrackArtworkThumbnail(
                                            artworkUri = topSong?.artworkUri ?: topArtist?.artworkUri,
                                            accentColor = cPrimary,
                                            size = 64.dp,
                                            shape = RoundedCornerShape(8.dp),
                                            title = topSong?.title
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isFa) "شخصیت شنیداری" else "AUDIO ARCHETYPE",
                                                color = cPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.5.sp
                                            )
                                            Text(
                                                text = if (isFa) stats.audioPersonalityFa else stats.audioPersonalityEn,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                                color = TextSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = Color(0x14FFFFFF))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StoryGlassStatPill(
                                            value = topArtist?.name ?: "-",
                                            label = if (isFa) "خواننده برتر" else "#1 Artist",
                                            accent = cPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StoryGlassStatPill(
                                            value = topSong?.title ?: "-",
                                            label = if (isFa) "آهنگ برتر" else "#1 Song",
                                            accent = Color.White,
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
                                            label = if (isFa) "زمان پخش" else "Minutes",
                                            accent = cPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StoryGlassStatPill(
                                            value = topGenre,
                                            label = if (isFa) "سبک برتر" else "Top Genre",
                                            accent = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Story Controls Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentSlide > 0) currentSlide-- },
                            enabled = currentSlide > 0,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                if (currentSlide > 0) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = if (currentSlide > 0) Color.White else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFa) "قبلی" else "Prev",
                                color = if (currentSlide > 0) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onShare,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "اشتراک‌گذاری" else "Share Story",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }

                        Button(
                            onClick = { if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (currentSlide < totalSlides - 1) (if (isFa) "بعدی" else "Next") else (if (isFa) "پایان" else "Done"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (currentSlide < totalSlides - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                                contentDescription = null,
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
private fun CleanStoryCardContainer(
    accentColor: Color,
    stepBadge: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AuraSurface)
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AuraSurfaceElevated)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = stepBadge,
                    color = accentColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

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
            .clip(RoundedCornerShape(10.dp))
            .background(AuraSurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = accent,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

