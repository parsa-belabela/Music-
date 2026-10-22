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
import com.example.data.model.Track
import com.example.util.SongShareCardGenerator
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun ShareSongDialog(
    track: Track,
    palette: AmbientPalette,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isFa = language == AppLanguage.PERSIAN
    var isGeneratingCard by remember { mutableStateOf(false) }
    var selectedCardThemeIdx by remember { mutableIntStateOf(0) }

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
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            text = if (isFa) "کارت استوری اینستاگرام (Spotify Style)" else "Spotify-Style Story Card",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
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
                        .fillMaxWidth(0.88f)
                        .aspectRatio(9f / 14.2f)
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
                            .padding(24.dp),
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
                                .size(195.dp)
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

                        // اطلاعات آهنگ و موج صوتی گرافیکی پایین کارت
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = track.title,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = track.artist,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // موج صوتی Spotify-Code Style
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(26.dp)
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
                        .height(52.dp),
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
                            text = if (isFa) "اشتراک‌گذاری در استوری اینستاگرام و..." else "Share High-Res Story Card",
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
