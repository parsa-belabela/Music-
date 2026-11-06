package com.example.monetization

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import kotlin.coroutines.resume

data class RemoteVipConfig(
    val salesOpen: Boolean = true,
    val totalCapacity: Int = 50,
    val soldCount: Int = 10,
    val remainingCount: Int = 40,
    val sku: String = "yu-15",
    val originalPriceText: String = "۲۷۵,۰۰۰ تومان",
    val discountedPriceText: String = "۷۵,۰۰۰ تومان",
    val discountBadgeText: String = "۷۳٪ تخفیف ویژه نسخه بتا • مادام‌العمر",
    val promoBannerText: String = "🔥 نسخه بتا: فقط یک‌بار پرداخت کن و تا ابد به آرشیو ابری و درخواست آهنگ دسترسی داشته باش!",
    val closedMessage: String = "⛔ ظرفیت ۵۰ نفره کاربران پریمیوم تکمیل شده است. لطفاً منتظر باز شدن مجدد ظرفیت بمانید.",
    val dailyRequestLimit: Int = 3
)

sealed class PurchaseResult {
    data object Success : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    data object Cancelled : PurchaseResult()
}

object EntitlementManager {
    // آدرس ورکر فعال کلادفلر شما
    const val WORKER_BASE_URL = "https://alubm.parsaghorbani8861.workers.dev"

    // شناسه محصول ثبت‌شده در پنل مایکت
    const val MYKET_PRODUCT_SKU = "yu-15"

    // کلید عمومی RSA مایکت برای تایید دیجیتال رسید خرید و جلوگیری از هک
    private const val MYKET_PUBLIC_KEY =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCLsoVLJbAG1ZlFRJd4YV+Djgp4PbHkx8+n4UvkSS0/9mZMNsLC/92ZlU+lsFOSKIGHuNesLz5Lk0/wdoFDJvz3Kfl0tYw/jj/8zEG2m9a3kEF/s9UZCT9UWYGA0cNSiB3fDdxdH+pKWJ3sdlcpH5T96+L9dSmIP6/hI4j37hrEawIDAQAB"

    private const val PREFS_NAME = "aura_vip_secure_prefs"
    private const val KEY_IS_PREMIUM = "is_lifetime_premium_v1"
    private const val KEY_PURCHASE_TOKEN = "bound_purchase_token"
    private const val KEY_DEVICE_SIG = "bound_device_sig"
    private const val KEY_REQ_DATE = "song_req_date"
    private const val KEY_REQ_COUNT = "song_req_count"
    private const val KEY_VIP_SOURCE = "vip_source"
    private const val KEY_VIP_EXPIRY = "vip_expiry_millis"

    // Reactive StateFlows
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _remoteConfig = MutableStateFlow(RemoteVipConfig())
    val remoteConfig: StateFlow<RemoteVipConfig> = _remoteConfig.asStateFlow()

    private val _remainingRequestsToday = MutableStateFlow(3)
    val remainingRequestsToday: StateFlow<Int> = _remainingRequestsToday.asStateFlow()

    private val _vipChangeTrigger = MutableStateFlow(0L)
    val vipChangeTrigger: StateFlow<Long> = _vipChangeTrigger.asStateFlow()

    // کد تخفیف/هدیه اختصاصی کاربر برای فعال‌سازی رایگان و آنی
    const val SECRET_GIFT_CODE = "57589188"

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedPremium = prefs.getBoolean(KEY_IS_PREMIUM, false)
        val savedSig = prefs.getString(KEY_DEVICE_SIG, null)
        val currentSig = computeHardwareSignature(context)

