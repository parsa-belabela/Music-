package com.example.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.collection.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.AppSettings
import com.example.data.model.AppTheme
import com.example.data.model.Track
import java.io.File
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.sqrt

data class AmbientPalette(
    val primary: Color,
    val secondary: Color,
    val haloGlow: Color,
    val accent: Color,
    val deepAtmosphere: Color = Color(0xFF07070E),
    val isLightLuminance: Boolean = false
)

/**
 * High-performance Artwork-Driven Color Engine.
 * Extracts the 3 authentic dominant colors directly from album artwork bitmap.
 * Falls back to harmonic, polished theme defaults when artwork is unavailable.
 * Cached in memory for 0ms subsequent queries.
 */
object ArtworkPaletteExtractor {

    private val paletteCache = LruCache<String, AmbientPalette>(64)

    fun extract(
        track: Track?,
        appSettings: AppSettings,
        context: Context? = null
    ): AmbientPalette {
        if (track == null) {
            return getDefaultPalette(appSettings.theme)
        }

        val cacheKey = "${track.id}_${track.artworkUri ?: "no_art"}_${appSettings.theme.name}"
        paletteCache.get(cacheKey)?.let { return it }

        if (!appSettings.autoColorFromArtwork) {
            val defaultPal = getDefaultPalette(appSettings.theme)
            paletteCache.put(cacheKey, defaultPal)
            return defaultPal
        }

        // Try extracting authentic colors from artwork bitmap
        val bitmap = loadThumbnailBitmap(context, track.artworkUri)
        val palette = if (bitmap != null) {
            extractDominantPaletteFromBitmap(bitmap, appSettings.theme)
        } else {
            getDefaultPalette(appSettings.theme)
        }

        paletteCache.put(cacheKey, palette)
        return palette
    }

