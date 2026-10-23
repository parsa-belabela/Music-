package com.example.audio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.collection.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.AppSettings
import com.example.data.model.AppTheme
import com.example.data.model.Track
import com.example.ui.components.GenerativeArtworkTheme
import java.io.File
import kotlin.math.sqrt

data class AmbientPalette(
    val primary: Color = Color(0xFF7C4DFF),
    val secondary: Color = Color(0xFF00E5FF),
    val haloGlow: Color = Color(0xFF651FFF),
    val accent: Color = Color(0xFF00E676),
    val deepAtmosphere: Color = Color(0xFF07070E),
    val richBlack: Color = Color(0xFF07080E),
    val isLightLuminance: Boolean = false,
    val isMonochrome: Boolean = false
)

object ArtworkPaletteExtractor {

    private val paletteCache = LruCache<String, AmbientPalette>(80)

    fun extract(
        track: Track?,
        appSettings: AppSettings,
        context: Context? = null
    ): AmbientPalette {
        if (track == null) {
            return getDefaultPalette(appSettings.theme)
        }

        val cacheKey = "${track.id}_${track.artworkUri ?: "embedded"}_${track.uri.hashCode()}"
        paletteCache.get(cacheKey)?.let { return it }

        // بارگذاری ۳ مرحله‌ای کاور آهنگ (URI مستقیم -> Thumbnail سیستم -> کاور داخل فایل MP3)
        val bitmap = loadTrackBitmapRobust(context, track)
        val palette = if (bitmap != null) {
            extractDominantPaletteFromBitmap(bitmap)
        } else {
            val gen = GenerativeArtworkTheme.getThemeForTrack(track.title, track.artist, track.id)
            AmbientPalette(
                primary = gen.primary,
                secondary = gen.secondary,
                haloGlow = gen.primary.copy(alpha = 0.85f),
                accent = gen.tertiary,
                deepAtmosphere = gen.deep,
                richBlack = Color(0xFF060710),
                isLightLuminance = false,
                isMonochrome = false
            )
        }

        paletteCache.put(cacheKey, palette)
        return palette
    }

