package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import com.example.monetization.PurchaseResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipPaywallSheet(
    onDismiss: () -> Unit,
    onUnlockedSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config by EntitlementManager.remoteConfig.collectAsState()
    val isPremium by EntitlementManager.isPremium.collectAsState()
    val hardwareId = remember { EntitlementManager.getHardwareUserId(context) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        EntitlementManager.init(context)
        EntitlementManager.fetchRemoteConfig(context)
    }

    LaunchedEffect(isPremium) {
        if (isPremium) {
            onUnlockedSuccess()
        }
    }

    val myketLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        scope.launch {
            isLoading = true
            when (val res = EntitlementManager.handleMyketActivityResult(context, result.resultCode, result.data)) {
                is PurchaseResult.Success -> {
                    isError = false
                    statusMessage = "🎉 اشتراک پریمیوم مادام‌العمر شما در لحظه فعال شد!"
                    onUnlockedSuccess()
                }
                is PurchaseResult.Error -> {
                    isError = true
                    statusMessage = res.message
                }
                is PurchaseResult.Cancelled -> {
                    isError = false
                    statusMessage = "فرآیند پرداخت لغو شد."
                }
            }
            isLoading = false
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0B0E17),
        scrimColor = Color.Black.copy(alpha = 0.78f),
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
    ) {
        AuraPremiumPaywallContent(
            config = config,
            hardwareId = hardwareId,
            isLoading = isLoading,
            statusMessage = statusMessage,
            isError = isError,
            onBuyClick = {
                scope.launch {
                    isLoading = true
                    statusMessage = null
                    val intentRes = EntitlementManager.createMyketBuyIntent(context)
                    intentRes.onSuccess { pendingIntent ->
                        try {
                            val req = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            myketLauncher.launch(req)
                        } catch (e: Exception) {
                            isError = true
                            statusMessage = "خطا در باز کردن پنجره مایکت: ${e.message}"
                        }
                    }.onFailure { err ->
                        if (err.message == "ALREADY_OWNED_AND_RESTORED") {
                            isError = false
                            statusMessage = "✅ اشتراک قبلی شما شناسایی و با موفقیت فعال شد!"
                            onUnlockedSuccess()
                        } else {
                            isError = true
                            statusMessage = err.message ?: "خطا در اتصال به مایکت"
                        }
                    }
                    isLoading = false
                }
            },
            onRestoreClick = {
                scope.launch {
                    isLoading = true
                    statusMessage = null
                    when (val res = EntitlementManager.restorePurchaseFromMyket(context)) {
                        is PurchaseResult.Success -> {
                            isError = false
                            statusMessage = "✅ اشتراک مادام‌العمر شما بازیابی و فعال شد!"
                            onUnlockedSuccess()
                        }
                        is PurchaseResult.Error -> {
                            isError = true
                            statusMessage = res.message
                        }
                        is PurchaseResult.Cancelled -> {}
                    }
                    isLoading = false
                }
            },
            onPromoSuccess = {
                onUnlockedSuccess()
            }
        )
    }
}

@Composable
fun AuraPremiumPaywallContent(
    config: com.example.monetization.RemoteVipConfig,
    hardwareId: String,
    isLoading: Boolean,
    statusMessage: String?,
    isError: Boolean,
    onBuyClick: () -> Unit,
    onRestoreClick: () -> Unit,
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
        // ۱. نشان VIP و عنوان اصلی
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

        // ۲. بنر پیام تبلیغاتی از راه دور (Cloudflare Remote Config)
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

        // ۳. کارت زنده ظرفیت سرور اختصاصی ۵۰ نفره
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(glassBg)
                .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (config.salesOpen) emeraldGreen else Color(0xFFEF4444))
                    )
                    Text(
                        text = "ظرفیت سرور اختصاصی بتا (${config.totalCapacity} نفر)",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${config.remainingCount} ظرفیت باقی‌مانده",
                    color = if (config.remainingCount > 10) neonCyan else goldAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            val progress = (config.soldCount.toFloat() / config.totalCapacity.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape),
                color = neonCyan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )

            Text(
                text = "تکمیل‌شده: ${config.soldCount} از ${config.totalCapacity} نفر • به محض تکمیل ۵۰ نفر، فروش بسته می‌شود.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.5.sp
            )
        }

        // ۴. کارت قیمت‌گذاری مادام‌العمر (۲۷۵ به ۷۵ هزار تومان با ۷۳٪ تخفیف)
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

        // ۵. ویژگی‌های ممتاز VIP
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
                title = "دسترسی نامحدود به آرشیو ابری موزیک",
                subtitle = "پخش آنی و دانلود مستقیم تمام آهنگ‌ها و فول‌آلبوم‌های خواننده‌ها بدون تحریم"
            )
            VipFeatureRow(
                icon = Icons.Rounded.QueueMusic,
                tint = goldAccent,
                title = "ارسال روزانه ۳ درخواست آهنگ اختصاصی",
                subtitle = "هر آهنگی که در آرشیو نبود را درخواست بده تا مستقیم به کانال آرشیو اضافه شود"
            )
            VipFeatureRow(
                icon = Icons.Rounded.AllInclusive,
                tint = emeraldGreen,
                title = "مادام‌العمر و همیشگی (Lifetime VIP)",
                subtitle = "فقط یک‌بار در نسخه بتا پرداخت می‌کنی و تا ابد به تمام آپدیت‌ها دسترسی داری"
            )
            VipFeatureRow(
                icon = Icons.Rounded.Security,
                tint = royalViolet,
                title = "قفل سخت‌افزاری اختصاصی: $hardwareId",
                subtitle = "اشتراک به صورت امن روی همین دستگاه شما قفل و فعال می‌شود"
            )
        }

        // ۶. بخش وارد کردن کد تخفیف / کد هدیه
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(glassBg)
                .border(1.dp, if (showPromoSection) neonCyan.copy(alpha = 0.45f) else glassBorder, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPromoSection = !showPromoSection },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalOffer,
                        contentDescription = null,
                        tint = goldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "کد تخفیف یا هدیه دارید؟",
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (showPromoSection) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            AnimatedVisibility(visible = showPromoSection) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = promoInput,
                            onValueChange = { promoInput = it },
                            placeholder = {
                                Text("کد تخفیف را وارد کنید...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp)
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
                                        promoFeedback = "🎉 کد تخفیف با موفقیت اعمال شد! اشتراک پریمیوم مادام‌العمر برای شما فعال گردید."
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
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
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
                                    promoFeedback = "🎉 کد تخفیف با موفقیت اعمال شد! اشتراک پریمیوم مادام‌العمر برای شما فعال گردید."
                                    onPromoSuccess()
                                } else {
                                    promoFeedback = "❌ کد تخفیف واردشده نامعتبر است."
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = goldAccent,
                                contentColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("اعمال", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }

                    if (!promoFeedback.isNullOrBlank()) {
                        Text(
                            text = promoFeedback!!,
                            color = if (promoIsSuccess) emeraldGreen else Color(0xFFEF4444),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        // ۷. پیام وضعیت خرید مایکت
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
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ۸. دکمه اصلی خرید مستقیم با مایکت
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
            } else if (!config.salesOpen || config.remainingCount <= 0) {
                Text(
                    text = "⛔ ظرفیت ۵۰ نفر تکمیل شده (فروش متوقف است)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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

        // ۹. دکمه بازیابی خرید قبلی از مایکت
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
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
