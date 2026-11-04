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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
    val previewUrl: String = ""
) {
    fun toAppTrack(streamUrl: String): Track {
        if (previewUrl.isNotBlank()) {
            OnlineMusicRepository.registerPreviewUrl(id, previewUrl)
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

object OnlineMusicRepository {
    // آدرس سرور اختصاصی Cloudflare Workers کاربر
    const val WORKER_URL = "https://alubm.parsaghorbani8861.workers.dev"

    private val imageMemoryCache = object : LruCache<String, Bitmap>(80) {}
    private val catalogCache = object : LruCache<String, List<OnlineTrack>>(40) {}
    private val previewUrlByTrackId = ConcurrentHashMap<String, String>()

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

    fun getFallbackStreamUrl(track: Track): String? {
        val preview = getPreviewUrl(track.id)
        if (!preview.isNullOrBlank() && track.uri != preview) {
            return preview
        }
        val workerUrl = buildStreamUrlForTitleArtist(track.artist, track.title)
        if (track.uri != workerUrl && track.uri.startsWith("http")) {
            return workerUrl
        }
        return null
    }

    suspend fun resolvePreviewUrlIfNeeded(track: Track): String? = withContext(Dispatchers.IO) {
        getPreviewUrl(track.id)?.let { if (it.isNotBlank()) return@withContext it }
        val results = searchCatalog("${track.artist} ${track.title}")
        val match = results.firstOrNull { it.previewUrl.isNotBlank() }
        match?.previewUrl?.also { registerPreviewUrl(track.id, it) }
    }

    suspend fun searchCatalog(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()
        catalogCache.get(cleanQuery.lowercase())?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        val results = mutableListOf<OnlineTrack>()
        var conn: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val url = URL("https://api.deezer.com/search?q=$encoded&limit=35")
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode in 200..299) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonText)
                val dataArray = root.optJSONArray("data")
                if (dataArray != null) {
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val artistObj = item.optJSONObject("artist")
                        val albumObj = item.optJSONObject("album")

                        val id = item.optLong("id").toString()
                        val title = item.optString("title", "Unknown")
                        val artistName = artistObj?.optString("name", "Unknown") ?: "Unknown"
                        val albumTitle = albumObj?.optString("title", "Single") ?: "Single"
                        val cover = albumObj?.optString("cover_xl")?.takeIf { it.isNotBlank() }
                            ?: albumObj?.optString("cover_big")?.takeIf { it.isNotBlank() }
                            ?: albumObj?.optString("cover_medium")
                            ?: ""
                        val duration = item.optInt("duration", 180)
                        val preview = item.optString("preview", "")

                        if (preview.isNotBlank()) {
                            registerPreviewUrl(id, preview)
                        }

                        results.add(
                            OnlineTrack(
                                id = id,
                                title = title,
                                artist = artistName,
                                album = albumTitle,
                                coverUrl = cover,
                                durationSec = duration,
                                previewUrl = preview
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.disconnect()
        }

        if (results.isNotEmpty()) {
            catalogCache.put(cleanQuery.lowercase(), results)
        }
        results
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
        if (track.previewUrl.isNotBlank()) {
            registerPreviewUrl(track.id, track.previewUrl)
        }
        val searchKeyword = URLEncoder.encode("${track.artist} - ${track.title} audio", "UTF-8")
        return "$WORKER_URL/?q=$searchKeyword"
    }

    fun buildStreamUrlForTitleArtist(artist: String, title: String): String {
        val searchKeyword = URLEncoder.encode("$artist - $title audio", "UTF-8")
        return "$WORKER_URL/?q=$searchKeyword"
    }
}

/**
 * موتور هوشمند یادگیری سلیقه کاربر + ترندهای نسل زد (تیک‌تاک، اینستاگرام، یوتیوب و رپرهای معروف)
 */
object OnlineTasteEngine {
    private const val PREFS_NAME = "aura_online_taste_prefs"
    private const val KEY_ARTIST_SCORES = "artist_scores_json"
    private const val KEY_STYLE_SCORES = "style_scores_json"
    private const val KEY_RECENT_ONLINE_TRACKS = "recent_online_tracks_json"

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
            .take(5)
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

    /**
     * فید ترندهای نسل زد (تیک‌تاک، اینستاگرام، یوتیوب، رپرهای معروف، فارسی و شخصی‌سازی با سلیقه کاربر)
     */
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
 * مدیریت دانلود انتخابی آهنگ‌ها در فضای گوشی (با فال‌بک خودکار دوگانه تا دانلود هیچ‌وقت خطا ندهد)
 */
object OnlineCacheHolder {
    private const val MIN_VALID_AUDIO_BYTES = 8_192L
    private val cacheScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = HashSet<String>()

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    private val _downloadedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedTrackIds: StateFlow<Set<String>> = _downloadedTrackIds.asStateFlow()

    fun getCacheDir(context: Context): File {
        val dir = File(context.filesDir, "aura_downloaded_music")
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
        getCacheDir(context).listFiles()?.forEach { f ->
            if (f.isFile && f.length() > MIN_VALID_AUDIO_BYTES) {
                val rawId = f.name.removePrefix("aura_track_").removeSuffix(".mp3")
                ids.add(rawId)
                ids.add("online_$rawId")
            }
        }
        getLegacyCacheDir(context).listFiles()?.forEach { f ->
            if (f.isFile && f.length() > MIN_VALID_AUDIO_BYTES) {
                val rawId = f.name.removePrefix("aura_track_").removeSuffix(".mp3")
                ids.add(rawId)
                ids.add("online_$rawId")
            }
        }
        _downloadedTrackIds.value = ids
    }

    fun isTrackCached(context: Context, trackId: String): Boolean {
        val key = normalizeKey(trackId)
        val primary = File(getCacheDir(context), key)
        if (primary.exists() && primary.length() > MIN_VALID_AUDIO_BYTES) return true
        val legacy = File(getLegacyCacheDir(context), key)
        return legacy.exists() && legacy.length() > MIN_VALID_AUDIO_BYTES
    }

    fun getCachedFileIfPresent(context: Context, trackId: String): File? {
        val key = normalizeKey(trackId)
        val primary = File(getCacheDir(context), key)
        if (primary.exists() && primary.length() > MIN_VALID_AUDIO_BYTES) {
            primary.setLastModified(System.currentTimeMillis())
            return primary
        }
        val legacy = File(getLegacyCacheDir(context), key)
        return if (legacy.exists() && legacy.length() > MIN_VALID_AUDIO_BYTES) {
            legacy.setLastModified(System.currentTimeMillis())
            legacy
        } else null
    }

    fun getCurrentCacheSizeBytes(context: Context): Long {
        val s1 = getCacheDir(context).listFiles()?.sumOf { it.length() } ?: 0L
        val s2 = getLegacyCacheDir(context).listFiles()?.sumOf { it.length() } ?: 0L
        return s1 + s2
    }

    fun getCachedTracksCount(context: Context): Int {
        val c1 = getCacheDir(context).listFiles()?.count { it.isFile && it.length() > MIN_VALID_AUDIO_BYTES } ?: 0
        val c2 = getLegacyCacheDir(context).listFiles()?.count { it.isFile && it.length() > MIN_VALID_AUDIO_BYTES } ?: 0
        return c1 + c2
    }

    private fun downloadUrlToTempFile(urlStr: String, tempFile: File, depth: Int = 0): Boolean {
        if (depth > 5 || urlStr.isBlank()) return false
        var currentUrl = urlStr
        var redirects = 0

        while (redirects < 6) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 20000
                    readTimeout = 35000
                    requestMethod = "GET"
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    setRequestProperty("Accept", "*/*")
                }

                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
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

                    if (tempFile.exists()) tempFile.delete()
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buf = ByteArray(16 * 1024)
                            var read: Int
                            while (input.read(buf).also { read = it } != -1) {
                                output.write(buf, 0, read)
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
                                return downloadUrlToTempFile(innerUrl, tempFile, depth + 1)
                            }
                        } else if (text.startsWith("http://") || text.startsWith("https://")) {
                            return downloadUrlToTempFile(text.lines().first().trim(), tempFile, depth + 1)
                        }
                        return false
                    }

                    if (tempFile.exists() && tempFile.length() > MIN_VALID_AUDIO_BYTES) {
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
     * دانلود صریح و انتخابی آهنگ توسط کاربر برای اضافه شدن دائمی به آهنگ‌های فضای گوشی
     */
    fun downloadTrackExplicitly(
        context: Context,
        track: Track,
        onComplete: ((File?) -> Unit)? = null
    ) {
        val rawId = track.id.removePrefix("online_")
        val key = normalizeKey(rawId)
        val existing = getCachedFileIfPresent(context, rawId)
        if (existing != null) {
            refreshDownloadedSet(context)
            onComplete?.invoke(existing)
            return
        }

        synchronized(activeDownloads) {
            if (activeDownloads.contains(key)) return
            activeDownloads.add(key)
        }
        _downloadingTrackIds.value = _downloadingTrackIds.value + setOf(rawId, "online_$rawId")

        val appCtx = context.applicationContext
        cacheScope.launch {
            val cacheDir = getCacheDir(appCtx)
            val tempFile = File(cacheDir, "$key.tmp")
            val finalFile = File(cacheDir, key)
            var resultFile: File? = null

            try {
                val primaryUrl = if (track.uri.startsWith("http")) {
                    track.uri
                } else {
                    OnlineMusicRepository.buildStreamUrlForTitleArtist(track.artist, track.title)
                }

                // تلاش اول: دانلود از سرور کلودفلر کاربر
                var downloaded = downloadUrlToTempFile(primaryUrl, tempFile)

                // تلاش دوم (فال‌بک تضمینی): اگر سرور کلودفلر برای این قطعه پاسخ نداد، مستقیماً از لینک صوتی CDN دانلود کن
                if (!downloaded) {
                    val fallbackPreview = OnlineMusicRepository.resolvePreviewUrlIfNeeded(track)
                    if (!fallbackPreview.isNullOrBlank() && fallbackPreview != primaryUrl) {
                        downloaded = downloadUrlToTempFile(fallbackPreview, tempFile)
                    }
                }

                if (downloaded && tempFile.exists() && tempFile.length() > MIN_VALID_AUDIO_BYTES) {
                    if (finalFile.exists()) finalFile.delete()
                    val moved = tempFile.renameTo(finalFile)
                    if (!moved) {
                        tempFile.copyTo(finalFile, overwrite = true)
                        tempFile.delete()
                    }
                    if (finalFile.exists() && finalFile.length() > MIN_VALID_AUDIO_BYTES) {
                        resultFile = finalFile
                    }
                } else {
                    if (tempFile.exists()) tempFile.delete()
                }
            } catch (_: Exception) {
                if (tempFile.exists()) tempFile.delete()
            } finally {
                synchronized(activeDownloads) {
                    activeDownloads.remove(key)
                }
                _downloadingTrackIds.value = _downloadingTrackIds.value - setOf(rawId, "online_$rawId")
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

    var activeLyricsTrack by remember { mutableStateOf<OnlineTrack?>(null) }
    var activeLyricsLines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var isFetchingLyrics by remember { mutableStateOf(false) }

    val downloadingIds by OnlineCacheHolder.downloadingTrackIds.collectAsState()
    val downloadedIds by OnlineCacheHolder.downloadedTrackIds.collectAsState()

    // دسته‌بندی‌های مینیمال و دقیق مخصوص نسل زد (تیک‌تاک، اینستاگرام، یوتیوب، رپرهای معروف و فارسی)
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
        tracks = OnlineMusicRepository.searchCatalog(selectedCategoryTag)
        isLoading = false
    }

    // پخش فوری و مستقیم آهنگ با یک کلیک (بدون تاخیر و بدون اشغال حافظه گوشی)
    fun triggerPlay(track: OnlineTrack) {
        currentTrack = track
        if (track.previewUrl.isNotBlank()) {
            OnlineMusicRepository.registerPreviewUrl(track.id, track.previewUrl)
        }
        val cachedFile = OnlineCacheHolder.getCachedFileIfPresent(context, track.id)
        val streamUrl = cachedFile?.absolutePath ?: OnlineMusicRepository.buildStreamUrl(track)

        if (onExternalPlay != null) {
            onExternalPlay(track, streamUrl, tracks.ifEmpty { listOf(track) })
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
                if (cachedFile != null) {
                    internalPlayer.setDataSource(cachedFile.absolutePath)
                } else {
                    internalPlayer.setDataSource(context, Uri.parse(streamUrl))
                }
                internalPlayer.setOnPreparedListener {
                    isBuffering = false
                    isPlaying = true
                    it.start()
                }
                internalPlayer.setOnErrorListener { mp, _, _ ->
                    if (track.previewUrl.isNotBlank() && streamUrl != track.previewUrl) {
                        try {
                            mp.reset()
                            mp.setDataSource(context, Uri.parse(track.previewUrl))
                            mp.setOnPreparedListener { p ->
                                isBuffering = false
                                isPlaying = true
                                p.start()
                            }
                            mp.setOnErrorListener { _, _, _ ->
                                isBuffering = false
                                isPlaying = false
                                true
                            }
                            mp.prepareAsync()
                        } catch (_: Exception) {
                            isBuffering = false
                            isPlaying = false
                        }
                    } else {
                        isBuffering = false
                        isPlaying = false
                    }
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
                    if (isFa) "«${track.title}» دانلود شد ✓" else "\"${track.title}\" downloaded ✓"
                } else {
                    if (isFa) "خطا در دانلود آهنگ" else "Download failed"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
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
            tracks = OnlineMusicRepository.searchCatalog(queryText)
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 6.dp, bottom = bottomPadding + 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ۱. کادر جستجوی مینیمال و شیشه‌ای
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
                                        tracks = OnlineMusicRepository.searchCatalog(newValue)
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
                                    text = if (isFa) "جستجوی آهنگ، رپر یا ترند..." else "Search songs, rappers, trends...",
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

            // ۲. دسته‌بندی‌های مینیمال نسل زد (تیک‌تاک، اینستا، یوتیوب، رپرها)
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
                                    if (isSelected) {
                                        Color(0xFF1ED760)
                                    } else {
                                        Color(0x1FFFFFFF)
                                    }
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

            // ۳. نشانگر لودینگ مینیمال
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1ED760),
                            strokeWidth = 2.4.dp,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // ۴. لیست مینیمال و تمیز آهنگ‌ها
            items(tracks, key = { it.id }) { track ->
                val isSelected = (activePlayingTrackId == "online_${track.id}") || (currentTrack?.id == track.id)
                val isCachedOffline = downloadedIds.contains(track.id) ||
                    downloadedIds.contains("online_${track.id}") ||
                    OnlineCacheHolder.isTrackCached(context, track.id)
                val isDownloadingThis = downloadingIds.contains(track.id) ||
                    downloadingIds.contains("online_${track.id}")

                OnlineTrackGlassRow(
                    track = track,
                    isSelected = isSelected,
                    isPlaying = isSelected && (isGlobalPlayerPlaying || isPlaying),
                    isCachedOffline = isCachedOffline,
                    isDownloading = isDownloadingThis,
                    palette = palette,
                    onClick = { triggerPlay(track) },
                    onDownloadClick = { triggerExplicitDownload(track) },
                    onLyricsClick = { openTrackLyrics(track) }
                )
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
private fun OnlineTrackGlassRow(
    track: OnlineTrack,
    isSelected: Boolean,
    isPlaying: Boolean,
    isCachedOffline: Boolean,
    isDownloading: Boolean,
    palette: AmbientPalette,
    onClick: () -> Unit,
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

                Text(
                    text = "${track.artist} • $durationFormatted",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFA6B0C6),
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

            // دکمه دانلود به فضای گوشی
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(36.dp)
            ) {
                when {
                    isDownloading -> {
                        CircularProgressIndicator(
                            color = Color(0xFF1ED760),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(17.dp)
                        )
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
