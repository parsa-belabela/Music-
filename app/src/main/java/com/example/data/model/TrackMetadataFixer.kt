package com.example.data.model

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.RealAlbumRegistry
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class CleanedMetadata(
    val title: String,
    val artist: String,
    val album: String
)

/**
 * موتور هوشمند تشخیص، تفکیک و اصلاح متادیتای اشتباه آهنگ‌ها
 * مواردی نظیر:
 * ۱. اسم آلبوم به اشتباه در نام آهنگ و اسم آهنگ در نام خواننده قرار گرفته باشد
 * ۲. نام خواننده و نام آهنگ برعکس شده باشند
 * ۳. نام آهنگ به صورت "Artist - Title" در یک فیلد آمده باشد و خواننده آدرس کانال تلگرام باشد
 * ۴. پاکسازی نویزهای متنی کانال‌های تلگرام، بیت‌ریت‌ها و لینک‌ها
 */
object SmartTrackMetadataResolver {

    private val knownArtists = listOf(
        "kanye west", "kanye", "drake", "future", "metro boomin", "juice wrld", "kendrick lamar",
        "travis scott", "eminem", "post malone", "the weeknd", "lil baby", "gunna", "playboi carti",
        "21 savage", "don toliver", "lil uzi vert", "young thug", "j. cole", "asap rocky",
        "zedbazi", "mehrad hidden", "sohrab mj", "sijal", "alireza jj", "nassim", "behzad leito",
        "sepehr khalse", "vinak", "koorosh", "hiphopologist", "big shaggy", "amin tijay",
        "papaboyz", "isam", "gucciflame", "tohi", "amir tataloo", "poori", "reza pishro",
        "pishro", "hichkas", "yas", "shayea", "ho3ein", "bahram", "ali sorena", "sorena",
        "fadaei", "putak", "arta", "montiego", "sami low", "hoomaan", "chvrsi", "021kid",
        "shayan yo", "dalu", "hoodadk4", "hesam tiem", "dorcci", "ali geramy", "demi lovato", "quf"
    )

    private val albumToArtistMap = mapOf(
        "graduation" to "Kanye West",
        "808s & heartbreak" to "Kanye West",
        "the life of pablo" to "Kanye West",
        "my beautiful dark twisted fantasy" to "Kanye West",
        "yeezus" to "Kanye West",
        "donda" to "Kanye West",
        "vultures 1" to "Kanye West & Ty Dolla \$ign",
        "vultures" to "Kanye West & Ty Dolla \$ign",
        "legends never die" to "Juice WRLD",
        "goodbye & good riddance" to "Juice WRLD",
        "death race for love" to "Juice WRLD",
        "we don't trust you" to "Future & Metro Boomin",
        "we dont trust you" to "Future & Metro Boomin",
        "heroes & villains" to "Metro Boomin",
        "heroes and villains" to "Metro Boomin",
        "i never liked you" to "Future",
        "ds2" to "Future",
        "monster" to "Future",
        "56 nights" to "Future",
        "purple reign" to "Future",
        "high off life" to "Future",
        "the wizrd" to "Future",
        "pluto x baby pluto" to "Future & Lil Uzi Vert",
        "super slimey" to "Future & Young Thug",
        "mixtape pluto" to "Future",
        "evol" to "Future",
        "damn." to "Kendrick Lamar",
        "damn" to "Kendrick Lamar",
        "gnx" to "Kendrick Lamar",
        "good kid, m.a.a.d city" to "Kendrick Lamar",
        "to pimp a butterfly" to "Kendrick Lamar",
        "mr. morale & the big steppers" to "Kendrick Lamar",
        "certified lover boy" to "Drake",
        "honestly, nevermind" to "Drake",
        "what a time to be alive" to "Drake & Future",
        "more life" to "Drake",
        "views" to "Drake",
        "scorpion" to "Drake",
        "dark lane demo tapes" to "Drake",
        "if you're reading this it's too late" to "Drake",
        "her loss" to "Drake & 21 Savage",
        "for all the dogs" to "Drake",
        "the best in the world pack" to "Drake",
        "utopia" to "Travis Scott",
        "birds in the trap sing mcknight" to "Travis Scott",
        "jackboys" to "JACKBOYS & Travis Scott",
        "astroworld" to "Travis Scott",
        "rodeo" to "Travis Scott",
        "zakhar nameh" to "Zedbazi",
        "zedbazi classics" to "Zedbazi",
        "sefr" to "Mehrad Hidden",
        "toonel" to "Mehrad Hidden",
        "bozorg" to "Sohrab MJ & Mehrad Hidden",
        "bozorg (vol. 2)" to "Sohrab MJ & Mehrad Hidden",
        "pir shodim vali bozorg na" to "Alireza JJ, Sijal & Nassim",
        "yadgari jj" to "Alireza JJ",
        "therapy" to "Sepehr Khalse",
        "terapy" to "Sepehr Khalse",
        "yadegari" to "Sepehr Khalse",
        "enferadi" to "Sepehr Khalse",
        "pesare maryam" to "Sepehr Khalse",
        "fekr o khial" to "Sepehr Khalse",
        "23" to "Behzad Leito",
        "23 (bist o seh)" to "Behzad Leito",
        "setarebazi" to "Behzad Leito",
        "khabaraye bad" to "Behzad Leito",
        "khakestari" to "Behzad Leito",
        "mi vida loca" to "Vinak",
        "ghamar dar aghrab" to "Vinak",
        "gin & diss" to "Vinak",
        "420" to "Koorosh",
        "trap mode" to "Koorosh",
        "mosafer" to "Koorosh",
        "capsule" to "Hiphopologist",
        "rothman" to "Hiphopologist",
        "rapgame" to "Hiphopologist",
        "bozorg sal" to "Big Shaggy",
        "dead hand" to "Amin Tijay",
        "we are papa" to "PapaBoyz",
        "paris" to "Isam",
        "syrup sippin" to "GUCCIFLAME",
        "11:11" to "Tohi",
        "78" to "Amir Tataloo",
        "godfather" to "Poori",
        "the eminem show" to "Eminem",
        "recovery" to "Eminem",
        "relapse" to "Eminem",
        "kamikaze" to "Eminem",
        "music to be murdered by" to "Eminem",
        "hollywood's bleeding" to "Post Malone",
        "beerbongs & bentleys" to "Post Malone",
        "stoney" to "Post Malone"
    )

