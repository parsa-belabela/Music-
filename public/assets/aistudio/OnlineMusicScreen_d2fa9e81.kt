package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CloudflareTrack(
    val id: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val coverUrl: String? = null,
    val durationMs: Long = 0L,
    val genre: String? = null
)

object CloudflareMusicApi {
    // The exact URL you provided
    var workerBaseUrl: String = "https://alubm.parsaghorbani8861.workers.dev"

    suspend fun searchOrFetchTracks(query: String = "", deviceId: String = ""): Result<List<CloudflareTrack>> = withContext(Dispatchers.IO) {
        try {
            val base = workerBaseUrl.trimEnd('/')
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            
            // Add deviceId to request if available to pass VIP checks
            val deviceParam = if (deviceId.isNotBlank()) "&deviceId=$deviceId" else ""

            val candidateUrls = if (query.isBlank()) {
                listOf("$base/tracks?$deviceParam", "$base/api/songs?$deviceParam", "$base?$deviceParam")
            } else {
                listOf(
                    "$base/tracks?q=$encodedQuery$deviceParam",
                    "$base/search?q=$encodedQuery$deviceParam",
                    "$base/api/songs?q=$encodedQuery$deviceParam"
                )
            }

            var lastError: Exception? = null

            for (endpoint in candidateUrls) {
                try {
                    val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 8000
                        readTimeout = 8000
                        setRequestProperty("Accept", "application/json")
                        if (deviceId.isNotBlank()) {
                            setRequestProperty("X-Device-Id", deviceId)
                        }
                    }

                    if (connection.responseCode in 200..299) {
                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                        val parsedTracks = parseCloudflareJson(responseText, base)
                        
                        val finalTracks = if (query.isBlank()) {
                            parsedTracks
                        } else {
                            parsedTracks.filter {
                                it.title.contains(query, ignoreCase = true) ||
                                        it.artist.contains(query, ignoreCase = true)
                            }.ifEmpty { parsedTracks }
                        }
                        return@withContext Result.success(finalTracks)
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }

            Result.failure(lastError ?: Exception("عدم ارتباط با سرور کلودفلر"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseCloudflareJson(jsonString: String, baseUrl: String): List<CloudflareTrack> {
        val trimmed = jsonString.trim()
        val jsonArray: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("tracks") 
                    ?: obj.optJSONArray("results")
                    ?: obj.optJSONArray("songs")
                    ?: obj.optJSONArray("data")
                    ?: obj.optJSONArray("items")
                    ?: JSONArray()
            }
            else -> JSONArray()
        }

        val list = mutableListOf<CloudflareTrack>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val id = item.optString("id").ifBlank {
                item.optString("file_id").ifBlank { i.toString() }
            }
            val title = item.optString("title")
                .ifBlank { item.optString("name") }
                .ifBlank { "آهنگ بدون نام" }
            val artist = item.optString("artist_name")
                .ifBlank { item.optString("artist") }
                .ifBlank { item.optString("performer") }
                .ifBlank { "ناشناس" }

            val rawStream = item.optString("stream_url")
                .ifBlank { item.optString("streamUrl") }
                .ifBlank { item.optString("audio_url") }
                .ifBlank { item.optString("url") }

            val streamUrl = when {
                rawStream.startsWith("http") -> rawStream
                rawStream.startsWith("/") -> "$baseUrl$rawStream"
                item.has("file_id") -> "$baseUrl/stream?id=${item.optString("file_id")}"
                else -> "$baseUrl/stream?id=$id"
            }

            val rawCover = item.optString("cover_url")
                .ifBlank { item.optString("coverUrl") }
                .ifBlank { item.optString("cover") }
                .ifBlank { item.optString("artwork") }

            val coverUrl = when {
                rawCover.startsWith("http") -> rawCover
                rawCover.startsWith("/") -> "$baseUrl$rawCover"
                rawCover.isNotBlank() -> rawCover
                else -> "$baseUrl/cover?id=$id&v=hd9"
            }

            list.add(
                CloudflareTrack(
                    id = id,
                    title = title,
                    artist = artist,
                    streamUrl = streamUrl,
                    coverUrl = coverUrl.takeIf { it.isNotBlank() },
                    durationMs = item.optLong("duration", 0L).let { if (it < 1000) it * 1000L else it },
                    genre = item.optString("genre").takeIf { it.isNotBlank() }
                )
            )
        }
        return list
    }
}
