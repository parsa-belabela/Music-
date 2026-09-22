package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.LyricsDisplayMode
import com.example.data.model.LyricsLine
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass

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
 * Apple Music level cinematic synced lyrics:
 * - Fluid line focus with dynamic scale, opacity, and soft glow
 * - Tactile tap-to-seek without lag
 * - Top & Bottom gradient fade masks for infinite depth
 * - Respects user scrolling interaction
 * - Gorgeous glass empty state with quick editor access
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
    val listState = rememberLazyListState()

    val currentPos = currentPositionProvider()

    val activeIndex = remember(lyrics, currentPos) {
        if (lyrics.isEmpty()) -1
        else {
            val idx = lyrics.indexOfLast { it.timestampMs <= currentPos }
            if (idx == -1) 0 else idx
        }
    }

    // Auto-scroll when active index changes, unless user is actively dragging
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices && !listState.isScrollInProgress) {
            val targetScroll = (activeIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll, scrollOffset = -80)
        }
    }

    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.12f
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
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
                        text = "Add lyrics or import an LRC file to experience cinematic karaoke.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A5BA)),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onOpenEditor,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("open_lyrics_editor_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Lyrics Editor", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 100.dp, bottom = 140.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(
                when (displayMode) {
                    LyricsDisplayMode.CINEMATIC -> 26.dp
                    LyricsDisplayMode.MINIMAL -> 12.dp
                    else -> 18.dp
                }
            )
        ) {
            itemsIndexed(lyrics, key = { idx, line -> "${line.timestampMs}_$idx" }) { index, line ->
                val isActive = index == activeIndex
                val isPast = index < activeIndex

                val alignment = when (displayMode) {
                    LyricsDisplayMode.CENTER, LyricsDisplayMode.CINEMATIC, LyricsDisplayMode.FLOATING -> TextAlign.Center
                    else -> TextAlign.Start
                }

                val textColor by animateColorAsState(
                    targetValue = if (isActive) Color.White else Color(0xFF9E9EB2),
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "lyricsTextColor"
                )

                val alpha by animateFloatAsState(
                    targetValue = when {
                        isActive -> 1.0f
                        isPast -> 0.38f
                        else -> 0.52f
                    },
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "lyricsAlpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alpha)
                        .then(
                            if (isActive) {
                                Modifier.liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    thickness = GlassThickness.THIN,
                                    tintColor = palette.accent,
                                    tintAlpha = 0.25f,
                                    borderWidth = 1.2.dp
                                )
                            } else {
                                Modifier.clip(RoundedCornerShape(12.dp))
                            }
                        )
                        .clickable { onSeekTo(line.timestampMs) }
                        .padding(horizontal = 16.dp, vertical = if (isActive) 14.dp else 8.dp),
                    contentAlignment = when (displayMode) {
                        LyricsDisplayMode.CENTER, LyricsDisplayMode.CINEMATIC, LyricsDisplayMode.FLOATING -> Alignment.Center
                        else -> Alignment.CenterStart
                    }
                ) {
                    if (isActive && enableWordHighlight && line.words.isNotEmpty()) {
                        // Word-level karaoke highlighting with continuous gradient sweep & glow
                        val curPos = currentPositionProvider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = when (displayMode) {
                                LyricsDisplayMode.CENTER, LyricsDisplayMode.CINEMATIC, LyricsDisplayMode.FLOATING -> Arrangement.Center
                                else -> Arrangement.Start
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (w in line.words) {
                                val isWordActive = curPos in w.startMs..w.endMs
                                val isWordPassed = curPos > w.endMs

                                val sweepFraction = if (isWordActive) {
                                    val duration = (w.endMs - w.startMs).coerceAtLeast(1L)
                                    ((curPos - w.startMs).toFloat() / duration).coerceIn(0f, 1f)
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
                                    fontSize = (baseFontSize + 2f).sp,
                                    fontWeight = if (isWordActive) FontWeight.Black else if (isWordPassed) FontWeight.Bold else FontWeight.SemiBold,
                                    style = androidx.compose.ui.text.TextStyle(
                                        brush = wordBrush,
                                        shadow = if (isWordActive) androidx.compose.ui.graphics.Shadow(
                                            color = palette.accent.copy(alpha = 0.8f),
                                            blurRadius = 14f
                                        ) else null
                                    ),
                                    modifier = Modifier.scale(wordScale),
                                    lineHeight = ((baseFontSize + 2f) * 1.45f).sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = line.text,
                            fontSize = if (isActive) (baseFontSize + 2f).sp else baseFontSize.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) Color.White else textColor,
                            textAlign = alignment,
                            lineHeight = ((if (isActive) baseFontSize + 2f else baseFontSize) * 1.45f).sp,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }

        // Top & Bottom gradient edge masks for cinematic depth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF06060A), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xEE06060A))
                    )
                )
        )
    }
}
