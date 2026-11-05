package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.LruCache
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.audio.AuraDspController
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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val WORKER_URL = "https://alubm.parsaghorbani8861.workers.dev"

/**
 * مدل خواننده دریافتی از سرور کلادفلر (آرشیو تلگرام)
 */
data class ArchiveArtistItem(
    val name: String,
    val trackCount: Int = 0,
    val cover: String = ""
)

/**
 * پاسخ کامل اندپوینت `$WORKER_URL/tracks?q=...&artist=...`
 */
data class WorkerTracksResponse(
    val artists: List<ArchiveArtistItem> = emptyList(),
    val matchedArtists: List<ArchiveArtistItem> = emptyList(),
    val tracks: List<OnlineTrack> = emptyList()
)

data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Single",
    val coverUrl: String = "",
    val durationSec: Int = 180,
    val streamUrl: String = "",
    val downloadUrl: String = "",
    val previewUrl: String = "",
    val artistId: String = "",
    val artistPictureUrl: String = "",
    val rank: Int = 500_000
) {
    val artist_name: String get() = artist
    val duration: Int get() = durationSec
    val stream_url: String get() = streamUrl
    val download_url: String get() = downloadUrl
    val cover_url: String get() = coverUrl

    fun resolvedStreamUrl(): String {
        return OnlineMusicRepository.resolveWorkerUrl(streamUrl).ifBlank {
            OnlineMusicRepository.buildStreamUrl(this)
        }
    }

    fun resolvedDownloadUrl(): String {
        return OnlineMusicRepository.resolveWorkerUrl(downloadUrl).ifBlank {
            OnlineMusicRepository.resolveWorkerUrl(streamUrl).ifBlank {
                OnlineMusicRepository.buildDownloadUrl(id, artist, title)
            }
        }
    }

    fun resolvedCoverUrl(): String {
        return OnlineMusicRepository.normalizeWorkerImageUrl(coverUrl).ifBlank {
            OnlineMusicRepository.getCoverUrlForTrack(id, artist)
        }
    }

    fun toAppTrack(customStreamUrl: String = ""): Track {
        val httpStream = resolvedStreamUrl()
        val finalStream = customStreamUrl.ifBlank { httpStream }
        val finalDownload = resolvedDownloadUrl()
        val finalCover = resolvedCoverUrl()
        OnlineMusicRepository.registerTrackUrls(
            trackId = id,
            streamUrl = httpStream,
            downloadUrl = finalDownload,
            coverUrl = finalCover,
            artistName = artist
        )
        return Track(
            id = if (id.startsWith("online_")) id else "online_$id",
            title = title,
            artist = artist,
            album = album.ifBlank { "Single" },
            durationMs = (durationSec.coerceAtLeast(1) * 1000L),
            uri = finalStream,
            artworkUri = finalCover.ifBlank { null },
            bitrate = 320,
            sampleRate = 48000,
            genre = "Online"
        )
    }
}

