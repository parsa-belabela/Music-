package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.monetization.AchievementItem
import com.example.monetization.AchievementManager
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass

@Composable
fun AchievementsDialog(
    achievements: List<AchievementItem>,
    palette: AmbientPalette,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onRewardClaimed: (Int) -> Unit
) {
    val context = LocalContext.current
    val gold = Color(0xFFFFD700)

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(28.dp),
                    thickness = GlassThickness.THICK,
                    tintColor = palette.primary,
                    tintAlpha = 0.32f
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(gold.copy(alpha = 0.2f))
                        .border(1.dp, gold.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = gold,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (lang == AppLanguage.PERSIAN) "دستاوردها و هدایای وفاداری" else "Achievements & Loyalty Gifts",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (lang == AppLanguage.PERSIAN)
                        "با گوش دادن مستمر به موسیقی، دستاورد کسب کنید و اشتراک VIP رایگان هدیه بگیرید."
                    else
                        "Earn milestones by listening to music and unlock free VIP passes.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFC0C0D4),
                        fontSize = 12.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(achievements) { item ->
                        AchievementRow(
                            item = item,
                            palette = palette,
                            lang = lang,
                            onClaim = {
                                val hours = AchievementManager.claimReward(context, item.id)
                                if (hours > 0) {
                                    Toast.makeText(
                                        context,
                                        if (lang == AppLanguage.PERSIAN)
                                            "هدیه $hours ساعته VIP با موفقیت به شما تقدیم شد! 🎉"
                                        else "$hours hours of VIP granted! Enjoy 🎉",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onRewardClaimed(hours)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text(if (lang == AppLanguage.PERSIAN) "بستن" else "Close")
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(
    item: AchievementItem,
    palette: AmbientPalette,
    lang: AppLanguage,
    onClaim: () -> Unit
) {
    val gold = Color(0xFFFFD700)
    val progress = if (item.target > 0) (item.current.toFloat() / item.target.toFloat()).coerceIn(0f, 1f) else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                if (item.isUnlocked) gold.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) item.titleFa else item.titleEn,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) item.descFa else item.descEn,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB0B0C4),
                            fontSize = 11.sp
                        )
                    )
                }

                if (item.isUnlocked) {
                    if (item.isRewardClaimed) {
                        Surface(
                            color = Color(0x334CAF50),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "دریافت شده ✓" else "Claimed ✓",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF81C784)),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (item.vipRewardHours > 0) {
                        Button(
                            onClick = onClaim,
                            colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "دریافت VIP هدیه" else "Claim VIP",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (item.isUnlocked) gold else palette.accent,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                Text(
                    text = "${item.current} / ${item.target}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFC0C0D4),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
