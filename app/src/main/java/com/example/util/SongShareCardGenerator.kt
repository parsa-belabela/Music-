package com.example.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import com.example.OnlineMusicRepository
import com.example.audio.AmbientPalette
import com.example.data.model.AppLanguage
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

object SongShareCardGenerator {

    suspend fun generateSongCardBitmap(
        context: Context,
        track: Track,
        palette: AmbientPalette,
        waveformEnvelope: FloatArray? = null,
        lyricSnippet: String? = null,
        selectedLyricLines: List<String> = emptyList(),
        lang: AppLanguage = AppLanguage.ENGLISH
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val pColor = palette.primary
            val sColor = palette.secondary

            val pInt = Color.rgb(
                (pColor.red * 255).toInt().coerceIn(0, 255),
                (pColor.green * 255).toInt().coerceIn(0, 255),
                (pColor.blue * 255).toInt().coerceIn(0, 255)
            )
            val sInt = Color.rgb(
                (sColor.red * 255).toInt().coerceIn(0, 255),
                (sColor.green * 255).toInt().coerceIn(0, 255),
                (sColor.blue * 255).toInt().coerceIn(0, 255)
            )
            val darkBase = Color.rgb(12, 12, 14)

            // Resolve artwork from Memory/Disk/Cloudflare or local ContentResolver
            var loadedArtBmp: Bitmap? = null
            val artUrl = track.artworkUri?.takeIf { it.isNotBlank() }
                ?: OnlineMusicRepository.getCoverUrlForTrack(track.id, track.artist)

            if (artUrl.isNotBlank()) {
                loadedArtBmp = OnlineMusicRepository.getCachedCoverBitmap(artUrl)
                    ?: OnlineMusicRepository.loadCoverBitmap(artUrl)
                if (loadedArtBmp == null) {
                    try {
                        val uri = Uri.parse(artUrl)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            loadedArtBmp = BitmapFactory.decodeStream(input)
                        }
                    } catch (_: Exception) {}
                }
            }

