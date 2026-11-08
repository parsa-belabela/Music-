package com.example.ui.screens

import android.content.Intent
import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
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
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization
import com.example.util.WrappedCardGenerator
import com.example.util.WrappedEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

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

    // استفاده مستقیم و ۱۰۰٪ واقعی از آمار آهنگ‌هایی که حداقل تا نیمه (۵۰٪) گوش داده شده‌اند (بدون هیچ آمار ساختگی!)
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
                            "✨ کارنامه شنیداری من در Aura Music (${stats.period.displayNameFa}) • ${formatNum(stats.totalListeningMinutes)} دقیقه موسیقی!"
                        else
                            "✨ My Aura Music Wrapped (${stats.period.displayNameEn}) • ${stats.totalListeningMinutes} mins of music!"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, Localization.getString("share_wrapped", lang)))
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("wrapped_screen_list"),
        contentPadding = PaddingValues(top = 14.dp, bottom = bottomPadding + 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ۱. هدر مینیمال به سبک اسپاتیفای
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF1ED760),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFa) "کپسول زمان اسپاتیفای • دسترسی ۴۸ ساعته" else "AURA WRAPPED • 48H LIMITED ACCESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF1ED760),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Localization.getString("wrapped_title", lang),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    )
                }

                // نشانگر وضعیت قفل یا باز بودن دوره فعلی
                Surface(
                    color = if (isPeriodLocked) Color(0x28F59E0B) else Color(0x281ED760),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        1.dp,
                        if (isPeriodLocked) Color(0xFFF59E0B) else Color(0xFF1ED760)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isPeriodLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isPeriodLocked) Color(0xFFFBBF24) else Color(0xFF1ED760),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isPeriodLocked) {
                                if (isFa) "قفل تا موعد" else "Locked"
                            } else {
                                if (isFa) "باز (۴۸ ساعت)" else "Unlocked (48h)"
                            },
                            color = if (isPeriodLocked) Color(0xFFFBBF24) else Color(0xFF1ED760),
                            fontWeight = FontWeight.Black,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        }

        // ۲. تب‌های ۳گانه: هفتگی (جمعه) | ماهانه (آخر ماه) | سالانه (آخر سال)
        item {
            val timeframes = listOf(
                Triple(
                    WrappedTimeframe.WEEKLY,
                    if (isFa) "هفتگی (جمعه)" else "Weekly (Fri)",
                    Icons.Default.DateRange
                ),
                Triple(
                    WrappedTimeframe.MONTHLY,
                    if (isFa) "ماهانه (آخر ماه)" else "Monthly",
                    Icons.Default.CalendarMonth
                ),
                Triple(
                    WrappedTimeframe.YEARLY,
                    if (isFa) "سالانه (آخر سال)" else "Yearly",
                    Icons.Default.EmojiEvents
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(20.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.14f,
                        borderWidth = 1.dp
                    )
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                timeframes.forEach { (tf, label, icon) ->
                    val isSelected = selectedTimeframe == tf
                    val targetPeriod = periods.firstOrNull { it.timeframe == tf }
                    val isTfLocked = targetPeriod?.isLocked == true

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF1ED760), palette.primary)
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, Color.Transparent)
                                    )
                                }
                            )
                            .clickable {
                                if (targetPeriod != null) {
                                    onSelectPeriod(targetPeriod)
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isTfLocked) Icons.Default.Lock else icon,
                                contentDescription = label,
                                tint = if (isSelected) Color(0xFF060A08) else Color(0xFFB4B9CE),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF060A08) else Color(0xFFB4B9CE),
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // ۳. بررسی قفل بودن دوره:
        // طبق دستور کاربر، تا روز جمعه (هفتگی)، روز آخر ماه (ماهانه) و روز آخر سال (سالانه)
        // این بخش باید قفل باشد و فقط در آن بازه ۴۸ ساعته باز شود!
        // =========================================================================
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
            // =====================================================================
            // زمانی که در بازه ۴۸ ساعته بازگشایی هستیم (یا پیش‌نمایش فعال شده):
            // =====================================================================
            item {
                val hoursStr = "%.1f".format(java.util.Locale.US, effectiveStats.totalListeningHours)
                val completionRatePct = (effectiveStats.completionRate.coerceIn(0f, 1f) * 100).toInt()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(22.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF1ED760))
                        .liquidGlass(
                            shape = RoundedCornerShape(28.dp),
                            thickness = GlassThickness.THICK,
                            tintColor = palette.primary,
                            tintAlpha = 0.24f,
                            borderWidth = 1.4.dp,
                            secondaryAccent = Color(0xFF1ED760)
                        )
                        .padding(22.dp)
                        .testTag("wrapped_card_total_time")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = Color(0x261ED760),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFF1ED760).copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = if (isFa) {
                                    "✨ ${effectiveStats.period.displayNameFa} • باز به مدت ۴۸ ساعت"
                                } else {
                                    "✨ ${effectiveStats.period.displayNameEn} • Open for 48h"
                                },
                                color = Color(0xFF1ED760),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = formatNum(effectiveStats.totalListeningMinutes),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 62.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (isFa) {
                                "دقیقه گوش دادن به موسیقی (${formatNum(hoursStr)} ساعت)"
                            } else {
                                "Minutes Listened ($hoursStr Hours)"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color(0xFFD0D6EA),
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x28000000))
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WrappedMiniMetric(
                                value = formatNum(effectiveStats.totalTracksPlayed),
                                label = if (isFa) "پخش کامل" else "Streams",
                                color = Color(0xFF1ED760)
                            )
                            WrappedMiniMetric(
                                value = formatNum(effectiveStats.uniqueArtistsCount.coerceAtLeast(effectiveStats.topArtists.size)),
                                label = if (isFa) "خواننده‌ها" else "Artists",
                                color = palette.accent
                            )
                            WrappedMiniMetric(
                                value = "${formatNum(completionRatePct)}%",
                                label = if (isFa) "نرخ تکمیل" else "Completed",
                                color = Color(0xFF38BDF8)
                            )
                            WrappedMiniMetric(
                                value = "${formatNum(effectiveStats.listeningStreakDays)}🔥",
                                label = if (isFa) "روز متوالی" else "Streak",
                                color = Color(0xFFFBBF24)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // دکمه تماشای استوری تمام‌صفحه اسپاتیفای (فقط زمانی که حداقل ۱ آهنگ تا ۵۰٪ گوش داده شده باشد)
                        val hasValidSongs = effectiveStats.topSongs.isNotEmpty()
                        Button(
                            onClick = { if (hasValidSongs) showInteractiveStoryModal = true },
                            enabled = hasValidSongs,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1ED760),
                                disabledContainerColor = Color.White.copy(alpha = 0.10f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = if (hasValidSongs) Icons.Default.PlayCircleFilled else Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = if (hasValidSongs) Color(0xFF050806) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasValidSongs) {
                                    if (isFa) "تماشای استوری کامل (Spotify Wrapped)" else "Watch Full Wrapped Story"
                                } else {
                                    if (isFa) "حداقل نصف یک آهنگ را گوش دهید تا استوری فعال شود" else "Listen to >=50% of a song to unlock Story"
                                },
                                color = if (hasValidSongs) Color(0xFF050806) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Black,
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
                            .liquidGlass(
                                shape = RoundedCornerShape(24.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.primary,
                                tintAlpha = 0.14f,
                                borderWidth = 1.dp
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF1ED760),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (isFa) "هنوز آهنگی تا نیمه (۵۰٪) گوش داده نشده است" else "No songs listened past 50% yet",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (isFa) {
                                    "در سیستم جدید وارپ، صرفاً کلیک کردن روی آهنگ شمارش نمی‌شود. برای ثبت شدن هر آهنگ در کارنامه شنیداری شما، باید حداقل نصف (۵۰٪) زمان آن آهنگ را واقعاً گوش دهید."
                                } else {
                                    "Clicking a track does not count it as played. You must genuinely listen to at least half (50%) of a track's duration for it to be recorded in your Wrapped."
                                },
                                color = Color(0xFFB4B9CE),
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ۵ آهنگ برتر دوره
            if (effectiveStats.topSongs.isNotEmpty()) {
                item {
                    val maxSongPlays = (effectiveStats.topSongs.maxOfOrNull { it.playCount } ?: 1).coerceAtLeast(1)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(24.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.primary,
                                tintAlpha = 0.16f,
                                borderWidth = 1.dp
                            )
                            .padding(18.dp)
                            .testTag("wrapped_card_top_songs")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Localization.getString("top_songs", lang),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    )
                                }
                                Text(
                                    text = if (isFa) "لمس برای پخش" else "Tap to Play",
                                    color = Color(0xFF1ED760),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            for ((index, song) in effectiveStats.topSongs.withIndex()) {
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
                                                        Color(0xFF1ED760).copy(alpha = if (index == 0) 0.28f else 0.14f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (index == 0) Color(0xFFFBBF24) else Color(0x28FFFFFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = formatNum(index + 1),
                                                color = if (index == 0) Color.Black else Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp
                                            )
                                        }

                                        TrackArtworkThumbnail(
                                            artworkUri = song.artworkUri,
                                            accentColor = palette.accent,
                                            size = 46.dp,
                                            shape = RoundedCornerShape(11.dp),
                                            title = song.title,
                                            artist = song.artist,
                                            trackId = song.trackId
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
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
                                                color = if (index == 0) Color(0xFFFBBF24) else Color(0xFF1ED760),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.5.sp
                                            )
                                            Text(
                                                text = formatListenedDuration(song.totalDurationMs),
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

            // خوانندگان برتر دوره
            if (effectiveStats.topArtists.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(24.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.secondary,
                                tintAlpha = 0.14f,
                                borderWidth = 1.dp
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF1ED760),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isFa) "خوانندگان برتر شما" else "Your Top Artists",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }

                            for ((idx, artist) in effectiveStats.topArtists.withIndex()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x22000000))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "#${formatNum(idx + 1)}",
                                        color = if (idx == 0) Color(0xFFFBBF24) else Color(0xFF1ED760),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(26.dp)
                                    )
                                    TrackArtworkThumbnail(
                                        artworkUri = artist.artworkUri,
                                        accentColor = palette.accent,
                                        size = 42.dp,
                                        shape = CircleShape,
                                        title = artist.name
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatNum(artist.formattedDuration),
                                            color = Color(0xFF9096AC),
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    Text(
                                        text = "${formatNum(artist.count)}x",
                                        color = Color(0xFF1ED760),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // دکمه اشتراک‌گذاری کارت خلاصه
            item {
                Button(
                    onClick = { shareRecap(effectiveStats) },
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

/**
 * کارت کپسول زمان قفل‌شده (Locked Time-Capsule Vault):
 * تا رسیدن روز جمعه (برای هفتگی)، روز آخر ماه (برای ماهانه) و روز آخر سال (برای سالانه)
 * تمام آمار را مخفی نگه می‌دارد و شمارش معکوس زنده تا زمان بازگشایی ۴۸ ساعته را نمایش می‌دهد.
 */
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

    val infiniteTransition = rememberInfiniteTransition(label = "VaultPulse")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), AnimRepeatMode.Restart),
        label = "ringRotation"
    )
    val lockPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), AnimRepeatMode.Reverse),
        label = "lockPulse"
    )

    val ruleDescription = when (period.timeframe) {
        WrappedTimeframe.WEEKLY -> if (isFa) {
            "گزارش هفتگی شما تا فرا رسیدن روز جمعه قفل است. این بخش فقط روزهای جمعه به مدت محدود ۴۸ ساعت باز می‌شود و سپس دوباره قفل می‌گردد."
        } else {
            "Your Weekly Wrapped is locked until Friday. It unlocks for a limited 48-hour window every Friday."
        }
        WrappedTimeframe.MONTHLY -> if (isFa) {
            "گزارش ماهانه شما تا روز آخر ماه قفل است. این بخش فقط در روز آخر هر ماه به مدت محدود ۴۸ ساعت برای تماشا باز می‌شود."
        } else {
            "Your Monthly Wrapped is locked until the last day of the month, opening for a limited 48-hour window."
        }
        WrappedTimeframe.YEARLY -> if (isFa) {
            "گزارش سالانه شما تا روز آخر سال قفل است. این بخش فقط در پایان سال به مدت محدود ۴۸ ساعت بازگشایی می‌شود."
        } else {
            "Your Annual Wrapped is locked until the final day of the year, opening for a limited 48-hour window."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFFF59E0B))
            .liquidGlass(
                shape = RoundedCornerShape(28.dp),
                thickness = GlassThickness.THICK,
                tintColor = Color(0xFFF59E0B),
                tintAlpha = 0.16f,
                borderWidth = 1.4.dp,
                secondaryAccent = palette.primary
            )
            .padding(24.dp)
            .testTag("wrapped_locked_vault_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // بج دسترسی محدود ۴۸ ساعته
            Surface(
                color = Color(0x28F59E0B),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isFa) "کپسول زمان قفل‌شده • بازگشایی محدود ۴۸ ساعته" else "TIME-LOCKED VAULT • 48H LIMITED WINDOW",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // آیکون قفل نئونی چرخان
            Box(
                modifier = Modifier.size(116.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    rotate(ringRotation) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFBBF24),
                                    Color.Transparent,
                                    Color(0xFF1ED760),
                                    Color.Transparent,
                                    Color(0xFFFBBF24)
                                )
                            ),
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    drawCircle(
                        color = Color(0xFFF59E0B).copy(alpha = 0.16f),
                        radius = (size.minDimension * 0.38f) * lockPulse
                    )
                }

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .scale(lockPulse)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF090B10),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isFa) period.displayNameFa else period.displayNameEn,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isFa) period.unlockTargetDateFa else period.unlockTargetDateEn,
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // شمارش معکوس روز / ساعت / دقیقه تا باز شدن قفل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CountdownUnitBox(
                    value = formatNum(daysLeft),
                    label = if (isFa) "روز مانده" else "Days",
                    accent = Color(0xFFFBBF24),
                    modifier = Modifier.weight(1f)
                )
                CountdownUnitBox(
                    value = formatNum(String.format("%02d", hoursLeft)),
                    label = if (isFa) "ساعت" else "Hours",
                    accent = Color(0xFF1ED760),
                    modifier = Modifier.weight(1f)
                )
                CountdownUnitBox(
                    value = formatNum(String.format("%02d", minsLeft)),
                    label = if (isFa) "دقیقه" else "Minutes",
                    accent = palette.accent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = ruleDescription,
                color = Color(0xFFC2C8DC),
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // نمایش زنده تعداد آهنگ‌های ثبت‌شده در این دوره (فقط آهنگ‌هایی که >= ۵۰٪ گوش داده شده‌اند)
            val recordedPlays = stats?.totalTracksPlayed ?: 0
            val recordedMins = stats?.totalListeningMinutes ?: 0L
            Surface(
                color = Color(0x221ED760),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1ED760).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isFa) {
                            "آمار ثبت‌شده تا این لحظه: ${formatNum(recordedPlays)} پخش کامل (بیش از نصف آهنگ) • ${formatNum(recordedMins)} دقیقه"
                        } else {
                            "Recorded so far: ${formatNum(recordedPlays)} qualified plays (>=50% listened) • ${formatNum(recordedMins)} min"
                        },
                        color = Color(0xFF1ED760),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isFa) {
                            "فقط آهنگ‌هایی که حداقل تا نیمه (۵۰٪) گوش داده شوند در وارپ ثبت می‌گردند."
                        } else {
                            "Only songs listened to for at least 50% of their duration are counted."
                        },
                        color = Color(0xFFB4B9CE),
                        fontSize = 10.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // دکمه تست و پیش‌نمایش استوری برای بررسی استوری بازطراحی‌شده
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPreviewStoryAnyway,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF1ED760).copy(alpha = 0.65f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        tint = Color(0xFF1ED760),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFa) "پیش‌نمایش استوری" else "Preview Story",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = onUnlockPreviewPage,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFa) "پیش‌نمایش آمار" else "Preview Stats",
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
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x2E000000))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )
            Text(
                text = label,
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
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
 * استوری تمام‌صفحه ۶ اسلایدی کاملاً بازطراحی‌شده به سبک Spotify Wrapped:
 * - بدون تکان خوردن یا کج شدن کارت با تکان دادن گوشی (ثابت، تمیز و استاندارد)
 * - هماهنگی ۱۰۰٪ جهت نوار پیشرفت و لمس چپ/راست صفحه (بدون باگ RTL در زبان فارسی)
 * - پخش نرم موسیقی پس‌زمینه بدون ری‌استارت شدن در هر اسلاید
 * - اسلایدهای مینیمال، خوانا و استاندارد که در هیچ صفحه‌ای بیرون نمی‌زنند
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
    var currentlyPlayingStoryTrackTitle by remember { mutableStateOf("") }

    val context = LocalContext.current
    val app = remember { try { com.example.AuraApplication.instance } catch (_: Exception) { null } }
    val allTracks by (app?.repository?.allTracks ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    fun fmt(v: Any): String = if (isFa) WrappedEngine.toPersianDigits(v.toString()) else v.toString()

    // پخش نرم آهنگ شماره ۱ کاربر در پس‌زمینه استوری (فقط اگر واقعاً آهنگ شماره ۱ وجود داشته باشد)
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

    // تایمر ۷ ثانیه‌ای برای هر اسلاید با قابلیت نگه داشتن انگشت برای توقف
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

    val slideColors = remember(currentSlide, palette) {
        when (currentSlide) {
            0 -> Triple(Color(0xFF1ED760), palette.primary, Color(0xFF00E5FF))
            1 -> Triple(Color(0xFF7B2CBF), Color(0xFF1ED760), Color(0xFF00E5FF))
            2 -> Triple(Color(0xFFFFBE0B), Color(0xFFFF1E56), Color(0xFF7209B7))
            3 -> Triple(Color(0xFF00F5D4), Color(0xFF3A86FF), Color(0xFFFF006E))
            4 -> Triple(Color(0xFF1ED760), Color(0xFF118AB2), Color(0xFFFFD166))
            else -> Triple(Color(0xFF1ED760), palette.primary, Color(0xFFFFBE0B))
        }
    }

    val cPrimary by animateColorAsState(slideColors.first, tween(500), label = "cPrimary")
    val cSecondary by animateColorAsState(slideColors.second, tween(500), label = "cSecondary")
    val cTertiary by animateColorAsState(slideColors.third, tween(500), label = "cTertiary")

    val infiniteTransition = rememberInfiniteTransition(label = "StoryBgAnim")
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), AnimRepeatMode.Restart),
        label = "rotationDeg"
    )
    val vinylSpinDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), AnimRepeatMode.Restart),
        label = "vinylSpinDeg"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        // استفاده از LayoutDirection.Ltr برای لایه استوری تا نوار پیشرفت و لمس چپ/راست در زبان فارسی معکوس نشوند
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF05070E))
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
                // ۱. پس‌زمینه نوری عمیق با ذرات معلق (بدون چرخش یا کج شدن کارت)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val rad = Math.toRadians(rotationDeg.toDouble())

                    drawIntoCanvas { canvas ->
                        val p1 = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                maskFilter = BlurMaskFilter(130f, BlurMaskFilter.Blur.NORMAL)
                                color = cPrimary.copy(alpha = 0.42f).toArgb()
                            }
                        }
                        val p2 = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                maskFilter = BlurMaskFilter(120f, BlurMaskFilter.Blur.NORMAL)
                                color = cSecondary.copy(alpha = 0.38f).toArgb()
                            }
                        }
                        canvas.drawCircle(
                            Offset(w * (0.3f + 0.15f * cos(rad).toFloat()), h * 0.25f),
                            w * 0.46f,
                            p1
                        )
                        canvas.drawCircle(
                            Offset(w * (0.7f - 0.15f * sin(rad).toFloat()), h * 0.72f),
                            w * 0.44f,
                            p2
                        )
                    }

                    for (i in 0 until 20) {
                        val seed = i * 41.0f
                        val px = ((sin(rad + seed) * 0.45f + 0.5f) * w).toFloat()
                        val py = ((((rotationDeg / 360f) + (i / 20f)) % 1f) * h)
                        val pRadius = (2f + (i % 3) * 1.8f).dp.toPx()
                        drawCircle(
                            color = Color.White.copy(alpha = 0.22f),
                            radius = pRadius,
                            center = Offset(px, h - py)
                        )
                    }
                }

                // ۲. محتوای اصلی استوری
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // الف) هدر استوری: نوار پیشرفت ۶ اسلایدی + دکمه صدا و بستن
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                                        .height(14.dp)
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
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color.White.copy(alpha = 0.24f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fill)
                                                .background(cPrimary)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(cPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF050806),
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
                                            color = cPrimary,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
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
                                        .background(Color.White.copy(alpha = 0.14f))
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
                                        .background(Color.White.copy(alpha = 0.14f))
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

                    // ب) اسلایدهای ۶گانه استوری (مینیمال، استاندارد و بدون بیرون‌زدگی)
                    AnimatedContent(
                        targetState = currentSlide,
                        transitionSpec = {
                            val forward = targetState >= initialState
                            (fadeIn(tween(300)) +
                                slideInHorizontally(
                                    initialOffsetX = { fullW -> if (forward) fullW / 6 else -fullW / 6 },
                                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                                ) +
                                scaleIn(initialScale = 0.94f, animationSpec = spring(dampingRatio = 0.82f)))
                                .togetherWith(
                                    fadeOut(tween(200)) +
                                        slideOutHorizontally(
                                            targetOffsetX = { fullW -> if (forward) -fullW / 7 else fullW / 7 },
                                            animationSpec = tween(200)
                                        )
                                )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        label = "CleanStorySlide"
                    ) { slideIndex ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            when (slideIndex) {
                                // اسلاید ۱: خوش‌آمدگویی و خلاصه دوره
                                0 -> CleanStoryCardContainer(
                                    primaryColor = cPrimary,
                                    secondaryColor = cSecondary,
                                    stepBadge = if (isFa) "بخش ۱ از ۶ • داستان شنیداری شما" else "01 / 06 • YOUR AUDIO STORY"
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(116.dp)
                                            .clip(CircleShape)
                                            .background(cPrimary.copy(alpha = 0.18f))
                                            .border(2.dp, cPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = cPrimary,
                                            modifier = Modifier.size(54.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Text(
                                        text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 26.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (isFa)
                                            "نگاهی به آهنگ‌ها، خواننده‌ها و لحظاتی که دنیای موسیقی شما را ساختند."
                                        else
                                            "A look back at the songs, artists, and moments that defined your sound.",
                                        color = Color(0xFFD0D6EA),
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

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
                                            accent = cTertiary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // اسلاید ۲: مجموع دقایق گوش دادن
                                1 -> CleanStoryCardContainer(
                                    primaryColor = cPrimary,
                                    secondaryColor = cSecondary,
                                    stepBadge = if (isFa) "بخش ۲ از ۶ • زمان شنیداری" else "02 / 06 • LISTENING TIME"
                                ) {
                                    Box(
                                        modifier = Modifier.size(156.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val strokeW = 10.dp.toPx()
                                            drawCircle(
                                                color = Color.White.copy(alpha = 0.10f),
                                                style = Stroke(width = strokeW)
                                            )
                                            drawArc(
                                                brush = Brush.sweepGradient(listOf(cSecondary, cPrimary, cTertiary, cSecondary)),
                                                startAngle = -90f,
                                                sweepAngle = 290f,
                                                useCenter = false,
                                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = fmt(stats.totalListeningMinutes),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 38.sp
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
                                            "معادل ${fmt(String.format(java.util.Locale.US, "%.1f", stats.totalListeningHours))} ساعت غرق شدن در موسیقی"
                                        else
                                            "That's ${String.format(java.util.Locale.US, "%.1f", stats.totalListeningHours)} hours of pure music",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StoryGlassStatPill(
                                            value = fmt(stats.totalTracksPlayed),
                                            label = if (isFa) "دفعات پخش" else "Total Streams",
                                            accent = cSecondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        StoryGlassStatPill(
                                            value = "${fmt(stats.listeningStreakDays)}🔥",
                                            label = if (isFa) "روز متوالی" else "Day Streak",
                                            accent = Color(0xFFFFBE0B),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // اسلاید ۳: آهنگ شماره ۱ شما
                                2 -> {
                                    val topSong = stats.topSongs.firstOrNull()
                                    CleanStoryCardContainer(
                                        primaryColor = cPrimary,
                                        secondaryColor = cSecondary,
                                        stepBadge = if (isFa) "بخش ۳ از ۶ • آهنگ شماره ۱ شما" else "03 / 06 • YOUR #1 SONG"
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(152.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Canvas(
                                                modifier = Modifier
                                                    .size(136.dp)
                                                    .offset(x = 32.dp)
                                            ) {
                                                rotate(vinylSpinDeg) {
                                                    drawCircle(color = Color(0xFF0E101A))
                                                    drawCircle(
                                                        color = Color(0xFFFFBE0B).copy(alpha = 0.6f),
                                                        style = Stroke(width = 2.5.dp.toPx())
                                                    )
                                                    drawCircle(
                                                        color = Color(0xFFFFBE0B),
                                                        radius = size.minDimension * 0.16f
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .offset(x = (-16).dp)
                                                    .shadow(18.dp, RoundedCornerShape(22.dp))
                                                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                                                    .clip(RoundedCornerShape(22.dp))
                                            ) {
                                                TrackArtworkThumbnail(
                                                    artworkUri = topSong?.artworkUri,
                                                    accentColor = Color(0xFFFFBE0B),
                                                    size = 140.dp,
                                                    shape = RoundedCornerShape(22.dp),
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
                                            fontSize = 21.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = topSong?.artist ?: "",
                                            color = cPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

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
                                                label = if (isFa) "زمان پخش" else "Minutes",
                                                accent = cSecondary,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        if (topSong != null) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = { onPlayTrack(topSong.trackId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFBE0B)),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isFa) "پخش کامل این آهنگ" else "Play Full Song",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // اسلاید ۴: خواننده شماره ۱ شما
                                3 -> {
                                    val topArtist = stats.topArtists.firstOrNull()
                                    CleanStoryCardContainer(
                                        primaryColor = cPrimary,
                                        secondaryColor = cSecondary,
                                        stepBadge = if (isFa) "بخش ۴ از ۶ • خواننده شماره ۱ شما" else "04 / 06 • YOUR #1 ARTIST"
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(136.dp)
                                                .border(3.dp, Color(0xFFFFD700), CircleShape)
                                                .padding(5.dp)
                                                .clip(CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TrackArtworkThumbnail(
                                                artworkUri = topArtist?.artworkUri ?: stats.topSongs.firstOrNull()?.artworkUri,
                                                accentColor = Color(0xFFFFD700),
                                                size = 124.dp,
                                                shape = CircleShape,
                                                title = topArtist?.name
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Text(
                                            text = topArtist?.name ?: "-",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

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
                                                label = if (isFa) "سهم از پخش" else "Share",
                                                accent = cPrimary,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // اسلاید ۵: ۵ آهنگ برتر شما
                                4 -> CleanStoryCardContainer(
                                    primaryColor = cPrimary,
                                    secondaryColor = cSecondary,
                                    stepBadge = if (isFa) "بخش ۵ از ۶ • ۵ آهنگ برتر شما" else "05 / 06 • YOUR TOP 5 SONGS"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        stats.topSongs.take(5).forEachIndexed { idx, s ->
                                            val medalColor = when (idx) {
                                                0 -> Color(0xFFFFD700)
                                                1 -> Color(0xFFE2E8F0)
                                                2 -> Color(0xFFF59E0B)
                                                else -> cPrimary
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Color.White.copy(alpha = 0.07f))
                                                    .border(0.8.dp, medalColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                                    .clickable { onPlayTrack(s.trackId) }
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "#${fmt(idx + 1)}",
                                                    color = medalColor,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.width(28.dp)
                                                )
                                                TrackArtworkThumbnail(
                                                    artworkUri = s.artworkUri,
                                                    accentColor = medalColor,
                                                    size = 36.dp,
                                                    shape = RoundedCornerShape(8.dp),
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
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // اسلاید ۶: کارت خلاصه نهایی استوری (Spotify Wrapped Poster)
                                else -> CleanStoryCardContainer(
                                    primaryColor = cPrimary,
                                    secondaryColor = cSecondary,
                                    stepBadge = if (isFa) "بخش ۶ از ۶ • کارت خلاصه استوری" else "06 / 06 • OFFICIAL SUMMARY CARD"
                                ) {
                                    val topSong = stats.topSongs.firstOrNull()
                                    val topArtist = stats.topArtists.firstOrNull()
                                    val topGenre = stats.topGenres.firstOrNull()?.name ?: "Hip-Hop & Pop"

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TrackArtworkThumbnail(
                                            artworkUri = topSong?.artworkUri ?: topArtist?.artworkUri,
                                            accentColor = cPrimary,
                                            size = 68.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            title = topSong?.title
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isFa) "شخصیت شنیداری شما" else "AUDIO ARCHETYPE",
                                                color = cPrimary,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.5.sp
                                            )
                                            Text(
                                                text = if (isFa) stats.audioPersonalityFa else stats.audioPersonalityEn,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 19.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (isFa) stats.period.displayNameFa else stats.period.displayNameEn,
                                                color = Color(0xFFB4B9CE),
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StoryGlassStatPill(
                                            value = topArtist?.name ?: "-",
                                            label = if (isFa) "خواننده برتر" else "#1 Artist",
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

                    // ج) نوار کنترل پایین استوری
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .liquidGlass(
                                shape = RoundedCornerShape(22.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = cPrimary,
                                tintAlpha = 0.18f,
                                borderWidth = 1.dp
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (currentSlide > 0) currentSlide-- },
                                enabled = currentSlide > 0,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (currentSlide > 0) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = if (currentSlide > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFa) "قبلی" else "Prev",
                                    color = if (currentSlide > 0) Color.White else Color.White.copy(alpha = 0.3f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = onShare,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFa) "استوری کن" else "Share Story",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { if (currentSlide < totalSlides - 1) currentSlide++ else onDismiss() },
                                colors = ButtonDefaults.buttonColors(containerColor = cPrimary),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (currentSlide < totalSlides - 1) (if (isFa) "بعدی" else "Next") else (if (isFa) "پایان" else "Done"),
                                    color = Color(0xFF050806),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (currentSlide < totalSlides - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF050806),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanStoryCardContainer(
    primaryColor: Color,
    secondaryColor: Color,
    stepBadge: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(30.dp), spotColor = primaryColor)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF14182B).copy(alpha = 0.94f),
                        Color(0xFF090C16).copy(alpha = 0.97f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.85f),
                        secondaryColor.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(30.dp)
            )
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
                    .background(primaryColor.copy(alpha = 0.16f))
                    .border(1.dp, primaryColor.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stepBadge,
                    color = Color.White,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
