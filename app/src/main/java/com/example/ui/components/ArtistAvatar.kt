package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * مدیریت کش خصوصی و دائمی تصاویر پروفایل هنرمندان در حافظه داخلی برنامه (filesDir)
 * - کاملاً خصوصی + فایل .nomedia (هرگز وارد گالری گوشی نمی‌شود)
 * - دارای فیلتر هوشمند تطبیق دقیق نام و بیشترین تعداد طرفدار (nb_fan) تا هرگز عکس افراد متفرقه یا تشابه اسمی نمایش داده نشود.
 */
object ArtistAvatarDiskCache {
    private const val PREFS_NAME = "aura_private_artist_avatars_prefs_v3"
    private const val DIR_NAME = "artist_avatars_private_cache_v3"
    private const val LEGACY_DIR_NAME = "artist_avatars_private_cache"

    private val memoryCache = object : LruCache<String, Bitmap>(160) {}
    private val failedUrls = ConcurrentHashMap.newKeySet<String>()
    private val resolvedVerifiedUrlCache = ConcurrentHashMap<String, String>()
    private val cleanedLegacyCache = AtomicBoolean(false)

    /**
     * بانک تصاویر رسمی و تاییدشده هنرمندان (انتخاب‌شده بر اساس تطبیق دقیق نام و بالاترین تعداد طرفدار واقعی)
     * تا از نمایش عکس اکانت‌های فیک/تشابه اسمی با طرفدار کم (مثل اکانت ۴۰۰ فالووری Lil Baby یا ۱۸ فالووری Arta) جلوگیری شود.
     */
    private val verifiedArtistPictureUrls = mapOf(
        "future" to "https://cdn-images.dzcdn.net/images/artist/1a695ee9c2cb49fab81d408836100ff3/1000x1000-000000-80-0-0.jpg",
        "drake" to "https://cdn-images.dzcdn.net/images/artist/eb0ed5b21d1ea5af021fc074ded0e91f/1000x1000-000000-80-0-0.jpg",
        "lil baby" to "https://cdn-images.dzcdn.net/images/artist/40eb91eecca8f455b0ce06da80509775/1000x1000-000000-80-0-0.jpg",
        "young thug" to "https://cdn-images.dzcdn.net/images/artist/89833703db63ec93f85f7979c2fb37f3/1000x1000-000000-80-0-0.jpg",
        "dababy" to "https://cdn-images.dzcdn.net/images/artist/0570f0dde1e2a1121d8da9b2664a3ad7/1000x1000-000000-80-0-0.jpg",
        "lil wayne" to "https://cdn-images.dzcdn.net/images/artist/3ced6f5ba94b771cf704fec83c58c798/1000x1000-000000-80-0-0.jpg",
        "offset" to "https://cdn-images.dzcdn.net/images/artist/976f6f16492eb34ba3119f5db93839e6/1000x1000-000000-80-0-0.jpg",
        "arta" to "https://cdn-images.dzcdn.net/images/artist/a96ee73d37513d8c59c1134d1a34845d/1000x1000-000000-80-0-0.jpg",
        "tyler, the creator" to "https://cdn-images.dzcdn.net/images/artist/5eceecd683beab6dd901a7931294a121/1000x1000-000000-80-0-0.jpg",
        "juice wrld" to "https://cdn-images.dzcdn.net/images/artist/2ecf2968fbbc8eb042b241b8e60e5d7e/1000x1000-000000-80-0-0.jpg",
        "metro boomin" to "https://cdn-images.dzcdn.net/images/artist/bc031b73f958987fa103031070be5c16/1000x1000-000000-80-0-0.jpg",
        "kanye west" to "https://cdn-images.dzcdn.net/images/artist/bb76c2ee3b068726ab4c37b0aabdb57a/1000x1000-000000-80-0-0.jpg",
        "travis scott" to "https://cdn-images.dzcdn.net/images/artist/8d8316146026d7e6ce377e314536df62/1000x1000-000000-80-0-0.jpg",
        "kendrick lamar" to "https://cdn-images.dzcdn.net/images/artist/be0a7c550567f4af0ed202d7235b74d6/1000x1000-000000-80-0-0.jpg",
        "gunna" to "https://cdn-images.dzcdn.net/images/artist/f6ae30d19f7e865c79b070de2087bd96/1000x1000-000000-80-0-0.jpg",
        "don toliver" to "https://cdn-images.dzcdn.net/images/artist/2670219ffd5035c27aab2295dca242dd/1000x1000-000000-80-0-0.jpg",
        "21 savage" to "https://cdn-images.dzcdn.net/images/artist/76b4cd56c7e94e8d2bdc3e2157e1080f/1000x1000-000000-80-0-0.jpg",
        "post malone" to "https://cdn-images.dzcdn.net/images/artist/a5a8cca44e7eab2db7d44e039bed2574/1000x1000-000000-80-0-0.jpg",
        "jack harlow" to "https://cdn-images.dzcdn.net/images/artist/acf97ceb30d48a97e47afd1c3f9b68ce/1000x1000-000000-80-0-0.jpg",
        "the weeknd" to "https://cdn-images.dzcdn.net/images/artist/581693b4724a7fcfa754455101e13a44/1000x1000-000000-80-0-0.jpg",
        "j. cole" to "https://cdn-images.dzcdn.net/images/artist/dc8d97f19855c8ea3f15ee6db784198e/1000x1000-000000-80-0-0.jpg",
        "nle choppa" to "https://cdn-images.dzcdn.net/images/artist/d71038f6c1c2010102550e5fad6743a4/1000x1000-000000-80-0-0.jpg",
        "latto" to "https://cdn-images.dzcdn.net/images/artist/01a5b9598e1de78a0cfad893c0e4e161/1000x1000-000000-80-0-0.jpg",
        "central cee" to "https://cdn-images.dzcdn.net/images/artist/25fe719f51af3ee2de27aa267e2a6ac9/1000x1000-000000-80-0-0.jpg",
        "dj khaled" to "https://cdn-images.dzcdn.net/images/artist/c9157de87a18015b5a898cf4d0d867d8/1000x1000-000000-80-0-0.jpg",
        "playboi carti" to "https://cdn-images.dzcdn.net/images/artist/b90097972a60d9d8598a79a786be1a3a/1000x1000-000000-80-0-0.jpg",
        "baby keem" to "https://cdn-images.dzcdn.net/images/artist/56a2450351b4a111dc01ed495374da55/1000x1000-000000-80-0-0.jpg",
        "eminem" to "https://cdn-images.dzcdn.net/images/artist/0f30bbd33a680030054af004d698d6ac/1000x1000-000000-80-0-0.jpg",
        "sepehr khalse" to "https://cdn-images.dzcdn.net/images/artist/f52e5e83fa1d8ebe06617c73027eb613/1000x1000-000000-80-0-0.jpg",
        "sijal" to "https://cdn-images.dzcdn.net/images/artist/0e74cd9bb0cf35a888abc43c469cabbe/1000x1000-000000-80-0-0.jpg",
        "behzad leito" to "https://cdn-images.dzcdn.net/images/artist/17372d758331fb0e196a27671f8d95b0/1000x1000-000000-80-0-0.jpg",
        "alireza jj" to "https://cdn-images.dzcdn.net/images/artist/23fb8aa3ca99689975a7c4f9f0e8fe5b/1000x1000-000000-80-0-0.jpg",
        "koorosh" to "https://cdn-images.dzcdn.net/images/artist/53f6b12590429159fa06ba74613620e0/1000x1000-000000-80-0-0.jpg",
        "vinak" to "https://cdn-images.dzcdn.net/images/artist/0c1f0c3e7511eef34135444f4effffd1/1000x1000-000000-80-0-0.jpg",
        "mehrad hidden" to "https://cdn-images.dzcdn.net/images/artist/6ab8f07d706c4c92ca0f2be39c7c357c/1000x1000-000000-80-0-0.jpg",
        "zedbazi" to "https://cdn-images.dzcdn.net/images/artist/26c60088921fba1d38cbd999b39f1250/1000x1000-000000-80-0-0.jpg",
        "hiphopologist" to "https://cdn-images.dzcdn.net/images/artist/50536ee109bda430d91dab5944c91c9d/1000x1000-000000-80-0-0.jpg",
        "big shaggy" to "https://cdn-images.dzcdn.net/images/artist/52a3dee255927c909e68760d2e78db84/1000x1000-000000-80-0-0.jpg",
        "amin tijay" to "https://cdn-images.dzcdn.net/images/artist/59c98807bedcff2e40eac0e82012e0df/1000x1000-000000-80-0-0.jpg",
        "papaboyz" to "https://cdn-images.dzcdn.net/images/artist/8a9faa05af0858d70a2f9b388b16a312/1000x1000-000000-80-0-0.jpg",
        "hesam tiem" to "https://cdn-images.dzcdn.net/images/artist/e5346065cd7bd5446fd5d96a245dffcb/1000x1000-000000-80-0-0.jpg",
        "hesamtiem" to "https://cdn-images.dzcdn.net/images/artist/e5346065cd7bd5446fd5d96a245dffcb/1000x1000-000000-80-0-0.jpg",
        "chvrsi" to "https://cdn-images.dzcdn.net/images/artist/5ade98299d76add77f6023a96e4c6154/1000x1000-000000-80-0-0.jpg",
        "tohi" to "https://cdn-images.dzcdn.net/images/artist/c3117334e4368e498ae1b396b38e2077/1000x1000-000000-80-0-0.jpg",
        "gucciflame" to "https://cdn-images.dzcdn.net/images/artist/4e4fdc174259d722b1396ee5f982ffdb/1000x1000-000000-80-0-0.jpg",
        "sohrab mj" to "https://cdn-images.dzcdn.net/images/artist/a531ffd3adb5a1c232023eeaa64d00c3/1000x1000-000000-80-0-0.jpg",
        "reza pishro" to "https://cdn-images.dzcdn.net/images/artist/2148d661b08c86718bed613e9f90e9cb/1000x1000-000000-80-0-0.jpg",
        "pouriya adroit" to "https://cdn-images.dzcdn.net/images/artist/f8d83a33ac19b005d9a73075cc2f3ec4/1000x1000-000000-80-0-0.jpg",
        "hoodadk4" to "https://cdn-images.dzcdn.net/images/artist/d678923cfb26b18d21259eda2f71cd61/1000x1000-000000-80-0-0.jpg",
        "021kid" to "https://cdn-images.dzcdn.net/images/artist/ff6bde7193fb3f2a0e32283ad5309c09/1000x1000-000000-80-0-0.jpg",
        "shayan yo" to "https://cdn-images.dzcdn.net/images/artist/7ccaf0b6329829ff7b2bbdde8f07b406/1000x1000-000000-80-0-0.jpg",
        "hoomaan" to "https://cdn-images.dzcdn.net/images/artist/8100c99802f10f8e4bde897b1dc6c907/1000x1000-000000-80-0-0.jpg",
        "sami low" to "https://cdn-images.dzcdn.net/images/artist/92cf6819d802bdff5c94107a8262fe7e/1000x1000-000000-80-0-0.jpg",
        "ali geramy" to "https://cdn-images.dzcdn.net/images/artist/3579fe28715d9635dffdf69efe05565b/1000x1000-000000-80-0-0.jpg",
        "catchybeatz" to "https://cdn-images.dzcdn.net/images/artist/37a8588f6f3bea377be3b4d71cbbb590/1000x1000-000000-80-0-0.jpg",
        "dorcci" to "https://cdn-images.dzcdn.net/images/artist/9b3e120aca9dc4487bceba6b8c8aa01e/1000x1000-000000-80-0-0.jpg",
        "gdaal" to "https://cdn-images.dzcdn.net/images/artist/69a35f3a05b71e5d2cfed1a4320e4f40/1000x1000-000000-80-0-0.jpg",
        "saman wilson" to "https://cdn-images.dzcdn.net/images/artist/59fe0272e9c5dff45704b092172d6dc8/1000x1000-000000-80-0-0.jpg",
        "sajad shahi" to "https://cdn-images.dzcdn.net/images/artist/a8b4b65746862c8f3d9df120908b9c1f/1000x1000-000000-80-0-0.jpg",
        "amir tataloo" to "https://cdn-images.dzcdn.net/images/artist/8a3981d4fcaf94d338845b584412aa5b/1000x1000-000000-80-0-0.jpg",
        "putak" to "https://cdn-images.dzcdn.net/images/artist/d6fbe7036c268eb162da702f90fa49a4/1000x1000-000000-80-0-0.jpg",
        "neemski" to "https://cdn-images.dzcdn.net/images/artist/3a288d374dc0335022e95b06fe92243c/1000x1000-000000-80-0-0.jpg",
        "ashkan kagan" to "https://cdn-images.dzcdn.net/images/artist/75ae560d7f6a410bf437ee5c92a5c909/1000x1000-000000-80-0-0.jpg",
        "erfan" to "https://cdn-images.dzcdn.net/images/artist/8a09b84e7627c5d24934c31f4f22dc2e/1000x1000-000000-80-0-0.jpg",
        "poori" to "https://cdn-images.dzcdn.net/images/artist/2846fde5978591240c4eecaead6fd3d2/1000x1000-000000-80-0-0.jpg",
        "poobon" to "https://cdn-images.dzcdn.net/images/artist/a8e0d6f7f525f4365f8749b9ae2a6a08/1000x1000-000000-80-0-0.jpg",
        "sogand" to "https://cdn-images.dzcdn.net/images/artist/efa98620da7a46be8ed149f4f31d6937/1000x1000-000000-80-0-0.jpg",
        "tm bax" to "https://cdn-images.dzcdn.net/images/artist/74a3d4fc2b3541c0f8ce9847f737af4e/1000x1000-000000-80-0-0.jpg",
        "ali owj" to "https://cdn-images.dzcdn.net/images/artist/d0917f285f132ffa0f770bb3abc92575/1000x1000-000000-80-0-0.jpg",
        "shayea" to "https://cdn-images.dzcdn.net/images/artist/a9033a3733db108c8bb56d740a4ce3de/1000x1000-000000-80-0-0.jpg",
        "ho3ein" to "https://cdn-images.dzcdn.net/images/artist/710e29b116592877da1c6861b82cd007/1000x1000-000000-80-0-0.jpg",
        "yas" to "https://cdn-images.dzcdn.net/images/artist/9ffa5d619d1342d6543e5f015bf9c60b/1000x1000-000000-80-0-0.jpg",
        "hichkas" to "https://cdn-images.dzcdn.net/images/artist/a91141c602b82709f8c6bc53ad9bff5a/1000x1000-000000-80-0-0.jpg",
        "bahram" to "https://cdn-images.dzcdn.net/images/artist/845b2f7aa48c38567bf76aeee2584e65/1000x1000-000000-80-0-0.jpg",
        "ali sorena" to "https://cdn-images.dzcdn.net/images/artist/6ed2f32d98df4e7e9d819b0b51b989f0/1000x1000-000000-80-0-0.jpg",
        "fadaei" to "https://cdn-images.dzcdn.net/images/artist/f513bbf0f78f7b0c01c1c19cea34dfbe/1000x1000-000000-80-0-0.jpg"
    )

