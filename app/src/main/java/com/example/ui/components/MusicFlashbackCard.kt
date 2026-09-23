package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.Track
import com.example.data.repository.FunMusicInsight
import com.example.data.repository.UserProfileManager
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass

/**
 * Spotify-like Random Music Flashback & Insight Card:
 * Displays fun, personalized musical trivia & memories for the current track/artist.
 */
@Composable
fun MusicFlashbackCard(
    currentTrack: Track?,
    palette: AmbientPalette,
    language: AppLanguage,
    customInsight: FunMusicInsight? = null,
    onShareInsight: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isFa = language == AppLanguage.PERSIAN
    val fallbackInsight = remember(currentTrack) {
        if (currentTrack != null) {
            FunMusicInsight(
                trackTitle = currentTrack.title,
                artistName = currentTrack.artist,
                messageFa = "🎧 قطعه «${currentTrack.title}» اثر ${currentTrack.artist} در حال پخش است.",
                messageEn = "Currently playing \"${currentTrack.title}\" by ${currentTrack.artist}.",
                badgeEmoji = "🎵",
                statHighlight = "در حال پخش"
            )
        } else {
            FunMusicInsight(
                trackTitle = "",
                artistName = "",
                messageFa = "هنوز آهنگی پخش نشده است. با پخش موسیقی‌های مورد علاقه‌تان، خاطرات و فکت‌های واقعی شما در دیتابیس ثبت می‌شود.",
                messageEn = "No music played yet. Start playing your favorite tracks to generate real music memories!",
                badgeEmoji = "🎧",
                statHighlight = "دفترچه خاطرات موسیقی"
            )
        }
    }

    var refreshedInsight by remember { mutableStateOf<FunMusicInsight?>(null) }
    val insight = refreshedInsight ?: customInsight ?: fallbackInsight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                thickness = GlassThickness.REGULAR,
                tintColor = palette.primary,
                tintAlpha = 0.16f,
                borderWidth = 1.2.dp
            )
            .padding(16.dp)
    ) {
        Column {
            // Header Tag & Randomizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = insight.badgeEmoji, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFa) "خاطره و فکت موسیقی (Spotify Insight)" else "Music Memory Flashback",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = palette.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val title = currentTrack?.title ?: "آهنگ"
                            val artist = currentTrack?.artist ?: "هنرمند"
                            refreshedInsight = UserProfileManager.generateFunInsight(title, artist, (3..14).random().toFloat())
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "New Insight",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val textToShare = "${insight.statHighlight}\n${if (isFa) insight.messageFa else insight.messageEn}\n— پخش شده در آئورا موزیک"
                            onShareInsight(textToShare)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Insight Content Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22000000))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = insight.statHighlight,
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFa) insight.messageFa else insight.messageEn,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
