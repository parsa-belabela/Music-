package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.OnlineMusicRepository
import com.example.audio.AmbientPalette
import com.example.buildAuraCoverImageRequest
import com.example.data.model.LyricsDisplayMode
import com.example.data.model.LyricsLine
import com.example.data.model.Track
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.util.SongShareCardGenerator
import com.example.util.StoryBackgroundStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

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
    activeTrackId: String = "",
    activeTrack: Track? = null,
    onShareLyricsToStory: ((List<String>) -> Unit)? = null
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
        activeTrackId = activeTrackId,
        activeTrack = activeTrack,
        onShareLyricsToStory = onShareLyricsToStory
    )
}

@OptIn(ExperimentalFoundationApi::class)
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
    activeTrackId: String = "",
    activeTrack: Track? = null,
    onShareLyricsToStory: ((List<String>) -> Unit)? = null
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var isCinematicFullscreen by remember { mutableStateOf(false) }
    var isFreestylePadOpen by remember { mutableStateOf(false) }

    var isSelectionMode by remember(activeTrackId) { mutableStateOf(false) }
    val selectedIndices = remember(activeTrackId) { mutableStateListOf<Int>() }
    var showStoryShareDialog by remember { mutableStateOf(false) }

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
        selectedIndices.clear()
        isSelectionMode = false
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

    LaunchedEffect(activeIndex, isSelectionMode) {
        if (isSelectionMode) return@LaunchedEffect
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

    val primaryTint = remember(palette.primary, palette.isMonochrome) {
        if (palette.isMonochrome) AuraSpotifyGreen else palette.primary
    }
    val accentTint = remember(palette.accent, palette.isMonochrome) {
        if (palette.isMonochrome) Color(0xFF14833B) else palette.accent
    }

    val liquidGlassBackground = remember(primaryTint, accentTint) {
        Brush.verticalGradient(
            colors = listOf(
                primaryTint.copy(alpha = 0.26f),
                accentTint.copy(alpha = 0.14f),
                Color(0xF2090C15)
            )
        )
    }

    fun toggleLineSelection(index: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index)
            if (selectedIndices.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            if (selectedIndices.size < 4) {
                selectedIndices.add(index)
                selectedIndices.sort()
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF090C15))
            .background(liquidGlassBackground)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        primaryTint.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onOpenEditor,
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("open_lyrics_editor_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "افزودن لیریک",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Surface(
                            onClick = { isFreestylePadOpen = true },
                            shape = RoundedCornerShape(50),
                            color = AuraSurfaceElevated.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "تکست‌پد",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }
            return@Box
        }

        val effectiveFontSize = if (isCinematicFullscreen) baseFontSize + 3f else baseFontSize + 1f

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 58.dp,
                bottom = 130.dp,
                start = 18.dp,
                end = 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(effectiveLyrics, key = { idx, line -> "${line.timestampMs}_$idx" }) { index, line ->
                val isActive = index == activeIndex && !isSelectionMode
                val isPassed = index < activeIndex && !isSelectionMode
                val isSelected = selectedIndices.contains(index)

                val textAlignment = when (displayMode) {
                    LyricsDisplayMode.CENTER -> TextAlign.Center
                    else -> TextAlign.Start
                }

                val targetColor = when {
                    isSelected -> Color.White
                    isSelectionMode -> Color.White.copy(alpha = 0.42f)
                    isActive -> Color.White
                    isPassed -> Color.White.copy(alpha = 0.55f)
                    else -> Color.White.copy(alpha = 0.32f)
                }

                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    label = "appleMusicLyricColor"
                )

                val lineScale by animateFloatAsState(
                    targetValue = when {
                        isSelected -> 1.02f
                        isActive -> 1.04f
                        else -> 0.98f
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "appleMusicLyricScale"
                )

                val rowBgBrush = when {
                    isSelected -> Brush.horizontalGradient(
                        listOf(
                            primaryTint.copy(alpha = 0.34f),
                            accentTint.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                    isActive -> Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.03f)
                        )
                    )
                    else -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = lineScale
                            scaleY = lineScale
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(rowBgBrush)
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(primaryTint.copy(alpha = 0.85f), Color.White.copy(alpha = 0.25f))
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            } else if (isActive) {
                                Modifier.border(
                                    width = 0.8.dp,
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (isSelectionMode) {
                                    toggleLineSelection(index)
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSeekTo(line.timestampMs)
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                }
                                toggleLineSelection(index)
                            }
                        )
                        .padding(
                            horizontal = if (isSelected || isActive) 14.dp else 8.dp,
                            vertical = if (isSelected || isActive) 10.dp else 6.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (textAlignment == TextAlign.Center && !isSelectionMode) Arrangement.Center else Arrangement.Start
                ) {
                    if (isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) primaryTint else Color.White.copy(alpha = 0.10f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else if (isActive) {
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .width(3.5.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(50))
                                .background(primaryTint)
                        )
                    }

                    Box(
                        modifier = Modifier.weight(1f, fill = (textAlignment != TextAlign.Center || isSelectionMode)),
                        contentAlignment = if (textAlignment == TextAlign.Center && !isSelectionMode) Alignment.Center else Alignment.CenterStart
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
                                        fontWeight = FontWeight.ExtraBold,
                                        style = TextStyle(
                                            brush = wordBrush,
                                            shadow = Shadow(
                                                color = primaryTint.copy(alpha = 0.55f),
                                                offset = Offset(0f, 2f),
                                                blurRadius = 14f
                                            )
                                        ),
                                        lineHeight = ((effectiveFontSize + 2f) * 1.38f).sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = line.text,
                                fontSize = if (isActive || isSelected) (effectiveFontSize + 1.5f).sp else effectiveFontSize.sp,
                                fontWeight = if (isActive || isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = animatedColor,
                                textAlign = if (isSelectionMode) TextAlign.Start else textAlignment,
                                style = TextStyle(
                                    shadow = if (isActive || isSelected) {
                                        Shadow(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            offset = Offset(0f, 3f),
                                            blurRadius = 12f
                                        )
                                    } else null
                                ),
                                lineHeight = (effectiveFontSize * 1.38f).sp
                            )
                        }
                    }
                }
            }
        }

        // Top-Right Action Pill (Select Lyrics for Story, Edit, Freestyle Pad, Fullscreen)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xD8141722))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isSelectionMode = !isSelectionMode
                    if (!isSelectionMode) {
                        selectedIndices.clear()
                    } else if (selectedIndices.isEmpty() && activeIndex in effectiveLyrics.indices) {
                        selectedIndices.add(activeIndex)
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.IosShare,
                    contentDescription = "Share Lyrics to Story",
                    tint = if (isSelectionMode) primaryTint else Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = onOpenEditor,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Lyrics",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isFreestylePadOpen = true
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Freestyle Pad",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(15.dp)
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isCinematicFullscreen = !isCinematicFullscreen
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isCinematicFullscreen) Icons.Default.CloseFullscreen else Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen Lyrics",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Floating Bottom Action Bar when lines are selected for Story
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xF0151926),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(primaryTint.copy(alpha = 0.7f), Color.White.copy(alpha = 0.2f))
                    )
                ),
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (selectedIndices.isEmpty()) "خطوط دلخواه را لمس کنید" else "${selectedIndices.size} از ۴ خط انتخاب شد",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "آماده ساخت کارت استوری ۹:۱۶",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 10.5.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                selectedIndices.clear()
                                isSelectionMode = false
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "لغو",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                val chosenLines = selectedIndices
                                    .sorted()
                                    .mapNotNull { effectiveLyrics.getOrNull(it)?.text?.trim() }
                                    .filter { it.isNotEmpty() }

                                if (onShareLyricsToStory != null) {
                                    onShareLyricsToStory(chosenLines)
                                    isSelectionMode = false
                                    selectedIndices.clear()
                                } else {
                                    showStoryShareDialog = true
                                }
                            },
                            enabled = selectedIndices.isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryTint,
                                contentColor = Color.Black,
                                disabledContainerColor = Color.White.copy(alpha = 0.12f),
                                disabledContentColor = Color.White.copy(alpha = 0.4f)
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ساخت استوری",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStoryShareDialog) {
        val chosenLines = selectedIndices
            .sorted()
            .mapNotNull { effectiveLyrics.getOrNull(it)?.text?.trim() }
            .filter { it.isNotEmpty() }

        val fallbackTrack = remember(activeTrack, activeTrackId) {
            activeTrack ?: OnlineMusicRepository.findTrackById(activeTrackId) ?: Track(
                id = activeTrackId.ifBlank { "aura_track" },
                title = "Aura Music",
                artist = "Aura Artist",
                album = "Single",
                durationMs = 208000L,
                streamUrl = "",
                artworkUri = ""
            )
        }

        InteractiveStoryPreviewDialog(
            track = fallbackTrack,
            palette = palette,
            selectedLyricLines = chosenLines,
            onDismiss = {
                showStoryShareDialog = false
                isSelectionMode = false
                selectedIndices.clear()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InteractiveStoryPreviewDialog(
    track: Track,
    palette: AmbientPalette,
    selectedLyricLines: List<String> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var styleIndex by remember { mutableIntStateOf(0) }
    var isSharing by remember { mutableStateOf(false) }
    val currentStyle = remember(styleIndex) { StoryBackgroundStyle.fromIndex(styleIndex) }

    val coverUrl = remember(track) {
        track.coverBig?.takeIf { it.isNotBlank() }
            ?: OnlineMusicRepository.getBigCoverUrlForTrack(
                trackId = track.id,
                fallbackUrl = track.artworkUri,
                artistName = track.artist
            )
    }

    val coverRequest = remember(track.id, coverUrl) {
        buildAuraCoverImageRequest(
            context = context,
            trackId = track.id,
            coverUrl = coverUrl,
            isLarge = true,
            allowHardware = false
        )
    }

    Dialog(
        onDismissRequest = { if (!isSharing) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val targetCardHeight = remember(maxHeight) {
                (maxHeight - 170.dp).coerceIn(280.dp, 450.dp)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Top Header Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            styleIndex = (styleIndex + 1) % StoryBackgroundStyle.entries.size
                        },
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF1A2035),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = currentStyle.labelFa,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 9:16 Interactive Story Card Preview
                Box(
                    modifier = Modifier
                        .height(targetCardHeight)
                        .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                styleIndex = (styleIndex + 1) % StoryBackgroundStyle.entries.size
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Background Layer
                    when (currentStyle) {
                        StoryBackgroundStyle.DYNAMIC_AMBIENT_BLUR -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF090C15))
                            ) {
                                AsyncImage(
                                    model = coverRequest,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(65.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF090C15).copy(alpha = 0.62f))
                                )
                            }
                        }
                        StoryBackgroundStyle.MIDNIGHT_NEON_AURA -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF090C15))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF7C3AED).copy(alpha = 0.35f),
                                                Color.Transparent
                                            ),
                                            center = Offset(120f, 180f),
                                            radius = 700f
                                        )
                                    )
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF2563EB).copy(alpha = 0.30f),
                                                Color.Transparent
                                            ),
                                            center = Offset(650f, 1100f),
                                            radius = 750f
                                        )
                                    )
                            )
                        }
                        StoryBackgroundStyle.CRIMSON_AFTER_HOURS -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF090508))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF991B1B).copy(alpha = 0.40f),
                                                Color.Transparent
                                            ),
                                            radius = 800f
                                        )
                                    )
                            )
                        }
                    }

                    // Centerpiece Floating Liquid Glass Card
                    val hasLyrics = selectedLyricLines.isNotEmpty()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .shadow(24.dp, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF1A2035).copy(alpha = 0.80f),
                                        Color(0xFF0D111D).copy(alpha = 0.90f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.28f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(if (hasLyrics) 16.dp else 18.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        val isTitlePersian = remember(track.title) { isPersianStoryText(track.title) }
                        val isArtistPersian = remember(track.artist) { isPersianStoryText(track.artist) }

                        if (hasLyrics) {
                            // Compact Header + Featured Lyrics Quote Layout
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = coverRequest,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .shadow(10.dp, RoundedCornerShape(14.dp))
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontFamily = if (isTitlePersian) StoryVazirmatnFontFamily else StoryIosSansFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = track.artist,
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 11.5.sp,
                                        fontFamily = if (isArtistPersian) StoryVazirmatnFontFamily else StoryIosSansFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.10f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedLyricLines.take(4).forEach { lineText ->
                                    val isPersianLine = isPersianStoryText(lineText)
                                    Text(
                                        text = lineText,
                                        color = Color.White,
                                        fontSize = if (isPersianLine) 15.5.sp else 15.sp,
                                        fontFamily = if (isPersianLine) StoryVazirmatnFontFamily else StoryIosSansFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = FontStyle.Normal,
                                        lineHeight = if (isPersianLine) 25.sp else 22.sp,
                                        textAlign = if (isPersianLine) TextAlign.Right else TextAlign.Left,
                                        style = TextStyle(
                                            textDirection = if (isPersianLine) androidx.compose.ui.text.style.TextDirection.Rtl else androidx.compose.ui.text.style.TextDirection.Ltr,
                                            shadow = Shadow(
                                                color = Color.Black.copy(alpha = 0.72f),
                                                offset = Offset(0f, 2f),
                                                blurRadius = 8f
                                            )
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        } else {
                            // Full Square Cover Art Layout
                            AsyncImage(
                                model = coverRequest,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .shadow(16.dp, RoundedCornerShape(20.dp))
                                    .clip(RoundedCornerShape(20.dp))
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = track.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = if (isTitlePersian) StoryVazirmatnFontFamily else StoryIosSansFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = track.artist,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 13.sp,
                                fontFamily = if (isArtistPersian) StoryVazirmatnFontFamily else StoryIosSansFontFamily,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Bottom Aura Branding Pill
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "AURA MUSIC",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.2.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Share Action Button (Attached directly under card)
                Button(
                    onClick = {
                        if (isSharing) return@Button
                        isSharing = true
                        scope.launch {
                            val uri = SongShareCardGenerator.generateSongCardBitmap(
                                context = context,
                                track = track,
                                palette = palette,
                                selectedLyricLines = selectedLyricLines,
                                styleIndex = styleIndex
                            )
                            isSharing = false
                            if (uri != null) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "اشتراک‌گذاری استوری Aura Music")
                                )
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اشتراک‌گذاری در استوری (کیفیت HD)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
