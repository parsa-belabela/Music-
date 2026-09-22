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
            val bucketBestColor = HashMap<Int, Color>()
            val bucketBestScore = HashMap<Int, Float>()

            for (pixel in pixels) {
                val alpha = (pixel ushr 24) and 0xff
                if (alpha < 128) continue

                android.graphics.Color.colorToHSV(pixel, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val value = hsv[2]

                // Discard extreme near-black or extreme washed-out white
                if (value < 0.15f || (sat < 0.10f && value > 0.88f)) continue

                // Quantize hue into 18 bins (20 deg each), sat into 3 bins, value into 3 bins
                val hueBin = (hue / 20f).toInt().coerceIn(0, 17)
                val satBin = (sat * 2.99f).toInt().coerceIn(0, 2)
                val valBin = (value * 2.99f).toInt().coerceIn(0, 2)
                val key = (hueBin shl 4) or (satBin shl 2) or valBin

                colorBuckets[key] = (colorBuckets[key] ?: 0) + 1
                val pixelVibrancy = (sat * sat) * value
                colorVibrancy[key] = (colorVibrancy[key] ?: 0f) + pixelVibrancy

                if (pixelVibrancy > (bucketBestScore[key] ?: -1f)) {
                    bucketBestScore[key] = pixelVibrancy
                    val boostedHsv = floatArrayOf(
                        hue,
                        (sat * 1.30f + 0.18f).coerceIn(0.65f, 1.0f),
                        (value * 1.25f + 0.15f).coerceIn(0.85f, 1.0f)
                    )
                    bucketBestColor[key] = Color(android.graphics.Color.HSVToColor(boostedHsv))
                }
            }

            if (colorBuckets.isEmpty()) {
                return getDefaultPalette(theme)
            }

            // Heavily reward saturation and vibrancy so lights pop
            val rankedKeys = colorBuckets.keys.sortedByDescending { key ->
                val count = colorBuckets[key] ?: 1
                val totalVibrancy = colorVibrancy[key] ?: 0f
                count * 0.3f + totalVibrancy * 2.5f
            }

            val dominantColors = mutableListOf<Color>()
            for (key in rankedKeys) {
                val candidateColor = bucketBestColor[key] ?: run {
                    val hueBin = (key shr 4) and 0x1F
                    val hue = (hueBin * 20f + 10f).coerceIn(0f, 360f)
                    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.85f, 0.95f)))
                }

                // Distinct color separation check
                if (dominantColors.isEmpty() || dominantColors.none { colorDistance(it, candidateColor) < 55 }) {
                    dominantColors.add(candidateColor)
                    if (dominantColors.size >= 3) break
                }
            }

            val rawPrimary = dominantColors.getOrNull(0) ?: Color(0xFF00E5FF)
            val rawSecondary = dominantColors.getOrNull(1) ?: Color(0xFF8B5CF6)
            val rawAccent = dominantColors.getOrNull(2) ?: Color(0xFFFF007F)

            // Cinematic color grading: boost saturation and vibrance like a movie poster
            val primary = enhanceCinematicVibrance(rawPrimary, boostSat = 1.25f, boostVal = 1.15f)
            val secondary = enhanceCinematicVibrance(rawSecondary, boostSat = 1.20f, boostVal = 1.10f)
            val accent = enhanceCinematicVibrance(rawAccent, boostSat = 1.30f, boostVal = 1.20f)

            // Compute perceived luminance (Rec. 709 / W3C formula)
            val perceivedLuminance = (0.299f * primary.red + 0.587f * primary.green + 0.114f * primary.blue)
            val isLightLuminance = perceivedLuminance > 0.62f

            // Deep background atmosphere derived cleanly from primary color hue
            val deepHsv = FloatArray(3)
            android.graphics.Color.colorToHSV(primary.toArgb(), deepHsv)
            val deepRgb = android.graphics.Color.HSVToColor(floatArrayOf(deepHsv[0], 0.70f, 0.08f))
            val deepAtmosphere = Color(deepRgb)

            AmbientPalette(
                primary = primary,
                secondary = secondary,
                haloGlow = primary.copy(alpha = 0.65f),
                accent = accent,
                deepAtmosphere = deepAtmosphere,
                isLightLuminance = isLightLuminance
            )
        } catch (_: Exception) {
            getDefaultPalette(theme)
        }
    }

    private fun enhanceCinematicVibrance(color: Color, boostSat: Float, boostVal: Float): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = (hsv[1] * boostSat).coerceIn(0.45f, 1.0f)
        hsv[2] = (hsv[2] * boostVal).coerceIn(0.70f, 1.0f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun colorDistance(c1: Color, c2: Color): Double {
        val r = (c1.red - c2.red) * 255
        val g = (c1.green - c2.green) * 255
        val b = (c1.blue - c2.blue) * 255
        return sqrt((r * r + g * g + b * b).toDouble())
    }

    fun getDefaultPalette(theme: AppTheme): AmbientPalette {
        return when (theme) {
            AppTheme.PURE_LIQUID_GLASS -> AmbientPalette(
                primary = Color(0xFF00E5FF), // Pure Crystal Cyan
                secondary = Color(0xFF8B5CF6), // Prismatic Violet
                haloGlow = Color(0x9900E5FF),
                accent = Color(0xFFFFFFFF), // Pure Ice White
                deepAtmosphere = Color(0xFF060810) // Pure ultra-deep midnight for maximum glass transparency
            )
            AppTheme.CYBER_NIGHTS -> AmbientPalette(
                primary = Color(0xFF00F0FF), // Electric Laser Cyan
                secondary = Color(0xFF1E3A8A), // Midnight Navy
                haloGlow = Color(0x9900F0FF),
                accent = Color(0xFFFF007F), // Laser Rose Magenta
                deepAtmosphere = Color(0xFF030712)
            )
            AppTheme.Y2K_CHROME -> AmbientPalette(
                primary = Color(0xFFE2E8F0), // Liquid Mercury Silver
                secondary = Color(0xFF38BDF8), // Electric Ice Blue
                haloGlow = Color(0x9938BDF8),
                accent = Color(0xFFFFFFFF), // Platinum White
                deepAtmosphere = Color(0xFF080B10)
            )
            AppTheme.VELVET_NOIR -> AmbientPalette(
                primary = Color(0xFFFFD700), // Imperial 24K Gold
                secondary = Color(0xFF581C87), // Deep Royal Velvet
                haloGlow = Color(0x99FFD700),
                accent = Color(0xFFF59E0B), // Champagne Gold
                deepAtmosphere = Color(0xFF0B020E)
            )
            AppTheme.SUNSET_RAVE -> AmbientPalette(
                primary = Color(0xFFFF6D00), // Solar Orange
                secondary = Color(0xFFFF007F), // Radiant Neon Pink
                haloGlow = Color(0x99FF6D00),
                accent = Color(0xFFFFD600), // Golden Sunset Ray
                deepAtmosphere = Color(0xFF130314)
            )
            AppTheme.DIGITAL_ACID -> AmbientPalette(
                primary = Color(0xFF39FF14), // Radioactive Acid Lime
                secondary = Color(0xFF059669), // Matrix Toxic Green
                haloGlow = Color(0x9939FF14),
                accent = Color(0xFF00FF66), // Laser Phosphor
                deepAtmosphere = Color(0xFF000000)
            )
        }
    }
}
