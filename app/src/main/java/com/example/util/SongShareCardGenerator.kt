package com.example.util

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import androidx.core.content.FileProvider
import com.example.AuraApplication
import com.example.OnlineMusicRepository
import com.example.audio.AmbientPalette
import com.example.buildAuraCoverImageRequest
import com.example.data.model.AppLanguage
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class StoryBackgroundStyle(val index: Int, val labelEn: String, val labelFa: String) {
    DYNAMIC_AMBIENT_BLUR(0, "Dynamic Ambient Blur", "بلور پویا از کاور"),
    MIDNIGHT_NEON_AURA(1, "Midnight Neon Aura", "نئون نیمه‌شب"),
    CRIMSON_AFTER_HOURS(2, "Crimson After-Hours", "سرخ نیمه‌شب");

    companion object {
        fun fromIndex(index: Int): StoryBackgroundStyle {
            val mod = ((index % entries.size) + entries.size) % entries.size
            return entries.firstOrNull { it.index == mod } ?: DYNAMIC_AMBIENT_BLUR
        }
    }
}

object SongShareCardGenerator {

    suspend fun loadSoftwareCoverBitmap(context: Context, track: Track): Bitmap? = withContext(Dispatchers.IO) {
        val bigCoverUrl = track.coverBig?.takeIf { it.isNotBlank() }
            ?: OnlineMusicRepository.getBigCoverUrlForTrack(
                trackId = track.id,
                fallbackUrl = track.artworkUri,
                artistName = track.artist
            )

        if (bigCoverUrl.isBlank()) return@withContext null

        try {
            val request = buildAuraCoverImageRequest(
                context = context,
                trackId = track.id,
                coverUrl = bigCoverUrl,
                isLarge = true,
                allowHardware = false
            )
            val result = AuraApplication.instance.globalImageLoader.execute(request)
            val drawable = result.drawable
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                val bmp = drawable.bitmap
                if (bmp.config == Bitmap.Config.HARDWARE) {
                    return@withContext bmp.copy(Bitmap.Config.ARGB_8888, false)
                }
                return@withContext bmp
            }
        } catch (e: Exception) {
            Log.w("SongShareCard", "Coil software bitmap load fallback: ${e.message}")
        }

        try {
            val fallback = OnlineMusicRepository.getCachedCoverBitmap(bigCoverUrl)
                ?: OnlineMusicRepository.loadCoverBitmap(bigCoverUrl, maxDim = 1000)
            if (fallback != null) {
                return@withContext if (fallback.config == Bitmap.Config.HARDWARE) {
                    fallback.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    fallback
                }
            }
        } catch (_: Exception) {}

