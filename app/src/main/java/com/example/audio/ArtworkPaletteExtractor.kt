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
import com.example.ui.components.GenerativeArtworkTheme
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

data class AmbientPalette(
    val primary: Color = Color(0xFF7C4DFF),
    val secondary: Color = Color(0xFF00E5FF),
    val haloGlow: Color = Color(0xFF651FFF),
    val accent: Color = Color(0xFF00E5FF),
    val deepAtmosphere: Color = Color(0xFF07070E),
    val richBlack: Color = Color(0xFF07080E),
    val isLightLuminance: Boolean = false,
    val isMonochrome: Boolean = false
)

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

        val cacheKey = "${track.id}_${track.artworkUri ?: "no_art"}_${appSettings.autoColorFromArtwork}"
        paletteCache.get(cacheKey)?.let { return it }

        if (!appSettings.autoColorFromArtwork) {
            val defaultPal = getDefaultPalette(appSettings.theme)
            paletteCache.put(cacheKey, defaultPal)
            return defaultPal
        }

        val bitmap = loadThumbnailBitmap(context, track.artworkUri)
        val palette = if (bitmap != null) {
            extractTrueCoverPalette(bitmap)
        } else {
            val gen = GenerativeArtworkTheme.getThemeForTrack(track.title, track.artist, track.id)
            AmbientPalette(
                primary = gen.primary,
                secondary = gen.secondary,
                haloGlow = gen.primary.copy(alpha = 0.85f),
                accent = gen.tertiary,
                deepAtmosphere = gen.deep,
                richBlack = Color(0xFF06070F),
                isLightLuminance = false,
                isMonochrome = false
            )
        }

        paletteCache.put(cacheKey, palette)
        return palette
    }

    private fun loadThumbnailBitmap(context: Context?, artworkUri: String?): Bitmap? {
        if (context == null || artworkUri.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(artworkUri)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            when (uri.scheme) {
                "content" -> context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                "file" -> {
                    val file = File(uri.path ?: artworkUri)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath, options) else null
                }
                else -> {
                    val file = File(artworkUri)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath, options) else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * استخراج دقیق رنگ غالب کاور بر اساس مساحت واقعی رنگها در تصویر (بدون تحریف Hue)
     */
    private fun extractTrueCoverPalette(bitmap: Bitmap): AmbientPalette {
        return try {
            val size = 48
            val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
            val pixels = IntArray(size * size)
            scaled.getPixels(pixels, 0, size, 0, 0, size, size)

            val hsv = FloatArray(3)
            // تقسیم طیف رنگ به 24 بخش دقیق (هر 15 درجه) برای حفظ کامل رنگ واقعی کاور
            val bucketWeight = FloatArray(24)
            val bucketR = FloatArray(24)
            val bucketG = FloatArray(24)
            val bucketB = FloatArray(24)

            var totalValid = 0f
            var lowSatWeight = 0f
            var avgLum = 0f

            for (y in 0 until size) {
                for (x in 0 until size) {
                    val pixel = pixels[y * size + x]
                    val a = (pixel ushr 24) and 0xFF
                    if (a < 128) continue

                    val r = ((pixel shr 16) and 0xFF) / 255f
                    val g = ((pixel shr 8) and 0xFF) / 255f
                    val b = (pixel and 0xFF) / 255f

                    android.graphics.Color.colorToHSV(pixel, hsv)
                    val hue = hsv[0]
                    val sat = hsv[1]
                    val value = hsv[2]

                    // وزندهی بیشتر به مرکز و بدنه اصلی کاور نسبت به حاشیههای پرت
                    val dx = (x - size / 2f) / (size / 2f)
                    val dy = (y - size / 2f) / (size / 2f)
                    val centerWeight = (1.35f - (dx * dx + dy * dy) * 0.5f).coerceIn(0.6f, 1.35f)

                    totalValid += centerWeight
                    avgLum += value * centerWeight

                    if (sat < 0.14f || value < 0.10f) {
                        lowSatWeight += centerWeight
                        continue
                    }

                    val bin = ((hue / 15f).toInt()).coerceIn(0, 23)
                    // اولویت با مساحت واقعی رنگ روی کاور است تا رنگهای کوچک و بیربط انتخاب نشوند
                    val weight = centerWeight * (0.65f + sat * 0.35f) * (0.5f + value * 0.5f)
                    bucketWeight[bin] += weight
                    bucketR[bin] += r * weight
                    bucketG[bin] += g * weight
                    bucketB[bin] += b * weight
                }
            }

            // اگر بیش از ۷۸٪ کاور سیاهوسفید یا خاکستری بود، هاله نقرهای/سفید خالص تولید شود
            val isMono = totalValid > 0f && (lowSatWeight / totalValid) > 0.78f
            if (isMono) {
                return AmbientPalette(
                    primary = Color(0xFFE2E8F0),
                    secondary = Color(0xFF94A3B8),
                    haloGlow = Color(0xB3E2E8F0),
                    accent = Color(0xFFFFFFFF),
                    deepAtmosphere = Color(0xFF080A12),
                    richBlack = Color(0xFF040508),
                    isLightLuminance = (avgLum / totalValid.coerceAtLeast(1f)) > 0.65f,
                    isMonochrome = true
                )
            }

            val sortedBins = (0 until 24).sortedByDescending { bucketWeight[it] }
            val extractedColors = mutableListOf<Color>()

            for (bin in sortedBins) {
                val w = bucketWeight[bin]
                if (w <= 0.01f) continue
                val avgR = (bucketR[bin] / w).coerceIn(0f, 1f)
                val avgG = (bucketG[bin] / w).coerceIn(0f, 1f)
                val avgB = (bucketB[bin] / w).coerceIn(0f, 1f)

                val candidate = refineCoverColor(Color(avgR, avgG, avgB, 1f))
                if (extractedColors.isEmpty() || extractedColors.none { colorDistance(it, candidate) < 42.0 }) {
                    extractedColors.add(candidate)
                    if (extractedColors.size >= 3) break
                }
            }

            val primary = extractedColors.getOrNull(0) ?: Color(0xFF8B5CF6)
            val secondary = extractedColors.getOrNull(1) ?: shiftHarmonicColor(primary, 22f, 0.90f)
            val accent = extractedColors.getOrNull(2) ?: shiftHarmonicColor(primary, -18f, 1.08f)

            val deepHsv = FloatArray(3)
            android.graphics.Color.colorToHSV(primary.toArgb(), deepHsv)
            val deepAtmosphere = Color(
                android.graphics.Color.HSVToColor(
                    floatArrayOf(deepHsv[0], (deepHsv[1] * 0.75f).coerceIn(0.3f, 0.85f), 0.12f)
                )
            )

            val perceivedLum = 0.299f * primary.red + 0.587f * primary.green + 0.114f * primary.blue

            AmbientPalette(
                primary = primary,
                secondary = secondary,
                haloGlow = primary.copy(alpha = 0.88f),
                accent = accent,
                deepAtmosphere = deepAtmosphere,
                richBlack = Color(0xFF05060C),
                isLightLuminance = perceivedLum > 0.65f,
                isMonochrome = false
            )
        } catch (_: Exception) {
            getDefaultPalette(AppTheme.PURE_LIQUID_GLASS)
        }
    }

    /**
     * حفظ دقیق Hue اصلی کاور و فقط تنظیم روشنایی برای اینکه نور دور کاور قوی و زنده دیده شود
     */
    private fun refineCoverColor(raw: Color): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(raw.toArgb(), hsv)
        // Hue (hsv[0]) مطلقاً تغییر نمیکند تا رنگ کاور عوض نشود
        hsv[1] = (hsv[1] * 1.12f).coerceIn(0.35f, 0.96f)
        hsv[2] = hsv[2].coerceIn(0.68f, 0.98f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun shiftHarmonicColor(base: Color, hueShift: Float, valMultiplier: Float): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(base.toArgb(), hsv)
        hsv[0] = (hsv[0] + hueShift + 360f) % 360f
        hsv[1] = (hsv[1] * 1.05f).coerceIn(0.35f, 0.95f)
        hsv[2] = (hsv[2] * valMultiplier).coerceIn(0.65f, 1.0f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun colorDistance(c1: Color, c2: Color): Double {
        val r = (c1.red - c2.red) * 255.0
        val g = (c1.green - c2.green) * 255.0
        val b = (c1.blue - c2.blue) * 255.0
        return sqrt(r * r + g * g + b * b)
    }

    fun applyThemeGrade(raw: AmbientPalette, theme: AppTheme): AmbientPalette = raw

    fun getDefaultPalette(theme: AppTheme): AmbientPalette {
        return AmbientPalette(
            primary = Color(0xFF8B5CF6),
            secondary = Color(0xFF00E5FF),
            haloGlow = Color(0xCC8B5CF6),
            accent = Color(0xFF38BDF8),
            deepAtmosphere = Color(0xFF060812)
        )
    }
}
