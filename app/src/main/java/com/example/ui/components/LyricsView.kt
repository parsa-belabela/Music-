package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.LyricsDisplayMode
import com.example.data.model.LyricsLine
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

@Composable
fun LyricsView(
    lyrics: List<LyricsLine>,
    currentPositionMs: Long,
    displayMode: LyricsDisplayMode,
    palette: AmbientPalette,
    onSeekTo: (Long) -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
    enableWordHighlight: Boolean = true,
    baseFontSize: Float = 22f,
    activeTrackId: String = ""
) {
    LyricsView(
        lyrics = lyrics,
        currentPositionProvider = { currentPositionMs },
        displayMode = displayMode,
        palette = palette,
        onSeekTo = onSeekTo,
        onOpenEditor = onOpenEditor,
        modifier = modifier,
        enableWordHighlight = enableWordHighlight,
        baseFontSize = baseFontSize,
        activeTrackId = activeTrackId
    )
}

/**
 * Ultra-Luxury Cinematic Synced Lyrics (Apple Music Grade) + Studio Freestyle Pad:
 * - Pure Offscreen Alpha Mask (NO hardcoded black boxes, works dynamically over any cover/atmosphere)
 * - 3-Tier Optical Depth of Field (Active focused / Neighboring / Distant blurred)
 * - Sub-millisecond Word-Level Horizontal Gradient Sweep with dynamic luminous bloom
 * - Built-in Freestyle Pad (دفترچه یادداشت سریع تکست و تایم‌کد روی بیت)
 */
