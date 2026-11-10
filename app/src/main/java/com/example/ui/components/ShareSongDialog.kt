package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.model.AppLanguage
import com.example.data.model.LyricsLine
import com.example.data.model.Track
import com.example.ui.theme.AuraCanvas
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import com.example.util.SongShareCardGenerator
import kotlinx.coroutines.launch
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
    var selectedCardThemeIdx by remember { mutableIntStateOf(0) }
    var showLyricPicker by remember { mutableStateOf(false) }

    val availableLines = remember(lyrics, track.id) {
        val fromLyrics = lyrics.map { it.text.trim() }.filter {
            it.isNotBlank() && !it.contains("متنی برای این آهنگ یافت نشد")
        }
        if (fromLyrics.isNotEmpty()) {
            fromLyrics
        } else {
            val prefs = context.getSharedPreferences("aura_freestyle_pad", Context.MODE_PRIVATE)
            val note = prefs.getString("note_${track.id}", "") ?: ""
            note.lines()
                .map { it.replace(Regex("\\[\\d+:\\d+(?:\\.\\d+)?]"), "").trim() }
                .filter { it.isNotBlank() }
        }
    }

    val selectedLyricIndices = remember(track.id) {
        mutableStateListOf<Int>().apply {
            if (availableLines.isNotEmpty()) {
                add(0)
                if (availableLines.size > 1) add(1)
            }
        }
    }

    val selectedLyricStrings = selectedLyricIndices.sorted()
        .mapNotNull { availableLines.getOrNull(it) }
        .take(4)

    // Curated Spotify Share Card Color Themes
    val cardThemes = remember(palette) {
        val dynamicPrimary = if (palette.isMonochrome) Color(0xFF282828) else lerp(Color(0xFF181818), palette.primary.copy(alpha = 1f), 0.75f)
        val dynamicSecondary = if (palette.isMonochrome) Color(0xFF141414) else lerp(Color(0xFF101010), palette.primary.copy(alpha = 1f), 0.42f)
        listOf(
            listOf(dynamicPrimary, dynamicSecondary),
            listOf(Color(0xFF1DB954), Color(0xFF0E6B30)),
            listOf(Color(0xFFE1306C), Color(0xFF7A1438)),
            listOf(Color(0xFF3B82F6), Color(0xFF1D3A8A)),
            listOf(Color(0xFFD97706), Color(0xFF78350F)),
            listOf(Color(0xFF242424), Color(0xFF141414))
        )
    }
    val activeGradient = cardThemes[selectedCardThemeIdx % cardThemes.size]

    var loadedCoverBitmap by remember(track.id, track.artworkUri) {
        mutableStateOf(
            OnlineMusicRepository.getCachedCoverBitmap(track.artworkUri ?: track.id)
        )
    }
    LaunchedEffect(track.id, track.artworkUri) {
        if (loadedCoverBitmap == null) {
            val url = track.artworkUri?.takeIf { it.isNotBlank() }
                ?: OnlineMusicRepository.getCoverUrlForTrack(track.id, track.artist)
            if (!url.isNullOrBlank()) {
                loadedCoverBitmap = OnlineMusicRepository.loadCoverBitmap(url)
            }
        }
    }

    val shareStoryCardImage: () -> Unit = {
        if (!isGeneratingCard) {
            isGeneratingCard = true
            scope.launch {
                val customPalette = palette.copy(
                    primary = activeGradient[0],
                    secondary = activeGradient[1],
                    accent = Color.White
                )
                val uri = SongShareCardGenerator.generateSongCardBitmap(
                    context = context,
                    track = track,
                    palette = customPalette,
                    selectedLyricLines = selectedLyricStrings,
                    lang = language
                )
                isGeneratingCard = false
                if (uri != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
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
                    Toast.makeText(context, "Error generating card", Toast.LENGTH_SHORT).show()
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
                .background(AuraCanvas)
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Clean Spotify Share Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedLyricStrings.isNotEmpty()) {
                            if (isFa) "اشتراک‌گذاری متن آهنگ" else "Share Lyrics"
                        } else {
                            if (isFa) "اشتراک‌گذاری آهنگ" else "Share Song"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AuraSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 2. Authentic Spotify Story Preview Card (Lyrics Mode vs Song Mode)
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(0.88f)
                        .aspectRatio(9f / 14f)
                        .shadow(24.dp, RoundedCornerShape(20.dp), spotColor = Color.Black)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(activeGradient))
                        .padding(22.dp)
                ) {
                    if (selectedLyricStrings.isNotEmpty()) {
                        // === SPOTIFY LYRICS SHARE CARD LAYOUT ===
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Track Header (Small Cover + Title + Artist)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF181818)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bmp = loadedCoverBitmap
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (!track.artworkUri.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = track.artworkUri,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        color = Color.White.copy(alpha = 0.78f),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Center Editorial Lyrics Block
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val dynamicFontSize = when (selectedLyricStrings.size) {
                                    1 -> 24.sp
                                    2 -> 21.sp
                                    3 -> 18.5.sp
                                    else -> 16.5.sp
                                }
                                selectedLyricStrings.forEach { lineText ->
                                    Text(
                                        text = lineText,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = dynamicFontSize,
                                        lineHeight = dynamicFontSize * 1.32f,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Bottom Spotify-Style Brand & Audio Code Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = activeGradient[0],
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = "Aura Music",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                SpotifyMiniWaveformCode(
                                    seed = track.title.hashCode(),
                                    modifier = Modifier
                                        .width(84.dp)
                                        .height(16.dp)
                                )
                            }
                        }
                    } else {
                        // === SPOTIFY SONG / ALBUM SHARE CARD LAYOUT ===
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))

                            // Crisp Centered Album Cover
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.84f)
                                    .aspectRatio(1f)
                                    .shadow(20.dp, RoundedCornerShape(12.dp), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF181818)),
                                contentAlignment = Alignment.Center
                            ) {
                                val bmp = loadedCoverBitmap
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (!track.artworkUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = track.artworkUri,
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

                            // Track Title, Artist & Soundwave Code
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = track.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = track.artist,
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = activeGradient[0],
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = "Aura Music",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    SpotifyMiniWaveformCode(
                                        seed = track.title.hashCode(),
                                        modifier = Modifier
                                            .width(96.dp)
                                            .height(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Controls Section (Lyric Picker, Color Swatches & Share Button)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (availableLines.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lyrics,
                                        contentDescription = null,
                                        tint = AuraSpotifyGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isFa) "انتخاب خطوط لیریک (${selectedLyricIndices.size}/4)" else "Lyrics on card (${selectedLyricIndices.size}/4)",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (selectedLyricIndices.isNotEmpty()) {
                                        Text(
                                            text = if (isFa) "حالت پوستر کاور" else "Cover Only",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { selectedLyricIndices.clear() }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = if (showLyricPicker) {
                                            if (isFa) "بستن لیست" else "Done"
                                        } else {
                                            if (isFa) "تغییر متن" else "Select Lines"
                                        },
                                        color = AuraSpotifyGreen,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showLyricPicker = !showLyricPicker }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (showLyricPicker) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AuraSurface)
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(availableLines.size) { idx ->
                                        val isChosen = idx in selectedLyricIndices
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isChosen) AuraSpotifyGreen.copy(alpha = 0.18f) else Color.Transparent)
                                                .clickable {
                                                    if (isChosen) {
                                                        selectedLyricIndices.remove(idx)
                                                    } else if (selectedLyricIndices.size < 4) {
                                                        selectedLyricIndices.add(idx)
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = availableLines[idx],
                                                color = if (isChosen) Color.White else TextSecondary,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isChosen) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = AuraSpotifyGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Color Swatches Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        cardThemes.forEachIndexed { idx, colors ->
                            val selected = selectedCardThemeIdx == idx
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 32.dp else 26.dp)
                                    .clip(CircleShape)
                                    .background(colors.first())
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCardThemeIdx = idx }
                            )
                        }
                    }

                    // Primary Share Button
                    Button(
                        onClick = shareStoryCardImage,
                        enabled = !isGeneratingCard,
                        modifier = Modifier
                            .fillMaxWidth()
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
                                Icons.Default.Share,
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

@Composable
private fun SpotifyMiniWaveformCode(
    seed: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val bars = 22
        val gap = 3.dp.toPx()
        val barW = ((size.width - gap * (bars - 1)) / bars).coerceAtLeast(2f)
        for (i in 0 until bars) {
            val norm = (sin(i * 0.65f + seed) * 0.38f + 0.56f).coerceIn(0.22f, 1f)
            val h = size.height * norm
            val x = i * (barW + gap)
            val y = (size.height - h) / 2f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.88f),
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}
