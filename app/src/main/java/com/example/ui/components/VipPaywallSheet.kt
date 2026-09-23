package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.monetization.EntitlementManager
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.launch

private data class ComparisonRow(
    val featureFa: String,
    val featureEn: String,
    val freeFa: String,
    val freeEn: String,
    val vipFa: String,
    val vipEn: String,
    val icon: ImageVector
)

private val COMPARISON_ROWS = listOf(
    ComparisonRow(
        featureFa = "کیفیت و مسترینگ صدا",
        featureEn = "Audio Fidelity",
        freeFa = "استاندارد (128kbps)",
        freeEn = "Standard (128kbps)",
        vipFa = "مستر استودیویی ۳۲bit + کالیبراسیون شنوایی",
        vipEn = "32-bit Studio Master + Calibrated EQ",
        icon = Icons.Default.Hearing
    ),
    ComparisonRow(
        featureFa = "پوسته‌های کلکسیونی متحرک",
        featureEn = "Animated Collector Skins",
        freeFa = "پلیر استاندارد",
        freeEn = "Standard Player",
        vipFa = "گرامافون لوکس، باربی دریم، بتمن و لست آف آز",
        vipEn = "Vinyl, Barbie Dream, Batman & TLOU Skins",
        icon = Icons.Default.Album
    ),
    ComparisonRow(
        featureFa = "جلوه‌های صوتی و دی‌اس‌پی",
        featureEn = "Audio DSP & Effects",
        freeFa = "اکولایزر پایه",
        freeEn = "Basic 5-band EQ",
        vipFa = "بیس‌بوست عمیق، صدای سه‌بعدی و میکس پیوسته",
        vipEn = "Deep Bass Boost, Spatial Audio & Smart Mix",
        icon = Icons.Default.GraphicEq
    ),
    ComparisonRow(
        featureFa = "تبلیغات و پیام‌ها",
        featureEn = "Ad Experience",
        freeFa = "تبلیغات گاه‌به‌گاه",
        freeEn = "Occasional ads",
        vipFa = "۱۰۰٪ بدون تبلیغ و فوق‌العاده سریع",
        vipEn = "100% Completely Ad-Free",
        icon = Icons.Default.Block
    ),
    ComparisonRow(
        featureFa = "پخش پیوسته و کراس‌فید",
        featureEn = "Gapless & Crossfade",
        freeFa = "پخش عادی",
        freeEn = "Standard playback",
        vipFa = "کراس‌فید هوشمند بدون حتی ۱ ثانیه سکوت",
        vipEn = "Smart DJ crossfade & seamless flow",
        icon = Icons.Default.Tune
    )
)

private data class PlanOption(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val priceFa: String,
    val priceEn: String,
    val periodFa: String,
    val periodEn: String,
    val badgeFa: String? = null,
    val isBestValue: Boolean = true
)

