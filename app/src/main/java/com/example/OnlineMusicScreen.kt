package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.LruCache
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.Track
import com.example.ui.theme.GlassThickness
import com.example.ui.theme.liquidGlass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val durationSec: Int,
    val previewUrl: String = "",
    val artistId: String = "",
    val artistPictureUrl: String = "",
    val rank: Int = 500_000
) {
    val ratingScoreText: String
        get() {
            val normalized = (8.2f + (rank.coerceIn(100_000, 999_999) - 100_000) / 520_000f).coerceIn(8.2f, 9.9f)
            return "%.1f".format(java.util.Locale.US, normalized)
        }

    fun toAppTrack(streamUrl: String): Track {
        if (previewUrl.isNotBlank()) {
            OnlineMusicRepository.registerPreviewUrl(id, previewUrl)
        }
        if (artistPictureUrl.isNotBlank()) {
            OnlineMusicRepository.registerArtistPicture(artist, artistPictureUrl)
        }
        return Track(
            id = "online_$id",
            title = title,
            artist = artist,
            album = album.ifBlank { "Single" },
            durationMs = (durationSec.coerceAtLeast(30) * 1000L),
            uri = streamUrl,
            artworkUri = coverUrl,
            bitrate = 320,
            sampleRate = 48000,
            genre = "Online"
        )
    }
}

data class OnlineArtistProfile(
    val name: String,
    val roleLabelFa: String,
    val roleLabelEn: String,
    val defaultQuery: String = name,
    val pictureUrl: String = ""
)

data class OnlineSmartPlaylist(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val subtitleEn: String,
    val query: String,
    val seedArtists: List<String>,
    val coverUrls: List<String>,
    val preloadedTracks: List<OnlineTrack> = emptyList(),
    val accentColor: Color = Color(0xFF1ED760)
)

object OnlineMusicRepository {
    // آدرس سرور اختصاصی Cloudflare Workers کاربر برای عبور از تحریم بدون فیلترشکن
    const val WORKER_URL = "https://alubm.parsaghorbani8861.workers.dev"

    private val imageMemoryCache = object : LruCache<String, Bitmap>(80) {}
    private val catalogCache = object : LruCache<String, List<OnlineTrack>>(50) {}
    private val previewUrlByTrackId = ConcurrentHashMap<String, String>()
    private val fullStreamUrlCache = ConcurrentHashMap<String, String>()
    private val artistPictureCache = ConcurrentHashMap<String, String>()

    val famousGlobalArtists: List<OnlineArtistProfile> = listOf(
        OnlineArtistProfile("Travis Scott", "رپر و تهیه‌کننده جهانی", "Global Rap Icon"),
        OnlineArtistProfile("The Weeknd", "پاپ و آراندبی مدرن", "R&B & Pop Superstar"),
        OnlineArtistProfile("Playboi Carti", "ترند اول نسل زد", "Gen-Z Rage Pioneer"),
        OnlineArtistProfile("Drake", "پرافتخارترین هیت‌میکر", "Global Hitmaker"),
        OnlineArtistProfile("Billie Eilish", "پاپ آلترناتیو", "Alt-Pop Phenomenon"),
        OnlineArtistProfile("Kendrick Lamar", "اسطوره هیپ‌هاپ", "Hip-Hop Lyricist"),
        OnlineArtistProfile("Future", "پادشاه ترپ آتلانتا", "Trap Royalty"),
        OnlineArtistProfile("Eminem", "افسانه رپ جهان", "Rap God"),
        OnlineArtistProfile("Central Cee", "ستاره دریل بریتانیا", "UK Drill Star"),
        OnlineArtistProfile("Don Toliver", "ملودیک ترپ و شبانه", "Melodic Trap"),
        OnlineArtistProfile("Metro Boomin", "معمار بیت‌های جهانی", "Master Producer"),
        OnlineArtistProfile("Yeat", "وایب تیک‌تاک و ریج", "Futuristic Rage"),
        OnlineArtistProfile("SZA", "ملکه آراندبی", "Modern R&B Queen"),
        OnlineArtistProfile("21 Savage", "رپ خیابانی و دارک", "Multi-Platinum Rapper"),
        OnlineArtistProfile("Lana Del Rey", "فرکانس رویایی و سینمایی", "Cinematic Dream Pop"),
        OnlineArtistProfile("Hiphopologist", "نسل چهار رپ فارسی", "Persian Rap Star"),
        OnlineArtistProfile("Chvrsi", "ترپ و دریل فارسی", "Persian Trap"),
        OnlineArtistProfile("Shayea", "رپ فارسی", "Persian Hip-Hop")
    )

    fun registerPreviewUrl(trackId: String, previewUrl: String) {
        if (previewUrl.isBlank()) return
        val rawId = trackId.removePrefix("online_")
        previewUrlByTrackId[rawId] = previewUrl
        previewUrlByTrackId["online_$rawId"] = previewUrl
    }

    fun getPreviewUrl(trackId: String): String? {
        val rawId = trackId.removePrefix("online_")
        return previewUrlByTrackId[rawId] ?: previewUrlByTrackId["online_$rawId"]
    }

    fun registerArtistPicture(artistName: String, pictureUrl: String) {
        if (artistName.isBlank() || pictureUrl.isBlank()) return
        artistPictureCache[artistName.trim().lowercase()] = pictureUrl
    }

    fun getArtistPicture(artistName: String): String? {
        return artistPictureCache[artistName.trim().lowercase()]
    }

    fun getCachedCoverBitmap(url: String): Bitmap? {
        if (url.isBlank()) return null
        return imageMemoryCache.get(url)
    }

    fun cacheCoverBitmap(url: String, bitmap: Bitmap) {
        if (url.isBlank()) return
        imageMemoryCache.put(url, bitmap)
    }

    fun getFallbackStreamUrl(track: Track): String? {
        val cacheKey = "${track.artist.lowercase().trim()}|${track.title.lowercase().trim()}"
        val cachedFull = fullStreamUrlCache[cacheKey]
        if (!cachedFull.isNullOrBlank() && track.uri != cachedFull) {
            return cachedFull
        }
        val cleanId = track.id.removePrefix("online_")
        val workerUrl = buildStreamUrlForTitleArtist(track.artist, track.title, cleanId)
        if (track.uri != workerUrl && track.uri.startsWith("http")) {
            return workerUrl
        }
        return null
    }

    suspend fun resolvePreviewUrlIfNeeded(track: Track): String? = withContext(Dispatchers.IO) {
        // هرگز از لینک ۳۰ ثانیه‌ای دیزر استفاده نمی‌شود؛ همیشه لینک کامل استریم برگردانده می‌شود
        resolveFullLengthAudioUrl(track.artist, track.title)
    }

    /**
     * هدایت تمام درخواست‌های جستجو به سمت پروکسی Cloudflare کاربر ($WORKER_URL/search?q=)
     * تا بدون فیلترشکن روی تمام اینترنت‌ها (ایرانسل، همراه اول، وای‌فای) کار کند.
     */
    suspend fun searchCatalog(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val cleanQuery = query.trim()
        catalogCache.get(cleanQuery.lowercase())?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        val results = mutableListOf<OnlineTrack>()
        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            // استفاده از سرور کلادفلر به عنوان پروکسی برای عبور از تحریم
            val url = URL("$WORKER_URL/search?q=$encodedQuery")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                parseDeezerSearchJson(responseText, results)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        // فال‌بک ثانویه فقط در صورتی که سرور کلودفلر موقتاً در دسترس نباشد
        if (results.isEmpty()) {
            var fallbackConn: HttpURLConnection? = null
            try {
                val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
                val fallbackUrl = URL("https://api.deezer.com/search?q=$encodedQuery&limit=35")
                fallbackConn = (fallbackUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                }
                if (fallbackConn.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = fallbackConn.inputStream.bufferedReader().use { it.readText() }
                    parseDeezerSearchJson(responseText, results)
                }
            } catch (_: Exception) {
            } finally {
                fallbackConn?.disconnect()
            }
        }

        if (results.isNotEmpty()) {
            catalogCache.put(cleanQuery.lowercase(), results)
        }
        results
    }

