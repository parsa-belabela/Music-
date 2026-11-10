package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.datasource.DefaultHttpDataSource
import coil.compose.AsyncImage
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.Track
import com.example.monetization.EntitlementManager
import com.example.monetization.PurchaseResult
import com.example.ui.components.ArtistAvatarDiskCache
import com.example.ui.components.AuraPremiumPaywallContent
import com.example.ui.components.SpotifyArtistAvatar
import com.example.ui.theme.AuraCanvas
import com.example.ui.theme.AuraSpotifyGreen
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPInputStream
import kotlin.math.PI
import kotlin.math.sin

const val WORKER_URL = "https://alubm.parsaghorbani8861.workers.dev/"

data class CloudflareTrack(
    val id: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val coverUrl: String? = null,
    val durationMs: Long = 0L,
    val genre: String? = null
) {
    fun toOnlineTrack(): OnlineTrack {
        val base = CloudflareMusicApi.workerBaseUrl.trimEnd('/')
        val resolvedCover = coverUrl?.takeIf { it.isNotBlank() } ?: "$base/cover?id=$id&v=hd9"
        return OnlineTrack(
            id = id,
            title = title,
            artist = artist,
            coverUrl = resolvedCover,
            durationSec = ((durationMs.takeIf { it > 0L } ?: 180_000L) / 1000L).toInt().coerceAtLeast(1),
            streamUrl = streamUrl,
            downloadUrl = streamUrl,
            previewUrl = streamUrl,
            rawGenre = genre.orEmpty()
        )
    }

    fun toAppTrack(): Track = toOnlineTrack().toAppTrack()
}

object CloudflareMusicApi {
    var workerBaseUrl: String = "https://alubm.parsaghorbani8861.workers.dev"

