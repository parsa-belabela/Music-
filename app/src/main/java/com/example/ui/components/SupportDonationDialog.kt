package com.example.ui.components

import android.app.Activity
import android.widget.Toast
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
import com.example.billing.MyketBillingManager
import com.example.data.model.AppLanguage
import com.example.data.repository.UserProfileManager
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass

/**
 * Clean & Heartwarming Donation & Support Dialog:
 * All music player features are 100% free for everyone.
 * Supporting via donation awards the "کاربر مهربون" badge,
 * and donations >= 100,000 Toman unlock the secret "فرشته نجات سازنده 💙" achievement!
 */
@Composable
fun SupportDonationDialog(
    palette: AmbientPalette,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSupportSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isFa = language == AppLanguage.PERSIAN

    var promoCodeInput by remember { mutableStateOf("") }
    var showPromoBox by remember { mutableStateOf(false) }
    var isPurchasing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isPurchasing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .padding(horizontal = 18.dp, vertical = 26.dp),
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

                    // Transparency Message Box
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
                                    "تمامی امکانات پخش موسیقی، اکولایزر، لیریکس و بخش‌های برنامه برای همیشه ۱۰۰٪ رایگان است. حمایت مالی فقط برای دلگرمی و ارتقای برنامه است. با دونیت بالای ۱۰۰ هزار تومان، مدال و پیام اختصاصی «فرشته نجات سازنده 💙» در پروفایل شما درخشان می‌شود!"
                                else
                                    "All music playback features, EQ & lyrics are 100% free for everyone forever. Supporting the developer is purely optional. Donating 100k+ Toman unlocks the secret \"Developer's Guardian Angel 💙\" badge!",
                                color = Color(0xFFE2E4F0),
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Support Option 0: Gummy Bear Package (25,000 Toman)
                    SupportOptionCard(
                        emoji = "🐻",
                        title = if (isFa) "یک آدامس خرسی" else "Buy a Gummy Bear",
                        desc = if (isFa) "۲۵,۰۰۰ تومان • پرداخت درون‌برنامه‌ای مایکت" else "25,000 Toman • Myket Billing",
                        badgeNote = if (isFa) "نشان «کاربر مهربون ❤️»" else "Kind User Badge",
                        onClick = {
                            handleDonationProcess(
                                activity = activity,
                                context = context,
                                sku = MyketBillingManager.SKU_SUPPORT_GUM,
                                amountToman = 25_000,
                                isFa = isFa,
                                onPurchasingStateChange = { isPurchasing = it },
                                onSuccess = onSupportSuccess
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Support Option 1: Coffee Package (50,000 Toman)
                    SupportOptionCard(
                        emoji = "☕",
                        title = if (isFa) "یک فنجان قهوه گرم" else "Buy a Warm Coffee",
                        desc = if (isFa) "۵۰,۰۰۰ تومان • پرداخت درون‌برنامه‌ای مایکت" else "50,000 Toman • Myket Billing",
                        badgeNote = if (isFa) "نشان «کاربر مهربون ❤️»" else "Kind User Badge",
                        onClick = {
                            handleDonationProcess(
                                activity = activity,
                                context = context,
                                sku = MyketBillingManager.SKU_SUPPORT_COFFEE,
                                amountToman = 50_000,
                                isFa = isFa,
                                onPurchasingStateChange = { isPurchasing = it },
                                onSuccess = onSupportSuccess
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Support Option 2: Pizza Package (150,000 Toman) -> Unlocks Guardian Angel!
                    SupportOptionCard(
                        emoji = "🍕",
                        title = if (isFa) "یک پیتزای دورهمی" else "Buy a Pizza",
                        desc = if (isFa) "۱۵۰,۰۰۰ تومان • پرداخت درون‌برنامه‌ای مایکت" else "150,000 Toman • Myket Billing",
                        badgeNote = if (isFa) "آنلاک مدال «فرشته نجات سازنده 💙»" else "Unlocks Guardian Angel Badge 💙",
                        isHighlight = true,
                        onClick = {
                            handleDonationProcess(
                                activity = activity,
                                context = context,
                                sku = MyketBillingManager.SKU_SUPPORT_PIZZA,
                                amountToman = 150_000,
                                isFa = isFa,
                                onPurchasingStateChange = { isPurchasing = it },
                                onSuccess = onSupportSuccess
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Support Option 3: Golden Patron Package (300,000 Toman) -> Unlocks Guardian Angel!
                    SupportOptionCard(
                        emoji = "🌟",
                        title = if (isFa) "حامی طلایی و ویژه آئورا" else "Golden Patron",
                        desc = if (isFa) "۳۰۰,۰۰0 تومان • پرداخت درون‌برنامه‌ای مایکت" else "300,000 Toman • Myket Billing",
                        badgeNote = if (isFa) "آنلاک مدال «فرشته نجات سازنده 💙» + حامی طلایی" else "Unlocks Guardian Angel Badge 💙",
                        isHighlight = true,
                        onClick = {
                            handleDonationProcess(
                                activity = activity,
                                context = context,
                                sku = MyketBillingManager.SKU_SUPPORT_GOLD,
                                amountToman = 300_000,
                                isFa = isFa,
                                onPurchasingStateChange = { isPurchasing = it },
                                onSuccess = onSupportSuccess
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                                        val redeemed = com.example.monetization.EntitlementManager.redeemPromoCode(context, promoCodeInput)
                                        if (redeemed) {
                                            Toast.makeText(
                                                context,
                                                if (isFa) "کد هدیه با موفقیت فعال شد ✨" else "Gift code redeemed successfully! ✨",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            onSupportSuccess()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                if (isFa) "کد واردشده نامعتبر است." else "Invalid gift code.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
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

private fun handleDonationProcess(
    activity: Activity?,
    context: android.content.Context,
    sku: String,
    amountToman: Int,
    isFa: Boolean,
    onPurchasingStateChange: (Boolean) -> Unit,
    onSuccess: () -> Unit
) {
    if (activity != null) {
        onPurchasingStateChange(true)
        MyketBillingManager.launchPurchase(
            activity = activity,
            sku = sku,
            onSuccess = {
                onPurchasingStateChange(false)
                val unlockedAngel = UserProfileManager.recordDonation(context, amountToman)
                if (unlockedAngel) {
                    Toast.makeText(
                        context,
                        if (isFa) "آچیومنت و مدال «فرشته نجات سازنده 💙» آنلاک شد! پیام برنامه‌نویس را در مدال‌ها بخوانید." else "Secret Guardian Angel badge unlocked! 💙",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        if (isFa) "بی‌نهایت از حمایت شیرین شما سپاسگزاریم ❤️" else "Thank you for your warm support! ❤️",
                        Toast.LENGTH_LONG
                    ).show()
                }
                onSuccess()
            },
            onError = { errorMsg ->
                onPurchasingStateChange(false)
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        Toast.makeText(
            context,
            if (isFa) "امکان برقراری ارتباط با محیط برنامه جهت پرداخت وجود ندارد." else "Activity unavailable for payment flow.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
private fun SupportOptionCard(
    emoji: String,
    title: String,
    desc: String,
    badgeNote: String? = null,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isHighlight) Color(0x3300E676) else Color(0xFF141727))
            .border(
                width = 1.2.dp,
                color = if (isHighlight) Color(0xFF00E676).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("support_option_card")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        if (badgeNote != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isHighlight) Color(0xFF00E676) else Color(0xFFFF4081))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeNote,
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
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