            // 1. Story Outer Canvas Gradient (Spotify Story Backdrop)
            val outerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        blendColors(pInt, Color.BLACK, 0.45f),
                        blendColors(sInt, Color.BLACK, 0.72f),
                        darkBase
                    ),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), outerBgPaint)

            val hasLyricLines = selectedLyricLines.isNotEmpty()

            if (hasLyricLines) {
                // =========================================================================
                // MODE A: AUTHENTIC SPOTIFY LYRICS SHARE CARD
                // =========================================================================
                val cardMarginH = 84f
                val cardTop = 260f
                val cardBottom = 1660f
                val cardRect = RectF(cardMarginH, cardTop, width - cardMarginH, cardBottom)

                // Drop Shadow behind Lyrics Card
                val cardShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(140, 0, 0, 0)
                    maskFilter = BlurMaskFilter(56f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawRoundRect(
                    RectF(cardRect.left + 8f, cardRect.top + 24f, cardRect.right - 8f, cardRect.bottom + 24f),
                    52f,
                    52f,
                    cardShadowPaint
                )

                // Inner Spotify Lyrics Card Fill
                val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        cardRect.left, cardRect.top, cardRect.left, cardRect.bottom,
                        intArrayOf(pInt, blendColors(pInt, sInt, 0.65f)),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(cardRect, 52f, 52f, cardFillPaint)

                val innerPad = 68f
                val headerTop = cardRect.top + innerPad
                val thumbSize = 132f
                val thumbRect = RectF(
                    cardRect.left + innerPad,
                    headerTop,
                    cardRect.left + innerPad + thumbSize,
                    headerTop + thumbSize
                )

                // Draw Album Cover Thumbnail in Card Header
                drawRoundedBitmapOrPlaceholder(
                    canvas = canvas,
                    bmp = loadedArtBmp,
                    rect = thumbRect,
                    cornerRadius = 20f,
                    fallbackColor = sInt
                )

                // Draw Song Title & Artist next to Thumbnail
                val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 44f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(210, 255, 255, 255)
                    textSize = 34f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }

                val textStartX = thumbRect.right + 32f
                val maxHeaderTextW = (cardRect.right - innerPad - textStartX).toInt().coerceAtLeast(100)
                val ellipsizedTitle = android.text.TextUtils.ellipsize(
                    track.title,
                    titlePaint,
                    maxHeaderTextW.toFloat(),
                    android.text.TextUtils.TruncateAt.END
                ).toString()
                val ellipsizedArtist = android.text.TextUtils.ellipsize(
                    track.artist,
                    artistPaint,
                    maxHeaderTextW.toFloat(),
                    android.text.TextUtils.TruncateAt.END
                ).toString()

                canvas.drawText(ellipsizedTitle, textStartX, headerTop + 56f, titlePaint)
                canvas.drawText(ellipsizedArtist, textStartX, headerTop + 106f, artistPaint)

                // Draw Lyric Lines in the Center of the Card
                val linesToRender = selectedLyricLines.take(4).joinToString("\n\n")
                val lyricFontSize = when (selectedLyricLines.size) {
                    1 -> 64f
                    2 -> 56f
                    3 -> 48f
                    else -> 42f
                }

                val isRtlText = linesToRender.any { it in '\u0600'..'\u06FF' }
                val lyricPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = lyricFontSize
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val contentWidth = (cardRect.width() - innerPad * 2).toInt().coerceAtLeast(200)
                val alignment = if (isRtlText) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL

                val staticLayout = StaticLayout.Builder
                    .obtain(linesToRender, 0, linesToRender.length, lyricPaint, contentWidth)
                    .setAlignment(alignment)
                    .setLineSpacing(8f, 1.15f)
                    .setIncludePad(false)
                    .build()

                val availableLyricTop = thumbRect.bottom + 72f
                val availableLyricBottom = cardRect.bottom - 160f
                val lyricBlockY = (availableLyricTop + (availableLyricBottom - availableLyricTop - staticLayout.height) / 2f)
                    .coerceAtLeast(availableLyricTop)

                canvas.save()
                canvas.translate(cardRect.left + innerPad, lyricBlockY)
                staticLayout.draw(canvas)
                canvas.restore()

                // Card Footer: Brand Badge + Soundwave Code
                val footerY = cardRect.bottom - 76f
                drawSpotifyFooterInsideCard(
                    canvas = canvas,
                    leftX = cardRect.left + innerPad,
                    rightX = cardRect.right - innerPad,
                    centerY = footerY,
                    seed = track.title.hashCode(),
                    accentColor = pInt
                )
            } else {
                // =========================================================================
                // MODE B: AUTHENTIC SPOTIFY SONG / ALBUM SHARE CARD
                // =========================================================================
                val cardMarginH = 84f
                val cardTop = 240f
                val cardBottom = 1680f
                val cardRect = RectF(cardMarginH, cardTop, width - cardMarginH, cardBottom)

                val cardShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(150, 0, 0, 0)
                    maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawRoundRect(
                    RectF(cardRect.left + 8f, cardRect.top + 24f, cardRect.right - 8f, cardRect.bottom + 24f),
                    52f,
                    52f,
                    cardShadowPaint
                )

                val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        cardRect.left, cardRect.top, cardRect.left, cardRect.bottom,
                        intArrayOf(pInt, blendColors(sInt, Color.BLACK, 0.35f)),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(cardRect, 52f, 52f, cardFillPaint)

                val innerPad = 68f
                val artSize = cardRect.width() - innerPad * 2
                val artLeft = cardRect.left + innerPad
                val artTop = cardRect.top + innerPad
                val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)

                // Cover Drop Shadow
                val artShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(120, 0, 0, 0)
                    maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawRoundRect(
                    RectF(artRect.left + 8f, artRect.top + 18f, artRect.right - 8f, artRect.bottom + 18f),
                    32f,
                    32f,
                    artShadow
                )

                drawRoundedBitmapOrPlaceholder(
                    canvas = canvas,
                    bmp = loadedArtBmp,
                    rect = artRect,
                    cornerRadius = 32f,
                    fallbackColor = sInt
                )

                // Track Title & Artist
                val titleY = artRect.bottom + 96f
                val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 56f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(210, 255, 255, 255)
                    textSize = 38f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }

                val maxTextW = artSize
                val cleanTitle = android.text.TextUtils.ellipsize(
                    track.title,
                    titlePaint,
                    maxTextW,
                    android.text.TextUtils.TruncateAt.END
                ).toString()
                val cleanArtist = android.text.TextUtils.ellipsize(
                    track.artist,
                    artistPaint,
                    maxTextW,
                    android.text.TextUtils.TruncateAt.END
                ).toString()

                canvas.drawText(cleanTitle, artLeft, titleY, titlePaint)
                canvas.drawText(cleanArtist, artLeft, titleY + 62f, artistPaint)

                // Footer inside card
                val footerY = cardRect.bottom - 82f
                drawSpotifyFooterInsideCard(
                    canvas = canvas,
                    leftX = artLeft,
                    rightX = artRect.right,
                    centerY = footerY,
                    seed = track.title.hashCode(),
                    accentColor = pInt
                )
            }

            val imagesDir = File(context.cacheDir, "images")
            imagesDir.mkdirs()
            val safeId = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(imagesDir, "share_song_${safeId}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
            }

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("SongShareCard", "Error generating share card: ${e.message}", e)
            null
        }
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
                textSize = rect.width() * 0.32f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("♪", rect.centerX(), rect.centerY() + rect.width() * 0.1f, notePaint)
        }
    }

    private fun drawSpotifyFooterInsideCard(
        canvas: Canvas,
        leftX: Float,
        rightX: Float,
        centerY: Float,
        seed: Int,
        accentColor: Int
    ) {
        val circleRadius = 22f
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawCircle(leftX + circleRadius, centerY, circleRadius, circlePaint)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("♪", leftX + circleRadius, centerY + 9f, iconPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Aura Music", leftX + circleRadius * 2 + 16f, centerY + 10f, brandPaint)

        // Spotify-Code style waveform on the right
        val bars = 22
        val totalWaveW = 240f
        val waveStartX = rightX - totalWaveW
        val barW = 6.5f
        val step = totalWaveW / bars
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 255, 255, 255)
        }
        for (i in 0 until bars) {
            val norm = (sin(i * 0.65f + seed) * 0.38f + 0.56f).coerceIn(0.22f, 1f)
            val bh = 42f * norm
            val bx = waveStartX + i * step
            canvas.drawRoundRect(
                RectF(bx, centerY - bh / 2f, bx + barW, centerY + bh / 2f),
                barW / 2f,
                barW / 2f,
                wavePaint
            )
        }
    }

    private fun blendColors(c1: Int, c2: Int, ratio: Float): Int {
        val r = (Color.red(c1) * (1f - ratio) + Color.red(c2) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(c1) * (1f - ratio) + Color.green(c2) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(c1) * (1f - ratio) + Color.blue(c2) * ratio).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }
}
