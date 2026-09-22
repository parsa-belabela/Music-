package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.Track
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import com.example.util.Localization

data class DuplicateGroup(
    val groupId: String,
    val winner: Track,
    val losers: List<Track>
)

@Composable
fun DuplicatesReviewDialog(
    groups: List<DuplicateGroup>,
    palette: AmbientPalette,
    lang: AppLanguage,
    onKeepAllHigherQuality: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .liquidGlass(
                    shape = RoundedCornerShape(24.dp),
                    thickness = GlassThickness.THICK,
                    tintColor = palette.primary,
                    tintAlpha = 0.22f,
                    borderWidth = 1.2.dp
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "مدیریت فایل‌های تکراری" else "Duplicate Audio Optimizer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (lang == AppLanguage.PERSIAN)
                        "${groups.size} آهنگ با کیفیت‌های مختلف یافت شد. نسخه با بالاترین بیت‌ریت برای پخش نگه داشته می‌شود."
                    else
                        "${groups.size} duplicate tracks detected with different qualities. Aura recommends keeping the highest bitrate.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFA0A5BA),
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groups) { group ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x1AFFFFFF))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = group.winner.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = group.winner.artist,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA0A5BA)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Keep: ${group.winner.bitrate} kbps",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = palette.accent,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = palette.accent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Text(
                                        text = "Hide ${group.losers.size} lower copy",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF8E8EA0)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(if (lang == AppLanguage.PERSIAN) "بستن" else "Dismiss")
                    }

                    Button(
                        onClick = {
                            onKeepAllHigherQuality()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                    ) {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "نگه‌داشتن نسخه بهتر" else "Keep Best Quality",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
