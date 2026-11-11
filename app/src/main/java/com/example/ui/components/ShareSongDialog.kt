package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.OnlineMusicRepository
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.LyricsLine
import com.example.data.model.Track
import com.example.rememberAuraCoverRequest
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.util.SongShareCardGenerator
import com.example.util.StoryBackgroundStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ShareSongDialog(
    track: Track,
    palette: AmbientPalette,
    language: AppLanguage,
    lyrics: List<LyricsLine> = emptyList(),
    initialSelectedLyrics: List<String> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isFa = language == AppLanguage.PERSIAN

    var isGeneratingCard by remember { mutableStateOf(false) }
    var selectedStyleIdx by remember { mutableIntStateOf(0) }
    val currentStyle = StoryBackgroundStyle.fromIndex(selectedStyleIdx)

    var isLyricsPickerOpen by remember { mutableStateOf(false) }

    val validLyrics = remember(lyrics) {
        lyrics.map { it.text.trim() }.filter { it.isNotEmpty() }
    }

    val selectedLyricIndices = remember(track.id) {
        mutableStateListOf<Int>().apply {
            if (initialSelectedLyrics.isNotEmpty() && validLyrics.isNotEmpty()) {
                initialSelectedLyrics.forEach { initLine ->
                    val foundIdx = validLyrics.indexOfFirst { it.equals(initLine.trim(), ignoreCase = true) }
                    if (foundIdx != -1 && !contains(foundIdx) && size < 4) {
                        add(foundIdx)
                    }
                }
                sort()
            }
        }
    }

    val activeSelectedLyrics: List<String> = remember(selectedLyricIndices.toList(), validLyrics, initialSelectedLyrics) {
        if (selectedLyricIndices.isNotEmpty()) {
            selectedLyricIndices.sorted().mapNotNull { validLyrics.getOrNull(it) }.take(4)
        } else {
            initialSelectedLyrics.map { it.trim() }.filter { it.isNotEmpty() }.take(4)
        }
    }

    val hasSelectedLyrics = activeSelectedLyrics.isNotEmpty()

    // GraphicsLayer restricted strictly to the 9:16 Story Card composable
    val storyCardGraphicsLayer = rememberGraphicsLayer()

    val bigCoverUrl = remember(track.id, track.artworkUri, track.artist) {
        track.coverBig?.takeIf { it.isNotBlank() }
            ?: OnlineMusicRepository.getBigCoverUrlForTrack(
                trackId = track.id,
                fallbackUrl = track.artworkUri,
                artistName = track.artist
            )
    }

    // Preload software Bitmap (allowHardware(false)) via Coil so software rendering never crashes
    var softwareCoverBitmap by remember(track.id, bigCoverUrl) {
        mutableStateOf<Bitmap?>(null)
    }
    var blurredBackgroundBitmap by remember(track.id, bigCoverUrl) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(track.id, bigCoverUrl) {
        val loaded = SongShareCardGenerator.loadSoftwareCoverBitmap(context, track)
        if (loaded != null) {
            softwareCoverBitmap = loaded
            blurredBackgroundBitmap = withContext(Dispatchers.Default) {
                SongShareCardGenerator.createHeavyBlurredBitmap(
                    source = loaded,
                    targetW = 180,
                    targetH = 320,
                    radius = 28
                )
            }
        }
    }

    val primaryTint = remember(palette.primary, palette.isMonochrome) {
        if (palette.isMonochrome) AuraSpotifyGreen else palette.primary
    }

    val cycleStyle: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selectedStyleIdx = (selectedStyleIdx + 1) % StoryBackgroundStyle.entries.size
    }

    val shareStoryCardImage: () -> Unit = {
        if (!isGeneratingCard) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isGeneratingCard = true
            scope.launch {
                val uri = try {
                    SongShareCardGenerator.generateSongCardBitmap(
                        context = context,
                        track = track,
                        palette = palette,
                        selectedLyricLines = activeSelectedLyrics,
                        lang = language,
                        styleIndex = currentStyle.index,
                        preloadedCover = softwareCoverBitmap
                    )
                } catch (_: Throwable) {
                    null
                }

                isGeneratingCard = false
                if (uri != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "${track.title} — ${track.artist}\n#AuraMusic"
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            if (isFa) "اشتراک‌گذاری در استوری" else "Share Story"
                        )
                    )
                } else {
                    Toast.makeText(
                        context,
                        if (isFa) "خطا در ساخت استوری" else "Error generating story card",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090C15).copy(alpha = 0.96f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val targetCardHeight = remember(maxHeight, isLyricsPickerOpen) {
                val reservedVertical = if (isLyricsPickerOpen) 340.dp else 195.dp
                (maxHeight - reservedVertical).coerceIn(280.dp, 450.dp)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Top Modal Header (Attached to Top)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFa) "استوری اینستاگرام و تلگرام" else "Share to Story",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (isFa) {
                                "تم: ${currentStyle.labelFa} • برای تغییر روی کارت بزنید"
                            } else {
                                "Style: ${currentStyle.labelEn} • Tap card to switch"
                            },
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (validLyrics.isNotEmpty()) {
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isLyricsPickerOpen = !isLyricsPickerOpen
                                },
                                shape = RoundedCornerShape(50),
                                color = if (hasSelectedLyrics || isLyricsPickerOpen) {
                                    primaryTint.copy(alpha = 0.22f)
                                } else {
                                    Color.White.copy(alpha = 0.08f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (hasSelectedLyrics || isLyricsPickerOpen) {
                                        primaryTint
                                    } else {
                                        Color.White.copy(alpha = 0.16f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatQuote,
                                        contentDescription = null,
                                        tint = if (hasSelectedLyrics || isLyricsPickerOpen) primaryTint else Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (hasSelectedLyrics) {
                                            if (isFa) "لیریک (${activeSelectedLyrics.size})" else "Lyrics (${activeSelectedLyrics.size})"
                                        } else {
                                            if (isFa) "+ لیریک" else "+ Lyrics"
                                        },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // Interactive Lyrics Line Picker Drawer (If user taps "+ Lyrics")
                AnimatedVisibility(
                    visible = isLyricsPickerOpen && validLyrics.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 155.dp)
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF121624).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFa) "حداکثر ۴ خط برای درج روی استوری انتخاب کنید:" else "Select up to 4 lyric lines for Story:",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedLyricIndices.isNotEmpty()) {
                                    Text(
                                        text = if (isFa) "پاک کردن" else "Clear",
                                        color = primaryTint,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedLyricIndices.clear()
                                        }
                                    )
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                itemsIndexed(validLyrics) { idx, lineText ->
                                    val isLineSelected = selectedLyricIndices.contains(idx)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isLineSelected) primaryTint.copy(alpha = 0.22f)
                                                else Color.White.copy(alpha = 0.04f)
                                            )
                                            .border(
                                                width = if (isLineSelected) 1.dp else 0.5.dp,
                                                color = if (isLineSelected) primaryTint else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (isLineSelected) {
                                                    selectedLyricIndices.remove(idx)
                                                } else if (selectedLyricIndices.size < 4) {
                                                    selectedLyricIndices.add(idx)
                                                    selectedLyricIndices.sort()
                                                } else {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isLineSelected) primaryTint else Color.White.copy(alpha = 0.12f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isLineSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = lineText,
                                            color = if (isLineSelected) Color.White else Color.White.copy(alpha = 0.72f),
                                            fontSize = 12.sp,
                                            fontWeight = if (isLineSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // =========================================================================
                // 9:16 VIRAL STORY CARD CANVAS (CAPTURED REGION ONLY)
                // =========================================================================
                Box(
                    modifier = Modifier
                        .height(targetCardHeight)
                        .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                        .shadow(
                            elevation = 28.dp,
                            shape = RoundedCornerShape(26.dp),
                            spotColor = Color.Black
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .drawWithContent {
                            storyCardGraphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(storyCardGraphicsLayer)
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = cycleStyle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 1. Tap-to-Switch Luxury 9:16 Background Layer
                    Crossfade(
                        targetState = currentStyle,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                        modifier = Modifier.fillMaxSize(),
                        label = "StoryBackgroundCrossfade"
                    ) { style ->
                        when (style) {
                            StoryBackgroundStyle.DYNAMIC_AMBIENT_BLUR -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF090C15))
                                ) {
                                    val preBlurred = blurredBackgroundBitmap
                                    val rawBmp = softwareCoverBitmap
                                    if (preBlurred != null) {
                                        Image(
                                            bitmap = preBlurred.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .blur(65.dp)
                                        )
                                    } else if (rawBmp != null) {
                                        Image(
                                            bitmap = rawBmp.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .blur(65.dp)
                                        )
                                    } else if (bigCoverUrl.isNotBlank()) {
                                        val req = rememberAuraCoverRequest(
                                            trackId = track.id,
                                            coverUrl = bigCoverUrl,
                                            isLarge = true,
                                            allowHardware = false
                                        )
                                        AsyncImage(
                                            model = req,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .blur(65.dp)
                                        )
                                    }

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRect(
                                            color = Color(0xFF090C15).copy(alpha = 0.62f)
                                        )
                                        drawRect(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xFF090C15).copy(alpha = 0.55f)
                                                ),
                                                center = Offset(size.width * 0.5f, size.height * 0.46f),
                                                radius = size.height * 0.65f
                                            )
                                        )
                                    }
                                }
                            }

                            StoryBackgroundStyle.MIDNIGHT_NEON_AURA -> {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF090C15))
                                ) {
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF7C3AED).copy(alpha = 0.35f),
                                                Color(0xFF7C3AED).copy(alpha = 0.12f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width * 0.18f, size.height * 0.20f),
                                            radius = size.width * 0.92f
                                        )
                                    )
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF2563EB).copy(alpha = 0.30f),
                                                Color(0xFF2563EB).copy(alpha = 0.10f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width * 0.85f, size.height * 0.80f),
                                            radius = size.width * 0.92f
                                        )
                                    )
                                }
                            }

                            StoryBackgroundStyle.CRIMSON_AFTER_HOURS -> {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF090508))
                                ) {
                                    drawRect(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF991B1B).copy(alpha = 0.40f),
                                                Color(0xFF991B1B).copy(alpha = 0.16f),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width * 0.5f, size.height * 0.44f),
                                            radius = size.width * 0.98f
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 2. Centerpiece Floating Liquid Glass Card (Adaptive for Lyrics or Full Cover)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .align(Alignment.Center)
                            .offset(y = (-8).dp)
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(28.dp),
                                spotColor = Color.Black,
                                ambientColor = Color.Black
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1A2035).copy(alpha = 0.80f),
                                        Color(0xFF0D111D).copy(alpha = 0.90f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.30f),
                                        Color.White.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(if (hasSelectedLyrics) 16.dp else 18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (hasSelectedLyrics) {
                                // Compact Luxury Header + Featured Lyrics Quote Block
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .shadow(12.dp, RoundedCornerShape(14.dp))
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF121624))
                                            .border(
                                                width = 0.8.dp,
                                                color = Color.White.copy(alpha = 0.22f),
                                                shape = RoundedCornerShape(14.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val bmp = softwareCoverBitmap
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = track.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else if (bigCoverUrl.isNotBlank()) {
                                            val req = rememberAuraCoverRequest(
                                                trackId = track.id,
                                                coverUrl = bigCoverUrl,
                                                isLarge = true,
                                                allowHardware = false
                                            )
                                            AsyncImage(
                                                model = req,
                                                contentDescription = track.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            MusicArtworkPlaceholder(
                                                title = track.title,
                                                artist = track.artist,
                                                trackId = track.id,
                                                palette = palette,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = track.artist,
                                            color = Color.White.copy(alpha = 0.72f),
                                            fontSize = 11.5.sp,
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
                                        .background(Color.White.copy(alpha = 0.12f))
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val formattedQuote = remember(activeSelectedLyrics) {
                                    "“" + activeSelectedLyrics.joinToString("\n") + "”"
                                }

                                Text(
                                    text = formattedQuote,
                                    color = Color.White,
                                    fontSize = 14.5.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 21.sp,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.75f),
                                            offset = Offset(0f, 3f),
                                            blurRadius = 10f
                                        )
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                // Standard Full Square 1:1 Cover Art
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .shadow(
                                            elevation = 16.dp,
                                            shape = RoundedCornerShape(20.dp),
                                            spotColor = Color.Black
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF121624))
                                        .border(
                                            width = 0.8.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.22f),
                                                    Color.White.copy(alpha = 0.03f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(20.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bmp = softwareCoverBitmap
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (bigCoverUrl.isNotBlank()) {
                                        val req = rememberAuraCoverRequest(
                                            trackId = track.id,
                                            coverUrl = bigCoverUrl,
                                            isLarge = true,
                                            allowHardware = false
                                        )
                                        AsyncImage(
                                            model = req,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        MusicArtworkPlaceholder(
                                            title = track.title,
                                            artist = track.artist,
                                            trackId = track.id,
                                            palette = palette,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = track.title,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = track.artist,
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 3. Luxury Aura Branding Pill at bottom of 9:16 canvas
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 0.5.dp,
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "AURA MUSIC",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.2.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // =========================================================================
                // Controls Section Attached Directly Below Story Card (Not Pushed to Bottom)
                // =========================================================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StoryBackgroundStyle.entries.forEach { style ->
                            val isSelected = currentStyle == style
                            val dotColor = when (style) {
                                StoryBackgroundStyle.DYNAMIC_AMBIENT_BLUR -> primaryTint
                                StoryBackgroundStyle.MIDNIGHT_NEON_AURA -> Color(0xFF7C3AED)
                                StoryBackgroundStyle.CRIMSON_AFTER_HOURS -> Color(0xFF991B1B)
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.16f)
                                        else Color.White.copy(alpha = 0.06f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.2.dp else 0.6.dp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.45f)
                                        else Color.White.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedStyleIdx = style.index
                                    }
                                    .padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Text(
                                    text = if (isFa) style.labelFa else style.labelEn,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = cycleStyle,
                            modifier = Modifier.height(46.dp),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.22f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AuraSurfaceElevated.copy(alpha = 0.65f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AuraSpotifyGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "تغییر تم" else "Switch Theme",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = shareStoryCardImage,
                            enabled = !isGeneratingCard,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuraSpotifyGreen,
                                contentColor = Color.Black
                            )
                        ) {
                            if (isGeneratingCard) {
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
                                    text = if (isFa) "اشتراک‌گذاری در استوری" else "Share to Story",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
