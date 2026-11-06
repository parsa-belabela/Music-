package com.example.monetization

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.provider.Settings
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

enum class FeatureGate(
    val key: String,
    val titleEn: String,
    val titleFa: String,
    val descEn: String,
    val descFa: String
) {
    CLOUD_ARCHIVE(
        "cloud_archive",
        "Online Music Archive & Search",
        "آرشیو موزیک آنلاین و دانلود مستقیم",
        "Stream and download unlimited songs from our high-speed cloud archive.",
        "دسترسی نامحدود به آرشیو آنلاین آهنگ‌ها، پخش با بالاترین کیفیت و درخواست آهنگ در کانال."
    ),
    WORD_SYNC_LYRICS(
        "word_sync_lyrics",
        "Word-by-Word Karaoke Lyrics",
        "متن آهنگ کارائوکه (کلمه‌به‌کلمه)",
        "Follow every word in real-time with glowing synced lyrics.",
        "نمایش زنده و کلمه‌به‌کلمه متن آهنگ‌ها هماهنگ با صدای خواننده."
    ),
    DSP_STUDIO_FX(
        "dsp_studio_fx",
        "Studio 10-Band EQ & 8D Spatial Audio",
        "اکولایزر استودیویی و صدای ۸ بعدی",
        "Unlock full 10-band equalizer, bass boost, and spatial audio effects.",
        "باز شدن اکولایزر حرفه‌ای ۱۰ بانده، تقویت بیس و افکت صدای سه‌بعدی."
    ),
    SHADER_VISUALIZERS(
        "shader_visualizers",
        "Pro GPU Shader Visualizers",
        "ویژوالایزرهای نوری و سینمایی",
        "Unlock all reactive spectrum and particle visualizers.",
        "دسترسی به تمام ویژوالایزرهای گرافیکی حساس به ضرباهنگ موزیک."
    ),
    SHARE_STORY_CARDS(
        "share_story_cards",
        "Instagram Story Share Cards",
        "ساخت استوری موزیک اختصاصی",
        "Generate custom lyric & artwork cards for social media.",
        "ساخت کارت‌های گرافیکی از کاور و متن آهنگ برای استوری."
    ),
    HEARING_PROFILE(
        "hearing_profile",
        "Personalized Hearing Calibration",
        "تنظیم هوشمند صدا بر اساس شنوایی",
        "Calibrate audio output frequencies tailored to your ears.",
        "کالیبراسیون فرکانس‌های صدا متناسب با پروفایل شنوایی شما."
    )
}

enum class SubscriptionPlan(
    val id: String,
    val sku: String,
    val titleFa: String,
    val originalPriceToman: Int,
    val discountPriceToman: Int,
    val priceLabelFa: String,
    val originalPriceLabelFa: String,
    val badgeFa: String
) {
    VIP_LIFETIME(
        id = "vip_lifetime",
        sku = "yu-15",
        titleFa = "اشتراک پریمیوم مادام‌العمر (۵۰ نفر اول)",
        originalPriceToman = 275000,
        discountPriceToman = 75000,
        priceLabelFa = "۷۵,۰۰۰ تومان",
        originalPriceLabelFa = "۲۷۵,۰۰۰ تومان",
        badgeFa = "🔥 ۷۳٪ تخفیف ویژه"
    )
}

data class VipServerConfig(
    val salesOpen: Boolean = true,
    val sku: String = "yu-15",
    val productTitle: String = "اشتراک پریمیوم",
    val originalPrice: Int = 275000,
    val discountPrice: Int = 75000,
    val totalCapacity: Int = 50,
    val soldCount: Int = 0,
    val remainingCapacity: Int = 50,
    val bannerText: String = "🔥 تخفیف ویژه برای ۵۰ نفر اول: اشتراک مادام‌العمر فقط ۷۵,۰۰۰ تومان (به‌جای ۲۷۵,۰۰۰ تومان)",
    val originalPriceToman: Int = originalPrice,
    val discountPriceToman: Int = discountPrice,
    val priceRials: Int = discountPrice * 10,
    val isVip: Boolean = false,
    val remainingCount: Int = remainingCapacity,
    val originalPriceText: String = "%,d تومان".format(Locale.US, originalPrice),
    val discountedPriceText: String = "%,d تومان".format(Locale.US, discountPrice),
    val discountBadgeText: String = "۷۳٪ تخفیف ویژه نسخه بتا • مادام‌العمر",
    val promoBannerText: String = bannerText,
    val closedMessage: String = "⛔ ظرفیت ۵۰ نفره کاربران پریمیوم تکمیل شده است. لطفاً منتظر باز شدن مجدد ظرفیت بمانید.",
    val dailyRequestLimit: Int = 3
)

