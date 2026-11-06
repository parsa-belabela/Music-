package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
        manager.refreshRemoteConfig()
    }

    if (showThankYouDialog) {
        AlertDialog(
            onDismissRequest = {
                showThankYouDialog = false
                notifyUnlocked()
                onDismiss()
            },
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
                    onClick = {
                        showThankYouDialog = false
                        notifyUnlocked()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                ) {
                    Text("متوجه شدم", color = Color.White, fontWeight = FontWeight.Bold)
                }
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

            Button(
                onClick = {
                    if (isVip) {
                        showThankYouDialog = true
                    } else if (onStartMyketPurchase != null) {
                        onStartMyketPurchase(vipConfig.sku)
                    } else {
                        scope.launch {
                            isLoading = true
                            statusMessage = null
                            val intentRes = manager.createMyketBuyIntent()
                            isLoading = false
                            intentRes.onSuccess {
                                showThankYouDialog = true
                            }.onFailure { err ->
                                isError = true
                                statusMessage = err.message ?: "خطا در اتصال به مایکت"
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
                Text(
                    text = if (isVip) "✅ شما کاربر VIP پریمیوم هستید (ورود)"
                    else if (vipConfig.salesOpen) "خرید اشتراک پریمیوم از مایکت (${vipConfig.sku})"
                    else "ظرفیت فروش تکمیل شده است",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "کد تخفیف یا کد فعال‌سازی VIP دارید؟",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it },
                    placeholder = { Text("مثلاً: AURAVIP", color = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
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
                        scope.launch {
                            isLoading = true
                            statusMessage = null
                            val res = manager.redeemPromoCode(promoCode)
                            isLoading = false
                            if (res.isSuccess) {
                                isError = false
                                statusMessage = res.getOrNull()
                                delay(300)
                                showThankYouDialog = true
                            } else {
                                isError = true
                                statusMessage = res.exceptionOrNull()?.message ?: "کد نامعتبر است"
                            }
                        }
                    },
                    enabled = !isLoading && promoCode.isNotBlank(),
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("اعمال کد", fontWeight = FontWeight.Bold, color = Color.White)
                    }
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
    val glassBg = Color.White.copy(alpha = 0.05f)
    val glassBorder = Color.White.copy(alpha = 0.12f)

    var promoInput by remember { mutableStateOf("") }
    var promoFeedback by remember { mutableStateOf<String?>(null) }
    var promoIsSuccess by remember { mutableStateOf(false) }
    var showPromoSection by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(goldAccent.copy(alpha = 0.28f), royalViolet.copy(alpha = 0.35f))
                    )
                )
                .border(1.5.dp, goldAccent.copy(alpha = 0.85f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = "VIP",
                tint = goldAccent,
                modifier = Modifier.size(38.dp)
            )
        }

        Text(
            text = "Aura Premium • نسخه بتا",
            color = Color.White,
            fontSize = 23.sp,
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF191D2E), Color(0xFF121521))
                    )
                )
                .border(1.5.dp, goldAccent.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(goldAccent.copy(alpha = 0.18f))
                    .border(1.dp, goldAccent.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text = config.discountBadgeText,
                    color = goldAccent,
                    fontSize = 12.sp,
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
                    fontSize = 16.sp,
                    textDecoration = TextDecoration.LineThrough
                )
                Text(
                    text = config.discountedPriceText,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = "اشتراک مادام‌العمر (بدون شارژ ماهانه یا سالانه - دسترسی همیشگی)",
                color = emeraldGreen,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onBuyClick,
            enabled = config.salesOpen && config.remainingCount > 0 && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (config.salesOpen && config.remainingCount > 0) neonCyan else Color.DarkGray,
                contentColor = Color(0xFF050811)
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF050811),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(Icons.Rounded.ShoppingBag, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "خرید اشتراک مادام‌العمر با مایکت (${config.discountedPriceText})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

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
