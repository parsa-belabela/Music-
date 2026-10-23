package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
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
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.LyricsLine
import com.example.data.model.Track
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

    // Available lyric lines (from synced lyrics or freestyle pad notes)
    val availableLines = remember(lyrics, track.id) {
        val fromLyrics = lyrics.map { it.text.trim() }.filter { it.isNotBlank() }
        if (fromLyrics.isNotEmpty()) {
            fromLyrics
        } else {
            val prefs = context.getSharedPreferences("aura_freestyle_pad", android.content.Context.MODE_PRIVATE)
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

    val selectedLyricStrings = selectedLyricIndices.sorted().mapNotNull { availableLines.getOrNull(it) }.take(3)

    val cardThemes = remember(palette) {
        listOf(
            listOf(palette.primary, palette.secondary, Color(0xFF070913)),
            listOf(Color(0xFFFF1E56), Color(0xFF7B2CBF), Color(0xFF06040D)),
            listOf(Color(0xFF00F5D4), Color(0xFF3A86FF), Color(0xFF040914)),
            listOf(Color(0xFFFFBE0B), Color(0xFFFB5607), Color(0xFF120804))
        )
    }
    val activeGradient = cardThemes[selectedCardThemeIdx % cardThemes.size]

    val shareStoryCardImage: () -> Unit = {
        if (!isGeneratingCard) {
            isGeneratingCard = true
            scope.launch {
                val customPalette = palette.copy(
                    primary = activeGradient[0],
                    secondary = activeGradient[1]
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
                            "🎵 ${track.title} — ${track.artist}\n#AuraMusic"
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            if (isFa) "اشتراک‌گذاری کارت استوری در اینستاگرام و..." else "Share Story Card"
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
                .background(Color(0xF504050A))
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // هدر بالا
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = activeGradient[0],
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFa) "پوستر استوری و لیریک (Spotify Lyric Card)" else "Spotify Lyric & Story Card",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // پیش‌نمایش کارت گرافیکی ۹:۱۶ استوری مشابه اسپاتیفای
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .aspectRatio(9f / 13.8f)
                        .shadow(28.dp, RoundedCornerShape(32.dp), spotColor = activeGradient[0])
                        .clip(RoundedCornerShape(32.dp))
                        .background(Brush.verticalGradient(activeGradient))
                        .border(
                            width = 1.8.dp,
                            brush = Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.75f), activeGradient[0], Color.White.copy(alpha = 0.25f))
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    if (!track.artworkUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = track.artworkUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(45.dp),
                            alpha = 0.35f
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // نشان بالای کارت
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AURA MUSIC",
                                color = Color.White.copy(alpha = 0.90f),
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${track.bitrate} kbps • HI-RES",
                                    color = Color.White,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // کاور وسط کارت با سایه عمیق
                        Box(
                            modifier = Modifier
                                .size(if (selectedLyricStrings.isNotEmpty()) 148.dp else 185.dp)
                                .shadow(24.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF10121E))
                                .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!track.artworkUri.isNullOrEmpty()) {
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

                        // بخش لیریک انتخاب‌شده (Spotify Lyric Card) یا موج صوتی
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.Black.copy(alpha = 0.48f))
                                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(22.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = track.title,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = track.artist,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (selectedLyricStrings.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    selectedLyricStrings.forEach { lineText ->
                                        Text(
                                            text = "“ $lineText ”",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(10.dp))
                                // موج صوتی Spotify-Code Style
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(24.dp)
                                ) {
                                    val bars = 28
                                    val gap = 4.dp.toPx()
                                    val barW = (size.width - gap * (bars - 1)) / bars
                                    for (i in 0 until bars) {
                                        val norm = (sin(i * 0.45f + track.title.hashCode()) * 0.4f + 0.55f).coerceIn(0.2f, 1f)
                                        val h = size.height * norm
                                        val x = i * (barW + gap)
                                        val y = (size.height - h) / 2f
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.90f),
                                            topLeft = Offset(x, y),
                                            size = Size(barW, h),
                                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // انتخابگر تا ۳ خط از متن آهنگ (Spotify Lyric Card Picker)
                if (availableLines.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "انتخاب متن آهنگ روی پوستر (حداکثر ۳ خط):" else "Select up to 3 lyric lines:",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { showLyricPicker = !showLyricPicker },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (showLyricPicker) {
                                        if (isFa) "بستن لیست" else "Hide Lines"
                                    } else {
                                        if (isFa) "انتخاب خطوط (${selectedLyricIndices.size}/3)" else "Pick Lines (${selectedLyricIndices.size}/3)"
                                    },
                                    color = activeGradient[0],
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        if (showLyricPicker) {
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 115.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF101322))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(availableLines.size) { idx ->
                                    val isChosen = idx in selectedLyricIndices
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isChosen) activeGradient[0].copy(alpha = 0.25f) else Color.Transparent)
                                            .clickable {
                                                if (isChosen) {
                                                    selectedLyricIndices.remove(idx)
                                                } else if (selectedLyricIndices.size < 3) {
                                                    selectedLyricIndices.add(idx)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = availableLines[idx],
                                            color = if (isChosen) Color.White else Color(0xFFB0B5C8),
                                            fontSize = 12.sp,
                                            fontWeight = if (isChosen) FontWeight.ExtraBold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // انتخابگر رنگ پس‌زمینه کارت استوری
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFa) "رنگ کارت:" else "Card Backdrop:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    cardThemes.forEachIndexed { idx, colors ->
                        val selected = selectedCardThemeIdx == idx
                        Box(
                            modifier = Modifier
                                .size(if (selected) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(colors.take(2)))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable { selectedCardThemeIdx = idx }
                        )
                    }
                }

                // دکمه اشتراک‌گذاری مستقیم تصویر کارت برای استوری اینستاگرام و شبکه‌های اجتماعی
                Button(
                    onClick = shareStoryCardImage,
                    enabled = !isGeneratingCard,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeGradient[0])
                ) {
                    if (isGeneratingCard) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFa) "اشتراک‌گذاری پوستر استوری در اینستاگرام و..." else "Share High-Res Story Poster",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