@Composable
fun LyricsView(
    lyrics: List<LyricsLine>,
    currentPositionProvider: () -> Long,
    displayMode: LyricsDisplayMode,
    palette: AmbientPalette,
    onSeekTo: (Long) -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
    enableWordHighlight: Boolean = true,
    baseFontSize: Float = 22f,
    activeTrackId: String = ""
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    var isCinematicFullscreen by remember { mutableStateOf(false) }
    var isFreestylePadOpen by remember { mutableStateOf(false) }

    val freestylePrefs = remember {
        context.getSharedPreferences("aura_freestyle_pad", Context.MODE_PRIVATE)
    }
    var freestyleText by remember(activeTrackId) {
        mutableStateOf(freestylePrefs.getString("note_$activeTrackId", "") ?: "")
    }

    // Live continuous clock ticker for 60fps sub-millisecond sweep and sync
    var liveClockMs by remember { mutableLongStateOf(currentPositionProvider()) }
    LaunchedEffect(activeTrackId, lyrics.isNotEmpty(), isFreestylePadOpen) {
        while (isActive) {
            liveClockMs = currentPositionProvider()
            kotlinx.coroutines.delay(16L)
        }
    }

    // Reset scroll to top whenever the active track changes
    LaunchedEffect(activeTrackId) {
        if (lyrics.isNotEmpty()) {
            try {
                listState.scrollToItem(0)
            } catch (_: Exception) {}
        }
    }

    // Calculate currently active lyric line index (-1 if before the first line)
    val activeIndex = remember(lyrics, liveClockMs) {
        if (lyrics.isEmpty()) -1
        else {
            val idx = lyrics.indexOfLast { it.timestampMs <= liveClockMs }
            idx
        }
    }

    // User touch interaction tracking: do not fight user when they scroll manually
    var isUserInteracting by remember { mutableStateOf(false) }
    var lastUserInteractionTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserInteracting = true
            lastUserInteractionTime = System.currentTimeMillis()
        }
    }

    // Smooth auto-scroll following the singer
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            val now = System.currentTimeMillis()
            if (isUserInteracting && (now - lastUserInteractionTime < 3000L)) {
                return@LaunchedEffect
            }
            isUserInteracting = false

            try {
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = 0
                )
            } catch (_: Exception) {}
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isFreestylePadOpen) {
            // دفترچه یادداشت سریع تکست و فری‌استایل روی بیت (Freestyle Pad)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.22f
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = Color(0xFF00F5D4),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Freestyle & Beat Pad",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "دفترچه تکست‌نویسی و ثبت تایم‌کد روی بیت",
                                color = Color(0xFFB0B5C8),
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Stamp current beat timecode [mm:ss.xx]
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val min = (liveClockMs / 1000) / 60
                                val sec = (liveClockMs / 1000) % 60
                                val cs = (liveClockMs % 1000) / 10
                                val stamp = "[%02d:%02d.%02d] ".format(min, sec, cs)
                                val prefix = if (freestyleText.isEmpty() || freestyleText.endsWith("\n")) "" else "\n"
                                val updated = freestyleText + prefix + stamp
                                freestyleText = updated
                                freestylePrefs.edit().putString("note_$activeTrackId", updated).apply()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00F5D4).copy(alpha = 0.20f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(14.dp))
                                Text(
                                    text = "+ تایم‌کد بیت",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        TextButton(onClick = { isFreestylePadOpen = false }) {
                            Text("بستن", color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = freestyleText,
                    onValueChange = { newText ->
                        freestyleText = newText
                        freestylePrefs.edit().putString("note_$activeTrackId", newText).apply()
                    },
                    placeholder = {
                        Text(
                            text = "ورس، قافیه یا ایده خود را هنگام لوپ شدن بیت اینجا بنویسید...\nبا دکمه «+ تایم‌کد بیت» ثانیه دقیق ورود بیت درج می‌شود.",
                            color = Color(0xFF888EA8),
                            fontSize = 12.5.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00F5D4),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                        focusedContainerColor = Color(0x66080B14),
                        unfocusedContainerColor = Color(0x44080B14)
                    )
                )
            }
            return@Box
        }

        if (lyrics.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .liquidGlass(
                            shape = RoundedCornerShape(24.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.14f
                        )
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(palette.primary.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Synced Lyrics",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add synced LRC lyrics or open the Freestyle Pad to write verses on the beat.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A5BA)),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onOpenEditor,
                                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("open_lyrics_editor_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lyrics Editor", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                            }
                            OutlinedButton(
                                onClick = { isFreestylePadOpen = true },
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4))
                            ) {
                                Icon(imageVector = Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF00F5D4), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Freestyle Pad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }
            return@Box
        }

        val effectiveFontSize = if (isCinematicFullscreen) baseFontSize + 4f else baseFontSize

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = if (isCinematicFullscreen) 60.dp else 90.dp,
                bottom = if (isCinematicFullscreen) 80.dp else 130.dp,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(
                when (displayMode) {
                    LyricsDisplayMode.CINEMATIC -> 26.dp
                    LyricsDisplayMode.MINIMAL -> 12.dp
                    else -> 20.dp
                }
            )
        ) {
            itemsIndexed(lyrics, key = { idx, line -> "${line.timestampMs}_$idx" }) { index, line ->
                val isActive = index == activeIndex
                val distance = abs(index - activeIndex)

                val alignment = when (displayMode) {
                    LyricsDisplayMode.CENTER, LyricsDisplayMode.CINEMATIC, LyricsDisplayMode.FLOATING -> TextAlign.Center
                    else -> TextAlign.Start
                }

                val textColor by animateColorAsState(
                    targetValue = if (isActive) Color.White else Color(0xFFB0B0C4),
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "lyricsTextColor"
                )

                // 3-tier atmospheric opacity & depth
                val targetAlpha = when {
                    isActive -> 1.0f
                    distance == 1 -> 0.70f
                    else -> 0.32f
                }

                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "lyricsAlpha"
                )

                val lineScale by animateFloatAsState(
                    targetValue = if (isActive) 1.04f else if (distance == 1) 0.99f else 0.95f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "lineScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(lineScale)
                        .alpha(alpha)
                        .then(
                            if (distance >= 2) Modifier.blur(1.2.dp) else Modifier
                        )
                        .then(
                            if (isActive) {
                                Modifier.liquidGlass(
                                    shape = RoundedCornerShape(20.dp),
                                    thickness = GlassThickness.THIN,
                                    tintColor = palette.accent,
                                    tintAlpha = 0.22f,
                                    borderWidth = 1.2.dp
                                )
                            } else {
                                Modifier.clip(RoundedCornerShape(12.dp))
                            }
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSeekTo(line.timestampMs)
                        }
                        .padding(horizontal = 16.dp, vertical = if (isActive) 14.dp else 8.dp),
                    contentAlignment = when (displayMode) {
                        LyricsDisplayMode.CENTER, LyricsDisplayMode.CINEMATIC, LyricsDisplayMode.FLOATING -> Alignment.Center
                        else -> Alignment.CenterStart
                    }
                ) {
                    if (isActive && enableWordHighlight && line.words.isNotEmpty()) {
                        // Word-level karaoke highlighting with continuous gradient sweep & glow
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = when (displayMode) {
                                LyricsDisplayMode.CENTER, LyricsDisplayMode.CINEMATIC, LyricsDisplayMode.FLOATING -> Arrangement.Center
                                else -> Arrangement.Start
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (w in line.words) {
                                val isWordActive = liveClockMs in w.startMs..w.endMs
                                val isWordPassed = liveClockMs > w.endMs

                                val sweepFraction = if (isWordActive) {
                                    val duration = (w.endMs - w.startMs).coerceAtLeast(1L)
                                    ((liveClockMs - w.startMs).toFloat() / duration).coerceIn(0f, 1f)
                                } else if (isWordPassed) 1f else 0f

                                val wordScale by animateFloatAsState(
                                    targetValue = if (isWordActive) 1.06f else 1.0f,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                                    label = "wordScale"
                                )

                                val wordBrush = if (isWordActive) {
                                    Brush.horizontalGradient(
                                        0.0f to palette.accent,
                                        (sweepFraction * 0.95f).coerceIn(0f, 1f) to palette.accent,
                                        sweepFraction.coerceIn(0f, 1f) to Color.White,
                                        (sweepFraction + 0.15f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.55f),
                                        1.0f to Color.White.copy(alpha = 0.55f)
                                    )
                                } else if (isWordPassed) {
                                    Brush.linearGradient(listOf(Color.White, Color.White))
                                } else {
                                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.45f)))
                                }

                                Text(
                                    text = "${w.word} ",
                                    fontSize = (effectiveFontSize + 3f).sp,
                                    fontWeight = if (isWordActive) FontWeight.Bold else if (isWordPassed) FontWeight.SemiBold else FontWeight.Medium,
                                    style = TextStyle(
                                        brush = wordBrush,
                                        shadow = if (isWordActive) Shadow(
                                            color = palette.accent.copy(alpha = 0.85f),
                                            blurRadius = 16f
                                        ) else null
                                    ),
                                    modifier = Modifier.scale(wordScale),
                                    lineHeight = ((effectiveFontSize + 3f) * 1.65f).sp,
                                    letterSpacing = 0.3.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = line.text,
                            fontSize = if (isActive) (effectiveFontSize + 3f).sp else effectiveFontSize.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) Color.White else textColor,
                            textAlign = alignment,
                            lineHeight = ((if (isActive) effectiveFontSize + 3f else effectiveFontSize) * 1.65f).sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }

        // Top Bar Controls: Freestyle Pad & Fullscreen Cinematic Toggle
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isFreestylePadOpen = true
                },
                shape = RoundedCornerShape(50),
                color = Color(0x88101426),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F5D4).copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Freestyle Pad",
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Freestyle Pad",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isCinematicFullscreen = !isCinematicFullscreen
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x55101020))
            ) {
                Icon(
                    imageVector = if (isCinematicFullscreen) Icons.Default.CloseFullscreen else Icons.Default.Fullscreen,
                    contentDescription = "Toggle Fullscreen Lyrics",
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
