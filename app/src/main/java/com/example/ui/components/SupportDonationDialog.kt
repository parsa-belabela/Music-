package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.repository.UserProfileManager
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Transparent, heartwarming Support & Donation Dialog:
 * Completely clear that music player features are 100% free for everyone.
 * Supporting via donation or watching an ad grants special Profile Prestige ("کاربر مهربون"),
 * unlocks music flashback cards, and supports future app development.
 */
@Composable
fun SupportDonationDialog(
    palette: AmbientPalette,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSupportSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isFa = language == AppLanguage.PERSIAN

    var isWatchingAd by remember { mutableStateOf(false) }
    var adProgress by remember { mutableFloatStateOf(0f) }
    var promoCodeInput by remember { mutableStateOf("") }
    var showPromoBox by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isWatchingAd) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(28.dp),
                        thickness = GlassThickness.THICK,
                        tintColor = palette.primary,
                        tintAlpha = 0.18f,
                        borderWidth = 1.5.dp
                    )
                    .padding(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Close & Heart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF4081).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF4081),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = if (isFa) "حمایت از سازنده و برنامه ❤️" else "Support the Developer ❤️",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 17.sp
                            )
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 100% Transparency Message Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x2BFFFFFF))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFa) "شفافیت کامل با شما دوستان:" else "Total Transparency:",
                                    color = palette.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isFa)
                                    "تمامی امکانات پخش موسیقی، اکولایزر، لیریکس و بخش‌های برنامه برای همیشه ۱۰۰٪ رایگان است. خرید حمایت یا تماشای تبلیغ هیچ قابلیت صوتی خاصی به شما نمی‌دهد، اما پروفایل شما را با نشان «کاربر مهربون» و کارت‌های خاطره موسیقی درخشان می‌کند!"
                                else
                                    "All music playback features, EQ & lyrics are 100% free for everyone forever. Supporting simply awards a prestigious \"Kind User\" badge on your profile and helps keep the app alive!",
                                color = Color(0xFFE2E4F0),
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Ad Watching State
                    if (isWatchingAd) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF161A2E))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    progress = { adProgress },
                                    modifier = Modifier.size(54.dp),
                                    color = Color(0xFFFF4081),
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isFa) "در حال تماشای ویدیوی حمایتی... ❤️" else "Playing reward sponsor video...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isFa) "از صبر و حمایت صمیمانه شما بی‌نهایت متشکریم" else "Thank you for your warm support!",
                                    color = Color(0xFFA0A5BA),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        // Support Option 1: Free Ad Support (Prominently Highlighted)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFFF4081).copy(alpha = 0.28f),
                                            Color(0xFF7C4DFF).copy(alpha = 0.20f)
                                        )
                                    )
                                )
                                .border(1.5.dp, Color(0xFFFF4081), RoundedCornerShape(18.dp))
                                .clickable {
                                    isWatchingAd = true
                                    adProgress = 0f
                                    coroutineScope.launch {
                                        for (i in 1..20) {
                                            delay(100)
                                            adProgress = i / 20f
                                        }
                                        UserProfileManager.markAsSupporter(context, viaAd = true)
                                        isWatchingAd = false
                                        Toast.makeText(
                                            context,
                                            if (isFa) "حمایت شما ثبت شد! نشان «کاربر مهربون» در پروفایل فعال گردید ❤️" else "Supported! Kind User badge unlocked ❤️",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onSupportSuccess()
                                    }
                                }
                                .padding(16.dp)
                                .testTag("support_via_ad_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF4081)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isFa) "تماشای یک تبلیغ (حمایت رایگان)" else "Watch 1 Ad (Free Support)",
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFFD700))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isFa) "۴۸ ساعت نشان طلایی" else "48h Badge",
                                                    color = Color.Black,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (isFa) "با صرف ۲۰ ثانیه، یک حمایت خیلی بزرگ و دلگرم‌کننده انجام می‌دهید ❤️" else "Spend 20s to warmly support the app's creator!",
                                            color = Color(0xFFC0C5D8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Support Option 2: Coffee
                        SupportOptionCard(
                            emoji = "☕",
                            title = if (isFa) "یک فنجان قهوه گرم" else "Buy a Warm Coffee",
                            desc = if (isFa) "۵۰,۰۰۰ تومان • انرژی برای کدنویسی" else "$1.99 • Coding Fuel",
                            onClick = {
                                UserProfileManager.markAsSupporter(context, viaAd = false)
                                Toast.makeText(
                                    context,
                                    if (isFa) "نوش جان! بی‌نهایت از حمایت شیرین شما سپاسگزاریم ☕❤️" else "Thank you for the coffee! ❤️",
                                    Toast.LENGTH_LONG
                                ).show()
                                onSupportSuccess()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Support Option 3: Pizza
                        SupportOptionCard(
                            emoji = "🍕",
                            title = if (isFa) "یک پیتزای دورهمی" else "Buy a Pizza",
                            desc = if (isFa) "۱۵۰,۰۰۰ تومان • حمایت ماندگار و پرانرژی" else "$4.99 • Pizza Party",
                            onClick = {
                                UserProfileManager.markAsSupporter(context, viaAd = false)
                                Toast.makeText(
                                    context,
                                    if (isFa) "خیلی باارزش و دلگرم‌کننده بود! پروفایل شما طلایی شد 🍕✨" else "Huge thanks for your generosity! ✨",
                                    Toast.LENGTH_LONG
                                ).show()
                                onSupportSuccess()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Support Option 4: Golden Sponsor
                        SupportOptionCard(
                            emoji = "🌟",
                            title = if (isFa) "حامی طلایی و ویژه آئورا" else "Golden Patron",
                            desc = if (isFa) "۳۰۰,۰۰۰ تومان • نام شما در قلب برنامه" else "$9.99 • Forever Patron",
                            isHighlight = true,
                            onClick = {
                                UserProfileManager.markAsSupporter(context, viaAd = false)
                                Toast.makeText(
                                    context,
                                    if (isFa) "شما یک فرشته مهربان هستید! دستاورد حامی طلایی باز شد 🌟❤️" else "You are awesome! Golden Patron unlocked 🌟",
                                    Toast.LENGTH_LONG
                                ).show()
                                onSupportSuccess()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Promo / Gift Code Toggle
                    TextButton(
                        onClick = { showPromoBox = !showPromoBox }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFa) "کد هدیه یا معرف دارید؟" else "Have a gift code?",
                            color = palette.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showPromoBox) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = promoCodeInput,
                                onValueChange = { promoCodeInput = it },
                                placeholder = { Text(if (isFa) "مثال: aura2024" else "e.g. aura2024", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = palette.accent,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                )
                            )

                            Button(
                                onClick = {
                                    if (promoCodeInput.trim().isNotEmpty()) {
                                        UserProfileManager.markAsSupporter(context, viaAd = false)
                                        Toast.makeText(
                                            context,
                                            if (isFa) "کد هدیه تایید شد! نشان کاربر مهربون فعال شد ✨" else "Gift code applied! ✨",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onSupportSuccess()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                            ) {
                                Text(if (isFa) "ثبت" else "Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportOptionCard(
    emoji: String,
    title: String,
    desc: String,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isHighlight) Color(0x33FFD700) else Color(0xFF141727))
            .border(
                width = 1.dp,
                color = if (isHighlight) Color(0xFFFFD700).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = desc,
                        color = Color(0xFFA0A5BA),
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