typealias VipRemoteConfig = VipServerConfig
typealias RemoteVipConfig = VipServerConfig

sealed class PurchaseResult {
    data object Success : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    data object Cancelled : PurchaseResult()
}

sealed class VipPurchaseState {
    object Idle : VipPurchaseState()
    data class Connecting(val message: String = "در حال اتصال به سرور...") : VipPurchaseState()
    data class Success(val message: String) : VipPurchaseState()
    data class Error(val message: String) : VipPurchaseState()
}

/**
 * مدیریت تبلیغات تپسل (Interstitial و Rewarded) با رعایت قانون طلایی VIP
 */
object TapsellPlus {
    private var initializedAppKey: String = ""

    const val INTERSTITIAL_ZONE_ID = "" // TODO: Insert Interstitial Zone ID
    const val REWARDED_ZONE_ID = ""     // TODO: Insert Rewarded Zone ID

    private val playedTracksCounter = AtomicInteger(0)
    private const val INTERSTITIAL_FREQUENCY = 3

    fun initialize(context: Context, appKey: String) {
        if (EntitlementManager.isPremium.value) return
        initializedAppKey = appKey.trim()
        if (initializedAppKey.isEmpty()) {
            Log.d("TapsellPlus", "Tapsell AppKey is empty. Waiting for key configuration.")
            return
        }
    }

    fun onTrackPlayed(
        context: Context? = null,
        zoneId: String = INTERSTITIAL_ZONE_ID
    ) {
        if (EntitlementManager.isPremium.value) return
        val currentCount = playedTracksCounter.incrementAndGet()
        if (currentCount >= INTERSTITIAL_FREQUENCY) {
            playedTracksCounter.set(0)
            loadAndShowInterstitial(context, zoneId)
        }
    }

    fun loadAndShowInterstitial(
        context: Context? = null,
        zoneId: String = INTERSTITIAL_ZONE_ID,
        activity: Activity? = null
    ) {
        if (EntitlementManager.isPremium.value) return
        val cleanZone = zoneId.trim()
        if (initializedAppKey.isEmpty() || cleanZone.isEmpty()) {
            return
        }
    }

    fun showRewardedAd(
        context: Context? = null,
        zoneId: String = REWARDED_ZONE_ID,
        activity: Activity? = null,
        onReward: () -> Unit
    ) {
        if (EntitlementManager.isPremium.value) {
            onReward()
            return
        }
        val cleanZone = zoneId.trim()
        if (initializedAppKey.isEmpty() || cleanZone.isEmpty()) {
            onReward()
            return
        }
    }
}

class EntitlementManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("aura_vip_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isVip = _globalIsVip
    val isVip: StateFlow<Boolean> = _isVip.asStateFlow()
    val isPremium: StateFlow<Boolean> get() = isVip
    val hasVipAccess: StateFlow<Boolean> get() = isVip

    private val _serverConfig = _globalServerConfig
    val serverConfig: StateFlow<VipServerConfig> = _serverConfig.asStateFlow()
    val vipConfig: StateFlow<VipServerConfig> get() = serverConfig
    val remoteConfig: StateFlow<VipServerConfig> get() = serverConfig

    private val _purchaseState = _globalPurchaseState
    val purchaseState: StateFlow<VipPurchaseState> = _purchaseState.asStateFlow()

    val activePlan: StateFlow<SubscriptionPlan?> = _globalActivePlan.asStateFlow()
    val remainingRequestsToday: StateFlow<Int> = _globalRemainingRequestsToday.asStateFlow()

    companion object {
        const val CLOUDFLARE_WORKER_URL = "https://alubm.parsaghorbani8861.workers.dev"
        const val WORKER_BASE_URL = "https://alubm.parsaghorbani8861.workers.dev"

        private const val KEY_IS_VIP = "is_vip_active"
        private const val KEY_PROMO_UNLOCKED = "is_promo_57589188_unlocked"
        private const val KEY_WORKER_URL = "cloudflare_worker_url"
        private const val KEY_REQ_DATE = "song_req_date"
        private const val KEY_REQ_COUNT = "song_req_count"

        const val MASTER_PROMO_CODE = "57589188"
        const val SECRET_GIFT_CODE = "57589188"
        const val DEFAULT_SKU = "yu-15"
        const val MYKET_SKU = "yu-15"
        const val MYKET_PRODUCT_SKU = "yu-15"
        private const val MYKET_PACKAGE = "ir.mservices.market"

        const val MYKET_PUBLIC_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCLsoVLJbAG1ZlFRJd4YV+Djgp4PbHkx8+n4UvkSS0/9mZMNsLC/92ZlU+lsFOSKIGHuNesLz5Lk0/wdoFDJvz3Kfl0tYw/jj/8zEG2m9a3kEF/s9UZCT9UWYGA0cNSiB3fDdxdH+pKWJ3sdlcpH5T96+L9dSmIP6/hI4j37hrEawIDAQAB"

        private val _globalIsVip = MutableStateFlow(false)
        private val _globalServerConfig = MutableStateFlow(VipServerConfig())
        private val _globalPurchaseState = MutableStateFlow<VipPurchaseState>(VipPurchaseState.Idle)
        private val _globalActivePlan = MutableStateFlow<SubscriptionPlan?>(null)
        private val _globalRemainingRequestsToday = MutableStateFlow(3)

        val isPremium: StateFlow<Boolean> get() = _globalIsVip.asStateFlow()
        val remoteConfig: StateFlow<VipServerConfig> get() = _globalServerConfig.asStateFlow()
        val remainingRequestsToday: StateFlow<Int> get() = _globalRemainingRequestsToday.asStateFlow()
        val activePlan: StateFlow<SubscriptionPlan?> get() = _globalActivePlan.asStateFlow()

        @Volatile
        private var INSTANCE: EntitlementManager? = null

        fun getInstance(context: Context): EntitlementManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EntitlementManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun init(context: Context) {
            getInstance(context)
        }

        fun isVip(context: Context): Boolean = getInstance(context).isVip.value

        fun hasEntitlement(context: Context, gate: FeatureGate): Boolean = getInstance(context).hasEntitlement(gate)

        fun hasEntitlement(context: Context, featureKey: String): Boolean = getInstance(context).hasEntitlement(featureKey)

        fun getHardwareUserId(context: Context): String = getInstance(context).getDeviceId()

        suspend fun fetchRemoteConfig(context: Context): VipServerConfig {
            val mgr = getInstance(context)
            mgr.syncWithCloudflare()
            return mgr.serverConfig.value
        }

        suspend fun createMyketBuyIntent(context: Context) = getInstance(context).createMyketBuyIntent()

        suspend fun handleMyketActivityResult(context: Context, resultCode: Int, data: Intent?) =
            getInstance(context).handleMyketActivityResult(resultCode, data)

        suspend fun restorePurchaseFromMyket(context: Context) = getInstance(context).restorePurchaseFromMyket()

        fun redeemPromoCode(context: Context, rawCode: String): Boolean {
            return getInstance(context).applyPromoCode(rawCode)
        }

        suspend fun submitSongRequest(context: Context, songTitle: String, artistName: String, extraNote: String = "") =
            getInstance(context).submitSongRequest(songTitle, artistName, extraNote)

        fun hasAccess(context: Context, featureId: String): Boolean = true

        fun grantVip(context: Context, days: Int, source: String) {
            getInstance(context).unlockAllForTesting()
        }

        fun revokeVip(context: Context) {
            getInstance(context).resetEntitlements()
        }

        fun unlockFullVipPreview(context: Context, hours: Int = 24) {
            getInstance(context).unlockAllForTesting()
        }

        fun getRemainingDays(context: Context): Int = if (_globalIsVip.value) 9999 else 0
        fun getVipSource(context: Context): String = if (_globalIsVip.value) "promo_or_myket" else "none"
        fun getVipExpiryMillis(context: Context): Long = if (_globalIsVip.value) Long.MAX_VALUE else 0L
    }

    init {
        val unlocked = prefs.getBoolean(KEY_IS_VIP, false) || prefs.getBoolean(KEY_PROMO_UNLOCKED, false)
        if (unlocked) {
            _isVip.value = true
            _globalActivePlan.value = SubscriptionPlan.VIP_LIFETIME
        }
        // Set the explicit worker URL as requested
        setWorkerBaseUrl("https://alubm.parsaghorbani8861.workers.dev/")
        refreshDailyRequestQuota()
        syncWithCloudflare()
    }

    fun isPromoUnlocked(): Boolean {
        return prefs.getBoolean(KEY_PROMO_UNLOCKED, false) || _isVip.value
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

    fun getHardwareDeviceId(): String = getDeviceId()

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

        // ۱. فعال‌سازی فوری و دائمی روی گوشی
        prefs.edit()
            .putBoolean(KEY_IS_VIP, true)
            .putBoolean(KEY_PROMO_UNLOCKED, true)
            .apply()
        _isVip.value = true
        _globalActivePlan.value = SubscriptionPlan.VIP_LIFETIME
        _serverConfig.value = _serverConfig.value.copy(isVip = true)

        scope.launch {
            _purchaseState.value = VipPurchaseState.Connecting("در حال ثبت اشتراک ویژه در سرور کلودفلر...")
            if (workerUrl.isNotBlank()) setWorkerBaseUrl(workerUrl)

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

    fun applyPromoCode(rawCode: String): Boolean {
        val cleanCode = normalizeDigits(rawCode)
        if (cleanCode != MASTER_PROMO_CODE) {
            _purchaseState.value = VipPurchaseState.Error("❌ کد تخفیف وارد شده نامعتبر است.")
            return false
        }
        redeemPromoCode(cleanCode)
        return true
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
            val localPromoActive = prefs.getBoolean(KEY_PROMO_UNLOCKED, false) || prefs.getBoolean(KEY_IS_VIP, false)
            if (localPromoActive) {
                _isVip.value = true
                _globalActivePlan.value = SubscriptionPlan.VIP_LIFETIME
            }

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

                        val serverVip = json.optBoolean("isVip", false) || json.optBoolean("vip", false)
                        val finalVip = serverVip || localPromoActive || _isVip.value

                        _serverConfig.value = VipServerConfig(
                            salesOpen = json.optBoolean("salesOpen", true),
                            sku = json.optString("sku", DEFAULT_SKU),
                            productTitle = json.optString("productTitle", "اشتراک پریمیوم"),
                            originalPrice = json.optInt("originalPrice", 275000),
                            discountPrice = json.optInt("discountPrice", 75000),
                            totalCapacity = json.optInt("totalCapacity", 50),
                            soldCount = json.optInt("soldCount", 0),
                            remainingCapacity = json.optInt("remainingCapacity", 50),
                            bannerText = json.optString("bannerText", _serverConfig.value.bannerText),
                            isVip = finalVip
                        )

                        if (finalVip) {
                            prefs.edit().putBoolean(KEY_IS_VIP, true).apply()
                            _isVip.value = true
                            _globalActivePlan.value = SubscriptionPlan.VIP_LIFETIME
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

    fun refreshRemoteConfig() {
        syncWithCloudflare()
    }

    suspend fun fetchRemoteConfigSync(): VipServerConfig {
        syncWithCloudflare()
        return _serverConfig.value
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

    fun hasEntitlement(gate: FeatureGate): Boolean {
        return _isVip.value || prefs.getBoolean(KEY_IS_VIP, false) || prefs.getBoolean(KEY_PROMO_UNLOCKED, false)
    }

    fun hasEntitlement(featureKey: String = "yu-15"): Boolean {
        return _isVip.value || prefs.getBoolean(KEY_IS_VIP, false) || prefs.getBoolean(KEY_PROMO_UNLOCKED, false)
    }

    fun unlockAllForTesting() {
        redeemPromoCode(MASTER_PROMO_CODE)
    }

    fun resetEntitlements() {
        prefs.edit().clear().apply()
        _isVip.value = false
        _globalActivePlan.value = null
        _purchaseState.value = VipPurchaseState.Idle
    }

    fun refreshDailyRequestQuota() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = prefs.getString(KEY_REQ_DATE, "") ?: ""
        val limit = _serverConfig.value.dailyRequestLimit
        if (savedDate != today) {
            prefs.edit().putString(KEY_REQ_DATE, today).putInt(KEY_REQ_COUNT, 0).apply()
            _globalRemainingRequestsToday.value = limit
        } else {
            val used = prefs.getInt(KEY_REQ_COUNT, 0)
            _globalRemainingRequestsToday.value = (limit - used).coerceAtLeast(0)
        }
    }

    suspend fun submitSongRequest(
        songTitle: String,
        artistName: String,
        note: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val deviceId = getDeviceId()
        val baseUrl = getWorkerBaseUrl()
        try {
            val url = URL("${baseUrl.trimEnd('/')}/request-song")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("X-Device-Id", deviceId)
            }

            val payload = JSONObject().apply {
                put("deviceId", deviceId)
                put("songTitle", songTitle.trim())
                put("artistName", artistName.trim())
                put("note", note.trim())
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(payload.toString())
            }

            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            val json = JSONObject(respText)

            if (conn.responseCode in 200..299 && (json.optBoolean("ok", false) || json.optBoolean("verified", false))) {
                val used = prefs.getInt(KEY_REQ_COUNT, 0) + 1
                prefs.edit().putInt(KEY_REQ_COUNT, used).apply()
                _globalRemainingRequestsToday.value = (_serverConfig.value.dailyRequestLimit - used).coerceAtLeast(0)
                return@withContext Result.success(
                    json.optString("message", "✅ درخواست شما با موفقیت در کانال ثبت شد!")
                )
            } else {
                return@withContext Result.failure(
                    IllegalStateException(json.optString("message", json.optString("error", "خطا در ثبت درخواست آهنگ.")))
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("خطا در ارتباط با سرور کلادفلر: ${e.localizedMessage}"))
        }
    }

    fun verifyMyketSignatureLocally(signedData: String, signatureBase64: String): Boolean {
        if (signedData.isBlank() || signatureBase64.isBlank()) return false
        return try {
            val keyBytes = Base64.decode(MYKET_PUBLIC_KEY, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(keyBytes))
            val sig = Signature.getInstance("SHA1withRSA")
            sig.initVerify(publicKey)
            sig.update(signedData.toByteArray(Charsets.UTF_8))
            sig.verify(Base64.decode(signatureBase64, Base64.DEFAULT))
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createMyketBuyIntent(): Result<PendingIntent> = withContext(Dispatchers.IO) {
        _purchaseState.value = VipPurchaseState.Connecting("در حال اتصال به سرور مایکت...")
        try {
            val cfg = _serverConfig.value
            val deviceId = getDeviceId()
            val binderResult = withTimeoutOrNull(4500L) {
                bindMyketServiceAndExecute(context) { binder ->
                    val data = Parcel.obtain()
                    val reply = Parcel.obtain()
                    try {
                        data.writeInterfaceToken("com.android.vending.billing.IInAppBillingService")
                        data.writeInt(3)
                        data.writeString(context.packageName)
                        data.writeString(cfg.sku.ifBlank { DEFAULT_SKU })
                        data.writeString("inapp")
                        data.writeString(deviceId)
                        binder.transact(3, data, reply, 0)
                        reply.readException()
                        if (reply.readInt() != 0) Bundle.CREATOR.createFromParcel(reply) else null
                    } finally {
                        reply.recycle()
                        data.recycle()
                    }
                }
            }

            if (binderResult == null) {
                openMyketFallback()
                val msg = "پاسخی از سرویس درون‌برنامه‌ای مایکت دریافت نشد. می‌توانید از کد تخفیف 57589188 استفاده نمایید."
                _purchaseState.value = VipPurchaseState.Error(msg)
                return@withContext Result.failure(IllegalStateException(msg))
            }

            val responseCode = binderResult.getInt("RESPONSE_CODE", -1)
            if (responseCode == 7) {
                prefs.edit().putBoolean(KEY_IS_VIP, true).putBoolean(KEY_PROMO_UNLOCKED, true).apply()
                _isVip.value = true
                _purchaseState.value = VipPurchaseState.Success("✅ اشتراک قبلی شما بازیابی و فعال شد!")
                return@withContext Result.failure(IllegalStateException("ALREADY_OWNED_AND_RESTORED"))
            }

            @Suppress("DEPRECATION")
            val pendingIntent = binderResult.getParcelable<PendingIntent>("BUY_INTENT")
            if (pendingIntent == null) {
                val err = "درگاه پرداخت مایکت آماده نشد. دوباره تلاش کنید یا کد تخفیف 57589188 را وارد نمایید."
                _purchaseState.value = VipPurchaseState.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            _purchaseState.value = VipPurchaseState.Idle
            Result.success(pendingIntent)
        } catch (e: Exception) {
            val err = "خطا در ارتباط با مایکت: ${e.localizedMessage ?: "دوباره تلاش کنید"}"
            _purchaseState.value = VipPurchaseState.Error(err)
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun handleMyketActivityResult(resultCode: Int, data: Intent?): PurchaseResult = withContext(Dispatchers.IO) {
        if (data == null) {
            _purchaseState.value = VipPurchaseState.Idle
            return@withContext PurchaseResult.Cancelled
        }
        val responseCode = data.getIntExtra("RESPONSE_CODE", -1)
        if (responseCode != 0) {
            _purchaseState.value = VipPurchaseState.Idle
            return@withContext PurchaseResult.Cancelled
        }
        prefs.edit().putBoolean(KEY_IS_VIP, true).putBoolean(KEY_PROMO_UNLOCKED, true).apply()
        _isVip.value = true
        sendPromoVerifyToCloudflare(getWorkerBaseUrl(), getDeviceId(), MASTER_PROMO_CODE)
        _purchaseState.value = VipPurchaseState.Success("✅ اشتراک پریمیوم مادام‌العمر با موفقیت فعال شد!")
        PurchaseResult.Success
    }

    suspend fun restorePurchaseFromMyket(): PurchaseResult = withContext(Dispatchers.IO) {
        if (_isVip.value || prefs.getBoolean(KEY_IS_VIP, false) || prefs.getBoolean(KEY_PROMO_UNLOCKED, false)) {
            _isVip.value = true
            return@withContext PurchaseResult.Success
        }
        PurchaseResult.Error("هیچ اشتراک فعالی یافت نشد. در صورت داشتن کد تخفیف، کد 57589188 را وارد کنید.")
    }

    private suspend fun <T> bindMyketServiceAndExecute(
        context: Context,
        block: (IBinder) -> T?
    ): T? = withTimeoutOrNull(4000L) {
        suspendCancellableCoroutine { cont ->
            val intent = Intent("ir.mservices.market.InAppBillingService.BIND").apply {
                setPackage(MYKET_PACKAGE)
            }
            var resumed = false
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    if (!resumed) {
                        resumed = true
                        val result = try {
                            if (service != null) block(service) else null
                        } catch (_: Exception) {
                            null
                        } finally {
                            try { context.unbindService(this) } catch (_: Exception) {}
                        }
                        cont.resume(result)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    if (!resumed) {
                        resumed = true
                        cont.resume(null)
                    }
                }
            }
            val bound = try {
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            } catch (_: Exception) {
                false
            }
            if (!bound && !resumed) {
                resumed = true
                cont.resume(null)
            }
            cont.invokeOnCancellation {
                try { context.unbindService(connection) } catch (_: Exception) {}
            }
        }
    }
}
