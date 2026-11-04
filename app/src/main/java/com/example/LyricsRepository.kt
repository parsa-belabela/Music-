package com.example

import com.example.data.model.LyricsLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(
    val timeMs: Long,
    val text: String
)

object LyricsRepository {

    // دریافت متن کامل و هماهنگ آهنگ از LRCLIB
    suspend fun fetchLyrics(artist: String, trackName: String, durationSec: Int): List<LyricLine> = withContext(Dispatchers.IO) {
        val lyricsList = mutableListOf<LyricLine>()
        var connection: HttpURLConnection? = null
        try {
            val cleanArtist = artist.substringBefore(" feat").substringBefore(" ft.").substringBefore(",").trim().ifBlank { artist.trim() }
            val cleanTrack = trackName.substringBefore("(").substringBefore("-").trim().ifBlank { trackName.trim() }

            val encodedArtist = URLEncoder.encode(artist.trim(), "UTF-8")
            val encodedTrack = URLEncoder.encode(trackName.trim(), "UTF-8")
            val urlStr = "https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTrack&duration=$durationSec"

            connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "AuraMusicPlayer/2.0 (https://lrclib.net)")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                // اولویت با متن هماهنگ با زمان (Synced) است
                val syncedLyrics = json.optString("syncedLyrics", "")
                if (syncedLyrics.isNotBlank() && syncedLyrics != "null") {
                    val parsed = parseSyncedLyrics(syncedLyrics)
                    if (parsed.isNotEmpty()) return@withContext parsed
                }

                // اگر متن هماهنگ نبود، متن ساده (Plain) را خط به خط برمی‌گردانیم
                val plainLyrics = json.optString("plainLyrics", "")
                if (plainLyrics.isNotBlank() && plainLyrics != "null") {
                    plainLyrics.lines().forEach { line ->
                        if (line.isNotBlank()) {
                            lyricsList.add(LyricLine(0L, line.trim()))
                        }
                    }
                    if (lyricsList.isNotEmpty()) return@withContext lyricsList
                }
            }

            // فالبک خودکار: جستجو در LRCLIB با فرمت دقیق https://lrclib.net/api/search?q=${encodedArtist}+${encodedTrack}
            connection.disconnect()
            val searchUrls = listOf(
                "https://lrclib.net/api/search?q=${encodedArtist}+${encodedTrack}",
                "https://lrclib.net/api/search?q=${URLEncoder.encode(cleanArtist, "UTF-8")}+${URLEncoder.encode(cleanTrack, "UTF-8")}"
            ).distinct()

            for (searchUrl in searchUrls) {
                var searchConn: HttpURLConnection? = null
                try {
                    searchConn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("User-Agent", "AuraMusicPlayer/2.0 (https://lrclib.net)")
                    }

                    if (searchConn.responseCode == HttpURLConnection.HTTP_OK) {
                        val searchRes = searchConn.inputStream.bufferedReader().use { it.readText() }
                        val arr = JSONArray(searchRes)

                        // اولویت اول: اولین آیتمی که دارای syncedLyrics معتبر است
                        for (i in 0 until arr.length()) {
                            val itemObj = arr.optJSONObject(i) ?: continue
                            val synced = itemObj.optString("syncedLyrics", "")
                            if (synced.isNotBlank() && synced != "null") {
                                val parsed = parseSyncedLyrics(synced)
                                if (parsed.isNotEmpty()) return@withContext parsed
                            }
                        }

                        // اولویت دوم: اولین آیتمی که دارای plainLyrics معتبر است
                        for (i in 0 until arr.length()) {
                            val itemObj = arr.optJSONObject(i) ?: continue
                            val plain = itemObj.optString("plainLyrics", "")
                            if (plain.isNotBlank() && plain != "null") {
                                plain.lines().forEach { line ->
                                    if (line.isNotBlank()) {
                                        lyricsList.add(LyricLine(0L, line.trim()))
                                    }
                                }
                                if (lyricsList.isNotEmpty()) return@withContext lyricsList
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    searchConn?.disconnect()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        // اگر موردی پیدا نشد
        listOf(LyricLine(0L, "متنی برای این آهنگ یافت نشد."))
    }

    // تبدیل متن زمان‌دار استاندارد LRC به لیست قابل نمایش در اپ
    fun parseSyncedLyrics(synced: String): List<LyricLine> {
        val list = mutableListOf<LyricLine>()
        val pattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

        synced.lines().forEach { line ->
            val match = pattern.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val millis = match.groupValues[3].padEnd(3, '0').toLong()
                val text = match.groupValues[4].trim()

                val totalMs = (min * 60 * 1000) + (sec * 1000) + millis
                if (text.isNotEmpty()) {
                    list.add(LyricLine(totalMs, text))
                }
            }
        }
        return list
    }

    // تبدیل به LyricsLine استاندارد پلیر اصلی
    fun toPlayerLyricsLines(lines: List<LyricLine>, trackDurationMs: Long): List<LyricsLine> {
        if (lines.isEmpty()) return emptyList()
        if (lines.size == 1 && lines[0].text == "متنی برای این آهنگ یافت نشد.") {
            return listOf(
                LyricsLine(
                    timestampMs = 0L,
                    text = lines[0].text
                )
            )
        }
        val isPlainOnly = lines.all { it.timeMs == 0L }
        val safeTotalDuration = trackDurationMs.coerceAtLeast(lines.size * 3500L)
        return lines.mapIndexed { idx, item ->
            if (isPlainOnly) {
                val step = safeTotalDuration / lines.size.coerceAtLeast(1)
                LyricsLine(
                    timestampMs = idx * step,
                    text = item.text
                )
            } else {
                LyricsLine(
                    timestampMs = item.timeMs,
                    text = item.text
                )
            }
        }
    }

    fun toRawLrcString(lines: List<LyricLine>): String {
        return lines.joinToString("\n") { line ->
            val min = (line.timeMs / 60000L)
            val sec = (line.timeMs % 60000L) / 1000L
            val cs = (line.timeMs % 1000L) / 10L
            "[%02d:%02d.%02d]%s".format(min, sec, cs, line.text)
        }
    }
}
