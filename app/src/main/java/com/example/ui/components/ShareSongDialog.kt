package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization

import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun ShareSongDialog(
    track: Track,
    palette: AmbientPalette,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val shareText = buildString {
        append("🎵 \"${track.title}\" - ${track.artist}\n")
        if (track.album.isNotBlank()) {
            append(if (language == AppLanguage.PERSIAN) "آلبوم: ${track.album}\n" else "Album: ${track.album}\n")
        }
        append(if (language == AppLanguage.PERSIAN) "کیفیت: ${track.bitrate} kbps صدای شفاف و استودیویی\n" else "Quality: ${track.bitrate} kbps Hi-Fi Audio\n")
        append(if (language == AppLanguage.PERSIAN) "ارسال شده از موزیک پلیر Aura" else "Shared via Aura Music Player")
    }

    val onShareIntent = {
        try {
            val mime = when {
                track.uri.endsWith(".flac", ignoreCase = true) -> "audio/flac"
                track.uri.endsWith(".wav", ignoreCase = true) -> "audio/wav"
                track.uri.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
                track.uri.endsWith(".m4a", ignoreCase = true) || track.uri.endsWith(".aac", ignoreCase = true) -> "audio/mp4"
                else -> "audio/mpeg"
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_SUBJECT, "${track.title} - ${track.artist}")
                putExtra(Intent.EXTRA_TEXT, shareText)

                val uri = Uri.parse(track.uri)
                val streamUri: Uri? = when {
                    track.uri.startsWith("content://") -> uri
                    track.uri.startsWith("file://") || track.uri.startsWith("/") -> {
                        val filePath = if (track.uri.startsWith("file://")) uri.path ?: "" else track.uri
                        val file = File(filePath)
                        if (file.exists()) {
                            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        } else null
                    }
                    track.isDemo || track.uri.startsWith("asset://") -> {
                        val demoFile = File(context.cacheDir, "${track.title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")}.mp3")
                        if (!demoFile.exists()) {
                            val assetName = if (track.uri.startsWith("asset://")) track.uri.removePrefix("asset://") else "demo_track.mp3"
                            try {
                                context.assets.open(assetName).use { input ->
                                    demoFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignore asset copy error
                            }
                        }
                        if (demoFile.exists()) {
                            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", demoFile)
                        } else null
                    }
                    else -> null
                }

                if (streamUri != null) {
                    putExtra(Intent.EXTRA_STREAM, streamUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            val chooserTitle = if (language == AppLanguage.PERSIAN) "اشتراک‌گذاری فایل موزیک" else "Share Music File"
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val onCopyClipboard = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Song Info", shareText)
        clipboard.setPrimaryClip(clip)
        val msg = if (language == AppLanguage.PERSIAN) "مشخصات آهنگ در کلیپ‌بورد کپی شد" else "Song info copied to clipboard"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(28.dp),
                        thickness = GlassThickness.REGULAR,
                        tintColor = palette.primary,
                        tintAlpha = 0.20f,
                        borderWidth = 1.2.dp
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.PERSIAN) "اشتراک‌گذاری کارت آهنگ" else "Share Music Card",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // The Premium Share Card Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.45f))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF1F1D36),
                                    Color(0xFF10101E)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    palette.accent.copy(alpha = 0.8f),
                                    palette.primary.copy(alpha = 0.4f),
                                    Color(0x22FFFFFF)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Artwork
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .shadow(12.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF141322)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (track.artworkUri != null) {
                                AsyncImage(
                                    model = track.artworkUri,
                                    contentDescription = track.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Title & Artist
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = palette.accent,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (track.album.isNotBlank()) {
                            Text(
                                text = track.album,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFA5A5BA)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Audio specs badge
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BadgePill(text = "${track.bitrate} kbps")
                            BadgePill(text = "${track.sampleRate / 1000} kHz")
                            BadgePill(text = track.durationFormatted)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Watermark
                        Text(
                            text = "AURA MUSIC PLAYER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0x66FFFFFF),
                                letterSpacing = 2.sp,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Share Apps & Copy Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copy Info Button
                    OutlinedButton(
                        onClick = onCopyClipboard,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.PERSIAN) "کپی متن" else "Copy Info",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Native Share Button
                    Button(
                        onClick = onShareIntent,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.PERSIAN) "اشتراک‌گذاری" else "Share",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x25FFFFFF))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFFD1D5DB),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
