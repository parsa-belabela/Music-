package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.sin

@Composable
fun ShareSongDialog(
    track: Track,
    palette: AmbientPalette,
    language: AppLanguage,
    lyrics: List<LyricsLine> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFa = language == AppLanguage.PERSIAN
    var isGeneratingCard by remember { mutableStateOf(false) }
    var selectedStyleIdx by remember { mutableIntStateOf(0) }
    val currentStyle = StoryBackgroundStyle.fromIndex(selectedStyleIdx)

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

    val cycleStyle: () -> Unit = {
        selectedStyleIdx = (selectedStyleIdx + 1) % StoryBackgroundStyle.entries.size
    }

    val shareStoryCardImage: () -> Unit = {
        if (!isGeneratingCard) {
            isGeneratingCard = true
            scope.launch {
                val uri = try {
                    // Generate crisp 1080x1920px high-resolution Story Card with software bitmap
                    SongShareCardGenerator.generateSongCardBitmap(
                        context = context,
                        track = track,
                        palette = palette,
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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090C15).copy(alpha = 0.96f))
                .systemBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Modal Header (Excluded from Story Capture)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isFa) "استوری اینستاگرام و تلگرام" else "Share to Story",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isFa) {
                                "تم: ${currentStyle.labelFa} (برای تغییر روی کارت بزنید)"
                            } else {
                                "Style: ${currentStyle.labelEn} (Tap card to switch)"
                            },
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // =========================================================================
                // 9:16 VIRAL STORY CARD CANVAS (CAPTURED REGION ONLY)
                // =========================================================================
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(vertical = 8.dp)
                        .aspectRatio(9f / 16f)
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

                                    // Dark radial vignette overlay Color(0xFF090C15).copy(alpha = 0.62f)
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
                                // Deep obsidian #090C15 with top-left #7C3AED (35% alpha) & bottom-right #2563EB (30% alpha)
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
                                // Deep #090508 background with moody crimson/ruby radial glow (#991B1B at 40% alpha)
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

                    // 2. Centerpiece Floating Liquid Glass Card (width = 82% of canvas width)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .align(Alignment.Center)
                            .offset(y = (-10).dp)
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
                                        Color(0xFF1A2035).copy(alpha = 0.78f),
                                        Color(0xFF0D111D).copy(alpha = 0.88f)
                                    )
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.28f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // High-Res Cover Art: Square 1:1 aspect ratio, RoundedCornerShape(20.dp), 16.dp drop shadow
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

                            // Track Metadata (18.dp below cover)
                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = track.title,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = track.artist,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Aesthetic Audio Waveform / Scrubber (16.dp below artist)
                            Spacer(modifier = Modifier.height(16.dp))

                            StoryLuxuryAudioWaveformScrubber(
                                seed = track.title.hashCode(),
                                durationMs = track.durationMs,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 3. Luxury Aura Branding Pill at bottom of 9:16 canvas
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 22.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 0.5.dp,
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "AURA MUSIC",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.5.sp
                            )
                        }
                    }
                }

                // =========================================================================
                // Bottom Controls Section (Theme Switcher + Share Action Button)
                // =========================================================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 3 Luxury Background Style Selector Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StoryBackgroundStyle.entries.forEach { style ->
                            val isSelected = currentStyle == style
                            val dotColor = when (style) {
                                StoryBackgroundStyle.DYNAMIC_AMBIENT_BLUR -> palette.primary
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
                                    .clickable { selectedStyleIdx = style.index }
                                    .padding(horizontal = 11.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
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
                        // Tap-to-switch button
                        OutlinedButton(
                            onClick = cycleStyle,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(
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

                        // Primary Share to Story Button
                        Button(
                            onClick = shareStoryCardImage,
                            enabled = !isGeneratingCard,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
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
                                    fontWeight = FontWeight.Bold,
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

@Composable
private fun StoryLuxuryAudioWaveformScrubber(
    seed: Int,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val formattedTotal = remember(durationMs) {
        val totalSeconds = (durationMs / 1000L).toInt()
        if (totalSeconds <= 80) {
            "3:28"
        } else {
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            String.format("%d:%02d", minutes, seconds)
        }
    }

    Column(modifier = modifier) {
        // 28 rounded vertical soundwave bars at 35% progress with a tiny glowing dot
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
        ) {
            val totalBars = 28
            val progressRatio = 0.35f
            val activeBars = (totalBars * progressRatio).toInt()
            val gap = 3.dp.toPx()
            val barW = ((size.width - gap * (totalBars - 1)) / totalBars).coerceAtLeast(2.dp.toPx())
            val centerY = size.height / 2f

            var dotCenterX = size.width * progressRatio
            for (i in 0 until totalBars) {
                val norm = (sin(i * 0.62f + seed) * 0.36f + 0.56f).coerceIn(0.22f, 1f)
                val h = (size.height * norm).coerceAtLeast(4.dp.toPx())
                val x = i * (barW + gap)
                val y = centerY - h / 2f
                if (i == activeBars) {
                    dotCenterX = x + barW / 2f
                }
                drawRoundRect(
                    color = if (i <= activeBars) {
                        Color.White.copy(alpha = 0.85f)
                    } else {
                        Color.White.copy(alpha = 0.25f)
                    },
                    topLeft = Offset(x, y),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
            }

            // Tiny glowing dot at 35% progress
            drawCircle(
                color = Color.White.copy(alpha = 0.38f),
                radius = 5.5.dp.toPx(),
                center = Offset(dotCenterX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.8.dp.toPx(),
                center = Offset(dotCenterX, centerY)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "1:14",
                color = Color.White.copy(alpha = 0.56f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formattedTotal,
                color = Color.White.copy(alpha = 0.56f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
