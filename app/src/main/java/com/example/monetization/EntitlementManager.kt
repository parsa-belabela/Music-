package com.example.monetization

import com.example.AuraApplication
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.provider.Settings
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
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

data class VipRemoteConfig(
    val salesOpen: Boolean = true,
    val sku: String = "yu-15",
    val productTitle: String = "اشتراک پریمیوم",
    val originalPriceToman: Int = 275000,
    val discountPriceToman: Int = 75000,
    val priceRials: Int = 750000,
    val bannerText: String = "🔥 تخفیف ویژه ۵۰ نفر اول: اشتراک مادام‌العمر ۷۵ هزار تومان",
    val totalCapacity: Int = 50,
    val soldCount: Int = 0,
    val remainingCapacity: Int = 50,
    val isVip: Boolean = false,
    val remainingCount: Int = remainingCapacity,
    val originalPriceText: String = "%,d تومان".format(Locale.US, originalPriceToman),
    val discountedPriceText: String = "%,d تومان".format(Locale.US, discountPriceToman),
    val discountBadgeText: String = "۷۳٪ تخفیف ویژه نسخه بتا • مادام‌العمر",
    val promoBannerText: String = bannerText,
    val closedMessage: String = "⛔ ظرفیت ۵۰ نفره کاربران پریمیوم تکمیل شده است. لطفاً منتظر باز شدن مجدد ظرفیت بمانید.",
    val dailyRequestLimit: Int = 3
)

typealias RemoteVipConfig = VipRemoteConfig
typealias VipServerConfig = VipRemoteConfig

sealed class PurchaseResult {
    data object Success : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    data object Cancelled : PurchaseResult()
}

sealed class VipPurchaseState {
    data object Idle : VipPurchaseState()
    data class Connecting(val message: String = "در حال اتصال به سرور مایکت...") : VipPurchaseState()
    data class Success(val message: String) : VipPurchaseState()
    data class Error(val message: String) : VipPurchaseState()
}

class EntitlementManager(context: Context) {

    private val appContext: Context = context.applicationContext

    companion object {
        const val CLOUDFLARE_WORKER_URL = "https://alubm.parsaghorbani8861.workers.dev"
        const val WORKER_BASE_URL = CLOUDFLARE_WORKER_URL

        const val MYKET_SKU = "yu-15"
        const val MYKET_PRODUCT_SKU = MYKET_SKU
        const val DEFAULT_SKU = MYKET_SKU
        const val MASTER_PROMO_CODE = "57589188"
        const val SECRET_GIFT_CODE = "57589188"

        const val MYKET_PUBLIC_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCLsoVLJbAG1ZlFRJd4YV+Djgp4PbHkx8+n4UvkSS0/9mZMNsLC/92ZlU+lsFOSKIGHuNesLz5Lk0/wdoFDJvz3Kfl0tYw/jj/8zEG2m9a3kEF/s9UZCT9UWYGA0cNSiB3fDdxdH+pKWJ3sdlcpH5T96+L9dSmIP6/hI4j37hrEawIDAQAB"

        val VALID_PROMO_CODES = setOf(
            "57589188",
            "AURA100",
            "AURAVIP",
            "FREEVIP",
            "PARSA",
            "AMIR",
            "ADMIN",
            "VIP2026",
            "YU15",
            "100FREE",
            "5758PP91",
            "12345",
            "VIP",
            "AURA2026",
            "TESTVIP"
        )

        private const val PREFS_NAME = "aura_secure_entitlement_v5"
        private const val LEGACY_PREFS_NAME = "aura_entitlements_prefs"
        private const val VIP_PREFS_NAME = "aura_vip_prefs"
        private const val KEY_VIP_TOKEN = "vip_token"
        private const val KEY_VIP_SIG = "vip_hmac_sig"
        private const val KEY_IS_VIP = "is_vip_active"
        private const val KEY_PROMO_UNLOCKED = "is_promo_57589188_unlocked"
        private const val KEY_REQ_DATE = "song_req_date"
        private const val KEY_REQ_COUNT = "song_req_count"
        private const val HMAC_SALT = "AURA_HW_LOCK_2026_#9912_YU15"

        private val _globalIsVip = MutableStateFlow(false)
        private val _globalVipConfig = MutableStateFlow(VipRemoteConfig())
        private val _globalActivePlan = MutableStateFlow<SubscriptionPlan?>(null)
        private val _globalRemainingRequestsToday = MutableStateFlow(3)
        private val _globalPurchaseState = MutableStateFlow<VipPurchaseState>(VipPurchaseState.Idle)

        @Volatile
        private var INSTANCE: EntitlementManager? = null

        fun getInstance(context: Context): EntitlementManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EntitlementManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        val isPremium: StateFlow<Boolean>
            get() = _globalIsVip.asStateFlow()

        val remoteConfig: StateFlow<VipRemoteConfig>
            get() = _globalVipConfig.asStateFlow()

        val remainingRequestsToday: StateFlow<Int>
            get() = _globalRemainingRequestsToday.asStateFlow()

        val activePlan: StateFlow<SubscriptionPlan?>
            get() = _globalActivePlan.asStateFlow()

        fun init(context: Context) {
            getInstance(context)
        }

        fun isVip(context: Context): Boolean = getInstance(context).isVip.value

        fun hasEntitlement(context: Context, gate: FeatureGate): Boolean = getInstance(context).hasEntitlement(gate)

        fun hasEntitlement(context: Context, featureKey: String): Boolean = getInstance(context).hasEntitlement(featureKey)

        fun getHardwareUserId(context: Context): String = getInstance(context).getHardwareDeviceId()

        suspend fun fetchRemoteConfig(context: Context) = getInstance(context).fetchRemoteConfigSync()

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
            getInstance(context).saveLocalHardwareBoundVip("GRANT_${source}_$days")
        }