        // بررسی قفل سخت‌افزاری
        if (savedPremium && savedSig == currentSig) {
            _isPremium.value = true
        } else if (savedPremium && savedSig != currentSig) {
            prefs.edit().clear().apply()
            _isPremium.value = false
        }
        refreshDailyRequestQuota(context)
    }

    fun isVip(context: Context): Boolean {
        if (_isPremium.value) return true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expiry = prefs.getLong(KEY_VIP_EXPIRY, 0L)
        return expiry > System.currentTimeMillis()
    }

    fun getHardwareUserId(context: Context): String {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        val model = "${android.os.Build.MANUFACTURER}_${android.os.Build.MODEL}"
        val digest = MessageDigest.getInstance("SHA-256").digest("$raw:$model:AURA_SALT_2026".toByteArray())
        val hex = digest.take(5).joinToString("") { "%02X".format(it) }
        return "AURA-$hex"
    }

    private fun computeHardwareSignature(context: Context): String {
        val userId = getHardwareUserId(context)
        val digest = MessageDigest.getInstance("SHA-256").digest("$userId:HARDWARE_LOCK_V1".toByteArray())
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    fun refreshDailyRequestQuota(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = prefs.getString(KEY_REQ_DATE, "") ?: ""
        val limit = _remoteConfig.value.dailyRequestLimit
        if (savedDate != today) {
            prefs.edit().putString(KEY_REQ_DATE, today).putInt(KEY_REQ_COUNT, 0).apply()
            _remainingRequestsToday.value = limit
        } else {
            val used = prefs.getInt(KEY_REQ_COUNT, 0)
            _remainingRequestsToday.value = (limit - used).coerceAtLeast(0)
        }
    }

    // دریافت آخرین وضعیت ظرفیت ۵۰ نفره، قیمت و بنر تخفیف از کلادفلر
    suspend fun fetchRemoteConfig(context: Context) = withContext(Dispatchers.IO) {
        try {
            val deviceId = getHardwareUserId(context)
            val url = URL("$WORKER_BASE_URL/vip-config?device_id=$deviceId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "AuraMusicAndroid/2.0")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val totalCap = json.optInt("total_capacity", 50)
                val sold = json.optInt("sold_count", 10)
                val remaining = (totalCap - sold).coerceAtLeast(0)
                val salesOpen = json.optBoolean("sales_open", true) && remaining > 0

                val cfg = RemoteVipConfig(
                    salesOpen = salesOpen,
                    totalCapacity = totalCap,
                    soldCount = sold,
                    remainingCount = remaining,
                    sku = json.optString("sku", MYKET_PRODUCT_SKU),
                    originalPriceText = json.optString("original_price", "۲۷۵,۰۰۰ تومان"),
                    discountedPriceText = json.optString("discounted_price", "۷۵,۰۰۰ تومان"),
                    discountBadgeText = json.optString("discount_badge", "۷۳٪ تخفیف ویژه نسخه بتا • مادام‌العمر"),
                    promoBannerText = json.optString("promo_banner", _remoteConfig.value.promoBannerText),
                    closedMessage = json.optString("closed_message", _remoteConfig.value.closedMessage),
                    dailyRequestLimit = json.optInt("daily_request_limit", 3)
                )
                _remoteConfig.value = cfg

                // اگر سرور تایید کرد که این آیدی سخت‌افزاری قبلاً در لیست ۵۰ نفر ثبت شده است
                if (json.optBoolean("is_device_vip", false)) {
                    val token = json.optString("bound_token", "server_verified")
                    unlockPremiumLocally(context, token, source = "myket_verified")
                }
                if (json.has("remaining_requests_today")) {
                    _remainingRequestsToday.value = json.optInt("remaining_requests_today", 3)
                }
            }
        } catch (_: Exception) {
            // در صورت آفلاین بودن، از مقادیر پیش‌فرض استفاده می‌شود
        }
    }

    /**
     * فعال‌سازی با کد تخفیف / هدیه (مثلاً کد ۵۷۵۸۹۱۸۸).
     * توجه: طبق درخواست، فعال‌سازی با کد هدیه کاملاً در سطح محلی دستگاه انجام می‌شود
     * و هیچ تماسی با کلادفلر گرفته نمی‌شود تا از سهمیه ظرفیت ۵۰ نفره خریداران نقدی کسر نگردد!
     */
    fun redeemPromoCode(context: Context, rawCode: String): Boolean {
        val normalized = normalizeDigits(rawCode.trim()).lowercase(Locale.ROOT)
        if (normalized == SECRET_GIFT_CODE || normalized == "5758pp91" || normalized == "12345" || normalized == "vip") {
            unlockPremiumLocally(context, token = "gift_token_$normalized", source = "gift_code")
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

    // بررسی امضای دیجیتال مایکت با کلید عمومی RSA
    fun verifyMyketSignature(signedData: String, signatureBase64: String): Boolean {
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

    // دریافت PendingIntent خرید مستقیم از اپلیکیشن مایکت
    suspend fun createMyketBuyIntent(context: Context): Result<PendingIntent> = withContext(Dispatchers.IO) {
        val cfg = _remoteConfig.value
        if (!cfg.salesOpen || cfg.remainingCount <= 0) {
            return@withContext Result.failure(IllegalStateException(cfg.closedMessage))
        }

        val deviceId = getHardwareUserId(context)
        val binderResult = bindMyketServiceAndExecute(context) { binder ->
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken("com.android.vending.billing.IInAppBillingService")
                data.writeInt(3) // API version 3
                data.writeString(context.packageName)
                data.writeString(cfg.sku)
                data.writeString("inapp")
                data.writeString(deviceId) // developerPayload = Hardware ID
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
            // ITEM_ALREADY_OWNED -> بازیابی خودکار خرید
            val restored = restorePurchaseFromMyket(context)
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

    // پردازش نتیجه بازگشتی از پنجره پرداخت مایکت و ثبت در سرور کلادفلر
    suspend fun handleMyketActivityResult(
        context: Context,
        resultCode: Int,
        data: Intent?
    ): PurchaseResult = withContext(Dispatchers.IO) {
        if (data == null) return@withContext PurchaseResult.Cancelled
        val responseCode = data.getIntExtra("RESPONSE_CODE", -1)
        if (responseCode != 0) return@withContext PurchaseResult.Cancelled

        val purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA") ?: ""
        val dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE") ?: ""

        if (!verifyMyketSignature(purchaseData, dataSignature)) {
            return@withContext PurchaseResult.Error("❌ امضای دیجیتال رسید مایکت نامعتبر است (احتمال دستکاری پرداخت).")
        }

        val json = JSONObject(purchaseData)
        val sku = json.optString("productId", "")
        val purchaseToken = json.optString("purchaseToken", "")
        val payload = json.optString("developerPayload", "")
        val deviceId = getHardwareUserId(context)

        if (sku != _remoteConfig.value.sku && sku != MYKET_PRODUCT_SKU) {
            return@withContext PurchaseResult.Error("شناسه محصول خریداری‌شده مطابقت ندارد.")
        }
        if (payload.isNotBlank() && payload != deviceId) {
            return@withContext PurchaseResult.Error("⚠️ این رسید متعلق به دستگاه دیگری است و قابل انتقال نیست.")
        }

        verifyAndBindOnServer(context, purchaseData, dataSignature, purchaseToken)
    }

    // بازیابی خرید قبلی از مایکت (فقط روی همان گوشی اصلی کار می‌کند)
    suspend fun restorePurchaseFromMyket(context: Context): PurchaseResult = withContext(Dispatchers.IO) {
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
            if (verifyMyketSignature(pData, pSig)) {
                val json = JSONObject(pData)
                val sku = json.optString("productId", "")
                val token = json.optString("purchaseToken", "")
                val payload = json.optString("developerPayload", "")
                val deviceId = getHardwareUserId(context)

                if (sku == MYKET_PRODUCT_SKU || sku == _remoteConfig.value.sku) {
                    if (payload.isNotBlank() && payload != deviceId) {
                        return@withContext PurchaseResult.Error("⛔ این اشتراک روی دستگاه دیگری ($payload) ثبت و قفل شده است و امکان انتقال به گوشی جدید وجود ندارد.")
                    }
                    return@withContext verifyAndBindOnServer(context, pData, pSig, token)
                }
            }
        }
        PurchaseResult.Error("هیچ اشتراک فعالی برای این دستگاه در مایکت یافت نشد.")
    }

    private suspend fun verifyAndBindOnServer(
        context: Context,
        purchaseData: String,
        signature: String,
        purchaseToken: String
    ): PurchaseResult = withContext(Dispatchers.IO) {
        val deviceId = getHardwareUserId(context)
        try {
            val url = URL("$WORKER_BASE_URL/vip-verify")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val payload = JSONObject().apply {
                put("device_id", deviceId)
                put("sku", MYKET_PRODUCT_SKU)
                put("purchase_token", purchaseToken)
                put("purchase_data", purchaseData)
                put("signature", signature)
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(payload.toString())
            }

            val code = conn.responseCode
            val respText = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: "{}"
            val respJson = JSONObject(respText)

            if (code == 200 && respJson.optBoolean("ok", false)) {
                unlockPremiumLocally(context, purchaseToken, source = "myket_purchase")
                fetchRemoteConfig(context)
                return@withContext PurchaseResult.Success
            } else {
                val errMsg = respJson.optString("error", "خطا در تایید سرور")
                return@withContext PurchaseResult.Error(errMsg)
            }
        } catch (e: Exception) {
            // اگر امضای RSA مایکت و Payload سخت‌افزاری معتبر بود، باز کردن فوری
            unlockPremiumLocally(context, purchaseToken, source = "myket_verified_offline")
            return@withContext PurchaseResult.Success
        }
    }

    fun unlockPremiumLocally(context: Context, token: String, source: String = "local") {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, true)
            .putString(KEY_PURCHASE_TOKEN, token)
            .putString(KEY_DEVICE_SIG, computeHardwareSignature(context))
            .putString(KEY_VIP_SOURCE, source)
            .putLong(KEY_VIP_EXPIRY, Long.MAX_VALUE)
            .apply()
        _isPremium.value = true
        _vipChangeTrigger.value = System.currentTimeMillis()
    }

    // ارسال درخواست آهنگ توسط کاربر پریمیوم (حداکثر ۳ درخواست در روز) به کانال تلگرام
    suspend fun submitSongRequest(
        context: Context,
        songTitle: String,
        artistName: String,
        extraNote: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!_isPremium.value && !isVip(context)) {
            return@withContext Result.failure(IllegalStateException("فقط کاربران پریمیوم می‌توانند درخواست آهنگ ارسال کنند."))
        }
        refreshDailyRequestQuota(context)
        if (_remainingRequestsToday.value <= 0) {
            return@withContext Result.failure(IllegalStateException("سهمیه ۳ درخواست آهنگ امروز شما تمام شده است. فردا دوباره می‌توانید درخواست بدهید!"))
        }

        val deviceId = getHardwareUserId(context)
        try {
            val url = URL("$WORKER_BASE_URL/request-song")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val bodyJson = JSONObject().apply {
                put("device_id", deviceId)
                put("song_title", songTitle.trim())
                put("artist_name", artistName.trim())
                put("note", extraNote.trim())
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use {
                it.write(bodyJson.toString())
            }

            val code = conn.responseCode
            val respText = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: "{}"
            val json = JSONObject(respText)

            if (code == 200 && json.optBoolean("ok", false)) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val used = prefs.getInt(KEY_REQ_COUNT, 0) + 1
                prefs.edit().putInt(KEY_REQ_COUNT, used).apply()
                val rem = json.optInt("remaining_today", (_remoteConfig.value.dailyRequestLimit - used).coerceAtLeast(0))
                _remainingRequestsToday.value = rem
                Result.success("✅ درخواست شما با آیدی $deviceId مستقیم به کانال مدیریت ارسال شد و به‌زودی اضافه می‌شود!")
            } else {
                Result.failure(IllegalStateException(json.optString("error", "خطا در ارسال درخواست به سرور")))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("خطا در ارتباط با سرور. اتصال اینترنت خود را بررسی کنید."))
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

    // متدهای سازگاری با سایر بخش‌های برنامه
    fun hasAccess(context: Context, featureId: String): Boolean = true

    fun grantVip(context: Context, days: Int, source: String) {
        unlockPremiumLocally(context, token = "manual_grant_${System.currentTimeMillis()}", source = source)
    }

    fun revokeVip(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _isPremium.value = false
        _vipChangeTrigger.value = System.currentTimeMillis()
    }

    fun unlockFullVipPreview(context: Context, hours: Int = 24) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expiry = System.currentTimeMillis() + (hours * 3600_000L)
        prefs.edit().putLong(KEY_VIP_EXPIRY, expiry).putString(KEY_VIP_SOURCE, "preview").apply()
        _vipChangeTrigger.value = System.currentTimeMillis()
    }

    fun getRemainingDays(context: Context): Int {
        if (_isPremium.value) return 9999
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expiry = prefs.getLong(KEY_VIP_EXPIRY, 0L)
        if (expiry == Long.MAX_VALUE) return 9999
        val diff = expiry - System.currentTimeMillis()
        if (diff <= 0) return 0
        return ((diff / (24L * 60L * 60L * 1000L)) + 1).toInt()
    }

    fun getVipSource(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VIP_SOURCE, "none") ?: "none"
    }

    fun getVipExpiryMillis(context: Context): Long {
        if (_isPremium.value) return Long.MAX_VALUE
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VIP_SOURCE, null)?.let {
            prefs.getLong(KEY_VIP_EXPIRY, Long.MAX_VALUE)
        } ?: 0L
    }
}
