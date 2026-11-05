package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
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
 * - ۱۰۰٪ از طریق سرور کلادفلر بدون نیاز به فیلترشکن
 * - کاملاً خصوصی + فایل .nomedia (هرگز وارد گالری گوشی نمی‌شود)
 * - فشرده‌سازی و Downsample هوشمند برای جلوگیری از مصرف رم، لگ و کرش
 */
object ArtistAvatarDiskCache {
    private const val WORKER_BASE = "https://alubm.parsaghorbani8861.workers.dev"
    private const val PREFS_NAME = "aura_private_artist_avatars_prefs_v4"
    private const val DIR_NAME = "artist_avatars_private_cache_v4"
    private val LEGACY_DIRS = listOf("artist_avatars_private_cache", "artist_avatars_private_cache_v3")

    private val memoryCache = object : LruCache<String, Bitmap>(180) {}
    private val failedUrls = ConcurrentHashMap.newKeySet<String>()
    private val inFlightLocks = ConcurrentHashMap<String, Any>()
    private val ioSemaphore = Semaphore(4)
    private val cleanedLegacyCache = AtomicBoolean(false)

    /**
     * کوئری‌های بهینه‌شده سرور کلادفلر برای خواننده‌هایی که در جستجوی پیش‌فرض تشابه اسمی داشتند
     * تمامی لینک‌ها مستقیماً از سرور کلادفلر کاربر لود می‌شوند و بدون فیلترشکن کار می‌کنند.
     */
    private val disambiguatedWorkerArtistUrls = mapOf(
        "lil baby" to "$WORKER_BASE/artist-cover?name=Lil-Baby",
        "young thug" to "$WORKER_BASE/artist-cover?name=Young-Thug",
        "lil wayne" to "$WORKER_BASE/artist-cover?name=Lil-Wayne",
        "dababy" to "$WORKER_BASE/artist-cover?name=%22DaBaby%22",
        "offset" to "$WORKER_BASE/artist-cover?name=%22Offset%22",
        "hesam tiem" to "$WORKER_BASE/artist-cover?name=HesamTiem",
        "hesamtiem" to "$WORKER_BASE/artist-cover?name=HesamTiem",
        "tyler, the creator" to "$WORKER_BASE/artist-cover?name=Tyler,%20The%20Creator",
        "arta" to "$WORKER_BASE/cover?id=AgADjiEAAtqGoVM"
    )

