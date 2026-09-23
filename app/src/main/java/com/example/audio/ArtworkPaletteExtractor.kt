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
    val richBlack: Color = Color(0xFF07080E),
    val isLightLuminance: Boolean = false,
    val isMonochrome: Boolean = false
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

            // First pass: analyze monochrome / grayscale characteristics
            var lowSatPixelCount = 0
            var validPixelCount = 0
            var sumSat = 0f
            var sumLum = 0f

            for (pixel in pixels) {
                val alpha = (pixel ushr 24) and 0xff
                if (alpha < 128) continue

                android.graphics.Color.colorToHSV(pixel, hsv)
                val sat = hsv[1]
                val value = hsv[2]
                validPixelCount++
                sumSat += sat
                sumLum += value
                if (sat < 0.16f) {
                    lowSatPixelCount++
                }
            }

            // If >= 70% of pixels have saturation < 0.16 or mean saturation is < 0.13, this is a Black & White / Grayscale album cover!
            val isMonochrome = validPixelCount > 0 && (
                (lowSatPixelCount.toFloat() / validPixelCount >= 0.70f) ||
                (sumSat / validPixelCount < 0.13f)
            )

            if (isMonochrome) {
                // High-Contrast Rich Obsidian Black & Diamond White Palette
                val avgLum = if (validPixelCount > 0) sumLum / validPixelCount else 0.5f
                val primary = if (avgLum > 0.6f) Color(0xFFFFFFFF) else Color(0xFFF8FAFC)
                val secondary = Color(0xFF090B12) // Rich Velvet Obsidian Black
                val accent = Color(0xFF282C3D) // Sleek Metallic Gunmetal
                val deepAtmosphere = Color(0xFF020204) // Deepest rich velvet pitch-black atmosphere
                val richBlack = Color(0xFF000000) // Absolute Pure Black

                return AmbientPalette(
                    primary = primary,
                    secondary = secondary,
                    haloGlow = Color(0x65FFFFFF),
                    accent = accent,
                    deepAtmosphere = deepAtmosphere,
                    richBlack = richBlack,
                    isLightLuminance = false,
                    isMonochrome = true
                )
            }

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

            val rawPrimary = dominantColors.getOrNull(0) ?: Color(0xFFFF007F)
            val rawSecondary = dominantColors.getOrNull(1) ?: Color(0xFF00E5FF)
            val rawAccent = dominantColors.getOrNull(2) ?: Color(0xFFFF9100)

            // Dynamic high-energy cinematic color grading: maximize saturation and luminosity for vibrant neon glow
            val primary = enhanceCinematicVibrance(rawPrimary, boostSat = 1.40f, boostVal = 1.30f)
            val secondary = enhanceCinematicVibrance(rawSecondary, boostSat = 1.35f, boostVal = 1.25f)
            val accent = enhanceCinematicVibrance(rawAccent, boostSat = 1.45f, boostVal = 1.35f)

            // Compute perceived luminance (Rec. 709 / W3C formula)
            val perceivedLuminance = (0.299f * primary.red + 0.587f * primary.green + 0.114f * primary.blue)
            val isLightLuminance = perceivedLuminance > 0.62f

            // Deep background atmosphere derived cleanly and richly from primary color hue
            val deepHsv = FloatArray(3)
            android.graphics.Color.colorToHSV(primary.toArgb(), deepHsv)
            val deepRgb = android.graphics.Color.HSVToColor(floatArrayOf(deepHsv[0], 0.85f, 0.09f))
            val deepAtmosphere = Color(deepRgb)

            val rawPalette = AmbientPalette(
                primary = primary,
                secondary = secondary,
                haloGlow = primary.copy(alpha = 0.80f),
                accent = accent,
                deepAtmosphere = deepAtmosphere,
                isLightLuminance = isLightLuminance
            )
            applyThemeGrade(rawPalette, theme)
        } catch (_: Exception) {
            getDefaultPalette(theme)
        }
    }

    fun applyThemeGrade(raw: AmbientPalette, theme: AppTheme): AmbientPalette {
        return when (theme) {
            AppTheme.MONOCHROME_NOIR -> AmbientPalette(
                primary = Color(0xFFF8FAFC), // Pure Diamond White
                secondary = Color(0xFF090B12), // Rich Velvet Obsidian Black
                haloGlow = Color(0x65FFFFFF),
                accent = Color(0xFF282C3D), // Sleek Graphite / Gunmetal
                deepAtmosphere = Color(0xFF020204), // Deepest Void Black
                richBlack = Color(0xFF000000),
                isLightLuminance = false,
                isMonochrome = true
            )
            AppTheme.CYBER_NIGHTS -> AmbientPalette(
                primary = blendColors(raw.primary, Color(0xFF00F0FF), 0.45f), // Cyan Cyberpunk
                secondary = Color(0xFFFF007F), // Laser Rose Pink
                haloGlow = Color(0x9900F0FF),
                accent = Color(0xFF9D4EDD), // Neon Ultraviolet
                deepAtmosphere = Color(0xFF020716),
                richBlack = Color(0xFF03050B)
            )
            AppTheme.VELVET_NOIR -> AmbientPalette(
                primary = blendColors(raw.primary, Color(0xFFFFD700), 0.35f), // Imperial Gold
                secondary = Color(0xFF9333EA), // Royal Velvet Purple
                haloGlow = Color(0x99FFD700),
                accent = Color(0xFFBE185D), // Radiant Burgundy
                deepAtmosphere = Color(0xFF0B020E),
                richBlack = Color(0xFF08020A)
            )
            AppTheme.SUNSET_RAVE -> AmbientPalette(
                primary = blendColors(raw.primary, Color(0xFFFF5E00), 0.40f), // Solar Orange
                secondary = Color(0xFFFF1361), // Hot Synth Coral Pink
                haloGlow = Color(0x99FF5E00),
                accent = Color(0xFFFFD200), // Radiant Sunbeam Gold
                deepAtmosphere = Color(0xFF130314),
                richBlack = Color(0xFF09010A)
            )
            AppTheme.DIGITAL_ACID -> AmbientPalette(
                primary = Color(0xFF39FF14), // Radioactive Acid Lime
                secondary = Color(0xFF00FF66), // Toxic Matrix Green
                haloGlow = Color(0x9939FF14),
                accent = Color(0xFF00F5D4), // Phosphor Cyan
                deepAtmosphere = Color(0xFF000500),
                richBlack = Color(0xFF000200)
            )
            AppTheme.Y2K_CHROME -> AmbientPalette(
                primary = blendColors(raw.primary, Color(0xFFE2E8F0), 0.50f), // Liquid Mercury Silver
                secondary = Color(0xFF38BDF8), // Electric Ice Blue
                haloGlow = Color(0x8038BDF8),
                accent = Color(0xFFCBD5E1), // Platinum Chrome
                deepAtmosphere = Color(0xFF080B10),
                richBlack = Color(0xFF040608)
            )
            AppTheme.PURE_LIQUID_GLASS -> AmbientPalette(
                primary = raw.primary,
                secondary = blendColors(raw.secondary, Color(0xFF8B5CF6), 0.30f),
                haloGlow = raw.primary.copy(alpha = 0.65f),
                accent = Color(0xFF00E5FF),
                deepAtmosphere = Color(0xFF060812),
                richBlack = Color(0xFF04050A)
            )
        }
    }

    private fun blendColors(c1: Color, c2: Color, ratio: Float): Color {
        val r = (c1.red * (1f - ratio) + c2.red * ratio).coerceIn(0f, 1f)
        val g = (c1.green * (1f - ratio) + c2.green * ratio).coerceIn(0f, 1f)
        val b = (c1.blue * (1f - ratio) + c2.blue * ratio).coerceIn(0f, 1f)
        return Color(r, g, b, 1f)
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
            AppTheme.MONOCHROME_NOIR -> AmbientPalette(
                primary = Color(0xFFFFFFFF), // Crisp Diamond White
                secondary = Color(0xFF0E1019), // Rich Onyx / Obsidian Black
                haloGlow = Color(0x99FFFFFF),
                accent = Color(0xFF1C1F2E), // Rich Charcoal Jet Black
                deepAtmosphere = Color(0xFF030306), // Obsidian Pitch Black
                richBlack = Color(0xFF07080E),
                isLightLuminance = false,
                isMonochrome = true
            )
        }
    }
}