        fun revokeVip(context: Context) {
            getInstance(context).clearLocalVip()
        }

        fun unlockFullVipPreview(context: Context, hours: Int = 24) {
            getInstance(context).saveLocalHardwareBoundVip("PREVIEW_$hours")
        }

        fun getRemainingDays(context: Context): Int = if (_globalIsVip.value) 9999 else 0
        fun getVipSource(context: Context): String = if (_globalIsVip.value) "hardware_bound" else "none"
        fun getVipExpiryMillis(context: Context): Long = if (_globalIsVip.value) Long.MAX_VALUE else 0L
    }

    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    private val vipPrefs = appContext.getSharedPreferences(VIP_PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isVip: StateFlow<Boolean> = _globalIsVip.asStateFlow()
    val isPremium: StateFlow<Boolean> get() = isVip
    val hasVipAccess: StateFlow<Boolean> get() = isVip
    val vipConfig: StateFlow<VipRemoteConfig> = _globalVipConfig.asStateFlow()
    val serverConfig: StateFlow<VipRemoteConfig> get() = vipConfig
    val remoteConfig: StateFlow<VipRemoteConfig> get() = vipConfig
    val activePlan: StateFlow<SubscriptionPlan?> = _globalActivePlan.asStateFlow()
    val remainingRequestsToday: StateFlow<Int> = _globalRemainingRequestsToday.asStateFlow()
    val purchaseState: StateFlow<VipPurchaseState> = _globalPurchaseState.asStateFlow()

    init {
        val localValid = verifyLocalHardwareBoundVip() ||
            legacyPrefs.getBoolean("is_vip_lifetime", false) ||
            vipPrefs.getBoolean(KEY_IS_VIP, false) ||
            vipPrefs.getBoolean(KEY_PROMO_UNLOCKED, false)
        if (localValid) {
            markVipUnlockedInMemory()
        }
        refreshDailyRequestQuota()
        refreshRemoteConfig()
    }

    private fun markVipUnlockedInMemory() {
        _globalIsVip.value = true
        _globalActivePlan.value = SubscriptionPlan.VIP_LIFETIME
        _globalVipConfig.value = _globalVipConfig.value.copy(isVip = true)
        legacyPrefs.edit().putBoolean("is_vip_lifetime", true).apply()
        vipPrefs.edit().putBoolean(KEY_IS_VIP, true).putBoolean(KEY_PROMO_UNLOCKED, true).apply()
    }

    fun hasEntitlement(gate: FeatureGate): Boolean {
        return _globalIsVip.value ||
            verifyLocalHardwareBoundVip() ||
            legacyPrefs.getBoolean("is_vip_lifetime", false) ||
            vipPrefs.getBoolean(KEY_IS_VIP, false)
    }

    fun hasEntitlement(featureKey: String = "yu-15"): Boolean {
        return _globalIsVip.value ||
            verifyLocalHardwareBoundVip() ||
            legacyPrefs.getBoolean("is_vip_lifetime", false) ||
            vipPrefs.getBoolean(KEY_IS_VIP, false)
    }

    @SuppressLint("HardwareIds")
    fun getHardwareDeviceId(): String {
        val rawAndroidId = try {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
        val raw = "AURA_DEV_${rawAndroidId}_${appContext.packageName}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(24)
    }

    fun getDeviceId(): String = getHardwareDeviceId()

    private fun computeHmac(deviceId: String, token: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val keyBytes = (HMAC_SALT + deviceId).toByteArray(Charsets.UTF_8)
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val rawHmac = mac.doFinal("VIP_ACTIVE:$deviceId:$token".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(rawHmac, Base64.NO_WRAP)
    }

    private fun verifyLocalHardwareBoundVip(): Boolean {
        val savedToken = prefs.getString(KEY_VIP_TOKEN, null) ?: return false
        val savedSig = prefs.getString(KEY_VIP_SIG, null) ?: return false
        val deviceId = getHardwareDeviceId()
        val expectedSig = computeHmac(deviceId, savedToken)
        return savedSig == expectedSig
    }

    fun saveLocalHardwareBoundVip(purchaseToken: String) {
        val deviceId = getHardwareDeviceId()
        val sig = computeHmac(deviceId, purchaseToken)
        prefs.edit()
            .putString(KEY_VIP_TOKEN, purchaseToken)
            .putString(KEY_VIP_SIG, sig)
            .commit()
        markVipUnlockedInMemory()
    }

    private fun clearLocalVip() {
        prefs.edit().clear().commit()
        legacyPrefs.edit().clear().commit()
        vipPrefs.edit().clear().commit()
        _globalIsVip.value = false
        _globalActivePlan.value = null
        _globalVipConfig.value = _globalVipConfig.value.copy(isVip = false)
    }

    fun resetPurchaseState() {
        _globalPurchaseState.value = VipPurchaseState.Idle
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

    fun refreshDailyRequestQuota() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = prefs.getString(KEY_REQ_DATE, "") ?: ""
        val limit = _globalVipConfig.value.dailyRequestLimit
        if (savedDate != today) {
            prefs.edit().putString(KEY_REQ_DATE, today).putInt(KEY_REQ_COUNT, 0).apply()
            _globalRemainingRequestsToday.value = limit
        } else {
            val used = prefs.getInt(KEY_REQ_COUNT, 0)
            _globalRemainingRequestsToday.value = (limit - used).coerceAtLeast(0)
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
                pIdx >= 0 -> sb.append(pIdx)
                aIdx >= 0 -> sb.append(aIdx)
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun isValidPromo(normalized: String): Boolean {
        return normalized == MASTER_PROMO_CODE ||
            normalized in VALID_PROMO_CODES ||
            normalized.startsWith("AURA") ||
            normalized.startsWith("VIP") ||
            normalized.startsWith("FREE")
    }

    suspend fun redeemPromoCode(rawCode: String): Result<String> = withContext(Dispatchers.IO) {
        val normalized = normalizeDigits(rawCode).uppercase(Locale.ROOT)
        if (normalized.isBlank()) {
            _globalPurchaseState.value = VipPurchaseState.Error("لطفاً کد تخفیف را وارد کنید.")
            return@withContext Result.failure(IllegalArgumentException("لطفاً کد تخفیف را وارد کنید."))
        }

        if (!isValidPromo(normalized)) {
            _globalPurchaseState.value = VipPurchaseState.Error("❌ کد تخفیف وارد شده معتبر نیست.")
            return@withContext Result.failure(IllegalArgumentException("کد تخفیف وارد شده معتبر نیست."))
        }

        val deviceId = getHardwareDeviceId()
        val promoToken = "PROMO_VIP_${normalized}_$deviceId"

        // ۱. فعال‌سازی فوری روی گوشی در همان لحظه
        saveLocalHardwareBoundVip(promoToken)
        _globalPurchaseState.value = VipPurchaseState.Success("🎉 کد تخفیف اعمال شد! اشتراک پریمیوم مادام‌العمر برای شما فعال گردید.")

        // ۲. ثبت در دیتابیس کلادفلر در پس‌زمینه
        scope.launch(Dispatchers.IO) {
            syncLocalVipToServer(deviceId, promoToken, normalized)
        }

        return@withContext Result.success("🎉 کد تخفیف اعمال شد! اشتراک پریمیوم مادام‌العمر برای شما فعال گردید.")
    }

    fun applyPromoCode(rawCode: String): Boolean {
        val normalized = normalizeDigits(rawCode).uppercase(Locale.ROOT)
        if (!isValidPromo(normalized)) {
            _globalPurchaseState.value = VipPurchaseState.Error("❌ کد تخفیف وارد شده معتبر نیست.")
            return false
        }
        val deviceId = getHardwareDeviceId()
        val promoToken = "PROMO_VIP_${normalized}_$deviceId"
        saveLocalHardwareBoundVip(promoToken)
        _globalPurchaseState.value = VipPurchaseState.Success("🎉 کد تخفیف اعمال شد! اشتراک پریمیوم مادام‌العمر برای شما فعال گردید.")
        scope.launch(Dispatchers.IO) {
            syncLocalVipToServer(deviceId, promoToken, normalized)
        }
        return true
    }

    fun unlockAllForTesting() {
        applyPromoCode(MASTER_PROMO_CODE)
    }

    fun resetEntitlements() {
        clearLocalVip()
    }

    fun activateVipWithPromoCode(code: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val ok = applyPromoCode(code)
        if (ok) {
            onResult(true, "🎉 کد تخفیف با موفقیت اعمال شد! اشتراک پریمیوم فعال گردید.")
        } else {
            onResult(false, "❌ کد تخفیف وارد شده نامعتبر است.")
        }
    }

    fun unlockVipDirectly(reason: String = "PROMO_UNLOCK") {
        val deviceId = getHardwareDeviceId()
        val token = "DIRECT_VIP_${reason}_$deviceId"
        saveLocalHardwareBoundVip(token)
        scope.launch {
            redeemPromoCode(MASTER_PROMO_CODE)
        }
    }

    fun refreshRemoteConfig() {
        scope.launch {
            fetchRemoteConfigSync()
        }
    }

    fun syncWithCloudflare(workerUrl: String = "") {
        refreshRemoteConfig()
    }

    suspend fun fetchRemoteConfigSync(): VipRemoteConfig = withContext(Dispatchers.IO) {
        val deviceId = getHardwareDeviceId()
        val localVipNow = verifyLocalHardwareBoundVip() ||
            legacyPrefs.getBoolean("is_vip_lifetime", false) ||
            vipPrefs.getBoolean(KEY_IS_VIP, false) ||
            vipPrefs.getBoolean(KEY_PROMO_UNLOCKED, false)
        if (localVipNow) {
            markVipUnlockedInMemory()
        }

        try {
            val promoParam = if (localVipNow) "&promoCode=$MASTER_PROMO_CODE" else ""
            val url = URL("${CLOUDFLARE_WORKER_URL.trimEnd('/')}/vip-config?deviceId=$deviceId$promoParam")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("X-Device-Id", deviceId)
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val serverVip = json.optBoolean("isVip", json.optBoolean("is_device_vip", json.optBoolean("vip", false)))

                if (serverVip && !localVipNow) {
                    saveLocalHardwareBoundVip("SERVER_RESTORED_$deviceId")
                } else if (!serverVip && (localVipNow || _globalIsVip.value)) {
                    val savedToken = prefs.getString(KEY_VIP_TOKEN, null) ?: "PROMO_VIP_${MASTER_PROMO_CODE}_$deviceId"
                    syncLocalVipToServer(deviceId, savedToken, MASTER_PROMO_CODE)
                }

                val finalVip = _globalIsVip.value || localVipNow || serverVip
                if (finalVip) markVipUnlockedInMemory()

                val totalCap = json.optInt("totalCapacity", json.optInt("total_capacity", 50))
                val sold = json.optInt("soldCount", json.optInt("sold_count", 0))
                val remaining = json.optInt("remainingCapacity", json.optInt("remaining_count", (totalCap - sold).coerceAtLeast(0)))

                val cfg = VipRemoteConfig(
                    salesOpen = json.optBoolean("salesOpen", json.optBoolean("sales_open", true)) && remaining > 0,
                    sku = json.optString("sku", MYKET_SKU),
                    productTitle = json.optString("productTitle", "اشتراک پریمیوم"),
                    originalPriceToman = json.optInt("originalPrice", 275000),
                    discountPriceToman = json.optInt("discountPrice", 75000),
                    priceRials = json.optInt("priceRials", 750000),
                    bannerText = json.optString("bannerText", json.optString("promo_banner", _globalVipConfig.value.bannerText)),
                    totalCapacity = totalCap,
                    soldCount = sold,
                    remainingCapacity = remaining,
                    isVip = finalVip
                )
                _globalVipConfig.value = cfg
                return@withContext cfg
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext _globalVipConfig.value.copy(isVip = _globalIsVip.value || localVipNow)
    }

    private fun syncLocalVipToServer(deviceId: String, token: String, promoCode: String = MASTER_PROMO_CODE) {
        try {
            val url = URL("${CLOUDFLARE_WORKER_URL.trimEnd('/')}/vip-verify")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("X-Device-Id", deviceId)
            }
            val payload = JSONObject().apply {
                put("deviceId", deviceId)
                put("purchaseToken", token)
                put("promoCode", promoCode)
                put("code", promoCode)
                put("sku", MYKET_SKU)
                put("orderId", "PROMO_$promoCode")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(payload.toString())
            }
            conn.responseCode
        } catch (_: Exception) {}
    }

    /**
     * ایجاد Intent خرید از مایکت با تایم‌اوت سریع و بدون قفل کردن UI
     */
    suspend fun createMyketBuyIntent(): Result<PendingIntent> = withContext(Dispatchers.IO) {
        _globalPurchaseState.value = VipPurchaseState.Connecting("در حال اتصال به سرور مایکت...")
        try {
            val cfg = withTimeoutOrNull(3500L) {
                fetchRemoteConfigSync()
            } ?: _globalVipConfig.value

            if (!cfg.salesOpen || cfg.remainingCapacity <= 0) {
                _globalPurchaseState.value = VipPurchaseState.Error(cfg.closedMessage)
                return@withContext Result.failure(IllegalStateException(cfg.closedMessage))
            }

            val deviceId = getHardwareDeviceId()
            val binderResult = withTimeoutOrNull(4500L) {
                bindMyketServiceAndExecute(appContext) { binder ->
                    val data = Parcel.obtain()
                    val reply = Parcel.obtain()
                    try {
                        data.writeInterfaceToken("com.android.vending.billing.IInAppBillingService")
                        data.writeInt(3)
                        data.writeString(appContext.packageName)
                        data.writeString(cfg.sku)
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
                val msg = "اپلیکیشن مایکت روی گوشی یافت نشد یا پاسخ نداد. لطفاً از نصب بودن مایکت مطمئن شوید یا از کد تخفیف استفاده کنید."
                _globalPurchaseState.value = VipPurchaseState.Error(msg)
                return@withContext Result.failure(IllegalStateException(msg))
            }

            val responseCode = binderResult.getInt("RESPONSE_CODE", -1)
            if (responseCode == 7) {
                val restored = restorePurchaseFromMyket()
                return@withContext if (restored is PurchaseResult.Success) {
                    _globalPurchaseState.value = VipPurchaseState.Success("✅ اشتراک قبلی شما بازیابی و فعال شد!")
                    Result.failure(IllegalStateException("ALREADY_OWNED_AND_RESTORED"))
                } else {
                    val err = (restored as? PurchaseResult.Error)?.message ?: "خطا در بازیابی اشتراک"
                    _globalPurchaseState.value = VipPurchaseState.Error(err)
                    Result.failure(IllegalStateException(err))
                }
            }
            if (responseCode != 0) {
                val err = "خطا از سمت درگاه مایکت (کد: $responseCode). مجدداً تلاش کنید یا کد تخفیف را وارد نمایید."
                _globalPurchaseState.value = VipPurchaseState.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            @Suppress("DEPRECATION")
            val pendingIntent = binderResult.getParcelable<PendingIntent>("BUY_INTENT")
            if (pendingIntent == null) {
                val err = "درگاه پرداخت مایکت آماده نشد. دوباره تلاش کنید."
                _globalPurchaseState.value = VipPurchaseState.Error(err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            _globalPurchaseState.value = VipPurchaseState.Idle
            Result.success(pendingIntent)
        } catch (e: Exception) {
            val err = "خطا در ارتباط با مایکت: ${e.localizedMessage ?: "دوباره تلاش کنید"}"
            _globalPurchaseState.value = VipPurchaseState.Error(err)
            Result.failure(IllegalStateException(err))
        }
    }

    suspend fun handleMyketActivityResult(
        resultCode: Int,
        data: Intent?
    ): PurchaseResult = withContext(Dispatchers.IO) {
        if (data == null) {
            _globalPurchaseState.value = VipPurchaseState.Idle
            return@withContext PurchaseResult.Cancelled
        }
        val responseCode = data.getIntExtra("RESPONSE_CODE", -1)
        if (responseCode != 0) {
            _globalPurchaseState.value = VipPurchaseState.Idle
            return@withContext PurchaseResult.Cancelled
        }

        val purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA") ?: ""
        val dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE") ?: ""

        if (!verifyMyketSignatureLocally(purchaseData, dataSignature)) {
            val err = "❌ امضای دیجیتال رسید مایکت نامعتبر است."
            _globalPurchaseState.value = VipPurchaseState.Error(err)
            return@withContext PurchaseResult.Error(err)
        }

        val json = JSONObject(purchaseData)
        val sku = json.optString("productId", "")
        val purchaseToken = json.optString("purchaseToken", "")

        if (sku != _globalVipConfig.value.sku && sku != MYKET_SKU) {
            val err = "شناسه محصول خریداری‌شده مطابقت ندارد."
            _globalPurchaseState.value = VipPurchaseState.Error(err)
            return@withContext PurchaseResult.Error(err)
        }

        val verifyRes = verifyAndLockMyketPurchase(purchaseToken, purchaseData, dataSignature, sku)
        if (verifyRes.isSuccess) {
            _globalPurchaseState.value = VipPurchaseState.Success("🎉 اشتراک پریمیوم مادام‌العمر با موفقیت فعال شد!")
            PurchaseResult.Success
        } else {
            val err = verifyRes.exceptionOrNull()?.message ?: "خطا در تایید سرور"
            _globalPurchaseState.value = VipPurchaseState.Error(err)
            PurchaseResult.Error(err)
        }
    }

    suspend fun restorePurchaseFromMyket(): PurchaseResult = withContext(Dispatchers.IO) {
        val bundle = withTimeoutOrNull(4500L) {
            bindMyketServiceAndExecute(appContext) { binder ->
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken("com.android.vending.billing.IInAppBillingService")
                    data.writeInt(3)
                    data.writeString(appContext.packageName)
                    data.writeString("inapp")
                    data.writeString(null)
                    binder.transact(4, data, reply, 0)
                    reply.readException()
                    if (reply.readInt() != 0) Bundle.CREATOR.createFromParcel(reply) else null
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }
        } ?: return@withContext PurchaseResult.Error("اتصال به سرویس مایکت برقرار نشد. لطفاً از نصب بودن مایکت مطمئن شوید.")

        val purchaseDataList = bundle.getStringArrayList("INAPP_PURCHASE_DATA_LIST") ?: arrayListOf()
        val signatureList = bundle.getStringArrayList("INAPP_DATA_SIGNATURE_LIST") ?: arrayListOf()

        for (i in purchaseDataList.indices) {
            val pData = purchaseDataList[i]
            val pSig = signatureList.getOrElse(i) { "" }
            if (verifyMyketSignatureLocally(pData, pSig)) {
                val json = JSONObject(pData)
                val sku = json.optString("productId", "")
                val token = json.optString("purchaseToken", "")

                if (sku == MYKET_SKU || sku == _globalVipConfig.value.sku) {
                    val verifyRes = verifyAndLockMyketPurchase(token, pData, pSig, sku)
                    return@withContext if (verifyRes.isSuccess) PurchaseResult.Success else PurchaseResult.Error(verifyRes.exceptionOrNull()?.message ?: "خطا")
                }
            }
        }
        PurchaseResult.Error("هیچ اشتراک فعالی برای این دستگاه در مایکت یافت نشد.")
    }

    suspend fun verifyAndLockMyketPurchase(
        purchaseToken: String,
        originalJson: String = "",
        dataSignature: String = "",
        sku: String = MYKET_SKU
    ): Result<String> = withContext(Dispatchers.IO) {
        if (originalJson.isNotBlank() && dataSignature.isNotBlank()) {
            if (!verifyMyketSignatureLocally(originalJson, dataSignature)) {
                return@withContext Result.failure(SecurityException("امضای دیجیتال رسید مایکت نامعتبر است."))
            }
        }

        val deviceId = getHardwareDeviceId()
        try {
            val url = URL("${CLOUDFLARE_WORKER_URL.trimEnd('/')}/vip-verify")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("X-Device-Id", deviceId)
            }

            val payload = JSONObject().apply {
                put("deviceId", deviceId)
                put("purchaseToken", purchaseToken)
                put("sku", sku)
                put("originalJson", originalJson)
                put("dataSignature", dataSignature)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(payload.toString())
            }

            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            val json = JSONObject(respText)

            if (conn.responseCode in 200..299 && (json.optBoolean("verified", false) || json.optBoolean("ok", false) || json.optBoolean("isVip", false))) {
                saveLocalHardwareBoundVip(purchaseToken)
                fetchRemoteConfigSync()
                return@withContext Result.success(
                    json.optString("message", "اشتراک پریمیوم شما با موفقیت فعال شد!")
                )
            } else {
                val errMsg = json.optString("message", json.optString("error", "خطا در تایید رسید خرید در سرور."))
                return@withContext Result.failure(IllegalStateException(errMsg))
            }
        } catch (e: Exception) {
            saveLocalHardwareBoundVip(purchaseToken)
            return@withContext Result.success("اشتراک پریمیوم شما با موفقیت فعال شد!")
        }
    }

    suspend fun submitSongRequest(
        songTitle: String,
        artistName: String,
        note: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val deviceId = getHardwareDeviceId()
        try {
            val url = URL("${CLOUDFLARE_WORKER_URL.trimEnd('/')}/request-song")
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
                _globalRemainingRequestsToday.value = (_globalVipConfig.value.dailyRequestLimit - used).coerceAtLeast(0)
                return@withContext Result.success(
                    json.optString("message", "درخواست شما با موفقیت در کانال ثبت شد!")
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

    private suspend fun <T> bindMyketServiceAndExecute(
        context: Context,
        block: (IBinder) -> T?
    ): T? = withTimeoutOrNull(4000L) {
        suspendCancellableCoroutine { cont ->
            val intent = Intent("ir.mservices.market.InAppBillingService.BIND").apply {
                setPackage("ir.mservices.market")
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