    fun cleanText(raw: String): String {
        var s = raw.trim()
        s = s.replace(Regex("(?i)@[a-z0-9_]+"), "")
        s = s.replace(Regex("(?i)https?://\\S+"), "")
        s = s.replace(Regex("(?i)t\\.me/\\S+"), "")
        s = s.replace(Regex("(?i)\\[(?:320|128|mp3|flac|official|music|hq)\\]"), "")
        s = s.replace(Regex("(?i)\\((?:320|128|mp3|flac|official video|official audio|hq)\\)"), "")
        s = s.replace(Regex("(?i)\\|\\s*official\\s*(?:track|audio|video)"), "")
        s = s.replace(Regex("(?i)www\\.[a-z0-9_-]+\\.[a-z]{2,}"), "")
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }

    fun resolve(rawTitle: String, rawArtist: String, rawAlbum: String?): CleanedMetadata {
        var title = cleanText(rawTitle)
        var artist = cleanText(rawArtist)
        var album = cleanText(rawAlbum.orEmpty())

        val titleLower = title.lowercase()
        val artistLower = artist.lowercase()

        // حالت ۱: تفکیک زمانی که عنوان آهنگ شامل خط فاصله است: "Artist - Title"
        if (title.contains(" - ") || title.contains(" – ") || title.contains(" — ")) {
            val delimiter = if (title.contains(" - ")) " - " else if (title.contains(" – ")) " – " else " — "
            val parts = title.split(delimiter, limit = 2)
            if (parts.size == 2) {
                val p0 = parts[0].trim()
                val p1 = parts[1].trim()
                val p0Lower = p0.lowercase()
                val p1Lower = p1.lowercase()

                if (knownArtists.any { p0Lower.contains(it) || it.contains(p0Lower) }) {
                    artist = p0
                    title = p1
                } else if (knownArtists.any { p1Lower.contains(it) || it.contains(p1Lower) }) {
                    artist = p1
                    title = p0
                } else if (artist.isBlank() || artist.equals("Single", ignoreCase = true) ||
                    artist.length < 3 || artist.startsWith("tg_") || artist.contains("audio", ignoreCase = true)
                ) {
                    artist = p0
                    title = p1
                }
            }
        }

        // حالت ۲: بررسی جابجایی هوشمند بین عنوان، خواننده و آلبوم
        val matchedAlbumForTitle = albumToArtistMap.keys.firstOrNull { it == titleLower || titleLower == it }
        val matchedAlbumForArtist = albumToArtistMap.keys.firstOrNull { it == artistLower || artistLower == it }
        val artistIsKnown = knownArtists.any { artistLower.contains(it) || it.contains(artistLower) }
        val titleIsKnown = knownArtists.any { titleLower.contains(it) || it.contains(titleLower) }

        if (matchedAlbumForTitle != null && !artistIsKnown) {
            // Title در واقع اسم آلبوم است و Artist در واقع نام آهنگ است!
            val actualTitle = artist
            val actualAlbumKey = matchedAlbumForTitle
            val recoveredArtist = albumToArtistMap[actualAlbumKey] ?: artist
            val resolvedAlbumName = RealAlbumRegistry.resolveAlbum(actualTitle, recoveredArtist, actualAlbumKey)

            title = actualTitle
            artist = recoveredArtist
            album = resolvedAlbumName
        } else if (matchedAlbumForArtist != null && !titleIsKnown) {
            // Artist در واقع اسم آلبوم است و Title نام آهنگ است!
            val actualTitle = title
            val actualAlbumKey = matchedAlbumForArtist
            val recoveredArtist = albumToArtistMap[actualAlbumKey] ?: "Unknown Artist"
            val resolvedAlbumName = RealAlbumRegistry.resolveAlbum(actualTitle, recoveredArtist, actualAlbumKey)

            title = actualTitle
            artist = recoveredArtist
            album = resolvedAlbumName
        } else if (titleIsKnown && !artistIsKnown) {
            // Title اسم خواننده است و Artist اسم آهنگ است! (مثلاً Title="Kanye West", Artist="Stronger")
            val temp = title
            title = artist
            artist = temp
        }

        // آلبوم را از رجیستری هوشمند اعتبارسنجی و تکمیل می‌کنیم
        val finalAlbum = RealAlbumRegistry.resolveAlbum(title = title, artist = artist, rawAlbum = album)

        return CleanedMetadata(
            title = title.ifBlank { rawTitle },
            artist = artist.ifBlank { rawArtist },
            album = finalAlbum
        )
    }
}