    private fun loadThumbnailBitmap(context: Context?, artworkUri: String?): Bitmap? {
        if (context == null || artworkUri.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(artworkUri)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4 // Subsample for fast decoding
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                }
                "file" -> {
                    val path = uri.path ?: artworkUri
                    val file = File(path)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath, options)
                    } else null
                }
                else -> {
                    if (artworkUri.startsWith("/")) {
                        val file = File(artworkUri)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath, options)
                        } else null
                    } else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractDominantPaletteFromBitmap(bitmap: Bitmap, theme: AppTheme): AmbientPalette {
        return try {
            // Resize to 40x40 thumbnail for instantaneous color clustering
            val scaled = if (bitmap.width > 48 || bitmap.height > 48) {
                Bitmap.createScaledBitmap(bitmap, 40, 40, true)
            } else {
                bitmap
            }

            val width = scaled.width
            val height = scaled.height
            val pixels = IntArray(width * height)
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)

            val hsv = FloatArray(3)
            val colorBuckets = HashMap<Int, Int>() // Key -> pixel count
            val colorVibrancy = HashMap<Int, Float>() // Key -> total saturation * value

            for (pixel in pixels) {
                val alpha = (pixel ushr 24) and 0xff
                if (alpha < 128) continue

                android.graphics.Color.colorToHSV(pixel, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val value = hsv[2]

                // Discard extreme near-black or extreme washed-out white
                if (value < 0.12f || (sat < 0.08f && value > 0.90f)) continue

                // Quantize hue into 18 bins (20 deg each), sat into 3 bins, value into 3 bins
                val hueBin = (hue / 20f).toInt().coerceIn(0, 17)
                val satBin = (sat * 2.99f).toInt().coerceIn(0, 2)
                val valBin = (value * 2.99f).toInt().coerceIn(0, 2)
                val key = (hueBin shl 4) or (satBin shl 2) or valBin

                colorBuckets[key] = (colorBuckets[key] ?: 0) + 1
                colorVibrancy[key] = (colorVibrancy[key] ?: 0f) + (sat * value)
            }

            if (colorBuckets.isEmpty()) {
                return getDefaultPalette(theme)
            }

            // Sort keys by count multiplied by vibrancy
            val rankedKeys = colorBuckets.keys.sortedByDescending { key ->
                val count = colorBuckets[key] ?: 1
                val avgVibrancy = (colorVibrancy[key] ?: 0f) / count
                count * (0.35f + avgVibrancy * 0.65f)
            }

            val dominantColors = mutableListOf<Color>()
            for (key in rankedKeys) {
                val hueBin = (key shr 4) and 0x1F
                val satBin = (key shr 2) and 0x03
                val valBin = key and 0x03

                val hue = (hueBin * 20f + 10f).coerceIn(0f, 360f)
                val sat = ((satBin + 1) * 0.32f).coerceIn(0.40f, 0.95f)
                val value = ((valBin + 1) * 0.32f).coerceIn(0.50f, 0.95f)

                val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
                val color = Color(rgb)

                // Distinct color separation check
                if (dominantColors.isEmpty() || dominantColors.none { colorDistance(it, color) < 45 }) {
                    dominantColors.add(color)
                    if (dominantColors.size >= 3) break
                }
            }

            val primary = dominantColors.getOrNull(0) ?: Color(0xFF8B5CF6)
            val secondary = dominantColors.getOrNull(1) ?: Color(0xFF38BDF8)
            val accent = dominantColors.getOrNull(2) ?: Color(0xFFC084FC)

            // Deep background atmosphere derived cleanly from primary color hue
            val deepHsv = FloatArray(3)
            android.graphics.Color.colorToHSV(primary.toArgb(), deepHsv)
            val deepRgb = android.graphics.Color.HSVToColor(floatArrayOf(deepHsv[0], 0.65f, 0.08f))
            val deepAtmosphere = Color(deepRgb)

            AmbientPalette(
                primary = primary,
                secondary = secondary,
                haloGlow = primary.copy(alpha = 0.45f),
                accent = accent,
                deepAtmosphere = deepAtmosphere,
                isLightLuminance = false
            )
        } catch (_: Exception) {
            getDefaultPalette(theme)
        }
    }

    private fun colorDistance(c1: Color, c2: Color): Double {
        val r = (c1.red - c2.red) * 255
        val g = (c1.green - c2.green) * 255
        val b = (c1.blue - c2.blue) * 255
        return sqrt((r * r + g * g + b * b).toDouble())
    }

    fun getDefaultPalette(theme: AppTheme): AmbientPalette {
        return when (theme) {
            AppTheme.GLASS -> AmbientPalette(
                primary = Color(0xFF06B6D4),
                secondary = Color(0xFF3B82F6),
                haloGlow = Color(0x6606B6D4),
                accent = Color(0xFF67E8F9),
                deepAtmosphere = Color(0xFF050B16)
            )
            AppTheme.LEGO -> AmbientPalette(
                primary = Color(0xFFE51D24),
                secondary = Color(0xFFFFC700),
                haloGlow = Color(0x75E51D24),
                accent = Color(0xFF00E5FF),
                deepAtmosphere = Color(0xFF101115)
            )
            AppTheme.CARTOON -> AmbientPalette(
                primary = Color(0xFFFF4081),
                secondary = Color(0xFF00E5FF),
                haloGlow = Color(0x66FF4081),
                accent = Color(0xFFFFD600),
                deepAtmosphere = Color(0xFF140718)
            )
            AppTheme.CYBER_CHROME -> AmbientPalette(
                primary = Color(0xFF00F0FF),
                secondary = Color(0xFFFF0055),
                haloGlow = Color(0x7700F0FF),
                accent = Color(0xFF7000FF),
                deepAtmosphere = Color(0xFF060913)
            )
            AppTheme.VAPORWAVE -> AmbientPalette(
                primary = Color(0xFFFF71CE),
                secondary = Color(0xFF01CDFE),
                haloGlow = Color(0x66FF71CE),
                accent = Color(0xFF05FFA1),
                deepAtmosphere = Color(0xFF130826)
            )
            AppTheme.OBSIDIAN_MATRIX -> AmbientPalette(
                primary = Color(0xFF00FF66),
                secondary = Color(0xFF00CC44),
                haloGlow = Color(0x6600FF66),
                accent = Color(0xFF39FF14),
                deepAtmosphere = Color(0xFF020904)
            )
        }
    }
}
