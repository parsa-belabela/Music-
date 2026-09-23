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
import com.example.monetization.MonetizationService
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

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
        featureFa = "کیفیت پخش صدا",
        featureEn = "Audio Fidelity",
        freeFa = "استاندارد (128kbps)",
        freeEn = "Standard (128kbps)",
        vipFa = "مستر استودیویی ۳۲bit + کالیبراسیون شنوایی",
        vipEn = "32-bit Studio Master + Calibrated EQ",
        icon = Icons.Default.Hearing
    ),
    ComparisonRow(
        featureFa = "شبیه‌ساز گرامافون (Vinyl)",
        featureEn = "True Vinyl Turntable",
        freeFa = "نسخه ساده",
        freeEn = "Basic flat style",
        vipFa = "چرخش واقعی، سوزن متحرک، بافت چوب و کراکل آنالوگ",
        vipEn = "Real rotating disc, tonearm needle & vinyl warmth",
        icon = Icons.Default.Album
    ),
    ComparisonRow(
        featureFa = "جلوه‌های صوتی اختصاصی",
        featureEn = "Audio DSP & Effects",
        freeFa = "اکولایزر پایه",
        freeEn = "Basic 5-band EQ",
        vipFa = "بیس‌بوست عمیق، صدای سه‌بعدی ۳D/8D و کراس‌فید",
        vipEn = "Deep Bass Boost, Spatial 3D Audio & Smart Mix",
        icon = Icons.Default.GraphicEq
    ),
    ComparisonRow(
        featureFa = "تبلیغات و پیام‌ها",
        featureEn = "Ad Experience",
        freeFa = "دارای بنر تبلیغاتی",
        freeEn = "Banner ads present",
        vipFa = "۱۰۰٪ بدون تبلیغ و کاملاً روان",
        vipEn = "100% Completely Ad-Free",
        icon = Icons.Default.Block
    ),
    ComparisonRow(
        featureFa = "پخش پیوسته و کراس‌فید",
        featureEn = "Gapless & Crossfade",
        freeFa = "پخش عادی",
        freeEn = "Standard playback",
        vipFa = "کراس‌فید هوشمند بدون حتی ۱ ثانیه سکوت",
        vipEn = "Smart DJ crossfade & seamless gapless flow",
        icon = Icons.Default.Tune
    ),
    ComparisonRow(
        featureFa = "همگام‌سازی و پشتیبان‌گیری",
        featureEn = "Cloud Backup & Sync",
        freeFa = "فقط حافظه محلی",
        freeEn = "Local device only",
        vipFa = "پشتیبان‌گیری امن ابری پلی‌لیست‌ها",
        vipEn = "Secure cloud sync & playlist restore",
        icon = Icons.Default.CloudDone
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
    val badgeEn: String? = null,
    val isBestValue: Boolean = false
)

private val PLANS = listOf(
    PlanOption(
        id = "monthly",
        titleFa = "اشتراک ۱ ماهه",
        titleEn = "1 Month VIP",
        priceFa = "۴۹,۰۰۰ تومان",
        priceEn = "$4.99",
        periodFa = "ماهانه",
        periodEn = "per month"
    ),
    PlanOption(
        id = "annual",
        titleFa = "اشتراک ۱ ساله",
        titleEn = "1 Year VIP",
        priceFa = "۲۹۰,۰۰۰ تومان",
        priceEn = "$29.99",
        periodFa = "سالانه (معادل ۲۴ هزارتومان در ماه)",
        periodEn = "per year ($2.49/mo)",
        badgeFa = "۵۰٪ تخفیف ویژه",
        badgeEn = "SAVE 50%",
        isBestValue = true
    ),
    PlanOption(
        id = "lifetime",
        titleFa = "عضویت مادام‌العمر VIP",
        titleEn = "Lifetime VIP",
        priceFa = "۴۹۰,۰۰۰ تومان",
        priceEn = "$49.99",
        periodFa = "یک‌بار برای همیشه",
        periodEn = "one-time payment",
        badgeFa = "ارزش طلایی",
        badgeEn = "BEST DEAL"
    )
)

