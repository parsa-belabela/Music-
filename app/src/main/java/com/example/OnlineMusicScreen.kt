package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.ArtistAvatarDiskCache
import com.example.ui.components.SpotifyArtistAvatar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

const val WORKER_URL = "https://alubm.parsaghorbani8861.workers.dev"

/**
 * مدل خواننده دریافتی از سرور کلادفلر (آرشیو تلگرام)
 */
data class ArchiveArtistItem(
    val name: String,
    val trackCount: Int = 0,
    val cover: String = "",
    val picture: String = cover
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
        return Track(
            id = appTrackId,
            title = title,
            artist = artist,
            album = album.ifBlank { "Single" },
            durationMs = (durationSec.coerceAtLeast(1) * 1000L),
            uri = finalStream,
            artworkUri = finalCover.ifBlank { null },
            bitrate = 320,
            sampleRate = 48000,
            genre = rawGenre.ifBlank { "Online" }
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
    DISCOVER("Discover", "ویترین", Icons.Default.AutoAwesome),
    CATEGORIES("Categories", "دسته‌بندی‌ها", Icons.Default.GridView),
    ARTISTS("Artists", "هنرمندان", Icons.Default.People),
    ALBUMS("Albums", "آلبوم‌ها", Icons.Default.Album)
}

private sealed class HubNavigationState {
    object MainHub : HubNavigationState()
    data class ArtistPage(val artist: CloudArtistProfile) : HubNavigationState()
    data class AlbumPage(val album: CloudAlbumCollection) : HubNavigationState()
    data class CategoryPage(val category: SpotifyBrowseCategory) : HubNavigationState()
}

/**
 * تفکیک‌کننده هوشمند نام خواننده‌ها و فیت‌ها (Collaborations & Features):
 * مثال: "Future feat. Drake" یا "فیوچر فیت فلانی" را به دو هنرمند مجزا ("Future" و "Drake") تقسیم می‌کند
 * تا هیچ پروفایل تکراری مثل "Future feat. X" ساخته نشود و آهنگ فیت در پروفایل هر دو خواننده قرار بگیرد.
 */
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

        // بررسی فیت‌های نوشته‌شده در عنوان آهنگ مثل "Song Name (feat. Travis Scott)"
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

/**
 * نگهداری ژانر/کتگوری واقعی هر ترک که از سرور کلادفلر/ساندکلاود/تلگرام دریافت می‌شود
 */
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

/**
 * موتور دسته‌بندی ۱۰۰٪ واقعی بر اساس متادیتا، تگ‌های سرور، ژانر و نام خواننده
 * بدون هیچ‌گونه استفاده از hashCode یا انتخاب تصادفی!
 */
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
                    titleEn = "Rap & Hip-Hop",
                    titleFa = "رپ و هیپ‌هاپ",
                    subtitleEn = "${rapTracks.size} real hip-hop & rap tracks",
                    subtitleFa = "${rapTracks.size} قطعه رپ و هیپ‌هاپ واقعی",
                    icon = Icons.Default.GraphicEq,
                    gradient = listOf(Color(0xFF8338EC), Color(0xFF240046)),
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
                    titleEn = "Romantic & Emotional",
                    titleFa = "عاشقانه و احساسی",
                    subtitleEn = "${romanticTracks.size} love & emotional songs",
                    subtitleFa = "${romanticTracks.size} آهنگ عاشقانه و احساسی",
                    icon = Icons.Default.Favorite,
                    gradient = listOf(Color(0xFFE1306C), Color(0xFF4A0E2E)),
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
                    titleEn = "DJ Mix & Electronic",
                    titleFa = "ریمیکس و الکترونیک",
                    subtitleEn = "${remixTracks.size} club, DJ & remix tracks",
                    subtitleFa = "${remixTracks.size} ریمیکس و موزیک پرانرژی",
                    icon = Icons.Default.Bolt,
                    gradient = listOf(Color(0xFFFF9F1C), Color(0xFF5C2D05)),
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
                    titleEn = "Chill & Instrumental",
                    titleFa = "بی‌کلام و آرامش‌بخش",
                    subtitleEn = "${chillTracks.size} relaxing & instrumental tracks",
                    subtitleFa = "${chillTracks.size} قطعه بی‌کلام و لوفای",
                    icon = Icons.Default.NightsStay,
                    gradient = listOf(Color(0xFF00B4D8), Color(0xFF03045E)),
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
                    titleEn = "Legends & Nostalgia",
                    titleFa = "سنتی و نوستالژی",
                    subtitleEn = "${traditionalTracks.size} classic & timeless tracks",
                    subtitleFa = "${traditionalTracks.size} اثر ماندگار و سنتی",
                    icon = Icons.Default.History,
                    gradient = listOf(Color(0xFFEC4899), Color(0xFF500724)),
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
                    subtitleEn = "${rockTracks.size} rock & guitar tracks",
                    subtitleFa = "${rockTracks.size} قطعه راک و آلترناتیو",
                    icon = Icons.Default.Speaker,
                    gradient = listOf(Color(0xFFEF4444), Color(0xFF450A0A)),
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
                    subtitleEn = "${persianTracks.size} Persian tracks",
                    subtitleFa = "${persianTracks.size} آهنگ پاپ فارسی",
                    icon = Icons.Default.MusicNote,
                    gradient = listOf(Color(0xFF1DB954), Color(0xFF093819)),
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
                    titleEn = "Global & International",
                    titleFa = "موزیک خارجی",
                    subtitleEn = "${internationalTracks.size} international tracks",
                    subtitleFa = "${internationalTracks.size} آهنگ خارجی و جهانی",
                    icon = Icons.Default.Public,
                    gradient = listOf(Color(0xFF3A86FF), Color(0xFF0B132B)),
                    matchedTracks = internationalTracks
                )
            )
        }

        return rawCategories
    }
}

