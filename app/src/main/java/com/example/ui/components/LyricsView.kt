package com.example.ui.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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

    val latestPositionProvider by rememberUpdatedState(currentPositionProvider)
    var liveClockMs by remember { mutableLongStateOf(latestPositionProvider()) }
    LaunchedEffect(activeTrackId, lyrics.size, isFreestylePadOpen) {
        while (isActive) {
            liveClockMs = latestPositionProvider()
            delay(32L)
        }
    }

    val effectiveLyrics = remember(lyrics) {
        if (lyrics.size > 1 && lyrics.all { it.timestampMs == 0L }) {
            lyrics.mapIndexed { idx, item ->
                item.copy(timestampMs = idx * 3500L)
            }
        } else {
            lyrics
        }
    }

    LaunchedEffect(activeTrackId) {
        if (effectiveLyrics.isNotEmpty()) {
            try {
                listState.scrollToItem(0)
            } catch (_: Exception) {}
        }
    }

    val activeIndex = remember(effectiveLyrics, liveClockMs) {
        if (effectiveLyrics.isEmpty()) -1
        else effectiveLyrics.indexOfLast { it.timestampMs <= liveClockMs }.coerceAtLeast(0)
    }

    var isUserInteracting by remember { mutableStateOf(false) }
    var lastUserInteractionTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserInteracting = true
            lastUserInteractionTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex in effectiveLyrics.indices) {
            val now = System.currentTimeMillis()
            if (isUserInteracting && (now - lastUserInteractionTime < 3000L)) {
                return@LaunchedEffect
            }
            isUserInteracting = false
            try {
                listState.animateScrollToItem(
                    index = (activeIndex - 1).coerceAtLeast(0),
                    scrollOffset = 0
                )
            } catch (_: Exception) {}
        }
    }

    val cardTint = remember(palette.primary, palette.isMonochrome) {
        if (palette.isMonochrome) Color(0xFF242424)
        else palette.primary.copy(alpha = 0.28f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(cardTint)
    ) {
        if (isFreestylePadOpen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AuraSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = AuraSpotifyGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Freestyle & Beat Pad",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "دفترچه تکست‌نویسی و ثبت تایم‌کد روی بیت",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            shape = RoundedCornerShape(50),
                            color = AuraSpotifyGreen
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "+ تایم‌کد",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        TextButton(onClick = { isFreestylePadOpen = false }) {
                            Text(
                                "بستن",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            )
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
                            text = "ورس، قافیه یا ایده خود را اینجا بنویسید...",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraSpotifyGreen,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = AuraSurfaceElevated,
                        unfocusedContainerColor = AuraSurfaceElevated
                    )
                )
            }
            return@Box
        }

        if (lyrics.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Couldn't load the lyrics for this song",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "You can add synced LRC lyrics manually or open the Freestyle Pad.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f)),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onOpenEditor,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("open_lyrics_editor_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Lyrics", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                        OutlinedButton(
                            onClick = { isFreestylePadOpen = true },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Freestyle Pad",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }
            return@Box
        }

        val effectiveFontSize = if (isCinematicFullscreen) baseFontSize + 3f else baseFontSize

        // Spotify Iconic Synced Lyrics List
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 20.dp,
                bottom = 110.dp,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(effectiveLyrics, key = { idx, line -> "${line.timestampMs}_$idx" }) { index, line ->
                val isActive = index == activeIndex
                val isPassed = index < activeIndex

                val textAlignment = when (displayMode) {
                    LyricsDisplayMode.CENTER -> TextAlign.Center
                    else -> TextAlign.Start
                }

                val targetColor = when {
                    isActive -> Color.White
                    isPassed -> Color.White.copy(alpha = 0.85f)
                    else -> Color.White.copy(alpha = 0.35f)
                }

                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "spotifyLyricColor"
                )

                val lineScale by animateFloatAsState(
                    targetValue = if (isActive) 1.03f else 1.0f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "spotifyLyricScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = lineScale
                            scaleY = lineScale
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSeekTo(line.timestampMs)
                        }
                        .padding(vertical = 4.dp),
                    contentAlignment = if (textAlignment == TextAlign.Center) Alignment.Center else Alignment.CenterStart
                ) {
                    if (isActive && enableWordHighlight && line.words.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (textAlignment == TextAlign.Center) Arrangement.Center else Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (w in line.words) {
                                val isWordActive = liveClockMs in w.startMs..w.endMs
                                val isWordPassed = liveClockMs > w.endMs

                                val sweepFraction = if (isWordActive) {
                                    val duration = (w.endMs - w.startMs).coerceAtLeast(1L)
                                    ((liveClockMs - w.startMs).toFloat() / duration).coerceIn(0f, 1f)
                                } else if (isWordPassed) 1f else 0f

                                val wordBrush = if (isWordActive) {
                                    Brush.horizontalGradient(
                                        0.0f to Color.White,
                                        sweepFraction.coerceIn(0f, 1f) to Color.White,
                                        (sweepFraction + 0.1f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.4f),
                                        1.0f to Color.White.copy(alpha = 0.4f)
                                    )
                                } else if (isWordPassed) {
                                    Brush.linearGradient(listOf(Color.White, Color.White))
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.4f),
                                            Color.White.copy(alpha = 0.4f)
                                        )
                                    )
                                }

                                Text(
                                    text = "${w.word} ",
                                    fontSize = (effectiveFontSize + 2f).sp,
                                    fontWeight = FontWeight.Bold,
                                    style = TextStyle(brush = wordBrush),
                                    lineHeight = ((effectiveFontSize + 2f) * 1.4f).sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = line.text,
                            fontSize = (effectiveFontSize + 1f).sp,
                            fontWeight = FontWeight.Bold,
                            color = animatedColor,
                            textAlign = textAlignment,
                            lineHeight = ((effectiveFontSize + 1f) * 1.38f).sp
                        )
                    }
                }
            }
        }

        // Top-Right Minimal Spotify Utility Pills (Editor, Freestyle Pad, Expand)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenEditor,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Lyrics",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp)
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isFreestylePadOpen = true
                },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f))
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Freestyle Pad",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp)
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isCinematicFullscreen = !isCinematicFullscreen
                },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f))
            ) {
                Icon(
                    imageVector = if (isCinematicFullscreen) Icons.Default.CloseFullscreen else Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen Lyrics",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