        try {
            val uri = Uri.parse(bigCoverUrl)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                return@withContext BitmapFactory.decodeStream(input, null, opts)
            }
        } catch (_: Exception) {}

        null
    }

    fun saveStoryBitmapToCacheUri(
        context: Context,
        bitmap: Bitmap,
        trackId: String
    ): Uri? {
        return try {
            val softBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
            val finalBitmap = if (softBitmap.width != 1080 || softBitmap.height != 1920) {
                Bitmap.createScaledBitmap(softBitmap, 1080, 1920, true)
            } else {
                softBitmap
            }

            val storiesDir = File(context.cacheDir, "shared_stories")
            if (!storiesDir.exists()) {
                storiesDir.mkdirs()
            }
            // Clean up old story files older than 1 hour to keep cache tidy
            storiesDir.listFiles()?.forEach { old ->
                if (System.currentTimeMillis() - old.lastModified() > 3600_000L) {
                    try { old.delete() } catch (_: Exception) {}
                }
            }

            val safeId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
            val file = File(storiesDir, "aura_story_${safeId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                fos.flush()
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("SongShareCard", "Failed saving story JPEG: ${e.message}", e)
            null
        }
    }

    suspend fun generateSongCardBitmap(
        context: Context,
        track: Track,
        palette: AmbientPalette,
        waveformEnvelope: FloatArray? = null,
        lyricSnippet: String? = null,
        selectedLyricLines: List<String> = emptyList(),
        lang: AppLanguage = AppLanguage.ENGLISH,
        styleIndex: Int = 0,
        preloadedCover: Bitmap? = null
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val loadedArtBmp = preloadedCover?.let {
                if (it.config == Bitmap.Config.HARDWARE) it.copy(Bitmap.Config.ARGB_8888, false) else it
            } ?: loadSoftwareCoverBitmap(context, track)

            val style = StoryBackgroundStyle.fromIndex(styleIndex)
            drawStoryBackground(
                canvas = canvas,
                width = width,
                height = height,
                style = style,
                coverBitmap = loadedArtBmp,
                palette = palette
            )

            drawCenterpieceLiquidGlassCard(
                canvas = canvas,
                width = width,
                height = height,
                track = track,
                coverBitmap = loadedArtBmp,
                palette = palette
            )

            drawLuxuryAuraBrandingPill(
                canvas = canvas,
                width = width,
                height = height
            )

            saveStoryBitmapToCacheUri(context, bitmap, track.id)
        } catch (e: Exception) {
            Log.e("SongShareCard", "Error generating 9:16 story card: ${e.message}", e)
            null
        }
    }

    private fun drawStoryBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        style: StoryBackgroundStyle,
        coverBitmap: Bitmap?,
        palette: AmbientPalette
    ) {
        val w = width.toFloat()
        val h = height.toFloat()

        when (style) {
            StoryBackgroundStyle.DYNAMIC_AMBIENT_BLUR -> {
                canvas.drawColor(Color.parseColor("#090C15"))
                if (coverBitmap != null) {
                    val blurred = createHeavyBlurredBitmap(coverBitmap, targetW = 180, targetH = 320, radius = 28)
                    val dstRect = RectF(0f, 0f, w, h)
                    val srcRatio = blurred.width.toFloat() / blurred.height.toFloat().coerceAtLeast(1f)
                    val dstRatio = w / h
                    val srcRect = if (srcRatio > dstRatio) {
                        val cropW = (blurred.height * dstRatio).toInt()
                        val xOff = (blurred.width - cropW) / 2
                        Rect(xOff, 0, xOff + cropW, blurred.height)
                    } else {
                        val cropH = (blurred.width / dstRatio).toInt()
                        val yOff = (blurred.height - cropH) / 2
                        Rect(0, yOff, blurred.width, yOff + cropH)
                    }
                    val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    canvas.drawBitmap(blurred, srcRect, dstRect, bmpPaint)
                } else {
                    val pColor = toAndroidColor(palette.primary, 0.55f)
                    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            w * 0.5f,
                            h * 0.42f,
                            w * 0.95f,
                            intArrayOf(pColor, Color.parseColor("#090C15")),
                            floatArrayOf(0f, 1f),
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(0f, 0f, w, h, glowPaint)
                }

                // Dark radial vignette overlay Color(0xFF090C15).copy(alpha = 0.62f) -> deeper at edges
                val vignetteCenter = Color.argb((0.52f * 255).toInt(), 9, 12, 21)
                val vignetteMid = Color.argb((0.64f * 255).toInt(), 9, 12, 21)
                val vignetteEdge = Color.argb((0.84f * 255).toInt(), 9, 12, 21)
                val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.5f,
                        h * 0.48f,
                        h * 0.68f,
                        intArrayOf(vignetteCenter, vignetteMid, vignetteEdge),
                        floatArrayOf(0f, 0.62f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, vignettePaint)
            }

            StoryBackgroundStyle.MIDNIGHT_NEON_AURA -> {
                // Deep obsidian #090C15 background
                canvas.drawColor(Color.parseColor("#090C15"))

                // Top-left #7C3AED purple at 35% alpha
                val purpleGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.18f,
                        h * 0.20f,
                        w * 0.92f,
                        intArrayOf(
                            Color.argb((0.35f * 255).toInt(), 0x7C, 0x3A, 0xED),
                            Color.argb((0.12f * 255).toInt(), 0x7C, 0x3A, 0xED),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, purpleGlow)

                // Bottom-right #2563EB blue at 30% alpha
                val blueGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.85f,
                        h * 0.80f,
                        w * 0.92f,
                        intArrayOf(
                            Color.argb((0.30f * 255).toInt(), 0x25, 0x63, 0xEB),
                            Color.argb((0.10f * 255).toInt(), 0x25, 0x63, 0xEB),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, blueGlow)
            }

            StoryBackgroundStyle.CRIMSON_AFTER_HOURS -> {
                // Deep #090508 background
                canvas.drawColor(Color.parseColor("#090508"))

                // Moody crimson/ruby radial glow (#991B1B at 40% alpha)
                val crimsonGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        w * 0.5f,
                        h * 0.44f,
                        w * 0.98f,
                        intArrayOf(
                            Color.argb((0.40f * 255).toInt(), 0x99, 0x1B, 0x1B),
                            Color.argb((0.16f * 255).toInt(), 0x99, 0x1B, 0x1B),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.58f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, w, h, crimsonGlow)
            }
        }
    }

    private fun drawCenterpieceLiquidGlassCard(
        canvas: Canvas,
        width: Int,
        height: Int,
        track: Track,
        coverBitmap: Bitmap?,
        palette: AmbientPalette
    ) {
        val w = width.toFloat()
        val h = height.toFloat()

        // 82% of canvas width
        val cardWidth = w * 0.82f
        val innerPad = 66f // 22.dp at 3x density (1080px width = 360dp * 3)
        val coverSize = cardWidth - innerPad * 2f

        // Vertical spacing inside card:
        // top padding (66) + coverSize + 54 (18.dp) + title (66) + 14 + artist (46) + 48 (16.dp) + waveform+timestamps (112) + bottom padding (66)
        val cardHeight = innerPad + coverSize + 54f + 66f + 14f + 46f + 48f + 112f + innerPad
        val cardLeft = (w - cardWidth) / 2f
        val cardTop = (h - cardHeight) / 2f - 28f
        val cardRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)
        val cardCorner = 84f // 28.dp * 3

        // 24.dp soft drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(165, 0, 0, 0)
            maskFilter = BlurMaskFilter(72f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(
            RectF(cardRect.left + 6f, cardRect.top + 28f, cardRect.right - 6f, cardRect.bottom + 32f),
            cardCorner,
            cardCorner,
            shadowPaint
        )

        // Glass Card Background: verticalGradient(#1A2035 at 0.78f -> #0D111D at 0.88f)
        val glassBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardRect.left,
                cardRect.top,
                cardRect.left,
                cardRect.bottom,
                intArrayOf(
                    Color.argb((0.78f * 255).toInt(), 0x1A, 0x20, 0x35),
                    Color.argb((0.88f * 255).toInt(), 0x0D, 0x11, 0x1D)
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, cardCorner, cardCorner, glassBgPaint)

        // 1.2.dp Specular Gradient Border (White 0.28f -> White 0.04f)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.6f
            shader = LinearGradient(
                cardRect.left,
                cardRect.top,
                cardRect.right,
                cardRect.bottom,
                intArrayOf(
                    Color.argb((0.28f * 255).toInt(), 255, 255, 255),
                    Color.argb((0.04f * 255).toInt(), 255, 255, 255)
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, cardCorner, cardCorner, borderPaint)

        // High-Res Cover Art (1:1 aspect ratio, RoundedCornerShape(20.dp) -> 60f, 16.dp drop shadow)
        val coverRect = RectF(
            cardRect.left + innerPad,
            cardRect.top + innerPad,
            cardRect.left + innerPad + coverSize,
            cardRect.top + innerPad + coverSize
        )
        val coverCorner = 60f // 20.dp * 3

        val coverShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 0, 0, 0)
            maskFilter = BlurMaskFilter(48f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(
            RectF(coverRect.left + 6f, coverRect.top + 20f, coverRect.right - 6f, coverRect.bottom + 22f),
            coverCorner,
            coverCorner,
            coverShadowPaint
        )

        drawRoundedBitmapOrPlaceholder(
            canvas = canvas,
            bmp = coverBitmap,
            rect = coverRect,
            cornerRadius = coverCorner,
            fallbackColor = toAndroidColor(palette.primary, 1f)
        )

        // Subtle inner highlight on Cover Art
        val coverHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            shader = LinearGradient(
                coverRect.left,
                coverRect.top,
                coverRect.right,
                coverRect.bottom,
                intArrayOf(
                    Color.argb(56, 255, 255, 255),
                    Color.argb(8, 255, 255, 255)
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(coverRect, coverCorner, coverCorner, coverHighlightPaint)

        // Track Metadata (18.dp = 54px below cover)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 60f // 20.sp * 3
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.72f * 255).toInt(), 255, 255, 255)
            textSize = 42f // 14.sp * 3
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val cleanTitle = TextUtils.ellipsize(
            track.title,
            titlePaint,
            coverSize,
            TextUtils.TruncateAt.END
        ).toString()

        val cleanArtist = TextUtils.ellipsize(
            track.artist,
            artistPaint,
            coverSize,
            TextUtils.TruncateAt.END
        ).toString()

        val titleBaselineY = coverRect.bottom + 54f + 50f
        canvas.drawText(cleanTitle, coverRect.left, titleBaselineY, titlePaint)

        val artistBaselineY = titleBaselineY + 56f
        canvas.drawText(cleanArtist, coverRect.left, artistBaselineY, artistPaint)

        // Aesthetic Audio Waveform / Scrubber (16.dp = 48px below artist)
        val waveTop = artistBaselineY + 48f
        val waveHeight = 56f
        val waveCenterY = waveTop + waveHeight / 2f
        val totalBars = 28
        val progressRatio = 0.35f
        val activeBars = (totalBars * progressRatio).toInt()
        val gap = 10f
        val barWidth = ((coverSize - gap * (totalBars - 1)) / totalBars).coerceAtLeast(6f)
        val seed = track.title.hashCode()

        val activeWavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.85f * 255).toInt(), 255, 255, 255)
        }
        val inactiveWavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.25f * 255).toInt(), 255, 255, 255)
        }

        var dotCenterX = coverRect.left + coverSize * progressRatio
        for (i in 0 until totalBars) {
            val waveNorm = (sin(i * 0.62f + seed) * 0.36f + 0.56f).coerceIn(0.22f, 1f)
            val bh = (waveHeight * waveNorm).coerceAtLeast(12f)
            val bx = coverRect.left + i * (barWidth + gap)
            val by = waveCenterY - bh / 2f
            if (i == activeBars) {
                dotCenterX = bx + barWidth / 2f
            }
            canvas.drawRoundRect(
                RectF(bx, by, bx + barWidth, by + bh),
                barWidth / 2f,
                barWidth / 2f,
                if (i <= activeBars) activeWavePaint else inactiveWavePaint
            )
        }

        // Tiny glowing dot at 35% progress
        val dotGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(130, 255, 255, 255)
            maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(dotCenterX, waveCenterY, 13f, dotGlowPaint)
        val dotCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawCircle(dotCenterX, waveCenterY, 7.5f, dotCorePaint)

        // Timestamps 1:14 / 3:28 below waveform
        val timePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.56f * 255).toInt(), 255, 255, 255)
            textSize = 31f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val timeY = waveTop + waveHeight + 40f
        val leftTimestamp = "1:14"
        val rightTimestamp = formatStoryDuration(track.durationMs)
        canvas.drawText(leftTimestamp, coverRect.left, timeY, timePaint)
        val rightW = timePaint.measureText(rightTimestamp)
        canvas.drawText(rightTimestamp, coverRect.right - rightW, timeY, timePaint)
    }

    private fun drawLuxuryAuraBrandingPill(
        canvas: Canvas,
        width: Int,
        height: Int
    ) {
        val w = width.toFloat()
        val h = height.toFloat()

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 33f // 11.sp * 3
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.22f
        }
        val label = "AURA MUSIC"
        val textWidth = textPaint.measureText(label)
        val iconWidth = 36f
        val iconGap = 20f
        val padH = 48f // 16.dp * 3
        val padV = 24f // 8.dp * 3

        val pillWidth = padH * 2f + iconWidth + iconGap + textWidth
        val pillHeight = 42f + padV * 2f
        val pillLeft = (w - pillWidth) / 2f
        val pillBottom = h - 120f
        val pillTop = pillBottom - pillHeight
        val pillRect = RectF(pillLeft, pillTop, pillLeft + pillWidth, pillBottom)
        val pillRadius = pillHeight / 2f

        // Background: Color.White.copy(alpha = 0.08f)
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((0.08f * 255).toInt(), 255, 255, 255)
        }
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillBgPaint)

        // Border: 0.5.dp Color.White.copy(alpha = 0.18f)
        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.argb((0.18f * 255).toInt(), 255, 255, 255)
        }
        canvas.drawRoundRect(pillRect, pillRadius, pillRadius, pillBorderPaint)

        // Tiny glowing musical equalizer bars icon
        val eqStartX = pillRect.left + padH
        val eqCenterY = pillRect.centerY()
        val barHeights = floatArrayOf(16f, 30f, 22f, 34f, 18f)
        val eqBarW = 4.5f
        val eqBarGap = 3.2f
        val eqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        for (i in barHeights.indices) {
            val bh = barHeights[i]
            val bx = eqStartX + i * (eqBarW + eqBarGap)
            canvas.drawRoundRect(
                RectF(bx, eqCenterY - bh / 2f, bx + eqBarW, eqCenterY + bh / 2f),
                eqBarW / 2f,
                eqBarW / 2f,
                eqPaint
            )
        }

        val fm = textPaint.fontMetrics
        val textBaseline = pillRect.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(label, eqStartX + iconWidth + iconGap, textBaseline, textPaint)
    }

    private fun formatStoryDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000L).toInt()
        if (totalSeconds <= 80) return "3:28"
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun toAndroidColor(composeColor: androidx.compose.ui.graphics.Color, alphaMultiplier: Float): Int {
        val a = ((composeColor.alpha * alphaMultiplier) * 255f).toInt().coerceIn(0, 255)
        val r = (composeColor.red * 255f).toInt().coerceIn(0, 255)
        val g = (composeColor.green * 255f).toInt().coerceIn(0, 255)
        val b = (composeColor.blue * 255f).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }

    private fun drawRoundedBitmapOrPlaceholder(
        canvas: Canvas,
        bmp: Bitmap?,
        rect: RectF,
        cornerRadius: Float,
        fallbackColor: Int
    ) {
        if (bmp != null) {
            val path = Path().apply {
                addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(path)
            val srcRatio = bmp.width.toFloat() / bmp.height.toFloat().coerceAtLeast(1f)
            val dstRatio = rect.width() / rect.height().coerceAtLeast(1f)
            val srcRect = if (srcRatio > dstRatio) {
                val newW = (bmp.height * dstRatio).toInt()
                val xOff = (bmp.width - newW) / 2
                Rect(xOff, 0, xOff + newW, bmp.height)
            } else {
                val newH = (bmp.width / dstRatio).toInt()
                val yOff = (bmp.height - newH) / 2
                Rect(0, yOff, bmp.width, yOff + newH)
            }
            canvas.drawBitmap(bmp, srcRect, rect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
        } else {
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fallbackColor
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fallbackPaint)
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = rect.width() * 0.30f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("♪", rect.centerX(), rect.centerY() + rect.width() * 0.1f, notePaint)
        }
    }

    /**
     * Produces a heavily blurred software Bitmap for Style 1 (Dynamic Ambient Blur)
     * that works across all Android versions and software Canvas renders without RenderScript/Hardware issues.
     */
    fun createHeavyBlurredBitmap(source: Bitmap, targetW: Int = 180, targetH: Int = 320, radius: Int = 26): Bitmap {
        val safeSrc = if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }
        val scaled = Bitmap.createScaledBitmap(safeSrc, targetW, targetH, true)
        val bitmap = scaled.copy(Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in dv.indices) {
            dv[idx] = idx / divsum
        }

        yi = 0
        yw = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + min(wm, max(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
                y++
            }
            x++
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