    /**
     * نام‌های کوتاه یا مشترک که در سرویس‌های جهانی فقط خواننده خارجی همنام دارند.
     * برای این موارد مستقیماً طرح گرافیکی نام خواننده نمایش داده می‌شود تا عکس فرد غریبه نیاید.
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

    private fun purgeLegacyCacheOnce(context: Context) {
        if (cleanedLegacyCache.compareAndSet(false, true)) {
            try {
                for (oldName in LEGACY_DIRS) {
                    val oldDir = File(context.filesDir, oldName)
                    if (oldDir.exists()) {
                        oldDir.deleteRecursively()
                    }
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

    fun resolveCloudflareArtistUrl(artistName: String, candidateUrl: String = ""): String {
        val cleanLower = artistName.trim().lowercase()
        if (cleanLower.isBlank() || ambiguousForeignNamesakes.contains(cleanLower)) return ""

        disambiguatedWorkerArtistUrls[cleanLower]?.let { return it }

        val trimmed = candidateUrl.trim()
        if (trimmed.isNotBlank()) {
            // اگر لینک مستقیم به deezer/dzcdn بود (که بدون فیلترشکن باز نمی‌شود)، آن را به لینک کلادفلر تبدیل کن
            if (trimmed.contains("dzcdn.net") || trimmed.contains("deezer.com")) {
                val encoded = try { URLEncoder.encode(artistName.trim(), "UTF-8") } catch (_: Exception) { artistName.trim() }
                return "$WORKER_BASE/artist-cover?name=$encoded"
            }
            return when {
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                trimmed.startsWith("file://") || trimmed.startsWith("content://") || trimmed.startsWith("/data/") || trimmed.startsWith("/storage/") -> trimmed
                trimmed.startsWith("/") -> "$WORKER_BASE$trimmed"
                else -> "$WORKER_BASE/$trimmed"
            }
        }

        val encoded = try { URLEncoder.encode(artistName.trim(), "UTF-8") } catch (_: Exception) { artistName.trim() }
        return "$WORKER_BASE/artist-cover?name=$encoded"
    }

    fun getSavedArtistUrl(context: Context, artistName: String): String {
        val cleanLower = artistName.trim().lowercase()
        if (ambiguousForeignNamesakes.contains(cleanLower)) return ""
        disambiguatedWorkerArtistUrls[cleanLower]?.let { return it }
        val key = normalizeArtistKey(artistName)
        val saved = try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString("url_$key", "").orEmpty()
        } catch (_: Exception) {
            ""
        }
        return resolveCloudflareArtistUrl(artistName, saved)
    }

    fun saveArtistUrl(context: Context, artistName: String, url: String) {
        val cleanLower = artistName.trim().lowercase()
        if (cleanLower.isBlank() || url.isBlank() || ambiguousForeignNamesakes.contains(cleanLower)) return
        val finalUrl = resolveCloudflareArtistUrl(artistName, url)
        if (finalUrl.isBlank()) return
        val key = normalizeArtistKey(artistName)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("url_$key", finalUrl).apply()
        } catch (_: Exception) {}
    }

    private fun decodeDownsampledBitmap(bytes: ByteArray, maxDim: Int = 280): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= maxDim && bounds.outHeight / (sampleSize * 2) >= maxDim) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeDownsampledFile(file: File, maxDim: Int = 280): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= maxDim && bounds.outHeight / (sampleSize * 2) >= maxDim) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (_: Throwable) {
            null
        }
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

        ioSemaphore.withPermit {
            memoryCache.get(key)?.let { return@withPermit it }

            // ۱. بررسی فایل ذخیره‌شده در حافظه خصوصی داخلی برنامه (بدون نیاز به اینترنت و بدون دیده‌شدن در گالری)
            val localFile = getPrivateAvatarFile(context, artistName)
            if (localFile.exists() && localFile.length() > 512L) {
                val diskBitmap = decodeDownsampledFile(localFile)
                if (diskBitmap != null) {
                    memoryCache.put(key, diskBitmap)
                    return@withPermit diskBitmap
                }
            }

            // ۲. تعیین لینک کلادفلر خواننده (بدون نیاز به فیلترشکن)
            val effectiveUrl = resolveCloudflareArtistUrl(artistName, remoteUrl)
            if (effectiveUrl.isBlank() || failedUrls.contains(effectiveUrl)) {
                return@withPermit null
            }

            val lock = inFlightLocks.getOrPut(key) { Any() }
            synchronized(lock) {
                memoryCache.get(key)?.let { return@withPermit it }
                if (localFile.exists() && localFile.length() > 512L) {
                    decodeDownsampledFile(localFile)?.let { bmp ->
                        memoryCache.put(key, bmp)
                        return@withPermit bmp
                    }
                }

                if (effectiveUrl.startsWith("/") || effectiveUrl.startsWith("file://")) {
                    val path = effectiveUrl.removePrefix("file://")
                    val f = File(path)
                    if (f.exists() && f.length() > 512L) {
                        val bmp = decodeDownsampledFile(f)
                        if (bmp != null) {
                            memoryCache.put(key, bmp)
                            try {
                                FileOutputStream(localFile).use { out ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
                                }
                            } catch (_: Exception) {}
                            return@withPermit bmp
                        }
                    }
                    return@withPermit null
                }

                var conn: HttpURLConnection? = null
                try {
                    conn = (URL(effectiveUrl).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 8000
                        readTimeout = 8000
                        doInput = true
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                    }
                    conn.connect()
                    val code = conn.responseCode
                    if (code in 200..299) {
                        val bytes = conn.inputStream.use { it.readBytes() }
                        if (bytes.size > 512) {
                            val decoded = decodeDownsampledBitmap(bytes)
                            if (decoded != null) {
                                memoryCache.put(key, decoded)
                                saveArtistUrl(context, artistName, effectiveUrl)
                                try {
                                    val tempFile = File(localFile.parentFile, "${localFile.name}.tmp")
                                    FileOutputStream(tempFile).use { out ->
                                        decoded.compress(Bitmap.CompressFormat.JPEG, 88, out)
                                    }
                                    if (tempFile.exists() && tempFile.length() > 512L) {
                                        if (localFile.exists()) localFile.delete()
                                        tempFile.renameTo(localFile)
                                    } else {
                                        tempFile.delete()
                                    }
                                } catch (_: Exception) {}
                                return@withPermit decoded
                            }
                        }
                    } else if (code in 400..499) {
                        failedUrls.add(effectiveUrl)
                    }
                } catch (_: Throwable) {
                } finally {
                    try {
                        conn?.disconnect()
                    } catch (_: Exception) {}
                }
            }
            null
        }
    }
}

/**
 * پروفایل خواننده به سبک Spotify & Apple Music:
 * - ابتدا بررسی می‌کند آیا عکس خواننده قبلاً در حافظه داخلی خصوصی برنامه (`filesDir`) ذخیره شده است یا خیر.
 * - اگر ذخیره شده باشد، بلافاصله و بدون دانلود مجدد نمایش می‌دهد.
 * - اگر جدید باشد، از سرور کلادفلر (بدون نیاز به فیلترشکن) دانلود کرده و برای دفعات بعدی در فضای خصوصی ذخیره می‌کند.
 * - در صورتی که عکس موجود نباشد یا لود نشود، طرح گرافیکی نام خواننده را به عنوان جایگزین نشان می‌دهد.
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
        ArtistAvatarDiskCache.resolveCloudflareArtistUrl(
            artistName = cleanName,
            candidateUrl = explicitArtistImageUrl?.trim().orEmpty()
        )
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
        if (cachedBitmap == null && resolvedImageUrl.isNotBlank()) {
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
                        end = Offset(300f, 300f)
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = accentColor.copy(alpha = 0.55f),
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
                ArtistMotionTypographyFallback(
                    cleanName = cleanName,
                    accentColor = accentColor,
                    size = size,
                    fontSize = fontSize
                )
            }
        }

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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = radius * 0.82f,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.2f),
                radius = radius * 0.64f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.24f),
                        Color.Transparent
                    )
                ),
                radius = radius * 0.72f
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