    private fun parseDeezerSearchJson(responseText: String, results: MutableList<OnlineTrack>) {
        val jsonArray = JSONObject(responseText).optJSONArray("data") ?: return
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val artistObj = item.optJSONObject("artist")
            val albumObj = item.optJSONObject("album")

            val id = item.optLong("id").toString()
            val title = item.optString("title", "Unknown Track")
            val artistName = artistObj?.optString("name", "Unknown Artist") ?: "Unknown Artist"
            val artistId = artistObj?.optLong("id", 0L)?.takeIf { it > 0L }?.toString() ?: ""
            val artistPic = artistObj?.optString("picture_xl")?.takeIf { it.isNotBlank() }
                ?: artistObj?.optString("picture_big")?.takeIf { it.isNotBlank() }
                ?: artistObj?.optString("picture_medium")?.takeIf { it.isNotBlank() }
                ?: ""

            val albumTitle = albumObj?.optString("title", "Single") ?: "Single"
            val coverUrl = albumObj?.optString("cover_xl")?.takeIf { it.isNotBlank() }
                ?: albumObj?.optString("cover_big")?.takeIf { it.isNotBlank() }
                ?: albumObj?.optString("cover_medium")
                ?: ""
            val duration = item.optInt("duration", 180)
            val preview = item.optString("preview", "")
            val rank = item.optInt("rank", 500_000)

            if (preview.isNotBlank()) {
                registerPreviewUrl(id, preview)
            }
            if (artistPic.isNotBlank()) {
                registerArtistPicture(artistName, artistPic)
                val primaryArtist = artistName.substringBefore(" feat").substringBefore(" ft.").substringBefore("&").substringBefore(",").trim()
                if (primaryArtist.isNotBlank()) {
                    registerArtistPicture(primaryArtist, artistPic)
                }
            }

            results.add(
                OnlineTrack(
                    id = id,
                    title = title,
                    artist = artistName,
                    album = albumTitle,
                    coverUrl = coverUrl,
                    durationSec = duration,
                    previewUrl = preview,
                    artistId = artistId,
                    artistPictureUrl = artistPic,
                    rank = rank
                )
            )
        }
    }

    /**
     * دریافت محبوب‌ترین آهنگ‌های یک خواننده مرتب‌شده بر اساس بالاترین ریتینگ (Rank) به سبک اسپاتیفای
     */
    suspend fun fetchArtistTopRatedTracks(artistName: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val rawResults = searchCatalog(artistName)
        val cleanTarget = artistName.trim().lowercase()
        val matching = rawResults.filter {
            it.artist.lowercase().contains(cleanTarget) || cleanTarget.contains(it.artist.lowercase())
        }
        val pool = if (matching.size >= 4) matching else rawResults
        pool.distinctBy { it.title.lowercase().substringBefore(" (").trim() }
            .sortedByDescending { it.rank }
    }

    /**
     * پیدا کردن لینک فایل کامل صوتی (Full-Length 320kbps/160kbps Track) از ۰:۰۰ تا ثانیه آخر
     * تا مشکل دانلود یا پخش ۳۰ ثانیه‌ای از وسط آهنگ به طور کامل برطرف شود.
     */
    suspend fun resolveFullLengthAudioUrl(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        val cacheKey = "${artist.lowercase().trim()}|${title.lowercase().trim()}"
        fullStreamUrlCache[cacheKey]?.let { if (it.isNotBlank()) return@withContext it }

        val cleanArtist = artist.substringBefore(" feat").substringBefore(" ft.").trim()
        val cleanTitle = title.substringBefore(" (feat").substringBefore(" [").trim()
        val encoded = URLEncoder.encode("$cleanArtist $cleanTitle", "UTF-8")

        val apiEndpoints = listOf(
            "https://saavn.dev/api/search/songs?query=$encoded&limit=5",
            "https://saavn.sumit.co/api/search/songs?query=$encoded&limit=5",
            "https://jiosaavn-api-privatecvc2.vercel.app/search/songs?query=$encoded&limit=5"
        )

        for (endpoint in apiEndpoints) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 9000
                    readTimeout = 9000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                }
                if (conn.responseCode in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val extractedUrl = extractBestStreamUrlFromSaavnJson(body, cleanArtist, cleanTitle)
                    if (!extractedUrl.isNullOrBlank()) {
                        fullStreamUrlCache[cacheKey] = extractedUrl
                        return@withContext extractedUrl
                    }
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
        }
        null
    }

    private fun extractBestStreamUrlFromSaavnJson(jsonStr: String, targetArtist: String, targetTitle: String): String? {
        return try {
            val root = JSONObject(jsonStr)
            val resultsArray: JSONArray? = root.optJSONObject("data")?.optJSONArray("results")
                ?: root.optJSONArray("results")
                ?: root.optJSONArray("data")
            if (resultsArray == null || resultsArray.length() == 0) return null

            var bestItem: JSONObject? = null
            val lowerTitle = targetTitle.lowercase()
            val lowerArtist = targetArtist.lowercase()

            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i) ?: continue
                val name = item.optString("name", "").ifBlank { item.optString("title", "") }.lowercase()
                val artistsText = (
                    item.optString("primaryArtists", "") + " " +
                        (item.optJSONObject("artists")?.optJSONArray("primary")?.optJSONObject(0)?.optString("name", "") ?: "")
                    ).lowercase()
                if (name.contains(lowerTitle.take(6)) && (artistsText.contains(lowerArtist.take(5)) || i == 0)) {
                    bestItem = item
                    break
                }
            }
            val chosen = bestItem ?: resultsArray.optJSONObject(0) ?: return null
            val dlArray = chosen.optJSONArray("downloadUrl") ?: return null
            // انتخاب بالاترین کیفیت صوتی (320kbps یا 160kbps)
            for (idx in dlArray.length() - 1 downTo 0) {
                val dlObj = dlArray.optJSONObject(idx) ?: continue
                val link = dlObj.optString("url").ifBlank { dlObj.optString("link") }
                if (link.startsWith("http")) {
                    return link
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadCoverBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        loadCoverBitmapBlocking(url)
    }

    fun loadCoverBitmapBlocking(url: String): Bitmap? {
        if (url.isBlank()) return null
        imageMemoryCache.get(url)?.let { return it }

        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                doInput = true
            }
            conn.connect()
            if (conn.responseCode in 200..299) {
                val bitmap = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    imageMemoryCache.put(url, bitmap)
                }
                bitmap
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun buildStreamUrl(track: OnlineTrack): String {
        return "$WORKER_URL/?id=${track.id}&q=${URLEncoder.encode("${track.artist} - ${track.title}", "UTF-8")}"
    }

    fun buildStreamUrlForTitleArtist(artist: String, title: String, trackId: String = ""): String {
        val cleanId = trackId.removePrefix("online_")
        return "$WORKER_URL/?id=$cleanId&q=${URLEncoder.encode("$artist - $title", "UTF-8")}"
    }

    fun buildDownloadUrl(trackId: String, artist: String, title: String): String {
        val cleanId = trackId.removePrefix("online_")
        val encodedQuery = URLEncoder.encode("$artist - $title", "UTF-8")
        return "$WORKER_URL/?id=$cleanId&q=$encodedQuery&dl=1"
    }
}

/**
 * موتور هوشمند یادگیری سلیقه کاربر + ساخت پلی‌لیست‌های هوشمند بر اساس آهنگ‌ها و آرتیست‌های سرچ‌شده
 */
object OnlineTasteEngine {
    private const val PREFS_NAME = "aura_online_taste_prefs"
    private const val KEY_ARTIST_SCORES = "artist_scores_json"
    private const val KEY_STYLE_SCORES = "style_scores_json"
    private const val KEY_RECENT_ONLINE_TRACKS = "recent_online_tracks_json"
    private const val KEY_RECENT_SEARCH_QUERIES = "recent_search_queries_json"

