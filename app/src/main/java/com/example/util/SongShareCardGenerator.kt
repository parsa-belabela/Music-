package com.example.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
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
            val aColor = palette.accent

            val pInt = android.graphics.Color.rgb((pColor.red * 255).toInt(), (pColor.green * 255).toInt(), (pColor.blue * 255).toInt())
            val sInt = android.graphics.Color.rgb((sColor.red * 255).toInt(), (sColor.green * 255).toInt(), (sColor.blue * 255).toInt())
            val aInt = android.graphics.Color.rgb((aColor.red * 255).toInt(), (aColor.green * 255).toInt(), (aColor.blue * 255).toInt())

            // Load artwork bitmap once if available
            var loadedArtBmp: Bitmap? = null
            if (!track.artworkUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(track.artworkUri)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        loadedArtBmp = BitmapFactory.decodeStream(input)
                    }
                } catch (_: Exception) {}
            }

            // 1. Rich Gradient + Blurred Artwork Backdrop
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(android.graphics.Color.rgb(8, 8, 18), pInt, sInt, android.graphics.Color.rgb(5, 6, 12)),
                    floatArrayOf(0.0f, 0.38f, 0.76f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Draw scaled soft-blurred background cover if available
            loadedArtBmp?.let { bmp ->
                try {
                    val tiny = Bitmap.createScaledBitmap(bmp, 36, 64, true)
                    val blurredBg = Bitmap.createScaledBitmap(tiny, width, height, true)
                    val bgBmpPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                        alpha = 78
                    }
                    canvas.drawBitmap(blurredBg, 0f, 0f, bgBmpPaint)
                } catch (_: Exception) {}
            }

            // Ambient Glow Orbs
            val orbPaint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    width * 0.5f, height * 0.32f, width * 0.62f,
                    intArrayOf(aInt, android.graphics.Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                alpha = 95
            }
            canvas.drawCircle(width * 0.5f, height * 0.32f, width * 0.62f, orbPaint)

            // 2. Liquid Glass Card Body
            val cardRect = RectF(78f, 170f, width - 78f, height - 220f)
            val cardBgPaint = Paint().apply {
                color = android.graphics.Color.argb(115, 10, 12, 24)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(cardRect, 64f, 64f, cardBgPaint)

            val cardBorderPaint = Paint().apply {
                color = android.graphics.Color.argb(135, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 3.5f
                isAntiAlias = true
            }
            canvas.drawRoundRect(cardRect, 64f, 64f, cardBorderPaint)

            val hasLyricLines = selectedLyricLines.isNotEmpty()
            val artSize = if (hasLyricLines) 500f else 650f
            val artLeft = (width - artSize) / 2f
            val artTop = if (hasLyricLines) 240f else 290f
            val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)

            // 3. 3D Shadow + Album Cover Area
            val shadowPaint = Paint().apply {
                color = android.graphics.Color.argb(120, 0, 0, 0)
                maskFilter = BlurMaskFilter(42f, BlurMaskFilter.Blur.NORMAL)
                isAntiAlias = true
            }
            canvas.drawRoundRect(
                RectF(artLeft + 10f, artTop + 22f, artLeft + artSize - 10f, artTop + artSize + 24f),
                48f,
                48f,
                shadowPaint
            )

            if (loadedArtBmp != null) {
                val path = Path().apply {
                    addRoundRect(artRect, 48f, 48f, Path.Direction.CW)
                }
                canvas.save()
                canvas.clipPath(path)
                canvas.drawBitmap(loadedArtBmp!!, null, artRect, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            } else {
                val placeholderPaint = Paint().apply {
                    shader = LinearGradient(artLeft, artTop, artLeft + artSize, artTop + artSize, pInt, sInt, Shader.TileMode.CLAMP)
                    isAntiAlias = true
                }
                canvas.drawRoundRect(artRect, 48f, 48f, placeholderPaint)

                val notePaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 130f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("♪", width / 2f, artTop + artSize / 2f + 45f, notePaint)
            }

            // Specular border around 3D cover
            val artBorderPaint = Paint().apply {
                color = android.graphics.Color.argb(160, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            canvas.drawRoundRect(artRect, 48f, 48f, artBorderPaint)

            // 4. Track Details
            val titleY = artTop + artSize + 88f
            val titlePaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = if (hasLyricLines) 52f else 58f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val title = if (track.title.length > 28) track.title.take(26) + "..." else track.title
            canvas.drawText(title, width / 2f, titleY, titlePaint)

            val artistPaint = Paint().apply {
                color = android.graphics.Color.argb(215, 205, 212, 235)
                textSize = 36f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val artist = if (track.artist.length > 34) track.artist.take(32) + "..." else track.artist
            canvas.drawText(artist, width / 2f, titleY + 58f, artistPaint)

            // 5. Spotify Lyric Card Block OR Classic Waveform Block
            if (hasLyricLines) {
                val lyricBoxTop = titleY + 105f
                val lyricBoxBottom = cardRect.bottom - 165f
                val lyricBoxRect = RectF(130f, lyricBoxTop, width - 130f, lyricBoxBottom)

                val lyricBoxPaint = Paint().apply {
                    color = android.graphics.Color.argb(120, 255, 255, 255)
                    shader = LinearGradient(
                        lyricBoxRect.left, lyricBoxRect.top, lyricBoxRect.right, lyricBoxRect.bottom,
                        intArrayOf(
                            android.graphics.Color.argb(55, 255, 255, 255),
                            android.graphics.Color.argb(25, 255, 255, 255)
                        ),
                        floatArrayOf(0f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    isAntiAlias = true
                }
                canvas.drawRoundRect(lyricBoxRect, 42f, 42f, lyricBoxPaint)

                // Accent vertical quote bar on the left
                val quoteBarPaint = Paint().apply {
                    color = aInt
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRoundRect(
                    RectF(lyricBoxRect.left + 32f, lyricBoxRect.top + 38f, lyricBoxRect.left + 44f, lyricBoxRect.bottom - 38f),
                    8f,
                    8f,
                    quoteBarPaint
                )

                val linesToDraw = selectedLyricLines.take(3)
                val linePaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = when (linesToDraw.size) {
                        1 -> 46f
                        2 -> 42f
                        else -> 38f
                    }
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                val totalLines = linesToDraw.size
                val lineSpacing = 68f
                val blockHeight = (totalLines - 1) * lineSpacing
                val startLineY = lyricBoxRect.centerY() - (blockHeight / 2f) + 14f

                linesToDraw.forEachIndexed { idx, rawLine ->
                    val clean = rawLine.trim().let { if (it.length > 42) it.take(40) + "…" else it }
                    canvas.drawText(clean, width / 2f, startLineY + idx * lineSpacing, linePaint)
                }
            } else {
                val waveTop = titleY + 115f
                val waveHeight = 80f
                val bars = waveformEnvelope ?: FloatArray(36) { (sin(it * 0.3) * 0.4 + 0.5).toFloat() }
                val barCount = bars.size.coerceAtMost(48)
                val totalWaveWidth = 600f
                val barWidth = totalWaveWidth / barCount * 0.65f
                val barGap = totalWaveWidth / barCount * 0.35f
                val waveStart = (width - totalWaveWidth) / 2f

                val wavePaint = Paint().apply {
                    color = aInt
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

                for (i in 0 until barCount) {
                    val h = (bars[i] * waveHeight).coerceIn(12f, waveHeight)
                    val bx = waveStart + i * (barWidth + barGap)
                    val by = waveTop + (waveHeight - h) / 2f
                    canvas.drawRoundRect(RectF(bx, by, bx + barWidth, by + h), 6f, 6f, wavePaint)
                }

                val snippet = lyricSnippet ?: if (lang == AppLanguage.PERSIAN) "در حال گوش دادن در آئورا موزیک" else "Now Listening on Aura Music"
                val snippetPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 34f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("“ $snippet ”", width / 2f, waveTop + 140f, snippetPaint)
            }

            // 6. Serato / Spotify Wave Code Footer inside Card
            val miniWaveY = cardRect.bottom - 90f
            val miniWavePaint = Paint().apply {
                color = android.graphics.Color.argb(200, 255, 255, 255)
                isAntiAlias = true
            }
            val codeBars = 28
            val codeTotalW = 420f
            val codeStartX = (width - codeTotalW) / 2f
            val codeBarW = 8f
            val codeStep = codeTotalW / codeBars
            for (i in 0 until codeBars) {
                val norm = (sin(i * 0.55f + track.title.hashCode()) * 0.4f + 0.55f).coerceIn(0.2f, 1f)
                val bh = 42f * norm
                canvas.drawRoundRect(
                    RectF(codeStartX + i * codeStep, miniWaveY - bh / 2f, codeStartX + i * codeStep + codeBarW, miniWaveY + bh / 2f),
                    4f,
                    4f,
                    miniWavePaint
                )
            }

            // 7. Aura Branding in bottom bar (Omitted for VIP users with watermark-free entitlement)
            val hasWatermarkFree = com.example.monetization.EntitlementManager.hasAccess(context, "share_card_no_watermark")
            if (!hasWatermarkFree) {
                val brandPaint = Paint().apply {
                    color = aInt
                    textSize = 36f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    letterSpacing = 0.15f
                }
                canvas.drawText("AURA MUSIC • LIQUID SOUND", width / 2f, height - 125f, brandPaint)
            }

            val imagesDir = File(context.cacheDir, "images")
            imagesDir.mkdirs()
            val file = File(imagesDir, "share_song_${track.id}_${System.currentTimeMillis()}.png")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Log.e("SongShareCard", "Error generating share card: ${e.message}", e)
            null
        }
    }
}