    private fun loadTrackBitmapRobust(context: Context?, track: Track): Bitmap? {
        if (context == null) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = 2
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        // مرحله ۱: تلاش برای خواندن از artworkUri
        val artworkUriStr = track.artworkUri
        if (!artworkUriStr.isNullOrBlank()) {
            try {
                val uri = Uri.parse(artworkUriStr)
                when (uri.scheme) {
                    "content" -> {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, options)?.let {
                                return ensureSoftwareBitmap(it)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            runCatching {
                                context.contentResolver.loadThumbnail(uri, Size(128, 128), null)
                            }.getOrNull()?.let {
                                return ensureSoftwareBitmap(it)
                            }
                        }
                    }
                    "file" -> {
                        val file = File(uri.path ?: artworkUriStr)
                        if (file.exists()) {
                            BitmapFactory.decodeFile(file.absolutePath, options)?.let {
                                return ensureSoftwareBitmap(it)
                            }
                        }
                    }
                    else -> {
                        if (artworkUriStr.startsWith("/")) {
                            val file = File(artworkUriStr)
                            if (file.exists()) {
                                BitmapFactory.decodeFile(file.absolutePath, options)?.let {
                                    return ensureSoftwareBitmap(it)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // مرحله ۲: استخراج مستقیم کاورِ جاسازی‌شده (Embedded ID3 Artwork) از داخل فایل صوتی
        if (track.uri.isNotBlank() && !track.isDemo) {
            val retriever = MediaMetadataRetriever()
            try {
                val trackUri = Uri.parse(track.uri)
                if (track.uri.startsWith("content://")) {
                    retriever.setDataSource(context, trackUri)
                } else {
                    retriever.setDataSource(track.uri)
                }
                val embeddedBytes = retriever.embeddedPicture
                if (embeddedBytes != null && embeddedBytes.isNotEmpty()) {
                    BitmapFactory.decodeByteArray(embeddedBytes, 0, embeddedBytes.size, options)?.let {
                        return ensureSoftwareBitmap(it)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        return null
    }

    private fun ensureSoftwareBitmap(bmp: Bitmap): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bmp.config == Bitmap.Config.HARDWARE) {
            bmp.copy(Bitmap.Config.ARGB_8888, false) ?: bmp
        } else if (bmp.config != Bitmap.Config.ARGB_8888) {
            bmp.copy(Bitmap.Config.ARGB_8888, false) ?: bmp
        } else {
            bmp
        }
    }

    private fun extractDominantPaletteFromBitmap(rawBitmap: Bitmap): AmbientPalette {
        return try {
            val softwareBmp = ensureSoftwareBitmap(rawBitmap)
            val scaled = Bitmap.createScaledBitmap(softwareBmp, 48, 48, true)

            val width = scaled.width
            val height = scaled.height
            val pixels = IntArray(width * height)
            scaled.getPixels(pixels, 0, width, 0, 0, width, height)

            val hsv = FloatArray(3)
            val colorBuckets = HashMap<Int, Int>()
            val colorVibrancy = HashMap<Int, Float>()
            val bucketBestColor = HashMap<Int, Color>()
            val bucketBestScore = HashMap<Int, Float>()

            var colorfulPixels = 0
            var validPixels = 0

            for (pixel in pixels) {
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 128) continue

                android.graphics.Color.colorToHSV(pixel, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val value = hsv[2]

                if (value < 0.12f) continue
                validPixels++
                if (sat >= 0.18f) colorfulPixels++

                // نادیده گرفتن سیاهی مطلق و سفیدی مطلقِ حاشیه کاورها در صورت وجود رنگ
                if (sat < 0.12f && value > 0.90f) continue

                val hueBin = (hue / 15f).toInt().coerceIn(0, 23)
                val satBin = (sat * 3.99f).toInt().coerceIn(0, 3)
                val valBin = (value * 3.99f).toInt().coerceIn(0, 3)
                val key = (hueBin shl 4) or (satBin shl 2) or valBin

                colorBuckets[key] = (colorBuckets[key] ?: 0) + 1
                // وزن‌دهی بالا به رنگ‌های زنده و اشباع‌شده‌ی کاور
                val vibrancyWeight = (sat * 1.6f + 0.2f) * (value * 1.2f)
                colorVibrancy[key] = (colorVibrancy[key] ?: 0f) + vibrancyWeight

                if (vibrancyWeight > (bucketBestScore[key] ?: -1f)) {
                    bucketBestScore[key] = vibrancyWeight
                    val boostedHsv = floatArrayOf(
                        hue,
                        (sat * 1.25f + 0.15f).coerceIn(0.60f, 1.0f),
                        (value * 1.20f + 0.15f).coerceIn(0.82f, 1.0f)
                    )
                    bucketBestColor[key] = Color(android.graphics.Color.HSVToColor(boostedHsv))
                }
            }

            // اگر کاور واقعاً سیاه‌وسفید (Monochrome) بود
            val isMonochrome = validPixels > 0 && (colorfulPixels.toFloat() / validPixels.toFloat() < 0.12f)
            if (isMonochrome || colorBuckets.isEmpty()) {
                return AmbientPalette(
                    primary = Color(0xFFE2E8F0),
                    secondary = Color(0xFF94A3B8),
                    haloGlow = Color(0xCCCBD5E1),
                    accent = Color(0xFFFFFFFF),
                    deepAtmosphere = Color(0xFF0A0D16),
                    richBlack = Color(0xFF040509),
                    isLightLuminance = false,
                    isMonochrome = true
                )
            }

            val rankedKeys = colorBuckets.keys.sortedByDescending { key ->
                val count = colorBuckets[key] ?: 1
                val vib = colorVibrancy[key] ?: 0f
                count * 0.45f + vib * 2.2f
            }

            val pickedColors = mutableListOf<Color>()
            for (key in rankedKeys) {
                val candidate = bucketBestColor[key] ?: continue
                if (pickedColors.isEmpty() || pickedColors.none { colorDistance(it, candidate) < 48.0 }) {
                    pickedColors.add(candidate)
                    if (pickedColors.size >= 3) break
                }
            }

            val rawPrimary = pickedColors.getOrNull(0) ?: Color(0xFF8B5CF6)
            val rawSecondary = pickedColors.getOrNull(1) ?: shiftHue(rawPrimary, 38f)
            val rawAccent = pickedColors.getOrNull(2) ?: shiftHue(rawPrimary, -42f)

            val primary = enhanceCinematicVibrance(rawPrimary, boostSat = 1.30f, boostVal = 1.22f)
            val secondary = enhanceCinematicVibrance(rawSecondary, boostSat = 1.25f, boostVal = 1.20f)
            val accent = enhanceCinematicVibrance(rawAccent, boostSat = 1.35f, boostVal = 1.28f)

            val deepHsv = FloatArray(3)
            android.graphics.Color.colorToHSV(primary.toArgb(), deepHsv)
            val deepAtmosphere = Color(
                android.graphics.Color.HSVToColor(floatArrayOf(deepHsv[0], 0.80f, 0.12f))
            )

            val luminance = 0.299f * primary.red + 0.587f * primary.green + 0.114f * primary.blue

            AmbientPalette(
                primary = primary,
                secondary = secondary,
                haloGlow = primary.copy(alpha = 0.88f),
                accent = accent,
                deepAtmosphere = deepAtmosphere,
                richBlack = Color(0xFF05060C),
                isLightLuminance = luminance > 0.65f,
                isMonochrome = false
            )
        } catch (_: Exception) {
            getDefaultPalette(AppTheme.PURE_LIQUID_GLASS)
        }
    }

    private fun shiftHue(color: Color, deltaDegrees: Float): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[0] = (hsv[0] + deltaDegrees + 360f) % 360f
        hsv[1] = hsv[1].coerceIn(0.65f, 1f)
        hsv[2] = hsv[2].coerceIn(0.85f, 1f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun enhanceCinematicVibrance(color: Color, boostSat: Float, boostVal: Float): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = (hsv[1] * boostSat).coerceIn(0.55f, 1.0f)
        hsv[2] = (hsv[2] * boostVal).coerceIn(0.78f, 1.0f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun colorDistance(c1: Color, c2: Color): Double {
        val r = (c1.red - c2.red) * 255
        val g = (c1.green - c2.green) * 255
        val b = (c1.blue - c2.blue) * 255
        return sqrt((r * r + g * g + b * b).toDouble())
    }

    fun getDefaultPalette(theme: AppTheme = AppTheme.PURE_LIQUID_GLASS): AmbientPalette {
        return AmbientPalette(
            primary = Color(0xFF8B5CF6),
            secondary = Color(0xFF00E5FF),
            haloGlow = Color(0xCC8B5CF6),
            accent = Color(0xFF38BDF8),
            deepAtmosphere = Color(0xFF070914),
            richBlack = Color(0xFF04050A)
        )
    }
}