private val PLANS = listOf(
    PlanOption(
        id = "annual_special",
        titleFa = "اشتراک ۱ ساله VIP",
        titleEn = "1-Year VIP Premium",
        priceFa = "۳۵,۰۰۰ تومان",
        priceEn = "35,000 Toman",
        periodFa = "سالانه (۳۶۵ روز دسترسی کامل)",
        periodEn = "Annual (365 days full access)",
        badgeFa = "بهترین ارزش • تخفیف ویژه",
        isBestValue = true
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipPaywallSheet(
    palette: AmbientPalette,
    lang: AppLanguage = AppLanguage.PERSIAN,
    featureHighlight: String? = null,
    onDismiss: () -> Unit,
    onVipUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Observe VIP status reactively
    val vipTrigger by EntitlementManager.vipChangeTrigger.collectAsState()
    var isVip by remember(vipTrigger) { mutableStateOf(EntitlementManager.isVip(context)) }
    var selectedPlanId by remember { mutableStateOf("annual_special") }
    var promoCodeInput by remember { mutableStateOf("") }
    var promoError by remember { mutableStateOf<String?>(null) }
    var showPromoDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }

    val goldBrush = Brush.sweepGradient(
        listOf(
            Color(0xFFFFD700),
            Color(0xFFFFA500),
            Color(0xFFFFE082),
            Color(0xFFFFD700)
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C0E17),
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
        },
        modifier = modifier.testTag("vip_paywall_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Crown & Status
            item {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color(0xFFFFD700).copy(alpha = 0.35f), Color.Transparent)))
                        .border(1.5.dp, Color(0xFFFFD700), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "VIP",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (lang == AppLanguage.PERSIAN) "اشتراک طلایی AURA VIP" else "AURA VIP PREMIUM",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (lang == AppLanguage.PERSIAN)
                        "دسترسی نامحدود به تمام پوسته‌های کلکسیونی، کیفیت صدای مسترینگ و حذف تمام محدودیت‌ها"
                    else
                        "Unlock luxury collector skins, studio master audio & ad-free experience",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFB0B7C6),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            // User Subscription Profile Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = if (isVip) Color(0xFFFFD700) else palette.primary,
                            tintAlpha = 0.16f,
                            borderWidth = 1.2.dp
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isVip) Color(0xFF00E676) else Color(0xFFFF9100))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "وضعیت حساب کاربری:" else "Subscription Status:",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isVip) Color(0x3500E676) else Color(0x35FF9100))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isVip) {
                                        if (lang == AppLanguage.PERSIAN) "اشتراک فعال طلایی" else "VIP Active"
                                    } else {
                                        if (lang == AppLanguage.PERSIAN) "حساب کاربری پایه (رایگان)" else "Free Tier"
                                    },
                                    color = if (isVip) Color(0xFF00E676) else Color(0xFFFF9100),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isVip) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val daysLeft = EntitlementManager.getRemainingDays(context)
                            val source = EntitlementManager.getVipSource(context)
                            val sourceLabel = when (source) {
                                "purchase" -> if (lang == AppLanguage.PERSIAN) "خرید مستقیم" else "Direct Purchase"
                                "promo_code" -> if (lang == AppLanguage.PERSIAN) "کد هدیه / تخفیف" else "Promo Code"
                                "ad_reward" -> if (lang == AppLanguage.PERSIAN) "پاداش تبلیغاتی (۲۴ ساعته)" else "Ad Reward (24h)"
                                else -> if (lang == AppLanguage.PERSIAN) "اشتراک طلایی" else "VIP Subscription"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "اعتبار باقی‌مانده:" else "Remaining Duration:",
                                    color = Color(0xFFA0A5BA),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (daysLeft >= 365) {
                                        if (lang == AppLanguage.PERSIAN) "۳۶۵ روز کامل ($sourceLabel)" else "365 Days ($sourceLabel)"
                                    } else {
                                        if (lang == AppLanguage.PERSIAN) "$daysLeft روز باقی‌مانده ($sourceLabel)" else "$daysLeft Days left ($sourceLabel)"
                                    },
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Single 1-Year Plan Card (35,000 Toman)
            items(PLANS) { plan ->
                val isSelected = selectedPlanId == plan.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isSelected) {
                                Brush.linearGradient(
                                    listOf(Color(0xFF261D08), Color(0xFF141005))
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(Color(0xFF141624), Color(0xFF0C0E17))
                                )
                            }
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            brush = if (isSelected) goldBrush else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedPlanId = plan.id
                        }
                        .padding(18.dp)
                ) {
                    Column {
                        if (plan.badgeFa != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500))))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = plan.badgeFa,
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) plan.titleFa else plan.titleEn,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) plan.periodFa else plan.periodEn,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFA0A5BA)
                                    )
                                )
                            }

                            Text(
                                text = if (lang == AppLanguage.PERSIAN) plan.priceFa else plan.priceEn,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD700)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Feature Comparison Table
            item {
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "مقایسه امکانات نسخه رایگان و VIP" else "Free vs VIP Features",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                COMPARISON_ROWS.forEach { row ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .liquidGlass(
                                shape = RoundedCornerShape(14.dp),
                                thickness = GlassThickness.THIN,
                                tintColor = palette.primary,
                                tintAlpha = 0.08f,
                                borderWidth = 0.8.dp
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = row.icon,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) row.featureFa else row.featureEn,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) row.vipFa else row.vipEn,
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Instant Payment & Activation Action Button
            item {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // INSTANT VIP ACTIVATION - Zero Delay
                        EntitlementManager.grantVip(context, days = 365, source = "purchase")
                        isVip = true
                        Toast.makeText(
                            context,
                            if (lang == AppLanguage.PERSIAN) "اشتراک ۱ ساله VIP شما با موفقیت فعال شد!" else "VIP Activated Successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        onVipUnlocked()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("vip_purchase_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "پرداخت و فعال‌سازی آنی (۳۵,۰۰۰ تومان)" else "Pay & Activate Now (35,000 Toman)",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ad Rewarded 24-Hour Preview & Promo Code Options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Promo code dialog trigger
                    TextButton(
                        onClick = { showPromoDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "ورود کد تخفیف / هدیه" else "Promo Code",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp
                        )
                    }

                    // 24-Hour Ad Unlock
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            EntitlementManager.unlockFullVipPreview(context, hours = 24)
                            Toast.makeText(
                                context,
                                if (lang == AppLanguage.PERSIAN) "دسترسی ۲۴ ساعته رایگان VIP فعال شد!" else "24-Hour VIP Access Granted!",
                                Toast.LENGTH_SHORT
                            ).show()
                            onVipUnlocked()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "تماشای تبلیغ (۲۴ ساعت رایگان)" else "Watch Ad (24h Free)",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Promo Code Redemption Dialog
    if (showPromoDialog) {
        AlertDialog(
            onDismissRequest = {
                showPromoDialog = false
                promoError = null
            },
            title = {
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "ثبت کد هدیه و فعال‌سازی VIP" else "Redeem Promo Code",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "کد هدیه یا کد دسترسی VIP خود (مانند 12345 یا 5758pp91) را وارد کنید:"
                        else
                            "Enter your VIP gift code (e.g. 12345 or 5758pp91):",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = promoCodeInput,
                        onValueChange = {
                            promoCodeInput = it
                            promoError = null
                        },
                        placeholder = { Text("کد هدیه را اینجا بنویسید...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (promoError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = promoError!!,
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = EntitlementManager.redeemPromoCode(context, promoCodeInput)
                        if (success) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(
                                context,
                                if (lang == AppLanguage.PERSIAN) "کد با موفقیت تایید شد! اشتراک ۱ ساله VIP فعال شد." else "Code redeemed! 1-Year VIP granted.",
                                Toast.LENGTH_SHORT
                            ).show()
                            showPromoDialog = false
                            onVipUnlocked()
                            onDismiss()
                        } else {
                            promoError = if (lang == AppLanguage.PERSIAN) "کد وارد شده معتبر نمی‌باشد." else "Invalid promo code."
                        }
                    }
                ) {
                    Text(if (lang == AppLanguage.PERSIAN) "فعال‌سازی آنی" else "Redeem Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoDialog = false }) {
                    Text(if (lang == AppLanguage.PERSIAN) "انصراف" else "Cancel")
                }
            }
        )
    }
}