    /**
     * نام‌های کوتاه/مشترک که در Deezer فقط خواننده خارجی همنام دارند (مثل nassím فرانسوی، گروه راک C.I.A.، Dalú مکزیکی، Tamara اسپانیایی و ...)
     * برای این موارد هرگز عکس فرد غریبه را لود نمی‌کنیم تا موشن‌گرافی نام خواننده نمایش داده شود.
     */
    private val ambiguousForeignNamesakes = setOf(
        "nassim",
        "cia",
        "tiem",
        "tamara",
        "dalu",
        "isam",
        "riri",
        "outlaw",
        "the don",
        "raha",
        "daniyal",
        "tyler",
        "the creator",
        "mahyar",
        "tahas",
        "masin",
        "ahood",
        "khojaste"
    )

    private fun normalizeArtistKey(artistName: String): String {
        return artistName.trim().lowercase()
            .replace(Regex("[^a-z0-9\\u0600-\\u06FF_-]"), "_")
            .take(64)
            .ifBlank { "unknown_artist" }
    }

    private fun normalizeStrictCompareName(name: String): String {
        return name.trim().lowercase()
            .replace(".", "")
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")
    }

    private fun purgeLegacyCacheOnce(context: Context) {
        if (cleanedLegacyCache.compareAndSet(false, true)) {
            try {
                val oldDir = File(context.filesDir, LEGACY_DIR_NAME)
                if (oldDir.exists()) {
                    oldDir.deleteRecursively()
                }
            } catch (_: Exception) {}
        }
    }