/**
 * مخزن ارتباط مستقیم با سرور Cloudflare Worker متصل به کانال آرشیو تلگرام
 * به همراه کش داخلی خصوصی کاتالوگ و پروفایل هنرمندان
 */
object OnlineMusicRepository {
    const val WORKER_URL = com.example.WORKER_URL
    val BASE_URL: String get() = WORKER_URL.trimEnd('/')
    private const val PRIVATE_CATALOG_CACHE_FILE = "aura_cloud_catalog_cache_v2.json"

    private val imageMemoryCache = object : LruCache<String, Bitmap>(110) {}
    private val streamUrlByTrackId = ConcurrentHashMap<String, String>()
    private val downloadUrlByTrackId = ConcurrentHashMap<String, String>()
    private val coverUrlByTrackId = ConcurrentHashMap<String, String>()
    private val artistPictureCache = ConcurrentHashMap<String, String>()
    private val prefetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

    fun registerArtistPicture(artistName: String, pictureUrl: String, overwrite: Boolean = true) {
        if (artistName.isBlank() || pictureUrl.isBlank()) return
        val key = artistName.trim().lowercase()
        val normalized = normalizeWorkerImageUrl(pictureUrl)
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

    /**
     * بارگذاری فوری کاتالوگ ذخیره‌شده در حافظه خصوصی داخلی گوشی (برای باز شدن آنی در دفعات بعدی)
     */
    fun loadCachedWorkerCatalog(): WorkerTracksResponse? {
        return try {
            val appCtx = AuraApplication.instance.applicationContext
            val file = File(appCtx.filesDir, PRIVATE_CATALOG_CACHE_FILE)
            if (file.exists() && file.length() > 64L) {
                val jsonText = file.readText()
                parseWorkerTracksJson(jsonText)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveWorkerCatalogToPrivateDisk(rawJson: String) {
        try {
            val appCtx = AuraApplication.instance.applicationContext
            val file = File(appCtx.filesDir, PRIVATE_CATALOG_CACHE_FILE)
            file.writeText(rawJson)
        } catch (_: Exception) {}
    }

    private fun prefetchArtistAvatarsInBackground(artists: List<ArchiveArtistItem>) {
        prefetchScope.launch {
            try {
                val appCtx = AuraApplication.instance.applicationContext
                for (artist in artists) {
                    val url = artist.cover.ifBlank { artist.picture }
                    if (url.isNotBlank()) {
                        ArtistAvatarDiskCache.loadOrDownloadArtistBitmap(
                            context = appCtx,
                            artistName = artist.name,
                            remoteUrl = url
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

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
                if (query.isBlank() && artist.isBlank() && body.length > 64) {
                    saveWorkerCatalogToPrivateDisk(body)
                }
                val parsed = parseWorkerTracksJson(body)
                if (parsed.artists.isNotEmpty()) {
                    prefetchArtistAvatarsInBackground(parsed.artists)
                }
                return@withContext parsed
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }
        loadCachedWorkerCatalog() ?: WorkerTracksResponse(artists = lastKnownArtists)
    }

    private fun parseWorkerTracksJson(jsonString: String): WorkerTracksResponse {
        return try {
            val root = JSONObject(jsonString)

            // ۱. پارس کردن آرایه خواننده‌ها و ادغام هوشمند نام‌های فیت در خواننده اصلی
            val mergedArtistsMap = LinkedHashMap<String, ArchiveArtistItem>()
            val artistsArray = root.optJSONArray("artists")
            if (artistsArray != null) {
                for (i in 0 until artistsArray.length()) {
                    val obj = artistsArray.optJSONObject(i) ?: continue
                    val rawName = obj.optString("name", "").trim()
                    if (rawName.isBlank()) continue
                    val trackCount = obj.optInt("trackCount", obj.optInt("count", 0))
                    val rawCover = obj.optString("cover", "")
                        .ifBlank { obj.optString("picture", "") }
                        .ifBlank { obj.optString("cover_url", "") }
                        .ifBlank { obj.optString("picture_url", "") }
                    val cover = normalizeWorkerImageUrl(rawCover)

                    val isCollab = SmartArtistParser.isCollaborationString(rawName)
                    val individualNames = SmartArtistParser.extractArtistsFromTrack(rawName)

                    if (!isCollab && individualNames.size == 1) {
                        val cleanName = individualNames.first()
                        val key = cleanName.lowercase()
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
                        // اگر سرور نام فیت مثل "Future feat. X" فرستاده بود، آن را به پروفایل خواننده‌های اصلی اضافه کن
                        // اما کاور ترک فیت را به عنوان عکس پروفایل خواننده ثبت نکن تا عکس شخص دیگری روی پروفایل نیفتد
                        individualNames.forEach { singleName ->
                            val key = singleName.lowercase()
                            val existing = mergedArtistsMap[key]
                            val bestCover = existing?.cover?.takeIf { it.isNotBlank() }
                                ?: getArtistPicture(singleName).orEmpty()
                            mergedArtistsMap[key] = ArchiveArtistItem(
                                name = existing?.name ?: singleName,
                                trackCount = (existing?.trackCount ?: 0) + trackCount,
                                cover = bestCover,
                                picture = bestCover
                            )
                        }
                    }
                }
            }

            val artistsList = mergedArtistsMap.values.toList()
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
                            val rawName = item.optString("name", "").trim()
                            if (rawName.isNotBlank()) {
                                val count = item.optInt("trackCount", item.optInt("count", 0))
                                val rawCover = item.optString("cover", "")
                                    .ifBlank { item.optString("picture", "") }
                                    .ifBlank { item.optString("cover_url", "") }
                                    .ifBlank { item.optString("picture_url", "") }
                                val primaryName = SmartArtistParser.extractPrimaryArtist(rawName)
                                val cover = normalizeWorkerImageUrl(rawCover).ifBlank {
                                    getArtistPicture(primaryName) ?: ""
                                }
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
                        is String -> {
                            val primaryName = SmartArtistParser.extractPrimaryArtist(item)
                            if (primaryName.isNotBlank()) {
                                val fromAll = artistsList.firstOrNull { it.name.equals(primaryName, ignoreCase = true) }
                                val pic = getArtistPicture(primaryName) ?: ""
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
                    val rawGenre = obj.optString("genre", "")
                        .ifBlank { obj.optString("category", "") }
                        .ifBlank { obj.optString("style", "") }
                        .ifBlank { obj.optString("mood", "") }
                        .trim()

                    val streamUrl = resolveWorkerUrl(rawStream)
                    val downloadUrl = resolveWorkerUrl(rawDownload)
                    val coverUrl = normalizeWorkerImageUrl(rawCover).ifBlank {
                        getArtistPicture(SmartArtistParser.extractPrimaryArtist(artistName, title)) ?: ""
                    }

                    registerTrackUrls(
                        trackId = id,
                        streamUrl = streamUrl,
                        downloadUrl = downloadUrl,
                        coverUrl = coverUrl,
                        artistName = artistName
                    )

                    if (rawGenre.isNotBlank()) {
                        RealTrackGenreRegistry.registerGenre(id, rawGenre)
                        RealTrackGenreRegistry.registerGenre("online_$id", rawGenre)
                    }

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
                            artistPictureUrl = getArtistPicture(SmartArtistParser.extractPrimaryArtist(artistName, title)) ?: coverUrl,
                            rawGenre = rawGenre
                        )
                    )
                }
            }

            WorkerTracksResponse(
                artists = if (artistsList.isNotEmpty()) artistsList else lastKnownArtists,
                matchedArtists = matchedList.distinctBy { it.name.lowercase() },
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
 * مدیریت دانلود فایل‌ها و کش صوتی
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
 * ذخیره و بازخوانی سابقه آنلاین
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

/**
 * صفحه اصلی کاوش و استریم آنلاین به سبک Spotify & Apple Music
 * - تفکیک هوشمند خواننده‌ها و فیت‌ها (بدون ساخت پروفایل تکراری برای فیت‌ها)
 * - دسته‌بندی‌های ۱۰۰٪ واقعی بر اساس ژانر و متادیتا
 * - پشتیبانی کامل دو زبانه (انگلیسی و فارسی)
 */
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
    var searchQuery by remember { mutableStateOf("") }
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

    // ۱. گروه‌بندی هوشمند آلبوم‌ها بر اساس خواننده اصلی (بدون تداخل فیت‌ها)
    val albums = remember(onlineTracks, isPersian) {
        onlineTracks
            .groupBy { track ->
                val primaryArtist = SmartArtistParser.extractPrimaryArtist(track.artist, track.title).ifBlank {
                    if (isPersian) "هنرمند ناشناس" else "Unknown Artist"
                }
                val cleanAlbum = track.album.trim().let {
                    if (it.isBlank() || it.equals("unknown", true) || it.equals("Telegram", true) || it.equals("Telegram Archive", true)) {
                        if (isPersian) "مجموعه آثار $primaryArtist" else "$primaryArtist Collection"
                    } else it
                }
                "${primaryArtist.lowercase()}__${cleanAlbum.lowercase()}"
            }
            .map { (key, list) ->
                val first = list.first()
                val primaryArtist = SmartArtistParser.extractPrimaryArtist(first.artist, first.title).ifBlank {
                    if (isPersian) "هنرمند ناشناس" else "Unknown Artist"
                }
                val cleanTitle = first.album.trim().let {
                    if (it.isBlank() || it.equals("unknown", true) || it.equals("Telegram", true) || it.equals("Telegram Archive", true)) {
                        if (isPersian) "مجموعه آثار $primaryArtist" else "$primaryArtist Collection"
                    } else it
                }
                CloudAlbumCollection(
                    id = key,
                    title = cleanTitle,
                    artist = primaryArtist,
                    coverUri = list.firstOrNull { !it.artworkUri.isNullOrBlank() }?.artworkUri,
                    tracks = list
                )
            }
            .sortedByDescending { it.tracks.size }
    }

    // ۲. استخراج هوشمند پروفایل یکتای هر هنرمند:
    // آهنگ‌های فیت (مثلاً "Future feat. Drake") هرگز به عنوان خواننده جدید ساخته نمی‌شوند،
    // بلکه هم در پروفایل Future و هم در پروفایل Drake قرار می‌گیرند.
    val artists = remember(onlineTracks, albums, isPersian) {
        val workerArtistsMap = OnlineMusicRepository.getKnownArtists()
            .associateBy { it.name.trim().lowercase() }

        val canonicalDisplayNames = LinkedHashMap<String, String>()
        val artistAllTracks = LinkedHashMap<String, MutableList<Track>>()
        val artistSoloTracks = LinkedHashMap<String, MutableList<Track>>()
        val artistFeatTracks = LinkedHashMap<String, MutableList<Track>>()

        // ابتدا نام‌های استاندارد هنرمندان از سرور کلادفلر را ثبت کن
        workerArtistsMap.values.forEach { item ->
            val clean = SmartArtistParser.cleanSingleArtistName(item.name)
            if (clean.isNotBlank() && !SmartArtistParser.isCollaborationString(clean)) {
                canonicalDisplayNames[clean.lowercase()] = clean
            }
        }

        for (track in onlineTracks) {
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

        artistAllTracks.entries
            .map { (key, allTrackList) ->
                val displayName = canonicalDisplayNames[key] ?: key
                val workerItem = workerArtistsMap[key]
                val resolvedCover = workerItem?.cover?.ifBlank { workerItem.picture }
                    ?.ifBlank { OnlineMusicRepository.getArtistPicture(displayName).orEmpty() }
                    ?: OnlineMusicRepository.getArtistPicture(displayName).orEmpty()

                val soloList = artistSoloTracks[key].orEmpty()
                val featList = artistFeatTracks[key].orEmpty()

                CloudArtistProfile(
                    name = displayName,
                    tracks = allTrackList,
                    soloTracks = soloList.ifEmpty { allTrackList },
                    featuredTracks = featList,
                    albums = albums.filter { it.artist.equals(displayName, ignoreCase = true) },
                    cover = resolvedCover,
                    picture = resolvedCover
                )
            }
            .sortedWith(
                compareByDescending<CloudArtistProfile> { it.soloTracks.size }
                    .thenByDescending { it.tracks.size }
            )
    }

    // پیش‌ذخیره تصاویر هنرمندان در کش خصوصی داخلی برنامه (بدون دیده‌شدن در گالری)
    LaunchedEffect(artists) {
        withContext(Dispatchers.IO) {
            artists.take(30).forEach { artist ->
                val imgUrl = artist.cover.ifBlank { artist.picture }
                if (imgUrl.isNotBlank()) {
                    ArtistAvatarDiskCache.loadOrDownloadArtistBitmap(
                        context = context,
                        artistName = artist.name,
                        remoteUrl = imgUrl
                    )
                }
            }
        }
    }

    // ۳. پیشنهاد سریع ۳ ردیفه افقی
    val quickPicks = remember(onlineTracks, quickPicksSeed) {
        if (onlineTracks.isEmpty()) emptyList()
        else onlineTracks.shuffled().take(15)
    }

    // ۴. ساخت دسته‌بندی‌های ۱۰۰٪ واقعی
    val browseCategories = remember(onlineTracks) {
        RealCategoryClassifier.buildRealCategories(onlineTracks)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF10141E), Color(0xFF07090E), Color(0xFF050608))
                )
            )
    ) {
        AnimatedContent(
            targetState = navState,
            label = "SpotifyHubNav"
        ) { state ->
            when (state) {
                is HubNavigationState.ArtistPage -> {
                    SpotifyFullArtistScreen(
                        artist = state.artist,
                        currentPlayingTrack = currentPlayingTrack,
                        isPersian = isPersian,
                        onBack = { navState = HubNavigationState.MainHub },
                        onAlbumClick = { navState = HubNavigationState.AlbumPage(it) },
                        onPlayTrack = onPlayTrack,
                        onDownloadTrack = onDownloadTrack
                    )
                }

                is HubNavigationState.AlbumPage -> {
                    SpotifyFullAlbumScreen(
                        album = state.album,
                        currentPlayingTrack = currentPlayingTrack,
                        isPersian = isPersian,
                        onBack = { navState = HubNavigationState.MainHub },
                        onPlayTrack = onPlayTrack,
                        onDownloadTrack = onDownloadTrack
                    )
                }

                is HubNavigationState.CategoryPage -> {
                    SpotifyCategoryPlaylistScreen(
                        category = state.category,
                        currentPlayingTrack = currentPlayingTrack,
                        isPersian = isPersian,
                        onBack = { navState = HubNavigationState.MainHub },
                        onPlayTrack = onPlayTrack,
                        onDownloadTrack = onDownloadTrack
                    )
                }

                HubNavigationState.MainHub -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = bottomPadding)
                    ) {
                        item {
                            SpotifyGlassTopBar(
                                totalTracks = onlineTracks.size,
                                totalArtists = artists.size,
                                searchQuery = searchQuery,
                                isPersian = isPersian,
                                onSearchChange = { searchQuery = it },
                                onRefresh = onRefresh
                            )
                        }

                        if (searchQuery.isNotBlank()) {
                            item {
                                CategorizedInstantSearchResults(
                                    query = searchQuery,
                                    artists = artists,
                                    albums = albums,
                                    allTracks = onlineTracks,
                                    currentPlayingTrack = currentPlayingTrack,
                                    isPersian = isPersian,
                                    onArtistClick = { navState = HubNavigationState.ArtistPage(it) },
                                    onAlbumClick = { navState = HubNavigationState.AlbumPage(it) },
                                    onPlayTrack = onPlayTrack,
                                    onDownloadTrack = onDownloadTrack
                                )
                            }
                        } else {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(TopHubTab.entries) { tab ->
                                        val isSelected = activeTab == tab
                                        Surface(
                                            onClick = { activeTab = tab },
                                            shape = RoundedCornerShape(50),
                                            color = if (isSelected) Color(0xFF1DB954) else Color.White.copy(alpha = 0.07f),
                                            border = if (isSelected) null else BorderStroke(
                                                1.dp,
                                                Color.White.copy(alpha = 0.12f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.Black else Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isPersian) tab.titleFa else tab.titleEn,
                                                    color = if (isSelected) Color.Black else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (isLoading && onlineTracks.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 90.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFF1DB954))
                                    }
                                }
                            } else {
                                when (activeTab) {
                                    TopHubTab.DISCOVER -> {
                                        item {
                                            AppleMusicHeroPager(
                                                onlineTracks = onlineTracks,
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
                                            item {
                                                HubSectionHeader(
                                                    title = if (isPersian) "هنرمندان برجسته" else "Featured Artists",
                                                    subtitle = if (isPersian) "پروفایل یکتا برای ${artists.size} هنرمند (همراه با فیت‌ها)" else "${artists.size} unified artist profiles (including features)",
                                                    actionLabel = if (isPersian) "مشاهده همه" else "See All",
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
                                                Spacer(modifier = Modifier.height(26.dp))
                                            }
                                        }

                                        if (quickPicks.isNotEmpty()) {
                                            item {
                                                HubSectionHeader(
                                                    title = if (isPersian) "پیشنهاد سریع" else "Quick Picks",
                                                    subtitle = if (isPersian) "پخش فوری از آرشیو ابری" else "Instant cloud stream picks",
                                                    actionLabel = if (isPersian) "پیشنهاد جدید" else "Refresh Picks",
                                                    onAction = { quickPicksSeed++ }
                                                )
                                                QuickPicksThreeRowGrid(
                                                    tracks = quickPicks,
                                                    currentPlayingTrack = currentPlayingTrack,
                                                    onPlayTrack = { track -> onPlayTrack(track, quickPicks) }
                                                )
                                                Spacer(modifier = Modifier.height(26.dp))
                                            }
                                        }

                                        if (albums.isNotEmpty()) {
                                            item {
                                                HubSectionHeader(
                                                    title = if (isPersian) "آلبوم‌ها و مجموعه‌ها" else "Albums & Collections",
                                                    subtitle = if (isPersian) "آهنگ‌های هر مجموعه در کنار هم" else "Grouped albums & artist collections",
                                                    actionLabel = if (isPersian) "همه آلبوم‌ها" else "See All",
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
                                                Spacer(modifier = Modifier.height(26.dp))
                                            }
                                        }

                                        if (browseCategories.isNotEmpty()) {
                                            item {
                                                HubSectionHeader(
                                                    title = if (isPersian) "مرور بر اساس سبک" else "Browse by Genre",
                                                    subtitle = if (isPersian) "دسته‌بندی واقعی بر اساس متادیتا و سبک آهنگ" else "Strictly matched by real genre & artist metadata"
                                                )
                                                SpotifyCategoriesGrid(
                                                    categories = browseCategories,
                                                    isPersian = isPersian,
                                                    onCategoryClick = { category ->
                                                        navState = HubNavigationState.CategoryPage(category)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    TopHubTab.CATEGORIES -> {
                                        item {
                                            HubSectionHeader(
                                                title = if (isPersian) "همه دسته‌بندی‌ها" else "All Genres & Categories",
                                                subtitle = if (isPersian) "تفکیک شده بر اساس ژانر واقعی" else "Verified genre classification"
                                            )
                                            SpotifyCategoriesGrid(
                                                categories = browseCategories,
                                                isPersian = isPersian,
                                                onCategoryClick = { category ->
                                                    navState = HubNavigationState.CategoryPage(category)
                                                }
                                            )
                                        }
                                    }

                                    TopHubTab.ARTISTS -> {
                                        item {
                                            ArtistsDirectoryGridSection(
                                                artists = artists,
                                                isPersian = isPersian,
                                                onArtistClick = { navState = HubNavigationState.ArtistPage(it) }
                                            )
                                        }
                                    }

                                    TopHubTab.ALBUMS -> {
                                        item {
                                            AlbumsDirectoryGridSection(
                                                albums = albums,
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
            }
        }
    }
}

/**
 * امضای سازگار با فراخوانی‌های قدیمی‌تر OnlineMusicScreen
 */
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
        val cached = OnlineMusicRepository.loadCachedWorkerCatalog()
        mutableStateOf(cached?.tracks?.map { it.toAppTrack() }.orEmpty())
    }
    var isLoading by remember { mutableStateOf(fetchedOnlineTracks.isEmpty()) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        if (fetchedOnlineTracks.isEmpty()) isLoading = true
        val resp = OnlineMusicRepository.fetchFromWorker()
        if (resp.tracks.isNotEmpty()) {
            fetchedOnlineTracks = resp.tracks.map { it.toAppTrack() }
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
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1DB954).copy(alpha = 0.25f),
                        Color(0xFF7B2CBF).copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Surface(
                    color = Color(0xFF1DB954).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "استریم ابری کلادفلر" else "CLOUD STREAM HUB",
                            color = Color(0xFF1DB954),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isPersian) "کاوش و جستجو" else "Explore & Search",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (isPersian)
                        "$totalTracks قطعه از $totalArtists هنرمند"
                    else
                        "$totalTracks tracks across $totalArtists artists",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = if (isPersian) "بروزرسانی" else "Refresh",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = if (isPersian)
                        "جستجوی خواننده، آهنگ یا آلبوم..."
                    else
                        "What do you want to listen to?",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF1DB954)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * بنر اسلایدی بالای صفحه با دکمه جمع‌وجور «پخش میکس» که هرگز روی متن‌ها نمی‌افتد
 */
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
                    if (isPersian) "سوپر میکس آرشیو ابری" else "Cloud Archive SuperMix",
                    if (isPersian) "پخش هوشمند از برترین آهنگ‌های سرور" else "Curated stream from your cloud library",
                    Pair(
                        listOf(Color(0xFF1DB954), Color(0xFF0A3D20), Color(0xFF07140D)),
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
                            cat.gradient + Color(0xFF090514),
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
                .height(162.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(colors))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable { onPlayMix(tracksToPlay) }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (isPersian) "میکس ویژه" else "FEATURED STATION",
                            color = Color.White,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { onPlayMix(tracksToPlay) },
                        color = Color.White,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPersian) "پخش میکس" else "Play Mix",
                                color = Color.Black,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
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
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(columns) { columnTracks ->
            Column(
                modifier = Modifier.width(290.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                columnTracks.forEach { track ->
                    val isPlaying = currentPlayingTrack?.id == track.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isPlaying) Color(0xFF1DB954).copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isPlaying) Color(0xFF1DB954) else Color.White.copy(alpha = 0.07f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onPlayTrack(track) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            if (!track.artworkUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = track.artworkUri,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                SpotifyArtistAvatar(
                                    artistName = SmartArtistParser.extractPrimaryArtist(track.artist, track.title),
                                    size = 52.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    showVerifiedBadge = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = if (isPlaying) Color(0xFF1DB954) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Equalizer else Icons.Default.PlayCircleFilled,
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * گرید دسته‌بندی‌های واقعی به سبک Spotify Browse
 */
@Composable
private fun SpotifyCategoriesGrid(
    categories: List<SpotifyBrowseCategory>,
    isPersian: Boolean,
    onCategoryClick: (SpotifyBrowseCategory) -> Unit
) {
    val rows = remember(categories) { categories.chunked(2) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { category ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(108.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(category.gradient))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onCategoryClick(category) }
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth(0.78f)
                        ) {
                            Text(
                                text = if (isPersian) category.titleFa else category.titleEn,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPersian) category.subtitleFa else category.subtitleEn,
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 8.dp, y = 8.dp)
                                .rotate(22f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
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
}

@Composable
private fun SpotifyArtistCard(
    artist: CloudArtistProfile,
    isPersian: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(116.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpotifyArtistAvatar(
            artistName = artist.name,
            size = 92.dp,
            shape = CircleShape,
            showVerifiedBadge = true,
            explicitArtistImageUrl = artist.cover.ifBlank { artist.picture }
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = artist.name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (isPersian) "${artist.tracks.size} قطعه" else "${artist.tracks.size} tracks",
            color = Color(0xFF1DB954),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
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
            .width(154.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(134.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            if (!album.coverUri.isNullOrBlank()) {
                AsyncImage(
                    model = album.coverUri,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                SpotifyArtistAvatar(
                    artistName = album.title,
                    size = 134.dp,
                    shape = RoundedCornerShape(14.dp),
                    showVerifiedBadge = false
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = if (isPersian) "${album.tracks.size} ترک" else "${album.tracks.size} tracks",
                    color = Color(0xFF1DB954),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = album.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategorizedInstantSearchResults(
    query: String,
    artists: List<CloudArtistProfile>,
    albums: List<CloudAlbumCollection>,
    allTracks: List<Track>,
    currentPlayingTrack: Track?,
    isPersian: Boolean,
    onArtistClick: (CloudArtistProfile) -> Unit,
    onAlbumClick: (CloudAlbumCollection) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onDownloadTrack: ((Track) -> Unit)? = null
) {
    val q = query.trim()
    val matchedArtists = remember(artists, q) {
        artists.filter { it.name.contains(q, ignoreCase = true) }.take(15)
    }
    val matchedAlbums = remember(albums, q) {
        albums.filter {
            it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
        }.take(15)
    }
    val matchedTracks = remember(allTracks, q) {
        allTracks.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }.take(35)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (matchedArtists.isNotEmpty()) {
            HubSectionHeader(
                title = if (isPersian) "هنرمندان (${matchedArtists.size})" else "Artists (${matchedArtists.size})"
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
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (matchedAlbums.isNotEmpty()) {
            HubSectionHeader(
                title = if (isPersian) "آلبوم‌ها (${matchedAlbums.size})" else "Albums (${matchedAlbums.size})"
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
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (matchedTracks.isNotEmpty()) {
            HubSectionHeader(
                title = if (isPersian) "آهنگ‌ها (${matchedTracks.size})" else "Songs (${matchedTracks.size})"
            )
            matchedTracks.forEach { track ->
                SpotifyTrackRowItem(
                    track = track,
                    isPlaying = currentPlayingTrack?.id == track.id,
                    onClick = { onPlayTrack(track, matchedTracks) },
                    onDownloadClick = onDownloadTrack?.let { { it(track) } }
                )
            }
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
                                Color(0xFF1DB954).copy(alpha = 0.35f),
                                Color(0xFF101824),
                                Color(0xFF07090E)
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
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SpotifyArtistAvatar(
                        artistName = artist.name,
                        size = 144.dp,
                        shape = CircleShape,
                        showVerifiedBadge = true,
                        explicitArtistImageUrl = artist.cover.ifBlank { artist.picture }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = artist.name,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isPersian) {
                            if (artist.featuredTracks.isNotEmpty())
                                "هنرمند تاییدشده • ${artist.tracks.size} قطعه (${artist.featuredTracks.size} فیت و همکاری)"
                            else
                                "هنرمند تاییدشده • ${artist.tracks.size} قطعه"
                        } else {
                            if (artist.featuredTracks.isNotEmpty())
                                "Verified Artist • ${artist.tracks.size} Tracks (${artist.featuredTracks.size} Features)"
                            else
                                "Verified Artist • ${artist.tracks.size} Tracks"
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                if (artist.tracks.isNotEmpty()) {
                                    onPlayTrack(artist.tracks.first(), artist.tracks)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPersian) "پخش همه" else "Play All",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (artist.tracks.isNotEmpty()) {
                                    val shuffled = artist.tracks.shuffled()
                                    onPlayTrack(shuffled.first(), shuffled)
                                }
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "پخش تصادفی" else "Shuffle")
                        }
                    }
                }
            }
        }

        item {
            HubSectionHeader(
                title = if (isPersian) "برترین آهنگ‌ها" else "Popular Releases",
                actionLabel = if (primaryList.size > 5) {
                    if (showAllSoloTracks) {
                        if (isPersian) "نمایش کمتر" else "Show Less"
                    } else {
                        if (isPersian) "مشاهده همه (${primaryList.size})" else "See All (${primaryList.size})"
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

        // نمایش بخش اختصاصی فیت‌ها و همکاری‌های این خواننده در داخل همان پروفایل
        if (artist.featuredTracks.isNotEmpty() && artist.soloTracks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(18.dp))
                HubSectionHeader(
                    title = if (isPersian) "همکاری‌ها و فیت‌ها (Feat)" else "Featuring & Collaborations",
                    subtitle = if (isPersian) "آهنگ‌های مشترک با سایر هنرمندان" else "Tracks featuring ${artist.name}",
                    actionLabel = if (artist.featuredTracks.size > 5) {
                        if (showAllFeatTracks) {
                            if (isPersian) "نمایش کمتر" else "Show Less"
                        } else {
                            if (isPersian) "مشاهده همه (${artist.featuredTracks.size})" else "See All (${artist.featuredTracks.size})"
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
                    title = if (isPersian) "آلبوم‌ها و مجموعه‌ها" else "Albums & Collections"
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 130.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF28344A), Color(0xFF07090E))
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
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    if (!album.coverUri.isNullOrBlank()) {
                        AsyncImage(
                            model = album.coverUri,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SpotifyArtistAvatar(
                            artistName = album.title,
                            size = 170.dp,
                            shape = RoundedCornerShape(18.dp),
                            showVerifiedBadge = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = album.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (isPersian)
                        "${album.artist} • ${album.tracks.size} قطعه"
                    else
                        "${album.artist} • ${album.tracks.size} Tracks",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        if (album.tracks.isNotEmpty()) {
                            onPlayTrack(album.tracks.first(), album.tracks)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPersian) "پخش آلبوم" else "Play Album",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        items(album.tracks, key = { "alb_trk_${it.id}" }) { track ->
            SpotifyTrackRowItem(
                track = track,
                isPlaying = currentPlayingTrack?.id == track.id,
                onClick = { onPlayTrack(track, album.tracks) },
                onDownloadClick = onDownloadTrack?.let { { it(track) } }
            )
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
                    .background(Brush.verticalGradient(category.gradient + Color(0xFF07090E)))
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Column {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isPersian) category.titleFa else category.titleEn,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "پخش تصادفی این سبک" else "Shuffle Category",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold
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
                            color = Color(0xFF1DB954)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistsDirectoryGridSection(
    artists: List<CloudArtistProfile>,
    isPersian: Boolean,
    onArtistClick: (CloudArtistProfile) -> Unit
) {
    val rows = remember(artists) { artists.chunked(3) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun AlbumsDirectoryGridSection(
    albums: List<CloudAlbumCollection>,
    isPersian: Boolean,
    onAlbumClick: (CloudAlbumCollection) -> Unit
) {
    val rows = remember(albums) { albums.chunked(2) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun SpotifyTrackRowItem(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onDownloadClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isPlaying) Color(0xFF1DB954).copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.04f)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            if (!track.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = track.artworkUri,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                SpotifyArtistAvatar(
                    artistName = SmartArtistParser.extractPrimaryArtist(track.artist, track.title),
                    size = 48.dp,
                    shape = RoundedCornerShape(10.dp),
                    showVerifiedBadge = false
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlaying) Color(0xFF1DB954) else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} • ${track.album}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
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
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Icon(
            imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (isPlaying) Color(0xFF1DB954) else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
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
                fontWeight = FontWeight.ExtraBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    color = Color(0xFF1DB954),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
