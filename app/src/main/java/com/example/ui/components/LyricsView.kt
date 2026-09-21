package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.LyricsDisplayMode
import com.example.data.model.LyricsLine
import kotlinx.coroutines.launch

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
    baseFontSize: Float = 20f,
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
    baseFontSize: Float = 20f,
    activeTrackId: String = ""
) {
    val listState = rememberLazyListState()

    // Derived active index only changes when line transition happens, saving 95% of recompositions
    val activeIndex by remember(lyrics) {
        derivedStateOf {
            val pos = currentPositionProvider()
            if (lyrics.isEmpty()) -1
            else {
                val idx = lyrics.indexOfLast { it.timestampMs <= pos }
                if (idx == -1) 0 else idx
            }
        }
    }

    // Smooth scroll ONLY when active index actually changes
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = palette.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Synced Lyrics Found",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Add lyrics, import an LRC file, or live sync to the beat.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A0B0)),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenEditor,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Lyrics Editor")
                }
            }
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 120.dp, horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(
            when (displayMode) {
                LyricsDisplayMode.CINEMATIC -> 28.dp
                LyricsDisplayMode.MINIMAL -> 12.dp
                else -> 18.dp
            }
        )
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            val isPast = index < activeIndex

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.08f else 1.0f,
                animationSpec = tween(250),
                label = "scale"
            )

            val alpha by animateFloatAsState(
                targetValue = when {
                    isActive -> 1.0f
                    isPast -> 0.35f
                    else -> 0.55f
                },
                animationSpec = tween(250),
                label = "alpha"
            )

            val textColor by animateColorAsState(
                targetValue = if (isActive) palette.accent else Color.White,
                animationSpec = tween(250),
                label = "textColor"
            )

            val alignment = when (displayMode) {
                LyricsDisplayMode.CENTER, LyricsDisplayMode.CINEMATIC, LyricsDisplayMode.FLOATING -> TextAlign.Center
                else -> TextAlign.Start
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSeekTo(line.timestampMs) }
                    .padding(vertical = 4.dp),
                contentAlignment = when (displayMode) {
                    LyricsDisplayMode.CENTER, LyricsDisplayMode.CINEMATIC, LyricsDisplayMode.FLOATING -> Alignment.Center
                    else -> Alignment.CenterStart
                }
            ) {
                if (isActive && enableWordHighlight && line.words.isNotEmpty()) {
                    // Word-level karaoke highlighting
                    val curPos = currentPositionProvider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = when (displayMode) {
                            LyricsDisplayMode.CENTER, LyricsDisplayMode.CINEMATIC, LyricsDisplayMode.FLOATING -> Arrangement.Center
                            else -> Arrangement.Start
                        }
                    ) {
                        for (w in line.words) {
                            val isWordActive = curPos in w.startMs..w.endMs
                            val isWordPassed = curPos > w.endMs
                            val wordColor = when {
                                isWordActive -> palette.secondary
                                isWordPassed -> palette.primary
                                else -> Color.White.copy(alpha = 0.5f)
                            }

                            Text(
                                text = "${w.word} ",
                                fontSize = (baseFontSize * (if (isActive) 1.1f else 1.0f)).sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = wordColor
                            )
                        }
                    }
                } else {
                    Text(
                        text = line.text,
                        fontSize = (baseFontSize * (if (isActive) 1.12f else 1.0f)).sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        textAlign = alignment,
                        lineHeight = (baseFontSize * 1.4f).sp
                    )
                }
            }
        }
    }
}