    suspend fun searchOrFetchTracks(query: String = "", deviceId: String = ""): Result<List<CloudflareTrack>> = withContext(Dispatchers.IO) {
        try {
            val base = workerBaseUrl.trimEnd('/')
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")

            val queryStringForEmpty = if (deviceId.isNotBlank()) "?deviceId=${URLEncoder.encode(deviceId, "UTF-8")}" else ""
            val deviceSuffix = if (deviceId.isNotBlank()) "&deviceId=${URLEncoder.encode(deviceId, "UTF-8")}" else ""

            val candidateUrls = if (query.isBlank()) {
                listOf(
                    "$base/tracks$queryStringForEmpty",
                    "$base/api/songs$queryStringForEmpty",
                    "$base$queryStringForEmpty"
                )
            } else {
                listOf(
                    "$base/tracks?q=$encodedQuery$deviceSuffix",
                    "$base/search?q=$encodedQuery$deviceSuffix",
                    "$base/api/songs?q=$encodedQuery$deviceSuffix"
                )
            }

            var lastError: Exception? = null

            for (endpoint in candidateUrls) {
                var connection: HttpURLConnection? = null
                try {
                    connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        requestMethod = "GET"
                        connectTimeout = 15000
                        readTimeout = 20000
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", "Mozilla/5.0")
                        if (deviceId.isNotBlank()) {
                            setRequestProperty("X-Device-Id", deviceId)
                        }
                    }

                    if (connection.responseCode in 200..299) {
                        val rawStream = connection.inputStream
                        val decodedStream = if (connection.contentEncoding?.contains("gzip", ignoreCase = true) == true) {
                            GZIPInputStream(rawStream)
                        } else {
                            rawStream
                        }
                        val parsedTracks = decodedStream.use { parseCloudflareStream(it, base) }
                        val finalTracks = if (query.isBlank()) {
                            parsedTracks
                        } else {
                            parsedTracks.filter {
                                it.title.contains(query, ignoreCase = true) ||
                                    it.artist.contains(query, ignoreCase = true)
                            }.ifEmpty { parsedTracks }
                        }
                        if (finalTracks.isNotEmpty() || query.isNotBlank()) {
                            return@withContext Result.success(finalTracks)
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                } finally {
                    try { connection?.disconnect() } catch (_: Throwable) {}
                }
            }

            Result.failure(lastError ?: Exception("عدم ارتباط با سرور کلودفلر"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readJsonScalarAsString(reader: JsonReader): String {
        return when (reader.peek()) {
            JsonToken.STRING -> reader.nextString()
            JsonToken.NUMBER -> reader.nextString()
            JsonToken.BOOLEAN -> reader.nextBoolean().toString()
            JsonToken.NULL -> {
                reader.nextNull()
                ""
            }
            else -> {
                reader.skipValue()
                ""
            }
        }
    }

    private fun parseCloudflareStream(inputStream: InputStream, baseUrl: String): List<CloudflareTrack> {
        val list = mutableListOf<CloudflareTrack>()
        try {
            val reader = JsonReader(BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 32768)).apply {
                isLenient = true
            }
            when (reader.peek()) {
                JsonToken.BEGIN_ARRAY -> {
                    parseTrackArray(reader, baseUrl, list)
                }
                JsonToken.BEGIN_OBJECT -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val key = reader.nextName()
                        if ((key == "tracks" || key == "results" || key == "songs" || key == "data" || key == "items") &&
                            list.isEmpty() && reader.peek() == JsonToken.BEGIN_ARRAY
                        ) {
                            parseTrackArray(reader, baseUrl, list)
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }
        } catch (_: Throwable) {
        }
        return list
    }

    private fun parseTrackArray(reader: JsonReader, baseUrl: String, outList: MutableList<CloudflareTrack>) {
        reader.beginArray()
        var index = 0
        while (reader.hasNext()) {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                reader.skipValue()
                continue
            }
            reader.beginObject()
            var id = ""
            var fileId = ""
            var messageId = ""
            var title = ""
            var name = ""
            var artist = ""
            var artistFallback = ""
            var rawStream = ""
            var rawCover = ""
            var durationRaw = 0L
            var genre = ""

            while (reader.hasNext()) {
                val field = reader.nextName()
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    continue
                }
                when (field) {
                    "id" -> id = readJsonScalarAsString(reader)
                    "file_id" -> fileId = readJsonScalarAsString(reader)
                    "message_id" -> messageId = readJsonScalarAsString(reader)
                    "title" -> title = readJsonScalarAsString(reader)
                    "name" -> name = readJsonScalarAsString(reader)
                    "artist_name", "artistName", "primary_artist", "primaryArtist", "performer" -> {
                        val v = readJsonScalarAsString(reader)
                        if (artist.isBlank() && v.isNotBlank()) artist = v
                    }
                    "artist" -> {
                        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val ak = reader.nextName()
                                if (ak == "name" && reader.peek() != JsonToken.NULL) {
                                    val av = readJsonScalarAsString(reader)
                                    if (artistFallback.isBlank()) artistFallback = av
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endObject()
                        } else {
                            val v = readJsonScalarAsString(reader)
                            if (artistFallback.isBlank()) artistFallback = v
                        }
                    }
                    "stream_url", "streamUrl", "audio_url", "url", "preview" -> {
                        val v = readJsonScalarAsString(reader)
                        if (rawStream.isBlank() && v.isNotBlank()) rawStream = v
                    }
                    "cover_url", "coverUrl", "cover", "artwork" -> {
                        val v = readJsonScalarAsString(reader)
                        if (rawCover.isBlank() && v.isNotBlank()) rawCover = v
                    }
                    "duration" -> {
                        durationRaw = readJsonScalarAsString(reader).toLongOrNull() ?: 0L
                    }
                    "genre", "category" -> {
                        val v = readJsonScalarAsString(reader)
                        if (genre.isBlank() && v.isNotBlank()) genre = v
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            val resolvedId = id.ifBlank { fileId.ifBlank { messageId.ifBlank { index.toString() } } }
            val resolvedTitle = title.ifBlank { name }.ifBlank { "آهنگ بدون نام" }
            val resolvedArtist = artist.ifBlank { artistFallback }.ifBlank { "ناشناس" }

            val streamUrl = when {
                rawStream.startsWith("http") -> rawStream
                rawStream.startsWith("/") -> "$baseUrl$rawStream"
                fileId.isNotBlank() -> "$baseUrl/stream?id=$fileId"
                else -> "$baseUrl/stream?id=$resolvedId"
            }

            val coverUrl = when {
                rawCover.startsWith("http") -> rawCover
                rawCover.startsWith("/") -> "$baseUrl$rawCover"
                rawCover.isNotBlank() -> rawCover
                else -> "$baseUrl/cover?id=$resolvedId&v=hd9"
            }

            outList.add(
                CloudflareTrack(
                    id = resolvedId,
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    streamUrl = streamUrl,
                    coverUrl = coverUrl.takeIf { it.isNotBlank() },
                    durationMs = durationRaw.let { if (it in 1..999) it * 1000L else it },
                    genre = genre.takeIf { it.isNotBlank() }
                )
            )
            index++
        }
        reader.endArray()
    }
}

data class ArchiveArtistItem(
    val name: String,
    val trackCount: Int = 0,
    val cover: String = "",
    val picture: String = cover
)

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
    val rank: Int = 500_000,
    val rawGenre: String = ""
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
        val appTrackId = if (id.startsWith("online_")) id else "online_$id"
        OnlineMusicRepository.registerTrackUrls(
            trackId = id,
            streamUrl = httpStream,
            downloadUrl = finalDownload,
            coverUrl = finalCover,
            artistName = artist
        )
        if (rawGenre.isNotBlank()) {
            RealTrackGenreRegistry.registerGenre(appTrackId, rawGenre)
            RealTrackGenreRegistry.registerGenre(id, rawGenre)
        }
        val cleaned = com.example.data.model.SmartTrackMetadataResolver.resolve(
            rawTitle = title,
            rawArtist = artist,
            rawAlbum = album
        )
        val initialTrack = Track(
            id = appTrackId,
            title = cleaned.title,
            artist = cleaned.artist,
            album = cleaned.album,
            durationMs = (durationSec.coerceAtLeast(1) * 1000L),
            uri = finalStream,
            artworkUri = finalCover.ifBlank { null },
            bitrate = 320,
            sampleRate = 48000,
            genre = rawGenre.ifBlank { "Online" }
        )
        return com.example.data.model.UserTrackMetadataStore.applyOverride(initialTrack)
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

data class CloudArtistProfile(
    val name: String,
    val tracks: List<Track>,
    val soloTracks: List<Track> = tracks,
    val featuredTracks: List<Track> = emptyList(),
    val albums: List<CloudAlbumCollection>,
    val cover: String = "",
    val picture: String = cover
)

data class CloudAlbumCollection(
    val id: String,
    val title: String,
    val artist: String,
    val coverUri: String?,
    val tracks: List<Track>
)

data class SpotifyBrowseCategory(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val subtitleEn: String,
    val subtitleFa: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val matchedTracks: List<Track>
)

private enum class TopHubTab(
    val titleEn: String,
    val titleFa: String,
    val icon: ImageVector
) {
    DISCOVER("All", "همه", Icons.Default.AutoAwesome),
    CATEGORIES("Genres", "سبک‌ها", Icons.Default.GridView),
    ARTISTS("Artists", "هنرمندان", Icons.Default.People),
    ALBUMS("Albums", "آلبوم‌ها", Icons.Default.Album)
}

private sealed class HubNavigationState {
    object MainHub : HubNavigationState()
    data class ArtistPage(val artist: CloudArtistProfile) : HubNavigationState()
    data class AlbumPage(val album: CloudAlbumCollection) : HubNavigationState()
    data class CategoryPage(val category: SpotifyBrowseCategory) : HubNavigationState()
}

object SmartArtistParser {
    private val collabSplitRegex = Regex(
        pattern = """(?i)\s+(?:feat\.?|ft\.?|featuring|with|vs\.?|x|×|&|and|فیت|به\s+همراه|با\s+همراهی|همراه\s+با)\s+|\s*[,،;/|&+]+\s*"""
    )

    private val inlineFeatCheckRegex = Regex(
        pattern = """(?i)(\s+(?:feat\.?|ft\.?|featuring|vs\.?|x|×|&|فیت|به\s+همراه|با\s+همراهی)\s+|[,،;/&+])"""
    )

    private val titleFeatExtractRegex = Regex(
        pattern = """(?i)[\(\[\-\s]+(?:feat\.?|ft\.?|featuring|فیت|به\s+همراه)\s+([^\)\]]+)[\)\]]?"""
    )

    private val trailingFeatStripRegex = Regex(
        pattern = """(?i)\s*[\(\[]?\s*(?:feat\.?|ft\.?|featuring|فیت|به\s+همراه)\b.*$"""
    )

    private const val TYLER_PLACEHOLDER = "__TYLER_THE_CREATOR__"
    private val tylerRegex = Regex("""(?i)tyler\s*,\s*the\s+creator""")

    fun isCollaborationString(rawArtist: String): Boolean {
        val protected = rawArtist.trim().replace(tylerRegex, TYLER_PLACEHOLDER)
        return inlineFeatCheckRegex.containsMatchIn(protected)
    }

    fun cleanSingleArtistName(raw: String): String {
        val stripped = raw
            .replace(TYLER_PLACEHOLDER, "Tyler, The Creator")
            .replace(trailingFeatStripRegex, "")
            .replace(Regex("""^[\(\[\s"'،,;-]+|[\)\]\s"'،,;-]+$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
        return when (stripped.lowercase()) {
            "hesamtiem" -> "Hesam Tiem"
            "tyler, the creator" -> "Tyler, The Creator"
            else -> stripped
        }
    }

    private fun isIgnoredToken(name: String): Boolean {
        val lower = name.lowercase().trim()
        return lower.isEmpty() ||
            lower == "feat" ||
            lower == "ft" ||
            lower == "featuring" ||
            lower == "فیت" ||
            lower == "prod" ||
            lower == "official" ||
            lower == "audio" ||
            lower == "video"
    }

    fun extractArtistsFromTrack(rawArtist: String, rawTitle: String = ""): List<String> {
        val normalizedInput = rawArtist
            .replace(tylerRegex, TYLER_PLACEHOLDER)
            .replace("(", " , ")
            .replace(")", " ")
            .replace("[", " , ")
            .replace("]", " ")

        val rawParts = normalizedInput
            .split(collabSplitRegex)
            .map { cleanSingleArtistName(it) }
            .filter { !isIgnoredToken(it) }
            .toMutableList()

        val titleMatch = titleFeatExtractRegex.find(rawTitle)
        if (titleMatch != null && titleMatch.groupValues.size > 1) {
            val featSegment = titleMatch.groupValues[1].replace(tylerRegex, TYLER_PLACEHOLDER)
            featSegment.split(collabSplitRegex)
                .map { cleanSingleArtistName(it) }
                .filter { !isIgnoredToken(it) }
                .forEach { rawParts.add(it) }
        }

        val unique = LinkedHashMap<String, String>()
        for (part in rawParts) {
            val key = part.lowercase().trim()
            if (key.isNotBlank() && !unique.containsKey(key)) {
                unique[key] = part
            }
        }
        return unique.values.toList()
    }

    fun extractPrimaryArtist(rawArtist: String, rawTitle: String = ""): String {
        return extractArtistsFromTrack(rawArtist, rawTitle).firstOrNull()
            ?: cleanSingleArtistName(rawArtist).ifBlank { "Unknown Artist" }
    }
}

object RealTrackGenreRegistry {
    private val trackGenres = ConcurrentHashMap<String, String>()

    fun registerGenre(trackId: String, rawGenreOrCategory: String?) {
        if (!rawGenreOrCategory.isNullOrBlank()) {
            trackGenres[trackId] = rawGenreOrCategory.trim().lowercase()
        }
    }

    fun getGenre(track: Track): String {
        return trackGenres[track.id].orEmpty().ifBlank { track.genre.lowercase() }
    }
}

object RealAlbumRegistry {

    fun resolveAlbum(title: String, artist: String, rawAlbum: String? = null): String {
        val raw = rawAlbum?.trim().orEmpty()
        if (raw.isNotBlank() &&
            !raw.equals("Single", ignoreCase = true) &&
            !raw.equals("unknown", ignoreCase = true) &&
            !raw.equals("Telegram", ignoreCase = true) &&
            !raw.equals("Telegram Archive", ignoreCase = true) &&
            !raw.startsWith("مجموعه", ignoreCase = true) &&
            !raw.contains("collection", ignoreCase = true) &&
            !raw.contains("category of", ignoreCase = true)
        ) {
            return raw
        }

        val t = title.lowercase()
        val a = artist.lowercase()

        if (a.contains("kanye") || t.contains("kanye")) {
            if (containsAny(t, listOf("good morning", "champion", "stronger", "i wonder", "good life", "can't tell me nothing", "barry bonds", "drunk and hot", "flashing lights", "everything i am", "the glory", "homecoming", "big brother", "good night"))) return "Graduation"
            if (containsAny(t, listOf("heartless", "say you will", "love lockdown", "coldest winter", "paranoid", "robocop", "street lights", "bad news"))) return "808s & Heartbreak"
            if (containsAny(t, listOf("30 hours", "father stretch", "famous", "waves", "ultralight beam", "feedback", "highlights", "freestyle 4", "fml", "real friends", "wolves", "fade", "saint pablo"))) return "The Life of Pablo"
            if (containsAny(t, listOf("carnival", "burn", "stars", "back to me", "paid", "talking", "fuk sumn", "vultures"))) return "Vultures 1"
            if (containsAny(t, listOf("power", "runaway", "monster", "devil in a new dress", "all of the lights", "dark fantasy", "gorgeous", "so appalled", "hell of a life", "lost in the world"))) return "My Beautiful Dark Twisted Fantasy"
            if (containsAny(t, listOf("bound 2", "black skinhead", "blood on the leaves", "on sight", "new slaves"))) return "Yeezus"
            if (containsAny(t, listOf("off the grid", "hurricane", "praise god", "moon", "jail", "jonah", "ok ok", "pure souls"))) return "Donda"
        }

        if (a.contains("juice wrld") || t.contains("juice wrld")) {
            if (containsAny(t, listOf("wishing well", "righteous", "come & go", "smile", "tell me u luv me", "hate the other side", "blood on my jeans", "bad energy", "man of the year", "up up and away", "titanic", "fighting demons", "stay high", "can't die", "conversations", "anxiety", "screw juice", "life's a mess", "myth, the legend", "speaks from heaven"))) return "Legends Never Die"
            if (containsAny(t, listOf("lucid dreams", "all girls are the same", "lean wit me", "armed and dangerous", "wasted", "scared of love", "hurt me", "black & white"))) return "Goodbye & Good Riddance"
            if (containsAny(t, listOf("robbery", "hear me calling", "fast", "empty", "bandit", "flaws and sins", "maze"))) return "Death Race for Love"
        }

        if (containsAny(t, listOf("we don't trust you", "type shit", "like that", "young metro", "ice attack", "cinderella", "fried", "everyday hustle", "wtfym", "ain't no love", "seen it all", "came to the party", "claustrophobic", "magic don juan", "slimed in"))) {
            return "WE DON'T TRUST YOU"
        }
        if (containsAny(t, listOf("superhero", "too many nights", "lock on me", "i can't save you", "metro spider", "creepin", "niagara falls", "umbrella", "trance", "around me", "feel the fiaaaah", "all the money"))) {
            return "HEROES & VILLAINS"
        }

        if (a.contains("future")) {
            if (containsAny(t, listOf("712pm", "i'm on one", "wait for u", "puffin on zootiez", "love you better", "massaging me", "gold stacks", "voodoo", "holy ghost"))) return "I NEVER LIKED YOU"
            if (containsAny(t, listOf("mask off", "scrape", "super trapper", "draco", "rent money", "zoom", "poppin tags"))) return "FUTURE"
            if (containsAny(t, listOf("thought it was a drought", "where ya at", "stick talk", "blood on the money", "percocet & stripper joint", "blow a bag", "trap niggas", "real sisters", "slave master", "kno the meaning", "fuck up some commas"))) return "DS2"
            if (containsAny(t, listOf("codeine crazy", "monster", "throw away", "my savages", "gangland", "hardly"))) return "Monster"
            if (containsAny(t, listOf("march madness", "56 nights", "diamonds from africa", "never gon lose"))) return "56 Nights"
            if (containsAny(t, listOf("purple reign", "wicked", "all right", "hater shit", "inside the mattress", "run up", "drip on me", "married to the game"))) return "Purple Reign"
            if (containsAny(t, listOf("life is good", "solitaires", "accepting my flaws", "too comfortable", "hard to choose one", "touch the sky", "hi tek tek", "riding strikers", "one of my", "posted with demons"))) return "High Off Life"
            if (containsAny(t, listOf("crushed up", "rocket ship", "jumpin on a jet", "first off", "never stop", "temptation", "baptiize"))) return "The WIZRD"
            if (containsAny(t, listOf("drankin n smokin", "real baby pluto", "stripes like burberry", "million dollar play", "plastic", "sleeping on the floor"))) return "Pluto x Baby Pluto"
            if (containsAny(t, listOf("no cap", "all da smoke", "feed me dope", "f&n", "patek water", "200", "killed before", "three", "drip on me"))) return "Super Slimey"
            if (containsAny(t, listOf("lil demon", "kick", "eye to eye", "if i could", "jersey", "fukk a interview", "no misery", "tank top pluto", "weight up", "money over everything", "streets made me a king", "out of my hands", "teflon don", "plutoski", "ski", "surfer", "ocean", "south of france"))) return "MIXTAPE PLUTO"
            if (containsAny(t, listOf("low life", "maybach", "fly shit only", "xanny family", "photo copied", "seven rings"))) return "EVOL"
        }

        if (a.contains("kendrick") || t.contains("kendrick")) {
            if (containsAny(t, listOf("humble", "love.", "lust.", "duckworth", "dna.", "element", "loyalty", "pride", "xxx", "fear", "god.", "feel."))) return "DAMN."
            if (containsAny(t, listOf("wacced out murals", "squabble up", "tv off", "chains & whips", "luther", "man at the garden", "reincarnated", "peekaboo", "dodger blue", "gloria", "heart pt. 6"))) return "GNX"
            if (t.contains("not like us") || t.contains("meet the grahams") || t.contains("euphoria") || t.contains("6:16 in la")) return "Not Like Us"
            if (containsAny(t, listOf("swimming pools", "bitch, don't kill my vibe", "m.a.a.d city", "poetic justice", "money trees", "backseat freestyle"))) return "good kid, m.A.A.d city"
            if (containsAny(t, listOf("alright", "king kunta", "the blacker the berry", "these walls", "i"))) return "To Pimp a Butterfly"
            if (containsAny(t, listOf("n95", "die hard", "father time", "rich spirit", "count me out", "silent hill", "united in grief"))) return "Mr. Morale & the Big Steppers"
        }

        if (a.contains("drake")) {
            if (containsAny(t, listOf("fair trade", "way 2 sexy", "wants and needs", "laugh now cry later", "girls want girls", "champagne poetry", "tsu", "in the bible", "love all", "pipe down", "no friends in the industry", "knife talk", "7am on bridle path", "race my mind", "fountains", "get along better", "you only live twice", "the remorse"))) return "Certified Lover Boy"
            if (containsAny(t, listOf("jimmy cooks", "massive", "falling back", "sticky", "texts go green", "currents", "a keeper", "flights booked", "overdrive", "down hill", "tie that binds", "liability"))) return "Honestly, Nevermind"
            if (containsAny(t, listOf("jumpman", "digital dash", "scholarships", "live from the gutter", "i'm the plug", "diamonds dancing", "big rings", "change locations", "plastic bag"))) return "What a Time to Be Alive"
            if (containsAny(t, listOf("lose you", "teenage fever", "get it together", "passionfruit", "fake love", "gyalchester", "bleep", "free smoke", "sacrifices", "blem", "4422", "kmt", "glow", "since way back", "ice melts", "do not disturb"))) return "More Life"
            if (containsAny(t, listOf("9", "one dance", "grammys", "u with me?", "feel no ways", "controlla", "too good", "pop style", "fire & desire", "hype", "weston road flows", "redemption", "with you", "faithful", "childs play"))) return "Views"
            if (containsAny(t, listOf("i'm upset", "mob ties", "nonstop", "god's plan", "nice for what", "in my feelings", "don't matter to me", "elevate", "emotionless", "survival", "talk up", "can't take a joke", "jaded", "finesse", "ratchet happy birthday", "blue tint", "after dark", "march 14"))) return "Scorpion"
            if (containsAny(t, listOf("toosie slide", "desires", "chicago freestyle", "war", "pain 1993", "demons", "time flies", "landed", "d4l", "not you too", "from florida with love"))) return "Dark Lane Demo Tapes"
            if (containsAny(t, listOf("jungle", "know yourself", "energy", "10 bands", "legend", "no tellin", "star67", "preach", "wednesday night interlude", "used to", "company", "you & the 6"))) return "If You're Reading This It's Too Late"
            if (containsAny(t, listOf("rich flex", "major distribution", "on bs", "spin bout u", "circo loco", "pussy & millions", "broke boys", "middle of the ocean", "treacherous twins", "privilege", "hours in silence", "more m's", "i guess it's fuck me"))) return "Her Loss"
            if (containsAny(t, listOf("slime you out", "first person shooter", "idgaf", "virginia beach", "amen", "fear of heights", "daylight", "search & rescue", "bahamas promises", "tried our best", "members only", "drew a picasso", "what would pluto do", "all the parties", "rich baby daddy", "another late night", "away from home", "polar opposites"))) return "For All The Dogs"
            if (containsAny(t, listOf("money in the grave", "omertà"))) return "The Best in the World Pack"
        }

        if (a.contains("travis") || t.contains("travis")) {
            if (containsAny(t, listOf("fe!n", "telekinesis", "say my grace", "modern jam", "topia twins", "meltdown", "hyaena", "thank god", "sirens", "my eyes", "god's country", "delresto", "i know ?", "circus maximus", "parasail", "skitzo", "lost forever", "looove", "k-pop", "telekenesis"))) return "UTOPIA"
            if (containsAny(t, listOf("goosebumps", "sweet sweet", "pick up the phone", "beebs in the trap", "through the late night", "coordinate", "the ends", "way back", "first take", "sdp interlude", "outside", "guidance", "wonderful"))) return "Birds in the Trap Sing McKnight"
            if (containsAny(t, listOf("highest in the room", "gatti", "out west", "what to do", "gang gang", "had enough"))) return "JACKBOYS"
            if (containsAny(t, listOf("sicko mode", "stargazing", "butterfly effect", "yosemite", "stop trying to be god", "can't say", "carousel", "wake up", "5% tint", "houstonfornication", "astroworld", "rip screw", "no bystanders", "skeletons", "nc-17", "who? what!", "coffee bean"))) return "Astroworld"
            if (containsAny(t, listOf("antidote", "3500", "90210", "maria i'm drunk", "pray 4 love", "nightcrawler", "pornography", "oh my dis side", "wasted", "flying high", "i can tell", "apple pie", "never catch me"))) return "Rodeo"
        }

        if (a.contains("zedbazi") || a.contains("hidden") || a.contains("sijal") || a.contains("alireza jj") || a.contains("sohrab mj")) {
            if (containsAny(t, listOf("cigare soorati", "chera badi", "tehran mibinamet", "bekhand masnooyi", "daro baz kon", "delam tang shode", "mahdekoodak", "pedare man bad mano", "nabayad vaysim", "nakoni bavar", "zakhar nameh", "montazer boodan"))) return "Zakhar Nameh"
            if (containsAny(t, listOf("tabestoon kootahe", "zamin seafe", "zamin safe", "zamin saafe", "kooche", "party", "iroonie l.a.", "bi hess", "obi", "disss agin", "berim faza", "shahremon tehran"))) return "Zedbazi Classics"
            if (containsAny(t, listOf("old money", "talafaate jang", "bache mahal", "khodesh midoone khoobe"))) return "Zedbazi Reunion"
            if (containsAny(t, listOf("fooroodgah", "to hamishe bekhand", "bill", "eloel", "berizam beham", "rooz az no", "raha", "ghol midam", "solhe daroon", "sefr"))) return "Sefr"
            if (containsAny(t, listOf("miresam", "bitab", "ghesse", "favaran", "balance", "nakhla", "toonel", "kalas", "omadam to donya", "shoroo", "bargardam"))) return "Toonel"
            if (containsAny(t, listOf("bozorg", "poshtdast", "gorgo mish pt. 2", "raftam", "ma", "kocheye ma", "tond naro", "sarab", "yeki dige", "parizan"))) return "Bozorg (Vol. 2)"
            if (containsAny(t, listOf("pir shodim vali bozorg na", "+3:30 tehran", "golo sangam", "sobe sobe", "har chi bade", "mehmooni khodemoone", "eyval eyval", "jozvi az man", "aston villa", "jack o ji", "ghable ejraa"))) return "Pir Shodim Vali Bozorg Na"
            if (containsAny(t, listOf("az man che khabar", "berim baham", "saal be saal", "maro eshtebah nagir", "chemical", "hate", "kaafar", "eva mendes", "bobby", "psycho", "balance", "bang bang"))) return "Yadgari JJ"
        }

        if (a.contains("khalse")) {
            if (containsAny(t, listOf("terapy", "therapy", "marde saal", "tool keshid", "prada", "number one", "after party", "penalty", "sober", "dokhtar", "chashm dast"))) return "Therapy"
            if (containsAny(t, listOf("fekr o khial", "salibi", "vancouver", "fana", "fekret", "yadegari", "cheghad sadeh", "boghz", "kamel"))) return "Yadegari"
            if (containsAny(t, listOf("dozdi ni", "day one", "begid berizan", "adat nadaram", "siah mese barf", "enferadi", "ghol", "bache bala"))) return "Enferadi"
            if (containsAny(t, listOf("pesare maryam", "heif", "antalya", "barbie & ken", "shahraam", "bombastic", "lifestyle", "champion", "az yad miram"))) return "Pesare Maryam"
            if (containsAny(t, listOf("kilimanjaro", "har chand vaght ye bar", "shar ba to nadarim", "gole man", "nameh be baba", "nagoo mese oonaam"))) return "Fekr o Khial"
        }

        if (a.contains("leito")) {
            if (containsAny(t, listOf("penalty", "ta betoonam", "del too delam nist", "2cb", "ravanitam", "fogholade", "nesfe raah", "1.6", "bazigar", "23", "ono mikham", "khobesh"))) return "23 (Bist O Seh)"
            if (containsAny(t, listOf("golo sangam", "+3:30 tehran", "jetlag", "traplife", "setarebazi", "setare bazi", "ghalat kardam", "man bahat gharam"))) return "Setarebazi"
            if (containsAny(t, listOf("ba man harf bezan", "bazam ke hichi", "bezar beri yadam", "mesle ghablan", "doost daram hamishe", "be goosham resid", "bi to", "chera rafti", "boodi bedehkar", "khabaraye bad"))) return "Khabaraye Bad"
            if (containsAny(t, listOf("faaze khalse", "palang", "doostam hastan", "tirip", "az alan be bad", "khakestari", "dore dor", "oon bala sari"))) return "Khakestari"
        }

        if (a.contains("vinak")) {
            if (containsAny(t, listOf("mi vida loca", "delgire", "tarantula", "shot biriz", "ba gun", "saret miad", "be darak", "jolly chimp", "javid shah", "payande", "yadam ni"))) return "Mi Vida Loca"
            if (containsAny(t, listOf("ghamar dar aghrab", "belaad gang", "flow", "paraffin", "bavar kon", "ghablana", "candy", "bi setare", "baila conmigo", "yadetam"))) return "Ghamar Dar Aghrab"
            if (containsAny(t, listOf("gin", "ama to migi na", "karam hite", "moosh zade", "cuckoldy"))) return "Gin & Diss"
        }

        if (a.contains("koorosh")) {
            if (containsAny(t, listOf("sphere", "catalan", "fiancée", "mituni mage", "bahare ma gozashte", "little boy", "420"))) return "420"
            if (containsAny(t, listOf("trap mode", "kulak", "farkarala", "too in khoone", "adamaye kiri", "caesar salad", "ma mizanim", "tdk", "roulette", "cheghad goftam"))) return "Trap Mode"
            if (containsAny(t, listOf("casa blanca", "dashte parvaneha", "mage man mordam", "vali khoobe", "marge roya", "shawty", "yallah", "vanish", "mosafer", "ck", "homealone"))) return "Mosafer"
        }

        if (a.contains("hiphopologist")) {
            if (containsAny(t, listOf("prada", "bache mardom", "mama koshtamesh", "sarbaz", "khode ghadimim", "erect", "double shot", "shahin felakat", "capsule", "bad b"))) return "Capsule"
            if (containsAny(t, listOf("binge", "rose 2", "dna", "kharabat", "khodaya mano bebakhsh", "shawty", "rothman", "carina", "100"))) return "Rothman"
            if (containsAny(t, listOf("mamanesh bishtar", "enhelale label", "plea deal", "ta dd soroush", "bare akhar", "bezar jibet baby"))) return "RapGame"
        }

        if (a.contains("shaggy")) {
            if (containsAny(t, listOf("jersey", "schizophrenia", "two big ones", "adlib", "kobra11", "bang o hashish", "oh shit", "die hard", "after party", "we will rock", "vice city", "raghse andam", "1.6", "sorry", "i walk alone"))) return "Bozorg Sal"
        }

        if (a.contains("tijay")) return "Dead Hand"
        if (a.contains("papa")) return "We Are Papa"
        if (a.contains("isam")) return "PARIS"
        if (a.contains("gucci")) return "SYRUP SIPPIN"
        if (a.contains("tohi")) return "11:11"
        if (a.contains("tataloo")) return "78"
        if (a.contains("poori")) return "Godfather"

        if (a.contains("eminem")) {
            if (containsAny(t, listOf("the real slim shady", "without me", "stan", "lose yourself", "cleanin' out my closet", "sing for the moment", "till i collapse", "superman", "white america", "hailie's song"))) return "The Eminem Show"
            if (containsAny(t, listOf("not afraid", "love the way you lie", "space bound", "no love", "cinderella man", "25 to life", "won't back down"))) return "Recovery"
            if (containsAny(t, listOf("beautiful", "crack a bottle", "we made you", "3 a.m.", "stay wide awake", "careful what you wish for"))) return "Relapse"
            if (containsAny(t, listOf("venom", "fall", "lucky you", "kamikaze", "the ringer", "greatest", "not alike", "stepping stone"))) return "Kamikaze"
            if (containsAny(t, listOf("godzilla", "darkness", "gnat", "lock it up", "premonition", "unaccommodating", "you gon' learn"))) return "Music to Be Murdered By"
        }

        if (a.contains("post malone")) {
            if (containsAny(t, listOf("circles", "goodbyes", "take what you want", "hollywood's bleeding", "sunflower", "enemies", "saint-tropez"))) return "Hollywood's Bleeding"
            if (containsAny(t, listOf("rockstar", "psycho", "better now", "stay", "ball for me", "paranoid", "rich & sad"))) return "beerbongs & bentleys"
            if (containsAny(t, listOf("white iverson", "congratulations", "i fall apart", "deja vu", "go flex", "patient"))) return "Stoney"
        }

        return "Single"
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { kw -> text.contains(kw) }
    }
}

object RealCategoryClassifier {

    private val rapArtistsAndKeywords = listOf(
        "rap", "hip hop", "hip-hop", "hiphop", "trap", "drill", "freestyle", "diss", "cypher",
        "رپ", "هیپ هاپ", "هیپ‌هاپ", "دیس", "فری استایل", "ترپ",
        "yas", "hichkas", "pishro", "reza pishro", "ho3ein", "shayea", "bahram", "sorena",
        "ali sorena", "fadaei", "qmars", "quf", "putak", "poori", "koorosh", "hidden",
        "mehrad hidden", "sohrab mj", "sijal", "khalse", "sepehr khalse", "leito", "behzad leito",
        "zedbazi", "tataloo", "amirtataloo", "021kid", "chvrsi", "hiphopologist", "dorcci",
        "eminem", "drake", "kendrick", "travis scott", "2pac", "50 cent", "kanye", "future",
        "metro boomin", "central cee", "pop smoke", "nf", "logic", "j. cole", "nicki minaj",
        "یاس", "هیچکس", "پیشرو", "حصین", "شایع", "بهرام", "سورنا", "فدایی", "پوتک", "پوری",
        "مهراد هیدن", "سهراب ام جی", "سیجل", "خلصه", "لیتو", "بهزاد لیتو", "زدبازی", "تتلو"
    )

    private val electronicRemixKeywords = listOf(
        "remix", "club", "dance", "edm", "house", "techno", "trance", "dubstep", "electro",
        "dj ", "dj-", "mix", "bootleg", "mashup", "phonk", "synthwave",
        "ریمیکس", "دی جی", "دی‌جی", "پادکست", "میکس", "تکنو", "هاوس", "کلاب", "رقص",
        "tiesto", "david guetta", "calvin harris", "alan walker", "marshmello", "skrillex",
        "armin van buuren", "martin garrix", "kygo", "daft punk", "alok", "meduza", "anyma"
    )

    private val rockMetalKeywords = listOf(
        "rock", "metal", "alternative", "indie rock", "punk", "grunge", "hard rock", "heavy metal",
        "acoustic rock", "guitar solo",
        "راک", "متال", "پانک", "آلترناتیو",
        "linkin park", "metallica", "queen", "nirvana", "pink floyd", "ac/dc", "guns n' roses",
        "radiohead", "coldplay", "imagine dragons", "arctic monkeys", "slipknot", "system of a down",
        "evanescence", "scorpions", "green day", "muse", "red hot chili", "theparvaz", "kaveh yaghmaei",
        "kourosh yaghmaei", "کاوه یغمایی", "کوروش یغمایی", "رستاک"
    )

    private val chillInstrumentalKeywords = listOf(
        "chill", "lofi", "lo-fi", "ambient", "instrumental", "piano", "violin", "guitar",
        "sleep", "relax", "study", "meditation", "soundtrack", "ost", "score", "acoustic",
        "بی کلام", "بی‌کلام", "لایت", "پیانو", "ویولن", "گیتار", "آرامش", "خواب", "لوفای", "سنتور",
        "hans zimmer", "ludovico einaudi", "max richter", "yanni", "vangelis", "fariborz lachini",
        "shahrdad rohani", "nasser cheshmazar", "فریبرز لاچینی", "ناصر چشم آذر", "شهرداد روحانی",
        "کیهان کلهر", "انوشتین", "بیژن مرتضوی"
    )

    private val traditionalClassicalKeywords = listOf(
        "traditional", "classical", "folk", "sonati", "persian classical", "ghazal", "bastani",
        "سنتی", "کلاسیک", "اصیل", "تصنیف", "آواز", "تار", "سه تار", "سه‌تار", "دف", "نی ",
        "shajarian", "homayoun shajarian", "mohammadreza shajarian", "shahram nazeri",
        "alireza ghorbani", "salar aghili", "parvaz homay", "irej", "golpa", "haydeh",
        "mahasti", "homeyra", "ebrahim hamedi", "ebi", "dariush", "googoosh", "moein", "siavash ghomayshi",
        "شجریان", "همایون شجریان", "محمدرضا شجریان", "شهرام ناظری", "علیرضا قربانی", "سالار عقیلی",
        "پرواز همای", "هایده", "مهستی", "حمیرا", "معین", "داریوش", "ابی", "گوگوش", "سیاوش قمیشی"
    )

    private val romanticSlowKeywords = listOf(
        "love", "romantic", "slow", "ballad", "sad", "emotional", "heart", "broken", "valentine",
        "عاشقانه", "رمانتیک", "غمگین", "احساسی", "دلشکسته", "تنهایی", "بارون", "باران", "عشق", "دلتنگی",
        "محسن یگانه", "محسن چاوشی", "مرتضی پاشایی", "شادمهر", "شادمهر عقیلی", "فرزاد فرزین",
        "رضا بهرام", "راغب", "گرشا رضایی", "حامیم", "عرفان طهماسبی", "امیرعباس گلاب", "علی یاسینی",
        "مهدی احمدوند", "میثم ابراهیمی", "بهنام بانی", "آصف آریا", "ناصر زینعلی", "ماکان بند",
        "mohsen yeganeh", "mohsen chavoshi", "morteza pashaei", "shadmehr", "shadmehr aghili",
        "reza bahram", "haamim", "erfan tahmasbi", "ali yasini", "mehdi ahmadvand", "adele",
        "lana del rey", "billie eilish", "the weeknd", "ed sheeran", "taylor swift", "sam smith"
    )

    private fun buildSearchableMeta(track: Track): String {
        val registeredGenre = RealTrackGenreRegistry.getGenre(track)
        return "${track.title} ${track.artist} ${track.album} $registeredGenre".lowercase()
    }

    private fun containsAnyKeyword(meta: String, keywords: List<String>): Boolean {
        return keywords.any { kw -> meta.contains(kw) }
    }

    private fun hasPersianScript(text: String): Boolean {
        return text.any { ch -> ch in '\u0600'..'\u06FF' }
    }

    fun buildRealCategories(allTracks: List<Track>): List<SpotifyBrowseCategory> {
        if (allTracks.isEmpty()) return emptyList()

        val rawCategories = mutableListOf<SpotifyBrowseCategory>()

        val rapTracks = allTracks.filter { track ->
            containsAnyKeyword(buildSearchableMeta(track), rapArtistsAndKeywords)
        }
        if (rapTracks.isNotEmpty()) {
            rawCategories.add(
                SpotifyBrowseCategory(
                    id = "rap_hiphop",
                    titleEn = "Hip-Hop & Rap",
                    titleFa = "رپ و هیپ‌هاپ",
                    subtitleEn = "${rapTracks.size} tracks",
                    subtitleFa = "${rapTracks.size} آهنگ",
                    icon = Icons.Default.GraphicEq,
                    gradient = listOf(Color(0xFFBA5D07), Color(0xFF7C3A03)),
                    matchedTracks = rapTracks
                )
            )
        }

        val romanticTracks = allTracks.filter { track ->
            val meta = buildSearchableMeta(track)
            containsAnyKeyword(meta, romanticSlowKeywords) &&
                !containsAnyKeyword(meta, rapArtistsAndKeywords) &&
                !containsAnyKeyword(meta, electronicRemixKeywords)
        }
        if (romanticTracks.isNotEmpty()) {
            rawCategories.add(
                SpotifyBrowseCategory(
                    id = "romantic_love",
                    titleEn = "Chill & Romance",
                    titleFa = "عاشقانه و احساسی",
                    subtitleEn = "${romanticTracks.size} tracks",
                    subtitleFa = "${romanticTracks.size} آهنگ",
                    icon = Icons.Default.Favorite,
                    gradient = listOf(Color(0xFFE1118C), Color(0xFF8D0A57)),
                    matchedTracks = romanticTracks
                )
            )
        }

        val remixTracks = allTracks.filter { track ->
            containsAnyKeyword(buildSearchableMeta(track), electronicRemixKeywords)
        }
        if (remixTracks.isNotEmpty()) {
            rawCategories.add(
                SpotifyBrowseCategory(
                    id = "electronic_remix",
                    titleEn = "Dance & Electronic",
                    titleFa = "ریمیکس و الکترونیک",
                    subtitleEn = "${remixTracks.size} tracks",
                    subtitleFa = "${remixTracks.size} آهنگ",
                    icon = Icons.Default.Bolt,
                    gradient = listOf(Color(0xFF0D73EC), Color(0xFF08448C)),
                    matchedTracks = remixTracks
                )
            )
        }

        val chillTracks = allTracks.filter { track ->
            containsAnyKeyword(buildSearchableMeta(track), chillInstrumentalKeywords)
        }
        if (chillTracks.isNotEmpty()) {
            rawCategories.add(
                SpotifyBrowseCategory(
                    id = "chill_instrumental",
                    titleEn = "Instrumental & Lo-Fi",
                    titleFa = "بی‌کلام و آرامش‌بخش",
                    subtitleEn = "${chillTracks.size} tracks",
                    subtitleFa = "${chillTracks.size} آهنگ",
                    icon = Icons.Default.NightsStay,
                    gradient = listOf(Color(0xFF148A08), Color(0xFF0B4F04)),
                    matchedTracks = chillTracks
                )
            )
        }

        val traditionalTracks = allTracks.filter { track ->
            containsAnyKeyword(buildSearchableMeta(track), traditionalClassicalKeywords)
        }
        if (traditionalTracks.isNotEmpty()) {
            rawCategories.add(
                SpotifyBrowseCategory(
                    id = "traditional_nostalgia",
                    titleEn = "Classics & Legends",
                    titleFa = "سنتی و نوستالژی",
                    subtitleEn = "${traditionalTracks.size} tracks",
                    subtitleFa = "${traditionalTracks.size} آهنگ",
                    icon = Icons.Default.History,
                    gradient = listOf(Color(0xFF8D67AB), Color(0xFF533A68)),
                    matchedTracks = traditionalTracks
                )
            )
        }

        val rockTracks = allTracks.filter { track ->
            containsAnyKeyword(buildSearchableMeta(track), rockMetalKeywords)
        }
        if (rockTracks.isNotEmpty()) {
            rawCategories.add(
                SpotifyBrowseCategory(
                    id = "rock_metal",
                    titleEn = "Rock & Alternative",
                    titleFa = "راک و متال",
                    subtitleEn = "${rockTracks.size} tracks",
                    subtitleFa = "${rockTracks.size} آهنگ",
                    icon = Icons.Default.Speaker,
                    gradient = listOf(Color(0xFFE91429), Color(0xFF8C0B18)),
                    matchedTracks = rockTracks
                )
            )
        }

        val persianTracks = allTracks.filter { track ->
            val isFa = hasPersianScript(track.title) || hasPersianScript(track.artist)
            val isRap = containsAnyKeyword(buildSearchableMeta(track), rapArtistsAndKeywords)
            isFa && !isRap
        }
        if (persianTracks.isNotEmpty()) {
            rawCategories.add(
                SpotifyBrowseCategory(
                    id = "persian_pop",
                    titleEn = "Persian Pop",
                    titleFa = "پاپ فارسی",
                    subtitleEn = "${persianTracks.size} tracks",
                    subtitleFa = "${persianTracks.size} آهنگ",
                    icon = Icons.Default.MusicNote,
                    gradient = listOf(Color(0xFF1DB954), Color(0xFF0E632C)),
                    matchedTracks = persianTracks
                )
            )
        }

        val internationalTracks = allTracks.filter { track ->
            !hasPersianScript(track.title) && !hasPersianScript(track.artist) && !hasPersianScript(track.album)
        }
        if (internationalTracks.isNotEmpty()) {
            rawCategories.add(
                SpotifyBrowseCategory(
                    id = "international_hits",
                    titleEn = "Global Hits",
                    titleFa = "موزیک خارجی",
                    subtitleEn = "${internationalTracks.size} tracks",
                    subtitleFa = "${internationalTracks.size} آهنگ",
                    icon = Icons.Default.Public,
                    gradient = listOf(Color(0xFF503750), Color(0xFF2E1F2E)),
                    matchedTracks = internationalTracks
                )
            )
        }

        return rawCategories
    }
}

data class HubComputedData(
    val albums: List<CloudAlbumCollection> = emptyList(),
    val artists: List<CloudArtistProfile> = emptyList(),
    val browseCategories: List<SpotifyBrowseCategory> = emptyList(),
    val artistRows: List<List<CloudArtistProfile>> = emptyList(),
    val albumRows: List<List<CloudAlbumCollection>> = emptyList(),
    val categoryRows: List<List<SpotifyBrowseCategory>> = emptyList()
)

object OnlineMusicRepository {
    const val WORKER_URL = "https://alubm.parsaghorbani8861.workers.dev/"
    val BASE_URL: String get() = WORKER_URL.trimEnd('/')
    private const val PRIVATE_CATALOG_CACHE_FILE = "aura_cloud_catalog_cache_v4.json"
    private const val LEGACY_CATALOG_CACHE_FILE_V3 = "aura_cloud_catalog_cache_v3.json"
    private const val LEGACY_CATALOG_CACHE_FILE = "aura_cloud_catalog_cache_v2.json"
    private const val PRIVATE_TRACK_COVERS_DIR = "track_covers_private_cache_hd_v3"
    private val LEGACY_TRACK_COVERS_DIRS = listOf("track_covers_private_cache", "track_covers_private_cache_v2", "track_covers_private_cache_hd")
    private val WORKER_HOST_REGEX = Regex("https?://[^/]+\\.workers\\.dev", RegexOption.IGNORE_CASE)

    private val imageMemoryCache = object : LruCache<String, Bitmap>(240) {}
    private val failedCoverUrls = ConcurrentHashMap.newKeySet<String>()
    private val coverDownloadLocks = ConcurrentHashMap<String, Any>()
    private val coverIoSemaphore = Semaphore(10)
    private val cleanedLegacyTrackCovers = AtomicBoolean(false)
    private val catalogFetchMutex = kotlinx.coroutines.sync.Mutex()
    private val backgroundWarmScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val backgroundWarmStarted = AtomicBoolean(false)
    private val startupPreloadCompleted = AtomicBoolean(false)
    private val streamUrlByTrackId = ConcurrentHashMap<String, String>()
    private val downloadUrlByTrackId = ConcurrentHashMap<String, String>()
    private val coverUrlByTrackId = ConcurrentHashMap<String, String>()
    private val artistPictureCache = ConcurrentHashMap<String, String>()
    private val hubDataCache = ConcurrentHashMap<String, HubComputedData>()

    @Volatile
    private var lastKnownArtists: List<ArchiveArtistItem> = emptyList()

    @Volatile
    private var memoryCachedResponse: WorkerTracksResponse? = null

    @Volatile
    private var lastFullFetchTimeMs: Long = 0L

    @Volatile
    private var memoryCachedAppTracks: List<Track> = emptyList()

    private val _onlineTracksFlow = MutableStateFlow<List<Track>>(emptyList())
    val onlineTracksFlow: StateFlow<List<Track>> = _onlineTracksFlow.asStateFlow()

    private val _isFetchingFlow = MutableStateFlow(false)
    val isFetchingFlow: StateFlow<Boolean> = _isFetchingFlow.asStateFlow()

    fun isStartupPreloadCompleted(): Boolean = startupPreloadCompleted.get()

    fun markStartupPreloadCompleted() {
        startupPreloadCompleted.set(true)
    }

    fun startBackgroundPreload(
        context: Context,
        localTracks: List<Track>,
        isPersian: Boolean,
        onProgress: (Float, String) -> Unit
    ) {
        backgroundWarmScope.launch {
            try {
                preloadStartupEverything(
                    context = context,
                    localTracks = localTracks,
                    isPersian = isPersian,
                    onProgress = onProgress
                )
            } catch (_: Throwable) {
                startupPreloadCompleted.set(true)
            }
        }
    }

    fun getMemoryCachedAppTracks(): List<Track> = memoryCachedAppTracks

    fun setMemoryCachedAppTracks(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            memoryCachedAppTracks = tracks
            _onlineTracksFlow.value = tracks
        }
    }

    suspend fun loadCachedAppTracksAsync(): List<Track> = withContext(Dispatchers.IO) {
        if (memoryCachedAppTracks.isNotEmpty()) {
            if (_onlineTracksFlow.value.isEmpty()) {
                _onlineTracksFlow.value = memoryCachedAppTracks
            }
            return@withContext memoryCachedAppTracks
        }
        val cached = loadCachedWorkerCatalog()
        val mapped = cached?.tracks?.map { it.toAppTrack() }.orEmpty()
        if (mapped.isNotEmpty()) {
            memoryCachedAppTracks = mapped
            _onlineTracksFlow.value = mapped
        }
        mapped
    }

    private fun buildHubSignature(tracks: List<Track>, isPersian: Boolean): String {
        return "${tracks.size}_${tracks.firstOrNull()?.id}_${tracks.lastOrNull()?.id}_$isPersian"
    }

    fun getMemoryCachedHubData(tracks: List<Track>, isPersian: Boolean): HubComputedData? {
        if (tracks.isEmpty()) return HubComputedData()
        val sig = buildHubSignature(tracks, isPersian)
        hubDataCache[sig]?.let { return it }
        if (memoryCachedAppTracks.isNotEmpty()) {
            val baseSig = buildHubSignature(memoryCachedAppTracks, isPersian)
            hubDataCache[baseSig]?.let { return it }
        }
        return null
    }

    suspend fun computeHubDataAsync(tracks: List<Track>, isPersian: Boolean): HubComputedData =
        withContext(Dispatchers.Default) {
            if (tracks.isEmpty()) return@withContext HubComputedData()
            val sig = buildHubSignature(tracks, isPersian)
            hubDataCache[sig]?.let { if (it.artists.isNotEmpty()) return@withContext it }

            val workerArtistsMap = getKnownArtists().associateBy { it.name.trim().lowercase() }
            val canonicalDisplayNames = LinkedHashMap<String, String>()
            val artistAllTracks = LinkedHashMap<String, MutableList<Track>>()
            val artistSoloTracks = LinkedHashMap<String, MutableList<Track>>()
            val artistFeatTracks = LinkedHashMap<String, MutableList<Track>>()

            workerArtistsMap.values.forEach { item ->
                val clean = SmartArtistParser.cleanSingleArtistName(item.name)
                if (clean.isNotBlank() && !SmartArtistParser.isCollaborationString(clean)) {
                    canonicalDisplayNames[clean.lowercase()] = clean
                }
            }

            for (track in tracks) {
                val participants = SmartArtistParser.extractArtistsFromTrack(track.artist, track.title)
                    .ifEmpty {
                        listOf(if (isPersian) "هنرمند ناشناس" else "Unknown Artist")
                    }
                val isCollabTrack = participants.size > 1 || SmartArtistParser.isCollaborationString(track.artist)

                participants.forEachIndexed { idx, participantName ->
                    val key = participantName.lowercase().trim()
                    if (key.isBlank()) return@forEachIndexed

                    if (!canonicalDisplayNames.containsKey(key)) {
                        canonicalDisplayNames[key] = participantName
                    }

                    val allList = artistAllTracks.getOrPut(key) { mutableListOf() }
                    if (allList.none { it.id == track.id }) {
                        allList.add(track)
                    }

                    if (!isCollabTrack && idx == 0) {
                        val soloList = artistSoloTracks.getOrPut(key) { mutableListOf() }
                        if (soloList.none { it.id == track.id }) {
                            soloList.add(track)
                        }
                    } else {
                        val featList = artistFeatTracks.getOrPut(key) { mutableListOf() }
                        if (featList.none { it.id == track.id }) {
                            featList.add(track)
                        }
                    }
                }
            }

            val allAlbumsList = mutableListOf<CloudAlbumCollection>()
            val albumsByArtistKey = LinkedHashMap<String, MutableList<CloudAlbumCollection>>()

            val validAlbumTracks = tracks.filter { trk ->
                val alb = trk.album.trim()
                alb.isNotBlank() &&
                    !alb.equals("Single", ignoreCase = true) &&
                    !alb.equals("unknown", ignoreCase = true) &&
                    !alb.equals("Telegram", ignoreCase = true) &&
                    !alb.equals("Telegram Archive", ignoreCase = true) &&
                    !alb.startsWith("مجموعه", ignoreCase = true) &&
                    !alb.contains("collection", ignoreCase = true) &&
                    !alb.contains("category of", ignoreCase = true)
            }

            val groupedByAlbum = validAlbumTracks.groupBy { trk ->
                val primArtist = SmartArtistParser.extractPrimaryArtist(trk.artist, trk.title).lowercase()
                "${primArtist}___${trk.album.trim().lowercase()}"
            }

            for ((_, albTracks) in groupedByAlbum) {
                if (albTracks.isEmpty()) continue
                val sampleTrack = albTracks.first()
                val albTitle = sampleTrack.album.trim()
                val primArtist = SmartArtistParser.extractPrimaryArtist(sampleTrack.artist, sampleTrack.title)
                val artistKey = primArtist.lowercase()
                val displayName = canonicalDisplayNames[artistKey] ?: primArtist

                val coverUri = albTracks.firstOrNull { !it.artworkUri.isNullOrBlank() }?.artworkUri

                val col = CloudAlbumCollection(
                    id = "alb_${artistKey}_${albTitle.lowercase().replace(" ", "_").replace("'", "")}",
                    title = albTitle,
                    artist = displayName,
                    coverUri = coverUri,
                    tracks = albTracks
                )
                allAlbumsList.add(col)

                val artistList = albumsByArtistKey.getOrPut(artistKey) { mutableListOf() }
                if (artistList.none { it.id == col.id }) {
                    artistList.add(col)
                }

                for (trk in albTracks) {
                    val participants = SmartArtistParser.extractArtistsFromTrack(trk.artist)
                    for (part in participants) {
                        val partKey = part.lowercase()
                        if (partKey != artistKey && artistAllTracks.containsKey(partKey)) {
                            val partList = albumsByArtistKey.getOrPut(partKey) { mutableListOf() }
                            if (partList.none { it.id == col.id }) {
                                partList.add(col)
                            }
                        }
                    }
                }
            }

            val sortedAlbums = allAlbumsList.sortedByDescending { it.tracks.size }

            val computedArtists = artistAllTracks.entries
                .map { (key, allTrackList) ->
                    val displayName = canonicalDisplayNames[key] ?: key
                    val workerItem = workerArtistsMap[key]
                    val resolvedCover = workerItem?.cover?.ifBlank { workerItem.picture }
                        ?.ifBlank { getArtistPicture(displayName).orEmpty() }
                        ?: getArtistPicture(displayName).orEmpty()

                    val soloList = artistSoloTracks[key].orEmpty()
                    val featList = artistFeatTracks[key].orEmpty()

                    CloudArtistProfile(
                        name = displayName,
                        tracks = allTrackList,
                        soloTracks = soloList.ifEmpty { allTrackList },
                        featuredTracks = featList,
                        albums = albumsByArtistKey[key].orEmpty(),
                        cover = resolvedCover,
                        picture = resolvedCover
                    )
                }
                .sortedWith(
                    compareByDescending<CloudArtistProfile> { it.soloTracks.size }
                        .thenByDescending { it.tracks.size }
                )

            val computedCategories = RealCategoryClassifier.buildRealCategories(tracks)
            val result = HubComputedData(
                albums = sortedAlbums,
                artists = computedArtists,
                browseCategories = computedCategories,
                artistRows = computedArtists.chunked(3),
                albumRows = sortedAlbums.chunked(2),
                categoryRows = computedCategories.chunked(2)
            )
            hubDataCache[sig] = result
            result
        }

    suspend fun preloadStartupEverything(
        context: Context,
        localTracks: List<Track>,
        isPersian: Boolean,
        onProgress: (Float, String) -> Unit
    ) {
        if (startupPreloadCompleted.get() && memoryCachedAppTracks.isNotEmpty()) {
            onProgress(1f, if (isPersian) "آماده ورود" else "Ready")
            return
        }

        val appCtx = context.applicationContext
        onProgress(
            0.08f,
            if (isPersian) "در حال بارگذاری آرشیو ابری..." else "Loading cloud catalog..."
        )

        var onlineAppTracks = loadCachedAppTracksAsync()
        if (onlineAppTracks.isEmpty()) {
            val fetched = try {
                fetchFromWorker()
            } catch (_: Throwable) {
                null
            }
            if (fetched != null && fetched.tracks.isNotEmpty()) {
                onlineAppTracks = withContext(Dispatchers.Default) {
                    fetched.tracks.map { it.toAppTrack() }
                }
                setMemoryCachedAppTracks(onlineAppTracks)
            }
        } else {
            backgroundWarmScope.launch {
                try {
                    val fresh = fetchFromWorker()
                    if (fresh.tracks.isNotEmpty()) {
                        val mapped = fresh.tracks.map { it.toAppTrack() }
                        setMemoryCachedAppTracks(mapped)
                        computeHubDataAsync(mapped, true)
                        computeHubDataAsync(mapped, false)
                    }
                } catch (_: Exception) {}
            }
        }

        onProgress(
            0.32f,
            if (isPersian) "در حال ساخت آلبوم‌ها، دسته‌بندی‌ها و پروفایل هنرمندان..." else "Indexing albums, categories & artists..."
        )

        val unifiedTracks = onlineAppTracks

        val hubPrimary = computeHubDataAsync(unifiedTracks, isPersian)
        computeHubDataAsync(unifiedTracks, !isPersian)

        onProgress(
            0.55f,
            if (isPersian) "در حال آماده‌سازی کاور آلبوم‌ها و عکس پروفایل هنرمندان..." else "Preloading album covers & artist profiles..."
        )

        val topArtistsToPreload = hubPrimary.artists.take(8)
        val topAlbumCoversToPreload = (
            hubPrimary.albums.take(10).mapNotNull { it.coverUri } +
                unifiedTracks.take(10).mapNotNull { it.artworkUri }
            ).distinct().take(14)

        val totalAssets = (topArtistsToPreload.size + topAlbumCoversToPreload.size).coerceAtLeast(1)
        val completedAssets = AtomicInteger(0)

        withTimeoutOrNull(850L) {
            coroutineScope {
                val preloaderSemaphore = Semaphore(8)
                val artistJobs = topArtistsToPreload.map { artist ->
                    async(Dispatchers.IO) {
                        try {
                            preloaderSemaphore.withPermit {
                                val url = artist.cover.ifBlank { artist.picture }
                                ArtistAvatarDiskCache.loadOrDownloadArtistBitmap(
                                    context = appCtx,
                                    artistName = artist.name,
                                    remoteUrl = url
                                )
                                val done = completedAssets.incrementAndGet()
                                val pct = 0.55f + (done.toFloat() / totalAssets.toFloat()) * 0.42f
                                onProgress(
                                    pct.coerceIn(0.55f, 0.97f),
                                    if (isPersian) "در حال کش کردن کاورها و آرتیست‌ها ($done/$totalAssets)..."
                                    else "Caching covers & artist avatars ($done/$totalAssets)..."
                                )
                            }
                        } catch (_: Throwable) {}
                    }
                }
                val coverJobs = topAlbumCoversToPreload.map { coverUrl ->
                    async(Dispatchers.IO) {
                        try {
                            preloaderSemaphore.withPermit {
                                loadCoverBitmapBlocking(coverUrl)
                                val done = completedAssets.incrementAndGet()
                                val pct = 0.55f + (done.toFloat() / totalAssets.toFloat()) * 0.42f
                                onProgress(
                                    pct.coerceIn(0.55f, 0.97f),
                                    if (isPersian) "در حال کش کردن کاورها و آرتیست‌ها ($done/$totalAssets)..."
                                    else "Caching covers & artist avatars ($done/$totalAssets)..."
                                )
                            }
                        } catch (_: Throwable) {}
                    }
                }
                (artistJobs + coverJobs).awaitAll()
            }
        }

        if (backgroundWarmStarted.compareAndSet(false, true)) {
            backgroundWarmScope.launch {
                val gentleSemaphore = Semaphore(2)
                for (album in hubPrimary.albums.take(40)) {
                    val uri = album.coverUri ?: continue
                    gentleSemaphore.withPermit {
                        try {
                            loadCoverBitmapBlocking(uri)
                        } catch (_: Throwable) {}
                    }
                }
                for (artist in hubPrimary.artists.drop(8).take(40)) {
                    gentleSemaphore.withPermit {
                        try {
                            ArtistAvatarDiskCache.loadOrDownloadArtistBitmap(
                                context = appCtx,
                                artistName = artist.name,
                                remoteUrl = artist.cover.ifBlank { artist.picture }
                            )
                        } catch (_: Throwable) {}
                    }
                }
            }
        }

        startupPreloadCompleted.set(true)
        onProgress(1f, if (isPersian) "آماده ورود به برنامه" else "Ready")
    }

    fun resolveWorkerUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") ->
                trimmed.replace(WORKER_HOST_REGEX, BASE_URL)
            trimmed.startsWith("/") -> "$BASE_URL$trimmed"
            else -> "$BASE_URL/$trimmed"
        }
    }

    fun normalizeWorkerImageUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        val resolved = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") ->
                trimmed.replace(WORKER_HOST_REGEX, BASE_URL)
            trimmed.startsWith("file://") || trimmed.startsWith("content://") -> return trimmed
            trimmed.startsWith("/storage/") || trimmed.startsWith("/data/") || trimmed.startsWith("/sdcard/") -> return trimmed
            trimmed.startsWith("/img?u=") -> "$BASE_URL$trimmed"
            trimmed.startsWith("/") -> "$BASE_URL$trimmed"
            else -> "$BASE_URL/$trimmed"
        }
        val upgraded = resolved
            .replace("size=small", "size=large")
            .replace("size=thumb", "size=large")
            .replace("size=medium", "size=large")
        return if (upgraded.contains("/cover?id=") && !upgraded.contains("size=")) {
            "$upgraded&size=large"
        } else {
            upgraded
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
            if (!resolvedCover.contains("dzcdn.net") && !resolvedCover.contains("deezer.com")) {
                coverUrlByTrackId[rawId] = resolvedCover
                coverUrlByTrackId["online_$rawId"] = resolvedCover
            }
        }
    }

    fun registerCoverUrlForTrack(trackId: String, coverUrl: String) {
        if (coverUrl.isBlank()) return
        val rawId = trackId.removePrefix("online_").trim()
        if (rawId.isBlank()) return
        val normalized = normalizeWorkerImageUrl(coverUrl)
        if (!normalized.contains("dzcdn.net") && !normalized.contains("deezer.com")) {
            coverUrlByTrackId[rawId] = normalized
            coverUrlByTrackId["online_$rawId"] = normalized
        }
    }

    fun getCoverUrlForTrack(trackId: String, artistName: String = ""): String {
        val rawId = trackId.removePrefix("online_").trim()
        if (rawId.isNotBlank()) {
            coverUrlByTrackId[rawId]?.takeIf {
                it.isNotBlank() && !it.contains("dzcdn.net") && !it.contains("deezer.com")
            }?.let { return normalizeWorkerImageUrl(it) }
            coverUrlByTrackId["online_$rawId"]?.takeIf {
                it.isNotBlank() && !it.contains("dzcdn.net") && !it.contains("deezer.com")
            }?.let { return normalizeWorkerImageUrl(it) }
            if (!rawId.startsWith("local_") && !rawId.startsWith("demo_")) {
                val encodedId = try { URLEncoder.encode(rawId, "UTF-8") } catch (_: Exception) { rawId }
                return "$BASE_URL/cover?id=$encodedId&size=large"
            }
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

    fun registerArtistPicture(artistName: String, pictureUrl: String, overwrite: Boolean = true) {
        if (artistName.isBlank() || pictureUrl.isBlank()) return
        val key = artistName.trim().lowercase()
        val normalized = ArtistAvatarDiskCache.resolveCloudflareArtistUrl(artistName, pictureUrl)
        if (normalized.isBlank()) return
        if (overwrite || !artistPictureCache.containsKey(key)) {
            artistPictureCache[key] = normalized
        }
        try {
            val appCtx = AuraApplication.instance.applicationContext
            ArtistAvatarDiskCache.saveArtistUrl(appCtx, artistName, normalized)
        } catch (_: Exception) {}
    }

    fun getArtistPicture(artistName: String): String? {
        val clean = artistName.trim().lowercase()
        artistPictureCache[clean]?.let { return it }
        return try {
            val appCtx = AuraApplication.instance.applicationContext
            ArtistAvatarDiskCache.getSavedArtistUrl(appCtx, artistName).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    fun getKnownArtists(): List<ArchiveArtistItem> = lastKnownArtists

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

    private fun getPrivateTrackCoverCacheDir(): File? {
        return try {
            val appCtx = AuraApplication.instance.applicationContext
            if (cleanedLegacyTrackCovers.compareAndSet(false, true)) {
                try {
                    for (oldName in LEGACY_TRACK_COVERS_DIRS) {
                        val oldDir = File(appCtx.filesDir, oldName)
                        if (oldDir.exists()) {
                            oldDir.deleteRecursively()
                        }
                    }
                } catch (_: Throwable) {}
            }
            val dir = File(appCtx.filesDir, PRIVATE_TRACK_COVERS_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val noMedia = File(dir, ".nomedia")
            if (!noMedia.exists()) {
                try { noMedia.createNewFile() } catch (_: Exception) {}
            }
            dir
        } catch (_: Exception) {
            null
        }
    }

    private fun getPrivateTrackCoverFile(resolvedUrl: String): File? {
        val dir = getPrivateTrackCoverCacheDir() ?: return null
        val idPart = if (resolvedUrl.contains("id=")) {
            resolvedUrl.substringAfter("id=").substringBefore("&")
        } else {
            resolvedUrl.hashCode().toUInt().toString(16)
        }
        val safeKey = idPart.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(64).ifBlank {
            resolvedUrl.hashCode().toUInt().toString(16)
        }
        return File(dir, "tg_cover_hd_$safeKey.jpg")
    }

    private fun decodeDownsampledCoverBytes(bytes: ByteArray, maxDim: Int = 1000): Bitmap? {
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
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeDownsampledCoverFile(file: File, maxDim: Int = 1000): Bitmap? {
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
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (_: Throwable) {
            null
        }
    }

    fun loadCachedWorkerCatalog(): WorkerTracksResponse? {
        memoryCachedResponse?.let { return it }
        return try {
            val appCtx = AuraApplication.instance.applicationContext
            try {
                val legacyFile = File(appCtx.filesDir, LEGACY_CATALOG_CACHE_FILE)
                if (legacyFile.exists()) legacyFile.delete()
                val legacyV3 = File(appCtx.filesDir, LEGACY_CATALOG_CACHE_FILE_V3)
                if (legacyV3.exists()) legacyV3.delete()
            } catch (_: Throwable) {}
            val file = File(appCtx.filesDir, PRIVATE_CATALOG_CACHE_FILE)
            if (file.exists() && file.length() > 64L) {
                val parsed = file.inputStream().use { parseWorkerTracksStream(it) }
                if (parsed.tracks.isNotEmpty()) {
                    memoryCachedResponse = parsed
                    val mapped = parsed.tracks.map { it.toAppTrack() }
                    setMemoryCachedAppTracks(mapped)
                    parsed
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun saveCompactCatalogToPrivateDisk(response: WorkerTracksResponse) {
        if (response.tracks.isEmpty()) return
        try {
            val appCtx = AuraApplication.instance.applicationContext
            val tempFile = File(appCtx.filesDir, "$PRIVATE_CATALOG_CACHE_FILE.tmp")
            val finalFile = File(appCtx.filesDir, PRIVATE_CATALOG_CACHE_FILE)
            FileOutputStream(tempFile).use { fos ->
                JsonWriter(OutputStreamWriter(fos, Charsets.UTF_8)).use { writer ->
                    writer.beginObject()
                    writer.name("ok").value(true)
                    writer.name("artists")
                    writer.beginArray()
                    for (artist in response.artists) {
                        writer.beginObject()
                        writer.name("name").value(artist.name)
                        writer.name("trackCount").value(artist.trackCount)
                        writer.name("cover").value(artist.cover)
                        writer.name("picture").value(artist.picture)
                        writer.endObject()
                    }
                    writer.endArray()

                    writer.name("tracks")
                    writer.beginArray()
                    for (track in response.tracks) {
                        writer.beginObject()
                        writer.name("id").value(track.id)
                        writer.name("title").value(track.title)
                        writer.name("artist_name").value(track.artist)
                        writer.name("album").value(track.album)
                        writer.name("duration").value(track.durationSec)
                        writer.name("stream_url").value(track.streamUrl)
                        writer.name("download_url").value(track.downloadUrl)
                        writer.name("cover_url").value(track.coverUrl)
                        writer.name("genre").value(track.rawGenre)
                        writer.endObject()
                    }
                    writer.endArray()
                    writer.endObject()
                }
            }
            if (tempFile.exists() && tempFile.length() > 64L) {
                if (finalFile.exists()) finalFile.delete()
                tempFile.renameTo(finalFile)
            }
        } catch (_: Throwable) {}
    }

    suspend fun fetchFromWorker(
        query: String = "",
        artist: String = "",
        forceRefresh: Boolean = false
    ): WorkerTracksResponse = withContext(Dispatchers.IO) {
        val isFullCatalogRequest = query.isBlank() && artist.isBlank()
        if (isFullCatalogRequest && !forceRefresh) {
            val existing = memoryCachedResponse
            if (existing != null && existing.tracks.isNotEmpty() && (System.currentTimeMillis() - lastFullFetchTimeMs) < 180_000L) {
                return@withContext existing
            }
        }

        if (isFullCatalogRequest) {
            catalogFetchMutex.lock()
            try {
                if (!forceRefresh) {
                    val existingAfterLock = memoryCachedResponse
                    if (existingAfterLock != null && existingAfterLock.tracks.isNotEmpty() && (System.currentTimeMillis() - lastFullFetchTimeMs) < 180_000L) {
                        return@withContext existingAfterLock
                    }
                }
                _isFetchingFlow.value = true
                return@withContext performWorkerFetchInternal(query = "", artist = "", isFullCatalogRequest = true)
            } finally {
                _isFetchingFlow.value = false
                catalogFetchMutex.unlock()
            }
        } else {
            return@withContext performWorkerFetchInternal(query = query, artist = artist, isFullCatalogRequest = false)
        }
    }

    private suspend fun performWorkerFetchInternal(
        query: String,
        artist: String,
        isFullCatalogRequest: Boolean
    ): WorkerTracksResponse {
        var connection: HttpURLConnection? = null
        try {
            val deviceId = try {
                EntitlementManager.getInstance(AuraApplication.instance).getDeviceId()
            } catch (_: Throwable) {
                "unknown"
            }
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val encodedArtist = URLEncoder.encode(artist.trim(), "UTF-8")
            val promoParam = if (EntitlementManager.isPremium.value) "&promoCode=57589188&vip=true" else ""
            val endpoint = "$BASE_URL/tracks?q=$encodedQuery&artist=$encodedArtist&deviceId=$deviceId$promoParam"
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 25000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Device-Id", deviceId)
                if (EntitlementManager.isPremium.value) {
                    setRequestProperty("X-Promo-Code", "57589188")
                }
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (connection.responseCode in 200..299) {
                val rawStream = connection.inputStream
                val decodedStream = if (connection.contentEncoding?.contains("gzip", ignoreCase = true) == true) {
                    GZIPInputStream(rawStream)
                } else {
                    rawStream
                }
                var parsed = try {
                    decodedStream.use { parseWorkerTracksStream(it) }
                } catch (_: OutOfMemoryError) {
                    System.gc()
                    WorkerTracksResponse(artists = lastKnownArtists)
                } catch (e: Exception) {
                    e.printStackTrace()
                    WorkerTracksResponse(artists = lastKnownArtists)
                }

                if (parsed.tracks.isEmpty()) {
                    val fallbackTracks = CloudflareMusicApi.searchOrFetchTracks(query, deviceId).getOrNull().orEmpty()
                    if (fallbackTracks.isNotEmpty()) {
                        parsed = parsed.copy(tracks = fallbackTracks.map { it.toOnlineTrack() })
                    }
                }
                if (isFullCatalogRequest && parsed.tracks.isNotEmpty()) {
                    memoryCachedResponse = parsed
                    lastFullFetchTimeMs = System.currentTimeMillis()
                    val mapped = parsed.tracks.map { it.toAppTrack() }
                    setMemoryCachedAppTracks(mapped)
                    backgroundWarmScope.launch {
                        saveCompactCatalogToPrivateDisk(parsed)
                    }
                }
                return parsed
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            try { connection?.disconnect() } catch (_: Throwable) {}
        }
        val deviceIdFallback = try {
            EntitlementManager.getInstance(AuraApplication.instance).getDeviceId()
        } catch (_: Throwable) {
            ""
        }
        val fallbackTracks = CloudflareMusicApi.searchOrFetchTracks(query, deviceIdFallback).getOrNull().orEmpty()
        if (fallbackTracks.isNotEmpty()) {
            val onlineList = fallbackTracks.map { it.toOnlineTrack() }
            val resp = WorkerTracksResponse(artists = lastKnownArtists, tracks = onlineList)
            if (isFullCatalogRequest) {
                memoryCachedResponse = resp
                lastFullFetchTimeMs = System.currentTimeMillis()
                val mapped = onlineList.map { it.toAppTrack() }
                setMemoryCachedAppTracks(mapped)
                backgroundWarmScope.launch {
                    saveCompactCatalogToPrivateDisk(resp)
                }
            }
            return resp
        }
        return loadCachedWorkerCatalog() ?: WorkerTracksResponse(artists = lastKnownArtists)
    }

    private fun readScalarString(reader: JsonReader): String {
        return when (reader.peek()) {
            JsonToken.STRING -> reader.nextString()
            JsonToken.NUMBER -> reader.nextString()
            JsonToken.BOOLEAN -> reader.nextBoolean().toString()
            JsonToken.NULL -> {
                reader.nextNull()
                ""
            }
            else -> {
                reader.skipValue()
                ""
            }
        }
    }

    private fun parseWorkerTracksStream(inputStream: InputStream): WorkerTracksResponse {
        val mergedArtistsMap = LinkedHashMap<String, ArchiveArtistItem>()
        val matchedList = mutableListOf<ArchiveArtistItem>()
        val tracksList = mutableListOf<OnlineTrack>()

        return try {
            val reader = JsonReader(BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 32768)).apply {
                isLenient = true
            }
            when (reader.peek()) {
                JsonToken.BEGIN_ARRAY -> {
                    parseWorkerTracksArrayStream(reader, tracksList)
                }
                JsonToken.BEGIN_OBJECT -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val key = reader.nextName()
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull()
                            continue
                        }
                        when (key) {
                            "artists" -> {
                                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                                    parseWorkerArtistsArrayStream(reader, mergedArtistsMap)
                                } else {
                                    reader.skipValue()
                                }
                            }
                            "matchedArtists" -> {
                                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                                    parseWorkerMatchedArtistsArrayStream(reader, mergedArtistsMap, matchedList)
                                } else {
                                    reader.skipValue()
                                }
                            }
                            "tracks", "data", "results", "songs", "items" -> {
                                if (tracksList.isEmpty() && reader.peek() == JsonToken.BEGIN_ARRAY) {
                                    parseWorkerTracksArrayStream(reader, tracksList)
                                } else {
                                    reader.skipValue()
                                }
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                else -> reader.skipValue()
            }

            val artistsList = mergedArtistsMap.values.toList()
            if (artistsList.isNotEmpty()) {
                lastKnownArtists = artistsList
            }

            WorkerTracksResponse(
                artists = if (artistsList.isNotEmpty()) artistsList else lastKnownArtists,
                matchedArtists = matchedList.distinctBy { it.name.lowercase() },
                tracks = tracksList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            val artistsList = mergedArtistsMap.values.toList()
            if (artistsList.isNotEmpty()) {
                lastKnownArtists = artistsList
            }
            WorkerTracksResponse(
                artists = if (artistsList.isNotEmpty()) artistsList else lastKnownArtists,
                matchedArtists = matchedList.distinctBy { it.name.lowercase() },
                tracks = tracksList
            )
        }
    }

    private fun parseWorkerArtistsArrayStream(
        reader: JsonReader,
        mergedArtistsMap: LinkedHashMap<String, ArchiveArtistItem>
    ) {
        reader.beginArray()
        while (reader.hasNext()) {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                reader.skipValue()
                continue
            }
            reader.beginObject()
            var rawName = ""
            var trackCount = 0
            var rawCover = ""
            while (reader.hasNext()) {
                val field = reader.nextName()
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    continue
                }
                when (field) {
                    "name" -> rawName = readScalarString(reader).trim()
                    "trackCount", "count" -> {
                        val c = readScalarString(reader).toIntOrNull() ?: 0
                        if (trackCount == 0 && c > 0) trackCount = c
                    }
                    "cover", "picture", "cover_url", "picture_url" -> {
                        val v = readScalarString(reader).trim()
                        if (rawCover.isBlank() && v.isNotBlank()) rawCover = v
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            if (rawName.isBlank()) continue
            val isCollab = SmartArtistParser.isCollaborationString(rawName)
            val individualNames = SmartArtistParser.extractArtistsFromTrack(rawName)

            if (!isCollab && individualNames.size == 1) {
                val cleanName = individualNames.first()
                val key = cleanName.lowercase()
                val cover = ArtistAvatarDiskCache.resolveCloudflareArtistUrl(cleanName, rawCover)
                if (cover.isNotBlank()) {
                    registerArtistPicture(cleanName, cover, overwrite = true)
                }
                val existing = mergedArtistsMap[key]
                mergedArtistsMap[key] = ArchiveArtistItem(
                    name = existing?.name ?: cleanName,
                    trackCount = (existing?.trackCount ?: 0) + trackCount,
                    cover = cover.ifBlank { existing?.cover.orEmpty() },
                    picture = cover.ifBlank { existing?.picture.orEmpty() }
                )
            } else {
                individualNames.forEach { singleName ->
                    val key = singleName.lowercase()
                    val existing = mergedArtistsMap[key]
                    val bestCover = existing?.cover?.takeIf { it.isNotBlank() }
                        ?: ArtistAvatarDiskCache.resolveCloudflareArtistUrl(singleName, "")
                    mergedArtistsMap[key] = ArchiveArtistItem(
                        name = existing?.name ?: singleName,
                        trackCount = (existing?.trackCount ?: 0) + trackCount,
                        cover = bestCover,
                        picture = bestCover
                    )
                }
            }
        }
        reader.endArray()
    }

    private fun parseWorkerMatchedArtistsArrayStream(
        reader: JsonReader,
        mergedArtistsMap: LinkedHashMap<String, ArchiveArtistItem>,
        matchedList: MutableList<ArchiveArtistItem>
    ) {
        reader.beginArray()
        while (reader.hasNext()) {
            when (reader.peek()) {
                JsonToken.BEGIN_OBJECT -> {
                    reader.beginObject()
                    var rawName = ""
                    var count = 0
                    var rawCover = ""
                    while (reader.hasNext()) {
                        val field = reader.nextName()
                        if (reader.peek() == JsonToken.NULL) {
                            reader.nextNull()
                            continue
                        }
                        when (field) {
                            "name" -> rawName = readScalarString(reader).trim()
                            "trackCount", "count" -> {
                                val c = readScalarString(reader).toIntOrNull() ?: 0
                                if (count == 0 && c > 0) count = c
                            }
                            "cover", "picture", "cover_url", "picture_url" -> {
                                val v = readScalarString(reader).trim()
                                if (rawCover.isBlank() && v.isNotBlank()) rawCover = v
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (rawName.isNotBlank()) {
                        val primaryName = SmartArtistParser.extractPrimaryArtist(rawName)
                        val cover = ArtistAvatarDiskCache.resolveCloudflareArtistUrl(primaryName, rawCover)
                        if (cover.isNotBlank()) {
                            registerArtistPicture(primaryName, cover, overwrite = !SmartArtistParser.isCollaborationString(rawName))
                        }
                        matchedList.add(
                            ArchiveArtistItem(
                                name = primaryName,
                                trackCount = count,
                                cover = cover,
                                picture = cover
                            )
                        )
                    }
                }
                JsonToken.STRING -> {
                    val item = reader.nextString()
                    val primaryName = SmartArtistParser.extractPrimaryArtist(item)
                    if (primaryName.isNotBlank()) {
                        val fromAll = mergedArtistsMap[primaryName.lowercase()]
                        val pic = ArtistAvatarDiskCache.resolveCloudflareArtistUrl(primaryName, "")
                        matchedList.add(
                            fromAll ?: ArchiveArtistItem(
                                name = primaryName,
                                trackCount = 0,
                                cover = pic,
                                picture = pic
                            )
                        )
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endArray()
    }

    private fun parseWorkerTracksArrayStream(
        reader: JsonReader,
        tracksList: MutableList<OnlineTrack>
    ) {
        reader.beginArray()
        var index = 0
        while (reader.hasNext()) {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                reader.skipValue()
                continue
            }
            reader.beginObject()
            var id = ""
            var fileId = ""
            var messageId = ""
            var title = ""
            var name = ""
            var artistName = ""
            var artistFallback = ""
            var rawAlbum = ""
            var durationRaw: String? = null
            var rawStream = ""
            var rawDownload = ""
            var rawCover = ""
            var rawGenre = ""

            while (reader.hasNext()) {
                val field = reader.nextName()
                if (reader.peek() == JsonToken.NULL) {
                    reader.nextNull()
                    continue
                }
                when (field) {
                    "id" -> id = readScalarString(reader).trim()
                    "file_id" -> fileId = readScalarString(reader).trim()
                    "message_id" -> messageId = readScalarString(reader).trim()
                    "title" -> title = readScalarString(reader).trim()
                    "name" -> {
                        val v = readScalarString(reader).trim()
                        if (name.isBlank()) name = v
                    }
                    "artist_name", "artistName", "primary_artist", "primaryArtist" -> {
                        val v = readScalarString(reader).trim()
                        if (artistName.isBlank() && v.isNotBlank()) artistName = v
                    }
                    "artist" -> {
                        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val ak = reader.nextName()
                                if (ak == "name" && reader.peek() != JsonToken.NULL) {
                                    val av = readScalarString(reader).trim()
                                    if (artistFallback.isBlank()) artistFallback = av
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endObject()
                        } else {
                            val v = readScalarString(reader).trim()
                            if (artistFallback.isBlank() && !v.startsWith("{")) artistFallback = v
                        }
                    }
                    "album", "album_name", "albumName", "album_title" -> {
                        if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                val alk = reader.nextName()
                                if ((alk == "title" || alk == "name") && reader.peek() != JsonToken.NULL) {
                                    val alv = readScalarString(reader).trim()
                                    if (rawAlbum.isBlank()) rawAlbum = alv
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endObject()
                        } else {
                            val v = readScalarString(reader).trim()
                            if (rawAlbum.isBlank() && !v.startsWith("{")) rawAlbum = v
                        }
                    }
                    "duration" -> durationRaw = readScalarString(reader)
                    "stream_url", "streamUrl", "url", "audio_url", "preview" -> {
                        val v = readScalarString(reader).trim()
                        if (rawStream.isBlank() && v.isNotBlank()) rawStream = v
                    }
                    "download_url", "downloadUrl" -> {
                        val v = readScalarString(reader).trim()
                        if (rawDownload.isBlank() && v.isNotBlank()) rawDownload = v
                    }
                    "cover_xl", "cover_big", "cover_url", "coverUrl", "cover", "artwork", "image" -> {
                        val v = readScalarString(reader).trim()
                        if (v.isNotBlank() && (rawCover.isBlank() || field == "cover_xl" || field == "cover_big")) {
                            rawCover = v
                        }
                    }
                    "genre", "category", "style", "mood" -> {
                        val v = readScalarString(reader).trim()
                        if (rawGenre.isBlank() && v.isNotBlank()) rawGenre = v
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            val finalId = id.ifBlank { fileId.ifBlank { messageId.ifBlank { "tg_$index" } } }
            val resolvedTitle = title.ifBlank { name.ifBlank { "Unknown Track" } }
            val resolvedArtist = artistName.ifBlank { artistFallback.ifBlank { "Unknown Artist" } }
            val durationSec = parseDurationToSeconds(durationRaw)
            val effectiveDownloadRaw = rawDownload.ifBlank { rawStream }

            val streamUrl = resolveWorkerUrl(rawStream)
            val downloadUrl = resolveWorkerUrl(effectiveDownloadRaw)
            val encodedId = try { URLEncoder.encode(finalId, "UTF-8") } catch (_: Exception) { finalId }
            val coverUrl = normalizeWorkerImageUrl(rawCover).let { candidate ->
                if (candidate.isBlank() || candidate.contains("dzcdn.net") || candidate.contains("deezer.com") || candidate.contains("/artist-cover")) {
                    "$BASE_URL/cover?id=$encodedId"
                } else {
                    candidate
                }
            }
            val primaryArtist = SmartArtistParser.extractPrimaryArtist(resolvedArtist, resolvedTitle)

            registerTrackUrls(
                trackId = finalId,
                streamUrl = streamUrl,
                downloadUrl = downloadUrl,
                coverUrl = coverUrl,
                artistName = resolvedArtist
            )

            if (rawGenre.isNotBlank()) {
                RealTrackGenreRegistry.registerGenre(finalId, rawGenre)
                RealTrackGenreRegistry.registerGenre("online_$finalId", rawGenre)
            }

            val cleaned = com.example.data.model.SmartTrackMetadataResolver.resolve(
                rawTitle = resolvedTitle,
                rawArtist = resolvedArtist,
                rawAlbum = rawAlbum
            )
            val userOv = com.example.data.model.UserTrackMetadataStore.getOverride(finalId)
            val finalTitle = userOv?.title ?: cleaned.title
            val finalArtist = userOv?.artist ?: cleaned.artist
            val finalAlbum = userOv?.album ?: cleaned.album

            tracksList.add(
                OnlineTrack(
                    id = finalId,
                    title = finalTitle,
                    artist = finalArtist,
                    album = finalAlbum,
                    coverUrl = coverUrl,
                    durationSec = durationSec,
                    streamUrl = streamUrl,
                    downloadUrl = downloadUrl,
                    previewUrl = streamUrl,
                    artistPictureUrl = ArtistAvatarDiskCache.resolveCloudflareArtistUrl(primaryArtist, ""),
                    rawGenre = rawGenre
                )
            )
            index++
        }
        reader.endArray()
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
        if (url.isBlank()) return@withContext null
        val resolvedUrl = normalizeWorkerImageUrl(url)
        imageMemoryCache.get(resolvedUrl)?.let { return@withContext it }
        imageMemoryCache.get(url)?.let { return@withContext it }
        if (failedCoverUrls.contains(resolvedUrl)) return@withContext null

        coverIoSemaphore.withPermit {
            loadCoverBitmapBlocking(url)
        }
    }

    fun loadCoverBitmapBlocking(url: String): Bitmap? {
        if (url.isBlank()) return null
        val resolvedUrl = normalizeWorkerImageUrl(url)
        imageMemoryCache.get(resolvedUrl)?.let { return it }
        imageMemoryCache.get(url)?.let { return it }

        if (failedCoverUrls.contains(resolvedUrl)) {
            return null
        }

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
                            decodeDownsampledCoverBytes(bytes)
                        } else null
                    } else {
                        decodeDownsampledCoverFile(file)
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

        val diskCoverFile = getPrivateTrackCoverFile(resolvedUrl)
        if (diskCoverFile != null && diskCoverFile.exists() && diskCoverFile.length() > 512L) {
            val diskBmp = decodeDownsampledCoverFile(diskCoverFile)
            if (diskBmp != null) {
                imageMemoryCache.put(resolvedUrl, diskBmp)
                imageMemoryCache.put(url, diskBmp)
                return diskBmp
            }
        }

        val lock = coverDownloadLocks.getOrPut(resolvedUrl) { Any() }
        synchronized(lock) {
            imageMemoryCache.get(resolvedUrl)?.let { return it }
            if (diskCoverFile != null && diskCoverFile.exists() && diskCoverFile.length() > 512L) {
                decodeDownsampledCoverFile(diskCoverFile)?.let { bmp ->
                    imageMemoryCache.put(resolvedUrl, bmp)
                    imageMemoryCache.put(url, bmp)
                    return bmp
                }
            }

            var connection: HttpURLConnection? = null
            try {
                connection = (URL(resolvedUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 8000
                    readTimeout = 8000
                    doInput = true
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                connection.connect()
                val code = connection.responseCode
                if (code in 200..299) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    if (bytes.size > 512) {
                        val bitmap = decodeDownsampledCoverBytes(bytes)
                        if (bitmap != null) {
                            imageMemoryCache.put(resolvedUrl, bitmap)
                            imageMemoryCache.put(url, bitmap)
                            if (diskCoverFile != null) {
                                try {
                                    val tmp = File(diskCoverFile.parentFile, "${diskCoverFile.name}.tmp")
                                    FileOutputStream(tmp).use { out ->
                                        out.write(bytes)
                                    }
                                    if (tmp.exists() && tmp.length() > 512L) {
                                        if (diskCoverFile.exists()) diskCoverFile.delete()
                                        tmp.renameTo(diskCoverFile)
                                    } else {
                                        tmp.delete()
                                    }
                                } catch (_: Exception) {}
                            }
                            return bitmap
                        }
                    }
                } else if (code in 400..499) {
                    failedCoverUrls.add(resolvedUrl)
                }
            } catch (_: Throwable) {
            } finally {
                try { connection?.disconnect() } catch (_: Exception) {}
            }
        }

        return null
    }
}
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
            val primaryArtist = SmartArtistParser.extractPrimaryArtist(track.artist, track.title)
            if (primaryArtist.isNotBlank() && !primaryArtist.equals("Unknown Artist", ignoreCase = true)) {
                val scoresJson = JSONObject(prefs.getString(KEY_ARTIST_SCORES, "{}") ?: "{}")
                scoresJson.put(primaryArtist, scoresJson.optInt(primaryArtist, 0) + 3)
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
                val rawUri = obj.optString("uri")
                val uri = cachedFile?.absolutePath ?: if (rawUri.startsWith("http")) OnlineMusicRepository.resolveWorkerUrl(rawUri) else rawUri
                if (id.isNotBlank() && uri.isNotBlank()) {
                    val tTitle = obj.optString("title", "Unknown")
                    val tArtist = obj.optString("artist", "Unknown")
                    val cleaned = com.example.data.model.SmartTrackMetadataResolver.resolve(
                        rawTitle = tTitle,
                        rawArtist = tArtist,
                        rawAlbum = obj.optString("album", "Single")
                    )
                    val userOv = com.example.data.model.UserTrackMetadataStore.getOverride(id)
                    val finalTitle = userOv?.title ?: cleaned.title
                    val finalArtist = userOv?.artist ?: cleaned.artist
                    val finalAlbum = userOv?.album ?: cleaned.album
                    val rawArtwork = obj.optString("artworkUri").takeIf { it.isNotBlank() }
                    val normalizedArtwork = rawArtwork?.let { OnlineMusicRepository.normalizeWorkerImageUrl(it) }
                    list.add(
                        Track(
                            id = id,
                            title = finalTitle,
                            artist = finalArtist,
                            album = finalAlbum,
                            durationMs = obj.optLong("durationMs", 180_000L),
                            uri = uri,
                            artworkUri = normalizedArtwork,
                            genre = obj.optString("genre", "Online")
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun getTopArtistsForUser(
        context: Context,
        localRecent: List<Track>,
        localFavorites: List<Track>
    ): List<String> {
        val list = mutableListOf<String>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val scoresJson = JSONObject(prefs.getString(KEY_ARTIST_SCORES, "{}") ?: "{}")
            val scored = scoresJson.keys().asSequence().map { it to scoresJson.optInt(it, 0) }
                .sortedByDescending { it.second }
                .map { it.first }
                .filter { it.isNotBlank() }
                .toList()
            list.addAll(scored)
        } catch (_: Exception) {}

        if (list.size < 5) {
            val fromLocal = (localFavorites + localRecent)
                .flatMap { SmartArtistParser.extractArtistsFromTrack(it.artist, it.title) }
                .filter { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }
                .distinct()
            list.addAll(fromLocal.filterNot { list.contains(it) })
        }
        return list.take(15)
    }

    suspend fun fetchTrendingAndTasteFeed(
        context: Context,
        mode: String,
        topArtists: List<String>
    ): List<Track> = withContext(Dispatchers.IO) {
        val queryArtist = topArtists.firstOrNull { it.isNotBlank() } ?: ""
        val response = OnlineMusicRepository.fetchFromWorker(query = "", artist = queryArtist)
        val list = if (response.tracks.isNotEmpty()) response.tracks else OnlineMusicRepository.fetchFromWorker().tracks
        list.map { it.toAppTrack() }
    }

    suspend fun fetchOnlineTimeSlotTracks(
        context: Context,
        hour: Int,
        topArtists: List<String>
    ): List<Track> = withContext(Dispatchers.IO) {
        val queryArtist = topArtists.firstOrNull { it.isNotBlank() } ?: ""
        val response = OnlineMusicRepository.fetchFromWorker(query = "", artist = queryArtist)
        val list = if (response.tracks.isNotEmpty()) response.tracks else OnlineMusicRepository.fetchFromWorker().tracks
        list.map { it.toAppTrack() }
    }
}

@Composable
fun OnlineMusicScreen(
    onlineTracks: List<Track>,
    isLoading: Boolean = false,
    currentPlayingTrack: Track? = null,
    isPersian: Boolean = false,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onRefresh: () -> Unit = {},
    bottomPadding: Dp = 130.dp,
    onDownloadTrack: ((Track) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPremium by EntitlementManager.isPremium.collectAsState()
    val remoteConfig by EntitlementManager.remoteConfig.collectAsState()
    val remainingRequests by EntitlementManager.remainingRequestsToday.collectAsState()

    var paywallLoading by remember { mutableStateOf(false) }
    var paywallMessage by remember { mutableStateOf<String?>(null) }
    var paywallError by remember { mutableStateOf(false) }
    var showSongRequestSheet by remember { mutableStateOf(false) }
    var showVipPaywallSheet by remember { mutableStateOf(false) }
    var showMyketThankYouDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        EntitlementManager.init(context)
        EntitlementManager.fetchRemoteConfig(context)
    }

    LaunchedEffect(isPremium) {
        if (isPremium) {
            showVipPaywallSheet = false
            onRefresh()
        }
    }

    if (showMyketThankYouDialog) {
        com.example.ui.components.MyketThankYouDialog(
            onConfirm = {
                showMyketThankYouDialog = false
                onRefresh()
            }
        )
    }

    val myketLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        scope.launch {
            paywallLoading = true
            try {
                when (val r = EntitlementManager.handleMyketActivityResult(context, res.resultCode, res.data)) {
                    is PurchaseResult.Success -> {
                        paywallError = false
                        paywallMessage = "اشتراک پریمیوم مادام‌العمر فعال شد!"
                        showVipPaywallSheet = false
                        showMyketThankYouDialog = true
                    }
                    is PurchaseResult.Error -> {
                        paywallError = true
                        paywallMessage = r.message
                        android.widget.Toast.makeText(context, r.message, android.widget.Toast.LENGTH_LONG).show()
                    }
                    is PurchaseResult.Cancelled -> {
                        paywallError = false
                        paywallMessage = "پرداخت لغو شد."
                    }
                }
            } finally {
                paywallLoading = false
            }
        }
    }

    val effectiveDownloadHandler: ((Track) -> Unit)? = onDownloadTrack?.let { handler ->
        { trackToDownload ->
            com.example.monetization.TapsellPlus.showRewardedAd(
                context = context,
                zoneId = ""
            ) {
                handler(trackToDownload)
            }
        }
    }

    val repoTracks by OnlineMusicRepository.onlineTracksFlow.collectAsState()
    val isRepoFetching by OnlineMusicRepository.isFetchingFlow.collectAsState()
    val effectiveOnlineTracks = if (onlineTracks.isNotEmpty()) onlineTracks else repoTracks

    var searchQuery by remember { mutableStateOf("") }
    var remoteSearchedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var activeTab by remember { mutableStateOf(TopHubTab.DISCOVER) }
    var navState by remember { mutableStateOf<HubNavigationState>(HubNavigationState.MainHub) }
    var quickPicksSeed by remember { mutableIntStateOf(0) }

    BackHandler(enabled = navState != HubNavigationState.MainHub || searchQuery.isNotBlank()) {
        if (navState != HubNavigationState.MainHub) {
            navState = HubNavigationState.MainHub
        } else {
            searchQuery = ""
        }
    }

    var hubData by remember(effectiveOnlineTracks, isPersian) {
        mutableStateOf(
            OnlineMusicRepository.getMemoryCachedHubData(effectiveOnlineTracks, isPersian)
                ?: HubComputedData()
        )
    }

    LaunchedEffect(effectiveOnlineTracks, isPersian) {
        if (effectiveOnlineTracks.isNotEmpty() && hubData.artists.isEmpty()) {
            hubData = OnlineMusicRepository.computeHubDataAsync(effectiveOnlineTracks, isPersian)
        }
    }

    LaunchedEffect(searchQuery) {
        val cleanQ = searchQuery.trim()
        if (cleanQ.length < 2) {
            remoteSearchedTracks = emptyList()
            return@LaunchedEffect
        }
        delay(350L)
        try {
            val resp = OnlineMusicRepository.fetchFromWorker(query = cleanQ)
            if (resp.tracks.isNotEmpty()) {
                remoteSearchedTracks = withContext(Dispatchers.Default) {
                    resp.tracks.map { it.toAppTrack() }
                }
            }
        } catch (_: Exception) {}
    }

    val albums = hubData.albums
    val artists = hubData.artists
    val browseCategories = hubData.browseCategories

    val quickPicks = remember(effectiveOnlineTracks, quickPicksSeed) {
        if (effectiveOnlineTracks.isEmpty()) emptyList()
        else if (quickPicksSeed == 0) effectiveOnlineTracks.take(15)
        else effectiveOnlineTracks.shuffled().take(15)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraCanvas)
    ) {
        when (val state = navState) {
            is HubNavigationState.ArtistPage -> {
                SpotifyFullArtistScreen(
                    artist = state.artist,
                    currentPlayingTrack = currentPlayingTrack,
                    isPersian = isPersian,
                    onBack = { navState = HubNavigationState.MainHub },
                    onAlbumClick = { navState = HubNavigationState.AlbumPage(it) },
                    onPlayTrack = onPlayTrack,
                    onDownloadTrack = effectiveDownloadHandler
                )
            }

            is HubNavigationState.AlbumPage -> {
                SpotifyFullAlbumScreen(
                    album = state.album,
                    currentPlayingTrack = currentPlayingTrack,
                    isPersian = isPersian,
                    onBack = { navState = HubNavigationState.MainHub },
                    onPlayTrack = onPlayTrack,
                    onDownloadTrack = effectiveDownloadHandler
                )
            }

            is HubNavigationState.CategoryPage -> {
                SpotifyCategoryPlaylistScreen(
                    category = state.category,
                    currentPlayingTrack = currentPlayingTrack,
                    isPersian = isPersian,
                    onBack = { navState = HubNavigationState.MainHub },
                    onPlayTrack = onPlayTrack,
                    onDownloadTrack = effectiveDownloadHandler
                )
            }

            HubNavigationState.MainHub -> {
                val q = searchQuery.trim()
                val matchedArtists = remember(artists, q) {
                    if (q.isBlank()) emptyList()
                    else artists.filter { it.name.contains(q, ignoreCase = true) }.take(15)
                }
                val matchedAlbums = remember(albums, q) {
                    if (q.isBlank()) emptyList()
                    else albums.filter {
                        it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
                    }.take(15)
                }
                val matchedTracks = remember(effectiveOnlineTracks, remoteSearchedTracks, q) {
                    if (q.isBlank()) emptyList()
                    else {
                        val localMatches = effectiveOnlineTracks.filter {
                            it.title.contains(q, ignoreCase = true) ||
                                it.artist.contains(q, ignoreCase = true) ||
                                it.album.contains(q, ignoreCase = true)
                        }
                        (localMatches + remoteSearchedTracks).distinctBy { it.id }.take(45)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = bottomPadding)
                ) {
                    item(key = "top_glass_bar") {
                        SpotifyGlassTopBar(
                            totalTracks = effectiveOnlineTracks.size,
                            totalArtists = artists.size,
                            searchQuery = searchQuery,
                            isPersian = isPersian,
                            isPremium = isPremium,
                            remainingRequests = remainingRequests,
                            onRequestSong = {
                                if (isPremium) {
                                    showSongRequestSheet = true
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "ارسال درخواست آهنگ (۳ بار در روز) فقط ویژه کاربران VIP است",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    showVipPaywallSheet = true
                                }
                            },
                            onOpenVipPaywall = { showVipPaywallSheet = true },
                            onSearchChange = { searchQuery = it },
                            onRefresh = onRefresh
                        )
                    }

                    if (q.isNotBlank()) {
                        categorizedInstantSearchItems(
                            matchedArtists = matchedArtists,
                            matchedAlbums = matchedAlbums,
                            matchedTracks = matchedTracks,
                            currentPlayingTrack = currentPlayingTrack,
                            isPersian = isPersian,
                            onArtistClick = { navState = HubNavigationState.ArtistPage(it) },
                            onAlbumClick = { navState = HubNavigationState.AlbumPage(it) },
                            onPlayTrack = onPlayTrack,
                            onDownloadTrack = effectiveDownloadHandler
                        )
                    } else {
                        // Spotify Filter Pills
                        item(key = "top_hub_tabs") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(TopHubTab.entries, key = { it.name }) { tab ->
                                    val isSelected = activeTab == tab
                                    Row(
                                        modifier = Modifier
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (isSelected) AuraSpotifyGreen else AuraSurfaceElevated)
                                            .clickable { activeTab = tab }
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isPersian) tab.titleFa else tab.titleEn,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        if ((isLoading || isRepoFetching) && effectiveOnlineTracks.isEmpty()) {
                            item(key = "hub_loading") {
                                AuraInlinePulseLoader(isPersian = isPersian)
                            }
                        } else if (effectiveOnlineTracks.isEmpty()) {
                            item(key = "hub_empty_retry") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 48.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isPersian) "در حال اتصال به آرشیو ابری..." else "Connecting to cloud archive...",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = onRefresh,
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AuraSpotifyGreen,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isPersian) "تلاش مجدد" else "Retry",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            when (activeTab) {
                                TopHubTab.DISCOVER -> {
                                    if (browseCategories.isNotEmpty()) {
                                        item(key = "discover_categories_header") {
                                            HubSectionHeader(
                                                title = if (isPersian) "مرور همه دسته‌بندی‌ها" else "Browse all"
                                            )
                                        }
                                        spotifyCategoriesLazyRows(
                                            categoryRows = hubData.categoryRows,
                                            isPersian = isPersian,
                                            onCategoryClick = { category ->
                                                navState = HubNavigationState.CategoryPage(category)
                                            }
                                        )
                                        item(key = "discover_cat_spacer") {
                                            Spacer(modifier = Modifier.height(20.dp))
                                        }
                                    }

                                    item(key = "discover_hero") {
                                        AppleMusicHeroPager(
                                            onlineTracks = effectiveOnlineTracks,
                                            browseCategories = browseCategories,
                                            isPersian = isPersian,
                                            onPlayMix = { mixTracks ->
                                                if (mixTracks.isNotEmpty()) {
                                                    onPlayTrack(mixTracks.first(), mixTracks)
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                    if (artists.isNotEmpty()) {
                                        item(key = "discover_featured_artists") {
                                            HubSectionHeader(
                                                title = if (isPersian) "هنرمندان محبوب" else "Popular artists",
                                                actionLabel = if (isPersian) "نمایش همه" else "Show all",
                                                onAction = { activeTab = TopHubTab.ARTISTS }
                                            )
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                items(artists.take(15), key = { "hub_art_${it.name}" }) { artist ->
                                                    SpotifyArtistCard(
                                                        artist = artist,
                                                        isPersian = isPersian,
                                                        onClick = { navState = HubNavigationState.ArtistPage(artist) }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                        }
                                    }

                                    if (quickPicks.isNotEmpty()) {
                                        item(key = "discover_quick_picks") {
                                            HubSectionHeader(
                                                title = if (isPersian) "پیشنهاد سریع" else "Quick picks",
                                                actionLabel = if (isPersian) "بروزرسانی" else "Refresh",
                                                onAction = { quickPicksSeed++ }
                                            )
                                            QuickPicksThreeRowGrid(
                                                tracks = quickPicks,
                                                currentPlayingTrack = currentPlayingTrack,
                                                onPlayTrack = { track -> onPlayTrack(track, quickPicks) }
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                        }
                                    }

                                    if (albums.isNotEmpty()) {
                                        item(key = "discover_albums") {
                                            HubSectionHeader(
                                                title = if (isPersian) "آلبوم‌های منتخب" else "Popular albums",
                                                actionLabel = if (isPersian) "نمایش همه" else "Show all",
                                                onAction = { activeTab = TopHubTab.ALBUMS }
                                            )
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                items(albums.take(15), key = { "hub_alb_${it.id}" }) { album ->
                                                    SpotifyAlbumCardItem(
                                                        album = album,
                                                        isPersian = isPersian,
                                                        onClick = { navState = HubNavigationState.AlbumPage(album) }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                        }
                                    }
                                }

                                TopHubTab.CATEGORIES -> {
                                    item(key = "all_categories_header") {
                                        HubSectionHeader(
                                            title = if (isPersian) "همه دسته‌بندی‌ها" else "All categories"
                                        )
                                    }
                                    spotifyCategoriesLazyRows(
                                        categoryRows = hubData.categoryRows,
                                        isPersian = isPersian,
                                        onCategoryClick = { category ->
                                            navState = HubNavigationState.CategoryPage(category)
                                        }
                                    )
                                }

                                TopHubTab.ARTISTS -> {
                                    artistsDirectoryLazyRows(
                                        artistRows = hubData.artistRows,
                                        isPersian = isPersian,
                                        onArtistClick = { navState = HubNavigationState.ArtistPage(it) }
                                    )
                                }

                                TopHubTab.ALBUMS -> {
                                    albumsDirectoryLazyRows(
                                        albumRows = hubData.albumRows,
                                        isPersian = isPersian,
                                        onAlbumClick = { navState = HubNavigationState.AlbumPage(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSongRequestSheet) {
            SongRequestModalSheet(
                remainingQuota = remainingRequests,
                initialQuery = searchQuery,
                onDismiss = { showSongRequestSheet = false }
            )
        }

        if (showVipPaywallSheet) {
            com.example.ui.components.VipPaywallSheet(
                isFarsi = isPersian,
                onDismiss = { showVipPaywallSheet = false },
                onUnlockedSuccess = {
                    showVipPaywallSheet = false
                    onRefresh()
                },
                onStartMyketPurchase = {
                    scope.launch {
                        paywallLoading = true
                        try {
                            val intentRes = withTimeoutOrNull(6000L) {
                                EntitlementManager.createMyketBuyIntent(context)
                            }
                            if (intentRes == null) {
                                android.widget.Toast.makeText(
                                    context,
                                    "پاسخی از سرور مایکت دریافت نشد. دوباره تلاش کنید یا از کد تخفیف استفاده نمایید.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                intentRes
                                    .onSuccess { pi ->
                                        myketLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                                    }
                                    .onFailure { err ->
                                        if (err.message == "ALREADY_OWNED_AND_RESTORED") {
                                            showVipPaywallSheet = false
                                            showMyketThankYouDialog = true
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                err.message ?: "خطا در اتصال به مایکت",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                            }
                        } finally {
                            paywallLoading = false
                        }
                    }
                }
            )
        }
    }
}

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
    var fetchedOnlineTracks by remember {
        mutableStateOf(OnlineMusicRepository.getMemoryCachedAppTracks())
    }
    var isLoading by remember { mutableStateOf(fetchedOnlineTracks.isEmpty()) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        if (fetchedOnlineTracks.isEmpty()) {
            isLoading = true
            val diskTracks = OnlineMusicRepository.loadCachedAppTracksAsync()
            if (diskTracks.isNotEmpty()) {
                fetchedOnlineTracks = diskTracks
                isLoading = false
            }
        }
        val resp = OnlineMusicRepository.fetchFromWorker()
        if (resp.tracks.isNotEmpty()) {
            val mapped = withContext(Dispatchers.Default) {
                resp.tracks.map { it.toAppTrack() }
            }
            OnlineMusicRepository.setMemoryCachedAppTracks(mapped)
            fetchedOnlineTracks = mapped
        }
        isLoading = false
    }

    OnlineMusicScreen(
        onlineTracks = fetchedOnlineTracks,
        isLoading = isLoading,
        currentPlayingTrack = null,
        isPersian = lang == AppLanguage.PERSIAN,
        onPlayTrack = { track, list ->
            val onlineTrack = OnlineTrack(
                id = track.id.removePrefix("online_"),
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationSec = (track.durationMs / 1000).toInt(),
                streamUrl = track.uri,
                coverUrl = track.artworkUri ?: ""
            )
            val queueOnline = list.map {
                OnlineTrack(
                    id = it.id.removePrefix("online_"),
                    title = it.title,
                    artist = it.artist,
                    album = it.album,
                    durationSec = (it.durationMs / 1000).toInt(),
                    streamUrl = it.uri,
                    coverUrl = it.artworkUri ?: ""
                )
            }
            onExternalPlay?.invoke(onlineTrack, track.uri, queueOnline)
        },
        onRefresh = { refreshKey++ },
        bottomPadding = bottomPadding,
        onDownloadTrack = onDownloadToDeviceLibrary
    )
}

@Composable
private fun SpotifyGlassTopBar(
    totalTracks: Int,
    totalArtists: Int,
    searchQuery: String,
    isPersian: Boolean,
    isPremium: Boolean = false,
    remainingRequests: Int = 3,
    onRequestSong: () -> Unit = {},
    onOpenVipPaywall: () -> Unit = {},
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isPersian) "جستجو" else "Search",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isPersian)
                        "$totalTracks آهنگ • $totalArtists هنرمند"
                    else
                        "$totalTracks songs • $totalArtists artists",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isPremium) AuraSpotifyGreen.copy(alpha = 0.16f) else AuraSurfaceElevated)
                        .clickable(onClick = onRequestSong)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isPremium) Icons.Default.Add else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isPremium) AuraSpotifyGreen else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isPersian) {
                            if (isPremium) "درخواست ($remainingRequests/۳)" else "درخواست آهنگ"
                        } else {
                            if (isPremium) "Request ($remainingRequests/3)" else "Request Song"
                        },
                        color = if (isPremium) AuraSpotifyGreen else Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AuraSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = if (isPersian) "بروزرسانی" else "Refresh",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (!isPremium) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AuraSurfaceElevated)
                    .clickable { onOpenVipPaywall() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = AuraSpotifyGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isPersian) "ارتقا به نسخه پریمیوم (بدون تبلیغات)" else "Upgrade to Premium (Ad-free)",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (isPersian) "مشاهده" else "View",
                    color = AuraSpotifyGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Authentic Spotify High-Contrast Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = if (isPersian)
                        "چه آهنگی می‌خواهید گوش دهید؟"
                    else
                        "What do you want to listen to?",
                    color = Color(0xFF535353),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF121212),
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFF121212)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color(0xFF121212),
                unfocusedTextColor = Color(0xFF121212),
                cursorColor = Color(0xFF121212)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppleMusicHeroPager(
    onlineTracks: List<Track>,
    browseCategories: List<SpotifyBrowseCategory>,
    isPersian: Boolean,
    onPlayMix: (List<Track>) -> Unit
) {
    val heroCards = remember(onlineTracks, browseCategories, isPersian) {
        buildList {
            add(
                Triple(
                    if (isPersian) "میکس روزانه شما" else "Your Daily Mix",
                    if (isPersian) "گلچینی از برترین آهنگ‌های آرشیو" else "Curated tracks from your cloud library",
                    Pair(
                        listOf(Color(0xFF1E3264), Color(0xFF141E3C)),
                        onlineTracks.shuffled().take(40)
                    )
                )
            )
            browseCategories.take(2).forEach { cat ->
                add(
                    Triple(
                        if (isPersian) cat.titleFa else cat.titleEn,
                        if (isPersian) cat.subtitleFa else cat.subtitleEn,
                        Pair(
                            cat.gradient,
                            cat.matchedTracks
                        )
                    )
                )
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { heroCards.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 12.dp
    ) { page ->
        val (title, subtitle, payload) = heroCards[page]
        val (colors, tracksToPlay) = payload
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(colors))
                .clickable { onPlayMix(tracksToPlay) }
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isPersian) "میکس ویژه" else "FEATURED MIX",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AuraSpotifyGreen)
                            .clickable { onPlayMix(tracksToPlay) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Mix",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPicksThreeRowGrid(
    tracks: List<Track>,
    currentPlayingTrack: Track?,
    onPlayTrack: (Track) -> Unit
) {
    val columns = remember(tracks) { tracks.chunked(3) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(columns) { columnTracks ->
            Column(
                modifier = Modifier.width(284.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                columnTracks.forEach { track ->
                    val isPlaying = currentPlayingTrack?.id == track.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AuraSurface)
                            .clickable { onPlayTrack(track) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CloudTrackCoverBox(
                            artworkUri = track.artworkUri,
                            trackId = track.id,
                            fallbackName = SmartArtistParser.extractPrimaryArtist(track.artist, track.title),
                            size = 48.dp,
                            shape = RoundedCornerShape(6.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = if (isPlaying) AuraSpotifyGreen else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = track.artist,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Equalizer else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isPlaying) AuraSpotifyGreen else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.spotifyCategoriesLazyRows(
    categoryRows: List<List<SpotifyBrowseCategory>>,
    isPersian: Boolean,
    onCategoryClick: (SpotifyBrowseCategory) -> Unit
) {
    items(
        items = categoryRows,
        key = { row -> "cat_row_${row.firstOrNull()?.id.orEmpty()}" }
    ) { rowItems ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rowItems.forEach { category ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(96.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(category.gradient))
                        .clickable { onCategoryClick(category) }
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxWidth(0.76f)
                    ) {
                        Text(
                            text = if (isPersian) category.titleFa else category.titleEn,
                            color = Color.White,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPersian) category.subtitleFa else category.subtitleEn,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 10.dp, y = 10.dp)
                            .rotate(25f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            if (rowItems.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpotifyArtistCard(
    artist: CloudArtistProfile,
    isPersian: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(118.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpotifyArtistAvatar(
            artistName = artist.name,
            size = 104.dp,
            shape = CircleShape,
            showVerifiedBadge = false,
            explicitArtistImageUrl = artist.cover.ifBlank { artist.picture }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isPersian) "${artist.tracks.size} آهنگ" else "Artist",
            color = TextSecondary,
            fontSize = 11.5.sp
        )
    }
}

@Composable
private fun SpotifyAlbumCardItem(
    album: CloudAlbumCollection,
    isPersian: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(146.dp)
            .clickable(onClick = onClick)
    ) {
        CloudTrackCoverBox(
            artworkUri = album.coverUri,
            trackId = album.tracks.firstOrNull()?.id,
            fallbackName = album.artist.ifBlank { album.title },
            size = 146.dp,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            color = Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = album.artist,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun LazyListScope.categorizedInstantSearchItems(
    matchedArtists: List<CloudArtistProfile>,
    matchedAlbums: List<CloudAlbumCollection>,
    matchedTracks: List<Track>,
    currentPlayingTrack: Track?,
    isPersian: Boolean,
    onArtistClick: (CloudArtistProfile) -> Unit,
    onAlbumClick: (CloudAlbumCollection) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onDownloadTrack: ((Track) -> Unit)? = null
) {
    if (matchedArtists.isNotEmpty()) {
        item(key = "srch_artists_sec") {
            HubSectionHeader(
                title = if (isPersian) "هنرمندان" else "Artists"
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(matchedArtists, key = { "srch_art_${it.name}" }) { artist ->
                    SpotifyArtistCard(
                        artist = artist,
                        isPersian = isPersian,
                        onClick = { onArtistClick(artist) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (matchedAlbums.isNotEmpty()) {
        item(key = "srch_albums_sec") {
            HubSectionHeader(
                title = if (isPersian) "آلبوم‌ها" else "Albums"
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(matchedAlbums, key = { "srch_alb_${it.id}" }) { album ->
                    SpotifyAlbumCardItem(
                        album = album,
                        isPersian = isPersian,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (matchedTracks.isNotEmpty()) {
        item(key = "srch_tracks_hdr") {
            HubSectionHeader(
                title = if (isPersian) "آهنگ‌ها" else "Songs"
            )
        }
        items(matchedTracks, key = { "srch_trk_${it.id}" }) { track ->
            SpotifyTrackRowItem(
                track = track,
                isPlaying = currentPlayingTrack?.id == track.id,
                onClick = { onPlayTrack(track, matchedTracks) },
                onDownloadClick = onDownloadTrack?.let { { it(track) } }
            )
        }
    }
}

@Composable
private fun SpotifyFullArtistScreen(
    artist: CloudArtistProfile,
    currentPlayingTrack: Track?,
    isPersian: Boolean,
    onBack: () -> Unit,
    onAlbumClick: (CloudAlbumCollection) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onDownloadTrack: ((Track) -> Unit)? = null
) {
    var showAllSoloTracks by remember { mutableStateOf(false) }
    var showAllFeatTracks by remember { mutableStateOf(false) }

    val primaryList = if (artist.soloTracks.isNotEmpty()) artist.soloTracks else artist.tracks
    val visiblePrimaryTracks = if (showAllSoloTracks) primaryList else primaryList.take(5)
    val visibleFeatTracks = if (showAllFeatTracks) artist.featuredTracks else artist.featuredTracks.take(5)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF282828),
                                AuraCanvas
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SpotifyArtistAvatar(
                        artistName = artist.name,
                        size = 148.dp,
                        shape = CircleShape,
                        showVerifiedBadge = true,
                        explicitArtistImageUrl = artist.cover.ifBlank { artist.picture }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = artist.name,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPersian) {
                            "${artist.tracks.size} آهنگ در آرشیو"
                        } else {
                            "${artist.tracks.size} tracks"
                        },
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (artist.tracks.isNotEmpty()) {
                                    val shuffled = artist.tracks.shuffled()
                                    onPlayTrack(shuffled.first(), shuffled)
                                }
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "پخش تصادفی" else "Shuffle", fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AuraSpotifyGreen)
                                .clickable {
                                    if (artist.tracks.isNotEmpty()) {
                                        onPlayTrack(artist.tracks.first(), artist.tracks)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            HubSectionHeader(
                title = if (isPersian) "محبوب‌ترین‌ها" else "Popular",
                actionLabel = if (primaryList.size > 5) {
                    if (showAllSoloTracks) {
                        if (isPersian) "کمتر" else "Show less"
                    } else {
                        if (isPersian) "نمایش همه (${primaryList.size})" else "See all (${primaryList.size})"
                    }
                } else null,
                onAction = { showAllSoloTracks = !showAllSoloTracks }
            )
        }

        items(visiblePrimaryTracks, key = { "art_trk_${it.id}" }) { track ->
            SpotifyTrackRowItem(
                track = track,
                isPlaying = currentPlayingTrack?.id == track.id,
                onClick = { onPlayTrack(track, artist.tracks) },
                onDownloadClick = onDownloadTrack?.let { { it(track) } }
            )
        }

        if (artist.featuredTracks.isNotEmpty() && artist.soloTracks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                HubSectionHeader(
                    title = if (isPersian) "همکاری‌ها (Featuring)" else "Appears On",
                    actionLabel = if (artist.featuredTracks.size > 5) {
                        if (showAllFeatTracks) {
                            if (isPersian) "کمتر" else "Show less"
                        } else {
                            if (isPersian) "نمایش همه (${artist.featuredTracks.size})" else "See all (${artist.featuredTracks.size})"
                        }
                    } else null,
                    onAction = { showAllFeatTracks = !showAllFeatTracks }
                )
            }

            items(visibleFeatTracks, key = { "art_feat_${it.id}" }) { track ->
                SpotifyTrackRowItem(
                    track = track,
                    isPlaying = currentPlayingTrack?.id == track.id,
                    onClick = { onPlayTrack(track, artist.tracks) },
                    onDownloadClick = onDownloadTrack?.let { { it(track) } }
                )
            }
        }

        if (artist.albums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                HubSectionHeader(
                    title = if (isPersian) "آلبوم‌ها" else "Albums"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(artist.albums, key = { "art_alb_${it.id}" }) { album ->
                        SpotifyAlbumCardItem(
                            album = album,
                            isPersian = isPersian,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyFullAlbumScreen(
    album: CloudAlbumCollection,
    currentPlayingTrack: Track?,
    isPersian: Boolean,
    onBack: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onDownloadTrack: ((Track) -> Unit)? = null
) {
    var visibleLimit by remember(album.id) { mutableIntStateOf(30) }
    val visibleTracks = remember(album.tracks, visibleLimit) {
        album.tracks.take(visibleLimit)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item(key = "alb_header_${album.id}") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF282828), AuraCanvas)
                        )
                    )
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                CloudTrackCoverBox(
                    artworkUri = album.coverUri,
                    trackId = album.tracks.firstOrNull()?.id,
                    fallbackName = album.artist.ifBlank { album.title },
                    size = 180.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = album.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPersian)
                        "${album.artist} • ${album.tracks.size} آهنگ"
                    else
                        "${album.artist} • ${album.tracks.size} songs",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AuraSpotifyGreen)
                        .clickable {
                            if (album.tracks.isNotEmpty()) {
                                onPlayTrack(album.tracks.first(), album.tracks)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Album",
                        tint = Color.Black,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        items(visibleTracks, key = { "alb_trk_${it.id}" }) { track ->
            SpotifyTrackRowItem(
                track = track,
                isPlaying = currentPlayingTrack?.id == track.id,
                onClick = { onPlayTrack(track, album.tracks) },
                onDownloadClick = onDownloadTrack?.let { { it(track) } }
            )
        }

        if (visibleLimit < album.tracks.size) {
            item(key = "alb_load_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { visibleLimit += 30 },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (isPersian) "نمایش بیشتر (${album.tracks.size - visibleLimit})" else "Load More (${album.tracks.size - visibleLimit})",
                            color = AuraSpotifyGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyCategoryPlaylistScreen(
    category: SpotifyBrowseCategory,
    currentPlayingTrack: Track?,
    isPersian: Boolean,
    onBack: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onDownloadTrack: ((Track) -> Unit)? = null
) {
    val tracks = category.matchedTracks
    var visibleLimit by remember { mutableIntStateOf(40) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(category.gradient + AuraCanvas))
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Column {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPersian) category.titleFa else category.titleEn,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPersian) category.subtitleFa else category.subtitleEn,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                val shuffled = tracks.shuffled()
                                onPlayTrack(shuffled.first(), shuffled)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuraSpotifyGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "پخش تصادفی" else "Shuffle Play",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        items(tracks.take(visibleLimit), key = { "cat_trk_${it.id}" }) { track ->
            SpotifyTrackRowItem(
                track = track,
                isPlaying = currentPlayingTrack?.id == track.id,
                onClick = { onPlayTrack(track, tracks) },
                onDownloadClick = onDownloadTrack?.let { { it(track) } }
            )
        }

        if (visibleLimit < tracks.size) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { visibleLimit += 40 },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (isPersian) "نمایش بیشتر" else "Load More",
                            color = AuraSpotifyGreen
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.artistsDirectoryLazyRows(
    artistRows: List<List<CloudArtistProfile>>,
    isPersian: Boolean,
    onArtistClick: (CloudArtistProfile) -> Unit
) {
    items(
        items = artistRows,
        key = { row -> "art_row_${row.firstOrNull()?.name.orEmpty()}" }
    ) { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            row.forEach { artist ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SpotifyArtistCard(
                        artist = artist,
                        isPersian = isPersian,
                        onClick = { onArtistClick(artist) }
                    )
                }
            }
            repeat(3 - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun LazyListScope.albumsDirectoryLazyRows(
    albumRows: List<List<CloudAlbumCollection>>,
    isPersian: Boolean,
    onAlbumClick: (CloudAlbumCollection) -> Unit
) {
    items(
        items = albumRows,
        key = { row -> "alb_row_${row.firstOrNull()?.id.orEmpty()}" }
    ) { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            row.forEach { album ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SpotifyAlbumCardItem(
                        album = album,
                        isPersian = isPersian,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
            if (row.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpotifyTrackRowItem(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDownloadClick: (() -> Unit)? = null,
    isPersian: Boolean = true
) {
    val context = LocalContext.current
    val storeVersion = com.example.data.model.UserTrackMetadataStore.version
    val effectiveTrack = remember(track, storeVersion) {
        com.example.data.model.UserTrackMetadataStore.applyOverride(track)
    }
    var showEditDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CloudTrackCoverBox(
            artworkUri = effectiveTrack.artworkUri,
            trackId = effectiveTrack.id,
            fallbackName = SmartArtistParser.extractPrimaryArtist(effectiveTrack.artist, effectiveTrack.title),
            size = 48.dp,
            shape = RoundedCornerShape(6.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = effectiveTrack.title,
                color = if (isPlaying) AuraSpotifyGreen else Color.White,
                fontSize = 15.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${effectiveTrack.artist} • ${effectiveTrack.album}",
                color = TextSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onDownloadClick != null) {
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DownloadForOffline,
                    contentDescription = "Download",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        IconButton(
            onClick = { showEditDialog = true },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showEditDialog) {
        com.example.ui.components.MetadataEditorDialog(
            track = effectiveTrack,
            lang = if (isPersian) AppLanguage.PERSIAN else AppLanguage.ENGLISH,
            onSave = { newTitle, newArtist, newAlbum, _, _, _ ->
                com.example.data.model.UserTrackMetadataStore.saveOverride(
                    context,
                    track.id,
                    newTitle,
                    newArtist,
                    newAlbum
                )
                android.widget.Toast.makeText(
                    context,
                    if (isPersian) "مشخصات آهنگ با موفقیت اصلاح شد" else "Track info updated",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
private fun HubSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Text(
                text = actionLabel,
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CloudTrackCoverBox(
    artworkUri: String?,
    trackId: String?,
    fallbackName: String,
    size: Dp,
    shape: Shape
) {
    val resolvedUrl = remember(artworkUri, trackId) {
        val raw = artworkUri?.trim().orEmpty()
        if (raw.isNotBlank() && !raw.contains("dzcdn.net") && !raw.contains("deezer.com") && !raw.contains("/artist-cover")) {
            OnlineMusicRepository.normalizeWorkerImageUrl(raw)
        } else if (!trackId.isNullOrBlank()) {
            OnlineMusicRepository.getCoverUrlForTrack(trackId)
        } else {
            ""
        }
    }

    var cachedBitmap by remember(resolvedUrl) {
        mutableStateOf(
            if (resolvedUrl.isNotBlank()) OnlineMusicRepository.getCachedCoverBitmap(resolvedUrl) else null
        )
    }

    LaunchedEffect(resolvedUrl) {
        if (cachedBitmap == null && resolvedUrl.isNotBlank()) {
            cachedBitmap = OnlineMusicRepository.loadCoverBitmap(resolvedUrl)
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(AuraSurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        val bmp = cachedBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = fallbackName,
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(if (size >= 100.dp) 32.dp else 20.dp)
            )
        }
    }
}

@Composable
fun AuraInlinePulseLoader(isPersian: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = AuraSpotifyGreen,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = if (isPersian) "در حال بارگذاری آرشیو ابری..." else "Loading cloud library...",
                color = TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AuraStartupPreloaderWrapper(
    localTracks: List<Track> = emptyList(),
    isPersian: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(OnlineMusicRepository.isStartupPreloadCompleted()) }
    var backendProgress by remember { mutableFloatStateOf(if (isReady) 1f else 0.08f) }
    var timerProgress by remember { mutableFloatStateOf(if (isReady) 1f else 0.08f) }
    var statusText by remember(isPersian) {
        mutableStateOf(
            if (isPersian) "در حال آماده‌سازی آرشیو موسیقی..."
            else "Preparing your music library..."
        )
    }

    val effectiveProgress = maxOf(backendProgress, timerProgress).coerceIn(0.08f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = effectiveProgress,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "StartupPreloadProgress"
    )

    LaunchedEffect(Unit) {
        if (!isReady) {
            OnlineMusicRepository.startBackgroundPreload(
                context = context,
                localTracks = localTracks,
                isPersian = isPersian
            ) { pct, msg ->
                backendProgress = pct
                if (msg.isNotBlank()) {
                    statusText = msg
                }
                if (pct >= 0.99f) {
                    OnlineMusicRepository.markStartupPreloadCompleted()
                    isReady = true
                }
            }

            val totalTicks = 50
            for (tick in 1..totalTicks) {
                if (isReady || OnlineMusicRepository.isStartupPreloadCompleted()) break
                delay(100L)
                val fraction = tick.toFloat() / totalTicks.toFloat()
                timerProgress = (0.08f + fraction * 0.92f).coerceIn(0.08f, 1f)
            }

            timerProgress = 1f
            backendProgress = 1f
            OnlineMusicRepository.markStartupPreloadCompleted()
            isReady = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = !isReady,
            exit = fadeOut(animationSpec = tween(320, easing = FastOutSlowInEasing))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AuraCanvas)
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .fillMaxWidth(0.82f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AuraSpotifyGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Aura Music",
                            tint = Color.Black,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Aura Music",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = statusText,
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AuraSurfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress.coerceIn(0.08f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(AuraSpotifyGreen)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongRequestModalSheet(
    remainingQuota: Int,
    initialQuery: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var songTitle by remember { mutableStateOf(initialQuery) }
    var artistName by remember { mutableStateOf("") }
    var extraNote by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AuraSurfaceElevated,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "درخواست آهنگ جدید (ویژه VIP)",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "سهمیه باقی‌مانده امروز شما: $remainingQuota از ۳ درخواست",
                color = AuraSpotifyGreen,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )

            OutlinedTextField(
                value = songTitle,
                onValueChange = { songTitle = it },
                label = { Text("نام آهنگ *") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AuraSpotifyGreen
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = artistName,
                onValueChange = { artistName = it },
                label = { Text("نام خواننده *") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AuraSpotifyGreen
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = extraNote,
                onValueChange = { extraNote = it },
                label = { Text("توضیحات یا لینک (اختیاری)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AuraSpotifyGreen
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (!feedback.isNullOrBlank()) {
                Text(
                    text = feedback!!,
                    color = AuraSpotifyGreen,
                    fontSize = 13.sp
                )
            }

            Button(
                onClick = {
                    if (songTitle.isBlank() || artistName.isBlank()) {
                        feedback = "لطفاً نام آهنگ و نام خواننده را وارد کنید."
                        return@Button
                    }
                    scope.launch {
                        isSending = true
                        val res = EntitlementManager.submitSongRequest(context, songTitle, artistName, extraNote)
                        res.onSuccess { msg ->
                            feedback = msg
                            songTitle = ""
                            artistName = ""
                            extraNote = ""
                        }.onFailure { err ->
                            feedback = err.message
                        }
                        isSending = false
                    }
                },
                enabled = !isSending && remainingQuota > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuraSpotifyGreen,
                    contentColor = Color.Black
                )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ارسال به آرشیو", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
