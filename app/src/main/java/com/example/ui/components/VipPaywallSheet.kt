package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

private data class VipBenefit(
    val icon: ImageVector,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val descEn: String
)

private val VIP_BENEFITS = listOf(
    VipBenefit(
        Icons.Default.Palette,
        "۵ تم لوکس با متریال شیشه‌ای",
        "5 Exclusive Glass Themes",
        "ولوت نوآر سلطنتی، ماتریکس فسفری، کروم مایع Y2K و سیاه و سفید کریستالی",
        "Royal Velvet Noir, Digital Acid, Liquid Chrome and Monochrome Noir"
    ),
    VipBenefit(
        Icons.Default.Album,
        "صفحه پخش اختصاصی گرامافون (وینیل)",
        "Vinyl Turntable Player",
        "چرخش واقع‌گرایانه صفحه وینیل، بازوی مکانیکی و لرزش ملایم آنالوگ",
        "Realistic vinyl record rotation with analog needle tracking"
    ),
    VipBenefit(
        Icons.Default.GraphicEq,
        "۷ ویژوالایزر پیشرفته و پویا",
        "7 Advanced Reactive Visualizers",
        "اسپکتروم دایره‌ای، شفق قطبی، حلقه ضربانی و ذره‌های کیهانی",
        "Circular Spectrum, Aurora, Pulse Ring, Liquid and Particle Field"
    ),
    VipBenefit(
        Icons.Default.Hearing,
        "کالیبراسیون و تست شنوایی هوشمند",
        "Hearing Calibration Profile",
        "تنظیم اختصاصی فرکانس‌های صوتی مطابق با گوش چپ و راست شما",
        "Custom frequency equalization tailored to your exact hearing curve"
    ),
    VipBenefit(
        Icons.Default.Tune,
        "میکس پیوسته دی‌جی (Continuous Mix)",
        "Continuous DJ Mix",
        "ترکیب و کراس‌فید بدون وقفه قطعات شبیه دی‌جی‌های کلاب",
        "Seamless cross-track mixing without any dead silence"
    ),
    VipBenefit(
        Icons.Default.Share,
        "حذف واترمارک از پوسترهای اشتراک",
        "Watermark-Free Share Cards",
        "اشتراک‌گذاری قطعات با پوستر تمیز بدون لوگوی برنامه",
        "Export and share pristine music cards without watermarks"
    ),
    VipBenefit(
        Icons.Default.AutoAwesome,
        "قاب کریستالی طلایی دور کاور آهنگ",
        "Exclusive VIP Crystal Aura",
        "جلوه انحصاری درخشان دور آلبوم در هنگام پخش موزیک",
        "Luminous gold & crystal aura around the album art"
    )
)

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

    var promoInput by remember { mutableStateOf("") }
    var promoError by remember { mutableStateOf<String?>(null) }
    var promoSuccessMessage by remember { mutableStateOf<String?>(null) }

    var isProcessingPurchase by remember { mutableStateOf(false) }
    var showAdConfirmDialog by remember { mutableStateOf(false) }
    var isWatchingAd by remember { mutableStateOf(false) }

    val remainingDailyAds = remember { EntitlementManager.getRemainingDailyAds(context) }
    val isAlreadyVip = remember { EntitlementManager.isVip(context) }

    val goldAccent = Color(0xFFFFD700)
    val goldGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFFDF00), Color(0xFFFFA500), Color(0xFFFF8C00))
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.75f),
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
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {
                // Header Bar: Drag Handle and Close Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.size(36.dp))
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(5.dp)
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
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Crown / VIP Emblem
                item {
                    val infiniteTransition = rememberInfiniteTransition(label = "vipGlow")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(goldAccent.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )
                            .border(1.5.dp, goldAccent.copy(alpha = 0.75f), CircleShape),
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

                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "اشتراک ویژه آئورا VIP" else "Aura VIP Subscription",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val triggerTitle = targetFeatureId?.let { EntitlementManager.getFeatureTitle(it, lang == AppLanguage.PERSIAN) }
                    Text(
                        text = if (triggerTitle != null) {
                            if (lang == AppLanguage.PERSIAN) "برای دسترسی کامل به «$triggerTitle» و تمامی امکانات لوکس"
                            else "To unlock \"$triggerTitle\" & all premium features"
                        } else {
                            if (lang == AppLanguage.PERSIAN) "نهایت شفافیت شیشه‌ای و قدرت صوتی بدون هیچ مرز و مانع"
                            else "Experience uncompromised liquid sound & aesthetics"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFC8C8DF),
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Promo Success Banner (if redeemed)
                item {
                    Column {
                        AnimatedVisibility(visible = promoSuccessMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x334CAF50))
                                    .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784))
                                    Text(
                                        text = promoSuccessMessage ?: "",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }

                // 24-Hour Preview via Rewarded Ad (Hero Card)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(22.dp),
                                thickness = GlassThickness.REGULAR,
                                tintColor = palette.accent,
                                tintAlpha = 0.24f
                            )
                            .border(1.2.dp, Color(0x66FFD700), RoundedCornerShape(22.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(goldAccent.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleFilled,
                                        contentDescription = null,
                                        tint = goldAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) "۲۴ ساعت همه‌چیز رو امتحان کن ✨" else "Try Everything for 24 Hours ✨",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    )
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) "با دیدن یک ویدیوی کوتاه (کل پکیج VIP باز می‌شود)"
                                        else "By watching a short video (unlocks entire VIP catalog)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFFC0C0D4),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN)
                                        "سقف روزانه: $remainingDailyAds از ${EntitlementManager.MAX_DAILY_ADS} تبلیغ باقی‌مانده"
                                    else
                                        "Daily allowance: $remainingDailyAds of ${EntitlementManager.MAX_DAILY_ADS} left",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (remainingDailyAds > 0) Color(0xFF90E0EF) else Color(0xFFFF8A80),
                                        fontSize = 11.sp
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (remainingDailyAds > 0) {
                                            showAdConfirmDialog = true
                                        } else {
                                            Toast.makeText(
                                                context,
                                                if (lang == AppLanguage.PERSIAN)
                                                    "سقف تماشای تبلیغ امروز پر شده است. فردا دوباره شارژ می‌شود."
                                                else "Daily limit reached. Resets tomorrow.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    enabled = remainingDailyAds > 0 && !isWatchingAd,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = goldAccent,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("rewarded_ad_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartDisplay,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) "تماشا و بازگشایی" else "Watch & Unlock",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Annual Subscription Plan Card
                item {
                    val formattedPrice = MonetizationService.billingProvider.getFormattedPrice()
                    val dailyBreakdown = MonetizationService.billingProvider.getDailyBreakdownPrice()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(22.dp),
                                thickness = GlassThickness.THICK,
                                tintColor = palette.primary,
                                tintAlpha = 0.38f
                            )
                            .border(1.5.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == AppLanguage.PERSIAN) "اشتراک سالانه طلایی" else "Annual Golden Access",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Surface(
                                    color = palette.accent.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN) "بهترین ارزش" else "BEST VALUE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = palette.accent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = formattedPrice,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "($dailyBreakdown)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = goldAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    isProcessingPurchase = true
                                    scope.launch {
                                        val success = activity?.let {
                                            MonetizationService.billingProvider.purchaseAnnualVip(it)
                                        } ?: false

                                        isProcessingPurchase = false
                                        if (success) {
                                            EntitlementManager.grantVip(context, days = 365, source = "purchase")
                                            promoSuccessMessage = if (lang == AppLanguage.PERSIAN)
                                                "خرید اشتراک سالانه با موفقیت انجام شد! 🎉"
                                            else "Annual VIP subscription activated! 🎉"
                                            onVipGranted()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                if (lang == AppLanguage.PERSIAN)
                                                    "درگاه پرداخت در حالت آماده‌سازی است. برای تست می‌توانید از کد تخفیف یا تبلیغ جایزه‌ای استفاده کنید."
                                                else "Billing provider is in stub mode. Use promo code or rewarded ad to test.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("purchase_vip_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary
                                )
                            ) {
                                if (isProcessingPurchase) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (lang == AppLanguage.PERSIAN)
                                            "فعال‌سازی آنی اشتراک (۳۵٬۰۰۰ تومان)"
                                        else "Activate Annual VIP (35,000 Tomans)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Features breakdown list
                item {
                    Text(
                        text = if (lang == AppLanguage.PERSIAN) "آنچه با اشتراک VIP به دست می‌آورید" else "What You Get with Aura VIP",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color(0xFFC0C0D8),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                items(VIP_BENEFITS) { benefit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(palette.primary.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = benefit.icon,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) benefit.titleFa else benefit.titleEn,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            )
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) benefit.descFa else benefit.descEn,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFA0A0B8),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Promo Code Section
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
                                            onVipGranted()
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
                                        text = if (lang == AppLanguage.PERSIAN) "اعمال" else "Redeem",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (promoError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = promoError!!,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFFF6B6B),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Transparent Legal & Store Policy Disclosure
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = if (lang == AppLanguage.PERSIAN)
                            "شفافیت و شرایط لغو: اشتراک به‌صورت سالانه بوده و در پایان دوره تمدید می‌شود. لغو اشتراک در هر زمان از بخش حساب کاربری کافه‌بازار یا تنظیمات گوگل‌پلی به‌سادگی و بدون هزینه اضافی امکان‌پذیر است."
                        else
                            "Transparent Policy: Subscriptions renew annually. You can cancel auto-renewal at any time through your Google Play or Bazaar Account settings without penalty.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF88889C),
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Developer / Testing simulation tool (only visible in dev mode or as safe fallback)
                if (isDeveloperMode) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                EntitlementManager.unlockFullVipPreview(context, hours = 24)
                                promoSuccessMessage = if (lang == AppLanguage.PERSIAN)
                                    "نسخه VIP برای ۲۴ ساعت در حالت توسعه‌دهنده فعال شد (Debug Pass)"
                                else "24h VIP unlocked in Developer Mode"
                                onVipGranted()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("dev_unlock_vip_button")
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = goldAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (lang == AppLanguage.PERSIAN) "شبیه‌سازی بازگشایی ۲۴ ساعته (حالت دولوپر)" else "Simulate 24h Unlock (Dev)",
                                color = goldAccent,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before playing rewarded video (Policy & Trust transparency)
    if (showAdConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAdConfirmDialog = false },
            title = {
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "مشاهده ویدیوی جایزه‌ای" else "Watch Rewarded Video",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (lang == AppLanguage.PERSIAN)
                        "با مشاهده یک ویدیوی تبلیغاتی کوتاه، تمام امکانات VIP (تم‌ها، گرامافون، ویژوالایزرها و میکس دی‌جی) به مدت ۲۴ ساعت برای شما بازگشایی می‌شوند. آیا مایل به ادامه هستید؟"
                    else
                        "By watching a short video, all VIP features will be unlocked for 24 hours. Would you like to proceed?",
                    color = Color(0xFFD0D0E0),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAdConfirmDialog = false
                        isWatchingAd = true
                        if (activity != null) {
                            MonetizationService.rewardedAdProvider.show(
                                activity = activity,
                                onEarned = {
                                    isWatchingAd = false
                                    EntitlementManager.recordAdWatched(context)
                                    EntitlementManager.unlockFullVipPreview(context, hours = 24)
                                    promoSuccessMessage = if (lang == AppLanguage.PERSIAN)
                                        "همه امکانات VIP برای ۲۴ ساعت با موفقیت باز شد! لذت ببرید ✨"
                                    else "All VIP features unlocked for 24 hours! Enjoy ✨"
                                    onVipGranted()
                                },
                                onFailed = { err ->
                                    isWatchingAd = false
                                    // Simulation fallback
                                    EntitlementManager.recordAdWatched(context)
                                    EntitlementManager.unlockFullVipPreview(context, hours = 24)
                                    promoSuccessMessage = if (lang == AppLanguage.PERSIAN)
                                        "همه امکانات VIP برای ۲۴ ساعت باز شد (نسخه شبیه‌سازی) ✨"
                                    else "VIP preview unlocked for 24 hours (simulated) ✨"
                                    onVipGranted()
                                }
                            )
                        } else {
                            isWatchingAd = false
                            EntitlementManager.recordAdWatched(context)
                            EntitlementManager.unlockFullVipPreview(context, hours = 24)
                            promoSuccessMessage = if (lang == AppLanguage.PERSIAN)
                                "همه امکانات VIP برای ۲۴ ساعت باز شد ✨"
                            else "VIP preview unlocked for 24 hours ✨"
                            onVipGranted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = goldAccent, contentColor = Color.Black)
                ) {
                    Text(if (lang == AppLanguage.PERSIAN) "تماشای ویدیو" else "Watch Video", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdConfirmDialog = false }) {
                    Text(if (lang == AppLanguage.PERSIAN) "انصراف" else "Cancel", color = Color(0xFFA0A0B0))
                }
            },
            containerColor = Color(0xFF141728),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