    private fun getPrivateCacheDir(context: Context): File {
        purgeLegacyCacheOnce(context)
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) {
            try {
                noMedia.createNewFile()
            } catch (_: Exception) {}
        }
        return dir
    }

    fun getPrivateAvatarFile(context: Context, artistName: String): File {
        val key = normalizeArtistKey(artistName)
        return File(getPrivateCacheDir(context), "avatar_${key}.jpg")
    }

    fun getMemoryCachedBitmap(artistName: String): Bitmap? {
        val cleanLower = artistName.trim().lowercase()
        if (ambiguousForeignNamesakes.contains(cleanLower)) return null
        val key = normalizeArtistKey(artistName)
        return memoryCache.get(key)
    }

    fun getSavedArtistUrl(context: Context, artistName: String): String {
        val cleanLower = artistName.trim().lowercase()
        if (ambiguousForeignNamesakes.contains(cleanLower)) return ""
        verifiedArtistPictureUrls[cleanLower]?.let { return it }
        val key = normalizeArtistKey(artistName)
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString("url_$key", "").orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    fun saveArtistUrl(context: Context, artistName: String, url: String) {
        val cleanLower = artistName.trim().lowercase()
        if (cleanLower.isBlank() || url.isBlank() || ambiguousForeignNamesakes.contains(cleanLower)) return
        val finalUrl = verifiedArtistPictureUrls[cleanLower] ?: url
        val key = normalizeArtistKey(artistName)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("url_$key", finalUrl).apply()
        } catch (_: Exception) {}
    }

    /**
     * جستجوی هوشمند عکس واقعی خواننده با فیلتر تطبیق دقیق نام و بیشترین تعداد طرفدار (nb_fan)
     * تا هرگز عکس افراد متفرقه با نام مشابه انتخاب نشود.
     */
    private fun resolveBestVerifiedArtistUrl(artistName: String, fallbackWorkerUrl: String): String {
        val cleanLower = artistName.trim().lowercase()
        if (cleanLower.isBlank() || ambiguousForeignNamesakes.contains(cleanLower)) {
            return ""
        }

        verifiedArtistPictureUrls[cleanLower]?.let { return it }
        resolvedVerifiedUrlCache[cleanLower]?.let { return it }

        val targetStrict = normalizeStrictCompareName(artistName)
        try {
            val encoded = URLEncoder.encode(artistName.trim(), "UTF-8")
            val conn = (URL("https://api.deezer.com/search/artist?q=$encoded&limit=8").openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val dataArr = JSONObject(body).optJSONArray("data")
                if (dataArr != null && dataArr.length() > 0) {
                    var bestPic = ""
                    var bestFans = -1
                    for (i in 0 until dataArr.length()) {
                        val item = dataArr.optJSONObject(i) ?: continue
                        val candName = item.optString("name", "").trim()
                        if (normalizeStrictCompareName(candName) != targetStrict) continue
                        val pic = item.optString("picture_xl", "")
                            .ifBlank { item.optString("picture_big", "") }
                            .ifBlank { item.optString("picture_medium", "") }
                        if (pic.isBlank() || pic.contains("/images/artist//")) continue
                        val fans = item.optInt("nb_fan", 0)
                        if (fans > bestFans) {
                            bestFans = fans
                            bestPic = pic
                        }
                    }
                    // فقط اگر تطبیق دقیق نام وجود داشت و پروفایل معتبر بود
                    if (bestPic.isNotBlank() && bestFans >= 50) {
                        resolvedVerifiedUrlCache[cleanLower] = bestPic
                        return bestPic
                    }
                }
            }
        } catch (_: Exception) {}

        // اگر URL ورکر از نوع artist-cover است اما در Deezer پروفایل معتبری با نام دقیق پیدا نشد،
        // از نمایش عکس اشتباه جلوگیری کن
        if (fallbackWorkerUrl.contains("/artist-cover")) {
            return ""
        }
        return fallbackWorkerUrl
    }

    suspend fun loadOrDownloadArtistBitmap(
        context: Context,
        artistName: String,
        remoteUrl: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cleanLower = artistName.trim().lowercase()
        if (cleanLower.isBlank() || ambiguousForeignNamesakes.contains(cleanLower)) {
            return@withContext null
        }

        val key = normalizeArtistKey(artistName)
        memoryCache.get(key)?.let { return@withContext it }

        // ۱. بررسی فایل ذخیره‌شده در حافظه خصوصی داخلی برنامه (بدون نیاز به اینترنت و بدون دیده‌شدن در گالری)
        val localFile = getPrivateAvatarFile(context, artistName)
        if (localFile.exists() && localFile.length() > 512L) {
            try {
                val diskBitmap = BitmapFactory.decodeFile(localFile.absolutePath)
                if (diskBitmap != null) {
                    memoryCache.put(key, diskBitmap)
                    return@withContext diskBitmap
                }
            } catch (_: Exception) {}
        }

        // ۲. تعیین لینک دقیق و تاییدشده عکس خواننده
        val rawCandidate = verifiedArtistPictureUrls[cleanLower]
            ?: remoteUrl.ifBlank { getSavedArtistUrl(context, artistName) }
        val effectiveUrl = resolveBestVerifiedArtistUrl(artistName, rawCandidate)

        if (effectiveUrl.isBlank() || failedUrls.contains(effectiveUrl)) {
            return@withContext null
        }

        saveArtistUrl(context, artistName, effectiveUrl)

        if (effectiveUrl.startsWith("/") || effectiveUrl.startsWith("file://")) {
            val path = effectiveUrl.removePrefix("file://")
            val f = File(path)
            if (f.exists() && f.length() > 512L) {
                val bmp = try {
                    BitmapFactory.decodeFile(f.absolutePath)
                } catch (_: Exception) {
                    null
                }
                if (bmp != null) {
                    memoryCache.put(key, bmp)
                    try {
                        FileOutputStream(localFile).use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                    } catch (_: Exception) {}
                    return@withContext bmp
                }
            }
            return@withContext null
        }

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(effectiveUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 12000
                readTimeout = 12000
                doInput = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            conn.connect()
            if (conn.responseCode in 200..299) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.size > 512) {
                    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (decoded != null) {
                        memoryCache.put(key, decoded)
                        try {
                            val tempFile = File(localFile.parentFile, "${localFile.name}.tmp")
                            FileOutputStream(tempFile).use { out ->
                                decoded.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            if (tempFile.exists() && tempFile.length() > 512L) {
                                if (localFile.exists()) localFile.delete()
                                tempFile.renameTo(localFile)
                            } else {
                                tempFile.delete()
                            }
                        } catch (_: Exception) {}
                        return@withContext decoded
                    }
                }
            } else if (conn.responseCode in 400..499) {
                failedUrls.add(effectiveUrl)
            }
        } catch (_: Exception) {
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {}
        }

        null
    }
}