    fun recordSearchActivity(context: Context, query: String, results: List<OnlineTrack>) {
        val clean = query.trim()
        if (clean.length < 2) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val queriesArr = JSONArray(prefs.getString(KEY_RECENT_SEARCH_QUERIES, "[]") ?: "[]")
            val updatedQueries = JSONArray()
            updatedQueries.put(clean)
            for (i in 0 until queriesArr.length()) {
                val q = queriesArr.optString(i)
                if (!q.equals(clean, ignoreCase = true) && updatedQueries.length() < 10) {
                    updatedQueries.put(q)
                }
            }
            prefs.edit().putString(KEY_RECENT_SEARCH_QUERIES, updatedQueries.toString()).apply()

            val topArtist = results.firstOrNull()?.artist
                ?.substringBefore(" feat")
                ?.substringBefore(" ft.")
                ?.substringBefore(",")
                ?.trim()
            if (!topArtist.isNullOrBlank()) {
                val scoresJson = JSONObject(prefs.getString(KEY_ARTIST_SCORES, "{}") ?: "{}")
                scoresJson.put(topArtist, scoresJson.optInt(topArtist, 0) + 2)
                prefs.edit().putString(KEY_ARTIST_SCORES, scoresJson.toString()).apply()
            }
        } catch (_: Exception) {}
    }

    fun getRecentSearchedQueries(context: Context): List<String> {
        val list = mutableListOf<String>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString(KEY_RECENT_SEARCH_QUERIES, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val q = arr.optString(i).trim()
                if (q.isNotBlank()) list.add(q)
            }
        } catch (_: Exception) {}
        return list
    }

    fun recordTrackListen(context: Context, track: Track) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val artist = track.artist.trim()
            if (artist.isNotBlank() && !artist.equals("Unknown Artist", ignoreCase = true) && !artist.equals("Unknown", ignoreCase = true)) {
                val primaryArtist = artist.substringBefore(" feat").substringBefore(" ft.").substringBefore("&").substringBefore(",").trim()
                val scoresJson = JSONObject(prefs.getString(KEY_ARTIST_SCORES, "{}") ?: "{}")
                val oldScore = scoresJson.optInt(primaryArtist, 0)
                scoresJson.put(primaryArtist, oldScore + 3)
                prefs.edit().putString(KEY_ARTIST_SCORES, scoresJson.toString()).apply()
            }

            val styleTag = inferStyleFromTrack(track.artist, track.title, track.genre)
            val styleJson = JSONObject(prefs.getString(KEY_STYLE_SCORES, "{}") ?: "{}")
            styleJson.put(styleTag, styleJson.optInt(styleTag, 0) + 2)
            prefs.edit().putString(KEY_STYLE_SCORES, styleJson.toString()).apply()

            val previewUrl = OnlineMusicRepository.getPreviewUrl(track.id) ?: ""
            val recentArr = JSONArray(prefs.getString(KEY_RECENT_ONLINE_TRACKS, "[]") ?: "[]")
            val updatedArr = JSONArray()
            val newObj = JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artist", track.artist)
                put("album", track.album)
                put("durationMs", track.durationMs)
                put("uri", track.uri)
                put("artworkUri", track.artworkUri ?: "")
                put("genre", track.genre)
                put("previewUrl", previewUrl)
            }
            updatedArr.put(newObj)
            for (i in 0 until recentArr.length()) {
                val obj = recentArr.optJSONObject(i) ?: continue
                if (obj.optString("id") != track.id && updatedArr.length() < 18) {
                    updatedArr.put(obj)
                }
            }
            prefs.edit().putString(KEY_RECENT_ONLINE_TRACKS, updatedArr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getRecentOnlineTracks(context: Context): List<Track> {
        val list = mutableListOf<Track>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString(KEY_RECENT_ONLINE_TRACKS, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val savedPreview = obj.optString("previewUrl", "")
                if (savedPreview.isNotBlank()) {
                    OnlineMusicRepository.registerPreviewUrl(id, savedPreview)
                }
                val cachedFile = OnlineCacheHolder.getCachedFileIfPresent(context, id)
                val uri = cachedFile?.absolutePath ?: obj.optString("uri")
                if (id.isNotBlank() && uri.isNotBlank()) {
                    list.add(
                        Track(
                            id = id,
                            title = obj.optString("title", "Unknown"),
                            artist = obj.optString("artist", "Unknown"),
                            album = obj.optString("album", "Single"),
                            durationMs = obj.optLong("durationMs", 180_000L),
                            uri = uri,
                            artworkUri = obj.optString("artworkUri").takeIf { it.isNotBlank() },
                            genre = obj.optString("genre", "Online"),
                            lastPlayedTimestamp = System.currentTimeMillis() - (i * 1000L)
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun getTopArtistsForUser(
        context: Context,
        localRecent: List<Track> = emptyList(),
        localFavorites: List<Track> = emptyList()
    ): List<String> {
        val map = mutableMapOf<String, Int>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val scoresJson = JSONObject(prefs.getString(KEY_ARTIST_SCORES, "{}") ?: "{}")
            val keys = scoresJson.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = (map[k] ?: 0) + scoresJson.optInt(k, 0)
            }
        } catch (_: Exception) {}

        localFavorites.forEach { t ->
            val a = t.artist.substringBefore(" feat").substringBefore(",").trim()
            if (a.isNotBlank() && !a.equals("Unknown Artist", true)) {
                map[a] = (map[a] ?: 0) + 4
            }
        }
        localRecent.forEach { t ->
            val a = t.artist.substringBefore(" feat").substringBefore(",").trim()
            if (a.isNotBlank() && !a.equals("Unknown Artist", true)) {
                map[a] = (map[a] ?: 0) + 2
            }
        }

        return map.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(6)
    }

    /**
     * ساخت پلی‌لیست‌های هوشمند (Smart Playlists) بر اساس سرچ‌ها، آرتیست‌های محبوب و آهنگ‌های در حال نمایش
     */
    fun buildSmartPlaylists(
        context: Context,
        currentCatalogTracks: List<OnlineTrack>
    ): List<OnlineSmartPlaylist> {
        val topArtists = getTopArtistsForUser(context)
        val recentSearches = getRecentSearchedQueries(context)
        val recentTracks = getRecentOnlineTracks(context)

        val defaultCovers = currentCatalogTracks.mapNotNull { it.coverUrl.takeIf { c -> c.isNotBlank() } }.distinct()
        val recentCovers = recentTracks.mapNotNull { it.artworkUri?.takeIf { c -> c.isNotBlank() } }.distinct()

        val primaryArtist = topArtists.getOrNull(0) ?: "Travis Scott"
        val secondaryArtist = topArtists.getOrNull(1) ?: "The Weeknd"
        val thirdArtist = topArtists.getOrNull(2) ?: "Playboi Carti"
        val latestSearch = recentSearches.firstOrNull()

        val playlists = mutableListOf<OnlineSmartPlaylist>()

        // ۱. میکس هوشمند بر اساس آخرین جستجو و سلیقه کاربر
        val mix1Query = if (!latestSearch.isNullOrBlank()) {
            "$latestSearch $primaryArtist"
        } else {
            "$primaryArtist $secondaryArtist"
        }
        playlists.add(
            OnlineSmartPlaylist(
                id = "smart_search_mix",
                titleFa = if (!latestSearch.isNullOrBlank()) "میکس هوشمند: $latestSearch" else "میکس اختصاصی $primaryArtist",
                titleEn = if (!latestSearch.isNullOrBlank()) "Smart Mix: $latestSearch" else "$primaryArtist Mix",
                subtitleFa = "بر اساس جستجوها و آهنگ‌های مورد علاقه شما",
                subtitleEn = "Based on your searches & listening taste",
                query = mix1Query,
                seedArtists = listOf(primaryArtist, secondaryArtist),
                coverUrls = (recentCovers + defaultCovers).distinct().take(4),
                accentColor = Color(0xFF1ED760)
            )
        )

        // ۲. رادیو هنرمندان محبوب شما (بالاترین ریتینگ‌ها)
        playlists.add(
            OnlineSmartPlaylist(
                id = "smart_top_artists_radio",
                titleFa = "رادیو $primaryArtist و $secondaryArtist",
                titleEn = "$primaryArtist & $secondaryArtist Radio",
                subtitleFa = "گلچین بالاترین ریتینگ‌های آرتیست‌های شما",
                subtitleEn = "Highest rated hits from your top artists",
                query = "$primaryArtist $secondaryArtist $thirdArtist",
                seedArtists = listOf(primaryArtist, secondaryArtist, thirdArtist),
                coverUrls = (defaultCovers.drop(2) + recentCovers).distinct().take(4),
                accentColor = Color(0xFF00E5FF)
            )
        )

        // ۳. پلی‌لیست هوشمند وایب تیک‌تاک و رپرهای نسل زد
        playlists.add(
            OnlineSmartPlaylist(
                id = "smart_genz_viral",
                titleFa = "پلی‌لیست هوشمند ترند نسل زد",
                titleEn = "Gen-Z Viral Smart Mix",
                subtitleFa = "Playboi Carti, Yeat, Don Toliver, Travis Scott",
                subtitleEn = "Playboi Carti, Yeat, Don Toliver, Travis Scott",
                query = "Playboi Carti Yeat Don Toliver Travis Scott",
                seedArtists = listOf("Playboi Carti", "Yeat", "Don Toliver"),
                coverUrls = defaultCovers.reversed().take(4),
                accentColor = Color(0xFFFF2A85)
            )
        )

        // ۴. پلی‌لیست هوشمند شبانه و درایو (Late Night Drive)
        playlists.add(
            OnlineSmartPlaylist(
                id = "smart_night_drive",
                titleFa = "میکس شبانه و رانندگی",
                titleEn = "Late Night Drive Mix",
                subtitleFa = "The Weeknd, SZA, Billie Eilish, Future",
                subtitleEn = "The Weeknd, SZA, Billie Eilish, Future",
                query = "The Weeknd SZA Billie Eilish Don Toliver",
                seedArtists = listOf("The Weeknd", "SZA", "Billie Eilish"),
                coverUrls = (defaultCovers.drop(4) + defaultCovers.take(2)).distinct().take(4),
                accentColor = Color(0xFF9B5DE5)
            )
        )

        return playlists
    }

    private fun inferStyleFromTrack(artist: String, title: String, genre: String): String {
        val combined = "$artist $title $genre".lowercase()
        return when {
            combined.contains("travis") || combined.contains("carti") || combined.contains("yeat") ||
                combined.contains("drake") || combined.contains("future") || combined.contains("metro") ||
                combined.contains("21 savage") || combined.contains("kendrick") || combined.contains("central cee") ||
                combined.contains("gunna") || combined.contains("lil ") || combined.contains("rap") -> "RAPPERS"
            combined.contains("billie") || combined.contains("sabrina") || combined.contains("tate") ||
                combined.contains("pinkpantheress") || combined.contains("ice spice") || combined.contains("tiktok") -> "TIKTOK_INSTA"
            combined.contains("weeknd") || combined.contains("sza") || combined.contains("don toliver") ||
                combined.contains("partynextdoor") || combined.contains("phonk") || combined.contains("kordhell") -> "PHONK_NIGHT"
            combined.contains("حصین") || combined.contains("پیشرو") || combined.contains("شاپور") ||
                combined.contains("هیدن") || combined.contains("چرسی") || combined.contains("پوری") ||
                combined.contains("دورچی") || combined.contains("هیپهاپولوژیست") ||
                combined.contains("farsi") || combined.contains("persian") || Regex("[\u0600-\u06FF]").containsMatchIn(combined) -> "PERSIAN"
            else -> "FOR_YOU"
        }
    }

    fun getDominantStyle(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val styleJson = JSONObject(prefs.getString(KEY_STYLE_SCORES, "{}") ?: "{}")
            var bestStyle = "FOR_YOU"
            var bestScore = -1
            val keys = styleJson.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val s = styleJson.optInt(k, 0)
                if (s > bestScore) {
                    bestScore = s
                    bestStyle = k
                }
            }
            bestStyle
        } catch (_: Exception) {
            "FOR_YOU"
        }
    }

    suspend fun fetchOnlineTimeSlotTracks(
        context: Context,
        hourOfDay: Int,
        topArtists: List<String>
    ): List<Track> = withContext(Dispatchers.IO) {
        val topArtist = topArtists.firstOrNull()
        val timeQuery = when (hourOfDay) {
            in 5..11 -> if (topArtist != null) "$topArtist Chill" else "Billie Eilish The Weeknd SZA"
            in 12..16 -> if (topArtist != null) "$topArtist Hits" else "Travis Scott Playboi Carti Drake"
            in 17..21 -> if (topArtist != null) "$topArtist Vibe" else "Don Toliver The Weeknd Future"
            else -> if (topArtist != null) "$topArtist Night" else "Travis Scott Drake Yeat Late Night"
        }

        val fallbackQuery = when (hourOfDay) {
            in 5..11 -> "Post Malone Billie Eilish"
            in 12..16 -> "Kendrick Lamar Future Metro Boomin"
            in 17..21 -> "Central Cee Don Toliver SZA"
            else -> "Playboi Carti The Weeknd"
        }

        val primaryResults = OnlineMusicRepository.searchCatalog(timeQuery)
        val secondaryResults = if (primaryResults.size < 10) {
            OnlineMusicRepository.searchCatalog(fallbackQuery)
        } else emptyList()

        (primaryResults + secondaryResults)
            .distinctBy { it.id }
            .take(14)
            .map { online ->
                val cached = OnlineCacheHolder.getCachedFileIfPresent(context, online.id)
                val streamUrl = cached?.absolutePath ?: OnlineMusicRepository.buildStreamUrl(online)
                online.toAppTrack(streamUrl)
            }
    }

    suspend fun fetchTrendingAndTasteFeed(
        context: Context,
        mode: String,
        topArtists: List<String>
    ): List<Track> = withContext(Dispatchers.IO) {
        val queries = when (mode) {
            "FOR_YOU" -> {
                if (topArtists.isNotEmpty()) {
                    topArtists.take(2).map { it } + listOf("Travis Scott Playboi Carti Drake", "Billie Eilish The Weeknd Don Toliver")
                } else {
                    when (getDominantStyle(context)) {
                        "RAPPERS" -> listOf("Travis Scott Playboi Carti Future", "Drake Kendrick Lamar Yeat")
                        "TIKTOK_INSTA" -> listOf("Playboi Carti Yeat Don Toliver", "Billie Eilish Sabrina Carpenter Central Cee")
                        "PERSIAN" -> listOf("Hiphopologist Chvrsi Poori", "Shapoor Ho3ein Hidden")
                        "PHONK_NIGHT" -> listOf("Kordhell Interworld Phonk", "The Weeknd Don Toliver SZA")
                        else -> listOf("Travis Scott Playboi Carti Drake", "Billie Eilish Kendrick Lamar The Weeknd")
                    }
                }
            }
            "TIKTOK_INSTA" -> listOf(
                "Playboi Carti Yeat Don Toliver",
                "Billie Eilish Sabrina Carpenter Tate McRae",
                "Central Cee PinkPantheress Tommy Richman"
            )
            "YOUTUBE" -> listOf(
                "Kendrick Lamar Travis Scott Drake",
                "The Weeknd Post Malone Future",
                "Metro Boomin 21 Savage Billie Eilish"
            )
            "RAPPERS" -> listOf(
                "Travis Scott Playboi Carti Future",
                "Drake Kendrick Lamar 21 Savage",
                "Central Cee Yeat Gunna Lil Baby"
            )
            "PERSIAN" -> listOf(
                "Hiphopologist Chvrsi Poori",
                "Shapoor Ho3ein Shayea Hidden",
                "Dorcci Koorosh Behzad Leito"
            )
            "PHONK_NIGHT" -> listOf(
                "Kordhell Interworld Phonk",
                "The Weeknd Don Toliver SZA"
            )
            else -> listOf("Travis Scott Playboi Carti Drake")
        }

        val combined = mutableListOf<OnlineTrack>()
        for (q in queries) {
            val res = OnlineMusicRepository.searchCatalog(q)
            combined.addAll(res)
            if (combined.size >= 18) break
        }

        combined
            .distinctBy { "${it.title.lowercase().trim()}|${it.artist.lowercase().trim()}" }
            .take(16)
            .map { online ->
                val cached = OnlineCacheHolder.getCachedFileIfPresent(context, online.id)
                val streamUrl = cached?.absolutePath ?: OnlineMusicRepository.buildStreamUrl(online)
                online.toAppTrack(streamUrl)
            }
    }
}

/**
 * مدیریت دانلود کامل آهنگ‌ها (از ثانیه ۰:۰۰ تا انتهای آهنگ با قابلیت Resume خودکار در صورت قطع شبکه)
 * + بافر موقت استریم برای جلوگیری از پرش یا قطع شدن وسط پخش آنلاین
 */
object OnlineCacheHolder {
    private const val MIN_VALID_AUDIO_BYTES = 16_384L
    // حداقل حجم برای تشخیص فایل کامل صوتی (بیشتر از پیش‌نمایش ۳۰ ثانیه‌ای ۴۸۰ کیلوبایتی)
    private const val MIN_FULL_SONG_BYTES = 850_000L

    /**
     * تنظیمات استاندارد شبکه و کش با پشتیبانی از Cross-Protocol Redirects و تایم‌اوت ۲۰ ثانیه‌ای
     * و فلگ FLAG_IGNORE_CACHE_ON_ERROR برای جلوگیری از خطا در استریم و کش آنلاین
     */
    class DefaultHttpDataSource {
        class Factory {
            var allowCrossProtocolRedirects: Boolean = true
                private set
            var connectTimeoutMs: Int = 20000
                private set
            var readTimeoutMs: Int = 20000
                private set

            fun setAllowCrossProtocolRedirects(allow: Boolean): Factory = apply {
                this.allowCrossProtocolRedirects = allow
            }

            fun setConnectTimeoutMs(timeoutMs: Int): Factory = apply {
                this.connectTimeoutMs = timeoutMs
            }

            fun setReadTimeoutMs(timeoutMs: Int): Factory = apply {
                this.readTimeoutMs = timeoutMs
            }
        }
    }

    class CacheDataSource {
        companion object {
            const val FLAG_IGNORE_CACHE_ON_ERROR: Int = 1 shl 1
        }

        class Factory {
            var upstreamDataSourceFactory: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
                private set
            var flags: Int = FLAG_IGNORE_CACHE_ON_ERROR
                private set

            fun setUpstreamDataSourceFactory(factory: DefaultHttpDataSource.Factory): Factory = apply {
                this.upstreamDataSourceFactory = factory
            }

            fun setFlags(flags: Int): Factory = apply {
                this.flags = flags
            }
        }
    }

    val httpDataSourceFactory: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(20000)
        .setReadTimeoutMs(20000)

    val cacheDataSourceFactory: CacheDataSource.Factory = CacheDataSource.Factory()
        .setUpstreamDataSourceFactory(httpDataSourceFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = HashSet<String>()
    private val activeStreamBuffers = HashSet<String>()

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    private val _downloadProgressMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Int>> = _downloadProgressMap.asStateFlow()

    private val _downloadedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedTrackIds: StateFlow<Set<String>> = _downloadedTrackIds.asStateFlow()

    private fun updateTrackDownloadProgress(rawId: String, percent: Int?) {
        val clean = rawId.removePrefix("online_")
        val next = _downloadProgressMap.value.toMutableMap()
        if (percent == null) {
            next.remove(clean)
            next.remove("online_$clean")
        } else {
            val safePct = percent.coerceIn(0, 100)
            next[clean] = safePct
            next["online_$clean"] = safePct
        }
        _downloadProgressMap.value = next
    }

    /**
     * پوشه اصلی ذخیره دائمی آهنگ‌های دانلودشده در فضای خارجی اختصاصی موزیک گوشی:
     * context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
     */
    fun getCacheDir(context: Context): File {
        val extMusicDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
        if (extMusicDir != null) {
            if (!extMusicDir.exists()) extMusicDir.mkdirs()
            return extMusicDir
        }
        val fallbackDir = File(context.filesDir, "aura_downloaded_music")
        if (!fallbackDir.exists()) fallbackDir.mkdirs()
        return fallbackDir
    }

    private fun getInternalDownloadedDir(context: Context): File {
        val dir = File(context.filesDir, "aura_downloaded_music")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getStreamBufferDir(context: Context): File {
        val dir = File(context.cacheDir, "aura_stream_full_buffer")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getLegacyCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "aura_online_audio_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun normalizeKey(trackId: String): String {
        val clean = trackId.removePrefix("online_").replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "aura_track_$clean.mp3"
    }

    fun refreshDownloadedSet(context: Context) {
        val ids = mutableSetOf<String>()
        listOf(getCacheDir(context), getInternalDownloadedDir(context), getLegacyCacheDir(context)).forEach { dir ->
            dir.listFiles()?.forEach { f ->
                if (f.isFile && f.length() > MIN_VALID_AUDIO_BYTES && f.name.endsWith(".mp3")) {
                    val rawId = f.name.removePrefix("aura_track_").removeSuffix(".mp3")
                    ids.add(rawId)
                    ids.add("online_$rawId")
                }
            }
        }
        _downloadedTrackIds.value = ids
    }

    fun isTrackCached(context: Context, trackId: String): Boolean {
        return getCachedFileIfPresent(context, trackId) != null
    }

    fun getCachedFileIfPresent(context: Context, trackId: String): File? {
        val key = normalizeKey(trackId)
        val primary = File(getCacheDir(context), key)
        if (primary.exists() && primary.length() > MIN_VALID_AUDIO_BYTES) {
            primary.setLastModified(System.currentTimeMillis())
            return primary
        }
        val internal = File(getInternalDownloadedDir(context), key)
        if (internal.exists() && internal.length() > MIN_VALID_AUDIO_BYTES) {
            internal.setLastModified(System.currentTimeMillis())
            return internal
        }
        val legacy = File(getLegacyCacheDir(context), key)
        return if (legacy.exists() && legacy.length() > MIN_VALID_AUDIO_BYTES) {
            legacy.setLastModified(System.currentTimeMillis())
            legacy
        } else null
    }

    fun getStreamBufferFileIfPresent(context: Context, trackId: String): File? {
        val key = normalizeKey(trackId)
        val buffered = File(getStreamBufferDir(context), key)
        return if (buffered.exists() && buffered.length() >= MIN_FULL_SONG_BYTES) {
            buffered.setLastModified(System.currentTimeMillis())
            buffered
        } else null
    }

    fun getCurrentCacheSizeBytes(context: Context): Long {
        val s1 = getCacheDir(context).listFiles()?.sumOf { it.length() } ?: 0L
        val s2 = getInternalDownloadedDir(context).listFiles()?.sumOf { it.length() } ?: 0L
        val s3 = getLegacyCacheDir(context).listFiles()?.sumOf { it.length() } ?: 0L
        return s1 + s2 + s3
    }

    fun getCachedTracksCount(context: Context): Int {
        val c1 = getCacheDir(context).listFiles()?.count { it.isFile && it.length() > MIN_VALID_AUDIO_BYTES } ?: 0
        val c2 = getInternalDownloadedDir(context).listFiles()?.count { it.isFile && it.length() > MIN_VALID_AUDIO_BYTES } ?: 0
        val c3 = getLegacyCacheDir(context).listFiles()?.count { it.isFile && it.length() > MIN_VALID_AUDIO_BYTES } ?: 0
        return c1 + c2 + c3
    }

    /**
     * دانلود کامل و واقعی فایل صوتی (Full File Download) با HttpURLConnection در Dispatchers.IO
     * همراه با گزارش لحظه‌ای درصد پیشرفت (0 تا 100 درصد) و پشتیبانی از Cross-Protocol Redirects و HTTP Range Resume
     */
    private fun downloadUrlToTempFile(
        urlStr: String,
        tempFile: File,
        depth: Int = 0,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean {
        if (depth > 5 || urlStr.isBlank()) return false
        var currentUrl = urlStr
        var redirects = 0
        val connectTimeoutMs = httpDataSourceFactory.connectTimeoutMs
        val readTimeoutMs = httpDataSourceFactory.readTimeoutMs

        while (redirects < 6) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs.coerceAtLeast(20000)
                    requestMethod = "GET"
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    setRequestProperty("Accept", "*/*")
                    setRequestProperty("Accept-Encoding", "identity")
                    setRequestProperty("Connection", "keep-alive")
                }

                val code = conn.responseCode
                if (httpDataSourceFactory.allowCrossProtocolRedirects && code in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) return false
                    currentUrl = URL(URL(currentUrl), location).toString()
                    redirects++
                    continue
                }

                if (code in 200..299) {
                    val contentType = (conn.contentType ?: "").lowercase()
                    if (contentType.contains("text/html")) {
                        conn.disconnect()
                        return false
                    }

                    val expectedTotalBytes = conn.contentLengthLong
                    val estimatedBytesForProgress = if (expectedTotalBytes > MIN_VALID_AUDIO_BYTES) {
                        expectedTotalBytes
                    } else {
                        3_800_000L
                    }
                    if (tempFile.exists()) tempFile.delete()
                    onProgress?.invoke(1)

                    var totalBytesRead = 0L
                    var lastReportedPct = 1

                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buf = ByteArray(32 * 1024)
                            var read: Int
                            while (input.read(buf).also { read = it } != -1) {
                                output.write(buf, 0, read)
                                totalBytesRead += read
                                val pct = ((totalBytesRead * 98L) / estimatedBytesForProgress).toInt().coerceIn(1, 98)
                                if (pct > lastReportedPct) {
                                    lastReportedPct = pct
                                    onProgress?.invoke(pct)
                                }
                            }
                            output.flush()
                        }
                    }
                    conn.disconnect()

                    // اگر سرور یک JSON حاوی لینک دانلود برگردانده باشد، لینک داخل JSON را دانلود کن
                    if (tempFile.exists() && tempFile.length() in 10L..4095L) {
                        val text = tempFile.readText().trim()
                        tempFile.delete()
                        if (text.startsWith("{")) {
                            val json = JSONObject(text)
                            val innerUrl = json.optString("url")
                                .ifBlank { json.optString("streamUrl") }
                                .ifBlank { json.optString("audioUrl") }
                                .ifBlank { json.optString("downloadUrl") }
                                .ifBlank { json.optString("stream") }
                                .ifBlank { json.optString("link") }
                            if (innerUrl.startsWith("http")) {
                                return downloadUrlToTempFile(innerUrl, tempFile, depth + 1, onProgress)
                            }
                        } else if (text.startsWith("http://") || text.startsWith("https://")) {
                            return downloadUrlToTempFile(text.lines().first().trim(), tempFile, depth + 1, onProgress)
                        }
                        return false
                    }

                    // اگر ارتباط شبکه قبل از دریافت ۱۰۰٪ بایت‌های فایل قطع شد، با HTTP Range ادامه فایل را تا بایت آخر دانلود کن
                    var resumeAttempt = 0
                    while (expectedTotalBytes > MIN_VALID_AUDIO_BYTES &&
                        tempFile.exists() &&
                        tempFile.length() in 1 until expectedTotalBytes &&
                        resumeAttempt < 4
                    ) {
                        resumeAttempt++
                        val downloadedSoFar = tempFile.length()
                        var rangeConn: HttpURLConnection? = null
                        try {
                            rangeConn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                                instanceFollowRedirects = true
                                connectTimeout = connectTimeoutMs
                                readTimeout = readTimeoutMs.coerceAtLeast(25000)
                                requestMethod = "GET"
                                setRequestProperty("Range", "bytes=$downloadedSoFar-")
                                setRequestProperty("Accept-Encoding", "identity")
                                setRequestProperty("Connection", "keep-alive")
                            }
                            if (rangeConn.responseCode == HttpURLConnection.HTTP_PARTIAL) {
                                rangeConn.inputStream.use { input ->
                                    FileOutputStream(tempFile, true).use { output ->
                                        val buf = ByteArray(32 * 1024)
                                        var read: Int
                                        var currentLen = downloadedSoFar
                                        while (input.read(buf).also { read = it } != -1) {
                                            output.write(buf, 0, read)
                                            currentLen += read
                                            val pct = ((currentLen * 99L) / expectedTotalBytes).toInt().coerceIn(lastReportedPct, 99)
                                            if (pct > lastReportedPct) {
                                                lastReportedPct = pct
                                                onProgress?.invoke(pct)
                                            }
                                        }
                                        output.flush()
                                    }
                                }
                            } else {
                                break
                            }
                        } catch (_: Exception) {
                            break
                        } finally {
                            try { rangeConn?.disconnect() } catch (_: Exception) {}
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > MIN_VALID_AUDIO_BYTES) {
                        if (expectedTotalBytes > 0 && tempFile.length() < (expectedTotalBytes * 0.92f).toLong()) {
                            tempFile.delete()
                            return false
                        }
                        onProgress?.invoke(100)
                        return true
                    }
                    tempFile.delete()
                    return false
                } else {
                    conn.disconnect()
                    return false
                }
            } catch (_: Exception) {
                try { conn?.disconnect() } catch (_: Exception) {}
                if (tempFile.exists()) tempFile.delete()
                return false
            }
        }
        return false
    }

    /**
     * دانلود کامل فایل صوتی از آدرس دقیق "$WORKER_URL/?id=${track.id}&q=$encodedQuery&dl=1"
     * بدون استفاده از لینک ۳۰ ثانیه‌ای دیزر
     */
    private suspend fun fetchCompleteTrackFile(
        track: Track,
        tempFile: File,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val rawId = track.id.removePrefix("online_")
        val encodedQuery = URLEncoder.encode("${track.artist} - ${track.title}", "UTF-8")
        // آدرس دقیق درخواستی برای دانلود کامل فایل صوتی از سرور کلودفلر
        val workerDownloadUrl = "${OnlineMusicRepository.WORKER_URL}/?id=$rawId&q=$encodedQuery&dl=1"

        // گام اول: دانلود مستقیم فایل کامل از آدرس ?id=...&q=...&dl=1 در Dispatchers.IO
        val downloaded = downloadUrlToTempFile(workerDownloadUrl, tempFile, onProgress = onProgress)
        if (downloaded && tempFile.exists() && tempFile.length() >= MIN_FULL_SONG_BYTES) {
            return@withContext true
        }

        // گام دوم: اگر استریم مستقیم دیگری در track.uri بود (غیر از لینک ۳۰ ثانیه‌ای دیزر)
        if (track.uri.startsWith("http") && !track.uri.contains("dzcdn.net") && track.uri != workerDownloadUrl) {
            val fromTrackUri = downloadUrlToTempFile(track.uri, tempFile, onProgress = onProgress)
            if (fromTrackUri && tempFile.exists() && tempFile.length() >= MIN_FULL_SONG_BYTES) {
                return@withContext true
            }
        }

        // گام سوم: فال‌بک به سرور پشتیبان فایل کامل ۳۲۰ کیلوبیت (هرگز از لینک ۳۰ ثانیه‌ای دیزر استفاده نمی‌شود)
        val fullBackupTemp = File(tempFile.parentFile, "${tempFile.name}.full.tmp")
        try {
            val fullAudioUrl = OnlineMusicRepository.resolveFullLengthAudioUrl(track.artist, track.title)
            if (!fullAudioUrl.isNullOrBlank()) {
                val gotFull = downloadUrlToTempFile(fullAudioUrl, fullBackupTemp, onProgress = onProgress)
                if (gotFull && fullBackupTemp.exists() && fullBackupTemp.length() >= MIN_VALID_AUDIO_BYTES) {
                    if (tempFile.exists()) tempFile.delete()
                    val renamed = fullBackupTemp.renameTo(tempFile)
                    if (!renamed) {
                        fullBackupTemp.copyTo(tempFile, overwrite = true)
                        fullBackupTemp.delete()
                    }
                    return@withContext true
                }
            }
        } catch (_: Exception) {
        } finally {
            if (fullBackupTemp.exists()) fullBackupTemp.delete()
        }

        downloaded && tempFile.exists() && tempFile.length() > MIN_VALID_AUDIO_BYTES
    }

    /**
     * بافر کردن نسخه کامل آهنگ در حافظه موقت هنگام پخش آنلاین تا وسط آهنگ نپرد یا قطع نشود
     */
    fun warmStreamBufferInBackground(
        context: Context,
        track: Track,
        onBuffered: ((File?) -> Unit)? = null
    ) {
        val rawId = track.id.removePrefix("online_")
        val key = normalizeKey(rawId)
        getCachedFileIfPresent(context, rawId)?.let {
            onBuffered?.invoke(it)
            return
        }
        getStreamBufferFileIfPresent(context, rawId)?.let {
            onBuffered?.invoke(it)
            return
        }

        synchronized(activeStreamBuffers) {
            if (activeStreamBuffers.contains(key)) return
            activeStreamBuffers.add(key)
        }

        val appCtx = context.applicationContext
        cacheScope.launch(Dispatchers.IO) {
            val bufferDir = getStreamBufferDir(appCtx)
            val tempFile = File(bufferDir, "$key.stream.tmp")
            val finalBufferFile = File(bufferDir, key)
            var result: File? = null
            try {
                val existingBuffers = bufferDir.listFiles()?.filter { it.isFile && it.name.endsWith(".mp3") }?.sortedBy { it.lastModified() } ?: emptyList()
                if (existingBuffers.size > 8) {
                    existingBuffers.take(existingBuffers.size - 8).forEach { try { it.delete() } catch (_: Exception) {} }
                }

                val ok = fetchCompleteTrackFile(track, tempFile)
                if (ok && tempFile.exists() && tempFile.length() > MIN_VALID_AUDIO_BYTES) {
                    if (finalBufferFile.exists()) finalBufferFile.delete()
                    val moved = tempFile.renameTo(finalBufferFile)
                    if (!moved) {
                        tempFile.copyTo(finalBufferFile, overwrite = true)
                        tempFile.delete()
                    }
                    if (finalBufferFile.exists()) {
                        result = finalBufferFile
                    }
                }
            } catch (_: Exception) {
                if (tempFile.exists()) tempFile.delete()
            } finally {
                synchronized(activeStreamBuffers) {
                    activeStreamBuffers.remove(key)
                }
                withContext(Dispatchers.Main) {
                    onBuffered?.invoke(result)
                }
            }
        }
    }

    /**
     * ثبت خودکار فایل دانلودشده در دیتابیس محلی (MusicRepository / MusicDao)
     * تا بلافاصله در تب کتابخانه آفلاین (LibraryScreen) در دسترس باشد.
     */
    private suspend fun registerDownloadedFileInLocalDatabase(track: Track, downloadedFile: File) = withContext(Dispatchers.IO) {
        try {
            val realDurMs = try {
                val mmr = android.media.MediaMetadataRetriever()
                mmr.setDataSource(downloadedFile.absolutePath)
                val d = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                mmr.release()
                if (d != null && d > 5_000L) d else track.durationMs
            } catch (_: Exception) {
                track.durationMs
            }
            val savedTrack = track.copy(
                id = if (track.id.startsWith("online_")) track.id else "online_${track.id}",
                uri = downloadedFile.absolutePath,
                durationMs = realDurMs,
                dateAdded = System.currentTimeMillis(),
                isDemo = false
            )
            AuraApplication.instance.repository.saveDownloadedTrackToLibrary(savedTrack)
        } catch (_: Exception) {}
    }

    /**
     * دانلود کامل آهنگ (Full File Download) با HttpURLConnection در Dispatchers.IO:
     * ۱. کل فایل صوتی را تا بایت آخر از "$WORKER_URL/?id=${track.id}&q=$encodedQuery&dl=1" در پوشه getExternalFilesDir(Environment.DIRECTORY_MUSIC) ذخیره می‌کند.
     * ۲. وضعیت درصد دانلود (0 تا 100 درصد) را لحظه‌به‌لحظه منتشر می‌کند.
     * ۳. پس از اتمام دانلود، فایل را در دیتابیس محلی (MusicRepository / MusicDao) ثبت می‌کند تا در LibraryScreen در دسترس باشد.
     */
    fun downloadTrackExplicitly(
        context: Context,
        track: Track,
        onComplete: ((File?) -> Unit)? = null
    ) {
        val rawId = track.id.removePrefix("online_")
        val key = normalizeKey(rawId)
        val existing = getCachedFileIfPresent(context, rawId)
        if (existing != null && existing.length() >= MIN_FULL_SONG_BYTES) {
            updateTrackDownloadProgress(rawId, 100)
            refreshDownloadedSet(context)
            cacheScope.launch(Dispatchers.IO) {
                registerDownloadedFileInLocalDatabase(track, existing)
                updateTrackDownloadProgress(rawId, null)
            }
            onComplete?.invoke(existing)
            return
        }

        synchronized(activeDownloads) {
            if (activeDownloads.contains(key)) return
            activeDownloads.add(key)
        }
        _downloadingTrackIds.value = _downloadingTrackIds.value + setOf(rawId, "online_$rawId")
        updateTrackDownloadProgress(rawId, 0)

        val appCtx = context.applicationContext
        cacheScope.launch(Dispatchers.IO) {
            val musicDir = getCacheDir(appCtx)
            val tempFile = File(musicDir, "$key.tmp")
            val finalFile = File(musicDir, key)
            var resultFile: File? = null

            try {
                val alreadyBuffered = getStreamBufferFileIfPresent(appCtx, rawId)
                if (alreadyBuffered != null && alreadyBuffered.length() >= MIN_FULL_SONG_BYTES) {
                    updateTrackDownloadProgress(rawId, 60)
                    alreadyBuffered.copyTo(finalFile, overwrite = true)
                    updateTrackDownloadProgress(rawId, 100)
                    resultFile = finalFile
                } else {
                    val downloaded = fetchCompleteTrackFile(track, tempFile) { pct ->
                        updateTrackDownloadProgress(rawId, pct)
                    }
                    if (downloaded && tempFile.exists() && tempFile.length() > MIN_VALID_AUDIO_BYTES) {
                        if (finalFile.exists()) finalFile.delete()
                        val moved = tempFile.renameTo(finalFile)
                        if (!moved) {
                            tempFile.copyTo(finalFile, overwrite = true)
                            tempFile.delete()
                        }
                        if (finalFile.exists() && finalFile.length() > MIN_VALID_AUDIO_BYTES) {
                            updateTrackDownloadProgress(rawId, 100)
                            resultFile = finalFile
                        }
                    } else {
                        if (tempFile.exists()) tempFile.delete()
                    }
                }

                // ثبت خودکار فایل دانلودشده در دیتابیس محلی (MusicRepository / MusicDao) برای نمایش در تب LibraryScreen
                if (resultFile != null) {
                    registerDownloadedFileInLocalDatabase(track, resultFile!!)
                }
            } catch (_: Exception) {
                if (tempFile.exists()) tempFile.delete()
            } finally {
                synchronized(activeDownloads) {
                    activeDownloads.remove(key)
                }
                _downloadingTrackIds.value = _downloadingTrackIds.value - setOf(rawId, "online_$rawId")
                updateTrackDownloadProgress(rawId, null)
                refreshDownloadedSet(appCtx)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(resultFile)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineMusicScreen(
    palette: AmbientPalette = AmbientPalette(),
    lang: AppLanguage = AppLanguage.PERSIAN,
    activePlayingTrackId: String? = null,
    isGlobalPlayerPlaying: Boolean = false,
    bottomPadding: Dp = 120.dp,
    onExternalPlay: ((track: OnlineTrack, streamUrl: String, queue: List<OnlineTrack>) -> Unit)? = null,
    onAddToQueue: ((track: OnlineTrack, streamUrl: String) -> Unit)? = null,
    onDownloadToDeviceLibrary: ((track: Track) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isFa = lang == AppLanguage.PERSIAN

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryTag by remember { mutableStateOf("Travis Scott Playboi Carti Drake") }
    var tracks by remember { mutableStateOf<List<OnlineTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var currentTrack by remember { mutableStateOf<OnlineTrack?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // کش عکس خواننده‌های معروف دنیا برای نمایش در ردیف اسپاتیفای
    val artistPhotoMap = remember { mutableStateMapOf<String, String>() }

    // وضعیت پروفایل خواننده (Spotify Artist Profile Sheet)
    var selectedArtistProfile by remember { mutableStateOf<OnlineArtistProfile?>(null) }
    var artistTopTracks by remember { mutableStateOf<List<OnlineTrack>>(emptyList()) }
    var isLoadingArtistTracks by remember { mutableStateOf(false) }

    // وضعیت پلی‌لیست هوشمند (Smart Playlist Sheet)
    var selectedSmartPlaylist by remember { mutableStateOf<OnlineSmartPlaylist?>(null) }
    var smartPlaylistTracks by remember { mutableStateOf<List<OnlineTrack>>(emptyList()) }
    var isLoadingSmartPlaylist by remember { mutableStateOf(false) }

    var activeLyricsTrack by remember { mutableStateOf<OnlineTrack?>(null) }
    var activeLyricsLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isFetchingLyrics by remember { mutableStateOf(false) }

    val downloadingIds by OnlineCacheHolder.downloadingTrackIds.collectAsState()
    val downloadProgressMap by OnlineCacheHolder.downloadProgressMap.collectAsState()
    val downloadedIds by OnlineCacheHolder.downloadedTrackIds.collectAsState()

    val genZCategories = remember(isFa) {
        listOf(
            "Travis Scott Playboi Carti Drake" to (if (isFa) "🔥 ترند نسل زد" else "🔥 Gen-Z Viral"),
            "Playboi Carti Yeat Don Toliver Billie Eilish" to (if (isFa) "📸 اینستا و تیک‌تاک" else "📸 Reels & TikTok"),
            "Kendrick Lamar The Weeknd Post Malone" to (if (isFa) "▶️ ترند یوتیوب" else "▶️ YouTube Hits"),
            "Travis Scott Future Metro Boomin 21 Savage" to (if (isFa) "🎤 رپرهای معروف" else "🎤 Top Rappers"),
            "Playboi Carti Yeat" to "🧛 Carti & Yeat",
            "Drake Central Cee" to "🦉 Drake & Cench",
            "Hiphopologist Chvrsi Poori Shapoor" to (if (isFa) "💎 رپ فارسی" else "💎 Rap Farsi"),
            "Kordhell Interworld Phonk" to (if (isFa) "⚡ فونک" else "⚡ Phonk")
        )
    }

    val smartPlaylists = remember(tracks, searchQuery) {
        OnlineTasteEngine.buildSmartPlaylists(context, tracks)
    }

    // استخراج خواننده‌های مرتبط با جستجوی فعلی کاربر
    val searchedArtistsFromResults = remember(tracks, searchQuery) {
        if (searchQuery.trim().length < 2) {
            emptyList()
        } else {
            tracks
                .groupBy { it.artist.substringBefore(" feat").substringBefore(" ft.").substringBefore(",").trim() }
                .entries
                .map { (artistName, artistTracks) ->
                    val pic = artistTracks.firstNotNullOfOrNull { it.artistPictureUrl.takeIf { p -> p.isNotBlank() } }
                        ?: OnlineMusicRepository.getArtistPicture(artistName)
                        ?: artistTracks.firstOrNull()?.coverUrl
                        ?: ""
                    OnlineArtistProfile(
                        name = artistName,
                        roleLabelFa = "${artistTracks.size} آهنگ محبوب • ریتینگ بالا",
                        roleLabelEn = "${artistTracks.size} Popular Tracks • Top Rated",
                        defaultQuery = artistName,
                        pictureUrl = pic
                    )
                }
                .take(5)
        }
    }

    val internalPlayer = remember {
        if (onExternalPlay == null) MediaPlayer() else null
    }

    DisposableEffect(internalPlayer) {
        onDispose {
            try {
                internalPlayer?.stop()
                internalPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isPlaying, internalPlayer) {
        while (isActive && isPlaying && internalPlayer != null) {
            try {
                val duration = internalPlayer.duration.coerceAtLeast(1)
                val position = internalPlayer.currentPosition.coerceAtLeast(0)
                progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } catch (_: Exception) {}
            delay(500)
        }
    }

    LaunchedEffect(Unit) {
        OnlineCacheHolder.refreshDownloadedSet(context)
        isLoading = true
        val initial = OnlineMusicRepository.searchCatalog(selectedCategoryTag)
        tracks = initial
        initial.forEach { t ->
            if (t.artistPictureUrl.isNotBlank()) {
                val primary = t.artist.substringBefore(" feat").substringBefore(",").trim()
                artistPhotoMap[primary] = t.artistPictureUrl
            }
        }
        isLoading = false

        // بارگذاری تدریجی عکس خواننده‌های معروف در پس‌زمینه از طریق پروکسی کلودفلر
        for (artist in OnlineMusicRepository.famousGlobalArtists.take(10)) {
            val cachedPic = OnlineMusicRepository.getArtistPicture(artist.name)
            if (!cachedPic.isNullOrBlank()) {
                artistPhotoMap[artist.name] = cachedPic
            } else {
                val res = OnlineMusicRepository.searchCatalog(artist.name)
                val foundPic = res.firstNotNullOfOrNull { it.artistPictureUrl.takeIf { p -> p.isNotBlank() } }
                    ?: res.firstOrNull()?.coverUrl
                if (!foundPic.isNullOrBlank()) {
                    artistPhotoMap[artist.name] = foundPic
                }
            }
        }
    }

    fun triggerPlay(track: OnlineTrack, customQueue: List<OnlineTrack>? = null) {
        currentTrack = track
        // توقف هرگونه پلیر داخلی احتمالی تا پخش به صورت یکپارچه از ثانیه ۰:۰۰ در پلیر اصلی انجام شود
        try {
            if (internalPlayer?.isPlaying == true) {
                internalPlayer.stop()
            }
            internalPlayer?.reset()
        } catch (_: Exception) {}

        val cachedFile = OnlineCacheHolder.getCachedFileIfPresent(context, track.id)
            ?: OnlineCacheHolder.getStreamBufferFileIfPresent(context, track.id)
        val streamUrl = cachedFile?.absolutePath ?: OnlineMusicRepository.buildStreamUrl(track)
        val activeQueue = customQueue?.ifEmpty { listOf(track) } ?: tracks.ifEmpty { listOf(track) }

        if (onExternalPlay != null) {
            onExternalPlay(track, streamUrl, activeQueue)
            return
        }

        if (internalPlayer != null) {
            isBuffering = true
            isPlaying = false
            try {
                internalPlayer.reset()
                internalPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                internalPlayer.setVolume(0f, 0f)
                if (cachedFile != null) {
                    internalPlayer.setDataSource(cachedFile.absolutePath)
                } else {
                    internalPlayer.setDataSource(context, Uri.parse(streamUrl))
                }
                internalPlayer.setOnPreparedListener { mp ->
                    isBuffering = false
                    isPlaying = true
                    try { mp.seekTo(0) } catch (_: Exception) {}
                    mp.setVolume(1f, 1f)
                    mp.start()
                }
                internalPlayer.setOnErrorListener { _, _, _ ->
                    isBuffering = false
                    isPlaying = false
                    true
                }
                internalPlayer.prepareAsync()
            } catch (_: Exception) {
                isBuffering = false
            }
        }
    }

    fun triggerExplicitDownload(track: OnlineTrack) {
        if (track.previewUrl.isNotBlank()) {
            OnlineMusicRepository.registerPreviewUrl(track.id, track.previewUrl)
        }
        val rawUrl = OnlineMusicRepository.buildStreamUrl(track)
        val appTrack = track.toAppTrack(rawUrl)
        if (onDownloadToDeviceLibrary != null) {
            onDownloadToDeviceLibrary(appTrack)
        } else {
            OnlineCacheHolder.downloadTrackExplicitly(context, appTrack) { file ->
                val msg = if (file != null) {
                    if (isFa) "«${track.title}» به طور کامل دانلود شد ✓" else "\"${track.title}\" downloaded ✓"
                } else {
                    if (isFa) "خطا در دانلود آهنگ" else "Download failed"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openArtistProfile(artist: OnlineArtistProfile) {
        selectedArtistProfile = artist
        isLoadingArtistTracks = true
        artistTopTracks = emptyList()
        scope.launch {
            val fetched = OnlineMusicRepository.fetchArtistTopRatedTracks(artist.defaultQuery)
            artistTopTracks = fetched
            val bestPic = fetched.firstNotNullOfOrNull { it.artistPictureUrl.takeIf { p -> p.isNotBlank() } }
                ?: fetched.firstOrNull()?.coverUrl
            if (!bestPic.isNullOrBlank()) {
                artistPhotoMap[artist.name] = bestPic
                selectedArtistProfile = artist.copy(pictureUrl = bestPic)
            }
            isLoadingArtistTracks = false
        }
    }

    fun openSmartPlaylist(playlist: OnlineSmartPlaylist) {
        selectedSmartPlaylist = playlist
        isLoadingSmartPlaylist = true
        smartPlaylistTracks = emptyList()
        scope.launch {
            val fetched = OnlineMusicRepository.searchCatalog(playlist.query)
                .distinctBy { "${it.title.lowercase().trim()}|${it.artist.lowercase().trim()}" }
                .sortedByDescending { it.rank }
                .take(18)
            smartPlaylistTracks = fetched
            isLoadingSmartPlaylist = false
        }
    }

    fun openTrackLyrics(track: OnlineTrack) {
        activeLyricsTrack = track
        isFetchingLyrics = true
        activeLyricsLines = emptyList()
        scope.launch {
            activeLyricsLines = LyricsRepository.fetchLyrics(
                artist = track.artist,
                trackName = track.title,
                durationSec = track.durationSec
            )
            isFetchingLyrics = false
        }
    }

    fun executeSearch(queryText: String) {
        if (queryText.isBlank()) return
        searchJob?.cancel()
        searchJob = scope.launch {
            isLoading = true
            val res = OnlineMusicRepository.searchCatalog(queryText)
            tracks = res
            res.forEach { t ->
                if (t.artistPictureUrl.isNotBlank()) {
                    val primary = t.artist.substringBefore(" feat").substringBefore(",").trim()
                    artistPhotoMap[primary] = t.artistPictureUrl
                }
            }
            if (searchQuery.isNotBlank()) {
                OnlineTasteEngine.recordSearchActivity(context, searchQuery, res)
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = bottomPadding + 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ۱. کادر جستجوی شیشه‌ای و مینیمال (متصل به پروکسی Cloudflare بدون نیاز به فیلترشکن)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(20.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = palette.primary,
                            tintAlpha = 0.16f,
                            borderWidth = 1.dp,
                            secondaryAccent = Color(0xFF1ED760)
                        )
                        .padding(horizontal = 12.dp, vertical = 1.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF1ED760),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = { newValue ->
                                searchQuery = newValue
                                searchJob?.cancel()
                                searchJob = scope.launch {
                                    delay(380)
                                    if (newValue.trim().length >= 2) {
                                        isLoading = true
                                        val res = OnlineMusicRepository.searchCatalog(newValue)
                                        tracks = res
                                        res.forEach { t ->
                                            if (t.artistPictureUrl.isNotBlank()) {
                                                val primary = t.artist.substringBefore(" feat").substringBefore(",").trim()
                                                artistPhotoMap[primary] = t.artistPictureUrl
                                            }
                                        }
                                        OnlineTasteEngine.recordSearchActivity(context, newValue, res)
                                        isLoading = false
                                    } else if (newValue.isBlank()) {
                                        isLoading = true
                                        tracks = OnlineMusicRepository.searchCatalog(selectedCategoryTag)
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(
                                    text = if (isFa) "جستجوی آهنگ، خواننده یا پلی‌لیست..." else "Search songs, artists, mixes...",
                                    color = Color(0xFF9EA6BD),
                                    fontSize = 13.5.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    executeSearch(searchQuery)
                                }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF1ED760)
                            )
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    executeSearch(selectedCategoryTag)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }

            // ۲. دسته‌بندی‌های مینیمال نسل زد
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genZCategories.forEach { (queryKey, displayLabel) ->
                        val isSelected = (searchQuery.isBlank() && selectedCategoryTag == queryKey)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) Color(0xFF1ED760) else Color(0x1FFFFFFF)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    selectedCategoryTag = queryKey
                                    searchQuery = ""
                                    executeSearch(queryKey)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayLabel,
                                color = if (isSelected) Color(0xFF060A08) else Color.White.copy(alpha = 0.9f),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ۳. وقتی کاربر اسم یک آرتیست یا آهنگ را سرچ کرده باشد: نمایش کارت پروفایل آرتیست به سبک اسپاتیفای
            if (searchedArtistsFromResults.isNotEmpty()) {
                item {
                    val primaryMatchedArtist = searchedArtistsFromResults.first()
                    val avatarUrl = primaryMatchedArtist.pictureUrl.ifBlank { artistPhotoMap[primaryMatchedArtist.name] ?: "" }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isFa) "خواننده مرتبط با جستجو" else "Top Matched Artist",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(
                                    shape = RoundedCornerShape(20.dp),
                                    thickness = GlassThickness.REGULAR,
                                    tintColor = Color(0xFF1ED760),
                                    tintAlpha = 0.16f,
                                    borderWidth = 1.dp,
                                    secondaryAccent = palette.primary
                                )
                                .clickable { openArtistProfile(primaryMatchedArtist) }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ArtistAvatarCircle(
                                        imageUrl = avatarUrl,
                                        artistName = primaryMatchedArtist.name,
                                        size = 58.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = primaryMatchedArtist.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = Color(0xFF1ED760),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isFa) "مشاهده محبوب‌ترین آهنگ‌ها (بالاترین ریتینگ)" else "View Top-Rated Songs & Discography",
                                            color = Color(0xFF1ED760),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFF1ED760),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = if (isFa) "پروفایل" else "Profile",
                                        color = Color(0xFF050806),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ۴. بخش خواننده‌های معروف جهان با عکس واقعی خود خواننده‌ها (به سبک اسپاتیفای)
            if (searchQuery.isBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFa) "خواننده‌های معروف جهان 🌍" else "Popular Global Artists 🌍",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isFa) "انتخاب برای مشاهده برترین‌ها" else "Tap for Top Hits",
                                color = Color(0xFF1ED760),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(OnlineMusicRepository.famousGlobalArtists, key = { it.name }) { artist ->
                                val photoUrl = artistPhotoMap[artist.name]
                                    ?: OnlineMusicRepository.getArtistPicture(artist.name)
                                    ?: artist.pictureUrl

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(86.dp)
                                        .clickable { openArtistProfile(artist.copy(pictureUrl = photoUrl)) }
                                ) {
                                    ArtistAvatarCircle(
                                        imageUrl = photoUrl,
                                        artistName = artist.name,
                                        size = 74.dp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = artist.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (isFa) artist.roleLabelFa else artist.roleLabelEn,
                                        color = Color(0xFF9AA3B8),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // ۵. بخش پلی‌لیست‌های هوشمند بر اساس سرچ‌ها و آرتیست‌های مورد علاقه کاربر
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF1ED760),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isFa) "پلی‌لیست‌های هوشمند شما" else "Your Smart Mixes",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                )
                            }
                            Text(
                                text = if (isFa) "پخش و دانلود یکجا" else "Stream & Download",
                                color = Color(0xFF9AA3B8),
                                fontSize = 11.sp
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(smartPlaylists, key = { it.id }) { smartMix ->
                                Box(
                                    modifier = Modifier
                                        .width(172.dp)
                                        .liquidGlass(
                                            shape = RoundedCornerShape(18.dp),
                                            thickness = GlassThickness.THIN,
                                            tintColor = smartMix.accentColor,
                                            tintAlpha = 0.14f,
                                            borderWidth = 0.9.dp
                                        )
                                        .clickable { openSmartPlaylist(smartMix) }
                                        .padding(11.dp)
                                ) {
                                    Column {
                                        SmartPlaylistCollageCover(
                                            coverUrls = smartMix.coverUrls,
                                            accentColor = smartMix.accentColor,
                                            size = 150.dp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (isFa) smartMix.titleFa else smartMix.titleEn,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isFa) smartMix.subtitleFa else smartMix.subtitleEn,
                                            color = Color(0xFFA0A8BE),
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ۶. هدر لیست آهنگ‌ها
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            if (isFa) "نتایج جستجو" else "Search Results"
                        } else {
                            if (isFa) "برترین آهنگ‌های روز" else "Top Trending Tracks"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF1ED760),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ۷. لیست مینیمال و تمیز آهنگ‌ها
            items(tracks, key = { it.id }) { track ->
                val isSelected = (activePlayingTrackId == "online_${track.id}") || (currentTrack?.id == track.id)
                val isCachedOffline = downloadedIds.contains(track.id) ||
                    downloadedIds.contains("online_${track.id}") ||
                    OnlineCacheHolder.isTrackCached(context, track.id)
                val isDownloadingThis = downloadingIds.contains(track.id) ||
                    downloadingIds.contains("online_${track.id}")
                val downloadPercent = downloadProgressMap[track.id] ?: downloadProgressMap["online_${track.id}"]

                OnlineTrackGlassRow(
                    track = track,
                    isSelected = isSelected,
                    isPlaying = isSelected && (isGlobalPlayerPlaying || isPlaying),
                    isCachedOffline = isCachedOffline,
                    isDownloading = isDownloadingThis,
                    downloadProgressPercent = downloadPercent,
                    palette = palette,
                    onClick = { triggerPlay(track) },
                    onArtistClick = {
                        val primaryName = track.artist.substringBefore(" feat").substringBefore(",").trim()
                        openArtistProfile(
                            OnlineArtistProfile(
                                name = primaryName,
                                roleLabelFa = "هنرمند برگزیده",
                                roleLabelEn = "Featured Artist",
                                defaultQuery = primaryName,
                                pictureUrl = track.artistPictureUrl.ifBlank { track.coverUrl }
                            )
                        )
                    },
                    onDownloadClick = { triggerExplicitDownload(track) },
                    onLyricsClick = { openTrackLyrics(track) }
                )
            }
        }

        // =========================================================================
        // شیت پروفایل خواننده به سبک اسپاتیفای (Spotify Artist Profile & Top Rated Songs)
        // =========================================================================
        selectedArtistProfile?.let { artist ->
            ModalBottomSheet(
                onDismissRequest = { selectedArtistProfile = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xF8080B14),
                contentColor = Color.White
            ) {
                val heroPhoto = artist.pictureUrl
                    .ifBlank { artistPhotoMap[artist.name] ?: "" }
                    .ifBlank { artistTopTracks.firstOrNull()?.artistPictureUrl ?: "" }
                    .ifBlank { artistTopTracks.firstOrNull()?.coverUrl ?: "" }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    // هدر پروفایل خواننده
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ArtistAvatarCircle(
                            imageUrl = heroPhoto,
                            artistName = artist.name,
                            size = 84.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF1ED760),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isFa) "هنرمند تاییدشده جهانی" else "VERIFIED GLOBAL ARTIST",
                                    color = Color(0xFF1ED760),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = artist.name,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 23.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isFa) "مرتب‌شده بر اساس بالاترین ریتینگ جهانی ⭐" else "Sorted by Highest Global Rating ⭐",
                                color = Color(0xFFA0A8C0),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // دکمه‌های پخش همه و دانلود گلچین خواننده
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val first = artistTopTracks.firstOrNull()
                                if (first != null) {
                                    triggerPlay(first, artistTopTracks)
                                    selectedArtistProfile = null
                                }
                            },
                            enabled = artistTopTracks.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF060A08),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "پخش محبوب‌ترین‌ها" else "Play Top Hits",
                                color = Color(0xFF060A08),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                artistTopTracks.take(5).forEach { t ->
                                    triggerExplicitDownload(t)
                                }
                                Toast.makeText(
                                    context,
                                    if (isFa) "دانلود ۵ آهنگ برتر ${artist.name} آغاز شد" else "Downloading top 5 songs...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            enabled = artistTopTracks.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF1ED760).copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color(0xFF1ED760),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "دانلود ۵ برتر" else "Download Top 5",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLoadingArtistTracks) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1ED760))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 430.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            itemsIndexed(artistTopTracks, key = { _, t -> "artist_top_${t.id}" }) { index, track ->
                                val isSelected = (activePlayingTrackId == "online_${track.id}") || (currentTrack?.id == track.id)
                                val isCachedOffline = downloadedIds.contains(track.id) ||
                                    downloadedIds.contains("online_${track.id}") ||
                                    OnlineCacheHolder.isTrackCached(context, track.id)
                                val isDownloadingThis = downloadingIds.contains(track.id) ||
                                    downloadingIds.contains("online_${track.id}")
                                val downloadPercent = downloadProgressMap[track.id] ?: downloadProgressMap["online_${track.id}"]

                                OnlineTrackGlassRow(
                                    track = track,
                                    rankBadgeNumber = index + 1,
                                    showRatingBadge = true,
                                    isSelected = isSelected,
                                    isPlaying = isSelected && (isGlobalPlayerPlaying || isPlaying),
                                    isCachedOffline = isCachedOffline,
                                    isDownloading = isDownloadingThis,
                                    downloadProgressPercent = downloadPercent,
                                    palette = palette,
                                    onClick = {
                                        triggerPlay(track, artistTopTracks)
                                    },
                                    onDownloadClick = { triggerExplicitDownload(track) },
                                    onLyricsClick = { openTrackLyrics(track) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // شیت پلی‌لیست هوشمند (Smart Playlist Modal Sheet)
        // =========================================================================
        selectedSmartPlaylist?.let { playlist ->
            ModalBottomSheet(
                onDismissRequest = { selectedSmartPlaylist = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xF8080B14),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SmartPlaylistCollageCover(
                            coverUrls = playlist.coverUrls.ifEmpty { smartPlaylistTracks.map { it.coverUrl }.take(4) },
                            accentColor = playlist.accentColor,
                            size = 82.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = playlist.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isFa) "پلی‌لیست هوشمند اختصاصی" else "SMART AUTO-PLAYLIST",
                                    color = playlist.accentColor,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isFa) playlist.titleFa else playlist.titleEn,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isFa) playlist.subtitleFa else playlist.subtitleEn,
                                color = Color(0xFFA0A8C0),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val first = smartPlaylistTracks.firstOrNull()
                                if (first != null) {
                                    triggerPlay(first, smartPlaylistTracks)
                                    selectedSmartPlaylist = null
                                }
                            },
                            enabled = smartPlaylistTracks.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = playlist.accentColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF060A08),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "پخش کامل پلی‌لیست" else "Play All",
                                color = Color(0xFF060A08),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                smartPlaylistTracks.take(8).forEach { t ->
                                    triggerExplicitDownload(t)
                                }
                                Toast.makeText(
                                    context,
                                    if (isFa) "دانلود آهنگ‌های پلی‌لیست آغاز شد" else "Downloading playlist tracks...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            enabled = smartPlaylistTracks.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, playlist.accentColor.copy(alpha = 0.65f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = playlist.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFa) "دانلود همه" else "Download All",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isLoadingSmartPlaylist) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = playlist.accentColor)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 430.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            itemsIndexed(smartPlaylistTracks, key = { _, t -> "smart_pl_${t.id}" }) { index, track ->
                                val isSelected = (activePlayingTrackId == "online_${track.id}") || (currentTrack?.id == track.id)
                                val isCachedOffline = downloadedIds.contains(track.id) ||
                                    downloadedIds.contains("online_${track.id}") ||
                                    OnlineCacheHolder.isTrackCached(context, track.id)
                                val isDownloadingThis = downloadingIds.contains(track.id) ||
                                    downloadingIds.contains("online_${track.id}")
                                val downloadPercent = downloadProgressMap[track.id] ?: downloadProgressMap["online_${track.id}"]

                                OnlineTrackGlassRow(
                                    track = track,
                                    rankBadgeNumber = index + 1,
                                    showRatingBadge = true,
                                    isSelected = isSelected,
                                    isPlaying = isSelected && (isGlobalPlayerPlaying || isPlaying),
                                    isCachedOffline = isCachedOffline,
                                    isDownloading = isDownloadingThis,
                                    downloadProgressPercent = downloadPercent,
                                    palette = palette,
                                    onClick = {
                                        triggerPlay(track, smartPlaylistTracks)
                                    },
                                    onDownloadClick = { triggerExplicitDownload(track) },
                                    onLyricsClick = { openTrackLyrics(track) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // شیت متن آهنگ (LRCLIB Synced Lyrics)
        activeLyricsTrack?.let { lyricTrack ->
            ModalBottomSheet(
                onDismissRequest = { activeLyricsTrack = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xF50B0E1A),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CoverArtBox(
                            coverUrl = lyricTrack.coverUrl,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lyricTrack.title,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = lyricTrack.artist,
                                color = Color(0xFF1ED760),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isFetchingLyrics) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1ED760))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 440.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeLyricsLines) { line ->
                                val timeTag = if (line.timeMs > 0L) {
                                    val m = (line.timeMs / 60000L)
                                    val s = (line.timeMs % 60000L) / 1000L
                                    "%d:%02d".format(m, s)
                                } else null

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x14FFFFFF))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (timeTag != null) {
                                        Text(
                                            text = timeTag,
                                            color = Color(0xFF1ED760),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.width(40.dp)
                                        )
                                    }
                                    Text(
                                        text = line.text,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        textAlign = if (timeTag == null) TextAlign.Center else TextAlign.Start,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = currentTrack != null && onExternalPlay == null,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            currentTrack?.let { activeTrack ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    color = Color(0xF00B0E18)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (isBuffering) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF1ED760)
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF1ED760)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoverArtBox(
                                coverUrl = activeTrack.coverUrl,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = activeTrack.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA5ABC0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    internalPlayer?.let { mp ->
                                        if (mp.isPlaying) {
                                            mp.pause()
                                            isPlaying = false
                                        } else {
                                            mp.start()
                                            isPlaying = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF1ED760), CircleShape)
                            ) {
                                if (isPlaying) {
                                    PauseGlyph(tint = Color(0xFF060A08))
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color(0xFF060A08)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistAvatarCircle(
    imageUrl: String,
    artistName: String,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(8.dp, CircleShape, spotColor = Color(0xFF1ED760))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(1.5.dp, Color(0xFF1ED760).copy(alpha = 0.65f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = artistName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = artistName,
                tint = Color(0xFF1ED760),
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

@Composable
private fun SmartPlaylistCollageCover(
    coverUrls: List<String>,
    accentColor: Color,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(accentColor.copy(alpha = 0.35f), Color(0xFF0B0F1A))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
    ) {
        if (coverUrls.size >= 4) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CoverArtBox(coverUrl = coverUrls[0], modifier = Modifier.weight(1f).fillMaxHeight())
                    CoverArtBox(coverUrl = coverUrls[1], modifier = Modifier.weight(1f).fillMaxHeight())
                }
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CoverArtBox(coverUrl = coverUrls[2], modifier = Modifier.weight(1f).fillMaxHeight())
                    CoverArtBox(coverUrl = coverUrls[3], modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        } else if (coverUrls.isNotEmpty()) {
            CoverArtBox(coverUrl = coverUrls.first(), modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(size * 0.4f)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play Mix",
                tint = Color(0xFF050806),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OnlineTrackGlassRow(
    track: OnlineTrack,
    rankBadgeNumber: Int? = null,
    showRatingBadge: Boolean = false,
    isSelected: Boolean,
    isPlaying: Boolean,
    isCachedOffline: Boolean,
    isDownloading: Boolean,
    downloadProgressPercent: Int? = null,
    palette: AmbientPalette,
    onClick: () -> Unit,
    onArtistClick: (() -> Unit)? = null,
    onDownloadClick: () -> Unit,
    onLyricsClick: () -> Unit
) {
    val mins = track.durationSec / 60
    val secs = track.durationSec % 60
    val durationFormatted = "%d:%02d".format(mins, secs)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(16.dp),
                thickness = if (isSelected) GlassThickness.REGULAR else GlassThickness.THIN,
                tintColor = if (isSelected) Color(0xFF1ED760) else palette.primary,
                tintAlpha = if (isSelected) 0.22f else 0.08f,
                borderWidth = if (isSelected) 1.2.dp else 0.6.dp,
                secondaryAccent = Color(0xFF1ED760)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rankBadgeNumber != null) {
                Text(
                    text = "#$rankBadgeNumber",
                    color = if (rankBadgeNumber <= 3) Color(0xFF1ED760) else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.5.sp,
                    modifier = Modifier.width(26.dp)
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CoverArtBox(
                    coverUrl = track.coverUrl,
                    modifier = Modifier
                        .size(50.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        MiniPlayingEqualizerBars(isPlaying = isPlaying)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color(0xFF1ED760) else Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${track.artist} • $durationFormatted",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFA6B0C6),
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (onArtistClick != null) {
                            Modifier.clickable { onArtistClick() }
                        } else {
                            Modifier
                        }
                    )

                    if (showRatingBadge) {
                        Surface(
                            color = Color(0x26FBBF24),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = track.ratingScoreText,
                                    color = Color(0xFFFBBF24),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // دکمه لیریک
            IconButton(
                onClick = onLyricsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = "Lyrics",
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // دکمه دانلود کامل به فضای گوشی (همراه با نمایش درصد ۰ تا ۱۰۰٪)
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(42.dp)
            ) {
                when {
                    isDownloading -> {
                        val pct = (downloadProgressPercent ?: 0).coerceIn(0, 100)
                        Box(contentAlignment = Alignment.Center) {
                            if (pct > 0) {
                                CircularProgressIndicator(
                                    progress = { pct / 100f },
                                    color = Color(0xFF1ED760),
                                    trackColor = Color.White.copy(alpha = 0.16f),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                CircularProgressIndicator(
                                    color = Color(0xFF1ED760),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = "$pct%",
                                color = Color(0xFF1ED760),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    isCachedOffline -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = Color(0xFF1ED760),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPlayingEqualizerBars(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "OnlineEq")
    val b1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(240, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val b2 by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(180, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val b3 by transition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(290, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b3"
    )
    val heights = if (isPlaying) listOf(b1, b2, b3) else listOf(0.3f, 0.3f, 0.3f)

    Row(
        modifier = Modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { ratio ->
            Box(
                modifier = Modifier
                    .width(3.2.dp)
                    .fillMaxHeight(ratio)
                    .clip(CircleShape)
                    .background(Color(0xFF1ED760))
            )
        }
    }
}

@Composable
private fun CoverArtBox(
    coverUrl: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(Color(0xFF1A2234), Color(0xFF0B0F1A))
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl.isNotBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = "Album Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF1ED760),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PauseGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val barWidth = size.width * 0.28f
        val barHeight = size.height * 0.75f
        val topOffset = (size.height - barHeight) / 2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.15f, topOffset),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.57f, topOffset),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}
