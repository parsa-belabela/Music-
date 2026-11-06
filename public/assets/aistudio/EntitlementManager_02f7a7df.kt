package com.example.monetization

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class VipServerConfig(
    val salesOpen: Boolean = true,
    val sku: String = "yu-15",
    val productTitle: String = "اشتراک پریمیوم",
    val originalPrice: Int = 275000,
    val discountPrice: Int = 75000,
    val totalCapacity: Int = 50,
    val soldCount: Int = 0,
    val remainingCapacity: Int = 50,
    val bannerText: String = "🔥 تخفیف ویژه برای ۵۰ نفر اول: اشتراک مادام‌العمر فقط ۷۵,۰۰۰ تومان (به‌جای ۲۷۵,۰۰۰ تومان)"
)

sealed class VipPurchaseState {
    object Idle : VipPurchaseState()
    data class Connecting(val message: String = "در حال اتصال به سرور...") : VipPurchaseState()
    data class Success(val message: String) : VipPurchaseState()
    data class Error(val message: String) : VipPurchaseState()
}

class EntitlementManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("aura_vip_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isVip = MutableStateFlow(
        prefs.getBoolean(KEY_IS_VIP, false) || prefs.getBoolean(KEY_PROMO_UNLOCKED, false)
    )
    val isVip: StateFlow<Boolean> = _isVip.asStateFlow()

    private val _serverConfig = MutableStateFlow(VipServerConfig())
    val serverConfig: StateFlow<VipServerConfig> = _serverConfig.asStateFlow()

    private val _purchaseState = MutableStateFlow<VipPurchaseState>(VipPurchaseState.Idle)
    val purchaseState: StateFlow<VipPurchaseState> = _purchaseState.asStateFlow()

    companion object {
        private const val KEY_IS_VIP = "is_vip_active"
        private const val KEY_PROMO_UNLOCKED = "is_promo_57589188_unlocked"
        private const val KEY_WORKER_URL = "cloudflare_worker_url"
        const val MASTER_PROMO_CODE = "57589188"
        const val DEFAULT_SKU = "yu-15"
        private const val MYKET_PACKAGE = "ir.mservices.market"

        @Volatile
        private var INSTANCE: EntitlementManager? = null

        fun getInstance(context: Context): EntitlementManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EntitlementManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        // Set the explicit worker URL as requested
        setWorkerBaseUrl("https://alubm.parsaghorbani8861.workers.dev/")
        syncWithCloudflare()
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
        return if (!androidId.isNullOrBlank()) {
            "aura_${androidId}_${Build.MODEL.replace("\\s+".toRegex(), "")}"
        } else {
            val saved = prefs.getString("fallback_device_id", null)
            if (saved != null) saved else {
                val generated = "aura_dev_${System.currentTimeMillis()}"
                prefs.edit().putString("fallback_device_id", generated).apply()
                generated
            }
        }
    }

    fun getWorkerBaseUrl(fallbackUrl: String = ""): String {
        val saved = prefs.getString(KEY_WORKER_URL, null)?.trimEnd('/')
        if (!saved.isNullOrBlank() && saved.startsWith("http")) return saved
        val cleanFallback = fallbackUrl.trimEnd('/')
        if (cleanFallback.isNotBlank()) return cleanFallback
        return "https://alubm.parsaghorbani8861.workers.dev" // Default to user's URL
    }

    fun setWorkerBaseUrl(url: String) {
        val clean = url.trim().trimEnd('/')
        if (clean.startsWith("http")) {
            prefs.edit().putString(KEY_WORKER_URL, clean).apply()
        }
    }

    fun normalizeDigits(input: String): String {
        val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val sb = StringBuilder()
        for (ch in input.trim()) {
            if (ch.isWhitespace() || ch == '-') continue
            val pIdx = persian.indexOf(ch)
            val aIdx = arabic.indexOf(ch)
            when {
                pIdx != -1 -> sb.append(pIdx)
                aIdx != -1 -> sb.append(aIdx)
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * بررسی و فعال‌سازی آنی کد تخفیف 57589188 + ثبت مستقیم در دیتابیس D1 کلودفلر
     */
    fun redeemPromoCode(rawCode: String, workerUrl: String = "", onSuccess: () -> Unit = {}) {
        val cleanCode = normalizeDigits(rawCode)
        if (cleanCode.isEmpty()) {
            _purchaseState.value = VipPurchaseState.Error("لطفاً کد تخفیف را وارد کنید.")
            return
        }

        if (cleanCode != MASTER_PROMO_CODE) {
            _purchaseState.value = VipPurchaseState.Error("❌ کد تخفیف وارد شده نامعتبر است.")
            return
        }

        scope.launch {
            _purchaseState.value = VipPurchaseState.Connecting("در حال ثبت اشتراک ویژه در سرور کلودفلر...")
            if (workerUrl.isNotBlank()) setWorkerBaseUrl(workerUrl)

            // ۱. فعال‌سازی فوری و دائمی روی گوشی
            prefs.edit()
                .putBoolean(KEY_IS_VIP, true)
                .putBoolean(KEY_PROMO_UNLOCKED, true)
                .apply()
            _isVip.value = true

            // ۲. ارسال به مسیر /vip-verify و /vip-config کلودفلر برای ثبت در جدول vip_users دیتابیس D1
            val baseUrl = getWorkerBaseUrl(workerUrl)
            if (baseUrl.startsWith("http")) {
                withTimeoutOrNull(6000L) {
                    sendPromoVerifyToCloudflare(baseUrl, getDeviceId(), cleanCode)
                }
            }

            _purchaseState.value = VipPurchaseState.Success("✅ اشتراک پریمیوم مادام‌العمر با موفقیت فعال شد!")
            onSuccess()
        }
    }

    private suspend fun sendPromoVerifyToCloudflare(baseUrl: String, deviceId: String, promoCode: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("${baseUrl.trimEnd('/')}/vip-verify")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("X-Device-Id", deviceId)
                }

                val payload = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("promoCode", promoCode)
                    put("code", promoCode)
                    put("purchaseToken", "PROMO_${promoCode}_$deviceId")
                    put("sku", DEFAULT_SKU)
                }

                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                code in 200..299
            } catch (e: Exception) {
                false
            }
        }

    /**
     * همگام‌سازی وضعیت اشتراک و قیمت (۷۵,۰۰۰ تومان) با مسیر /vip-config کلودفلر
     */
    fun syncWithCloudflare(workerUrl: String = "") {
        scope.launch {
            if (workerUrl.isNotBlank()) setWorkerBaseUrl(workerUrl)
            val baseUrl = getWorkerBaseUrl(workerUrl)
            if (!baseUrl.startsWith("http")) return@launch

            val deviceId = getDeviceId()
            val localPromoActive = prefs.getBoolean(KEY_PROMO_UNLOCKED, false)

            withContext(Dispatchers.IO) {
                try {
                    val promoParam = if (localPromoActive) "&promoCode=$MASTER_PROMO_CODE" else ""
                    val endpoint = "${baseUrl.trimEnd('/')}/vip-config?deviceId=$deviceId$promoParam"
                    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 6000
                        readTimeout = 6000
                        setRequestProperty("X-Device-Id", deviceId)
                    }

                    if (conn.responseCode in 200..299) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(text)

                        _serverConfig.value = VipServerConfig(
                            salesOpen = json.optBoolean("salesOpen", true),
                            sku = json.optString("sku", DEFAULT_SKU),
                            productTitle = json.optString("productTitle", "اشتراک پریمیوم"),
                            originalPrice = json.optInt("originalPrice", 275000),
                            discountPrice = json.optInt("discountPrice", 75000),
                            totalCapacity = json.optInt("totalCapacity", 50),
                            soldCount = json.optInt("soldCount", 0),
                            remainingCapacity = json.optInt("remainingCapacity", 50),
                            bannerText = json.optString("bannerText", _serverConfig.value.bannerText)
                        )

                        val serverVip = json.optBoolean("isVip", false) || json.optBoolean("vip", false)
                        if (serverVip || localPromoActive) {
                            prefs.edit().putBoolean(KEY_IS_VIP, true).apply()
                            _isVip.value = true
                            // اگر روی گوشی با کد تخفیف VIP بوده ولی سرور هنوز ثبت نکرده، سرور را هم آپدیت کن
                            if (localPromoActive && !serverVip) {
                                sendPromoVerifyToCloudflare(baseUrl, deviceId, MASTER_PROMO_CODE)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * شروع خرید از مایکت بدون قفل شدن دکمه روی «در حال اتصال به سرور...»
     */
    fun launchMyketPurchase(
        customBillingAction: (suspend (sku: String) -> Boolean)? = null
    ) {
        scope.launch {
            _purchaseState.value = VipPurchaseState.Connecting("در حال اتصال به سرور مایکت...")
            try {
                val sku = _serverConfig.value.sku.ifBlank { DEFAULT_SKU }
                val launched = withTimeoutOrNull(5500L) {
                    delay(400L)
                    if (customBillingAction != null) {
                        customBillingAction.invoke(sku)
                    } else {
                        openMyketFallback()
                    }
                }

                when (launched) {
                    null -> {
                        _purchaseState.value = VipPurchaseState.Error(
                            "پاسخی از سرور مایکت دریافت نشد. دوباره تلاش کنید یا از کد تخفیف 57589188 استفاده نمایید."
                        )
                    }
                    false -> {
                        _purchaseState.value = VipPurchaseState.Error(
                            "برنامه مایکت روی دستگاه شما یافت نشد یا اتصال برقرار نشد. می‌توانید کد تخفیف را در کادر پایین وارد کنید."
                        )
                    }
                    true -> {
                        delay(800L)
                        // آزادسازی دکمه تا اگر کاربر از مایکت برگشت، دکمه قفل نباشد
                        _purchaseState.value = VipPurchaseState.Idle
                    }
                }
            } catch (e: Exception) {
                _purchaseState.value = VipPurchaseState.Error(
                    "خطا در اتصال به مایکت: ${e.localizedMessage ?: "مجدداً تلاش کنید"}"
                )
            }
        }
    }

    private fun openMyketFallback(): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(MYKET_PACKAGE, 0)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("myket://details?id=${context.packageName}")
                setPackage(MYKET_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = VipPurchaseState.Idle
    }
}