/**
 * پروفایل خواننده به سبک Spotify & Apple Music:
 * - ابتدا بررسی می‌کند آیا عکس تاییدشده خواننده قبلاً در حافظه داخلی خصوصی برنامه (`filesDir`) ذخیره شده است یا خیر.
 * - اگر ذخیره شده باشد، بلافاصله و بدون دانلود مجدد نمایش می‌دهد.
 * - اگر جدید باشد، عکس تاییدشده خواننده را دانلود کرده و برای دفعات بعدی در فضای خصوصی ذخیره می‌کند (وارد گالری گوشی نمی‌شود).
 * - در صورتی که عکس موجود نباشد یا لود نشود (`onError`)، موشن‌گرافیِ نام خواننده را به عنوان جایگزین نشان می‌دهد.
 */
@Composable
fun SpotifyArtistAvatar(
    artistName: String,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    shape: Shape = CircleShape,
    showVerifiedBadge: Boolean = true,
    explicitArtistImageUrl: String? = null,
    cloudflareWorkerBaseUrl: String? = null
) {
    val context = LocalContext.current
    val cleanName = remember(artistName) {
        artistName.trim()
            .removePrefix("@")
            .ifBlank { "Artist" }
    }

    val resolvedImageUrl = remember(explicitArtistImageUrl, cloudflareWorkerBaseUrl, cleanName) {
        val raw = explicitArtistImageUrl?.trim().orEmpty().ifBlank {
            ArtistAvatarDiskCache.getSavedArtistUrl(context, cleanName)
        }
        if (raw.isBlank()) {
            ""
        } else if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("file://") || raw.startsWith("content://") || raw.startsWith("/storage/") || raw.startsWith("/data/")) {
            raw
        } else if (!cloudflareWorkerBaseUrl.isNullOrBlank()) {
            val base = cloudflareWorkerBaseUrl.trimEnd('/')
            if (raw.startsWith("/")) "$base$raw" else "$base/$raw"
        } else {
            val base = "https://alubm.parsaghorbani8861.workers.dev"
            if (raw.startsWith("/")) "$base$raw" else "$base/$raw"
        }
    }

    val hash = remember(cleanName) { abs(cleanName.hashCode()) }

    val luxuryPalettes = remember {
        listOf(
            listOf(Color(0xFF071A12), Color(0xFF0F4C3A), Color(0xFF1DB954)), // Spotify Emerald
            listOf(Color(0xFF160829), Color(0xFF3F1670), Color(0xFF9D4EDD)), // Apple Music Violet
            listOf(Color(0xFF240714), Color(0xFF6E1133), Color(0xFFFF2A85)), // Neon Crimson
            listOf(Color(0xFF06162E), Color(0xFF0E3A6F), Color(0xFF00D2FF)), // Cyber Ocean
            listOf(Color(0xFF211103), Color(0xFF6B3407), Color(0xFFFF9F1C)), // Sunset Gold
            listOf(Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFFE0E1DD)), // Platinum Dark
            listOf(Color(0xFF1A0B12), Color(0xFF591C33), Color(0xFFF72585)), // Magenta Pulse
            listOf(Color(0xFF081C15), Color(0xFF2D6A4F), Color(0xFF74C69D))  // Mint Aurora
        )
    }

    val palette = luxuryPalettes[hash % luxuryPalettes.size]
    val accentColor = palette.last()

    val fontSize = when {
        size <= 50.dp -> if (cleanName.length > 12) 9.sp else 11.sp
        size <= 80.dp -> if (cleanName.length > 14) 11.sp else 13.sp
        size <= 110.dp -> if (cleanName.length > 14) 13.sp else 15.sp
        else -> if (cleanName.length > 14) 18.sp else 22.sp
    }

    var cachedBitmap by remember(cleanName, resolvedImageUrl) {
        mutableStateOf(ArtistAvatarDiskCache.getMemoryCachedBitmap(cleanName))
    }

    LaunchedEffect(cleanName, resolvedImageUrl) {
        if (cachedBitmap == null) {
            cachedBitmap = ArtistAvatarDiskCache.loadOrDownloadArtistBitmap(
                context = context,
                artistName = cleanName,
                remoteUrl = resolvedImageUrl
            )
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    brush = Brush.linearGradient(
                        colors = palette,
                        start = Offset(0f, 0f),
                        end = Offset(400f, 400f)
                    )
                )
                .border(
                    width = 1.8.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            accentColor.copy(alpha = 0.9f),
                            Color.White.copy(alpha = 0.2f),
                            accentColor.copy(alpha = 0.6f),
                            Color.White.copy(alpha = 0.3f),
                            accentColor.copy(alpha = 0.9f)
                        )
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            val bmp = cachedBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = cleanName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                )
            } else {
                // در صورتی که عکس موجود نباشد یا لود نشود (onError)، موشن‌گرافی نام خواننده نمایش داده می‌شود
                ArtistMotionTypographyFallback(
                    cleanName = cleanName,
                    accentColor = accentColor,
                    size = size,
                    fontSize = fontSize
                )
            }
        }

        // تیک تایید هنرمند (مشابه Verified Artist اسپاتیفای)
        if (showVerifiedBadge && size >= 72.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .size(if (size >= 110.dp) 26.dp else 20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF090B10))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified Artist",
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ArtistMotionTypographyFallback(
    cleanName: String,
    accentColor: Color,
    size: Dp,
    fontSize: TextUnit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArtistAvatarMotion")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.62f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(ringRotation)
        ) {
            val radius = this.size.minDimension / 2f
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = radius * 0.82f,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.18f),
                radius = radius * 0.64f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.26f),
                        Color.Transparent
                    )
                ),
                radius = radius * pulseScale
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (size >= 76.dp) {
                Text(
                    text = "ARTIST",
                    color = accentColor.copy(alpha = 0.85f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = cleanName,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = (fontSize.value + 3).sp,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