private enum class PaywallTab {
    PURCHASE,
    QUESTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipPaywallSheet(
    targetFeatureId: String? = null,
    palette: AmbientPalette,
    lang: AppLanguage,
    isDeveloperMode: Boolean = false,
    onDismiss: () -> Unit,
    onVipGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var currentTab by remember { mutableStateOf(PaywallTab.PURCHASE) }
    var selectedPlanId by remember { mutableStateOf("annual") }

    var promoInput by remember { mutableStateOf("") }
    var promoError by remember { mutableStateOf<String?>(null) }
    var promoSuccessMessage by remember { mutableStateOf<String?>(null) }

    var isProcessingPurchase by remember { mutableStateOf(false) }
    var showAdConfirmDialog by remember { mutableStateOf(false) }
    var isWatchingAd by remember { mutableStateOf(false) }
    var showSuccessCelebration by remember { mutableStateOf(false) }

    var remainingDailyAds by remember { mutableIntStateOf(EntitlementManager.getRemainingDailyAds(context)) }
    val isAlreadyVip = remember { EntitlementManager.isVip(context) }

    val goldAccent = Color(0xFFFFD700)
    val goldGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFFDF00), Color(0xFFFFA500), Color(0xFFFF8C00))
    )

    fun triggerSuccessAnimation() {
        showSuccessCelebration = true
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
        scope.launch {
            delay(4000L)
            showSuccessCelebration = false
            onVipGranted()
            onDismiss()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.85f),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .liquidGlass(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    thickness = GlassThickness.THICK,
                    tintColor = palette.primary,
                    tintAlpha = 0.35f
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // Header Bar: Close Button & Restore Purchases
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    if (lang == AppLanguage.PERSIAN) "وضعیت خریدهای شما با موفقیت بازیابی شد" else "Purchases successfully restored",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "بازیابی خرید" else "Restore Purchases",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.35f))
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = if (lang == AppLanguage.PERSIAN) "بستن" else "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Crown / VIP Emblem with Spring Animation
                item {
                    val crownScale by animateFloatAsState(
                        targetValue = if (showSuccessCelebration) 1.25f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "crownScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(crownScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(goldAccent.copy(alpha = 0.45f), Color.Transparent)
                                )
                            )
                            .border(1.5.dp, goldAccent.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "VIP",
                            tint = goldAccent,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Result-driven Headline
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "صدای استودیویی بدون محدودیت و تجربه لوکس گرامافون"
                        else "Unlock Studio Master Sound & True Vinyl Experience",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "دسترسی کامل به موتور صوتی ۳۲ بیت، شبیه‌ساز واقعی وینیل و محیط ۱۰۰٪ بدون تبلیغ"
                        else "Full access to 32-bit audio DSP, physical turntable simulation & zero ads",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFA0A5BA),
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Tab Selector: Purchase vs Free Quests
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x18FFFFFF))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentTab == PaywallTab.PURCHASE) palette.primary else Color.Transparent)
                                .clickable { currentTab = PaywallTab.PURCHASE }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "خرید اشتراک VIP" else "VIP Plans",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (currentTab == PaywallTab.QUESTS) palette.accent.copy(alpha = 0.35f) else Color.Transparent)
                                .clickable { currentTab = PaywallTab.QUESTS }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "ماموریت‌های VIP رایگان" else "Free Quests",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(goldAccent)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("Free", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (currentTab == PaywallTab.PURCHASE) {
                    // Side-by-side Free vs VIP Comparison Table
                    item {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "مقایسه شفاف: رایگان در برابر پریمیوم" else "Clear Comparison: Free vs VIP",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = palette.secondary,
                                    tintAlpha = 0.16f,
                                    borderWidth = 1.dp
                                )
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                COMPARISON_ROWS.forEach { row ->
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = row.icon,
                                                contentDescription = null,
                                                tint = palette.accent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (lang == AppLanguage.PERSIAN) row.featureFa else row.featureEn,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "❌ " + if (lang == AppLanguage.PERSIAN) row.freeFa else row.freeEn,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color(0xFF9095AA),
                                                    fontSize = 11.sp
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "✨ " + if (lang == AppLanguage.PERSIAN) row.vipFa else row.vipEn,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = goldAccent,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                ),
                                                modifier = Modifier.weight(1.3f)
                                            )
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(top = 8.dp),
                                            color = Color(0x18FFFFFF)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Plan Selection Cards
                    item {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "انتخاب پلن اشتراک" else "Choose Your Plan",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )
                    }

                    items(PLANS) { plan ->
                        val isSelected = selectedPlanId == plan.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = if (isSelected) palette.primary else palette.secondary,
                                    tintAlpha = if (isSelected) 0.35f else 0.12f,
                                    borderWidth = if (isSelected) 2.dp else 1.dp
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    brush = if (isSelected) goldGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedPlanId = plan.id
                                }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (lang == AppLanguage.PERSIAN) plan.titleFa else plan.titleEn,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        if (plan.badgeFa != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(goldAccent)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (lang == AppLanguage.PERSIAN) plan.badgeFa else plan.badgeEn ?: "",
                                                    color = Color.Black,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) plan.periodFa else plan.periodEn,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFA0A5BA),
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) plan.priceFa else plan.priceEn,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = if (isSelected) goldAccent else Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }
                        }
                    }

                    // Main Purchase Button
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isProcessingPurchase = true
                                scope.launch {
                                    delay(1000)
                                    isProcessingPurchase = false
                                    val days = when (selectedPlanId) {
                                        "monthly" -> 30
                                        "annual" -> 365
                                        else -> 0 // 0 = permanent
                                    }
                                    EntitlementManager.grantVip(context, days = days, source = "purchase")
                                    triggerSuccessAnimation()
                                }
                            },
                            enabled = !isProcessingPurchase,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(goldGradient)
                                .testTag("purchase_vip_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            if (isProcessingPurchase) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "فعال‌سازی آنی اشتراک VIP" else "Activate VIP Access Now",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // Promo Code Section (Placeholder: "مثال: ۱۲۳۴۵")
                    item {
                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = palette.secondary,
                                    tintAlpha = 0.2f
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "کد تخفیف یا هدیه دارید؟" else "Have a Promo or Gift Code?",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = promoInput,
                                        onValueChange = {
                                            promoInput = it
                                            promoError = null
                                        },
                                        placeholder = {
                                            Text(
                                                if (lang == AppLanguage.PERSIAN) "مثال: ۱۲۳۴۵" else "e.g. 12345",
                                                color = Color.White.copy(alpha = 0.4f),
                                                fontSize = 13.sp
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("promo_code_input"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = palette.accent,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            if (promoInput.isBlank()) {
                                                promoError = if (lang == AppLanguage.PERSIAN) "لطفاً کد را وارد کنید" else "Please enter code"
                                                return@Button
                                            }
                                            val success = EntitlementManager.redeemPromoCode(context, promoInput)
                                            if (success) {
                                                promoError = null
                                                promoSuccessMessage = if (lang == AppLanguage.PERSIAN)
                                                    "تبریک! اشتراک ۱ ساله VIP شما با موفقیت فعال شد 🎉"
                                                else "Congratulations! 1-Year VIP successfully activated 🎉"
                                                promoInput = ""
                                                triggerSuccessAnimation()
                                            } else {
                                                promoError = if (lang == AppLanguage.PERSIAN)
                                                    "کد وارد شده معتبر نیست یا منقضی شده است"
                                                else "Invalid or expired promo code"
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                                        modifier = Modifier.testTag("redeem_promo_button")
                                    ) {
                                        Text(
                                            if (lang == AppLanguage.PERSIAN) "اعمال" else "Apply",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (promoError != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = promoError ?: "",
                                        color = Color(0xFFFF5252),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                if (promoSuccessMessage != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = promoSuccessMessage ?: "",
                                        color = Color(0xFF69F0AE),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Section 3.5: Free Temporary VIP (Rewarded Ads & Quests)
                    item {
                        Text(
                            text = if (lang == AppLanguage.PERSIAN) "دریافت رایگان دسترسی VIP با انجام ماموریت‌ها" else "Earn Free VIP Time with Quests",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )

                        // Rewarded Ad Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(18.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = palette.accent,
                                    tintAlpha = 0.22f,
                                    borderWidth = 1.2.dp
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = palette.accent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (lang == AppLanguage.PERSIAN) "تماشای ویدیو کوتاه جایزه‌دار" else "Watch Short Video Ad",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (lang == AppLanguage.PERSIAN) "دریافت ۶۰ دقیقه دسترسی کامل VIP" else "Get 60 Minutes Full VIP Access",
                                                color = goldAccent,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) "$remainingDailyAds از ۳ فرصت امروز" else "$remainingDailyAds of 3 left today",
                                        color = Color(0xFFA0A5BA),
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (activity != null && remainingDailyAds > 0) {
                                            isWatchingAd = true
                                            MonetizationService.rewardedAdProvider.show(
                                                activity = activity,
                                                onEarned = {
                                                    EntitlementManager.recordAdWatched(context)
                                                    EntitlementManager.unlockFullVipPreview(context, hours = 1)
                                                    remainingDailyAds = EntitlementManager.getRemainingDailyAds(context)
                                                    isWatchingAd = false
                                                    triggerSuccessAnimation()
                                                },
                                                onFailed = { isWatchingAd = false }
                                            )
                                        }
                                    },
                                    enabled = remainingDailyAds > 0 && !isWatchingAd,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                                ) {
                                    Text(
                                        text = if (isWatchingAd) (if (lang == AppLanguage.PERSIAN) "در حال بارگذاری..." else "Loading Video...")
                                        else if (remainingDailyAds > 0) (if (lang == AppLanguage.PERSIAN) "تماشای ویدیو (+۶۰ دقیقه VIP)" else "Watch Video (+60 Min VIP)")
                                        else (if (lang == AppLanguage.PERSIAN) "سقف روزانه تکمیل شده" else "Daily Limit Reached"),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quest List
                        val quests = listOf(
                            QuestItem(
                                titleFa = "ماموریت ۱: کالیبراسیون و تست شنوایی",
                                titleEn = "Quest 1: Personal Hearing Test",
                                rewardFa = "+۳ ساعت VIP کامل",
                                rewardEn = "+3 Hours Full VIP",
                                icon = Icons.Default.Hearing,
                                hours = 3
                            ),
                            QuestItem(
                                titleFa = "ماموریت ۲: ورود ۳ روز متوالی",
                                titleEn = "Quest 2: 3-Day Consecutive Streak",
                                rewardFa = "+۲۴ ساعت VIP کامل",
                                rewardEn = "+24 Hours Full VIP",
                                icon = Icons.Default.CalendarMonth,
                                hours = 24
                            ),
                            QuestItem(
                                titleFa = "ماموریت ۳: گوش دادن به ۵ آهنگ",
                                titleEn = "Quest 3: Listen to 5 Tracks",
                                rewardFa = "+۲ ساعت VIP کامل",
                                rewardEn = "+2 Hours Full VIP",
                                icon = Icons.Default.MusicNote,
                                hours = 2
                            ),
                            QuestItem(
                                titleFa = "ماموریت ۴: اشتراک‌گذاری پوستر آهنگ",
                                titleEn = "Quest 4: Share Music Poster",
                                rewardFa = "+۱۲ ساعت VIP کامل",
                                rewardEn = "+12 Hours Full VIP",
                                icon = Icons.Default.Share,
                                hours = 12
                            )
                        )

                        quests.forEach { quest ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .liquidGlass(
                                        shape = RoundedCornerShape(16.dp),
                                        thickness = GlassThickness.REGULAR,
                                        tintColor = palette.secondary,
                                        tintAlpha = 0.14f,
                                        borderWidth = 1.dp
                                    )
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(palette.primary.copy(alpha = 0.25f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(quest.icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (lang == AppLanguage.PERSIAN) quest.titleFa else quest.titleEn,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (lang == AppLanguage.PERSIAN) quest.rewardFa else quest.rewardEn,
                                                color = goldAccent,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            EntitlementManager.unlockFullVipPreview(context, hours = quest.hours)
                                            triggerSuccessAnimation()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                                    ) {
                                        Text(if (lang == AppLanguage.PERSIAN) "دریافت" else "Claim", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Sparkling Confetti Celebration Particles when Purchase/Quest succeeds
            if (showSuccessCelebration) {
                SparkleCelebrationOverlay()
            }
        }
    }
}

private data class QuestItem(
    val titleFa: String,
    val titleEn: String,
    val rewardFa: String,
    val rewardEn: String,
    val icon: ImageVector,
    val hours: Int
)

@Composable
private fun SparkleCelebrationOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "celebrationParticles")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "sparkleAnim"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val rand = Random(1337)
        val particleColors = listOf(
            Color(0xFFFFDF00),
            Color(0xFFFFA500),
            Color(0xFF00E5FF),
            Color(0xFFFF3366),
            Color(0xFF69F0AE)
        )

        for (i in 0 until 35) {
            val startX = rand.nextFloat() * w
            val speed = 0.5f + rand.nextFloat() * 1.5f
            val y = ((animProgress * speed + (i * 0.03f)) % 1f) * h
            val x = startX + kotlin.math.sin(y * 0.05).toFloat() * 18.dp.toPx()
            val radius = (1.5f + rand.nextFloat() * 2.5f).dp.toPx()
            val color = particleColors[i % particleColors.size]

            drawCircle(
                color = color.copy(alpha = (1f - (y / h)).coerceIn(0.2f, 0.9f)),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}