data class OnlineArtistProfile(
    val name: String,
    val roleLabelFa: String = "آرشیو اختصاصی",
    val roleLabelEn: String = "Featured Artist",
    val defaultQuery: String = name,
    val pictureUrl: String = "",
    val trackCount: Int = 0
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

/**
 * مخزن ارتباط مستقیم با سرور Cloudflare Worker متصل به کانال آرشیو تلگرام
 */
object OnlineMusicRepository {
    const val WORKER_URL = com.example.WORKER_URL
    val BASE_URL: String get() = WORKER_URL.trimEnd('/')

    private val imageMemoryCache = object : LruCache<String, Bitmap>(110) {}
    private val streamUrlByTrackId = ConcurrentHashMap<String, String>()
    private val downloadUrlByTrackId = ConcurrentHashMap<String, String>()
    private val coverUrlByTrackId = ConcurrentHashMap<String, String>()
    private val artistPictureCache = ConcurrentHashMap<String, String>()
    @Volatile
    private var lastKnownArtists: List<ArchiveArtistItem> = emptyList()

    fun resolveWorkerUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("/") -> "$BASE_URL$trimmed"
            else -> "$BASE_URL/$trimmed"
        }
    }

    fun normalizeWorkerImageUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("file://") || trimmed.startsWith("content://") -> trimmed
            trimmed.startsWith("/storage/") || trimmed.startsWith("/data/") || trimmed.startsWith("/sdcard/") -> trimmed
            trimmed.startsWith("/img?u=") -> "$BASE_URL$trimmed"
            trimmed.startsWith("/") -> "$BASE_URL$trimmed"
            else -> "$BASE_URL/$trimmed"
        }
    }

    fun registerTrackUrls(
        trackId: String,
        streamUrl: String,
        downloadUrl: String,
        coverUrl: String = "",
        artistName: String = ""
    ) {
        val rawId = trackId.removePrefix("online_").trim()
        if (rawId.isBlank()) return
        if (streamUrl.isNotBlank() && (streamUrl.startsWith("http") || streamUrl.startsWith("/"))) {
            if (!streamUrl.startsWith("/storage/") && !streamUrl.startsWith("/data/")) {
                val resolved = resolveWorkerUrl(streamUrl)
                streamUrlByTrackId[rawId] = resolved
                streamUrlByTrackId["online_$rawId"] = resolved
            }
        }
        if (downloadUrl.isNotBlank() && (downloadUrl.startsWith("http") || downloadUrl.startsWith("/"))) {
            if (!downloadUrl.startsWith("/storage/") && !downloadUrl.startsWith("/data/")) {
                val resolvedDl = resolveWorkerUrl(downloadUrl)
                downloadUrlByTrackId[rawId] = resolvedDl
                downloadUrlByTrackId["online_$rawId"] = resolvedDl
            }
        }
        if (coverUrl.isNotBlank()) {
            val resolvedCover = normalizeWorkerImageUrl(coverUrl)
            coverUrlByTrackId[rawId] = resolvedCover
            coverUrlByTrackId["online_$rawId"] = resolvedCover
            if (artistName.isNotBlank()) {
                registerArtistPicture(artistName, resolvedCover)
            }
        }
    }

    fun registerCoverUrlForTrack(trackId: String, coverUrl: String) {
        if (coverUrl.isBlank()) return
        val rawId = trackId.removePrefix("online_").trim()
        if (rawId.isBlank()) return
        val normalized = normalizeWorkerImageUrl(coverUrl)
        coverUrlByTrackId[rawId] = normalized
        coverUrlByTrackId["online_$rawId"] = normalized
    }

    fun getCoverUrlForTrack(trackId: String, artistName: String = ""): String {
        val rawId = trackId.removePrefix("online_").trim()
        if (rawId.isNotBlank()) {
            coverUrlByTrackId[rawId]?.takeIf { it.isNotBlank() }?.let { return it }
            coverUrlByTrackId["online_$rawId"]?.takeIf { it.isNotBlank() }?.let { return it }
        }
        if (artistName.isNotBlank()) {
            getArtistPicture(artistName)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return ""
    }

    fun registerPreviewUrl(trackId: String, previewUrl: String) {
        if (previewUrl.isBlank()) return
        val rawId = trackId.removePrefix("online_")
        streamUrlByTrackId[rawId] = resolveWorkerUrl(previewUrl)
        streamUrlByTrackId["online_$rawId"] = resolveWorkerUrl(previewUrl)
    }

    fun getPreviewUrl(trackId: String): String? {
        val rawId = trackId.removePrefix("online_")
        return streamUrlByTrackId[rawId] ?: streamUrlByTrackId["online_$rawId"]
    }

    fun getDownloadUrlForTrack(trackId: String): String? {
        val rawId = trackId.removePrefix("online_")
        return downloadUrlByTrackId[rawId] ?: downloadUrlByTrackId["online_$rawId"]
    }

    fun registerArtistPicture(artistName: String, pictureUrl: String) {
        if (artistName.isBlank() || pictureUrl.isBlank()) return
        artistPictureCache[artistName.trim().lowercase()] = normalizeWorkerImageUrl(pictureUrl)
    }

    fun getArtistPicture(artistName: String): String? {
        return artistPictureCache[artistName.trim().lowercase()]
    }

    fun getCachedCoverBitmap(url: String): Bitmap? {
        if (url.isBlank()) return null
        val normalized = normalizeWorkerImageUrl(url)
        return imageMemoryCache.get(normalized) ?: imageMemoryCache.get(url)
    }

    fun cacheCoverBitmap(url: String, bitmap: Bitmap) {
        if (url.isBlank()) return
        val normalized = normalizeWorkerImageUrl(url)
        imageMemoryCache.put(normalized, bitmap)
        imageMemoryCache.put(url, bitmap)
    }

    /**
     * فراخوانی مستقیم API سرور کلادفلر:
     * `$WORKER_URL/tracks?q=$encodedQuery&artist=$encodedArtist`
     */
    suspend fun fetchFromWorker(
        query: String = "",
        artist: String = ""
    ): WorkerTracksResponse = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val encodedArtist = URLEncoder.encode(artist.trim(), "UTF-8")
            val endpoint = "$BASE_URL/tracks?q=$encodedQuery&artist=$encodedArtist"
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (connection.responseCode in 200..299) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                return@withContext parseWorkerTracksJson(body)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        WorkerTracksResponse(artists = lastKnownArtists)
    }

    private fun parseWorkerTracksJson(jsonString: String): WorkerTracksResponse {
        return try {
            val root = JSONObject(jsonString)

            val artistsList = mutableListOf<ArchiveArtistItem>()
            val artistsArray = root.optJSONArray("artists")
            if (artistsArray != null) {
                for (i in 0 until artistsArray.length()) {
                    val obj = artistsArray.optJSONObject(i) ?: continue
                    val name = obj.optString("name", "").trim()
                    if (name.isBlank()) continue
                    val trackCount = obj.optInt("trackCount", obj.optInt("count", 0))
                    val rawCover = obj.optString("cover", "").ifBlank { obj.optString("cover_url", "") }
                    val cover = normalizeWorkerImageUrl(rawCover)
                    if (cover.isNotBlank()) {
                        registerArtistPicture(name, cover)
                    }
                    artistsList.add(
                        ArchiveArtistItem(
                            name = name,
                            trackCount = trackCount,
                            cover = cover
                        )
                    )
                }
            }
            if (artistsList.isNotEmpty()) {
                lastKnownArtists = artistsList
            }

            val matchedList = mutableListOf<ArchiveArtistItem>()
            val matchedArray = root.optJSONArray("matchedArtists")
            if (matchedArray != null) {
                for (i in 0 until matchedArray.length()) {
                    val item = matchedArray.opt(i)
                    when (item) {
                        is JSONObject -> {
                            val name = item.optString("name", "").trim()
                            if (name.isNotBlank()) {
                                val count = item.optInt("trackCount", item.optInt("count", 0))
                                val rawCover = item.optString("cover", "").ifBlank { item.optString("cover_url", "") }
                                val cover = normalizeWorkerImageUrl(rawCover).ifBlank {
                                    getArtistPicture(name) ?: ""
                                }
                                matchedList.add(ArchiveArtistItem(name = name, trackCount = count, cover = cover))
                            }
                        }
                        is String -> {
                            val name = item.trim()
                            if (name.isNotBlank()) {
                                val fromAll = artistsList.firstOrNull { it.name.equals(name, ignoreCase = true) }
                                matchedList.add(
                                    fromAll ?: ArchiveArtistItem(
                                        name = name,
                                        trackCount = 0,
                                        cover = getArtistPicture(name) ?: ""
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val tracksList = mutableListOf<OnlineTrack>()
            val dataArray = root.optJSONArray("data") ?: root.optJSONArray("tracks")
            if (dataArray != null) {
                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.optJSONObject(i) ?: continue
                    val id = obj.opt("id")?.toString()?.trim().orEmpty().ifBlank { "tg_$i" }
                    val title = obj.optString("title", "").ifBlank { obj.optString("name", "Unknown Track") }.trim()
                    val artistName = obj.optString("artist_name", "")
                        .ifBlank { obj.optString("artist", "Unknown Artist") }
                        .trim()
                    val durationSec = parseDurationToSeconds(obj.opt("duration"))
                    val rawStream = obj.optString("stream_url", "")
                        .ifBlank { obj.optString("streamUrl", "") }
                        .ifBlank { obj.optString("url", "") }
                    val rawDownload = obj.optString("download_url", "")
                        .ifBlank { obj.optString("downloadUrl", "") }
                        .ifBlank { rawStream }
                    val rawCover = obj.optString("cover_url", "")
                        .ifBlank { obj.optString("coverUrl", "") }
                        .ifBlank { obj.optString("cover", "") }

                    val streamUrl = resolveWorkerUrl(rawStream)
                    val downloadUrl = resolveWorkerUrl(rawDownload)
                    val coverUrl = normalizeWorkerImageUrl(rawCover).ifBlank {
                        getArtistPicture(artistName) ?: ""
                    }

                    registerTrackUrls(
                        trackId = id,
                        streamUrl = streamUrl,
                        downloadUrl = downloadUrl,
                        coverUrl = coverUrl,
                        artistName = artistName
                    )

                    tracksList.add(
                        OnlineTrack(
                            id = id,
                            title = title,
                            artist = artistName,
                            album = obj.optString("album", "Telegram Archive"),
                            coverUrl = coverUrl,
                            durationSec = durationSec,
                            streamUrl = streamUrl,
                            downloadUrl = downloadUrl,
                            previewUrl = streamUrl,
                            artistPictureUrl = getArtistPicture(artistName) ?: coverUrl
                        )
                    )
                }
            }

            WorkerTracksResponse(
                artists = if (artistsList.isNotEmpty()) artistsList else lastKnownArtists,
                matchedArtists = matchedList,
                tracks = tracksList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            WorkerTracksResponse(artists = lastKnownArtists)
        }
    }

    private fun parseDurationToSeconds(rawDuration: Any?): Int {
        if (rawDuration == null) return 180
        when (rawDuration) {
            is Number -> {
                val v = rawDuration.toInt()
                return if (v > 3600) (v / 1000).coerceAtLeast(1) else v.coerceAtLeast(1)
            }
            is String -> {
                val clean = rawDuration.trim()
                if (clean.contains(":")) {
                    val parts = clean.split(":").mapNotNull { it.trim().toIntOrNull() }
                    if (parts.size == 2) return (parts[0] * 60 + parts[1]).coerceAtLeast(1)
                    if (parts.size == 3) return (parts[0] * 3600 + parts[1] * 60 + parts[2]).coerceAtLeast(1)
                }
                val num = clean.toIntOrNull() ?: return 180
                return if (num > 3600) (num / 1000).coerceAtLeast(1) else num.coerceAtLeast(1)
            }
            else -> return 180
        }
    }

    suspend fun searchCatalog(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        val response = fetchFromWorker(query = clean, artist = "")
        if (response.tracks.isNotEmpty() || clean.isBlank()) {
            return@withContext response.tracks
        }
        fetchFromWorker(query = "", artist = "").tracks
    }

    suspend fun fetchArtistTopRatedTracks(artistName: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        fetchFromWorker(query = "", artist = artistName.trim()).tracks
    }

    fun buildStreamUrl(track: OnlineTrack): String {
        if (track.streamUrl.isNotBlank()) {
            return resolveWorkerUrl(track.streamUrl)
        }
        val cached = getPreviewUrl(track.id)
        if (!cached.isNullOrBlank()) return cached
        val cleanId = track.id.removePrefix("online_")
        return "$BASE_URL/stream?id=${URLEncoder.encode(cleanId, "UTF-8")}"
    }

    fun buildDownloadUrl(trackId: String, artist: String = "", title: String = ""): String {
        getDownloadUrlForTrack(trackId)?.let { if (it.isNotBlank()) return it }
        getPreviewUrl(trackId)?.let { if (it.isNotBlank()) return it }
        val cleanId = trackId.removePrefix("online_")
        return "$BASE_URL/download?id=${URLEncoder.encode(cleanId, "UTF-8")}"
    }

    fun getFallbackStreamUrl(track: Track): String? {
        val rawId = track.id.removePrefix("online_")
        val registered = getPreviewUrl(rawId) ?: getDownloadUrlForTrack(rawId)
        if (!registered.isNullOrBlank() && registered != track.uri) {
            return registered
        }
        return null
    }

    suspend fun loadCoverBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        loadCoverBitmapBlocking(url)
    }

    fun loadCoverBitmapBlocking(url: String): Bitmap? {
        if (url.isBlank()) return null
        val resolvedUrl = normalizeWorkerImageUrl(url)
        imageMemoryCache.get(resolvedUrl)?.let { return it }
        imageMemoryCache.get(url)?.let { return it }

        // بررسی مسیر فایل محلی یا فایل صوتی دارای کاور جاسازی‌شده (ID3)
        if (resolvedUrl.startsWith("/") || resolvedUrl.startsWith("file://")) {
            val filePath = if (resolvedUrl.startsWith("file://")) {
                Uri.parse(resolvedUrl).path ?: resolvedUrl.removePrefix("file://")
            } else {
                resolvedUrl
            }
            val file = File(filePath)
            if (file.exists() && file.length() > 256L) {
                val bmp = try {
                    if (file.name.endsWith(".mp3", ignoreCase = true) || file.name.endsWith(".m4a", ignoreCase = true)) {
                        val mmr = MediaMetadataRetriever()
                        val bytes = try {
                            mmr.setDataSource(file.absolutePath)
                            mmr.embeddedPicture
                        } finally {
                            try { mmr.release() } catch (_: Exception) {}
                        }
                        if (bytes != null && bytes.isNotEmpty()) {
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } else null
                    } else {
                        BitmapFactory.decodeFile(file.absolutePath)
                    }
                } catch (_: Exception) {
                    null
                }
                if (bmp != null) {
                    imageMemoryCache.put(resolvedUrl, bmp)
                    imageMemoryCache.put(url, bmp)
                    return bmp
                }
            }
            return null
        }

        var connection: HttpURLConnection? = null
        val bitmap = try {
            connection = (URL(resolvedUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 12000
                readTimeout = 12000
                doInput = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            connection.connect()
            if (connection.responseCode in 200..299) {
                connection.inputStream.use { BitmapFactory.decodeStream(it) }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }

        if (bitmap != null) {
            imageMemoryCache.put(resolvedUrl, bitmap)
            imageMemoryCache.put(url, bitmap)
        }
        return bitmap
    }
}

/**
 * موتور هماهنگ‌سازی پیشنهادها و تاریخچه پخش آنلاین با صفحه خانه و آمار برنامه
 */
object OnlineTasteEngine {
    private const val PREFS_NAME = "aura_online_taste_prefs"
    private const val KEY_ARTIST_SCORES = "artist_scores_json"
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
        } catch (_: Exception) {}
    }

    fun recordTrackListen(context: Context, track: Track) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val artist = track.artist.trim()
            if (artist.isNotBlank() && !artist.equals("Unknown Artist", ignoreCase = true)) {
                val scoresJson = JSONObject(prefs.getString(KEY_ARTIST_SCORES, "{}") ?: "{}")
                scoresJson.put(artist, scoresJson.optInt(artist, 0) + 3)
                prefs.edit().putString(KEY_ARTIST_SCORES, scoresJson.toString()).apply()
            }

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
            }
            updatedArr.put(newObj)
            for (i in 0 until recentArr.length()) {
                val obj = recentArr.optJSONObject(i) ?: continue
                if (obj.optString("id") != track.id && updatedArr.length() < 20) {
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
            val a = t.artist.trim()
            if (a.isNotBlank() && !a.equals("Unknown Artist", true)) {
                map[a] = (map[a] ?: 0) + 4
            }
        }
        localRecent.forEach { t ->
            val a = t.artist.trim()
            if (a.isNotBlank() && !a.equals("Unknown Artist", true)) {
                map[a] = (map[a] ?: 0) + 2
            }
        }

        return map.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(6)
    }

    fun buildSmartPlaylists(
        context: Context,
        currentCatalogTracks: List<OnlineTrack> = emptyList()
    ): List<OnlineSmartPlaylist> {
        val recentTracks = getRecentOnlineTracks(context)
        val covers = (currentCatalogTracks.mapNotNull { it.resolvedCoverUrl().takeIf { c -> c.isNotBlank() } } +
            recentTracks.mapNotNull { it.artworkUri?.takeIf { c -> c.isNotBlank() } }).distinct()
        val topArtists = getTopArtistsForUser(context)
        val firstArtist = topArtists.firstOrNull() ?: ""

        return listOf(
            OnlineSmartPlaylist(
                id = "telegram_archive_all",
                titleFa = "آرشیو کامل کانال",
                titleEn = "Full Channel Archive",
                subtitleFa = "پخش مستقیم از سرور کلادفلر",
                subtitleEn = "Direct Cloudflare Worker Stream",
                query = firstArtist,
                seedArtists = topArtists,
                coverUrls = covers.take(4),
                accentColor = Color(0xFF1ED760)
            )
        )
    }

    suspend fun fetchOnlineTimeSlotTracks(
        context: Context,
        hourOfDay: Int,
        topArtists: List<String>
    ): List<Track> = withContext(Dispatchers.IO) {
        val response = OnlineMusicRepository.fetchFromWorker(query = "", artist = topArtists.firstOrNull() ?: "")
        val list = if (response.tracks.isNotEmpty()) response.tracks else OnlineMusicRepository.fetchFromWorker().tracks
        list.take(16).map { online ->
            val cached = OnlineCacheHolder.getCachedFileIfPresent(context, online.id)
            val streamUrl = cached?.absolutePath ?: online.resolvedStreamUrl()
            online.toAppTrack(streamUrl)
        }
    }

    suspend fun fetchTrendingAndTasteFeed(
        context: Context,
        mode: String,
        topArtists: List<String>
    ): List<Track> = withContext(Dispatchers.IO) {
        val response = OnlineMusicRepository.fetchFromWorker(query = "", artist = "")
        response.tracks.take(20).map { online ->
            val cached = OnlineCacheHolder.getCachedFileIfPresent(context, online.id)
            val streamUrl = cached?.absolutePath ?: online.resolvedStreamUrl()
            online.toAppTrack(streamUrl)
        }
    }
}

/**
 * مدیریت دانلود مستقیم فایل‌ها از `download_url` به پوشه Music گوشی (بدون SimpleCache)
 */
object OnlineCacheHolder {
    private const val MIN_VALID_AUDIO_BYTES = 16_384L

    fun createDirectHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setUserAgent("Mozilla/5.0")
    }

    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = HashSet<String>()

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

    fun getMusicDownloadDir(context: Context): File {
        val extMusicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (extMusicDir != null) {
            if (!extMusicDir.exists()) extMusicDir.mkdirs()
            return extMusicDir
        }
        val fallbackDir = File(context.filesDir, "Music")
        if (!fallbackDir.exists()) fallbackDir.mkdirs()
        return fallbackDir
    }

    fun normalizeKey(trackId: String): String {
        val clean = trackId.removePrefix("online_").replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "aura_track_$clean.mp3"
    }

    fun refreshDownloadedSet(context: Context) {
        val ids = mutableSetOf<String>()
        val dir = getMusicDownloadDir(context)
        dir.listFiles()?.forEach { f ->
            if (f.isFile && f.name.endsWith(".mp3") && f.length() > MIN_VALID_AUDIO_BYTES) {
                val rawId = f.name.removePrefix("aura_track_").removeSuffix(".mp3")
                ids.add(rawId)
                ids.add("online_$rawId")
            }
        }
        _downloadedTrackIds.value = ids
    }

    fun isTrackCached(context: Context, trackId: String): Boolean {
        return getCachedFileIfPresent(context, trackId) != null
    }

    fun getCachedFileIfPresent(context: Context, trackId: String): File? {
        val key = normalizeKey(trackId)
        val file = File(getMusicDownloadDir(context), key)
        return if (file.exists() && file.length() > MIN_VALID_AUDIO_BYTES) file else null
    }

    fun getStreamBufferFileIfPresent(context: Context, trackId: String): File? {
        return getCachedFileIfPresent(context, trackId)
    }

    fun warmStreamBufferInBackground(
        context: Context,
        track: Track,
        onBuffered: ((File?) -> Unit)? = null
    ) {
        // استریم مستقیم بدون کش میانی (طبق درخواست کاربر)
        onBuffered?.invoke(getCachedFileIfPresent(context, track.id))
    }

    private fun downloadFromUrlToFile(
        targetUrl: String,
        destFile: File,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean {
        if (targetUrl.isBlank()) return false
        var currentUrl = targetUrl
        var redirects = 0
        while (redirects < 6) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 20000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Accept", "*/*")
                }
                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc.isNullOrBlank()) return false
                    currentUrl = URL(URL(currentUrl), loc).toString()
                    redirects++
                    continue
                }
                if (code in 200..299) {
                    val totalBytes = conn.contentLengthLong
                    val estTotal = if (totalBytes > MIN_VALID_AUDIO_BYTES) totalBytes else 4_000_000L
                    if (destFile.exists()) destFile.delete()
                    onProgress?.invoke(1)
                    var downloaded = 0L
                    var lastPct = 1
                    conn.inputStream.use { input ->
                        FileOutputStream(destFile).use { output ->
                            val buffer = ByteArray(32 * 1024)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                val pct = ((downloaded * 99L) / estTotal).toInt().coerceIn(1, 99)
                                if (pct > lastPct) {
                                    lastPct = pct
                                    onProgress?.invoke(pct)
                                }
                            }
                            output.flush()
                        }
                    }
                    conn.disconnect()
                    if (destFile.exists() && destFile.length() > MIN_VALID_AUDIO_BYTES) {
                        onProgress?.invoke(100)
                        return true
                    }
                    destFile.delete()
                    return false
                } else {
                    conn.disconnect()
                    return false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try { conn?.disconnect() } catch (_: Exception) {}
                if (destFile.exists()) destFile.delete()
                return false
            }
        }
        return false
    }

    fun getCoverFileForTrack(context: Context, trackId: String): File {
        val clean = trackId.removePrefix("online_").replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return File(getMusicDownloadDir(context), "aura_cover_$clean.jpg")
    }

    private suspend fun registerDownloadedFileInLocalDatabase(track: Track, downloadedFile: File) = withContext(Dispatchers.IO) {
        try {
            val appCtx = AuraApplication.instance.applicationContext
            var embeddedCoverBytes: ByteArray? = null
            val realDurMs = try {
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(downloadedFile.absolutePath)
                    val d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    embeddedCoverBytes = mmr.embeddedPicture
                    if (d != null && d > 1000L) d else track.durationMs
                } finally {
                    try { mmr.release() } catch (_: Exception) {}
                }
            } catch (_: Exception) {
                track.durationMs
            }

            val coverFile = getCoverFileForTrack(appCtx, track.id)
            var finalArtworkUri = track.artworkUri?.takeIf { it.isNotBlank() }
                ?: OnlineMusicRepository.getCoverUrlForTrack(track.id, track.artist).takeIf { it.isNotBlank() }

            try {
                if (embeddedCoverBytes != null && embeddedCoverBytes!!.isNotEmpty()) {
                    FileOutputStream(coverFile).use { it.write(embeddedCoverBytes!!) }
                    BitmapFactory.decodeByteArray(embeddedCoverBytes, 0, embeddedCoverBytes!!.size)?.let { bmp ->
                        OnlineMusicRepository.cacheCoverBitmap(coverFile.absolutePath, bmp)
                        if (!finalArtworkUri.isNullOrBlank()) {
                            OnlineMusicRepository.cacheCoverBitmap(finalArtworkUri!!, bmp)
                        }
                    }
                    finalArtworkUri = coverFile.absolutePath
                    OnlineMusicRepository.registerCoverUrlForTrack(track.id, coverFile.absolutePath)
                } else if (!finalArtworkUri.isNullOrBlank() && (finalArtworkUri!!.startsWith("http") || finalArtworkUri!!.startsWith("/img"))) {
                    val bmp = OnlineMusicRepository.loadCoverBitmapBlocking(finalArtworkUri!!)
                    if (bmp != null) {
                        FileOutputStream(coverFile).use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        }
                        if (coverFile.exists() && coverFile.length() > 512L) {
                            OnlineMusicRepository.cacheCoverBitmap(coverFile.absolutePath, bmp)
                            finalArtworkUri = coverFile.absolutePath
                            OnlineMusicRepository.registerCoverUrlForTrack(track.id, coverFile.absolutePath)
                        }
                    }
                }
            } catch (_: Exception) {}

            val savedTrack = track.copy(
                id = if (track.id.startsWith("online_")) track.id else "online_${track.id}",
                uri = downloadedFile.absolutePath,
                artworkUri = finalArtworkUri,
                durationMs = realDurMs,
                dateAdded = System.currentTimeMillis(),
                isDemo = false
            )
            AuraApplication.instance.repository.saveDownloadedTrackToLibrary(savedTrack)
        } catch (_: Exception) {}
    }

    fun downloadOnlineTrack(
        context: Context,
        onlineTrack: OnlineTrack,
        onComplete: ((File?) -> Unit)? = null
    ) {
        val dlUrl = onlineTrack.resolvedDownloadUrl()
        val appTrack = onlineTrack.toAppTrack(onlineTrack.resolvedStreamUrl())
        downloadTrackExplicitly(context, appTrack, explicitDownloadUrl = dlUrl, onComplete = onComplete)
    }

    fun downloadTrackExplicitly(
        context: Context,
        track: Track,
        explicitDownloadUrl: String? = null,
        onComplete: ((File?) -> Unit)? = null
    ) {
        val rawId = track.id.removePrefix("online_")
        val key = normalizeKey(rawId)
        getCachedFileIfPresent(context, rawId)?.let { existing ->
            refreshDownloadedSet(context)
            downloadScope.launch {
                registerDownloadedFileInLocalDatabase(track, existing)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(existing)
                }
            }
            return
        }

        synchronized(activeDownloads) {
            if (activeDownloads.contains(key)) return
            activeDownloads.add(key)
        }

        _downloadingTrackIds.value = _downloadingTrackIds.value + setOf(rawId, "online_$rawId")
        updateTrackDownloadProgress(rawId, 1)

        val appCtx = context.applicationContext
        downloadScope.launch {
            val musicDir = getMusicDownloadDir(appCtx)
            val tempFile = File(musicDir, "$key.tmp")
            val finalFile = File(musicDir, key)
            var resultFile: File? = null
            try {
                val targetUrl = explicitDownloadUrl?.takeIf { it.isNotBlank() }
                    ?: OnlineMusicRepository.getDownloadUrlForTrack(rawId)
                    ?: track.uri.takeIf { it.startsWith("http") }
                    ?: OnlineMusicRepository.buildDownloadUrl(rawId, track.artist, track.title)

                val ok = downloadFromUrlToFile(targetUrl, tempFile) { pct ->
                    updateTrackDownloadProgress(rawId, pct)
                }
                if (ok && tempFile.exists() && tempFile.length() > MIN_VALID_AUDIO_BYTES) {
                    if (finalFile.exists()) finalFile.delete()
                    val moved = tempFile.renameTo(finalFile)
                    if (!moved) {
                        tempFile.copyTo(finalFile, overwrite = true)
                        tempFile.delete()
                    }
                    if (finalFile.exists()) {
                        updateTrackDownloadProgress(rawId, 100)
                        resultFile = finalFile
                        registerDownloadedFileInLocalDatabase(track, finalFile)
                    }
                } else {
                    if (tempFile.exists()) tempFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

/**
 * صفحه استریم و آرشیو آنلاین متصل به Cloudflare Worker و کانال تلگرام
 * با طراحی Spotify + Liquid Glass
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineMusicScreen(
    palette: AmbientPalette = AmbientPalette(),
    lang: AppLanguage = AppLanguage.PERSIAN,
    activePlayingTrackId: String? = null,
    isGlobalPlayerPlaying: Boolean = false,
    bottomPadding: Dp = 120.dp,
    onPauseOfflinePlayer: (() -> Unit)? = null,
    onExternalPlay: ((track: OnlineTrack, streamUrl: String, queue: List<OnlineTrack>) -> Unit)? = null,
    onAddToQueue: ((track: OnlineTrack, streamUrl: String) -> Unit)? = null,
    onDownloadToDeviceLibrary: ((track: Track) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isFa = lang == AppLanguage.PERSIAN
    val spotifyGreen = Color(0xFF1ED760)

    var searchQuery by remember { mutableStateOf("") }
    var selectedArtist by remember { mutableStateOf<ArchiveArtistItem?>(null) }

    var allArtists by remember { mutableStateOf<List<ArchiveArtistItem>>(emptyList()) }
    var matchedArtists by remember { mutableStateOf<List<ArchiveArtistItem>>(emptyList()) }
    var tracks by remember { mutableStateOf<List<OnlineTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // وضعیت پخش در ExoPlayer مستقیم
    var currentQueue by remember { mutableStateOf<List<OnlineTrack>>(emptyList()) }
    var currentQueueIndex by remember { mutableStateOf(-1) }
    var currentTrack by remember { mutableStateOf<OnlineTrack?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }

    var fetchJob by remember { mutableStateOf<Job?>(null) }

    val downloadingIds by OnlineCacheHolder.downloadingTrackIds.collectAsState()
    val downloadProgressMap by OnlineCacheHolder.downloadProgressMap.collectAsState()
    val downloadedIds by OnlineCacheHolder.downloadedTrackIds.collectAsState()

    // پلیر ExoPlayer مستقیم با DefaultHttpDataSource.Factory و بدون هیچگونه SimpleCache
    val exoPlayer = remember(context) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setUserAgent("Mozilla/5.0")

        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(context, httpDataSourceFactory)
        )

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    // هندل کردن دکمه بازگشت گوشی وقتی در صفحه اختصاصی خواننده (Artist View) هستیم
    BackHandler(enabled = selectedArtist != null) {
        selectedArtist = null
    }

    fun loadTracksFromServer(queryText: String, artistFilter: String) {
        fetchJob?.cancel()
        fetchJob = scope.launch {
            isLoading = true
            val response = OnlineMusicRepository.fetchFromWorker(
                query = queryText,
                artist = artistFilter
            )
            if (response.artists.isNotEmpty()) {
                allArtists = response.artists
            }
            matchedArtists = response.matchedArtists
            tracks = response.tracks
            if (queryText.isNotBlank()) {
                OnlineTasteEngine.recordSearchActivity(context, queryText, response.tracks)
            }
            isLoading = false
        }
    }

    fun playTrackAtIndex(index: Int, queueList: List<OnlineTrack>) {
        if (index !in queueList.indices) return
        val target = queueList[index]
        currentQueue = queueList
        currentQueueIndex = index
        currentTrack = target

        val cachedFile = OnlineCacheHolder.getCachedFileIfPresent(context, target.id)
        val playUri = if (cachedFile != null && cachedFile.exists()) {
            cachedFile.absolutePath
        } else {
            target.resolvedStreamUrl()
        }

        // اگر پلیر اصلی برنامه (NowPlayingScreen) متصل است، مستقیماً آهنگ و صف پخش را به پلیر اصلی بفرست تا صفحه اصلی پلیر همراه با کاور ۳بعدی و اسلایدر باز شود
        if (onExternalPlay != null) {
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
            } catch (_: Exception) {}
            isPlaying = false
            isBuffering = false
            onExternalPlay.invoke(target, playUri, queueList)
            return
        }

        // متوقف کردن پلیر آفلاین در صورت فعال بودن
        onPauseOfflinePlayer?.invoke()

        isBuffering = true
        currentPositionMs = 0L
        totalDurationMs = (target.durationSec.coerceAtLeast(1) * 1000L)

        try {
            val mediaUri = if (playUri.startsWith("/")) Uri.fromFile(File(playUri)).toString() else playUri
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(mediaUri))
            exoPlayer.seekTo(0L)
            exoPlayer.prepare()
            exoPlayer.play()
            if (exoPlayer.audioSessionId > 0) {
                AuraDspController.bindToAudioSession(exoPlayer.audioSessionId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isBuffering = false
            isPlaying = false
        }

        scope.launch(Dispatchers.IO) {
            OnlineTasteEngine.recordTrackListen(context, target.toAppTrack(playUri))
        }
    }

    fun playNextTrack() {
        if (currentQueue.isEmpty()) return
        val nextIndex = if (currentQueueIndex + 1 < currentQueue.size) currentQueueIndex + 1 else 0
        playTrackAtIndex(nextIndex, currentQueue)
    }

    fun playPreviousTrack() {
        if (currentQueue.isEmpty()) return
        if (exoPlayer.currentPosition > 4000L) {
            exoPlayer.seekTo(0L)
            return
        }
        val prevIndex = if (currentQueueIndex - 1 >= 0) currentQueueIndex - 1 else currentQueue.lastIndex
        playTrackAtIndex(prevIndex, currentQueue)
    }

    fun openArtistView(artistItem: ArchiveArtistItem) {
        focusManager.clearFocus()
        selectedArtist = artistItem
        loadTracksFromServer(queryText = "", artistFilter = artistItem.name)
    }

    fun openArtistByName(artistName: String) {
        val cleanName = artistName.trim()
        if (cleanName.isBlank()) return
        val existing = allArtists.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
            ?: matchedArtists.firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
            ?: ArchiveArtistItem(
                name = cleanName,
                trackCount = tracks.count { it.artist.equals(cleanName, ignoreCase = true) },
                cover = OnlineMusicRepository.getArtistPicture(cleanName) ?: ""
            )
        openArtistView(existing)
    }

    fun triggerTrackDownload(track: OnlineTrack) {
        Toast.makeText(
            context,
            if (isFa) "در حال دانلود «${track.title}» در پوشه Music..." else "Downloading ${track.title}...",
            Toast.LENGTH_SHORT
        ).show()

        OnlineCacheHolder.downloadOnlineTrack(context, track) { file ->
            if (file != null && file.exists()) {
                Toast.makeText(
                    context,
                    if (isFa) "«${track.title}» دانلود شد و به آهنگ‌های شما اضافه گردید ✓" else "${track.title} saved to your library ✓",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    if (isFa) "خطا در دانلود فایل صوتی" else "Download failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // اتصال Player.Listener به ExoPlayer و اتصال مستقیم audioSessionId به اکولایزر (AuraDspController)
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId > 0) {
                    AuraDspController.bindToAudioSession(audioSessionId)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                when (playbackState) {
                    Player.STATE_READY -> {
                        isBuffering = false
                        isPlaying = exoPlayer.isPlaying
                        val dur = exoPlayer.duration
                        if (dur > 0) totalDurationMs = dur
                        if (exoPlayer.audioSessionId > 0) {
                            AuraDspController.bindToAudioSession(exoPlayer.audioSessionId)
                        }
                    }
                    Player.STATE_ENDED -> {
                        if (currentQueue.isNotEmpty() && currentQueueIndex + 1 < currentQueue.size) {
                            playNextTrack()
                        } else {
                            isPlaying = false
                            currentPositionMs = 0L
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    isBuffering = false
                    if (exoPlayer.audioSessionId > 0) {
                        AuraDspController.bindToAudioSession(exoPlayer.audioSessionId)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                isPlaying = false
            }
        }

        exoPlayer.addListener(listener)
        onDispose {
            try {
                exoPlayer.removeListener(listener)
                exoPlayer.stop()
                exoPlayer.release()
            } catch (_: Exception) {}
        }
    }

    // به‌روزرسانی زنده اسلایدر زمان پخش
    LaunchedEffect(isPlaying, exoPlayer) {
        while (isActive && isPlaying) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            val dur = exoPlayer.duration
            if (dur > 0) totalDurationMs = dur
            delay(250L)
        }
    }

    // بارگذاری اولیه آرشیو و جستجوی هوشمند با Debounce
    LaunchedEffect(Unit) {
        OnlineCacheHolder.refreshDownloadedSet(context)
        loadTracksFromServer(queryText = "", artistFilter = "")
    }

    LaunchedEffect(searchQuery, selectedArtist) {
        val activeArtistName = selectedArtist?.name ?: ""
        if (searchQuery.isBlank() && activeArtistName.isBlank() && tracks.isNotEmpty()) {
            return@LaunchedEffect
        }
        delay(300L)
        loadTracksFromServer(queryText = searchQuery, artistFilter = activeArtistName)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = if (onExternalPlay == null && currentTrack != null) bottomPadding + 132.dp else bottomPadding + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ۱. نوار جستجوی هوشمند (Spotify Liquid Glass Search Bar)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(22.dp),
                            thickness = GlassThickness.REGULAR,
                            tintColor = spotifyGreen,
                            tintAlpha = 0.14f,
                            borderWidth = 1.dp
                        )
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = spotifyGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { newValue ->
                                searchQuery = newValue
                                if (newValue.isNotBlank() && selectedArtist != null) {
                                    selectedArtist = null
                                }
                            },
                            placeholder = {
                                Text(
                                    text = if (isFa) "جستجوی خواننده یا نام آهنگ (مثلاً Kanye West)..." else "Search artist or song (e.g. Kanye West)...",
                                    color = Color(0xFF96A0B8),
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    loadTracksFromServer(queryText = searchQuery, artistFilter = selectedArtist?.name ?: "")
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
                                cursorColor = spotifyGreen
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("online_search_input")
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                    loadTracksFromServer(queryText = "", artistFilter = selectedArtist?.name ?: "")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    loadTracksFromServer(queryText = searchQuery, artistFilter = selectedArtist?.name ?: "")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ۲. نمای اختصاصی خواننده (Spotify Artist View) در صورت انتخاب شدن یک خواننده
            if (selectedArtist != null) {
                val activeArtist = selectedArtist!!
                val displayCover = activeArtist.cover.ifBlank {
                    tracks.firstOrNull()?.resolvedCoverUrl() ?: ""
                }
                val displayTrackCount = if (activeArtist.trackCount > 0) activeArtist.trackCount else tracks.size

                item {
                    SpotifyArtistHeroView(
                        artistName = activeArtist.name,
                        trackCount = displayTrackCount,
                        coverUrl = displayCover,
                        isFa = isFa,
                        hasTracks = tracks.isNotEmpty(),
                        onBackToArchive = {
                            selectedArtist = null
                            loadTracksFromServer(queryText = searchQuery, artistFilter = "")
                        },
                        onPlayAll = {
                            if (tracks.isNotEmpty()) {
                                playTrackAtIndex(0, tracks)
                            }
                        }
                    )
                }
            } else {
                // ۳. الف) اگر کاربر سرچ کرده و matchedArtists خالی نیست -> کارت بزرگ پروفایل خواننده (Spotify Artist Card)
                if (searchQuery.isNotBlank() && matchedArtists.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (isFa) "خواننده مرتبط با جستجو" else "Top Matched Artist",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = spotifyGreen,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            matchedArtists.forEach { matched ->
                                SpotifyMatchedArtistCard(
                                    artist = matched,
                                    isFa = isFa,
                                    onClick = { openArtistView(matched) }
                                )
                            }
                        }
                    }
                }

                // ۳. ب) لیست افقی خواننده‌های موجود در آرشیو (Artists Row) در بالای صفحه اصلی آنلاین
                if (allArtists.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFa) "خواننده‌های آرشیو" else "Archive Artists",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                                Text(
                                    text = if (isFa) "${allArtists.size} هنرمند" else "${allArtists.size} Artists",
                                    color = Color(0xFF96A0B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(allArtists, key = { "artist_circle_${it.name}" }) { artistItem ->
                                    ArchiveArtistCircleCard(
                                        artist = artistItem,
                                        isFa = isFa,
                                        onClick = { openArtistView(artistItem) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ۴. هدر لیست آهنگ‌ها + دکمه پخش همه
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when {
                                selectedArtist != null -> if (isFa) "تمام آهنگ‌های ${selectedArtist!!.name}" else "All Tracks by ${selectedArtist!!.name}"
                                searchQuery.isNotBlank() -> if (isFa) "نتایج جستجو برای «$searchQuery»" else "Search Results for \"$searchQuery\""
                                else -> if (isFa) "آهنگ‌های آرشیو کانال" else "Channel Archive Tracks"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = if (isFa) "${tracks.size} قطعه آماده پخش" else "${tracks.size} tracks available",
                            color = Color(0xFF96A0B8),
                            fontSize = 11.5.sp
                        )
                    }

                    if (tracks.isNotEmpty() && selectedArtist == null) {
                        Button(
                            onClick = { playTrackAtIndex(0, tracks) },
                            colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF050A07),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFa) "پخش همه" else "Play All",
                                color = Color(0xFF050A07),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ۵. وضعیت لودینگ یا لیست آهنگ‌ها
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = spotifyGreen)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isFa) "در حال دریافت آرشیو از سرور کلادفلر..." else "Loading archive from Cloudflare Worker...",
                                color = Color(0xFFA0A8C0),
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            } else if (tracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(
                                shape = RoundedCornerShape(22.dp),
                                thickness = GlassThickness.THIN,
                                tintColor = palette.primary,
                                tintAlpha = 0.12f
                            )
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = spotifyGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isFa) "آهنگی با این مشخصات در آرشیو پیدا نشد" else "No matching tracks found in archive",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(tracks, key = { idx, t -> "tg_track_${t.id}_$idx" }) { index, track ->
                    val isThisTrackActive = activePlayingTrackId == "online_${track.id}" ||
                        activePlayingTrackId == track.id ||
                        (onExternalPlay == null && currentTrack?.id == track.id)
                    val isThisTrackPlaying = ((activePlayingTrackId == "online_${track.id}" || activePlayingTrackId == track.id) && isGlobalPlayerPlaying) ||
                        (onExternalPlay == null && currentTrack?.id == track.id && isPlaying)

                    val isDownloaded = downloadedIds.contains(track.id) ||
                        downloadedIds.contains("online_${track.id}") ||
                        OnlineCacheHolder.isTrackCached(context, track.id)
                    val isDownloading = downloadingIds.contains(track.id) ||
                        downloadingIds.contains("online_${track.id}")
                    val progressPct = downloadProgressMap[track.id] ?: downloadProgressMap["online_${track.id}"]

                    SpotifyArchiveTrackRow(
                        track = track,
                        indexNumber = index + 1,
                        isSelected = isThisTrackActive,
                        isPlaying = isThisTrackPlaying,
                        isDownloaded = isDownloaded,
                        isDownloading = isDownloading,
                        downloadPercent = progressPct,
                        onTrackClick = {
                            playTrackAtIndex(index, tracks)
                        },
                        onArtistClick = {
                            openArtistByName(track.artist)
                        },
                        onDownloadClick = {
                            triggerTrackDownload(track)
                        }
                    )
                }
            }
        }

        // ۶. پلیر شناور پایین صفحه (فقط زمانی که پلیر اصلی برنامه متصل نباشد)
        AnimatedVisibility(
            visible = onExternalPlay == null && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp)
                .padding(bottom = bottomPadding + 8.dp)
        ) {
            currentTrack?.let { active ->
                SpotifyFloatingPlayerBar(
                    track = active,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    currentPositionMs = currentPositionMs,
                    totalDurationMs = totalDurationMs,
                    onTogglePlayPause = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            onPauseOfflinePlayer?.invoke()
                            if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0L)
                                exoPlayer.prepare()
                            }
                            exoPlayer.play()
                        }
                    },
                    onPrevious = { playPreviousTrack() },
                    onNext = { playNextTrack() },
                    onSeekTo = { targetMs ->
                        currentPositionMs = targetMs
                        exoPlayer.seekTo(targetMs)
                    },
                    onArtistClick = { openArtistByName(active.artist) }
                )
            }
        }
    }
}

/**
 * کارت بزرگ پروفایل خواننده در بالای نتایج سرچ (Spotify Artist Card)
 */
@Composable
private fun SpotifyMatchedArtistCard(
    artist: ArchiveArtistItem,
    isFa: Boolean,
    onClick: () -> Unit
) {
    val spotifyGreen = Color(0xFF1ED760)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(24.dp),
                thickness = GlassThickness.THICK,
                tintColor = spotifyGreen,
                tintAlpha = 0.22f,
                borderWidth = 1.3.dp
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                ArtistAvatarImage(
                    imageUrl = artist.cover,
                    artistName = artist.name,
                    size = 68.dp
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = spotifyGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFa) "پروفایل خواننده در آرشیو" else "ARCHIVE ARTIST PROFILE",
                            color = spotifyGreen,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = artist.name,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (artist.trackCount > 0) {
                        Text(
                            text = if (isFa) "${artist.trackCount} آهنگ در آرشیو • مشاهده همه" else "${artist.trackCount} tracks in archive • View all",
                            color = Color(0xFFB8C2D8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(spotifyGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Open Artist",
                    tint = Color(0xFF050A07),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * کارت دایره‌ای خواننده‌ها در ردیف افقی بالای صفحه (Artists Row)
 */
@Composable
private fun ArchiveArtistCircleCard(
    artist: ArchiveArtistItem,
    isFa: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(92.dp)
            .clickable { onClick() }
    ) {
        ArtistAvatarImage(
            imageUrl = artist.cover,
            artistName = artist.name,
            size = 78.dp
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = artist.name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (artist.trackCount > 0) {
            Text(
                text = if (isFa) "${artist.trackCount} آهنگ" else "${artist.trackCount} tracks",
                color = Color(0xFF96A0B8),
                fontSize = 10.5.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * هدر صفحه اختصاصی خواننده (Spotify Artist View) با دکمه بازگشت به کل آرشیو و دکمه پخش همه
 */
@Composable
private fun SpotifyArtistHeroView(
    artistName: String,
    trackCount: Int,
    coverUrl: String,
    isFa: Boolean,
    hasTracks: Boolean,
    onBackToArchive: () -> Unit,
    onPlayAll: () -> Unit
) {
    val spotifyGreen = Color(0xFF1ED760)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(26.dp),
                thickness = GlassThickness.THICK,
                tintColor = spotifyGreen,
                tintAlpha = 0.2f,
                borderWidth = 1.2.dp
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBackToArchive,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFa) "بازگشت به کل آرشیو" else "Back to Archive",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = spotifyGreen.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = spotifyGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isFa) "آرشیو خواننده" else "VERIFIED ARTIST",
                            color = spotifyGreen,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ArtistAvatarImage(
                    imageUrl = coverUrl,
                    artistName = artistName,
                    size = 86.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artistName,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFa) "$trackCount آهنگ در آرشیو کانال" else "$trackCount tracks in channel archive",
                        color = Color(0xFFB8C2D8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = onPlayAll,
                enabled = hasTracks,
                colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF050A07),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isFa) "پخش همه آهنگ‌های $artistName (Play All)" else "Play All ($artistName)",
                    color = Color(0xFF050A07),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.5.sp
                )
            }
        }
    }
}

/**
 * ردیف نمایش هر آهنگ به سبک Spotify با امکان کلیک روی نام خواننده و دانلود مستقیم از `download_url`
 */
@Composable
private fun SpotifyArchiveTrackRow(
    track: OnlineTrack,
    indexNumber: Int,
    isSelected: Boolean,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadPercent: Int?,
    onTrackClick: () -> Unit,
    onArtistClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val spotifyGreen = Color(0xFF1ED760)
    val durationFormatted = remember(track.durationSec) {
        val m = track.durationSec / 60
        val s = track.durationSec % 60
        String.format(Locale.US, "%d:%02d", m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                thickness = if (isSelected) GlassThickness.REGULAR else GlassThickness.THIN,
                tintColor = if (isSelected) spotifyGreen else Color.White,
                tintAlpha = if (isSelected) 0.22f else 0.06f,
                borderWidth = if (isSelected) 1.3.dp else 0.8.dp
            )
            .clickable { onTrackClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // شماره ردیف یا آیکون اکولایزر در حال پخش
            Box(
                modifier = Modifier.width(26.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = spotifyGreen,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = indexNumber.toString(),
                        color = if (isSelected) spotifyGreen else Color(0xFF8A94AC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            CoverArtBox(
                coverUrl = track.resolvedCoverUrl(),
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isSelected) spotifyGreen else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // با کلیک روی نام خواننده زیر هر آهنگ، صفحه اختصاصی آن خواننده باز می‌شود
                    Text(
                        text = track.artist,
                        color = Color(0xFFB4BED4),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onArtistClick() }
                    )
                    Text(
                        text = "• $durationFormatted",
                        color = Color(0xFF7D869C),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // دکمه دانلود مستقیم از download_url به پوشه Music گوشی
            IconButton(
                onClick = onDownloadClick,
                enabled = !isDownloading,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                when {
                    isDownloading -> {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = spotifyGreen,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            if (downloadPercent != null) {
                                Text(
                                    text = "$downloadPercent",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    isDownloaded -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = spotifyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * نوار پخش شناور پایین صفحه با کنترل‌های کامل (قبلی، پخش/توقف، بعدی و اسلایدر جلو/عقب)
 */
@Composable
private fun SpotifyFloatingPlayerBar(
    track: OnlineTrack,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onArtistClick: () -> Unit
) {
    val spotifyGreen = Color(0xFF1ED760)
    val safeDuration = totalDurationMs.coerceAtLeast(1000L)
    val sliderFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    fun formatMs(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val m = totalSec / 60L
        val s = totalSec % 60L
        return String.format(Locale.US, "%d:%02d", m, s)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(24.dp),
                thickness = GlassThickness.THICK,
                tintColor = spotifyGreen,
                tintAlpha = 0.2f,
                borderWidth = 1.2.dp
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isBuffering) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(CircleShape),
                    color = spotifyGreen,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArtBox(
                    coverUrl = track.resolvedCoverUrl(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = spotifyGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onArtistClick() }
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(spotifyGreen)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF050A07),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // اسلایدر جلو و عقب بردن آهنگ به همراه تایمر
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatMs(currentPositionMs),
                    color = Color(0xFFA8B2C8),
                    fontSize = 10.5.sp
                )
                Slider(
                    value = sliderFraction,
                    onValueChange = { frac ->
                        onSeekTo((frac * safeDuration).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = spotifyGreen,
                        activeTrackColor = spotifyGreen,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp)
                )
                Text(
                    text = formatMs(safeDuration),
                    color = Color(0xFFA8B2C8),
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

@Composable
private fun ArtistAvatarImage(
    imageUrl: String,
    artistName: String,
    size: Dp
) {
    val spotifyGreen = Color(0xFF1ED760)
    val resolvedUrl = remember(imageUrl, artistName) {
        OnlineMusicRepository.normalizeWorkerImageUrl(imageUrl).ifBlank {
            OnlineMusicRepository.getArtistPicture(artistName) ?: ""
        }
    }
    var bitmap by remember(resolvedUrl) {
        mutableStateOf(OnlineMusicRepository.getCachedCoverBitmap(resolvedUrl))
    }

    LaunchedEffect(resolvedUrl) {
        if (bitmap == null && resolvedUrl.isNotBlank()) {
            bitmap = OnlineMusicRepository.loadCoverBitmap(resolvedUrl)
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(spotifyGreen.copy(alpha = 0.3f), Color(0xFF101524))
                )
            )
            .border(1.5.dp, spotifyGreen.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            resolvedUrl.isNotBlank() -> {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Text(
                    text = artistName.trim().take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.34f).sp
                )
            }
        }
    }
}

@Composable
private fun CoverArtBox(
    coverUrl: String,
    modifier: Modifier = Modifier
) {
    val resolvedUrl = remember(coverUrl) {
        OnlineMusicRepository.normalizeWorkerImageUrl(coverUrl)
    }
    var loadedBitmap by remember(resolvedUrl) {
        mutableStateOf(OnlineMusicRepository.getCachedCoverBitmap(resolvedUrl))
    }

    LaunchedEffect(resolvedUrl) {
        if (loadedBitmap == null && resolvedUrl.isNotBlank()) {
            loadedBitmap = OnlineMusicRepository.loadCoverBitmap(resolvedUrl)
        }
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(Color(0xFF1E2638), Color(0xFF0D111C))
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        when {
            loadedBitmap != null -> {
                Image(
                    bitmap = loadedBitmap!!.asImageBitmap(),
                    contentDescription = "Cover Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            resolvedUrl.isNotBlank() -> {
                AsyncImage(
                    model = resolvedUrl,
                    contentDescription = "Cover Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF1ED760),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
