package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.monetization.VipPurchaseState

@Composable
fun VipLockGateCard(
    workerUrl: String = "https://alubm.parsaghorbani8861.workers.dev/",
    onUnlocked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val entitlementManager = remember { EntitlementManager.getInstance(context) }

    val config by entitlementManager.serverConfig.collectAsState()
    val purchaseState by entitlementManager.purchaseState.collectAsState()
    val isVip by entitlementManager.isVip.collectAsState()

    var promoCodeText by remember { mutableStateOf("") }

    LaunchedEffect(workerUrl) {
        entitlementManager.syncWithCloudflare(workerUrl)
    }

    LaunchedEffect(isVip) {
        if (isVip) {
            onUnlocked()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFF6366F1))),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131124))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = config.productTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = config.bannerText,
                fontSize = 13.sp,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // نمایش قیمت ۲۷۵,۰۰۰ خط‌خورده و ۷۵,۰۰۰ تومان
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${config.originalPrice} تومان",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    textDecoration = TextDecoration.LineThrough
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${config.discountPrice} تومان",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E676)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "ظرفیت باقی‌مانده: ${config.remainingCapacity} از ${config.totalCapacity} نفر",
                fontSize = 12.sp,
                color = Color(0xFFA855F7)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ۱. دکمه خرید از مایکت (بدون قفل شدن)
            val isConnecting = purchaseState is VipPurchaseState.Connecting
            Button(
                onClick = {
                    entitlementManager.launchMyketPurchase()
                },
                enabled = !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853),
                    disabledContainerColor = Color(0xFF00C853).copy(alpha = 0.5f)
                )
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = (purchaseState as? VipPurchaseState.Connecting)?.message ?: "در حال اتصال به سرور...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "خرید اشتراک از مایکت (${config.discountPrice} تومان)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(14.dp))

            // ۲. بخش وارد کردن کد تخفیف / کد هدیه (57589188)
            Text(
                text = "🎁 کد تخفیف یا کد فعال‌سازی دارید؟",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        entitlementManager.redeemPromoCode(
                            rawCode = promoCodeText,
                            workerUrl = workerUrl,
                            onSuccess = onUnlocked
                        )
                    },
                    enabled = !isConnecting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ثبت کد", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = promoCodeText,
                    onValueChange = { promoCodeText = it },
                    placeholder = {
                        Text(
                            text = "کد تخفیف را وارد کنید...",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            entitlementManager.redeemPromoCode(
                                rawCode = promoCodeText,
                                workerUrl = workerUrl,
                                onSuccess = onUnlocked
                            )
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // نمایش پیام خطا یا تایید
            when (val st = purchaseState) {
                is VipPurchaseState.Error -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = st.message,
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                is VipPurchaseState.Success -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = st.message,
                            color = Color(0xFF00E676),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

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

    fun notifyUnlocked() {
        onUnlocked()
        onVipUnlocked()
        onUnlockedSuccess()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F111A),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                }
            }

            if (highlightedFeature != null) {
                Text(
                    text = if (isFarsi) highlightedFeature.descFa else highlightedFeature.descEn,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            VipLockGateCard(
                workerUrl = "https://alubm.parsaghorbani8861.workers.dev/",
                onUnlocked = {
                    notifyUnlocked()
                    onDismiss()
                }
            )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VipLockGateCard(
            workerUrl = "https://alubm.parsaghorbani8861.workers.dev/",
            onUnlocked = onPromoSuccess
        )
    }
}
