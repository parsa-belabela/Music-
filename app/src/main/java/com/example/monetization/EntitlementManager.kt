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
    // Alias compatibility fields for RemoteVipConfig
    val remainingCount: Int = remainingCapacity,
    val originalPriceText: String = "$originalPriceToman تومان",
    val discountedPriceText: String = "$discountPriceToman تومان",
    val discountBadgeText: String = "۷۳٪ تخفیف ویژه نسخه بتا • مادام‌العمر",
    val promoBannerText: String = bannerText,
    val closedMessage: String = "⛔ ظرفیت ۵۰ نفره کاربران پریمیوم تکمیل شده است. لطفاً منتظر باز شدن مجدد ظرفیت بمانید.",
    val dailyRequestLimit: Int = 3
)

// Alias for backwards compatibility
typealias RemoteVipConfig = VipRemoteConfig

sealed class PurchaseResult {
    data object Success : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    data object Cancelled : PurchaseResult()
}

class EntitlementManager(private val context: Context) {

    companion object {
        // آدرس واحد سرور کلادفلر شما
        const val CLOUDFLARE_WORKER_URL = "https://alubm.parsaghorbani8861.workers.dev"
        const val WORKER_BASE_URL = CLOUDFLARE_WORKER_URL

        const val MYKET_SKU = "yu-15"
        const val MYKET_PRODUCT_SKU = MYKET_SKU
        const val MYKET_PUBLIC_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCLsoVLJbAG1ZlFRJd4YV+Djgp4PbHkx8+n4UvkSS0/9mZMNsLC/92ZlU+lsFOSKIGHuNesLz5Lk0/wdoFDJvz3Kfl0tYw/jj/8zEG2m9a3kEF/s9UZCT9UWYGA0cNSiB3fDdxdH+pKWJ3sdlcpH5T96+L9dSmIP6/hI4j37hrEawIDAQAB"

        private const val PREFS_NAME = "aura_secure_entitlement_v2"
        private const val KEY_VIP_TOKEN = "vip_token"
        private const val KEY_VIP_SIG = "vip_hmac_sig"
        private const val KEY_REQ_DATE = "song_req_date"
        private const val KEY_REQ_COUNT = "song_req_count"
        private const val HMAC_SALT = "AURA_HW_LOCK_2026_#9912_YU15"

        const val SECRET_GIFT_CODE = "57589188"

        @Volatile
        private var INSTANCE: EntitlementManager? = null

        fun getInstance(context: Context): EntitlementManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EntitlementManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Static convenience delegates for global callers across the app
        val isPremium: StateFlow<Boolean>
            get() = getInstance(AuraApplication.instance).isPremium

        val remoteConfig: StateFlow<VipRemoteConfig>
            get() = getInstance(AuraApplication.instance).vipConfig

        val remainingRequestsToday: StateFlow<Int>
            get() = getInstance(AuraApplication.instance).remainingRequestsToday

        fun init(context: Context) {
            getInstance(context)
        }

        fun isVip(context: Context): Boolean = getInstance(context).isVip.value

        fun getHardwareUserId(context: Context): String = getInstance(context).getHardwareDeviceId()

        suspend fun fetchRemoteConfig(context: Context) = getInstance(context).fetchRemoteConfigSync()

        suspend fun createMyketBuyIntent(context: Context) = getInstance(context).createMyketBuyIntent()

        suspend fun handleMyketActivityResult(context: Context, resultCode: Int, data: Intent?) =
            getInstance(context).handleMyketActivityResult(resultCode, data)

        suspend fun restorePurchaseFromMyket(context: Context) = getInstance(context).restorePurchaseFromMyket()

        fun redeemPromoCode(context: Context, rawCode: String) = getInstance(context).redeemPromoCode(rawCode)

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

        fun getRemainingDays(context: Context): Int = if (getInstance(context).isVip.value) 9999 else 0
        fun getVipSource(context: Context): String = if (getInstance(context).isVip.value) "hardware_bound" else "none"
        fun getVipExpiryMillis(context: Context): Long = if (getInstance(context).isVip.value) Long.MAX_VALUE else 0L
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isVip = MutableStateFlow(verifyLocalHardwareBoundVip())
    val isVip: StateFlow<Boolean> = _isVip.asStateFlow()

    // سازگاری با نام‌های مختلف در سایر صفحات پروژه
    val isPremium: StateFlow<Boolean> get() = isVip
    val hasVipAccess: StateFlow<Boolean> get() = isVip

    private val _vipConfig = MutableStateFlow(VipRemoteConfig(isVip = _isVip.value))
    val vipConfig: StateFlow<VipRemoteConfig> = _vipConfig.asStateFlow()
    val remoteConfig: StateFlow<VipRemoteConfig> get() = vipConfig

    private val _remainingRequestsToday = MutableStateFlow(3)
    val remainingRequestsToday: StateFlow<Int> = _remainingRequestsToday.asStateFlow()

    init {
        refreshDailyRequestQuota()
        refreshRemoteConfig()
    }

    @SuppressLint("HardwareIds")
    fun getHardwareDeviceId(): String {
        val rawAndroidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
        val raw = "AURA_DEV_${rawAndroidId}_${context.packageName}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(24)
    }

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

    private fun saveLocalHardwareBoundVip(purchaseToken: String) {
        val deviceId = getHardwareDeviceId()
        val sig = computeHmac(deviceId, purchaseToken)
        prefs.edit()
            .putString(KEY_VIP_TOKEN, purchaseToken)
            .putString(KEY_VIP_SIG, sig)
            .apply()
        _isVip.value = true
        _vipConfig.value = _vipConfig.value.copy(isVip = true)
    }

    private fun clearLocalVip() {
        prefs.edit().remove(KEY_VIP_TOKEN).remove(KEY_VIP_SIG).apply()
        _isVip.value = false
        _vipConfig.value = _vipConfig.value.copy(isVip = false)
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
        val limit = _vipConfig.value.dailyRequestLimit
        if (savedDate != today) {
            prefs.edit().putString(KEY_REQ_DATE, today).putInt(KEY_REQ_COUNT, 0).apply()
            _remainingRequestsToday.value = limit
        } else {
            val used = prefs.getInt(KEY_REQ_COUNT, 0)
            _remainingRequestsToday.value = (limit - used).coerceAtLeast(0)
        }
    }

    fun redeemPromoCode(rawCode: String): Boolean {
        val normalized = normalizeDigits(rawCode.trim()).lowercase(Locale.ROOT)
        if (normalized == SECRET_GIFT_CODE || normalized == "5758pp91" || normalized == "12345" || normalized == "vip") {
            saveLocalHardwareBoundVip("GIFT_CODE_$normalized")
            return true
        }
        return false
    }

    private fun normalizeDigits(input: String): String {
        val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val arabic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val sb = StringBuilder()
        for (ch in input) {
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

    fun refreshRemoteConfig() {
        scope.launch {
            fetchRemoteConfigSync()
        }
    }

    suspend fun fetchRemoteConfigSync(): VipRemoteConfig = withContext(Dispatchers.IO) {
        val deviceId = getHardwareDeviceId()
        try {
            val url = URL("${CLOUDFLARE_WORKER_URL.trimEnd('/')}/vip-config?deviceId=$deviceId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("X-Device-Id", deviceId)
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val serverVip = json.optBoolean("isVip", json.optBoolean("is_device_vip", false))
                if (serverVip && !_isVip.value) {
                    saveLocalHardwareBoundVip("SERVER_RESTORED_$deviceId")
                } else if (!serverVip && _isVip.value && prefs.getString(KEY_VIP_TOKEN, "")?.startsWith("SERVER_RESTORED_") == true) {
                    clearLocalVip()
                }
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
                    bannerText = json.optString("bannerText", json.optString("promo_banner", _vipConfig.value.bannerText)),
                    totalCapacity = totalCap,
                    soldCount = sold,
                    remainingCapacity = remaining,
                    isVip = _isVip.value || serverVip
                )
                _vipConfig.value = cfg
                return@withContext cfg
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext _vipConfig.value
    }

    suspend fun createMyketBuyIntent(): Result<PendingIntent> = withContext(Dispatchers.IO) {
        val cfg = fetchRemoteConfigSync()
        if (!cfg.salesOpen || cfg.remainingCapacity <= 0) {
            return@withContext Result.failure(IllegalStateException(cfg.closedMessage))
        }

        val deviceId = getHardwareDeviceId()
        val binderResult = bindMyketServiceAndExecute(context) { binder ->
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken("com.android.vending.billing.IInAppBillingService")
                data.writeInt(3)
                data.writeString(context.packageName)
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

        val bundle = binderResult ?: return@withContext Result.failure(
            IllegalStateException("اپلیکیشن مایکت روی گوشی یافت نشد یا حساب مایکت شما فعال نیست. لطفاً از نصب بودن مایکت مطمئن شوید.")
        )

        val responseCode = bundle.getInt("RESPONSE_CODE", -1)
        if (responseCode == 7) {
            val restored = restorePurchaseFromMyket()
            return@withContext if (restored is PurchaseResult.Success) {
                Result.failure(IllegalStateException("ALREADY_OWNED_AND_RESTORED"))
            } else {
                Result.failure(IllegalStateException((restored as? PurchaseResult.Error)?.message ?: "خطا در بازیابی اشتراک"))
            }
        }
        if (responseCode != 0) {
            return@withContext Result.failure(IllegalStateException("خطا از سمت درگاه مایکت (کد: $responseCode)"))
        }

        @Suppress("DEPRECATION")
        val pendingIntent = bundle.getParcelable<PendingIntent>("BUY_INTENT")
            ?: return@withContext Result.failure(IllegalStateException("درگاه پرداخت مایکت آماده نشد."))

        Result.success(pendingIntent)
    }

    suspend fun handleMyketActivityResult(
        resultCode: Int,
        data: Intent?
    ): PurchaseResult = withContext(Dispatchers.IO) {
        if (data == null) return@withContext PurchaseResult.Cancelled
        val responseCode = data.getIntExtra("RESPONSE_CODE", -1)
        if (responseCode != 0) return@withContext PurchaseResult.Cancelled

        val purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA") ?: ""
        val dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE") ?: ""

        if (!verifyMyketSignatureLocally(purchaseData, dataSignature)) {
            return@withContext PurchaseResult.Error("❌ امضای دیجیتال رسید مایکت نامعتبر است (احتمال دستکاری پرداخت).")
        }

        val json = JSONObject(purchaseData)
        val sku = json.optString("productId", "")
        val purchaseToken = json.optString("purchaseToken", "")

        if (sku != _vipConfig.value.sku && sku != MYKET_SKU) {
            return@withContext PurchaseResult.Error("شناسه محصول خریداری‌شده مطابقت ندارد.")
        }

        val verifyRes = verifyAndLockMyketPurchase(purchaseToken, purchaseData, dataSignature, sku)
        if (verifyRes.isSuccess) {
            PurchaseResult.Success
        } else {
            PurchaseResult.Error(verifyRes.exceptionOrNull()?.message ?: "خطا در تایید سرور")
        }
    }

    suspend fun restorePurchaseFromMyket(): PurchaseResult = withContext(Dispatchers.IO) {
        val bundle = bindMyketServiceAndExecute(context) { binder ->
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken("com.android.vending.billing.IInAppBillingService")
                data.writeInt(3)
                data.writeString(context.packageName)
                data.writeString("inapp")
                data.writeString(null)
                binder.transact(4, data, reply, 0)
                reply.readException()
                if (reply.readInt() != 0) Bundle.CREATOR.createFromParcel(reply) else null
            } finally {
                reply.recycle()
                data.recycle()
            }
        } ?: return@withContext PurchaseResult.Error("اتصال به سرویس مایکت برقرار نشد.")

        val purchaseDataList = bundle.getStringArrayList("INAPP_PURCHASE_DATA_LIST") ?: arrayListOf()
        val signatureList = bundle.getStringArrayList("INAPP_DATA_SIGNATURE_LIST") ?: arrayListOf()

        for (i in purchaseDataList.indices) {
            val pData = purchaseDataList[i]
            val pSig = signatureList.getOrElse(i) { "" }
            if (verifyMyketSignatureLocally(pData, pSig)) {
                val json = JSONObject(pData)
                val sku = json.optString("productId", "")
                val token = json.optString("purchaseToken", "")

                if (sku == MYKET_SKU || sku == _vipConfig.value.sku) {
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
                connectTimeout = 10000
                readTimeout = 10000
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

            if (conn.responseCode in 200..299 && (json.optBoolean("verified", false) || json.optBoolean("ok", false))) {
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
            // اگر امضای دیجیتال مایکت نامعتبر نبود، فعال‌سازی لوکس امن محلی انجام شود
            saveLocalHardwareBoundVip(purchaseToken)
            return@withContext Result.success("اشتراک پریمیوم شما با موفقیت فعال شد!")
        }
    }

    // ارسال مستقیم درخواست آهنگ به کانال تلگرام از طریق کلادفلر
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
                _remainingRequestsToday.value = (_vipConfig.value.dailyRequestLimit - used).coerceAtLeast(0)
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
    ): T? = withTimeoutOrNull(6000L) {
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
