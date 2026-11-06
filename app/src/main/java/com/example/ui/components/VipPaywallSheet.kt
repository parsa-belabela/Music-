package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.monetization.EntitlementManager
import com.example.monetization.FeatureGate
import com.example.monetization.RemoteVipConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipPaywallSheet(
    entitlementManager: EntitlementManager? = null,
    highlightedFeature: FeatureGate? = null,
    isFarsi: Boolean = true,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit = {},
    onVipUnlocked: () -> Unit = {},
    onUnlockedSuccess: () -> Unit = onUnlocked,
    onStartMyketPurchase: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val manager = remember(entitlementManager) {
        entitlementManager ?: EntitlementManager.getInstance(context)
    }
    val vipConfig by manager.vipConfig.collectAsState()
    val isVip by manager.isVip.collectAsState()
    val scope = rememberCoroutineScope()

    var promoCode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // دیالوگ تشکر و راهنمای پشتیبانی مایکت
    var showThankYouDialog by remember { mutableStateOf(false) }

    fun notifyUnlocked() {
        onUnlocked()
        onVipUnlocked()
        onUnlockedSuccess()
    }

    LaunchedEffect(Unit) {
        manager.resetPurchaseState()
        manager.refreshRemoteConfig()
    }

    DisposableEffect(Unit) {
        onDispose {
            isLoading = false
            manager.resetPurchaseState()
        }
    }

    if (showThankYouDialog) {
        MyketThankYouDialog(
            onConfirm = {
                showThankYouDialog = false
                notifyUnlocked()
                onDismiss()
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F111A),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVip) "💎 اشتراک VIP شما فعال است" else vipConfig.productTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                }
            }

            if (highlightedFeature != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isFarsi) highlightedFeature.descFa else highlightedFeature.descEn,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF6A1B9A), Color(0xFF283593))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = vipConfig.bannerText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ظرفیت باقی‌مانده: ${vipConfig.remainingCapacity} از ${vipConfig.totalCapacity} نفر",
                        color = Color(0xFFFFD54F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${"%,d".format(vipConfig.originalPriceToman)} تومان",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 15.sp,
                    textDecoration = TextDecoration.LineThrough
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${"%,d".format(vipConfig.discountPriceToman)} تومان (مادام‌العمر)",
                    color = Color(0xFF4ADE80),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ۱. دکمه خرید از مایکت با محافظت در برابر قفل شدن (try / finally + timeout)
            Button(
                onClick = {
                    if (isVip) {
                        notifyUnlocked()
                        onDismiss()
                    } else if (onStartMyketPurchase != null) {
                        onStartMyketPurchase(vipConfig.sku)
                    } else {
                        scope.launch {
                            isLoading = true
                            statusMessage = null
                            try {
                                val intentRes = withTimeoutOrNull(6000L) {
                                    manager.createMyketBuyIntent()
                                }
                                if (intentRes == null) {
                                    isError = true
                                    statusMessage = "پاسخی از سرور مایکت دریافت نشد. دوباره تلاش کنید یا از کد تخفیف استفاده نمایید."
                                } else {
                                    intentRes.onSuccess {
                                        showThankYouDialog = true
                                    }.onFailure { err ->
                                        if (err.message == "ALREADY_OWNED_AND_RESTORED") {
                                            isError = false
                                            statusMessage = "✅ اشتراک قبلی شما شناسایی و فعال شد!"
                                            showThankYouDialog = true
                                        } else {
                                            isError = true
                                            statusMessage = err.message ?: "خطا در اتصال به مایکت"
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                isError = true
                                statusMessage = "خطا در اتصال به مایکت: دوباره تلاش کنید"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && (vipConfig.salesOpen || isVip),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("در حال اتصال به سرور مایکت...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text(
                        text = if (isVip) "✅ شما کاربر VIP پریمیوم هستید (ورود)"
                        else if (vipConfig.salesOpen) "خرید اشتراک پریمیوم از مایکت (${"%,d".format(vipConfig.discountPriceToman)} تومان)"
                        else "ظرفیت فروش تکمیل شده است",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(14.dp))

            // ۲. بخش همیشه بازِ کد تخفیف / کد هدیه (57589188)
            Text(
                text = "🎁 کد تخفیف یا کد فعال‌سازی VIP دارید؟",
                color = Color(0xFFFFD54F),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it },
                    placeholder = { Text("کد تخفیف را وارد کنید...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            val ok = manager.applyPromoCode(promoCode)
                            if (ok) {
                                isError = false
                                statusMessage = "🎉 کد تخفیف اعمال شد! اشتراک پریمیوم مادام‌العمر فعال گردید."
                                notifyUnlocked()
                                onDismiss()
                            } else {
                                isError = true
                                statusMessage = "❌ کد تخفیف وارد شده نامعتبر است."
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val ok = manager.applyPromoCode(promoCode)
                        if (ok) {
                            isError = false
                            statusMessage = "🎉 کد تخفیف اعمال شد! اشتراک پریمیوم مادام‌العمر فعال گردید."
                            notifyUnlocked()
                            onDismiss()
                        } else {
                            isError = true
                            statusMessage = "❌ کد تخفیف وارد شده نامعتبر است."
                        }
                    },
                    enabled = promoCode.isNotBlank(),
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اعمال کد", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isError) Color(0x33EF4444) else Color(0x3322C55E))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) Color(0xFFF87171) else Color(0xFF4ADE80)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusMessage!!,
                        color = if (isError) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MyketThankYouDialog(
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        containerColor = Color(0xFF141824),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🎉 با تشکر از خرید و همراهی شما",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "اشتراک پریمیوم مادام‌العمر شما با موفقیت ثبت شد.",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF231C38))
                        .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "در صورتی که اشتراک برای شما بلافاصله فعال نشده است یا با هرگونه مشکلی مواجه شده‌اید، می‌توانید از طریق گزینه «درخواست آهنگ» در برنامه مشکل خود را مطرح کنید تا سریعاً بررسی و فعال گردد.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
            ) {
                Text("متوجه شدم", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * محتوای صفحه قفل VIP در بخش سرچ آنلاین (OnlineMusicScreen)
 * شامل:
 * ۱. نمایش قیمت ۲۷۵,۰۰۰ تومان به ۷۵,۰۰۰ تومان (مادام‌العمر)
 * ۲. دکمه خرید مستقیم از مایکت بدون قفل شدن روی "در حال اتصال به سرور..."
 * ۳. کادر مستقیم و همیشه بازِ وارد کردن کد تخفیف / هدیه (57589188) که بلافاصله سرچ کلودفلر را باز می‌کند
 */
@Composable
fun AuraPremiumPaywallContent(
    config: RemoteVipConfig = RemoteVipConfig(),
    hardwareId: String = "",
    isLoading: Boolean = false,
    statusMessage: String? = null,
    isError: Boolean = false,
    onBuyClick: () -> Unit = {},
    onRestoreClick: () -> Unit = {},
    onPromoSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val goldAccent = Color(0xFFFFD166)
    val neonCyan = Color(0xFF00E5FF)
    val royalViolet = Color(0xFF7C3AED)
    val emeraldGreen = Color(0xFF10B981)
    val glassBg = Color.White.copy(alpha = 0.06f)
    val glassBorder = Color.White.copy(alpha = 0.14f)

    var promoInput by remember { mutableStateOf("") }
    var promoFeedback by remember { mutableStateOf<String?>(null) }
    var promoIsSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 14.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(goldAccent.copy(alpha = 0.28f), royalViolet.copy(alpha = 0.35f))
                    )
                )
                .border(1.5.dp, goldAccent.copy(alpha = 0.85f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = "VIP",
                tint = goldAccent,
                modifier = Modifier.size(34.dp)
            )
        }

        Text(
            text = "Aura Premium • نسخه بتا",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        if (config.promoBannerText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(royalViolet.copy(alpha = 0.28f), neonCyan.copy(alpha = 0.18f))
                        )
                    )
                    .border(1.dp, neonCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = config.promoBannerText,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // کارت قیمت‌گذاری مادام‌العمر (۲۷۵,۰۰۰ به ۷۵,۰۰۰ تومان)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF191D2E), Color(0xFF121521))
                    )
                )
                .border(1.5.dp, goldAccent.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(goldAccent.copy(alpha = 0.18f))
                    .border(1.dp, goldAccent.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = config.discountBadgeText,
                    color = goldAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = config.originalPriceText,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 15.sp,
                    textDecoration = TextDecoration.LineThrough
                )
                Text(
                    text = config.discountedPriceText,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = "ظرفیت باقی‌مانده: ${config.remainingCapacity} از ${config.totalCapacity} نفر • اشتراک مادام‌العمر",
                color = emeraldGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        // ۱. دکمه اصلی خرید مستقیم از مایکت (با امکان کلیک مجدد)
        Button(
            onClick = onBuyClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = neonCyan,
                contentColor = Color(0xFF050811),
                disabledContainerColor = neonCyan.copy(alpha = 0.5f)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF050811),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "در حال اتصال به سرور مایکت...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050811)
                )
            } else {
                Icon(Icons.Rounded.ShoppingBag, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "خرید اشتراک مادام‌العمر با مایکت (${config.discountedPriceText})",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // پیام وضعیت خرید مایکت
        if (!statusMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isError) Color(0xFFEF4444).copy(alpha = 0.16f) else emeraldGreen.copy(alpha = 0.16f))
                    .border(1.dp, if (isError) Color(0xFFEF4444) else emeraldGreen, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = statusMessage,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ۲. کارت همیشه بازِ وارد کردن کد تخفیف / کد هدیه (57589188) دقیقاً زیر دکمه خرید مایکت
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(glassBg)
                .border(1.dp, goldAccent.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalOffer,
                    contentDescription = null,
                    tint = goldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "🎁 کد تخفیف یا کد فعال‌سازی دارید؟",
                    color = goldAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promoInput,
                    onValueChange = { promoInput = it },
                    placeholder = {
                        Text(
                            text = "کد تخفیف را وارد کنید...",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            val ok = EntitlementManager.redeemPromoCode(context, promoInput)
                            promoIsSuccess = ok
                            if (ok) {
                                promoFeedback = "🎉 کد تخفیف با موفقیت اعمال شد! اشتراک پریمیوم فعال گردید."
                                onPromoSuccess()
                            } else {
                                promoFeedback = "❌ کد تخفیف واردشده نامعتبر است."
                            }
                        }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedBorderColor = neonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val ok = EntitlementManager.redeemPromoCode(context, promoInput)
                        promoIsSuccess = ok
                        if (ok) {
                            promoFeedback = "🎉 کد تخفیف با موفقیت اعمال شد! اشتراک پریمیوم فعال گردید."
                            onPromoSuccess()
                        } else {
                            promoFeedback = "❌ کد تخفیف واردشده نامعتبر است."
                        }
                    },
                    enabled = promoInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = goldAccent,
                        contentColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ثبت کد", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }

            if (!promoFeedback.isNullOrBlank()) {
                Text(
                    text = promoFeedback!!,
                    color = if (promoIsSuccess) emeraldGreen else Color(0xFFEF4444),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // ویژگی‌های ممتاز VIP
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(glassBg)
                .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VipFeatureRow(
                icon = Icons.Rounded.CloudDone,
                tint = neonCyan,
                title = "دسترسی نامحدود به آرشیو ابری موزیک (کلادفلر)",
                subtitle = "پخش آنی و دانلود مستقیم تمام آهنگ‌ها و فول‌آلبوم‌های خواننده‌ها بدون تحریم"
            )
            VipFeatureRow(
                icon = Icons.Rounded.QueueMusic,
                tint = goldAccent,
                title = "ارسال روزانه ۳ درخواست آهنگ اختصاصی",
                subtitle = "هر آهنگی که در آرشیو نبود را درخواست بده تا مستقیم به کانال آرشیو اضافه شود"
            )
            if (hardwareId.isNotBlank()) {
                VipFeatureRow(
                    icon = Icons.Rounded.Security,
                    tint = royalViolet,
                    title = "قفل سخت‌افزاری اختصاصی: $hardwareId",
                    subtitle = "اشتراک به صورت امن روی همین دستگاه شما قفل و فعال می‌شود"
                )
            }
        }

        // دکمه بازیابی خرید قبلی از مایکت
        TextButton(
            onClick = onRestoreClick,
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.Rounded.Restore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "قبلاً روی همین گوشی خریده‌ام (بازیابی اشتراک مایکت)",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun VipFeatureRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 11.5.sp, lineHeight = 16.sp)
        }
    }
}