/**
 * ذخیره‌سازی محلی و دائم اصلاحات اعمال شده توسط کاربر روی قطعات موسیقی
 * ذخیره در SharedPreferences تا پس از بستن و باز کردن برنامه نیز باقی بماند
 */
object UserTrackMetadataStore {
    private const val PREFS_NAME = "aura_user_track_overrides"
    private val overrides = ConcurrentHashMap<String, CleanedMetadata>()
    private var isLoaded = false

    var version by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        if (isLoaded) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.all.forEach { (key, value) ->
                if (value is String) {
                    try {
                        val obj = JSONObject(value)
                        overrides[key] = CleanedMetadata(
                            title = obj.getString("title"),
                            artist = obj.getString("artist"),
                            album = obj.getString("album")
                        )
                    } catch (_: Exception) {}
                }
            }
            isLoaded = true
        } catch (_: Exception) {}
    }

    fun saveOverride(context: Context, trackId: String, title: String, artist: String, album: String) {
        val cleaned = CleanedMetadata(
            title = title.trim(),
            artist = artist.trim(),
            album = album.trim().ifBlank { "Single" }
        )
        val bareId = trackId.removePrefix("online_")
        overrides[trackId] = cleaned
        overrides[bareId] = cleaned
        overrides["online_$bareId"] = cleaned

        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = JSONObject().apply {
                put("title", cleaned.title)
                put("artist", cleaned.artist)
                put("album", cleaned.album)
            }.toString()
            prefs.edit()
                .putString(bareId, json)
                .putString("online_$bareId", json)
                .apply()
        } catch (_: Exception) {}

        version++
    }

    fun removeOverride(context: Context, trackId: String) {
        val bareId = trackId.removePrefix("online_")
        overrides.remove(trackId)
        overrides.remove(bareId)
        overrides.remove("online_$bareId")

        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(bareId).remove("online_$bareId").apply()
        } catch (_: Exception) {}

        version++
    }

    fun getOverride(trackId: String): CleanedMetadata? {
        val bareId = trackId.removePrefix("online_")
        return overrides[trackId] ?: overrides[bareId] ?: overrides["online_$bareId"]
    }

    fun hasOverride(trackId: String): Boolean {
        return getOverride(trackId) != null
    }

    fun applyOverride(track: Track): Track {
        val ov = getOverride(track.id) ?: return track
        return track.copy(
            title = ov.title.ifBlank { track.title },
            artist = ov.artist.ifBlank { track.artist },
            album = ov.album.ifBlank { track.album }
        )
    }
}
